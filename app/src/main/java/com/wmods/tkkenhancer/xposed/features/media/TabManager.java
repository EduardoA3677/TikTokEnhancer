package com.wmods.tkkenhancer.xposed.features.media;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Tab Manager Feature for TikTok
 * Allows users to show/hide specific tabs in the TikTok bottom navigation:
 * - Home tab
 * - Friends tab
 * - Plus/Create tab
 * - Inbox tab
 * - Profile tab
 * - Shop tab (if available)
 * 
 * Based on smali analysis of TikTok bottom navigation components
 * 
 * @author TikTok Enhancer
 */
public class TabManager extends Feature {

    private static final String TAG = "TabManager";
    
    private boolean hideHomeTab = false;
    private boolean hideFriendsTab = false;
    private boolean hideCreateTab = false;
    private boolean hideInboxTab = false;
    private boolean hideProfileTab = false;
    private boolean hideShopTab = false;

    public TabManager(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {
        // Load preferences
        hideHomeTab = prefs.getBoolean("hide_home_tab", false);
        hideFriendsTab = prefs.getBoolean("hide_friends_tab", false);
        hideCreateTab = prefs.getBoolean("hide_create_tab", false);
        hideInboxTab = prefs.getBoolean("hide_inbox_tab", false);
        hideProfileTab = prefs.getBoolean("hide_profile_tab", false);
        hideShopTab = prefs.getBoolean("hide_shop_tab", false);

        // Don't activate if no tabs are hidden
        if (!hideHomeTab && !hideFriendsTab && !hideCreateTab && 
            !hideInboxTab && !hideProfileTab && !hideShopTab) {
            return;
        }

        logDebug("Initializing Tab Manager");
        logDebug("Hide Home: " + hideHomeTab);
        logDebug("Hide Friends: " + hideFriendsTab);
        logDebug("Hide Create: " + hideCreateTab);
        logDebug("Hide Inbox: " + hideInboxTab);
        logDebug("Hide Profile: " + hideProfileTab);
        logDebug("Hide Shop: " + hideShopTab);

        try {
            // Hook bottom tab view classes
            hookBottomTabView();

            // Hook tab protocol classes
            hookTabProtocols();

            // Hook main activity to hide tabs on startup
            hookMainActivity();

            logDebug("Tab Manager initialized successfully");
        } catch (Throwable e) {
            logDebug("Error initializing Tab Manager: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Hook bottom tab view to control tab visibility
     */
    private void hookBottomTabView() {
        try {
            // Hook View.setVisibility for tab views
            XposedHelpers.findAndHookMethod(View.class, "setVisibility", int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        
                        if (shouldHideTab(view)) {
                            logDebug("Hiding tab view: " + view.getClass().getSimpleName());
                            param.args[0] = View.GONE;
                        }
                    }
                }
            );
            logDebug("Hooked View.setVisibility for tabs");
        } catch (Throwable e) {
            logDebug("Could not hook View.setVisibility: " + e.getMessage());
        }
    }

    /**
     * Hook tab protocol classes
     */
    private void hookTabProtocols() {
        try {
            String[] tabProtocolClasses = {
                "com.ss.android.ugc.aweme.homepage.ui.view.tab.bottom.hometab.HomeTabProtocol",
                "com.ss.android.ugc.aweme.homepage.ui.view.tab.bottom.publishtab.PublishTabProtocol",
                "com.bytedance.tiktok.homepage.mainfragment.BottomTabProtocol",
                "com.bytedance.tiktok.homepage.mainfragment.HomeTabAbility"
            };

            for (String className : tabProtocolClasses) {
                try {
                    Class<?> tabClass = XposedHelpers.findClass(className, classLoader);
                    
                    // Hook visibility-related methods
                    XposedHelpers.findAndHookMethod(tabClass, "setVisibility", int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                Object tabInstance = param.thisObject;
                                
                                // Check if this tab should be hidden
                                if (shouldHideTabProtocol(tabInstance)) {
                                    logDebug("Hiding tab protocol: " + className);
                                    param.args[0] = View.GONE;
                                }
                            }
                        }
                    );
                    logDebug("Hooked tab protocol: " + className);
                } catch (Throwable e) {
                    logDebug("Could not hook tab protocol " + className + ": " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            logDebug("Error hooking tab protocols: " + e.getMessage());
        }
    }

    /**
     * Hook main activity to hide tabs on app start
     */
    private void hookMainActivity() {
        try {
            String[] mainActivityClasses = {
                "com.ss.android.ugc.aweme.main.MainActivity",
                "com.ss.android.ugc.aweme.main.MainActivityV2"
            };

            for (String className : mainActivityClasses) {
                try {
                    Class<?> mainClass = XposedHelpers.findClass(className, classLoader);
                    
                    XposedHelpers.findAndHookMethod(mainClass, "onResume",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                logDebug("MainActivity resumed - scanning for tabs to hide");
                                
                                try {
                                    Object activity = param.thisObject;
                                    // Get window and scan for tab views
                                    Object window = XposedHelpers.callMethod(activity, "getWindow");
                                    View decorView = (View) XposedHelpers.callMethod(window, "getDecorView");
                                    
                                    if (decorView != null) {
                                        hideTabsInViewHierarchy(decorView);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    );
                    logDebug("Hooked MainActivity: " + className);
                } catch (Throwable e) {
                    logDebug("Could not hook MainActivity " + className + ": " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            logDebug("Error hooking MainActivity: " + e.getMessage());
        }
    }

    /**
     * Check if a view is a tab that should be hidden
     */
    private boolean shouldHideTab(View view) {
        if (view == null) return false;

        try {
            String viewId = getResourceName(view);
            String contentDesc = view.getContentDescription() != null ? 
                view.getContentDescription().toString().toLowerCase() : "";
            String className = view.getClass().getName().toLowerCase();

            // Check for home tab
            if (hideHomeTab && (viewId.contains("home") || contentDesc.contains("home") || 
                className.contains("home"))) {
                return true;
            }

            // Check for friends tab
            if (hideFriendsTab && (viewId.contains("friend") || contentDesc.contains("friend") || 
                className.contains("friend"))) {
                return true;
            }

            // Check for create/plus tab
            if (hideCreateTab && (viewId.contains("create") || viewId.contains("publish") ||
                viewId.contains("plus") || contentDesc.contains("create") || 
                contentDesc.contains("record") || className.contains("publish"))) {
                return true;
            }

            // Check for inbox tab
            if (hideInboxTab && (viewId.contains("inbox") || viewId.contains("message") ||
                contentDesc.contains("inbox") || contentDesc.contains("message") ||
                className.contains("inbox"))) {
                return true;
            }

            // Check for profile tab
            if (hideProfileTab && (viewId.contains("profile") || viewId.contains("me") ||
                contentDesc.contains("profile") || contentDesc.contains("me") ||
                className.contains("profile"))) {
                return true;
            }

            // Check for shop tab
            if (hideShopTab && (viewId.contains("shop") || viewId.contains("mall") ||
                contentDesc.contains("shop") || contentDesc.contains("mall") ||
                className.contains("shop"))) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * Check if a tab protocol instance should be hidden
     */
    private boolean shouldHideTabProtocol(Object tabInstance) {
        try {
            String className = tabInstance.getClass().getName().toLowerCase();
            
            if (hideHomeTab && className.contains("home")) return true;
            if (hideFriendsTab && className.contains("friend")) return true;
            if (hideCreateTab && (className.contains("publish") || className.contains("create"))) return true;
            if (hideInboxTab && className.contains("inbox")) return true;
            if (hideProfileTab && className.contains("profile")) return true;
            if (hideShopTab && className.contains("shop")) return true;
        } catch (Throwable ignored) {
        }
        
        return false;
    }

    /**
     * Recursively hide tabs in view hierarchy
     */
    private void hideTabsInViewHierarchy(View view) {
        if (view == null) return;

        try {
            // Check and hide current view if it's a tab
            if (shouldHideTab(view)) {
                logDebug("Hiding tab in view hierarchy");
                view.setVisibility(View.GONE);
            }

            // Recursively check children
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                int childCount = viewGroup.getChildCount();
                
                for (int i = 0; i < childCount; i++) {
                    View child = viewGroup.getChildAt(i);
                    hideTabsInViewHierarchy(child);
                }
            }
        } catch (Throwable e) {
            logDebug("Error hiding tabs in hierarchy: " + e.getMessage());
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
