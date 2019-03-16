package com.camera.camera;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.camera.camera.ui.base.BaseFragment;

public class Portret extends BaseFragment {

    private PortretViewModel mViewModel;

    public static Portret newInstance() {
        return new Portret();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.portret_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(PortretViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public String getName() {
        return "Портрет";
    }
}
