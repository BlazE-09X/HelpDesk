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

import androidx.activity.OnBackPressedCallback;
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

public class Companies extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<CompanyModel, CompanyViewHolder> adapter;
    private boolean isDeleteMode = false;
    private TextView tvCancel;

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
        setContentView(R.layout.admin_companies);

        mDatabase = FirebaseDatabase.getInstance().getReference("Organizations");

        tvCancel = findViewById(R.id.tv_cancel);
        recyclerView = findViewById(R.id.rv_companies);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        EditText etSearch = findViewById(R.id.et_search_companies);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchCompanies(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        setupAdapter(mDatabase);

        ImageView btnAdd = findViewById(R.id.btn_add_company);
        btnAdd.setOnClickListener(v -> startActivity(new Intent(Companies.this, CreateCompany.class)));

        ImageView btnBin = findViewById(R.id.btn_delete_company);
        btnBin.setOnClickListener(v -> {
            isDeleteMode = true;
            tvCancel.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        });

        tvCancel.setOnClickListener(v -> {
            exitDeleteMode();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isDeleteMode) {
                    exitDeleteMode();
                } else {
                    finish();
                }
            }
        });

        setupNavigation();
    }

    private void exitDeleteMode() {
        isDeleteMode = false;
        tvCancel.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void setupAdapter(Query query) {
        FirebaseRecyclerOptions<CompanyModel> options =
                new FirebaseRecyclerOptions.Builder<CompanyModel>()
                        .setQuery(query, CompanyModel.class)
                        .build();

        if (adapter != null) adapter.stopListening();

        adapter = new FirebaseRecyclerAdapter<CompanyModel, CompanyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CompanyViewHolder holder, int position, @NonNull CompanyModel model) {
                holder.name.setText(model.getName());
                holder.domain.setText(model.getDomain());
                holder.btnDelete.setVisibility(isDeleteMode ? View.VISIBLE : View.GONE);
                holder.btnDelete.setOnClickListener(v -> {
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        getRef(currentPosition).removeValue().addOnSuccessListener(aVoid -> 
                            Toast.makeText(Companies.this, "Организация удалена", Toast.LENGTH_SHORT).show());
                    }
                });
            }
            @NonNull
            @Override
            public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company, parent, false);
                return new CompanyViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void searchCompanies(String text) {
        if (text.isEmpty()) {
            setupAdapter(mDatabase);
        } else {
            String searchFormatted = text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
            Query query = mDatabase.orderByChild("name").startAt(searchFormatted).endAt(searchFormatted + "\uf8ff");
            setupAdapter(query);
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_companies);
        bottomNav.setSelectedItemId(R.id.nav_tasks);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, AdminMain.class)); return true; }
            if (id == R.id.nav_users) { startActivity(new Intent(this, AdminUsers.class)); return true; }
            if (id == R.id.nav_workers) { startActivity(new Intent(this, AdminWorkersActivity.class)); return true; }
            if (id == R.id.nav_housing) { startActivity(new Intent(this, HousingComplexesActivity.class)); return true; }
            return id == R.id.nav_tasks;
        });
    }

    public static class CompanyViewHolder extends RecyclerView.ViewHolder {
        TextView name, domain;
        ImageView btnDelete;
        public CompanyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_company_name);
            domain = itemView.findViewById(R.id.tv_company_domain);
            btnDelete = itemView.findViewById(R.id.iv_delete_item);
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}
