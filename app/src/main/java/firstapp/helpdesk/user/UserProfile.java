package firstapp.helpdesk.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import firstapp.helpdesk.Login;
import firstapp.helpdesk.R;
import firstapp.helpdesk.admin.UserModel;

public class UserProfile extends AppCompatActivity {

    private TextView tvName, tvPhone, tvEmail, tvLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(uid);

        tvName = findViewById(R.id.tv_profile_name);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvEmail = findViewById(R.id.tv_profile_email);
        tvLogin = findViewById(R.id.tv_profile_login);

        loadUserData();

        // Кнопка Мои заявки
        findViewById(R.id.btn_my_requests).setOnClickListener(v -> {
            startActivity(new Intent(this, UserMain.class));
            finish();
        });

        // Кнопка Выйти
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Переходы на новые экраны изменения данных
        findViewById(R.id.btn_change_email).setOnClickListener(v ->
                startActivity(new Intent(this, ChangeEmail.class)));

        findViewById(R.id.btn_change_password).setOnClickListener(v ->
                startActivity(new Intent(this, ChangePassword.class)));

        // Настройка нижнего меню
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_user);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile); // Устанавливаем профиль активным

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, UserMain.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CreateSelectionActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true; // Уже в профиле
            } else if (id == R.id.nav_search) {
                // Возвращаемся на главную к поиску
                startActivity(new Intent(this, UserMain.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void loadUserData() {
        mDatabase.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user != null) {
                    // Используем геттеры из твоей модели
                    tvName.setText(user.getFullname());
                    tvEmail.setText(user.getEmail());
                    tvPhone.setText(snapshot.child("phone").getValue(String.class));
                    tvLogin.setText(snapshot.child("login").getValue(String.class));
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
    }
}