

package com.midoreigh.midopicker;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;


public class PagerAdapter_Picker extends FragmentPagerAdapter {
    private static final String TAG = CameraKitFragment.class.getSimpleName();



    String[] tab_titles;


    public PagerAdapter_Picker(Context context, FragmentManager fm) {
        super(fm);
            tab_titles = context.getResources().getStringArray(R.array.tab_titles);

    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tab_titles[position];
    }

    @Override
    public int getCount() {
        return tab_titles.length;
    }

    @Override
    public Fragment getItem(int position) {


        switch (position) {
            case 0:
                CameraKitFragment cameraKitFragment = new CameraKitFragment();
                CameraKitFragment.setConfig(ImagePickerActivity.getConfig());
                return cameraKitFragment;


            case 1:
                return new GalleryFragment();


            default:
                return null;
        }


    }


}
