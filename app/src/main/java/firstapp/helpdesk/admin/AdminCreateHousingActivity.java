package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import firstapp.helpdesk.R;

public class AdminCreateHousingActivity extends AppCompatActivity {

    private EditText etName, etAddress;
    private Button btnSave;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_create_housing);

        mDatabase = FirebaseDatabase.getInstance().getReference("HousingComplexes");

        etName = findViewById(R.id.et_housing_name);
        etAddress = findViewById(R.id.et_housing_address);
        btnSave = findViewById(R.id.btn_save_housing);

        btnSave.setOnClickListener(v -> saveHousing());
    }

    private void saveHousing() {
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = mDatabase.push().getKey();
        if (id != null) {
            HousingComplexModel housing = new HousingComplexModel(name, address);
            housing.setId(id);
            mDatabase.child(id).setValue(housing).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "ЖК добавлен", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}