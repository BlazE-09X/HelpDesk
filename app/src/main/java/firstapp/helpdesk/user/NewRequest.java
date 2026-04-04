package firstapp.helpdesk.user;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

public class NewRequest extends AppCompatActivity {
    // Внутри NewRequestActivity
    private Uri fileUri;
    private String uploadedUrl = "";
    private String fileType = ""; // "image" или "video"

    // 1. Сохранение черновика
    @Override
    protected void onPause() {super.onPause();
        getSharedPreferences("Drafts", MODE_PRIVATE).edit()
                .putString("title", etTitle.getText().toString())
                .putString("desc", etDescription.getText().toString())
                .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("Drafts", MODE_PRIVATE);
        etTitle.setText(prefs.getString("title", ""));
        etDescription.setText(prefs.getString("desc", ""));
    }

    // 2. Выбор медиа
    private void selectMedia() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        startActivityForResult(intent, 101);
    }

    // 3. Загрузка в Firebase Storage (требуется зависимость firebase-storage)
    private void uploadFile(Uri uri) {
        com.google.firebase.storage.StorageReference ref = com.google.firebase.storage.FirebaseStorage.getInstance().getReference("uploads/" + System.currentTimeMillis());
        ref.putFile(uri).addOnSuccessListener(task -> {
            ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                uploadedUrl = downloadUri.toString();
                fileType = uri.toString().contains("video") ? "video" : "image";
                Toast.makeText(this, "Файл загружен", Toast.LENGTH_SHORT).show();
            });
        });
    }
}
