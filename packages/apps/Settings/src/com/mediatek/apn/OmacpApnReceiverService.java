package com.mediatek.apn;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;

import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.net.Uri;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Telephony;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.settings.ext.IReplaceApnProfileExt;
import com.android.settings.R;
import com.android.settings.Utils;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * @author mtk54093 ,refractor code
 * receive omacp action to install apn
 * 
 */
public class OmacpApnReceiverService extends IntentService {

    /**
     * Action: com.mediatek.omacp.settings Extra: appId, String Extra: context,
     * String Extra: simId, int Please refer to OmacpApplicationCapability part.
     * Notes: Only those value=true application attributes will be included in
     * the intent extra.
     */
    private static final String TAG = "OmacpApnReceiverService";
    

    private static final String ACTION_OMACP_RESULT = "com.mediatek.omacp.settings.result";
    
    // feedback item
    private static final String APP_ID = "appId";
    private static final String APP_ID_APN = "apn";
    
    // parse items from intent
    private static final String APN_NAME = "NAP-NAME";// name
    private static final String APN_APN = "NAP-ADDRESS";// apn
    private static final String APN_PROXY = "PXADDR";// proxy
    private static final String APN_PORT = "PORTNBR";// port
    private static final String APN_USERNAME = "AUTHNAME";// username
    private static final String APN_PASSWORD = "AUTHSECRET";// password
    private static final String APN_SERVER = "SERVER";// server
    private static final String APN_MMSC = "MMSC";// mmsc
    private static final String APN_MMS_PROXY = "MMS-PROXY";// mms proxy
    private static final String APN_MMS_PORT = "MMS-PORT";// mms port
    private static final String APN_AUTH_TYPE = "AUTHTYPE";// auth type
    private static final String APN_TYPE = "APN-TYPE";// type
    private static final String APN_ID = "APN-ID";// type
    private static final String APN_NAP_ID = "NAPID";// type
    private static final String APN_PROXY_ID = "PROXY-ID";// type
    private static final String NAP_AUTH_INFO = "NAPAUTHINFO";
    private static final String PORT = "PORT";
    private static final String APN_SETTING_INTENT = "apn_setting_intent";

    // auth type
    private static int sAuthType = -1;
    // MMS type
    private static final String MMS_TYPE = "mms";
    
    // an apn items
    private String mName; // the carrier , the same with title of displaying in APN Settings
    private String mApn; // the apn item of config
    private String mProxy;
    private String mPort;
    private String mUserName;
    private String mPassword;
    private String mServer;
    private String mMmsc;
    private String mMmsProxy;
    private String mMmsPort;
    private String mAuthType;
    private String mType;
    private String mApnId;
    private String mMcc;
    private String mMnc;
    private String mNapId;
    private String mProxyId;
    
    // intent list
    private ArrayList<Intent> mIntentList;

    // update DB by these conditions
    private Uri mUri;
    private String mNumeric;
    private Uri mPreferedUri;

    // the type is mms or not
    private boolean mIsMmsApn = false;
    // set the default as true , set it as false when any step fails
    private boolean mResult = true;
    
    // for omacp update result state
    // 0 stands for the apn with the apnId exists , not update it and not insert it again
    private static final long APN_EXIST = 0;
    // -1 stands for the apn  inserted fail
    private static final long APN_NO_UPDATE = -1;
        
    // telephony service
    private ITelephony mTelephonyService;
    
    // plugin
    private IReplaceApnProfileExt mReplaceApnExt;

    private static Context sCONTEXT;
    
    private static final String ACTION_BROADCAST = "broadcast_receiver";
    
    private int mSimId;
    
    public OmacpApnReceiverService() {
        super("OmacpApnReceiverService");
    }
    
    
    public static void processBroadcastIntent(Context context, Intent broadcastIntent) {
        // Launch the Service
        sCONTEXT = context;
        Intent i = new Intent(context, OmacpApnReceiverService.class);
        i.setAction(ACTION_BROADCAST);
        i.putExtra(Intent.EXTRA_INTENT, broadcastIntent);
        Xlog.d(TAG, "startService");
        context.startService(i);
    }
    
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        Xlog.d(TAG, "get action = " + action);
        if (!ACTION_BROADCAST.equals(action)) {
            return;
        }
        
        mReplaceApnExt = Utils.getReplaceApnPlugin(sCONTEXT);
        mTelephonyService = ITelephony.Stub.asInterface(ServiceManager.getService(sCONTEXT.TELEPHONY_SERVICE));

        Intent broadcastIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
        
        mIntentList = broadcastIntent.getParcelableArrayListExtra(APN_SETTING_INTENT);

        if (mIntentList == null) {
            mResult = false;
            sendFeedback(sCONTEXT);
            Xlog.e(TAG, "mIntentList == null");
            return;
        }

        int sizeIntent = mIntentList.size();

        Xlog.d(TAG, "apn list size is " + sizeIntent);

        if (sizeIntent <= 0) {
            mResult = false;
            sendFeedback(sCONTEXT);
            Xlog.e(TAG, "Intent list size is wrong");
            return;
        }
        
        // firstly , get numeric and preferred uri one time , not put it into the for circle
        if (!initState(mIntentList.get(0))) {
            sendFeedback(sCONTEXT);
            Xlog.e(TAG, "Can not get MCC+MNC");
            return;
        }
            
        
        // must identify the gemini and non_gemini ,because the uri is not
        // different between one sim card inserted on non_gemini and slot 0 inserted
        // on gemini
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Xlog.d(TAG, "PhoneConstants.GEMINI_SIM_NUM " + PhoneConstants.GEMINI_SIM_NUM);
            for (int slotId = 0; mResult && slotId < PhoneConstants.GEMINI_SIM_NUM; slotId++) {
                mUri = ApnUtils.URI_LIST[slotId];
                Xlog.d(TAG, "slotId = " + slotId + " mUri = " + mUri + " mNumeric = " + mNumeric + " mPreferedUri = "
                        + mPreferedUri);
                for (int k = 0; mResult && k < sizeIntent ; k++) {
                    // extract APN parameters
                    extractAPN(mIntentList.get(k), sCONTEXT);
                    ContentValues values = new ContentValues();
                    validateProfile(values);
                    updateApn(sCONTEXT, mUri, mApn, mApnId, mName, values, mNumeric, 
                            mPreferedUri,slotId);
                }
            }
        } else { // for non gemini
            mUri = Telephony.Carriers.CONTENT_URI;
            Xlog.d(TAG, "mUri = " + mUri + " mNumeric = " + mNumeric + " mPreferedUri = " + mPreferedUri);
            for (int k = 0; mResult && k < sizeIntent; k++) {
                // extract APN parameters
                extractAPN(mIntentList.get(k), sCONTEXT);
                ContentValues values = new ContentValues();
                validateProfile(values);
                // the last parameter set 0 , it's useless for non gemini
                updateApn(sCONTEXT, mUri, mApn, mApnId, mName, values, mNumeric, 
                        mPreferedUri, ApnUtils.SIM_CARD_SINGLE);
            }
        }

        sendFeedback(sCONTEXT);
    }

    /**
     * send installation result to OMA CP service
     * 
     * @param context
     * @param result
     */

    private void sendFeedback(Context context) {
        Intent it = new Intent();
        it.setAction(ACTION_OMACP_RESULT);
        it.putExtra(APP_ID, APP_ID_APN);
        it.putExtra("result", mResult);
        context.sendBroadcast(it);
    }

    /**
     * Check the key fields' validity and save if valid.
     * 
     * @param force save even if the fields are not valid
     * 
     */
    private void validateProfile(ContentValues values) {
        values.put(ApnUtils.PROJECTION[ApnUtils.NAME_INDEX], mName);
        values.put(ApnUtils.PROJECTION[ApnUtils.APN_INDEX], ApnUtils.checkNotSet(mApn));
        values.put(ApnUtils.PROJECTION[ApnUtils.PROXY_INDEX],ApnUtils.checkNotSet(mProxy));
        values.put(ApnUtils.PROJECTION[ApnUtils.PORT_INDEX],ApnUtils.checkNotSet(mPort));
        values.put(ApnUtils.PROJECTION[ApnUtils.USER_INDEX],ApnUtils.checkNotSet(mUserName));
        values.put(ApnUtils.PROJECTION[ApnUtils.SERVER_INDEX],ApnUtils.checkNotSet(mServer));
        values.put(ApnUtils.PROJECTION[ApnUtils.PASSWORD_INDEX],ApnUtils.checkNotSet(mPassword));
        values.put(ApnUtils.PROJECTION[ApnUtils.MMSC_INDEX],ApnUtils.checkNotSet(mMmsc));
        values.put(ApnUtils.PROJECTION[ApnUtils.MCC_INDEX], mMcc);
        values.put(ApnUtils.PROJECTION[ApnUtils.MNC_INDEX], mMnc);
        values.put(ApnUtils.PROJECTION[ApnUtils.MMSPROXY_INDEX],ApnUtils.checkNotSet(mMmsProxy));
        values.put(ApnUtils.PROJECTION[ApnUtils.MMSPORT_INDEX],ApnUtils.checkNotSet(mMmsPort));
        values.put(ApnUtils.PROJECTION[ApnUtils.AUTH_TYPE_INDEX], sAuthType);
        values.put(ApnUtils.PROJECTION[ApnUtils.TYPE_INDEX],ApnUtils.checkNotSet(mType));
        values.put(ApnUtils.PROJECTION[ApnUtils.SOURCE_TYPE_INDEX], 2);
        values.put(ApnUtils.PROJECTION[ApnUtils.APN_ID_INDEX],ApnUtils.checkNotSet(mApnId));
        values.put(ApnUtils.PROJECTION[ApnUtils.NAP_ID_INDEX],ApnUtils.checkNotSet(mNapId));
        values.put(ApnUtils.PROJECTION[ApnUtils.PROXY_ID_INDEX],ApnUtils.checkNotSet(mProxyId));
        values.put(ApnUtils.PROJECTION[ApnUtils.NUMERIC_INDEX], mNumeric);
    }

    /* judge the numeric is right or not.
     * */
    private boolean verifyMccMnc(String numeric) {
        // MCC is first 3 chars and then in 2 - 3 chars of MNC
        if (mNumeric != null && mNumeric.length() > 4) {
            // Country code
            String mcc = mNumeric.substring(0, 3);
            // Network code
            String mnc = mNumeric.substring(3);
            // Auto populate MNC and MCC for new entries, based on what SIM
            // reports
            mMcc = mcc;
            mMnc = mnc;
            Xlog.d(TAG, "mcc&mnc is right , mMcc = " + mMcc + " mMnc = " + mMnc);
        } else {
            mResult = false;
            Xlog.d(TAG, "mcc&mnc is wrong , set mResult = false");
        }
        return mResult;
    }

    /**
     * get port
     */
    private void getPort(Intent intent) {
        mPort = null;
        ArrayList<HashMap<String, String>> portList = (ArrayList<HashMap<String, String>>) intent
                .getExtra(PORT);
        if (portList != null) {
            if (portList.size() > 0) {
                HashMap<String, String> portMap = portList.get(0);//using the first one, ignore others 
                mPort = portMap.get(APN_PORT);//port
            }
        }
    }

    /**
     * get username,password,auth_type
     */
    private void getNapAuthInfo(Intent intent) {

        mUserName = null;
        mPassword = null;
        mAuthType = null;
        sAuthType = -1;

        ArrayList<HashMap<String, String>> napAuthInfo = (ArrayList<HashMap<String, String>>) intent
                .getExtra(NAP_AUTH_INFO);
        if (napAuthInfo != null) {
            if (napAuthInfo.size() > 0) {
                HashMap<String, String> napAuthInfoMap = napAuthInfo.get(0);
                mUserName = napAuthInfoMap.get(APN_USERNAME);// username
                mPassword = napAuthInfoMap.get(APN_PASSWORD);// password
                mAuthType = napAuthInfoMap.get(APN_AUTH_TYPE);// auth type

                if (mAuthType != null) {
                    if ("PAP".equalsIgnoreCase(mAuthType)) {
                        sAuthType = 1;// PAP
                    } else if ("CHAP".equalsIgnoreCase(mAuthType)) {
                        sAuthType = 2;// CHAP
                    } else {
                        sAuthType = 3;// PAP or CHAP
                    }
                }
            }
        }
    }


    /**
     * Extract APN parameters from the intent
     */
    private void extractAPN(Intent intent, Context context) {

        // apn parameters
        mName = intent.getStringExtra(APN_NAME);// name

        if ((mName == null) || (mName.length() < 1)) {
            mName = context.getResources().getString(R.string.untitled_apn);
        }
        mApn = intent.getStringExtra(APN_APN);// apn
        mProxy = intent.getStringExtra(APN_PROXY);// proxy

        // get port
        getPort(intent);
        // get username,password,auth_type
        getNapAuthInfo(intent);

        mServer = intent.getStringExtra(APN_SERVER);// server
        mMmsc = intent.getStringExtra(APN_MMSC);// MMSC
        mMmsProxy = intent.getStringExtra(APN_MMS_PROXY);// MMSC proxy
        mMmsPort = intent.getStringExtra(APN_MMS_PORT);// MMSC port
        mType = intent.getStringExtra(APN_TYPE);// type
        mApnId = intent.getStringExtra(APN_ID);// apnId:should be unique
        mNapId = intent.getStringExtra(APN_NAP_ID);
        mProxyId = intent.getStringExtra(APN_PROXY_ID);

        mIsMmsApn = MMS_TYPE.equalsIgnoreCase(mType);
        Xlog.d(TAG, "extractAPN: mName: " + mName + " | mApn: " + mApn
                + " | mProxy: " + mProxy + " | mServer: " + mServer
                + " | mMmsc: " + mMmsc + " | mMmsProxy: " + mMmsProxy
                + " | mMmsPort: " + mMmsPort + " | mType: " + mType
                + " | mApnId: " + mApnId + " | mNapId: " + mNapId
                + " | mMmsPort: " + mMmsPort + " | mProxyId: " + mProxyId 
                + " | mIsMmsApn: " + mIsMmsApn);
    }

    /* set the apn as the selected one. 
     */
    private boolean setCurrentApn(final Context context, final long apnToUseId,
            final Uri preferedUri) {
        int row = 0;
        ContentValues values = new ContentValues();
        values.put("apn_id", apnToUseId);
        ContentResolver mContentResolver = context.getContentResolver();
        try {
            row = mContentResolver.update(preferedUri, values, null, null);
            Xlog.d(TAG,"update preferred uri ,row = " + row);
        } catch (SQLException e) {
            Xlog.d(TAG, "SetCurrentApn SQLException happened!");
        }
        return (row > 0) ? true : false;
    }

    /*
     * update apn , firstly judge it exists or not according apn id or name,
     * if exists , update it or keep the previous state
     * if no exist , insert it as a new one to DataBase
     */
       
    private void updateApn(Context context, Uri uri, String apn,String apnId, String name,
            ContentValues values, String numeric, Uri peferredUri , int slotId) {
        // firstly try to find the apn exist in DB or not , if
        // yes , to update it
        long replaceNum = mReplaceApnExt.replaceApn(context, uri, apn,apnId, name,
                values, numeric);

        Xlog.d(TAG,"replace number = " + replaceNum);
        
        long insertNum = replaceNum;
        
        // secondly, if the apn does not exist , insert it into
        // DB as a new item
        if (replaceNum == APN_NO_UPDATE) {
            values = addMVNOItem(values,slotId);
            try {
                Uri newRow = context.getContentResolver().insert(uri, values);
                if (newRow != null) {
                    Xlog.d(TAG, "uri = " + newRow);
                    if (newRow.getPathSegments().size() == 2) {
                        insertNum = Long.parseLong(newRow.getLastPathSegment());
                        Xlog.d(TAG, "insert row id = " + insertNum);
                    }
                }
            } catch (SQLException e) {
                Xlog.d(TAG, "insert SQLException happened!");
                mResult = false;
            }
        }
        Xlog.d(TAG,"insert number = " + insertNum);
        
        // default value of mResult is true , so set it as false when meet the following cases:
        // mms type , replaced num is 0 , fail to update 
        if (mIsMmsApn) {
            if (insertNum == APN_NO_UPDATE) {
                 mResult = false;
                 Xlog.d(TAG,"mms ,insertNum is APN_NO_UPDATE ,mResult = false");
            }
        } else { // if the type is not mms and update or insert
            // successfully,
            // should set updated apn as the current default
            // selected one
            if (insertNum == APN_NO_UPDATE) {     
                mResult = false;
                Xlog.d(TAG,"not mms ,insertNum is APN_NO_UPDATE ,mResult = false");
            } else if (insertNum == APN_EXIST) {
                mResult = true;
                Xlog.d(TAG,"not mms ,  insertNum is APN_EXIST ,mResult = true");
            } else {                
                if (slotId == mSimId) { // update the selected only the sim inserted
                    mResult = setCurrentApn(context, insertNum, peferredUri);
                    Xlog.d(TAG,"set current apn result , mResult = " + mResult);
                }
            }
        }
    }
    
    
    /**
     *  add the mvno item : spn, imsi ,pnn if support MVNO
     * @param values
     * @param slotId 
     * @return the added mvno item values
     */
    ContentValues addMVNOItem(ContentValues values, int slotId) {
        String spn;
        String imsi;
        String pnn;
        boolean isMVNO = false;
        if (FeatureOption.MTK_MVNO_SUPPORT) {
            try {
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    isMVNO = mTelephonyService.isIccCardProviderAsMvnoGemini(slotId);
                } else {
                    isMVNO = mTelephonyService.isIccCardProviderAsMvno();
                }
            } catch (android.os.RemoteException e) {
                    Xlog.d(TAG, "RemoteException");
            }
        }
        Xlog.d(TAG, "isMVNO = " + isMVNO);
        if (FeatureOption.MTK_MVNO_SUPPORT && isMVNO) {
            try {
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    spn = mTelephonyService.getSpNameInEfSpnGemini(slotId);
                    imsi = mTelephonyService.isOperatorMvnoForImsiGemini(slotId);
                    pnn = mTelephonyService.isOperatorMvnoForEfPnnGemini(slotId);
                } else {
                    spn = mTelephonyService.getSpNameInEfSpn();
                    imsi = mTelephonyService.isOperatorMvnoForImsi();
                    pnn = mTelephonyService.isOperatorMvnoForEfPnn();
                }
                Xlog.d(TAG, "spn = " + spn);
                Xlog.d(TAG, "imsi = " + imsi);
                Xlog.d(TAG, "pnn = " + pnn);
                
                if (imsi != null && !imsi.isEmpty()) {
                    values.put(Telephony.Carriers.IMSI, imsi);
                } else if (spn != null && !spn.isEmpty()) {
                    values.put(Telephony.Carriers.SPN, spn);
                } else {
                    values.put(Telephony.Carriers.PNN, pnn);
                }
            } catch (android.os.RemoteException e) {
                Xlog.d(TAG, "RemoteException");
                return values;
            }
        }
        return values;
    }
    
    
    /**
     * Get basic param Init Content preferrid & numeric
     */
    private boolean initState(Intent intent) {
        // get simId
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mSimId = intent.getIntExtra("simId", ApnUtils.SIM_CARD_UNDEFINED);
            Xlog.d(TAG, "GEMINI_SIM_ID_KEY = " + mSimId);
            // only get the numeric and preferred uri one time
            mNumeric = ApnUtils.NUMERIC_LIST[mSimId];
            mPreferedUri = ApnUtils.PREFERRED_URI_LIST[mSimId];
        } else {
            mSimId = ApnUtils.SIM_CARD_SINGLE;
            Xlog.d(TAG, "Not support GEMINI");
            mNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "-1");
            mPreferedUri = Uri.parse(ApnUtils.PREFERRED_APN_URI);
        }
        Xlog.d(TAG, "initState: mSimId: " + mSimId + " | mNumeric: " + mNumeric 
                + " | mPreferedUri: " + mPreferedUri);
        
        return verifyMccMnc(mNumeric);
    }
}
