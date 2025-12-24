package com.wmods.tkkenhancer.xposed.features.media;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Profile Icon Hider Feature for TikTok
 * Allows users to hide specific icons from the profile page:
 * - Live icon (goes live button/indicator)
 * - Coin icon (wallet/money icon)
 * 
 * Based on smali analysis of TikTok profile UI components
 * 
 * @author TikTok Enhancer
 */
public class ProfileIconHider extends Feature {

    private static final String TAG = "ProfileIconHider";
    
    private boolean hideLiveIcon = false;
    private boolean hideCoinIcon = false;

    public ProfileIconHider(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        // Load preferences
        hideLiveIcon = prefs.getBoolean("hide_profile_live_icon", false);
        hideCoinIcon = prefs.getBoolean("hide_profile_coin_icon", false);

        if (!hideLiveIcon && !hideCoinIcon) return;

        logDebug("Initializing Profile Icon Hider");
        logDebug("Hide Live Icon: " + hideLiveIcon);
        logDebug("Hide Coin Icon: " + hideCoinIcon);

        try {
            // Hook View setVisibility to intercept icon visibility changes
            hookViewSetVisibility();

            // Hook ImageView setImageResource to prevent icon loading
            hookImageViewSetImageResource();

            // Hook profile fragment/activity classes
            hookProfileFragments();

            logDebug("Profile Icon Hider initialized successfully");
        } catch (Throwable e) {
            logDebug("Error initializing Profile Icon Hider: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook View.setVisibility to hide profile icons
     */
    private void hookViewSetVisibility() {
        try {
            XposedHelpers.findAndHookMethod(View.class, "setVisibility", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        
                        // Check if this view should be hidden
                        if (shouldHideView(view)) {
                            logDebug("Hiding profile icon view: " + view.getClass().getSimpleName());
                            param.args[0] = View.GONE;
                        }
                    }
                }
            );
            logDebug("Hooked View.setVisibility successfully");
        } catch (Throwable e) {
            logDebug("Could not hook View.setVisibility: " + e.getMessage());
        }
    }

    /**
     * Hook ImageView.setImageResource to prevent loading profile icons
     */
    private void hookImageViewSetImageResource() {
        try {
            XposedHelpers.findAndHookMethod(ImageView.class, "setImageResource", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ImageView imageView = (ImageView) param.thisObject;
                        
                        // Check if this is an icon we should hide
                        if (shouldHideView(imageView)) {
                            logDebug("Preventing icon load for: " + imageView.getClass().getSimpleName());
                            imageView.setVisibility(View.GONE);
                            param.setResult(null);
                        }
                    }
                }
            );
            logDebug("Hooked ImageView.setImageResource successfully");
        } catch (Throwable e) {
            logDebug("Could not hook ImageView.setImageResource: " + e.getMessage());
        }
    }

    /**
     * Hook profile fragments to hide icons on profile page
     */
    private void hookProfileFragments() {
        try {
            // Try to find and hook profile-related classes
            String[] profileClasses = {
                "com.ss.android.ugc.aweme.profile.ui.ProfileFragment",
                "com.ss.android.ugc.aweme.profile.ui.UserProfileFragment",
                "com.ss.android.ugc.aweme.profile.ui.MyProfileFragment",
                "com.ss.android.ugc.aweme.profile.presenter.ProfilePresenter"
            };

            for (String className : profileClasses) {
                try {
                    Class<?> profileClass = XposedHelpers.findClass(className, classLoader);
                    
                    // Hook onViewCreated or onResume to hide icons
                    XposedHelpers.findAndHookMethod(profileClass, "onResume",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                logDebug("Profile screen resumed - scanning for icons to hide");
                                
                                // Try to get the root view and scan for icons
                                try {
                                    Object fragment = param.thisObject;
                                    View rootView = (View) XposedHelpers.callMethod(fragment, "getView");
                                    
                                    if (rootView != null) {
                                        hideIconsInViewHierarchy(rootView);
                                    }
                                } catch (Throwable ignored) {
                                    // Method might not exist
                                }
                            }
                        }
                    );
                    logDebug("Hooked profile class: " + className);
                } catch (Throwable e) {
                    // Class might not exist, continue with next
                    logDebug("Could not hook profile class " + className + ": " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            logDebug("Error hooking profile fragments: " + e.getMessage());
        }
    }

    /**
     * Check if a view should be hidden based on its properties
     */
    private boolean shouldHideView(View view) {
        if (view == null) return false;

        try {
            String viewId = getResourceName(view);
            String contentDesc = view.getContentDescription() != null ? 
                view.getContentDescription().toString().toLowerCase() : "";

            // Check for live icon
            if (hideLiveIcon) {
                if (viewId.contains("live") || 
                    contentDesc.contains("live") ||
                    contentDesc.contains("go live") ||
                    viewId.contains("streaming")) {
                    return true;
                }
            }

            // Check for coin/wallet icon
            if (hideCoinIcon) {
                if (viewId.contains("coin") || 
                    viewId.contains("wallet") ||
                    viewId.contains("balance") ||
                    contentDesc.contains("coin") ||
                    contentDesc.contains("wallet") ||
                    contentDesc.contains("money") ||
                    contentDesc.contains("balance")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Ignore errors in resource name retrieval
        }

        return false;
    }

    /**
     * Recursively hide icons in view hierarchy
     */
    private void hideIconsInViewHierarchy(View view) {
        if (view == null) return;

        try {
            // Check and hide current view if needed
            if (shouldHideView(view)) {
                logDebug("Hiding icon in view hierarchy");
                view.setVisibility(View.GONE);
            }

            // Recursively check children
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                int childCount = viewGroup.getChildCount();
                
                for (int i = 0; i < childCount; i++) {
                    View child = viewGroup.getChildAt(i);
                    hideIconsInViewHierarchy(child);
                }
            }
        } catch (Throwable e) {
            logDebug("Error hiding icons in hierarchy: " + e.getMessage());
        }
    }

    /**
     * Get resource name for a view
     */
    private String getResourceName(View view) {
        try {
            int id = view.getId();
            if (id != View.NO_ID && view.getResources() != null) {
                return view.getResources().getResourceEntryName(id).toLowerCase();
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    @NonNull
    @Override
    public String getPluginName() {
        return TAG;
    }
}
