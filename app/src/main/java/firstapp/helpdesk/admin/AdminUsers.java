package firstapp.helpdesk.admin;

import android.content.Intent;
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

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import firstapp.helpdesk.R;

public class AdminUsers extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<UserModel, UserViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_users);

        mDatabase = FirebaseDatabase.getInstance().getReference("users");
        recyclerView = findViewById(R.id.rv_admin_users);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        EditText etSearch = findViewById(R.id.et_search_users);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Вызываем поиск при каждом вводе символа
                searchUsers(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        FirebaseRecyclerOptions<UserModel> options =
                new FirebaseRecyclerOptions.Builder<UserModel>()
                        .setQuery(mDatabase, UserModel.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<UserModel, UserViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull UserModel model) {
                // Try to get fullname, then name + surname if fullname is null
                String displayName = model.getFullname();
                if (displayName == null || displayName.isEmpty()) {
                    String firstName = model.getName() != null ? model.getName() : "";
                    String lastName = model.getSurname() != null ? model.getSurname() : "";
                    displayName = (firstName + " " + lastName).trim();
                }
                
                if (displayName.isEmpty()) {
                    displayName = model.getEmail();
                }

                holder.name.setText(displayName);
                holder.role.setText(model.getRole());

                holder.btnDelete.setOnClickListener(v -> {
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        String userId = getRef(currentPosition).getKey();
                        if (userId != null) {
                            mDatabase.child(userId).removeValue()
                                    .addOnSuccessListener(unused -> Toast.makeText(AdminUsers.this, "Пользователь удален", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            }

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
                return new UserViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);


        findViewById(R.id.fab_add_worker).setOnClickListener(v -> {
            Intent intent = new Intent(AdminUsers.this, AdminRegisterWorker.class);
            startActivity(intent);
        });

        // Находим новую FAB кнопку
        FloatingActionButton fabAddUser = findViewById(R.id.fab_add_user);
        fabAddUser.setOnClickListener(v -> {
            startActivity(new Intent(AdminUsers.this, AdminRegisterUserActivity.class));
        });

        // Навигация
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_users);
        bottomNav.setSelectedItemId(R.id.nav_users);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminMain.class));
                return true;
            } else if (id == R.id.nav_tasks) {
                startActivity(new Intent(this, Companies.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, AdminProfile.class));
                return true;
            }
            return false;
        });
    }


    private void searchUsers(String searchText) {
        Query query;
        if (searchText.isEmpty()) {
            // Если поиск пустой, показываем всех
            query = mDatabase;
        } else {
            // Поиск по полю "fullname"
            // \uf8ff — это специальный символ Unicode, который помогает Firebase найти все совпадения после текста
            query = mDatabase.orderByChild("fullname")
                    .startAt(searchText)
                    .endAt(searchText + "\uf8ff");
        }

        FirebaseRecyclerOptions<UserModel> options =
                new FirebaseRecyclerOptions.Builder<UserModel>()
                        .setQuery(query, UserModel.class)
                        .build();

        // Обновляем опции в существующем адаптере
        adapter.updateOptions(options);
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

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView name, role;
        ImageView btnDelete;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_user_name);
            role = itemView.findViewById(R.id.tv_user_role);
            btnDelete = itemView.findViewById(R.id.iv_delete_user);
        }
    }
}