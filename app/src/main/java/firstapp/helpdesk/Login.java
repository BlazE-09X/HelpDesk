package firstapp.helpdesk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import firstapp.helpdesk.admin.AdminMain;
import firstapp.helpdesk.executor.ExecutorMain;
import firstapp.helpdesk.user.UserMain;
import firstapp.helpdesk.executor.WorkerMain;

public class Login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login_activity);

        etEmail = findViewById(R.id.login);
        etPassword = findViewById(R.id.password);
        btnLogin = findViewById(R.id.button);
        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            redirectByUserRole();
                        } else {
                            Toast.makeText(this, "Ошибка входа: проверьте почту и пароль", Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void redirectByUserRole() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        // ВАЖНО: Проверь в Firebase, что папка называется "users" (маленькими буквами)
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String role = task.getResult().child("role").getValue(String.class);

                if (role == null) {
                    Toast.makeText(this, "Роль не указана", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent;
                // Проверяем все возможные варианты написания роли
                if (role.equalsIgnoreCase("Администратор") || role.equalsIgnoreCase("admin")) {
                    intent = new Intent(this, AdminMain.class);
                } else if (role.equalsIgnoreCase("Исполнитель") || role.equalsIgnoreCase("worker") || role.equalsIgnoreCase("executor")) {
                    intent = new Intent(this, ExecutorMain.class);
                } else {
                    intent = new Intent(this, UserMain.class);
                }

                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Данные пользователя не найдены в БД", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
            }
        });
    }
}