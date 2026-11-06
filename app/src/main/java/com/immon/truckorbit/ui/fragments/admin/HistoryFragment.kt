package com.immon.truckorbit.ui.fragments.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.immon.truckorbit.R
import com.immon.truckorbit.data.Constants.TRUCK_DATABASE
import com.immon.truckorbit.data.models.TruckModel
import com.immon.truckorbit.databinding.FragmentHistoryBinding
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryFragment : BaseFragment() {

    private lateinit var binding: FragmentHistoryBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var osmdroidMap: MapView? = null
    private var selectedDate = Calendar.getInstance()
    
    // Lista para guardar los camiones (que tienen conductores asignados)
    private val truckList = mutableListOf<TruckModel>()
    private var selectedTruckId: String? = null

    override val isLightStatusbar: Boolean
        get() = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryBinding.inflate(inflater, container, false)
        
        Configuration.getInstance().userAgentValue = requireContext().packageName
        
        initializeMap()
        setupDatePicker()
        loadDriversForSpinner()
        setupFilterButton() // <--- AQUÍ ESTÁ LA CORRECCIÓN
        
        return binding.root
    }

    private fun initializeMap() {
        osmdroidMap = view?.findViewById(R.id.map_history)
        osmdroidMap?.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
        }
    }

    private fun setupDatePicker() {
        binding.btnSelectDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate.set(year, month, day)
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.tvSelectedDate.text = "Fecha: ${format.format(selectedDate.time)}"
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    private fun loadDriversForSpinner() {
        firestore.collection(TRUCK_DATABASE).get().addOnSuccessListener { snapshot ->
            truckList.clear()
            val driverNames = mutableListOf<String>()
            
            for (doc in snapshot) {
                val truck = doc.toObject(TruckModel::class.java)
                truckList.add(truck)
                // Mostramos el nombre del camión y el nombre del conductor
                val driverName = truck.currentDriver?.name ?: "Sin conductor"
                driverNames.add("${truck.truckName} - $driverName")
            }

            if (context != null) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, driverNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerDrivers.adapter = adapter
            }
        }
    }

    private fun setupFilterButton() {
        binding.btnFilter.setOnClickListener {
            val selectedPosition = binding.spinnerDrivers.selectedItemPosition
            if (selectedPosition >= 0 && selectedPosition < truckList.size) {
                selectedTruckId = truckList[selectedPosition].id
                loadHistoryForDateAndTruck()
            } else {
                Toast.makeText(requireContext(), "Selecciona un conductor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadHistoryForDateAndTruck() {
        val startOfDay = selectedDate.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0); startOfDay.set(Calendar.MILLISECOND, 0)

        val endOfDay = selectedDate.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23); endOfDay.set(Calendar.MINUTE, 59)
        endOfDay.set(Calendar.SECOND, 59); endOfDay.set(Calendar.MILLISECOND, 999)

        firestore.collection("history")
            .whereEqualTo("truckId", selectedTruckId) // Filtro por conductor
            .whereGreaterThanOrEqualTo("timestamp", startOfDay.timeInMillis)
            .whereLessThanOrEqualTo("timestamp", endOfDay.timeInMillis)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val points = mutableListOf<GeoPoint>()
                for (document in querySnapshot) {
                    val lat = document.getDouble("latitude") ?: continue
                    val lng = document.getDouble("longitude") ?: continue
                    points.add(GeoPoint(lat, lng))
                }

                if (points.isNotEmpty()) {
                    drawRoute(points)
                } else {
                    Toast.makeText(requireContext(), "No hay recorridos para este conductor en esa fecha", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun drawRoute(points: List<GeoPoint>) {
        osmdroidMap?.overlays?.clear()
        val polyline = Polyline(osmdroidMap)
        polyline.outlinePaint.strokeWidth = 8f
        polyline.outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
        polyline.setPoints(points)
        osmdroidMap?.overlays?.add(polyline)

        val startMarker = Marker(osmdroidMap)
        startMarker.position = points.first()
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.title = "Inicio"
        startMarker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_truck_marker)
        osmdroidMap?.overlays?.add(startMarker)

        val endMarker = Marker(osmdroidMap)
        endMarker.position = points.last()
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        endMarker.title = "Fin"
        endMarker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_truck_marker)
        osmdroidMap?.overlays?.add(endMarker)

        osmdroidMap?.controller?.setCenter(points.first())
        osmdroidMap?.controller?.setZoom(15.0)
        osmdroidMap?.invalidate()
    }
}