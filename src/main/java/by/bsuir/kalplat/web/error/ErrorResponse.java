package by.bsuir.kalplat.web.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/** Единый формат ответа об ошибке. */
@Schema(description = "Описание ошибки выполнения запроса")
public record ErrorResponse(
        @Schema(description = "Код состояния HTTP", example = "409")
        int status,
        @Schema(description = "Краткое наименование ошибки", example = "Conflict")
        String error,
        @Schema(description = "Описание причины ошибки")
        String message,
        @Schema(description = "Ошибки проверки отдельных полей запроса")
        List<String> details,
        @Schema(description = "Время формирования ответа")
        LocalDateTime timestamp) {

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, List.of(), LocalDateTime.now());
    }
}
