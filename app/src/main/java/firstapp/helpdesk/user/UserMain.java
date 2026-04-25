package firstapp.helpdesk.user;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import firstapp.helpdesk.R;

public class UserMain extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<RequestModel, RequestViewHolder> adapter;
    private DatabaseReference mDatabase;
    private String currentUid;
    private EditText etSearch;

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
        setContentView(R.layout.user_main);

        currentUid = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("Requests");

        recyclerView = findViewById(R.id.rv_user_requests);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        etSearch = findViewById(R.id.et_search_requests);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSearch(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        setupAdapter(mDatabase.orderByChild("userId").equalTo(currentUid));

        findViewById(R.id.fab_add_request).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditRequestActivity.class);
            intent.putExtra("isNewRequest", true);
            startActivity(intent);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_user);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_profile) { startActivity(new Intent(this, UserProfile.class)); return true; }
            if (id == R.id.nav_search) { startActivity(new Intent(this, AllWorkersActivity.class)); return true; }
            if (id == R.id.nav_add) { startActivity(new Intent(this, CreateSelectionActivity.class)); return true; }
            return false;
        });
    }

    private void updateSearch(String text) {
        Query query;
        if (text.isEmpty()) {
            query = mDatabase.orderByChild("userId").equalTo(currentUid);
        } else {
            query = mDatabase.orderByChild("title").startAt(text).endAt(text + "\uf8ff");
        }
        setupAdapter(query);
    }

    private void setupAdapter(Query query) {
        FirebaseRecyclerOptions<RequestModel> options = new FirebaseRecyclerOptions.Builder<RequestModel>()
                .setQuery(query, RequestModel.class).build();

        if (adapter != null) adapter.stopListening();

        adapter = new FirebaseRecyclerAdapter<RequestModel, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull RequestModel model) {
                if (etSearch.getText().toString().length() > 0 && model.getUserId() != null && !model.getUserId().equals(currentUid)) {
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0,0));
                    return;
                }
                holder.itemView.setVisibility(View.VISIBLE);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                holder.number.setText("#" + String.format("%03d", position + 1));
                holder.title.setText("Тема: " + model.getTitle());
                
                String status = model.getStatus() != null ? model.getStatus() : "Новая";
                holder.statusText.setText(status);
                
                int color;
                switch (status) {
                    case "Выполнено": color = 0xFF27AE60; break;
                    case "Отклонено": color = 0xFFB02A2A; break;
                    case "В работе": color = 0xFFF2994A; break;
                    default: color = 0xFF56CCF2; break;
                }
                holder.statusColor.setBackgroundColor(color);
                holder.statusText.setTextColor(color);

                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                if (model.getTimestamp() > 0) {
                    holder.date.setText("Создана: " + sdf.format(new Date(model.getTimestamp())));
                }

                if (holder.deadline != null) {
                    if ("immediate".equals(model.getExecutionType()) || model.getDeadlineDate() == 0) {
                        holder.deadline.setText("Дедлайн: Немедленно");
                        holder.deadline.setTextColor(Color.RED);
                    } else {
                        SimpleDateFormat sdfD = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                        holder.deadline.setText("Дедлайн: " + sdfD.format(new Date(model.getDeadlineDate())));
                        holder.deadline.setTextColor(Color.GRAY);
                    }
                }

                if (model.getRating() > 0) {
                    holder.ratingBar.setVisibility(View.VISIBLE);
                    holder.ratingBar.setRating(model.getRating());
                } else {
                    holder.ratingBar.setVisibility(View.GONE);
                }

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(UserMain.this, EditRequestActivity.class);
                    intent.putExtra("requestId", getRef(holder.getBindingAdapterPosition()).getKey());
                    intent.putExtra("requestNumber", holder.number.getText().toString());
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

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        public TextView number, title, date, deadline, statusText;
        public View statusColor;
        public RatingBar ratingBar;
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.tv_request_number);
            title = itemView.findViewById(R.id.tv_request_title);
            date = itemView.findViewById(R.id.tv_request_date);
            deadline = itemView.findViewById(R.id.tv_request_deadline);
            statusText = itemView.findViewById(R.id.tv_request_status_text);
            statusColor = itemView.findViewById(R.id.view_status_color);
            ratingBar = itemView.findViewById(R.id.rb_item_rating);
        }
    }
}
