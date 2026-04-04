package firstapp.helpdesk.executor;

// Импортируем базовую модель из пакета user
import firstapp.helpdesk.user.RequestModel;

public class ExecuterRequestModel extends RequestModel {
    private String executorComment;

    public ExecuterRequestModel() {
        // Пустой конструктор нужен для Firebase
    }

    public String getExecutorComment() {
        return executorComment;
    }

    public void setExecutorComment(String executorComment) {
        this.executorComment = executorComment;
    }
}