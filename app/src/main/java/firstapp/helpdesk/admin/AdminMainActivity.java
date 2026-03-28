package firstapp.helpdesk.admin;

import android.content.Intent;
import android.graphics.Color;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import firstapp.helpdesk.R;

public class AdminMainActivity extends AppCompatActivity {

    private TextView statRejected, statInProgress, statDone, statNew;
    private DatabaseReference mDatabase;
    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, RequestViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // 1. Инициализация базы (сначала базу, потом всё остальное)
        mDatabase = FirebaseDatabase.getInstance().getReference("requests");

        // 2. Инициализация View
        statRejected = findViewById(R.id.stat_rejected);
        statInProgress = findViewById(R.id.stat_in_progress);
        statDone = findViewById(R.id.stat_done);
        statNew = findViewById(R.id.stat_new);
        recyclerView = findViewById(R.id.rv_recent_requests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3. Настройка списка (Query)
        Query query = mDatabase.limitToLast(5);

        FirebaseRecyclerOptions<RequestModel> options =
                new FirebaseRecyclerOptions.Builder<RequestModel>()
                        .setQuery(query, RequestModel.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<RequestModel, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull RequestModel model) {
                holder.id.setText("#" + model.getTicketNumber());
                holder.title.setText("Тема: " + model.getTopic());

                if ("new".equals(model.getStatus())) holder.statusView.setBackgroundColor(Color.BLUE);
                else if ("rejected".equals(model.getStatus())) holder.statusView.setBackgroundColor(Color.RED);
                else holder.statusView.setBackgroundColor(Color.GREEN);
            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
                return new RequestViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);

        // 4. Навигация
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_users) {
                startActivity(new Intent(AdminMainActivity.this, AdminRegisterUserActivity.class));
                return true;
            }
            return id == R.id.nav_home;
        });

        loadStatistics();
    }

    // Эти методы ДОЛЖНЫ быть вне onCreate
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

    private void loadStatistics() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int rejected = 0, inWork = 0, done = 0, news = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    String status = data.child("status").getValue(String.class);
                    if (status == null) continue;
                    switch (status) {
                        case "rejected": rejected++; break;
                        case "working": inWork++; break;
                        case "done": done++; break;
                        case "new": news++; break;
                    }
                }
                statRejected.setText("Отказано: " + rejected);
                statInProgress.setText("В работе: " + inWork);
                statDone.setText("Сделано: " + done);
                statNew.setText("Новые: " + news);
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    // Вспомогательный класс тоже вне onCreate
    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView id, title;
        View statusView;
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.tv_request_id);
            title = itemView.findViewById(R.id.tv_request_title);
            statusView = itemView.findViewById(R.id.view_status_color);
        }
    }
}