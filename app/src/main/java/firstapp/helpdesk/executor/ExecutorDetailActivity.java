package firstapp.helpdesk.executor;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import firstapp.helpdesk.R;
// Убираем импорт старой модели и используем новую
// import firstapp.helpdesk.user.RequestModel;

public class ExecutorDetailActivity extends AppCompatActivity {

    private EditText etTitle, etCategory, etPriority, etDesc, etComment;
    private TextView tvNumber;
    private Spinner spinnerStatus;
    private String requestId;
    private DatabaseReference requestRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Убедись, что файл разметки называется именно так
        setContentView(R.layout.executor_detail);

        requestId = getIntent().getStringExtra("requestId");
        if (requestId == null) {
            Toast.makeText(this, "Ошибка: ID заявки не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        requestRef = FirebaseDatabase.getInstance().getReference("Requests").child(requestId);

        initViews();
        loadRequestData();

        findViewById(R.id.btn_detail_save).setOnClickListener(v -> saveStatusUpdate());
        findViewById(R.id.tv_detail_back).setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvNumber = findViewById(R.id.tv_detail_number);
        etTitle = findViewById(R.id.et_detail_title);
        etCategory = findViewById(R.id.et_detail_category);
        etPriority = findViewById(R.id.et_detail_priority);
        etDesc = findViewById(R.id.et_detail_description);
        etComment = findViewById(R.id.et_detail_executor_comment);
        spinnerStatus = findViewById(R.id.spinner_detail_status);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array_executor, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void loadRequestData() {
        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Используем ExecuterRequestModel
                    ExecuterRequestModel model = snapshot.getValue(ExecuterRequestModel.class);
                    if (model != null) {
                        etTitle.setText(model.getTitle());
                        etCategory.setText(model.getCategory());
                        etPriority.setText(model.getPriority());
                        etDesc.setText(model.getDescription());


                        etComment.setText(model.getExecutorComment());

                        String currentStatus = model.getStatus();
                        if (currentStatus != null) {
                            ArrayAdapter adapter = (ArrayAdapter) spinnerStatus.getAdapter();
                            int position = adapter.getPosition(currentStatus);
                            if (position >= 0) {
                                spinnerStatus.setSelection(position);
                            }
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveStatusUpdate() {
        String newStatus = spinnerStatus.getSelectedItem().toString();
        String comment = etComment.getText().toString();

        // Обновляем поля в Firebase
        requestRef.child("status").setValue(newStatus);
        requestRef.child("executorComment").setValue(comment)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ExecutorDetailActivity.this, "Данные сохранены!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ExecutorDetailActivity.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                });
    }
}