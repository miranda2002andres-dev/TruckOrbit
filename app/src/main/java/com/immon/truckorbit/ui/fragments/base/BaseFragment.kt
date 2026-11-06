package com.immon.truckorbit.ui.fragments.base

import androidx.fragment.app.Fragment
import com.immon.truckorbit.utils.setLightStatusBar

abstract class BaseFragment : Fragment() {

    abstract val isLightStatusbar: Boolean

    override fun onResume() {
        super.onResume()

        requireActivity().window.setLightStatusBar(isLightStatusbar)
    }
}
