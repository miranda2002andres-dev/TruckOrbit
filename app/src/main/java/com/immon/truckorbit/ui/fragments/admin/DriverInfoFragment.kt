package com.immon.truckorbit.ui.fragments.admin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.immon.truckorbit.R
import com.immon.truckorbit.data.Constants.TRUCK_DATABASE
import com.immon.truckorbit.data.Constants.USER_DATABASE
import com.immon.truckorbit.data.enums.AccountStatusModel
import com.immon.truckorbit.data.enums.AccountTypeModel
import com.immon.truckorbit.data.enums.DrivingStatusModel
import com.immon.truckorbit.data.models.TruckModel
import com.immon.truckorbit.data.models.UserModel
import com.immon.truckorbit.databinding.FragmentDriverInfoBinding
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import com.immon.truckorbit.utils.setTitle

class DriverInfoFragment : BaseFragment() {

    private lateinit var binding: FragmentDriverInfoBinding
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var userId: String

    override val isLightStatusbar: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            userId = it.getString("driverId", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDriverInfoBinding.inflate(inflater, container, false)

        binding.header.toolbar.setTitle(requireContext(), "Driver Info", true)

        binding.ivChangeAccountType.setOnClickListener {
            switchAccountType()
        }

        binding.ivChangeAccountStatus.setOnClickListener {
            switchAccountStatus()
        }

        listenToUserModelChanges()

        return binding.root
    }

    private fun getCurrentUserDocumentRef(): DocumentReference {
        return firestore.collection(USER_DATABASE).document(userId)
    }

    private fun switchAccountType() {
        startRotationAnimation(binding.ivChangeAccountType)

        getCurrentUserDocumentRef().get().addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(UserModel::class.java)
                user?.let {
                    val newType = if (it.accountType == AccountTypeModel.DRIVER)
                        AccountTypeModel.ADMIN else AccountTypeModel.DRIVER

                    getCurrentUserDocumentRef().update("accountType", newType)
                        .addOnSuccessListener {
                            toast("Account type updated")
                            stopRotationAnimation(binding.ivChangeAccountType)
                        }
                        .addOnFailureListener { e ->
                            toast("Failed to update: ${e.message}")
                            stopRotationAnimation(binding.ivChangeAccountType)
                        }
                }
            }
        }
    }

    private fun switchAccountStatus() {
        startRotationAnimation(binding.ivChangeAccountStatus)

        getCurrentUserDocumentRef().get().addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(UserModel::class.java)
                user?.let {
                    val newStatus = if (it.accountStatus == AccountStatusModel.ACTIVE)
                        AccountStatusModel.INACTIVE else AccountStatusModel.ACTIVE

                    getCurrentUserDocumentRef().update("accountStatus", newStatus)
                        .addOnSuccessListener {
                            toast("Account status updated")
                            stopRotationAnimation(binding.ivChangeAccountStatus)
                        }
                        .addOnFailureListener { e ->
                            toast("Failed to update: ${e.message}")
                            stopRotationAnimation(binding.ivChangeAccountStatus)
                        }
                }
            }
        }
    }

    private fun listenToUserModelChanges() {
        getCurrentUserDocumentRef().addSnapshotListener { snapshot, error ->
            if (error != null) {
                toast("Error fetching user: ${error.message}")
                return@addSnapshotListener
            }

            snapshot?.let {
                if (it.exists()) {
                    val user = it.toObject(UserModel::class.java)
                    user?.let {
                        updateUI(user)
                        fetchTruckModel(user.id)
                    }
                }
            }
        }
    }

    private fun fetchTruckModel(driverId: String) {
        firestore.collection(TRUCK_DATABASE)
            .whereEqualTo("currentDriver.id", driverId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val truck = documents.documents[0].toObject(TruckModel::class.java)
                    updateTruckUI(truck)
                } else {
                    updateTruckUI(null)
                }
            }
            .addOnFailureListener { e ->
                toast("Failed to fetch truck: ${e.message}")
            }
    }

    private fun updateUI(user: UserModel) {
        binding.tvName.text = user.name
        binding.tvMail.text = user.email
        binding.tvAccountType.text =
            if (user.accountType == AccountTypeModel.DRIVER) "Driver" else "Admin"
        binding.tvAccountStatus.text =
            if (user.accountStatus == AccountStatusModel.ACTIVE) "Active" else "Inactive"
        binding.tvDrivingStatus.text = when (user.drivingStatus) {
            DrivingStatusModel.DRIVING -> "Driving"
            DrivingStatusModel.IDLE -> "Idle"
            DrivingStatusModel.STOPPED -> "Offline"
        }
        binding.tvAccountType.setTextColor(
            if (user.accountType == AccountTypeModel.DRIVER)
                Color.BLUE
            else
                Color.GREEN
        )
        binding.tvAccountStatus.setTextColor(
            if (user.accountStatus == AccountStatusModel.ACTIVE)
                Color.GREEN
            else
                Color.RED
        )
        if (context != null) {
            binding.profileImage.setImageDrawable(
                if (user.accountType == AccountTypeModel.DRIVER)
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.img_driver_profile_picture_placeholder
                    )
                else
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.img_admin_profile_picture_placeholder
                    )
            )
        }
    }

    private fun updateTruckUI(truck: TruckModel?) {
        binding.tvVehicleName.text = truck?.truckName ?: "N/A"
        binding.tvVehicleLicense.text = truck?.licenseNo ?: "N/A"
        binding.tvGoingTo.text = truck?.destination ?: "N/A"
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun startRotationAnimation(view: ImageView) {
        view.animate()
            .rotationBy(-360f)
            .setDuration(1000)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                if (view.tag == "rotating") {
                    startRotationAnimation(view)
                }
            }.start()
        view.tag = "rotating"
    }

    private fun stopRotationAnimation(view: ImageView) {
        view.tag = null
        view.animate().cancel()
        view.rotation = 0f
    }
}
