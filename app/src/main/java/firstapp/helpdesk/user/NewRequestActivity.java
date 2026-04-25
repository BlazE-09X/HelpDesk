package firstapp.helpdesk.user;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import firstapp.helpdesk.R;
import firstapp.helpdesk.admin.UserModel;

public class NewRequestActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private Spinner spinnerCategory, spinnerPriority, spinnerWorker;
    private TextView tvAttachmentStatus, tvSelectedDate;
    private ImageView ivImagePreview;
    private VideoView vvVideoPreview;
    private Button btnRemoveImage, btnRemoveVideo;
    private RadioGroup rgExecutionType;
    private LinearLayout llDatePicker;
    
    private DatabaseReference mDatabase;
    private String currentUid, userJKId;
    private Uri imageUri, videoUri;
    
    private Calendar selectedCalendar = Calendar.getInstance();
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    private List<UserModel> workerList = new ArrayList<>();
    private List<String> workerNames = new ArrayList<>();
    private List<String> servingOrgIds = new ArrayList<>();

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) { imageUri = uri; updateAttachmentStatus(); renderLocalImagePreview(); } }
    );

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) { videoUri = uri; updateAttachmentStatus(); renderVideoPreview(uri); } }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_new_request);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        initViews();
        setupSpinners();
        loadUserJKInfo();

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!servingOrgIds.isEmpty()) loadFilteredWorkers();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        rgExecutionType.setOnCheckedChangeListener((group, checkedId) -> llDatePicker.setVisibility(checkedId == R.id.rb_immediate ? View.GONE : View.VISIBLE));
        findViewById(R.id.btn_pick_date).setOnClickListener(v -> showDateTimePicker());
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_request_title);
        etDescription = findViewById(R.id.et_request_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerPriority = findViewById(R.id.spinner_priority);
        spinnerWorker = findViewById(R.id.spinner_worker);
        tvAttachmentStatus = findViewById(R.id.tv_attachment_status);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        ivImagePreview = findViewById(R.id.iv_request_image_preview);
        vvVideoPreview = findViewById(R.id.vv_request_video_preview);
        rgExecutionType = findViewById(R.id.rg_execution_type);
        llDatePicker = findViewById(R.id.ll_date_picker);
        btnRemoveImage = findViewById(R.id.btn_remove_image);
        btnRemoveVideo = findViewById(R.id.btn_remove_video);

        findViewById(R.id.btn_attach_image).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.btn_attach_video).setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        findViewById(R.id.btn_create_request).setOnClickListener(v -> validateAndCreate());
        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
    }

    private void loadUserJKInfo() {
        mDatabase.child("Residents").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userJKId = snapshot.child("companyId").getValue(String.class);
                    loadServingOrganizations(userJKId);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadServingOrganizations(String jkId) {
        mDatabase.child("JK_Services").child(jkId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                servingOrgIds.clear();
                for (DataSnapshot ds : snapshot.getChildren()) servingOrgIds.add(ds.getKey());
                loadFilteredWorkers();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadFilteredWorkers() {
        if (spinnerCategory.getSelectedItem() == null) return;
        String selectedCategory = spinnerCategory.getSelectedItem().toString();
        mDatabase.child("Workers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workerList.clear();
                workerNames.clear();
                workerNames.add("Выберите специалиста...");
                for (DataSnapshot ds : snapshot.getChildren()) {
                    UserModel w = ds.getValue(UserModel.class);
                    if (w != null && servingOrgIds.contains(w.getCompanyId()) && selectedCategory.equalsIgnoreCase(w.getSpecialization())) {
                        workerList.add(w);
                        workerNames.add(w.getSurname() + " " + w.getName());
                    }
                }
                updateWorkerSpinner();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateWorkerSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, workerNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWorker.setAdapter(adapter);
    }

    private void validateAndCreate() {
        if (etTitle.getText().toString().isEmpty() || spinnerWorker.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Заполните тему и выберите мастера", Toast.LENGTH_SHORT).show();
            return;
        }
        uploadFilesAndSaveRequest();
    }

    private void uploadFilesAndSaveRequest() {
        String requestId = mDatabase.child("Requests").push().getKey();
        saveToFirebase(requestId, null, null);
    }

    private void saveToFirebase(String requestId, String imgUrl, String vidUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("userId", currentUid);
        data.put("executorId", workerList.get(spinnerWorker.getSelectedItemPosition()-1).getUid());
        data.put("status", "Новая");
        data.put("title", etTitle.getText().toString());
        data.put("description", etDescription.getText().toString());
        data.put("category", spinnerCategory.getSelectedItem().toString());
        data.put("priority", spinnerPriority.getSelectedItem().toString());
        data.put("timestamp", System.currentTimeMillis());
        data.put("executionType", rgExecutionType.getCheckedRadioButtonId() == R.id.rb_immediate ? "immediate" : "deadline");
        if (selectedCalendar.getTimeInMillis() > System.currentTimeMillis()) {
            data.put("deadlineDate", selectedCalendar.getTimeInMillis());
        }
        
        mDatabase.child("Requests").child(requestId).setValue(data).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Заявка создана", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupSpinners() {
        // Загружаем категории из БД вместо ресурсов
        mDatabase.child("Categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> categories = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String cat = ds.getValue(String.class);
                    if (cat != null) categories.add(cat);
                }
                if (categories.isEmpty()) {
                    categories.add("Отопление"); // Дефолт
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(NewRequestActivity.this, android.R.layout.simple_spinner_item, categories);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);
                loadFilteredWorkers();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        ArrayAdapter<CharSequence> prioAdapter = ArrayAdapter.createFromResource(this, R.array.priority_array, android.R.layout.simple_spinner_item);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(prioAdapter);
    }

    private void updateAttachmentStatus() { tvAttachmentStatus.setText(imageUri != null || videoUri != null ? "Файлы прикреплены" : "Нет файлов"); }
    private void renderLocalImagePreview() { ivImagePreview.setImageURI(imageUri); ivImagePreview.setVisibility(View.VISIBLE); }
    private void renderVideoPreview(Uri uri) { vvVideoPreview.setVideoURI(uri); vvVideoPreview.setVisibility(View.VISIBLE); }
    private void showDateTimePicker() { /* Код выбора даты */ }
}
