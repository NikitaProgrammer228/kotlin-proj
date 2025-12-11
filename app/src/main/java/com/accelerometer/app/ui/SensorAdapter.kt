package com.accelerometer.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.accelerometer.app.R
import com.accelerometer.app.bluetooth.BluetoothAccelerometerService

class SensorAdapter(
    private val onSensorClick: (BluetoothAccelerometerService.DiscoveredDevice) -> Unit
) : RecyclerView.Adapter<SensorAdapter.SensorViewHolder>() {

    private var sensors: List<BluetoothAccelerometerService.DiscoveredDevice> = emptyList()
    private var selectedPosition = -1

    fun updateSensors(newSensors: List<BluetoothAccelerometerService.DiscoveredDevice>) {
        sensors = newSensors
        notifyDataSetChanged()
    }

    inner class SensorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bluetoothIcon: ImageView = itemView.findViewById(R.id.bluetoothIcon)
        val sensorNameText: TextView = itemView.findViewById(R.id.sensorNameText)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val previousSelected = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(previousSelected)
                    notifyItemChanged(selectedPosition)
                    onSensorClick(sensors[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor, parent, false)
        return SensorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val device = sensors[position]
        holder.sensorNameText.text = device.name ?: device.mac

        // Изменяем фон и иконку в зависимости от выбора
        val isSelected = position == selectedPosition
        val backgroundColor = if (isSelected) {
            ContextCompat.getColor(holder.itemView.context, R.color.selected_sensor_background)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.unselected_sensor_background)
        }

        holder.itemView.setBackgroundColor(backgroundColor)
        
        // Для иконки используем drawable с разными цветами
        if (isSelected) {
            // Синяя иконка для выбранного
            holder.bluetoothIcon.setImageResource(R.drawable.ic_bluetooth_badge)
            holder.bluetoothIcon.clearColorFilter()
        } else {
            // Серая иконка для невыбранного - используем tint
            holder.bluetoothIcon.setImageResource(R.drawable.ic_bluetooth_badge)
            val iconColor = ContextCompat.getColor(holder.itemView.context, R.color.bluetooth_inactive)
            holder.bluetoothIcon.setColorFilter(iconColor)
        }
    }

    override fun getItemCount(): Int = sensors.size
}
