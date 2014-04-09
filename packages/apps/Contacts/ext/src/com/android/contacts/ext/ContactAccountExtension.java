package com.android.contacts.ext;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;

import java.util.ArrayList;

public class ContactAccountExtension {
    public static final int CONTENTVALUE_NICKNAME = 0;

    public static final int CONTENTVALUE_ANR_INSERT = CONTENTVALUE_NICKNAME + 1;

    public static final int CONTENTVALUE_ANR_UPDATE = CONTENTVALUE_ANR_INSERT + 1;

    public static final int CONTENTVALUE_INSERT_SIM = CONTENTVALUE_ANR_UPDATE + 1;

    public static final String FEATURE_AAS = "AAS";

    public static final String FEATURE_SNE = "SNE";

    public static final int DB_UPDATE_NICKNAME = 0;

    public static final int DB_UPDATE_ANR = DB_UPDATE_NICKNAME + 1;

    public static final int TYPE_OPERATION_AAS = 0;

    public static final int TYPE_OPERATION_SNE = TYPE_OPERATION_AAS + 1;

    public static final int TYPE_OPERATION_INSERT = TYPE_OPERATION_SNE + 1;

    public static final int PROJECTION_COPY_TO_SIM = 1;

    public static final int PROJECTION_LOAD_DATA = 2;

    public static final int PROJECTION_ADDRESS_BOOK = 3;

    public boolean needNewDataKind(String commd) {
        return false;
    }

    /** M:AAS & SNE @ { */
    public void setCurrentSlot(int slotId, String commd) {
    }

    public int getCurrentSlot(String commd) {
        return -1;
    }

    public boolean hidePhoneLabel(String accountType, String mimeType, String value, String commd) {
        return false;
    }

    public boolean isFeatureEnabled(String commd) {
        return false;
    }

    public boolean isFeatureAccount(String accountType, String commd) {
        return false;
    }

    public boolean isPhone(String mimeType, String commd) {
        return false;
    }

    /**
     * Default return Phone.getTypeLabel(res, type, label);
     * 
     * @param res
     * @param type
     * @param label
     * @param slotId
     * @return
     */
    public CharSequence getTypeLabel(Resources res, int type, CharSequence label, int slotId,
            String commd) {
        return Phone.getTypeLabel(res, type, label);
    }

    /**
     * @param type
     * @param customColumn
     * @return
     */
    public String getCustomTypeLabel(int type, String customColumn, String commd) {
        return null;
    }

    /**
     * @param accountType
     * @param updatevalues
     * @param anrsList
     * @param text
     * @param type
     * @param commd TODO
     * @return
     */
    public boolean updateContentValues(String accountType, ContentValues updatevalues,
            ArrayList anrsList, String text, int type, String commd) {
        if (type == CONTENTVALUE_ANR_UPDATE) {
            // text is update_additional_number
            String newAnr = TextUtils.isEmpty(text) ? "" : text;
            updatevalues.put("newAnr", newAnr);
            return true;
        }
        return false;
    }

    /**
     * @param text
     * @param slotId
     * @param feature
     * @param commd TODO
     * @return
     */
    public boolean isTextValid(String text, int slotId, int feature, String cmd) {
        return true;
    }

    public boolean updateDataToDb(String accountType, ContentResolver resolver, ArrayList newArr,
            ArrayList oldArr, long rawId, int type, String commd) {
        return false;
    }

    public boolean buildOperation(String accountType,
            ArrayList<ContentProviderOperation> operationList, ArrayList anrList, String text,
            int backRef, int type, String commd) {
        return false;
    }

    public boolean buildOperationFromCursor(String accountType,
            ArrayList<ContentProviderOperation> operationList, final Cursor cursor, int index,
            String cmd) {
        return false;
    }

    public boolean checkOperationBuilder(String accountType,
            ContentProviderOperation.Builder builder, Cursor cursor, int type, String commd) {
        if (type == TYPE_OPERATION_INSERT) {
            builder.withValue(Data.DATA2, 2);
            return true;
        }
        return false;
    }

    public boolean buildValuesForSim(String accountType, Context context, ContentValues values,
            ArrayList<String> additionalNumberArray, ArrayList<Integer> phoneTypeArray,
            int maxAnrCount, int dstSlotId, ArrayList anrsList, String commd) {
        return false;
    }

    /**
     * return the default value if Plugin do nothing
     * 
     * @param type
     * @param defaultProjection
     * @param commd
     * @return
     */
    public String[] getProjection(int type, String[] defaultProjection, String commd) {
        return defaultProjection;
    }
    /** M: @ } */

    /**
     * Called when the app want to show application guide
     * @param activity: The parent activity
     * @param type: The app type, such as "CONTACTS"
     */
    public void switchSimGuide(Activity activity, String type, String commd) {

    }
}
