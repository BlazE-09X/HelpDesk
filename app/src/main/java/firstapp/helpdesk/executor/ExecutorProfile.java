package firstapp.helpdesk.executor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import firstapp.helpdesk.Login;
import firstapp.helpdesk.R;

public class ExecutorProfile extends AppCompatActivity {

    private TextView tvName, tvLogin, tvPhone, tvEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.executor_profile);

        tvName = findViewById(R.id.tv_executor_name_header);
        tvLogin = findViewById(R.id.tv_executor_login);
        tvPhone = findViewById(R.id.tv_executor_phone);
        tvEmail = findViewById(R.id.tv_executor_email);

        loadExecutorData();

        // Кнопка выхода
        findViewById(R.id.btn_executor_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, Login.class));
            finishAffinity(); // Закрыть все окна
        });

        // Навигация
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_executor);
        bottomNav.setSelectedItemId(R.id.nav_executor_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_executor_home) {
                startActivity(new Intent(this, ExecutorMain.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return true;
        });
    }

    private void loadExecutorData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    tvName.setText(name);
                    tvLogin.setText(name); // Или поле login, если оно есть
                    tvPhone.setText(phone);
                    tvEmail.setText(email);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}