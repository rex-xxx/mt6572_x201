package com.android.contacts.ext;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import java.util.HashMap;

public class ContactDetailExtension {
    private static final String TAG = "ContactDetailExtension";

    public static final int VIEW_UPDATE_NONE = 0;

    public static final int VIEW_UPDATE_HINT = 1;

    public static final int VIEW_UPDATE_VISIBILITY = 2;

    public static final char STRING_PRIMART = 0;

    public static final char STRING_ADDITINAL = 1;

        public String getCommand() {
        return "";
    }

    public void setMenu(ContextMenu menu, boolean isNotDirectoryEntry, int simId,
            boolean mOptionsMenuOptions, int delSim, int newSim, Activity activity,
            int removeAssociation, int menuAssociation, String commd) {
        Log.i(TAG, "[setMenu]  | isNotDirectoryEntry : " + isNotDirectoryEntry + " | simId : "
                + simId + " | mOptionsMenuOptions : " + mOptionsMenuOptions + " | delSim : "
                + delSim + " | newSim : " + newSim + " | activity : " + activity
                + " | removeAssociation : " + removeAssociation + " | menuAssociation : "
                + menuAssociation);
        if (activity != null) {
            if (isNotDirectoryEntry) {
                if (simId > -1) {
                    menu.add(ContextMenu.NONE, delSim, ContextMenu.NONE, activity
                            .getString(removeAssociation));
                } else {
                    MenuItem item = menu.add(ContextMenu.NONE, newSim, ContextMenu.NONE, activity
                            .getString(menuAssociation));
                    item.setEnabled(mOptionsMenuOptions);
                }
            }
        }
    }

    public String setSPChar(String string, String commd) {
        Log.i(TAG, "[setSPChar] string : " + string);
        return string;
    }

    public void setViewKeyListener(EditText fieldView, String commd) {
        Log.i(TAG, "[setViewKeyListener]");
    }

    public String TextChanged(int inputType, Editable s, String phoneText, int location,
            String commd) {
        return phoneText;
    }

    public boolean checkMenuItem(boolean mtkGeminiSupport, boolean hasPhoneEntry, boolean notMe,
            String commd) {
        Log.i(TAG, "[checkMenuItem]mtkGeminiSupport : " + mtkGeminiSupport + " | hasPhoneEntry : "
                + hasPhoneEntry + " | notMe : " + notMe);
        return (mtkGeminiSupport && hasPhoneEntry && notMe);
    }

    public void setMenuVisible(MenuItem associationMenuItem, boolean mOptionsMenuOptions,
            boolean isEnabled, String commd) {
        Log
                .i(TAG, "[setMenuVisible] associationMenuItem : " + associationMenuItem
                        + " | mOptionsMenuOptions : " + mOptionsMenuOptions + " | isEnabled : "
                        + isEnabled);
        if (associationMenuItem != null) {
            associationMenuItem.setVisible(mOptionsMenuOptions);
            associationMenuItem.setEnabled(isEnabled);
        } else {
            Log.e(TAG, "[setMenuVisible] associationMenuItem is null");
        }
    }

    public String repChar(String phoneNumber, char pause, char p, char wait, char w, String commd) {
        Log.i(TAG, "phoneNumber : " + phoneNumber);
        return phoneNumber;
    }

    public boolean collapsePhoneEntries(String commd) {
        Log.i(TAG, "[collapsePhoneEntries()]");
        return true;
    }

    /** M:AAS @ { */
    /**
     * @param view
     * @param type To indicate which view
     * @param action Operator action (set visibility, update text, set hint
     *            text, etc.)
     * @return
     */
    public boolean updateView(View view, int type, int action, String commd) {
        return false;
    }

    public int getMaxEmptyEditors(String mimeType, String commd) {
        return 1;
    }

    public int getAdditionNumberCount(int slotId, String commd) {
        return 0;
    }

    public boolean isDoublePhoneNumber(String[] buffer, String[] bufferName, String commd) {
        return (!TextUtils.isEmpty(buffer[1])) || (!TextUtils.isEmpty(bufferName[1]));
    }

    /** M: @ } */

    public void onContactDetailOpen(Uri contactLookupUri, String commd) {
    }

    public String getExtensionTitles(String data, String mimeType, String kind,
            HashMap<String, String> mPhoneAndSubtitle, String commd) {
        return kind;
    }

    public String getExtentionMimeType(String commd) {
        return null;
    }

    public int layoutExtentionIcon(int leftBound, int topBound, int bottomBound, int rightBound,
            int mGapBetweenImageAndText, ImageView mExtentionIcon, String commd) {
        return rightBound;
    }

    public void measureExtentionIcon(ImageView mExtentionIcon, String commd) {
    }

    public Intent getExtentionIntent(int action01, int action02, String commd) {
        return null;
    }

    public boolean getExtentionKind(String mimeType, boolean needSetName, String name, String commd) {
        return false;
    }

    public boolean checkPluginSupport(String commd) {
        return false;
    }

    public boolean canSetExtensionIcon(long contactId, String commd) {
        return false;
    }

    public void setExtensionImageView(ImageView view, long contactId, String commd) {
        // do nothing
    }

    public void setViewVisible(View view, Activity activity, String data1, String data2,
            String data3, String commd, int res1, int res2, int res3, int res4, int res5, int res6) {
        // do nothing
    }

    public void setViewVisibleWithCharSequence(View view, Activity activity, String data1,
            String data2, CharSequence data3, String commd, int res1, int res2, int res3, int res4,
            int res5, int res6) {
        // do nothing
    }

///M: for SNS plugin @{
    /**
     * Is mime type supported by plugin.
     * 
     * @param mimeType
     *            the mime type
     * @param plugin
     *            name of the plugin
     * @param commd
     *            command
     * @return true if the mime type supported, false otherwise
     */
    public boolean isMimeTypeSupported(String mimeType, String plugin,
            String commd) {
        return false;
    }

    /**
     * Configure view passed in.
     * 
     * @param view
     *            view want to configure
     * @param activity
     *            host's activity
     * @param data
     *            data used to configure the view
     * @param res1
     *            resource id or parameter
     * @param res2
     *            resource id or parameter
     * @param res3
     *            resource id or parameter
     * @param res4
     *            resource id or parameter
     * @param res5
     *            resource id or parameter
     * @param res6
     *            resource id or parameter
     * @param res7
     *            resource id or parameter
     * @param res8
     *            resource id or parameter
     * @param commd
     *            command
     */
    public void setViewVisibleWithBundle(View view, Activity activity,
            Bundle data, int res1, int res2, int res3, int res4, int res5,
            int res6, int res7, int res8, String commd) {
        // do nothing
    }
///@}

    /**
     * get the rcs-e icon on the Detailt Actvitiy's action bar
     * @return if there isn't show rcs-e icon,return null.
     */
    public Drawable getRCSIcon(long id) {
        return null;
    }
}
