package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.database.ValueEventListener;

import firstapp.helpdesk.R;

public class AdminManageJKServicesActivity extends AppCompatActivity {

    private String jkId, jkName;
    private DatabaseReference mDatabase;
    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<CompanyModel, OrgViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_jk_services);

        jkId = getIntent().getStringExtra("jkId");
        jkName = getIntent().getStringExtra("jkName");

        TextView tvTitle = findViewById(R.id.tv_title_manage);
        tvTitle.setText("Обслуживание: " + jkName);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        recyclerView = findViewById(R.id.rv_orgs_to_link);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupAdapter();
        
        findViewById(R.id.btn_done_linking).setOnClickListener(v -> finish());
    }

    private void setupAdapter() {
        Query query = mDatabase.child("Organizations");
        FirebaseRecyclerOptions<CompanyModel> options = new FirebaseRecyclerOptions.Builder<CompanyModel>()
                .setQuery(query, CompanyModel.class).build();

        adapter = new FirebaseRecyclerAdapter<CompanyModel, OrgViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull OrgViewHolder holder, int position, @NonNull CompanyModel model) {
                String orgId = getRef(position).getKey();
                holder.name.setText(model.getName());
                
                // Проверяем, привязана ли уже эта организация
                mDatabase.child("JK_Services").child(jkId).child(orgId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        holder.checkBox.setChecked(snapshot.exists());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

                holder.checkBox.setOnClickListener(v -> {
                    if (holder.checkBox.isChecked()) {
                        mDatabase.child("JK_Services").child(jkId).child(orgId).setValue(true);
                        // И обратная связь для быстрого поиска
                        mDatabase.child("Org_Serves_JK").child(orgId).child(jkId).setValue(true);
                    } else {
                        mDatabase.child("JK_Services").child(jkId).child(orgId).removeValue();
                        mDatabase.child("Org_Serves_JK").child(orgId).child(jkId).removeValue();
                    }
                });
            }

            @NonNull
            @Override
            public OrgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_link_org, parent, false);
                return new OrgViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    public static class OrgViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        CheckBox checkBox;
        public OrgViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_org_name_link);
            checkBox = itemView.findViewById(R.id.cb_link_org);
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}
