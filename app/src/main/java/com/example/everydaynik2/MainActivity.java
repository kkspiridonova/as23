package com.example.everydaynik2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private DatabaseHelper dbHelper;
    private EventAdapter eventAdapter;
    private List<Event> eventList;
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Started");
        setContentView(R.layout.activity_main);

        Spinner spinnerSort = findViewById(R.id.spinner_sort);
        Spinner spinnerTimeFilter = findViewById(R.id.spinner_time_filter);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(adapter);

        ArrayAdapter<CharSequence> timeFilterAdapter = ArrayAdapter.createFromResource(this,
                R.array.time_filter_options, android.R.layout.simple_spinner_item);
        timeFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeFilter.setAdapter(timeFilterAdapter);

        dbHelper = new DatabaseHelper(this);
        eventList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(eventList);
        recyclerView.setAdapter(eventAdapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateEvents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerTimeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateEvents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
        }

        Button addEventButton = findViewById(R.id.btnAddEvent);
        addEventButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddEventActivity.class)));

        loadEvents("dateTime ASC", null);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Event event = eventList.get(position);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Удаление события")
                        .setMessage("Вы уверены, что хотите удалить это событие?")
                        .setPositiveButton("Удалить", (dialog, which) -> {
                            if (dbHelper.deleteEvent(event.getId())) {
                                cancelNotification(event.getId());
                                eventList.remove(position);
                                eventAdapter.notifyItemRemoved(position);
                                Toast.makeText(MainActivity.this, "Событие удалено", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Отмена", (dialog, which) -> {
                            eventAdapter.notifyItemChanged(position);
                        })
                        .create()
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void cancelNotification(int eventId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                eventId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(eventId);

        Log.d(TAG, "cancelNotification: Cancelled all for event ID " + eventId);
    }

    private void updateEvents() {
        Spinner spinnerSort = findViewById(R.id.spinner_sort);
        Spinner spinnerTimeFilter = findViewById(R.id.spinner_time_filter);

        String sortOrder = "";
        switch (spinnerSort.getSelectedItemPosition()) {
            case 0: sortOrder = "dateTime ASC"; break;
            case 1: sortOrder = "dateTime DESC"; break;
            case 2: sortOrder = "priority ASC"; break;
            case 3: sortOrder = "priority DESC"; break;
        }

        String timeFilter = null;
        switch (spinnerTimeFilter.getSelectedItemPosition()) {
            case 0: timeFilter = getDateRangeFilter(Calendar.DAY_OF_YEAR, -1); break;
            case 1: timeFilter = getDateRangeFilter(Calendar.WEEK_OF_YEAR, -1); break;
            case 2: timeFilter = getDateRangeFilter(Calendar.MONTH, -1); break;
        }

        loadEvents(sortOrder, timeFilter);
    }

    private String getDateRangeFilter(int field, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(field, amount);
        return "dateTime >= " + calendar.getTimeInMillis();
    }

    @SuppressLint("Range")
    private void loadEvents(String orderBy, String timeFilter) {
        eventList.clear();
        String query = "SELECT * FROM events" + (timeFilter != null ? " WHERE " + timeFilter : "") + " ORDER BY " + orderBy;

        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                eventList.add(new Event(
                        cursor.getInt(cursor.getColumnIndex("id")),
                        cursor.getString(cursor.getColumnIndex("title")),
                        cursor.getString(cursor.getColumnIndex("description")),
                        cursor.getLong(cursor.getColumnIndex("dateTime")),
                        cursor.getInt(cursor.getColumnIndex("priority"))
                ));
            }
        }
        eventAdapter.notifyDataSetChanged();
    }
}