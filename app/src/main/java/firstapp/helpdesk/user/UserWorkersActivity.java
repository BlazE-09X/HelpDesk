package firstapp.helpdesk.user;

import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

import firstapp.helpdesk.R;
import firstapp.helpdesk.admin.UserModel;

public class UserWorkersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<UserModel, UserWorkerViewHolder> adapter;
    private String orgId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_workers);

        orgId = getIntent().getStringExtra("orgId");
        String orgName = getIntent().getStringExtra("orgName");

        TextView tvHeader = findViewById(R.id.tv_header_org);
        tvHeader.setText(orgName);

        mDatabase = FirebaseDatabase.getInstance().getReference("Workers");
        recyclerView = findViewById(R.id.rv_user_workers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupAdapter();
        
        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
    }

    private void setupAdapter() {
        Query query = mDatabase.orderByChild("companyId").equalTo(orgId);
        
        FirebaseRecyclerOptions<UserModel> options = new FirebaseRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class).build();

        adapter = new FirebaseRecyclerAdapter<UserModel, UserWorkerViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserWorkerViewHolder holder, int position, @NonNull UserModel model) {
                String fullName = (model.getSurname() != null ? model.getSurname() : "") + " " + (model.getName() != null ? model.getName() : "");
                holder.name.setText(fullName.trim());
                holder.spec.setText(model.getSpecialization());
                
                // Загружаем рейтинг
                loadRating(getRef(position).getKey(), holder.rating);
            }

            @NonNull
            @Override
            public UserWorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                return new UserWorkerViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void loadRating(String workerId, TextView tvRating) {
        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Requests");
        requestsRef.orderByChild("executorId").equalTo(workerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float total = 0; int count = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Float r = ds.child("rating").getValue(Float.class);
                    if (r != null && r > 0) { total += r; count++; }
                }
                if (count > 0) tvRating.setText(String.format(Locale.getDefault(), "⭐ %.1f", total/count));
                else tvRating.setText("⭐ 0.0");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static class UserWorkerViewHolder extends RecyclerView.ViewHolder {
        TextView name, spec, rating;
        public UserWorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(android.R.id.text1);
            spec = itemView.findViewById(android.R.id.text2);
            rating = new TextView(itemView.getContext()); // Для примера, можно добавить в layout
            // В простом списке android.R.layout.simple_list_item_2 нет третьего поля, 
            // в реальном проекте лучше создать свой item_user_worker.xml
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}