package firstapp.helpdesk.user;

import android.content.Context;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import firstapp.helpdesk.R;

public class HistoryLoginActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    // Используем именно HistoryViewHolder, чтобы не было конфликтов с UserMain
    private FirebaseRecyclerAdapter<HistoryModel, HistoryViewHolder> adapter;

    // Кастомный менеджер для предотвращения краша "Inconsistency detected"
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
        setContentView(R.layout.user_history_login);

        recyclerView = findViewById(R.id.rv_login_history);
        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));

        // Кнопка назад
        View backBtn = findViewById(R.id.tv_back_button);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("LoginHistory").child(uid);

        FirebaseRecyclerOptions<HistoryModel> options =
                new FirebaseRecyclerOptions.Builder<HistoryModel>()
                        .setQuery(historyRef, HistoryModel.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<HistoryModel, HistoryViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull HistoryViewHolder holder, int position, @NonNull HistoryModel model) {
                holder.tvType.setText(model.getType());
                holder.tvDate.setText("Дата: " + model.getDate());

                // Установка иконок
                if ("Вход".equals(model.getType())) {
                    holder.ivIcon.setImageResource(R.drawable.monitor);
                } else if ("Изменение пароля".equals(model.getType())) {
                    holder.ivIcon.setImageResource(R.drawable.key);
                } else if ("Изменение почты".equals(model.getType())) {
                    holder.ivIcon.setImageResource(R.drawable.mail);
                }
            }

            @NonNull
            @Override
            public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
                return new HistoryViewHolder(view);
            }
        };

        recyclerView.setAdapter(adapter);
    }

    // Внутренний класс ViewHolder с уникальным именем
    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDate;
        ImageView ivIcon;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tv_history_type);
            tvDate = itemView.findViewById(R.id.tv_history_date);
            ivIcon = itemView.findViewById(R.id.iv_history_icon);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
