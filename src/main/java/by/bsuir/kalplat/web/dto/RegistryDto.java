package by.bsuir.kalplat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Реестр платежей на дату. */
@Schema(description = "Реестр платежей")
public record RegistryDto(
        @Schema(description = "Дата реестра", example = "2026-11-05") LocalDate date,
        @Schema(description = "Применённое правило приоритизации",
                example = "BY_DUE_DATE") String strategy,
        @Schema(description = "Остаток денежных средств, руб.", example = "500000.00")
        BigDecimal accountBalance,
        @Schema(description = "Сумма включённых в реестр заявок, руб.")
        BigDecimal includedAmount,
        @Schema(description = "Заявки, включённые в реестр")
        List<RequestViewDto> included,
        @Schema(description = "Заявки, перенесённые на следующий платёжный день")
        List<RequestViewDto> postponed) {
}
