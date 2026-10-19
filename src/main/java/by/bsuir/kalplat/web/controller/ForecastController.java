package by.bsuir.kalplat.web.controller;

import by.bsuir.kalplat.service.ForecastService;
import by.bsuir.kalplat.web.dto.ForecastDto;
import by.bsuir.kalplat.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ресурс «Прогноз ликвидности». */
@RestController
@RequestMapping("/api/v1/forecast")
@Tag(name = "Прогноз ликвидности",
     description = "Расчёт прогнозного остатка денежных средств и кассовых разрывов")
public class ForecastController {

    private final ForecastService forecastService;

    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @GetMapping
    @Operation(summary = "UC-10 Получить прогноз ликвидности")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Прогноз рассчитан"),
        @ApiResponse(responseCode = "400", description = "Недопустимый горизонт прогноза",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ForecastDto forecast(
            @Parameter(description = "Дата начала прогноза", example = "2026-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "Горизонт прогнозирования, дней", example = "14")
            @RequestParam(defaultValue = "14") int horizon,
            @Parameter(description = "Начальный остаток денежных средств, руб.",
                    example = "750000.00")
            @RequestParam BigDecimal opening) {
        return forecastService.forecast(start, horizon, opening);
    }
}
