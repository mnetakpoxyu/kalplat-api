# -*- coding: utf-8 -*-
"""Выполнение тест-кейсов проверки базовых пользовательских требований.

Каждый тест-кейс выполняется на работающем экземпляре программного средства
через его программный интерфейс; фактический результат сравнивается с
ожидаемым, итоги сохраняются для включения в отчёт.
"""
import datetime
import json
import os
import subprocess
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api/v1"
HERE = os.path.dirname(os.path.abspath(__file__))
TODAY = datetime.date.today()
USERS = {"инициатор": 1, "руководитель ЦФО": 2, "казначей": 3, "администратор": 4}


def call(method, path, user=None, body=None, params=None):
    url = BASE + path
    if params:
        url += "?" + "&".join(f"{k}={v}" for k, v in params.items())
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        request.add_header("Content-Type", "application/json")
    if user is not None:
        request.add_header("X-User-Id", str(user))
    try:
        with urllib.request.urlopen(request) as response:
            payload = response.read().decode("utf-8")
            return response.status, json.loads(payload) if payload else None
    except urllib.error.HTTPError as error:
        payload = error.read().decode("utf-8")
        return error.code, json.loads(payload) if payload else None


def create(amount, days, user=USERS["инициатор"], contract=1, cfo=1, purpose=None):
    return call("POST", "/requests", user, {
        "cfoId": cfo, "contractId": contract, "amount": amount,
        "purpose": purpose or "Оплата по счёту № 148 от 12.10.2026",
        "plannedDate": str(TODAY + datetime.timedelta(days=days))})


def approved(amount, days):
    _, request = create(amount, days)
    call("POST", f"/requests/{request['id']}/submit", USERS["инициатор"])
    call("POST", f"/requests/{request['id']}/approve", USERS["руководитель ЦФО"])
    status, data = call("POST", f"/requests/{request['id']}/approve",
                        USERS["казначей"])
    return data


CASES = []


def case(identifier, requirement, title, steps, expected, run):
    CASES.append({"id": identifier, "requirement": requirement, "title": title,
                  "steps": steps, "expected": expected, "run": run})


# ---------------------------------------------------------------- тест-кейсы
case("ТК-01", "ФТ-1", "Проверить регистрацию заявки на оплату",
     ["1 Передать запрос на создание заявки от имени инициатора платежа "
      "с корректными реквизитами",
      "2 Получить созданную заявку по идентификатору"],
     ["1 Заявка создана, код состояния 201, присвоен идентификатор, "
      "статус «Черновик»",
      "2 Заявка доступна по идентификатору, реквизиты совпадают с переданными"],
     lambda: (lambda status, data: (lambda got, view: {
         "ok": status == 201 and data["statusTitle"] == "Черновик"
               and got == 200 and view["amount"] == 125000.00,
         "fact": f"код {status}, статус «{data['statusTitle']}», "
                 f"повторное чтение: код {got}, сумма {view['amount']}"})(
             *call("GET", f"/requests/{data['id']}", USERS["инициатор"])))(
         *create(125000.00, 5)))

case("ТК-02", "ФТ-2", "Проверить контроль остатка лимита бюджета ЦФО",
     ["1 Создать заявку на сумму, превышающую остаток лимита бюджета",
      "2 Создать заявку на сумму в пределах остатка лимита"],
     ["1 Заявка создана с признаком превышения лимита overLimit = true",
      "2 Заявка создана с признаком overLimit = false"],
     lambda: (lambda over, normal: {
         "ok": over[1]["overLimit"] is True and normal[1]["overLimit"] is False,
         "fact": f"превышение: overLimit={over[1]['overLimit']}, "
                 f"в пределах лимита: overLimit={normal[1]['overLimit']}"})(
         create(9900000.00, 5), create(1000.00, 5)))

case("ТК-03", "ФТ-3", "Проверить передачу заявки на согласование",
     ["1 Создать заявку на оплату",
      "2 Передать заявку на согласование",
      "3 Повторно передать ту же заявку на согласование"],
     ["1 Заявка создана со статусом «Черновик»",
      "2 Статус заявки изменён на «На согласовании», код состояния 200",
      "3 Операция отклонена, код состояния 409"],
     lambda: (lambda request: (lambda first, second: {
         "ok": first[0] == 200 and first[1]["statusTitle"] == "На согласовании"
               and second[0] == 409,
         "fact": f"передача: код {first[0]}, статус «{first[1]['statusTitle']}»; "
                 f"повторная передача: код {second[0]}"})(
         call("POST", f"/requests/{request['id']}/submit", USERS["инициатор"]),
         call("POST", f"/requests/{request['id']}/submit", USERS["инициатор"])))(
         create(2500.00, 6)[1]))

case("ТК-04", "ФТ-4", "Проверить согласование заявки руководителем ЦФО",
     ["1 Создать заявку и передать её на согласование",
      "2 Согласовать заявку от имени руководителя ЦФО"],
     ["1 Статус заявки «На согласовании»",
      "2 Статус заявки изменён на «Согласована ЦФО», код состояния 200"],
     lambda: (lambda request: (lambda _, result: {
         "ok": result[0] == 200 and result[1]["statusTitle"] == "Согласована ЦФО",
         "fact": f"код {result[0]}, статус «{result[1]['statusTitle']}»"})(
         call("POST", f"/requests/{request['id']}/submit", USERS["инициатор"]),
         call("POST", f"/requests/{request['id']}/approve",
              USERS["руководитель ЦФО"])))(create(3200.00, 6)[1]))

case("ТК-05", "ФТ-5", "Проверить одобрение заявки казначеем",
     ["1 Создать заявку, передать на согласование и согласовать "
      "руководителем ЦФО",
      "2 Одобрить заявку от имени казначея"],
     ["1 Статус заявки «Согласована ЦФО»",
      "2 Статус заявки изменён на «Одобрена», сумма зарезервирована в бюджете"],
     lambda: (lambda data: {
         "ok": data["statusTitle"] == "Одобрена",
         "fact": f"статус «{data['statusTitle']}», заявка № {data['id']}"})(
         approved(4100.00, 6)))

case("ТК-06", "ФТ-6", "Проверить отклонение заявки с указанием причины",
     ["1 Создать заявку и передать её на согласование",
      "2 Отклонить заявку от имени руководителя ЦФО с указанием причины",
      "3 Отклонить заявку без указания причины"],
     ["1 Статус заявки «На согласовании»",
      "2 Статус заявки «Отклонена», причина сохранена, код состояния 200",
      "3 Операция отклонена, код состояния 400"],
     lambda: (lambda request: (lambda _, rejected, empty: {
         "ok": rejected[0] == 200 and rejected[1]["statusTitle"] == "Отклонена"
               and rejected[1]["rejectReason"] == "Не подтверждена потребность"
               and empty[0] == 400,
         "fact": f"отклонение: код {rejected[0]}, статус "
                 f"«{rejected[1]['statusTitle']}», причина "
                 f"«{rejected[1]['rejectReason']}»; без причины: код {empty[0]}"})(
         call("POST", f"/requests/{request['id']}/submit", USERS["инициатор"]),
         call("POST", f"/requests/{request['id']}/reject",
              USERS["руководитель ЦФО"], {"reason": "Не подтверждена потребность"}),
         call("POST", f"/requests/{request['id']}/reject",
              USERS["руководитель ЦФО"], {"reason": ""})))(create(5300.00, 6)[1]))

case("ТК-07", "ФТ-7", "Проверить просмотр списка заявок с фильтрацией",
     ["1 Запросить список всех заявок",
      "2 Запросить список заявок со статусом «Одобрена»"],
     ["1 Получен список заявок, код состояния 200",
      "2 В списке присутствуют только заявки со статусом «Одобрена»"],
     lambda: (lambda full, filtered: {
         "ok": full[0] == 200 and filtered[0] == 200
               and all(item["status"] == "APPROVED" for item in filtered[1]),
         "fact": f"всего заявок: {len(full[1])}, со статусом «Одобрена»: "
                 f"{len(filtered[1])}"})(
         call("GET", "/requests", USERS["казначей"]),
         call("GET", "/requests", USERS["казначей"], params={"status": "APPROVED"})))

case("ТК-08", "ФТ-8", "Проверить просмотр карточки заявки",
     ["1 Запросить заявку по существующему идентификатору",
      "2 Запросить заявку по несуществующему идентификатору"],
     ["1 Получена карточка заявки, код состояния 200",
      "2 Возвращена ошибка «Ресурс не найден», код состояния 404"],
     lambda: (lambda existing, missing: {
         "ok": existing[0] == 200 and missing[0] == 404,
         "fact": f"существующая заявка: код {existing[0]}; "
                 f"несуществующая: код {missing[0]}, сообщение "
                 f"«{missing[1]['message']}»"})(
         call("GET", "/requests/1", USERS["инициатор"]),
         call("GET", "/requests/9999", USERS["инициатор"])))

case("ТК-09", "ФТ-9", "Проверить получение справочника статусов заявок",
     ["1 Запросить справочник статусов заявок"],
     ["1 Получен перечень из семи статусов, код состояния 200"],
     lambda: (lambda status, data: {
         "ok": status == 200 and len(data) == 7,
         "fact": f"код {status}, получено значений: {len(data)}"})(
         *call("GET", "/requests/statuses", USERS["инициатор"])))

case("ТК-10", "ФТ-10", "Проверить формирование реестра платежей на дату",
     ["1 Одобрить две заявки на суммы 120 000 и 900 000 руб.",
      "2 Сформировать реестр платежей при остатке средств 500 000 руб."],
     ["1 Обе заявки получили статус «Одобрена»",
      "2 В реестр включена заявка, обеспеченная остатком средств; "
      "заявка на 900 000 руб. перенесена на следующий платёжный день"],
     lambda: (lambda first, second: (lambda status, registry: {
         "ok": status == 200 and any(r["id"] == first["id"]
                                     for r in registry["included"])
               and any(r["id"] == second["id"] for r in registry["postponed"]),
         "fact": f"код {status}, включено заявок: {len(registry['included'])} "
                 f"на сумму {registry['includedAmount']}, перенесено: "
                 f"{len(registry['postponed'])}"})(
         *call("GET", "/registry", USERS["казначей"], params={
             "date": str(TODAY + datetime.timedelta(days=9)),
             "balance": "500000.00", "strategy": "BY_DUE_DATE"})))(
         approved(120000.00, 7), approved(900000.00, 8)))

case("ТК-11", "ФТ-11", "Проверить выбор правила приоритизации платежей",
     ["1 Сформировать реестр с правилом BY_PENALTY",
      "2 Сформировать реестр с несуществующим правилом BY_UNKNOWN"],
     ["1 Реестр сформирован, применённое правило BY_PENALTY, код состояния 200",
      "2 Операция отклонена, код состояния 400"],
     lambda: (lambda valid, invalid: {
         "ok": valid[0] == 200 and valid[1]["strategy"] == "BY_PENALTY"
               and invalid[0] == 400,
         "fact": f"правило BY_PENALTY: код {valid[0]}, применено "
                 f"«{valid[1]['strategy']}»; неизвестное правило: код {invalid[0]}"})(
         call("GET", "/registry", USERS["казначей"], params={
             "date": str(TODAY + datetime.timedelta(days=9)),
             "balance": "500000.00", "strategy": "BY_PENALTY"}),
         call("GET", "/registry", USERS["казначей"], params={
             "date": str(TODAY + datetime.timedelta(days=9)),
             "balance": "500000.00", "strategy": "BY_UNKNOWN"})))

case("ТК-12", "ФТ-12", "Проверить исполнение реестра платежей",
     ["1 Одобрить заявку на сумму 70 000 руб.",
      "2 Исполнить реестр платежей при остатке средств 300 000 руб.",
      "3 Запросить карточку заявки"],
     ["1 Заявка получила статус «Одобрена»",
      "2 Реестр исполнен, код состояния 200",
      "3 Статус заявки изменён на «Оплачена»"],
     lambda: (lambda request: (lambda status, registry: (lambda code, view: {
         "ok": status == 200 and view["statusTitle"] == "Оплачена",
         "fact": f"исполнение: код {status}, исполнено заявок "
                 f"{len(registry['included'])}; статус заявки № {request['id']}: "
                 f"«{view['statusTitle']}»"})(
         *call("GET", f"/requests/{request['id']}", USERS["казначей"])))(
         *call("POST", "/registry/execute", USERS["казначей"], params={
             "date": str(TODAY + datetime.timedelta(days=11)),
             "balance": "300000.00"})))(approved(70000.00, 10)))

case("ТК-13", "ФТ-13", "Проверить прогнозирование ликвидности",
     ["1 Запросить прогноз на 14 дней при начальном остатке 100 000 руб.",
      "2 Запросить прогноз с недопустимым горизонтом 0 дней"],
     ["1 Получен прогноз по дням, выявлены кассовые разрывы, сформированы "
      "рекомендации, код состояния 200",
      "2 Операция отклонена, код состояния 400"],
     lambda: (lambda valid, invalid: {
         "ok": valid[0] == 200 and len(valid[1]["days"]) == 14
               and invalid[0] == 400,
         "fact": f"код {valid[0]}, дней в прогнозе: {len(valid[1]['days'])}, "
                 f"кассовых разрывов: {valid[1]['gapCount']}, рекомендаций: "
                 f"{len(valid[1]['recommendations'])}; недопустимый горизонт: "
                 f"код {invalid[0]}"})(
         call("GET", "/forecast", USERS["казначей"], params={
             "start": str(TODAY), "horizon": 14, "opening": "100000.00"}),
         call("GET", "/forecast", USERS["казначей"], params={
             "start": str(TODAY), "horizon": 0, "opening": "100000.00"})))

case("ТК-14", "ФТ-14", "Проверить разграничение доступа по ролям",
     ["1 Выполнить запрос без заголовка идентификации пользователя",
      "2 Создать заявку от имени казначея",
      "3 Сформировать реестр платежей от имени инициатора платежа"],
     ["1 Операция отклонена, код состояния 401",
      "2 Операция отклонена, код состояния 403",
      "3 Операция отклонена, код состояния 403"],
     lambda: (lambda anonymous, treasurer, initiator: {
         "ok": anonymous[0] == 401 and treasurer[0] == 403 and initiator[0] == 403,
         "fact": f"без идентификации: код {anonymous[0]}; создание заявки "
                 f"казначеем: код {treasurer[0]}; реестр инициатором: "
                 f"код {initiator[0]}"})(
         call("GET", "/requests"),
         create(1000.00, 5, user=USERS["казначей"]),
         call("GET", "/registry", USERS["инициатор"], params={
             "date": str(TODAY), "balance": "1000.00"})))


def main():
    results = []
    for item in CASES:
        started = time.perf_counter()
        try:
            outcome = item["run"]()
        except Exception as error:                                # noqa: BLE001
            outcome = {"ok": False, "fact": f"ошибка выполнения: {error}"}
        results.append({
            "id": item["id"], "requirement": item["requirement"],
            "title": item["title"], "steps": item["steps"],
            "expected": item["expected"], "fact": outcome["fact"],
            "passed": bool(outcome["ok"]),
            "duration_ms": round((time.perf_counter() - started) * 1000, 1)})
        print(("  пройден " if outcome["ok"] else "  ОШИБКА "),
              item["id"], item["title"])
    passed = sum(1 for r in results if r["passed"])
    print(f"\nвыполнено тест-кейсов: {len(results)}, пройдено: {passed}, "
          f"не пройдено: {len(results) - passed}")
    path = os.path.join(HERE, "testcases.json")
    json.dump({"date": str(TODAY), "cases": results},
              open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("результаты сохранены:", path)


if __name__ == "__main__":
    main()
