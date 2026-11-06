package com.immon.truckorbit.ui.fragments.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.immon.truckorbit.R
import com.immon.truckorbit.data.Constants.USER_DATABASE
import com.immon.truckorbit.data.LocalDB
import com.immon.truckorbit.data.enums.AccountTypeModel
import com.immon.truckorbit.data.models.UserModel
import com.immon.truckorbit.databinding.FragmentDriversBinding
import com.immon.truckorbit.ui.adapters.DriverAdapter
import com.immon.truckorbit.ui.fragments.admin.MainFragment.Companion.replaceFragment
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import com.immon.truckorbit.utils.setTitle

@Suppress("DEPRECATION")
class DriversFragment : BaseFragment() {

    private lateinit var binding: FragmentDriversBinding
    private lateinit var driverAdapter: DriverAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var driverList = mutableListOf<UserModel>()

    override val isLightStatusbar: Boolean
        get() = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDriversBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        binding.header.toolbar.setTitle(requireContext(), "Drivers", false)

        binding.tvNoDrivers.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE

        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDrivers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        driverAdapter = DriverAdapter(driverList = emptyList()) {
            replaceFragment(DriverInfoFragment().apply {
                arguments = Bundle().apply {
                    putString("driverId", it)
                }
            })
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = driverAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeDriversFromFirestore()
    }

    private fun observeDriversFromFirestore() {
        firestore.collection(USER_DATABASE).addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                exception.printStackTrace()
                return@addSnapshotListener
            }

            if (snapshots != null) {
                driverList.clear()
                val showAdmins = LocalDB.getBoolean("show_admins", false)

                for (document in snapshots) {
                    val user = document.toObject<UserModel>()
                    if (showAdmins || user.accountType == AccountTypeModel.DRIVER) {
                        driverList.add(user)
                    }
                }
                driverAdapter.updateData(driverList)

                if (driverList.isEmpty()) {
                    binding.tvNoDrivers.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.tvNoDrivers.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun filterDrivers(query: String) {
        if (query.isEmpty()) {
            driverAdapter.updateData(driverList)
        } else {
            val filteredList = driverList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }
            driverAdapter.updateData(filteredList)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_corner_menu_admin_drivers, menu)
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
            R.id.search -> {
                binding.searchDriverContainer.visibility =
                    if (binding.searchDriverContainer.visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
