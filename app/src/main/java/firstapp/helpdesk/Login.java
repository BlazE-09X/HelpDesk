package firstapp.helpdesk;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import firstapp.helpdesk.admin.AdminMain;
import firstapp.helpdesk.executor.ExecutorMain;
import firstapp.helpdesk.user.UserMain;

public class Login extends AppCompatActivity {

    private static final String TAG = "AUTH";
    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login_activity);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("HelpDeskPrefs", Context.MODE_PRIVATE);

        // Проверка существующей сессии
        if (mAuth.getCurrentUser() != null && sharedPreferences.contains("userRole")) {
            String role = sharedPreferences.getString("userRole", "");
            Log.d(TAG, "session_restore: userId=" + mAuth.getCurrentUser().getUid() + ", role=" + role);
            navigateToMain(role);
            return;
        }

        etEmail = findViewById(R.id.login);
        etPassword = findViewById(R.id.password);
        btnLogin = findViewById(R.id.button);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.textView3);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            signIn(email, password);
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Введите ваш email в поле выше, чтобы сбросить пароль", Toast.LENGTH_LONG).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Письмо для сброса пароля отправлено на " + email, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "login_attempt: email=" + email);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "login_success: userId=" + mAuth.getCurrentUser().getUid());
                        fetchUserRoleAndRedirect();
                    } else {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Log.e(TAG, "login_error: " + (task.getException() != null ? task.getException().getMessage() : "unknown"));
                        Toast.makeText(this, "Ошибка входа: проверьте данные или сбросьте пароль", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void fetchUserRoleAndRedirect() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        Log.d(TAG, "fetch_role: userId=" + uid);
        userRef.get().addOnCompleteListener(task -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);

            if (task.isSuccessful() && task.getResult().exists()) {
                String role = task.getResult().child("role").getValue(String.class);
                if (role == null) role = "User";

                Log.d(TAG, "role_found: userId=" + uid + ", role=" + role);
                // Сохранение сессии
                sharedPreferences.edit()
                        .putString("userId", uid)
                        .putString("userRole", role)
                        .apply();

                navigateToMain(role);
            } else {
                Log.e(TAG, "user_data_missing: userId=" + uid);
                Toast.makeText(this, "Данные пользователя не найдены", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
            }
        });
    }

    private void navigateToMain(String role) {
        Log.d("FLOW", "navigate_to_main: role=" + role);
        Intent intent;
        if (role.equalsIgnoreCase("Администратор") || role.equalsIgnoreCase("admin")) {
            intent = new Intent(this, AdminMain.class);
        } else if (role.equalsIgnoreCase("Исполнитель") || role.equalsIgnoreCase("worker") || role.equalsIgnoreCase("executor") || role.equalsIgnoreCase("Worker")) {
            intent = new Intent(this, ExecutorMain.class);
        } else {
            intent = new Intent(this, UserMain.class);
        }
        startActivity(intent);
        finish();
    }
}
