package com.immon.truckorbit.ui.fragments.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.immon.truckorbit.R
import com.immon.truckorbit.databinding.FragmentMainBinding
import com.immon.truckorbit.ui.fragments.base.BaseFragment

class MainFragment : BaseFragment() {

    private lateinit var binding: FragmentMainBinding

    override val isLightStatusbar: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MonitoringFragment())
                .commit()
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_monitoring -> MonitoringFragment()
                R.id.nav_trucks -> TrucksFragment()
                R.id.nav_history -> HistoryFragment() // Nueva pantalla
                R.id.nav_drivers -> DriversFragment()
                R.id.nav_more -> MoreFragment()
                else -> MonitoringFragment()
            }

            childFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()

            true
        }

        return binding.root
    }

    companion object {
        fun replaceFragment(fragment: Fragment) {
            com.immon.truckorbit.ui.activities.MainActivity.replaceFragment(fragment)
        }
    }
}