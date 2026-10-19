package by.bsuir.kalplat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Результат прогнозирования ликвидности. */
@Schema(description = "Прогноз ликвидности")
public record ForecastDto(
        @Schema(description = "Дата начала прогноза", example = "2026-11-01")
        LocalDate start,
        @Schema(description = "Горизонт прогнозирования, дней", example = "14")
        int horizon,
        @Schema(description = "Начальный остаток денежных средств, руб.")
        BigDecimal opening,
        @Schema(description = "Число выявленных кассовых разрывов", example = "2")
        int gapCount,
        @Schema(description = "Прогноз по дням") List<ForecastDayDto> days,
        @Schema(description = "Рекомендации по устранению кассовых разрывов")
        List<String> recommendations) {
}
