package com.example.calend.data

import androidx.room.*

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY date ASC, time ASC")
    suspend fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Long): Event?

    @Query("SELECT * FROM events WHERE date = :date ORDER BY time ASC")
    suspend fun getEventsByDate(date: String): List<Event>

    @Query("SELECT * FROM events WHERE title LIKE '%' || :query || '%' ORDER BY date ASC")
    suspend fun searchEvents(query: String): List<Event>

    @Insert
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)
}