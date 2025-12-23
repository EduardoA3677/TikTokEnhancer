package com.wmods.wppenhacer.xposed.core;

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

import com.wmods.wppenhacer.views.dialog.BottomDialogWpp;
import com.wmods.wppenhacer.xposed.bridge.WaeIIFace;
import com.wmods.wppenhacer.xposed.bridge.client.BaseClient;
import com.wmods.wppenhacer.xposed.bridge.client.BridgeClient;
import com.wmods.wppenhacer.xposed.bridge.client.ProviderClient;
import com.wmods.wppenhacer.xposed.core.components.AlertDialogWpp;
import com.wmods.wppenhacer.xposed.core.components.FMessageWpp;
import com.wmods.wppenhacer.xposed.core.devkit.Unobfuscator;
import com.wmods.wppenhacer.xposed.core.devkit.UnobfuscatorCache;
import com.wmods.wppenhacer.xposed.utils.ReflectionUtils;
import com.wmods.wppenhacer.xposed.utils.ResId;
import com.wmods.wppenhacer.xposed.utils.Utils;

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

public class WppCore {

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
        privPrefs = Utils.getApplication().getSharedPreferences("WaGlobal", Context.MODE_PRIVATE);
        // init UserJID
        var mSendReadClass = Unobfuscator.findFirstClassUsingName(loader, StringMatchType.EndsWith, "SendReadReceiptJob");
        var subClass = ReflectionUtils.findConstructorUsingFilter(mSendReadClass, (constructor) -> constructor.getParameterCount() == 8).getParameterTypes()[0];
        mGenJidClass = ReflectionUtils.findFieldUsingFilter(subClass, (field) -> Modifier.isStatic(field.getModifiers())).getType();
        mGenJidMethod = ReflectionUtils.findMethodUsingFilter(mGenJidClass, (method) -> method.getParameterCount() == 1 && !Modifier.isStatic(method.getModifiers()));
        // Bottom Dialog
        bottomDialog = Unobfuscator.loadDialogViewClass(loader);

        convChatField = Unobfuscator.loadAntiRevokeConvChatField(loader);
        chatJidField = Unobfuscator.loadAntiRevokeChatJidField(loader);

        // Settings notifications activity (required for ActivityController.EXPORTED_ACTIVITY)
        mSettingsNotificationsClass = getSettingsNotificationsActivityClass(loader);

        // StartUpPrefs
        var startPrefsConfig = Unobfuscator.loadStartPrefsConfig(loader);
        XposedBridge.hookMethod(startPrefsConfig, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                mStartUpConfig = param.thisObject;
            }
        });

        // ActionUser
        actionUser = Unobfuscator.loadActionUser(loader);
        XposedBridge.log("ActionUser: " + actionUser.getName());
        XposedBridge.hookAllConstructors(actionUser, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mActionUser = param.thisObject;
            }
        });

        // CachedMessageStore
        cachedMessageStoreKey = Unobfuscator.loadCachedMessageStoreKey(loader);
        XposedBridge.hookAllConstructors(cachedMessageStoreKey.getDeclaringClass(), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mCachedMessageStore = param.thisObject;
            }
        });

        // WaJidMap
        convertLidToJid = Unobfuscator.loadConvertLidToJid(loader);
        convertJidToLid = Unobfuscator.loadConvertJidToLid(loader);
        XposedBridge.hookAllConstructors(convertLidToJid.getDeclaringClass(), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mWaJidMapRepository = param.thisObject;
            }
        });

        // Load wa database
        loadWADatabase();

        if (!pref.getBoolean("lite_mode", false)) {
            initBridge(Utils.getApplication());
        }

    }

    public static Object getPhoneJidFromUserJid(Object lid) {
        if (lid == null) return null;
        try {
            var rawString = (String) XposedHelpers.callMethod(lid, "getRawString");
            if (rawString == null || !rawString.contains("@lid")) return lid;
            rawString = rawString.replaceFirst("\\.[\\d:]+@", "@");
            var newUser = WppCore.createUserJid(rawString);
            var result = ReflectionUtils.callMethod(convertLidToJid, mWaJidMapRepository, newUser);
            return result == null ? lid : result;
        } catch (Exception e) {
            XposedBridge.log(e);
        }
        return lid;
    }

    public static Object getUserJidFromPhoneJid(Object userJid) {
        if (userJid == null) return null;
        try {
            var rawString = (String) XposedHelpers.callMethod(userJid, "getRawString");
            if (rawString == null || rawString.contains("@lid")) return userJid;
            rawString = rawString.replaceFirst("\\.[\\d:]+@", "@");
            var newUser = WppCore.createUserJid(rawString);
            var result = ReflectionUtils.callMethod(convertJidToLid, mWaJidMapRepository, newUser);
            return result == null ? userJid : result;
        } catch (Exception e) {
            XposedBridge.log(e);
        }
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

    public static void sendMessage(String number, String message) {
        // TODO: Implement TikTok message sending if applicable
        // TikTok may not have direct messaging or uses different protocols
        // Commented out WhatsApp-specific JID code
        Utils.showToast("Message sending not implemented for TikTok", Toast.LENGTH_SHORT);
    }

    public static void sendReaction(String s, Object objMessage) {
        try {
            var senderMethod = ReflectionUtils.findMethodUsingFilter(actionUser, (method) -> method.getParameterCount() == 3 && Arrays.equals(method.getParameterTypes(), new Class[]{FMessageWpp.TYPE, String.class, boolean.class}));
            senderMethod.invoke(getActionUser(), objMessage, s, !TextUtils.isEmpty(s));
        } catch (Exception e) {
            Utils.showToast("Error in sending reaction:" + e.getMessage(), Toast.LENGTH_SHORT);
            XposedBridge.log(e);
        }
    }

    public static Object getActionUser() {
        try {
            if (mActionUser == null) {
                mActionUser = actionUser.getConstructors()[0].newInstance();
            }
        } catch (Exception e) {
            XposedBridge.log(e);
        }
        return mActionUser;
    }


    public static void loadWADatabase() {
        if (mWaDatabase != null) return;
        var dataDir = Utils.getApplication().getFilesDir().getParentFile();
        var database = new File(dataDir, "databases/wa.db");
        if (database.exists()) {
            mWaDatabase = SQLiteDatabase.openDatabase(database.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        }
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
        // Commented out WhatsApp-specific code
        return XposedHelpers.findClassIfExists("android.view.View", loader);
    }

    public synchronized static Class getViewOnceViewerActivityClass(@NonNull ClassLoader loader) {
        // TODO: Update with TikTok video viewer if applicable  
        // Commented out WhatsApp-specific code
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getAboutActivityClass(@NonNull ClassLoader loader) {
        // TODO: Update with TikTok settings/about if applicable
        // Commented out WhatsApp-specific code
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getSettingsNotificationsActivityClass(@NonNull ClassLoader loader) {
        if (mSettingsNotificationsClass != null)
            return mSettingsNotificationsClass;

        // TODO: Update with TikTok settings if applicable
        // Commented out WhatsApp-specific code
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getDataUsageActivityClass(@NonNull ClassLoader loader) {
        // TODO: Update with TikTok data usage settings if applicable
        // Commented out WhatsApp-specific code
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getTextStatusComposerFragmentClass(@NonNull ClassLoader loader) throws Exception {
        // TODO: Update with TikTok video composer if applicable
        // Commented out WhatsApp-specific status composer
        return XposedHelpers.findClassIfExists("android.app.Fragment", loader);
    }
            if ((result = XposedHelpers.findClassIfExists(clazz, loader)) != null)
                return result;
        }
        // Commented out WhatsApp-specific code - TikTok doesn't have status composer
        return XposedHelpers.findClassIfExists("android.app.Fragment", loader);
    }

    public synchronized static Class getVoipManagerClass(@NonNull ClassLoader loader) throws Exception {
        // TODO: Update with TikTok video call classes if applicable
        // Commented out WhatsApp-specific voip code
        return XposedHelpers.findClassIfExists("android.app.Activity", loader);
    }

    public synchronized static Class getVoipCallInfoClass(@NonNull ClassLoader loader) throws Exception {
        // TODO: Update with TikTok call info if applicable
        // Commented out WhatsApp-specific call info code
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

    @NonNull
    public static String getContactName(FMessageWpp.UserJid userJid) {
        loadWADatabase();
        if (mWaDatabase == null || userJid.isNull()) return "";
        String name = getSContactName(userJid, false);
        if (!TextUtils.isEmpty(name)) return name;
        return getWppContactName(userJid);
    }

    @NonNull
    public static String getSContactName(FMessageWpp.UserJid userJid, boolean saveOnly) {
        loadWADatabase();
        if (mWaDatabase == null || userJid == null) return "";
        String selection;
        if (saveOnly) {
            selection = "jid = ? AND raw_contact_id > 0";
        } else {
            selection = "jid = ?";
        }
        String name = null;
        var rawJid = userJid.getPhoneRawString();
        var cursor = mWaDatabase.query("wa_contacts", new String[]{"display_name"}, selection, new String[]{rawJid}, null, null, null);
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
            cursor.close();
        }
        return name == null ? "" : name;
    }

    @NonNull
    public static String getWppContactName(FMessageWpp.UserJid userJid) {
        loadWADatabase();
        if (mWaDatabase == null || userJid.isNull()) return "";
        String name = null;
        var rawJid = userJid.getPhoneRawString();
        var cursor2 = mWaDatabase.query("wa_vnames", new String[]{"verified_name"}, "jid = ?", new String[]{rawJid}, null, null, null);
        if (cursor2.moveToFirst()) {
            name = cursor2.getString(0);
            cursor2.close();
        }
        return name == null ? "" : name;
    }

    public static Object getFMessageFromKey(Object messageKey) {
        if (messageKey == null) return null;
        try {
            if (mCachedMessageStore == null) {
                XposedBridge.log("CachedMessageStore is null");
                return null;
            }
            return cachedMessageStoreKey.invoke(mCachedMessageStore, messageKey);
        } catch (Exception e) {
            XposedBridge.log(e);
            return null;
        }
    }


    @Nullable
    public static Object createUserJid(@Nullable String rawjid) {
        if (rawjid == null) return null;
        var genInstance = XposedHelpers.newInstance(mGenJidClass);
        try {
            return mGenJidMethod.invoke(genInstance, rawjid);
        } catch (Exception e) {
            XposedBridge.log(e);
        }
        return null;
    }

    @Nullable
    public static FMessageWpp.UserJid getCurrentUserJid() {
        try {
            var conversation = getCurrentConversation();
            if (conversation == null) return null;
            Object chatField;
            if (conversation.getClass().getSimpleName().equals("HomeActivity")) {
                // tablet mode found
                var convFragmentMethod = Unobfuscator.loadHomeConversationFragmentMethod(conversation.getClassLoader());
                var convFragment = convFragmentMethod.invoke(null, conversation);
                var convField = Unobfuscator.loadAntiRevokeConvFragmentField(conversation.getClassLoader());
                chatField = convField.get(convFragment);
            } else {
                chatField = convChatField.get(conversation);
            }
            var chatJidObj = chatJidField.get(chatField);
            return new FMessageWpp.UserJid(chatJidObj);
        } catch (Exception e) {
            XposedBridge.log(e);
            return null;
        }
    }

    public static String stripJID(String str) {
        try {
            if (str == null) return null;
            if (str.contains(".") && str.contains("@") && str.indexOf(".") < str.indexOf("@")) {
                return str.substring(0, str.indexOf("."));
            } else if (str.contains("@g.us") || str.contains("@s.whatsapp.net") || str.contains("@broadcast") || str.contains("@lid")) {
                return str.substring(0, str.indexOf("@"));
            }
            return str;
        } catch (Exception e) {
            XposedBridge.log(e);
            return str;
        }
    }

    public static Drawable getContactPhotoDrawable(String jid) {
        var file = getContactPhotoFile(jid);
        if (file == null) return null;
        return Drawable.createFromPath(file.getAbsolutePath());
    }

    public static File getContactPhotoFile(String jid) {
        String datafolder = Utils.getApplication().getCacheDir().getParent() + "/";
        File file = new File(datafolder + "/cache/" + "Profile Pictures" + "/" + stripJID(jid) + ".jpg");
        if (!file.exists())
            file = new File(datafolder + "files" + "/" + "Avatars" + "/" + jid + ".j");
        if (file.exists()) return file;
        return null;
    }

    public static String getMyName() {
        var startup_prefs = Utils.getApplication().getSharedPreferences("startup_prefs", Context.MODE_PRIVATE);
        return startup_prefs.getString("push_name", "WhatsApp");
    }

//    public static String getMyNumber() {
//        var mainPrefs = getMainPrefs();
//        return mainPrefs.getString("registration_jid", "");
//    }

    public static SharedPreferences getMainPrefs() {
        return Utils.getApplication().getSharedPreferences(Utils.getApplication().getPackageName() + "_preferences_light", Context.MODE_PRIVATE);
    }


    public static String getMyBio() {
        var mainPrefs = getMainPrefs();
        return mainPrefs.getString("my_current_status", "");
    }

    public static Drawable getMyPhoto() {
        String datafolder = Utils.getApplication().getCacheDir().getParent() + "/";
        File file = new File(datafolder + "files" + "/" + "me.jpg");
        if (file.exists()) return Drawable.createFromPath(file.getAbsolutePath());
        return null;
    }

    public static BottomDialogWpp createBottomDialog(Context context) {
        return new BottomDialogWpp((Dialog) XposedHelpers.newInstance(bottomDialog, context, 0));
    }

    @Nullable
    public static Activity getCurrentConversation() {
        // TODO: Update for TikTok video viewing/commenting activity
        // TikTok doesn't have a "Conversation" activity like WhatsApp
        // Commented out WhatsApp-specific code
        return mCurrentActivity;
    }

    public static SharedPreferences getPrivPrefs() {
        return privPrefs;
    }

    @SuppressLint("ApplySharedPref")
    public static void setPrivString(String key, String value) {
        privPrefs.edit().putString(key, value).commit();
    }

    public static String getPrivString(String key, String defaultValue) {
        return privPrefs.getString(key, defaultValue);
    }

    public static JSONObject getPrivJSON(String key, JSONObject defaultValue) {
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
        privPrefs.edit().putString(key, value == null ? null : value.toString()).commit();
    }

    @SuppressLint("ApplySharedPref")
    public static void removePrivKey(String s) {
        if (s != null && privPrefs.contains(s))
            privPrefs.edit().remove(s).commit();
    }


    @SuppressLint("ApplySharedPref")
    public static void setPrivBoolean(String key, boolean value) {
        privPrefs.edit().putBoolean(key, value).commit();
    }

    public static boolean getPrivBoolean(String key, boolean defaultValue) {
        return privPrefs.getBoolean(key, defaultValue);
    }

    public static void addListenerActivity(ActivityChangeState listener) {
        listenerAcitivity.add(listener);
    }

    public static WaeIIFace getClientBridge() throws Exception {
        if (client == null || client.getService() == null || !client.getService().asBinder().isBinderAlive() || !client.getService().asBinder().pingBinder()) {
            WppCore.getCurrentActivity().runOnUiThread(() -> {
                var dialog = new AlertDialogWpp(WppCore.getCurrentActivity());
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
