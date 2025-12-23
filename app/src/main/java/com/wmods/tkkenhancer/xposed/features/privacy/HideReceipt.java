package com.wmods.tkkenhancer.xposed.features.privacy;

import static com.wmods.tkkenhancer.xposed.features.privacy.HideSeen.getKeyMessage;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.TkkCore;
import com.wmods.tkkenhancer.xposed.core.components.FMessageTkk;
import com.wmods.tkkenhancer.xposed.core.db.MessageHistory;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;
import com.wmods.tkkenhancer.xposed.features.customization.HideSeenView;
import com.wmods.tkkenhancer.xposed.utils.ReflectionUtils;

import org.luckypray.dexkit.query.enums.StringMatchType;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class HideReceipt extends Feature {
    public HideReceipt(ClassLoader loader, XSharedPreferences preferences) {
        super(loader, preferences);
    }

    @Override
    public void doHook() throws Exception {
        var hideReceipt = prefs.getBoolean("hidereceipt", false);
        var ghostmode = TkkCore.getPrivBoolean("ghostmode", false);
        var hideread = prefs.getBoolean("hideread", false);

        logDebug("hideReceipt: " + hideReceipt + ", ghostmode: " + ghostmode + ", hideread: " + hideread);

        var method = Unobfuscator.loadReceiptMethod(classLoader);
        logDebug("hook method:" + Unobfuscator.getMethodDescriptor(method));

        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                var userJidObject = ReflectionUtils.getArg(param.args, Unobfuscator.findFirstClassUsingName(classLoader, StringMatchType.EndsWith, "jid.Jid"), 0);
                if (userJidObject == null) return;
                var strings = ReflectionUtils.findClassesOfType(((Method) param.method).getParameterTypes(), String.class);
                FMessageTkk.Key keyMessage = getKeyMessage(param, userJidObject, strings);
                if (keyMessage == null)
                    return;
                var currentUserJid = new FMessageTkk.UserJid(userJidObject);
                var fmessage = keyMessage.getFMessage();
                if (fmessage != null) {
                    if (MessageHistory.getInstance().getHideSeenMessage(fmessage.getKey().remoteJid.getPhoneRawString(), fmessage.getKey().messageID, fmessage.isViewOnce() ? MessageHistory.MessageType.VIEW_ONCE_TYPE : MessageHistory.MessageType.MESSAGE_TYPE) != null) {
                        return;
                    }
                }
                var privacy = CustomPrivacy.getJSON(currentUserJid.getPhoneNumber());
                var customHideReceipt = privacy.optBoolean("HideReceipt", hideReceipt);
                var msgTypeIdx = strings.get(strings.size() - 1).first;
                var customHideRead = privacy.optBoolean("HideSeen", hideread);
                if (param.args[msgTypeIdx] != "sender" && (customHideReceipt || ghostmode)) {
                    if (TkkCore.getCurrentConversation() == null || customHideRead)
                        param.args[msgTypeIdx] = "inactive";
                }
                if (param.args[msgTypeIdx] == "inactive") {
                    MessageHistory.getInstance().insertHideSeenMessage(currentUserJid.getPhoneRawString(), fmessage.getKey().messageID, fmessage.isViewOnce() ? MessageHistory.MessageType.VIEW_ONCE_TYPE : MessageHistory.MessageType.MESSAGE_TYPE, false);
                    HideSeenView.updateAllBubbleViews();
                }
            }
        });
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Hide Receipt";
    }
}
