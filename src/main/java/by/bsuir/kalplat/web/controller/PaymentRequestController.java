package by.bsuir.kalplat.web.controller;

import by.bsuir.kalplat.domain.model.PaymentRequest;
import by.bsuir.kalplat.domain.model.RequestStatus;
import by.bsuir.kalplat.domain.model.Role;
import by.bsuir.kalplat.domain.model.User;
import by.bsuir.kalplat.service.PaymentRequestService;
import by.bsuir.kalplat.web.dto.RejectDto;
import by.bsuir.kalplat.web.dto.RequestCreateDto;
import by.bsuir.kalplat.web.dto.RequestViewDto;
import by.bsuir.kalplat.web.error.ErrorResponse;
import by.bsuir.kalplat.web.mapper.PaymentRequestMapper;
import by.bsuir.kalplat.web.security.CurrentUser;
import by.bsuir.kalplat.web.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Ресурс «Заявки на оплату».
 * Контроллер принимает запросы, передаёт их сервисному слою и преобразует
 * результат в объекты передачи данных; бизнес-правила в контроллере не
 * реализуются.
 */
@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Заявки на оплату",
     description = "Регистрация, согласование и отклонение заявок на оплату")
public class PaymentRequestController {

    private final PaymentRequestService requestService;
    private final PaymentRequestMapper mapper;
    private final CurrentUser currentUser;

    public PaymentRequestController(PaymentRequestService requestService,
                                    PaymentRequestMapper mapper,
                                    CurrentUser currentUser) {
        this.requestService = requestService;
        this.mapper = mapper;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "UC-1 Получить список заявок на оплату",
               description = "Возвращает заявки, при необходимости отфильтрованные "
                       + "по статусу обработки")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список заявок получен"),
        @ApiResponse(responseCode = "401", description = "Пользователь не определён",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public List<RequestViewDto> list(
            @Parameter(description = "Статус заявки", example = "ON_APPROVAL")
            @RequestParam(required = false) RequestStatus status) {
        return mapper.toDtoList(requestService.findAll(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "UC-2 Получить заявку по идентификатору")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Заявка найдена"),
        @ApiResponse(responseCode = "404", description = "Заявка не найдена",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public RequestViewDto get(@PathVariable Long id) {
        return mapper.toDto(requestService.findById(id));
    }

    @PostMapping
    @Operation(summary = "UC-3 Создать заявку на оплату",
               description = "Создаёт заявку со статусом «Черновик» и проверяет "
                       + "остаток лимита бюджета центра финансовой ответственности")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Заявка создана"),
        @ApiResponse(responseCode = "400", description = "Реквизиты заявки не заполнены",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Договор или бюджет не найдены",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @RequireRole(Role.INITIATOR)
    public ResponseEntity<RequestViewDto> create(@Valid @RequestBody RequestCreateDto dto) {
        User author = currentUser.get();
        PaymentRequest request = requestService.create(dto, author);
        return ResponseEntity
                .created(UriComponentsBuilder.fromPath("/api/v1/requests/{id}")
                        .buildAndExpand(request.getId()).toUri())
                .body(mapper.toDto(request));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "UC-4 Передать заявку на согласование")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Заявка передана на согласование"),
        @ApiResponse(responseCode = "409", description = "Недопустимый статус заявки",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @RequireRole(Role.INITIATOR)
    public RequestViewDto submit(@PathVariable Long id) {
        User author = currentUser.get();
        return mapper.toDto(requestService.submit(id, author));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "UC-5 Согласовать заявку",
               description = "Руководитель ЦФО согласовывает заявку, казначей "
                       + "одобряет её с резервированием лимита бюджета")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Решение сохранено"),
        @ApiResponse(responseCode = "403", description = "Операция недоступна роли",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Недопустимый статус заявки",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @RequireRole({Role.CFO_MANAGER, Role.TREASURER})
    public RequestViewDto approve(@PathVariable Long id) {
        User user = currentUser.get();
        return mapper.toDto(requestService.approve(id, user));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "UC-6 Отклонить заявку")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Заявка отклонена"),
        @ApiResponse(responseCode = "400", description = "Не указана причина отклонения",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Операция недоступна роли",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    @RequireRole({Role.CFO_MANAGER, Role.TREASURER})
    public RequestViewDto reject(@PathVariable Long id,
                                 @Valid @RequestBody RejectDto dto) {
        User user = currentUser.get();
        return mapper.toDto(requestService.reject(id, user, dto.reason()));
    }

    @GetMapping("/statuses")
    @Operation(summary = "UC-7 Получить справочник статусов заявок")
    @ApiResponse(responseCode = "200", description = "Справочник получен")
    public List<String> statuses() {
        return List.of(RequestStatus.values()).stream()
                .map(status -> status.name() + " – " + status.getTitle())
                .toList();
    }

}
