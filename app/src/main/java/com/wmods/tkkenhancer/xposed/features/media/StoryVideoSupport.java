package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Story Video Download Support
 * Based on smali analysis: com.ss.android.ugc.aweme.story.model.Story
 * 
 * Enables downloading story videos without watermark, similar to regular videos.
 */
public class StoryVideoSupport extends Feature {

    public StoryVideoSupport(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("story_download", false)) return;

        logDebug("Initializing Story Video Support feature");

        // Hook Story class methods
        hookStoryClass();

        logDebug("Story Video Support feature initialized");
    }

    /**
     * Hook Story class to enable downloads
     */
    private void hookStoryClass() {
        try {
            Class<?> storyClass = Unobfuscator.loadTikTokStoryClass(classLoader);
            if (storyClass == null) {
                logDebug("Story class not found");
                return;
            }

            logDebug("Found Story class: " + storyClass.getName());

            // Hook methods that provide video access
            for (Method method : storyClass.getDeclaredMethods()) {
                String methodName = method.getName().toLowerCase();
                
                // Look for methods related to video or awemes
                if (methodName.contains("aweme") || 
                    methodName.contains("video") ||
                    methodName.contains("download")) {
                    
                    logDebug("Hooking Story method: " + method.getName());
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result != null) {
                                logDebug("Story video accessed: " + method.getName());
                                
                                // Enable download for story video
                                XposedHelpers.setAdditionalInstanceField(
                                    param.thisObject,
                                    "tiktok_enhancer_story_download_enabled",
                                    true
                                );
                            }
                        }
                    });
                }
            }

        } catch (Throwable e) {
            log("Error hooking Story class: " + e.getMessage());
            log(e);
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Story Video Support";
    }
}
