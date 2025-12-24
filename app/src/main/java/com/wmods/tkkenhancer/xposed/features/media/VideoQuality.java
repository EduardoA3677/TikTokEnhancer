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
 * Video Quality Enhancement Feature
 * Allows users to force higher video quality and control quality settings
 */
public class VideoQuality extends Feature {

    public VideoQuality(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("video_quality", false)) return;

        logDebug("Initializing Video Quality feature");

        // Hook video quality selection
        hookVideoQualitySelection();

        // Hook video bitrate settings
        hookVideoBitrate();

        // Hook resolution settings
        hookVideoResolution();

        logDebug("Video Quality feature initialized");
    }

    /**
     * Hook video quality selection
     */
    private void hookVideoQualitySelection() {
        try {
            // Try to find video quality classes
            Class<?> qualityClass = null;

            try {
                qualityClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.video.VideoBitRateABManager",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    qualityClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.player.sdk.api.VideoQualityManager",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Video quality class not found");
                    return;
                }
            }

            if (qualityClass != null) {
                logDebug("Found video quality class: " + qualityClass.getName());
                hookQualityMethods(qualityClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook quality selection methods
     */
    private void hookQualityMethods(Class<?> qualityClass) {
        try {
            Method[] methods = qualityClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Look for quality selection methods
                if (methodName.toLowerCase().contains("quality") ||
                    methodName.toLowerCase().contains("bitrate") ||
                    methodName.toLowerCase().contains("resolution")) {

                    logDebug("Hooking quality method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // Force higher quality if requested
                            boolean forceHD = prefs.getBoolean("force_hd_quality", false);
                            
                            if (forceHD) {
                                // If method returns quality level, try to upgrade it
                                Object result = param.getResult();
                                if (result instanceof Integer) {
                                    int quality = (Integer) result;
                                    // Try to force highest quality (typically highest number)
                                    if (quality < 1080) {
                                        logDebug("Upgrading quality from " + quality + " to 1080");
                                        param.setResult(1080);
                                    }
                                } else if (result instanceof String) {
                                    String qualityStr = (String) result;
                                    if (!qualityStr.contains("1080") && !qualityStr.contains("hd")) {
                                        logDebug("Upgrading quality: " + qualityStr);
                                    }
                                }
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
     * Hook video bitrate settings
     */
    private void hookVideoBitrate() {
        try {
            // Try to find bitrate manager
            Class<?> bitrateClass = null;

            try {
                bitrateClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.video.VideoBitRateABManager",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    bitrateClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.playerkit.simapicommon.model.SimBitRate",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Bitrate class not found");
                    return;
                }
            }

            if (bitrateClass != null) {
                logDebug("Found bitrate class: " + bitrateClass.getName());
                hookBitrateMethods(bitrateClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook bitrate methods
     */
    private void hookBitrateMethods(Class<?> bitrateClass) {
        try {
            Method[] methods = bitrateClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Look for bitrate getter methods
                if ((methodName.toLowerCase().contains("get") || 
                     methodName.toLowerCase().contains("select")) &&
                    methodName.toLowerCase().contains("bitrate")) {

                    logDebug("Hooking bitrate method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            boolean forceHighBitrate = prefs.getBoolean("force_high_bitrate", false);
                            
                            if (forceHighBitrate) {
                                Object result = param.getResult();
                                if (result instanceof Integer) {
                                    int bitrate = (Integer) result;
                                    // Force higher bitrate (e.g., 5000000 = 5 Mbps)
                                    int targetBitrate = prefs.getInt("target_bitrate", 5000000);
                                    if (bitrate < targetBitrate) {
                                        logDebug("Upgrading bitrate from " + bitrate + " to " + targetBitrate);
                                        param.setResult(targetBitrate);
                                    }
                                }
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
     * Hook video resolution settings
     */
    private void hookVideoResolution() {
        try {
            // Try to find video player classes
            Class<?> playerClass = null;

            try {
                playerClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.player.sdk.api.OnUIPlayListener",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    playerClass = Unobfuscator.loadTikTokVideoPlayerClass(classLoader);
                } catch (Throwable e2) {
                    logDebug("Player class not found");
                    return;
                }
            }

            if (playerClass != null) {
                logDebug("Found player class: " + playerClass.getName());
                hookResolutionMethods(playerClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook resolution methods
     */
    private void hookResolutionMethods(Class<?> playerClass) {
        try {
            Method[] methods = playerClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Look for resolution-related methods
                if (methodName.toLowerCase().contains("resolution") ||
                    methodName.toLowerCase().contains("size") ||
                    methodName.toLowerCase().contains("dimension")) {

                    logDebug("Hooking resolution method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("Resolution method called: " + methodName);
                            // Can be extended to modify resolution settings
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
        return "Video Quality";
    }
}
