package firstapp.helpdesk.admin;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class UserModel {
    private String id;
    private String uid;
    private String fullname;
    private String name;
    private String surname;
    private String patronymic;
    private String email;
    private String login;
    private String phone;
    private String role;
    private String department;
    private String companyId;
    private String companyName;
    private String specialization;
    private String search_name; // Новое поле для поиска

    public UserModel() {}

    public String getId() { return id != null ? id : uid; }
    public void setId(String id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getPatronymic() { return patronymic; }
    public void setPatronymic(String patronymic) { this.patronymic = patronymic; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getSearch_name() { return search_name; }
    public void setSearch_name(String search_name) { this.search_name = search_name; }
}