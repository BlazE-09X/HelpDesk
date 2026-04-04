package firstapp.helpdesk.user;

public class RequestModel {
    private String title;
    private String status;
    private String userId;
    private String description;
    private String category;
    private String priority;
    private String userComment;
    private String imageUrl;
    private String videoUrl;
    private long timestamp;

    public RequestModel() {} // Обязательно для Firebase

    // Геттеры
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getUserId() { return userId; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getUserComment() { return userComment; }
    public String getImageUrl() { return imageUrl; }
    public String getVideoUrl() { return videoUrl; }
    public long getTimestamp() { return timestamp; }

    // Сеттеры
    public void setTitle(String title) { this.title = title; }
    public void setStatus(String status) { this.status = status; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setUserComment(String userComment) { this.userComment = userComment; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    // Additional getters for Admin compatibility if needed
    public String getTopic() { return title; }
    public String getTicketNumber() { return String.valueOf(timestamp).substring(Math.max(0, String.valueOf(timestamp).length() - 5)); }
}