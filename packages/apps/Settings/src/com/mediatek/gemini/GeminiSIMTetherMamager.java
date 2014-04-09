package com.mediatek.gemini;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.Telephony.SIMInfo;

import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GeminiSIMTetherMamager {
    public static final Uri GEMINI_TETHER_URI = Data.CONTENT_URI;

    // database column
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_MIME_TYPE = "mimetype";
    public static final String PHONE_NUM_MIME_TYPE = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
    public static final String COLUMN_SIM_ID = "sim_id";// tethered SIM id
    public static final String COLUMN_PHONE_NUM = "data1";// phone number
    // phone number type, tel or mobile etc
    public static final String COLUMN_PHONE_NUM_TYPE = "data2";
    public static final String COLUMN_DISPLAY_NAME = "display_name";
    /*
     * indicator for where the number come from, <0 means stored in phone, >=0
     * means stored in some SIM card
     */
    public static final String COLUMN_SIM_SOURCE_TYPE = "indicate_phone_or_sim_contact";

    private static final String TAG = "GeminiSIMTetherManager";
    private static Context sContext;
    private static GeminiSIMTetherMamager sManager;
    private String mCurrSIMID = "1";

    private static final int BGCOLOR_SIM_ABSENT = 10;

    // a cache for SIMInfo, for avoiding to query from frameworkd every time
    private static HashMap<String, SIMInfo> sSimInfoMap = new HashMap<String, SIMInfo>();

    private GeminiSIMTetherMamager(Context context) {
        sContext = context;
    }

    // columns select from contacts data table
    private String[] mDataColumnArr = { COLUMN_ID, COLUMN_DISPLAY_NAME,
            COLUMN_PHONE_NUM, COLUMN_PHONE_NUM_TYPE, COLUMN_SIM_ID };

    /**
     * single instance factory method
     * 
     * @param context
     *            Context
     * @return single instanse of GeminiSIMTetherMamager
     */
    public static GeminiSIMTetherMamager getInstance(Context context) {
        if (sManager == null) {
            sManager = new GeminiSIMTetherMamager(context);
        }
        sContext = context;
        return sManager;
    }

    /**
     * 
     * @return current sim id
     */
    public String getCurrSIMID() {
        return mCurrSIMID;
    }

    /**
     * 
     * @param currSIMID
     *            String
     */
    public void setCurrSIMID(String currSIMID) {
        this.mCurrSIMID = currSIMID;
    }

    /**
     * Set new tether info into tether table
     * 
     * @param tetheredContactList
     *            ArrayList<Integer>
     * @param clearAll
     *            boolean
     */
    public void setCurrTetheredNum(ArrayList<Integer> tetheredContactList,
            boolean clearAll) {
        Xlog.i(TAG, "setCurrTetheredNum(), begin");
        // remove all record from tether table where simId==currSIMID
        // first un-bind all record from contacts table which is bind to current
        // SIM card
        String unBindSelectStr = COLUMN_MIME_TYPE + "='" + PHONE_NUM_MIME_TYPE
                + "' and " + COLUMN_SIM_ID + "= '" + mCurrSIMID + "'";
        ContentValues values = new ContentValues();
        values.put(COLUMN_SIM_ID, "-1");
        if (clearAll) {
            Xlog.i(TAG, "setCurrTetheredNum(), clear all tether begin");
            sContext.getContentResolver().update(GEMINI_TETHER_URI, values,
                    unBindSelectStr, null);
            Xlog.i(TAG, "setCurrTetheredNum(), clear all tether end");
        }
        // the re-bind select record to current SIM
        String bindSelectStr = "";
        int selectedSize = tetheredContactList.size();
        Xlog.i(TAG, "setCurrTetheredNum(), tethered contact num = "
                + selectedSize);
        for (int i = 0; i < selectedSize; i++) {
            if (i > 0) {
                bindSelectStr = bindSelectStr + ", ";
            }
            bindSelectStr = bindSelectStr
                    + tetheredContactList.get(i).toString();
        }
        Xlog.i(TAG, "setCurrTetheredNum(), bindSelectStr=[" + bindSelectStr
                + "]");
        bindSelectStr = "_id in (" + bindSelectStr + ") ";
        values.put(COLUMN_SIM_ID, mCurrSIMID);
        Xlog.i(TAG, "setCurrTetheredNum(), reset all tether begin");
        sContext.getContentResolver().update(GEMINI_TETHER_URI, values,
                bindSelectStr, null);
        Xlog.i(TAG, "setCurrTetheredNum(), reset all tether end");

    }

    /**
     * Get all phone number data tethered with simId
     * 
     * @return ArrayList
     */
    public ArrayList<GeminiSIMTetherItem> getCurrSimData() {
        Xlog.i(TAG, "getCurrSimData() begin");
        // get SIM name and color for simId
        String simName = "";
        int simColor = -1;
        Cursor cursor = null;
        SIMInfo simInfo = null;
        sSimInfoMap.clear();
        if (sSimInfoMap.containsKey(mCurrSIMID)) {
            simInfo = sSimInfoMap.get(mCurrSIMID);
        } else {
            simInfo = SIMInfo.getSIMInfoById(sContext, Integer
                    .parseInt(mCurrSIMID));
            sSimInfoMap.put(mCurrSIMID, simInfo);
        }
        if (simInfo != null) {
            simName = simInfo.mDisplayName;
            simColor = simInfo.mColor;
        }

        // get record from contact table first
        ArrayList<GeminiSIMTetherItem> resultList = new ArrayList<GeminiSIMTetherItem>();
        String contactSelectStr = COLUMN_MIME_TYPE + "='" + PHONE_NUM_MIME_TYPE
                + "' and " + COLUMN_SIM_ID + "='" + mCurrSIMID + "' and "
                + COLUMN_SIM_SOURCE_TYPE + " < 0";
        String contactOrderStr = COLUMN_DISPLAY_NAME + " asc";
        Xlog.i(TAG, "query data from data table, uri=" + GEMINI_TETHER_URI);
        try {
            cursor = sContext.getContentResolver().query(GEMINI_TETHER_URI, null,
                contactSelectStr, null, contactOrderStr);
            if (cursor != null) {
                cursor.moveToFirst();
                int numType;
                String numPrefix;
                while (!cursor.isAfterLast()) {
                    numPrefix = "";
                    // String displayName = cursor.getString(1);
                    String displayName = (String) cursor.getString(cursor
                            .getColumnIndex(COLUMN_DISPLAY_NAME));
                    // if no display name to show, just jump this record
                    if (displayName == null || displayName.equals("")) {
                        cursor.moveToNext();
                        continue;
                    }
                    GeminiSIMTetherItem item = new GeminiSIMTetherItem();
                    item.setContactId(cursor.getInt(cursor.getColumnIndex("_id")));
                    item.setName(displayName);
                    numType = cursor.getInt(cursor.getColumnIndex("data2"));
                    numPrefix = CommonDataKinds.Phone.getTypeLabel(
                            sContext.getResources(), numType, null).toString();
                    item.setPhoneNum(numPrefix + ": "
                            + cursor.getString(cursor.getColumnIndex("data1")));
                    item.setSimName(simName);
                    item.setSimColor(simColor);
                    item.setCheckedStatus(GeminiSIMTetherAdapter.FLAG_CHECKBOX_STSTUS_UNCHECKED);
                    resultList.add(item);
                    try {
                        cursor.moveToNext();
                    } catch (java.lang.IllegalStateException e) {
                        Xlog.d(TAG,"getCurrSimData---java.lang.IllegalStateException happen");
                        cursor.close();
                        cursor = sContext.getContentResolver().query(
                                GEMINI_TETHER_URI, null, contactSelectStr, null,
                                contactOrderStr);
                        cursor.moveToFirst();
                        resultList.clear();
                    }
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();    
            }
        }

        Xlog.i(TAG, "==getCurrSimData== size=" + resultList.size());
        return resultList;
    }

    /**
     * 
     * @return total contact number
     */
    public int getTotalContactNum() {
        Xlog.i(TAG, "getTotalContactNum(), begin");
        Cursor cursor = null;
        int number = 0;
        String contactSelectStr = COLUMN_MIME_TYPE + "='" + PHONE_NUM_MIME_TYPE
                + "' and " + COLUMN_SIM_SOURCE_TYPE + " < 0";
        String contactOrderStr = COLUMN_DISPLAY_NAME + " asc";
        try {
            cursor = sContext.getContentResolver().query(GEMINI_TETHER_URI, null,
                contactSelectStr, null, contactOrderStr);
            number = cursor.getCount();
        } finally {
            cursor.close(); 
        }        
        Xlog.i(TAG, "getTotalContactNum() = " + number);
        return number;
    }

    /**
     * Get all contact data, let user to choose
     * 
     * @return all contact list
     */
    public ArrayList<GeminiSIMTetherItem> getAllContactData() {
        Xlog.i(TAG, "getAllContactData(), begin");
        // get current inserted sim list
        List<SIMInfo> simInfoList = SIMInfo.getInsertedSIMList(sContext);
        String[] currInsertedSIM = {};
        if (simInfoList != null && simInfoList.size() > 0) {
            int listSize = simInfoList.size();
            Xlog.i(TAG, "current inserted sim num = " + listSize);
            currInsertedSIM = new String[listSize];
            for (int i = 0; i < simInfoList.size(); i++) {
                long simId = simInfoList.get(i).mSimId;
                Xlog.i(TAG, "Inserted sim id at " + i + " is " + simId);
                currInsertedSIM[i] = String.valueOf(simId);
            }
        }
        // get SIM name and color for simId
        String simName = "";
        int simColor = -1;
        Cursor cursor = null;

        // get record from contact table first
        ArrayList<GeminiSIMTetherItem> resultList = new ArrayList<GeminiSIMTetherItem>();
        String contactSelectStr = COLUMN_MIME_TYPE + "='" + PHONE_NUM_MIME_TYPE
                + "' and " + COLUMN_SIM_ID + "='" + -1 + "' and "
                + COLUMN_SIM_SOURCE_TYPE + " < 0";
        String contactOrderStr = COLUMN_DISPLAY_NAME + " asc";
        Xlog.i(TAG, "getAllContactData(), get all contact cursor begin");
        try {
            cursor = sContext.getContentResolver().query(GEMINI_TETHER_URI, null,
                contactSelectStr, null, contactOrderStr);
            Xlog.i(TAG, "getAllContactData(), get all contact cursor end");
            sSimInfoMap.clear();
            if (cursor != null) {
                cursor.moveToFirst();
                int numType;
                String numPrefix;
                SIMInfo simInfo = null;
                Xlog.i(TAG, "getAllContactData(), loop cursor begin");
                while (!cursor.isAfterLast()) {
                    numPrefix = "";
                    simName = "";
                    simColor = -1;

                    String displayName = cursor.getString(cursor
                            .getColumnIndex(COLUMN_DISPLAY_NAME));
                    // if no display name to show, just jump this record
                    if (displayName == null || displayName.equals("")) {
                        cursor.moveToNext();
                        continue;
                    }

                    GeminiSIMTetherItem item = new GeminiSIMTetherItem();
                    item.setContactId(cursor.getInt(cursor.getColumnIndex("_id")));
                    item.setName(displayName);

                    numType = cursor.getInt(cursor.getColumnIndex("data2"));
                    numPrefix = CommonDataKinds.Phone.getTypeLabel(
                            sContext.getResources(), numType, null).toString();
                    item.setPhoneNum(numPrefix + ": "
                            + cursor.getString(cursor.getColumnIndex("data1")));

                    String simIdString = cursor.getString(cursor
                            .getColumnIndex(COLUMN_SIM_ID));
                    if (simIdString != null && !simIdString.equals("")) {
                        if (sSimInfoMap.containsKey(simIdString)) {
                            simInfo = sSimInfoMap.get(simIdString);
                        } else {
                            simInfo = SIMInfo.getSIMInfoById(sContext, Integer
                                    .parseInt(simIdString));
                            sSimInfoMap.put(simIdString, simInfo);
                        }
                        if (simInfo != null) {
                            simName = simInfo.mDisplayName;
                            simColor = BGCOLOR_SIM_ABSENT;
                            // if the tethered SIM card exists,
                            // give it correct background color, else, just grey it
                            if (currInsertedSIM != null
                                    && currInsertedSIM.length > 0) {
                                for (int j = 0; j < currInsertedSIM.length; j++) {
                                    if (simIdString.equals(currInsertedSIM[j])) {
                                        simColor = simInfo.mColor;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (mCurrSIMID != null && mCurrSIMID.equals(simIdString)) {
                        item.setCheckedStatus(GeminiSIMTetherAdapter.FLAG_CHECKBOX_STSTUS_CHECKED);
                    } else {
                        item.setCheckedStatus(GeminiSIMTetherAdapter.FLAG_CHECKBOX_STSTUS_UNCHECKED);
                    }
                    item.setSimName(simName);
                    item.setSimColor(simColor);
                    if (item.getCheckedStatus() == GeminiSIMTetherAdapter.FLAG_CHECKBOX_STSTUS_UNCHECKED) {
                        resultList.add(item);
                    }
                    try {
                        cursor.moveToNext();
                    } catch (java.lang.IllegalStateException e) {
                        Xlog.d(TAG,"getAllContactData---java.lang.IllegalStateException happen");
                        cursor.close();
                        cursor = sContext.getContentResolver().query(
                                GEMINI_TETHER_URI, null, contactSelectStr, null,
                                contactOrderStr);
                        sSimInfoMap.clear();
                        resultList.clear();
                        cursor.moveToFirst();
                        numType = 0;
                    }
                }
                Xlog.i(TAG, "getAllContactData(), loop cursor end");
            }
        } finally {
            if (cursor != null) {
                cursor.close();    
            }
        }
        Xlog.i(TAG, "getAllContactData(), end");
        return resultList;
    }
}
