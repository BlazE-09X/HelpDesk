package firstapp.helpdesk.tickets;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import firstapp.helpdesk.R;
import firstapp.helpdesk.admin.UserModel;
import firstapp.helpdesk.user.RequestModel;

public class CreateTicketActivity extends AppCompatActivity {

    private EditText etSubject, etDescription;
    private Spinner spinnerCategory, spinnerPriority, spinnerExecutor;
    private Button btnCreate, btnRemoveFiles;
    private TextView tvStartDate, tvEndDate;
    private RadioGroup rgDeadlineType;
    private LinearLayout llDatePickers;
    private ImageView ivPreview;
    private VideoView vvPreview;
    
    private DatabaseReference mDatabase;
    private Uri imageUri, videoUri;
    private List<UserModel> workersList = new ArrayList<>();
    private long startTimestamp = 0, endTimestamp = 0;
    private String executionType = "immediate";

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) { imageUri = uri; videoUri = null; updatePreviews(); }
            }
    );

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) { videoUri = uri; imageUri = null; updatePreviews(); }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_create_ticket_activity);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        initViews();
        setupSpinners();
        setupDeadlineLogic();
        loadUserInfoAndWorkers();
    }

    private void initViews() {
        etSubject = findViewById(R.id.etSubject);
        etDescription = findViewById(R.id.etDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerExecutor = findViewById(R.id.spinnerExecutor);
        btnCreate = findViewById(R.id.btnCreate);
        rgDeadlineType = findViewById(R.id.rgDeadlineType);
        llDatePickers = findViewById(R.id.llDatePickers);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        ivPreview = findViewById(R.id.ivPreview);
        vvPreview = findViewById(R.id.vvPreview);
        btnRemoveFiles = findViewById(R.id.btnRemoveFiles);

        findViewById(R.id.btnAttachImage).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.btnAttachVideo).setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        btnRemoveFiles.setOnClickListener(v -> { imageUri = null; videoUri = null; updatePreviews(); });
        btnCreate.setOnClickListener(v -> uploadAndCreate());
    }

    private void updatePreviews() {
        ivPreview.setVisibility(imageUri != null ? View.VISIBLE : View.GONE);
        if (imageUri != null) ivPreview.setImageURI(imageUri);
        
        vvPreview.setVisibility(videoUri != null ? View.VISIBLE : View.GONE);
        if (videoUri != null) { vvPreview.setVideoURI(videoUri); vvPreview.start(); }
        
        btnRemoveFiles.setVisibility((imageUri != null || videoUri != null) ? View.VISIBLE : View.GONE);
    }

    private void setupSpinners() {
        spinnerCategory.setAdapter(ArrayAdapter.createFromResource(this, R.array.categories_array, android.R.layout.simple_spinner_dropdown_item));
        spinnerPriority.setAdapter(ArrayAdapter.createFromResource(this, R.array.priority_array, android.R.layout.simple_spinner_dropdown_item));
    }

    private void setupDeadlineLogic() {
        rgDeadlineType.setOnCheckedChangeListener((group, checkedId) -> {
            executionType = (checkedId == R.id.rbPlanned) ? "planned" : "immediate";
            llDatePickers.setVisibility(checkedId == R.id.rbPlanned ? View.VISIBLE : View.GONE);
        });
        tvStartDate.setOnClickListener(v -> showDatePicker(true));
        tvEndDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isStart) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> {
            c.set(y, m, d);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            if (isStart) { startTimestamp = c.getTimeInMillis(); tvStartDate.setText(sdf.format(c.getTime())); }
            else { endTimestamp = c.getTimeInMillis(); tvEndDate.setText(sdf.format(c.getTime())); }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadUserInfoAndWorkers() {
        String uid = FirebaseAuth.getInstance().getUid();
        mDatabase.child("Residents").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String jkId = snapshot.child("companyId").getValue(String.class);
                    if (jkId != null) loadServingOrgs(jkId);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadServingOrgs(String jkId) {
        mDatabase.child("JK_Services").child(jkId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> orgIds = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) orgIds.add(ds.getKey());
                loadWorkers(orgIds);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadWorkers(Set<String> orgIds) {
        mDatabase.child("Workers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workersList.clear();
                List<String> names = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    UserModel w = ds.getValue(UserModel.class);
                    if (w != null && orgIds.contains(w.getCompanyId())) {
                        w.setId(ds.getKey());
                        workersList.add(w);
                        names.add(w.getSurname() + " " + w.getName());
                    }
                }
                if (names.isEmpty()) names.add("Нет исполнителей");
                spinnerExecutor.setAdapter(new ArrayAdapter<>(CreateTicketActivity.this, android.R.layout.simple_spinner_dropdown_item, names));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void uploadAndCreate() {
        String title = etSubject.getText().toString().trim();
        if (title.isEmpty()) { Toast.makeText(this, "Введите тему", Toast.LENGTH_SHORT).show(); return; }

        btnCreate.setEnabled(false);
        if (imageUri != null) uploadToCloudinary(imageUri, true);
        else if (videoUri != null) uploadToCloudinary(videoUri, false);
        else saveRequest(null, null);
    }

    private void uploadToCloudinary(Uri uri, boolean isImage) {
        MediaManager.get().upload(uri).callback(new UploadCallback() {
            @Override public void onSuccess(String r, Map res) { saveRequest(isImage ? (String)res.get("secure_url") : null, !isImage ? (String)res.get("secure_url") : null); }
            @Override public void onError(String r, ErrorInfo e) { saveRequest(null, null); }
            @Override public void onStart(String r) {}
            @Override public void onProgress(String r, long b, long t) {}
            @Override public void onReschedule(String r, ErrorInfo e) {}
        }).dispatch();
    }

    private void saveRequest(String img, String vid) {
        String id = mDatabase.child("Requests").push().getKey();
        RequestModel r = new RequestModel();
        r.setRequestId(id);
        r.setTitle(etSubject.getText().toString());
        r.setDescription(etDescription.getText().toString());
        r.setCategory(spinnerCategory.getSelectedItem().toString());
        r.setPriority(spinnerPriority.getSelectedItem().toString());
        r.setStatus("Новая");
        r.setUserId(FirebaseAuth.getInstance().getUid());
        r.setTimestamp(System.currentTimeMillis());
        r.setImageUrl(img);
        r.setVideoUrl(vid);
        r.setExecutionType(executionType);
        r.setStartDate(startTimestamp);
        r.setDeadlineDate(endTimestamp);
        
        if (!workersList.isEmpty() && spinnerExecutor.getSelectedItemPosition() != Spinner.INVALID_POSITION) {
            r.setExecutorId(workersList.get(spinnerExecutor.getSelectedItemPosition()).getId());
        }

        mDatabase.child("Requests").child(id).setValue(r).addOnSuccessListener(aVoid -> {
            mDatabase.child("Chats").child(id).child("createdAt").setValue(System.currentTimeMillis());
            Toast.makeText(this, "Создано!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
