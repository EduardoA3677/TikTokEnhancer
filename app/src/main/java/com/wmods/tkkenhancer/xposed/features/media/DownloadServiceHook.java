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

        // Get granular hook preferences
        boolean hookDownloadAwemeService = prefs.getBoolean("hook_download_aweme_service", true);
        boolean hookShareHelper = prefs.getBoolean("hook_share_helper", false);
        boolean hookNetworkUtils = prefs.getBoolean("hook_network_utils", false);

        logDebug("Hook configuration: DownloadAwemeService=" + hookDownloadAwemeService + 
                 ", ShareHelper=" + hookShareHelper + ", NetworkUtils=" + hookNetworkUtils);

        // Hook the actual download service
        if (hookDownloadAwemeService) {
            hookDownloadAwemeVideoService();
        }

        // Hook ShareHelper for custom downloads from share
        if (hookShareHelper) {
            hookShareHelper();
        }

        // Hook NetworkUtils low-level download
        if (hookNetworkUtils) {
            hookNetworkUtils();
        }

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

    /**
     * Hook ShareHelper classes for custom download from share
     * Targets: SharePanelHelper, ShareHelper
     */
    private void hookShareHelper() {
        try {
            String[] shareHelperClasses = {
                "com.ss.android.ugc.aweme.share.SharePanelHelper",
                "com.ss.android.ugc.aweme.share.ShareHelper",
                "com.ss.android.ugc.aweme.share.improve.ShareHelper"
            };

            for (String className : shareHelperClasses) {
                try {
                    Class<?> shareHelperClass = XposedHelpers.findClass(className, classLoader);
                    if (shareHelperClass != null) {
                        logDebug("Found ShareHelper class: " + className);
                        hookShareHelperMethods(shareHelperClass);
                    }
                } catch (Throwable e) {
                    logDebug("ShareHelper class not found: " + className);
                }
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook ShareHelper methods
     */
    private void hookShareHelperMethods(Class<?> shareHelperClass) {
        try {
            Method[] methods = shareHelperClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Hook share-related methods that might trigger downloads
                if (methodName.toLowerCase().contains("share") ||
                    methodName.toLowerCase().contains("save") ||
                    methodName.toLowerCase().contains("download")) {

                    logDebug("Hooking ShareHelper method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("ShareHelper method called: " + methodName);
                            // Log parameters for debugging
                            if (param.args != null && param.args.length > 0) {
                                for (int i = 0; i < param.args.length; i++) {
                                    if (param.args[i] != null) {
                                        logDebug("  Param[" + i + "]: " + param.args[i].getClass().getName());
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
     * Hook NetworkUtils.downloadVideo() - Low-level download hook
     * Location: ./smali_classes29/com/ss/android/common/util/NetworkUtils.smali:599
     */
    private void hookNetworkUtils() {
        try {
            Class<?> networkUtilsClass = null;

            try {
                networkUtilsClass = XposedHelpers.findClass(
                    "com.ss.android.common.util.NetworkUtils",
                    classLoader
                );
            } catch (Throwable e) {
                logDebug("NetworkUtils not found");
                return;
            }

            if (networkUtilsClass != null) {
                logDebug("Found NetworkUtils: " + networkUtilsClass.getName());
                hookNetworkUtilsMethods(networkUtilsClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook NetworkUtils methods
     */
    private void hookNetworkUtilsMethods(Class<?> networkUtilsClass) {
        try {
            Method[] methods = networkUtilsClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Hook downloadVideo and other download methods
                if (methodName.equals("downloadVideo") ||
                    methodName.toLowerCase().contains("download")) {

                    logDebug("Hooking NetworkUtils method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("NetworkUtils download method called: " + methodName);
                            // Log parameters
                            if (param.args != null && param.args.length > 0) {
                                for (int i = 0; i < param.args.length; i++) {
                                    if (param.args[i] != null) {
                                        logDebug("  Param[" + i + "]: " + param.args[i]);
                                    }
                                }
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("NetworkUtils download completed: " + methodName);
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
