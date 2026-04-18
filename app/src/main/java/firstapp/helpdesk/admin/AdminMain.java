package firstapp.helpdesk.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

public class AdminMain extends AppCompatActivity {

    private TextView statRejected, statInProgress, statDone, statNew;
    private DatabaseReference mDatabase;
    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, RequestViewHolder> adapter;

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
        setContentView(R.layout.admin_main);

        mDatabase = FirebaseDatabase.getInstance().getReference("Requests");

        statRejected = findViewById(R.id.stat_rejected);
        statInProgress = findViewById(R.id.stat_in_progress);
        statDone = findViewById(R.id.stat_done);
        statNew = findViewById(R.id.stat_new);
        
        Button btnReports = findViewById(R.id.btn_reports);
        btnReports.setOnClickListener(v -> startActivity(new Intent(AdminMain.this, AdminReportsActivity.class)));

        ImageView ivProfile = findViewById(R.id.iv_admin_profile);
        ivProfile.setOnClickListener(v -> startActivity(new Intent(AdminMain.this, AdminProfile.class)));

        recyclerView = findViewById(R.id.rv_recent_requests);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        Query query = mDatabase.limitToLast(10);
        FirebaseRecyclerOptions<RequestModel> options = new FirebaseRecyclerOptions.Builder<RequestModel>()
                .setQuery(query, RequestModel.class).build();

        adapter = new FirebaseRecyclerAdapter<RequestModel, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull RequestModel model) {
                holder.id.setText("#" + (model.getTicketNumber() != null ? model.getTicketNumber() : "---"));
                holder.title.setText("Тема: " + (model.getTopic() != null ? model.getTopic() : model.getTitle()));
                String status = (model.getStatus() != null) ? model.getStatus().toLowerCase() : "";
                if (status.contains("нов")) holder.statusView.setBackgroundColor(0xFF56CCF2);
                else if (status.contains("отказ")) holder.statusView.setBackgroundColor(0xFFB02A2A);
                else if (status.contains("выполн")) holder.statusView.setBackgroundColor(0xFF27AE60);
                else holder.statusView.setBackgroundColor(0xFFF2994A); 
            }
            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
                return new RequestViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_users) { startActivity(new Intent(this, AdminUsers.class)); return true; }
            if (id == R.id.nav_workers) { startActivity(new Intent(this, AdminWorkersActivity.class)); return true; }
            if (id == R.id.nav_tasks) { startActivity(new Intent(this, Companies.class)); return true; }
            if (id == R.id.nav_housing) { startActivity(new Intent(this, HousingComplexesActivity.class)); return true; }
            return false;
        });

        loadStatistics();
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }

    private void loadStatistics() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int rejected = 0, inWork = 0, done = 0, news = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    String status = data.child("status").getValue(String.class);
                    if (status == null) continue;
                    String s = status.toLowerCase();
                    if (s.contains("отказ")) rejected++;
                    else if (s.contains("работ")) inWork++;
                    else if (s.contains("выполн")) done++;
                    else if (s.contains("нов")) news++;
                }
                statRejected.setText("Отказано: " + rejected);
                statInProgress.setText("В работе: " + inWork);
                statDone.setText("Сделано: " + done);
                statNew.setText("Новые: " + news);
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView id, title;
        View statusView;
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.tv_request_number);
            title = itemView.findViewById(R.id.tv_request_title);
            statusView = itemView.findViewById(R.id.view_status_color);
        }
    }
}
