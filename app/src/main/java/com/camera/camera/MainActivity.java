package com.camera.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.camera.camera.ui.adapters.MyFragmentsPagerAdapter;
import com.camera.camera.ui.base.BaseFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ViewPager container;
    TabLayout tabLayout;
    ImageView captureBtn;
    ImageView changeCamera;
    RelativeLayout relativeLayout;

    BaseFragment photo;
    BaseFragment portret;
    BaseFragment video;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemUI();
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!checkPermissions()) {
            setPermissions();
        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "This application needs camera permissions!", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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
    private boolean checkPermissions () {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }
    private void setPermissions () {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                },
                101);
    }

    private BaseFragment getActiveFragment () {
        return (BaseFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.container + ":" + container.getCurrentItem());
    }
}

