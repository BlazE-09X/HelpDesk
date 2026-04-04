package firstapp.helpdesk.user;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import firstapp.helpdesk.R;

public class ChangeEmail extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_change_email);

        EditText etEmail = findViewById(R.id.et_new_email);
        Button btnSave = findViewById(R.id.btn_save_email);

        btnSave.setOnClickListener(v -> {
            String newEmail = etEmail.getText().toString().trim();
            if (newEmail.isEmpty()) return;

            FirebaseAuth.getInstance().getCurrentUser().updateEmail(newEmail).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    FirebaseDatabase.getInstance().getReference("users").child(uid).child("email").setValue(newEmail);
                    Toast.makeText(this, "Email успешно изменен", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Ошибка: требуется перезайти в аккаунт", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}