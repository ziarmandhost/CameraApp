package com.camera.camera;

import android.arch.lifecycle.ViewModelProviders;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.camera.camera.ui.base.BaseFragment;

public class Video extends BaseFragment {
    private VideoViewModel mViewModel;
    private Camera mCamera;

    @Override
    public void switchCamera() {

    }

    public boolean cameraFront = false;

    public static Video newInstance() {
        return new Video();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.video_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(VideoViewModel.class);
        // TODO: Use the ViewModel
    }

    @Override public String getName() {
        return "Видео";
    }
    public Camera getCamera() {
        return mCamera;
    }
}
