package firstapp.helpdesk;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CreateTicketActivity extends AppCompatActivity {

    private EditText etSubject, etDescription;
    private Spinner spinnerCategory, spinnerPriority;
    private Button btnCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_ticket_activity);

        // Инициализация элементов
        etSubject = findViewById(R.id.etSubject);
        etDescription = findViewById(R.id.etDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        btnCreate = findViewById(R.id.btnCreate);

        // Наполняем категории (из твоего Frame 1)
        String[] categories = {"Сантехника", "Тех Поддержка", "Лифт", "Электрика"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(catAdapter);

        // Наполняем приоритеты (из твоего Frame 2)
        String[] priorities = {"Высокая", "Средняя", "Низкая"};
        ArrayAdapter<String> prioAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, priorities);
        spinnerPriority.setAdapter(prioAdapter);

        // Пока просто выводим сообщение при нажатии
        btnCreate.setOnClickListener(v -> {
            String theme = etSubject.getText().toString();
            if (theme.isEmpty()) {
                Toast.makeText(this, "Введите тему!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Готово! Скоро подключим Firebase", Toast.LENGTH_LONG).show();
            }
        });
    }
}