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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import firstapp.helpdesk.R;

public class HousingComplexesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<HousingComplexModel, HousingViewHolder> adapter;
    private Spinner spinnerOrg;
    private List<String> orgList = new ArrayList<>();
    private String selectedOrg = "Все организации";
    private Map<String, Set<String>> orgToJKMap = new HashMap<>();
    private Map<String, String> orgIdToNameMap = new HashMap<>();

    private static class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) { super(context); }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try { super.onLayoutChildren(recycler, state); } catch (IndexOutOfBoundsException e) { Log.e("RecyclerView", "Inconsistency"); }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_housing_complexes);

        mDatabase = FirebaseDatabase.getInstance().getReference("HousingComplexes");

        recyclerView = findViewById(R.id.rv_housing_complexes);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        spinnerOrg = findViewById(R.id.spinner_filter_org_housing);
        loadOrgAndJKServices();

        EditText etSearch = findViewById(R.id.et_search_housing);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        setupAdapter(mDatabase);

        findViewById(R.id.fab_add_housing).setOnClickListener(v -> startActivity(new Intent(this, AdminCreateHousingActivity.class)));
        setupNavigation();
    }

    private void loadOrgAndJKServices() {
        FirebaseDatabase.getInstance().getReference("Organizations").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orgList.clear();
                orgIdToNameMap.clear();
                orgList.add("Все организации");
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("name").getValue(String.class);
                    if (name != null) {
                        orgList.add(name);
                        orgIdToNameMap.put(ds.getKey(), name);
                    }
                }
                ArrayAdapter<String> adapterOrg = new ArrayAdapter<>(HousingComplexesActivity.this, android.R.layout.simple_spinner_item, orgList);
                adapterOrg.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerOrg.setAdapter(adapterOrg);
                
                // Загружаем связи ЖК и организаций
                loadJKConnections();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        spinnerOrg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedOrg = orgList.get(position);
                applyFilters(((EditText)findViewById(R.id.et_search_housing)).getText().toString().trim());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadJKConnections() {
        FirebaseDatabase.getInstance().getReference("JK_Services").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                orgToJKMap.clear();
                for (DataSnapshot jkSnapshot : snapshot.getChildren()) {
                    String jkId = jkSnapshot.getKey();
                    for (DataSnapshot orgSnapshot : jkSnapshot.getChildren()) {
                        String orgId = orgSnapshot.getKey();
                        String orgName = orgIdToNameMap.get(orgId);
                        if (orgName != null) {
                            if (!orgToJKMap.containsKey(orgName)) orgToJKMap.put(orgName, new HashSet<>());
                            orgToJKMap.get(orgName).add(jkId);
                        }
                    }
                }
                if (adapter != null) adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void applyFilters(String searchText) {
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void setupAdapter(Query query) {
        FirebaseRecyclerOptions<HousingComplexModel> options = new FirebaseRecyclerOptions.Builder<HousingComplexModel>()
                        .setQuery(query, HousingComplexModel.class).build();

        if (adapter != null) adapter.stopListening();

        adapter = new FirebaseRecyclerAdapter<HousingComplexModel, HousingViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull HousingViewHolder holder, int position, @NonNull HousingComplexModel model) {
                String jkId = getRef(position).getKey();
                String searchText = ((EditText)findViewById(R.id.et_search_housing)).getText().toString().trim().toLowerCase();

                // Фильтр по организации
                if (!selectedOrg.equals("Все организации")) {
                    Set<String> validJKs = orgToJKMap.get(selectedOrg);
                    if (validJKs == null || !validJKs.contains(jkId)) {
                        hideItem(holder); return;
                    }
                }

                // Фильтр по поиску
                if (!searchText.isEmpty() && !model.getName().toLowerCase().contains(searchText)) {
                    hideItem(holder); return;
                }

                showItem(holder);
                holder.name.setText(model.getName());
                holder.address.setText(model.getAddress());
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(HousingComplexesActivity.this, AdminManageJKServicesActivity.class);
                    intent.putExtra("jkId", jkId);
                    intent.putExtra("jkName", model.getName());
                    startActivity(intent);
                });
                holder.btnDelete.setOnClickListener(v -> getRef(holder.getBindingAdapterPosition()).removeValue());
            }

            private void hideItem(HousingViewHolder holder) {
                holder.itemView.setVisibility(View.GONE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            }
            private void showItem(HousingViewHolder holder) {
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            @NonNull
            @Override
            public HousingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_housing, parent, false);
                return new HousingViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_housing);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, AdminMain.class)); return true; }
            if (id == R.id.nav_users) { startActivity(new Intent(this, AdminUsers.class)); return true; }
            if (id == R.id.nav_workers) { startActivity(new Intent(this, AdminWorkersActivity.class)); return true; }
            if (id == R.id.nav_tasks) { startActivity(new Intent(this, Companies.class)); return true; }
            return id == R.id.nav_housing;
        });
    }

    public static class HousingViewHolder extends RecyclerView.ViewHolder {
        TextView name, address;
        ImageView btnDelete;
        public HousingViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_housing_name);
            address = itemView.findViewById(R.id.tv_housing_address);
            btnDelete = itemView.findViewById(R.id.iv_delete_housing);
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}
