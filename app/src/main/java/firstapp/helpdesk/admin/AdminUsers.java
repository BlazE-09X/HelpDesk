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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

import firstapp.helpdesk.R;

public class AdminUsers extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<UserModel, UserViewHolder> adapter;
    private Spinner spinnerJK;
    private List<String> jkList = new ArrayList<>();
    private String selectedJK = "Все ЖК";

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
        setContentView(R.layout.admin_users);

        mDatabase = FirebaseDatabase.getInstance().getReference("Residents");
        
        recyclerView = findViewById(R.id.rv_admin_users);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        spinnerJK = findViewById(R.id.spinner_filter_jk);
        loadJKList();

        EditText etSearch = findViewById(R.id.et_search_users);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters(s.toString().trim());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupAdapter(mDatabase);

        findViewById(R.id.fab_add_user).setOnClickListener(v -> 
            startActivity(new Intent(AdminUsers.this, AdminRegisterUserActivity.class)));

        setupNavigation();
    }

    private void loadJKList() {
        jkList.clear();
        jkList.add("Все ЖК");
        FirebaseDatabase.getInstance().getReference("HousingComplexes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("name").getValue(String.class);
                    if (name != null) jkList.add(name);
                }
                ArrayAdapter<String> adapterJK = new ArrayAdapter<>(AdminUsers.this, android.R.layout.simple_spinner_item, jkList);
                adapterJK.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerJK.setAdapter(adapterJK);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        spinnerJK.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedJK = jkList.get(position);
                applyFilters(((EditText)findViewById(R.id.et_search_users)).getText().toString().trim());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilters(String searchText) {
        Query query = mDatabase;
        if (!selectedJK.equals("Все ЖК")) {
            query = mDatabase.orderByChild("companyName").equalTo(selectedJK);
        } else if (!searchText.isEmpty()) {
            String text = searchText.toLowerCase();
            query = mDatabase.orderByChild("search_name").startAt(text).endAt(text + "\uf8ff");
        }
        setupAdapter(query);
    }

    private void setupAdapter(Query query) {
        FirebaseRecyclerOptions<UserModel> options = new FirebaseRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class)
                .build();

        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new FirebaseRecyclerAdapter<UserModel, UserViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull UserModel model) {
                // Если выбран фильтр по ЖК, но в поиске что-то есть, фильтруем локально (Firebase не поддерживает кратные фильтры)
                String searchText = ((EditText)findViewById(R.id.et_search_users)).getText().toString().trim().toLowerCase();
                if (!searchText.isEmpty() && !selectedJK.equals("Все ЖК")) {
                    String fullName = ((model.getSurname() != null ? model.getSurname() : "") + " " + 
                                     (model.getName() != null ? model.getName() : "") + " " + 
                                     (model.getPatronymic() != null ? model.getPatronymic() : "")).toLowerCase();
                    if (!fullName.contains(searchText) && !model.getEmail().toLowerCase().contains(searchText)) {
                        holder.itemView.setVisibility(View.GONE);
                        holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                        return;
                    }
                }
                
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                String fullName = ((model.getSurname() != null ? model.getSurname() : "") + " " + 
                                 (model.getName() != null ? model.getName() : "") + " " + 
                                 (model.getPatronymic() != null ? model.getPatronymic() : "")).trim();
                
                holder.name.setText(fullName.isEmpty() ? model.getEmail() : fullName);
                holder.info.setText("ЖК: " + (model.getCompanyName() != null ? model.getCompanyName() : "Не указан"));

                holder.btnDelete.setOnClickListener(v -> {
                    int currentPos = holder.getBindingAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        String uid = getRef(currentPos).getKey();
                        getRef(currentPos).removeValue().addOnSuccessListener(aVoid -> {
                            FirebaseDatabase.getInstance().getReference("users").child(uid).removeValue();
                            Toast.makeText(AdminUsers.this, "Житель удален", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
                return new UserViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_users);
        bottomNav.setSelectedItemId(R.id.nav_users);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, AdminMain.class)); return true; }
            if (id == R.id.nav_workers) { startActivity(new Intent(this, AdminWorkersActivity.class)); return true; }
            if (id == R.id.nav_tasks) { startActivity(new Intent(this, Companies.class)); return true; }
            if (id == R.id.nav_housing) { startActivity(new Intent(this, HousingComplexesActivity.class)); return true; }
            return id == R.id.nav_users;
        });
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView name, info;
        ImageView btnDelete;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_user_name);
            info = itemView.findViewById(R.id.tv_user_role);
            btnDelete = itemView.findViewById(R.id.iv_delete_user);
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}
