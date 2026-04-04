package firstapp.helpdesk.admin;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class CompanyModel {
    private String id;
    private String name;
    private String domain;

    public CompanyModel() {} // Обязательно для Firebase

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
}