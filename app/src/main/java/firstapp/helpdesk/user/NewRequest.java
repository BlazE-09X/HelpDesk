package firstapp.helpdesk.user;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NewRequest extends AppCompatActivity {
    private EditText etTitle;
    private EditText etDescription;
    private Uri fileUri;
    private String uploadedUrl = "";
    private String fileType = "";

    @Override
    protected void onPause() {
        super.onPause();
        if (etTitle == null || etDescription == null) {
            return;
        }
        getSharedPreferences("Drafts", MODE_PRIVATE).edit()
                .putString("title", etTitle.getText().toString())
                .putString("desc", etDescription.getText().toString())
                .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (etTitle == null || etDescription == null) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences("Drafts", MODE_PRIVATE);
        etTitle.setText(prefs.getString("title", ""));
        etDescription.setText(prefs.getString("desc", ""));
    }

    private void selectMedia() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        startActivityForResult(intent, 101);
    }

    private void uploadFile(Uri uri) {
        fileUri = uri;
        com.google.firebase.storage.StorageReference ref =
                com.google.firebase.storage.FirebaseStorage.getInstance()
                        .getReference("uploads/" + System.currentTimeMillis());
        
        // Добавлен 'this' для автоматической отписки при уничтожении Activity
        ref.putFile(uri).addOnSuccessListener(this, task -> {
            ref.getDownloadUrl().addOnSuccessListener(this, downloadUri -> {
                uploadedUrl = downloadUri.toString();
                fileType = uri.toString().contains("video") ? "video" : "image";
                Toast.makeText(this, "Файл загружен", Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(this, e -> {
            Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
