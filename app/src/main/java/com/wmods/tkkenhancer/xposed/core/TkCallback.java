package com.wmods.tkkenhancer.xposed.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TkCallback implements Application.ActivityLifecycleCallbacks {
    private static void triggerActivityState(@NonNull Activity activity, TkkCore.ActivityChangeState.ChangeType type) {
        TkkCore.listenerAcitivity.forEach((listener) -> listener.onChange(activity, type));
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        TkkCore.mCurrentActivity = activity;
        triggerActivityState(activity, TkkCore.ActivityChangeState.ChangeType.CREATED);
        TkkCore.activities.add(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        TkkCore.mCurrentActivity = activity;
        triggerActivityState(activity, TkkCore.ActivityChangeState.ChangeType.STARTED);
        TkkCore.activities.add(activity);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        TkkCore.mCurrentActivity = activity;
        TkkCore.activities.add(activity);
        triggerActivityState(activity, TkkCore.ActivityChangeState.ChangeType.RESUMED);
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        triggerActivityState(activity, TkkCore.ActivityChangeState.ChangeType.PAUSED);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        triggerActivityState(activity, TkkCore.ActivityChangeState.ChangeType.ENDED);
        TkkCore.activities.remove(activity);
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        TkkCore.activities.remove(activity);
    }
}
