package firstapp.helpdesk.user;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import firstapp.helpdesk.R;

public class ChangeEmail extends AppCompatActivity {
    private EditText etNewEmail, etConfirmEmail;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_email_activity);

        etNewEmail = findViewById(R.id.etSubject2);
        etConfirmEmail = findViewById(R.id.etSubject3);
        btnSave = findViewById(R.id.btnCreate2);

        btnSave.setOnClickListener(v -> {
            String email = etNewEmail.getText().toString().trim();
            String confirmEmail = etConfirmEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email) || !email.contains("@") || !email.equals(confirmEmail)) {
                Toast.makeText(this, "Проверьте корректность Email", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String uid = user.getUid();
                user.updateEmail(email).addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Синхронизация с базой данных
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid);
                        ref.child("email").setValue(email).addOnCompleteListener(this, dbTask -> {
                            Toast.makeText(this, "Email успешно обновлен", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    } else {
                        Toast.makeText(this, "Ошибка безопасности. Перезайдите в систему.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
