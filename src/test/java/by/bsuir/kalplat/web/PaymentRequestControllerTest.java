package by.bsuir.kalplat.web;

import by.bsuir.kalplat.web.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Проверка программного интерфейса заявок на оплату. */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentRequestControllerTest {

    private static final long INITIATOR = 1L;
    private static final long CFO_MANAGER = 2L;
    private static final long TREASURER = 3L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String requestBody(String amount) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "cfoId", 1,
                "contractId", 1,
                "amount", amount,
                "purpose", "Оплата по счёту № 148",
                "plannedDate", LocalDate.now().plusDays(10).toString()));
    }

    @Test
    @DisplayName("Запрос без заголовка идентификации завершается кодом 401")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Создание заявки казначеем запрещено: код 403")
    void forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .header(CurrentUser.HEADER, TREASURER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("1000.00")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Заявка без обязательных полей отклоняется с кодом 400")
    void validation() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .header(CurrentUser.HEADER, INITIATOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    @DisplayName("Полный цикл: создание, передача на согласование, согласование, одобрение")
    void fullApprovalFlow() throws Exception {
        String created = mockMvc.perform(post("/api/v1/requests")
                        .header(CurrentUser.HEADER, INITIATOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("125000.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.overLimit").value(false))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/api/v1/requests/" + id + "/submit")
                        .header(CurrentUser.HEADER, INITIATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_APPROVAL"));

        mockMvc.perform(post("/api/v1/requests/" + id + "/approve")
                        .header(CurrentUser.HEADER, CFO_MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CFO_APPROVED"));

        mockMvc.perform(post("/api/v1/requests/" + id + "/approve")
                        .header(CurrentUser.HEADER, TREASURER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Повторная передача на согласование завершается кодом 409")
    void conflictOnWrongStatus() throws Exception {
        String created = mockMvc.perform(post("/api/v1/requests")
                        .header(CurrentUser.HEADER, INITIATOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("5000.00")))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/api/v1/requests/" + id + "/submit")
                .header(CurrentUser.HEADER, INITIATOR)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/requests/" + id + "/submit")
                        .header(CurrentUser.HEADER, INITIATOR))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Несуществующая заявка возвращает код 404")
    void notFound() throws Exception {
        mockMvc.perform(get("/api/v1/requests/9999")
                        .header(CurrentUser.HEADER, INITIATOR))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Отклонение заявки фиксирует причину")
    void rejectKeepsReason() throws Exception {
        String created = mockMvc.perform(post("/api/v1/requests")
                        .header(CurrentUser.HEADER, INITIATOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody("9000.00")))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();
        mockMvc.perform(post("/api/v1/requests/" + id + "/submit")
                .header(CurrentUser.HEADER, INITIATOR));

        mockMvc.perform(post("/api/v1/requests/" + id + "/reject")
                        .header(CurrentUser.HEADER, CFO_MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Не подтверждена потребность\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("Не подтверждена потребность"));
    }
}
