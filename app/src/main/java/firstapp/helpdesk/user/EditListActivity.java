package firstapp.helpdesk.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import firstapp.helpdesk.R;

public class EditListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, UserMain.RequestViewHolder> adapter;

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
        setContentView(R.layout.activity_edit_list);

        recyclerView = findViewById(R.id.rv_edit_requests);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Requests");
        // Запрашиваем все заявки пользователя, фильтрацию сделаем в адаптере для надежности
        Query query = requestsRef.orderByChild("userId").equalTo(currentUid);

        FirebaseRecyclerOptions<RequestModel> options =
                new FirebaseRecyclerOptions.Builder<RequestModel>()
                        .setQuery(query, RequestModel.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<RequestModel, UserMain.RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserMain.RequestViewHolder holder, int position, @NonNull RequestModel model) {
                String status = model.getStatus() != null ? model.getStatus().trim() : "Новая";

                // СТРОГИЙ ФИЛЬТР: только "Новая" (без учета регистра)
                if (!"Новая".equalsIgnoreCase(status)) {
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                }

                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                String formattedNumber = String.format("#%03d", position + 1);
                holder.number.setText(formattedNumber);
                holder.title.setText("Тема: " + model.getTitle());
                
                if (holder.statusText != null) {
                    holder.statusText.setText(status);
                }
                holder.statusColor.setBackgroundColor(0xFF00BFFF); // Голубой для новых

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(EditListActivity.this, EditRequestActivity.class);
                    intent.putExtra("requestId", getRef(holder.getBindingAdapterPosition()).getKey());
                    intent.putExtra("requestNumber", formattedNumber);
                    startActivity(intent);
                });
            }

            @NonNull
            @Override
            public UserMain.RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
                return new UserMain.RequestViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_user);
        bottomNav.setSelectedItemId(R.id.nav_add);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, UserMain.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfile.class));
                finish();
                return true;
            }
            return id == R.id.nav_add;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }
}