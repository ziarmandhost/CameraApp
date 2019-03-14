package com.camera.camera.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.camera.camera.R;
import com.camera.camera.ui.base.BaseFragment;

public class VioletFragment extends BaseFragment {

    private VioletViewModel mViewModel;

    public static VioletFragment newInstance() {
        return new VioletFragment();
    }

    @Override
    public String getName() {
        return "VioletFragment";
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.violet_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(VioletViewModel.class);
        // TODO: Use the ViewModel
    }

}
