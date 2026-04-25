package firstapp.helpdesk.executor;

import android.content.Intent;
import android.graphics.Color;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import firstapp.helpdesk.R;
import firstapp.helpdesk.admin.UserModel;
import firstapp.helpdesk.chat.ChatActivity;
import firstapp.helpdesk.user.RequestModel;

public class ExecutorDetailActivity extends AppCompatActivity {

    private EditText etTitle, etCategory, etPriority, etDesc, etComment;
    private TextView tvNumber, tvAttachmentStatus, tvJKName, tvUserName, tvDeadline, tvCreatedDate;
    private ImageView ivImagePreview;
    private VideoView vvVideoPreview;
    private Button btnOpenImage, btnOpenVideo, btnSave;
    private Spinner spinnerStatus;
    private RatingBar rbRating;
    private String requestId;
    private DatabaseReference requestRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.executor_detail);

        requestId = getIntent().getStringExtra("requestId");
        String requestNumber = getIntent().getStringExtra("requestNumber");
        
        if (requestId == null) {
            finish();
            return;
        }

        requestRef = FirebaseDatabase.getInstance().getReference("Requests").child(requestId);
        initViews();
        
        if (requestNumber != null) tvNumber.setText(requestNumber);
        
        setupVideoView();
        loadRequestData();

        btnSave.setOnClickListener(v -> saveStatusUpdate());
        findViewById(R.id.tv_detail_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_detail_open_chat).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("requestId", requestId);
            startActivity(intent);
        });
    }

    private void initViews() {
        tvNumber = findViewById(R.id.tv_detail_number);
        tvJKName = findViewById(R.id.tv_detail_jk_name);
        tvUserName = findViewById(R.id.tv_detail_user_name);
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
        btnSave = findViewById(R.id.btn_detail_save);
        spinnerStatus = findViewById(R.id.spinner_detail_status);
        tvDeadline = findViewById(R.id.tv_detail_deadline);
        tvCreatedDate = findViewById(R.id.tv_detail_created_date);
        rbRating = findViewById(R.id.rb_detail_rating);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.status_array_executor, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);
    }

    private void setupVideoView() {
        MediaController controller = new MediaController(this);
        controller.setAnchorView(vvVideoPreview);
        vvVideoPreview.setMediaController(controller);
    }

    private void loadRequestData() {
        requestRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                
                RequestModel model = snapshot.getValue(RequestModel.class);
                if (model == null) return;

                etTitle.setText(model.getTitle());
                etCategory.setText(model.getCategory());
                etPriority.setText(model.getPriority());
                etDesc.setText(model.getDescription());
                etComment.setText(snapshot.child("executorComment").getValue(String.class));
                
                // Отображение рейтинга, если он есть
                if (model.getRating() > 0) {
                    rbRating.setVisibility(View.VISIBLE);
                    rbRating.setRating(model.getRating());
                } else {
                    rbRating.setVisibility(View.GONE);
                }

                // Дата создания
                if (model.getTimestamp() > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    tvCreatedDate.setText("Создана: " + sdf.format(new Date(model.getTimestamp())));
                }

                // Дедлайн
                if ("immediate".equals(model.getExecutionType()) || model.getDeadlineDate() == 0) {
                    tvDeadline.setText("Дедлайн: Немедленно");
                    tvDeadline.setTextColor(Color.RED);
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    tvDeadline.setText("Дедлайн: " + sdf.format(new Date(model.getDeadlineDate())));
                    tvDeadline.setTextColor(Color.GRAY);
                }

                String img = model.getImageUrl();
                String vid = model.getVideoUrl();
                bindAttachments(img, vid);

                String currentStatus = model.getStatus();
                setSpinnerValue(spinnerStatus, currentStatus);

                if ("Выполнено".equalsIgnoreCase(currentStatus)) {
                    spinnerStatus.setEnabled(false);
                    etComment.setEnabled(false);
                    btnSave.setVisibility(View.GONE);
                }

                String userId = model.getUserId();
                if (userId != null) {
                    FirebaseDatabase.getInstance().getReference("Residents").child(userId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot s) {
                                    UserModel u = s.getValue(UserModel.class);
                                    if (u != null) {
                                        tvUserName.setText("От: " + (u.getSurname() + " " + u.getName()).trim());
                                        tvJKName.setText("ЖК: " + (u.getCompanyName() != null ? u.getCompanyName() : "Не указан"));
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void bindAttachments(String imageUrl, String videoUrl) {
        boolean hasImg = imageUrl != null && !imageUrl.isEmpty();
        boolean hasVid = videoUrl != null && !videoUrl.isEmpty();
        tvAttachmentStatus.setText("Вложения: " + (hasImg ? "фото" : "-") + " / " + (hasVid ? "видео" : "-"));
        
        if (hasImg) {
            ivImagePreview.setVisibility(View.VISIBLE);
            if (!isFinishing() && !isDestroyed()) {
                Glide.with(this).load(imageUrl).into(ivImagePreview);
            }
            btnOpenImage.setVisibility(View.VISIBLE);
            btnOpenImage.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl))));
        } else {
            ivImagePreview.setVisibility(View.GONE);
            btnOpenImage.setVisibility(View.GONE);
        }
        
        if (hasVid) {
            btnOpenVideo.setVisibility(View.VISIBLE);
            btnOpenVideo.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))));
        } else {
            btnOpenVideo.setVisibility(View.GONE);
        }
    }

    private void saveStatusUpdate() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", spinnerStatus.getSelectedItem().toString());
        updates.put("executorComment", etComment.getText().toString());
        requestRef.updateChildren(updates).addOnSuccessListener(aVoid -> finish());
    }

    private void setSpinnerValue(Spinner s, String v) {
        if (v == null) return;
        ArrayAdapter adapter = (ArrayAdapter) s.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) if (v.equals(adapter.getItem(i).toString())) { s.setSelection(i); break; }
    }
}
