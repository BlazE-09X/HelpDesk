package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.text.TextUtils;
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

        mDatabase = FirebaseDatabase.getInstance().getReference("companies");

        etName = findViewById(R.id.et_company_name);
        etDomain = findViewById(R.id.et_company_domain);
        btnCreate = findViewById(R.id.btn_create_company);
        tvBack = findViewById(R.id.tv_back);

        tvBack.setOnClickListener(v -> finish());

        btnCreate.setOnClickListener(v -> createCompany());
    }

    private void createCompany() {
        String name = etName.getText().toString().trim();
        String domain = etDomain.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(domain)) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        // Генерируем уникальный ID для компании
        String companyId = mDatabase.push().getKey();

        HashMap<String, Object> companyData = new HashMap<>();
        companyData.put("name", name);
        companyData.put("domain", domain);
        companyData.put("id", companyId);

        if (companyId != null) {
            mDatabase.child(companyId).setValue(companyData)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Компания добавлена", Toast.LENGTH_SHORT).show();
                        finish(); // Возвращаемся к списку
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}