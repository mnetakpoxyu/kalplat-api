package by.bsuir.kalplat.domain.model;

/** Роль пользователя системы. */
public enum Role {
    INITIATOR("Инициатор платежа"),
    CFO_MANAGER("Руководитель ЦФО"),
    TREASURER("Казначей"),
    ADMIN("Администратор");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
