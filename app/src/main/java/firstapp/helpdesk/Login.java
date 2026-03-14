package firstapp.helpdesk;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Login extends AppCompatActivity {

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

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();

            //кто именно зашел в приложение
            if (email.contains("admin")){
                startActivity(new Intent(this, AdminMain.class));
            }
            else if (email.contains("worker")){
                startActivity(new Intent(this, WorkerMain.class));
            }
            else {
                startActivity(new Intent(this, UserMain.class));
            }
        });

    }
}