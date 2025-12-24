package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Feed Filter Feature for TikTok
 * Provides custom filtering options for the feed:
 * - Filter by hashtags
 * - Filter by keywords
 * - Filter by user
 * - Custom content filters
 * 
 * Based on smali analysis:
 * - Feed classes: com.ss.android.ugc.aweme.feed.*
 * - FeedItemList and FeedAdapter
 * - Content filtering mechanisms
 */
public class FeedFilter extends Feature {

    private List<String> blockedKeywords = new ArrayList<>();
    private List<String> blockedHashtags = new ArrayList<>();
    private List<String> blockedUsers = new ArrayList<>();

    public FeedFilter(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
        loadFilterLists();
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("feed_filter", false)) return;

        logDebug("Initializing Feed Filter feature");

        try {
            // Hook feed loading to apply filters
            hookFeedLoading();
            
            // Hook feed item processing
            hookFeedItemProcessing();

            logDebug("Feed Filter feature initialized");
        } catch (Exception e) {
            logDebug("Failed to initialize Feed Filter: " + e.getMessage());
        }
    }

    /**
     * Load filter lists from preferences
     */
    private void loadFilterLists() {
        try {
            String keywords = prefs.getString("blocked_keywords", "");
            if (!keywords.isEmpty()) {
                String[] items = keywords.split(",");
                for (String item : items) {
                    blockedKeywords.add(item.trim().toLowerCase());
                }
            }

            String hashtags = prefs.getString("blocked_hashtags", "");
            if (!hashtags.isEmpty()) {
                String[] items = hashtags.split(",");
                for (String item : items) {
                    blockedHashtags.add(item.trim().toLowerCase());
                }
            }

            String users = prefs.getString("blocked_users", "");
            if (!users.isEmpty()) {
                String[] items = users.split(",");
                for (String item : items) {
                    blockedUsers.add(item.trim().toLowerCase());
                }
            }

            logDebug("Loaded " + blockedKeywords.size() + " keywords, " + 
                    blockedHashtags.size() + " hashtags, " + 
                    blockedUsers.size() + " users");
        } catch (Exception e) {
            logDebug("Failed to load filter lists: " + e.getMessage());
        }
    }

    /**
     * Hook feed loading to filter content
     */
    private void hookFeedLoading() {
        try {
            Class<?> feedFilterClass = Unobfuscator.loadTikTokFeedFilterClass(classLoader);
            if (feedFilterClass == null) {
                logDebug("Feed filter class not found");
                hookFeedLoadingBySearch();
                return;
            }

            // Hook methods that return List
            for (Method method : feedFilterClass.getDeclaredMethods()) {
                if (method.getReturnType() == List.class) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result instanceof List) {
                                List<?> feedItems = (List<?>) result;
                                List<Object> filtered = filterFeedItems(feedItems);
                                
                                if (filtered.size() < feedItems.size()) {
                                    logDebug("Filtered " + (feedItems.size() - filtered.size()) + " items from feed");
                                    param.setResult(filtered);
                                }
                            }
                        }
                    });
                }
            }

            logDebug("Hooked feed loading methods in " + feedFilterClass.getName());
        } catch (Exception e) {
            logDebug("Failed to hook feed loading: " + e.getMessage());
        }
    }

    /**
     * Hook feed loading by searching for feed classes
     */
    private void hookFeedLoadingBySearch() {
        try {
            String[] feedClasses = {
                "com.ss.android.ugc.aweme.feed.model.FeedItemList",
                "com.ss.android.ugc.aweme.feed.adapter.FeedAdapter",
                "com.ss.android.ugc.aweme.homepage.api.data.HomePageDataViewModel"
            };

            for (String className : feedClasses) {
                try {
                    Class<?> clazz = XposedHelpers.findClass(className, classLoader);
                    
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.getReturnType() == List.class) {
                            XposedBridge.hookMethod(method, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    Object result = param.getResult();
                                    if (result instanceof List) {
                                        List<?> items = (List<?>) result;
                                        if (!items.isEmpty()) {
                                            List<Object> filtered = filterFeedItems(items);
                                            if (filtered.size() != items.size()) {
                                                param.setResult(filtered);
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            logDebug("Failed to hook feed by search: " + e.getMessage());
        }
    }

    /**
     * Hook individual feed item processing
     */
    private void hookFeedItemProcessing() {
        try {
            Class<?> awemeClass = Unobfuscator.loadTikTokFeedItemClass(classLoader);
            if (awemeClass == null) return;

            // Hook getters to inspect feed items
            for (Method method : awemeClass.getDeclaredMethods()) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.startsWith("get") || methodName.startsWith("is")) && 
                    method.getParameterCount() == 0) {
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result != null) {
                                String resultStr = result.toString().toLowerCase();
                                
                                // Check if item should be filtered
                                boolean shouldFilter = false;
                                
                                // Check keywords
                                for (String keyword : blockedKeywords) {
                                    if (resultStr.contains(keyword)) {
                                        shouldFilter = true;
                                        logDebug("Filtering item by keyword: " + keyword);
                                        break;
                                    }
                                }
                                
                                // Check hashtags
                                for (String hashtag : blockedHashtags) {
                                    if (resultStr.contains(hashtag)) {
                                        shouldFilter = true;
                                        logDebug("Filtering item by hashtag: " + hashtag);
                                        break;
                                    }
                                }
                                
                                if (shouldFilter) {
                                    // Mark item for filtering
                                    XposedHelpers.setAdditionalInstanceField(
                                        param.thisObject,
                                        "tiktok_enhancer_filter_flag",
                                        true
                                    );
                                }
                            }
                        }
                    });
                }
            }

            logDebug("Hooked feed item processing methods");
        } catch (Exception e) {
            logDebug("Failed to hook feed item processing: " + e.getMessage());
        }
    }

    /**
     * Filter a list of feed items based on configured filters
     */
    private List<Object> filterFeedItems(List<?> items) {
        List<Object> filtered = new ArrayList<>();
        
        for (Object item : items) {
            if (item == null) continue;
            
            try {
                // Check if item is marked for filtering
                Object filterFlag = XposedHelpers.getAdditionalInstanceField(
                    item,
                    "tiktok_enhancer_filter_flag"
                );
                
                if (filterFlag != null && (Boolean) filterFlag) {
                    // Skip this item
                    continue;
                }
                
                // Check item content
                String itemStr = item.toString().toLowerCase();
                boolean shouldFilter = false;
                
                // Check against filter lists
                for (String keyword : blockedKeywords) {
                    if (itemStr.contains(keyword)) {
                        shouldFilter = true;
                        break;
                    }
                }
                
                if (!shouldFilter) {
                    for (String hashtag : blockedHashtags) {
                        if (itemStr.contains(hashtag)) {
                            shouldFilter = true;
                            break;
                        }
                    }
                }
                
                if (!shouldFilter) {
                    for (String user : blockedUsers) {
                        if (itemStr.contains(user)) {
                            shouldFilter = true;
                            break;
                        }
                    }
                }
                
                if (!shouldFilter) {
                    filtered.add(item);
                }
            } catch (Exception e) {
                // If error, include item by default
                filtered.add(item);
            }
        }
        
        return filtered;
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "FeedFilter";
    }
}
