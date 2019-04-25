package com.camera.camera.ui.base;

import android.widget.ImageView;

interface IFragment {
    String getName();

    void onShutter();
    void changeCamera();

    void turnOnFlashLight();
    void turnOffFlashLight();

    void turnHdrOn();
    void turnHdrOff();

    void showGrid();
    void hideGrid();

    void setFilter(int filter);
}
