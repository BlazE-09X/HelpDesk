package firstapp.helpdesk.tickets;

public class Ticket {
    public String id;
    public String title;
    public String category;
    public String description;
    public String status;
    public String priority;
    public String executorId;
    public String executorName;

    // Пустой конструктор (обязателен для работы Firebase)
    public Ticket() {
    }

    // Конструктор для создания новой заявки
    public Ticket(String id, String title, String category, String description, String status, String priority, String executorId, String executorName) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.executorId = executorId;
        this.executorName = executorName;
    }
}
