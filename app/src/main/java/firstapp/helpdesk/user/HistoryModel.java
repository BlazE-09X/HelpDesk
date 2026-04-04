package firstapp.helpdesk.user;

public class HistoryModel {
    private String type; // "Вход", "Изменение пароля", "Изменение почты"
    private String date; // Например, "01:01. 01.01.2026"

    public HistoryModel() {} // Для Firebase

    public HistoryModel(String type, String date) {
        this.type = type;
        this.date = date;
    }

    public String getType() { return type; }
    public String getDate() { return date; }
}