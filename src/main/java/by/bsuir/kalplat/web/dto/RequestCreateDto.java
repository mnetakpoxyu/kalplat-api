package by.bsuir.kalplat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Данные для создания заявки на оплату. */
@Schema(description = "Запрос на создание заявки на оплату")
public record RequestCreateDto(
        @Schema(description = "Идентификатор центра финансовой ответственности",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "не указан центр финансовой ответственности")
        Long cfoId,

        @Schema(description = "Идентификатор договора – основания платежа",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "не указан договор")
        Long contractId,

        @Schema(description = "Сумма заявки, руб.", example = "125000.00",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "не указана сумма заявки")
        @DecimalMin(value = "0.01", message = "сумма заявки должна быть положительной")
        BigDecimal amount,

        @Schema(description = "Назначение платежа",
                example = "Оплата по счёту № 148 от 12.10.2026",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "не указано назначение платежа")
        @Size(max = 255, message = "назначение платежа длиннее 255 символов")
        String purpose,

        @Schema(description = "Плановая дата оплаты", example = "2026-11-05",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "не указана плановая дата оплаты")
        @FutureOrPresent(message = "плановая дата оплаты не может быть в прошлом")
        LocalDate plannedDate) {
}
