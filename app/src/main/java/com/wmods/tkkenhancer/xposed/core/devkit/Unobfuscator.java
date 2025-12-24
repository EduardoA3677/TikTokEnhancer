package com.wmods.tkkenhancer.xposed.core.devkit;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.wmods.tkkenhancer.xposed.core.TkkCore;
import com.wmods.tkkenhancer.xposed.core.components.FMessageTkk;
import com.wmods.tkkenhancer.xposed.utils.ReflectionUtils;
import com.wmods.tkkenhancer.xposed.utils.Utils;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.OpCodeMatchType;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.result.UsingFieldData;
import org.luckypray.dexkit.util.DexSignUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class Unobfuscator {

    private static final String TAG = "Unobfuscator";
    private static DexKitBridge dexkit;

    public static final HashMap<String, Class<?>> cacheClasses = new HashMap<>();

    static {
        System.loadLibrary("dexkit");
    }

    public static boolean initWithPath(String path) {
        try {
            dexkit = DexKitBridge.create(path);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // TODO: Functions to find classes and methods
    public synchronized static Method findFirstMethodUsingStrings(ClassLoader classLoader, StringMatchType type, String... strings) throws Exception {
        MethodMatcher matcher = new MethodMatcher();
        for (String string : strings) {
            matcher.addUsingString(string, type);
        }
        MethodDataList result = dexkit.findMethod(FindMethod.create().matcher(matcher));
        if (result.isEmpty()) return null;
        for (MethodData methodData : result) {
            if (methodData.isMethod()) return methodData.getMethodInstance(classLoader);
        }
        return null;
    }

    public synchronized static Method findFirstMethodUsingStringsFilter(ClassLoader classLoader, String packageFilter, StringMatchType type, String... strings) throws Exception {
        MethodMatcher matcher = new MethodMatcher();
        for (String string : strings) {
            matcher.addUsingString(string, type);
        }
        MethodDataList result = dexkit.findMethod(FindMethod.create().searchPackages(packageFilter).matcher(matcher));
        if (result.isEmpty()) return null;

        for (MethodData methodData : result) {
            if (methodData.isMethod()) return methodData.getMethodInstance(classLoader);
        }
        throw new NoSuchMethodException();
    }

    public synchronized static Method[] findAllMethodUsingStrings(ClassLoader classLoader, StringMatchType type, String... strings) {
        MethodMatcher matcher = new MethodMatcher();
        for (String string : strings) {
            matcher.addUsingString(string, type);
        }
        MethodDataList result = dexkit.findMethod(FindMethod.create().matcher(matcher));
        if (result.isEmpty()) return new Method[0];
        return result.stream().filter(MethodData::isMethod).map(methodData -> convertRealMethod(methodData, classLoader)).filter(Objects::nonNull).toArray(Method[]::new);
    }

    public synchronized static Class<?> findFirstClassUsingStrings(ClassLoader classLoader, StringMatchType type, String... strings) throws Exception {
        var matcher = new ClassMatcher();
        for (String string : strings) {
            matcher.addUsingString(string, type);
        }
        var result = dexkit.findClass(FindClass.create().matcher(matcher));
        if (result.isEmpty()) return null;
        return result.get(0).getInstance(classLoader);
    }


    public synchronized static Class<?>[] findAllClassUsingStrings(ClassLoader classLoader, StringMatchType type, String... strings) throws Exception {
        var matcher = new ClassMatcher();
        for (String string : strings) {
            matcher.addUsingString(string, type);
        }
        var result = dexkit.findClass(FindClass.create().matcher(matcher));
        if (result.isEmpty()) return null;
        return result.stream().map(classData -> convertRealClass(classData, classLoader)).filter(Objects::nonNull).toArray(Class[]::new);
    }


    public synchronized static Class<?> findFirstClassUsingStringsFilter(ClassLoader classLoader, String packageFilter, StringMatchType type, String... strings) throws Exception {
        var matcher = new ClassMatcher();
        for (String string : strings) {
            matcher.addUsingString(string, type);
        }
        var result = dexkit.findClass(FindClass.create().searchPackages(packageFilter).matcher(matcher));
        if (result.isEmpty()) return null;
        return result.get(0).getInstance(classLoader);
    }

    public synchronized static Class<?> findFirstClassUsingName(ClassLoader classLoader, StringMatchType type, String name) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, name, () -> {
            var result = dexkit.findClass(FindClass.create().matcher(ClassMatcher.create().className(name, type)));
            if (result.isEmpty())
                throw new ClassNotFoundException("Class not found: " + name);
            return result.get(0).getInstance(classLoader);
        });
    }

    public synchronized static String getMethodDescriptor(Method method) {
        if (method == null) return null;
        return method.getDeclaringClass().getName() + "->" + method.getName() + "(" + Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")) + ")";
    }

    public synchronized static String getFieldDescriptor(Field field) {
        return field.getDeclaringClass().getName() + "->" + field.getName() + ":" + field.getType().getName();
    }

    @Nullable
    public synchronized static Method convertRealMethod(MethodData methodData, ClassLoader classLoader) {
        try {
            return methodData.getMethodInstance(classLoader);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public synchronized static Class<?> convertRealClass(ClassData classData, ClassLoader classLoader) {
        try {
            return classData.getInstance(classLoader);
        } catch (Exception e) {
            return null;
        }
    }

    // ========== TikTok-specific Methods ==========

    /**
     * Load TikTok Video model class
     * This class contains video information including download URLs (with and without watermark)
     */
    public synchronized static Class<?> loadTikTokVideoClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, "TikTokVideoClass", () -> {
            try {
                // Try standard class name first
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Video", classLoader);
            } catch (Throwable e1) {
                try {
                    // Try alternate location
                    return XposedHelpers.findClass("com.ss.android.ugc.aweme.video.Video", classLoader);
                } catch (Throwable e2) {
                    // Use DexKit to find by characteristics
                    return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "download_no_watermark_addr", "download_addr");
                }
            }
        });
    }

    /**
     * Load TikTok Feed model class
     * This class represents feed items (Aweme)
     */
    public synchronized static Class<?> loadTikTokFeedItemClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, "TikTokFeedItemClass", () -> {
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.Aweme", classLoader);
            } catch (Throwable e) {
                // Use DexKit to find by characteristics
                return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "aweme", "video", "author");
            }
        });
    }

    /**
     * Load TikTok Download Service class
     */
    public synchronized static Class<?> loadTikTokDownloadServiceClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, "TikTokDownloadServiceClass", () -> {
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.download.component_api.DownloadServiceManager", classLoader);
            } catch (Throwable e1) {
                try {
                    return XposedHelpers.findClass("com.ss.android.ugc.aweme.download.DownloadAwemeVideoServiceImpl", classLoader);
                } catch (Throwable e2) {
                    return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "DownloadService", "aweme");
                }
            }
        });
    }

    /**
     * Load TikTok Ad/Commercialize class
     */
    public synchronized static Class<?> loadTikTokAdClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, "TikTokAdClass", () -> {
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.commercialize.media.impl.utils.CommercializeMediaServiceImpl", classLoader);
            } catch (Throwable e1) {
                try {
                    return XposedHelpers.findClass("com.bytedance.ies.ugc.aweme.commercialize.splash.service.CommercializeSplashServiceImpl", classLoader);
                } catch (Throwable e2) {
                    return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "commercialize", "ad");
                }
            }
        });
    }

    /**
     * Load TikTok Video Player class
     */
    public synchronized static Class<?> loadTikTokVideoPlayerClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, "TikTokVideoPlayerClass", () -> {
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.player.sdk.api.OnUIPlayListener", classLoader);
            } catch (Throwable e1) {
                try {
                    return XposedHelpers.findClass("com.ss.android.ugc.aweme.video.VideoBitmapManager", classLoader);
                } catch (Throwable e2) {
                    return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "player", "video", "play");
                }
            }
        });
    }

    /**
     * Find method to get no-watermark video URL
     * Based on smali analysis: getDownloadNoWatermarkAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
     */
    public synchronized static Method loadTikTokNoWatermarkUrlMethod(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getMethod(classLoader, () -> {
            Class<?> videoClass = loadTikTokVideoClass(classLoader);
            if (videoClass == null) throw new Exception("Video class not found");
            
            // Try exact method name from smali analysis first
            try {
                return videoClass.getDeclaredMethod("getDownloadNoWatermarkAddr");
            } catch (NoSuchMethodException e) {
                // Fallback to search
                for (Method method : videoClass.getDeclaredMethods()) {
                    if ((method.getName().toLowerCase().contains("downloadnowatermark") ||
                         method.getName().toLowerCase().contains("download") && method.getName().toLowerCase().contains("nowatermark")) &&
                        (method.getReturnType().getName().contains("UrlModel") || 
                         method.getReturnType() == String.class)) {
                        return method;
                    }
                }
            }
            throw new Exception("No-watermark URL method not found");
        });
    }

    /**
     * Find method to check if feed item is an ad
     * Based on smali analysis: isAd()Z
     */
    public synchronized static Method loadTikTokIsAdMethod(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getMethod(classLoader, () -> {
            Class<?> feedItemClass = loadTikTokFeedItemClass(classLoader);
            if (feedItemClass == null) throw new Exception("Feed item class not found");
            
            // Try exact method name from smali analysis first
            try {
                return feedItemClass.getDeclaredMethod("isAd");
            } catch (NoSuchMethodException e) {
                // Fallback to search
                for (Method method : feedItemClass.getDeclaredMethods()) {
                    if (method.getName().equals("isAd") && method.getReturnType() == boolean.class) {
                        return method;
                    }
                }
            }
            
            // Try using DexKit - wrap in try-catch to handle potential exceptions
            try {
                Method result = findFirstMethodUsingStringsFilter(classLoader, "com.ss.android.ugc.aweme", StringMatchType.Contains, "isAd");
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                // Method not found, will throw below
            }
            
            throw new Exception("TikTok isAd method not found");
        });
    }

    /**
     * Load TikTok Story model class
     * Based on smali analysis: com.ss.android.ugc.aweme.story.model.Story
     */
    public synchronized static Class<?> loadTikTokStoryClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, "TikTokStoryClass", () -> {
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.story.model.Story", classLoader);
            } catch (Throwable e) {
                // Use DexKit to find by characteristics
                return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "story", "awemes", "userInfo");
            }
        });
    }

    /**
     * Load TikTok Bitrate Selector class
     * Based on smali analysis: com.ss.android.ugc.aweme.bitrateselector.impl.DTBitrateSelectorServiceImpl
     */
    public synchronized static Class<?> loadTikTokBitrateSelectorClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, "TikTokBitrateSelectorClass", () -> {
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.bitrateselector.impl.DTBitrateSelectorServiceImpl", classLoader);
            } catch (Throwable e1) {
                try {
                    return XposedHelpers.findClass("com.ss.android.ugc.aweme.video.bitrate.RateSettingCombineModel", classLoader);
                } catch (Throwable e2) {
                    // Use DexKit to find by characteristics
                    return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "bitrate", "selector");
                }
            }
        });
    }

    /**
     * Load method to get download address with watermark
     * Based on smali analysis: getDownloadAddr()Lcom/ss/android/ugc/aweme/base/model/UrlModel;
     */
    public synchronized static Method loadTikTokDownloadUrlMethod(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getMethod(classLoader, () -> {
            Class<?> videoClass = loadTikTokVideoClass(classLoader);
            if (videoClass == null) throw new Exception("Video class not found");
            
            try {
                return videoClass.getDeclaredMethod("getDownloadAddr");
            } catch (NoSuchMethodException e) {
                for (Method method : videoClass.getDeclaredMethods()) {
                    if (method.getName().toLowerCase().contains("downloadaddr") &&
                        !method.getName().toLowerCase().contains("nowatermark") &&
                        method.getReturnType().getName().contains("UrlModel")) {
                        return method;
                    }
                }
            }
            throw new Exception("Download URL method not found");
        });
    }

    /**
     * Load method to check if video is ad traffic
     * Based on smali analysis: isAdTraffic()Z
     */
    public synchronized static Method loadTikTokIsAdTrafficMethod(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getMethod(classLoader, () -> {
            Class<?> feedItemClass = loadTikTokFeedItemClass(classLoader);
            if (feedItemClass == null) throw new Exception("Feed item class not found");
            
            try {
                return feedItemClass.getDeclaredMethod("isAdTraffic");
            } catch (NoSuchMethodException e) {
                // Method might not exist in all versions
                return null;
            }
        });
    }

    /**
     * Load TikTok UrlModel class
     * This class contains URL lists for video downloads
     */
    public synchronized static Class<?> loadTikTokUrlModelClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, "TikTokUrlModelClass", () -> {
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.base.model.UrlModel", classLoader);
            } catch (Throwable e) {
                return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "url_list", "url_key");
            }
        });
    }

    /**
     * Load method to get video from Aweme
     * Based on smali analysis: getVideo()Lcom/ss/android/ugc/aweme/feed/model/Video;
     */
    public synchronized static Method loadTikTokGetVideoMethod(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getMethod(classLoader, () -> {
            Class<?> awemeClass = loadTikTokFeedItemClass(classLoader);
            if (awemeClass == null) throw new Exception("Aweme class not found");
            
            try {
                return awemeClass.getDeclaredMethod("getVideo");
            } catch (NoSuchMethodException e) {
                // Search for method returning Video class
                Class<?> videoClass = loadTikTokVideoClass(classLoader);
                for (Method method : awemeClass.getDeclaredMethods()) {
                    if (method.getReturnType() == videoClass && 
                        method.getParameterCount() == 0) {
                        return method;
                    }
                }
            }
            throw new Exception("getVideo method not found");
        });
    }

    /**
     * Load method to get URL list from UrlModel
     */
    public synchronized static Method loadTikTokUrlListMethod(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getMethod(classLoader, () -> {
            Class<?> urlModelClass = loadTikTokUrlModelClass(classLoader);
            if (urlModelClass == null) throw new Exception("UrlModel class not found");
            
            // Try common method names
            for (String methodName : new String[]{"getUrlList", "getUrls", "getUrlArray"}) {
                try {
                    return urlModelClass.getDeclaredMethod(methodName);
                } catch (NoSuchMethodException ignored) {}
            }
            
            // Search for method returning List
            for (Method method : urlModelClass.getDeclaredMethods()) {
                if (method.getReturnType() == List.class && 
                    method.getParameterCount() == 0 &&
                    method.getName().toLowerCase().contains("url")) {
                    return method;
                }
            }
            throw new Exception("URL list method not found");
        });
    }

    /**
     * Load prevent download field from Aweme
     * Based on smali analysis: preventDownload field
     */
    public synchronized static Method loadTikTokPreventDownloadMethod(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getMethod(classLoader, () -> {
            Class<?> awemeClass = loadTikTokFeedItemClass(classLoader);
            if (awemeClass == null) throw new Exception("Aweme class not found");
            
            // Try getter method
            try {
                return awemeClass.getDeclaredMethod("isPreventDownload");
            } catch (NoSuchMethodException e) {
                try {
                    return awemeClass.getDeclaredMethod("getPreventDownload");
                } catch (NoSuchMethodException e2) {
                    // Search for boolean method with prevent/download in name
                    for (Method method : awemeClass.getDeclaredMethods()) {
                        if (method.getReturnType() == boolean.class &&
                            method.getName().toLowerCase().contains("prevent") &&
                            method.getName().toLowerCase().contains("download")) {
                            return method;
                        }
                    }
                }
            }
            // Return null if not found - not all versions have this
            return null;
        });
    }

    /**
     * Load TikTok LiveStream class
     * Based on smali analysis: com.ss.android.ugc.aweme.live.*
     */
    public synchronized static Class<?> loadTikTokLiveStreamClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, () -> {
            // Try standard class name first
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.live.LivePlayActivity", classLoader);
            } catch (Exception ignored) {}

            // Try alternate names
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.live.LiveRoomActivity", classLoader);
            } catch (Exception ignored) {}

            // Use DexKit to find live stream class
            ClassMatcher matcher = ClassMatcher.create()
                    .className("com.ss.android.ugc.aweme.live", StringMatchType.Contains);

            ClassDataList result = dexkit.findClass(FindClass.create().matcher(matcher));
            if (!result.isEmpty()) {
                return result.get(0).getInstance(classLoader);
            }
            throw new Exception("LiveStream class not found");
        });
    }

    /**
     * Load TikTok Comment class
     * Based on smali analysis: com.ss.android.ugc.aweme.comment.model.Comment
     */
    public synchronized static Class<?> loadTikTokCommentClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, () -> {
            // Try standard class name
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.comment.model.Comment", classLoader);
            } catch (Exception ignored) {}

            // Use DexKit to find comment class
            ClassMatcher matcher = ClassMatcher.create()
                    .className("comment", StringMatchType.Contains)
                    .fieldCount(10, 50); // Comment class has multiple fields

            ClassDataList result = dexkit.findClass(FindClass.create().matcher(matcher));
            if (!result.isEmpty()) {
                // Find class with typical comment fields
                for (ClassData classData : result) {
                    try {
                        Class<?> clazz = classData.getInstance(classLoader);
                        if (clazz.getName().contains("comment") && 
                            clazz.getName().contains("model")) {
                            return clazz;
                        }
                    } catch (Exception ignored) {}
                }
            }
            throw new Exception("Comment class not found");
        });
    }

    /**
     * Load TikTok Profile/User class
     * Based on smali analysis: com.ss.android.ugc.aweme.profile.model.User
     */
    public synchronized static Class<?> loadTikTokProfileClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, () -> {
            // Try standard class names
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.profile.model.User", classLoader);
            } catch (Exception ignored) {}

            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.user.model.User", classLoader);
            } catch (Exception ignored) {}

            // Use DexKit to find user profile class
            ClassMatcher matcher = ClassMatcher.create()
                    .className("User", StringMatchType.Contains)
                    .addMethod(MethodMatcher.create().name("getUid"))
                    .addMethod(MethodMatcher.create().name("getNickname"));

            ClassDataList result = dexkit.findClass(FindClass.create().matcher(matcher));
            if (!result.isEmpty()) {
                return result.get(0).getInstance(classLoader);
            }
            throw new Exception("Profile/User class not found");
        });
    }

    /**
     * Load TikTok Analytics/Tracker class
     * Based on smali analysis: com.ss.android.ugc.aweme.analytics.*
     */
    public synchronized static Class<?> loadTikTokAnalyticsClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, () -> {
            // Try standard class names
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.analytics.AnalyticsHelper", classLoader);
            } catch (Exception ignored) {}

            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.app.api.Api", classLoader);
            } catch (Exception ignored) {}

            // Use DexKit to find analytics class
            MethodMatcher methodMatcher = MethodMatcher.create()
                    .addUsingString("analytics", StringMatchType.Contains)
                    .addUsingString("track", StringMatchType.Contains);

            MethodDataList result = dexkit.findMethod(FindMethod.create().matcher(methodMatcher));
            if (!result.isEmpty()) {
                return result.get(0).getMethodInstance(classLoader).getDeclaringClass();
            }
            throw new Exception("Analytics class not found");
        });
    }

    /**
     * Load TikTok Feed Filter/Recommendation class
     * Based on smali analysis: com.ss.android.ugc.aweme.feed.model.FeedItemList
     */
    public synchronized static Class<?> loadTikTokFeedFilterClass(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getClass(classLoader, () -> {
            // Try standard class names
            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.model.FeedItemList", classLoader);
            } catch (Exception ignored) {}

            try {
                return XposedHelpers.findClass("com.ss.android.ugc.aweme.feed.adapter.FeedAdapter", classLoader);
            } catch (Exception ignored) {}

            // Use DexKit to find feed filter class
            ClassMatcher matcher = ClassMatcher.create()
                    .className("feed", StringMatchType.Contains)
                    .addMethod(MethodMatcher.create().returnType(List.class));

            ClassDataList result = dexkit.findClass(FindClass.create().matcher(matcher));
            if (!result.isEmpty()) {
                // Look for class with feed/list in name
                for (ClassData classData : result) {
                    try {
                        Class<?> clazz = classData.getInstance(classLoader);
                        if (clazz.getName().toLowerCase().contains("feed") && 
                            (clazz.getName().toLowerCase().contains("list") || 
                             clazz.getName().toLowerCase().contains("adapter"))) {
                            return clazz;
                        }
                    } catch (Exception ignored) {}
                }
            }
            throw new Exception("Feed filter class not found");
        });
    }

    /**
     * Load method to track live stream playback
     */
    public synchronized static Method loadTikTokLiveStreamPlayMethod(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getMethod(classLoader, () -> {
            Class<?> liveStreamClass = loadTikTokLiveStreamClass(classLoader);
            if (liveStreamClass == null) throw new Exception("LiveStream class not found");

            // Try common method names for starting live stream
            for (String methodName : new String[]{"startPlay", "playLive", "onLiveStart", "startLiveStream"}) {
                try {
                    return liveStreamClass.getDeclaredMethod(methodName);
                } catch (NoSuchMethodException ignored) {}
            }

            // Search for play method
            for (Method method : liveStreamClass.getDeclaredMethods()) {
                if (method.getName().toLowerCase().contains("play") || 
                    method.getName().toLowerCase().contains("start")) {
                    return method;
                }
            }
            throw new Exception("Live stream play method not found");
        });
    }

    /**
     * Load method to post or view comments
     */
    public synchronized static Method loadTikTokCommentPostMethod(ClassLoader classLoader) throws Exception {
        return UnobfuscatorCache.getInstance().getMethod(classLoader, () -> {
            Class<?> commentClass = loadTikTokCommentClass(classLoader);
            if (commentClass == null) throw new Exception("Comment class not found");

            // Try common method names
            for (String methodName : new String[]{"postComment", "sendComment", "submitComment"}) {
                for (Method method : commentClass.getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        return method;
                    }
                }
            }

            // Search for post/send method
            for (Method method : commentClass.getDeclaredMethods()) {
                if ((method.getName().toLowerCase().contains("post") || 
                     method.getName().toLowerCase().contains("send")) &&
                    method.getParameterCount() > 0) {
                    return method;
                }
            }
            throw new Exception("Comment post method not found");
        });
    }
}
