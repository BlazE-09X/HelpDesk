package firstapp.helpdesk.user;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import firstapp.helpdesk.R;
import java.util.HashMap;
import java.util.Map;

public class EditRequestActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etComment;
    private Spinner spinnerCategory, spinnerPriority;
    private TextView tvNumber;
    private String requestId;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_request);

        // Получаем ID и номер заявки из Intent
        requestId = getIntent().getStringExtra("requestId");
        String requestNumber = getIntent().getStringExtra("requestNumber");

        mDatabase = FirebaseDatabase.getInstance().getReference("Requests").child(requestId);

        etTitle = findViewById(R.id.et_edit_title);
        etDescription = findViewById(R.id.et_edit_description);
        etComment = findViewById(R.id.et_edit_comment);
        spinnerCategory = findViewById(R.id.spinner_edit_category);
        spinnerPriority = findViewById(R.id.spinner_edit_priority);
        tvNumber = findViewById(R.id.tv_edit_request_number);
        Button btnSave = findViewById(R.id.btn_save_changes);

        // Установка номера заявки в заголовке (#005)
        if (requestNumber != null) {
            tvNumber.setText(requestNumber);
        } else {
            tvNumber.setText("#---");
        }

        setupSpinners();
        loadRequestData();

        btnSave.setOnClickListener(v -> updateRequest());
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(this, R.array.categories_array, android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<CharSequence> prioAdapter = ArrayAdapter.createFromResource(this, R.array.priority_array, android.R.layout.simple_spinner_item);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(prioAdapter);
    }

    private void loadRequestData() {
        mDatabase.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                RequestModel model = snapshot.getValue(RequestModel.class);
                if (model != null) {
                    etTitle.setText(model.getTitle());
                    etDescription.setText(model.getDescription());

                    // Загружаем комментарий, если он есть
                    if (snapshot.hasChild("userComment")) {
                        etComment.setText(snapshot.child("userComment").getValue(String.class));
                    }

                    // Установка значений спиннеров
                    setSpinnerValue(spinnerCategory, model.getCategory());
                    setSpinnerValue(spinnerPriority, model.getPriority()); // Было getStatus()
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) {
            spinner.setSelection(position);
        }
    }

    private void updateRequest() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Тема и описание не могут быть пустыми", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", desc);
        updates.put("category", spinnerCategory.getSelectedItem().toString());
        updates.put("priority", spinnerPriority.getSelectedItem().toString());
        updates.put("userComment", etComment.getText().toString().trim());

        mDatabase.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}