package com.immon.truckorbit.ui.fragments.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.immon.truckorbit.data.Constants.TRUCK_DATABASE
import com.immon.truckorbit.data.Constants.USER_DATABASE
import com.immon.truckorbit.data.enums.AccountStatusModel
import com.immon.truckorbit.data.enums.AccountTypeModel
import com.immon.truckorbit.data.enums.DrivingStatusModel
import com.immon.truckorbit.data.models.TruckModel
import com.immon.truckorbit.data.models.UserModel
import com.immon.truckorbit.databinding.FragmentDashboardBinding
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import com.immon.truckorbit.utils.setTitle

class DashboardFragment : BaseFragment() {

    private lateinit var binding: FragmentDashboardBinding
    private val firestore = FirebaseFirestore.getInstance()

    override val isLightStatusbar: Boolean
        get() = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        binding.header.toolbar.setTitle(requireContext(), "Dashboard", true)

        fetchTruckData()
        fetchUserData()

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    private fun fetchTruckData() {
        firestore.collection(TRUCK_DATABASE)
            .get()
            .addOnSuccessListener { result ->
                val trucks = result.toObjects(TruckModel::class.java)
                val totalTrucks = trucks.size
                val movingTrucks = trucks.count { it.drivingStatus == DrivingStatusModel.DRIVING }
                val idleTrucks = trucks.count { it.drivingStatus == DrivingStatusModel.IDLE }
                val stoppedTrucks = trucks.count { it.drivingStatus == DrivingStatusModel.STOPPED }

                binding.tvTotalTrucks.text = totalTrucks.toString()
                binding.tvMovingTrucks.text = movingTrucks.toString()
                binding.tvIdleTrucks.text = idleTrucks.toString()
                binding.tvStoppedTrucks.text = stoppedTrucks.toString()
            }
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Error fetching trucks", e)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchUserData() {
        firestore.collection(USER_DATABASE)
            .get()
            .addOnSuccessListener { result ->
                val users = result.toObjects(UserModel::class.java)
                val totalUsers = users.size
                val totalAdmins = users.count { it.accountType == AccountTypeModel.ADMIN }
                val totalDrivers = users.count { it.accountType == AccountTypeModel.DRIVER }
                val activeAccounts = users.count { it.accountStatus == AccountStatusModel.ACTIVE }
                val inactiveAccounts =
                    users.count { it.accountStatus == AccountStatusModel.INACTIVE }

                binding.tvTotalUsers.text = totalUsers.toString()
                binding.tvTotalAdmins.text = totalAdmins.toString()
                binding.tvTotalDrivers.text = totalDrivers.toString()
                binding.tvActiveAccounts.text = activeAccounts.toString()
                binding.tvInactiveAccounts.text = inactiveAccounts.toString()
            }
            .addOnFailureListener { e ->
                Log.e("DashboardFragment", "Error fetching users", e)
            }
    }
}
