package firstapp.helpdesk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import firstapp.helpdesk.admin.AdminMain;
import firstapp.helpdesk.user.UserMain;
import firstapp.helpdesk.worker.WorkerMain;

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

            // Пытаемся войти
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Вход успешен
                            redirectByUserRole(email);
                        } else {
                            // Если вход не удался — просто сообщаем об ошибке.
                            Toast.makeText(this, "Ошибка доступа: проверьте данные или обратитесь к администратору", Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void redirectByUserRole(String email) {
        Intent intent;
        if (email.contains("admin")) {
            intent = new Intent(this, AdminMain.class);
        } else if (email.contains("worker")) {
            intent = new Intent(this, WorkerMain.class);
        } else {
            intent = new Intent(this, UserMain.class);
        }
        startActivity(intent);
        finish(); // Закрываем экран логина, чтобы нельзя было вернуться назад кнопкой
    }
}