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
        
        // Отображаем имя с пометкой типа устройства
        val displayName = if (device.isPhone) {
            "📱 ${device.name ?: device.mac}"
        } else {
            device.name ?: device.mac
        }
        holder.sensorNameText.text = displayName

        // Изменяем фон и иконку в зависимости от выбора
        val isSelected = position == selectedPosition
        val backgroundColor = if (isSelected) {
            ContextCompat.getColor(holder.itemView.context, R.color.selected_sensor_background)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.unselected_sensor_background)
        }

        holder.itemView.setBackgroundColor(backgroundColor)
        
        // Выбираем иконку в зависимости от типа устройства
        val iconRes = if (device.isPhone) {
            android.R.drawable.stat_sys_data_bluetooth // Временно используем системную иконку для телефона
        } else {
            R.drawable.ic_bluetooth_badge
        }
        
        // Для иконки используем drawable с разными цветами
        if (isSelected) {
            holder.bluetoothIcon.setImageResource(iconRes)
            holder.bluetoothIcon.clearColorFilter()
        } else {
            holder.bluetoothIcon.setImageResource(iconRes)
            val iconColor = ContextCompat.getColor(holder.itemView.context, R.color.bluetooth_inactive)
            holder.bluetoothIcon.setColorFilter(iconColor)
        }
    }

    override fun getItemCount(): Int = sensors.size
}
