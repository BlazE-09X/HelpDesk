package firstapp.helpdesk.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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

    private EditText etNewCategory;
    private Button btnAdd;
    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private DatabaseReference mDatabase;
    private List<String> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_categories);

        mDatabase = FirebaseDatabase.getInstance().getReference("Categories");

        etNewCategory = findViewById(R.id.et_new_category);
        btnAdd = findViewById(R.id.btn_add_category);
        recyclerView = findViewById(R.id.rv_categories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CategoryAdapter();
        recyclerView.setAdapter(adapter);

        loadCategories();

        btnAdd.setOnClickListener(v -> {
            String name = etNewCategory.getText().toString().trim();
            if (!name.isEmpty()) {
                mDatabase.push().setValue(name).addOnSuccessListener(aVoid -> {
                    etNewCategory.setText("");
                    Toast.makeText(this, "Категория добавлена", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadCategories() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categories.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    categories.add(ds.getValue(String.class));
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String cat = categories.get(position);
            holder.text.setText(cat);
            holder.itemView.setOnLongClickListener(v -> {
                mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            if (cat.equals(ds.getValue(String.class))) {
                                ds.getRef().removeValue();
                                break;
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
                return true;
            });
        }

        @Override public int getItemCount() { return categories.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(View v) { super(v); text = v.findViewById(android.R.id.text1); }
        }
    }
}
