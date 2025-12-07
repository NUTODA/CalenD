package com.example.calend

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.calend.data.AppDatabase
import com.example.calend.data.Event
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EventDetailActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var detailTitle: TextView
    private lateinit var detailDatetime: TextView
    private lateinit var detailDescription: TextView
    private lateinit var reminderSection: LinearLayout
    private lateinit var descriptionSection: LinearLayout

    private var eventId: Long = -1
    private var currentEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        database = AppDatabase.getDatabase(this)

        eventId = intent.getLongExtra("event_id", -1)
        if (eventId == -1L) {
            Toast.makeText(this, "Событие не найдено", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        loadEvent()
    }

    private fun initViews() {
        detailTitle = findViewById(R.id.detail_title)
        detailDatetime = findViewById(R.id.detail_datetime)
        detailDescription = findViewById(R.id.detail_description)
        reminderSection = findViewById(R.id.reminder_section)
        descriptionSection = findViewById(R.id.description_section)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.btn_edit).setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            intent.putExtra("event_id", eventId)
            startActivity(intent)
        }

        findViewById<MaterialButton>(R.id.btn_delete).setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadEvent() {
        lifecycleScope.launch {
            val event = database.eventDao().getEventById(eventId)
            if (event != null) {
                currentEvent = event
                displayEvent(event)
            } else {
                Toast.makeText(this@EventDetailActivity, "Событие не найдено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayEvent(event: Event) {
        detailTitle.text = event.title

        // Форматируем дату и время
        val datetime = formatDateTime(event.date, event.time)
        detailDatetime.text = datetime

        // Показываем описание если есть
        if (event.description.isNotEmpty()) {
            descriptionSection.visibility = View.VISIBLE
            detailDescription.text = event.description
        } else {
            descriptionSection.visibility = View.GONE
        }

        // Показываем статус напоминания
        if (event.reminder) {
            reminderSection.visibility = View.VISIBLE
        } else {
            reminderSection.visibility = View.GONE
        }
    }

    private fun formatDateTime(date: String, time: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
            val parsedDate = inputFormat.parse(date)
            val formattedDate = parsedDate?.let { outputFormat.format(it) } ?: date

            if (time.isNotEmpty()) {
                "$formattedDate, $time"
            } else {
                formattedDate
            }
        } catch (e: Exception) {
            if (time.isNotEmpty()) "$date, $time" else date
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Удалить событие?")
            .setMessage("Вы уверены, что хотите удалить это событие? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                deleteEvent()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteEvent() {
        lifecycleScope.launch {
            // Отменяем напоминание если было
            cancelReminder(eventId)

            // Удаляем из базы
            database.eventDao().deleteEventById(eventId)

            Toast.makeText(this@EventDetailActivity, "Событие удалено", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun cancelReminder(eventId: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}