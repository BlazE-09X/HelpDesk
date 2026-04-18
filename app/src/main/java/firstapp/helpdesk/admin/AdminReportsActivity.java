package firstapp.helpdesk.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import firstapp.helpdesk.R;
import firstapp.helpdesk.user.RequestModel;

public class AdminReportsActivity extends AppCompatActivity {

    private TextView tvStartDate, tvEndDate, tvReportResult;
    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);

        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        tvReportResult = findViewById(R.id.tv_report_result);
        Button btnGenerate = findViewById(R.id.btn_generate_report);
        Button btnDownload = findViewById(R.id.btn_download_report);

        startCalendar = Calendar.getInstance();
        endCalendar = Calendar.getInstance();

        tvStartDate.setOnClickListener(v -> showDatePicker(startCalendar, tvStartDate));
        tvEndDate.setOnClickListener(v -> showDatePicker(endCalendar, tvEndDate));

        btnGenerate.setOnClickListener(v -> generateReport());
        btnDownload.setOnClickListener(v -> downloadReport());
    }

    private void showDatePicker(Calendar calendar, TextView textView) {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            textView.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void generateReport() {
        long startTs = startCalendar.getTimeInMillis();
        long endTs = endCalendar.getTimeInMillis();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Requests");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int total = 0;
                int completed = 0;
                int active = 0;
                StringBuilder list = new StringBuilder("Список заявок:\n");

                for (DataSnapshot ds : snapshot.getChildren()) {
                    RequestModel model = ds.getValue(RequestModel.class);
                    if (model != null && model.getTimestamp() >= startTs && model.getTimestamp() <= endTs) {
                        total++;
                        String status = model.getStatus() != null ? model.getStatus() : "Новая";
                        if (status.equalsIgnoreCase("Выполнено")) completed++;
                        else active++;
                        
                        list.append("- ").append(model.getTitle()).append(" (").append(status).append(")\n");
                    }
                }

                String result = "Всего: " + total + "\nВыполнено: " + completed + "\nВ процессе: " + active + "\n\n" + list.toString();
                tvReportResult.setText(result);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void downloadReport() {
        String data = tvReportResult.getText().toString();
        if (data.isEmpty()) {
            Toast.makeText(this, "Сначала создайте отчет", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File file = new File(getExternalFilesDir(null), "report_" + System.currentTimeMillis() + ".json");
            FileWriter writer = new FileWriter(file);
            
            JSONObject report = new JSONObject();
            report.put("content", data);
            writer.write(report.toString());
            writer.close();
            
            Toast.makeText(this, "Отчет сохранен: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException | JSONException e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}