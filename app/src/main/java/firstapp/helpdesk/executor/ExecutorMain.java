package firstapp.helpdesk.executor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import firstapp.helpdesk.user.RequestModel;
import firstapp.helpdesk.user.UserMain;

public class ExecutorMain extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, UserMain.RequestViewHolder> adapter;
    private String executorSpecialization = "";
    private SharedPreferences sharedPreferences;

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
        setContentView(R.layout.executor_main);

        sharedPreferences = getSharedPreferences("HelpDeskPrefs", Context.MODE_PRIVATE);
        executorSpecialization = sharedPreferences.getString("specialization", "");

        recyclerView = findViewById(R.id.rv_executor_requests);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        if (executorSpecialization.isEmpty()) {
            fetchExecutorDataAndLoad();
        } else {
            setupAdapter();
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_executor);
        bottomNavigationView.setSelectedItemId(R.id.nav_executor_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_executor_home) {
                return true;
            } else if (id == R.id.nav_executor_profile) {
                startActivity(new Intent(this, ExecutorProfile.class));
                return true;
            }
            return false;
        });
    }

    private void fetchExecutorDataAndLoad() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.child("specialization").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                executorSpecialization = snapshot.getValue(String.class);
                sharedPreferences.edit().putString("specialization", executorSpecialization).apply();
                setupAdapter();
            }
        });
    }

    private void setupAdapter() {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Requests");

        // Фильтрация по специализации: исполнитель видит только свои категории
        Query query = requestsRef.orderByChild("category").equalTo(executorSpecialization);

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
                holder.number.setText(String.format("#%03d", position + 1));
                holder.title.setText("Тема: " + model.getTitle());

                int color;
                String status = model.getStatus() != null ? model.getStatus() : "Новая";
                switch (status) {
                    case "Новая": color = 0xFF00BFFF; break;
                    case "В работе": color = 0xFFFFA500; break;
                    default: color = 0xFF32CD32; break;
                }
                holder.statusColor.setBackgroundColor(color);

                holder.itemView.setOnClickListener(v -> {
                    int currentPos = holder.getBindingAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        Intent intent = new Intent(ExecutorMain.this, ExecutorDetailActivity.class);
                        intent.putExtra("requestId", getRef(currentPos).getKey());
                        startActivity(intent);
                    }
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
