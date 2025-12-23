package com.wmods.tkkenhancer.xposed.core.components;

import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;
import com.wmods.tkkenhancer.xposed.utils.ReflectionUtils;

import org.luckypray.dexkit.query.enums.StringMatchType;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedBridge;

public class TkContactTkk {

    public static Class<?> TYPE;
    private static Field fieldContactData;
    private static Field fieldUserJid;
    private static Field fieldPhoneUserJid;
    private final Object mInstance;


    public TkContactTkk(Object object) {
        if (TYPE == null) throw new RuntimeException("TkContactTkk not initialized");
        if (object == null) throw new RuntimeException("object is null");
        this.mInstance = object;
    }

    public static void initialize(ClassLoader classLoader) {
        try {
            TYPE = Unobfuscator.loadWaContactClass(classLoader);
            var classPhoneUserJid = Unobfuscator.findFirstClassUsingName(classLoader, StringMatchType.EndsWith, "jid.PhoneUserJid");
            var classJid = Unobfuscator.findFirstClassUsingName(classLoader, StringMatchType.EndsWith, "jid.Jid");

            var phoneUserJid = ReflectionUtils.getFieldByExtendType(TYPE, classPhoneUserJid);
            if (phoneUserJid == null) {
                var contactDataClass = Unobfuscator.loadWaContactData(classLoader);
                fieldContactData = ReflectionUtils.getFieldByType(TYPE, contactDataClass);
                fieldUserJid = ReflectionUtils.getFieldByExtendType(contactDataClass, classJid);
                fieldPhoneUserJid = ReflectionUtils.getFieldByExtendType(contactDataClass, classPhoneUserJid);
            } else {
                fieldUserJid = ReflectionUtils.getFieldByExtendType(TYPE, classJid);
                fieldPhoneUserJid = ReflectionUtils.getFieldByExtendType(TYPE, classPhoneUserJid);
            }
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    public Object getObject() {
        return mInstance;
    }


    public FMessageTkk.UserJid getUserJid() {
        try {
            if (fieldContactData != null) {
                var coreData = fieldContactData.get(mInstance);
                return new FMessageTkk.UserJid(fieldUserJid.get(coreData), fieldPhoneUserJid.get(coreData));
            }
            return new FMessageTkk.UserJid(fieldUserJid.get(mInstance), fieldPhoneUserJid.get(mInstance));
        } catch (Exception e) {
            XposedBridge.log(e);
        }
        return null;
    }

}
