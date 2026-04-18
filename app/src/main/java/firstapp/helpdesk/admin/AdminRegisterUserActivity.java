package firstapp.helpdesk.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import firstapp.helpdesk.R;

public class AdminRegisterUserActivity extends AppCompatActivity {

    private EditText etName, etSurname, etPatronymic, etLogin, etPassword, etEmail, etPhone;
    private Spinner spinnerCompany;
    private Button btnCreate, btnAddCompany;
    private TextView tvBack;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private List<HousingComplexModel> housingList = new ArrayList<>();
    private List<String> housingNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_register_user);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initViews();
        loadHousingComplexes();

        btnCreate.setOnClickListener(v -> createRealUser());
        btnAddCompany.setOnClickListener(v -> startActivity(new Intent(this, AdminCreateHousingActivity.class)));
        tvBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etSurname = findViewById(R.id.et_surname);
        etPatronymic = findViewById(R.id.et_patronymic);
        etLogin = findViewById(R.id.et_login);
        etPassword = findViewById(R.id.et_password);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        spinnerCompany = findViewById(R.id.spinner_company);
        btnCreate = findViewById(R.id.btn_create);
        btnAddCompany = findViewById(R.id.btn_add_company_quick);
        tvBack = findViewById(R.id.tv_back);
    }

    private void loadHousingComplexes() {
        mDatabase.child("HousingComplexes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                housingList.clear();
                housingNames.clear();
                housingNames.add("Выберите ЖК...");
                for (DataSnapshot ds : snapshot.getChildren()) {
                    HousingComplexModel hc = ds.getValue(HousingComplexModel.class);
                    if (hc != null) {
                        housingList.add(hc);
                        housingNames.add(hc.getName());
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminRegisterUserActivity.this, android.R.layout.simple_spinner_item, housingNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCompany.setAdapter(adapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void createRealUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        int companyPos = spinnerCompany.getSelectedItemPosition();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || companyPos == 0) {
            Toast.makeText(this, "Заполните все данные и выберите ЖК", Toast.LENGTH_SHORT).show();
            return;
        }

        String companyId = housingList.get(companyPos - 1).getId();
        String companyName = housingList.get(companyPos - 1).getName();

        btnCreate.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToDatabase(user.getUid(), email, companyId, companyName);
                        }
                    } else {
                        btnCreate.setEnabled(true);
                        Toast.makeText(this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToDatabase(String uid, String email, String companyId, String companyName) {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String patronymic = etPatronymic.getText().toString().trim();
        
        // Создаем поле для поиска (Фамилия Имя Отчество в нижнем регистре)
        String searchName = (surname + " " + name + " " + patronymic).toLowerCase();

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("email", email);
        userData.put("name", name);
        userData.put("surname", surname);
        userData.put("patronymic", patronymic);
        userData.put("phone", etPhone.getText().toString().trim());
        userData.put("companyId", companyId);
        userData.put("companyName", companyName);
        userData.put("role", "user");
        userData.put("search_name", searchName);

        mDatabase.child("users").child(uid).setValue(userData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Житель успешно зарегистрирован", Toast.LENGTH_SHORT).show();
                finish();
            }
            btnCreate.setEnabled(true);
        });
    }
}
