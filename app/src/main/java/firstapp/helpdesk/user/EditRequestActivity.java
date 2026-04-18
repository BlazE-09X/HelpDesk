package firstapp.helpdesk.user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import firstapp.helpdesk.R;
import firstapp.helpdesk.chat.ChatActivity;

public class EditRequestActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private Spinner spinnerCategory, spinnerPriority;
    private TextView tvNumber, tvAttachmentStatus, tvStatus;
    private ImageView ivImagePreview;
    private VideoView vvVideoPreview;
    private Button btnOpenImage, btnOpenVideo, btnRemoveImage, btnRemoveVideo, btnDelete;
    private RatingBar rbRating;
    private View llRatingBlock;

    private String requestId;
    private DatabaseReference mDatabase;
    private String currentUid;
    private RequestModel currentModel;

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
        currentUid = FirebaseAuth.getInstance().getUid();

        if (requestId == null || currentUid == null) {
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference("Requests").child(requestId);

        initViews();
        setupSpinners();
        loadRequestData();

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

        findViewById(R.id.btn_save_changes).setOnClickListener(v -> updateRequest());
        findViewById(R.id.btn_open_chat).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("requestId", requestId);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> deleteRequest());

        rbRating.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                mDatabase.child("rating").setValue(rating);
                Toast.makeText(this, "Оценка сохранена", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_edit_title);
        etDescription = findViewById(R.id.et_edit_description);
        spinnerCategory = findViewById(R.id.spinner_edit_category);
        spinnerPriority = findViewById(R.id.spinner_edit_priority);
        tvNumber = findViewById(R.id.tv_edit_request_number);
        tvStatus = findViewById(R.id.tv_status);
        tvAttachmentStatus = findViewById(R.id.tv_edit_attachment_status);
        ivImagePreview = findViewById(R.id.iv_edit_image_preview);
        vvVideoPreview = findViewById(R.id.vv_edit_video_preview);
        btnOpenImage = findViewById(R.id.btn_edit_open_image);
        btnOpenVideo = findViewById(R.id.btn_edit_open_video);
        btnRemoveImage = findViewById(R.id.btn_edit_remove_image);
        btnRemoveVideo = findViewById(R.id.btn_edit_remove_video);
        btnDelete = findViewById(R.id.btn_delete_request);
        rbRating = findViewById(R.id.rb_request_rating);
        llRatingBlock = findViewById(R.id.ll_rating_block);
    }

    private void loadRequestData() {
        mDatabase.get().addOnSuccessListener(snapshot -> {
            currentModel = snapshot.getValue(RequestModel.class);
            if (currentModel == null) return;

            // Проверка прав доступа
            if (!currentModel.getUserId().equals(currentUid)) {
                Toast.makeText(this, "Нет доступа к чужой заявке", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            etTitle.setText(currentModel.getTitle());
            etDescription.setText(currentModel.getDescription());
            tvStatus.setText(currentModel.getStatus());
            currentImageUrl = currentModel.getImageUrl();
            currentVideoUrl = currentModel.getVideoUrl();
            rbRating.setRating(currentModel.getRating());

            if ("Выполнено".equalsIgnoreCase(currentModel.getStatus())) {
                llRatingBlock.setVisibility(View.VISIBLE);
            }

            setSpinnerValue(spinnerCategory, currentModel.getCategory());
            setSpinnerValue(spinnerPriority, currentModel.getPriority());
            updateAttachmentStatus();
            renderImagePreview();
            renderVideoPreview();
        });
    }

    private void deleteRequest() {
        mDatabase.removeValue().addOnSuccessListener(aVoid -> {
            FirebaseDatabase.getInstance().getReference("Chats").child(requestId).removeValue();
            Toast.makeText(this, "Заявка удалена", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void updateRequest() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        if (title.isEmpty() || desc.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", desc);
        updates.put("category", spinnerCategory.getSelectedItem().toString());
        updates.put("priority", spinnerPriority.getSelectedItem().toString());

        uploadAttachmentsAndSave(updates);
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
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
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
        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(this, R.array.categories_array, android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);
        ArrayAdapter<CharSequence> prioAdapter = ArrayAdapter.createFromResource(this, R.array.priority_array, android.R.layout.simple_spinner_item);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(prioAdapter);
    }

    private void updateAttachmentStatus() {
        boolean hasImg = !imageRemoved && (newImageUri != null || currentImageUrl != null);
        boolean hasVid = !videoRemoved && (newVideoUri != null || currentVideoUrl != null);
        tvAttachmentStatus.setText("Вложения: " + (hasImg ? "фото" : "-") + " / " + (hasVid ? "видео" : "-"));
        btnOpenImage.setVisibility(currentImageUrl != null && !imageRemoved ? View.VISIBLE : View.GONE);
        btnOpenVideo.setVisibility(currentVideoUrl != null && !videoRemoved ? View.VISIBLE : View.GONE);
        btnRemoveImage.setEnabled(hasImg);
        btnRemoveVideo.setEnabled(hasVid);
    }

    private void renderImagePreview() {
        if (newImageUri != null) ivImagePreview.setImageURI(newImageUri);
        else if (currentImageUrl != null && !imageRemoved) Glide.with(this).load(currentImageUrl).into(ivImagePreview);
        ivImagePreview.setVisibility((newImageUri != null || (currentImageUrl != null && !imageRemoved)) ? View.VISIBLE : View.GONE);
    }

    private void renderVideoPreview() {
        Uri uri = newVideoUri != null ? newVideoUri : (currentVideoUrl != null && !videoRemoved ? Uri.parse(currentVideoUrl) : null);
        if (uri != null) { vvVideoPreview.setVideoURI(uri); vvVideoPreview.setVisibility(View.VISIBLE); }
        else { vvVideoPreview.stopPlayback(); vvVideoPreview.setVisibility(View.GONE); }
    }

    private void openAttachment(String url) { if (url != null) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
    private void setSpinnerValue(Spinner s, String v) {
        if (v == null) return;
        ArrayAdapter adapter = (ArrayAdapter) s.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) if (v.equals(adapter.getItem(i).toString())) { s.setSelection(i); break; }
    }
    interface OnUploadCompleteListener { void onComplete(String url); }
}