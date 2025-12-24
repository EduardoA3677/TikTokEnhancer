package com.wmods.tkkenhancer.xposed.features.privacy;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

/**
 * Analytics Blocker Feature for TikTok
 * Blocks analytics and tracking functionality
 * 
 * Based on verified smali analysis:
 * - FirebaseAnalytics class: com.google.firebase.analytics.FirebaseAnalytics (smali_classes22)
 * - Blocks setCurrentScreen to prevent screen tracking
 * 
 * Note: This is a minimal, safe implementation that blocks key tracking without
 * causing performance issues or startup delays.
 */
public class AnalyticsBlocker extends Feature {

    public AnalyticsBlocker(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("analytics_blocker", false)) return;

        logDebug("Initializing Analytics Blocker feature");

        try {
            hookFirebaseAnalytics();
            logDebug("Analytics Blocker feature initialized successfully");
        } catch (Exception e) {
            logDebug("Failed to initialize Analytics Blocker: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook FirebaseAnalytics.setCurrentScreen() to block screen tracking
     * Verified class: com.google.firebase.analytics.FirebaseAnalytics
     */
    private void hookFirebaseAnalytics() {
        try {
            Class<?> firebaseClass = classLoader.loadClass("com.google.firebase.analytics.FirebaseAnalytics");
            
            // Hook setCurrentScreen to block screen tracking
            XposedHelpers.findAndHookMethod(
                firebaseClass,
                "setCurrentScreen",
                android.app.Activity.class,
                String.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // Block the tracking call
                        param.setResult(null);
                        logDebug("Blocked Firebase screen tracking: " + param.args[1]);
                    }
                }
            );
            
            logDebug("Hooked FirebaseAnalytics successfully");
        } catch (Exception e) {
            logDebug("Failed to hook FirebaseAnalytics: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "AnalyticsBlocker";
    }
}
