package com.immon.truckorbit.ui.fragments.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.immon.truckorbit.R
import com.immon.truckorbit.data.Constants.TRUCK_DATABASE
import com.immon.truckorbit.data.enums.DrivingStatusModel
import com.immon.truckorbit.data.models.TruckModel
import com.immon.truckorbit.databinding.FragmentTruckInfoBinding
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import com.immon.truckorbit.utils.AnimationQueue
import com.immon.truckorbit.utils.setTitle
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline 

class TruckInfoFragment : BaseFragment() {

    private lateinit var binding: FragmentTruckInfoBinding
    private lateinit var animationQueue: AnimationQueue
    private val firestore = FirebaseFirestore.getInstance()
    private var osmdroidMap: MapView? = null
    private var currentMarker: Marker? = null
    private var markerLoadedFirstTime = true
    private lateinit var truckId: String

    // Variables para el rastro
    private var truckPolyline: Polyline? = null
    private val truckHistory = mutableListOf<GeoPoint>()

    override val isLightStatusbar: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            truckId = it.getString("truckId", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTruckInfoBinding.inflate(inflater, container, false)
        binding.header.toolbar.setTitle(requireContext(), "Truck Info", true)

        Configuration.getInstance().userAgentValue = requireContext().packageName

        loadMapFragment()
        return binding.root
    }

    private fun loadMapFragment() {
        binding.progressBar.visibility = View.VISIBLE
        val mapView = view?.findViewById<MapView>(R.id.map_fragment)
        if (mapView != null) {
            initializeMap(mapView)
        }
    }

    private fun initializeMap(map: MapView) {
        osmdroidMap = map
        osmdroidMap?.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
        listenForTruckUpdates()
    }

    private fun listenForTruckUpdates() {
        firestore.collection(TRUCK_DATABASE).document(truckId)
            .addSnapshotListener { snapshot, exception ->
                binding.progressBar.visibility = View.GONE
                if (exception != null) return@addSnapshotListener

                if (snapshot != null) {
                    val truck = snapshot.toObject<TruckModel>()

                    binding.tvTruckName.text = truck?.truckName ?: "N/A"
                    binding.tvLicenseNo.text = truck?.licenseNo ?: "N/A"
                    binding.tvDriverName.text = truck?.currentDriver?.name ?: "N/A"
                    binding.tvDriverContact.text = truck?.currentDriver?.email ?: "N/A"
                    binding.tvDriverStatus.text = when (truck?.drivingStatus) {
                        DrivingStatusModel.DRIVING -> "Moving"
                        DrivingStatusModel.IDLE -> "Idle"
                        DrivingStatusModel.STOPPED -> "Stopped"
                        else -> "N/A"
                    }
                    binding.tvGoingTo.text = truck?.destination ?: "N/A"

                    val geoPoint = if (truck?.location != null) {
                        GeoPoint(truck.location!!.latitude, truck.location!!.longitude)
                    } else null

                    updateMapLocation(geoPoint, truck)
                }
            }
    }

    private fun updateMapLocation(geoPoint: GeoPoint? = null, truck: TruckModel? = null) {
        if (osmdroidMap != null && geoPoint != null) {
            binding.progressBar.visibility = View.GONE

            // 1. MARCADOR
            if (currentMarker == null) {
                currentMarker = Marker(osmdroidMap)
                currentMarker?.position = geoPoint
                currentMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                currentMarker?.title = "Current Location"
                
                val userColor = truck?.currentDriver?.colorHex ?: "#FF0000"
                try {
                    val colorInt = android.graphics.Color.parseColor(userColor)
                    currentMarker?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_truck_marker)
                    currentMarker?.icon?.setTint(colorInt)
                } catch (e: Exception) {
                    currentMarker?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_truck_marker)
                }
                osmdroidMap?.overlays?.add(currentMarker)
            }

            // 2. RASTRO
            if (truckHistory.isEmpty() || truckHistory.last() != geoPoint) {
                truckHistory.add(geoPoint)
                if (truckHistory.size > 50) truckHistory.removeAt(0)

                if (truckPolyline == null) {
                    truckPolyline = Polyline(osmdroidMap)
                    truckPolyline?.outlinePaint?.strokeWidth = 5f
                    truckPolyline?.outlinePaint?.color = android.graphics.Color.parseColor("#2196F3") 
                    osmdroidMap?.overlays?.add(truckPolyline)
                }
                truckPolyline?.setPoints(truckHistory)
            }

            // 3. ANIMACIÓN
            if (markerLoadedFirstTime) {
                animationQueue = AnimationQueue(
                    startPosition = geoPoint,
                    scope = lifecycleScope,
                ) { updatedLat, updatedLng ->
                    currentMarker?.position = GeoPoint(updatedLat, updatedLng)
                    osmdroidMap?.invalidate()
                }
                markerLoadedFirstTime = false
            } else {
                animationQueue.addToQueue(geoPoint, threshold = 0f)
            }

            osmdroidMap?.controller?.setCenter(geoPoint)
            osmdroidMap?.invalidate()
        }
    }
}