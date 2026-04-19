package firstapp.helpdesk.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import firstapp.helpdesk.R;

public class HousingComplexesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<HousingComplexModel, HousingViewHolder> adapter;

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
        setContentView(R.layout.admin_housing_complexes);

        mDatabase = FirebaseDatabase.getInstance().getReference("HousingComplexes");

        recyclerView = findViewById(R.id.rv_housing_complexes);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        FirebaseRecyclerOptions<HousingComplexModel> options =
                new FirebaseRecyclerOptions.Builder<HousingComplexModel>()
                        .setQuery(mDatabase, HousingComplexModel.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<HousingComplexModel, HousingViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull HousingViewHolder holder, int position, @NonNull HousingComplexModel model) {
                String jkId = getRef(position).getKey();
                holder.name.setText(model.getName());
                holder.address.setText(model.getAddress());
                
                // Переход к управлению обслуживающими организациями
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(HousingComplexesActivity.this, AdminManageJKServicesActivity.class);
                    intent.putExtra("jkId", jkId);
                    intent.putExtra("jkName", model.getName());
                    startActivity(intent);
                });

                holder.btnDelete.setOnClickListener(v -> {
                    int currentPos = holder.getBindingAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        getRef(currentPos).removeValue().addOnSuccessListener(aVoid -> 
                            Toast.makeText(HousingComplexesActivity.this, "ЖК удален", Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @NonNull
            @Override
            public HousingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_housing, parent, false);
                return new HousingViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);

        findViewById(R.id.fab_add_housing).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminCreateHousingActivity.class));
        });

        setupNavigation();
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
