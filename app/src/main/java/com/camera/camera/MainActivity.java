package com.camera.camera;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.camera.camera.ui.adapters.MyFragmentsPagerAdapter;
import com.camera.camera.ui.base.BaseFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ViewPager container;
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemUI();
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);
        tabLayout = findViewById(R.id.tabLayout);

        BaseFragment photoFragment = new Photo();
        BaseFragment portretFragment = new Portret();
        BaseFragment videoFragment = new Video();

        ArrayList<BaseFragment> fragments = new ArrayList<>();
        fragments.add(photoFragment);
        fragments.add(portretFragment);
        fragments.add(videoFragment);

        MyFragmentsPagerAdapter adapter = new MyFragmentsPagerAdapter(getSupportFragmentManager());
        adapter.setList(fragments);
        container.setAdapter(adapter);
        tabLayout.setupWithViewPager(container);
    }

    private void hideSystemUI () {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
