package com.wmods.tkkenhancer.xposed.features.general;

import static com.wmods.tkkenhancer.xposed.features.general.MenuStatus.menuStatuses;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.wmods.tkkenhancer.xposed.core.Feature;
import com.wmods.tkkenhancer.xposed.core.TkkCore;
import com.wmods.tkkenhancer.xposed.core.components.FMessageTkk;
import com.wmods.tkkenhancer.xposed.core.devkit.Unobfuscator;
import com.wmods.tkkenhancer.xposed.utils.ReflectionUtils;
import com.wmods.tkkenhancer.xposed.utils.ResId;

import org.luckypray.dexkit.query.enums.StringMatchType;

import java.lang.reflect.Field;

import de.robv.android.xposed.XSharedPreferences;

public class DeleteStatus extends Feature {


    public DeleteStatus(@NonNull ClassLoader classLoader, @NonNull XSharedPreferences preferences) {
        super(classLoader, preferences);
    }

    @Override
    public void doHook() throws Throwable {

        var fragmentloader = Unobfuscator.loadFragmentLoader(classLoader);
        var showDialogStatus = Unobfuscator.loadShowDialogStatusMethod(classLoader);
        Class<?> StatusDeleteDialogFragmentClass = Unobfuscator.findFirstClassUsingName(classLoader, StringMatchType.EndsWith, ".StatusDeleteDialogFragment");
        Field fieldBundle = ReflectionUtils.getFieldByType(fragmentloader, Bundle.class);

        var item = new MenuStatus.MenuItemStatus() {

            @Override
            public MenuItem addMenu(Menu menu, FMessageTkk fMessage) {
                if (menu.findItem(ResId.string.delete_for_me) != null) return null;
                if (fMessage.getKey().isFromMe) return null;
                return menu.add(0, ResId.string.delete_for_me, 0, ResId.string.delete_for_me);
            }

            @Override
            public void onClick(MenuItem item, Object fragmentInstance, FMessageTkk fMessage) {
                try {
                    var status = StatusDeleteDialogFragmentClass.newInstance();
                    var key = fMessage.getKey();
                    var bundle = getBundle(key);
                    TkkCore.setPrivBoolean(key.messageID + "_delpass", true);
                    fieldBundle.set(status, bundle);
                    showDialogStatus.invoke(null, status, fragmentInstance);
                } catch (Exception e) {
                    logDebug(e);
                }
            }
        };
        menuStatuses.add(item);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "Delete Status";
    }

    @NonNull
    private static Bundle getBundle(FMessageTkk.Key key) {
        var bundle = new Bundle();
        bundle.putString("fMessageKeyJid", key.remoteJid.getUserRawString());
        bundle.putBoolean("fMessageKeyFromMe", key.isFromMe);
        bundle.putString("fMessageKeyId", key.messageID);
        return bundle;
    }
}
