package com.camera.camera;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.camera.camera.ui.base.BaseFragment;

public class Photo extends BaseFragment {

    private PhotoViewModel mViewModel;

    public static Photo newInstance() {
        return new Photo();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.photo_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(PhotoViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override
    public String getName() {
        return "Фото";
    }
}
