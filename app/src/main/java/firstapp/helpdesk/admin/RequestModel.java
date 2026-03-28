package firstapp.helpdesk.admin; // Проверь, чтобы пакет совпадал с твоим

public class RequestModel {
    private String ticketNumber;
    private String topic;
    private String status;

    // Пустой конструктор нужен для Firebase
    public RequestModel() {}

    public RequestModel(String ticketNumber, String topic, String status) {
        this.ticketNumber = ticketNumber;
        this.topic = topic;
        this.status = status;
    }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}