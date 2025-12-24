package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Improved Ad Blocker feature based on smali analysis
 * Filters out sponsored content and commercial posts
 * 
 * Key findings from smali analysis:
 * - Aweme class has isAd field: .field public isAd:Z
 * - Method: isAd()Z that checks the field
 * - Method: isAdTraffic()Z that calls isAd()
 * - Field: adAwemeSource:I for ad source tracking
 * - Field: commercialVideoInfo for commercial content
 */
public class AdBlockerImproved extends Feature {

    public AdBlockerImproved(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("ad_blocker", false)) return;

        logDebug("Initializing Improved Ad Blocker feature");

        // Hook isAd() method in Aweme class
        hookIsAdMethod();
        
        // Hook isAdTraffic() method
        hookIsAdTrafficMethod();
        
        // Hook feed list loading to filter ads
        hookFeedListLoader();
        
        // Hook commercialize services
        hookCommercializeServices();

        logDebug("Improved Ad Blocker feature initialized");
    }

    /**
     * Hook isAd() method in Aweme class
     * Based on smali: .method public isAd()Z
     */
    private void hookIsAdMethod() {
        try {
            Method isAdMethod = Unobfuscator.loadTikTokIsAdMethod(classLoader);
            if (isAdMethod == null) {
                log("Failed to find isAd method");
                return;
            }

            logDebug("Hooking isAd method: " + isAdMethod.getName());

            XposedBridge.hookMethod(isAdMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean isAd = (boolean) param.getResult();
                    if (isAd) {
                        logDebug("Detected ad item via isAd method");
                        
                        // Mark for removal
                        XposedHelpers.setAdditionalInstanceField(
                            param.thisObject,
                            "tiktok_enhancer_is_ad_blocked",
                            true
                        );
                    }
                }
            });

        } catch (Throwable e) {
            log("Error hooking isAd method: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook isAdTraffic() method in Aweme class
     * Based on smali: .method public isAdTraffic()Z
     */
    private void hookIsAdTrafficMethod() {
        try {
            Method isAdTrafficMethod = Unobfuscator.loadTikTokIsAdTrafficMethod(classLoader);
            if (isAdTrafficMethod == null) {
                logDebug("isAdTraffic method not found - may not exist in this version");
                return;
            }

            logDebug("Hooking isAdTraffic method");

            XposedBridge.hookMethod(isAdTrafficMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean isAdTraffic = (boolean) param.getResult();
                    if (isAdTraffic) {
                        logDebug("Detected ad traffic via isAdTraffic method");
                        
                        // Mark for removal
                        XposedHelpers.setAdditionalInstanceField(
                            param.thisObject,
                            "tiktok_enhancer_is_ad_blocked",
                            true
                        );
                    }
                }
            });

        } catch (Throwable e) {
            logDebug("Error hooking isAdTraffic method: " + e.getMessage());
        }
    }

    /**
     * Hook feed list loading to filter out ads
     */
    private void hookFeedListLoader() {
        try {
            // Hook FeedItemList class that contains the feed items
            Class<?> feedItemListClass = XposedHelpers.findClass(
                "com.ss.android.ugc.aweme.feed.model.FeedItemList",
                classLoader
            );

            if (feedItemListClass == null) {
                logDebug("FeedItemList class not found");
                return;
            }

            logDebug("Found FeedItemList class: " + feedItemListClass.getName());

            // Hook methods that return lists
            for (Method method : feedItemListClass.getDeclaredMethods()) {
                if (method.getReturnType() == List.class && 
                    method.getParameterCount() == 0) {
                    
                    logDebug("Hooking FeedItemList method: " + method.getName());
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result instanceof List) {
                                List<?> feedItems = (List<?>) result;
                                int originalSize = feedItems.size();
                                
                                // Filter out ads
                                filterAds(feedItems);
                                
                                int removedCount = originalSize - feedItems.size();
                                if (removedCount > 0) {
                                    logDebug("Filtered " + removedCount + " ads from feed");
                                }
                            }
                        }
                    });
                }
            }

        } catch (Throwable e) {
            logDebug("Error hooking feed list loader: " + e.getMessage());
        }
    }

    /**
     * Filter ads from a list of Aweme items
     */
    private void filterAds(List<?> items) {
        try {
            Class<?> awemeClass = Unobfuscator.loadTikTokFeedItemClass(classLoader);
            Method isAdMethod = Unobfuscator.loadTikTokIsAdMethod(classLoader);
            
            if (awemeClass == null || isAdMethod == null) {
                logDebug("Cannot filter ads - missing classes/methods");
                return;
            }

            items.removeIf(item -> {
                if (item == null || !awemeClass.isInstance(item)) {
                    return false;
                }
                
                try {
                    // Check if marked as blocked
                    Object isBlocked = XposedHelpers.getAdditionalInstanceField(
                        item,
                        "tiktok_enhancer_is_ad_blocked"
                    );
                    if (Boolean.TRUE.equals(isBlocked)) {
                        return true;
                    }
                    
                    // Check isAd field directly
                    boolean isAd = (boolean) isAdMethod.invoke(item);
                    if (isAd) {
                        logDebug("Removing ad item from feed");
                        return true;
                    }
                    
                    // Check isAd field directly via reflection (backup method)
                    try {
                        Field isAdField = awemeClass.getDeclaredField("isAd");
                        isAdField.setAccessible(true);
                        boolean isAdDirect = isAdField.getBoolean(item);
                        if (isAdDirect) {
                            logDebug("Removing ad item (direct field check)");
                            return true;
                        }
                    } catch (Throwable ignored) {}
                    
                    // Check adAwemeSource field
                    try {
                        Field adSourceField = awemeClass.getDeclaredField("adAwemeSource");
                        adSourceField.setAccessible(true);
                        int adSource = adSourceField.getInt(item);
                        if (adSource > 0) {
                            logDebug("Removing ad item (adAwemeSource check)");
                            return true;
                        }
                    } catch (Throwable ignored) {}
                    
                    // Check commercialVideoInfo field
                    try {
                        Field commercialField = awemeClass.getDeclaredField("commercialVideoInfo");
                        commercialField.setAccessible(true);
                        Object commercial = commercialField.get(item);
                        if (commercial != null) {
                            logDebug("Removing ad item (commercial video info)");
                            return true;
                        }
                    } catch (Throwable ignored) {}
                    
                } catch (Throwable e) {
                    logDebug("Error checking ad status: " + e.getMessage());
                }
                
                return false;
            });

        } catch (Throwable e) {
            logDebug("Error filtering ads: " + e.getMessage());
        }
    }

    /**
     * Hook commercialize services to block ad loading
     */
    private void hookCommercializeServices() {
        try {
            Class<?> commercializeClass = Unobfuscator.loadTikTokAdClass(classLoader);
            if (commercializeClass == null) {
                logDebug("Commercialize class not found");
                return;
            }

            logDebug("Found commercialize class: " + commercializeClass.getName());

            // Hook methods that load or show ads
            for (Method method : commercializeClass.getDeclaredMethods()) {
                String methodName = method.getName().toLowerCase();
                
                if (methodName.contains("load") || 
                    methodName.contains("show") || 
                    methodName.contains("display") ||
                    methodName.contains("request")) {
                    
                    logDebug("Hooking commercialize method: " + method.getName());
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("Blocked commercialize call: " + method.getName());
                            
                            // Return appropriate value based on return type
                            Class<?> returnType = method.getReturnType();
                            if (returnType == void.class) {
                                param.setResult(null);
                            } else if (returnType == boolean.class || returnType == Boolean.class) {
                                param.setResult(false);
                            } else if (returnType == int.class || returnType == Integer.class) {
                                param.setResult(0);
                            } else {
                                param.setResult(null);
                            }
                        }
                    });
                }
            }

        } catch (Throwable e) {
            logDebug("Error hooking commercialize services: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Ad Blocker - Improved";
    }
}
