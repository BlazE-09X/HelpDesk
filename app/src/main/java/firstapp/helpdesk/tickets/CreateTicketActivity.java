package firstapp.helpdesk.tickets;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import firstapp.helpdesk.R;
import firstapp.helpdesk.admin.UserModel;

public class CreateTicketActivity extends AppCompatActivity {

    private EditText etSubject, etDescription;
    private Spinner spinnerCategory, spinnerPriority, spinnerExecutor;
    private Button btnCreate;
    private DatabaseReference mDatabase;
    
    private List<UserModel> workersList = new ArrayList<>();
    private List<String> workerNames = new ArrayList<>();
    private ArrayAdapter<String> execAdapter;
    
    private String userJkId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_create_ticket_activity);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        etSubject = findViewById(R.id.etSubject);
        etDescription = findViewById(R.id.etDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerExecutor = findViewById(R.id.spinnerExecutor);
        btnCreate = findViewById(R.id.btnCreate);

        setupSpinners();
        loadUserInfoAndWorkers();

        btnCreate.setOnClickListener(v -> createTicket());
    }

    private void setupSpinners() {
        String[] categories = {"Сантехника", "Тех Поддержка", "Лифт", "Электрика"};
        spinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));

        String[] priorities = {"Высокая", "Средняя", "Низкая"};
        spinnerPriority.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities));
    }

    private void loadUserInfoAndWorkers() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // 1. Получаем jkId пользователя
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userJkId = snapshot.child("housingComplexId").getValue(String.class);
                    if (userJkId == null) userJkId = snapshot.child("jkId").getValue(String.class);
                    
                    if (userJkId != null && !userJkId.isEmpty()) {
                        loadLinkedOrganizations(userJkId);
                    } else {
                        loadAllWorkers(); // Если ЖК не привязан, грузим всех (на всякий случай)
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLinkedOrganizations(String jkId) {
        // 2. Ищем организации, обслуживающие этот ЖК
        mDatabase.child("JK_Services").child(jkId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> orgIds = new HashSet<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    orgIds.add(ds.getKey());
                }
                loadWorkersByOrgs(orgIds);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadWorkersByOrgs(Set<String> orgIds) {
        // 3. Грузим исполнителей, которые состоят в этих организациях
        mDatabase.child("Workers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workersList.clear();
                workerNames.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    UserModel worker = ds.getValue(UserModel.class);
                    if (worker != null && orgIds.contains(worker.getCompanyId())) {
                        worker.setId(ds.getKey());
                        workersList.add(worker);
                        String name = (worker.getSurname() != null ? worker.getSurname() : "") + " " +
                                     (worker.getName() != null ? worker.getName() : "");
                        workerNames.add(name.trim().isEmpty() ? worker.getEmail() : name);
                    }
                }
                updateExecutorSpinner();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAllWorkers() {
        mDatabase.child("Workers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                workersList.clear();
                workerNames.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    UserModel worker = ds.getValue(UserModel.class);
                    if (worker != null) {
                        worker.setId(ds.getKey());
                        workersList.add(worker);
                        String name = (worker.getSurname() != null ? worker.getSurname() : "") + " " +
                                     (worker.getName() != null ? worker.getName() : "");
                        workerNames.add(name.trim().isEmpty() ? worker.getEmail() : name);
                    }
                }
                updateExecutorSpinner();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateExecutorSpinner() {
        if (workerNames.isEmpty()) {
            workerNames.add("Нет доступных исполнителей для вашего ЖК");
        }
        execAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, workerNames);
        spinnerExecutor.setAdapter(execAdapter);
    }

    private void createTicket() {
        String title = etSubject.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String priority = spinnerPriority.getSelectedItem().toString();
        
        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Заполните тему и описание", Toast.LENGTH_SHORT).show();
            return;
        }

        String executorId = "";
        String executorName = "Не назначен";
        int pos = spinnerExecutor.getSelectedItemPosition();
        if (pos != Spinner.INVALID_POSITION && !workersList.isEmpty()) {
            UserModel selected = workersList.get(pos);
            executorId = selected.getId();
            executorName = workerNames.get(pos);
        }

        String ticketId = mDatabase.child("tickets").push().getKey();
        Ticket newTicket = new Ticket(ticketId, title, category, description, "Новая", priority, executorId, executorName);
        
        if (ticketId != null) {
            mDatabase.child("tickets").child(ticketId).setValue(newTicket).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Заявка создана", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }
}
