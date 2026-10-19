package by.bsuir.kalplat.web.controller;

import by.bsuir.kalplat.domain.model.Role;
import by.bsuir.kalplat.service.PaymentRegistryService;
import by.bsuir.kalplat.web.dto.RegistryDto;
import by.bsuir.kalplat.web.error.ErrorResponse;
import by.bsuir.kalplat.web.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ресурс «Реестр платежей». */
@RestController
@RequestMapping("/api/v1/registry")
@Tag(name = "Реестр платежей",
     description = "Формирование реестра платежей и исполнение платёжных поручений")
public class PaymentRegistryController {

    private final PaymentRegistryService registryService;

    public PaymentRegistryController(PaymentRegistryService registryService) {
        this.registryService = registryService;
    }

    @GetMapping
    @Operation(summary = "UC-8 Сформировать реестр платежей на дату",
               description = "Отбирает одобренные заявки с плановой датой оплаты не "
                       + "позднее заданной и распределяет остаток денежных средств "
                       + "согласно выбранному правилу приоритизации")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Реестр сформирован"),
        @ApiResponse(responseCode = "400", description = "Неизвестное правило приоритизации",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Операция доступна только казначею",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @RequireRole(Role.TREASURER)
    public RegistryDto build(
            @Parameter(description = "Дата реестра", example = "2026-11-05")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Остаток денежных средств, руб.", example = "500000.00")
            @RequestParam BigDecimal balance,
            @Parameter(description = "Правило приоритизации",
                    example = "BY_DUE_DATE")
            @RequestParam(required = false) String strategy) {
        return registryService.buildRegistry(date, balance, strategy);
    }

    @PostMapping("/execute")
    @Operation(summary = "UC-9 Исполнить реестр платежей",
               description = "Формирует платёжные поручения по заявкам реестра, "
                       + "переводит заявки в статус «Оплачена» и списывает лимит бюджета")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Реестр исполнен"),
        @ApiResponse(responseCode = "409", description = "Нет заявок, обеспеченных остатком",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @RequireRole(Role.TREASURER)
    public RegistryDto execute(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam BigDecimal balance,
            @RequestParam(required = false) String strategy) {
        return registryService.execute(date, balance, strategy);
    }
}
