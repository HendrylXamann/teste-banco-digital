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

Toda resposta de erro traz `title`, `detail` e um campo `code` estável (o nome do `ErrorCode`), para o cliente tratar o erro sem depender do texto.

| Situação | `code` | Status |
|---|---|---|
| Campos inválidos (com mapa `errors` por campo) | `INVALID_FIELDS` | 400 |
| JSON mal formatado ou corpo ausente | `MALFORMED_REQUEST` | 400 |
| Parâmetro com tipo errado (ex.: `/accounts/abc`) | `INVALID_PARAMETER` | 400 |
| Header `Idempotency-Key` ausente | `MISSING_HEADER` | 400 |
| Mesma conta de origem e destino / valor não positivo | `SAME_ACCOUNT_TRANSFER` / `INVALID_AMOUNT` | 400 |
| Conta inexistente | `ACCOUNT_NOT_FOUND` | 404 |
| Mesma `Idempotency-Key` processada em paralelo | `IDEMPOTENCY_KEY_CONFLICT` | 409 |
| Conta bloqueada por outra operação além do `lock_timeout` | `ACCOUNT_LOCKED` | 409 |
| Saldo insuficiente | `INSUFFICIENT_BALANCE` | 422 |
| Constraint do banco violada (defesa em profundidade) | `DATA_INTEGRITY_VIOLATION` | 422 |
| Banco fora do ar / sem conexão | `DATABASE_UNAVAILABLE` | 503 |
| Qualquer erro não previsto | `INTERNAL_ERROR` | 500 |

Erros 500 e 503 nunca expõem stack trace nem mensagem interna: o detalhe vai para o log, e o cliente recebe uma mensagem genérica.

## Decisões de design e arquitetura

### Arquitetura em camadas (layered)

O projeto usa **arquitetura em camadas**, com quatro camadas e dependências em um único sentido:

```
api  ──►  application  ──►  domain  ◄──  infrastructure
```

| Camada | Responsabilidade | Conteúdo |
|---|---|---|
| `api` | Entrada HTTP: recebe e devolve DTOs, valida o formato, traduz erros para HTTP | controllers, DTOs (`records`), mappers, `GlobalExceptionHandler` |
| `application` | Casos de uso: orquestra o domínio, controla transações e publica eventos | interfaces de serviço e suas `*Impl`, validador, evento, listener |
| `domain` | O negócio: o que é uma conta, uma transferência e o que pode dar errado | entidades, repositórios, `ErrorCode` e exceções, contrato `NotificationGateway` |
| `infrastructure` | Detalhes técnicos que implementam contratos do domínio | configuração (async, OpenAPI), implementação da notificação |

Regras que mantêm as camadas separadas: controllers nunca acessam repositórios nem devolvem entidades; a `application` não conhece DTOs HTTP (recebe `TransferCommand`); o `domain` não depende de nenhuma outra camada (nem de HTTP: o status de cada erro é decidido na `api`).

### CQS: escrita separada da leitura em transferências

Nas transferências, comando e consulta ficam em serviços diferentes (*Command Query Separation*):

- `TransferService` (escrita): transação de escrita, lock pessimista nas contas, validação de saldo, idempotência e evento de notificação.
- `TransferQueryService` (leitura): transação `readOnly`, sem lock, com paginação e ordenação do extrato.

Os dois lados mudam por motivos diferentes e têm requisitos de desempenho opostos: a escrita serializa o acesso às contas, e a leitura não deve disputar esses locks. Separados, cada um evolui sem afetar o outro; a leitura poderia, por exemplo, passar a usar uma réplica do banco.

Em contas, a separação **não** foi aplicada: `AccountService` tem apenas um cadastro e uma busca por ID, sem lock, sem regra concorrente e sem consulta pesada. Separar ali seria cerimônia sem ganho. A regra que segui foi separar quando escrita e leitura têm necessidades diferentes de verdade.

### SOLID

- **S**: cada classe tem um motivo para mudar. `AccountServiceImpl` cuida de contas, `TransferServiceImpl` executa a transferência, `TransferQueryServiceImpl` monta o extrato (consulta separada da escrita), `TransferValidator` concentra as regras, mappers só convertem, `TransferNotificationListener` só reage ao evento, `GlobalExceptionHandler` só traduz erros para HTTP.
- **O**: um novo canal de notificação (e-mail, SMS, fila) é uma nova implementação de `NotificationGateway`, sem tocar no listener nem no serviço.
- **L**: qualquer implementação de `NotificationGateway` substitui a atual; os testes usam um mock da interface.
- **I**: `NotificationGateway` expõe um único método.
- **D**: os controllers dependem das interfaces `AccountService`, `TransferService` e `TransferQueryService`, não das implementações (`*Impl` em `application.service.impl`). O listener depende da abstração `NotificationGateway`, definida no domínio. Os controllers não conhecem repositórios, e a camada `application` não conhece DTOs HTTP (recebe `TransferCommand`).

### Por que camadas, e não hexagonal completo

Considerei arquitetura hexagonal: domínio sem anotações JPA, `AccountEntity`/`TransferEntity` e repositórios JPA em `infrastructure`, portas no domínio implementadas por adapters. Com o modelo anêmico, `Account` e `AccountEntity` seriam praticamente idênticas, e o ganho não compensaria cerca de doze classes a mais neste escopo. Por isso os repositórios estendem `JpaRepository` e ficam em `domain.repository`, um acoplamento ao Spring Data aceito conscientemente. Em um domínio maior, eu faria essa separação.

### Modelo anêmico (decisão consciente)

Considerei um modelo rico, com `debit()` e `credit()` na própria entidade `Account`. Optei pelo modelo anêmico pela simplicidade do escopo: as entidades carregam apenas dados e as regras ficam em `TransferValidator` e `TransferService`. Em um domínio maior, eu moveria essas regras para a entidade, para que nenhuma camada consiga alterar saldo sem passar pela regra.

### Concorrência: lock pessimista ordenado

- As duas contas são carregadas com `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`), dentro de uma única transação.
- O bloqueio acontece **sempre na ordem crescente de ID**, independentemente de quem é origem ou destino. Assim, A→B e B→A simultâneos nunca esperam um pelo outro em ordem cruzada, eliminando deadlock.
- `lock_timeout` de 3 s no PostgreSQL, configurado por conexão (`hikari.connection-init-sql`), evita threads presas indefinidamente; estourando, a API responde 409 `ACCOUNT_LOCKED`. A hint JPA `jakarta.persistence.lock.timeout` não foi usada porque o Hibernate não a aplica no PostgreSQL.
- **Por que não otimista** (`@Version` + retry): em contas muito disputadas o otimista gera colisões e retentativas em cascata. O pessimista serializa apenas as operações que tocam as mesmas contas, mantendo throughput nas demais.
- **Defesa em profundidade**: as regras são validadas no código antes de chegar ao banco, e o banco repete as críticas (`ck_accounts_balance_non_negative`, `ck_transfers_amount_positive`, `ck_transfers_distinct_accounts`). Pela API essas constraints nunca deveriam disparar; elas protegem contra bug, script manual ou outro sistema escrevendo na mesma base. Se dispararem, o handler responde 422 `DATA_INTEGRITY_VIOLATION`, e os testes de integração provam que o banco rejeita esses dados.
- As constraints recebem nomes explícitos na migration `V3`, em vez de editar a `V1` já aplicada, para que o handler identifique qual regra foi violada.

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

### Erros centralizados

Todas as mensagens de erro ficam no enum `ErrorCode` (título + template da mensagem). Cada exceção de negócio estende `BusinessException` e carrega o seu `ErrorCode`, e um único `@ExceptionHandler` resolve todas. O status HTTP **não** fica no enum de domínio: ele é mapeado em `ProblemDetailFactory`, na camada `api`, com um `switch` exaustivo. Assim o domínio não depende de HTTP, e criar um `ErrorCode` sem status vira erro de compilação. As mensagens de notificação seguem a mesma ideia no enum `NotificationMessage`.

### Outras decisões

- `BigDecimal` com escala 2 e `HALF_EVEN` para dinheiro; nunca `double`.
- PostgreSQL em vez de H2 para que o lock se comporte como em produção; Testcontainers prova isso nos testes.
- Flyway versiona o schema; `ddl-auto: validate` garante que entidade e schema estão alinhados.
- `open-in-view: false` para não abrir conexões fora das transações.
- Lombok para reduzir boilerplate (`@Getter`/`@Setter` nas entidades, `@RequiredArgsConstructor` na injeção). `@Data` evitado em entidades JPA por gerar `equals`/`hashCode`/`toString` problemáticos com proxies do Hibernate. DTOs são `records`.
- Dockerfile multi-stage com usuário não-root; compose com healthcheck no banco (a aplicação só sobe com ele pronto) e na aplicação, via Actuator.
- Tipos primitivos (`int`) só onde o valor nunca é nulo e tem default, como `page`/`size` do extrato; wrappers (`Long`, `BigDecimal`) onde o nulo é possível ou significativo: o ID é nulo antes de persistir, e no DTO o nulo é o que permite ao `@NotNull` detectar campo ausente.

### Convenções de código adotadas

- Sem comentários no código: nomes de classes, métodos e variáveis se explicam.
- Imports sempre no topo, explícitos, sem curinga (`*`) e sem nomes totalmente qualificados no meio do código.
- No máximo **3 dependências injetadas por classe**, sempre via construtor.
- No máximo **20 linhas por método**; métodos maiores são quebrados em métodos privados com nomes descritivos.
- Variáveis locais sempre com `var`. O tipo fica explícito onde o Java exige: campos, parâmetros e retornos.
- Objetos de dados (entidades, records de request/response, comandos, eventos) são criados por **builder** (`@Builder` do Lombok). Nas entidades, o construtor sem argumentos é `protected` (exigência do JPA) e o completo é `private`, então o único jeito de montar uma entidade é pelo builder. Beans do Spring (services, gateways) e exceções continuam com construtor: os beans são instanciados pelo container via injeção, e exceções são criadas no ponto do erro com o `ErrorCode`.
- Controllers nunca expõem entidades: recebem e devolvem apenas DTOs (`records`), convertidos pelos mappers.

## Testes

| Tipo | Classes | O que garante |
|---|---|---|
| Unitário | `TransferValidatorTest`, `TransferServiceImplTest`, `AccountServiceImplTest`, `TransferQueryServiceImplTest`, `TransferNotificationListenerTest`, `LogNotificationGatewayTest`, `ErrorCodeTest`, `ProblemDetailFactoryTest` | Regras e limites (saldo exato, um centavo a menos, valor nulo/zero/negativo), ordem dos locks, idempotência, normalização de escala, paginação fora dos limites, evento publicado só em sucesso, todo `ErrorCode` com status |
| Web (`@WebMvcTest`) | `AccountControllerTest`, `TransferControllerTest` | Cada campo inválido, JSON mal formado, corpo ausente, tipo errado na URL, header ausente/em branco/longo demais, 404/409/422/500/503 com `code` correto e sem vazar detalhe interno |
| Integração (Testcontainers) | `TransferConcurrencyIT` | 100 transferências cruzadas simultâneas sem perder centavo; 50 débitos concorrentes sem saldo negativo; mesma chave em paralelo gera uma única transferência; constraints do banco rejeitando dados inválidos |

**Por que um único teste de integração.** A maior parte do comportamento é coberta por testes unitários e de controller, que são rápidos e isolam cada regra. O que eles não conseguem provar é o que depende do banco real: `SELECT ... FOR UPDATE`, ordem de lock sem deadlock, `lock_timeout`, a constraint única da idempotência e os `CHECK`s. Com mocks, um teste de concorrência passaria mesmo com o lock quebrado. Por isso existe um teste de integração, focado justamente no requisito mais crítico do desafio (consistência sob alta concorrência), rodando contra um PostgreSQL de verdade via Testcontainers. Fluxos simples, como cadastrar ou consultar conta, não ganhariam nada com um teste de integração além de lentidão.

O sufixo `IT` (*Integration Test*) é a convenção do Maven: `*Test` roda no `mvn test` (plugin Surefire, rápido, sem infraestrutura), e `*IT` roda no `mvn verify` (plugin Failsafe), porque precisa de Docker e é mais lento.

Os dados de teste ficam em fixtures (`AccountFixture`, `TransferFixture`, `Amounts`), com valores fixos e nomeados, para que uma falha seja reproduzível. Valores aleatórios aparecem só onde o conteúdo é irrelevante, como as chaves de idempotência (UUID). Nos testes de controller, os payloads são gerados a partir dos próprios records de request via `ObjectMapper`; JSON escrito à mão só existe no teste de JSON mal formado.

## Próximos passos em produção

- Padrão **Outbox** + mensageria (Kafka/RabbitMQ) para garantir entrega da notificação mesmo se a aplicação cair após o commit.
- Autenticação e autorização (OAuth2/JWT) e verificação de titularidade da conta de origem.
- Observabilidade: métricas (Micrometer/Prometheus), tracing distribuído e logs estruturados.
- Rate limiting por cliente.
- Expiração das chaves de idempotência.
