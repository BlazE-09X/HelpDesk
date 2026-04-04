package firstapp.helpdesk.user;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import firstapp.helpdesk.R;

public class ChangePassword extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_change_password);

        EditText etPass = findViewById(R.id.et_new_password);
        EditText etConfirm = findViewById(R.id.et_confirm_password);
        Button btnSave = findViewById(R.id.btn_save_password);

        btnSave.setOnClickListener(v -> {
            String p1 = etPass.getText().toString().trim();
            String p2 = etConfirm.getText().toString().trim();

            if (p1.length() < 6 || !p1.equals(p2)) {
                Toast.makeText(this, "Пароли не совпадают или слишком короткие", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance().getCurrentUser().updatePassword(p1).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Пароль обновлен", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Ошибка безопасности. Перезайдите в систему.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}