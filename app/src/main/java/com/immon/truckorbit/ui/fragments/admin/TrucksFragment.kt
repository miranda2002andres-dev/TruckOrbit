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
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.immon.truckorbit.R
import com.immon.truckorbit.data.Constants.TRUCK_DATABASE
import com.immon.truckorbit.data.models.TruckModel
import com.immon.truckorbit.databinding.FragmentTrucksBinding
import com.immon.truckorbit.ui.adapters.TruckAdapter
import com.immon.truckorbit.ui.fragments.admin.MainFragment.Companion.replaceFragment
import com.immon.truckorbit.ui.fragments.base.BaseFragment
import com.immon.truckorbit.utils.setTitle

@Suppress("DEPRECATION")
class TrucksFragment : BaseFragment() {

    private lateinit var binding: FragmentTrucksBinding
    private lateinit var truckAdapter: TruckAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var truckList = mutableListOf<TruckModel>()

    override val isLightStatusbar: Boolean
        get() = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrucksBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        binding.header.toolbar.setTitle(requireContext(), "Trucks", false)

        binding.tvNoTrucks.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE

        binding.btnAddTruck.setOnClickListener {
            binding.btnAddTruck.startAnimation {
                saveNewTruckToFirestore()
            }
        }

        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTrucks(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        truckAdapter = TruckAdapter(emptyList()) {
            replaceFragment(TruckInfoFragment().apply {
                arguments = Bundle().apply {
                    putString("truckId", it)
                }
            })
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = truckAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeTrucksFromFirestore()
    }

    private fun observeTrucksFromFirestore() {
        firestore.collection(TRUCK_DATABASE).addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                exception.printStackTrace()
                return@addSnapshotListener
            }

            if (snapshots != null) {
                truckList.clear()
                for (document in snapshots) {
                    document.toObject<TruckModel>().let { truckList.add(it) }
                }
                truckAdapter.updateData(truckList)

                if (truckList.isEmpty()) {
                    binding.tvNoTrucks.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.tvNoTrucks.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun saveNewTruckToFirestore() {
        val truckCompany = binding.etTruckCompany.text.toString().trim()
        val licensePlate = binding.etLicensePlate.text.toString().trim()

        if (truckCompany.isEmpty() || licensePlate.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please fill in all fields",
                Toast.LENGTH_SHORT
            ).show()
            binding.btnAddTruck.revertAnimation()
            return
        }

        val newTruck = TruckModel(
            truckName = truckCompany,
            licenseNo = licensePlate
        )

        firestore.collection(TRUCK_DATABASE).document(newTruck.id).set(newTruck)
            .addOnSuccessListener {
                binding.btnAddTruck.revertAnimation()
                binding.etTruckCompany.text.clear()
                binding.etLicensePlate.text.clear()

                Toast.makeText(
                    requireContext(),
                    "Truck added successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->
                binding.btnAddTruck.revertAnimation()
                Toast.makeText(requireContext(), exception.message, Toast.LENGTH_SHORT).show()
                exception.printStackTrace()
            }
    }

    private fun filterTrucks(query: String) {
        if (query.isEmpty()) {
            truckAdapter.updateData(truckList)
        } else {
            val filteredList = truckList.filter {
                it.truckName.contains(query, ignoreCase = true) ||
                        it.licenseNo.contains(query, ignoreCase = true)
            }
            truckAdapter.updateData(filteredList)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_corner_menu_admin_trucks, menu)
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
            R.id.add -> {
                binding.addTruckContainer.visibility =
                    if (binding.addTruckContainer.visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                binding.searchTruckContainer.visibility = View.GONE
            }

            R.id.search -> {
                binding.searchTruckContainer.visibility =
                    if (binding.searchTruckContainer.visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                binding.addTruckContainer.visibility = View.GONE
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
