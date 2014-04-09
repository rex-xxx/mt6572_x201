/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 */

package com.android.exchange.adapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpUriRequest;

import com.android.exchange.EasResponse;
import com.android.exchange.EasSyncService;
import com.android.exchange.EasSyncServiceTests;

public class MockEasResponse extends EasResponse implements
        EasResponse.Callbacks {

    private InputStream mInputStream;
    private boolean mClosed = false;
    private static int sCallNums = 0;
    private static int sNum = 0;
    private int mStatus = HttpStatus.SC_OK;
    /**
     * Whether or not a certificate was requested by the server and missing. If
     * this is set, it is essentially a 403 whereby the failure was due
     */
    private boolean mClientCertRequested = false;

    @Override
    public EasResponse fromHttpRequest(HttpUriRequest request)
            throws IOException {
        URI uri = request.getURI();
        String cmd = uri.toString();
        Serializer s = new Serializer();
        if (cmd.indexOf("ItemOperations") != -1) {
            s.start(Tags.ITEMS_ITEMS).tag(Tags.ITEMS).data(Tags.ITEMS_STATUS,
                    "1").start(Tags.ITEMS_RESPONSE).tag(Tags.ITEMS).start(
                    Tags.ITEMS_FETCH).tag(Tags.ITEMS).data(Tags.ITEMS_STATUS,
                    "2").data(Tags.SYNC_SERVER_ID, "serverid message1").tag(
                    Tags.SEARCH_LONG_ID).start(Tags.ITEMS_PROPERTIES).data(
                    Tags.EMAIL_BODY, "body").end().end().end().end().done();
        } else if (cmd.indexOf("GetItemEstimate") != -1) {
            s.start(Tags.GIE_GET_ITEM_ESTIMATE).start(
                    Tags.GIE_RESPONSE).data(Tags.GIE_STATUS, "2")
                    .start(Tags.GIE_COLLECTION).data(
                            Tags.GIE_COLLECTION_ID, "test_id").data(
                            Tags.GIE_ESTIMATE, "110").end().end().end().done();
        } else if (cmd.indexOf("Search") != -1) {
            s.start(Tags.SEARCH_SEARCH).start(Tags.SEARCH_RESPONSE).start(
                    Tags.SEARCH_STORE).data(Tags.SEARCH_TOTAL, "10").end()
                    .end().end().done();
        } else if (cmd.indexOf("MoveItems") != -1) {
            s.start(Tags.MOVE_MOVE_ITEMS).start(Tags.MOVE_RESPONSE).data(
                    Tags.MOVE_STATUS, "3").data(Tags.MOVE_DSTMSGID, "10086")
                    .end().end().done();
        } else if (cmd.indexOf("MeetingResponse") != -1) {
            s.start(Tags.MREQ_MEETING_RESPONSE).end().done();
        } else if (cmd.indexOf("autodiscover") != -1) {
            InputStream is = EasSyncServiceTests.openTestFile();
            byte[] buf = new byte[is.available()];
            is.read(buf);
            MockEasResponse mockEasResponse = new MockEasResponse();
            mockEasResponse.mInputStream = new ByteArrayInputStream(buf);
            is.close();
            return mockEasResponse;
        } else if (cmd.indexOf("Provision") != -1) {
            if (sCallNums < 3) {
                sCallNums++;
                s.start(Tags.PROVISION_PROVISION).data(Tags.PROVISION_STATUS,
                        "1").tag(Tags.PROVISION_REMOTE_WIPE).start(
                        Tags.PROVISION_POLICIES).start(Tags.PROVISION_POLICY)
                        .data(Tags.PROVISION_POLICY_KEY, "sync-key").data(
                                Tags.PROVISION_POLICY_TYPE,
                                EasSyncService.EAS_12_POLICY_TYPE).start(
                                Tags.PROVISION_DATA).start(
                                Tags.PROVISION_EAS_PROVISION_DOC).data(
                                Tags.PROVISION_ALLOW_INTERNET_SHARING, "0")
                        .end().end().end().end().end().done();
            } else {
                sCallNums++;
                s.start(Tags.PROVISION_PROVISION).data(Tags.PROVISION_STATUS,
                        "1").start(Tags.PROVISION_POLICIES).start(
                        Tags.PROVISION_POLICY).data(Tags.PROVISION_POLICY_KEY,
                        "sync-key").data(Tags.PROVISION_POLICY_TYPE,
                        EasSyncService.EAS_12_POLICY_TYPE).start(
                        Tags.PROVISION_DATA).start(
                        Tags.PROVISION_EAS_PROVISION_DOC).data(
                        Tags.PROVISION_ALLOW_INTERNET_SHARING, "1").end().end()
                        .end().end().end().done();
                if (sCallNums == 5) {
                    sCallNums = 0;
                }
            }
        } else if (cmd.indexOf("Settings") != -1) {
            if (sNum < 2 || sNum == 3) {
                sNum++;
                s.start(Tags.SETTINGS_SETTINGS).data(Tags.SETTINGS_STATUS, "1")
                .start(Tags.SETTINGS_OOF).data(Tags.SETTINGS_STATUS,"1")
                .end().end().done();
            } else if (sNum == 2) {
                sNum++;
                s.start(Tags.SETTINGS_SETTINGS).data(Tags.SETTINGS_STATUS, "1")
                .start(Tags.SETTINGS_OOF).data(Tags.SETTINGS_STATUS,"1")
                .start(Tags.SETTINGS_GET).data(Tags.SETTINGS_OOF_STATE, "2")
                .data(Tags.SETTINGS_START_TIME, "2012-10-30T08:00:00.000Z")
                .data(Tags.SETTINGS_END_TIME, "2012-11-30T08:00:00.000Z")
                .tag(Tags.SETTINGS_BODY_TYPE)
                .start(Tags.SETTINGS_OOF_MESSAGE).tag(Tags.SETTINGS_APPLIES_TO_INTERNAL)
                .tag(Tags.SETTINGS_APPLIES_TO_EXTERNAL_KNOWN)
                .data(Tags.SETTINGS_ENABLED, "1").data(Tags.SETTINGS_REPLY_MESSAGE, "hello")
                .end().end().end().end().done();
            } else if (sNum == 4) {
                sNum = 0;
                s.start(Tags.SETTINGS_SETTINGS).data(Tags.SETTINGS_STATUS, "1")
                .start(Tags.SETTINGS_OOF).data(Tags.SETTINGS_STATUS,"1")
                .start(Tags.SETTINGS_GET).data(Tags.SETTINGS_OOF_STATE, "2")
                .data(Tags.SETTINGS_START_TIME, "2012-10-30T08:00:00.000Z")
                .data(Tags.SETTINGS_END_TIME, "2012-11-30T08:00:00.000Z")
                .tag(Tags.SETTINGS_BODY_TYPE)
                .start(Tags.SETTINGS_OOF_MESSAGE).tag(Tags.SETTINGS_APPLIES_TO_INTERNAL)
                .tag(Tags.SETTINGS_APPLIES_TO_EXTERNAL_KNOWN)
                .data(Tags.SETTINGS_ENABLED, "0").data(Tags.SETTINGS_REPLY_MESSAGE, "hello")
                .end().end().end().end().done();
            }
        } else if (cmd.indexOf("SmartReply") != -1) {
            s.start(Tags.COMPOSE_SMART_REPLY).data(Tags.COMPOSE_STATUS, "150")
                    .tag(Tags.COMPOSE).end().done();
        } else if (cmd.indexOf("SendMail") != -1) {
            s.start(Tags.COMPOSE_SEND_MAIL).data(Tags.COMPOSE_STATUS, "150")
                    .tag(Tags.COMPOSE).end().done();
            mStatus = 449;
        } else if (cmd.indexOf("FolderSync") != -1) {
            s.start(Tags.FOLDER_FOLDER_SYNC).data(Tags.FOLDER_STATUS, "1")
                    .data(Tags.FOLDER_SYNC_KEY, "1").start(Tags.FOLDER_CHANGES)
                    .data(Tags.FOLDER_COUNT, "1").start(Tags.FOLDER_ADD).data(
                            Tags.FOLDER_SERVER_ID, "3").data(
                            Tags.FOLDER_PARENT_ID, "0").data(
                            Tags.FOLDER_DISPLAY_NAME, "mockFolder").data(
                            Tags.FOLDER_TYPE, "12").end().end().end().done();
        } else if (cmd.indexOf("Ping") != -1) {
            s.start(Tags.PING_PING).data(Tags.PING_STATUS, "2").tag(
                    Tags.PING_PING).start(Tags.PING_FOLDERS).data(
                    Tags.PING_FOLDER, "0").tag(Tags.PING_PING).end().end()
                    .done();
        } else if (cmd.indexOf("Sync") != -1) {
            s.start(Tags.SYNC_SYNC).end().done();
        }

        byte[] bytes = s.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return new MockEasResponse(inputStream);
    }

    public MockEasResponse(InputStream inputStream) {
        this.mInputStream = inputStream;
    }

    public MockEasResponse() {
        super();
    }

    @Override
    public void close() {
        if (!mClosed) {
            if (mInputStream instanceof GZIPInputStream) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    // We tried
                }
            }
            mClosed = true;
        }
    }

    @Override
    public Header getHeader(String name) {
        if ("MS-ASProtocolCommands".equalsIgnoreCase(name)) {
            return new MockHeader(name, "Commands-0");
        } else if ("ms-asprotocolversions".equalsIgnoreCase(name)) {
            return new MockHeader(name, MockHeader.PROTOCAL_VALUES);
        } else if ("X-MS-Location".equalsIgnoreCase(name)) {
            return new MockHeader(name, MockHeader.URI_VALUES);
        } else {
            return super.getHeader(name);
        }
    }

    @Override
    public InputStream getInputStream() {
        return mInputStream;
    }

    @Override
    public int getLength() {
        int len = 0;
        try {
            len = mInputStream.available();
        } catch (Exception e) {
        }
        return len;
    }

    @Override
    public int getStatus() {
        return mStatus;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isMissingCertificate() {
        return super.isMissingCertificate();
    }

    public static void setSettingNums(int num) {
        sNum = num;
    }

    public static int getSettingNums() {
        return sNum;
    }

    private class MockHeader implements Header {
        private String name;
        private String value = PROTOCAL_VALUES;
        public static final String PROTOCAL_VALUES = "1.0,2.0,2.5,12.0,12.1,14.0,14.1";
        public static final String URI_VALUES = "http://bob@google.com:80";

        public MockHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public HeaderElement[] getElements() throws ParseException {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

    }
}
