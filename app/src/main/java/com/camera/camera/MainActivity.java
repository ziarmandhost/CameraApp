package com.camera.camera;

import android.hardware.Camera;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.camera.camera.ui.adapters.MyFragmentsPagerAdapter;
import com.camera.camera.ui.base.BaseFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ViewPager container;
    TabLayout tabLayout;
    ImageView captureBtn;
    ImageView changeCamera;

    BaseFragment photo;
    BaseFragment portret;
    BaseFragment video;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemUI();
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initTabs();

        captureBtn = findViewById(R.id.capture);
        changeCamera = findViewById(R.id.changeCamera);

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), getActiveFragment().getName(), Toast.LENGTH_SHORT).show();
                // чисто потестить, метод getActiveFragment позволяет узнать активный фрагмент
                // вообще ТУТ нужно делать фото
            }
        });

        changeCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActiveFragment().switchCamera();
            }
        });
    }

    private void initTabs () {
        container = findViewById(R.id.container);
        tabLayout = findViewById(R.id.tabLayout);

        photo = new Photo();
        portret = new Portret();
        video = new Video();

        ArrayList<BaseFragment> fragments = new ArrayList<>();
        fragments.add(photo);
        fragments.add(portret);
        fragments.add(video);

        container.setOffscreenPageLimit(3);

        MyFragmentsPagerAdapter adapter = new MyFragmentsPagerAdapter(getSupportFragmentManager());
        adapter.setList(fragments);
        container.setAdapter(adapter);
        tabLayout.setupWithViewPager(container);
    }
    private void hideSystemUI () {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private BaseFragment getActiveFragment () {
        return (BaseFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + container.getCurrentItem());
    }
}

