package com.wmods.tkkenhancer.xposed.features.media;

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
 * Image Reel Control Feature for TikTok
 * Allows users to enable/disable photo/image reels (photo mode posts) from appearing in the feed
 * 
 * Based on smali analysis:
 * - Hooks: Aweme.isPhotoMode() and Aweme.isImage()
 * - Feed filtering to remove image reel items when disabled
 * 
 * @author TikTok Enhancer
 */
public class ImageReelControl extends Feature {

    private static final String TAG = "ImageReelControl";

    public ImageReelControl(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("hide_image_reels", false)) return;

        logDebug("Initializing Image Reel Control");

        try {
            // Load TikTok Aweme class
            Class<?> awemeClass = Unobfuscator.loadTikTokFeedItemClass(classLoader);
            if (awemeClass == null) {
                logDebug("Could not find Aweme class");
                return;
            }

            logDebug("Found Aweme class: " + awemeClass.getName());

            // Hook isPhotoMode() method to filter image reels in feed
            hookIsPhotoModeMethod(awemeClass);

            // Hook isImage() method as backup
            hookIsImageMethod(awemeClass);

            // Hook feed list loading to filter out image reels
            hookFeedItemList();

            logDebug("Image Reel Control initialized successfully");
        } catch (Throwable e) {
            logDebug("Error initializing Image Reel Control: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook the isPhotoMode() method to control image reel visibility
     */
    private void hookIsPhotoModeMethod(Class<?> awemeClass) {
        try {
            Method isPhotoModeMethod = awemeClass.getDeclaredMethod("isPhotoMode");
            XposedBridge.hookMethod(isPhotoModeMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean("hide_image_reels", false)) return;
                    
                    boolean isPhotoMode = (boolean) param.getResult();
                    if (isPhotoMode) {
                        logDebug("Detected photo mode post in feed - marking as non-photo");
                        // Return false to hide the photo mode post from feed
                        param.setResult(false);
                    }
                }
            });
            logDebug("Hooked isPhotoMode() method successfully");
        } catch (Throwable e) {
            logDebug("Could not hook isPhotoMode() method: " + e.getMessage());
        }
    }

    /**
     * Hook the isImage() method as backup to control image visibility
     */
    private void hookIsImageMethod(Class<?> awemeClass) {
        try {
            Method isImageMethod = awemeClass.getDeclaredMethod("isImage");
            XposedBridge.hookMethod(isImageMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!prefs.getBoolean("hide_image_reels", false)) return;
                    
                    boolean isImage = (boolean) param.getResult();
                    if (isImage) {
                        logDebug("Detected image post in feed - marking as non-image");
                        // Return false to hide the image post from feed
                        param.setResult(false);
                    }
                }
            });
            logDebug("Hooked isImage() method successfully");
        } catch (Throwable e) {
            logDebug("Could not hook isImage() method: " + e.getMessage());
        }
    }

    /**
     * Hook feed item list to filter out image reels
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

            // Hook the items list getter to filter image reels
            XposedHelpers.findAndHookMethod(feedItemListClass, "getItems",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!prefs.getBoolean("hide_image_reels", false)) return;

                        Object result = param.getResult();
                        if (result instanceof List) {
                            List<?> items = (List<?>) result;
                            Iterator<?> iterator = items.iterator();
                            int removedCount = 0;

                            while (iterator.hasNext()) {
                                Object item = iterator.next();
                                try {
                                    // Check if this item is a photo mode post
                                    Boolean isPhotoMode = (Boolean) XposedHelpers.callMethod(item, "isPhotoMode");
                                    if (isPhotoMode != null && isPhotoMode) {
                                        iterator.remove();
                                        removedCount++;
                                        continue;
                                    }

                                    // Also check isImage() as backup
                                    Boolean isImage = (Boolean) XposedHelpers.callMethod(item, "isImage");
                                    if (isImage != null && isImage) {
                                        iterator.remove();
                                        removedCount++;
                                    }
                                } catch (Throwable ignored) {
                                    // Item might not have these methods, skip it
                                }
                            }

                            if (removedCount > 0) {
                                logDebug("Filtered out " + removedCount + " image reel(s) from feed");
                            }
                        }
                    }
                }
            );
            logDebug("Hooked FeedItemList for image filtering successfully");
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
