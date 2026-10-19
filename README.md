# КалПлат: Платежный календарь — REST API

Серверная часть программного средства автоматизации управления платежным
календарём предприятия. Лабораторная работа № 9 по дисциплине «Технологии
проектирования сложных информационных систем» (БГУИР, ФКП, гр. 314302).

## Функциональность

- регистрация заявок на оплату с контролем остатка лимита бюджета ЦФО;
- маршрут согласования: инициатор → руководитель ЦФО → казначей;
- формирование реестра платежей с выбираемым правилом приоритизации;
- исполнение платёжных поручений со списанием лимита бюджета;
- прогнозирование ликвидности и выявление кассовых разрывов.

## Технологии

Java 17, Spring Boot 3.3.5 (Web, Data JPA, Validation), springdoc-openapi 2.6,
H2 (разработка), MySQL 8 (эксплуатация), JUnit 5, MockMvc, Maven.

## Запуск

```bash
mvn spring-boot:run                 # профиль разработки, база H2 в памяти
mvn test                            # 12 автоматических тестов
java -jar target/kalplat-api-1.0.0.jar
```

Документация API: http://localhost:8080/swagger-ui.html
Описание OpenAPI: http://localhost:8080/v3/api-docs

Идентификатор пользователя передаётся в заголовке `X-User-Id`. Демонстрационные
пользователи: 1 — инициатор платежа, 2 — руководитель ЦФО, 3 — казначей,
4 — администратор.

## Пример использования

```bash
curl -X POST http://localhost:8080/api/v1/requests \
  -H "X-User-Id: 1" -H "Content-Type: application/json" \
  -d '{"cfoId":1,"contractId":1,"amount":125000.00,
       "purpose":"Оплата по счёту № 148","plannedDate":"2026-11-05"}'

curl -X POST http://localhost:8080/api/v1/requests/1/submit -H "X-User-Id: 1"
curl -X POST http://localhost:8080/api/v1/requests/1/approve -H "X-User-Id: 2"
curl -X POST http://localhost:8080/api/v1/requests/1/approve -H "X-User-Id: 3"
curl "http://localhost:8080/api/v1/registry?date=2026-11-10&balance=500000.00" \
  -H "X-User-Id: 3"
```

## Структура проекта

```
src/main/java/by/bsuir/kalplat/
├── domain/model/       сущности предметной области и правила переходов статусов
├── domain/strategy/    правила приоритизации платежей (паттерн «Стратегия»)
├── repository/         доступ к данным (Spring Data JPA)
├── service/            интерфейсы бизнес-логики
├── service/impl/       реализации бизнес-логики
├── web/controller/     ресурсы REST API
├── web/dto/            объекты передачи данных
├── web/mapper/         преобразование модели в представление
├── web/security/       идентификация пользователя и проверка роли
├── web/error/          единая обработка ошибок
└── config/             конфигурация OpenAPI, веб-слоя и демонстрационных данных
```

## Принципы и шаблоны

- **SRP** — контроллер, сервис, репозиторий, маппер и перехватчик решают по одной задаче;
- **OCP** — новое правило приоритизации добавляется реализацией `PaymentPriorityStrategy`;
- **LSP** — реализации стратегий взаимозаменяемы для `PaymentRegistryService`;
- **ISP** — сервисы разделены на `PaymentRequestService`, `BudgetService`, `PaymentRegistryService`, `ForecastService`;
- **DIP** — сервисы зависят от интерфейсов репозиториев, а не от их реализаций;
- **DRY** — обработка ошибок вынесена в `GlobalExceptionHandler`, преобразование — в `PaymentRequestMapper`;
- **KISS** — расчёт прогноза выполнен простым проходом по дням горизонта;
- **DI** — все зависимости внедряются через конструктор.
