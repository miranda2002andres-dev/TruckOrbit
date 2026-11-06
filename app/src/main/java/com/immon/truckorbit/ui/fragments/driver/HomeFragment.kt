package com.immon.truckorbit.ui.fragments.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.immon.truckorbit.R
import com.immon.truckorbit.data.Constants.TRUCK_DATABASE
import com.immon.truckorbit.data.Constants.USER_DATABASE
import com.immon.truckorbit.data.LocalDB
import com.immon.truckorbit.data.enums.AccountStatusModel
import com.immon.truckorbit.data.enums.DrivingStatusModel
import com.immon.truckorbit.data.models.TruckModel
import com.immon.truckorbit.data.models.UserModel
import com.immon.truckorbit.databinding.FragmentHomeBinding
import com.immon.truckorbit.ui.activities.MainActivity.Companion.replaceFragment
import com.immon.truckorbit.ui.fragments.LandingFragment
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import com.immon.truckorbit.utils.setTitle

@Suppress("DEPRECATION")
class HomeFragment : BaseFragment() {

    private lateinit var binding: FragmentHomeBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var selectedTruckId: String? = null
    private var currentUserId = firebaseAuth.currentUser?.uid!!
    private var userListener: ListenerRegistration? = null

    override val isLightStatusbar: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocalDB.putBoolean("logged_in", true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        binding.header.toolbar.setTitle(requireContext(), R.string.app_name, false)

        fetchTrucks { truckList ->
            val truckNames = truckList.map { it.truckName }
            binding.spinnerTrucks.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                truckNames
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.spinnerTrucks.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedTruckId = truckList[position].id
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        selectedTruckId = null
                    }
                }
        }

        binding.btnGetStarted.setOnClickListener {
            binding.btnGetStarted.startAnimation {
                assignDrivingStatus()
            }
        }

        return binding.root
    }

    private fun fetchTrucks(callback: (List<TruckModel>) -> Unit) {
        firestore.collection(TRUCK_DATABASE).get()
            .addOnSuccessListener { result ->
                val truckList = result.documents.mapNotNull { it.toObject<TruckModel>() }
                selectedTruckId = truckList.firstOrNull()?.id
                callback(truckList)
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }

    private fun assignDrivingStatus() {
        if (selectedTruckId == null) {
            Toast.makeText(
                requireContext(),
                "Please select a truck first",
                Toast.LENGTH_SHORT
            ).show()
            binding.btnGetStarted.revertAnimation()
            return
        }

        if (binding.etDestination.text.toString().trim().isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please enter a destination",
                Toast.LENGTH_SHORT
            ).show()
            binding.btnGetStarted.revertAnimation()
            return
        }

        val truckDocRef = firestore.collection(TRUCK_DATABASE).document(selectedTruckId!!)
        val userDocRef = firestore.collection(USER_DATABASE).document(currentUserId)

        firestore.runTransaction { transaction ->
            val truckSnapshot = transaction.get(truckDocRef)
            val truckModel = truckSnapshot.toObject<TruckModel>()
                ?: throw IllegalStateException("Truck not found")

            val userSnapshot = transaction.get(userDocRef)
            val userModel = userSnapshot.toObject<UserModel>()
                ?: throw IllegalStateException("User not found")

            if (truckModel.currentDriver != null) {
                throw IllegalStateException("Truck is already assigned to another driver")
            }

            if (truckModel.drivingStatus != DrivingStatusModel.STOPPED) {
                throw IllegalStateException("Truck is already moving")
            }

            if (userModel.drivingStatus != DrivingStatusModel.STOPPED) {
                throw IllegalStateException("User is already driving")
            }

            truckModel.currentDriver = userModel
            truckModel.drivingStatus = DrivingStatusModel.IDLE
            truckModel.destination = binding.etDestination.text.toString().trim()

            userModel.drivingStatus = DrivingStatusModel.IDLE

            transaction.set(truckDocRef, truckModel)
            transaction.set(userDocRef, userModel)
        }.addOnSuccessListener {
            binding.btnGetStarted.revertAnimation()
        }.addOnFailureListener { exception ->
            binding.btnGetStarted.revertAnimation()
            exception.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Failed to assign truck: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()

        binding.spinnerTrucks.isEnabled = false
        binding.etDestination.isEnabled = false
        binding.btnGetStarted.isEnabled = false
        binding.btnGetStarted.alpha = 0.5f

        val userDocRef = firestore.collection(USER_DATABASE).document(currentUserId)

        if (userListener == null) {
            userListener = userDocRef.addSnapshotListener { document, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val user = document.toObject(UserModel::class.java)

                    if (user?.accountStatus == AccountStatusModel.ACTIVE) {
                        binding.cardAwaitingApproval.visibility = View.GONE
                        binding.spinnerTrucks.isEnabled = true
                        binding.etDestination.isEnabled = true

                        firestore.collection(TRUCK_DATABASE)
                            .whereEqualTo("currentDriver.id", currentUserId)
                            .get()
                            .addOnSuccessListener { documents ->
                                val assignedTrucks = documents.mapNotNull {
                                    it.toObject(TruckModel::class.java)
                                }

                                if (assignedTrucks.isNotEmpty()) {
                                    binding.btnGetStarted.isEnabled = false
                                    binding.btnGetStarted.alpha = 0.5f

                                    replaceFragment(LocationSharingFragment().apply {
                                        arguments = Bundle().apply {
                                            putString("selectedTruckId", assignedTrucks.first().id)
                                        }
                                    })
                                } else {
                                    val isEnabled = user.accountStatus == AccountStatusModel.ACTIVE
                                    binding.btnGetStarted.isEnabled = isEnabled
                                    binding.btnGetStarted.alpha = if (isEnabled) 1f else 0.5f
                                }
                            }
                            .addOnFailureListener { exception ->
                                exception.printStackTrace()
                            }
                    } else {
                        binding.cardAwaitingApproval.visibility = View.VISIBLE
                        binding.spinnerTrucks.isEnabled = false
                        binding.etDestination.isEnabled = false
                        binding.btnGetStarted.isEnabled = false
                        binding.btnGetStarted.alpha = 0.5f
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        userListener?.remove()
        userListener = null
    }

    override fun onStop() {
        super.onStop()
        userListener?.remove()
        userListener = null
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_corner_menu_driver_home, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith(
            "super.onOptionsItemSelected(item)",
            "androidx.fragment.app.Fragment"
        )
    )
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                LocalDB.putBoolean("logged_in", false)

                replaceFragment(LandingFragment())
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
