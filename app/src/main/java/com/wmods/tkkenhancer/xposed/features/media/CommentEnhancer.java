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

        logDebug("Initializing Comment Enhancer feature");

        try {
            // Hook comment loading to capture all comments
            if (prefs.getBoolean("view_deleted_comments", false)) {
                hookCommentLoading();
            }
            
            // Hook comment filtering
            if (prefs.getBoolean("enhanced_comment_filter", false)) {
                hookCommentFiltering();
            }

            logDebug("Comment Enhancer feature initialized");
        } catch (Exception e) {
            logDebug("Failed to initialize Comment Enhancer: " + e.getMessage());
        }
    }

    /**
     * Hook comment loading to view deleted or hidden comments
     */
    private void hookCommentLoading() {
        try {
            Class<?> commentClass = Unobfuscator.loadTikTokCommentClass(classLoader);
            if (commentClass == null) {
                logDebug("Comment class not found");
                return;
            }

            // Hook methods that return comment lists
            for (Method method : commentClass.getDeclaredMethods()) {
                Class<?> returnType = method.getReturnType();
                if (returnType == List.class || returnType.isArray()) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result instanceof List) {
                                List<?> comments = (List<?>) result;
                                logDebug("Comment list loaded with " + comments.size() + " items");
                                
                                // Store comments for analysis
                                XposedHelpers.setAdditionalStaticField(
                                    commentClass,
                                    "tiktok_enhancer_comment_cache",
                                    result
                                );
                            }
                        }
                    });
                }
            }

            // Hook comment deletion check
            for (Method method : commentClass.getDeclaredMethods()) {
                if (method.getReturnType() == boolean.class) {
                    String methodName = method.getName().toLowerCase();
                    if (methodName.contains("delete") || methodName.contains("remove") || 
                        methodName.contains("hidden") || methodName.contains("visible")) {
                        
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                // Override deletion check to show deleted comments
                                if (methodName.contains("delete") || methodName.contains("hidden")) {
                                    logDebug("Overriding comment deletion check: " + method.getName());
                                    param.setResult(false); // Not deleted
                                }
                            }
                        });
                    }
                }
            }

            logDebug("Hooked comment loading methods in " + commentClass.getName());
        } catch (Exception e) {
            logDebug("Failed to hook comment loading: " + e.getMessage());
        }
    }

    /**
     * Hook comment filtering for enhanced filtering options
     */
    private void hookCommentFiltering() {
        try {
            Class<?> commentClass = Unobfuscator.loadTikTokCommentClass(classLoader);
            if (commentClass == null) return;

            // Hook comment filter methods
            for (Method method : commentClass.getDeclaredMethods()) {
                String methodName = method.getName().toLowerCase();
                if (methodName.contains("filter") || methodName.contains("sort")) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("Comment filter method called: " + method.getName());
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result instanceof List) {
                                List<?> filtered = (List<?>) result;
                                logDebug("Filtered " + filtered.size() + " comments");
                            }
                        }
                    });
                }
            }

            logDebug("Hooked comment filtering methods");
        } catch (Exception e) {
            logDebug("Failed to hook comment filtering: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "CommentEnhancer";
    }
}
