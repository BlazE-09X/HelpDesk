package firstapp.helpdesk.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import firstapp.helpdesk.R;
import firstapp.helpdesk.admin.CompanyModel;

public class UserOrganizationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<CompanyModel, CompanyViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_organizations);

        mDatabase = FirebaseDatabase.getInstance().getReference("Organizations");
        recyclerView = findViewById(R.id.rv_user_organizations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        EditText etSearch = findViewById(R.id.et_search_orgs);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchOrgs(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        setupAdapter(mDatabase);
        
        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
    }

    private void setupAdapter(Query query) {
        FirebaseRecyclerOptions<CompanyModel> options = new FirebaseRecyclerOptions.Builder<CompanyModel>()
                .setQuery(query, CompanyModel.class).build();

        if (adapter != null) adapter.stopListening();

        adapter = new FirebaseRecyclerAdapter<CompanyModel, CompanyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CompanyViewHolder holder, int position, @NonNull CompanyModel model) {
                holder.name.setText(model.getName());
                holder.domain.setText(model.getDomain());
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(UserOrganizationsActivity.this, UserWorkersActivity.class);
                    intent.putExtra("orgId", getRef(position).getKey());
                    intent.putExtra("orgName", model.getName());
                    startActivity(intent);
                });
            }
            @NonNull
            @Override
            public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
                return new CompanyViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void searchOrgs(String text) {
        if (text.isEmpty()) {
            setupAdapter(mDatabase);
        } else {
            String searchFormatted = text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
            Query query = mDatabase.orderByChild("name").startAt(searchFormatted).endAt(searchFormatted + "\uf8ff");
            setupAdapter(query);
        }
    }

    public static class CompanyViewHolder extends RecyclerView.ViewHolder {
        TextView name, domain;
        public CompanyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(android.R.id.text1);
            domain = itemView.findViewById(android.R.id.text2);
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}