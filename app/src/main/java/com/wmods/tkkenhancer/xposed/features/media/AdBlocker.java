package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

/**
 * Feature to block advertisements in TikTok feed
 * Based on smali analysis of TikTok 43.0.0
 * 
 * Target class: com.ss.android.ugc.aweme.feed.model.Aweme
 * Key methods: 
 *   - isAd()Z - Returns true if video is an ad
 *   - isAdTraffic()Z - Returns true if ad traffic
 * Key field: isAd
 */
public class AdBlocker extends Feature {

    private static final String AWEME_CLASS = "com.ss.android.ugc.aweme.feed.model.Aweme";

    public AdBlocker(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("ad_blocker", false)) return;

        logDebug("Initializing Ad Blocker feature (smali-based)");

        // Hook Aweme.isAd() to filter ads
        hookIsAdMethod();
        
        // Hook Aweme.isAdTraffic() to block ad traffic
        hookIsAdTrafficMethod();

        logDebug("Ad Blocker feature initialized successfully");
    }

    /**
     * Hook Aweme.isAd() to always return false
     * This hides ads from the feed
     */
    private void hookIsAdMethod() {
        try {
            Class<?> awemeClass = XposedHelpers.findClass(AWEME_CLASS, classLoader);
            
            XposedHelpers.findAndHookMethod(
                awemeClass,
                "isAd",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // Always return false to hide ads
                        param.setResult(false);
                        logDebug("Blocked ad via isAd()");
                    }
                }
            );

            logDebug("Successfully hooked Aweme.isAd()");

        } catch (Throwable e) {
            log("Failed to hook Aweme.isAd(): " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook Aweme.isAdTraffic() to always return false
     * This blocks ad traffic detection
     */
    private void hookIsAdTrafficMethod() {
        try {
            Class<?> awemeClass = XposedHelpers.findClass(AWEME_CLASS, classLoader);
            
            XposedHelpers.findAndHookMethod(
                awemeClass,
                "isAdTraffic",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // Always return false to block ad traffic
                        param.setResult(false);
                        logDebug("Blocked ad traffic via isAdTraffic()");
                    }
                }
            );

            logDebug("Successfully hooked Aweme.isAdTraffic()");

        } catch (Throwable e) {
            log("Failed to hook Aweme.isAdTraffic(): " + e.getMessage());
            log(e);
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Ad Blocker";
    }
}
