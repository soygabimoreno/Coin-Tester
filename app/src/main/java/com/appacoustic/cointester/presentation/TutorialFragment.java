package com.appacoustic.cointester.presentation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.appacoustic.cointester.R;

public class TutorialFragment extends Fragment {

    public TutorialFragment() {
    }

    public static TutorialFragment newInstance() {
        return new TutorialFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutorial, container, false);
    }
}
