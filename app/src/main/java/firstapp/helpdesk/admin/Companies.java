package firstapp.helpdesk.admin;

import android.content.Intent;
import android.os.Bundle;
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

public class Companies extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference mDatabase;
    private FirebaseRecyclerAdapter<CompanyModel, CompanyViewHolder> adapter;
    private boolean isDeleteMode = false;
    private TextView tvCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_companies);

        mDatabase = FirebaseDatabase.getInstance().getReference("companies");

        tvCancel = findViewById(R.id.tv_cancel);
        recyclerView = findViewById(R.id.rv_companies);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 1. Настройка опций для адаптера
        FirebaseRecyclerOptions<CompanyModel> options =
                new FirebaseRecyclerOptions.Builder<CompanyModel>()
                        .setQuery(mDatabase, CompanyModel.class)
                        .build();

        // 2. Инициализация адаптера (делаем один раз)
        adapter = new FirebaseRecyclerAdapter<CompanyModel, CompanyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CompanyViewHolder holder, int position, @NonNull CompanyModel model) {
                holder.name.setText(model.getName());
                holder.domain.setText(model.getDomain());

                // Управление видимостью красной корзины
                holder.btnDelete.setVisibility(isDeleteMode ? View.VISIBLE : View.GONE);

                // Логика удаления
                holder.btnDelete.setOnClickListener(v -> {
                    int currentPosition = holder.getBindingAdapterPosition();
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        String id = getRef(currentPosition).getKey();
                        if (id != null) {
                            mDatabase.child(id).removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(Companies.this, "Удалено", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
            }

            @NonNull
            @Override
            public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company, parent, false);
                return new CompanyViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);

        // Кнопка добавления (+)
        ImageView btnAdd = findViewById(R.id.btn_add_company);
        btnAdd.setOnClickListener(v -> startActivity(new Intent(Companies.this, CreateCompany.class)));

        // Кнопка входа в режим удаления
        ImageView btnBin = findViewById(R.id.btn_delete_company);
        btnBin.setOnClickListener(v -> {
            isDeleteMode = true;
            tvCancel.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        });

        // Кнопка выхода из режима (Отмена)
        // Кнопка выхода из режима или закрытия экрана
        tvCancel.setOnClickListener(v -> {
            if (isDeleteMode) {
                // 1. Если включен режим удаления — просто выключаем его
                isDeleteMode = false;
                tvCancel.setVisibility(View.GONE); // Скрываем саму кнопку
                adapter.notifyDataSetChanged();    // Обновляем список, чтобы спрятать корзины
            } else {
                // 2. Если режим удаления и так выключен — закрываем активность
                finish();
            }
        });

        // Навигация
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_companies);
        bottomNav.setSelectedItemId(R.id.nav_tasks);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, AdminMain.class));
                // finish();
                return true;
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(this, AdminUsers.class));
                // finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, AdminProfile.class));
                return true;
            }
            return false;
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

    // Класс ViewHolder описываем один раз в конце
    public static class CompanyViewHolder extends RecyclerView.ViewHolder {
        TextView name, domain;
        ImageView btnDelete;

        public CompanyViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_company_name);
            domain = itemView.findViewById(R.id.tv_company_domain);
            btnDelete = itemView.findViewById(R.id.iv_delete_item);
        }
    }
}