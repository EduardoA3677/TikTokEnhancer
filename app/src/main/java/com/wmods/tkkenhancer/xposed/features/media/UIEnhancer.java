package com.wmods.tkkenhancer.xposed.features.media;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * UI Enhancer Feature for TikTok
 * Provides additional UI enhancements discovered through smali analysis:
 * - Hide "Live" badge
 * - Hide "Shop" tab
 * - Hide sponsored badges
 * - Remove watermarks from UI elements
 * - Hide suggestion overlays
 * 
 * Based on smali analysis of com.ss.android.ugc.aweme
 */
public class UIEnhancer extends Feature {

    private static final String TAG = "UIEnhancer";
    
    // UI element patterns to hide
    private boolean hideLiveBadge = false;
    private boolean hideShopTab = false;
    private boolean hideSponsoredBadge = false;
    private boolean hideWatermarks = false;
    private boolean hideSuggestions = false;

    public UIEnhancer(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
        loadPreferences();
    }

    @Override
    public void doHook() throws Throwable {
        if (!prefs.getBoolean("ui_enhancer", false)) return;

        logDebug("Initializing UI Enhancer feature");

        try {
            if (hideLiveBadge) {
                hookLiveBadge();
            }
            
            if (hideShopTab) {
                hookShopTab();
            }
            
            if (hideSponsoredBadge) {
                hookSponsoredBadge();
            }
            
            if (hideWatermarks) {
                hookWatermarks();
            }
            
            if (hideSuggestions) {
                hookSuggestions();
            }

            logDebug("UI Enhancer feature initialized successfully");
        } catch (Exception e) {
            logDebug("Failed to initialize UI Enhancer: " + e.getMessage());
            log(e);
        }
    }

    /**
     * Load preferences for UI enhancements
     */
    private void loadPreferences() {
        hideLiveBadge = prefs.getBoolean("hide_live_badge", false);
        hideShopTab = prefs.getBoolean("hide_shop_tab", false);
        hideSponsoredBadge = prefs.getBoolean("hide_sponsored_badge", true);
        hideWatermarks = prefs.getBoolean("hide_watermarks", false);
        hideSuggestions = prefs.getBoolean("hide_suggestions", false);
        
        logDebug("UI preferences loaded - Live: " + hideLiveBadge + 
                ", Shop: " + hideShopTab + 
                ", Sponsored: " + hideSponsoredBadge +
                ", Watermarks: " + hideWatermarks +
                ", Suggestions: " + hideSuggestions);
    }

    /**
     * Hook to hide Live badge
     */
    private void hookLiveBadge() {
        try {
            String[] liveBadgeClasses = {
                "com.ss.android.ugc.aweme.feed.ui.LiveBadgeView",
                "com.ss.android.ugc.aweme.live.LiveBadge",
                "com.ss.android.ugc.aweme.feed.model.live.LiveBanner"
            };

            for (String className : liveBadgeClasses) {
                try {
                    Class<?> liveBadgeClass = XposedHelpers.findClass(className, classLoader);
                    
                    // Hook setVisibility to force GONE
                    XposedHelpers.findAndHookMethod(
                        liveBadgeClass,
                        "setVisibility",
                        int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.args[0] = View.GONE;
                                logDebug("Hidden Live badge");
                            }
                        }
                    );
                    
                    logDebug("Successfully hooked Live badge class: " + className);
                } catch (Exception e) {
                    // Try next class
                }
            }
        } catch (Exception e) {
            logDebug("Failed to hook Live badge: " + e.getMessage());
        }
    }

    /**
     * Hook to hide Shop tab
     */
    private void hookShopTab() {
        try {
            String[] shopTabClasses = {
                "com.ss.android.ugc.aweme.commerce.ShopTabFragment",
                "com.ss.android.ugc.aweme.ecommerce.base.mall.MallFragment",
                "com.ss.android.ugc.aweme.ecommerce.base.osp.repository.ShopRepository"
            };

            for (String className : shopTabClasses) {
                try {
                    Class<?> shopTabClass = XposedHelpers.findClass(className, classLoader);
                    
                    // Hook lifecycle methods to prevent display
                    for (Method method : shopTabClass.getDeclaredMethods()) {
                        String methodName = method.getName().toLowerCase();
                        if (methodName.equals("onresume") || 
                            methodName.equals("onstart") ||
                            methodName.contains("show")) {
                            
                            XposedBridge.hookMethod(method, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    param.setResult(null);
                                    logDebug("Blocked Shop tab display");
                                }
                            });
                        }
                    }
                    
                    logDebug("Successfully hooked Shop tab class: " + className);
                } catch (Exception e) {
                    // Try next class
                }
            }
        } catch (Exception e) {
            logDebug("Failed to hook Shop tab: " + e.getMessage());
        }
    }

    /**
     * Hook to hide sponsored/promoted badges
     */
    private void hookSponsoredBadge() {
        try {
            // Hook TextView.setText to filter sponsored labels
            Class<?> textViewClass = TextView.class;
            
            XposedHelpers.findAndHookMethod(
                textViewClass,
                "setText",
                CharSequence.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        CharSequence text = (CharSequence) param.args[0];
                        if (text != null) {
                            String textStr = text.toString().toLowerCase();
                            
                            // Check for sponsored/promoted keywords
                            if (textStr.contains("sponsored") || 
                                textStr.contains("promoted") ||
                                textStr.contains("advertisement") ||
                                textStr.contains("ad")) {
                                
                                // Hide the TextView
                                TextView tv = (TextView) param.thisObject;
                                tv.setVisibility(View.GONE);
                                logDebug("Hidden sponsored badge: " + text);
                            }
                        }
                    }
                }
            );
            
            logDebug("Successfully hooked TextView for sponsored badges");
        } catch (Exception e) {
            logDebug("Failed to hook sponsored badge: " + e.getMessage());
        }
    }

    /**
     * Hook to hide watermarks
     */
    private void hookWatermarks() {
        try {
            String[] watermarkClasses = {
                "com.ss.android.ugc.aweme.shortvideo.ui.WatermarkView",
                "com.ss.android.ugc.aweme.feed.ui.WaterMark",
                "com.ss.android.ugc.aweme.common.ui.WatermarkLayout"
            };

            for (String className : watermarkClasses) {
                try {
                    Class<?> watermarkClass = XposedHelpers.findClass(className, classLoader);
                    
                    // Hook setVisibility
                    XposedHelpers.findAndHookMethod(
                        watermarkClass,
                        "setVisibility",
                        int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.args[0] = View.GONE;
                                logDebug("Hidden watermark view");
                            }
                        }
                    );
                    
                    logDebug("Successfully hooked watermark class: " + className);
                } catch (Exception e) {
                    // Try next class
                }
            }
        } catch (Exception e) {
            logDebug("Failed to hook watermarks: " + e.getMessage());
        }
    }

    /**
     * Hook to hide suggestion overlays
     */
    private void hookSuggestions() {
        try {
            String[] suggestionClasses = {
                "com.ss.android.ugc.aweme.feed.ui.SuggestionPanel",
                "com.ss.android.ugc.aweme.discover.ui.SuggestionView",
                "com.ss.android.ugc.aweme.recommend.RecommendationOverlay"
            };

            for (String className : suggestionClasses) {
                try {
                    Class<?> suggestionClass = XposedHelpers.findClass(className, classLoader);
                    
                    // Hook show/display methods
                    for (Method method : suggestionClass.getDeclaredMethods()) {
                        String methodName = method.getName().toLowerCase();
                        if (methodName.equals("show") || 
                            methodName.equals("display") ||
                            methodName.contains("recommend")) {
                            
                            XposedBridge.hookMethod(method, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    param.setResult(null);
                                    logDebug("Blocked suggestion display");
                                }
                            });
                        }
                    }
                    
                    logDebug("Successfully hooked suggestion class: " + className);
                } catch (Exception e) {
                    // Try next class
                }
            }
        } catch (Exception e) {
            logDebug("Failed to hook suggestions: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public String getPluginName() {
        return TAG;
    }
}
