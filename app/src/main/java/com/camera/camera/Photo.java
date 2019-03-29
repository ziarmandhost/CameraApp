package com.camera.camera;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.camera.camera.ui.base.BaseFragment;
import com.camera.camera.ui.components.CameraPreview;

public class Photo extends BaseFragment {
    private PhotoViewModel mViewModel;

    private Camera mCamera;
    private CameraPreview mPreview;
    FrameLayout preview;

    private Camera.PictureCallback mPicture;
    public static Bitmap bitmap;

    public boolean cameraFront = false;

    Camera.Parameters params;

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

        // CAMERA INIT
            mCamera = getCameraInstance();
            mCamera.setDisplayOrientation(90);

            params = mCamera.getParameters();

            // There are camera config
                setAutofocus();

            mPreview = new CameraPreview(getContext(), mCamera);
            preview = getView().findViewById(R.id.cameraPreview);
            preview.addView(mPreview);
            mCamera.startPreview();
        // CAMERA INIT END

    }

    @Override
    public void switchCamera() {
        releaseCamera();

        cameraFront = !cameraFront;
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);
        mPreview.refreshCamera(mCamera);
    }

    public void setAutofocus () {
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        mCamera.setParameters(params);
    }

    public Camera getCameraInstance(){
        Camera c = null;
        try {
            if (cameraFront) {
                c = Camera.open(findFrontFacingCamera());
            }
            else {
                c = Camera.open(findBackFacingCamera());
            }
        }
        catch (Exception e){
            Log.d("ID", "Camera doesn't exist!");
        }
        return c;
    }
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
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

    @Override public String getName() {
        return "Фото";
    }
}