package com.dreambig.app4it.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.dreambig.app4it.R;

public class LoadingFragment extends Fragment {
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
    	return inflater.inflate(R.layout.loading_fragment, container, false);
    }



           
}
