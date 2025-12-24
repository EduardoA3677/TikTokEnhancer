package com.wmods.tkkenhancer.xposed.core;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wmods.tkkenhancer.views.dialog.BottomDialogTkk;
import com.wmods.tkkenhancer.xposed.bridge.TkeIIFace;
import com.wmods.tkkenhancer.xposed.bridge.client.BaseClient;
import com.wmods.tkkenhancer.xposed.bridge.client.BridgeClient;
import com.wmods.tkkenhancer.xposed.bridge.client.ProviderClient;
import com.wmods.tkkenhancer.xposed.core.components.AlertDialogTkk;
import com.wmods.tkkenhancer.xposed.core.components.FMessageTkk;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;
import com.wmods.tkkenhancer.xposed.core.devkit.UnobfuscatorCache;
import com.wmods.tkkenhancer.xposed.utils.ReflectionUtils;
import com.wmods.tkkenhancer.xposed.utils.ResId;
import com.wmods.tkkenhancer.xposed.utils.Utils;

import org.json.JSONObject;
import org.luckypray.dexkit.query.enums.StringMatchType;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class TkkCore {

    static final HashSet<ActivityChangeState> listenerAcitivity = new HashSet<>();
    @SuppressLint("StaticFieldLeak")
    static Activity mCurrentActivity;
    static LinkedHashSet<Activity> activities = new LinkedHashSet<>();
    private static Class<?> mGenJidClass;
    private static Method mGenJidMethod;
    private static Class bottomDialog;
    private static Field convChatField;
    private static Field chatJidField;
    private static SharedPreferences privPrefs;
    private static Object mStartUpConfig;
    private static Object mActionUser;
    private static SQLiteDatabase mWaDatabase;
    public static BaseClient client;
    private static Object mCachedMessageStore;
    private static Class<?> mSettingsNotificationsClass;
    private static Method convertLidToJid;

    private static Object mWaJidMapRepository;
    private static Method convertJidToLid;
    private static Class actionUser;
    private static Method cachedMessageStoreKey;


    public static void Initialize(ClassLoader loader, XSharedPreferences pref) throws Exception {
        // Initialize SharedPreferences for storing module settings
        try {
            privPrefs = Utils.getApplication().getSharedPreferences("TkkGlobal", Context.MODE_PRIVATE);
            if (privPrefs == null) {
                XposedBridge.log("Warning: Failed to create SharedPreferences, will retry on first access");
            } else {
                XposedBridge.log("privPrefs initialized successfully");
            }
        } catch (Exception e) {
            XposedBridge.log("Error initializing privPrefs: " + e.getMessage());
            XposedBridge.log(e);
            // privPrefs will remain null and will be checked in each method that uses it
        }

        // TikTok-specific initialization
        // Note: Most of the original legacy initialization has been removed
        // as it's not applicable to TikTok
        
        XposedBridge.log("TkkCore initialized for TikTok");

        // Initialize bridge if not in lite mode
        if (!pref.getBoolean("lite_mode", false)) {
            try {
                initBridge(Utils.getApplication());
            } catch (Exception e) {
                XposedBridge.log("Failed to init bridge: " + e.getMessage());
                // Non-fatal, continue without bridge
            }
        }
    }

    @Deprecated
    public static Object getPhoneJidFromUserJid(Object lid) {
        // Legacy method - no longer applicable to TikTok
        return lid;
    }

    @Deprecated
    public static Object getUserJidFromPhoneJid(Object userJid) {
        // Legacy method - no longer applicable to TikTok
        return userJid;
    }

    public static void initBridge(Context context) throws Exception {
        var prefsCacheHooks = UnobfuscatorCache.getInstance().sPrefsCacheHooks;
        int preferredOrder = prefsCacheHooks.getInt("preferredOrder", 1); // 0 for ProviderClient first, 1 for BridgeClient first

        boolean connected = false;
        if (preferredOrder == 0) {
            if (tryConnectBridge(new ProviderClient(context))) {
                connected = true;
            } else if (tryConnectBridge(new BridgeClient(context))) {
                connected = true;
                preferredOrder = 1; // Update preference to BridgeClient first
            }
        } else {
            if (tryConnectBridge(new BridgeClient(context))) {
                connected = true;
            } else if (tryConnectBridge(new ProviderClient(context))) {
                connected = true;
                preferredOrder = 0; // Update preference to ProviderClient first
            }
        }

        if (!connected) {
            throw new Exception(context.getString(ResId.string.bridge_error));
        }

        // Update the preferred order if it changed
        prefsCacheHooks.edit().putInt("preferredOrder", preferredOrder).apply();
    }


    private static boolean tryConnectBridge(BaseClient baseClient) throws Exception {
        try {
            XposedBridge.log("Trying to connect to " + baseClient.getClass().getSimpleName());
            client = baseClient;
            CompletableFuture<Boolean> canLoadFuture = baseClient.connect();
            Boolean canLoad = canLoadFuture.get();
            if (!canLoad) throw new Exception();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /* ===== LEGACY METHODS - NOT APPLICABLE TO TIKTOK =====
     * The following methods are commented out as they are legacy methods
     * from the original codebase and not applicable to TikTok's architecture
     * 
     * Stub implementations provided to prevent compilation errors in legacy code
     */
     
    @Deprecated
    public static void sendMessage(String number, String message) {
        XposedBridge.log("sendMessage() is not implemented for TikTok");
    }

    @Deprecated
    public static void sendReaction(String s, Object objMessage) {
        XposedBridge.log("sendReaction() is not implemented for TikTok");
    }

    @Deprecated
    public static Object getActionUser() {
        XposedBridge.log("getActionUser() is not implemented for TikTok");
        return null;
    }

    @Deprecated
    public static void loadWADatabase() {
        // No-op for TikTok
    }

    @Deprecated
    public static String getContactName(Object userJid) {
        return "";
    }

    @Deprecated
    public static Object getFMessageFromKey(Object messageKey) {
        return null;
    }

    @Deprecated
    public static Object createUserJid(String rawjid) {
        return null;
    }

    @Deprecated
    public static Object getCurrentUserJid() {
        return null;
    }

    @Deprecated
    public static String stripJID(String str) {
        return str;
    }

    @Deprecated
    public static Drawable getContactPhotoDrawable(String jid) {
        return null;
    }

    @Deprecated
    public static File getContactPhotoFile(String jid) {
        return null;
    }

    @Deprecated
    public static String getMyName() {
        return "TikTok User";
    }

    @Deprecated
    public static SharedPreferences getMainPrefs() {
        return privPrefs;
    }

    @Deprecated
    public static String getMyBio() {
        return "";
    }

    @Deprecated
    public static Drawable getMyPhoto() {
        return null;
    }

    @Deprecated
    public static Object createBottomDialog(Context context) {
        return null;
    }

    @Deprecated
    @Nullable
    public static Activity getCurrentConversation() {
        return mCurrentActivity;
    }


    public static Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    public synchronized static Class getHomeActivityClass(@NonNull ClassLoader loader) {
        // TODO: Update with TikTok main activity class
        // TikTok main activity is likely com.ss.android.ugc.aweme.main.MainActivity or similar
        Class tiktokHomeClass = XposedHelpers.findClassIfExists("com.ss.android.ugc.aweme.main.MainActivity", loader);
        if (tiktokHomeClass != null) {
            return tiktokHomeClass;
        }
        // Fallback - try to find any activity as a placeholder
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getTabsPagerClass(@NonNull ClassLoader loader) {
        // TODO: Update with TikTok equivalent if applicable
        // Placeholder for legacy code compatibility
        return XposedHelpers.findClassIfExists("android.view.View", loader);
    }

    public synchronized static Class getViewOnceViewerActivityClass(@NonNull ClassLoader loader) {
        // TODO: Update with TikTok video viewer if applicable  
        // Placeholder for legacy code compatibility
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getAboutActivityClass(@NonNull ClassLoader loader) {
        // TODO: Update with TikTok settings/about if applicable
        // Placeholder for legacy code compatibility
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getSettingsNotificationsActivityClass(@NonNull ClassLoader loader) {
        if (mSettingsNotificationsClass != null)
            return mSettingsNotificationsClass;

        // TODO: Update with TikTok settings if applicable
        // Placeholder for legacy code compatibility
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getDataUsageActivityClass(@NonNull ClassLoader loader) {
        // TODO: Update with TikTok data usage settings if applicable
        // Placeholder for legacy code compatibility
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getTextStatusComposerFragmentClass(@NonNull ClassLoader loader) throws Exception {
        // TODO: Update with TikTok video composer if applicable
        // Placeholder for legacy code compatibility
        return XposedHelpers.findClassIfExists("android.app.Fragment", loader);
    }

    public synchronized static Class getVoipManagerClass(@NonNull ClassLoader loader) throws Exception {
        // TODO: Update with TikTok video call classes if applicable
        // Placeholder for legacy code compatibility
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getVoipCallInfoClass(@NonNull ClassLoader loader) throws Exception {
        // TODO: Update with TikTok call info if applicable
        // Placeholder for legacy code compatibility
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

//    public static Activity getActivityBySimpleName(String name) {
//        for (var activity : activities) {
//            if (activity.getClass().getSimpleName().equals(name)) {
//                return activity;
//            }
//        }
//        return null;
//    }


    public static int getDefaultTheme() {
        if (mStartUpConfig != null) {
            var result = ReflectionUtils.findMethodUsingFilterIfExists(mStartUpConfig.getClass(), (method) -> method.getParameterCount() == 0 && method.getReturnType() == int.class);
            if (result != null) {
                var value = ReflectionUtils.callMethod(result, mStartUpConfig);
                if (value != null) return (int) value;
            }
        }
        var startup_prefs = Utils.getApplication().getSharedPreferences("startup_prefs", Context.MODE_PRIVATE);
        return startup_prefs.getInt("night_mode", 0);
    }

    /* ===== LEGACY CONTACT/JID METHODS - NOT APPLICABLE TO TIKTOK =====
     * These methods handle legacy contact and JID management
     * TikTok uses a different user identification system
     * Stub implementations provided below
     */

    /* ===== LEGACY UI METHODS - NOT APPLICABLE TO TIKTOK =====
     * These methods handle legacy UI components
     * Stub implementations provided above
     */

    private static synchronized void ensurePrivPrefsInitialized() {
        if (privPrefs == null) {
            try {
                privPrefs = Utils.getApplication().getSharedPreferences("TkkGlobal", Context.MODE_PRIVATE);
                if (privPrefs != null) {
                    XposedBridge.log("privPrefs initialized lazily");
                }
            } catch (Exception e) {
                XposedBridge.log("Failed to initialize privPrefs lazily: " + e.getMessage());
            }
        }
    }

    public static SharedPreferences getPrivPrefs() {
        ensurePrivPrefsInitialized();
        return privPrefs;
    }

    @SuppressLint("ApplySharedPref")
    public static void setPrivString(String key, String value) {
        ensurePrivPrefsInitialized();
        if (privPrefs == null) {
            XposedBridge.log("Warning: privPrefs is null in setPrivString, skipping operation");
            return;
        }
        privPrefs.edit().putString(key, value).commit();
    }

    public static String getPrivString(String key, String defaultValue) {
        ensurePrivPrefsInitialized();
        if (privPrefs == null) {
            XposedBridge.log("Warning: privPrefs is null in getPrivString, returning default value");
            return defaultValue;
        }
        return privPrefs.getString(key, defaultValue);
    }

    public static JSONObject getPrivJSON(String key, JSONObject defaultValue) {
        ensurePrivPrefsInitialized();
        if (privPrefs == null) {
            XposedBridge.log("Warning: privPrefs is null in getPrivJSON, returning default value");
            return defaultValue;
        }
        var jsonStr = privPrefs.getString(key, null);
        if (jsonStr == null) return defaultValue;
        try {
            return new JSONObject(jsonStr);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @SuppressLint("ApplySharedPref")
    public static void setPrivJSON(String key, JSONObject value) {
        ensurePrivPrefsInitialized();
        if (privPrefs == null) {
            XposedBridge.log("Warning: privPrefs is null in setPrivJSON, skipping operation");
            return;
        }
        privPrefs.edit().putString(key, value == null ? null : value.toString()).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void removePrivKey(String s) {
        ensurePrivPrefsInitialized();
        if (privPrefs == null) {
            XposedBridge.log("Warning: privPrefs is null in removePrivKey, skipping operation");
            return;
        }
        if (s != null && privPrefs.contains(s))
            privPrefs.edit().remove(s).commit();
    }


    @SuppressLint("ApplySharedPref")
    public static void setPrivBoolean(String key, boolean value) {
        ensurePrivPrefsInitialized();
        if (privPrefs == null) {
            XposedBridge.log("Warning: privPrefs is null in setPrivBoolean, skipping operation");
            return;
        }
        privPrefs.edit().putBoolean(key, value).commit();
    }

    public static boolean getPrivBoolean(String key, boolean defaultValue) {
        ensurePrivPrefsInitialized();
        if (privPrefs == null) {
            XposedBridge.log("Warning: privPrefs is null in getPrivBoolean, returning default value");
            return defaultValue;
        }
        return privPrefs.getBoolean(key, defaultValue);
    }

    public static void addListenerActivity(ActivityChangeState listener) {
        listenerAcitivity.add(listener);
    }

    public static TkeIIFace getClientBridge() throws Exception {
        if (client == null || client.getService() == null || !client.getService().asBinder().isBinderAlive() || !client.getService().asBinder().pingBinder()) {
            TkkCore.getCurrentActivity().runOnUiThread(() -> {
                var dialog = new AlertDialogTkk(TkkCore.getCurrentActivity());
                dialog.setTitle("Bridge Error");
                dialog.setMessage("The Connection with WaEnhancer was lost, it is necessary to reconnect with WaEnhancer in order to reestablish the connection.");
                dialog.setPositiveButton("reconnect", (dialog1, which) -> {
                    client.tryReconnect();
                    dialog.dismiss();
                });
                dialog.setNegativeButton("cancel", null);
                dialog.show();
            });
            throw new Exception("Failed connect to Bridge");
        }
        return client.getService();
    }


    public interface ActivityChangeState {

        void onChange(Activity activity, ChangeType type);

        enum ChangeType {
            CREATED, STARTED, ENDED, RESUMED, PAUSED
        }
    }


}
