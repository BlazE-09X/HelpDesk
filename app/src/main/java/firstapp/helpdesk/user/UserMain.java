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
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
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

public class UserMain extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, RequestViewHolder> adapter;
    private DatabaseReference requestsRef;

    private static class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) { super(context); }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try { super.onLayoutChildren(recycler, state); } catch (IndexOutOfBoundsException e) { Log.e("RV", "Error"); }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_main);

        requestsRef = FirebaseDatabase.getInstance().getReference("Requests");
        recyclerView = findViewById(R.id.rv_user_requests);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        setupAdapter("");

        EditText etSearch = findViewById(R.id.et_search_requests);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { setupAdapter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_user);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_add) {
                startActivity(new Intent(this, CreateSelectionActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfile.class));
                return true;
            }
            return true;
        });
    }

    private void setupAdapter(String searchText) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        Query query = requestsRef.orderByChild("userId").equalTo(currentUid);

        FirebaseRecyclerOptions<RequestModel> options = new FirebaseRecyclerOptions.Builder<RequestModel>()
                .setQuery(query, RequestModel.class).build();

        if (adapter != null) adapter.stopListening();

        adapter = new FirebaseRecyclerAdapter<RequestModel, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull RequestModel model) {
                if (!model.getTitle().toLowerCase().contains(searchText.toLowerCase())) {
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0,0));
                    return;
                }
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                String formattedNum = String.format("#%03d", position + 1);
                holder.number.setText(formattedNum);
                holder.title.setText(model.getTitle());
                
                String status = model.getStatus() != null ? model.getStatus() : "Новая";
                holder.statusText.setText(status);

                int color;
                boolean isCompleted = "Выполнено".equalsIgnoreCase(status);
                switch (status) {
                    case "Новая": color = 0xFF00BFFF; break;
                    case "В работе": color = 0xFFFFA500; break;
                    case "Выполнено": color = 0xFF888888; break; // Сделали серым по запросу
                    default: color = 0xFF888888; break;
                }
                holder.statusColor.setBackgroundColor(color);
                holder.statusText.setTextColor(color);

                // Дедлайн становится серым, если заявка выполнена
                if (isCompleted) {
                    holder.deadline.setTextColor(0xFF888888);
                } else {
                    holder.deadline.setTextColor(0xFFD32F2F); // Исходный красный
                }

                if (isCompleted && model.getRating() > 0) {
                    holder.ratingBar.setVisibility(View.VISIBLE);
                    holder.ratingBar.setRating(model.getRating());
                } else {
                    holder.ratingBar.setVisibility(View.GONE);
                }

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(UserMain.this, EditRequestActivity.class);
                    intent.putExtra("requestId", getRef(holder.getBindingAdapterPosition()).getKey());
                    intent.putExtra("requestNumber", formattedNum);
                    startActivity(intent);
                });
            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
                return new RequestViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        public TextView number, title, statusText, deadline;
        public View statusColor;
        public RatingBar ratingBar;
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.tv_request_number);
            title = itemView.findViewById(R.id.tv_request_title);
            statusText = itemView.findViewById(R.id.tv_request_status_text);
            statusColor = itemView.findViewById(R.id.view_status_color);
            ratingBar = itemView.findViewById(R.id.rb_item_rating);
            deadline = itemView.findViewById(R.id.tv_request_deadline);
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}