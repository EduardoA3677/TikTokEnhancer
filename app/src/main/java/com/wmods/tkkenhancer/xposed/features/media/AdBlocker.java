package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Feature to block advertisements in TikTok feed
 * Filters out sponsored content and commercial posts
 */
public class AdBlocker extends Feature {

    public AdBlocker(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("ad_blocker", false)) return;

        logDebug("Initializing Ad Blocker feature");

        // Hook into TikTok's feed loading mechanism
        hookFeedLoader();
        
        // Hook commercialize/ad detection
        hookAdDetection();
        
        // Hook sponsored content filter
        hookSponsoredContent();

        logDebug("Ad Blocker feature initialized");
    }

    /**
     * Hook feed loader to filter out ads
     */
    private void hookFeedLoader() {
        try {
            // Try to find feed-related classes
            Class<?> feedClass = null;
            
            try {
                feedClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.feed.model.FeedItemList",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    feedClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.feed.adapter.FeedAdapter",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Feed class not found in standard locations");
                    return;
                }
            }

            if (feedClass != null) {
                logDebug("Found feed class: " + feedClass.getName());
                hookFeedMethods(feedClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook feed methods to filter ads
     */
    private void hookFeedMethods(Class<?> feedClass) {
        try {
            Method[] methods = feedClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                
                // Look for methods that return lists (likely feed items)
                if (method.getReturnType() == List.class ||
                    methodName.toLowerCase().contains("list") ||
                    methodName.toLowerCase().contains("items")) {
                    
                    logDebug("Hooking feed method: " + methodName);
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result instanceof List) {
                                List<?> feedItems = (List<?>) result;
                                filterAdsFromList(feedItems);
                                logDebug("Filtered " + feedItems.size() + " feed items");
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
     * Filter ads from a list of feed items
     */
    private void filterAdsFromList(List<?> items) {
        try {
            items.removeIf(item -> {
                if (item == null) return false;
                
                boolean isAd = isAdItem(item);
                if (isAd) {
                    logDebug("Removed ad item: " + item.getClass().getName());
                }
                return isAd;
            });
        } catch (Throwable e) {
            logDebug("Failed to filter ads", e);
        }
    }

    /**
     * Check if an item is an advertisement
     */
    private boolean isAdItem(Object item) {
        try {
            Class<?> itemClass = item.getClass();
            
            // Check for ad-related fields
            try {
                Object isAd = XposedHelpers.getObjectField(item, "isAd");
                if (isAd instanceof Boolean && (Boolean) isAd) {
                    return true;
                }
            } catch (Throwable ignored) {}
            
            try {
                Object adLabel = XposedHelpers.getObjectField(item, "adLabel");
                if (adLabel != null) {
                    return true;
                }
            } catch (Throwable ignored) {}
            
            try {
                Object commercialize = XposedHelpers.getObjectField(item, "commercialize");
                if (commercialize != null) {
                    return true;
                }
            } catch (Throwable ignored) {}
            
            try {
                Object sponsored = XposedHelpers.getObjectField(item, "sponsored");
                if (sponsored instanceof Boolean && (Boolean) sponsored) {
                    return true;
                }
            } catch (Throwable ignored) {}
            
            // Check class name for ad indicators
            String className = itemClass.getName().toLowerCase();
            if (className.contains("ad") || 
                className.contains("commercialize") || 
                className.contains("sponsor")) {
                return true;
            }
            
        } catch (Throwable e) {
            logDebug("Error checking ad item", e);
        }
        
        return false;
    }

    /**
     * Hook ad detection mechanisms
     */
    private void hookAdDetection() {
        try {
            // Try to find commercialize classes
            Class<?> commercializeClass = null;
            
            try {
                commercializeClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.commercialize.media.impl.utils.CommercializeMediaServiceImpl",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    commercializeClass = XposedHelpers.findClass(
                        "com.bytedance.ies.ugc.aweme.commercialize.splash.service.CommercializeSplashServiceImpl",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Commercialize class not found");
                    return;
                }
            }

            if (commercializeClass != null) {
                logDebug("Found commercialize class: " + commercializeClass.getName());
                hookCommercializeMethods(commercializeClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook commercialize methods
     */
    private void hookCommercializeMethods(Class<?> commercializeClass) {
        try {
            Method[] methods = commercializeClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                
                // Hook methods that might show ads
                if (methodName.toLowerCase().contains("show") ||
                    methodName.toLowerCase().contains("display") ||
                    methodName.toLowerCase().contains("load")) {
                    
                    logDebug("Hooking commercialize method: " + methodName);
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Block ad display
                            logDebug("Blocked ad display: " + methodName);
                            param.setResult(null);
                        }
                    });
                }
            }
        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook sponsored content detection
     */
    private void hookSponsoredContent() {
        try {
            // Try to find ad-related classes
            Class<?> adClass = null;
            
            try {
                adClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.ad.feed.FeedAdServiceImpl",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    adClass = XposedHelpers.findClass(
                        "com.bytedance.ies.ad.base.AdBaseServiceImpl",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Ad service class not found");
                    return;
                }
            }

            if (adClass != null) {
                logDebug("Found ad service class: " + adClass.getName());
                hookAdServiceMethods(adClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook ad service methods
     */
    private void hookAdServiceMethods(Class<?> adClass) {
        try {
            Method[] methods = adClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                
                // Hook methods that load or display ads
                if (methodName.toLowerCase().contains("load") ||
                    methodName.toLowerCase().contains("show") ||
                    methodName.toLowerCase().contains("request")) {
                    
                    logDebug("Hooking ad service method: " + methodName);
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Block ad loading/showing
                            logDebug("Blocked ad service call: " + methodName);
                            
                            // Return appropriate empty/null values based on return type
                            Class<?> returnType = method.getReturnType();
                            if (returnType == boolean.class || returnType == Boolean.class) {
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
            log(e);
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Ad Blocker";
    }
}
