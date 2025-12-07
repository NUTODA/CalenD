package com.example.calend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.calend.data.Event
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private var events: List<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.event_title)
        val datetime: TextView = view.findViewById(R.id.event_datetime)
        val reminderIcon: ImageView = view.findViewById(R.id.reminder_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.title.text = event.title
        holder.datetime.text = formatDateTime(event.date, event.time)
        holder.reminderIcon.visibility = if (event.reminder) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }

    private fun formatDateTime(date: String, time: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMMM", Locale("ru"))
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
}