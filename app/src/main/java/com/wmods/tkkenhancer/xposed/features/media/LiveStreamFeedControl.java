package com.wmods.tkkenhancer.xposed.features.media;

import android.view.View;
import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Live Stream Feed Control Feature for TikTok
 * Allows users to enable/disable live streams from appearing in the feed
 * 
 * Based on smali analysis:
 * - Hooks: Aweme.isLive() and Aweme.isLiveNoDeduplicate()
 * - Feed filtering to remove live stream items when disabled
 * 
 * @author TikTok Enhancer
 */
public class LiveStreamFeedControl extends Feature {

    private static final String TAG = "LiveStreamFeedControl";

    public LiveStreamFeedControl(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("hide_live_streams", false)) return;

        logDebug("Initializing Live Stream Feed Control");

        try {
            // Load TikTok Aweme class
            Class<?> awemeClass = Unobfuscator.loadTikTokFeedItemClass(classLoader);
            if (awemeClass == null) {
                logDebug("Could not find Aweme class");
                return;
            }

            logDebug("Found Aweme class: " + awemeClass.getName());

            // Hook isLive() method to filter live streams in feed
            hookIsLiveMethod(awemeClass);

            // Hook feed list loading to filter out live streams
            hookFeedItemList();

            logDebug("Live Stream Feed Control initialized successfully");
        } catch (Throwable e) {
            logDebug("Error initializing Live Stream Feed Control: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook the isLive() method to control live stream visibility
     */
    private void hookIsLiveMethod(Class<?> awemeClass) {
        try {
            Method isLiveMethod = awemeClass.getDeclaredMethod("isLive");
            XposedBridge.hookMethod(isLiveMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean("hide_live_streams", false)) return;
                    
                    boolean isLive = (boolean) param.getResult();
                    if (isLive) {
                        logDebug("Detected live stream in feed - marking as non-live");
                        // Return false to hide the live stream from feed
                        param.setResult(false);
                    }
                }
            });
            logDebug("Hooked isLive() method successfully");
        } catch (Throwable e) {
            logDebug("Could not hook isLive() method: " + e.getMessage());
        }
    }

    /**
     * Hook feed item list to filter out live streams
     */
    private void hookFeedItemList() {
        try {
            Class<?> feedItemListClass = XposedHelpers.findClass(
                "com.ss.android.ugc.aweme.feed.model.FeedItemList",
                classLoader
            );

            if (feedItemListClass == null) {
                logDebug("Could not find FeedItemList class");
                return;
            }

            // Hook the items list getter to filter live streams
            XposedHelpers.findAndHookMethod(feedItemListClass, "getItems",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!prefs.getBoolean("hide_live_streams", false)) return;

                        Object result = param.getResult();
                        if (result instanceof List) {
                            List<?> items = (List<?>) result;
                            Iterator<?> iterator = items.iterator();
                            int removedCount = 0;

                            while (iterator.hasNext()) {
                                Object item = iterator.next();
                                try {
                                    // Check if this item is a live stream
                                    Boolean isLive = (Boolean) XposedHelpers.callMethod(item, "isLive");
                                    if (isLive != null && isLive) {
                                        iterator.remove();
                                        removedCount++;
                                    }
                                } catch (Throwable ignored) {
                                    // Item might not have isLive() method, skip it
                                }
                            }

                            if (removedCount > 0) {
                                logDebug("Filtered out " + removedCount + " live stream(s) from feed");
                            }
                        }
                    }
                }
            );
            logDebug("Hooked FeedItemList successfully");
        } catch (Throwable e) {
            logDebug("Could not hook FeedItemList: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return TAG;
    }
}
