package firstapp.helpdesk.tickets;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import firstapp.helpdesk.R;

public class CreateTicketActivity extends AppCompatActivity {

    private EditText etSubject, etDescription;
    private Spinner spinnerCategory, spinnerPriority;
    private Button btnCreate;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_create_ticket_activity);


        mDatabase = FirebaseDatabase.getInstance().getReference("tickets");

        // Инициализация элементов
        etSubject = findViewById(R.id.etSubject);
        etDescription = findViewById(R.id.etDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        btnCreate = findViewById(R.id.btnCreate);

        // Наполняем категории (из Frame 1)
        String[] categories = {"Сантехника", "Тех Поддержка", "Лифт", "Электрика"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(catAdapter);

        // Наполняем приоритеты (из Frame 2)
        String[] priorities = {"Высокая", "Средняя", "Низкая"};
        ArrayAdapter<String> prioAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, priorities);
        spinnerPriority.setAdapter(prioAdapter);

        // Пока просто выводим сообщение при нажатии
        btnCreate.setOnClickListener(v -> {
            // Достаем текст из полей
            String title = etSubject.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String priority = spinnerPriority.getSelectedItem().toString();

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            // Генерируем уникальный ID для каждой заявки
            String ticketId = mDatabase.push().getKey();

            // Создаем объект заявки
            // (Нужно убедится, что в классе Ticket есть такой конструктор)
            Ticket newTicket = new Ticket(ticketId, title, category, description, "Новая", priority);

            // Отправляем
            if (ticketId != null) {
                mDatabase.child(ticketId).setValue(newTicket)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Заявка отправлена в базу!", Toast.LENGTH_SHORT).show();
                            finish(); // Закрываем экран после успеха
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
}