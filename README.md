# Teste Banco Digital

API REST simplificada de banco digital: cadastro e consulta de contas, transferência de valores entre contas com garantia de consistência sob alta concorrência, extrato paginado e notificação ao cliente após cada transferência concluída.

## Stack

Java 21 · Spring Boot 3.5 · Maven · PostgreSQL 16 · Flyway · springdoc-openapi (Swagger) · Resilience4j · Lombok · JUnit 5, Mockito, AssertJ, Testcontainers · Docker

## Como rodar

### Tudo em Docker (recomendado)

Pré-requisito: Docker com Docker Compose.

```bash
docker compose up --build
```

A aplicação sobe em `http://localhost:8080` assim que o PostgreSQL estiver saudável.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI (JSON): http://localhost:8080/v3/api-docs
- Health check: http://localhost:8080/actuator/health

### Modo desenvolvimento (app na IDE ou terminal)

Pré-requisitos: JDK 21 e Docker.

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

### Testes

```bash
./mvnw test      # unitários e de camada web, sem dependências externas
./mvnw verify    # inclui o teste de concorrência com PostgreSQL real via Testcontainers (requer Docker)
```

`src/test/resources/docker-java.properties` fixa `api.version=1.44`, necessário para o Testcontainers 1.21 conversar com o Docker Engine 29+ (Docker Desktop recente).

### Contas pré-carregadas

| ID | Nome  | Saldo inicial |
|----|-------|---------------|
| 1  | Ana   | 1000.00       |
| 2  | Bruno | 500.00        |
| 3  | Carla | 0.00          |

## Endpoints

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| POST | `/accounts` | Cria conta (nome e saldo inicial) | 201 + `Location` |
| GET | `/accounts/{id}` | Consulta conta e saldo | 200 |
| GET | `/accounts/{id}/transfers?page=0&size=20` | Extrato paginado, mais recentes primeiro | 200 |
| POST | `/transfers` | Transfere valor. Header obrigatório `Idempotency-Key` | 201 nova / 200 repetida |

### Exemplos

```bash
curl -X POST http://localhost:8080/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"Daniel","initialBalance":250.00}'

curl http://localhost:8080/accounts/1

curl -X POST http://localhost:8080/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 6f1c2a3e-0b7d-4a51-9a3e-2f1d0c9b8a71" \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":100.00}'

curl "http://localhost:8080/accounts/1/transfers?page=0&size=20"
```

### Erros (RFC 7807 — `ProblemDetail`)

| Situação | Status |
|---|---|
| Conta inexistente | 404 |
| Saldo insuficiente | 422 |
| Mesma conta de origem e destino, valor inválido, payload inválido, header ausente | 400 |
| Mesma `Idempotency-Key` processada em paralelo, ou conta bloqueada além do timeout | 409 |

## Decisões de design e arquitetura

### Camadas e SOLID

```
api             controllers, DTOs (records), mappers, tratamento global de erros
application     serviços de caso de uso, validador, evento e listener
domain          entidades, repositórios, exceções de negócio, contrato de notificação
infrastructure  configurações (async, OpenAPI) e implementação da notificação
```

- **S**: cada classe tem um motivo para mudar. `AccountService` cuida de contas, `TransferService` executa a transferência, `TransferQueryService` monta o extrato, `TransferValidator` concentra as regras, mappers só convertem, `TransferNotificationListener` só reage ao evento, `GlobalExceptionHandler` só traduz erros para HTTP.
- **O**: um novo canal de notificação (e-mail, SMS, fila) é uma nova implementação de `NotificationGateway`, sem tocar no listener nem no serviço.
- **L**: qualquer implementação de `NotificationGateway` substitui a atual; os testes usam um mock da interface.
- **I**: `NotificationGateway` expõe um único método.
- **D**: o listener depende da abstração `NotificationGateway`, definida no domínio, e não da implementação de infraestrutura. Os controllers não conhecem repositórios; a camada `application` não conhece DTOs HTTP (recebe `TransferCommand`).

Os repositórios estendem `JpaRepository` e ficam em `domain.repository`. Em um projeto maior eu separaria uma interface de domínio pura de um adapter JPA; aqui o acoplamento ao Spring Data foi aceito conscientemente pela simplicidade.

### Modelo anêmico (decisão consciente)

Considerei um modelo rico, com `debit()` e `credit()` na própria entidade `Account`. Optei pelo modelo anêmico pela simplicidade do escopo: as entidades carregam apenas dados e as regras ficam em `TransferValidator` e `TransferService`. Em um domínio maior, eu moveria essas regras para a entidade, para que nenhuma camada consiga alterar saldo sem passar pela regra.

### Concorrência: lock pessimista ordenado

- As duas contas são carregadas com `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`), dentro de uma única transação.
- O bloqueio acontece **sempre na ordem crescente de ID**, independentemente de quem é origem ou destino. Assim, A→B e B→A simultâneos nunca esperam um pelo outro em ordem cruzada, eliminando deadlock.
- Timeout de lock de 3 s (`jakarta.persistence.lock.timeout`) evita threads presas indefinidamente; estourando, a API responde 409.
- **Por que não otimista** (`@Version` + retry): em contas muito disputadas o otimista gera colisões e retentativas em cascata. O pessimista serializa apenas as operações que tocam as mesmas contas, mantendo throughput nas demais.
- **Defesa em profundidade**: `CHECK (balance >= 0)` e `CHECK (amount > 0)` no banco garantem a integridade mesmo que uma regra de aplicação falhe.

### Idempotência

`POST /transfers` exige o header `Idempotency-Key`. A chave tem constraint `UNIQUE`:

- chave já usada → devolve a transferência original com **200**, sem mover dinheiro de novo;
- mesma chave em requisições paralelas → apenas uma é efetivada; as concorrentes recebem 200 ou 409, nunca uma segunda transferência.

Isso protege contra reenvios do cliente (timeout de rede, duplo clique), o problema mais comum em pagamentos.

### Notificação: pós-commit, assíncrona e resiliente

- `TransferService` publica `TransferCompletedEvent`.
- `TransferNotificationListener` usa `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` em um pool dedicado: só notifica transferências efetivamente gravadas e não atrasa a resposta HTTP.
- O pagador e o recebedor são notificados.
- `LogNotificationGateway` simula o provedor (log) e é protegido por `@Retry` (3 tentativas) e `@CircuitBreaker`. A variável `NOTIFICATION_FAILURE_RATE` (0.0 a 1.0) simula falhas para demonstrar o comportamento.
- Falha na notificação **não** desfaz a transferência.

### Por que não há CRUD completo

- **Contas**: sem `PUT`, porque saldo só pode mudar por transferência; sem `DELETE`, porque a conta tem histórico financeiro.
- **Transferências**: registro financeiro é imutável. Correção se faz com estorno, uma nova transferência inversa.
- **Notificações**: são efeito colateral da transferência, não um recurso exposto na API.

### Outras decisões

- `BigDecimal` com escala 2 e `HALF_EVEN` para dinheiro; nunca `double`.
- PostgreSQL em vez de H2 para que o lock se comporte como em produção; Testcontainers prova isso nos testes.
- Flyway versiona o schema; `ddl-auto: validate` garante que entidade e schema estão alinhados.
- `open-in-view: false` para não abrir conexões fora das transações.
- Lombok para reduzir boilerplate (`@Getter`/`@Setter` nas entidades, `@RequiredArgsConstructor` na injeção). `@Data` evitado em entidades JPA por gerar `equals`/`hashCode`/`toString` problemáticos com proxies do Hibernate. DTOs são `records`.
- Dockerfile multi-stage com usuário não-root; compose com healthcheck para a aplicação só subir com o banco pronto.

### Convenções de código adotadas

- Sem comentários no código: nomes de classes, métodos e variáveis se explicam.
- Imports sempre no topo, explícitos, sem curinga (`*`) e sem nomes totalmente qualificados no meio do código.
- No máximo **3 dependências injetadas por classe**, sempre via construtor.
- No máximo **20 linhas por método**; métodos maiores são quebrados em métodos privados com nomes descritivos.

## Testes

| Tipo | Classes | O que garante |
|---|---|---|
| Unitário | `TransferValidatorTest`, `TransferServiceTest`, `AccountServiceTest`, `TransferQueryServiceTest`, `TransferNotificationListenerTest`, `LogNotificationGatewayTest` | Regras, ordem dos locks, idempotência, publicação do evento só em sucesso, direção do extrato |
| Web (`@WebMvcTest`) | `AccountControllerTest`, `TransferControllerTest` | Status HTTP, `Location`, validação, `ProblemDetail` |
| Integração (Testcontainers) | `TransferConcurrencyIT` | 100 transferências cruzadas simultâneas sem perder centavo; 50 débitos concorrentes sem saldo negativo; mesma chave em paralelo gera uma única transferência |

## Próximos passos em produção

- Padrão **Outbox** + mensageria (Kafka/RabbitMQ) para garantir entrega da notificação mesmo se a aplicação cair após o commit.
- Autenticação e autorização (OAuth2/JWT) e verificação de titularidade da conta de origem.
- Observabilidade: métricas (Micrometer/Prometheus), tracing distribuído e logs estruturados.
- Rate limiting por cliente.
- Expiração das chaves de idempotência.
