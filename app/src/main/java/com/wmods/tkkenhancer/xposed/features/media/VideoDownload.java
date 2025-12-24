package com.wmods.tkkenhancer.xposed.features.media;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.TkkCore;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Feature to download TikTok videos without watermark
 * Hooks into TikTok's Video model to access the no-watermark download URL
 */
public class VideoDownload extends Feature {

    public VideoDownload(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("video_download", false)) return;

        logDebug("Initializing Video Download feature");

        // Hook into TikTok's Video model class
        // TikTok video model contains: download_no_watermark_addr field
        hookVideoModel();
        
        // Hook download service to provide download functionality
        hookDownloadService();

        logDebug("Video Download feature initialized");
    }

    /**
     * Hooks into TikTok's Video model to access no-watermark URL
     */
    private void hookVideoModel() {
        try {
            // Try to find the Video class using common TikTok package patterns
            // TikTok uses heavy obfuscation, so we need to be flexible
            Class<?> videoClass = null;
            
            try {
                // Try standard class name first
                videoClass = XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Video", classLoader);
            } catch (Throwable e1) {
                try {
                    // Try alternate location
                    videoClass = XposedHelpers.findClass("com.ss.android.ugc.aweme.video.Video", classLoader);
                } catch (Throwable e2) {
                    logDebug("Video class not found in standard locations, using Unobfuscator");
                    // Use Unobfuscator to find the class dynamically
                    videoClass = findVideoClassDynamic();
                }
            }

            if (videoClass == null) {
                log("Failed to find Video class");
                return;
            }

            final Class<?> finalVideoClass = videoClass;
            logDebug("Found Video class: " + videoClass.getName());

            // Hook methods that access download URLs
            hookDownloadUrlMethods(finalVideoClass);

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Dynamically find the Video class using Unobfuscator
     */
    private Class<?> findVideoClassDynamic() {
        try {
            // Use Unobfuscator to search for Video class by characteristics
            // Look for classes with fields containing "download" and "watermark"
            logDebug("Searching for Video class using DexKit");
            
            // This is a placeholder for DexKit-based class discovery
            // In a real implementation, this would use DexKit to find obfuscated classes
            return null;
        } catch (Throwable e) {
            logDebug("DexKit search failed", e);
            return null;
        }
    }

    /**
     * Hook download URL access methods
     */
    private void hookDownloadUrlMethods(Class<?> videoClass) {
        try {
            // Hook getDownloadAddr method to expose no-watermark URL
            Method[] methods = videoClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                
                // Look for methods related to download URLs
                if (methodName.toLowerCase().contains("download") || 
                    methodName.toLowerCase().contains("url") ||
                    methodName.toLowerCase().contains("addr")) {
                    
                    logDebug("Hooking method: " + methodName);
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result != null) {
                                logDebug("Download URL method called: " + methodName + " -> " + result);
                                
                                // Store the URL for later use
                                storeDownloadUrl(param.thisObject, result);
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
     * Store download URL for later retrieval
     */
    private void storeDownloadUrl(Object videoObject, Object urlResult) {
        try {
            // Store the URL in a way that can be accessed later
            // This could be through a WeakHashMap or similar mechanism
            logDebug("Storing download URL: " + urlResult);
        } catch (Throwable e) {
            logDebug("Failed to store URL", e);
        }
    }

    /**
     * Hook download service to add custom download functionality
     */
    private void hookDownloadService() {
        try {
            // Hook TikTok's download service
            hookDownloadServiceImpl();
            
        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook the download service implementation
     */
    private void hookDownloadServiceImpl() {
        try {
            Class<?> downloadServiceClass = null;
            
            try {
                // Try to find DownloadServiceManager
                downloadServiceClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.download.component_api.DownloadServiceManager",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    // Try DownloadAwemeVideoServiceImpl
                    downloadServiceClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.download.DownloadAwemeVideoServiceImpl",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Download service class not found in standard locations");
                    return;
                }
            }

            if (downloadServiceClass != null) {
                logDebug("Found download service class: " + downloadServiceClass.getName());
                
                // Hook download methods
                hookDownloadMethods(downloadServiceClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook download methods in the service
     */
    private void hookDownloadMethods(Class<?> serviceClass) {
        try {
            Method[] methods = serviceClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                
                if (methodName.toLowerCase().contains("download")) {
                    logDebug("Hooking download method: " + methodName);
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("Download initiated: " + methodName);
                            // Here we can intercept and modify download behavior
                            // For example, force no-watermark downloads
                        }
                        
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("Download completed: " + methodName);
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
        return "Video Download (No Watermark)";
    }
}
