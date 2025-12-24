package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

/**
 * Profile Enhancer Feature for TikTok
 * Provides enhanced profile viewing:
 * - View private profiles (bypass isPrivateAccount)
 * 
 * Based on verified smali analysis:
 * - User class: com.ss.android.ugc.aweme.profile.model.User (smali_classes35)
 * - Field: public isPrivateAccount:Z
 * - Method: public isPrivateAccount()Z
 * - Method: public isSecret()Z
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
            hookProfilePrivacy();
            logDebug("Profile Enhancer feature initialized successfully");
        } catch (Exception e) {
            logDebug("Failed to initialize Profile Enhancer: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook User.isPrivateAccount() and isSecret() to bypass private profile restrictions
     * Verified class: com.ss.android.ugc.aweme.profile.model.User
     */
    private void hookProfilePrivacy() {
        try {
            Class<?> userClass = classLoader.loadClass("com.ss.android.ugc.aweme.profile.model.User");
            
            // Hook isPrivateAccount() to always return false
            XposedHelpers.findAndHookMethod(userClass, "isPrivateAccount", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean originalValue = (boolean) param.getResult();
                    
                    if (originalValue) {
                        param.setResult(false);
                        logDebug("Bypassed private account check");
                    }
                }
            });
            
            // Hook isSecret() to always return false
            XposedHelpers.findAndHookMethod(userClass, "isSecret", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean originalValue = (boolean) param.getResult();
                    
                    if (originalValue) {
                        param.setResult(false);
                        logDebug("Bypassed secret profile check");
                    }
                }
            });
            
            logDebug("Hooked User profile privacy methods successfully");
        } catch (Exception e) {
            logDebug("Failed to hook User privacy: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "ProfileEnhancer";
    }
}
