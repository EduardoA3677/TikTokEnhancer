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
import com.wmods.tkkenhancer.xposed.features.customization.BubbleColors;
import com.wmods.tkkenhancer.xposed.features.customization.CustomThemeV2;
import com.wmods.tkkenhancer.xposed.features.customization.CustomTime;
import com.wmods.tkkenhancer.xposed.features.customization.CustomToolbar;
import com.wmods.tkkenhancer.xposed.features.customization.CustomView;
import com.wmods.tkkenhancer.xposed.features.customization.FilterGroups;
import com.wmods.tkkenhancer.xposed.features.customization.HideSeenView;
import com.wmods.tkkenhancer.xposed.features.customization.HideTabs;
import com.wmods.tkkenhancer.xposed.features.customization.IGStatus;
import com.wmods.tkkenhancer.xposed.features.customization.SeparateGroup;
import com.wmods.tkkenhancer.xposed.features.customization.ShowOnline;
import com.wmods.tkkenhancer.xposed.features.general.AntiRevoke;
import com.wmods.tkkenhancer.xposed.features.general.CallType;
import com.wmods.tkkenhancer.xposed.features.general.ChatLimit;
import com.wmods.tkkenhancer.xposed.features.general.DeleteStatus;
import com.wmods.tkkenhancer.xposed.features.general.LiteMode;
import com.wmods.tkkenhancer.xposed.features.general.MenuStatus;
import com.wmods.tkkenhancer.xposed.features.general.NewChat;
import com.wmods.tkkenhancer.xposed.features.general.Others;
import com.wmods.tkkenhancer.xposed.features.general.PinnedLimit;
import com.wmods.tkkenhancer.xposed.features.general.SeenTick;
import com.wmods.tkkenhancer.xposed.features.general.ShareLimit;
import com.wmods.tkkenhancer.xposed.features.general.ShowEditMessage;
import com.wmods.tkkenhancer.xposed.features.general.Tasker;
import com.wmods.tkkenhancer.xposed.features.media.DownloadProfile;
import com.wmods.tkkenhancer.xposed.features.media.DownloadViewOnce;
import com.wmods.tkkenhancer.xposed.features.media.MediaPreview;
import com.wmods.tkkenhancer.xposed.features.media.MediaQuality;
import com.wmods.tkkenhancer.xposed.features.media.StatusDownload;
import com.wmods.tkkenhancer.xposed.features.others.ActivityController;
import com.wmods.tkkenhancer.xposed.features.others.AudioTranscript;
import com.wmods.tkkenhancer.xposed.features.others.Channels;
import com.wmods.tkkenhancer.xposed.features.others.ChatFilters;
import com.wmods.tkkenhancer.xposed.features.others.CopyStatus;
import com.wmods.tkkenhancer.xposed.features.others.DebugFeature;
import com.wmods.tkkenhancer.xposed.features.others.GoogleTranslate;
import com.wmods.tkkenhancer.xposed.features.others.GroupAdmin;
import com.wmods.tkkenhancer.xposed.features.others.MenuHome;
import com.wmods.tkkenhancer.xposed.features.others.Stickers;
import com.wmods.tkkenhancer.xposed.features.others.TextStatusComposer;
import com.wmods.tkkenhancer.xposed.features.others.ToastViewer;
import com.wmods.tkkenhancer.xposed.features.privacy.AntiWa;
import com.wmods.tkkenhancer.xposed.features.privacy.CallPrivacy;
import com.wmods.tkkenhancer.xposed.features.privacy.CustomPrivacy;
import com.wmods.tkkenhancer.xposed.features.privacy.DndMode;
import com.wmods.tkkenhancer.xposed.features.privacy.FreezeLastSeen;
import com.wmods.tkkenhancer.xposed.features.privacy.HideChat;
import com.wmods.tkkenhancer.xposed.features.privacy.HideReceipt;
import com.wmods.tkkenhancer.xposed.features.privacy.HideSeen;
import com.wmods.tkkenhancer.xposed.features.privacy.TagMessage;
import com.wmods.tkkenhancer.xposed.features.privacy.TypingPrivacy;
import com.wmods.tkkenhancer.xposed.features.privacy.ViewOnce;
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

    public final static String PACKAGE_WPP = "com.zhiliaoapp.musically";
    public final static String PACKAGE_BUSINESS = "com.zhiliaoapp.musically";

    private static final ArrayList<ErrorItem> list = new ArrayList<>();
    private static List<String> supportedVersions;
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
                supportedVersions = Arrays.asList(mApp.getResources().getStringArray(ResId.array.supported_versions_tkk));
                mApp.registerActivityLifecycleCallbacks(new TkCallback());
                registerReceivers();
                try {
                    var timemillis = System.currentTimeMillis();
                    SharedPreferencesWrapper.hookInit(mApp.getClassLoader());
                    UnobfuscatorCache.init(mApp);
                    ReflectionUtils.initCache(mApp);
                    boolean isSupported = supportedVersions.stream().anyMatch(s -> packageInfo.versionName.startsWith(s.replace(".xx", "")));
                    if (!isSupported) {
                        disableExpirationVersion(mApp.getClassLoader());
                        if (!pref.getBoolean("bypass_version_check", false)) {
                            String sb = "Unsupported version: " +
                                    packageInfo.versionName +
                                    "\n" +
                                    "Only the function of ignoring the expiration of the TikTok version has been applied!";
                            throw new Exception(sb);
                        }
                    }
                    initComponents(loader, pref);
                    plugins(loader, pref, packageInfo.versionName);
                    sendEnabledBroadcast(mApp);
//                    XposedHelpers.setStaticIntField(XposedHelpers.findClass("com.whatsapp.util.Log", loader), "level", 5);
                    var timemillis2 = System.currentTimeMillis() - timemillis;
                    XposedBridge.log("Loaded Hooks in " + timemillis2 + "ms");
                } catch (Throwable e) {
                    XposedBridge.log(e);
                    var error = new ErrorItem();
                    error.setPluginName("MainFeatures[Critical]");
                    error.setWhatsAppVersion(packageInfo.versionName);
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
        BroadcastReceiver wppReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendEnabledBroadcast(context);
            }
        };
        ContextCompat.registerReceiver(mApp, wppReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".CHECK_TIKTOK"), ContextCompat.RECEIVER_EXPORTED);

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
            Intent wppIntent = new Intent(BuildConfig.APPLICATION_ID + ".RECEIVER_TIKTOK");
            wppIntent.putExtra("VERSION", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
            wppIntent.putExtra("PKG", context.getPackageName());
            wppIntent.setPackage(BuildConfig.APPLICATION_ID);
            context.sendBroadcast(wppIntent);
        } catch (Exception ignored) {
        }
    }

    private static void plugins(@NonNull ClassLoader loader, @NonNull XSharedPreferences pref, @NonNull String versionWpp) throws Exception {

        // TikTok-specific features - Corrected based on deep smali analysis
        // All class names and methods verified against actual TikTok smali code
        var classes = new Class<?>[]{
                DebugFeature.class,
                com.wmods.tkkenhancer.xposed.features.media.VideoDownload.class,      // ✅ Verified: getDownloadNoWatermarkAddr()
                com.wmods.tkkenhancer.xposed.features.media.AdBlocker.class,          // ✅ Verified: isAd()
                com.wmods.tkkenhancer.xposed.features.media.AutoPlayControl.class,    // Player control
                com.wmods.tkkenhancer.xposed.features.media.VideoQuality.class,       // ✅ Corrected: RateSettingCombineModel, GearSet
                com.wmods.tkkenhancer.xposed.features.media.StoryDownload.class,      // ✅ Verified: Story model
                com.wmods.tkkenhancer.xposed.features.media.DownloadServiceHook.class, // 🆕 NEW: DownloadAwemeVideoServiceImpl.LIZ()
                com.wmods.tkkenhancer.xposed.features.privacy.PrivacyEnhancer.class   // ✅ Enhanced: + FirebaseAnalytics
                // CustomThemeV2.class,  // Commented out - needs TikTok-specific implementation
                // LiteMode.class,  // Commented out - needs TikTok-specific implementation
                // Others.class,  // Commented out - needs TikTok-specific implementation
                // ToastViewer.class  // Commented out - needs TikTok-specific implementation
        };
        XposedBridge.log("Loading TikTok Plugins");
        var executorService = Executors.newWorkStealingPool(Math.min(Runtime.getRuntime().availableProcessors(), 4));
        var times = new ArrayList<String>();
        for (var classe : classes) {
            CompletableFuture.runAsync(() -> {
                var timemillis = System.currentTimeMillis();
                try {
                    var constructor = classe.getConstructor(ClassLoader.class, XSharedPreferences.class);
                    var plugin = (Feature) constructor.newInstance(loader, pref);
                    plugin.doHook();
                } catch (Throwable e) {
                    XposedBridge.log(e);
                    var error = new ErrorItem();
                    error.setPluginName(classe.getSimpleName());
                    error.setWhatsAppVersion(versionWpp);
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
        private String whatsAppVersion;
        private String error;
        private String moduleVersion;
        private String message;

        @NonNull
        @Override
        public String toString() {
            return "pluginName='" + getPluginName() + '\'' +
                    "\nmoduleVersion='" + getModuleVersion() + '\'' +
                    "\nwhatsAppVersion='" + getWhatsAppVersion() + '\'' +
                    "\nMessage=" + getMessage() +
                    "\nerror='" + getError() + '\'';
        }

    }
}
