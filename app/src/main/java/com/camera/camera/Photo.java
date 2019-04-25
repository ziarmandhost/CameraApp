package com.camera.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.camera.camera.ui.AutoFitTextureView;
import com.camera.camera.ui.base.BaseFragment;
import com.camera.camera.ui.sharedpreferences.ISharedPreferences;
import com.camera.camera.ui.OrientationListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Photo extends BaseFragment {
    Activity a;

    private AutoFitTextureView photoPreviewContainer;
    private CameraManager cm;

    private Size previewSize;

    private CameraDevice cameraDevice;
    private CameraCharacteristics cc;
    private String cameraID;

    private boolean cameraFront = false;
    protected int sensorOrientation;

    private int surfaceWidth = 1920;
    private int surfaceHeight = 1080;

    //Zoom
    public float fingerSpacing = 0;
    public double zoomLevel = 1;
    public float maximumZoomLevel;
    public Rect zoom;

    // For "CameraCaptureSession.CaptureCallback"
    private int state = STATE_PREVIEW;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    // For detecting phone orientation
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;

    private ImageReader imageReader;
    private File galleryFolder;

    private boolean flashSupported;
    FileOutputStream outputPhoto = null;

    ISharedPreferences preferences;
    OrientationListener mOrientationListener;

    private int orient;

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            surfaceWidth = width;
            surfaceHeight = height;
            setUpCamera();
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (state) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    }
                    else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_INACTIVE == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            state = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        }
                        else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        state = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        state = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

    };
    private ImageView gridlines;

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.photo_fragment, container, false);
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setTabsStyle();
        hideTimer();
        ((ImageView)a.findViewById(R.id.capture)).setImageResource(R.drawable.ic_shutter);
        gridlines = a.findViewById(R.id.gridlines);

        preferences = new ISharedPreferences(a);

        if (preferences.getGridlinesShow()) {
            showGrid();
        }
        else {
            hideGrid();
        }

        mOrientationListener = new OrientationListener(getContext()) {

            @Override
            public void onSimpleOrientationChanged(int orientation) {
                if(orientation == Configuration.ORIENTATION_LANDSCAPE){
                    orient = 1;
                }
                else if(orientation == Configuration.ORIENTATION_PORTRAIT){
                    orient = 0;
                }
            }
        };
        mOrientationListener.enable();

        startCamera();
    }



    private void setTabsStyle() {
        a = getActivity();
        TextView tabTextPhoto = a.findViewById(R.id.photoFrg);
        TextView tabTextVideo = a.findViewById(R.id.videoFrg);
        TextView tabTextQrcode = a.findViewById(R.id.qrcodeFrg);

        tabTextVideo.setTextColor(Color.WHITE);
        tabTextVideo.setTypeface(null, Typeface.NORMAL);

        tabTextQrcode.setTextColor(Color.WHITE);
        tabTextQrcode.setTypeface(null, Typeface.NORMAL);

        // Special styles of text btn to show this fragment
        tabTextPhoto.setTextColor(Color.RED);
        tabTextPhoto.setTypeface(null, Typeface.BOLD);
    }
    private void hideTimer() {
        View counter = a.findViewById(R.id.counter);
        View hdrDiv = a.findViewById(R.id.hdrDiv);
        View flashDiv = a.findViewById(R.id.flashDiv);

        hdrDiv.setVisibility(View.VISIBLE);
        flashDiv.setVisibility(View.VISIBLE);
        counter.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        zoom = null;
        openBackgroundThread();
        if (photoPreviewContainer.isAvailable()) {
            setUpCamera();
            openCamera(surfaceWidth, surfaceHeight);
        }
        else {
            photoPreviewContainer.setSurfaceTextureListener(surfaceTextureListener);
        }
    }
    @Override
    public void onStop() {
        closeCamera();
        closeBackgroundThread();

        mOrientationListener.disable();

        super.onStop();
    }

    @SuppressLint("ClickableViewAccessibility") public void startCamera () {
        photoPreviewContainer = getView().findViewById(R.id.photoPreviewContainer);
        photoPreviewContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return Photo.this.onTouch(v, event);
            }
        });
        assert photoPreviewContainer != null;
        photoPreviewContainer.setSurfaceTextureListener(surfaceTextureListener);
    }

    @Override
    public void showGrid() {
        gridlines.setVisibility(View.VISIBLE);
    }
    @Override
    public void hideGrid() {
        gridlines.setVisibility(View.INVISIBLE);
    }

    @Override
    public void changeCamera() {
        cameraFront = !cameraFront;

        closeCamera();
        closeBackgroundThread();

        setUpCamera();
        openCamera(surfaceWidth, surfaceHeight);
    }

    private void setUpCamera() {
        cm = (CameraManager) a.getSystemService(Context.CAMERA_SERVICE);

        try {
            int type = cameraFront ? 1 : 0; // 1 => front camera , 0 => back camera
            cameraID = cm.getCameraIdList()[type];

            cc = cm.getCameraCharacteristics(cameraID); // характеристики камеры по ее ID

            StreamConfigurationMap streamConfigs = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
           assert streamConfigs != null;

            Size largest = Collections.max(Arrays.asList(streamConfigs.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
            previewSize = chooseOptimalSize(streamConfigs.getOutputSizes(SurfaceTexture.class), surfaceWidth, surfaceHeight, largest);

            photoPreviewContainer.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());

            Boolean available = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            flashSupported = available == null ? false : available;
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else {
            return choices[0];
        }
    }

    @Override public void turnOnFlashLight() {
        try {
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override public void turnOffFlashLight() {
        try {
            captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void turnHdrOn () {
        captureRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_HDR);
    }
    @Override public void turnHdrOff () {
        captureRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_DISABLED);
    }

    private void openCamera(int width, int height) {
        configureTransform(width, height);
        try {
            if (ActivityCompat.checkSelfPermission(a, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cm.openCamera(cameraID, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        createPreviewSession();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        cameraDevice.close();
                        cameraDevice = null;
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        cameraDevice.close();
                        cameraDevice = null;
                    }
                }, mBackgroundHandler);
            }
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == photoPreviewContainer || null == activity) {
            return;
        }

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();

        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());

        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) viewHeight / previewSize.getHeight(), (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        photoPreviewContainer.setTransform(matrix);
    }

    private void createPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = photoPreviewContainer.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(previewSurface);

            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, colorTemperature(CaptureRequest.CONTROL_AWB_MODE_AUTO));

            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT);
            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, colorTemperature(CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT));

            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, colorTemperature(CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT));

            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT);
            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, colorTemperature(CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT));

            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
            captureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, colorTemperature(CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT));

            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
            Range<Integer> range2 = cc.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            int iso = ((200 * (200 - 100)) / 100 + 100);
            if (range2 != null) {
                int max1 = range2.getUpper();//10000
                int min1 = range2.getLower();//100
                iso = ((max1 * (max1 - min1)) / 100 + min1);
            }
            captureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);

            if (zoom != null) { captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom); }

            captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 100);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession captureSession) {
                            if (cameraDevice == null) {
                                return;
                            }

                            try {
                                captureRequest = captureRequestBuilder.build();
                                cameraCaptureSession = captureSession;
                                cameraCaptureSession.setRepeatingRequest(captureRequest, null, mBackgroundHandler);
                            }
                            catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession captureSession) {

                        }
                    }, mBackgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public RggbChannelVector colorTemperature(int whiteBalance) {
        float temperature = whiteBalance / 100;
        float red;
        float green;
        float blue;

        //Calculate red
        if (temperature <= 66)
            red = 255;
        else {
            red = temperature - 60;
            red = (float) (329.698727446 * (Math.pow((double) red, -0.1332047592)));
            if (red < 0)
                red = 0;
            if (red > 255)
                red = 255;
        }

        if (temperature <= 66) {
            green = temperature;
            green = (float) (99.4708025861 * Math.log(green) - 161.1195681661);
            if (green < 0)
                green = 0;
            if (green > 255)
                green = 255;
        } else {
            green = temperature - 60;
            green = (float) (288.1221695283 * (Math.pow((double) green, -0.0755148492)));
            if (green < 0)
                green = 0;
            if (green > 255)
                green = 255;
        }

        if (temperature >= 66)
            blue = 255;
        else if (temperature <= 19)
            blue = 0;
        else {
            blue = temperature - 10;
            blue = (float) (138.5177312231 * Math.log(blue) - 305.0447927307);
            if (blue < 0)
                blue = 0;
            if (blue > 255)
                blue = 255;
        }
        return new RggbChannelVector((red / 255) * 2, (green / 255), (green / 255), (blue / 255) * 2);
    }

    private void createImageGallery() {
        File storageDirectory = null;
        if (preferences.getSaveToSD()) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                storageDirectory = Environment.getExternalStorageDirectory();
            }
            else {
                Toast.makeText(a, "Cant write to the SD card!", Toast.LENGTH_LONG).show();
            }

        }
        else {
            storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        }

        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name)+"/Photo");

        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("ID", "Failed to create directory");
            }
        }
    }
    private File createImageFile(File galleryFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "image_" + timeStamp + "_";
        return File.createTempFile(imageFileName, ".jpg", galleryFolder);
    }

    private void openBackgroundThread() {
        mBackgroundThread = new HandlerThread("camera_background_thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void captureStillPicture() {
        try {
            if (null == cameraDevice) {
                return;
            }

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            // Orientation
            int rotation = photoPreviewContainer.getDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            //Zoom
            if (zoom != null) {
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            }

            CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    unlock();
                }
            };

            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.abortCaptures();
            cameraCaptureSession.capture(captureBuilder.build(), captureCallback, null);
        }
        catch (CameraAccessException e) {
           e.printStackTrace();
        }
    }
    private void runPrecaptureSequence() {
        try {
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            state = STATE_WAITING_PRECAPTURE;
            cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, mBackgroundHandler);
        }
        catch (CameraAccessException e) {
           e.printStackTrace();
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        assert cc != null;
        maximumZoomLevel = (cc.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM))*16;

        Rect m = cc.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int action = event.getAction();
        float current_finger_spacing;

        if (event.getPointerCount() > 1) {
            // Multi touch
            current_finger_spacing = getFingerSpacing(event);
            if(fingerSpacing != 0){
                if(current_finger_spacing > fingerSpacing && maximumZoomLevel > zoomLevel){
                    zoomLevel+=1;
                }
                else if (current_finger_spacing < fingerSpacing && zoomLevel > 1){
                    zoomLevel-=1;
                }
                int minW = (int) (m.width() / maximumZoomLevel);
                int minH = (int) (m.height() / maximumZoomLevel);
                int difW = m.width() - minW;
                int difH = m.height() - minH;
                int cropW = difW /100 *(int)zoomLevel;
                int cropH = difH /100 *(int)zoomLevel;
                cropW -= cropW & 3;
                cropH -= cropH & 3;
                Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
                captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            }
            fingerSpacing = current_finger_spacing;
        }
        else {
            // Single touch

        }

        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, null);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
        catch (NullPointerException z) {
            z.printStackTrace();
        }

        return true;
    }

    private void closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        } // Not "else if", we need to conditions
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
    private void closeBackgroundThread() {
        if (mBackgroundHandler != null) {
            mBackgroundThread.quitSafely();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
    }

    private void lock() {
        try {
            cameraCaptureSession.capture(captureRequestBuilder.build(), null, mBackgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void unlock() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onShutter() {
        lock();
        createImageGallery();

        try {
            File img = createImageFile(galleryFolder);
            outputPhoto = new FileOutputStream(img);

            Bitmap bitmap = photoPreviewContainer.getBitmap();

            if (orient == 1) {
                rotateBitmap(bitmap, 90).compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);
            }
            else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);
            }

            Toast.makeText(getContext(), "Saved", Toast.LENGTH_LONG).show();

//            final Bitmap bitmap = photoPreviewContainer.getBitmap();
//            Toast.makeText(getContext(), "Saved", Toast.LENGTH_LONG).show();
//            rotateBitmap(bitmap, 90).compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);
            //photoPreviewContainer.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, outputPhoto);
//            photoPreviewContainer.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto);
//            Toast.makeText(getContext(), "Saved", Toast.LENGTH_LONG).show();

            MediaScannerConnection.scanFile(getActivity(), new String[]{img.getAbsolutePath()}, null, new MediaScannerConnection.MediaScannerConnectionClient() {
                @Override
                public void onMediaScannerConnected() {

                }

                @Override
                public void onScanCompleted(String path, Uri uri) {
                    Log.d("Scan","Scanning Completed");
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            unlock();
            try {
                if (outputPhoto != null) {
                    outputPhoto.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setFilter(int filter) {
        try {
            captureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, filter);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private int getScreenOrientation() {
//        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
//        DisplayMetrics dm = new DisplayMetrics();
//        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
//        int width = dm.widthPixels;
//        int height = dm.heightPixels;
//        int orientation;
//        // if the device's natural orientation is portrait:
//        if ((rotation == Surface.ROTATION_0
//                || rotation == Surface.ROTATION_180) && height > width ||
//                (rotation == Surface.ROTATION_90
//                        || rotation == Surface.ROTATION_270) && width > height) {
//            switch(rotation) {
//                case Surface.ROTATION_0:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//                    break;
//                case Surface.ROTATION_90:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//                    break;
//                case Surface.ROTATION_180:
//                    orientation =
//                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
//                    break;
//                case Surface.ROTATION_270:
//                    orientation =
//                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
//                    break;
//                default:
//                    Log.e("ID", "Unknown screen orientation. Defaulting to " +
//                            "portrait.");
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//                    break;
//            }
//        }
//        // if the device's natural orientation is landscape or if the device
//        // is square:
//        else {
//            switch(rotation) {
//                case Surface.ROTATION_0:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//                    break;
//                case Surface.ROTATION_90:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//                    break;
//                case Surface.ROTATION_180:
//                    orientation =
//                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
//                    break;
//                case Surface.ROTATION_270:
//                    orientation =
//                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
//                    break;
//                default:
//                    Log.e("ID", "Unknown screen orientation. Defaulting to " +
//                            "landscape.");
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//                    break;
//            }
//        }
//
//        return orientation;
//    }

    public Bitmap rotateBitmap (Bitmap bitmap, int deg) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Matrix matrix = new Matrix();
        matrix.postRotate(deg);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,width, height, matrix, true);
        return resizedBitmap;
    }

    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + sensorOrientation + 270) % 360;
    }
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    @Override public String getName() {
        return "Фото";
    }
}

