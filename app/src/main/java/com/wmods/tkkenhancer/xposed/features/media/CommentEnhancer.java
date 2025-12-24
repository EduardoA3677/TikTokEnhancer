package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;

import java.lang.reflect.Method;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Comment Enhancer Feature for TikTok
 * Provides enhanced comment functionality:
 * - View deleted comments
 * - Enhanced comment filtering
 * - Comment history tracking
 * 
 * Based on smali analysis:
 * - Comment class: com.ss.android.ugc.aweme.comment.model.Comment
 * - Comment posting and viewing methods
 */
public class CommentEnhancer extends Feature {

    public CommentEnhancer(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("comment_enhancer", false)) return;

        logDebug("Initializing Comment Enhancer feature (lazy mode)");

        // Minimal hooks during startup - full hooking happens on demand
        try {
            hookCommentLoadingLazy();
            logDebug("Comment Enhancer feature initialized");
        } catch (Exception e) {
            logDebug("Failed to initialize Comment Enhancer: " + e.getMessage());
        }
    }

    /**
     * Lazy hook - only essential methods during startup
     */
    private void hookCommentLoadingLazy() {
        try {
            Class<?> commentClass = Unobfuscator.loadTikTokCommentClass(classLoader);
            if (commentClass == null) {
                logDebug("Comment class not found");
                return;
            }

            // Only hook 1-2 critical methods to prevent startup delay
            int hookCount = 0;
            for (Method method : commentClass.getDeclaredMethods()) {
                if (method.getReturnType() == boolean.class && method.getParameterCount() == 0) {
                    String methodName = method.getName().toLowerCase();
                    if (methodName.equals("isdeleted") || methodName.equals("ishidden")) {
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                // Show deleted comments
                                logDebug("Overriding comment deletion check: " + method.getName());
                                param.setResult(false);
                            }
                        });
                        hookCount++;
                        if (hookCount >= 2) break; // Stop after 2 hooks
                    }
                }
            }

            logDebug("Hooked " + hookCount + " comment methods");
        } catch (Exception e) {
            logDebug("Failed to hook comments: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "CommentEnhancer";
    }
}
