package com.wmods.tkkenhancer.xposed.features.media;

import android.content.Context;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Download Service Hook
 * Hooks into the actual download service implementation
 * Based on smali analysis: DownloadAwemeVideoServiceImpl.LIZ()
 */
public class DownloadServiceHook extends Feature {

    public DownloadServiceHook(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("download_service_hook", false)) return;

        logDebug("Initializing Download Service Hook");

        // Hook the actual download service
        hookDownloadAwemeVideoService();

        logDebug("Download Service Hook initialized");
    }

    /**
     * Hook DownloadAwemeVideoServiceImpl - verified in smali
     * Location: ./smali_classes25/com/ss/android/ugc/aweme/download/DownloadAwemeVideoServiceImpl.smali
     */
    private void hookDownloadAwemeVideoService() {
        try {
            Class<?> downloadServiceClass = null;

            try {
                // Exact class from smali analysis
                downloadServiceClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.download.DownloadAwemeVideoServiceImpl",
                    classLoader
                );
            } catch (Throwable e) {
                logDebug("DownloadAwemeVideoServiceImpl not found");
                return;
            }

            if (downloadServiceClass != null) {
                logDebug("Found DownloadAwemeVideoServiceImpl: " + downloadServiceClass.getName());
                hookDownloadMethods(downloadServiceClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook download service methods
     * Key method: LIZ(Context, Aweme, String, listener)
     */
    private void hookDownloadMethods(Class<?> downloadServiceClass) {
        try {
            Method[] methods = downloadServiceClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                Class<?>[] paramTypes = method.getParameterTypes();

                // Hook LIZ method (obfuscated name found in smali)
                // Signature: (Context, Aweme, String, listener)V
                if (methodName.equals("LIZ") && paramTypes.length == 4) {
                    // Check if first param is Context
                    if (paramTypes[0] == Context.class || 
                        Context.class.isAssignableFrom(paramTypes[0])) {
                        
                        logDebug("Hooking download service method: LIZ");

                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                // param.args[0] = Context
                                // param.args[1] = Aweme object
                                // param.args[2] = filePathDir (String)
                                // param.args[3] = listener
                                
                                logDebug("Download service LIZ called");
                                logDebug("Context: " + param.args[0]);
                                logDebug("Aweme: " + param.args[1]);
                                logDebug("FilePath: " + param.args[2]);
                                
                                // Get Aweme object
                                Object aweme = param.args[1];
                                if (aweme != null) {
                                    try {
                                        // Get video object
                                        Object video = XposedHelpers.callMethod(aweme, "getVideo");
                                        if (video != null) {
                                            logDebug("Video object: " + video);
                                            
                                            // Try to get no-watermark URL
                                            try {
                                                Object noWatermarkAddr = XposedHelpers.callMethod(
                                                    video, "getDownloadNoWatermarkAddr"
                                                );
                                                if (noWatermarkAddr != null) {
                                                    logDebug("No-watermark URL available: " + noWatermarkAddr);
                                                    
                                                    // Store for later use
                                                    XposedHelpers.setAdditionalInstanceField(
                                                        aweme, "download_no_watermark_url", noWatermarkAddr
                                                    );
                                                }
                                            } catch (Throwable e) {
                                                logDebug("Failed to get no-watermark URL", e);
                                            }
                                        }
                                    } catch (Throwable e) {
                                        logDebug("Failed to get video object", e);
                                    }
                                }
                            }

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                logDebug("Download service LIZ completed");
                            }
                        });
                    }
                }

                // Hook other download-related methods
                if (methodName.toLowerCase().contains("download") ||
                    methodName.toLowerCase().contains("save")) {

                    logDebug("Hooking download method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("Download method called: " + methodName);
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
        return "Download Service Hook";
    }
}
