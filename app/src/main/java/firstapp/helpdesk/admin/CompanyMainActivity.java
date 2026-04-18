package firstapp.helpdesk.admin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

import firstapp.helpdesk.Login;
import firstapp.helpdesk.R;
import firstapp.helpdesk.user.RequestModel;

public class CompanyMainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<UserModel, WorkerViewHolder> adapter;
    private String companyId;
    private SharedPreferences sharedPreferences;
    private DatabaseReference mDatabase;
    private TextView tvWorkersCount;

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
        setContentView(R.layout.activity_company_main);

        sharedPreferences = getSharedPreferences("HelpDeskPrefs", Context.MODE_PRIVATE);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        companyId = sharedPreferences.getString("companyId", "");

        tvWorkersCount = findViewById(R.id.tv_workers_count);
        recyclerView = findViewById(R.id.rv_company_workers);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        findViewById(R.id.btn_company_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sharedPreferences.edit().clear().apply();
            startActivity(new Intent(this, Login.class));
            finishAffinity();
        });

        findViewById(R.id.btn_add_worker).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminRegisterWorker.class);
            intent.putExtra("fixedCompanyId", companyId);
            startActivity(intent);
        });

        if (companyId.isEmpty()) {
            fetchCompanyId();
        } else {
            setupAdapter();
            updateWorkersCount();
        }
    }

    private void fetchCompanyId() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        mDatabase.child("users").child(uid).child("companyId").get().addOnSuccessListener(snapshot -> {
            companyId = snapshot.getValue(String.class);
            sharedPreferences.edit().putString("companyId", companyId).apply();
            setupAdapter();
            updateWorkersCount();
        });
    }

    private void setupAdapter() {
        Query query = mDatabase.child("users").orderByChild("companyId").equalTo(companyId);

        FirebaseRecyclerOptions<UserModel> options = new FirebaseRecyclerOptions.Builder<UserModel>()
                .setQuery(query, snapshot -> {
                    UserModel user = snapshot.getValue(UserModel.class);
                    if (user != null && "Исполнитель".equalsIgnoreCase(user.getRole())) {
                        user.setUid(snapshot.getKey());
                        return user;
                    }
                    return null; 
                })
                .build();

        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new FirebaseRecyclerAdapter<UserModel, WorkerViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull WorkerViewHolder holder, int position, @NonNull UserModel model) {
                if (model.getUid() == null) {
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                }
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                
                holder.name.setText(model.getName());
                holder.spec.setText(model.getSpecialization());
                
                loadWorkerStats(model.getUid(), holder.rating, holder.doneCount);

                holder.btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(CompanyMainActivity.this)
                            .setTitle("Удаление")
                            .setMessage("Удалить исполнителя " + model.getName() + "?")
                            .setPositiveButton("Да", (dialog, which) -> deleteWorker(model.getUid()))
                            .setNegativeButton("Нет", null)
                            .show();
                });
            }

            @NonNull
            @Override
            public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worker_company, parent, false);
                return new WorkerViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void loadWorkerStats(String workerId, TextView tvRating, TextView tvDone) {
        mDatabase.child("Requests").orderByChild("executorId").equalTo(workerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                float totalRating = 0;
                int ratingCount = 0;
                int doneCount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    RequestModel req = ds.getValue(RequestModel.class);
                    if (req != null) {
                        if ("Выполнено".equalsIgnoreCase(req.getStatus())) doneCount++;
                        if (req.getRating() > 0) {
                            totalRating += req.getRating();
                            ratingCount++;
                        }
                    }
                }
                tvDone.setText("Сделано: " + doneCount);
                if (ratingCount > 0) tvRating.setText(String.format(Locale.getDefault(), "%.1f ⭐", totalRating/ratingCount));
                else tvRating.setText("0.0 ⭐");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateWorkersCount() {
        mDatabase.child("Companies").child(companyId).child("workers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvWorkersCount.setText("Исполнителей: " + count);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deleteWorker(String workerId) {
        // Удаляем из общего списка пользователей
        mDatabase.child("users").child(workerId).removeValue().addOnSuccessListener(aVoid -> {
            // Удаляем из списка рабочих компании
            mDatabase.child("Companies").child(companyId).child("workers").child(workerId).removeValue();
            Toast.makeText(this, "Исполнитель удален", Toast.LENGTH_SHORT).show();
        });
    }

    public static class WorkerViewHolder extends RecyclerView.ViewHolder {
        TextView name, spec, rating, doneCount;
        ImageButton btnDelete;
        public WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_worker_name);
            spec = itemView.findViewById(R.id.tv_worker_spec);
            rating = itemView.findViewById(R.id.tv_worker_rating);
            doneCount = itemView.findViewById(R.id.tv_worker_done_count);
            btnDelete = itemView.findViewById(R.id.btn_delete_worker);
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}
