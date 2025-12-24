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

        // Delay initialization to prevent startup ANR
        // Hook will be set up on first live stream access
        try {
            hookLiveStreamLazy();
            logDebug("Live Stream Download feature initialized (lazy mode)");
        } catch (Exception e) {
            logDebug("Failed to initialize Live Stream Download: " + e.getMessage());
        }
    }

    /**
     * Lazy initialization - only hook when needed
     */
    private void hookLiveStreamLazy() {
        try {
            Class<?> liveStreamClass = Unobfuscator.loadTikTokLiveStreamClass(classLoader);
            if (liveStreamClass == null) {
                logDebug("LiveStream class not found - will retry on access");
                return;
            }

            // Only hook play methods, not all methods
            Method[] methods = liveStreamClass.getDeclaredMethods();
            int hookCount = 0;
            for (Method method : methods) {
                String methodName = method.getName();
                if (methodName.equals("startPlay") || methodName.equals("playLive")) {
                    hookLiveStreamPlayback(method);
                    hookCount++;
                    if (hookCount >= 2) break; // Limit hooks to prevent startup delay
                }
            }

            logDebug("Hooked " + hookCount + " live stream methods");
        } catch (Exception e) {
            logDebug("Failed to hook live stream (lazy): " + e.getMessage());
        }
    }

    /**
     * Hook specific live stream playback method
     */
    private void hookLiveStreamPlayback(Method method) {
        try {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    logDebug("Live stream play method called: " + method.getName());
                    
                    // Log parameters to find stream URL (simplified)
                    if (param.args != null && param.args.length > 0) {
                        for (Object arg : param.args) {
                            if (arg != null) {
                                String argStr = arg.toString();
                                if (argStr.contains("http")) {
                                    logDebug("Potential stream URL: " + argStr);
                                    XposedHelpers.setAdditionalInstanceField(
                                        param.thisObject,
                                        "tiktok_enhancer_live_stream_url",
                                        arg
                                    );
                                    break; // Found URL, stop processing
                                }
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            logDebug("Failed to hook method " + method.getName() + ": " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "LiveStreamDownload";
    }
}
