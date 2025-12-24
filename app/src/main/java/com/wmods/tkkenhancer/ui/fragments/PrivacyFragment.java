package com.wmods.tkkenhancer.ui.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.wmods.tkkenhancer.R;
import com.wmods.tkkenhancer.ui.fragments.base.BasePreferenceFragment;

public class PrivacyFragment extends BasePreferenceFragment {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        setPreferencesFromResource(R.xml.fragment_privacy, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        setDisplayHomeAsUpEnabled(false);
    }

}
