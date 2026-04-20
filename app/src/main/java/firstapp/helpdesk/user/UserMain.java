package firstapp.helpdesk.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RatingBar;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import firstapp.helpdesk.R;

public class UserMain extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, RequestViewHolder> adapter;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    private static class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) { super(context); }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try { super.onLayoutChildren(recycler, state); } catch (IndexOutOfBoundsException e) { Log.e("RecyclerView", "Inconsistency detected"); }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_main);

        recyclerView = findViewById(R.id.rv_user_requests);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        EditText etSearch = findViewById(R.id.et_search_requests);
        if (etSearch != null) {
            etSearch.setFocusable(false);
            etSearch.setOnClickListener(v -> startActivity(new Intent(UserMain.this, Search.class)));
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Requests");
        // Запрос всех заявок пользователя. Сортировка по статусу на уровне БД сложна для FirebaseUI, 
        // поэтому оставим базовый запрос, а визуально будем выделять.
        Query query = requestsRef.orderByChild("userId").equalTo(currentUid);

        FirebaseRecyclerOptions<RequestModel> options = new FirebaseRecyclerOptions.Builder<RequestModel>()
                .setQuery(query, RequestModel.class).build();

        adapter = new FirebaseRecyclerAdapter<RequestModel, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull RequestModel model) {
                holder.number.setText(String.format("#%03d", position + 1));
                holder.title.setText("Тема: " + model.getTitle());
                holder.date.setText("Создано: " + dateFormat.format(new Date(model.getTimestamp())));

                String status = model.getStatus() != null ? model.getStatus() : "Новая";
                holder.statusText.setText(status);

                // Настройка дедлайна
                String deadlineStr = "Немедленно";
                if ("planned".equals(model.getExecutionType()) || "deadline".equals(model.getExecutionType())) {
                    deadlineStr = "Дедлайн: " + dateFormat.format(new Date(model.getDeadlineDate()));
                }
                holder.deadline.setText(deadlineStr);

                // Цвета и состояния
                int color;
                boolean isCompleted = "Выполнено".equalsIgnoreCase(status);
                
                if (isCompleted) {
                    color = 0xFF32CD32; // Зеленый
                    holder.deadline.setTextColor(0xFF888888); // Серый для выполненных
                    holder.ratingBar.setVisibility(View.VISIBLE);
                    holder.ratingBar.setRating(model.getRating());
                } else {
                    holder.ratingBar.setVisibility(View.GONE);
                    switch (status) {
                        case "В работе": 
                            color = 0xFFFFA500; // Оранжевый
                            holder.deadline.setTextColor(0xFFFFA500);
                            break;
                        case "Отклонено": 
                            color = 0xFFFF0000; // Красный
                            holder.deadline.setTextColor(0xFFFF0000);
                            break;
                        default: 
                            color = 0xFF00BFFF; // Голубой
                            holder.deadline.setTextColor(0xFFFF0000); // Красный для "Немедленно"
                            break;
                    }
                }
                
                holder.statusColor.setBackgroundColor(color);
                holder.statusText.setTextColor(color);

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(UserMain.this, EditRequestActivity.class);
                    intent.putExtra("requestId", getRef(holder.getBindingAdapterPosition()).getKey());
                    intent.putExtra("requestNumber", holder.number.getText().toString());
                    startActivity(intent);
                });
            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
                return new RequestViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_user);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_search) { startActivity(new Intent(this, Search.class)); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, UserProfile.class)); return true; }
            if (id == R.id.nav_add) { startActivity(new Intent(this, CreateSelectionActivity.class)); return true; }
            return false;
        });
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        public TextView number, title, date, deadline, statusText;
        public View statusColor;
        public RatingBar ratingBar;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.tv_request_number);
            title = itemView.findViewById(R.id.tv_request_title);
            date = itemView.findViewById(R.id.tv_request_date);
            deadline = itemView.findViewById(R.id.tv_request_deadline);
            statusText = itemView.findViewById(R.id.tv_request_status_text);
            statusColor = itemView.findViewById(R.id.view_status_color);
            ratingBar = itemView.findViewById(R.id.rb_item_rating);
        }
    }
}
