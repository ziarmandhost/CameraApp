package com.camera.camera;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.camera.camera.ui.base.BaseFragment;

public class Photo extends BaseFragment {

    private PhotoViewModel mViewModel;
    private FrameLayout preview;
    private Camera mCamera;
    private Preview mPreview;

    public static Photo newInstance() {
        return new Photo();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(PhotoViewModel.class);

        mCamera = getCameraInstance();
        mPreview = new Preview(getContext(), mCamera);
        preview = getView().findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
        }
        return c;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.photo_fragment, container, false);
    }

    @Override
    public String getName() {
        return "Фото";
    }
}
