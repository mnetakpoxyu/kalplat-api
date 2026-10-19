package by.bsuir.kalplat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Представление платежа в ответах API. */
@Schema(description = "Платёж по заявке")
public record PaymentViewDto(
        @Schema(description = "Идентификатор платежа", example = "1") Long id,
        @Schema(description = "Идентификатор заявки", example = "1") Long requestId,
        @Schema(description = "Номер платёжного поручения", example = "PP-000123")
        String orderNumber,
        @Schema(description = "Сумма платежа, руб.", example = "125000.00")
        BigDecimal amount,
        @Schema(description = "Статус платежа", example = "EXECUTED") String status,
        @Schema(description = "Наименование статуса", example = "Исполнен")
        String statusTitle,
        @Schema(description = "Дата и время исполнения") LocalDateTime paidAt) {
}
