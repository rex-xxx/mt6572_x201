/* //device/content/providers/telephony/TelephonyProvider.java
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.database.SQLException;
import android.net.Uri;
import android.os.Environment;
import android.os.FileUtils;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony;
import android.util.Log;
import android.util.Xml;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.BaseCommands;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
//MTK-START [mtk80601][111215][ALPS00093395] add SIMInfo
import android.provider.Telephony.SimInfo;
//MTK-END [mtk80601][111215][ALPS00093395] add SIMInfo
//MTK-START [mtk80601][111215]
import android.provider.Telephony.Carriers;
import android.provider.Telephony.GprsInfo;
import android.provider.Telephony.SIMInfo;
import android.provider.Settings;
import android.util.Config;
import android.os.SystemProperties;
import com.android.internal.telephony.Phone;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.ITelephonyProviderExt;
//MTK-START [mtk80601][111215]

public class TelephonyProvider extends ContentProvider
{
    private static final String DATABASE_NAME = "telephony.db";
    private static final boolean DBG = true;

    private static final int DATABASE_VERSION = 12 << 16;
    private static final int URL_TELEPHONY = 1;
    private static final int URL_CURRENT = 2;
    private static final int URL_ID = 3;
    private static final int URL_RESTOREAPN = 4;
    private static final int URL_PREFERAPN = 5;
    private static final int URL_PREFERAPN_NO_UPDATE = 6;

    // M: MTK added URL ID
    private static final int URL_PREFERTETHERINGAPN = 11;
    private static final int URL_TELEPHONY_DM = 21;
    private static final int URL_ID_DM = 22;
    private static final int URL_TELEPHONY_DM_GEMINI = 25;
    private static final int URL_ID_DM_GEMINI = 26;
    private static final int URL_SIMINFO = 101;
    private static final int URL_SIMINFO_ID = 102;
    private static final int URL_GPRSINFO = 1001;
    private static final int URL_GPRSINFO_ID = 1002;	


    private static final String TAG = "TelephonyProvider";
    private static final String CARRIERS_TABLE = "carriers";
//MTK-START [mtk04170][111215]

    private static final String CARRIERS_DM_TABLE = "carriers_dm";
    private static final String CARRIERS_DM_TABLE_GEMINI = "carriers_dm_gemini";
    private static final String GPRSINFO_TABLE = "gprsinfo";
//MTK-END [mtk04170][111215]

    private static final String PREF_FILE = "preferred-apn";
//MTK-START [mtk04170][111215]
    private static final String PREF_TETHERING_FILE = "preferred-tethering-apn";

//MTK-END [mtk04170][111215]
    private static final String COLUMN_APN_ID = "apn_id";
    private static final String APN_CONFIG_CHECKSUM = "apn_conf_checksum";
//MTK-START [mtk80601][111215][ALPS00093395] add SIMInfo
    private static final String SIMINFO_TABLE = "siminfo";
//MTK-END [mtk80601][111215][ALPS00093395] add SIMInfo
    private static final String PARTNER_APNS_PATH = "etc/apns-conf.xml";

    private static final UriMatcher s_urlMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final ContentValues s_currentNullMap;
    private static final ContentValues s_currentSetMap;
//MTK-START [mtk04170][111215]
    private static final int URL_MAIN_CARD = 1;
    private static final int URL_CARD_GEMINI[] = {1, 2, 3, 4};

    private static final String PREF_LOAD_APN = "load-apn";
    private static final String  PREF_LOAD_APN_SLOT_KEY[] = {"load_slot1_apn", "load_slot2_apn", "load_slot3_apn", "load_slot4_apn"};    
    private static final boolean mInitAPNGemini[] = {false, false, false, false};

    private Object mLock[] = {new Object(), new Object(), new Object(), new Object()};

    private static final String carriersUri[] = {"carriers_sim1", "carriers_sim2", "carriers_sim3", "carriers_sim4"};
    private static final String CARRIERS_TABLE_GEMINI[] = {CARRIERS_TABLE, "carriers_2", "carriers_3", "carriers_4"};
    private static final String PREF_FILE_GEMINI[] = {PREF_FILE, "preferred-apn-2", "preferred-apn-3", "preferred-apn-4"};
    private static final String PREF_TETHERING_FILE_GEMINI[] = {PREF_TETHERING_FILE, "preferred-tethering-apn-2", "preferred-tethering-apn-3", "preferred-tethering-apn-4"};
    private static final Uri	 URI_GEMINI[] = {Telephony.Carriers.SIM1Carriers.CONTENT_URI, 
            Telephony.Carriers.SIM2Carriers.CONTENT_URI, Telephony.Carriers.SIM3Carriers.CONTENT_URI, Telephony.Carriers.SIM4Carriers.CONTENT_URI};

    private static final UriMatcher s_urlMatcherGemini[] = new UriMatcher[PhoneConstants.GEMINI_SIM_NUM];
    private ITelephony iTelephony;
//MTK-END [mtk04170][111215]

    static {
        s_urlMatcher.addURI("telephony", "carriers", URL_TELEPHONY);
        s_urlMatcher.addURI("telephony", "carriers/current", URL_CURRENT);
        s_urlMatcher.addURI("telephony", "carriers/#", URL_ID);
        s_urlMatcher.addURI("telephony", "carriers/restore", URL_RESTOREAPN);
        s_urlMatcher.addURI("telephony", "carriers/preferapn", URL_PREFERAPN);
        s_urlMatcher.addURI("telephony", "carriers/preferapn_no_update", URL_PREFERAPN_NO_UPDATE);

        // M: MTK-START
        s_urlMatcher.addURI("telephony", "carriers/prefertetheringapn", URL_PREFERTETHERINGAPN);
		
        for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
            s_urlMatcherGemini[i] = new UriMatcher(UriMatcher.NO_MATCH);
            s_urlMatcherGemini[i].addURI("telephony", carriersUri[i] + "/", URL_TELEPHONY);
            s_urlMatcherGemini[i].addURI("telephony", carriersUri[i] + "/current", URL_CURRENT);
            s_urlMatcherGemini[i].addURI("telephony", carriersUri[i] + "/#", URL_ID);
            s_urlMatcherGemini[i].addURI("telephony", carriersUri[i] + "/restore", URL_RESTOREAPN);			
            s_urlMatcherGemini[i].addURI("telephony", carriersUri[i] + "/preferapn", URL_PREFERAPN);
            s_urlMatcherGemini[i].addURI("telephony", carriersUri[i] + "/preferapn_no_update", URL_PREFERAPN_NO_UPDATE);
            s_urlMatcherGemini[i].addURI("telephony", carriersUri[i] + "/prefertetheringapn", URL_PREFERTETHERINGAPN);
        }

        s_urlMatcher.addURI("telephony", "siminfo", URL_SIMINFO);
        s_urlMatcher.addURI("telephony", "siminfo/#", URL_SIMINFO_ID);
        s_urlMatcher.addURI("telephony", "gprsinfo", URL_GPRSINFO);
        s_urlMatcher.addURI("telephony", "gprsinfo/#", URL_GPRSINFO_ID);		
        // M: MTK-END

        // M: DM related URI
        s_urlMatcher.addURI("telephony", "carriers_dm", URL_TELEPHONY_DM); 
        s_urlMatcher.addURI("telephony", "carriers_dm/#", URL_ID_DM);
        s_urlMatcher.addURI("telephony", "carriers_dm_gemini", URL_TELEPHONY_DM_GEMINI); 
        s_urlMatcher.addURI("telephony", "carriers_dm_gemini/#", URL_ID_DM_GEMINI);

        s_currentNullMap = new ContentValues(1);
        s_currentNullMap.put("current", (Long) null);

        s_currentSetMap = new ContentValues(1);
        s_currentSetMap.put("current", "1");
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        // Context to access resources with
        private Context mContext;

        //MTK-START [mtk04170][111215]
        private SQLiteStatement mCarrierIntertStatement;
        private SQLiteStatement mCarrierIntertGeminiStatement;
        private ITelephonyProviderExt mTelephonyProviderExt;
        //MTK-END [mtk04170][111215] 

        /**
         * DatabaseHelper helper class for loading apns into a database.
         *
         * @param context of the user.
         */
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, getVersion(context));
            mContext = context;
            try{
                mTelephonyProviderExt = MediatekClassFactory.createInstance(ITelephonyProviderExt.class, mContext);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        private static int getVersion(Context context) {
            // Get the database version, combining a static schema version and the XML version
            Resources r = context.getResources();
            XmlResourceParser parser = r.getXml(com.android.internal.R.xml.apns);
            try {
                XmlUtils.beginDocument(parser, "apns");
                int publicversion = Integer.parseInt(parser.getAttributeValue(null, "version"));
                return DATABASE_VERSION | publicversion;
            } catch (Exception e) {
                Log.e(TAG, "Can't get version of APN database", e);
                return DATABASE_VERSION;
            } finally {
                parser.close();
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
//MTK-START [mtk80601][111215][ALPS00093395] add SIMInfo
            db.execSQL("CREATE TABLE " + SIMINFO_TABLE + " ( "
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + SimInfo.DISPLAY_NAME + " TEXT,"
                    + SimInfo.NAME_SOURCE + " INTEGER DEFAULT " + SimInfo.DEFAULT_SOURCE + ","
                    + SimInfo.NUMBER + " TEXT,"
                    + SimInfo.DISPLAY_NUMBER_FORMAT + " INTEGER NOT NULL DEFAULT "
                    + SimInfo.DISLPAY_NUMBER_DEFAULT + ","
                    + SimInfo.ICC_ID + " TEXT NOT NULL,"
                    + SimInfo.COLOR + " INTEGER DEFAULT " + SimInfo.COLOR_DEFAULT + ","
                    + SimInfo.SLOT + " INTEGER DEFAULT " + SimInfo.SLOT_NONE + ","
                    + SimInfo.OPERATOR + " TEXT,"
                    + SimInfo.WAP_PUSH + " INTEGER DEFAULT " + SimInfo.WAP_PUSH_DEFAULT + ","
                    + SimInfo.DATA_ROAMING + " INTEGER DEFAULT " + SimInfo.DATA_ROAMING_DEFAULT
                    + ");");
//MTK-END [mtk80601][111215][ALPS00093395] add SIMInfo
            // Set up the database schema

            //MTK-START [mtk04170][111215]
            db.execSQL("CREATE TABLE " + GPRSINFO_TABLE + " ( "
                    + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + GprsInfo.SIM_ID + " INTEGER REFERENCES siminfo(_id) NOT NULL,"
                    + GprsInfo.GPRS_IN + " INTEGER DEFAULT 0,"
                    + GprsInfo.GPRS_OUT + " INTEGER DEFAULT 0"
                    + ");");
            // Set up the database schema
            
            String columns;
            
            if (FeatureOption.MTK_OMACP_SUPPORT == true && FeatureOption.MTK_MVNO_SUPPORT) {
                columns = "(_id INTEGER PRIMARY KEY,"
                    + "name TEXT,"
                    + "numeric TEXT,"
                    + "mcc TEXT,"
                    + "mnc TEXT,"
                    + "apn TEXT,"
                    + "user TEXT,"
                    + "server TEXT,"
                    + "password TEXT,"
                    + "proxy TEXT,"
                    + "port TEXT,"
                    + "mmsproxy TEXT,"
                    + "mmsport TEXT,"
                    + "mmsc TEXT,"
                    + "authtype INTEGER,"
                    + "type TEXT,"
                    + "current INTEGER,"
                    + "sourcetype INTEGER,"
                    +"csdnum TEXT,"
                    + "protocol TEXT,"
                    + "roaming_protocol TEXT,"
                    + "omacpid TEXT,"
                    + "napid TEXT,"
                    + "proxyid TEXT,"
                    + "carrier_enabled BOOLEAN,"
                    + "bearer INTEGER,"
                    + "spn TEXT,"
                    + "imsi TEXT,"
                    + "pnn TEXT);";
            } else if (FeatureOption.MTK_MVNO_SUPPORT){
                columns = "(_id INTEGER PRIMARY KEY,"
                    + "name TEXT,"
                    + "numeric TEXT,"
                    + "mcc TEXT,"
                    + "mnc TEXT,"
                    + "apn TEXT,"
                    + "user TEXT,"
                    + "server TEXT,"
                    + "password TEXT,"
                    + "proxy TEXT,"
                    + "port TEXT,"
                    + "mmsproxy TEXT,"
                    + "mmsport TEXT,"
                    + "mmsc TEXT,"
                    + "authtype INTEGER,"
                    + "type TEXT,"
                    + "current INTEGER,"
                    + "sourcetype INTEGER,"
                    + "csdnum TEXT,"
                    + "protocol TEXT,"
                    + "roaming_protocol TEXT,"
                    + "carrier_enabled BOOLEAN,"
                    + "bearer INTEGER,"
                    + "spn TEXT,"
                    + "imsi TEXT,"
                    + "pnn TEXT);";
            } else if (FeatureOption.MTK_OMACP_SUPPORT == true) {
                columns = "(_id INTEGER PRIMARY KEY,"
                    + "name TEXT,"
                    + "numeric TEXT,"
                    + "mcc TEXT,"
                    + "mnc TEXT,"
                    + "apn TEXT,"
                    + "user TEXT,"
                    + "server TEXT,"
                    + "password TEXT,"
                    + "proxy TEXT,"
                    + "port TEXT,"
                    + "mmsproxy TEXT,"
                    + "mmsport TEXT,"
                    + "mmsc TEXT,"
                    + "authtype INTEGER,"
                    + "type TEXT,"
                    + "current INTEGER,"
                    + "sourcetype INTEGER,"
                    +"csdnum TEXT,"
                    + "protocol TEXT,"
                    + "roaming_protocol TEXT,"
                    + "omacpid TEXT,"
                    + "napid TEXT,"
                    + "proxyid TEXT,"
                    + "carrier_enabled BOOLEAN,"
                    + "bearer INTEGER);";

            } else {
                columns = "(_id INTEGER PRIMARY KEY,"
                    + "name TEXT,"
                    + "numeric TEXT,"
                    + "mcc TEXT,"
                    + "mnc TEXT,"
                    + "apn TEXT,"
                    + "user TEXT,"
                    + "server TEXT,"
                    + "password TEXT,"
                    + "proxy TEXT,"
                    + "port TEXT,"
                    + "mmsproxy TEXT,"
                    + "mmsport TEXT,"
                    + "mmsc TEXT,"
                    + "authtype INTEGER,"
                    + "type TEXT,"
                    + "current INTEGER,"
                    + "sourcetype INTEGER,"
                    +"csdnum TEXT,"
                    + "protocol TEXT,"
                    + "roaming_protocol TEXT,"
                    + "carrier_enabled BOOLEAN,"
                    + "bearer INTEGER);";
            }

            for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                db.execSQL("CREATE TABLE " + CARRIERS_TABLE_GEMINI[i] + columns);
            }

            db.execSQL("CREATE TABLE " + CARRIERS_DM_TABLE + columns);
            db.execSQL("CREATE TABLE " + CARRIERS_DM_TABLE_GEMINI + columns);
        }

//MTK-START [mtk04170][111215]
        public void initDatabase(SQLiteDatabase db, String table) {
//MTK-END [mtk04170][111215]
            // Read internal APNS data
            Resources r = mContext.getResources();
            XmlResourceParser parser = r.getXml(com.android.internal.R.xml.apns);
            int publicversion = -1;
            try {
                XmlUtils.beginDocument(parser, "apns");
                publicversion = Integer.parseInt(parser.getAttributeValue(null, "version"));
//MTK-START [mtk04170][111215]
                loadApns(db, parser, table);
//MTK-END [mtk04170][111215]
            } catch (Exception e) {
                Log.e(TAG, "Got exception while loading APN database.", e);
            } finally {
                parser.close();
            }

           // Read external APNS data (partner-provided)
            XmlPullParser confparser = null;
            // Environment.getRootDirectory() is a fancy way of saying ANDROID_ROOT or "/system".
            File confFile = new File(Environment.getRootDirectory(), PARTNER_APNS_PATH);
            FileReader confreader = null;
            try {
                confreader = new FileReader(confFile);
                confparser = Xml.newPullParser();
                confparser.setInput(confreader);
                XmlUtils.beginDocument(confparser, "apns");

                // Sanity check. Force internal version and confidential versions to agree
                int confversion = Integer.parseInt(confparser.getAttributeValue(null, "version"));
                if (publicversion != confversion) {
                    throw new IllegalStateException("Internal APNS file version doesn't match "
                            + confFile.getAbsolutePath());
                }
//MTK-START [mtk04170][111215]
                db.beginTransaction();
                try {
                	loadApns(db, confparser, table);
                	db.setTransactionSuccessful();
                } finally {
                	db.endTransaction();
                }
//MTK-END [mtk04170][111215]
            } catch (FileNotFoundException e) {
                // It's ok if the file isn't found. It means there isn't a confidential file
                // Log.e(TAG, "File not found: '" + confFile.getAbsolutePath() + "'");
            } catch (Exception e) {
                Log.e(TAG, "Exception while parsing '" + confFile.getAbsolutePath() + "'", e);
            } finally {
                try { if (confreader != null) confreader.close(); } catch (IOException e) { }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < (5 << 16 | 6)) {
                // 5 << 16 is the Database version and 6 in the xml version.

                // This change adds a new authtype column to the database.
                // The auth type column can have 4 values: 0 (None), 1 (PAP), 2 (CHAP)
                // 3 (PAP or CHAP). To avoid breaking compatibility, with already working
                // APNs, the unset value (-1) will be used. If the value is -1.
                // the authentication will default to 0 (if no user / password) is specified
                // or to 3. Currently, there have been no reported problems with
                // pre-configured APNs and hence it is set to -1 for them. Similarly,
                // if the user, has added a new APN, we set the authentication type
                // to -1.

                // M: For Single SIM and MTK Gemini support
                for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                    db.execSQL("ALTER TABLE " + CARRIERS_TABLE_GEMINI[i]
                            + " ADD COLUMN authtype INTEGER DEFAULT -1;");
                }
                oldVersion = 5 << 16 | 6;
            }
            if (oldVersion < (6 << 16 | 6)) {
                // Add protcol fields to the APN. The XML file does not change.

                // M: For Single SIM and MTK Gemini support
                for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                    db.execSQL("ALTER TABLE " + CARRIERS_TABLE_GEMINI[i] +
                            " ADD COLUMN protocol TEXT DEFAULT IP;");
                    db.execSQL("ALTER TABLE " + CARRIERS_TABLE_GEMINI[i] +
                            " ADD COLUMN roaming_protocol TEXT DEFAULT IP;");
                }
                oldVersion = 6 << 16 | 6;
            }
            if (oldVersion < (7 << 16 | 6)) {
//MTK-START [mtk80601][111215][ALPS00093395] add SIMInfo
                db.execSQL("ALTER TABLE " + SIMINFO_TABLE +
                " ADD COLUMN wap_push INTEGER DEFAULT -1;");
//MTK-END [mtk80601][111215][ALPS00093395] add SIMInfo
                oldVersion = 7 << 16 | 6;
            }
            if (oldVersion < (8 << 16 | 6)) {
                // Add protcol fields to the APN. The XML file does not change.

                // M: For Single SIM and MTK Gemini support
                for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                    db.execSQL("ALTER TABLE " + CARRIERS_TABLE_GEMINI[i] +
                            " ADD COLUMN carrier_enabled BOOLEAN DEFAULT 1;");
                    db.execSQL("ALTER TABLE " + CARRIERS_TABLE_GEMINI[i] +
                            " ADD COLUMN bearer INTEGER DEFAULT 0;");
                }
                int size = Telephony.SIMBackgroundRes.length;
                Log.e(TAG, "Update GB to ICS, color size " + size);
                db.execSQL("UPDATE " + SIMINFO_TABLE + 
                        " SET " + SimInfo.COLOR + "=" + SimInfo.COLOR + "%" + size + ";");
                oldVersion = 8 << 16 | 6;
            }
            if (oldVersion < (9 << 16 | 6)) {
                // Add MVNO support columns
                if (FeatureOption.MTK_MVNO_SUPPORT) {
                    // M: For Single SIM and MTK Gemini support
                    for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                        try {

                            db.execSQL("ALTER TABLE " + CARRIERS_TABLE_GEMINI[i] +
                                    " ADD COLUMN spn TEXT;");
                            db.execSQL("ALTER TABLE " + CARRIERS_TABLE_GEMINI[i] +
                                    " ADD COLUMN imsi TEXT;");
                            Log.d(TAG, "Update ICS to JB, add MVNO columns");
                        } catch  (SQLException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Add MVNO columns fail with table " + CARRIERS_TABLE_GEMINI[i] + ".");
                        }
                    }
                }
                oldVersion = 9 << 16 | 6;
            }
            
            if (oldVersion < (10 << 16 | 6)) {
                // Add new column which decribe the source of sim display name
                try {
                db.execSQL("ALTER TABLE " + SIMINFO_TABLE +
                        " ADD COLUMN name_source INTEGER DEFAULT 0;");

                Log.d(TAG, "Update JB, add SIMInfo name_source columns");
                } catch  (SQLException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Add SIMInfo name_source columns fail.");
                }
                oldVersion = 10 << 16 | 6;
            }
            if (oldVersion < (11 << 16 | 6)) {
                // Add MVNO support columns
                if (FeatureOption.MTK_MVNO_SUPPORT) {
                    // M: For Single SIM and MTK Gemini support
                    for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                        try {

                            db.execSQL("ALTER TABLE " + CARRIERS_TABLE_GEMINI[i] +
                                    " ADD COLUMN pnn TEXT;");
                            Log.d(TAG, "Update ICS to JB, add MVNO columns");
                        } catch  (SQLException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Add MVNO columns fail with table " + CARRIERS_TABLE_GEMINI[i] + ".");
                        }
                    }
                }
                oldVersion = 11 << 16 | 6;
            }
            if (oldVersion < (12 << 16 | 6)) {
                // Add new column which decribe the operator
                try {
                    db.execSQL("ALTER TABLE " + SIMINFO_TABLE +
                            " ADD COLUMN operator TEXT;");

                    Log.d(TAG, "Update JB2, add SIMInfo operator columns");
                } catch  (SQLException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Add SIMInfo operator columns fail.");
                }
                oldVersion = 12 << 16 | 6;
            }
        }

        /**
         * Gets the next row of apn values.
         *
         * @param parser the parser
         * @return the row or null if it's not an apn
         */
        private ContentValues getRow(XmlPullParser parser) {
            if (!"apn".equals(parser.getName())) {
                return null;
            }

            ContentValues map = new ContentValues();

            String mcc = parser.getAttributeValue(null, "mcc");
            String mnc = parser.getAttributeValue(null, "mnc");
            String numeric = mcc + mnc;

            map.put(Telephony.Carriers.NUMERIC,numeric);
            map.put(Telephony.Carriers.MCC, mcc);
            map.put(Telephony.Carriers.MNC, mnc);
            map.put(Telephony.Carriers.NAME, parser.getAttributeValue(null, "carrier"));
            map.put(Telephony.Carriers.APN, parser.getAttributeValue(null, "apn"));
            map.put(Telephony.Carriers.USER, parser.getAttributeValue(null, "user"));
            map.put(Telephony.Carriers.SERVER, parser.getAttributeValue(null, "server"));
            map.put(Telephony.Carriers.PASSWORD, parser.getAttributeValue(null, "password"));
//MTK-START [mtk04170][111215]
            map.put(Telephony.Carriers.SOURCE_TYPE, 0);
//MTK-END [mtk04170][111215]

            // do not add NULL to the map so that insert() will set the default value
            String proxy = parser.getAttributeValue(null, "proxy");
            if (proxy != null) {
                map.put(Telephony.Carriers.PROXY, proxy);
            }
            String port = parser.getAttributeValue(null, "port");
            if (port != null) {
                map.put(Telephony.Carriers.PORT, port);
            }
            String mmsproxy = parser.getAttributeValue(null, "mmsproxy");
            if (mmsproxy != null) {
                map.put(Telephony.Carriers.MMSPROXY, mmsproxy);
            }
            String mmsport = parser.getAttributeValue(null, "mmsport");
            if (mmsport != null) {
                map.put(Telephony.Carriers.MMSPORT, mmsport);
            }
            map.put(Telephony.Carriers.MMSC, parser.getAttributeValue(null, "mmsc"));
            String type = parser.getAttributeValue(null, "type");
            if (type != null) {
                map.put(Telephony.Carriers.TYPE, type);
            }

            String auth = parser.getAttributeValue(null, "authtype");
            if (auth != null) {
                map.put(Telephony.Carriers.AUTH_TYPE, Integer.parseInt(auth));
            }

            String protocol = parser.getAttributeValue(null, "protocol");
            if (protocol != null) {
                map.put(Telephony.Carriers.PROTOCOL, protocol);
            }

            String roamingProtocol = parser.getAttributeValue(null, "roaming_protocol");
            if (roamingProtocol != null) {
                map.put(Telephony.Carriers.ROAMING_PROTOCOL, roamingProtocol);
            }

            String carrierEnabled = parser.getAttributeValue(null, "carrier_enabled");
            if (carrierEnabled != null) {
                map.put(Telephony.Carriers.CARRIER_ENABLED, Boolean.parseBoolean(carrierEnabled));
            }

            String bearer = parser.getAttributeValue(null, "bearer");
            if (bearer != null) {
                map.put(Telephony.Carriers.BEARER, Integer.parseInt(bearer));
            }

            if (FeatureOption.MTK_MVNO_SUPPORT) {
                String spn = parser.getAttributeValue(null, "spn");
                if (spn != null) {
                    map.put(Telephony.Carriers.SPN, spn);
                }
                String imsi = parser.getAttributeValue(null, "imsi");
                if (imsi != null) {
                    map.put(Telephony.Carriers.IMSI, imsi);
                }
                String pnn = parser.getAttributeValue(null, "pnn");
                if (pnn != null) {
                    map.put(Telephony.Carriers.PNN, pnn);
                }
            }

            return map;
        }

        /*
         * Loads apns from xml file into the database
         *
         * @param db the sqlite database to write to
         * @param parser the xml parser
         *
         */
//MTK-START [mtk04170][111215]
        private void loadApns(SQLiteDatabase db, XmlPullParser parser,
                String table) {
//MTK-END [mtk04170][111215]
            if (parser != null) {
                try {
                    while (true) {
                        XmlUtils.nextElement(parser);

                        ContentValues row = getRow(parser);
                        if (row != null) {
                            // Add operator customized configuration in onLoadApns if need
                            mTelephonyProviderExt.onLoadApns(row);
							
                            if (!FeatureOption.MTK_MVNO_SUPPORT && 
                                    (row.containsKey(Telephony.Carriers.SPN) ||
                                    row.containsKey(Telephony.Carriers.IMSI) ||
                                    row.containsKey(Telephony.Carriers.PNN))) {
                                    // If it is not MVNO support, should not load MVNO profiles into DB.
                                    continue;
                            }
							
                            insertAddingDefaults(db, table, row);
                        } else {
                            break;  // do we really want to skip the rest of the file?
                        }
                    }
                } catch (XmlPullParserException e)  {
                    Log.e(TAG, "Got execption while getting perferred time zone.", e);
                } catch (IOException e) {
                    Log.e(TAG, "Got execption while getting perferred time zone.", e);
                }
            }
        }

        private void insertAddingDefaults(SQLiteDatabase db, String table, ContentValues row) {
            // Initialize defaults if any
            if (row.containsKey(Telephony.Carriers.AUTH_TYPE) == false) {
                row.put(Telephony.Carriers.AUTH_TYPE, -1);
            }
            if (row.containsKey(Telephony.Carriers.PROTOCOL) == false) {
                row.put(Telephony.Carriers.PROTOCOL, "IP");
            }
            if (row.containsKey(Telephony.Carriers.ROAMING_PROTOCOL) == false) {
                row.put(Telephony.Carriers.ROAMING_PROTOCOL, "IP");
            }
            if (row.containsKey(Telephony.Carriers.CARRIER_ENABLED) == false) {
                row.put(Telephony.Carriers.CARRIER_ENABLED, true);
            }
            if (row.containsKey(Telephony.Carriers.BEARER) == false) {
                row.put(Telephony.Carriers.BEARER, 0);
            }
//MTK-START [mtk04170][111215]

            
            
            if (CARRIERS_TABLE.equals(table)) {
                if (FeatureOption.MTK_OMACP_SUPPORT && FeatureOption.MTK_MVNO_SUPPORT) {
                    if (mCarrierIntertStatement == null) {
                        mCarrierIntertStatement = db.compileStatement(ReplaceSqlStatement.CARRIER_MVNO_OMACP_INSERT_SQL);
                    }
                } else if (FeatureOption.MTK_MVNO_SUPPORT) {
                     if (mCarrierIntertStatement == null) {
                        mCarrierIntertStatement = db.compileStatement(ReplaceSqlStatement.CARRIER_MVNO_INSERT_SQL);
                    }
                } else if (FeatureOption.MTK_OMACP_SUPPORT) {
                    if (mCarrierIntertStatement == null) {
                        mCarrierIntertStatement = db.compileStatement(ReplaceSqlStatement.CARRIER_OMACP_INSERT_SQL);
                    }
                } else {
                    if (mCarrierIntertStatement == null) {
                        mCarrierIntertStatement = db.compileStatement(ReplaceSqlStatement.CARRIER_INSERT_SQL);
                    }
                }
                bindStatement(mCarrierIntertStatement, row);
            } else if (CARRIERS_TABLE_GEMINI.equals(table)) {
                if (FeatureOption.MTK_OMACP_SUPPORT && FeatureOption.MTK_MVNO_SUPPORT) {
                    if (mCarrierIntertGeminiStatement == null) {
                        mCarrierIntertGeminiStatement = db.compileStatement(ReplaceSqlStatement.CARRIER_MVNO_OMACP_GEMINI_INSERT_SQL);
                    }
                } else if (FeatureOption.MTK_MVNO_SUPPORT) {
                     if (mCarrierIntertGeminiStatement == null) {
                        mCarrierIntertGeminiStatement = db.compileStatement(ReplaceSqlStatement.CARRIER_MVNO_GEMINI_INSERT_SQL);
                    }
                } else if (FeatureOption.MTK_OMACP_SUPPORT) {
                    if (mCarrierIntertGeminiStatement == null) {
                        mCarrierIntertGeminiStatement = db.compileStatement(ReplaceSqlStatement.CARRIER_OMACP_GEMINI_INSERT_SQL);
                    }
                } else {
                    if (mCarrierIntertGeminiStatement == null) {
                        mCarrierIntertGeminiStatement = db.compileStatement(ReplaceSqlStatement.CARRIER_GEMINI_INSERT_SQL);
                    }
                }
                bindStatement(mCarrierIntertGeminiStatement, row);
            } else {
                db.insert(table, null, row);
            }
            
//MTK-END [mtk04170][111215]
        }
        
        private void bindStatement(SQLiteStatement ss, ContentValues row) {
            if (ss == null) {
                Log.e(TAG, "SQLiteStatement should not be null!");
                return;
            }
            String name = row.getAsString(Carriers.NAME);
            if (name != null) {
                ss.bindString(ReplaceSqlStatement.NAME, name);
            } else {
                ss.bindNull(ReplaceSqlStatement.NAME);
            }
            String numeric = row.getAsString(Carriers.NUMERIC);
            if (numeric != null) {
                ss.bindString(ReplaceSqlStatement.NUMERIC, numeric);
            } else {
                ss.bindNull(ReplaceSqlStatement.NUMERIC);
            }
            String mcc = row.getAsString(Carriers.MCC);
            if (mcc != null) {
                ss.bindString(ReplaceSqlStatement.MCC, mcc);
            } else {
                ss.bindNull(ReplaceSqlStatement.MCC);
            }
            String mnc = row.getAsString(Carriers.MNC);
            if (mnc != null) {
                ss.bindString(ReplaceSqlStatement.MNC, mnc);
            } else {
                ss.bindNull(ReplaceSqlStatement.MNC);
            }
            String apn = row.getAsString(Carriers.APN);
            if (apn != null) {
                ss.bindString(ReplaceSqlStatement.APN, apn);
            } else {
                ss.bindNull(ReplaceSqlStatement.APN);
            }
            String user = row.getAsString(Carriers.USER);
            if (user != null) {
                ss.bindString(ReplaceSqlStatement.USER, user);
            } else {
                ss.bindNull(ReplaceSqlStatement.USER);
            }
            String server = row.getAsString(Carriers.SERVER);
            if (server != null) {
                ss.bindString(ReplaceSqlStatement.SERVER, server);
            } else {
                ss.bindNull(ReplaceSqlStatement.SERVER);
            }
            String password = row.getAsString(Carriers.PASSWORD);
            if (password != null) {
                ss.bindString(ReplaceSqlStatement.PASSWORD, password);
            } else {
                ss.bindNull(ReplaceSqlStatement.PASSWORD);
            }
            String proxy = row.getAsString(Carriers.PROXY);
            if (proxy != null) {
                ss.bindString(ReplaceSqlStatement.PROXY, proxy);
            } else {
                ss.bindNull(ReplaceSqlStatement.PROXY);
            }
            String port = row.getAsString(Carriers.PORT);
            if (port != null) {
                ss.bindString(ReplaceSqlStatement.PORT, port);
            } else {
                ss.bindNull(ReplaceSqlStatement.PORT);
            }
            String mmsproxy = row.getAsString(Carriers.MMSPROXY);
            if (mmsproxy != null) {
                ss.bindString(ReplaceSqlStatement.MMSPROXY, mmsproxy);
            } else {
                ss.bindNull(ReplaceSqlStatement.MMSPROXY);
            }
            String mmsport = row.getAsString(Carriers.MMSPORT);
            if (mmsport != null) {
                ss.bindString(ReplaceSqlStatement.MMSPORT, mmsport);
            } else {
                ss.bindNull(ReplaceSqlStatement.MMSPORT);
            }
            String mmsc = row.getAsString(Carriers.MMSC);
            if (mmsc != null) {
                ss.bindString(ReplaceSqlStatement.MMSC, mmsc);
            } else {
                ss.bindNull(ReplaceSqlStatement.MMSC);
            }
            Integer authtype = row.getAsInteger(Carriers.AUTH_TYPE);
            if (authtype != null) {
                ss.bindLong(ReplaceSqlStatement.AUTH_TYPE, authtype);
            } else {
                ss.bindLong(ReplaceSqlStatement.AUTH_TYPE, -1);
            }
            String type = row.getAsString(Carriers.TYPE);
            if (type != null) {
                ss.bindString(ReplaceSqlStatement.TYPE, type);
            } else {
                ss.bindNull(ReplaceSqlStatement.TYPE);
            }
            Integer current = row.getAsInteger(Carriers.CURRENT);
            if (current != null) {
                ss.bindLong(ReplaceSqlStatement.CURRENT, current);
            } else {
                ss.bindNull(ReplaceSqlStatement.CURRENT);
            }
//            Integer sourcetype = row.getAsInteger(Carriers.SOURCE_TYPE);
//            if (sourcetype != null) {
//                ss.bindLong(ReplaceSqlStatement.SOURCE_TYPE, sourcetype);
//            } else {
            ss.bindLong(ReplaceSqlStatement.SOURCE_TYPE, 0);
//            }
            String csdnum = row.getAsString(Carriers.CSD_NUM);
            if (csdnum != null) {
                ss.bindString(ReplaceSqlStatement.CSD_NUM, csdnum);
            } else {
                ss.bindNull(ReplaceSqlStatement.CSD_NUM);
            }
            String protocol = row.getAsString(Carriers.PROTOCOL);
            if (protocol != null) {
                ss.bindString(ReplaceSqlStatement.PROTOCOL, protocol);
            } else {
                ss.bindString(ReplaceSqlStatement.PROTOCOL, "IP");
            }
            String roaming_protocol = row.getAsString(Carriers.ROAMING_PROTOCOL);
            if (roaming_protocol != null) {
                ss.bindString(ReplaceSqlStatement.ROAMING_PROTOCOL, roaming_protocol);
            } else {
                ss.bindString(ReplaceSqlStatement.ROAMING_PROTOCOL, "IP");
            }
            Boolean carrier_enabled = row.getAsBoolean(Carriers.CARRIER_ENABLED);
            Integer bearer = row.getAsInteger(Carriers.BEARER);
            
            if (FeatureOption.MTK_OMACP_SUPPORT) {
                String omacpid = row.getAsString(Carriers.OMACPID);
                if (omacpid != null) {
                    ss.bindString(ReplaceSqlStatement.OMACP_OMACPID, omacpid);
                } else {
                    ss.bindNull(ReplaceSqlStatement.OMACP_OMACPID);
                }
                String napid = row.getAsString(Carriers.NAPID);
                if (napid != null) {
                    ss.bindString(ReplaceSqlStatement.OMACP_NAPID, napid);
                } else {
                    ss.bindNull(ReplaceSqlStatement.OMACP_NAPID);
                }
                String proxyid = row.getAsString(Carriers.PROXYID);
                if (proxyid != null) {
                    ss.bindString(ReplaceSqlStatement.OMACP_PROXYID, proxyid);
                } else {
                    ss.bindNull(ReplaceSqlStatement.OMACP_PROXYID);
                }
                
                if (carrier_enabled != null) {
                    ss.bindLong(ReplaceSqlStatement.OMACP_CARRIER_ENABLED, carrier_enabled.booleanValue() ? 1 : 0);
                } else {
                    ss.bindLong(ReplaceSqlStatement.OMACP_CARRIER_ENABLED, 1);
                }
                
                if (bearer != null) {
                    ss.bindLong(ReplaceSqlStatement.OMACP_BEARER, bearer);
                } else {
                    ss.bindLong(ReplaceSqlStatement.OMACP_BEARER, 0);
                }

                if (FeatureOption.MTK_MVNO_SUPPORT) {
                    String spn = row.getAsString(Carriers.SPN);
                    if (spn != null) {
                        ss.bindString(ReplaceSqlStatement.OMACP_SPN, spn);
                    } else {
                        ss.bindNull(ReplaceSqlStatement.OMACP_SPN);
                    }
                    String imsi = row.getAsString(Carriers.IMSI);
                    if (imsi != null) {
                        ss.bindString(ReplaceSqlStatement.OMACP_IMSI, imsi);
                    } else {
                        ss.bindNull(ReplaceSqlStatement.OMACP_IMSI);
                    }
                    String pnn = row.getAsString(Carriers.PNN);
                    if (pnn != null) {
                        ss.bindString(ReplaceSqlStatement.OMACP_PNN, pnn);
                    } else {
                        ss.bindNull(ReplaceSqlStatement.OMACP_PNN);
                    }
                }
            } else {
                if (carrier_enabled != null) {
                    ss.bindLong(ReplaceSqlStatement.CARRIER_ENABLED, carrier_enabled.booleanValue() ? 1 : 0);
                } else {
                    ss.bindLong(ReplaceSqlStatement.CARRIER_ENABLED, 1);
                }
                
                if (bearer != null) {
                    ss.bindLong(ReplaceSqlStatement.BEARER, bearer);
                } else {
                    ss.bindLong(ReplaceSqlStatement.BEARER, 0);
                }

                if (FeatureOption.MTK_MVNO_SUPPORT) {
                    String spn = row.getAsString(Carriers.SPN);
                    if (spn != null) {
                        ss.bindString(ReplaceSqlStatement.SPN, spn);
                    } else {
                        ss.bindNull(ReplaceSqlStatement.SPN);
                    }
                    String imsi = row.getAsString(Carriers.IMSI);
                    if (imsi != null) {
                        ss.bindString(ReplaceSqlStatement.IMSI, imsi);
                    } else {
                        ss.bindNull(ReplaceSqlStatement.IMSI);
                    }
                    String pnn = row.getAsString(Carriers.PNN);
                    if (pnn != null) {
                        ss.bindString(ReplaceSqlStatement.PNN, pnn);
                    } else {
                        ss.bindNull(ReplaceSqlStatement.PNN);
                    }
                }
            }
            ss.executeInsert();
        }
    }

    @Override
    public boolean onCreate() {
        long oldCheckSum = getAPNConfigCheckSum();
        File confFile = new File(Environment.getRootDirectory(), PARTNER_APNS_PATH);
        long newCheckSum = -1L;

        if (DBG) {
            Log.w(TAG, "onCreate: confFile=" + confFile.getAbsolutePath() +
                    " oldCheckSum=" + oldCheckSum);
        }
        mOpenHelper = new DatabaseHelper(getContext());

        // M: MTK modified
        SharedPreferences sp = getContext().getSharedPreferences(PREF_LOAD_APN, Context.MODE_PRIVATE);
        for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
            mInitAPNGemini[i] = sp.getBoolean(PREF_LOAD_APN_SLOT_KEY[i], false);
            Log.i(TAG, "mInitAPNGemini[" + i +"]: " + mInitAPNGemini[i]);
        }
        iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));		

        if (isLteOnCdma()) {
            // Check to see if apns-conf.xml file changed. If so, generate db again.
            //
            // TODO: Generalize so we can handle apns-conf.xml updates
            // and preserve any modifications the user might make. For
            // now its safe on LteOnCdma devices because the user cannot
            // make changes.
            try {
                newCheckSum = FileUtils.checksumCrc32(confFile);
                if (DBG) Log.w(TAG, "onCreate: newCheckSum=" + newCheckSum);
                if (oldCheckSum != newCheckSum) {
                    Log.w(TAG, "Rebuilding Telephony.db");

                    // M: For Single SIM and MTK Gemini support
                    for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                        restoreDefaultAPN(PREF_FILE_GEMINI[i], CARRIERS_TABLE_GEMINI[i]);
                    }
					
                    setAPNConfigCheckSum(newCheckSum);
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "FileNotFoundException: '" + confFile.getAbsolutePath() + "'", e);
            } catch (IOException e) {
                Log.e(TAG, "IOException: '" + confFile.getAbsolutePath() + "'", e);
            }
        }
        return true;
    }

    private boolean isLteOnCdma() {
        return BaseCommands.getLteOnCdmaModeStatic() == PhoneConstants.LTE_ON_CDMA_TRUE;
    }

//MTK-START [mtk04170][111215]
    private void setPreferredApnId(String file, Long id) {
        SharedPreferences sp = getContext().getSharedPreferences(file,
                Context.MODE_PRIVATE);
//MTK-END [mtk04170][111215]
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(COLUMN_APN_ID, id != null ? id.longValue() : -1);
        editor.apply();
    }

//MTK-START [mtk04170][111215]
    private long getPreferredApnId(String file) {
        SharedPreferences sp = getContext().getSharedPreferences(file,
                Context.MODE_PRIVATE);
//MTK-END [mtk04170][111215]
        return sp.getLong(COLUMN_APN_ID, -1);
    }

    private long getAPNConfigCheckSum() {
        SharedPreferences sp = getContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        return sp.getLong(APN_CONFIG_CHECKSUM, -1);
    }

    private void setAPNConfigCheckSum(long id) {
        SharedPreferences sp = getContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(APN_CONFIG_CHECKSUM, id);
        editor.apply();
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // M: MTK modified
        initDatabaseIfNeeded(url);

        int match = s_urlMatcher.match(url);
        int simId = PhoneConstants.GEMINI_SIM_1;

        // M: For Gemini support
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (match != UriMatcher.NO_MATCH) {
                simId = getPreferSimdId();
            } else {
                for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                    match = s_urlMatcherGemini[i].match(url);
                    if (match != UriMatcher.NO_MATCH) {
                        simId = i;
                        break;
                    }
                }
            }
            if (DBG) Log.d(TAG, "query(): match=" + match + ", simId=" + simId);
        }

        switch (match) {
            case URL_TELEPHONY: {
                // M: For Gemini support
                qb.setTables(CARRIERS_TABLE_GEMINI[simId]);
                break;
            }
            case URL_CURRENT: {
                // M: For Gemini support
                qb.setTables(CARRIERS_TABLE_GEMINI[simId]);
                qb.appendWhere("current IS NOT NULL");
                break;
            }
            case URL_ID: {
                // M: For Gemini support
                qb.setTables(CARRIERS_TABLE_GEMINI[simId]);
                qb.appendWhere("_id = " + url.getPathSegments().get(1));
                break;
            }
            case URL_PREFERAPN:
            case URL_PREFERAPN_NO_UPDATE: {
                // M: For Gemini support
                qb.setTables(CARRIERS_TABLE_GEMINI[simId]);
                qb.appendWhere("_id = " + getPreferredApnId(PREF_FILE_GEMINI[simId]));
                break;
            }

            // M: MTK tethering support
            case URL_PREFERTETHERINGAPN: {
                qb.setTables(CARRIERS_TABLE_GEMINI[simId]);
                qb.appendWhere("_id = " + getPreferredApnId(PREF_TETHERING_FILE_GEMINI[simId]));
                break;
            }

            // M: Gemini enhancement support
            case URL_SIMINFO: {
                qb.setTables(SIMINFO_TABLE);
                break;
            }
            case URL_SIMINFO_ID: {
                qb.setTables(SIMINFO_TABLE);
                qb.appendWhere(SimInfo._ID + "=" + ContentUris.parseId(url));
                break;
            }
            case URL_GPRSINFO: {
                qb.setTables(GPRSINFO_TABLE);
                break;
            }
            case URL_GPRSINFO_ID: {
                qb.setTables(GPRSINFO_TABLE);
                qb.appendWhere(GprsInfo._ID + "=" + ContentUris.parseId(url));
                break;
            }
            case URL_TELEPHONY_DM: {
                qb.setTables(CARRIERS_DM_TABLE);
                break;
            }
            case URL_TELEPHONY_DM_GEMINI: {
                qb.setTables(CARRIERS_DM_TABLE_GEMINI);
                // qb.appendWhere("simid = 1"); // add by LY 07-08
                break;
            }
            case URL_ID_DM: {
                qb.setTables(CARRIERS_DM_TABLE);
                qb.appendWhere("_id = " + url.getPathSegments().get(1));
                break;
            }
            case URL_ID_DM_GEMINI: {
                qb.setTables(CARRIERS_DM_TABLE_GEMINI);
                qb.appendWhere("_id = " + url.getPathSegments().get(1));
                break;
            }

            default: {
                return null;
            }
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs, null, null, sort);
        ret.setNotificationUri(getContext().getContentResolver(), url);
        return ret;
    }

//MTK-START [mtk04170][111215]
    public void initDatabaseIfNeeded(Uri uri) {
        Log.i(TAG,"initDatabaseIfNeeded begin " + uri);
        String table = null;

        int match = s_urlMatcher.match(uri);
        int simId = PhoneConstants.GEMINI_SIM_1;

        // M: For Gemini support
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (match != UriMatcher.NO_MATCH) {
                simId = getPreferSimdId();
            } else {
                for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                    match = s_urlMatcherGemini[i].match(uri);
                    if (match != UriMatcher.NO_MATCH) {
                        simId = i;
                        break;
                    }
                }
            }
            if (DBG) Log.d(TAG, "initDatabaseIfNeeded(): match=" + match + ", simId=" + simId);
        }

        if (match != UriMatcher.NO_MATCH) {
            table = CARRIERS_TABLE_GEMINI[simId];
        }
        
        if (table != null) {
            final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            final SharedPreferences sp = getContext().getSharedPreferences(PREF_LOAD_APN,
                    Context.MODE_PRIVATE);
            final String initTable = table;

            Log.i(TAG, " mInitAPNGemini[" + simId +"]: " + mInitAPNGemini[simId] + ",uri" + uri);
            synchronized (mLock[simId]) {
                Log.i(TAG, " mInitAPNGemini[" + simId +"] unlock: " + mInitAPNGemini[simId] + ",uri" + uri);
                if (!mInitAPNGemini[simId]) {
                    SharedPreferences.Editor editor = sp.edit();
                    ((DatabaseHelper) mOpenHelper).initDatabase(db, initTable);
                    editor.putBoolean(PREF_LOAD_APN_SLOT_KEY[simId], true);
                    editor.apply();
                    mInitAPNGemini[simId] = true;
                }
            }	
        }
        Log.i(TAG,"initDatabaseIfNeeded end " + uri);
    }
//MTK-END [mtk04170][111215]

    @Override
    public String getType(Uri url)
    {
        int match = s_urlMatcher.match(url);
        // M: MTK Gemini support
        if (FeatureOption.MTK_GEMINI_SUPPORT &&
                match == UriMatcher.NO_MATCH) {
            for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                match = s_urlMatcherGemini[i].match(url);
                if (match != UriMatcher.NO_MATCH) break;
            }
        }
		
        switch (match) {
        case URL_TELEPHONY:
            return "vnd.android.cursor.dir/telephony-carrier";

        case URL_ID:
            return "vnd.android.cursor.item/telephony-carrier";

        case URL_PREFERAPN:
        case URL_PREFERAPN_NO_UPDATE:
            return "vnd.android.cursor.item/telephony-carrier";

        default:
            throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues)
    {
        Uri result = null;

        checkPermission();

        // M: MTK modified
        initDatabaseIfNeeded(url);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = s_urlMatcher.match(url);
        int simId = PhoneConstants.GEMINI_SIM_1;

        // M: For Gemini support
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (match != UriMatcher.NO_MATCH) {
                simId = getPreferSimdId();
            } else {
                for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                    match = s_urlMatcherGemini[i].match(url);
                    if (match != UriMatcher.NO_MATCH) {
                        simId = i;
                        break;
                    }
                }
            }
            if (DBG) Log.d(TAG, "insert(): match=" + match + ", simId=" + simId);
        }

        boolean notify = false;
        switch (match)
        {
            case URL_TELEPHONY:
            {
                ContentValues values;
                if (initialValues != null) {
                    values = new ContentValues(initialValues);
                } else {
                    values = new ContentValues();
                }

                // TODO Review this. This code should probably not bet here.
                // It is valid for the database to return a null string.
                if (!values.containsKey(Telephony.Carriers.NAME)) {
                    values.put(Telephony.Carriers.NAME, "");
                }
                if (!values.containsKey(Telephony.Carriers.APN)) {
                    values.put(Telephony.Carriers.APN, "");
                }
                if (!values.containsKey(Telephony.Carriers.PORT)) {
                    values.put(Telephony.Carriers.PORT, "");
                }
                if (!values.containsKey(Telephony.Carriers.PROXY)) {
                    values.put(Telephony.Carriers.PROXY, "");
                }
                if (!values.containsKey(Telephony.Carriers.USER)) {
                    values.put(Telephony.Carriers.USER, "");
                }
                if (!values.containsKey(Telephony.Carriers.SERVER)) {
                    values.put(Telephony.Carriers.SERVER, "");
                }
                if (!values.containsKey(Telephony.Carriers.PASSWORD)) {
                    values.put(Telephony.Carriers.PASSWORD, "");
                }
                if (!values.containsKey(Telephony.Carriers.MMSPORT)) {
                    values.put(Telephony.Carriers.MMSPORT, "");
                }
                if (!values.containsKey(Telephony.Carriers.MMSPROXY)) {
                    values.put(Telephony.Carriers.MMSPROXY, "");
                }
                if (!values.containsKey(Telephony.Carriers.AUTH_TYPE)) {
                    values.put(Telephony.Carriers.AUTH_TYPE, -1);
                }
                if (!values.containsKey(Telephony.Carriers.PROTOCOL)) {
                    values.put(Telephony.Carriers.PROTOCOL, "IP");
                }
                if (!values.containsKey(Telephony.Carriers.ROAMING_PROTOCOL)) {
                    values.put(Telephony.Carriers.ROAMING_PROTOCOL, "IP");
                }
//MTK-START [mtk04170][111215]
                if (values.containsKey(Telephony.Carriers.SOURCE_TYPE) == false) {
                    values.put(Telephony.Carriers.SOURCE_TYPE, 2);
                }
//MTK-END [mtk04170][111215]
                if (!values.containsKey(Telephony.Carriers.CARRIER_ENABLED)) {
                    values.put(Telephony.Carriers.CARRIER_ENABLED, true);
                }
                if (!values.containsKey(Telephony.Carriers.BEARER)) {
                    values.put(Telephony.Carriers.BEARER, 0);
                }
                if (FeatureOption.MTK_MVNO_SUPPORT) {
                    if (!values.containsKey(Telephony.Carriers.SPN)) {
                        values.put(Telephony.Carriers.SPN, "");
                    }
                    if (!values.containsKey(Telephony.Carriers.IMSI)) {
                        values.put(Telephony.Carriers.IMSI, "");
                    }
                    if (!values.containsKey(Telephony.Carriers.PNN)) {
                        values.put(Telephony.Carriers.PNN, "");
                    }
                }

                long rowID = db.insert(CARRIERS_TABLE_GEMINI[simId], null, values);
                if (rowID > 0)
                {
                    result = ContentUris.withAppendedId(url, rowID);
                    notify = true;
                }

                // M: log insert information
                if (Config.LOGD) Log.d(TAG, "inserted " + values.toString() + " rowID = " + rowID);

                break;
            }
            case URL_CURRENT:
            {
                // M: null out the previous operator
                db.update(CARRIERS_TABLE_GEMINI[simId], s_currentNullMap, "current IS NOT NULL", null);


                String numeric = initialValues.getAsString("numeric");

                // M:
                int updated = db.update(CARRIERS_TABLE_GEMINI[simId], s_currentSetMap,
                        "numeric = '" + numeric + "'", null);
                if (updated > 0) {
                    if (Config.LOGD) {
                        Log.d(TAG, "Setting numeric '" + numeric
                                + "' to be the current operator1");
                    }
                } else {
                    Log.e(TAG, "Failed setting numeric '" + numeric
                            + "' to the current operator");
                }
                break;
            }
            case URL_PREFERAPN:
            case URL_PREFERAPN_NO_UPDATE:
            {
                if (initialValues != null) {
                    if(initialValues.containsKey(COLUMN_APN_ID)) {
                        setPreferredApnId(PREF_FILE_GEMINI[simId], initialValues
                                .getAsLong(COLUMN_APN_ID));
                    }
                }
                break;
            }
            case URL_PREFERTETHERINGAPN: {
                if (initialValues != null) {
                    if (initialValues.containsKey(COLUMN_APN_ID)) {
                        setPreferredApnId(PREF_TETHERING_FILE_GEMINI[simId], initialValues
                                .getAsLong(COLUMN_APN_ID));
                    }
                }
                break;
            }
			
            // M: MTK modified
            case URL_SIMINFO: {
               long id = db.insert(SIMINFO_TABLE, null, initialValues);
               result = ContentUris.withAppendedId(SimInfo.CONTENT_URI, id);
               notify = true;
               break;
            }
            case URL_GPRSINFO: {
                long id = db.insert(GPRSINFO_TABLE, null, initialValues);
                result = ContentUris.withAppendedId(GprsInfo.CONTENT_URI, id);
                notify = true;
                break;
            }
            case URL_TELEPHONY_DM: {
                ContentValues values = genContentValue(initialValues);
				
                long rowID = db.insert(CARRIERS_DM_TABLE, null, values);
                if (rowID > 0) {
                    result = ContentUris.withAppendedId(
                             Telephony.Carriers.CONTENT_URI_DM, rowID);
                    notify = true;
                }

                if (Config.LOGD)
                    Log.d(TAG, "inserted " + values.toString() + " rowID = "
                            + rowID);
                break;
            }

            case URL_TELEPHONY_DM_GEMINI: {
                ContentValues values = genContentValue(initialValues);

                long rowID = db.insert(CARRIERS_DM_TABLE_GEMINI, null, values);
                if (rowID > 0) {
                    result = ContentUris.withAppendedId(
                            Telephony.Carriers.GeminiCarriers.CONTENT_URI_DM, rowID);
                    notify = true;
                }

                if (Config.LOGD)
                    Log.d(TAG, "inserted " + values.toString() + " rowID = "
                            + rowID);
               break;
            }
        }

        if (notify) {
            getContext().getContentResolver().notifyChange(url, null);
        }

        return result;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs)
    {
        int count = 0;

        checkPermission();

        // M: MTK modified
        initDatabaseIfNeeded(url);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = s_urlMatcher.match(url);
        int simId = PhoneConstants.GEMINI_SIM_1;

        // M: For Gemini support
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (match != UriMatcher.NO_MATCH) {
                simId = getPreferSimdId();
            } else {
                for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                    match = s_urlMatcherGemini[i].match(url);
                    if (match != UriMatcher.NO_MATCH) {
                        simId = i;
                        break;
                    }
                }
            }
            if (DBG) Log.d(TAG, "delete(): match=" + match + ", simId=" + simId);
        }

        int urlType = -1;
        switch (match)
        {
            case URL_TELEPHONY:
            {
                // M: For Gemini support
                count = db.delete(CARRIERS_TABLE_GEMINI[simId], where, whereArgs);
                urlType = URL_CARD_GEMINI[simId];
                break;
            }
            case URL_CURRENT:
            {
                // M: For Gemini support
                count = db.delete(CARRIERS_TABLE_GEMINI[simId], where, whereArgs);
                urlType = URL_CARD_GEMINI[simId];
                break;
            }
            case URL_ID:
            {
                // M: For Gemini support
                count = db.delete(CARRIERS_TABLE_GEMINI[simId], Telephony.Carriers._ID + "=?",
                        new String[] { url.getLastPathSegment() });
                break;
            }
            case URL_RESTOREAPN: {
                // M: For Gemini support
                count = 1;
                restoreDefaultAPN(PREF_FILE_GEMINI[simId], CARRIERS_TABLE_GEMINI[simId]);
                urlType = URL_CARD_GEMINI[simId];
                break;
            }
            case URL_PREFERAPN:
            case URL_PREFERAPN_NO_UPDATE:
            {
                // M: For Gemini support
                setPreferredApnId(PREF_FILE_GEMINI[simId], (long)-1);
                if (match == URL_PREFERAPN) count = 1;
                urlType = URL_CARD_GEMINI[simId];
                break;
            }

            // M: MTK modfified
            case URL_PREFERTETHERINGAPN: {
                setPreferredApnId(PREF_TETHERING_FILE_GEMINI[simId], (long) -1);
                count = 1;
                urlType = URL_CARD_GEMINI[simId];
                break;
            }
            case URL_SIMINFO: {
                count = db.delete(SIMINFO_TABLE, where, whereArgs);
                break;
            }
            case URL_SIMINFO_ID: {
                String selectionWithId =
                    (SimInfo._ID + "=" + ContentUris.parseId(url) + " ")
                    + (where == null ? "" : " AND (" + where + ")");
                count = db.delete(SIMINFO_TABLE, selectionWithId, whereArgs);
                break;
            }
            case URL_GPRSINFO: {
                count = db.delete(GPRSINFO_TABLE, where, whereArgs);
                break;
            }
            case URL_GPRSINFO_ID: {
                String selectionWithId =
                    (GprsInfo._ID + "=" + ContentUris.parseId(url) + " ")
                    + (where == null ? "" : " AND (" + where + ")");
                count = db.delete(GPRSINFO_TABLE, selectionWithId, whereArgs);
                break;
            }
            case URL_TELEPHONY_DM: {
                count = db.delete(CARRIERS_DM_TABLE, where, whereArgs);

                if(count>0) {
                    getContext().getContentResolver().notifyChange(
                            Telephony.Carriers.CONTENT_URI_DM, null);
                }
                 return count;
            }
            case URL_TELEPHONY_DM_GEMINI: {
                count = db.delete(CARRIERS_DM_TABLE_GEMINI, where, whereArgs);
                if(count>0) {
                    getContext().getContentResolver().notifyChange(
                            Telephony.Carriers.GeminiCarriers.CONTENT_URI_DM, null);
                }
                return count;
            }
            case URL_ID_DM: {
                count = db.delete(CARRIERS_DM_TABLE, where, whereArgs);
                if(count>0) {
                    getContext().getContentResolver().notifyChange(
                            Telephony.Carriers.CONTENT_URI_DM, null);
                }
                return count;
            }
            case URL_ID_DM_GEMINI: {
                count = db.delete(CARRIERS_DM_TABLE_GEMINI, Telephony.Carriers._ID
                        + "=?", new String[] { url.getLastPathSegment() });
                if(count>0) {
                    getContext().getContentResolver().notifyChange(
                            Telephony.Carriers.GeminiCarriers.CONTENT_URI_DM, null);
                }
                return count;
            }

            default: {
                throw new UnsupportedOperationException("Cannot delete that URL: " + url);
            }
        }

        if (count > 0) {
            // M: MTK Gemini support
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                getContext().getContentResolver().notifyChange(URI_GEMINI[simId], null);
                if (simId == getPreferSimdId()) {
                    getContext().getContentResolver().notifyChange(Telephony.Carriers.CONTENT_URI, null);
                }
            } else {
                getContext().getContentResolver().notifyChange(Telephony.Carriers.CONTENT_URI, null);
            }

            switch (urlType) {
                // M: MTK modified
                case URL_SIMINFO:
                case URL_SIMINFO_ID: {
                    getContext().getContentResolver().notifyChange(SimInfo.CONTENT_URI, null);
                    break;
                }
                case URL_GPRSINFO:
                case URL_GPRSINFO_ID: {
                    getContext().getContentResolver().notifyChange(GprsInfo.CONTENT_URI, null);
                    break;
                }

                default:
                    break;
            }
        }

        return count;
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs)
    {
        int count = 0;

        checkPermission();

        // M: MTK modified
        int urlType = -1;
        initDatabaseIfNeeded(url);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = s_urlMatcher.match(url);
        int simId = PhoneConstants.GEMINI_SIM_1;

        // M: For Gemini support
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (match != UriMatcher.NO_MATCH) {
                simId = getPreferSimdId();
            } else {
                for (int i=0; i<PhoneConstants.GEMINI_SIM_NUM; ++i) {
                    match = s_urlMatcherGemini[i].match(url);
                    if (match != UriMatcher.NO_MATCH) {
                        simId = i;
                        break;
                    }
                }
            }
            if (DBG) Log.d(TAG, "update(): match=" + match + ", simId=" + simId);
        }

        switch (match)
        {
            case URL_TELEPHONY:
            {
                // M: MTK Gemini support
                count = db.update(CARRIERS_TABLE_GEMINI[simId], values, where, whereArgs);
                urlType = URL_CARD_GEMINI[simId];
                break;
            }
            case URL_CURRENT:
            {
                // M: MTK Gemini support
                count = db.update(CARRIERS_TABLE_GEMINI[simId], values, where, whereArgs);
                urlType = URL_CARD_GEMINI[simId];
                break;
            }
            case URL_ID:
            {
                // M: MTK Gemini support
                if (where != null || whereArgs != null) {
                    throw new UnsupportedOperationException(
                            "Cannot update URL " + url + " with a where clause");
                }
                count = db.update(CARRIERS_TABLE_GEMINI[simId], values, Telephony.Carriers._ID + "=?",
                        new String[] { url.getLastPathSegment() });
                urlType = URL_CARD_GEMINI[simId];
                break;
            }
            case URL_PREFERAPN: 				
            case URL_PREFERAPN_NO_UPDATE:
            {
                // M: MTK Gemini support
                if (values != null) {
                    if (values.containsKey(COLUMN_APN_ID)) {
                        setPreferredApnId(PREF_FILE_GEMINI[simId], values
                                .getAsLong(COLUMN_APN_ID));
                        if (match == URL_PREFERAPN) count = 1;
                        urlType = URL_CARD_GEMINI[simId];
                    }
                }
                break;
            }

            // M: MTK modified
            case URL_SIMINFO: {
                count = db.update(SIMINFO_TABLE, values, where, whereArgs);
                break;
            }
            case URL_SIMINFO_ID: {
                String selectionWithId =
                    (SimInfo._ID + "=" + ContentUris.parseId(url) + " ")
                    + (where == null ? "" : " AND (" + where + ")");
                count = db.update(SIMINFO_TABLE, values, selectionWithId, whereArgs);
                break;
            }
            case URL_GPRSINFO: {
                count = db.update(GPRSINFO_TABLE, values, where, whereArgs);
                break;
            }
            case URL_GPRSINFO_ID: {
                String selectionWithId =
                    (GprsInfo._ID + "=" + ContentUris.parseId(url) + " ")
                    + (where == null ? "" : " AND (" + where + ")");
                count = db.update(GPRSINFO_TABLE, values, selectionWithId, whereArgs);
                break;
            }
            case URL_TELEPHONY_DM: {
                count = db.update(CARRIERS_DM_TABLE, values, where, whereArgs);
                if(count>0) {
                    getContext().getContentResolver().notifyChange(
                            Telephony.Carriers.CONTENT_URI_DM, null);
                }
                return count;

            }
            case URL_TELEPHONY_DM_GEMINI: {
                count = db.update(CARRIERS_DM_TABLE_GEMINI, values, where,
                        whereArgs);
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Telephony.Carriers.GeminiCarriers.CONTENT_URI_DM, null);
                }
                return count;

            }
            case URL_ID_DM: {
                if (where != null || whereArgs != null) {
                    throw new UnsupportedOperationException("Cannot update URL "
                            + url + " with a where clause");
                }
                count = db.update(CARRIERS_DM_TABLE, values, Telephony.Carriers._ID
                        + "=?", new String[] { url.getLastPathSegment() });
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Telephony.Carriers.CONTENT_URI_DM, null);
                }
                return count;
            }

            case URL_ID_DM_GEMINI: {
                if (where != null || whereArgs != null) {
                    throw new UnsupportedOperationException("Cannot update URL "
                            + url + " with a where clause");
                }
                count = db.update(CARRIERS_DM_TABLE_GEMINI, values,
                        Telephony.Carriers._ID + "=?", new String[] { url
                                .getLastPathSegment() });
                if (count > 0) {
                    getContext().getContentResolver().notifyChange(
                            Telephony.Carriers.GeminiCarriers.CONTENT_URI_DM, null);
                }
                return count;

            }
			
            default: {
                throw new UnsupportedOperationException("Cannot update that URL: " + url);
            }
        }

        if (count > 0) {
            // M: MTK Gemini support
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                getContext().getContentResolver().notifyChange(URI_GEMINI[simId], null);
                if (simId == getPreferSimdId()) {
                    getContext().getContentResolver().notifyChange(Telephony.Carriers.CONTENT_URI, null);
                }
            } else {
                getContext().getContentResolver().notifyChange(Telephony.Carriers.CONTENT_URI, null);
            }

            // M: MTK modified			
            switch (urlType) {
                case URL_SIMINFO:
                case URL_SIMINFO_ID:
                    getContext().getContentResolver().notifyChange(
                            Telephony.SimInfo.CONTENT_URI, null);
                    break;
                case URL_GPRSINFO:
                case URL_GPRSINFO_ID:
                    getContext().getContentResolver().notifyChange(
                            Telephony.GprsInfo.CONTENT_URI, null);
                    break;

                default:
                    break;
            } 
        }

        return count;
    }

    private void checkPermission() {
        // Check the permissions
        getContext().enforceCallingOrSelfPermission("android.permission.WRITE_APN_SETTINGS",
                "No permission to write APN settings");
    }

    private DatabaseHelper mOpenHelper;

    // M: add transaction for performance issue
    private void restoreDefaultAPN(String file, String table) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Log.i(TAG,"into restoreDefaultAPN");
        if (db == null) {
            return;
        }
        db.beginTransaction();
        db.delete(table, null, null);
        Log.i(TAG,"delete");
        setPreferredApnId(file, (long) -1);
        ((DatabaseHelper) mOpenHelper).initDatabase(db, table);
        Log.i(TAG,"initDatabase");
        db.setTransactionSuccessful();
        db.endTransaction();
    }
    private static void log(String key, String val) {
        if (DBG) {
            Log.d(TAG, key + val);
        }
    }
    
    private ContentValues genContentValue(ContentValues initialValues) {
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        // TODO Review this. This code should probably not bet here.
        // It is valid for the database to return a null string.
        if (values.containsKey(Telephony.Carriers.MCC) && values.containsKey(Telephony.Carriers.MNC)) {
            String mcc = values.getAsString(Telephony.Carriers.MCC);
            String mnc = values.getAsString(Telephony.Carriers.MNC);
            String numeric = mcc + mnc;
            values.put(Telephony.Carriers.NUMERIC,numeric);
        }

        
        if (values.containsKey(Telephony.Carriers.NAME) == false) {
            values.put(Telephony.Carriers.NAME, "");
        }
        if (values.containsKey(Telephony.Carriers.APN) == false) {
            values.put(Telephony.Carriers.APN, "");
        }
        if (values.containsKey(Telephony.Carriers.PORT) == false) {
            values.put(Telephony.Carriers.PORT, "");
        }
        if (values.containsKey(Telephony.Carriers.PROXY) == false) {
            values.put(Telephony.Carriers.PROXY, "");
        }
        if (values.containsKey(Telephony.Carriers.USER) == false) {
            values.put(Telephony.Carriers.USER, "");
        }
        if (values.containsKey(Telephony.Carriers.SERVER) == false) {
            values.put(Telephony.Carriers.SERVER, "");
        }
        if (values.containsKey(Telephony.Carriers.PASSWORD) == false) {
            values.put(Telephony.Carriers.PASSWORD, "");
        }
        if (values.containsKey(Telephony.Carriers.MMSPORT) == false) {
            values.put(Telephony.Carriers.MMSPORT, "");
        }
        if (values.containsKey(Telephony.Carriers.MMSPROXY) == false) {
            values.put(Telephony.Carriers.MMSPROXY, "");
        }
        if (values.containsKey(Telephony.Carriers.AUTH_TYPE) == false) {
            values.put(Telephony.Carriers.AUTH_TYPE, -1);
        }
        if (!values.containsKey(Telephony.Carriers.PROTOCOL)) {
            values.put(Telephony.Carriers.PROTOCOL, "IP");
        }
        if (!values.containsKey(Telephony.Carriers.ROAMING_PROTOCOL)) {
            values.put(Telephony.Carriers.ROAMING_PROTOCOL, "IP");
        }
        if (!values.containsKey(Telephony.Carriers.CARRIER_ENABLED)) {
            values.put(Telephony.Carriers.CARRIER_ENABLED, true);
        }
        if (!values.containsKey(Telephony.Carriers.BEARER)) {
            values.put(Telephony.Carriers.BEARER, 0);
        }
        values.put(Telephony.Carriers.TYPE, PhoneConstants.APN_TYPE_DM);

        return values;
    }
//MTK-END [mtk04170][111215]    
    private interface ReplaceSqlStatement {
        String CARRIER_INSERT_SQL =
                "INSERT INTO " + CARRIERS_TABLE + " ("
                        + Carriers.NAME + ", "
                        + Carriers.NUMERIC + ", "
                        + Carriers.MCC + ", "
                        + Carriers.MNC + ", "
                        + Carriers.APN + ", "
                        + Carriers.USER + ", "
                        + Carriers.SERVER + ", "
                        + Carriers.PASSWORD + ", "
                        + Carriers.PROXY + ", "
                        + Carriers.PORT + ", "
                        + Carriers.MMSPROXY + ", "
                        + Carriers.MMSPORT + ", "
                        + Carriers.MMSC + ", "
                        + Carriers.AUTH_TYPE + ", "
                        + Carriers.TYPE + ", "
                        + Carriers.CURRENT + ", "
                        + Carriers.SOURCE_TYPE + ", "
                        + Carriers.CSD_NUM + ", "
                        + Carriers.PROTOCOL + ", "
                        + Carriers.ROAMING_PROTOCOL + ", "
//                        + Carriers.OMACPID + ", "
//                        + Carriers.NAPID + ", "
//                        + Carriers.PROXYID + ", "
                        + Carriers.CARRIER_ENABLED + ", "
                        + Carriers.BEARER + ") "
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String CARRIER_GEMINI_INSERT_SQL =
            "INSERT INTO " + CARRIERS_TABLE_GEMINI + " ("
                    + Carriers.NAME + ", "
                    + Carriers.NUMERIC + ", "
                    + Carriers.MCC + ", "
                    + Carriers.MNC + ", "
                    + Carriers.APN + ", "
                    + Carriers.USER + ", "
                    + Carriers.SERVER + ", "
                    + Carriers.PASSWORD + ", "
                    + Carriers.PROXY + ", "
                    + Carriers.PORT + ", "
                    + Carriers.MMSPROXY + ", "
                    + Carriers.MMSPORT + ", "
                    + Carriers.MMSC + ", "
                    + Carriers.AUTH_TYPE + ", "
                    + Carriers.TYPE + ", "
                    + Carriers.CURRENT + ", "
                    + Carriers.SOURCE_TYPE + ", "
                    + Carriers.CSD_NUM + ", "
                    + Carriers.PROTOCOL + ", "
                    + Carriers.ROAMING_PROTOCOL + ", "
//                    + Carriers.OMACPID + ", "
//                    + Carriers.NAPID + ", "
//                    + Carriers.PROXYID + ", "
                    + Carriers.CARRIER_ENABLED + ", "
                    + Carriers.BEARER + ") "
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String CARRIER_OMACP_INSERT_SQL =
            "INSERT INTO " + CARRIERS_TABLE + " ("
                    + Carriers.NAME + ", "
                    + Carriers.NUMERIC + ", "
                    + Carriers.MCC + ", "
                    + Carriers.MNC + ", "
                    + Carriers.APN + ", "
                    + Carriers.USER + ", "
                    + Carriers.SERVER + ", "
                    + Carriers.PASSWORD + ", "
                    + Carriers.PROXY + ", "
                    + Carriers.PORT + ", "
                    + Carriers.MMSPROXY + ", "
                    + Carriers.MMSPORT + ", "
                    + Carriers.MMSC + ", "
                    + Carriers.AUTH_TYPE + ", "
                    + Carriers.TYPE + ", "
                    + Carriers.CURRENT + ", "
                    + Carriers.SOURCE_TYPE + ", "
                    + Carriers.CSD_NUM + ", "
                    + Carriers.PROTOCOL + ", "
                    + Carriers.ROAMING_PROTOCOL + ", "
                    + Carriers.OMACPID + ", "
                    + Carriers.NAPID + ", "
                    + Carriers.PROXYID + ", "
                    + Carriers.CARRIER_ENABLED + ", "
                    + Carriers.BEARER + ") "
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    String CARRIER_OMACP_GEMINI_INSERT_SQL =
        "INSERT INTO " + CARRIERS_TABLE_GEMINI + " ("
                + Carriers.NAME + ", "
                + Carriers.NUMERIC + ", "
                + Carriers.MCC + ", "
                + Carriers.MNC + ", "
                + Carriers.APN + ", "
                + Carriers.USER + ", "
                + Carriers.SERVER + ", "
                + Carriers.PASSWORD + ", "
                + Carriers.PROXY + ", "
                + Carriers.PORT + ", "
                + Carriers.MMSPROXY + ", "
                + Carriers.MMSPORT + ", "
                + Carriers.MMSC + ", "
                + Carriers.AUTH_TYPE + ", "
                + Carriers.TYPE + ", "
                + Carriers.CURRENT + ", "
                + Carriers.SOURCE_TYPE + ", "
                + Carriers.CSD_NUM + ", "
                + Carriers.PROTOCOL + ", "
                + Carriers.ROAMING_PROTOCOL + ", "
                + Carriers.OMACPID + ", "
                + Carriers.NAPID + ", "
                + Carriers.PROXYID + ", "
                + Carriers.CARRIER_ENABLED + ", "
                + Carriers.BEARER + ") "
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String CARRIER_MVNO_INSERT_SQL =
                "INSERT INTO " + CARRIERS_TABLE + " ("
                        + Carriers.NAME + ", "
                        + Carriers.NUMERIC + ", "
                        + Carriers.MCC + ", "
                        + Carriers.MNC + ", "
                        + Carriers.APN + ", "
                        + Carriers.USER + ", "
                        + Carriers.SERVER + ", "
                        + Carriers.PASSWORD + ", "
                        + Carriers.PROXY + ", "
                        + Carriers.PORT + ", "
                        + Carriers.MMSPROXY + ", "
                        + Carriers.MMSPORT + ", "
                        + Carriers.MMSC + ", "
                        + Carriers.AUTH_TYPE + ", "
                        + Carriers.TYPE + ", "
                        + Carriers.CURRENT + ", "
                        + Carriers.SOURCE_TYPE + ", "
                        + Carriers.CSD_NUM + ", "
                        + Carriers.PROTOCOL + ", "
                        + Carriers.ROAMING_PROTOCOL + ", "
                        + Carriers.CARRIER_ENABLED + ", "
                        + Carriers.BEARER + ", "
                        + Carriers.SPN + ", "
                        + Carriers.IMSI + ", "
                        + Carriers.PNN + ") "
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String CARRIER_MVNO_GEMINI_INSERT_SQL =
                "INSERT INTO " + CARRIERS_TABLE_GEMINI + " ("
                        + Carriers.NAME + ", "
                        + Carriers.NUMERIC + ", "
                        + Carriers.MCC + ", "
                        + Carriers.MNC + ", "
                        + Carriers.APN + ", "
                        + Carriers.USER + ", "
                        + Carriers.SERVER + ", "
                        + Carriers.PASSWORD + ", "
                        + Carriers.PROXY + ", "
                        + Carriers.PORT + ", "
                        + Carriers.MMSPROXY + ", "
                        + Carriers.MMSPORT + ", "
                        + Carriers.MMSC + ", "
                        + Carriers.AUTH_TYPE + ", "
                        + Carriers.TYPE + ", "
                        + Carriers.CURRENT + ", "
                        + Carriers.SOURCE_TYPE + ", "
                        + Carriers.CSD_NUM + ", "
                        + Carriers.PROTOCOL + ", "
                        + Carriers.ROAMING_PROTOCOL + ", "
                        + Carriers.CARRIER_ENABLED + ", "
                        + Carriers.BEARER + ", "
                        + Carriers.SPN + ", "
                        + Carriers.IMSI + ", "
                        + Carriers.PNN + ") "
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String CARRIER_MVNO_OMACP_INSERT_SQL =
            "INSERT INTO " + CARRIERS_TABLE + " ("
                    + Carriers.NAME + ", "
                    + Carriers.NUMERIC + ", "
                    + Carriers.MCC + ", "
                    + Carriers.MNC + ", "
                    + Carriers.APN + ", "
                    + Carriers.USER + ", "
                    + Carriers.SERVER + ", "
                    + Carriers.PASSWORD + ", "
                    + Carriers.PROXY + ", "
                    + Carriers.PORT + ", "
                    + Carriers.MMSPROXY + ", "
                    + Carriers.MMSPORT + ", "
                    + Carriers.MMSC + ", "
                    + Carriers.AUTH_TYPE + ", "
                    + Carriers.TYPE + ", "
                    + Carriers.CURRENT + ", "
                    + Carriers.SOURCE_TYPE + ", "
                    + Carriers.CSD_NUM + ", "
                    + Carriers.PROTOCOL + ", "
                    + Carriers.ROAMING_PROTOCOL + ", "
                    + Carriers.OMACPID + ", "
                    + Carriers.NAPID + ", "
                    + Carriers.PROXYID + ", "
                    + Carriers.CARRIER_ENABLED + ", "
                    + Carriers.BEARER + ", "
                    + Carriers.SPN + ", "
                    + Carriers.IMSI + ", "
                    + Carriers.PNN + ") "
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String CARRIER_MVNO_OMACP_GEMINI_INSERT_SQL =
            "INSERT INTO " + CARRIERS_TABLE_GEMINI + " ("
                    + Carriers.NAME + ", "
                    + Carriers.NUMERIC + ", "
                    + Carriers.MCC + ", "
                    + Carriers.MNC + ", "
                    + Carriers.APN + ", "
                    + Carriers.USER + ", "
                    + Carriers.SERVER + ", "
                    + Carriers.PASSWORD + ", "
                    + Carriers.PROXY + ", "
                    + Carriers.PORT + ", "
                    + Carriers.MMSPROXY + ", "
                    + Carriers.MMSPORT + ", "
                    + Carriers.MMSC + ", "
                    + Carriers.AUTH_TYPE + ", "
                    + Carriers.TYPE + ", "
                    + Carriers.CURRENT + ", "
                    + Carriers.SOURCE_TYPE + ", "
                    + Carriers.CSD_NUM + ", "
                    + Carriers.PROTOCOL + ", "
                    + Carriers.ROAMING_PROTOCOL + ", "
                    + Carriers.OMACPID + ", "
                    + Carriers.NAPID + ", "
                    + Carriers.PROXYID + ", "
                    + Carriers.CARRIER_ENABLED + ", "
                    + Carriers.BEARER + ", "
                    + Carriers.SPN + ", "
                    + Carriers.IMSI + ", "
                    + Carriers.PNN + ") "
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        int NAME = 1;
        int NUMERIC = 2;
        int MCC = 3;
        int MNC = 4;
        int APN = 5;
        int USER = 6;
        int SERVER = 7;
        int PASSWORD = 8;
        int PROXY = 9;
        int PORT = 10;
        int MMSPROXY = 11;
        int MMSPORT = 12;
        int MMSC = 13;
        int AUTH_TYPE = 14;
        int TYPE = 15;
        int CURRENT = 16;
        int SOURCE_TYPE = 17;
        int CSD_NUM = 18;
        int PROTOCOL = 19;
        int ROAMING_PROTOCOL = 20;
        int OMACP_OMACPID = 21;
        int OMACP_NAPID = 22;
        int OMACP_PROXYID = 23;
        int OMACP_CARRIER_ENABLED = 24;
        int OMACP_BEARER = 25;
        int OMACP_SPN = 26;
        int OMACP_IMSI = 27;
        int OMACP_PNN = 28;
        int CARRIER_ENABLED = 21;
        int BEARER = 22;
        int SPN = 23;
        int IMSI = 24;
        int PNN = 25;
    }

    private int getPreferSimdId(){
        int simId = -1;
        
        simId =  Settings.System.getInt(getContext().getContentResolver(), Settings.System.GPRS_CONNECTION_SETTING, Settings.System.GPRS_CONNECTION_SETTING_DEFAULT) - 1;
        if (simId < PhoneConstants.GEMINI_SIM_1 
|| simId >= PhoneConstants.GEMINI_SIM_NUM) {
            if (iTelephony != null) {
                try {				
                    simId = iTelephony.get3GCapabilitySIM();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "iTelephony exception");
                }
            } else {
                Log.e(TAG, "iTelephony is null!!!");
            }
        }

        if (simId < PhoneConstants.GEMINI_SIM_1 
|| simId >= PhoneConstants.GEMINI_SIM_NUM) {
            simId = PhoneConstants.GEMINI_SIM_1;
        }

        Log.d(TAG, "Default Data Setting value=" + simId);

        return simId;
    }
}
