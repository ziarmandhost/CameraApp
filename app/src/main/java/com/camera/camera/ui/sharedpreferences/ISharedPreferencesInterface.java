package com.camera.camera.ui.sharedpreferences;

public interface ISharedPreferencesInterface {
    boolean getGridlinesShow();
    void setGridlinesShow(boolean data);

    boolean getShutterSound();
    void setShutterSound(boolean data);

    boolean getSaveToSD();
    void setSaveToSD(boolean data);

    boolean getSoundInVideo();
    void setSoundInVideo(boolean data);

    int getShutterTimeOut();
    void setShutterTimeOut(int data);

    boolean getFlashOn();
    void setFlashOn(boolean data);

    boolean getHDROn();
    void setHDROn(boolean data);

    //modes
    boolean getQrCodeMode();
    void setQrCodeMode(boolean data);
}