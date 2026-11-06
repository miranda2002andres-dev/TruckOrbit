package com.immon.truckorbit.ui.fragments.admin

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import com.immon.truckorbit.R
import com.immon.truckorbit.TruckOrbit.getAppContext
import com.immon.truckorbit.data.Constants.TRUCK_DATABASE
import com.immon.truckorbit.data.LocalDB
import com.immon.truckorbit.data.enums.DrivingStatusModel
import com.immon.truckorbit.data.models.TruckModel
import com.immon.truckorbit.databinding.FragmentMonitoringBinding
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import com.immon.truckorbit.utils.AnimationQueue
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MonitoringFragment : BaseFragment() {

    private lateinit var binding: FragmentMonitoringBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var animationQueue: AnimationQueue
    private val firestore = FirebaseFirestore.getInstance()
    private var osmdroidMap: MapView? = null
    private var currentMarker: Marker? = null
    private var markerLoadedFirstTime = true
    private var lastLocation: GeoPoint? = null
    private val requestLocationPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocationPermission: Boolean ->
            startLocationUpdates()
            loadMapFragment(hasLocationPermission)
        }
    private val truckMarkers = mutableMapOf<String, Marker>()
    private val animationQueues = mutableMapOf<String, AnimationQueue>()

    override val isLightStatusbar: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMonitoringBinding.inflate(inflater, container, false)
        Configuration.getInstance().userAgentValue = requireContext().packageName
        askLocationPermission()
        return binding.root
    }

    private fun askLocationPermission() {
        binding.progressBar.visibility = View.VISIBLE
        if (::locationRequest.isInitialized.not()) {
            locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(0)
                .setMaxUpdateDelayMillis(0)
                .build()
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true)
        LocationServices.getSettingsClient(requireActivity()).checkLocationSettings(builder.build())
            .addOnSuccessListener { requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        exception.startResolutionForResult(requireActivity(), 1)
                        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } catch (sendEx: IntentSender.SendIntentException) { sendEx.printStackTrace() }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Unable to enable GPS.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadMapFragment(hasLocationPermission: Boolean) {
        val mapView = view?.findViewById<MapView>(R.id.map_fragment)
        if (mapView != null && hasLocationPermission) {
            initializeMap(mapView)
        }
    }

    private fun initializeMap(map: MapView) {
        osmdroidMap = map
        osmdroidMap?.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
        }
        updateMapLocation(latLng = lastLocation)
    }

    private fun startLocationUpdates() {
        if (::fusedLocationClient.isInitialized.not()) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getAppContext())
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        updateMapLocation(location = location)
                    }
                    listenForTruckUpdates()
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun updateMapLocation(location: Location? = null, latLng: GeoPoint? = null) {
        if (osmdroidMap != null && (location != null || latLng != null)) {
            binding.progressBar.visibility = View.GONE
            val geoPoint = latLng ?: GeoPoint(location!!.latitude, location.longitude)

            if (currentMarker == null) {
                currentMarker = Marker(osmdroidMap)
                currentMarker?.position = geoPoint
                currentMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                currentMarker?.title = "Current Location"
                osmdroidMap?.overlays?.add(currentMarker)
            }

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
            lastLocation = geoPoint
            osmdroidMap?.controller?.setCenter(geoPoint)
            osmdroidMap?.invalidate()
        }
    }

    private fun listenForTruckUpdates() {
        firestore.collection(TRUCK_DATABASE)
            .whereNotEqualTo("drivingStatus", DrivingStatusModel.STOPPED.name)
            .addSnapshotListener { querySnapshot, exception ->
                if (exception != null) return@addSnapshotListener
                val truckList = mutableListOf<TruckModel>()
                val showIdleTrucks = LocalDB.getBoolean("show_idle_trucks", true)
                for (document in querySnapshot?.documents ?: emptyList()) {
                    val truck = document.toObject(TruckModel::class.java)
                    if (truck?.location != null) {
                        if (!showIdleTrucks && truck.drivingStatus == DrivingStatusModel.IDLE) continue
                        truckList.add(truck)
                    }
                }
                updateTruckMarkersOnMap(truckList)
            }
    }

    private fun updateTruckMarkersOnMap(truckList: List<TruckModel>) {
        if (context == null || osmdroidMap == null) return

        truckList.forEach { truck ->
            truck.location?.let { latLng ->
                val geoPoint = GeoPoint(latLng.latitude, latLng.longitude)
                var truckMarker = truckMarkers[truck.id]

                if (truckMarker == null) {
                    truckMarker = Marker(osmdroidMap)
                    truckMarker.position = geoPoint
                    truckMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    truckMarker.title = truck.truckName
                    
                    val userColor = truck.currentDriver?.colorHex ?: "#FF0000"
                    try {
                        val colorInt = android.graphics.Color.parseColor(userColor)
                        truckMarker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_truck_marker)
                        truckMarker.icon?.setTint(colorInt) 
                    } catch (e: Exception) {
                        truckMarker.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_truck_marker)
                    }

                    truckMarker.relatedObject = truck.id
                    osmdroidMap?.overlays?.add(truckMarker)
                    truckMarkers[truck.id] = truckMarker
                }

                if (animationQueues[truck.id] == null) {
                    animationQueues[truck.id] = AnimationQueue(
                        startPosition = geoPoint,
                        scope = lifecycleScope,
                    ) { updatedLat, updatedLng ->
                        truckMarker?.position = GeoPoint(updatedLat, updatedLng)
                        osmdroidMap?.invalidate()
                    }
                } else {
                    animationQueues[truck.id]!!.addToQueue(geoPoint, threshold = 0f)
                }
            }
        }
        osmdroidMap?.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}