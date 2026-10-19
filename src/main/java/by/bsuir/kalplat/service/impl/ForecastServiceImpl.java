package by.bsuir.kalplat.service.impl;

import by.bsuir.kalplat.repository.PaymentRequestRepository;
import by.bsuir.kalplat.service.ForecastService;
import by.bsuir.kalplat.web.dto.ForecastDayDto;
import by.bsuir.kalplat.web.dto.ForecastDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Прогнозирование ликвидности.
 * Расчёт выполнен простым последовательным проходом по дням горизонта
 * планирования без промежуточных абстракций, что соответствует принципу KISS.
 */
@Service
public class ForecastServiceImpl implements ForecastService {

    private static final int MAX_HORIZON = 180;

    private final PaymentRequestRepository requests;

    public ForecastServiceImpl(PaymentRequestRepository requests) {
        this.requests = requests;
    }

    @Override
    public ForecastDto forecast(LocalDate start, int horizon, BigDecimal opening) {
        if (horizon < 1 || horizon > MAX_HORIZON) {
            throw new IllegalArgumentException(
                    "Горизонт прогнозирования должен быть в диапазоне 1..." + MAX_HORIZON);
        }
        List<ForecastDayDto> days = new ArrayList<>(horizon);
        List<String> recommendations = new ArrayList<>();
        BigDecimal balance = opening;

        for (int day = 0; day < horizon; day++) {
            LocalDate date = start.plusDays(day);
            BigDecimal outflow = requests.plannedOutflow(date);
            balance = balance.subtract(outflow);
            boolean gap = balance.signum() < 0;
            if (gap) {
                recommendations.add(date + ": дефицит " + balance.abs()
                        + " руб. – перенести платежи низшего приоритета");
            }
            days.add(new ForecastDayDto(date, outflow, balance, gap));
        }
        int gapCount = (int) days.stream().filter(ForecastDayDto::gap).count();
        return new ForecastDto(start, horizon, opening, gapCount, days, recommendations);
    }
}
