package com.camera.camera.ui.base;

import android.support.v4.app.Fragment;
import android.view.View;

public abstract class BaseFragment extends Fragment implements IFragment {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public void onShutter() {

    }

    @Override
    public void changeCamera() {

    }
}
