package firstapp.helpdesk.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import firstapp.helpdesk.R;
import firstapp.helpdesk.tickets.CreateTicketActivity;

public class UserMain extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.user_main);

        // Находим нашу кнопку с плюсом
        View btnAdd = findViewById(R.id.btnAddTicket);

        btnAdd.setOnClickListener(v -> {
            // Создаем намерение (Intent) перейти с этого экрана на экран создания заявки
            Intent intent = new Intent(UserMain.this, CreateTicketActivity.class);
            startActivity(intent);
        });
    }
}
