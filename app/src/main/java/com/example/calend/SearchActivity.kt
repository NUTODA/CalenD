package com.example.calend

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calend.data.AppDatabase
import com.example.calend.data.Event
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var searchInput: TextInputEditText
    private lateinit var searchRecycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var resultsHeader: TextView
    private lateinit var eventAdapter: EventAdapter

    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        database = AppDatabase.getDatabase(this)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupSearch()

        // Загружаем все события при открытии
        loadAllEvents()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем результаты при возврате
        val query = searchInput.text.toString()
        if (query.isEmpty()) {
            loadAllEvents()
        } else {
            performSearch(query)
        }
    }

    private fun initViews() {
        searchInput = findViewById(R.id.search_input)
        searchRecycler = findViewById(R.id.search_recycler)
        emptyState = findViewById(R.id.empty_state)
        emptyText = findViewById(R.id.empty_text)
        resultsHeader = findViewById(R.id.results_header)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(emptyList()) { event ->
            // Открываем детали события
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("event_id", event.id)
            startActivity(intent)
        }

        searchRecycler.layoutManager = LinearLayoutManager(this)
        searchRecycler.adapter = eventAdapter
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Отменяем предыдущий поиск
                searchJob?.cancel()

                val query = s.toString().trim()

                // Запускаем поиск с небольшой задержкой (debounce)
                searchJob = lifecycleScope.launch {
                    delay(300) // Ждём 300мс после последнего ввода

                    if (query.isEmpty()) {
                        loadAllEvents()
                    } else {
                        performSearch(query)
                    }
                }
            }
        })

        // Фокус на поле поиска при открытии
        searchInput.requestFocus()
    }

    private fun loadAllEvents() {
        lifecycleScope.launch {
            val events = database.eventDao().getAllEvents()
            resultsHeader.text = "Все события (${events.size})"
            updateResults(events, "Нет событий")
        }
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            val events = database.eventDao().searchEvents(query)
            resultsHeader.text = "Найдено: ${events.size}"
            updateResults(events, "События не найдены")
        }
    }

    private fun updateResults(events: List<Event>, emptyMessage: String) {
        eventAdapter.updateEvents(events)

        if (events.isEmpty()) {
            searchRecycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            emptyText.text = emptyMessage
        } else {
            searchRecycler.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }
}