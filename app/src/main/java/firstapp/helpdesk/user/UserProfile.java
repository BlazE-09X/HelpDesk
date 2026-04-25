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

        findViewById(R.id.btn_my_requests).setOnClickListener(v -> {
            startActivity(new Intent(this, UserMain.class));
            finish();
        });

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        findViewById(R.id.btn_change_email).setOnClickListener(v ->
                startActivity(new Intent(this, ChangeEmail.class)));

        findViewById(R.id.btn_change_password).setOnClickListener(v ->
                startActivity(new Intent(this, ChangePassword.class)));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_user);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, UserMain.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CreateSelectionActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(this, AllWorkersActivity.class));
                finish();
                return true;
            }
            return id == R.id.nav_profile;
        });
    }

    private void loadUserData() {
        mDatabase.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user != null) {
                    // Собираем ФИО (Фамилия + Имя + Отчество)
                    String fullName = (user.getSurname() != null ? user.getSurname() : "") + " " +
                                      (user.getName() != null ? user.getName() : "") + " " +
                                      (user.getPatronymic() != null ? user.getPatronymic() : "");
                    
                    if (fullName.trim().isEmpty()) fullName = user.getFullname();
                    if (fullName == null || fullName.trim().isEmpty()) fullName = user.getEmail();
                    
                    tvName.setText(fullName.trim());
                    tvEmail.setText(user.getEmail());

                    // Используем String.valueOf для защиты от Long-to-String ошибки
                    Object phoneObj = snapshot.child("phone").getValue();
                    tvPhone.setText(phoneObj != null ? String.valueOf(phoneObj) : "Не указан");

                    Object loginObj = snapshot.child("login").getValue();
                    tvLogin.setText(loginObj != null ? String.valueOf(loginObj) : "Не указан");
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
    }
}
