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
import com.immon.truckorbit.data.models.TruckModel

class TruckAdapter(
    private var truckList: List<TruckModel>,
    private val onItemClick: (String) -> Unit
) :
    RecyclerView.Adapter<TruckAdapter.TruckViewHolder>() {

    class TruckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val truckName: TextView = itemView.findViewById(R.id.tv_truck_name)
        val driverName: TextView = itemView.findViewById(R.id.tv_truck_driver)
        val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        val drivingStatus: TextView = itemView.findViewById(R.id.tv_status)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TruckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_truck_list, parent, false)
        return TruckViewHolder(view)
    }

    override fun onBindViewHolder(holder: TruckViewHolder, position: Int) {
        val truck = truckList[position]
        holder.truckName.text = truck.truckName
        holder.driverName.text =
            if (truck.currentDriver?.name != null) "${truck.currentDriver?.name} is driving"
            else "No driver assigned"

        holder.drivingStatus.text = when (truck.drivingStatus) {
            DrivingStatusModel.DRIVING -> "Moving"
            DrivingStatusModel.IDLE -> "Idle"
            DrivingStatusModel.STOPPED -> "Stopped"
        }

        holder.statusIndicator.backgroundTintList = ColorStateList.valueOf(
            when (truck.drivingStatus) {
                DrivingStatusModel.DRIVING -> Color.GREEN
                DrivingStatusModel.IDLE -> Color.BLUE
                DrivingStatusModel.STOPPED -> Color.RED
            }
        )

        holder.itemView.setOnClickListener {
            onItemClick(truck.id)
        }
    }

    override fun getItemCount(): Int = truckList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newTruckList: List<TruckModel>) {
        truckList = newTruckList
        notifyDataSetChanged()
    }
}
