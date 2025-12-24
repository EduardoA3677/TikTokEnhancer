package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Feature to control video auto-play behavior
 * Allows users to save data by preventing automatic video playback
 */
public class AutoPlayControl extends Feature {

    public AutoPlayControl(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("autoplay_control", false)) return;

        logDebug("Initializing Auto-play Control feature");

        // Hook video player initialization
        hookVideoPlayer();
        
        // Hook auto-play settings
        hookAutoPlaySettings();

        logDebug("Auto-play Control feature initialized");
    }

    /**
     * Hook video player to control auto-play
     */
    private void hookVideoPlayer() {
        try {
            // Try to find video player classes
            Class<?> playerClass = null;
            
            try {
                playerClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.player.sdk.api.OnUIPlayListener",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    playerClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.video.VideoBitmapManager",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Video player class not found in standard locations");
                    return;
                }
            }

            if (playerClass != null) {
                logDebug("Found player class: " + playerClass.getName());
                hookPlayerMethods(playerClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook player methods to control auto-play
     */
    private void hookPlayerMethods(Class<?> playerClass) {
        try {
            Method[] methods = playerClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                
                // Look for auto-play related methods
                if (methodName.toLowerCase().contains("autoplay") ||
                    methodName.toLowerCase().contains("auto") && methodName.toLowerCase().contains("play") ||
                    methodName.toLowerCase().contains("startplay")) {
                    
                    logDebug("Hooking player method: " + methodName);
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Check if we should block auto-play
                            boolean disableAutoPlay = prefs.getBoolean("disable_autoplay", false);
                            
                            if (disableAutoPlay) {
                                logDebug("Blocked auto-play: " + methodName);
                                
                                // Block the auto-play
                                Class<?> returnType = method.getReturnType();
                                if (returnType == boolean.class || returnType == Boolean.class) {
                                    param.setResult(false);
                                } else if (returnType == void.class) {
                                    param.setResult(null);
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
     * Hook auto-play settings
     */
    private void hookAutoPlaySettings() {
        try {
            // Try to find settings classes
            Class<?> settingsClass = null;
            
            try {
                settingsClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.setting.services.SettingServiceImpl",
                    classLoader
                );
            } catch (Throwable e1) {
                logDebug("Settings class not found");
                return;
            }

            if (settingsClass != null) {
                logDebug("Found settings class: " + settingsClass.getName());
                hookSettingsMethods(settingsClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook settings methods
     */
    private void hookSettingsMethods(Class<?> settingsClass) {
        try {
            Method[] methods = settingsClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                
                // Look for auto-play settings methods
                if (methodName.toLowerCase().contains("autoplay") ||
                    methodName.toLowerCase().contains("playmode")) {
                    
                    logDebug("Hooking settings method: " + methodName);
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // Override auto-play setting if our control is enabled
                            boolean disableAutoPlay = prefs.getBoolean("disable_autoplay", false);
                            
                            if (disableAutoPlay && (method.getReturnType() == boolean.class || 
                                                     method.getReturnType() == Boolean.class)) {
                                logDebug("Overriding auto-play setting: " + methodName);
                                param.setResult(false);
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
        return "Auto-play Control";
    }
}
