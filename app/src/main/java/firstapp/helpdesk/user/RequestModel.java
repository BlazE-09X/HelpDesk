package firstapp.helpdesk.user;

public class RequestModel {
    private String requestId;
    private String title;
    private String status;
    private String userId;
    private String executorId;
    private String description;
    private String category;
    private String priority;
    private String userComment;
    private String imageUrl;
    private String videoUrl;
    private long timestamp;
    private long startDate;
    private long deadlineDate;
    private String executionType; // "immediate", "planned", "deadline"
    private float rating; // Оценка от пользователя (1-5)

    public RequestModel() {} // Обязательно для Firebase

    // Геттеры
    public String getRequestId() { return requestId; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getUserId() { return userId; }
    public String getExecutorId() { return executorId; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getUserComment() { return userComment; }
    public String getImageUrl() { return imageUrl; }
    public String getVideoUrl() { return videoUrl; }
    public long getTimestamp() { return timestamp; }
    public long getStartDate() { return startDate; }
    public long getDeadlineDate() { return deadlineDate; }
    public String getExecutionType() { return executionType; }
    public float getRating() { return rating; }

    // Сеттеры
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setTitle(String title) { this.title = title; }
    public void setStatus(String status) { this.status = status; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setExecutorId(String executorId) { this.executorId = executorId; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setUserComment(String userComment) { this.userComment = userComment; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public void setDeadlineDate(long deadlineDate) { this.deadlineDate = deadlineDate; }
    public void setExecutionType(String executionType) { this.executionType = executionType; }
    public void setRating(float rating) { this.rating = rating; }
    
    // Additional getters for Admin compatibility if needed
    public String getTopic() { return title; }
    public String getTicketNumber() { 
        if (requestId != null && requestId.length() > 5) {
            return requestId.substring(requestId.length() - 5);
        }
        return String.valueOf(timestamp).substring(Math.max(0, String.valueOf(timestamp).length() - 5)); 
    }
}