package firstapp.helpdesk.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import firstapp.helpdesk.R;

public class UserMain extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, RequestViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_main);

        recyclerView = findViewById(R.id.rv_user_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 1. Проверка авторизации
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 2. Запрос заявок текущего пользователя
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Requests");
        Query query = requestsRef.orderByChild("userId").equalTo(currentUid);

        FirebaseRecyclerOptions<RequestModel> options =
                new FirebaseRecyclerOptions.Builder<RequestModel>()
                        .setQuery(query, RequestModel.class)
                        .build();

        // 3. Адаптер
        adapter = new FirebaseRecyclerAdapter<RequestModel, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull RequestModel model) {
                holder.setRealNumber(position);
                holder.title.setText("Тема: " + model.getTitle());

                int color;
                String status = model.getStatus();
                if (status == null) status = "Новая";

                switch (status) {
                    case "Новая": color = 0xFF00BFFF; break;
                    case "В работе": color = 0xFFFFA500; break;
                    case "Отклонено": color = 0xFFFF0000; break;
                    case "Выполнено": color = 0xFF32CD32; break;
                    default: color = 0xFF808080; break;
                }
                holder.statusColor.setBackgroundColor(color);
            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
                return new RequestViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);

        // 4. Настройка нижнего меню
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_user);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(UserMain.this, UserProfile.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(UserMain.this, CreateSelectionActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_search) {
                findViewById(R.id.et_search_requests).requestFocus();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }


    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        public TextView number, title;
        public View statusColor;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.tv_request_number);
            title = itemView.findViewById(R.id.tv_request_title);
            statusColor = itemView.findViewById(R.id.view_status_color);
        }


        // Метод для установки номера заявки в формате #000
        public void setRealNumber(int position) {
            // позиция в списке (сверху вниз) + 1
            int realNum = position + 1;

            // Форматирование: добавляем ведущие нули, чтобы всегда было 3 цифры
            String formattedNum = String.format("#%03d", realNum);
            number.setText(formattedNum);
        }
    }
}