package com.camera.camera.ui.adapters;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.camera.camera.ui.base.BaseFragment;

import java.util.List;

public class MyFragmentsPagerAdapter extends FragmentPagerAdapter {
    private List<BaseFragment> fragments;

    public MyFragmentsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        return fragments.get(i);
    }

    public void setList (List<BaseFragment> fragments) {
        this.fragments = fragments;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return fragments.get(position).getName();
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}
