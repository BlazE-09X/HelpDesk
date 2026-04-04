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
import firstapp.helpdesk.R;

public class AdminChangePassword extends AppCompatActivity {

    private EditText etNewPass, etConfirmPass;
    private Button btnSave;
    private TextView tvBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password_activity);

        etNewPass = findViewById(R.id.etSubject2);
        etConfirmPass = findViewById(R.id.etSubject3);
        btnSave = findViewById(R.id.btnCreate2);
        tvBack = findViewById(R.id.textView8);

        tvBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String pass = etNewPass.getText().toString().trim();
            String confirmPass = etConfirmPass.getText().toString().trim();

            if (TextUtils.isEmpty(pass) || pass.length() < 6) {
                Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                user.updatePassword(pass).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Ошибка безопасности. Перезайдите в аккаунт", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}