package com.appacoustic.cointester.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.appacoustic.cointester.R

class TutorialFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_tutorial,
            container,
            false
        )
    }

    companion object {
        fun newInstance(): TutorialFragment {
            return TutorialFragment()
        }
    }
}
