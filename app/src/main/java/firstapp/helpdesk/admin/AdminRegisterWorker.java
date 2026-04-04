package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import firstapp.helpdesk.R;

public class AdminRegisterWorker extends AppCompatActivity {

    private EditText etEmail, etPassword, etFullName;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_register_worker);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("users");


        etEmail = findViewById(R.id.et_worker_email);
        etPassword = findViewById(R.id.et_worker_password);
        etFullName = findViewById(R.id.et_worker_name);
        Button btnRegister = findViewById(R.id.btn_register_worker);

        btnRegister.setOnClickListener(v -> registerWorker());
    }

    private void registerWorker() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etFullName.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаем пользователя в Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Получаем UID только что созданного исполнителя
                        String workerUid = task.getResult().getUser().getUid();

                        // Создаем объект данных для базы
                        Map<String, Object> workerData = new HashMap<>();
                        workerData.put("email", email);
                        workerData.put("name", name);
                        workerData.put("role", "Исполнитель"); // Важно для твоего Login.java

                        // Сохраняем в Realtime Database
                        mDatabase.child(workerUid).setValue(workerData)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        Toast.makeText(this, "Исполнитель успешно добавлен", Toast.LENGTH_SHORT).show();
                                        // ВАЖНО: Firebase переключил Auth на нового юзера.
                                        // Для учебного проекта просто закрываем экран.
                                        finish();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}