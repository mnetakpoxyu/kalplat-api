package by.bsuir.kalplat.domain.model;

/** Статус исполнения платежа. */
public enum PaymentStatus {
    PREPARED("Подготовлен"),
    EXECUTED("Исполнен"),
    REJECTED("Отклонён банком");

    private final String title;

    PaymentStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
