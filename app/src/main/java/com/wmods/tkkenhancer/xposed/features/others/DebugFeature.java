package com.wmods.tkkenhancer.xposed.features.others;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import de.robv.android.xposed.XSharedPreferences;

public class DebugFeature extends Feature {


    public DebugFeature(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {

    }


    @NonNull
    @Override
    public String getPluginName() {
        return "Debug Feature";
    }


}
