package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import firstapp.helpdesk.R;

public class AdminCategoriesActivity extends AppCompatActivity {

    private EditText etNewCategory, etSearch;
    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private DatabaseReference mDatabase;
    private List<CategoryItem> fullList = new ArrayList<>();
    private List<CategoryItem> filteredList = new ArrayList<>();

    private static class CategoryItem {
        String id;
        String name;
        CategoryItem(String id, String name) { this.id = id; this.name = name; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_categories);

        mDatabase = FirebaseDatabase.getInstance().getReference("Categories");

        etNewCategory = findViewById(R.id.et_new_category);
        etSearch = findViewById(R.id.et_search_category);
        recyclerView = findViewById(R.id.rv_categories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CategoryAdapter();
        recyclerView.setAdapter(adapter);

        loadCategories();

        findViewById(R.id.btn_add_category).setOnClickListener(v -> {
            String name = etNewCategory.getText().toString().trim();
            if (!name.isEmpty()) {
                mDatabase.push().setValue(name).addOnSuccessListener(aVoid -> {
                    etNewCategory.setText("");
                    Toast.makeText(this, "Добавлено", Toast.LENGTH_SHORT).show();
                });
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCategories() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.getValue(String.class);
                    if (name != null) fullList.add(new CategoryItem(ds.getKey(), name));
                }
                filter(etSearch.getText().toString());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filter(String text) {
        filteredList.clear();
        for (CategoryItem item : fullList) {
            if (item.name.toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryItem item = filteredList.get(position);
            holder.text.setText(item.name);
            holder.btnDelete.setOnClickListener(v -> {
                mDatabase.child(item.id).removeValue().addOnSuccessListener(aVoid -> 
                    Toast.makeText(AdminCategoriesActivity.this, "Удалено", Toast.LENGTH_SHORT).show());
            });
        }

        @Override public int getItemCount() { return filteredList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ImageView btnDelete;
            ViewHolder(View v) { 
                super(v); 
                text = v.findViewById(R.id.tv_category_name); 
                btnDelete = v.findViewById(R.id.iv_delete_category);
            }
        }
    }
}
