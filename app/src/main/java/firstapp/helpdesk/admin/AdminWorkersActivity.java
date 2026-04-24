package firstapp.helpdesk.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import firstapp.helpdesk.R;

public class AdminWorkersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<UserModel, WorkerViewHolder> adapter;

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
        setContentView(R.layout.activity_admin_workers);

        mDatabase = FirebaseDatabase.getInstance().getReference("Workers");
        recyclerView = findViewById(R.id.rv_admin_workers);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        EditText etSearch = findViewById(R.id.et_search_workers);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchWorkers(s.toString().trim());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupAdapter(mDatabase);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_workers);
        bottomNav.setSelectedItemId(R.id.nav_workers);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, AdminMain.class)); return true; }
            if (id == R.id.nav_users) { startActivity(new Intent(this, AdminUsers.class)); return true; }
            if (id == R.id.nav_tasks) { startActivity(new Intent(this, Companies.class)); return true; }
            if (id == R.id.nav_housing) { startActivity(new Intent(this, HousingComplexesActivity.class)); return true; }
            return id == R.id.nav_workers;
        });

        findViewById(R.id.fab_add_worker_new).setOnClickListener(v -> startActivity(new Intent(this, AdminRegisterWorker.class)));
    }

    private void setupAdapter(Query query) {
        FirebaseRecyclerOptions<UserModel> options = new FirebaseRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class).build();

        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new FirebaseRecyclerAdapter<UserModel, WorkerViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull WorkerViewHolder holder, int position, @NonNull UserModel model) {
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                
                // Исправлено: добавляем Отчество в строку полного имени
                String fullName = ((model.getSurname() != null ? model.getSurname() : "") + " " + 
                                 (model.getName() != null ? model.getName() : "") + " " +
                                 (model.getPatronymic() != null ? model.getPatronymic() : "")).trim();
                
                holder.name.setText(fullName.isEmpty() ? model.getEmail() : fullName);
                holder.info.setText("Спец.: " + (model.getSpecialization() != null ? model.getSpecialization() : "Не указана"));
                
                holder.btnDelete.setOnClickListener(v -> {
                    int currentPos = holder.getBindingAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        String uid = getRef(currentPos).getKey();
                        getRef(currentPos).removeValue().addOnSuccessListener(aVoid -> {
                            FirebaseDatabase.getInstance().getReference("users").child(uid).removeValue();
                            Toast.makeText(AdminWorkersActivity.this, "Удалено", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            @NonNull
            @Override
            public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
                return new WorkerViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void searchWorkers(String text) {
        if (text.isEmpty()) {
            setupAdapter(mDatabase);
        } else {
            String searchText = text.toLowerCase();
            Query query = mDatabase.orderByChild("search_name").startAt(searchText).endAt(searchText + "\uf8ff");
            setupAdapter(query);
        }
    }

    public static class WorkerViewHolder extends RecyclerView.ViewHolder {
        TextView name, info;
        ImageView btnDelete;
        public WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_user_name);
            info = itemView.findViewById(R.id.tv_user_role);
            btnDelete = itemView.findViewById(R.id.iv_delete_user);
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}
