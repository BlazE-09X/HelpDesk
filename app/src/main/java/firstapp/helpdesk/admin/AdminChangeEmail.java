package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import firstapp.helpdesk.R;

public class AdminChangeEmail extends AppCompatActivity {

    private EditText etNewEmail, etConfirmEmail;
    private Button btnSave;
    private TextView tvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_email_activity);

        etNewEmail = findViewById(R.id.etSubject2);
        etConfirmEmail = findViewById(R.id.etSubject3);
        btnSave = findViewById(R.id.btnCreate2);
        tvBack = findViewById(R.id.textView8);

        tvBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String email = etNewEmail.getText().toString().trim();
            String confirmEmail = etConfirmEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email) || !email.contains("@")) {
                Toast.makeText(this, "Введите корректный Email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!email.equals(confirmEmail)) {
                Toast.makeText(this, "Email не совпадают", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Проверка: не совпадает ли новая почта с текущей
                if (email.equalsIgnoreCase(user.getEmail())) {
                    Toast.makeText(this, "Этот Email уже используется в вашем профиле", Toast.LENGTH_SHORT).show();
                    return;
                }

                String uid = user.getUid();
                
                // Используем более современный метод или обычный с детальным логом
                user.updateEmail(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Обновляем в БД
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
                        userRef.child("email").setValue(email).addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                Toast.makeText(this, "Email успешно изменен везде", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "В Auth изменено, но в БД — нет!", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        // ПОКАЗЫВАЕМ РЕАЛЬНУЮ ПРИЧИНУ ОШИБКИ
                        String error = task.getException() != null ? task.getException().getMessage() : "Неизвестная ошибка";
                        if (error.contains("recent login")) {
                            Toast.makeText(this, "Ошибка безопасности! Пожалуйста, выйдите из приложения и войдите СНОВА, прежде чем менять почту.", Toast.LENGTH_LONG).show();
                        } else if (error.contains("already in use")) {
                            Toast.makeText(this, "Этот Email уже занят другим аккаунтом", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Ошибка: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }
}
