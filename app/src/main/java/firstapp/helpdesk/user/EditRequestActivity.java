package firstapp.helpdesk.user;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.Date;

import firstapp.helpdesk.R;
import firstapp.helpdesk.admin.UserModel;
import firstapp.helpdesk.chat.ChatActivity;

public class EditRequestActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private Spinner spinnerCategory, spinnerPriority, spinnerExecutor;
    private TextView tvNumber, tvAttachmentStatus, tvStatus, tvDeadline, tvExecutorComment, tvCreatedDate;
    private ImageView ivImagePreview;
    private VideoView vvVideoPreview;
    private Button btnOpenImage, btnOpenVideo, btnRemoveImage, btnRemoveVideo, btnDelete, btnSave, btnChangeDeadline, btnOpenChat;
    private RatingBar rbRating;
    private View llRatingBlock, llExecutorCommentBlock, llDeadlineArea;

    private String requestId, requestNumber;
    private DatabaseReference mDatabase;
    private String currentUid, userJKId;
    private RequestModel currentModel;
    private boolean isCreatingMode = false;

    private List<UserModel> workersList = new ArrayList<>();
    private List<String> workerNames = new ArrayList<>();
    private List<String> servingOrgIds = new ArrayList<>();
    private long selectedDeadline = 0;

    private Uri newImageUri, newVideoUri;
    private String currentImageUrl, currentVideoUrl;
    private boolean imageRemoved, videoRemoved;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    newImageUri = uri;
                    imageRemoved = false;
                    updateAttachmentStatus();
                    renderImagePreview();
                }
            }
    );

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    newVideoUri = uri;
                    videoRemoved = false;
                    updateAttachmentStatus();
                    renderVideoPreview();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_request);

        requestId = getIntent().getStringExtra("requestId");
        requestNumber = getIntent().getStringExtra("requestNumber");
        isCreatingMode = getIntent().getBooleanExtra("isNewRequest", false);
        currentUid = FirebaseAuth.getInstance().getUid();

        if (currentUid == null) {
            finish();
            return;
        }

        initViews();
        setupSpinners();
        
        if (isCreatingMode) {
            prepareCreateMode();
        } else if (requestId != null) {
            mDatabase = FirebaseDatabase.getInstance().getReference("Requests").child(requestId);
            if (requestNumber != null) tvNumber.setText(requestNumber);
            loadRequestData();
        } else {
            finish();
            return;
        }

        findViewById(R.id.btn_edit_attach_image).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.btn_edit_attach_video).setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        btnOpenImage.setOnClickListener(v -> openAttachment(currentImageUrl));
        btnOpenVideo.setOnClickListener(v -> openAttachment(currentVideoUrl));
        
        btnRemoveImage.setOnClickListener(v -> {
            newImageUri = null; currentImageUrl = null; imageRemoved = true;
            updateAttachmentStatus(); renderImagePreview();
        });
        
        btnRemoveVideo.setOnClickListener(v -> {
            newVideoUri = null; currentVideoUrl = null; videoRemoved = true;
            updateAttachmentStatus(); renderVideoPreview();
        });

        btnSave.setOnClickListener(v -> {
            if (isCreatingMode) createRequest();
            else updateRequest();
        });
        
        btnOpenChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("requestId", requestId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> deleteRequest());
        btnChangeDeadline.setOnClickListener(v -> showDatePicker());
        
        findViewById(R.id.tv_edit_back).setOnClickListener(v -> finish());

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isCreatingMode || (currentModel != null && "Новая".equalsIgnoreCase(currentModel.getStatus()))) {
                    loadFilteredWorkers();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void prepareCreateMode() {
        tvNumber.setText("Новая заявка");
        tvStatus.setText("Черновик");
        btnSave.setText("Создать заявку");
        btnDelete.setVisibility(View.GONE);
        btnOpenChat.setVisibility(View.GONE);
        tvCreatedDate.setVisibility(View.GONE);
        setFieldsEnabled(true);
        loadUserJKInfo();
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_edit_title);
        etDescription = findViewById(R.id.et_edit_description);
        spinnerCategory = findViewById(R.id.spinner_edit_category);
        spinnerPriority = findViewById(R.id.spinner_edit_priority);
        spinnerExecutor = findViewById(R.id.spinner_edit_executor);
        tvNumber = findViewById(R.id.tv_edit_request_number);
        tvStatus = findViewById(R.id.tv_status);
        tvDeadline = findViewById(R.id.tv_edit_deadline_display);
        tvAttachmentStatus = findViewById(R.id.tv_edit_attachment_status);
        ivImagePreview = findViewById(R.id.iv_edit_image_preview);
        vvVideoPreview = findViewById(R.id.vv_edit_video_preview);
        btnOpenImage = findViewById(R.id.btn_edit_open_image);
        btnOpenVideo = findViewById(R.id.btn_edit_open_video);
        btnRemoveImage = findViewById(R.id.btn_edit_remove_image);
        btnRemoveVideo = findViewById(R.id.btn_edit_remove_video);
        btnDelete = findViewById(R.id.btn_delete_request);
        btnSave = findViewById(R.id.btn_save_changes);
        btnChangeDeadline = findViewById(R.id.btn_edit_change_deadline);
        btnOpenChat = findViewById(R.id.btn_open_chat);
        rbRating = findViewById(R.id.rb_request_rating);
        llRatingBlock = findViewById(R.id.ll_rating_block);
        tvExecutorComment = findViewById(R.id.tv_executor_comment_display);
        llExecutorCommentBlock = findViewById(R.id.ll_executor_comment_block);
        llDeadlineArea = findViewById(R.id.ll_deadline_area);
        tvCreatedDate = findViewById(R.id.tv_edit_created_date);
    }

    private void loadUserJKInfo() {
        FirebaseDatabase.getInstance().getReference("Residents").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
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
        FirebaseDatabase.getInstance().getReference("JK_Services").child(jkId).addListenerForSingleValueEvent(new ValueEventListener() {
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
        
        FirebaseDatabase.getInstance().getReference("Workers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workersList.clear();
                workerNames.clear();
                int selection = -1;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    UserModel w = ds.getValue(UserModel.class);
                    if (w != null) {
                        w.setId(ds.getKey());
                        boolean isServing = servingOrgIds.isEmpty() || servingOrgIds.contains(w.getCompanyId());
                        if (isServing && selectedCategory.equalsIgnoreCase(w.getSpecialization())) {
                            workersList.add(w);
                            workerNames.add(w.getSurname() + " " + w.getName());
                            if (!isCreatingMode && currentModel != null && ds.getKey().equals(currentModel.getExecutorId())) {
                                selection = workerNames.size() - 1;
                            }
                        }
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditRequestActivity.this, android.R.layout.simple_spinner_dropdown_item, workerNames);
                spinnerExecutor.setAdapter(adapter);
                if (selection != -1) spinnerExecutor.setSelection(selection);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadRequestData() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentModel = snapshot.getValue(RequestModel.class);
                if (currentModel == null) return;

                etTitle.setText(currentModel.getTitle());
                etDescription.setText(currentModel.getDescription());
                tvStatus.setText(currentModel.getStatus());
                currentImageUrl = currentModel.getImageUrl();
                currentVideoUrl = currentModel.getVideoUrl();
                rbRating.setRating(currentModel.getRating());
                selectedDeadline = currentModel.getDeadlineDate();
                
                updateDeadlineText();

                if (currentModel.getTimestamp() > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    tvCreatedDate.setVisibility(View.VISIBLE);
                    tvCreatedDate.setText("Создана: " + sdf.format(new Date(currentModel.getTimestamp())));
                }

                String status = currentModel.getStatus() != null ? currentModel.getStatus() : "";
                boolean isNew = "Новая".equalsIgnoreCase(status);
                boolean isCompleted = "Выполнено".equalsIgnoreCase(status);
                boolean isRejected = "Отклонено".equalsIgnoreCase(status);
                
                setFieldsEnabled(isNew);
                llDeadlineArea.setVisibility(isNew ? View.VISIBLE : View.GONE);

                btnDelete.setVisibility(isNew ? View.VISIBLE : View.GONE);

                if (isCompleted || isRejected) {
                    llExecutorCommentBlock.setVisibility(View.VISIBLE);
                    String comment = snapshot.child("executorComment").getValue(String.class);
                    tvExecutorComment.setText(comment != null ? comment : "Комментарий отсутствует");
                } else {
                    llExecutorCommentBlock.setVisibility(View.GONE);
                }

                if (isCompleted) {
                    llRatingBlock.setVisibility(View.VISIBLE);
                    if (currentModel.getRating() > 0) {
                        rbRating.setIsIndicator(true);
                        btnSave.setVisibility(View.GONE);
                    } else {
                        rbRating.setIsIndicator(false);
                        btnSave.setVisibility(View.VISIBLE);
                        btnSave.setText("Оценить и сохранить");
                    }
                } else {
                    llRatingBlock.setVisibility(View.GONE);
                }

                setSpinnerValue(spinnerCategory, currentModel.getCategory());
                setSpinnerValue(spinnerPriority, currentModel.getPriority());
                loadUserJKInfo();
                
                updateAttachmentStatus();
                renderImagePreview();
                renderVideoPreview();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setFieldsEnabled(boolean enabled) {
        etTitle.setEnabled(enabled);
        etDescription.setEnabled(enabled);
        spinnerCategory.setEnabled(enabled);
        spinnerPriority.setEnabled(enabled);
        spinnerExecutor.setEnabled(enabled);
        btnChangeDeadline.setEnabled(enabled);
    }

    private void createRequest() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        if (title.isEmpty() || desc.isEmpty() || spinnerExecutor.getSelectedItem() == null) {
            Toast.makeText(this, "Заполните тему, описание и выберите мастера", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference requestsRef = FirebaseDatabase.getInstance().getReference("Requests");
        requestId = requestsRef.push().getKey();
        mDatabase = requestsRef.child(requestId);

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("userId", currentUid);
        data.put("executorId", workersList.get(spinnerExecutor.getSelectedItemPosition()).getId());
        data.put("status", "Новая");
        data.put("title", title);
        data.put("description", desc);
        data.put("category", spinnerCategory.getSelectedItem().toString());
        data.put("priority", spinnerPriority.getSelectedItem().toString());
        data.put("timestamp", System.currentTimeMillis());
        data.put("executionType", selectedDeadline > 0 ? "deadline" : "immediate");
        if (selectedDeadline > 0) data.put("deadlineDate", selectedDeadline);

        uploadAttachmentsAndSave(data);
    }

    private void updateRequest() {
        String status = tvStatus.getText().toString();
        Map<String, Object> updates = new HashMap<>();

        if ("Выполнено".equalsIgnoreCase(status)) {
            updates.put("rating", rbRating.getRating());
        } else {
            String title = etTitle.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            if (title.isEmpty() || desc.isEmpty()) return;

            updates.put("title", title);
            updates.put("description", desc);
            updates.put("category", spinnerCategory.getSelectedItem().toString());
            updates.put("priority", spinnerPriority.getSelectedItem().toString());
            updates.put("deadlineDate", selectedDeadline);
            updates.put("executionType", selectedDeadline > 0 ? "deadline" : "immediate");

            if (spinnerExecutor.getSelectedItem() != null) {
                updates.put("executorId", workersList.get(spinnerExecutor.getSelectedItemPosition()).getId());
            }
        }
        uploadAttachmentsAndSave(updates);
    }

    private void deleteRequest() {
        mDatabase.removeValue().addOnSuccessListener(aVoid -> finish());
    }

    private void uploadAttachmentsAndSave(Map<String, Object> updates) {
        if (newImageUri != null) {
            uploadToCloudinary(newImageUri, url -> { updates.put("imageUrl", url); checkVideoAndSave(updates); });
        } else {
            if (imageRemoved) updates.put("imageUrl", null);
            checkVideoAndSave(updates);
        }
    }

    private void checkVideoAndSave(Map<String, Object> updates) {
        if (newVideoUri != null) {
            uploadToCloudinary(newVideoUri, url -> { updates.put("videoUrl", url); save(updates); });
        } else {
            if (videoRemoved) updates.put("videoUrl", null);
            save(updates);
        }
    }

    private void save(Map<String, Object> updates) {
        mDatabase.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, isCreatingMode ? "Заявка создана" : "Данные сохранены", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void uploadToCloudinary(Uri uri, OnUploadCompleteListener l) {
        MediaManager.get().upload(uri).callback(new UploadCallback() {
            @Override public void onSuccess(String requestId, Map resultData) { l.onComplete((String) resultData.get("secure_url")); }
            @Override public void onError(String requestId, ErrorInfo error) { l.onComplete(null); }
            @Override public void onStart(String requestId) {}
            @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
            @Override public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void setupSpinners() {
        FirebaseDatabase.getInstance().getReference("Categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> categories = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String cat = ds.getValue(String.class);
                    if (cat != null) categories.add(cat);
                }
                if (categories.isEmpty()) categories.add("Прочее");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditRequestActivity.this, android.R.layout.simple_spinner_item, categories);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);
                if (currentModel != null) setSpinnerValue(spinnerCategory, currentModel.getCategory());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        ArrayAdapter<CharSequence> prioAdapter = ArrayAdapter.createFromResource(this, R.array.priority_array, android.R.layout.simple_spinner_item);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(prioAdapter);
    }

    private void updateDeadlineText() {
        if (selectedDeadline > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            tvDeadline.setText("Срок: " + sdf.format(selectedDeadline));
            tvDeadline.setTextColor(Color.GRAY);
        } else {
            tvDeadline.setText("Дедлайн: Немедленно");
            tvDeadline.setTextColor(Color.RED);
        }
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDeadline > 0) calendar.setTimeInMillis(selectedDeadline);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            selectedDeadline = calendar.getTimeInMillis();
            updateDeadlineText();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openAttachment(String url) { if (url != null) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
    
    private void setSpinnerValue(Spinner s, String v) {
        if (v == null || s.getAdapter() == null) return;
        for (int i = 0; i < s.getAdapter().getCount(); i++) if (v.equals(s.getAdapter().getItem(i).toString())) { s.setSelection(i); break; }
    }

    private void updateAttachmentStatus() {
        boolean hasImg = !imageRemoved && (newImageUri != null || currentImageUrl != null);
        boolean hasVid = !videoRemoved && (newVideoUri != null || currentVideoUrl != null);
        tvAttachmentStatus.setText("Вложения: " + (hasImg ? "фото" : "-") + " / " + (hasVid ? "видео" : "-"));
        btnOpenImage.setVisibility(currentImageUrl != null && !imageRemoved ? View.VISIBLE : View.GONE);
        btnOpenVideo.setVisibility(currentVideoUrl != null && !videoRemoved ? View.VISIBLE : View.GONE);
    }

    private void renderImagePreview() {
        if (newImageUri != null) ivImagePreview.setImageURI(newImageUri);
        else if (currentImageUrl != null && !imageRemoved) {
            if (!isFinishing() && !isDestroyed()) {
                Glide.with(this).load(currentImageUrl).into(ivImagePreview);
            }
        }
        ivImagePreview.setVisibility((newImageUri != null || (currentImageUrl != null && !imageRemoved)) ? View.VISIBLE : View.GONE);
    }

    private void renderVideoPreview() {
        Uri uri = newVideoUri != null ? newVideoUri : (currentVideoUrl != null && !videoRemoved ? Uri.parse(currentVideoUrl) : null);
        if (uri != null) { vvVideoPreview.setVideoURI(uri); vvPreview(uri); vvVideoPreview.setVisibility(View.VISIBLE); }
        else { vvVideoPreview.stopPlayback(); vvVideoPreview.setVisibility(View.GONE); }
    }
    
    private void vvPreview(Uri uri) {
        MediaController mc = new MediaController(this);
        mc.setAnchorView(vvVideoPreview);
        vvVideoPreview.setMediaController(mc);
        vvVideoPreview.setVideoURI(uri);
        vvVideoPreview.setOnPreparedListener(mp -> vvVideoPreview.seekTo(1));
    }

    interface OnUploadCompleteListener { void onComplete(String url); }
}
