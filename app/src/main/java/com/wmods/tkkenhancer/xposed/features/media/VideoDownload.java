package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

/**
 * Feature to download TikTok videos without watermark
 * Based on smali analysis of TikTok 43.0.0
 * 
 * Target class: com.ss.android.ugc.aweme.feed.model.Video
 * Key method: getDownloadNoWatermarkAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
 * Field: downloadNoWatermarkAddr
 */
public class VideoDownload extends Feature {

    private static final String VIDEO_CLASS = "com.ss.android.ugc.aweme.feed.model.Video";
    private static final String URL_MODEL_CLASS = "com.ss.android.ugc.aweme.base.model.UrlModel";

    public VideoDownload(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("video_download", false)) return;

        logDebug("Initializing Video Download feature (smali-based)");

        // Hook the Video model to ensure downloadNoWatermarkAddr is available
        hookVideoDownloadAddr();

        logDebug("Video Download feature initialized successfully");
    }

    /**
     * Hook Video.getDownloadNoWatermarkAddr() to ensure it returns a valid URL
     * If downloadNoWatermarkAddr is null, use regular downloadAddr as fallback
     */
    private void hookVideoDownloadAddr() {
        try {
            Class<?> videoClass = XposedHelpers.findClass(VIDEO_CLASS, classLoader);
            
            // Hook getDownloadNoWatermarkAddr()
            XposedHelpers.findAndHookMethod(
                videoClass,
                "getDownloadNoWatermarkAddr",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object result = param.getResult();
                        
                        // If no-watermark URL is null, try to get regular download URL
                        if (result == null) {
                            logDebug("downloadNoWatermarkAddr is null, using downloadAddr as fallback");
                            Object video = param.thisObject;
                            try {
                                result = XposedHelpers.callMethod(video, "getDownloadAddr");
                                param.setResult(result);
                                logDebug("Using downloadAddr as fallback");
                            } catch (Throwable e) {
                                logDebug("Failed to get downloadAddr: " + e.getMessage());
                            }
                        } else {
                            logDebug("downloadNoWatermarkAddr is available");
                        }
                    }
                }
            );

            logDebug("Successfully hooked Video.getDownloadNoWatermarkAddr()");

        } catch (Throwable e) {
            log("Failed to hook Video download methods: " + e.getMessage());
            log(e);
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Video Download (No Watermark)";
    }
}
