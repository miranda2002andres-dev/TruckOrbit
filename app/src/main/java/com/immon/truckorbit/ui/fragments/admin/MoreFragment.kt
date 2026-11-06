package com.immon.truckorbit.ui.fragments.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.immon.truckorbit.data.Constants.DEV_HELP_SUPPORT
import com.immon.truckorbit.data.Constants.DEV_MAIL_ADDRESS
import com.immon.truckorbit.data.Constants.USER_DATABASE
import com.immon.truckorbit.data.LocalDB
import com.immon.truckorbit.data.models.UserModel
import com.immon.truckorbit.databinding.FragmentMoreBinding
import com.immon.truckorbit.ui.activities.MainActivity
import com.immon.truckorbit.ui.fragments.LandingFragment
import com.immon.truckorbit.ui.fragments.admin.MainFragment.Companion.replaceFragment
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import com.immon.truckorbit.utils.applyWindowInsets


class MoreFragment : BaseFragment() {

    private lateinit var binding: FragmentMoreBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var currentUserId = firebaseAuth.currentUser?.uid!!

    override val isLightStatusbar: Boolean
        get() = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMoreBinding.inflate(inflater, container, false)

        binding.profileLayout.applyWindowInsets(top = true, bottom = false)

        fetchUserInfo()

        binding.logoutLayout.setOnClickListener {
            LocalDB.putBoolean("logged_in", false)

            MainActivity.replaceFragment(LandingFragment())
        }

        binding.showIdleTrucksSwitch.isChecked = LocalDB.getBoolean("show_idle_trucks", true)
        binding.showIdleTrucksSwitch.setOnCheckedChangeListener { _, isChecked ->
            LocalDB.putBoolean("show_idle_trucks", isChecked)
        }
        binding.showIdleTrucksLayout.setOnClickListener {
            binding.showIdleTrucksSwitch.performClick()
        }

        binding.showAdminsSwitch.isChecked = LocalDB.getBoolean("show_admins", false)
        binding.showAdminsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LocalDB.putBoolean("show_admins", isChecked)
        }
        binding.showAdminsLayout.setOnClickListener {
            binding.showAdminsSwitch.performClick()
        }

        binding.dashboardLayout.setOnClickListener {
            replaceFragment(DashboardFragment())
        }

        binding.reportIssueLayout.setOnClickListener {
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("mailto:$DEV_MAIL_ADDRESS")).apply {
                        putExtra(Intent.EXTRA_SUBJECT, "Bug report for Truck Orbit")
                        putExtra(Intent.EXTRA_TEXT, "Please describe the bug here")
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to send mail", Toast.LENGTH_SHORT).show()
            }
        }

        binding.privacyLayout.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://www.privacypolicies.com/generic/")))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }

        binding.helpLayout.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(DEV_HELP_SUPPORT)))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun fetchUserInfo() {
        firestore.collection(USER_DATABASE).document(currentUserId).get()
            .addOnSuccessListener { result ->
                val user = result.toObject<UserModel>()

                binding.profileName.text = user?.name ?: "John Doe"
                binding.profileEmail.text = user?.email ?: "mymail@yourmail.com"
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }
}
