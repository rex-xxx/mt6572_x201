/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony.gsm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.os.Environment;
import android.util.Log;
import android.util.Xml;

import com.android.internal.util.XmlUtils;

// MVNO-API START
import com.mediatek.common.featureoption.FeatureOption;
import java.util.ArrayList;
import java.lang.Integer;
// MVNO-API END

public class SpnOverride {
    private static HashMap<String, String> CarrierSpnMap;
    private static SpnOverride sInstance;
    static final Object sInstSync = new Object();
    static final String LOG_TAG = "GSM";
    static final String PARTNER_SPN_OVERRIDE_PATH ="etc/spn-conf.xml";

    // MVNO-API START
      // EF_SPN
    private static HashMap<String, String> CarrierVirtualSpnMapByEfSpn;
    private static final String PARTNER_VIRTUAL_SPN_BY_EF_SPN_OVERRIDE_PATH ="etc/virtual-spn-conf-by-efspn.xml";

      // IMSI
    private ArrayList CarrierVirtualSpnMapByImsi;
    private static final String PARTNER_VIRTUAL_SPN_BY_IMSI_OVERRIDE_PATH ="etc/virtual-spn-conf-by-imsi.xml";
    public class VirtualSpnByImsi {
        public String mccmnc;
        public int index;
        public int length;
        public String pattern;
        public String name;
        public VirtualSpnByImsi(String mccmnc, int index, int length, String pattern, String name) {
            this.mccmnc = mccmnc;
            this.index = index;
            this.length = length;
            this.pattern = pattern;
            this.name = name;
        }
    }

      // EF_PNN
    private static HashMap<String, String> CarrierVirtualSpnMapByEfPnn;
    private static final String PARTNER_VIRTUAL_SPN_BY_EF_PNN_OVERRIDE_PATH ="etc/virtual-spn-conf-by-efpnn.xml";
    // MVNO-API END

    public static SpnOverride getInstance() {
        synchronized (sInstSync) {
            if (sInstance == null) {
                sInstance = new SpnOverride();
            }
        }
        return sInstance;
    }

    private SpnOverride() {
        CarrierSpnMap = new HashMap<String, String>();
        loadSpnOverrides();

        // MVNO-API
        if(FeatureOption.MTK_MVNO_SUPPORT) {
            // EF_SPN
            CarrierVirtualSpnMapByEfSpn = new HashMap<String, String>();
            loadVirtualSpnOverridesByEfSpn();

            // IMSI
            this.CarrierVirtualSpnMapByImsi = new ArrayList();
            this.loadVirtualSpnOverridesByImsi();

            // EF_PNN
            CarrierVirtualSpnMapByEfPnn = new HashMap<String, String>();
            loadVirtualSpnOverridesByEfPnn();
        }
    }

    public boolean containsCarrier(String carrier) {
        return CarrierSpnMap.containsKey(carrier);
    }

    public String getSpn(String carrier) {
        return CarrierSpnMap.get(carrier);
    }

    private static void loadSpnOverrides() {
        FileReader spnReader;
        Log.d(LOG_TAG, "loadSpnOverrides");
        final File spnFile = new File(Environment.getRootDirectory(),
                PARTNER_SPN_OVERRIDE_PATH);

        try {
            spnReader = new FileReader(spnFile);
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, "Can't open " +
                    Environment.getRootDirectory() + "/" + PARTNER_SPN_OVERRIDE_PATH);
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(spnReader);

            XmlUtils.beginDocument(parser, "spnOverrides");

            while (true) {
                XmlUtils.nextElement(parser);

                String name = parser.getName();
                if (!"spnOverride".equals(name)) {
                    break;
                }

                String numeric = parser.getAttributeValue(null, "numeric");
                String data    = parser.getAttributeValue(null, "spn");

                CarrierSpnMap.put(numeric, data);
            }
        } catch (XmlPullParserException e) {
            Log.w(LOG_TAG, "Exception in spn-conf parser " + e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Exception in spn-conf parser " + e);
        }
    }

    // MVNO-API START
    private static void loadVirtualSpnOverridesByEfSpn() {
        FileReader spnReader;
        Log.d(LOG_TAG, "loadVirtualSpnOverridesByEfSpn");
        final File spnFile = new File(Environment.getRootDirectory(), PARTNER_VIRTUAL_SPN_BY_EF_SPN_OVERRIDE_PATH);

        try {
            spnReader = new FileReader(spnFile);
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, "Can't open " +
                    Environment.getRootDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_EF_SPN_OVERRIDE_PATH);
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(spnReader);

            XmlUtils.beginDocument(parser, "virtualSpnOverridesByEfSpn");

            while (true) {
                XmlUtils.nextElement(parser);

                String name = parser.getName();
                if (!"virtualSpnOverride".equals(name)) {
                    break;
                }

                String mccmncspn = parser.getAttributeValue(null, "mccmncspn");
                String spn = parser.getAttributeValue(null, "name");
                Log.w(LOG_TAG, "test mccmncspn = " + mccmncspn + ", name = " + spn); //////////// test
                CarrierVirtualSpnMapByEfSpn.put(mccmncspn, spn);
            }
            spnReader.close();
        } catch (XmlPullParserException e) {
            Log.w(LOG_TAG, "Exception in virtual-spn-conf-by-efspn parser " + e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Exception in virtual-spn-conf-by-efspn parser " + e);
        }
    }

    public String getSpnByEfSpn(String mccmnc, String spn) {
        if(mccmnc == null || spn == null || mccmnc.isEmpty() || spn.isEmpty())
            return null;

        return CarrierVirtualSpnMapByEfSpn.get(mccmnc + spn);
    }

    private void loadVirtualSpnOverridesByImsi() {
        FileReader spnReader;
        Log.d(LOG_TAG, "loadVirtualSpnOverridesByImsi");
        final File spnFile = new File(Environment.getRootDirectory(), PARTNER_VIRTUAL_SPN_BY_IMSI_OVERRIDE_PATH);

        try {
            spnReader = new FileReader(spnFile);
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, "Can't open " +
                    Environment.getRootDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_IMSI_OVERRIDE_PATH);
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(spnReader);

            XmlUtils.beginDocument(parser, "virtualSpnOverridesByImsi");

            while (true) {
                XmlUtils.nextElement(parser);

                String name = parser.getName();
                if (!"virtualSpnOverride".equals(name)) {
                    break;
                }

                String mccmnc = parser.getAttributeValue(null, "mccmnc");
                int index = Integer.parseInt(parser.getAttributeValue(null, "index"));
                int length = Integer.parseInt(parser.getAttributeValue(null, "length"));
                String pattern = parser.getAttributeValue(null, "pattern");
                String spn = parser.getAttributeValue(null, "name");
                Log.w(LOG_TAG, "test mccmnc = " + mccmnc + ", index = " + index + ", length = " + length + ", pattern = " + pattern + ", name = " + spn); //////////// test
                this.CarrierVirtualSpnMapByImsi.add(new VirtualSpnByImsi(mccmnc, index, length, pattern, spn));
            }
            spnReader.close();
        } catch (XmlPullParserException e) {
            Log.w(LOG_TAG, "Exception in virtual-spn-conf-by-imsi parser " + e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Exception in virtual-spn-conf-by-imsi parser " + e);
        }
    }

    public String getSpnByImsi(String mccmnc, String imsi) {
        if(mccmnc == null || imsi == null || mccmnc.isEmpty() || imsi.isEmpty())
            return null;

        VirtualSpnByImsi vsbi;
        String pattern;
        for(int i = 0; i < this.CarrierVirtualSpnMapByImsi.size(); i++) {
            vsbi = (VirtualSpnByImsi)(this.CarrierVirtualSpnMapByImsi.get(i));
            Log.w(LOG_TAG, "getSpnByImsi(): imsi = " + imsi + ", index = " + vsbi.index + ", length = " + vsbi.length + ", pattern = " + vsbi.pattern);

            try {
                pattern = imsi.substring(vsbi.index, vsbi.index + vsbi.length);
            } catch(Exception e) {
                Log.w(LOG_TAG, "Exception in getSpnByImsi " + e);
                continue;
            }

            if(vsbi.mccmnc.equals(mccmnc) && pattern.equals(vsbi.pattern))
                return vsbi.name;
        }
        return null;
    }

    public String isOperatorMvnoForImsi(String mccmnc, String imsi) {
        if(mccmnc == null || imsi == null || mccmnc.isEmpty() || imsi.isEmpty())
            return null;

        VirtualSpnByImsi vsbi;
        String pattern;
        for(int i = 0; i < this.CarrierVirtualSpnMapByImsi.size(); i++) {
            vsbi = (VirtualSpnByImsi)(this.CarrierVirtualSpnMapByImsi.get(i));
            pattern = imsi.substring(vsbi.index, vsbi.index + vsbi.length);

            if(vsbi.mccmnc.equals(mccmnc) && pattern.equals(vsbi.pattern))
                return vsbi.pattern;
        }
        return null;
    }

    private static void loadVirtualSpnOverridesByEfPnn() {
        FileReader spnReader;
        Log.d(LOG_TAG, "loadVirtualSpnOverridesByEfPnn");
        final File spnFile = new File(Environment.getRootDirectory(), PARTNER_VIRTUAL_SPN_BY_EF_PNN_OVERRIDE_PATH);

        try {
            spnReader = new FileReader(spnFile);
        } catch (FileNotFoundException e) {
            Log.w(LOG_TAG, "Can't open " +
                    Environment.getRootDirectory() + "/" + PARTNER_VIRTUAL_SPN_BY_EF_PNN_OVERRIDE_PATH);
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(spnReader);

            XmlUtils.beginDocument(parser, "virtualSpnOverridesByEfPnn");

            while (true) {
                XmlUtils.nextElement(parser);

                String name = parser.getName();
                if (!"virtualSpnOverride".equals(name)) {
                    break;
                }

                String mccmncpnn = parser.getAttributeValue(null, "mccmncpnn");
                String spn = parser.getAttributeValue(null, "name");
                Log.w(LOG_TAG, "test mccmncpnn = " + mccmncpnn + ", name = " + spn); //////////// test
                CarrierVirtualSpnMapByEfPnn.put(mccmncpnn, spn);
            }
            spnReader.close();
        } catch (XmlPullParserException e) {
            Log.w(LOG_TAG, "Exception in virtual-spn-conf-by-efpnn parser " + e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Exception in virtual-spn-conf-by-efpnn parser " + e);
        }
    }

    public String getSpnByEfPnn(String mccmnc, String pnn) {
        if(mccmnc == null || pnn == null || mccmnc.isEmpty() || pnn.isEmpty())
            return null;

        return CarrierVirtualSpnMapByEfPnn.get(mccmnc + pnn);
    }
    // MVNO-API END
}
