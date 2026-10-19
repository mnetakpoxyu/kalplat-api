package by.bsuir.kalplat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Представление заявки на оплату в ответах API. */
@Schema(description = "Заявка на оплату")
public record RequestViewDto(
        @Schema(description = "Идентификатор заявки", example = "1") Long id,
        @Schema(description = "Инициатор платежа") String author,
        @Schema(description = "Центр финансовой ответственности") String cfo,
        @Schema(description = "Договор – основание платежа") String contract,
        @Schema(description = "Контрагент") String contractor,
        @Schema(description = "Сумма заявки, руб.", example = "125000.00")
        BigDecimal amount,
        @Schema(description = "Назначение платежа") String purpose,
        @Schema(description = "Плановая дата оплаты", example = "2026-11-05")
        LocalDate plannedDate,
        @Schema(description = "Статус заявки", example = "ON_APPROVAL") String status,
        @Schema(description = "Наименование статуса", example = "На согласовании")
        String statusTitle,
        @Schema(description = "Признак превышения лимита бюджета", example = "false")
        boolean overLimit,
        @Schema(description = "Причина отклонения заявки") String rejectReason,
        @Schema(description = "Дата и время создания") LocalDateTime createdAt) {
}
