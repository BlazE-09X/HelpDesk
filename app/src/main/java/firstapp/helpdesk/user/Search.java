package firstapp.helpdesk.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

public class Search extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, UserMain.RequestViewHolder> adapter;
    private DatabaseReference requestsRef;
    private String currentUid;

    private static class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) { super(context); }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try { super.onLayoutChildren(recycler, state); } catch (IndexOutOfBoundsException e) { Log.e("RecyclerView", "Inconsistency detected"); }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_search);

        requestsRef = FirebaseDatabase.getInstance().getReference("Requests");
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.rv_search_results);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        android.widget.EditText etSearch = findViewById(R.id.et_search_input);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                startSearch(s.toString().trim());
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });

        setupBottomNavigation();
        startSearch(""); 
    }

    private void startSearch(String searchText) {
        // Firebase не поддерживает поиск case-insensitive напрямую через Query, 
        // поэтому обычно данные сохраняют в нижнем регистре (title_lowercase).
        // В данном случае применим фильтрацию на клиенте или поиск по префиксу.
        
        Query query = requestsRef.orderByChild("userId").equalTo(currentUid);

        FirebaseRecyclerOptions<RequestModel> options = new FirebaseRecyclerOptions.Builder<RequestModel>()
                        .setQuery(query, RequestModel.class)
                        .build();

        if (adapter != null) adapter.stopListening();

        adapter = new FirebaseRecyclerAdapter<RequestModel, UserMain.RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserMain.RequestViewHolder holder, int position, @NonNull RequestModel model) {
                // Ручная фильтрация по тексту (регистронезависимая)
                String title = model.getTitle() != null ? model.getTitle().toLowerCase() : "";
                if (!searchText.isEmpty() && !title.contains(searchText.toLowerCase())) {
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                }

                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                holder.number.setText("#" + String.format("%03d", position + 1));
                holder.title.setText(model.getTitle());
                
                String status = model.getStatus() != null ? model.getStatus() : "Новая";
                holder.statusText.setText(status);

                int color;
                switch (status) {
                    case "Выполнено": color = 0xFF32CD32; break;
                    case "Отклонено": color = 0xFFFF0000; break;
                    case "В работе": color = 0xFFFFA500; break;
                    default: color = 0xFF00BFFF; break;
                }
                holder.statusColor.setBackgroundColor(color);
                holder.statusText.setTextColor(color);
                
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(Search.this, EditRequestActivity.class);
                    intent.putExtra("requestId", getRef(holder.getBindingAdapterPosition()).getKey());
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

        adapter.startListening();
        recyclerView.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_user);
        bottomNav.setSelectedItemId(R.id.nav_search);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, UserMain.class)); finish(); return true; }
            if (id == R.id.nav_add) { startActivity(new Intent(this, CreateSelectionActivity.class)); finish(); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, UserProfile.class)); finish(); return true; }
            return id == R.id.nav_search;
        });
    }
}
