package com.appacoustic.cointester.presentation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.appacoustic.cointester.R
import com.appacoustic.cointester.aaa.CoinsAdapter
import com.appacoustic.cointester.coredomain.Coin
import java.util.*

class CoinsFragment : Fragment() {
    private var listener: OnListFragmentInteractionListener? = null

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: Coin)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(
            R.layout.fragment_coins,
            container,
            false
        )
        val recyclerView = rootView as RecyclerView
        val list: MutableList<Coin> = ArrayList(20)
        for (i in 0..19) {
            if (i % 2 == 0) {
                list.add(
                    Coin(
                        "Peseta",
                        "España",
                        R.drawable.peseta
                    )
                )
            } else {
                list.add(
                    Coin(
                        "Lunar",
                        "Australia",
                        R.drawable.lunar
                    )
                )
            }
        }
        recyclerView.adapter = CoinsAdapter(
            list,
            listener
        )
        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as OnListFragmentInteractionListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        fun newInstance(): CoinsFragment {
            return CoinsFragment()
        }
    }
}
