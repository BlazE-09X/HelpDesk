package firstapp.helpdesk.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

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

import java.util.Map;

import firstapp.helpdesk.R;

public class AdminPhotoActivity extends AppCompatActivity {

    private ImageView ivFullPhoto;
    private ProgressBar progressBar;
    private String currentUid;
    private DatabaseReference mDatabase;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadImage(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_photo);

        ivFullPhoto = findViewById(R.id.iv_full_photo);
        progressBar = findViewById(R.id.pb_upload);
        currentUid = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("users").child(currentUid);

        loadCurrentPhoto();

        findViewById(R.id.btn_change_photo).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadCurrentPhoto() {
        mDatabase.child("profileImage").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String url = snapshot.getValue(String.class);
                if (url != null && !isFinishing()) {
                    Glide.with(AdminPhotoActivity.this).load(url).placeholder(R.drawable.profile).into(ivFullPhoto);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void uploadImage(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        MediaManager.get().upload(uri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        mDatabase.child("profileImage").setValue(url).addOnSuccessListener(aVoid -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AdminPhotoActivity.this, "Фото обновлено", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdminPhotoActivity.this, "Ошибка загрузки: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }
}
