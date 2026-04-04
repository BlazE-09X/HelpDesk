package firstapp.helpdesk.executor;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import firstapp.helpdesk.R;
import firstapp.helpdesk.user.RequestModel;
import firstapp.helpdesk.user.UserMain;

public class ExecutorMain extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, UserMain.RequestViewHolder> adapter;
    private String executorSpecialization = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.executor_main);

        recyclerView = findViewById(R.id.rv_executor_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        getExecutorDataAndLoadRequests();

        // Находим нижнее меню
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_executor);
// Устанавливаем иконку текущего экрана как выбранную
        bottomNavigationView.setSelectedItemId(R.id.nav_executor_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_executor_home) {
                // Мы уже здесь
                return true;
            } else if (id == R.id.nav_executor_profile) {
                // Переход в профиль исполнителя
                startActivity(new Intent(this, ExecutorProfile.class));
                return true;
            }
            return false;
        });
    }

    private void getExecutorDataAndLoadRequests() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // 1. Узнаем категорию исполнителя (например, "Лифт")
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    executorSpecialization = snapshot.child("specialization").getValue(String.class);
                    setupAdapter();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupAdapter() {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Requests");

        // 2. Фильтруем заявки: показывать только те, что относятся к специализации
        Query query = requestsRef.orderByChild("category").equalTo(executorSpecialization);

        FirebaseRecyclerOptions<RequestModel> options =
                new FirebaseRecyclerOptions.Builder<RequestModel>()
                        .setQuery(query, RequestModel.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<RequestModel, UserMain.RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserMain.RequestViewHolder holder, int position, @NonNull RequestModel model) {
                // Используем уже готовый метод из UserMain для нумерации
                holder.setRealNumber(position);
                holder.title.setText("Тема: " + model.getTitle());

                // Цвет полоски в зависимости от статуса
                int color;
                switch (model.getStatus()) {
                    case "Новая": color = 0xFF00BFFF; break;
                    case "В работе": color = 0xFFFFA500; break;
                    default: color = 0xFF32CD32; break;
                }
                holder.statusColor.setBackgroundColor(color);

                // Переход к деталям заявки для взятия в работу
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(ExecutorMain.this, ExecutorDetailActivity.class);
                    intent.putExtra("requestId", getRef(position).getKey());
                    startActivity(intent);
                });
            }

            @NonNull
            @Override
            public UserMain.RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
                return new UserMain.RequestViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
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
}