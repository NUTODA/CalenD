package com.example.calend

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calend.data.AppDatabase
import com.example.calend.data.Event
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var calendar: CalendarView
    private lateinit var dateView: TextView
    private lateinit var toolbarTitle: TextView
    private lateinit var eventsRecycler: RecyclerView
    private lateinit var noEventsText: TextView
    private lateinit var eventAdapter: EventAdapter

    private var selectedDate: String = ""

    companion object {
        const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // === СТАТУС БАР СИНИМ ===
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)

        // Белые иконки на синем фоне
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                0,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = 0
        }

        database = AppDatabase.getDatabase(this)

        initViews()
        setupCalendar()
        setupButtons()
        setupRecyclerView()
        requestNotificationPermission()

        // Устанавливаем сегодняшнюю дату
        setTodayDate()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем события при возврате на экран
        if (selectedDate.isNotEmpty()) {
            loadEventsForDate(selectedDate)
        }
    }

    private fun initViews() {
        calendar = findViewById(R.id.calendar)
        dateView = findViewById(R.id.date_view)
        toolbarTitle = findViewById(R.id.toolbar_title)
        eventsRecycler = findViewById(R.id.events_recycler)
        noEventsText = findViewById(R.id.no_events_text)
    }

    private fun setupCalendar() {
        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)

            // Обновляем отображение даты
            val displayDate = formatDisplayDate(dayOfMonth, month, year)
            dateView.text = displayDate

            // Обновляем заголовок
            updateToolbarTitle(month, year)

            // Загружаем события для выбранной даты
            loadEventsForDate(selectedDate)
        }
    }

    private fun setupButtons() {
        findViewById<ImageButton>(R.id.btn_search).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btn_add).setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            intent.putExtra("selected_date", selectedDate)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(emptyList()) { event ->
            // Открываем детали события
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("event_id", event.id)
            startActivity(intent)
        }

        eventsRecycler.layoutManager = LinearLayoutManager(this)
        eventsRecycler.adapter = eventAdapter
    }

    private fun setTodayDate() {
        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH)
        val day = today.get(Calendar.DAY_OF_MONTH)

        selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
        dateView.text = formatDisplayDate(day, month, year)
        updateToolbarTitle(month, year)
        loadEventsForDate(selectedDate)
    }

    private fun formatDisplayDate(day: Int, month: Int, year: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val format = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        return format.format(calendar.time)
    }

    private fun updateToolbarTitle(month: Int, year: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, month)
        val format = SimpleDateFormat("LLLL yyyy", Locale("ru"))
        val title = format.format(calendar.time)
        toolbarTitle.text = title.replaceFirstChar { it.uppercase() }
    }

    private fun loadEventsForDate(date: String) {
        lifecycleScope.launch {
            val events = database.eventDao().getEventsByDate(date)
            updateEventsList(events)
        }
    }

    private fun updateEventsList(events: List<Event>) {
        eventAdapter.updateEvents(events)

        if (events.isEmpty()) {
            noEventsText.visibility = View.VISIBLE
            eventsRecycler.visibility = View.GONE
        } else {
            noEventsText.visibility = View.GONE
            eventsRecycler.visibility = View.VISIBLE
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }
}