package com.yaxer.timetrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class LapTime(val lapNumber: Int, val lapDuration: Long, val totalTime: Long)

class LapTimesAdapter(private val lapTimes: List<LapTime>) :
    RecyclerView.Adapter<LapTimesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lapNumber: TextView = view.findViewById(R.id.lapNumber)
        val lapDuration: TextView = view.findViewById(R.id.lapDuration)
        val totalTime: TextView = view.findViewById(R.id.totalTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lap_time, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lap = lapTimes[position]
        holder.lapNumber.text = "Lap ${lap.lapNumber}"
        holder.lapDuration.text = formatTime(lap.lapDuration)
        holder.totalTime.text = formatTime(lap.totalTime)
    }

    override fun getItemCount() = lapTimes.size

    private fun formatTime(millis: Long): String {
        val minutes = (millis % 3600000) / 60000
        val seconds = (millis % 60000) / 1000
        val centiseconds = (millis % 1000) / 10
        return String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
    }
}
