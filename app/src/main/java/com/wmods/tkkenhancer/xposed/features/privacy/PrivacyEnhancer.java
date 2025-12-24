package com.wmods.tkkenhancer.xposed.features.privacy;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Privacy Enhancement Feature for TikTok
 * Provides privacy controls including:
 * - Hide view history
 * - Hide profile visits
 * - Disable analytics tracking
 * - Block data collection
 */
public class PrivacyEnhancer extends Feature {

    public PrivacyEnhancer(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("privacy_enhancer", false)) return;

        logDebug("Initializing Privacy Enhancer feature");

        // Hook view history tracking
        if (prefs.getBoolean("hide_view_history", false)) {
            hookViewHistory();
        }

        // Hook profile visit tracking
        if (prefs.getBoolean("hide_profile_visits", false)) {
            hookProfileVisits();
        }

        // Hook analytics
        if (prefs.getBoolean("disable_analytics", false)) {
            hookAnalytics();
        }

        // Hook data collection
        if (prefs.getBoolean("block_data_collection", false)) {
            hookDataCollection();
        }

        logDebug("Privacy Enhancer feature initialized");
    }

    /**
     * Hook view history to prevent tracking of watched videos
     */
    private void hookViewHistory() {
        try {
            // Try to find view history classes
            Class<?> viewHistoryClass = null;

            try {
                viewHistoryClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.history.HistoryManager",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    viewHistoryClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.feed.history.FeedHistoryManager",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("View history class not found");
                    return;
                }
            }

            if (viewHistoryClass != null) {
                logDebug("Found view history class: " + viewHistoryClass.getName());
                hookViewHistoryMethods(viewHistoryClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook methods that track view history
     */
    private void hookViewHistoryMethods(Class<?> historyClass) {
        try {
            Method[] methods = historyClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Look for methods that save or track history
                if (methodName.toLowerCase().contains("save") ||
                    methodName.toLowerCase().contains("add") ||
                    methodName.toLowerCase().contains("record") ||
                    methodName.toLowerCase().contains("track")) {

                    logDebug("Hooking history method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Block history tracking
                            logDebug("Blocked view history tracking: " + methodName);
                            param.setResult(null);
                        }
                    });
                }
            }
        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook profile visit tracking
     */
    private void hookProfileVisits() {
        try {
            // Try to find profile tracking classes
            Class<?> profileClass = null;

            try {
                profileClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.profile.ProfileServiceImpl",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    profileClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.profile.api.ProfileService",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Profile service class not found");
                    return;
                }
            }

            if (profileClass != null) {
                logDebug("Found profile service class: " + profileClass.getName());
                hookProfileTrackingMethods(profileClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook methods that track profile visits
     */
    private void hookProfileTrackingMethods(Class<?> profileClass) {
        try {
            Method[] methods = profileClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Look for methods that track profile visits
                if (methodName.toLowerCase().contains("visit") ||
                    methodName.toLowerCase().contains("view") ||
                    methodName.toLowerCase().contains("track")) {

                    logDebug("Hooking profile tracking method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Check if it's a tracking call
                            if (methodName.toLowerCase().contains("track") ||
                                methodName.toLowerCase().contains("log")) {
                                logDebug("Blocked profile visit tracking: " + methodName);
                                param.setResult(null);
                            }
                        }
                    });
                }
            }
        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook analytics tracking
     */
    private void hookAnalytics() {
        try {
            // Try to find analytics classes
            Class<?> analyticsClass = null;

            try {
                analyticsClass = XposedHelpers.findClass(
                    "com.bytedance.ies.ugc.aweme.tiktok.analysis.Analytics",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    analyticsClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.analysis.Analysis",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Analytics class not found");
                    return;
                }
            }

            if (analyticsClass != null) {
                logDebug("Found analytics class: " + analyticsClass.getName());
                hookAnalyticsMethods(analyticsClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook analytics methods
     */
    private void hookAnalyticsMethods(Class<?> analyticsClass) {
        try {
            Method[] methods = analyticsClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Block all analytics calls
                if (methodName.toLowerCase().contains("track") ||
                    methodName.toLowerCase().contains("log") ||
                    methodName.toLowerCase().contains("event") ||
                    methodName.toLowerCase().contains("report")) {

                    logDebug("Hooking analytics method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Block analytics
                            logDebug("Blocked analytics call: " + methodName);
                            param.setResult(null);
                        }
                    });
                }
            }
        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook data collection services
     */
    private void hookDataCollection() {
        try {
            // Hook data collection services
            hookDataCollectionService();

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook data collection service
     */
    private void hookDataCollectionService() {
        try {
            Class<?> dataCollectionClass = null;

            try {
                dataCollectionClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.services.DataCollectionService",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    dataCollectionClass = XposedHelpers.findClass(
                        "com.bytedance.ies.ugc.aweme.datacollection.DataCollector",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Data collection class not found");
                    return;
                }
            }

            if (dataCollectionClass != null) {
                logDebug("Found data collection class: " + dataCollectionClass.getName());
                hookDataCollectionMethods(dataCollectionClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook data collection methods
     */
    private void hookDataCollectionMethods(Class<?> dataCollectionClass) {
        try {
            Method[] methods = dataCollectionClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Block data collection
                if (methodName.toLowerCase().contains("collect") ||
                    methodName.toLowerCase().contains("send") ||
                    methodName.toLowerCase().contains("upload") ||
                    methodName.toLowerCase().contains("sync")) {

                    logDebug("Hooking data collection method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Block data collection
                            logDebug("Blocked data collection: " + methodName);
                            param.setResult(null);
                        }
                    });
                }
            }
        } catch (Throwable e) {
            log(e);
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Privacy Enhancer";
    }
}
