package by.bsuir.kalplat.service;

import by.bsuir.kalplat.web.dto.ForecastDto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Прогнозирование ликвидности и выявление кассовых разрывов. */
public interface ForecastService {

    ForecastDto forecast(LocalDate start, int horizon, BigDecimal opening);
}
