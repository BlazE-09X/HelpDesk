package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
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

public class AdminRegisterWorker extends AppCompatActivity {

    private EditText etEmail, etPassword, etName, etSurname;
    private Spinner spinnerSpecialization, spinnerCompany;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    
    private List<CompanyModel> companyList = new ArrayList<>();
    private List<String> companyNames = new ArrayList<>();
    private String fixedCompanyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_register_worker);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        fixedCompanyId = getIntent().getStringExtra("fixedCompanyId");

        etEmail = findViewById(R.id.et_worker_email);
        etPassword = findViewById(R.id.et_worker_password);
        etName = findViewById(R.id.et_worker_name);
        etSurname = findViewById(R.id.et_worker_surname);
        spinnerSpecialization = findViewById(R.id.spinner_worker_specialization);
        spinnerCompany = findViewById(R.id.spinner_worker_company);
        Button btnRegister = findViewById(R.id.btn_register_worker);

        setupSpecializationSpinner();
        
        if (fixedCompanyId != null) {
            findViewById(R.id.spinner_worker_company).setVisibility(View.GONE);
        } else {
            loadOrganizations();
        }

        btnRegister.setOnClickListener(v -> registerWorker());
    }

    private void setupSpecializationSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpecialization.setAdapter(adapter);
    }

    private void loadOrganizations() {
        mDatabase.child("Organizations").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                companyList.clear();
                companyNames.clear();
                companyNames.add("Выберите организацию...");
                for (DataSnapshot ds : snapshot.getChildren()) {
                    CompanyModel company = ds.getValue(CompanyModel.class);
                    if (company != null) {
                        company.setId(ds.getKey());
                        companyList.add(company);
                        companyNames.add(company.getName());
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminRegisterWorker.this, android.R.layout.simple_spinner_item, companyNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCompany.setAdapter(adapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void registerWorker() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String spec = spinnerSpecialization.getSelectedItem().toString();
        
        String companyId = fixedCompanyId;
        String companyName = "";
        
        if (companyId == null) {
            int companyPos = spinnerCompany.getSelectedItemPosition();
            if (companyPos == 0) {
                Toast.makeText(this, "Выберите организацию!", Toast.LENGTH_SHORT).show();
                return;
            }
            companyId = companyList.get(companyPos - 1).getId();
            companyName = companyList.get(companyPos - 1).getName();
        }

        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || surname.isEmpty()) {
            Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalCompanyId = companyId;
        final String finalCompanyName = companyName;
        final String finalSurname = surname;
        final String finalName = name;
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String workerUid = task.getResult().getUser().getUid();
                        saveWorkerToDatabase(workerUid, email, finalName, finalSurname, spec, finalCompanyId, finalCompanyName);
                    } else {
                        Toast.makeText(this, "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveWorkerToDatabase(String uid, String email, String name, String surname, String spec, String companyId, String companyName) {
        String searchName = (surname + " " + name).toLowerCase();

        Map<String, Object> workerData = new HashMap<>();
        workerData.put("uid", uid);
        workerData.put("email", email);
        workerData.put("name", name);
        workerData.put("surname", surname);
        workerData.put("role", "Исполнитель");
        workerData.put("specialization", spec);
        workerData.put("companyId", companyId);
        workerData.put("companyName", companyName);
        workerData.put("search_name", searchName);

        // Сохраняем в новую таблицу Workers
        mDatabase.child("Workers").child(uid).setValue(workerData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Дублируем в users для логина
                        mDatabase.child("users").child(uid).setValue(workerData);
                        mDatabase.child("Organizations").child(companyId).child("workers").child(uid).setValue(true);
                        Toast.makeText(this, "Исполнитель добавлен", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }
}