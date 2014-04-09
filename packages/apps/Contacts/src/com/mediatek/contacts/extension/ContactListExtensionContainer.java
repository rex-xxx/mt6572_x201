package com.mediatek.contacts.extension;

import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import com.android.contacts.ext.ContactListExtension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;


public class ContactListExtensionContainer extends ContactListExtension {

    private static final String TAG = "ContactListExtensionContainer";

    private LinkedList<ContactListExtension> mSubExtensionList;

    public void add(ContactListExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<ContactListExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(ContactListExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public void setMenuItem(MenuItem blockVoiceCallmenu, boolean mOptionsMenuOptions, String commd) {
        Log.i(TAG, "[setMenuItem] mOptionsMenuOptions : " + mOptionsMenuOptions);
        if (null != mSubExtensionList) {
            Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                ContactListExtension extension = iterator.next();
                if (extension.getCommand().equals(commd)) {
                    extension.setMenuItem(blockVoiceCallmenu, mOptionsMenuOptions, commd);
                    return ;
                }
            }
        }
        super.setMenuItem(blockVoiceCallmenu, mOptionsMenuOptions, commd);
    }

//    public boolean[] setAllRejectedCall() {
//        Log.i(TAG, "[setAllRejectedCall] ");
//        if (null == mSubExtensionList) {
//            return null;
//        } else {
//            Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
//            while (iterator.hasNext()) {
//                return iterator.next().setAllRejectedCall();
//            }
//        }
//        return null;
//    }

    public void setLookSimStorageMenuVisible(MenuItem lookSimStorageMenu, boolean flag, String commd) {
        Log.i(TAG, "[setLookSimStorageMenuVisible] flag : " + flag);
        if (null != mSubExtensionList) {
            Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                ContactListExtension extension = iterator.next();
                if (extension.getCommand().equals(commd)) {
                    extension.setLookSimStorageMenuVisible(lookSimStorageMenu, flag, commd);
                    return;
                }
            }
        }
        super.setLookSimStorageMenuVisible(lookSimStorageMenu, flag, commd);
    }

    public String getReplaceString(final String src, String commd) {
        Log.i(TAG, "[getReplaceString] src : " + src);
        if (null == mSubExtensionList) {
            return src.replace('p', PhoneNumberUtils.PAUSE).replace('w', PhoneNumberUtils.WAIT);
        } else {
            Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                final String result = iterator.next().getReplaceString(src, commd);
                if (result != null) {
                    return result;
                }
            }
        }
        return src.replace('p', PhoneNumberUtils.PAUSE).replace('w', PhoneNumberUtils.WAIT);
    }

    public void setExtentionImageView(ImageView view, String commd) {
        Log.i(TAG, "[setExtentionIcon]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setExtentionImageView(view, commd);
        }
    }

    /** M:AAS & SNE @ { */
    public void checkPhoneTypeArray(String accountType, ArrayList<Integer> phoneTypeArray,
            String commd) {
        Log.i(TAG, "[checkPhoneTypeArray]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().checkPhoneTypeArray(accountType, phoneTypeArray, commd);
        }
    }

    public boolean generateDataBuilder(Context context, Cursor dataCursor, Builder builder,
            String[] columnNames, String accountType, String mimeType, int slotId, int index,
            String commd) {
        Log.i(TAG, "[generateDataBuilder()]");
        if (null != mSubExtensionList) {
            for (ContactListExtension subExtension : mSubExtensionList) {
                if (subExtension.generateDataBuilder(context, dataCursor, builder, columnNames,
                        accountType, mimeType, slotId, index, commd)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String buildSimNickname(String accountType, ContentValues values,
            ArrayList<String> nicknameArray, int slotId, String defSimNickname, String cmd) {
        if (null != mSubExtensionList) {
            for (ContactListExtension subExtension : mSubExtensionList) {
                String nickName = subExtension.buildSimNickname(accountType, values, nicknameArray,
                        slotId, defSimNickname, cmd);
                if (!TextUtils.equals(nickName, defSimNickname)) {
                    return nickName;
                }
            }
        }
        return defSimNickname;
    }
    /** M: @ } */

    ///M: for SNS plugin @{
    @Override
    public Drawable getPresenceIcon(Cursor cursor, int statusResPackageColumn,
            int statusIconColumn, String commd) {
        Log.i(TAG, "[getPresenceIcon]");
        if (null == mSubExtensionList) {
            return null;
        }

        Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            ContactListExtension cle = iterator.next();
            if (commd.equals(cle.getCommand())) {
                return cle.getPresenceIcon(cursor, statusResPackageColumn,
                        statusIconColumn, commd);
            }
        }

        return null;
    }

    @Override
    public String getStatusString(Cursor cursor, int statusResPackageColumn,
            int contactsStatusColumn, String commd) {
        Log.i(TAG, "[getStatusString]");
        if (null == mSubExtensionList) {
            return null;
        }

        Iterator<ContactListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            ContactListExtension cle = iterator.next();
            if (commd.equals(cle.getCommand())) {
                return cle.getStatusString(cursor, statusResPackageColumn,
                        contactsStatusColumn, commd);
            }
        }

        return null;
    }
    ///@}
}
