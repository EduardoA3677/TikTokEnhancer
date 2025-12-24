package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Video Quality/Bitrate Control Feature
 * Based on smali analysis:
 * - com.ss.android.ugc.aweme.bitrateselector.impl.DTBitrateSelectorServiceImpl
 * - com.ss.android.ugc.aweme.video.bitrate.RateSettingCombineModel
 * 
 * Allows users to control video quality and bitrate selection for better quality
 * or data savings.
 */
public class BitrateControl extends Feature {

    public BitrateControl(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("bitrate_control", false)) return;

        logDebug("Initializing Bitrate Control feature");

        // Hook bitrate selector
        hookBitrateSelector();

        logDebug("Bitrate Control feature initialized");
    }

    /**
     * Hook bitrate selector to control video quality
     * Based on verified smali: GearSet.getBitRate() and AutoBitrateSet.getMinBitrate()
     */
    private void hookBitrateSelector() {
        try {
            // Hook GearSet.getBitRate() - verified in smali_classes25
            hookGearSetBitRate();
            
            // Hook AutoBitrateSet.getMinBitrate() - verified in smali_classes25
            hookAutoBitrateSet();
            
        } catch (Throwable e) {
            log("Error hooking bitrate selector: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook GearSet.getBitRate() to force higher bitrate
     * Verified class: com.ss.android.ugc.aweme.simkit.model.bitrateselect.GearSet
     */
    private void hookGearSetBitRate() {
        try {
            Class<?> gearSetClass = classLoader.loadClass("com.ss.android.ugc.aweme.simkit.model.bitrateselect.GearSet");
            
            XposedHelpers.findAndHookMethod(gearSetClass, "getBitRate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int originalBitrate = (int) param.getResult();
                    
                    // Force higher bitrate for better quality
                    // ListPreference stores values as String, so we need to parse it
                    String targetBitrateStr = prefs.getString("target_bitrate", "5000000");
                    int targetBitrate;
                    try {
                        targetBitrate = Integer.parseInt(targetBitrateStr);
                    } catch (NumberFormatException e) {
                        targetBitrate = 5000000; // 5 Mbps default fallback
                        logDebug("Invalid bitrate value, using default: " + targetBitrate);
                    }
                    
                    if (originalBitrate < targetBitrate) {
                        param.setResult(targetBitrate);
                        logDebug("Increased bitrate from " + originalBitrate + " to " + targetBitrate);
                    }
                }
            });
            
            logDebug("Hooked GearSet.getBitRate() successfully");
        } catch (Exception e) {
            logDebug("Failed to hook GearSet: " + e.getMessage());
        }
    }

    /**
     * Hook AutoBitrateSet to control minimum bitrate
     * Verified class: com.ss.android.ugc.aweme.simkit.model.bitrateselect.AutoBitrateSet
     */
    private void hookAutoBitrateSet() {
        try {
            Class<?> autoBitrateClass = classLoader.loadClass("com.ss.android.ugc.aweme.simkit.model.bitrateselect.AutoBitrateSet");
            
            XposedHelpers.findAndHookMethod(autoBitrateClass, "getMinBitrate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    double originalMinBitrate = (double) param.getResult();
                    
                    // Set higher minimum bitrate
                    // ListPreference stores values as String, so we need to parse it
                    String targetBitrateStr = prefs.getString("target_bitrate", "5000000");
                    int targetBitrate;
                    try {
                        targetBitrate = Integer.parseInt(targetBitrateStr);
                    } catch (NumberFormatException e) {
                        targetBitrate = 5000000; // 5 Mbps default fallback
                        logDebug("Invalid bitrate value, using default: " + targetBitrate);
                    }
                    
                    double targetMinBitrate = targetBitrate * 0.8; // 80% of target
                    if (originalMinBitrate < targetMinBitrate) {
                        param.setResult(targetMinBitrate);
                        logDebug("Increased min bitrate from " + originalMinBitrate + " to " + targetMinBitrate);
                    }
                }
            });
            
            logDebug("Hooked AutoBitrateSet.getMinBitrate() successfully");
        } catch (Exception e) {
            logDebug("Failed to hook AutoBitrateSet: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "BitrateControl";
    }
}
