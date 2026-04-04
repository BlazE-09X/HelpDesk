package firstapp.helpdesk.user;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import firstapp.helpdesk.R;

public class CreateSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_selection);

        CardView btnCreateNew = findViewById(R.id.card_create_new);
        CardView btnEditExisting = findViewById(R.id.card_edit_existing);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_user);

        // Переход к созданию новой
        btnCreateNew.setOnClickListener(v -> {
            startActivity(new Intent(this, NewRequestActivity.class));
        });

        // Переход к редактированию
        btnEditExisting.setOnClickListener(v -> {
            // Тут можно направить на тот же UserMain, но с режимом выбора
            // Или на отдельный экран
            startActivity(new Intent(this, EditListActivity.class));
        });

        // Настройка нижнего меню
        bottomNav.setSelectedItemId(R.id.nav_add);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, UserMain.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfile.class));
                finish();
                return true;
            }
            return id == R.id.nav_add;
        });
    }
}