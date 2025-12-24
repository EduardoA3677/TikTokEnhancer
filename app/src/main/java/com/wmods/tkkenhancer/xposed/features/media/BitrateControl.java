package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

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
     */
    private void hookBitrateSelector() {
        try {
            Class<?> bitrateSelectorClass = Unobfuscator.loadTikTokBitrateSelectorClass(classLoader);
            if (bitrateSelectorClass == null) {
                logDebug("Bitrate selector class not found");
                return;
            }

            logDebug("Found bitrate selector class: " + bitrateSelectorClass.getName());

            // Get user preference for quality
            String qualityPref = prefs.getString("video_quality_preference", "high");
            logDebug("Video quality preference: " + qualityPref);

            // Hook methods related to bitrate selection
            for (Method method : bitrateSelectorClass.getDeclaredMethods()) {
                String methodName = method.getName().toLowerCase();
                
                if (methodName.contains("bitrate") || 
                    methodName.contains("quality") ||
                    methodName.contains("select") ||
                    methodName.contains("rate")) {
                    
                    logDebug("Hooking bitrate method: " + method.getName());
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("Bitrate method called: " + method.getName());
                        }
                        
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // Force high quality if preferred
                            if ("high".equals(qualityPref)) {
                                Class<?> returnType = method.getReturnType();
                                
                                // If returning an int (bitrate value), maximize it
                                if (returnType == int.class || returnType == Integer.class) {
                                    Object result = param.getResult();
                                    if (result instanceof Integer) {
                                        int bitrate = (Integer) result;
                                        // Increase bitrate for better quality
                                        int newBitrate = Math.max(bitrate, bitrate * 2);
                                        logDebug("Increased bitrate from " + bitrate + " to " + newBitrate);
                                        param.setResult(newBitrate);
                                    }
                                }
                            }
                        }
                    });
                }
            }

        } catch (Throwable e) {
            log("Error hooking bitrate selector: " + e.getMessage());
            log(e);
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Bitrate/Quality Control";
    }
}
