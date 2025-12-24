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
 * Feed Scroll Customizer Feature for TikTok
 * Provides customization options for feed scrolling behavior:
 * - Adjust scroll speed
 * - Control paging behavior
 * - Customize scroll animations
 * - Intercept feed loading on scroll
 * 
 * Based on smali analysis of com.ss.android.ugc.aweme.feed
 * Target classes:
 * - RecyclerView adapters for feed
 * - LayoutManager for vertical paging
 * - Scroll listeners
 */
public class FeedScrollCustomizer extends Feature {

    private static final String TAG = "FeedScrollCustomizer";
    
    // Scroll speed multiplier (1.0 = normal, 0.5 = slower, 2.0 = faster)
    private float scrollSpeedMultiplier = 1.0f;
    
    // Enable/disable smooth scroll
    private boolean enableSmoothScroll = true;
    
    // Enable custom paging behavior
    private boolean customPagingEnabled = false;

    public FeedScrollCustomizer(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
        loadPreferences();
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("feed_scroll_customizer", false)) return;

        logDebug("Initializing Feed Scroll Customizer feature");

        try {
            // Hook RecyclerView scroll behavior
            hookRecyclerViewScroll();
            
            // Hook feed adapter for scroll events (optional - may not exist)
            try {
                hookFeedAdapter();
            } catch (Throwable e) {
                logDebug("Feed adapter hooks skipped: " + e.getMessage());
            }
            
            // Hook LayoutManager for custom paging (optional - may not exist)
            try {
                hookLayoutManager();
            } catch (Throwable e) {
                logDebug("LayoutManager hooks skipped: " + e.getMessage());
            }

            logDebug("Feed Scroll Customizer feature initialized successfully");
        } catch (Throwable e) {
            logDebug("Feed Scroll Customizer skipped: " + e.getMessage());
        }
    }

    /**
     * Load preferences for scroll customization
     */
    private void loadPreferences() {
        try {
            // Load scroll speed multiplier (default 1.0)
            String speedStr = prefs.getString("scroll_speed", "1.0");
            try {
                scrollSpeedMultiplier = Float.parseFloat(speedStr);
                // Clamp speed multiplier to reasonable range (0.1 to 5.0)
                if (scrollSpeedMultiplier < 0.1f) {
                    scrollSpeedMultiplier = 0.1f;
                } else if (scrollSpeedMultiplier > 5.0f) {
                    scrollSpeedMultiplier = 5.0f;
                }
            } catch (NumberFormatException e) {
                logDebug("Invalid scroll speed value: " + speedStr + ", using default 1.0");
                scrollSpeedMultiplier = 1.0f;
            }
            
            // Load smooth scroll setting
            enableSmoothScroll = prefs.getBoolean("smooth_scroll", true);
            
            // Load custom paging setting
            customPagingEnabled = prefs.getBoolean("custom_paging", false);
            
            logDebug("Loaded preferences - Speed: " + scrollSpeedMultiplier + 
                    ", SmoothScroll: " + enableSmoothScroll +
                    ", CustomPaging: " + customPagingEnabled);
        } catch (Exception e) {
            logDebug("Failed to load preferences: " + e.getMessage());
        }
    }

    /**
     * Hook RecyclerView scroll methods
     */
    private void hookRecyclerViewScroll() {
        try {
            Class<?> recyclerViewClass = XposedHelpers.findClass(
                "androidx.recyclerview.widget.RecyclerView", 
                classLoader
            );
            
            if (recyclerViewClass == null) {
                recyclerViewClass = XposedHelpers.findClass(
                    "android.support.v7.widget.RecyclerView", 
                    classLoader
                );
            }
            
            if (recyclerViewClass == null) {
                logDebug("RecyclerView class not found");
                return;
            }

            // Hook scrollBy method to adjust scroll speed
            XposedHelpers.findAndHookMethod(
                recyclerViewClass,
                "scrollBy",
                int.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (scrollSpeedMultiplier != 1.0f) {
                            int x = (int) param.args[0];
                            int y = (int) param.args[1];
                            
                            // Apply speed multiplier
                            param.args[0] = (int) (x * scrollSpeedMultiplier);
                            param.args[1] = (int) (y * scrollSpeedMultiplier);
                            
                            logDebug("Adjusted scroll: x=" + x + "->" + param.args[0] + 
                                    ", y=" + y + "->" + param.args[1]);
                        }
                    }
                }
            );

            // Hook smoothScrollBy for smooth scroll control
            XposedHelpers.findAndHookMethod(
                recyclerViewClass,
                "smoothScrollBy",
                int.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (!enableSmoothScroll) {
                            // Convert smooth scroll to instant scroll
                            Object recyclerView = param.thisObject;
                            int x = (int) param.args[0];
                            int y = (int) param.args[1];
                            XposedHelpers.callMethod(recyclerView, "scrollBy", x, y);
                            param.setResult(null);
                            logDebug("Converted smooth scroll to instant scroll");
                        } else if (scrollSpeedMultiplier != 1.0f) {
                            // Apply speed multiplier to smooth scroll
                            int x = (int) param.args[0];
                            int y = (int) param.args[1];
                            param.args[0] = (int) (x * scrollSpeedMultiplier);
                            param.args[1] = (int) (y * scrollSpeedMultiplier);
                        }
                    }
                }
            );

            logDebug("Successfully hooked RecyclerView scroll methods");
        } catch (Throwable e) {
            logDebug("Failed to hook RecyclerView scroll: " + e.getMessage());
        }
    }

    /**
     * Hook feed adapter methods
     */
    private void hookFeedAdapter() {
        // Try to find TikTok's feed adapter class
        String[] adapterClasses = {
            "com.ss.android.ugc.aweme.feed.adapter.FeedAdapter",
            "com.ss.android.ugc.aweme.feed.adapter.VideoFeedAdapter",
            "com.ss.android.ugc.aweme.homepage.ui.view.MainActivityFeedAdapter"
        };

        boolean hooked = false;
        for (String className : adapterClasses) {
            try {
                Class<?> adapterClass = XposedHelpers.findClass(className, classLoader);
                
                // Hook onBindViewHolder to customize feed items
                for (Method method : adapterClass.getDeclaredMethods()) {
                    String methodName = method.getName();
                    if (methodName.equals("onBindViewHolder") || 
                        methodName.contains("bind") || 
                        methodName.contains("Bind")) {
                        
                        XposedBridge.hookMethod(method, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                logDebug("Feed item bound - position available for customization");
                                // Custom logic can be added here to modify feed items during scroll
                            }
                        });
                    }
                }
                
                logDebug("Successfully hooked feed adapter: " + className);
                hooked = true;
                break;
            } catch (Throwable e) {
                // Try next adapter class
            }
        }
        
        if (!hooked) {
            logDebug("No feed adapter class found - skipping adapter hooks");
        }
    }

    /**
     * Hook LayoutManager for custom paging behavior
     */
    private void hookLayoutManager() {
        if (!customPagingEnabled) return;
        
        try {
            Class<?> layoutManagerClass = XposedHelpers.findClass(
                "androidx.recyclerview.widget.LinearLayoutManager",
                classLoader
            );
            
            if (layoutManagerClass == null) {
                layoutManagerClass = XposedHelpers.findClass(
                    "android.support.v7.widget.LinearLayoutManager",
                    classLoader
                );
            }
            
            if (layoutManagerClass == null) {
                logDebug("LinearLayoutManager class not found");
                return;
            }

            // Determine the RecyclerView package prefix based on which LayoutManager was found
            String recyclerViewPackage = layoutManagerClass.getName().contains("androidx") 
                ? "androidx.recyclerview.widget.RecyclerView" 
                : "android.support.v7.widget.RecyclerView";

            // Hook scrollVerticallyBy for custom paging
            try {
                XposedHelpers.findAndHookMethod(
                    layoutManagerClass,
                    "scrollVerticallyBy",
                    int.class,
                    XposedHelpers.findClass(recyclerViewPackage + "$Recycler", classLoader),
                    XposedHelpers.findClass(recyclerViewPackage + "$State", classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Custom paging logic can be implemented here
                            int dy = (int) param.args[0];
                            logDebug("Vertical scroll by: " + dy);
                        }
                    }
                );
                
                logDebug("Successfully hooked LayoutManager for custom paging");
            } catch (Exception e) {
                logDebug("Failed to hook scrollVerticallyBy: " + e.getMessage());
            }
        } catch (Throwable e) {
            logDebug("Failed to hook LayoutManager: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return TAG;
    }
}
