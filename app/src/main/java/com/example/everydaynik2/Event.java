package com.example.everydaynik2;

import android.content.Context;

import androidx.core.content.ContextCompat;

public class Event {
    private int id;
    private String title;
    private String description;
    private long dateTime;
    private int priority;

    public Event(int id, String title, String description, long dateTime, int priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
        this.priority = priority;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getDateTime() { return dateTime; }
    public int getPriority() { return priority; }

    public void setId(int id) {
        this.id = id; // Добавьте эту строку!
    }
    public int getPriorityColor(Context context) {
        switch (priority) {
            case 0:
                return ContextCompat.getColor(context, R.color.low_priority); // Зеленый
            case 1:
                return ContextCompat.getColor(context, R.color.medium_priority); // Желтый
            case 2:
                return ContextCompat.getColor(context, R.color.high_priority); // Красный
            default:
                return ContextCompat.getColor(context, R.color.default_priority); // По умолчанию
        }
    }
}
