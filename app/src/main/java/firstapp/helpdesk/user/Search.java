package firstapp.helpdesk.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class Search extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, UserMain.RequestViewHolder> adapter;
    private DatabaseReference requestsRef;
    private String currentUid;

    // Кастомный менеджер для предотвращения краша "Inconsistency detected"
    private static class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) {
            super(context);
        }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                Log.e("RecyclerView", "Inconsistency detected");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_search);

        requestsRef = FirebaseDatabase.getInstance().getReference("Requests");
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.rv_search_results);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        android.widget.EditText etSearch = findViewById(R.id.et_search_input);

        // Слушатель изменения текста для мгновенного поиска
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                startSearch(s.toString());
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupBottomNavigation();
        startSearch(""); // По умолчанию показываем все
    }

    private void startSearch(String searchText) {
        // Запрос: ищем по заголовку (title) среди заявок пользователя
        Query query = requestsRef.orderByChild("title")
                .startAt(searchText)
                .endAt(searchText + "\uf8ff");

        FirebaseRecyclerOptions<RequestModel> options =
                new FirebaseRecyclerOptions.Builder<RequestModel>()
                        .setQuery(query, RequestModel.class)
                        .build();

        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new FirebaseRecyclerAdapter<RequestModel, UserMain.RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserMain.RequestViewHolder holder, int position, @NonNull RequestModel model) {
                // Фильтруем, чтобы видеть только свои заявки (если Firebase не сделал этого)
                if (model.getUserId() == null || !model.getUserId().equals(currentUid)) {
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                }

                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                holder.number.setText("#" + String.format("%03d", position + 1));
                holder.title.setText(model.getTitle());

                // Установка цвета в зависимости от статуса
                int color = getStatusColor(model.getStatus());
                holder.statusColor.setBackgroundColor(color);
            }

            @NonNull
            @Override
            public UserMain.RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
                return new UserMain.RequestViewHolder(view);
            }
        };

        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }

    private int getStatusColor(String status) {
        if (status == null) return 0xFF00BFFF;
        switch (status) {
            case "Выполнено": return 0xFF32CD32; // Зеленый
            case "Отклонено": return 0xFFFF0000; // Красный
            case "В работе": return 0xFFFFA500;  // Оранжевый
            default: return 0xFF00BFFF;          // Голубой
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_user);
        bottomNav.setSelectedItemId(R.id.nav_search);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, UserMain.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, CreateSelectionActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfile.class));
                finish();
                return true;
            }
            return id == R.id.nav_search;
        });
    }
}
