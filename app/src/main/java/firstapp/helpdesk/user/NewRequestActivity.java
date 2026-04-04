package firstapp.helpdesk.user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import firstapp.helpdesk.R;
import java.util.HashMap;
import java.util.Map;

public class NewRequestActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private Spinner spinnerCategory, spinnerPriority;
    private TextView tvAttachmentStatus;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private String currentUid;
    
    private Uri imageUri, videoUri;
    
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    updateAttachmentStatus();
                }
            }
    );

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    videoUri = uri;
                    updateAttachmentStatus();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_new_request);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUid = user.getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("Requests");
        mStorage = FirebaseStorage.getInstance().getReference("Attachments");

        etTitle = findViewById(R.id.et_request_title);
        etDescription = findViewById(R.id.et_request_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerPriority = findViewById(R.id.spinner_priority);
        tvAttachmentStatus = findViewById(R.id.tv_attachment_status);
        
        Button btnAttachImage = findViewById(R.id.btn_attach_image);
        Button btnAttachVideo = findViewById(R.id.btn_attach_video);
        Button btnCreate = findViewById(R.id.btn_create_request);
        TextView tvBack = findViewById(R.id.tv_back);

        setupSpinners();

        btnAttachImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnAttachVideo.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));

        btnCreate.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String priority = spinnerPriority.getSelectedItem().toString();

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadFilesAndSaveRequest(title, desc, category, priority);
        });

        if (tvBack != null) {
            tvBack.setOnClickListener(v -> finish());
        }
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(this,
                R.array.categories_array, android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<CharSequence> prioAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        prioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(prioAdapter);
    }

    private void updateAttachmentStatus() {
        StringBuilder status = new StringBuilder("Выбрано: ");
        if (imageUri != null) status.append("Фото ");
        if (videoUri != null) status.append("Видео");
        if (imageUri == null && videoUri == null) status.append("ничего");
        tvAttachmentStatus.setText(status.toString());
    }

    private void uploadFilesAndSaveRequest(String title, String desc, String category, String priority) {
        final String requestId = mDatabase.push().getKey();
        if (requestId == null) return;

        Toast.makeText(this, "Создание заявки...", Toast.LENGTH_SHORT).show();

        if (imageUri != null && videoUri != null) {
            uploadImage(requestId, imageUri, imgUrl -> {
                uploadVideo(requestId, videoUri, vidUrl -> {
                    saveToDatabase(requestId, title, desc, category, priority, imgUrl, vidUrl);
                });
            });
        } else if (imageUri != null) {
            uploadImage(requestId, imageUri, imgUrl -> {
                saveToDatabase(requestId, title, desc, category, priority, imgUrl, null);
            });
        } else if (videoUri != null) {
            uploadVideo(requestId, videoUri, vidUrl -> {
                saveToDatabase(requestId, title, desc, category, priority, null, vidUrl);
            });
        } else {
            saveToDatabase(requestId, title, desc, category, priority, null, null);
        }
    }

    private void uploadImage(String requestId, Uri uri, OnUploadCompleteListener listener) {
        StorageReference ref = mStorage.child(requestId + "_img.jpg");
        ref.putFile(uri).addOnSuccessListener(taskSnapshot -> 
            ref.getDownloadUrl().addOnSuccessListener(uri1 -> listener.onComplete(uri1.toString()))
        ).addOnFailureListener(e -> {
            Toast.makeText(this, "Ошибка фото: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            listener.onComplete(null);
        });
    }

    private void uploadVideo(String requestId, Uri uri, OnUploadCompleteListener listener) {
        StorageReference ref = mStorage.child(requestId + "_vid.mp4");
        ref.putFile(uri).addOnSuccessListener(taskSnapshot -> 
            ref.getDownloadUrl().addOnSuccessListener(uri1 -> listener.onComplete(uri1.toString()))
        ).addOnFailureListener(e -> {
            Toast.makeText(this, "Ошибка видео: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            listener.onComplete(null);
        });
    }

    private void saveToDatabase(String requestId, String title, String desc, String category, String priority, String imgUrl, String vidUrl) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("requestId", requestId);
        requestData.put("userId", currentUid);
        requestData.put("title", title);
        requestData.put("description", desc);
        requestData.put("category", category);
        requestData.put("priority", priority);
        requestData.put("status", "Новая");
        requestData.put("imageUrl", imgUrl);
        requestData.put("videoUrl", vidUrl);
        requestData.put("timestamp", System.currentTimeMillis());

        mDatabase.child(requestId).setValue(requestData).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Заявка успешно создана!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Ошибка БД: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    interface OnUploadCompleteListener {
        void onComplete(String downloadUrl);
    }
}