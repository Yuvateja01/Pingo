package com.ezerka.pingo.issues;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ezerka.pingo.R;

/**
 * Created by User on 4/16/2018.
 */

public class ProjectsFragment extends Fragment {

    private static final String TAG = "ProjectsFragment";

    //widgets

    //vars


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_projects, container, false);

        return view;
    }


}
















