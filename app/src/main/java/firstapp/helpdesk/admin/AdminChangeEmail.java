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
                user.updateEmail(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email успешно изменен", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Ошибка: требуется недавний вход в систему", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}