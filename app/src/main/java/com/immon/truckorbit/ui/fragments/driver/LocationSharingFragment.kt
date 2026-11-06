package com.immon.truckorbit.ui.fragments.driver

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.immon.truckorbit.data.Constants.USER_DATABASE
import com.immon.truckorbit.data.enums.DrivingStatusModel
import com.immon.truckorbit.data.models.TruckModel
import com.immon.truckorbit.data.models.UserModel
import com.immon.truckorbit.databinding.FragmentLocationSharingBinding
import com.immon.truckorbit.ui.activities.MainActivity.Companion.myFragmentManager
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import com.immon.truckorbit.utils.AnimationQueue
import com.immon.truckorbit.utils.applyWindowInsets
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.UUID

class LocationSharingFragment : BaseFragment() {

    private lateinit var binding: FragmentLocationSharingBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var animationQueue: AnimationQueue
    private lateinit var selectedTruckId: String
    private var disableBackButton = true
    private val firestore = FirebaseFirestore.getInstance()
    private var osmdroidMap: MapView? = null
    private var currentMarker: Marker? = null
    private var markerLoadedFirstTime = true
    private var lastLocation: GeoPoint? = null
    
    // Variable para guardar la última ubicación y medir la distancia
    private var lastSavedLocation: GeoPoint? = null 
    
    private val requestLocationPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocationPermission: Boolean ->
            startLocationUpdates()
            loadMapFragment(hasLocationPermission)
        }

    override val isLightStatusbar: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTruckId = arguments?.getString("selectedTruckId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLocationSharingBinding.inflate(inflater, container, false)
        (binding.btnStartDriving.parent as ViewGroup).applyWindowInsets(top = false, bottom = true)
        setButtonVisibility()
        
        Configuration.getInstance().userAgentValue = requireContext().packageName
        
        askLocationPermission()

        binding.btnStartDriving.setOnClickListener { updateDrivingStatus(true) }
        binding.btnStopDriving.setOnClickListener { updateDrivingStatus(false) }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!disableBackButton) {
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            }
        )
        return binding.root
    }

    private fun setButtonVisibility() {
        if (::selectedTruckId.isInitialized.not()) return

        firestore.collection(TRUCK_DATABASE).document(selectedTruckId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val drivingStatus = document.getString("drivingStatus")
                    if (drivingStatus == DrivingStatusModel.IDLE.name) {
                        binding.btnStartDriving.visibility = View.VISIBLE
                        binding.btnStopDriving.visibility = View.INVISIBLE
                    } else {
                        binding.btnStartDriving.visibility = View.INVISIBLE
                        binding.btnStopDriving.visibility = View.VISIBLE
                    }
                }
            }
    }

    private fun updateDrivingStatus(isDriving: Boolean) {
        if (::selectedTruckId.isInitialized.not()) return

        binding.btnStartDriving.startAnimation()
        binding.btnStopDriving.startAnimation()

        val truckDocRef = firestore.collection(TRUCK_DATABASE).document(selectedTruckId)

        firestore.runTransaction { transaction ->
            val truckSnapshot = transaction.get(truckDocRef)
            val truckModel = truckSnapshot.toObject(TruckModel::class.java)
                ?: throw IllegalStateException("Truck not found")

            val userDocRef = truckModel.currentDriver?.let {
                firestore.collection(USER_DATABASE).document(it.id)
            } ?: throw IllegalStateException("No driver assigned to this truck")

            val userSnapshot = transaction.get(userDocRef)
            val userModel = userSnapshot.toObject(UserModel::class.java)
                ?: throw IllegalStateException("User not found")

            val drivingStatus = if (isDriving) DrivingStatusModel.DRIVING else DrivingStatusModel.STOPPED
            truckModel.drivingStatus = drivingStatus
            userModel.drivingStatus = drivingStatus

            if (!isDriving) {
                truckModel.currentDriver = null
                truckModel.destination = null
            }

            transaction.set(truckDocRef, truckModel)
            transaction.set(userDocRef, userModel)
        }.addOnSuccessListener {
            if (isDriving) {
                binding.btnStartDriving.visibility = View.INVISIBLE
                binding.btnStopDriving.visibility = View.VISIBLE
            } else {
                binding.btnStartDriving.visibility = View.INVISIBLE
                binding.btnStopDriving.visibility = View.INVISIBLE
                disableBackButton = false
                myFragmentManager.popBackStackImmediate()
            }
            binding.btnStartDriving.revertAnimation()
            binding.btnStopDriving.revertAnimation()
        }.addOnFailureListener { exception ->
            Log.e("Firestore", "Failed: ${exception.message}", exception)
            binding.btnStartDriving.revertAnimation()
            binding.btnStopDriving.revertAnimation()
        }
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
                } else { binding.progressBar.visibility = View.GONE }
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
            controller.setZoom(15.0)
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
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun updateMapLocation(location: Location? = null, latLng: GeoPoint? = null) {
        if (osmdroidMap != null && (location != null || latLng != null)) {
            binding.progressBar.visibility = View.GONE

            val geoPoint = latLng ?: GeoPoint(location!!.latitude, location.longitude)
            saveTruckLocation(geoPoint)

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

    private fun saveTruckLocation(geoPoint: GeoPoint) {
        if (::selectedTruckId.isInitialized.not()) return

        // 1. Guardar ubicación actual (para el mapa en vivo)
        val locationMap = mapOf(
            "location" to mapOf(
                "id" to UUID.randomUUID().toString(),
                "latitude" to geoPoint.latitude,
                "longitude" to geoPoint.longitude
            )
        )
        firestore.collection(TRUCK_DATABASE).document(selectedTruckId).update(locationMap)

        // 2. Guardar en el Historial (cada 10 metros)
        if (lastSavedLocation == null || lastSavedLocation!!.distanceToAsDouble(geoPoint) >= 10.0) {
            val historyMap = mapOf(
                "latitude" to geoPoint.latitude,
                "longitude" to geoPoint.longitude,
                "timestamp" to System.currentTimeMillis(),
                "truckId" to selectedTruckId
            )
            // Guardamos en una colección separada llamada "history"
            firestore.collection("history").add(historyMap)
                .addOnSuccessListener {
                    lastSavedLocation = geoPoint // Actualizamos la última posición guardada
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error guardando historial: ${e.message}")
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}