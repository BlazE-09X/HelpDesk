package firstapp.helpdesk.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import firstapp.helpdesk.R;
import firstapp.helpdesk.user.RequestModel;

public class AdminRequestDetailActivity extends AppCompatActivity {

    private TextView tvJK, tvNumber, tvDate, tvCreator, tvTitle, tvExecutor, tvStatus;
    private ImageView ivImage;
    private Button btnOpenVideo, btnBack;
    private String requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_request_detail);

        requestId = getIntent().getStringExtra("requestId");
        String formattedNum = getIntent().getStringExtra("formattedNum");

        initViews();
        if (formattedNum != null) tvNumber.setText("Заявка " + formattedNum);

        if (requestId != null) {
            loadRequestData();
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvJK = findViewById(R.id.tv_detail_jk_name);
        tvNumber = findViewById(R.id.tv_detail_number);
        tvDate = findViewById(R.id.tv_detail_date);
        tvCreator = findViewById(R.id.tv_detail_creator_name);
        tvTitle = findViewById(R.id.tv_detail_title);
        tvExecutor = findViewById(R.id.tv_detail_executor_name);
        tvStatus = findViewById(R.id.tv_detail_status);
        ivImage = findViewById(R.id.iv_detail_image);
        btnOpenVideo = findViewById(R.id.btn_detail_open_video);
        btnBack = findViewById(R.id.btn_back);
    }

    private void loadRequestData() {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Requests").child(requestId);
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                RequestModel model = snapshot.getValue(RequestModel.class);
                if (model != null) {
                    tvTitle.setText(model.getTitle());
                    tvStatus.setText(model.getStatus());
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    tvDate.setText(sdf.format(new Date(model.getTimestamp())));

                    // Загружаем данные о создателе (жителе)
                    if (model.getUserId() != null) {
                        FirebaseDatabase.getInstance().getReference("Residents").child(model.getUserId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot s) {
                                    UserModel user = s.getValue(UserModel.class);
                                    if (user != null) {
                                        String fio = (user.getSurname() + " " + user.getName() + " " + (user.getPatronymic() != null ? user.getPatronymic() : "")).trim();
                                        tvCreator.setText(fio);
                                        tvJK.setText("ЖК: " + (user.getCompanyName() != null ? user.getCompanyName() : "Не указан"));
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                    }

                    // Загружаем имя исполнителя
                    if (model.getExecutorId() != null) {
                        FirebaseDatabase.getInstance().getReference("Workers").child(model.getExecutorId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot s) {
                                    UserModel worker = s.getValue(UserModel.class);
                                    if (worker != null) {
                                        tvExecutor.setText(worker.getSurname() + " " + worker.getName());
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                    }

                    if (model.getImageUrl() != null && !model.getImageUrl().isEmpty()) {
                        ivImage.setVisibility(View.VISIBLE);
                        Glide.with(AdminRequestDetailActivity.this).load(model.getImageUrl()).into(ivImage);
                    }

                    if (model.getVideoUrl() != null && !model.getVideoUrl().isEmpty()) {
                        btnOpenVideo.setVisibility(View.VISIBLE);
                        btnOpenVideo.setOnClickListener(v -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.getVideoUrl()));
                            startActivity(intent);
                        });
                    }
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
