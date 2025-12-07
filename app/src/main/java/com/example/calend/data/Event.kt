package com.example.calend.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val date: String,        // Формат: "2024-12-25"
    val time: String,        // Формат: "14:30"
    val description: String = "",
    val reminder: Boolean = false
)