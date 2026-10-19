package by.bsuir.kalplat.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Описание программного интерфейса в формате OpenAPI 3.1. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI kalplatOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("КалПлат: Платежный календарь. REST API")
                        .version("1.0.0")
                        .description("Серверная часть программного средства "
                                + "автоматизации управления платежным календарём "
                                + "предприятия. Идентификатор пользователя "
                                + "передаётся в заголовке X-User-Id.")
                        .contact(new Contact().name("Фурсов А. В., гр. 314302")
                                .email("fursov@bsuir.by"))
                        .license(new License().name("MIT")))
                .servers(List.of(new Server().url("http://localhost:8080")
                        .description("Сервер разработки")));
    }
}
