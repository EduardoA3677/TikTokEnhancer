package com.wmods.tkkenhancer.xposed.features.media;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;

/**
 * Comment Enhancer Feature for TikTok
 * Provides enhanced comment functionality:
 * - View deleted comments (status != 1)
 * 
 * Based on verified smali analysis:
 * - Comment class: com.ss.android.ugc.aweme.comment.model.Comment (smali_classes15)
 * - Field: public status:I
 * - Method: public getStatus()I
 * - Status values: 0 = deleted/hidden, 1 = visible
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
            hookCommentStatus();
            logDebug("Comment Enhancer feature initialized successfully");
        } catch (Exception e) {
            logDebug("Failed to initialize Comment Enhancer: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook Comment.getStatus() to show deleted comments
     * Verified class: com.ss.android.ugc.aweme.comment.model.Comment
     */
    private void hookCommentStatus() {
        try {
            Class<?> commentClass = classLoader.loadClass("com.ss.android.ugc.aweme.comment.model.Comment");
            
            XposedHelpers.findAndHookMethod(commentClass, "getStatus", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int originalStatus = (int) param.getResult();
                    
                    // Status 0 = deleted/hidden, 1 = visible
                    // Override to always return 1 (visible) to show deleted comments
                    if (originalStatus != 1) {
                        param.setResult(1);
                        logDebug("Showing deleted comment (status " + originalStatus + " -> 1)");
                    }
                }
            });
            
            logDebug("Hooked Comment.getStatus() successfully");
        } catch (Exception e) {
            logDebug("Failed to hook Comment: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "CommentEnhancer";
    }
}
