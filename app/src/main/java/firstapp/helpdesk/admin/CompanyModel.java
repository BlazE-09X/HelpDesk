package firstapp.helpdesk.admin;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class CompanyModel {
    private String id;
    private String name;
    private String domain; // Может использоваться как уникальный код
    private Map<String, Boolean> workers = new HashMap<>(); // Список ID исполнителей
    private Map<String, Boolean> users = new HashMap<>();   // Список ID пользователей (клиентов)

    public CompanyModel() {}

    public CompanyModel(String name, String domain) {
        this.name = name;
        this.domain = domain;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public Map<String, Boolean> getWorkers() { return workers; }
    public void setWorkers(Map<String, Boolean> workers) { this.workers = workers; }

    public Map<String, Boolean> getUsers() { return users; }
    public void setUsers(Map<String, Boolean> users) { this.users = users; }
}