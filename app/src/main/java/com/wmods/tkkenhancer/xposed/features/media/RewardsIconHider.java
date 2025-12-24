package com.wmods.tkkenhancer.xposed.features.media;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Rewards Icon Hider Feature for TikTok
 * Hides the TikTok Rewards floating icon from the main page
 * 
 * Based on smali analysis:
 * - Target: Floating action buttons and reward icons
 * - Classes: MainActivity, HomePageFragment, FloatingButton views
 * - Methods: View visibility, button initialization
 * 
 * The TikTok Rewards icon is typically a floating action button (FAB)
 * displayed on the main feed page to promote the rewards program.
 */
public class RewardsIconHider extends Feature {

    private static final String TAG = "RewardsIconHider";
    
    // Maximum depth for recursive view traversal to prevent performance issues
    private static final int MAX_VIEW_HIERARCHY_DEPTH = 10;
    
    // Common class name patterns for rewards-related components
    private static final String[] REWARDS_PATTERNS = {
        "reward",
        "Reward",
        "REWARD",
        "point",
        "Point",
        "coin",
        "Coin",
        "incentive",
        "Incentive"
    };

    public RewardsIconHider(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("hide_rewards_icon", false)) return;

        logDebug("Initializing Rewards Icon Hider feature");

        try {
            // Hook floating action button visibility
            hookFloatingActionButton();
            
            // Hook reward-related views
            hookRewardViews();
            
            // Hook main activity for reward icon
            hookMainActivity();

            logDebug("Rewards Icon Hider feature initialized successfully");
        } catch (Exception e) {
            logDebug("Failed to initialize Rewards Icon Hider: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook FloatingActionButton to hide reward icons
     */
    private void hookFloatingActionButton() {
        try {
            // Try Material Design FloatingActionButton
            String[] fabClasses = {
                "com.google.android.material.floatingactionbutton.FloatingActionButton",
                "android.support.design.widget.FloatingActionButton"
            };

            for (String fabClassName : fabClasses) {
                try {
                    Class<?> fabClass = XposedHelpers.findClass(fabClassName, classLoader);
                    
                    // Hook setVisibility to hide reward FABs
                    XposedHelpers.findAndHookMethod(
                        fabClass,
                        "setVisibility",
                        int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                Object fab = param.thisObject;
                                
                                // Check if this FAB is reward-related
                                if (isRewardRelated(fab)) {
                                    // Force to GONE
                                    param.args[0] = View.GONE;
                                    logDebug("Hidden reward FloatingActionButton");
                                }
                            }
                        }
                    );
                    
                    logDebug("Successfully hooked FloatingActionButton: " + fabClassName);
                    break;
                } catch (Exception e) {
                    // Try next FAB class
                }
            }
        } catch (Exception e) {
            logDebug("Failed to hook FloatingActionButton: " + e.getMessage());
        }
    }

    /**
     * Hook reward-related view classes
     */
    private void hookRewardViews() {
        try {
            // Search for reward-related classes in TikTok package
            String[] rewardClasses = {
                "com.ss.android.ugc.aweme.reward.RewardIconView",
                "com.ss.android.ugc.aweme.incentive.IncentiveFloatButton",
                "com.ss.android.ugc.aweme.incentive.IncentiveView",
                "com.ss.android.ugc.aweme.compliance.business.banappeal.RewardEntrance"
            };

            for (String className : rewardClasses) {
                try {
                    Class<?> rewardClass = XposedHelpers.findClass(className, classLoader);
                    
                    // Hook all methods to hide the view
                    hookViewMethods(rewardClass);
                    
                    logDebug("Successfully hooked reward class: " + className);
                } catch (Exception e) {
                    // Class doesn't exist, continue to next
                }
            }
        } catch (Exception e) {
            logDebug("Failed to hook reward views: " + e.getMessage());
        }
    }

    /**
     * Hook main activity to intercept reward icon initialization
     */
    private void hookMainActivity() {
        try {
            String[] activityClasses = {
                "com.ss.android.ugc.aweme.main.MainActivity",
                "com.ss.android.ugc.aweme.main.HomeActivity",
                "com.ss.android.ugc.aweme.splash.SplashActivity"
            };

            for (String className : activityClasses) {
                try {
                    Class<?> activityClass = XposedHelpers.findClass(className, classLoader);
                    
                    // Hook onCreate to intercept reward icon setup
                    XposedHelpers.findAndHookMethod(
                        activityClass,
                        "onCreate",
                        android.os.Bundle.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                Object activity = param.thisObject;
                                
                                // Search for reward views in activity and hide them
                                hideRewardViewsInActivity(activity);
                                
                                logDebug("Checked for reward views in activity: " + className);
                            }
                        }
                    );
                    
                    logDebug("Successfully hooked main activity: " + className);
                } catch (Exception e) {
                    // Activity doesn't exist, continue
                }
            }
        } catch (Exception e) {
            logDebug("Failed to hook main activity: " + e.getMessage());
        }
    }

    /**
     * Hook view methods to hide reward-related views
     */
    private void hookViewMethods(Class<?> viewClass) {
        try {
            // Hook setVisibility
            XposedHelpers.findAndHookMethod(
                viewClass,
                "setVisibility",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // Force to GONE
                        param.args[0] = View.GONE;
                        logDebug("Hidden reward view via setVisibility");
                    }
                }
            );

            // Hook show() methods if they exist
            for (Method method : viewClass.getDeclaredMethods()) {
                String methodName = method.getName().toLowerCase();
                if (methodName.equals("show") || 
                    methodName.equals("display") || 
                    methodName.equals("visible")) {
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Prevent showing
                            param.setResult(null);
                            logDebug("Blocked reward view show method");
                        }
                    });
                }
            }
        } catch (Exception e) {
            logDebug("Failed to hook view methods: " + e.getMessage());
        }
    }

    /**
     * Check if a view is reward-related based on class name or ID
     */
    private boolean isRewardRelated(Object view) {
        try {
            String className = view.getClass().getName().toLowerCase();
            
            // Check class name against patterns
            for (String pattern : REWARDS_PATTERNS) {
                if (className.contains(pattern.toLowerCase())) {
                    return true;
                }
            }
            
            // Check view ID if it's a View
            if (view instanceof View) {
                View v = (View) view;
                try {
                    int id = v.getId();
                    if (id != View.NO_ID) {
                        try {
                            String idName = v.getResources().getResourceEntryName(id);
                            if (idName != null) {
                                String idNameLower = idName.toLowerCase();
                                for (String pattern : REWARDS_PATTERNS) {
                                    if (idNameLower.contains(pattern.toLowerCase())) {
                                        return true;
                                    }
                                }
                            }
                        } catch (android.content.res.Resources.NotFoundException e) {
                            // Resource not found, skip ID check
                        }
                    }
                } catch (Exception e) {
                    // Ignore other resource lookup errors
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hide reward views in an activity
     */
    private void hideRewardViewsInActivity(Object activity) {
        try {
            if (!(activity instanceof android.app.Activity)) {
                return;
            }
            
            android.app.Activity act = (android.app.Activity) activity;
            View rootView = act.getWindow().getDecorView();
            
            // Recursively search and hide reward views
            hideRewardViewsRecursive(rootView);
            
        } catch (Exception e) {
            logDebug("Failed to hide reward views in activity: " + e.getMessage());
        }
    }

    /**
     * Recursively search and hide reward views with depth limit
     */
    private void hideRewardViewsRecursive(View view) {
        hideRewardViewsRecursive(view, 0);
    }

    /**
     * Recursively search and hide reward views with depth tracking
     * @param view The view to check
     * @param depth Current depth in view hierarchy
     */
    private void hideRewardViewsRecursive(View view, int depth) {
        try {
            if (view == null) return;
            
            // Limit recursion depth to prevent performance issues
            if (depth > MAX_VIEW_HIERARCHY_DEPTH) {
                logDebug("Reached max depth, stopping recursion");
                return;
            }
            
            // Check if current view is reward-related
            if (isRewardRelated(view)) {
                view.setVisibility(View.GONE);
                logDebug("Hidden reward view: " + view.getClass().getSimpleName() + " at depth " + depth);
            }
            
            // Check children if it's a ViewGroup
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                int childCount = group.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View child = group.getChildAt(i);
                    hideRewardViewsRecursive(child, depth + 1);
                }
            }
        } catch (Exception e) {
            // Ignore errors during recursive search
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return TAG;
    }
}
