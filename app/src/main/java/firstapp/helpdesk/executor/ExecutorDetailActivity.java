package firstapp.helpdesk.executor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import firstapp.helpdesk.R;
import firstapp.helpdesk.chat.ChatActivity;

public class ExecutorDetailActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etCategory;
    private EditText etPriority;
    private EditText etDesc;
    private EditText etComment;
    private TextView tvNumber;
    private TextView tvAttachmentStatus;
    private ImageView ivImagePreview;
    private VideoView vvVideoPreview;
    private Button btnOpenImage;
    private Button btnOpenVideo;
    private Spinner spinnerStatus;
    private String requestId;
    private DatabaseReference requestRef;
    private String currentExecutorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.executor_detail);

        requestId = getIntent().getStringExtra("requestId");
        if (requestId == null) {
            Toast.makeText(this, "Ошибка: ID заявки не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentExecutorId = FirebaseAuth.getInstance().getUid();
        requestRef = FirebaseDatabase.getInstance().getReference("Requests").child(requestId);
        initViews();
        setupVideoView();
        loadRequestData();

        findViewById(R.id.btn_detail_save).setOnClickListener(v -> saveStatusUpdate());
        findViewById(R.id.tv_detail_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_detail_open_chat).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("requestId", requestId);
            startActivity(intent);
        });
    }

    private void initViews() {
        tvNumber = findViewById(R.id.tv_detail_number);
        etTitle = findViewById(R.id.et_detail_title);
        etCategory = findViewById(R.id.et_detail_category);
        etPriority = findViewById(R.id.et_detail_priority);
        etDesc = findViewById(R.id.et_detail_description);
        etComment = findViewById(R.id.et_detail_executor_comment);
        tvAttachmentStatus = findViewById(R.id.tv_detail_attachment_status);
        ivImagePreview = findViewById(R.id.iv_detail_image_preview);
        vvVideoPreview = findViewById(R.id.vv_detail_video_preview);
        btnOpenImage = findViewById(R.id.btn_detail_open_image);
        btnOpenVideo = findViewById(R.id.btn_detail_open_video);
        spinnerStatus = findViewById(R.id.spinner_detail_status);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.status_array_executor, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void setupVideoView() {
        MediaController controller = new MediaController(this);
        controller.setAnchorView(vvVideoPreview);
        vvVideoPreview.setMediaController(controller);
        vvVideoPreview.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            vvVideoPreview.seekTo(100);
        });
    }

    private void loadRequestData() {
        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                ExecuterRequestModel model = snapshot.getValue(ExecuterRequestModel.class);
                if (model == null) return;

                etTitle.setText(model.getTitle());
                etCategory.setText(model.getCategory());
                etPriority.setText(model.getPriority());
                etDesc.setText(model.getDescription());
                etComment.setText(model.getExecutorComment());
                bindAttachments(model.getImageUrl(), model.getVideoUrl());

                String currentStatus = model.getStatus();
                if (currentStatus != null) {
                    ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinnerStatus.getAdapter();
                    for (int i = 0; i < adapter.getCount(); i++) {
                        Object item = adapter.getItem(i);
                        if (currentStatus.equals(String.valueOf(item))) {
                            spinnerStatus.setSelection(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExecutorDetailActivity.this, "Ошибка БД: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindAttachments(String imageUrl, String videoUrl) {
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
        boolean hasVideo = videoUrl != null && !videoUrl.trim().isEmpty();

        tvAttachmentStatus.setText("Вложения: " + (hasImage ? "фото" : "-") + " / " + (hasVideo ? "видео" : "-"));
        btnOpenImage.setVisibility(hasImage ? View.VISIBLE : View.GONE);
        btnOpenVideo.setVisibility(hasVideo ? View.VISIBLE : View.GONE);
        btnOpenImage.setOnClickListener(v -> openAttachment(imageUrl));
        btnOpenVideo.setOnClickListener(v -> openAttachment(videoUrl));

        if (hasImage) {
            Glide.with(this).load(imageUrl).into(ivImagePreview);
            ivImagePreview.setVisibility(View.VISIBLE);
        } else {
            ivImagePreview.setImageDrawable(null);
            ivImagePreview.setVisibility(View.GONE);
        }

        if (hasVideo) {
            vvVideoPreview.setVideoURI(Uri.parse(videoUrl));
            vvVideoPreview.setVisibility(View.VISIBLE);
            vvVideoPreview.seekTo(100);
        } else {
            vvVideoPreview.stopPlayback();
            vvVideoPreview.setVisibility(View.GONE);
        }
    }

    private void openAttachment(String url) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Вложение не найдено", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void saveStatusUpdate() {
        String newStatus = spinnerStatus.getSelectedItem().toString();
        String comment = etComment.getText().toString();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("executorComment", comment);
        updates.put("executorId", currentExecutorId); // Привязываем исполнителя к заявке

        requestRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show());
    }
}