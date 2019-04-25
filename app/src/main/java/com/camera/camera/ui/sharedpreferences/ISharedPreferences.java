package com.camera.camera.ui.sharedpreferences;

import android.content.Context;
import android.content.SharedPreferences;

public class ISharedPreferences implements ISharedPreferencesInterface {
    SharedPreferences prefs;

    boolean gridlinesShow = false;
    boolean shutterSound = false;
    boolean saveToSD = false;
    boolean soundInVideo = false;
    int shutterTimeOut = 0;
    boolean flash = false;
    boolean hdr = false;

    @Override
    public boolean getFlashOn() {
        return prefs.getBoolean("FLASH", flash);
    }

    @Override
    public void setFlashOn(boolean data) {
        prefs.edit().putBoolean("FLASH", data).apply();
    }

    boolean qrCodeMode = false;

    @Override
    public boolean getGridlinesShow() {
        return prefs.getBoolean("SHOW_GRID", gridlinesShow);
    }
    @Override
    public void setGridlinesShow(boolean data) {
        prefs.edit().putBoolean("SHOW_GRID", data).apply();
    }

    @Override
    public boolean getShutterSound() {
        return prefs.getBoolean("SHUTTER_SOUND", shutterSound);
    }
    @Override
    public void setShutterSound(boolean data) {
        prefs.edit().putBoolean("SHUTTER_SOUND", data).apply();
    }

    @Override
    public boolean getSaveToSD() {
        return prefs.getBoolean("SAVE_TO_SD", saveToSD);
    }
    @Override
    public void setSaveToSD(boolean data) {
        prefs.edit().putBoolean("SAVE_TO_SD", data).apply();
    }

    @Override
    public boolean getHDROn() {
        return prefs.getBoolean("HDR", hdr);
    }
    @Override
    public void setHDROn(boolean data) {
        prefs.edit().putBoolean("HDR", data).apply();
    }

    @Override
    public boolean getSoundInVideo() {
        return prefs.getBoolean("SOUND_IN_VIDEO", soundInVideo);
    }
    @Override
    public void setSoundInVideo(boolean data) {
        prefs.edit().putBoolean("SOUND_IN_VIDEO", data).apply();
    }

    @Override
    public int getShutterTimeOut() {
        return prefs.getInt("SHUTTER_TIME_OUT", shutterTimeOut);
    }
    @Override
    public void setShutterTimeOut(int data) {
        prefs.edit().putInt("SHUTTER_TIME_OUT", data).apply();
    }

    @Override
    public boolean getQrCodeMode() {
        return prefs.getBoolean("QR_CODE_SHOW", qrCodeMode);
    }
    @Override
    public void setQrCodeMode(boolean data) {
        prefs.edit().putBoolean("QR_CODE_SHOW", data).apply();
    }


    public ISharedPreferences(Context context) {
        prefs = context.getSharedPreferences("CameraApp", Context.MODE_PRIVATE);
    }
}