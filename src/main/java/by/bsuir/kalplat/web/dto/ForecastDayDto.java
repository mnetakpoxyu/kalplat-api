package by.bsuir.kalplat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Прогноз остатка денежных средств на один день. */
@Schema(description = "Прогноз на день")
public record ForecastDayDto(
        @Schema(description = "Дата", example = "2026-11-05") LocalDate date,
        @Schema(description = "Плановый отток денежных средств, руб.")
        BigDecimal outflow,
        @Schema(description = "Прогнозный остаток на конец дня, руб.")
        BigDecimal balance,
        @Schema(description = "Признак кассового разрыва", example = "false")
        boolean gap) {
}
