package firstapp.helpdesk.user;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "REQUEST";
    private EditText etTitle, etDescription;
    private Spinner spinnerCategory, spinnerPriority, spinnerWorker;
    private TextView tvAttachmentStatus, tvSelectedDate;
    private ImageView ivImagePreview;
    private VideoView vvVideoPreview;
    private Button btnRemoveImage, btnRemoveVideo;
    private RadioGroup rgExecutionType;
    private LinearLayout llDatePicker;
    
    private DatabaseReference mDatabase;
    private String currentUid;
    private String userCompanyId;
    private Uri imageUri, videoUri;
    private SharedPreferences draftPrefs;
    
    private Calendar selectedCalendar = Calendar.getInstance();
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    private List<UserModel> workerList = new ArrayList<>();
    private List<String> workerNames = new ArrayList<>();
    
    private ValueEventListener workersListener;
    private DatabaseReference workersRef;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    updateAttachmentStatus();
                    renderLocalImagePreview();
                }
            }
    );

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    videoUri = uri;
                    updateAttachmentStatus();
                    renderVideoPreview(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_new_request);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        currentUid = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        draftPrefs = getSharedPreferences("RequestDrafts", Context.MODE_PRIVATE);

        initViews();
        setupSpinners();
        setupVideoView();
        loadUserDataAndWorkers();
        loadDraft();

        rgExecutionType.setOnCheckedChangeListener((group, checkedId) -> {
            llDatePicker.setVisibility(checkedId == R.id.rb_immediate ? View.GONE : View.VISIBLE);
        });

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

        findViewById(R.id.btn_attach_image).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.btn_attach_video).setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        
        btnRemoveImage = findViewById(R.id.btn_remove_image);
        btnRemoveVideo = findViewById(R.id.btn_remove_video);
        
        btnRemoveImage.setOnClickListener(v -> { imageUri = null; ivImagePreview.setVisibility(View.GONE); updateAttachmentStatus(); });
        btnRemoveVideo.setOnClickListener(v -> { videoUri = null; vvVideoPreview.setVisibility(View.GONE); updateAttachmentStatus(); });

        findViewById(R.id.btn_create_request).setOnClickListener(v -> validateAndCreate());
        findViewById(R.id.tv_back).setOnClickListener(v -> finish());
    }

    private void loadUserDataAndWorkers() {
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user != null && user.getCompanyId() != null) {
                    userCompanyId = user.getCompanyId();
                    loadWorkersForCompany(userCompanyId);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadWorkersForCompany(String companyId) {
        workersRef = mDatabase.child("users");
        workersListener = workersRef.orderByChild("companyId").equalTo(companyId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workerList.clear();
                workerNames.clear();
                workerNames.add("Выберите исполнителя...");
                for (DataSnapshot ds : snapshot.getChildren()) {
                    UserModel worker = ds.getValue(UserModel.class);
                    if (worker != null && "Исполнитель".equalsIgnoreCase(worker.getRole())) {
                        workerList.add(worker);
                        workerNames.add(worker.getName() + " (" + worker.getSpecialization() + ")");
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(NewRequestActivity.this, android.R.layout.simple_spinner_item, workerNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerWorker.setAdapter(adapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showDateTimePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedCalendar.set(Calendar.YEAR, year);
            selectedCalendar.set(Calendar.MONTH, month);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedCalendar.set(Calendar.MINUTE, minute);
                tvSelectedDate.setText(dateTimeFormat.format(selectedCalendar.getTime()));
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show();
        }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void validateAndCreate() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        int workerPos = spinnerWorker.getSelectedItemPosition();

        if (title.isEmpty() || desc.isEmpty() || workerPos == 0) {
            Toast.makeText(this, "Заполните все поля и выберите исполнителя", Toast.LENGTH_SHORT).show();
            return;
        }

        String workerId = workerList.get(workerPos - 1).getUid();
        uploadFilesAndSaveRequest(title, desc, workerId);
    }

    private void uploadFilesAndSaveRequest(String title, String desc, String workerId) {
        final String requestId = mDatabase.child("Requests").push().getKey();
        if (requestId == null) return;

        Toast.makeText(this, "Загрузка...", Toast.LENGTH_SHORT).show();

        if (imageUri != null) {
            uploadToCloudinary(imageUri, imgUrl -> {
                if (videoUri != null) uploadToCloudinary(videoUri, vidUrl -> saveToFirebase(requestId, title, desc, workerId, imgUrl, vidUrl));
                else saveToFirebase(requestId, title, desc, workerId, imgUrl, null);
            });
        } else if (videoUri != null) {
            uploadToCloudinary(videoUri, vidUrl -> saveToFirebase(requestId, title, desc, workerId, null, vidUrl));
        } else {
            saveToFirebase(requestId, title, desc, workerId, null, null);
        }
    }

    private void saveToFirebase(String requestId, String title, String desc, String workerId, String imgUrl, String vidUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("userId", currentUid);
        data.put("executorId", workerId);
        data.put("companyId", userCompanyId);
        data.put("title", title);
        data.put("description", desc);
        data.put("category", spinnerCategory.getSelectedItem().toString());
        data.put("priority", spinnerPriority.getSelectedItem().toString());
        data.put("status", "Новая");
        data.put("imageUrl", imgUrl);
        data.put("videoUrl", vidUrl);
        data.put("timestamp", System.currentTimeMillis());

        if (rgExecutionType.getCheckedRadioButtonId() == R.id.rb_planned) data.put("startDate", selectedCalendar.getTimeInMillis());
        else if (rgExecutionType.getCheckedRadioButtonId() == R.id.rb_deadline) data.put("deadlineDate", selectedCalendar.getTimeInMillis());
        data.put("executionType", rgExecutionType.getCheckedRadioButtonId() == R.id.rb_immediate ? "immediate" : (rgExecutionType.getCheckedRadioButtonId() == R.id.rb_planned ? "planned" : "deadline"));

        // Добавлен 'this' для привязки к жизненному циклу
        mDatabase.child("Requests").child(requestId).setValue(data).addOnSuccessListener(this, aVoid -> {
            clearDraft();
            Toast.makeText(this, "Заявка отправлена", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(this, e -> {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadToCloudinary(Uri uri, OnUploadCompleteListener listener) {
        MediaManager.get().upload(uri).callback(new UploadCallback() {
            @Override public void onSuccess(String requestId, Map resultData) { 
                listener.onComplete((String) resultData.get("secure_url")); 
            }
            @Override public void onError(String requestId, ErrorInfo error) { 
                listener.onComplete(null); 
            }
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(this, R.array.categories_array, android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);
        ArrayAdapter<CharSequence> prioAdapter = ArrayAdapter.createFromResource(this, R.array.priority_array, android.R.layout.simple_spinner_item);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(prioAdapter);
    }

    private void setupVideoView() {
        MediaController controller = new MediaController(this);
        controller.setAnchorView(vvVideoPreview);
        vvVideoPreview.setMediaController(controller);
    }

    private void updateAttachmentStatus() {
        String status = "Выбрано: " + (imageUri != null ? "фото " : "") + (videoUri != null ? "видео" : "");
        tvAttachmentStatus.setText(status.trim().isEmpty() ? "Файлы не выбраны" : status);
        btnRemoveImage.setVisibility(imageUri != null ? View.VISIBLE : View.GONE);
        btnRemoveVideo.setVisibility(videoUri != null ? View.VISIBLE : View.GONE);
    }

    private void renderLocalImagePreview() { ivImagePreview.setImageURI(imageUri); ivImagePreview.setVisibility(View.VISIBLE); }
    private void renderVideoPreview(Uri uri) { vvVideoPreview.setVideoURI(uri); vvVideoPreview.setVisibility(View.VISIBLE); }
    private void loadDraft() { etTitle.setText(draftPrefs.getString("title_" + currentUid, "")); etDescription.setText(draftPrefs.getString("desc_" + currentUid, "")); }
    private void clearDraft() { draftPrefs.edit().clear().apply(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Удаляем активный слушатель при уничтожении Activity
        if (workersRef != null && workersListener != null) {
            workersRef.removeEventListener(workersListener);
        }
    }

    interface OnUploadCompleteListener { void onComplete(String url); }
}
