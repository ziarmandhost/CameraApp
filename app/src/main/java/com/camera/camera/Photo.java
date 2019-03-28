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

    private boolean cameraFront = false;

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

    public Camera getCameraInstance(){
        Camera c = null;
        try {
//            c = Camera.open(findFrontFacingCamera()); // Фронталка
            c = Camera.open(findBackFacingCamera());
        }
        catch (Exception e){
            Log.d("ID", "Camera doesn't exist!");
        }
        return c;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    @Override
    public String getName() {
        return "Фото";
    }
}