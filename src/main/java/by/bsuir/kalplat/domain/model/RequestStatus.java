package by.bsuir.kalplat.domain.model;

/** Статус заявки на оплату. */
public enum RequestStatus {
    DRAFT("Черновик"),
    ON_APPROVAL("На согласовании"),
    CFO_APPROVED("Согласована ЦФО"),
    APPROVED("Одобрена"),
    IN_REGISTRY("В реестре платежей"),
    PAID("Оплачена"),
    REJECTED("Отклонена");

    private final String title;

    RequestStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
