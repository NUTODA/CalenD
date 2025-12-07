package com.example.calend

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.calend.data.AppDatabase
import com.example.calend.data.Event
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddEventActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var editTitle: TextInputEditText
    private lateinit var editDate: TextInputEditText
    private lateinit var editTime: TextInputEditText
    private lateinit var editDescription: TextInputEditText
    private lateinit var switchReminder: SwitchMaterial

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var selectedHour = 9
    private var selectedMinute = 0

    private var editingEventId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

        database = AppDatabase.getDatabase(this)

        initViews()
        setupToolbar()
        setupDatePicker()
        setupTimePicker()
        setupButtons()

        // Проверяем, редактируем ли существующее событие
        editingEventId = intent.getLongExtra("event_id", -1)
        if (editingEventId != -1L) {
            loadEventForEditing()
        } else {
            // Устанавливаем дату из intent или сегодняшнюю
            val selectedDate = intent.getStringExtra("selected_date")
            if (selectedDate != null) {
                parseAndSetDate(selectedDate)
            } else {
                setTodayDate()
            }
        }
    }

    private fun initViews() {
        editTitle = findViewById(R.id.edit_title)
        editDate = findViewById(R.id.edit_date)
        editTime = findViewById(R.id.edit_time)
        editDescription = findViewById(R.id.edit_description)
        switchReminder = findViewById(R.id.switch_reminder)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        if (editingEventId != -1L) {
            toolbar.title = "Редактировать событие"
        }
    }

    private fun setupDatePicker() {
        editDate.setOnClickListener {
            val dialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedYear = year
                    selectedMonth = month
                    selectedDay = dayOfMonth
                    updateDateDisplay()
                },
                selectedYear,
                selectedMonth,
                selectedDay
            )
            dialog.show()
        }
    }

    private fun setupTimePicker() {
        editTime.setOnClickListener {
            val dialog = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    updateTimeDisplay()
                },
                selectedHour,
                selectedMinute,
                true
            )
            dialog.show()
        }
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener {
            saveEvent()
        }
    }

    private fun setTodayDate() {
        val today = Calendar.getInstance()
        selectedYear = today.get(Calendar.YEAR)
        selectedMonth = today.get(Calendar.MONTH)
        selectedDay = today.get(Calendar.DAY_OF_MONTH)
        updateDateDisplay()
    }

    private fun parseAndSetDate(dateString: String) {
        try {
            val parts = dateString.split("-")
            selectedYear = parts[0].toInt()
            selectedMonth = parts[1].toInt() - 1
            selectedDay = parts[2].toInt()
            updateDateDisplay()
        } catch (e: Exception) {
            setTodayDate()
        }
    }

    private fun updateDateDisplay() {
        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth, selectedDay)
        val format = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        editDate.setText(format.format(calendar.time))
    }

    private fun updateTimeDisplay() {
        val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
        editTime.setText(timeString)
    }

    private fun loadEventForEditing() {
        lifecycleScope.launch {
            val event = database.eventDao().getEventById(editingEventId)
            event?.let {
                editTitle.setText(it.title)
                editDescription.setText(it.description)
                switchReminder.isChecked = it.reminder

                // Парсим дату
                parseAndSetDate(it.date)

                // Парсим время
                if (it.time.isNotEmpty()) {
                    val timeParts = it.time.split(":")
                    if (timeParts.size == 2) {
                        selectedHour = timeParts[0].toInt()
                        selectedMinute = timeParts[1].toInt()
                        updateTimeDisplay()
                    }
                }
            }
        }
    }

    private fun saveEvent() {
        val title = editTitle.text.toString().trim()

        if (title.isEmpty()) {
            editTitle.error = "Введите название"
            return
        }

        val date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
        val time = if (editTime.text.toString().isNotEmpty()) {
            String.format("%02d:%02d", selectedHour, selectedMinute)
        } else ""
        val description = editDescription.text.toString().trim()
        val reminder = switchReminder.isChecked

        lifecycleScope.launch {
            val event = Event(
                id = if (editingEventId != -1L) editingEventId else 0,
                title = title,
                date = date,
                time = time,
                description = description,
                reminder = reminder
            )

            if (editingEventId != -1L) {
                database.eventDao().updateEvent(event)

                // Отменяем старое напоминание и ставим новое если нужно
                cancelReminder(editingEventId)
                if (reminder && time.isNotEmpty()) {
                    scheduleReminder(event)
                }

                Toast.makeText(this@AddEventActivity, "Событие обновлено", Toast.LENGTH_SHORT).show()
            } else {
                val newId = database.eventDao().insertEvent(event)

                if (reminder && time.isNotEmpty()) {
                    val newEvent = event.copy(id = newId)
                    scheduleReminder(newEvent)
                }

                Toast.makeText(this@AddEventActivity, "Событие создано", Toast.LENGTH_SHORT).show()
            }

            finish()
        }
    }

    private fun scheduleReminder(event: Event) {
        if (event.time.isEmpty()) {
            android.util.Log.d("REMINDER", "❌ Time is empty, skipping")
            return
        }

        android.util.Log.d("REMINDER", "📅 Scheduling reminder for: ${event.title}")
        android.util.Log.d("REMINDER", "📅 Date: ${event.date}, Time: ${event.time}")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Проверка для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                android.util.Log.d("REMINDER", "❌ Cannot schedule exact alarms!")
                Toast.makeText(
                    this,
                    "Разрешите точные будильники в настройках",
                    Toast.LENGTH_LONG
                ).show()
                // Открываем настройки
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(ReminderReceiver.EXTRA_EVENT_TITLE, event.title)
            putExtra(ReminderReceiver.EXTRA_EVENT_TIME, event.time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Парсим дату и время
        val calendar = Calendar.getInstance()
        val dateParts = event.date.split("-")
        val timeParts = event.time.split(":")

        calendar.set(Calendar.YEAR, dateParts[0].toInt())
        calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)
        calendar.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        calendar.set(Calendar.MINUTE, timeParts[1].toInt())
        calendar.set(Calendar.SECOND, 0)

        android.util.Log.d("REMINDER", "⏰ Alarm time: ${calendar.time}")
        android.util.Log.d("REMINDER", "⏰ Alarm millis: ${calendar.timeInMillis}")
        android.util.Log.d("REMINDER", "⏰ Current millis: ${System.currentTimeMillis()}")

        // Устанавливаем будильник только если время в будущем
        if (calendar.timeInMillis > System.currentTimeMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                android.util.Log.d("REMINDER", "✅ Alarm scheduled successfully!")
                Toast.makeText(this, "Напоминание установлено", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                android.util.Log.e("REMINDER", "❌ SecurityException: ${e.message}")
                Toast.makeText(this, "Ошибка: нет разрешения на будильники", Toast.LENGTH_LONG).show()
            }
        } else {
            android.util.Log.d("REMINDER", "❌ Time is in the past! Not scheduling.")
            Toast.makeText(this, "Время уже прошло", Toast.LENGTH_SHORT).show()
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