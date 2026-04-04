package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import firstapp.helpdesk.R;

public class AdminRegisterUserActivity extends AppCompatActivity {


    private EditText etName, etSurname, etPatronymic, etLogin, etPassword, etEmail, etPhone;
    private Spinner spinnerDept;
    private Button btnCreate;
    private TextView tvBack;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_register_user);

        mAuth = FirebaseAuth.getInstance();
        // Указываем путь к узлу "users"
        mDatabase = FirebaseDatabase.getInstance().getReference("users");

        initViews();
        setupDepartmentSpinner();

        btnCreate.setOnClickListener(v -> createRealUser());
        tvBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        // Связываем с ID из XML файла
        etName = findViewById(R.id.et_name);
        etSurname = findViewById(R.id.et_surname);
        etPatronymic = findViewById(R.id.et_patronymic);
        etLogin = findViewById(R.id.et_login);
        etPassword = findViewById(R.id.et_password);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        spinnerDept = findViewById(R.id.spinner_dept);
        btnCreate = findViewById(R.id.btn_create);
        tvBack = findViewById(R.id.tv_back);
    }

    private void setupDepartmentSpinner() {
        String[] departments = {"Выберите отдел...", "IT", "Бухгалтерия", "Администрация", "Тех. поддержка"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, departments);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDept.setAdapter(adapter);
    }

    private void createRealUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String dept = spinnerDept.getSelectedItem().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Заполните Email и Пароль!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Минимум 6 символов");
            return;
        }

        if (dept.equals("Выберите отдел...")) {
            Toast.makeText(this, "Выберите отдел!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreate.setEnabled(false);
        btnCreate.setText("Регистрация...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid(), email, name, surname, dept);
                        }
                    } else {
                        btnCreate.setEnabled(true);
                        btnCreate.setText("Создать");
                        Toast.makeText(this, "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String email, String name, String surname, String dept) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("email", email);
        userData.put("name", name);
        userData.put("surname", surname);
        userData.put("patronymic", etPatronymic.getText().toString().trim());
        userData.put("login", etLogin.getText().toString().trim());
        userData.put("phone", etPhone.getText().toString().trim());
        userData.put("department", dept);
        userData.put("role", "user");

        mDatabase.child(uid).setValue(userData)
                .addOnCompleteListener(task -> {
                    btnCreate.setEnabled(true);
                    btnCreate.setText("Создать");
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Пользователь создан!", Toast.LENGTH_SHORT).show();
                        clearFields();
                        mAuth.signOut(); // Выходим, чтобы админ не залогинился как новый юзер
                    }
                });
    }

    private void clearFields() {
        etName.setText(""); etSurname.setText(""); etPatronymic.setText("");
        etLogin.setText(""); etEmail.setText(""); etPassword.setText("");
        etPhone.setText(""); spinnerDept.setSelection(0);
    }
}