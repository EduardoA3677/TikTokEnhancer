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
 * Live Stream Download Feature for TikTok
 * Enables downloading of live stream content
 * 
 * Based on smali analysis:
 * - LiveStream classes: com.ss.android.ugc.aweme.live.*
 * - Live stream playback methods
 * - Stream URL extraction
 */
public class LiveStreamDownload extends Feature {

    public LiveStreamDownload(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("live_stream_download", false)) return;

        logDebug("Initializing Live Stream Download feature");

        try {
            // Hook live stream playback to capture stream URLs
            hookLiveStreamPlayback();
            
            // Hook live stream URL extraction
            hookLiveStreamUrl();

            logDebug("Live Stream Download feature initialized");
        } catch (Exception e) {
            logDebug("Failed to initialize Live Stream Download: " + e.getMessage());
        }
    }

    /**
     * Hook live stream playback to intercept stream information
     */
    private void hookLiveStreamPlayback() {
        try {
            Class<?> liveStreamClass = Unobfuscator.loadTikTokLiveStreamClass(classLoader);
            if (liveStreamClass == null) {
                logDebug("LiveStream class not found");
                return;
            }

            // Hook all methods to find stream playback
            for (Method method : liveStreamClass.getDeclaredMethods()) {
                if (method.getName().toLowerCase().contains("play") || 
                    method.getName().toLowerCase().contains("start")) {
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("Live stream play method called: " + method.getName());
                            
                            // Log parameters to find stream URL
                            if (param.args != null) {
                                for (int i = 0; i < param.args.length; i++) {
                                    Object arg = param.args[i];
                                    if (arg != null) {
                                        String argStr = arg.toString();
                                        if (argStr.contains("http") || argStr.contains("stream")) {
                                            logDebug("Potential stream URL: " + argStr);
                                            // Store for later retrieval
                                            XposedHelpers.setAdditionalInstanceField(
                                                param.thisObject,
                                                "tiktok_enhancer_live_stream_url",
                                                arg
                                            );
                                        }
                                    }
                                }
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // Check result for stream URL
                            Object result = param.getResult();
                            if (result != null && result.toString().contains("http")) {
                                logDebug("Stream URL from result: " + result.toString());
                            }
                        }
                    });
                }
            }

            logDebug("Hooked live stream playback methods in " + liveStreamClass.getName());
        } catch (Exception e) {
            logDebug("Failed to hook live stream playback: " + e.getMessage());
        }
    }

    /**
     * Hook live stream URL extraction methods
     */
    private void hookLiveStreamUrl() {
        try {
            Class<?> liveStreamClass = Unobfuscator.loadTikTokLiveStreamClass(classLoader);
            if (liveStreamClass == null) return;

            // Hook methods that return URLs
            for (Method method : liveStreamClass.getDeclaredMethods()) {
                Class<?> returnType = method.getReturnType();
                if (returnType == String.class) {
                    String methodName = method.getName().toLowerCase();
                    if (methodName.contains("url") || methodName.contains("stream") || 
                        methodName.contains("link") || methodName.contains("address")) {
                        
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                String result = (String) param.getResult();
                                if (result != null && result.startsWith("http")) {
                                    logDebug("Live stream URL intercepted: " + result);
                                    // Store for download capability
                                    XposedHelpers.setAdditionalStaticField(
                                        liveStreamClass,
                                        "tiktok_enhancer_current_stream_url",
                                        result
                                    );
                                }
                            }
                        });
                    }
                }
            }

            logDebug("Hooked live stream URL methods");
        } catch (Exception e) {
            logDebug("Failed to hook live stream URLs: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "LiveStreamDownload";
    }
}
