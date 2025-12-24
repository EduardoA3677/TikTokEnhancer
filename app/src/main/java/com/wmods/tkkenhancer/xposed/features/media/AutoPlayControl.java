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
 * Targets based on smali analysis:
 * - com.ss.android.ugc.aweme.player.sdk.api.OnUIPlayListener (play control interface)
 * - com.ss.android.ugc.aweme.video.VideoBitmapManager (video manager)
 * - com.ss.android.ugc.aweme.feed.ui.* (feed UI player components)
 * - Auto-play methods: setAutoPlay, enableAutoPlay, shouldAutoPlay
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
     */
    private void hookAutoPlaySettings() {
        try {
            // Search for settings classes that control auto-play
            String[] settingsPackages = {
                "com.ss.android.ugc.aweme.setting",
                "com.ss.android.ugc.aweme.settings",
                "com.ss.android.ugc.aweme.profile.settings"
            };

            for (String pkg : settingsPackages) {
                try {
                    // Try to find settings class
                    Class<?> settingsClass = XposedHelpers.findClass(pkg + ".SettingsManager", classLoader);
                    hookAutoPlayInClass(settingsClass);
                } catch (Exception ignored) {
                    // Try alternate names
                    try {
                        Class<?> settingsClass = XposedHelpers.findClass(pkg + ".Settings", classLoader);
                        hookAutoPlayInClass(settingsClass);
                    } catch (Exception ignored2) {}
                }
            }
        } catch (Exception e) {
            logDebug("Failed to hook settings: " + e.getMessage());
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
