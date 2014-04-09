package com.mediatek.contacts.extension.aassne;

import java.util.ArrayList;

import com.android.contacts.ext.ContactAccountExtension;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactModifier;
import com.android.internal.telephony.IIccPhoneBook;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.model.UsimAccountType;
import com.mediatek.contacts.simcontact.SlotUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.text.TextUtils;
import android.util.Log;

public class SneExt {
    private static final String TAG = "SnesExt";

    private static final int ERROR = -1;

    private static int[] MAX_USIM_SNE_MAX_LENGTH = { -1, -1 };

    /**
     * check whether the Usim support SNE field.
     * @return
     */
    public static boolean hasSne(int slot) {
        Log.d(TAG, "[hasSne]slot:" + slot);
        boolean hasSne = false;
        if (!SlotUtils.isSlotValid(slot)) {
            return hasSne;
        }
        try {
            final IIccPhoneBook iIccPhb = SimUtils.getIIccPhoneBook(slot);
            if (iIccPhb != null) {
                hasSne = iIccPhb.hasSne();
                Log.d(TAG, "hasSne, hasSne=" + hasSne);
            }
        } catch (android.os.RemoteException e) {
            Log.e(TAG, "[hasSne] exception.");
        }
        Log.d(TAG, "[hasSne] hasSne:" + hasSne);
        return hasSne;
    }

    /**
     * ensure Nickname updated and exists
     * @param type
     * @param slotId
     * @param entity
     */
    public static void ensureNicknameKindForEditorExt(AccountType type, int slotId, RawContactDelta entity) {
        if (ExtensionManager.getInstance().getContactAccountExtension().isFeatureAccount(type.accountType,
                ExtensionManager.COMMD_FOR_SNE)) {
            DataKind dataKind = type.getKindForMimetype(Nickname.CONTENT_ITEM_TYPE);
            if (dataKind != null) {
                UsimAccountType.updateNickname(dataKind, hasSne(slotId));
            }
            RawContactModifier.ensureKindExists(entity, type, Nickname.CONTENT_ITEM_TYPE);
        }
    }

    public static boolean isNickname(String mimeType) {
        if (isSneEnable()) {
            return Nickname.CONTENT_ITEM_TYPE.equals(mimeType);
        }
        return false;
    }

    public static boolean buildNicknameValueForInsert(int slotId, ContentValues cv, String nickname) {
        if (hasSne(slotId)) {
            cv.put("sne", TextUtils.isEmpty(nickname) ? "" : nickname);
            return true;
        }
        return false;
    }

    public static void updateDataToDb(int slotId, String accountType, ContentResolver resolver, String updateNickname,
            String oldNickname, long rawId) {
        if (ExtensionManager.getInstance().getContactAccountExtension().isFeatureAccount(accountType,
                ExtensionManager.COMMD_FOR_SNE)
                && hasSne(slotId)) {
            ArrayList<String> arrNickname = new ArrayList<String>();
            arrNickname.add(updateNickname);
            arrNickname.add(oldNickname);
            ExtensionManager.getInstance().getContactAccountExtension().updateDataToDb(accountType, resolver,
                    arrNickname, null, rawId, ContactAccountExtension.DB_UPDATE_NICKNAME, ExtensionManager.COMMD_FOR_SNE);
        }
    }

    private static boolean isSneEnable() {
        return ExtensionManager.getInstance().getContactAccountExtension().isFeatureEnabled(
                ExtensionManager.COMMD_FOR_SNE);
    }
}
