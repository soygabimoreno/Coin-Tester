package com.appacoustic.cointester;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.appacoustic.cointester.domain.Coin;

import java.util.ArrayList;
import java.util.List;

public class CoinsFragment extends Fragment {

    private OnListFragmentInteractionListener listener;

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(Coin item);
    }

    public CoinsFragment() {
    }

    public static CoinsFragment newInstance() {
        return new CoinsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_coins, container, false);
        RecyclerView recyclerView = (RecyclerView) rootView;
        List<Coin> list = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                list.add(new Coin("Peseta", "España", R.drawable.peseta));
            } else {
                list.add(new Coin("Lunar", "Australia", R.drawable.lunar));
            }
        }
        recyclerView.setAdapter(new CoinsAdapter(list, listener));
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        listener = (OnListFragmentInteractionListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}