package com.immon.truckorbit.ui.adapters

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.immon.truckorbit.R
import com.immon.truckorbit.data.enums.DrivingStatusModel
import com.immon.truckorbit.data.models.UserModel

class DriverAdapter(
    private var driverList: List<UserModel>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<DriverAdapter.DriverViewHolder>() {

    class DriverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val driverName: TextView = itemView.findViewById(R.id.tv_driver_name)
        val driverMail: TextView = itemView.findViewById(R.id.tv_driver_mail)
        val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        val drivingStatus: TextView = itemView.findViewById(R.id.tv_status)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DriverViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_driver_list, parent, false)
        return DriverViewHolder(view)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
        val driver = driverList[position]
        holder.driverName.text = driver.name
        holder.driverMail.text = driver.email

        holder.drivingStatus.text = when (driver.drivingStatus) {
            DrivingStatusModel.DRIVING -> "Driving"
            DrivingStatusModel.IDLE -> "Idle"
            DrivingStatusModel.STOPPED -> "Offline"
        }

        holder.statusIndicator.backgroundTintList = ColorStateList.valueOf(
            when (driver.drivingStatus) {
                DrivingStatusModel.DRIVING -> Color.GREEN
                DrivingStatusModel.IDLE -> Color.BLUE
                DrivingStatusModel.STOPPED -> Color.RED
            }
        )

        holder.itemView.setOnClickListener {
            onItemClick(driver.id)
        }
    }

    override fun getItemCount(): Int = driverList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newDriverList: List<UserModel>) {
        driverList = newDriverList
        notifyDataSetChanged()
    }
}
