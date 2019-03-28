package com.camera.camera;

import android.arch.lifecycle.ViewModelProviders;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.camera.camera.ui.base.BaseFragment;
import com.camera.camera.ui.components.CameraPreview;

public class Photo extends BaseFragment {
    private PhotoViewModel mViewModel;

    private Camera mCamera;
    private CameraPreview mPreview;
    FrameLayout preview;

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

        mCamera = getCameraInstance();
        mPreview = new CameraPreview(getContext(), mCamera);
        preview = (FrameLayout) getView().findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            Log.d("ID", "Camera doesn't exist!");
        }
        return c;
    }

    @Override
    public String getName() {
        return "Фото";
    }
}