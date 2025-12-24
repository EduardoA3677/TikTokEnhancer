package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Improved Video Download feature based on smali analysis
 * Hooks into TikTok's Video model to access the no-watermark download URL
 * 
 * Key findings from smali analysis:
 * - Aweme class: com.ss.android.ugc.aweme.feed.model.Aweme
 * - Video class: com.ss.android.ugc.aweme.feed.model.Video
 * - UrlModel class: com.ss.android.ugc.aweme.base.model.UrlModel
 * - Field: downloadNoWatermarkAddr:Lcom/ss/android/ugc/aweme/base/model/UrlModel;
 * - Method: getDownloadNoWatermarkAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
 * - Method: getDownloadAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
 */
public class VideoDownloadImproved extends Feature {

    public VideoDownloadImproved(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("video_download", false)) return;

        logDebug("Initializing Improved Video Download feature");

        // Hook into TikTok's Video model class
        hookVideoModelMethods();
        
        // Hook Aweme getVideo method to intercept video access
        hookAwemeVideoAccess();
        
        // Hook prevent download check
        hookPreventDownloadCheck();

        logDebug("Improved Video Download feature initialized");
    }

    /**
     * Hook Video model methods to provide no-watermark URLs
     */
    private void hookVideoModelMethods() {
        try {
            // Load the Video class
            Class<?> videoClass = Unobfuscator.loadTikTokVideoClass(classLoader);
            if (videoClass == null) {
                log("Failed to find Video class");
                return;
            }

            logDebug("Found Video class: " + videoClass.getName());

            // Hook getDownloadNoWatermarkAddr method
            hookNoWatermarkMethod(videoClass);
            
            // Hook getDownloadAddr method to log and potentially modify
            hookDownloadAddrMethod(videoClass);

        } catch (Throwable e) {
            log("Error hooking Video model: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook getDownloadNoWatermarkAddr method
     * Based on smali: .method public getDownloadNoWatermarkAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
     */
    private void hookNoWatermarkMethod(Class<?> videoClass) {
        try {
            Method method = Unobfuscator.loadTikTokNoWatermarkUrlMethod(classLoader);
            if (method == null) {
                logDebug("No-watermark method not found, trying direct access");
                method = videoClass.getDeclaredMethod("getDownloadNoWatermarkAddr");
            }

            final Method finalMethod = method;
            logDebug("Hooking getDownloadNoWatermarkAddr method");

            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object result = param.getResult();
                    if (result != null) {
                        logDebug("No-watermark URL retrieved: " + extractUrlFromModel(result));
                        
                        // Store for UI access
                        XposedHelpers.setAdditionalInstanceField(
                            param.thisObject, 
                            "tiktok_enhancer_nowatermark_url", 
                            result
                        );
                    }
                }
            });

        } catch (Throwable e) {
            logDebug("Error hooking getDownloadNoWatermarkAddr: " + e.getMessage());
        }
    }

    /**
     * Hook getDownloadAddr method
     * Based on smali: .method public getDownloadAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
     */
    private void hookDownloadAddrMethod(Class<?> videoClass) {
        try {
            Method method = Unobfuscator.loadTikTokDownloadUrlMethod(classLoader);
            if (method == null) {
                logDebug("Download addr method not found, trying direct access");
                method = videoClass.getDeclaredMethod("getDownloadAddr");
            }

            logDebug("Hooking getDownloadAddr method");

            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object result = param.getResult();
                    
                    // Check if we should replace with no-watermark URL
                    boolean forceNoWatermark = prefs.getBoolean("force_no_watermark", true);
                    
                    if (forceNoWatermark) {
                        // Try to get no-watermark URL instead
                        try {
                            Object videoObj = param.thisObject;
                            Object noWatermarkUrl = XposedHelpers.getAdditionalInstanceField(
                                videoObj, 
                                "tiktok_enhancer_nowatermark_url"
                            );
                            
                            if (noWatermarkUrl != null) {
                                logDebug("Replacing download URL with no-watermark version");
                                param.setResult(noWatermarkUrl);
                            }
                        } catch (Throwable e) {
                            logDebug("Failed to replace with no-watermark URL: " + e.getMessage());
                        }
                    }
                    
                    if (result != null) {
                        logDebug("Download URL retrieved: " + extractUrlFromModel(result));
                    }
                }
            });

        } catch (Throwable e) {
            logDebug("Error hooking getDownloadAddr: " + e.getMessage());
        }
    }

    /**
     * Hook Aweme's getVideo method to intercept video access
     * Based on smali: .method public getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;
     */
    private void hookAwemeVideoAccess() {
        try {
            Method getVideoMethod = Unobfuscator.loadTikTokGetVideoMethod(classLoader);
            if (getVideoMethod == null) {
                logDebug("getVideo method not found");
                return;
            }

            logDebug("Hooking Aweme.getVideo method");

            XposedBridge.hookMethod(getVideoMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object videoObj = param.getResult();
                    if (videoObj != null) {
                        logDebug("Video object accessed from Aweme");
                        
                        // Pre-fetch no-watermark URL to cache it
                        try {
                            Method noWatermarkMethod = Unobfuscator.loadTikTokNoWatermarkUrlMethod(classLoader);
                            if (noWatermarkMethod != null) {
                                Object noWatermarkUrl = noWatermarkMethod.invoke(videoObj);
                                if (noWatermarkUrl != null) {
                                    logDebug("Pre-cached no-watermark URL");
                                }
                            }
                        } catch (Throwable e) {
                            logDebug("Failed to pre-cache no-watermark URL: " + e.getMessage());
                        }
                    }
                }
            });

        } catch (Throwable e) {
            logDebug("Error hooking Aweme.getVideo: " + e.getMessage());
        }
    }

    /**
     * Hook prevent download check to always allow downloads
     * Based on smali: preventDownload field
     */
    private void hookPreventDownloadCheck() {
        try {
            Method preventDownloadMethod = Unobfuscator.loadTikTokPreventDownloadMethod(classLoader);
            if (preventDownloadMethod == null) {
                logDebug("Prevent download method not found - may not exist in this version");
                return;
            }

            logDebug("Hooking prevent download check");

            XposedBridge.hookMethod(preventDownloadMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // Always allow downloads
                    param.setResult(false);
                    logDebug("Bypassed prevent download check");
                }
            });

        } catch (Throwable e) {
            logDebug("Error hooking prevent download: " + e.getMessage());
        }
    }

    /**
     * Extract URL string from UrlModel object
     */
    private String extractUrlFromModel(Object urlModel) {
        if (urlModel == null) return "null";
        
        try {
            // Try to get URL list from UrlModel
            Method getUrlListMethod = Unobfuscator.loadTikTokUrlListMethod(classLoader);
            if (getUrlListMethod != null) {
                Object urlListObj = getUrlListMethod.invoke(urlModel);
                if (urlListObj instanceof List) {
                    List<?> urlList = (List<?>) urlListObj;
                    if (!urlList.isEmpty()) {
                        return urlList.get(0).toString();
                    }
                }
            }
        } catch (Throwable e) {
            logDebug("Failed to extract URL from model: " + e.getMessage());
        }
        
        return urlModel.toString();
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Video Download (No Watermark) - Improved";
    }
}
