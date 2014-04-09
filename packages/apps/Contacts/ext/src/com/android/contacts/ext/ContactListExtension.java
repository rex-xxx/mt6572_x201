package com.android.contacts.ext;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContentProviderOperation.Builder;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;

import java.util.ArrayList;

public class ContactListExtension {
    private static final String TAG = "ContactListExtension";
    
    public String getCommand() {
        return "";
    }
    
    public void setMenuItem(MenuItem blockVoiceCallmenu, boolean mOptionsMenuOptions, String commd) {
        Log.i(TAG, "[setMenuItem] mOptionsMenuOptions : " + mOptionsMenuOptions);
        blockVoiceCallmenu.setVisible(mOptionsMenuOptions);

    }

//    public boolean[] setAllRejectedCall() {
//        return null;
//    }

    public void setLookSimStorageMenuVisible(MenuItem lookSimStorageMenu, boolean flag, String commd) {
        Log.i(TAG, "PeopleActivity: [setLookSimStorageMenuVisible()]");
        lookSimStorageMenu.setVisible(false);
    }

    public String getReplaceString(final String src, String commd) {
        Log.i(TAG, "AbstractStartSIMService: [getReplaceString()]");
        return src.replace('p', PhoneNumberUtils.PAUSE).replace('w', PhoneNumberUtils.WAIT);
    }

    /** M:AAS & SNE @ { */
    public void checkPhoneTypeArray(String accountType, ArrayList<Integer> phoneTypeArray,
            String commd) {
    }

    public String buildSimNickname(String accountType, ContentValues values,
            ArrayList<String> nicknameArray, int slotId, String defSimNickname, String cmd) {
        return defSimNickname;
    }

    public boolean generateDataBuilder(Context context, Cursor dataCursor, Builder builder,
            String[] columnNames, String accountType, String mimeType, int slotId, int index,
            String commd) {
        return false;
    }

    /** M: @ } */

    public void setExtentionImageView(ImageView view, String commd) {
        // do nothing
    }

///M: for SNS plugin @{
    /**
     * Get a icon will be displayed in a ContactListItemView.
     * 
     * @param cursor
     *            cursor containing data
     * @param statusResPackageColumn
     *            main column of status resource
     * @param statusIconColumn
     *            column of status icon
     * @param commd
     *            command
     * @return a icon if succeed, null otherwise
     */
    public Drawable getPresenceIcon(Cursor cursor, int statusResPackageColumn,
            int statusIconColumn, String commd) {
        return null;
    }

    /**
     * Get a status string will be displayed in a ContactListItemView.
     * 
     * @param cursor
     *            cursor containing data
     * @param statusResPackageColumn
     *            main column of status resource
     * @param contactsStatusColumn
     *            column of status string
     * @param commd
     *            command
     * @return a status string if succeed, null otherwise
     */
    public String getStatusString(Cursor cursor, int statusResPackageColumn,
            int contactsStatusColumn, String commd) {
        return null;
    }
///@}
}
