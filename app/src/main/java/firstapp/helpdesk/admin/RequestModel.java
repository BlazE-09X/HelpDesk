package firstapp.helpdesk.admin;

public class RequestModel {
    private String requestId;     // Уникальный ID из Firebase
    private String title;         // Тема/Заголовок
    private String status;        // Статус (New, Working, Done)
    private String description;
    private String userId;        // ID автора (Пользователя)
    private String userName;      // Имя автора
    private String executorId;    // ID исполнителя
    private String category;
    private String priority;
    private String companyId;     // ID компании, к которой привязан запрос

    // Мультимедиа
    private String attachmentUrl;
    private String attachmentType; // "image" или "video"

    // Временные метки
    private long createdAt;
    private long deadline;

    public String imageUrl; // Ссылка на фото
    public String videoUrl; // Ссылка на видео

    // Не забудь добавить их в пустой конструктор и создать геттеры/сеттеры
    public RequestModel() {}

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    // Система отзывов
    private float rating;
    private String review;

    public RequestModel() {} // Обязателен для Firebase

    // Геттеры и сеттеры
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    // Совместимость со старым кодом (если используется getTopic или getTicketNumber)
    public String getTopic() { return title; }
    public void setTopic(String title) { this.title = title; }
    public String getTicketNumber() { return requestId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status != null ? status : "new"; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getExecutorId() { return executorId; }
    public void setExecutorId(String executorId) { this.executorId = executorId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getDeadline() { return deadline; }
    public void setDeadline(long deadline) { this.deadline = deadline; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
}