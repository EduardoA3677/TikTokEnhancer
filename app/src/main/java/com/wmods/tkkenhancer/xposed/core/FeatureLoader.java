package com.wmods.tkkenhancer.xposed.core;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.wmods.tkkenhancer.App;
import com.wmods.tkkenhancer.BuildConfig;
import com.wmods.tkkenhancer.UpdateChecker;
import com.wmods.tkkenhancer.xposed.core.components.AlertDialogTkk;
import com.wmods.tkkenhancer.xposed.core.components.FMessageTkk;
import com.wmods.tkkenhancer.xposed.core.components.SharedPreferencesWrapper;
import com.wmods.tkkenhancer.xposed.core.components.TkContactTkk;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;
import com.wmods.tkkenhancer.xposed.core.devkit.UnobfuscatorCache;
import com.wmods.tkkenhancer.xposed.features.others.DebugFeature;
import com.wmods.tkkenhancer.xposed.spoofer.HookBL;
import com.wmods.tkkenhancer.xposed.utils.DesignUtils;
import com.wmods.tkkenhancer.xposed.utils.ReflectionUtils;
import com.wmods.tkkenhancer.xposed.utils.ResId;
import com.wmods.tkkenhancer.xposed.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import lombok.Getter;
import lombok.Setter;

public class FeatureLoader {
    public static Application mApp;

    public final static String PACKAGE_TKK = "com.zhiliaoapp.musically";

    private static final ArrayList<ErrorItem> list = new ArrayList<>();
    private static final List<String> FALLBACK_VERSIONS = Arrays.asList("43.xx", "44.xx", "45.xx");
    private static List<String> supportedVersions = FALLBACK_VERSIONS; // Initialize with fallback
    private static String currentVersion;

    public static void start(@NonNull ClassLoader loader, @NonNull XSharedPreferences pref, String sourceDir) {

        if (!Unobfuscator.initWithPath(sourceDir)) {
            XposedBridge.log("Can't init dexkit");
            return;
        }
        Feature.DEBUG = pref.getBoolean("enablelogs", true);
        Utils.xprefs = pref;

        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @SuppressWarnings("deprecation")
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mApp = (Application) param.args[0];

                // Inject Booloader Spoofer
                if (pref.getBoolean("bootloader_spoofer", false)) {
                    HookBL.hook(loader, pref);
                    XposedBridge.log("Bootloader Spoofer is Injected");
                }

                PackageManager packageManager = mApp.getPackageManager();
                pref.registerOnSharedPreferenceChangeListener((sharedPreferences, s) -> pref.reload());
                PackageInfo packageInfo = packageManager.getPackageInfo(mApp.getPackageName(), 0);
                XposedBridge.log(packageInfo.versionName);
                currentVersion = packageInfo.versionName;
                
                // Try to load supported versions, with fallback if resources not initialized yet
                // Check if ResId.array.supported_versions_tkk is initialized (not 0)
                try {
                    if (ResId.array.supported_versions_tkk != 0) {
                        String[] versionsArray = mApp.getResources().getStringArray(ResId.array.supported_versions_tkk);
                        if (versionsArray != null && versionsArray.length > 0) {
                            supportedVersions = Arrays.asList(versionsArray);
                            XposedBridge.log("Loaded supported versions from resources: " + String.join(", ", supportedVersions));
                        } else {
                            // Fallback to hardcoded versions if resources not loaded
                            supportedVersions = FALLBACK_VERSIONS;
                            XposedBridge.log("Using fallback supported versions list (resource array was empty)");
                        }
                    } else {
                        // ResId not initialized yet, use fallback
                        supportedVersions = FALLBACK_VERSIONS;
                        XposedBridge.log("ResId.array.supported_versions_tkk not initialized, using fallback");
                    }
                } catch (Exception e) {
                    // Resources not initialized yet, use fallback
                    supportedVersions = FALLBACK_VERSIONS;
                    XposedBridge.log("Resources not ready, using fallback supported versions: " + e.getMessage());
                    XposedBridge.log(e);
                }
                
                mApp.registerActivityLifecycleCallbacks(new TkCallback());
                try {
                    var timemillis = System.currentTimeMillis();
                    SharedPreferencesWrapper.hookInit(mApp.getClassLoader());
                    UnobfuscatorCache.init(mApp);
                    ReflectionUtils.initCache(mApp);
                    
                    // Check version support with improved pattern matching
                    // Support patterns like "43.xx" which should match "43.0.0", "43.1.5", etc.
                    // Ensure supportedVersions is not null or empty
                    if (supportedVersions == null || supportedVersions.isEmpty()) {
                        XposedBridge.log("WARNING: supportedVersions is null or empty, using fallback");
                        supportedVersions = FALLBACK_VERSIONS;
                    }
                    
                    boolean isSupported = false;
                    String matchedPattern = null;
                    
                    XposedBridge.log("Checking version " + packageInfo.versionName + " against patterns: " + String.join(", ", supportedVersions));
                    
                    for (String pattern : supportedVersions) {
                        // Replace .xx with empty string to get version prefix
                        String versionPrefix = pattern.replace(".xx", ".");
                        // Also support exact version matching
                        if (packageInfo.versionName.startsWith(versionPrefix) || packageInfo.versionName.equals(pattern)) {
                            isSupported = true;
                            matchedPattern = pattern;
                            break;
                        }
                    }
                    
                    if (!isSupported) {
                        XposedBridge.log("Version check: TikTok " + packageInfo.versionName + " not in supported list: " + String.join(", ", supportedVersions));
                        // Try to disable expiration version check (WhatsApp-specific feature)
                        // For TikTok, this may not be applicable, so we catch and log any errors
                        try {
                            disableExpirationVersion(mApp.getClassLoader());
                        } catch (Exception expError) {
                            XposedBridge.log("Note: Expiration version check not applicable for TikTok: " + expError.getMessage());
                        }
                        if (!pref.getBoolean("bypass_version_check", false)) {
                            // Ensure we have a valid list to display
                            String versionsList = (supportedVersions != null && !supportedVersions.isEmpty()) 
                                ? String.join(", ", supportedVersions) 
                                : String.join(", ", FALLBACK_VERSIONS);
                            
                            String sb = "Unsupported version: " +
                                    packageInfo.versionName +
                                    "\n" +
                                    "Supported versions: " + versionsList +
                                    "\n" +
                                    "Enable 'Bypass Version Check' in module settings to continue";
                            throw new Exception(sb);
                        } else {
                            XposedBridge.log("Version check bypassed by user preference");
                        }
                    } else {
                        XposedBridge.log("TikTok version " + packageInfo.versionName + " is supported (matched pattern: " + matchedPattern + ")");
                    }
                    initComponents(loader, pref);
                    // Register receivers AFTER initComponents to ensure privPrefs is initialized
                    registerReceivers();
                    plugins(loader, pref, packageInfo.versionName);
                    sendEnabledBroadcast(mApp);
//                    XposedHelpers.setStaticIntField(XposedHelpers.findClass("com.whatsapp.util.Log", loader), "level", 5);
                    var timemillis2 = System.currentTimeMillis() - timemillis;
                    XposedBridge.log("Loaded Hooks in " + timemillis2 + "ms");
                } catch (Throwable e) {
                    XposedBridge.log(e);
                    var error = new ErrorItem();
                    error.setPluginName("MainFeatures[Critical]");
                    error.setTiktokVersion(packageInfo.versionName);
                    error.setModuleVersion(BuildConfig.VERSION_NAME);
                    error.setMessage(e.getMessage());
                    error.setError(Arrays.toString(Arrays.stream(e.getStackTrace()).filter(s -> !s.getClassName().startsWith("android") && !s.getClassName().startsWith("com.android")).map(StackTraceElement::toString).toArray()));
                    list.add(error);
                }

            }
        });

        XposedHelpers.findAndHookMethod(TkkCore.getHomeActivityClass(loader), "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (!list.isEmpty()) {
                    var activity = (Activity) param.thisObject;
                    var msg = String.join("\n", list.stream().map(item -> item.getPluginName() + " - " + item.getMessage()).toArray(String[]::new));

                    new AlertDialogTkk(activity)
                            .setTitle(activity.getString(ResId.string.error_detected))
                            .setMessage(activity.getString(ResId.string.version_error) + msg + "\n\nCurrent Version: " + currentVersion + "\nSupported Versions:\n" + String.join("\n", supportedVersions))
                            .setPositiveButton(activity.getString(ResId.string.copy_to_clipboard), (dialog, which) -> {
                                var clipboard = (ClipboardManager) mApp.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("text", String.join("\n", list.stream().map(ErrorItem::toString).toArray(String[]::new)));
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(mApp, ResId.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .show();
                }
            }
        });
    }

    public static void disableExpirationVersion(ClassLoader classLoader) throws Exception {
        var expirationClass = Unobfuscator.loadExpirationClass(classLoader);
        var method = ReflectionUtils.findMethodUsingFilter(expirationClass, m -> m.getReturnType().equals(Date.class));
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var calendar = Calendar.getInstance();
                calendar.set(2099, 12, 31);
                param.setResult(calendar.getTime());
            }
        });
    }

    private static void initComponents(ClassLoader loader, XSharedPreferences pref) throws Exception {
        FMessageTkk.initialize(loader);
        TkkCore.Initialize(loader, pref);
        DesignUtils.setPrefs(pref);
        Utils.init(loader);
        AlertDialogTkk.initDialog(loader);
        TkContactTkk.initialize(loader);
        TkkCore.addListenerActivity((activity, state) -> {

            if (state == TkkCore.ActivityChangeState.ChangeType.RESUMED) {
                checkUpdate(activity);
            }

            // Check for WAE Update
            //noinspection ConstantValue
            if (App.isOriginalPackage() && pref.getBoolean("update_check", true)) {
                if (activity.getClass().getSimpleName().equals("HomeActivity") && state == TkkCore.ActivityChangeState.ChangeType.CREATED) {
                    CompletableFuture.runAsync(new UpdateChecker(activity));
                }
            }
        });
    }


    private static void checkUpdate(@NonNull Activity activity) {
        if (TkkCore.getPrivBoolean("need_restart", false)) {
            TkkCore.setPrivBoolean("need_restart", false);
            try {
                new AlertDialogTkk(activity).
                        setMessage(activity.getString(ResId.string.restart_tkk)).
                        setPositiveButton(activity.getString(ResId.string.yes), (dialog, which) -> {
                            if (!Utils.doRestart(activity))
                                Toast.makeText(activity, "Unable to rebooting activity", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(activity.getString(ResId.string.no), null)
                        .show();
            } catch (Throwable ignored) {
            }
        }
    }

    private static void registerReceivers() {
        // Reboot receiver
        BroadcastReceiver restartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (context.getPackageName().equals(intent.getStringExtra("PKG"))) {
                    var appName = context.getPackageManager().getApplicationLabel(context.getApplicationInfo());
                    Toast.makeText(context, context.getString(ResId.string.rebooting) + " " + appName + "...", Toast.LENGTH_SHORT).show();
                    if (!Utils.doRestart(context))
                        Toast.makeText(context, "Unable to rebooting " + appName, Toast.LENGTH_SHORT).show();
                }
            }
        };
        ContextCompat.registerReceiver(mApp, restartReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".TIKTOK.RESTART"), ContextCompat.RECEIVER_EXPORTED);

        /// TikTok receiver
        BroadcastReceiver tkkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendEnabledBroadcast(context);
            }
        };
        ContextCompat.registerReceiver(mApp, tkkReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".CHECK_TIKTOK"), ContextCompat.RECEIVER_EXPORTED);

        // Dialog receiver restart
        BroadcastReceiver restartManualReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TkkCore.setPrivBoolean("need_restart", true);
            }
        };
        ContextCompat.registerReceiver(mApp, restartManualReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".MANUAL_RESTART"), ContextCompat.RECEIVER_EXPORTED);
    }

    private static void sendEnabledBroadcast(Context context) {
        try {
            Intent tkkIntent = new Intent(BuildConfig.APPLICATION_ID + ".RECEIVER_TIKTOK");
            tkkIntent.putExtra("VERSION", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
            tkkIntent.putExtra("PKG", context.getPackageName());
            tkkIntent.setPackage(BuildConfig.APPLICATION_ID);
            context.sendBroadcast(tkkIntent);
        } catch (Exception ignored) {
        }
    }

    private static void plugins(@NonNull ClassLoader loader, @NonNull XSharedPreferences pref, @NonNull String versionTkk) throws Exception {

        // TikTok Features - Based on Smali Analysis
        // Repository: https://github.com/Eduardob3677/com_zhiliaoapp_musically_6
        // 
        // Core working features + newly implemented features enabled
        // Features load asynchronously to prevent ANR during startup
        
        var classes = new Class<?>[]{
                // Debug feature - always enabled for logging
                DebugFeature.class,
                
                // ✅ WORKING FEATURES - Verified against smali
                com.wmods.tkkenhancer.xposed.features.media.VideoDownload.class,      // Hooks: Video.getDownloadNoWatermarkAddr()
                com.wmods.tkkenhancer.xposed.features.media.AdBlocker.class,          // Hooks: Aweme.isAd(), isAdTraffic()
                
                // ✅ NEWLY IMPLEMENTED FEATURES - Enabled for testing
                com.wmods.tkkenhancer.xposed.features.media.AutoPlayControl.class,    // Auto-play control with player analysis
                com.wmods.tkkenhancer.xposed.features.media.StoryVideoSupport.class,  // Story video download
                com.wmods.tkkenhancer.xposed.features.media.LiveStreamDownload.class, // Live stream download
                com.wmods.tkkenhancer.xposed.features.privacy.AnalyticsBlocker.class, // Analytics blocking
                
                // ❌ DISABLED - Need proper TikTok implementation or causing issues
                // com.wmods.tkkenhancer.xposed.features.media.VideoDownloadImproved.class,
                // com.wmods.tkkenhancer.xposed.features.media.AdBlockerImproved.class,
                // com.wmods.tkkenhancer.xposed.features.media.BitrateControl.class,
                // com.wmods.tkkenhancer.xposed.features.media.CommentEnhancer.class,
                // com.wmods.tkkenhancer.xposed.features.media.ProfileEnhancer.class,
                // com.wmods.tkkenhancer.xposed.features.media.FeedFilter.class,
        };
        
        XposedBridge.log("Loading TikTok Plugins (Core + New Features)");
        
        // Use single thread to prevent race conditions and CPU overload
        var executorService = Executors.newFixedThreadPool(1);
        var times = new ArrayList<String>();
        
        for (var classe : classes) {
            CompletableFuture.runAsync(() -> {
                var timemillis = System.currentTimeMillis();
                try {
                    var constructor = classe.getConstructor(ClassLoader.class, XSharedPreferences.class);
                    var plugin = (Feature) constructor.newInstance(loader, pref);
                    plugin.doHook();
                    XposedBridge.log("✓ Loaded: " + classe.getSimpleName());
                } catch (Throwable e) {
                    XposedBridge.log("✗ Failed: " + classe.getSimpleName());
                    XposedBridge.log(e);
                    var error = new ErrorItem();
                    error.setPluginName(classe.getSimpleName());
                    error.setTiktokVersion(versionTkk);
                    error.setModuleVersion(BuildConfig.VERSION_NAME);
                    error.setMessage(e.getMessage());
                    error.setError(Arrays.toString(Arrays.stream(e.getStackTrace()).filter(s -> !s.getClassName().startsWith("android") && !s.getClassName().startsWith("com.android")).map(StackTraceElement::toString).toArray()));
                    list.add(error);
                }
                var timemillis2 = System.currentTimeMillis() - timemillis;
                times.add("* Loaded Plugin " + classe.getSimpleName() + " in " + timemillis2 + "ms");
            }, executorService);
        }
        
        executorService.shutdown();
        // Wait up to 15 seconds for all plugins to load
        executorService.awaitTermination(15, TimeUnit.SECONDS);
        
        if (DebugFeature.DEBUG) {
            for (var time : times) {
                if (time != null)
                    XposedBridge.log(time);
            }
        }
    }

    @Getter
    @Setter
    private static class ErrorItem {
        private String pluginName;
        private String tiktokVersion;
        private String error;
        private String moduleVersion;
        private String message;

        @NonNull
        @Override
        public String toString() {
            return "pluginName='" + getPluginName() + '\'' +
                    "\nmoduleVersion='" + getModuleVersion() + '\'' +
                    "\ntiktokVersion='" + getTiktokVersion() + '\'' +
                    "\nMessage=" + getMessage() +
                    "\nerror='" + getError() + '\'';
        }

    }
}
