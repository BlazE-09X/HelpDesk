package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import firstapp.helpdesk.R;
import java.util.HashMap;

public class CreateCompany extends AppCompatActivity {

    private EditText etName, etDomain;
    private Button btnCreate;
    private TextView tvBack;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_create_company);

        // Используем путь "Organizations", чтобы Companies.java мог их найти
        mDatabase = FirebaseDatabase.getInstance().getReference("Organizations");

        etName = findViewById(R.id.et_company_name);
        etDomain = findViewById(R.id.et_company_domain);
        btnCreate = findViewById(R.id.btn_create_company);
        tvBack = findViewById(R.id.tv_back);

        if (etDomain != null) {
            etDomain.setHint("Специализация или описание");
        }

        tvBack.setOnClickListener(v -> finish());

        btnCreate.setOnClickListener(v -> createCompany());
        
        View bottomNav = findViewById(R.id.bottom_navigation_create);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }
    }

    private void createCompany() {
        String name = etName.getText().toString().trim();
        String domain = etDomain != null ? etDomain.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Введите название организации", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreate.setEnabled(false);
        String orgId = mDatabase.push().getKey();

        HashMap<String, Object> orgData = new HashMap<>();
        orgData.put("id", orgId);
        orgData.put("name", name);
        orgData.put("domain", domain); // Теперь сохраняем описание/специализацию
        orgData.put("type", "organization");

        if (orgId != null) {
            mDatabase.child(orgId).setValue(orgData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(CreateCompany.this, "Организация добавлена", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreate.setEnabled(true);
                    Toast.makeText(CreateCompany.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }
}
