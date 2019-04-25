package com.camera.camera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
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
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
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
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.camera.camera.ui.AutoFitTextureView;
import com.camera.camera.ui.base.BaseFragment;
import com.camera.camera.ui.sharedpreferences.ISharedPreferences;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Video extends BaseFragment {
    private Activity a;
    private Chronometer timer;
    ImageView gridlines;
    ISharedPreferences preferences;

    private AutoFitTextureView videoPreviewContainer;

    private Size previewSize;
    private Size videoSize;

    private CameraDevice cameraDevice;
    private String cameraId;

    private boolean cameraFront = false;

    //Zoom
    public float fingerSpacing = 0;
    public double zoomLevel = 1;
    public float maximumZoomLevel;
    public Rect zoom;

    private MediaRecorder mediaRecorder;

    private boolean isRecordingVideo;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder captureRequestBuilder;

    private String nextVideoAbsolutePath;
    private File galleryFolder;

    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    @Override
    public void setFilter(int filter) {

    }

    private Integer sensorOrientation;

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private int savedWidth = 1920;

    @Override
    public void showGrid() {

    }
    @Override
    public void hideGrid() {
        gridlines.setVisibility(View.INVISIBLE);
    }

    private int savedHeight = 1080;

    @Override
    public void turnHdrOn() {

    }

    @Override
    public void turnHdrOff() {

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

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            savedWidth = width;
            savedHeight = height;
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camDevice) {
            cameraDevice = camDevice;
            startPreview();
            cameraOpenCloseLock.release();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camDevice) {
            cameraOpenCloseLock.release();
            camDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camDevice, int error) {
            cameraOpenCloseLock.release();
            camDevice.close();
            cameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.video_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setTabsStyle();
        showTimer();
        ((ImageView) a.findViewById(R.id.capture)).setImageResource(R.drawable.shutter3b_video);
        gridlines = a.findViewById(R.id.gridlines);
        preferences = new ISharedPreferences(getActivity());
        hideGrid();

        View filterBar = a.findViewById(R.id.filterBar);
        filterBar.setVisibility(View.GONE);

        videoPreviewContainer = getView().findViewById(R.id.videoView);
        videoPreviewContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return Video.this.onTouch(v, event);
            }
        });
    }

    private void setTabsStyle() {
        a = getActivity();
        TextView tabTextPhoto = a.findViewById(R.id.photoFrg);
        TextView tabTextVideo = a.findViewById(R.id.videoFrg);
        TextView tabTextQrcode = a.findViewById(R.id.qrcodeFrg);

        tabTextPhoto.setTextColor(Color.WHITE);
        tabTextPhoto.setTypeface(null, Typeface.NORMAL);

        tabTextQrcode.setTextColor(Color.WHITE);
        tabTextQrcode.setTypeface(null, Typeface.NORMAL);

        // Special styles of text btn to show this fragment
        tabTextVideo.setTextColor(Color.RED);
        tabTextVideo.setTypeface(null, Typeface.BOLD);
    }

    private void showTimer() {
        View counter = a.findViewById(R.id.counter);
        View hdrDiv = a.findViewById(R.id.hdrDiv);
        View flashDiv = a.findViewById(R.id.flashDiv);
        timer = a.findViewById(R.id.timer);

        hdrDiv.setVisibility(View.GONE);
        flashDiv.setVisibility(View.VISIBLE);
        counter.setVisibility(View.VISIBLE);
    }


    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (videoPreviewContainer.isAvailable()) {
            openCamera(videoPreviewContainer.getWidth(), videoPreviewContainer.getHeight());
        }
        else {
            videoPreviewContainer.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @SuppressWarnings("MissingPermission")
    private void openCamera(int width, int height) {
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }

        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            int type = cameraFront ? 1 : 0; // 1 => front camera , 0 => back camera
            cameraId = manager.getCameraIdList()[type];

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }

            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, videoSize);

            videoPreviewContainer.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());

            mediaRecorder = new MediaRecorder();

            if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, null);
            }

        }
        catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera!", Toast.LENGTH_LONG).show();
            activity.finish();
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            activity.finish();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != mediaRecorder) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        }
        finally {
            cameraOpenCloseLock.release();
        }
    }

    private void startPreview() {
        if (null == cameraDevice || !videoPreviewContainer.isAvailable() || null == previewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = videoPreviewContainer.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            captureRequestBuilder.addTarget(previewSurface);

            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());

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
            Range<Integer> range2 = ((CameraManager) a.getSystemService(Context.CAMERA_SERVICE)).getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
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
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    }, mBackgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void updatePreview() {
        if (null == cameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(captureRequestBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closePreviewSession() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
    }

    private void startRecordingVideo() {
        if (null == cameraDevice || !videoPreviewContainer.isAvailable() || null == previewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();

            SurfaceTexture texture = videoPreviewContainer.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            captureRequestBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            captureRequestBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession camCaptureSession) {
                    cameraCaptureSession = camCaptureSession;
                    updatePreview();

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ((ImageView) a.findViewById(R.id.capture)).setImageResource(R.drawable.shutter3b_playing);
                                isRecordingVideo = true;

                                if (preferences.getFlashOn()) {
                                    turnOnFlashLight();
                                }
                                Thread.sleep(350);

                                mediaRecorder.start();

                                timer.setBase(SystemClock.elapsedRealtime());
                                timer.start();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession camCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_LONG).show();
                    }
                }
            }, mBackgroundHandler);
        }
        catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }
    private void stopRecordingVideo() {
        Activity activity = getActivity();

        timer.stop();
        timer.setBase(SystemClock.elapsedRealtime());

        isRecordingVideo = false;
        ((ImageView) activity.findViewById(R.id.capture)).setImageResource(R.drawable.shutter3b_video);

        mediaRecorder.stop();
        mediaRecorder.reset();

        if (null != activity) {
            Toast.makeText(activity, "Video saved", Toast.LENGTH_SHORT).show();
        }
        nextVideoAbsolutePath = null;
        startPreview();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("VideoCameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder capReqBuilder) {
        capReqBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);

        mediaRecorder.reset();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        if (nextVideoAbsolutePath == null || nextVideoAbsolutePath.isEmpty()) {
            nextVideoAbsolutePath = getVideoFilePath();
        }

        mediaRecorder.setOutputFile(nextVideoAbsolutePath);
        mediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (sensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }

        mediaRecorder.prepare();
    }

    private String getVideoFilePath() {
        final File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        galleryFolder = new File(storageDirectory, getResources().getString(R.string.app_name)+"/Video");

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "video_" + timeStamp + "_";

        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }

        return (galleryFolder == null ? "" : (galleryFolder.getAbsolutePath() + "/")) + imageFileName + ".mp4";
    }

    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {

        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        }
        else {
            Log.e("ID", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private Range<Integer> getRange() {
        CameraCharacteristics chars = null;
        try {
            chars = ((CameraManager) a.getSystemService(Context.CAMERA_SERVICE)).getCameraCharacteristics(cameraId);
            Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            Range<Integer> result = null;
            for (Range<Integer> range : ranges) {
                int upper = range.getUpper();
                // 10 - min range upper for my needs
                if (upper >= 10) {
                    if (result == null || upper < result.getUpper().intValue()) {
                        result = range;
                    }
                }
            }
            if (result == null) {
                result = ranges[0];
            }
            return result;
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
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

    public boolean onTouch(View v, MotionEvent event) {
        CameraCharacteristics cc = null;
        try {
            cc = ((CameraManager) a.getSystemService(Context.CAMERA_SERVICE)).getCameraCharacteristics(cameraId);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
        catch (NullPointerException z) {
            z.printStackTrace();
        }

        return true;
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
        if (isRecordingVideo) {
            stopRecordingVideo();
            MediaScannerConnection.scanFile(getActivity(), new String[]{getVideoFilePath()}, null, new MediaScannerConnection.MediaScannerConnectionClient() {
                @Override
                public void onMediaScannerConnected() {

                }

                @Override
                public void onScanCompleted(String path, Uri uri) {
                    Log.d("Scan","Scanning Completed");
                }
            });
        }
        else {
            startRecordingVideo();
        }
    }

    @Override
    public void changeCamera() {
        if (isRecordingVideo) {
            Toast.makeText(getActivity(), "Stop video and then change camera!", Toast.LENGTH_LONG).show();
            return;
        }
        else {
            cameraFront = !cameraFront;

            closeCamera();
            stopBackgroundThread();

            openCamera(savedWidth, savedHeight);
        }
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
        return "Видео";
    }
}
