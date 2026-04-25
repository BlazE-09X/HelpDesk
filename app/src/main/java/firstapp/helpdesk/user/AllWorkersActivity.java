package firstapp.helpdesk.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import firstapp.helpdesk.R;
import firstapp.helpdesk.admin.UserModel;

public class AllWorkersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseRecyclerAdapter<UserModel, WorkerViewHolder> adapter;
    private DatabaseReference mDatabase;
    private Map<String, Float> ratingsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_workers);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        recyclerView = findViewById(R.id.rv_all_workers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        EditText etSearch = findViewById(R.id.et_search_workers);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSearch(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadRatingsAndSetupAdapter();
    }

    private void loadRatingsAndSetupAdapter() {
        // Сначала считаем средний рейтинг для всех исполнителей
        mDatabase.child("Requests").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Float> total = new HashMap<>();
                Map<String, Integer> count = new HashMap<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String execId = ds.child("executorId").getValue(String.class);
                    Float rating = ds.child("rating").getValue(Float.class);
                    if (execId != null && rating != null && rating > 0) {
                        total.put(execId, total.getOrDefault(execId, 0f) + rating);
                        count.put(execId, count.getOrDefault(execId, 0) + 1);
                    }
                }

                for (String id : total.keySet()) {
                    ratingsMap.put(id, total.get(id) / count.get(id));
                }
                setupAdapter(mDatabase.child("Workers"));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSearch(String text) {
        Query query = mDatabase.child("Workers");
        if (!text.isEmpty()) {
            query = query.orderByChild("search_name").startAt(text.toLowerCase()).endAt(text.toLowerCase() + "\uf8ff");
        }
        setupAdapter(query);
    }

    private void setupAdapter(Query query) {
        FirebaseRecyclerOptions<UserModel> options = new FirebaseRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel.class).build();

        if (adapter != null) adapter.stopListening();

        adapter = new FirebaseRecyclerAdapter<UserModel, WorkerViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull WorkerViewHolder holder, int position, @NonNull UserModel model) {
                String fullName = (model.getSurname() + " " + model.getName()).trim();
                holder.name.setText(fullName);
                holder.category.setText("Категория: " + (model.getSpecialization() != null ? model.getSpecialization() : "-"));
                holder.org.setText("Организация: " + (model.getCompanyName() != null ? model.getCompanyName() : "-"));
                
                Float rating = ratingsMap.get(getRef(position).getKey());
                holder.ratingBar.setRating(rating != null ? rating : 0f);
            }

            @NonNull
            @Override
            public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worker_card, parent, false);
                return new WorkerViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    public static class WorkerViewHolder extends RecyclerView.ViewHolder {
        TextView name, category, org;
        RatingBar ratingBar;
        public WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_worker_name);
            category = itemView.findViewById(R.id.tv_worker_category);
            org = itemView.findViewById(R.id.tv_worker_org);
            ratingBar = itemView.findViewById(R.id.rb_worker_stars);
        }
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }
}
