package com.camera.camera.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.WindowManager;

import java.util.concurrent.locks.ReentrantLock;

public abstract class OrientationListener extends OrientationEventListener {

    public static final int CONFIGURATION_ORIENTATION_UNDEFINED = Configuration.ORIENTATION_UNDEFINED;
    private volatile int defaultScreenOrientation = CONFIGURATION_ORIENTATION_UNDEFINED;
    public int prevOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private Context ctx;
    private ReentrantLock lock = new ReentrantLock(true);

    public OrientationListener(Context context) {
        super(context);
        ctx = context;
    }

    public OrientationListener(Context context, int rate) {
        super(context, rate);
        ctx = context;
    }

    @Override
    public void onOrientationChanged(final int orientation) {
        int currentOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
        if (orientation >= 330 || orientation < 30) {
            currentOrientation = Surface.ROTATION_0;
        }
        else if (orientation >= 60 && orientation < 120) {
            currentOrientation = Surface.ROTATION_90;
        }
        else if (orientation >= 150 && orientation < 210) {
            currentOrientation = Surface.ROTATION_180;
        }
        else if (orientation >= 240 && orientation < 300) {
            currentOrientation = Surface.ROTATION_270;
        }

        if (prevOrientation != currentOrientation && orientation != OrientationEventListener.ORIENTATION_UNKNOWN) {
            prevOrientation = currentOrientation;
            if (currentOrientation != OrientationEventListener.ORIENTATION_UNKNOWN)
                reportOrientationChanged(currentOrientation);
        }

    }

    private void reportOrientationChanged(final int currentOrientation) {

        int defaultOrientation = getDeviceDefaultOrientation();
        int orthogonalOrientation = defaultOrientation == Configuration.ORIENTATION_LANDSCAPE ? Configuration.ORIENTATION_PORTRAIT
                : Configuration.ORIENTATION_LANDSCAPE;

        int toReportOrientation;

        if (currentOrientation == Surface.ROTATION_0 || currentOrientation == Surface.ROTATION_180)
            toReportOrientation = defaultOrientation;
        else
            toReportOrientation = orthogonalOrientation;

        onSimpleOrientationChanged(toReportOrientation);
    }

    private int getDeviceDefaultOrientation() {
        if (defaultScreenOrientation == CONFIGURATION_ORIENTATION_UNDEFINED) {
            lock.lock();
            defaultScreenOrientation = initDeviceDefaultOrientation(ctx);
            lock.unlock();
        }
        return defaultScreenOrientation;
    }

    private int initDeviceDefaultOrientation(Context context) {

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Configuration config = context.getResources().getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();

        boolean isLand = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        boolean isDefaultAxis = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;

        int result = CONFIGURATION_ORIENTATION_UNDEFINED;
        if ((isDefaultAxis && isLand) || (!isDefaultAxis && !isLand)) {
            result = Configuration.ORIENTATION_LANDSCAPE;
        } else {
            result = Configuration.ORIENTATION_PORTRAIT;
        }
        return result;
    }

    public abstract void onSimpleOrientationChanged(int orientation);

}