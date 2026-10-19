package by.bsuir.kalplat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Причина отклонения заявки. */
@Schema(description = "Запрос на отклонение заявки")
public record RejectDto(
        @Schema(description = "Причина отклонения",
                example = "Превышен лимит бюджета центра финансовой ответственности",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "не указана причина отклонения")
        @Size(max = 255, message = "причина отклонения длиннее 255 символов")
        String reason) {
}
