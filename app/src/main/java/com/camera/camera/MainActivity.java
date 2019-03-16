package com.camera.camera;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.camera.camera.ui.adapters.MyFragmentsPagerAdapter;
import com.camera.camera.ui.base.BaseFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ViewPager container;
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = findViewById(R.id.container);
        tabLayout = findViewById(R.id.tabLayout);

        Fragment photoFragment = new Photo();
        Fragment portretFragment = new Portret();
        Fragment videoFragment = new Video();

        ArrayList<BaseFragment> fragments = new ArrayList<>();
        fragments.add((BaseFragment) photoFragment);
        fragments.add((BaseFragment) portretFragment);
        fragments.add((BaseFragment) videoFragment);

        MyFragmentsPagerAdapter adapter = new MyFragmentsPagerAdapter(getSupportFragmentManager());
        adapter.setList(fragments);
        container.setAdapter(adapter);
        tabLayout.setupWithViewPager(container);
    }
}
