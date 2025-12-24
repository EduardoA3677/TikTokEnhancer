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
 * Feature to control video auto-play behavior in TikTok
 * 
 * Targets based on smali analysis of TikTok 43.0.0:
 * - com.bytedance.ies.abmock.SettingsManager (verified settings manager)
 * - com.ss.android.ugc.aweme.global.config.settings.pojo.IESSettingsProxy (fallback)
 * - com.ss.android.ugc.aweme.player.sdk.api.OnUIPlayListener (play control interface)
 * 
 * Settings Manager verified in:
 * - ./smali_classes4/com/bytedance/ies/abmock/SettingsManager.smali
 * - Method LIZLLL() returns singleton instance
 * - Method LIZ(String, boolean) gets boolean settings
 */
public class AutoPlayControl extends Feature {

    // Configuration constants
    private static final int MAX_PLAYER_HOOKS = 5;
    private static final int MAX_SETTINGS_HOOKS = 3;
    private static final int MAX_FEED_HOOKS = 3;
    
    private boolean disableAutoPlay;

    public AutoPlayControl(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("autoplay_control", false)) return;

        disableAutoPlay = prefs.getBoolean("disable_autoplay", false);
        logDebug("Auto-play Control enabled. Disable auto-play: " + disableAutoPlay);

        try {
            // Hook player classes
            hookPlayerClass();
            
            // Hook auto-play settings
            hookAutoPlaySettings();
            
            // Hook feed player initialization
            hookFeedPlayer();

            log("Auto-play Control initialized successfully");
        } catch (Exception e) {
            log("Failed to initialize Auto-play Control: " + e.getMessage());
            logDebug(e);
        }
    }

    /**
     * Hook TikTok player class methods
     */
    private void hookPlayerClass() {
        try {
            Class<?> playerClass = Unobfuscator.loadTikTokVideoPlayerClass(classLoader);
            if (playerClass == null) {
                logDebug("Player class not found");
                return;
            }

            logDebug("Found player class: " + playerClass.getName());

            // Hook methods related to auto-play
            int hookedMethods = 0;
            for (Method method : playerClass.getDeclaredMethods()) {
                final Method finalMethod = method; // Make effectively final for lambda
                String methodName = finalMethod.getName().toLowerCase();
                
                // Look for auto-play related methods
                if (methodName.contains("autoplay") || 
                    methodName.contains("shouldplay") ||
                    (methodName.contains("play") && finalMethod.getParameterCount() == 0 && 
                     finalMethod.getReturnType() == boolean.class)) {
                    
                    logDebug("Hooking player method: " + finalMethod.getName());
                    XposedBridge.hookMethod(finalMethod, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (disableAutoPlay && finalMethod.getReturnType() == boolean.class) {
                                logDebug("Blocking auto-play in: " + finalMethod.getName());
                                param.setResult(false);
                            }
                        }
                    });
                    
                    hookedMethods++;
                    if (hookedMethods >= MAX_PLAYER_HOOKS) break; // Limit hooks to prevent overhead
                }
            }
            
            logDebug("Hooked " + hookedMethods + " player methods");
        } catch (Exception e) {
            logDebug("Failed to hook player class: " + e.getMessage());
        }
    }

    /**
     * Hook auto-play settings methods
     * Uses the correct TikTok SettingsManager: com.bytedance.ies.abmock.SettingsManager
     * Verified from smali analysis of TikTok 43.0.0
     */
    private void hookAutoPlaySettings() {
        try {
            // Use the correct TikTok SettingsManager class
            // Found in smali: ./smali_classes4/com/bytedance/ies/abmock/SettingsManager.smali
            Class<?> settingsManagerClass = XposedHelpers.findClass(
                "com.bytedance.ies.abmock.SettingsManager", 
                classLoader
            );
            
            logDebug("Found TikTok SettingsManager: " + settingsManagerClass.getName());
            
            // Hook the boolean getter method (LIZ method with String and boolean params)
            // This method is used to get boolean settings values
            XposedHelpers.findAndHookMethod(
                settingsManagerClass,
                "LIZ",
                String.class,
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!disableAutoPlay) return;
                        
                        String key = (String) param.args[0];
                        // Check if this is an auto-play related setting
                        if (key != null && (key.contains("auto_play") || 
                                           key.contains("autoplay") || 
                                           key.contains("play_mode"))) {
                            logDebug("Overriding auto-play setting for key: " + key);
                            param.setResult(false); // Disable auto-play
                        }
                    }
                }
            );
            
            logDebug("Successfully hooked SettingsManager.LIZ() for auto-play control");
            
        } catch (Throwable e) {
            logDebug("Failed to hook SettingsManager: " + e.getMessage());
            // Try fallback methods if primary fails
            tryFallbackSettingsHook();
        }
    }
    
    /**
     * Fallback method to hook IESSettingsProxy if SettingsManager fails
     */
    private void tryFallbackSettingsHook() {
        try {
            Class<?> settingsProxyClass = XposedHelpers.findClass(
                "com.ss.android.ugc.aweme.global.config.settings.pojo.IESSettingsProxy",
                classLoader
            );
            
            logDebug("Found IESSettingsProxy as fallback: " + settingsProxyClass.getName());
            
            // Hook methods that might return auto-play settings
            int hookedMethods = 0;
            for (Method method : settingsProxyClass.getDeclaredMethods()) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.contains("play") || methodName.contains("video")) && 
                    method.getReturnType() != void.class &&
                    method.getParameterCount() == 0) {
                    
                    try {
                        final Method finalMethod = method;
                        XposedBridge.hookMethod(finalMethod, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                if (!disableAutoPlay) return;
                                
                                Object result = param.getResult();
                                if (result != null) {
                                    logDebug("Intercepted settings from: " + finalMethod.getName());
                                }
                            }
                        });
                        hookedMethods++;
                        if (hookedMethods >= MAX_SETTINGS_HOOKS) break;
                    } catch (Throwable ignored) {}
                }
            }
            
            if (hookedMethods > 0) {
                logDebug("Hooked " + hookedMethods + " IESSettingsProxy methods as fallback");
            }
            
        } catch (Throwable e) {
            logDebug("Fallback settings hook also failed: " + e.getMessage());
        }
    }

    /**
     * Hook auto-play methods in a specific class
     */
    private void hookAutoPlayInClass(Class<?> clazz) {
        int hookedMethods = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            final Method finalMethod = method; // Make effectively final for lambda
            String methodName = finalMethod.getName().toLowerCase();
            
            if (methodName.contains("autoplay") || methodName.contains("shouldautoplay")) {
                try {
                    XposedBridge.hookMethod(finalMethod, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (disableAutoPlay && finalMethod.getReturnType() == boolean.class) {
                                logDebug("Overriding auto-play setting: " + finalMethod.getName());
                                param.setResult(false);
                            }
                        }
                    });
                    hookedMethods++;
                    if (hookedMethods >= MAX_SETTINGS_HOOKS) break;
                } catch (Throwable e) {
                    logDebug("Failed to hook method " + finalMethod.getName() + ": " + e.getMessage());
                }
            }
        }
        if (hookedMethods > 0) {
            logDebug("Hooked " + hookedMethods + " auto-play methods in " + clazz.getSimpleName());
        }
    }

    /**
     * Hook feed player initialization
     */
    private void hookFeedPlayer() {
        try {
            // Hook feed UI classes
            Method[] feedMethods = Unobfuscator.findAllMethodUsingStrings(
                classLoader,
                org.luckypray.dexkit.query.enums.StringMatchType.Contains,
                "feed_player", "auto_play"
            );
            
            if (feedMethods != null && feedMethods.length > 0) {
                int count = 0;
                for (Method method : feedMethods) {
                    final Method finalMethod = method; // Make effectively final for lambda
                    if (finalMethod.getReturnType() == boolean.class) {
                        XposedBridge.hookMethod(finalMethod, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (disableAutoPlay) {
                                    logDebug("Blocking feed auto-play: " + finalMethod.getName());
                                    param.setResult(false);
                                }
                            }
                        });
                        count++;
                        if (count >= MAX_FEED_HOOKS) break;
                    }
                }
                logDebug("Hooked " + count + " feed player methods");
            }
        } catch (Throwable e) {
            logDebug("Failed to hook feed player: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "AutoPlayControl";
    }
}
