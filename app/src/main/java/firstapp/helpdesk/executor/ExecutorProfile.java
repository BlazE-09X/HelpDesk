package firstapp.helpdesk.executor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import firstapp.helpdesk.user.RequestModel;

import java.util.Locale;

public class ExecutorProfile extends AppCompatActivity {

    private TextView tvName, tvLogin, tvPhone, tvEmail, tvRating;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.executor_profile);

        sharedPreferences = getSharedPreferences("HelpDeskPrefs", Context.MODE_PRIVATE);

        tvName = findViewById(R.id.tv_executor_name_header);
        tvLogin = findViewById(R.id.tv_executor_login);
        tvPhone = findViewById(R.id.tv_executor_phone);
        tvEmail = findViewById(R.id.tv_executor_email);
        tvRating = findViewById(R.id.tv_executor_rating);

        loadExecutorData();
        calculateAverageRating();

        findViewById(R.id.btn_executor_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sharedPreferences.edit().clear().apply();
            startActivity(new Intent(this, Login.class));
            finishAffinity();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_executor);
        bottomNav.setSelectedItemId(R.id.nav_executor_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_executor_home) {
                startActivity(new Intent(this, ExecutorMain.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return true;
        });
    }

    private void loadExecutorData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        
        // Сначала ищем в новой таблице Workers
        DatabaseReference workerRef = FirebaseDatabase.getInstance().getReference("Workers").child(uid);

        workerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    displayData(snapshot);
                } else {
                    // Если нет в Workers, ищем в общей таблице users
                    FirebaseDatabase.getInstance().getReference("users").child(uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snap) {
                                    if (snap.exists()) displayData(snap);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    
    private void displayData(DataSnapshot snapshot) {
        String name = snapshot.child("name").getValue(String.class);
        String surname = snapshot.child("surname").getValue(String.class);
        String phone = snapshot.child("phone").getValue(String.class);
        String email = snapshot.child("email").getValue(String.class);

        // Формируем полное имя: Фамилия + Имя
        String fullName = ((surname != null ? surname : "") + " " + (name != null ? name : "")).trim();
        
        if (fullName.isEmpty()) fullName = email; // Запасной вариант

        tvName.setText(fullName);
        tvLogin.setText(fullName);
        tvPhone.setText(phone != null ? phone : "Не указан");
        tvEmail.setText(email);
    }

    private void calculateAverageRating() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Requests");
        requestsRef.orderByChild("executorId").equalTo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float totalRating = 0;
                int count = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    RequestModel model = ds.getValue(RequestModel.class);
                    if (model != null && model.getRating() > 0) {
                        totalRating += model.getRating();
                        count++;
                    }
                }
                
                if (count > 0) {
                    float avg = totalRating / count;
                    tvRating.setText(String.format(Locale.getDefault(), "Рейтинг: %.1f ⭐", avg));
                } else {
                    tvRating.setText("Рейтинг: 0.0 ⭐");
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}