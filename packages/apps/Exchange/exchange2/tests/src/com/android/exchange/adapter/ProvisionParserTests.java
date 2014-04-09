/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.exchange.adapter;

import com.android.emailcommon.provider.Policy;
import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.ProvisionParser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

import android.test.suitebuilder.annotation.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.adapter.ProvisionParserTests exchange
 */
@SmallTest
public class ProvisionParserTests extends SyncAdapterTestCase {
    private final ByteArrayInputStream mTestInputStream =
        new ByteArrayInputStream("ABCDEFG".getBytes());

    // A good sample of an Exchange 2003 (WAP) provisioning document for end-to-end testing
    private String mWapProvisioningDoc1 =
        "<wap-provisioningdoc>" +
            "<characteristic type=\"SecurityPolicy\"><parm name=\"4131\" value=\"0\"/>" +
            "</characteristic>" +
            "<characteristic type=\"Registry\">" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\\AE\\" +
                        "{50C13377-C66D-400C-889E-C316FC4AB374}\">" +
                    "<parm name=\"AEFrequencyType\" value=\"1\"/>" +
                    "<parm name=\"AEFrequencyValue\" value=\"5\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\">" +
                    "<parm name=\"DeviceWipeThreshold\" value=\"20\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\">" +
                    "<parm name=\"CodewordFrequency\" value=\"5\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\\LAP\\lap_pw\">" +
                    "<parm name=\"MinimumPasswordLength\" value=\"8\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\\LAP\\lap_pw\">" +
                    "<parm name=\"PasswordComplexity\" value=\"0\"/>" +
                "</characteristic>" +
            "</characteristic>" +
        "</wap-provisioningdoc>";

    // Provisioning document with passwords turned off
    private String mWapProvisioningDoc2 =
        "<wap-provisioningdoc>" +
            "<characteristic type=\"SecurityPolicy\"><parm name=\"4131\" value=\"1\"/>" +
            "</characteristic>" +
            "<characteristic type=\"Registry\">" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\\AE\\" +
                        "{50C13377-C66D-400C-889E-C316FC4AB374}\">" +
                    "<parm name=\"AEFrequencyType\" value=\"0\"/>" +
                    "<parm name=\"AEFrequencyValue\" value=\"5\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\">" +
                    "<parm name=\"DeviceWipeThreshold\" value=\"20\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\">" +
                    "<parm name=\"CodewordFrequency\" value=\"5\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\\LAP\\lap_pw\">" +
                    "<parm name=\"MinimumPasswordLength\" value=\"8\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\\LAP\\lap_pw\">" +
                    "<parm name=\"PasswordComplexity\" value=\"0\"/>" +
                "</characteristic>" +
            "</characteristic>" +
        "</wap-provisioningdoc>";

    // Provisioning document with simple password, 4 chars, 5 failures
    private String mWapProvisioningDoc3 =
        "<wap-provisioningdoc>" +
            "<characteristic type=\"SecurityPolicy\"><parm name=\"4131\" value=\"0\"/>" +
            "</characteristic>" +
            "<characteristic type=\"Registry\">" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\\AE\\" +
                        "{50C13377-C66D-400C-889E-C316FC4AB374}\">" +
                    "<parm name=\"AEFrequencyType\" value=\"1\"/>" +
                    "<parm name=\"AEFrequencyValue\" value=\"2\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\">" +
                    "<parm name=\"DeviceWipeThreshold\" value=\"5\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\">" +
                    "<parm name=\"CodewordFrequency\" value=\"5\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\\LAP\\lap_pw\">" +
                    "<parm name=\"MinimumPasswordLength\" value=\"4\"/>" +
                "</characteristic>" +
                "<characteristic type=\"HKLM\\Comm\\Security\\Policy\\LASSD\\LAP\\lap_pw\">" +
                    "<parm name=\"PasswordComplexity\" value=\"1\"/>" +
                "</characteristic>" +
            "</characteristic>" +
        "</wap-provisioningdoc>";

    public void testWapProvisionParser1() throws IOException {
        ProvisionParser parser = new ProvisionParser(mTestInputStream, getTestService());
        parser.parseProvisionDocXml(mWapProvisioningDoc1);
        Policy policy = parser.getPolicy();
        assertNotNull(policy);
        // Check the settings to make sure they were parsed correctly
        assertEquals(5*60, policy.mMaxScreenLockTime);  // Screen lock time is in seconds
        assertEquals(8, policy.mPasswordMinLength);
        assertEquals(Policy.PASSWORD_MODE_STRONG, policy.mPasswordMode);
        assertEquals(20, policy.mPasswordMaxFails);
        assertTrue(policy.mRequireRemoteWipe);
    }

    public void testWapProvisionParser2() throws IOException {
        ProvisionParser parser = new ProvisionParser(mTestInputStream, getTestService());
        parser.parseProvisionDocXml(mWapProvisioningDoc2);
        Policy policy = parser.getPolicy();
        assertNotNull(policy);
        // Password should be set to none; others are ignored in this case.
        assertEquals(Policy.PASSWORD_MODE_NONE, policy.mPasswordMode);
    }

    public void testWapProvisionParser3() throws IOException {
        ProvisionParser parser = new ProvisionParser(mTestInputStream, getTestService());
        parser.parseProvisionDocXml(mWapProvisioningDoc3);
        Policy policy = parser.getPolicy();
        assertNotNull(policy);
        // Password should be set to simple
        assertEquals(2*60, policy.mMaxScreenLockTime);  // Screen lock time is in seconds
        assertEquals(4, policy.mPasswordMinLength);
        assertEquals(Policy.PASSWORD_MODE_SIMPLE, policy.mPasswordMode);
        assertEquals(5, policy.mPasswordMaxFails);
        assertTrue(policy.mRequireRemoteWipe);
    }

    /**
     * M: Test the ProvisionParser
    */
    public void testProvisionParser() throws IOException {
        // Get the test service
        EasSyncService service = getTestService();

        // Set up an input stream
        Serializer s = new Serializer();
        s.start(Tags.PROVISION_PROVISION).tag(Tags.PROVISION).data(Tags.PROVISION_STATUS, "1")
        .tag(Tags.PROVISION_REMOTE_WIPE).start(Tags.SETTINGS_DEVICE_INFORMATION).tag(Tags.SETTINGS)
        .data(Tags.SETTINGS_STATUS, "2").end().start(Tags.PROVISION_POLICIES).tag(Tags.PROVISION)
        .start(Tags.PROVISION_POLICY).tag(Tags.PROVISION).data(Tags.PROVISION_STATUS, "3")
        .data(Tags.PROVISION_POLICY_TYPE, "MS-EAS-Provisioning-WBXML")
        .data(Tags.PROVISION_POLICY_KEY, "4").start(Tags.PROVISION_DATA).tag(Tags.PROVISION)
        .start(Tags.PROVISION_EAS_PROVISION_DOC).data(Tags.PROVISION_DEVICE_PASSWORD_ENABLED, "1")
        .data(Tags.PROVISION_MIN_DEVICE_PASSWORD_LENGTH, "6").data(Tags.PROVISION_ALLOW_CAMERA, "0")
        .data(Tags.PROVISION_ALPHA_DEVICE_PASSWORD_ENABLED, "1")
        .data(Tags.PROVISION_MAX_INACTIVITY_TIME_DEVICE_LOCK, "30")
        .data(Tags.PROVISION_MAX_DEVICE_PASSWORD_FAILED_ATTEMPTS, "5")
        .data(Tags.PROVISION_DEVICE_PASSWORD_EXPIRATION, "15")
        .data(Tags.PROVISION_DEVICE_PASSWORD_HISTORY, "10")
        .data(Tags.PROVISION_ALLOW_SIMPLE_DEVICE_PASSWORD, "0")
        .data(Tags.PROVISION_ALLOW_STORAGE_CARD, "0")
        .data(Tags.PROVISION_ALLOW_UNSIGNED_APPLICATIONS, "0")
        .data(Tags.PROVISION_ALLOW_UNSIGNED_INSTALLATION_PACKAGES, "0")
        .data(Tags.PROVISION_ALLOW_WIFI, "0")
        .data(Tags.PROVISION_ALLOW_TEXT_MESSAGING, "0")
        .data(Tags.PROVISION_ALLOW_POP_IMAP_EMAIL, "0")
        .data(Tags.PROVISION_ALLOW_IRDA, "0")
        .data(Tags.PROVISION_ALLOW_HTML_EMAIL, "0")
        .data(Tags.PROVISION_ALLOW_BROWSER, "0")
        .data(Tags.PROVISION_ALLOW_CONSUMER_EMAIL, "0")
        .data(Tags.PROVISION_ALLOW_INTERNET_SHARING, "0")
        .data(Tags.PROVISION_ATTACHMENTS_ENABLED, "0")
        .data(Tags.PROVISION_ALLOW_BLUETOOTH, "1")
        .data(Tags.PROVISION_REQUIRE_DEVICE_ENCRYPTION, "1")
        .data(Tags.PROVISION_DEVICE_ENCRYPTION_ENABLED, "1")
        .data(Tags.PROVISION_REQUIRE_MANUAL_SYNC_WHEN_ROAMING, "1")
        .data(Tags.PROVISION_PASSWORD_RECOVERY_ENABLED, "1")
        .data(Tags.PROVISION_REQUIRE_SIGNED_SMIME_MESSAGES, "1")
        .data(Tags.PROVISION_MAX_ATTACHMENT_SIZE, "10")
        .data(Tags.PROVISION_MIN_DEVICE_PASSWORD_COMPLEX_CHARS, "1")
        .tag(Tags.PROVISION_ALLOW_DESKTOP_SYNC)
        .start(Tags.PROVISION_UNAPPROVED_IN_ROM_APPLICATION_LIST)
        .tag(Tags.PROVISION)
        .tag(Tags.PROVISION_APPLICATION_NAME)
        .end()
        .start(Tags.PROVISION_APPROVED_APPLICATION_LIST)
        .tag(Tags.PROVISION)
        .tag(Tags.PROVISION_APPLICATION_NAME)
        .end()
        .data(Tags.PROVISION_MAX_CALENDAR_AGE_FILTER, "14")
        .data(Tags.PROVISION_MAX_EMAIL_AGE_FILTER, "50")
        .data(Tags.PROVISION_MAX_EMAIL_BODY_TRUNCATION_SIZE, "10")
        .data(Tags.PROVISION_MAX_EMAIL_HTML_BODY_TRUNCATION_SIZE, "10")
        .tag(Tags.PROVISION)
        .end().end().end().end().end().done();

        byte[] bytes = s.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ProvisionParser parser = new ProvisionParser(bis, service);

        boolean result = parser.parse();
        assertTrue(result);
        assertTrue(parser.getRemoteWipe());
        assertEquals("4", parser.getSecuritySyncKey());
        assertFalse(parser.hasSupportablePolicySet());

        Policy policy = parser.getPolicy();
        assertEquals(policy.mPasswordMode, Policy.PASSWORD_MODE_STRONG);
        assertEquals(policy.mPasswordMinLength, 6);
        assertEquals(policy.mMaxScreenLockTime, 30);
        assertEquals(policy.mPasswordMaxFails, 5);
        assertEquals(policy.mPasswordExpirationDays, 15);
        assertEquals(policy.mPasswordHistory, 10);
        assertTrue(policy.mDontAllowCamera);
        assertTrue(policy.mDontAllowAttachments);
        assertEquals(policy.mMaxAttachmentSize, 10);
        assertEquals(policy.mPasswordComplexChars, 1);
        assertEquals(policy.mMaxCalendarLookback, 14);
        assertEquals(policy.mMaxEmailLookback, 50);
        assertEquals(policy.mMaxTextTruncationSize, 10);
        assertEquals(policy.mMaxHtmlTruncationSize, 10);

        parser.setSecuritySyncKey("SecuritySyncKey");
        parser.clearUnsupportablePolicies();
        assertEquals("SecuritySyncKey", parser.getSecuritySyncKey());
    }
}
