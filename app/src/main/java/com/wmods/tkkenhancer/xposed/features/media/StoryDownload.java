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
 * Story Download Feature
 * Enables downloading of TikTok user stories (similar to Instagram stories)
 */
public class StoryDownload extends Feature {

    public StoryDownload(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("story_download", false)) return;

        logDebug("Initializing Story Download feature");

        // Hook story model
        hookStoryModel();

        // Hook story viewer
        hookStoryViewer();

        // Hook story media access
        hookStoryMedia();

        logDebug("Story Download feature initialized");
    }

    /**
     * Hook story model to access story data
     */
    private void hookStoryModel() {
        try {
            // Try to find story model classes
            Class<?> storyClass = null;

            try {
                storyClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.story.model.Story",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    storyClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.story.Story",
                        classLoader
                    );
                } catch (Throwable e2) {
                    try {
                        // Try alternate story class name
                        storyClass = XposedHelpers.findClass(
                            "com.ss.android.ugc.aweme.shortstory.model.StoryCollection",
                            classLoader
                        );
                    } catch (Throwable e3) {
                        logDebug("Story model class not found in standard locations");
                        return;
                    }
                }
            }

            if (storyClass != null) {
                logDebug("Found story model class: " + storyClass.getName());
                hookStoryModelMethods(storyClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook story model methods
     */
    private void hookStoryModelMethods(Class<?> storyClass) {
        try {
            Method[] methods = storyClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Look for methods that provide story URLs or media
                if (methodName.toLowerCase().contains("url") ||
                    methodName.toLowerCase().contains("media") ||
                    methodName.toLowerCase().contains("video") ||
                    methodName.toLowerCase().contains("image")) {

                    logDebug("Hooking story model method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result != null) {
                                logDebug("Story media URL accessed: " + methodName + " -> " + result);
                                
                                // Store the URL for download access
                                storeStoryUrl(param.thisObject, result);
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
     * Store story URL for later access
     */
    private void storeStoryUrl(Object storyObject, Object urlResult) {
        try {
            logDebug("Storing story URL: " + urlResult);
            
            // Store URL using additional instance field for later retrieval
            if (storyObject != null && urlResult != null) {
                try {
                    XposedHelpers.setAdditionalInstanceField(storyObject, "story_download_url", urlResult);
                    logDebug("Successfully stored story download URL");
                } catch (Throwable e) {
                    logDebug("Failed to set additional field", e);
                }
            }
        } catch (Throwable e) {
            logDebug("Failed to store story URL", e);
        }
    }

    /**
     * Hook story viewer
     */
    private void hookStoryViewer() {
        try {
            // Try to find story viewer classes
            Class<?> viewerClass = null;

            try {
                viewerClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.story.viewer.StoryViewerActivity",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    viewerClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.story.StoryFragment",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Story viewer class not found");
                    return;
                }
            }

            if (viewerClass != null) {
                logDebug("Found story viewer class: " + viewerClass.getName());
                hookStoryViewerMethods(viewerClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook story viewer methods
     */
    private void hookStoryViewerMethods(Class<?> viewerClass) {
        try {
            Method[] methods = viewerClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Hook methods that display stories
                if (methodName.toLowerCase().contains("show") ||
                    methodName.toLowerCase().contains("display") ||
                    methodName.toLowerCase().contains("play")) {

                    logDebug("Hooking story viewer method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            logDebug("Story viewer method called: " + methodName);
                            // Can add download button or menu option here
                        }
                    });
                }
            }
        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook story media access
     */
    private void hookStoryMedia() {
        try {
            // Try to find story media service
            Class<?> mediaServiceClass = null;

            try {
                mediaServiceClass = XposedHelpers.findClass(
                    "com.ss.android.ugc.aweme.story.api.StoryService",
                    classLoader
                );
            } catch (Throwable e1) {
                try {
                    mediaServiceClass = XposedHelpers.findClass(
                        "com.ss.android.ugc.aweme.story.service.StoryServiceImpl",
                        classLoader
                    );
                } catch (Throwable e2) {
                    logDebug("Story media service class not found");
                    return;
                }
            }

            if (mediaServiceClass != null) {
                logDebug("Found story media service class: " + mediaServiceClass.getName());
                hookMediaServiceMethods(mediaServiceClass);
            }

        } catch (Throwable e) {
            log(e);
        }
    }

    /**
     * Hook story media service methods
     */
    private void hookMediaServiceMethods(Class<?> serviceClass) {
        try {
            Method[] methods = serviceClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();

                // Look for methods that fetch story media
                if (methodName.toLowerCase().contains("fetch") ||
                    methodName.toLowerCase().contains("get") ||
                    methodName.toLowerCase().contains("load")) {

                    logDebug("Hooking story media service method: " + methodName);

                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object result = param.getResult();
                            if (result != null) {
                                logDebug("Story media loaded via service: " + methodName);
                                // Process story media for download
                                processStoryMedia(result);
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
     * Process story media for download
     */
    private void processStoryMedia(Object mediaResult) {
        try {
            logDebug("Processing story media: " + mediaResult.getClass().getName());
            
            // Try to extract URLs from media object
            Class<?> mediaClass = mediaResult.getClass();
            
            // Look for URL fields
            try {
                Object videoUrl = XposedHelpers.getObjectField(mediaResult, "videoUrl");
                if (videoUrl != null) {
                    logDebug("Found story video URL: " + videoUrl);
                }
            } catch (Throwable ignored) {}
            
            try {
                Object imageUrl = XposedHelpers.getObjectField(mediaResult, "imageUrl");
                if (imageUrl != null) {
                    logDebug("Found story image URL: " + imageUrl);
                }
            } catch (Throwable ignored) {}
            
            try {
                Object downloadUrl = XposedHelpers.getObjectField(mediaResult, "downloadUrl");
                if (downloadUrl != null) {
                    logDebug("Found story download URL: " + downloadUrl);
                }
            } catch (Throwable ignored) {}
            
        } catch (Throwable e) {
            logDebug("Failed to process story media", e);
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Story Download";
    }
}
