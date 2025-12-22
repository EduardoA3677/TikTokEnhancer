package com.wmods.tiktokenhancer.ui.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.wmods.tiktokenhancer.R;
import com.wmods.tiktokenhancer.ui.fragments.base.BasePreferenceFragment;

public class MediaFragment extends BasePreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        setDisplayHomeAsUpEnabled(false);
    }


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.fragment_media, rootKey);
    }
}
