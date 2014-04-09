/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.internal.telephony;

import java.util.HashMap;

/**
 * Implement the WSP data type decoder.
 *
 * @hide
 */
public class WspTypeDecoder {

    private static final int WAP_PDU_SHORT_LENGTH_MAX = 30;
    private static final int WAP_PDU_LENGTH_QUOTE = 31;

    public static final int PDU_TYPE_PUSH = 0x06;
    public static final int PDU_TYPE_CONFIRMED_PUSH = 0x07;

    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    public static final int CONTENT_TYPE_B_CONNECTIVITY = 0x35;
    public static final String CONTENT_MIME_TYPE_B_CONNECTIVITY = 
            "application/vnd.wap.connectivity-wbxml";
    // Add For AGPS exchanging SUPL init Message between SLP(SUPL Locaiton Platform) and UE.
    // Reference to specification OMA-TS-ULP-v1.0-20070615-A.pdf page 50-51 for detailed infromtion.
    public static final String CONTENT_MIME_TYPE_B_VND_SULP_INIT = "application/vnd.omaloc-supl-init";
    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

    protected final static HashMap<Integer, String> WELL_KNOWN_MIME_TYPES =
            new HashMap<Integer, String>();

    protected final static HashMap<Integer, String> WELL_KNOWN_PARAMETERS =
            new HashMap<Integer, String>();

    protected final static HashMap<Integer, String> WELL_KNOWN_HEADERS =
            new HashMap<Integer, String>();

    public static final int PARAMETER_ID_X_WAP_APPLICATION_ID = 0x2f;
    private static final int Q_VALUE = 0x00;

    static {
        WELL_KNOWN_MIME_TYPES.put(0x00, "*/*");
        WELL_KNOWN_MIME_TYPES.put(0x01, "text/*");
        WELL_KNOWN_MIME_TYPES.put(0x02, "text/html");
        WELL_KNOWN_MIME_TYPES.put(0x03, "text/plain");
        WELL_KNOWN_MIME_TYPES.put(0x04, "text/x-hdml");
        WELL_KNOWN_MIME_TYPES.put(0x05, "text/x-ttml");
        WELL_KNOWN_MIME_TYPES.put(0x06, "text/x-vCalendar");
        WELL_KNOWN_MIME_TYPES.put(0x07, "text/x-vCard");
        WELL_KNOWN_MIME_TYPES.put(0x08, "text/vnd.wap.wml");
        WELL_KNOWN_MIME_TYPES.put(0x09, "text/vnd.wap.wmlscript");
        WELL_KNOWN_MIME_TYPES.put(0x0A, "text/vnd.wap.wta-event");
        WELL_KNOWN_MIME_TYPES.put(0x0B, "multipart/*");
        WELL_KNOWN_MIME_TYPES.put(0x0C, "multipart/mixed");
        WELL_KNOWN_MIME_TYPES.put(0x0D, "multipart/form-data");
        WELL_KNOWN_MIME_TYPES.put(0x0E, "multipart/byterantes");
        WELL_KNOWN_MIME_TYPES.put(0x0F, "multipart/alternative");
        WELL_KNOWN_MIME_TYPES.put(0x10, "application/*");
        WELL_KNOWN_MIME_TYPES.put(0x11, "application/java-vm");
        WELL_KNOWN_MIME_TYPES.put(0x12, "application/x-www-form-urlencoded");
        WELL_KNOWN_MIME_TYPES.put(0x13, "application/x-hdmlc");
        WELL_KNOWN_MIME_TYPES.put(0x14, "application/vnd.wap.wmlc");
        WELL_KNOWN_MIME_TYPES.put(0x15, "application/vnd.wap.wmlscriptc");
        WELL_KNOWN_MIME_TYPES.put(0x16, "application/vnd.wap.wta-eventc");
        WELL_KNOWN_MIME_TYPES.put(0x17, "application/vnd.wap.uaprof");
        WELL_KNOWN_MIME_TYPES.put(0x18, "application/vnd.wap.wtls-ca-certificate");
        WELL_KNOWN_MIME_TYPES.put(0x19, "application/vnd.wap.wtls-user-certificate");
        WELL_KNOWN_MIME_TYPES.put(0x1A, "application/x-x509-ca-cert");
        WELL_KNOWN_MIME_TYPES.put(0x1B, "application/x-x509-user-cert");
        WELL_KNOWN_MIME_TYPES.put(0x1C, "image/*");
        WELL_KNOWN_MIME_TYPES.put(0x1D, "image/gif");
        WELL_KNOWN_MIME_TYPES.put(0x1E, "image/jpeg");
        WELL_KNOWN_MIME_TYPES.put(0x1F, "image/tiff");
        WELL_KNOWN_MIME_TYPES.put(0x20, "image/png");
        WELL_KNOWN_MIME_TYPES.put(0x21, "image/vnd.wap.wbmp");
        WELL_KNOWN_MIME_TYPES.put(0x22, "application/vnd.wap.multipart.*");
        WELL_KNOWN_MIME_TYPES.put(0x23, "application/vnd.wap.multipart.mixed");
        WELL_KNOWN_MIME_TYPES.put(0x24, "application/vnd.wap.multipart.form-data");
        WELL_KNOWN_MIME_TYPES.put(0x25, "application/vnd.wap.multipart.byteranges");
        WELL_KNOWN_MIME_TYPES.put(0x26, "application/vnd.wap.multipart.alternative");
        WELL_KNOWN_MIME_TYPES.put(0x27, "application/xml");
        WELL_KNOWN_MIME_TYPES.put(0x28, "text/xml");
        WELL_KNOWN_MIME_TYPES.put(0x29, "application/vnd.wap.wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x2A, "application/x-x968-cross-cert");
        WELL_KNOWN_MIME_TYPES.put(0x2B, "application/x-x968-ca-cert");
        WELL_KNOWN_MIME_TYPES.put(0x2C, "application/x-x968-user-cert");
        WELL_KNOWN_MIME_TYPES.put(0x2D, "text/vnd.wap.si");
        WELL_KNOWN_MIME_TYPES.put(0x2E, "application/vnd.wap.sic");
        WELL_KNOWN_MIME_TYPES.put(0x2F, "text/vnd.wap.sl");
        WELL_KNOWN_MIME_TYPES.put(0x30, "application/vnd.wap.slc");
        WELL_KNOWN_MIME_TYPES.put(0x31, "text/vnd.wap.co");
        WELL_KNOWN_MIME_TYPES.put(0x32, "application/vnd.wap.coc");
        WELL_KNOWN_MIME_TYPES.put(0x33, "application/vnd.wap.multipart.related");
        WELL_KNOWN_MIME_TYPES.put(0x34, "application/vnd.wap.sia");
        WELL_KNOWN_MIME_TYPES.put(0x35, "text/vnd.wap.connectivity-xml");
        WELL_KNOWN_MIME_TYPES.put(0x36, "application/vnd.wap.connectivity-wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x37, "application/pkcs7-mime");
        WELL_KNOWN_MIME_TYPES.put(0x38, "application/vnd.wap.hashed-certificate");
        WELL_KNOWN_MIME_TYPES.put(0x39, "application/vnd.wap.signed-certificate");
        WELL_KNOWN_MIME_TYPES.put(0x3A, "application/vnd.wap.cert-response");
        WELL_KNOWN_MIME_TYPES.put(0x3B, "application/xhtml+xml");
        WELL_KNOWN_MIME_TYPES.put(0x3C, "application/wml+xml");
        WELL_KNOWN_MIME_TYPES.put(0x3D, "text/css");
        WELL_KNOWN_MIME_TYPES.put(0x3E, "application/vnd.wap.mms-message");
        WELL_KNOWN_MIME_TYPES.put(0x3F, "application/vnd.wap.rollover-certificate");
        WELL_KNOWN_MIME_TYPES.put(0x40, "application/vnd.wap.locc+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x41, "application/vnd.wap.loc+xml");
        WELL_KNOWN_MIME_TYPES.put(0x42, "application/vnd.syncml.dm+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x43, "application/vnd.syncml.dm+xml");
        WELL_KNOWN_MIME_TYPES.put(0x44, "application/vnd.syncml.notification");
        WELL_KNOWN_MIME_TYPES.put(0x45, "application/vnd.wap.xhtml+xml");
        WELL_KNOWN_MIME_TYPES.put(0x46, "application/vnd.wv.csp.cir");
        WELL_KNOWN_MIME_TYPES.put(0x47, "application/vnd.oma.dd+xml");
        WELL_KNOWN_MIME_TYPES.put(0x48, "application/vnd.oma.drm.message");
        WELL_KNOWN_MIME_TYPES.put(0x49, "application/vnd.oma.drm.content");
        WELL_KNOWN_MIME_TYPES.put(0x4A, "application/vnd.oma.drm.rights+xml");
        WELL_KNOWN_MIME_TYPES.put(0x4B, "application/vnd.oma.drm.rights+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x4C, "application/vnd.wv.csp+xml");
        WELL_KNOWN_MIME_TYPES.put(0x4D, "application/vnd.wv.csp+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x4E, "application/vnd.syncml.ds.notification");
        WELL_KNOWN_MIME_TYPES.put(0x4F, "audio/*");
        WELL_KNOWN_MIME_TYPES.put(0x50, "video/*");
        WELL_KNOWN_MIME_TYPES.put(0x51, "application/vnd.oma.dd2+xml");
        WELL_KNOWN_MIME_TYPES.put(0x52, "application/mikey");
        WELL_KNOWN_MIME_TYPES.put(0x53, "application/vnd.oma.dcd");
        WELL_KNOWN_MIME_TYPES.put(0x54, "application/vnd.oma.dcdc");

        WELL_KNOWN_MIME_TYPES.put(0x0201, "application/vnd.uplanet.cacheop-wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x0202, "application/vnd.uplanet.signal");
        WELL_KNOWN_MIME_TYPES.put(0x0203, "application/vnd.uplanet.alert-wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x0204, "application/vnd.uplanet.list-wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x0205, "application/vnd.uplanet.listcmd-wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x0206, "application/vnd.uplanet.channel-wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x0207, "application/vnd.uplanet.provisioning-status-uri");
        WELL_KNOWN_MIME_TYPES.put(0x0208, "x-wap.multipart/vnd.uplanet.header-set");
        WELL_KNOWN_MIME_TYPES.put(0x0209, "application/vnd.uplanet.bearer-choice-wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x020A, "application/vnd.phonecom.mmc-wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x020B, "application/vnd.nokia.syncset+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x020C, "image/x-up-wpng");
        WELL_KNOWN_MIME_TYPES.put(0x0300, "application/iota.mmc-wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x0301, "application/iota.mmc-xml");
        WELL_KNOWN_MIME_TYPES.put(0x0302, "application/vnd.syncml+xml");
        WELL_KNOWN_MIME_TYPES.put(0x0303, "application/vnd.syncml+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x0304, "text/vnd.wap.emn+xml");
        WELL_KNOWN_MIME_TYPES.put(0x0305, "text/calendar");
        WELL_KNOWN_MIME_TYPES.put(0x0306, "application/vnd.omads-email+xml");
        WELL_KNOWN_MIME_TYPES.put(0x0307, "application/vnd.omads-file+xml");
        WELL_KNOWN_MIME_TYPES.put(0x0308, "application/vnd.omads-folder+xml");
        WELL_KNOWN_MIME_TYPES.put(0x0309, "text/directory;profile=vCard");
        WELL_KNOWN_MIME_TYPES.put(0x030A, "application/vnd.wap.emn+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x030B, "application/vnd.nokia.ipdc-purchase-response");
        WELL_KNOWN_MIME_TYPES.put(0x030C, "application/vnd.motorola.screen3+xml");
        WELL_KNOWN_MIME_TYPES.put(0x030D, "application/vnd.motorola.screen3+gzip");
        WELL_KNOWN_MIME_TYPES.put(0x030E, "application/vnd.cmcc.setting+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x030F, "application/vnd.cmcc.bombing+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x0310, "application/vnd.docomo.pf");
        WELL_KNOWN_MIME_TYPES.put(0x0311, "application/vnd.docomo.ub");
        WELL_KNOWN_MIME_TYPES.put(0x0312, "application/vnd.omaloc-supl-init");
        WELL_KNOWN_MIME_TYPES.put(0x0313, "application/vnd.oma.group-usage-list+xml");
        WELL_KNOWN_MIME_TYPES.put(0x0314, "application/oma-directory+xml");
        WELL_KNOWN_MIME_TYPES.put(0x0315, "application/vnd.docomo.pf2");
        WELL_KNOWN_MIME_TYPES.put(0x0316, "application/vnd.oma.drm.roap-trigger+wbxml");
        WELL_KNOWN_MIME_TYPES.put(0x0317, "application/vnd.sbm.mid2");
        WELL_KNOWN_MIME_TYPES.put(0x0318, "application/vnd.wmf.bootstrap");
        WELL_KNOWN_MIME_TYPES.put(0x0319, "application/vnc.cmcc.dcd+xml");
        WELL_KNOWN_MIME_TYPES.put(0x031A, "application/vnd.sbm.cid");
        WELL_KNOWN_MIME_TYPES.put(0x031B, "application/vnd.oma.bcast.provisioningtrigger");

        WELL_KNOWN_PARAMETERS.put(0x00, "Q");
        WELL_KNOWN_PARAMETERS.put(0x01, "Charset");
        WELL_KNOWN_PARAMETERS.put(0x02, "Level");
        WELL_KNOWN_PARAMETERS.put(0x03, "Type");
        WELL_KNOWN_PARAMETERS.put(0x07, "Differences");
        WELL_KNOWN_PARAMETERS.put(0x08, "Padding");
        WELL_KNOWN_PARAMETERS.put(0x09, "Type");
        WELL_KNOWN_PARAMETERS.put(0x0E, "Max-Age");
        WELL_KNOWN_PARAMETERS.put(0x10, "Secure");
        WELL_KNOWN_PARAMETERS.put(0x11, "SEC");
        WELL_KNOWN_PARAMETERS.put(0x12, "MAC");
        WELL_KNOWN_PARAMETERS.put(0x13, "Creation-date");
        WELL_KNOWN_PARAMETERS.put(0x14, "Modification-date");
        WELL_KNOWN_PARAMETERS.put(0x15, "Read-date");
        WELL_KNOWN_PARAMETERS.put(0x16, "Size");
        WELL_KNOWN_PARAMETERS.put(0x17, "Name");
        WELL_KNOWN_PARAMETERS.put(0x18, "Filename");
        WELL_KNOWN_PARAMETERS.put(0x19, "Start");
        WELL_KNOWN_PARAMETERS.put(0x1A, "Start-info");
        WELL_KNOWN_PARAMETERS.put(0x1B, "Comment");
        WELL_KNOWN_PARAMETERS.put(0x1C, "Domain");
        WELL_KNOWN_PARAMETERS.put(0x1D, "Path");

        // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
        WELL_KNOWN_HEADERS.put(0x00, "Accept");
        WELL_KNOWN_HEADERS.put(0x01, "Accept-Charset");
        WELL_KNOWN_HEADERS.put(0x02, "Accept-Encoding");
        WELL_KNOWN_HEADERS.put(0x03, "Accept-Language");
        WELL_KNOWN_HEADERS.put(0x04, "Accept-Ranges");
        WELL_KNOWN_HEADERS.put(0x05, "Age");
        WELL_KNOWN_HEADERS.put(0x06, "Allow");
        WELL_KNOWN_HEADERS.put(0x07, "Authorization");
        WELL_KNOWN_HEADERS.put(0x08, "Cache-Control");
        WELL_KNOWN_HEADERS.put(0x09, "Connection");
        WELL_KNOWN_HEADERS.put(0x0A, "Content-Base");
        WELL_KNOWN_HEADERS.put(0x0B, "Content-Encoding");
        WELL_KNOWN_HEADERS.put(0x0C, "Content-Language");
        WELL_KNOWN_HEADERS.put(0x0D, "Content-Length");
        WELL_KNOWN_HEADERS.put(0x0E, "Content-Location");
        WELL_KNOWN_HEADERS.put(0x0F, "Content-MD5");

        WELL_KNOWN_HEADERS.put(0x10, "Content-Range");
        WELL_KNOWN_HEADERS.put(0x11, "Content-Type");
        WELL_KNOWN_HEADERS.put(0x12, "Date");
        WELL_KNOWN_HEADERS.put(0x13, "Etag");
        WELL_KNOWN_HEADERS.put(0x14, "Expires");
        WELL_KNOWN_HEADERS.put(0x15, "From");
        WELL_KNOWN_HEADERS.put(0x16, "Host");
        WELL_KNOWN_HEADERS.put(0x17, "If-Modified-Since");
        WELL_KNOWN_HEADERS.put(0x18, "If-Match");
        WELL_KNOWN_HEADERS.put(0x19, "If-None-Match");
        WELL_KNOWN_HEADERS.put(0x1A, "If-Range");
        WELL_KNOWN_HEADERS.put(0x1B, "If-Unmodified-Since");
        WELL_KNOWN_HEADERS.put(0x1C, "Location");
        WELL_KNOWN_HEADERS.put(0x1D, "Last-Modified");
        WELL_KNOWN_HEADERS.put(0x1E, "Max-Forwards");
        WELL_KNOWN_HEADERS.put(0x1F, "Pragma");

        WELL_KNOWN_HEADERS.put(0x20, "Proxy-Authenticate");
        WELL_KNOWN_HEADERS.put(0x21, "Proxy-Authorization");
        WELL_KNOWN_HEADERS.put(0x22, "Public");
        WELL_KNOWN_HEADERS.put(0x23, "Range");
        WELL_KNOWN_HEADERS.put(0x24, "Referer");
        WELL_KNOWN_HEADERS.put(0x25, "Retry-After");
        WELL_KNOWN_HEADERS.put(0x26, "Server");
        WELL_KNOWN_HEADERS.put(0x27, "Transfer-Encoding");
        WELL_KNOWN_HEADERS.put(0x28, "Upgrade");
        WELL_KNOWN_HEADERS.put(0x29, "User-Agent");
        WELL_KNOWN_HEADERS.put(0x2A, "Vary");
        WELL_KNOWN_HEADERS.put(0x2B, "Via");
        WELL_KNOWN_HEADERS.put(0x2C, "Warning");
        WELL_KNOWN_HEADERS.put(0x2D, "WWW-Authenticate");
        WELL_KNOWN_HEADERS.put(0x2E, "Content-Disposition");
        WELL_KNOWN_HEADERS.put(0x2F, "X-Wap-Application-Id");

        WELL_KNOWN_HEADERS.put(0x30, "X-Wap-Content-URI");
        WELL_KNOWN_HEADERS.put(0x31, "X-Wap-Initiator-URI");
        WELL_KNOWN_HEADERS.put(0x32, "Accept-Application");
        WELL_KNOWN_HEADERS.put(0x33, "Bearer-Indication");
        WELL_KNOWN_HEADERS.put(0x34, "Push-Flag");
        WELL_KNOWN_HEADERS.put(0x35, "Profile");
        WELL_KNOWN_HEADERS.put(0x36, "Profile-Diff");
        WELL_KNOWN_HEADERS.put(0x37, "Profile-Warning");
        WELL_KNOWN_HEADERS.put(0x38, "Expect");
        WELL_KNOWN_HEADERS.put(0x39, "TE");
        WELL_KNOWN_HEADERS.put(0x3A, "Trailer");
        WELL_KNOWN_HEADERS.put(0x3B, "Accept-Charset");
        WELL_KNOWN_HEADERS.put(0x3C, "Accept-Encoding");
        WELL_KNOWN_HEADERS.put(0x3D, "Cache-Control");
        WELL_KNOWN_HEADERS.put(0x3E, "Content-Range");
        WELL_KNOWN_HEADERS.put(0x3F, "X-Wap-Tod");

        WELL_KNOWN_HEADERS.put(0x40, "Content-ID");
        WELL_KNOWN_HEADERS.put(0x41, "Set-Cookie");
        WELL_KNOWN_HEADERS.put(0x42, "Cookie");
        WELL_KNOWN_HEADERS.put(0x43, "Encoding-Version");
        WELL_KNOWN_HEADERS.put(0x44, "Profile-Warning");
        WELL_KNOWN_HEADERS.put(0x45, "Content-Disposition");
        WELL_KNOWN_HEADERS.put(0x46, "X-WAP-Security");
        WELL_KNOWN_HEADERS.put(0x47, "Cache-Control");
        WELL_KNOWN_HEADERS.put(0x48, "Expect");
        WELL_KNOWN_HEADERS.put(0x49, "X-Wap-Loc-Invocation");
        WELL_KNOWN_HEADERS.put(0x4A, "X-Wap-Loc-Delivery");
        // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    }

    public static final String CONTENT_TYPE_B_PUSH_CO = "application/vnd.wap.coc";
    public static final String CONTENT_TYPE_B_MMS = "application/vnd.wap.mms-message";
    public static final String CONTENT_TYPE_B_PUSH_SYNCML_NOTI = "application/vnd.syncml.notification";

    byte[] wspData;
    int    dataLength;
    long   unsigned32bit;
    String stringValue;

    HashMap<String, String> contentParameters;
    HashMap<String, String> mHeaders;

    public WspTypeDecoder(byte[] pdu) {
        wspData = pdu;
    }

    /**
     * Decode the "Text-string" type for WSP pdu
     *
     * @param startIndex The starting position of the "Text-string" in this pdu
     *
     * @return false when error(not a Text-string) occur
     *         return value can be retrieved by getValueString() method length of data in pdu can be
     *         retrieved by getDecodedDataLength() method
     */
    public boolean decodeTextString(int startIndex) {
        int index = startIndex;
        while (wspData[index] != 0) {
            index++;
        }
        dataLength = index - startIndex + 1;
        if (wspData[startIndex] == 127) {
            stringValue = new String(wspData, startIndex + 1, dataLength - 2);
        } else {
            stringValue = new String(wspData, startIndex, dataLength - 1);
        }
        return true;
    }

    /**
     * Decode the "Token-text" type for WSP pdu
     *
     * @param startIndex The starting position of the "Token-text" in this pdu
     *
     * @return always true
     *         return value can be retrieved by getValueString() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeTokenText(int startIndex) {
        int index = startIndex;
        while (wspData[index] != 0) {
            index++;
        }
        dataLength = index - startIndex + 1;
        stringValue = new String(wspData, startIndex, dataLength - 1);

        return true;
    }

    /**
     * Decode the "Short-integer" type for WSP pdu
     *
     * @param startIndex The starting position of the "Short-integer" in this pdu
     *
     * @return false when error(not a Short-integer) occur
     *         return value can be retrieved by getValue32() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeShortInteger(int startIndex) {
        if ((wspData[startIndex] & 0x80) == 0) {
            return false;
        }
        unsigned32bit = wspData[startIndex] & 0x7f;
        dataLength = 1;
        return true;
    }

    /**
     * Decode the "Long-integer" type for WSP pdu
     *
     * @param startIndex The starting position of the "Long-integer" in this pdu
     *
     * @return false when error(not a Long-integer) occur
     *         return value can be retrieved by getValue32() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeLongInteger(int startIndex) {
        int lengthMultiOctet = wspData[startIndex] & 0xff;

        if (lengthMultiOctet > WAP_PDU_SHORT_LENGTH_MAX) {
            return false;
        }
        unsigned32bit = 0;
        for (int i = 1; i <= lengthMultiOctet; i++) {
            unsigned32bit = (unsigned32bit << 8) | (wspData[startIndex + i] & 0xff);
        }
        dataLength = 1 + lengthMultiOctet;
        return true;
    }

    /**
     * Decode the "Integer-Value" type for WSP pdu
     *
     * @param startIndex The starting position of the "Integer-Value" in this pdu
     *
     * @return false when error(not a Integer-Value) occur
     *         return value can be retrieved by getValue32() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeIntegerValue(int startIndex) {
        if (decodeShortInteger(startIndex) == true) {
            return true;
        }
        return decodeLongInteger(startIndex);
    }

    /**
     * Decode the "Uintvar-integer" type for WSP pdu
     *
     * @param startIndex The starting position of the "Uintvar-integer" in this pdu
     *
     * @return false when error(not a Uintvar-integer) occur
     *         return value can be retrieved by getValue32() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeUintvarInteger(int startIndex) {
        int index = startIndex;

        unsigned32bit = 0;
        while ((wspData[index] & 0x80) != 0) {
            if ((index - startIndex) >= 4) {
                return false;
            }
            unsigned32bit = (unsigned32bit << 7) | (wspData[index] & 0x7f);
            index++;
        }
        unsigned32bit = (unsigned32bit << 7) | (wspData[index] & 0x7f);
        dataLength = index - startIndex + 1;
        return true;
    }

    /**
     * Decode the "Value-length" type for WSP pdu
     *
     * @param startIndex The starting position of the "Value-length" in this pdu
     *
     * @return false when error(not a Value-length) occur
     *         return value can be retrieved by getValue32() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeValueLength(int startIndex) {
        if ((wspData[startIndex] & 0xff) > WAP_PDU_LENGTH_QUOTE) {
            return false;
        }
        if (wspData[startIndex] < WAP_PDU_LENGTH_QUOTE) {
            unsigned32bit = wspData[startIndex];
            dataLength = 1;
        } else {
            decodeUintvarInteger(startIndex + 1);
            dataLength++;
        }
        return true;
    }

    /**
     * Decode the "Extension-media" type for WSP PDU.
     *
     * @param startIndex The starting position of the "Extension-media" in this PDU.
     *
     * @return false on error, such as if there is no Extension-media at startIndex.
     *         Side-effects: updates stringValue (available with
     *         getValueString()), which will be null on error. The length of the
     *         data in the PDU is available with getValue32(), 0 on error.
     */
    public boolean decodeExtensionMedia(int startIndex) {
        int index = startIndex;
        dataLength = 0;
        stringValue = null;
        int length = wspData.length;
        boolean rtrn = index < length;

        while (index < length && wspData[index] != 0) {
            index++;
        }

        dataLength = index - startIndex + 1;
        stringValue = new String(wspData, startIndex, dataLength - 1);

        return rtrn;
    }

    /**
     * Decode the "Constrained-encoding" type for WSP pdu
     *
     * @param startIndex The starting position of the "Constrained-encoding" in this pdu
     *
     * @return false when error(not a Constrained-encoding) occur
     *         return value can be retrieved first by getValueString() and second by getValue32() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeConstrainedEncoding(int startIndex) {
        if (decodeShortInteger(startIndex) == true) {
            stringValue = null;
            return true;
        }
        return decodeExtensionMedia(startIndex);
    }

    /**
     * Decode the "Content-type" type for WSP pdu
     *
     * @param startIndex The starting position of the "Content-type" in this pdu
     *
     * @return false when error(not a Content-type) occurs
     *         If a content type exists in the headers (either as inline string, or as well-known
     *         value), getValueString() will return it. If a 'well known value' is encountered that
     *         cannot be mapped to a string mime type, getValueString() will return null, and
     *         getValue32() will return the unknown content type value.
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     *         Any content type parameters will be accessible via getContentParameters()
     */
    public boolean decodeContentType(int startIndex) {
        int mediaPrefixLength;
        contentParameters = new HashMap<String, String>();

        try {
            if (decodeValueLength(startIndex) == false) {
                boolean found = decodeConstrainedEncoding(startIndex);
                if (found) {
                    expandWellKnownMimeType();
                }
                return found;
            }
            int headersLength = (int) unsigned32bit;
            mediaPrefixLength = getDecodedDataLength();
            if (decodeIntegerValue(startIndex + mediaPrefixLength) == true) {
                dataLength += mediaPrefixLength;
                int readLength = dataLength;
                stringValue = null;
                expandWellKnownMimeType();
                long wellKnownValue = unsigned32bit;
                String mimeType = stringValue;
                if (readContentParameters(startIndex + dataLength,
                        (headersLength - (dataLength - mediaPrefixLength)), 0)) {
                    dataLength += readLength;
                    unsigned32bit = wellKnownValue;
                    stringValue = mimeType;
                    return true;
                }
                return false;
            }
            if (decodeExtensionMedia(startIndex + mediaPrefixLength) == true) {
                dataLength += mediaPrefixLength;
                int readLength = dataLength;
                expandWellKnownMimeType();
                long wellKnownValue = unsigned32bit;
                String mimeType = stringValue;
                if (readContentParameters(startIndex + dataLength,
                        (headersLength - (dataLength - mediaPrefixLength)), 0)) {
                    dataLength += readLength;
                    unsigned32bit = wellKnownValue;
                    stringValue = mimeType;
                    return true;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            //something doesn't add up
            return false;
        }
        return false;
    }

    private boolean readContentParameters(int startIndex, int leftToRead, int accumulator) {

        int totalRead = 0;

        if (leftToRead > 0) {
            byte nextByte = wspData[startIndex];
            String value = null;
            String param = null;
            if ((nextByte & 0x80) == 0x00 && nextByte > 31) { // untyped
                decodeTokenText(startIndex);
                param = stringValue;
                totalRead += dataLength;
            } else { // typed
                if (decodeIntegerValue(startIndex)) {
                    totalRead += dataLength;
                    int wellKnownParameterValue = (int) unsigned32bit;
                    param = WELL_KNOWN_PARAMETERS.get(wellKnownParameterValue);
                    if (param == null) {
                        param = "unassigned/0x" + Long.toHexString(wellKnownParameterValue);
                    }
                    // special case for the "Q" parameter, value is a uintvar
                    if (wellKnownParameterValue == Q_VALUE) {
                        if (decodeUintvarInteger(startIndex + totalRead)) {
                            totalRead += dataLength;
                            value = String.valueOf(unsigned32bit);
                            contentParameters.put(param, value);
                            return readContentParameters(startIndex + totalRead, leftToRead
                                                            - totalRead, accumulator + totalRead);
                        } else {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            }

            if (decodeNoValue(startIndex + totalRead)) {
                totalRead += dataLength;
                value = null;
            } else if (decodeIntegerValue(startIndex + totalRead)) {
                totalRead += dataLength;
                int intValue = (int) unsigned32bit;
                value = String.valueOf(intValue);
            } else {
                decodeTokenText(startIndex + totalRead);
                totalRead += dataLength;
                value = stringValue;
                if (value.startsWith("\"")) {
                    // quoted string, so remove the quote
                    value = value.substring(1);
                }
            }
            contentParameters.put(param, value);
            return readContentParameters(startIndex + totalRead, leftToRead - totalRead,
                                            accumulator + totalRead);

        } else {
            dataLength = accumulator;
            return true;
        }
    }

    /**
     * Check if the next byte is No-Value
     *
     * @param startIndex The starting position of the "Content length" in this pdu
     *
     * @return true if and only if the next byte is 0x00
     */
    private boolean decodeNoValue(int startIndex) {
        if (wspData[startIndex] == 0) {
            dataLength = 1;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Populate stringValue with the mime type corresponding to the value in unsigned32bit
     *
     * Sets unsigned32bit to -1 if stringValue is already populated
     */
    private void expandWellKnownMimeType() {
        if (stringValue == null) {
            int binaryContentType = (int) unsigned32bit;
            stringValue = WELL_KNOWN_MIME_TYPES.get(binaryContentType);
        } else {
            unsigned32bit = -1;
        }
    }

    /**
     * Decode the "Content length" type for WSP pdu
     *
     * @param startIndex The starting position of the "Content length" in this pdu
     *
     * @return false when error(not a Content length) occur
     *         return value can be retrieved by getValue32() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeContentLength(int startIndex) {
        return decodeIntegerValue(startIndex);
    }

    /**
     * Decode the "Content location" type for WSP pdu
     *
     * @param startIndex The starting position of the "Content location" in this pdu
     *
     * @return false when error(not a Content location) occur
     *         return value can be retrieved by getValueString() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeContentLocation(int startIndex) {
        return decodeTextString(startIndex);
    }

    /**
     * Decode the "X-Wap-Application-Id" type for WSP pdu
     *
     * @param startIndex The starting position of the "X-Wap-Application-Id" in this pdu
     *
     * @return false when error(not a X-Wap-Application-Id) occur
     *         return value can be retrieved first by getValueString() and second by getValue32()
     *         method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeXWapApplicationId(int startIndex) {
        if (decodeIntegerValue(startIndex) == true) {
            stringValue = null;
            return true;
        }
        return decodeTextString(startIndex);
    }

    /**
     * Seek for the "X-Wap-Application-Id" field for WSP pdu
     *
     * @param startIndex The starting position of seek pointer
     * @param endIndex Valid seek area end point
     *
     * @return false when error(not a X-Wap-Application-Id) occur
     *         return value can be retrieved by getValue32()
     */
    public boolean seekXWapApplicationId(int startIndex, int endIndex) {
        int index = startIndex;

        try {
            for (index = startIndex; index <= endIndex; ) {
                /**
                 * 8.4.1.1  Field name
                 * Field name is integer or text.
                 */
                if (decodeIntegerValue(index)) {
                    int fieldValue = (int) getValue32();

                    if (fieldValue == PARAMETER_ID_X_WAP_APPLICATION_ID) {
                        unsigned32bit = index + 1;
                        return true;
                    }
                } else {
                    if (!decodeTextString(index)) return false;
                }
                index += getDecodedDataLength();
                if (index > endIndex) return false;

                /**
                 * 8.4.1.2 Field values
                 * Value Interpretation of First Octet
                 * 0 - 30 This octet is followed by the indicated number (0 - 30)
                        of data octets
                 * 31 This octet is followed by a uintvar, which indicates the number
                 *      of data octets after it
                 * 32 - 127 The value is a text string, terminated by a zero octet
                        (NUL character)
                 * 128 - 255 It is an encoded 7-bit value; this header has no more data
                 */
                byte val = wspData[index];
                if (0 <= val && val <= WAP_PDU_SHORT_LENGTH_MAX) {
                    index += wspData[index] + 1;
                } else if (val == WAP_PDU_LENGTH_QUOTE) {
                    if (index + 1 >= endIndex) return false;
                    index++;
                    if (!decodeUintvarInteger(index)) return false;
                    index += getDecodedDataLength();
                } else if (WAP_PDU_LENGTH_QUOTE < val && val <= 127) {
                    if (!decodeTextString(index)) return false;
                    index += getDecodedDataLength();
                } else {
                    index++;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            //seek application ID failed. WSP header might be corrupted
            return false;
        }
        return false;
    }

    /**
     * Decode the "X-Wap-Content-URI" type for WSP pdu
     *
     * @param startIndex The starting position of the "X-Wap-Content-URI" in this pdu
     *
     * @return false when error(not a X-Wap-Content-URI) occur
     *         return value can be retrieved by getValueString() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeXWapContentURI(int startIndex) {
        return decodeTextString(startIndex);
    }

    /**
     * Decode the "X-Wap-Initiator-URI" type for WSP pdu
     *
     * @param startIndex The starting position of the "X-Wap-Initiator-URI" in this pdu
     *
     * @return false when error(not a X-Wap-Initiator-URI) occur
     *         return value can be retrieved by getValueString() method
     *         length of data in pdu can be retrieved by getDecodedDataLength() method
     */
    public boolean decodeXWapInitiatorURI(int startIndex) {
        return decodeTextString(startIndex);
    }

    /**
     * The data length of latest operation.
     */
    public int getDecodedDataLength() {
        return dataLength;
    }

    /**
     * The 32-bits result of latest operation.
     */
    public long getValue32() {
        return unsigned32bit;
    }

    /**
     * The String result of latest operation.
     */
    public String getValueString() {
        return stringValue;
    }

    /**
     * Any parameters encountered as part of a decodeContentType() invocation.
     *
     * @return a map of content parameters keyed by their names, or null if
     *         decodeContentType() has not been called If any unassigned
     *         well-known parameters are encountered, the key of the map will be
     *         'unassigned/0x...', where '...' is the hex value of the
     *         unassigned parameter.  If a parameter has No-Value the value will be null.
     *
     */
    public HashMap<String, String> getContentParameters() {
        return contentParameters;
    }
    
    /**
     * Decode the "Headers" for WSP pdu headers
     *
     * @param startIndex The starting position of the "Headers" in this pdu
     *        headerLength The Headers Length
     *
     * @return a map of headers keyed by their names, which can be get by
     *         getHeaders().
     *          
     * @Notes  The headers name may be encoded with integer value or in text format, and the well known name will 
     *         be expanded to a string. If it can not be expanded to a string, the integer value will be transformed
     *         to a string.
     *         
     *         The headers value may be encoded with integer value or in text format, and the integer value will be
     *         transformed to a string.
     *         
     *      
     */
    public void decodeHeaders(int startIndex, int headerLength) {
        mHeaders = new HashMap<String, String>();
        String headerName = null;
        String headerValue = null;
        int intValues;

        int index = startIndex;
        while (index < startIndex + headerLength) {
            decodeHeaderFieldName(index);
            index += getDecodedDataLength();
            expandWellKnownHeadersName();
            intValues = (int) unsigned32bit;
            if (stringValue != null) {
                headerName = stringValue;
            } else if (intValues >= 0) {
                headerName = String.valueOf(intValues);
            } else {
                continue;
            }

            decodeHeaderFieldValues(index);
            index += getDecodedDataLength();
            intValues = (int) unsigned32bit;
            if (stringValue != null) {
                headerValue = stringValue;
            } else if (intValues >= 0) {
                headerValue = String.valueOf(intValues);
            } else {
                continue;
            }

            mHeaders.put(headerName, headerValue);
        }
    }

    /**
     * Decode the "Field Name" for WSP pdu headers
     *
     * @param startIndex The starting position of the "Field Name" in this pdu
     *
     * @return return value can be retrieved by getValueString() or getValue32() method
     *         
     * @Notes  According OMA-WAP-TS-WSP-V1_0-20020290-C,
     *         the well-known header field name should be binany-coded.
     *         Page code shifting is currently not supported. 
     *         
     */
    public boolean decodeHeaderFieldName(int startIndex) {
        // Log.i("WAP PUSH", "startIndex:" + startIndex + "data:" + (wspData[startIndex]));
        if (decodeShortInteger(startIndex) == true) {
            stringValue = null;
            return true;
        } else {
            return decodeTextString(startIndex);
        }
    }

    /**
     * Decode the "Field values" for WSP pdu headers
     *
     * @param startIndex The starting position of the "Field values" in this pdu
     *
     * @return return value can be retrieved by getValueString() or getValue32() method 
     *         
     * @Notes  See OMA-WAP-TS-WSP-V1_0-20020290-C, Page 81, Field values.
     *           
     *      
     */
    public boolean decodeHeaderFieldValues(int startIndex) {
        byte first = wspData[startIndex];
        // Log.i("WAP PUSH", "startIndex:" + startIndex + "data:"+ wspData[startIndex]);
        if ((first == WAP_PDU_LENGTH_QUOTE)
                && decodeUintvarInteger(startIndex + 1)) {
            stringValue = null;
            dataLength++;
            return true;
        }

        if (decodeIntegerValue(startIndex) == true) {
            stringValue = null;
            return true;
        }
        return decodeTextString(startIndex);
    }

    public void expandWellKnownHeadersName() {
        if (stringValue == null) {
            int binaryHeadersName = (int) unsigned32bit;
            stringValue = WELL_KNOWN_HEADERS.get(binaryHeadersName);
        } else {
            unsigned32bit = -1;
        }
    }

    public HashMap<String, String> getHeaders() {
        expandWellKnownXWapApplicationIdName();
        return mHeaders;
    }

    /*
     * Notes: use for expand well known x-application-id 
     * 
     * See OMNA Push Application ID
     */
    private final static HashMap<Integer, String> WELL_KNOWN_X_WAP_APPLICATION_ID =
        new HashMap<Integer, String>();
    static{
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x00, "x-wap-application:*");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x01, "x-wap-application:push.sia");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x02, "x-wap-application:wml.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x03, "x-wap-application:wta.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x04, "x-wap-application:mms.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x05, "x-wap-application:push.syncml");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x06, "x-wap-application:loc.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x07, "x-wap-application:syncml.dm");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x08, "x-wap-application:drm.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x09, "x-wap-application:emn.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x0A, "x-wap-application:wv.ua");
    	//0x0B~0F unused
    	
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x10, "x-oma-application:ulp.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x11, "x-oma-application:dlota.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x12, "x-oma-application:java-ams");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x13, "x-oma-application:bcast.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x14, "x-oma-application:dpe.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x15, "x-oma-application:cpm:ua");
    	
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8000, "x-wap-microsoft:localcontent.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8001, "x-wap-microsoft:IMclient.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8002, "x-wap-docomo:imode.mail.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8003, "x-wap-docomo:imode.mr.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8004, "x-wap-docomo:imode.mf.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8005, "x-motorola:location.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8006, "x-motorola:now.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8007, "x-motorola:otaprov.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8008, "x-motorola:browser.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8009, "x-motorola:splash.ua");
    	//0x800A not used
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x800B, "x-wap-nai:mvsw.command");
    	//0800C~0800F not used
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x8010, "x-wap-openwave:iota.ua");
    	
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9000, "x-wap-docomo:imode.mail2.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9001, "x-oma-nec:otaprov.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9002, "x-oma-nokia:call.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9003, "x-oma-coremobility:sqa.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9004, "x-oma-docomo:doja.jam.ua");
    	
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9010, "x-oma-nokia:sip.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9011, "x-oma-vodafone:otaprov.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9012, "x-hutchison:ad.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9013, "x-oma-nokia:voip.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9014, "x-oma-docomo:voice.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9015, "x-oma-docomo:browser.ctl");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9016, "x-oma-docomo:dan.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9017, "x-oma-nokia:vs.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9018, "x-oma-nokia:voip.ext1.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9019, "x-wap-vodafone:casting.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x901A, "x-oma-docomo:imode.data.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x901B, "x-oma-snapin:otaprov.ctl");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x901C, "x-oma-nokia:vrs.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x901D, "x-oma-nokia:vrpg.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x901E, "x-oma-motorola:screen3.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x901F, "x-oma-docomo:device.ctl");
    	
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9020, "x-oma-nokia:msc.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9021, "x-3gpp2:lcs.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9022, "x-wap-vodafone:dcd.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9023, "x-3gpp:mbms.service.announcement.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9024, "x-oma-vodafone:dltmtbl.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9025, "x-oma-vodafone:dvcctl.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9026, "x-oma-cmcc:mail.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9027, "x-oma-nokia:vmb.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9028, "x-oma-nokia:ldapss.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9029, "x-hutchison:al.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x902A, "x-oma-nokia:uma.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x902B, "x-oma-nokia:news.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x902C, "x-oma-docomo:pf");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x902D, "x-oma-docomo:ub>");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x902E, "x-oma-nokia:nat.traversal.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x902F, "x-oma-intromobile:intropad.ua");
    	
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9030, "x-oma-docomo:uin.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9031, "x-oma-nokia:iptv.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9032, "x-hutchison:il.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9033, "x-oma-nokia:voip.general.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9034, "x-microsoft:drm.meter");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9035, "x-microsoft:drm.license");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9036, "x-oma-docomo:ic.ctl");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9037, "x-oma-slingmedia:SPM.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9038, "x-cibenix:odp.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9039, "x-oma-motorola:voip.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x903A, "x-oma-motorola:ims");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x903B, "x-oma-docomo:imode.remote.ctl");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x903C, "x-oma-docomo:device.ctl.um");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x903D, "x-microsoft:playready.drm.initiator");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x903E, "x-microsoft:playready.drm");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x903F, "x-oma-sbm:ms.mexa.ua");
    	
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9040, "urn:oma:drms:org-LGE:L650V");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9041, "x-oma-docomo:um");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9042, "x-oma-docomo:uin.um");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9043, "urn:oma:drms:org-LGE:KU450");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9044, "x-wap-microsoft:cfgmgr.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9045, "x-3gpp:mbms.download.delivery.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9046, "x-oma-docomo:star.ctl");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9047, "urn:oma:drms:org-LGE:KU380");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9048, "x-oma-docomo:pf2");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9049, "x-oma-motorola:blogcentral.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x904A, "x-oma-docomo:imode.agent.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x904B, "x-wap-application:push.sia");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x904C, "x-oma-nokia:destination.network.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x904D, "x-oma-sbm:mid2.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x904E, "x-carrieriq:avm.ctl");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x904F, "x-oma-sbm:ms.xml.ua");
    	
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9050, "urn:dvb:ipdc:notification:2008");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9051, "x-oma-docomo:imode.mvch.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9052, "x-oma-motorola:webui.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9053, "x-oma-sbm:cid.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9054, "x-oma-nokia:vcc.v1.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9055, "x-oma-docomo:open.ctl");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9056, "x-oma-docomo:sp.mail.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9057, "x-essoy-application:push.erace");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9058, "x-oma-docomo:open.fu");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9059, "x-samsung:osp.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x905A, "x-oma-docomo:imode.mchara.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x905B, "X-Wap-Application-Id:x-oma-application: scidm.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x905C, "x-oma-docomo:xmd.mail.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x905D, "x-oma-application:pal.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x905E, "x-oma-docomo:imode.relation.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x905F, "x-oma-docomo:xmd.storage.ua");
    	WELL_KNOWN_X_WAP_APPLICATION_ID.put(0x9060, "x-oma-docomo:xmd.lcsapp.ua");
    }

    /*
     * Notes: The well known X-Wap-Application-Id will be expand to String.
     */

    public void expandWellKnownXWapApplicationIdName() {
        String X_WAP_APPLICATION_ID = "X-Wap-Application-Id";
        int binaryCode = -1;
        try {
            binaryCode = Integer.valueOf(mHeaders.get(X_WAP_APPLICATION_ID));
        } catch (Exception e) {
            return;
        }
        if (binaryCode != -1) {
            String value = WELL_KNOWN_X_WAP_APPLICATION_ID.get(binaryCode);
            if (value != null) {
                mHeaders.put(X_WAP_APPLICATION_ID, value);
            }
        }
    }
}
