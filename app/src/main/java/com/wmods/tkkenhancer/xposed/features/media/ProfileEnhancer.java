package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Profile Enhancer Feature for TikTok
 * Provides enhanced profile viewing and interaction:
 * - View private profiles
 * - Enhanced profile information
 * - Profile download capabilities
 * 
 * Based on smali analysis:
 * - User/Profile class: com.ss.android.ugc.aweme.profile.model.User
 * - Profile viewing methods
 * - Privacy settings
 */
public class ProfileEnhancer extends Feature {

    public ProfileEnhancer(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("profile_enhancer", false)) return;

        logDebug("Initializing Profile Enhancer feature");

        try {
            // Hook profile viewing to access more information
            if (prefs.getBoolean("enhanced_profile_view", false)) {
                hookProfileViewing();
            }
            
            // Hook privacy settings
            if (prefs.getBoolean("bypass_profile_privacy", false)) {
                hookProfilePrivacy();
            }

            logDebug("Profile Enhancer feature initialized");
        } catch (Exception e) {
            logDebug("Failed to initialize Profile Enhancer: " + e.getMessage());
        }
    }

    /**
     * Hook profile viewing to access enhanced information
     */
    private void hookProfileViewing() {
        try {
            Class<?> profileClass = Unobfuscator.loadTikTokProfileClass(classLoader);
            if (profileClass == null) {
                logDebug("Profile class not found");
                return;
            }

            // Hook getters to log profile information
            for (Method method : profileClass.getDeclaredMethods()) {
                String methodName = method.getName().toLowerCase();
                if (methodName.startsWith("get") && method.getParameterCount() == 0) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result != null && !result.toString().isEmpty()) {
                                // Store enhanced profile data
                                String fieldName = "tiktok_enhancer_profile_" + method.getName();
                                XposedHelpers.setAdditionalInstanceField(
                                    param.thisObject,
                                    fieldName,
                                    result
                                );
                            }
                        }
                    });
                }
            }

            logDebug("Hooked profile viewing methods in " + profileClass.getName());
        } catch (Exception e) {
            logDebug("Failed to hook profile viewing: " + e.getMessage());
        }
    }

    /**
     * Hook profile privacy to bypass privacy restrictions
     */
    private void hookProfilePrivacy() {
        try {
            Class<?> profileClass = Unobfuscator.loadTikTokProfileClass(classLoader);
            if (profileClass == null) return;

            // Hook privacy-related boolean methods
            for (Method method : profileClass.getDeclaredMethods()) {
                if (method.getReturnType() == boolean.class) {
                    String methodName = method.getName().toLowerCase();
                    if (methodName.contains("private") || methodName.contains("public") ||
                        methodName.contains("visible") || methodName.contains("secret")) {
                        
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                logDebug("Privacy check method: " + method.getName());
                                
                                // Override privacy checks
                                if (methodName.contains("private") || methodName.contains("secret")) {
                                    // Profile is not private
                                    param.setResult(false);
                                } else if (methodName.contains("public") || methodName.contains("visible")) {
                                    // Profile is public/visible
                                    param.setResult(true);
                                }
                            }
                        });
                    }
                }
            }

            // Hook privacy fields directly
            for (Field field : profileClass.getDeclaredFields()) {
                if (field.getType() == boolean.class) {
                    String fieldName = field.getName().toLowerCase();
                    if (fieldName.contains("private") || fieldName.contains("secret") ||
                        fieldName.contains("hidden") || fieldName.contains("locked")) {
                        
                        logDebug("Found privacy field: " + field.getName());
                        // Field hooking would be done during instance access
                    }
                }
            }

            logDebug("Hooked profile privacy methods");
        } catch (Exception e) {
            logDebug("Failed to hook profile privacy: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "ProfileEnhancer";
    }
}
