package com.camera.camera;

import android.hardware.Camera;
import android.support.design.widget.TabLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemUI();
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initTabs();

        captureBtn = findViewById(R.id.capture);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Camera view clicked", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initTabs () {
        container = findViewById(R.id.container);
        tabLayout = findViewById(R.id.tabLayout);

        BaseFragment photoFragment = new Photo();
        BaseFragment portretFragment = new Portret();
        BaseFragment videoFragment = new Video();

        ArrayList<BaseFragment> fragments = new ArrayList<>();
        fragments.add(photoFragment);
        fragments.add(portretFragment);
        fragments.add(videoFragment);

        container.setOffscreenPageLimit(3);

        MyFragmentsPagerAdapter adapter = new MyFragmentsPagerAdapter(getSupportFragmentManager());
        adapter.setList(fragments);
        container.setAdapter(adapter);
        tabLayout.setupWithViewPager(container);
    }

    private void hideSystemUI () {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
