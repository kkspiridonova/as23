package com.example.everydaynik2;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.Calendar;

public class AddEventActivity extends AppCompatActivity {
    private static final String TAG = "AddEventActivity";
    private EditText etTitle, etDescription;
    private Spinner spinnerPriority;
    private TextView priorityColorIndicator; // Для отображения цвета
    private long selectedDateTime = -1;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started");
        setContentView(R.layout.activity_add_event);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        priorityColorIndicator = findViewById(R.id.priorityColorIndicator); // Инициализация TextView
        dbHelper = new DatabaseHelper(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.priorities, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);

        // Используем анонимный класс для реализации обоих методов интерфейса
        spinnerPriority.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parentView, android.view.View selectedItemView, int position, long id) {
                updatePriorityColor(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parentView) {
                // Можно оставить пустым или добавить нужную логику
            }
        });

        Button btnDateTime = findViewById(R.id.btnDateTime);
        btnDateTime.setOnClickListener(v -> showDateTimePicker());

        findViewById(R.id.btnSave).setOnClickListener(v -> saveEvent());
    }

    private void updatePriorityColor(int priority) {
        int color;
        switch (priority) {
            case 0:
                color = ContextCompat.getColor(this, R.color.low_priority); // Зеленый
                break;
            case 1:
                color = ContextCompat.getColor(this, R.color.medium_priority); // Желтый
                break;
            case 2:
                color = ContextCompat.getColor(this, R.color.high_priority); // Красный
                break;
            default:
                color = ContextCompat.getColor(this, R.color.default_priority); // По умолчанию
                break;
        }
        priorityColorIndicator.setBackgroundColor(color);
    }

    private void showDateTimePicker() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, day) -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (tpView, hour, minute) -> {
                calendar.set(year, month, day, hour, minute);
                selectedDateTime = calendar.getTimeInMillis();
                Log.d(TAG, "showDateTimePicker: Selected date/time = " + selectedDateTime);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
            timePicker.show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
    }

    private void saveEvent() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        int priority = spinnerPriority.getSelectedItemPosition();  // Получаем индекс из Spinner без изменения

        if (title.isEmpty() || description.isEmpty() || selectedDateTime == -1) {
            Toast.makeText(this, "Заполните все поля и выберите дату!", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = new Event(0, title, description, selectedDateTime, priority);
        long id = dbHelper.insertEvent(event);

        if (id > 0) {
            event.setId((int) id);
            scheduleNotification(event);
            Toast.makeText(this, "Событие добавлено", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Ошибка сохранения!", Toast.LENGTH_SHORT).show();
        }
    }
    private void scheduleNotification(Event event) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("title", event.getTitle());
        intent.putExtra("description", event.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, event.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Разрешите точные будильники в настройках!", Toast.LENGTH_LONG).show();
                return;
            }
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.getDateTime(), pendingIntent);
            Log.d(TAG, "scheduleNotification: Alarm set for " + event.getDateTime());
        }
    }
}
