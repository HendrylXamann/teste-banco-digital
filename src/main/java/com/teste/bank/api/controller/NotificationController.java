package com.teste.bank.api.controller;

import com.teste.bank.api.dto.NotificationResponse;
import com.teste.bank.api.dto.PageResponse;
import com.teste.bank.api.mapper.NotificationMapper;
import com.teste.bank.application.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Notificações", description = "Notificações enviadas ao cliente após cada transferência concluída")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping("/{id}/notifications")
    @Operation(summary = "Lista as notificações recebidas pela conta, mais recentes primeiro",
            description = "O envio é simulado (log da aplicação). Cada notificação também é gravada no banco "
                    + "para comprovar o requisito: status SENT quando entregue, FAILED quando o envio "
                    + "falhou após as tentativas de retry. A notificação é assíncrona e pode levar "
                    + "alguns milissegundos para aparecer após a transferência.")
    public PageResponse<NotificationResponse> findByAccount(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(notificationQueryService.findByAccount(id, page, size),
                NotificationMapper::toResponse);
    }
}
