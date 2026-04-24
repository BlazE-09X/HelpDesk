package firstapp.helpdesk.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import firstapp.helpdesk.Login;
import firstapp.helpdesk.R;

public class AdminProfile extends AppCompatActivity {

    private TextView tvFullName, tvLogin, tvPhone, tvEmail;
    private Button btnChangeEmail, btnChangePassword, btnLogout;
    private BottomNavigationView bottomNavigationView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }

        tvFullName = findViewById(R.id.tv_admin_full_name);
        tvLogin = findViewById(R.id.tv_profile_login);
        tvPhone = findViewById(R.id.tv_profile_phone);
        tvEmail = findViewById(R.id.tv_profile_email);

        btnChangeEmail = findViewById(R.id.btn_change_email);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);
        bottomNavigationView = findViewById(R.id.bottom_navigation_profile);

        loadAdminData();

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(AdminProfile.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        btnChangeEmail.setOnClickListener(v -> startActivity(new Intent(this, AdminChangeEmail.class)));
        btnChangePassword.setOnClickListener(v -> startActivity(new Intent(this, AdminChangePassword.class)));

        setupNavigation();
    }

    private void loadAdminData() {
        if (mDatabase == null) return;

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Используем String.valueOf() для безопасного получения данных, если они в БД сохранены как Long
                    
                    Object roleObj = snapshot.child("role").getValue();
                    tvFullName.setText(roleObj != null ? String.valueOf(roleObj) : "Администратор");

                    Object loginObj = snapshot.child("login").getValue();
                    tvLogin.setText(loginObj != null ? String.valueOf(loginObj) : "Не указан");

                    Object emailObj = snapshot.child("email").getValue();
                    tvEmail.setText(emailObj != null ? String.valueOf(emailObj) : "Не указана");
                    
                    Object phoneObj = snapshot.child("phone").getValue();
                    tvPhone.setText(phoneObj != null ? String.valueOf(phoneObj) : "Не указан");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminMain.class));
                finish();
                return true;
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(this, AdminUsers.class));
                finish();
                return true;
            } else if (id == R.id.nav_tasks) {
                startActivity(new Intent(this, Companies.class));
                finish();
                return true;
            }
            return id == R.id.nav_profile;
        });
    }
}
