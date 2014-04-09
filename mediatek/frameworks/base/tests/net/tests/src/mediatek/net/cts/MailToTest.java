/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

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

package mediatek.net.cts;

import android.net.MailTo;
import android.test.AndroidTestCase;
import android.util.Log;
import dalvik.annotation.TestTargets;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargetClass;

@TestTargetClass(MailTo.class)
public class MailToTest extends AndroidTestCase {
    private static final String MAILTOURI_1 = "mailto:chris@example.com";
    private static final String MAILTOURI_2 = "mailto:infobot@example.com?subject=current-issue";
    private static final String MAILTOURI_3 =
            "mailto:infobot@example.com?body=send%20current-issue";
    private static final String MAILTOURI_4 = "mailto:infobot@example.com?body=send%20current-" +
                                              "issue%0D%0Asend%20index";
    private static final String MAILTOURI_5 = "mailto:joe@example.com?" +
                                              "cc=bob@example.com&body=hello";
    private static final String MAILTOURI_6 = "mailto:?to=joe@example.com&" +
                                              "cc=bob@example.com&body=hello";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test parse mailto URI.",
            method = "parse",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test parse mailto URI.",
            method = "isMailTo",
            args = {java.lang.String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test parse mailto URI.",
            method = "getTo",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test parse mailto URI.",
            method = "getSubject",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test parse mailto URI.",
            method = "getBody",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test parse mailto URI.",
            method = "getCc",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test parse mailto URI.",
            method = "toString",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            notes = "Test parse mailto URI.",
            method = "getHeaders",
            args = {}
        )
    })
    public void testParseMailToURI() {
        assertFalse(MailTo.isMailTo(null));
        assertFalse(MailTo.isMailTo(""));
        assertFalse(MailTo.isMailTo("http://www.google.com"));

        assertTrue(MailTo.isMailTo(MAILTOURI_1));
        MailTo mailTo_1 = MailTo.parse(MAILTOURI_1);
        Log.d("Trace", mailTo_1.toString());
        assertEquals("chris@example.com", mailTo_1.getTo());
        assertEquals(1, mailTo_1.getHeaders().size());
        assertNull(mailTo_1.getBody());
        assertNull(mailTo_1.getCc());
        assertNull(mailTo_1.getSubject());
        assertEquals("mailto:?to=chris%40example.com&", mailTo_1.toString());

        assertTrue(MailTo.isMailTo(MAILTOURI_2));
        MailTo mailTo_2 = MailTo.parse(MAILTOURI_2);
        Log.d("Trace", mailTo_2.toString());
        assertEquals(2, mailTo_2.getHeaders().size());
        assertEquals("infobot@example.com", mailTo_2.getTo());
        assertEquals("current-issue", mailTo_2.getSubject());
        assertNull(mailTo_2.getBody());
        assertNull(mailTo_2.getCc());
        String stringUrl = mailTo_2.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("to=infobot%40example.com&"));
        assertTrue(stringUrl.contains("subject=current-issue&"));

        assertTrue(MailTo.isMailTo(MAILTOURI_3));
        MailTo mailTo_3 = MailTo.parse(MAILTOURI_3);
        Log.d("Trace", mailTo_3.toString());
        assertEquals(2, mailTo_3.getHeaders().size());
        assertEquals("infobot@example.com", mailTo_3.getTo());
        assertEquals("send current-issue", mailTo_3.getBody());
        assertNull(mailTo_3.getCc());
        assertNull(mailTo_3.getSubject());
        stringUrl = mailTo_3.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("to=infobot%40example.com&"));
        assertTrue(stringUrl.contains("body=send%20current-issue&"));

        assertTrue(MailTo.isMailTo(MAILTOURI_4));
        MailTo mailTo_4 = MailTo.parse(MAILTOURI_4);
        Log.d("Trace", mailTo_4.toString() + " " + mailTo_4.getBody());
        assertEquals(2, mailTo_4.getHeaders().size());
        assertEquals("infobot@example.com", mailTo_4.getTo());
        assertEquals("send current-issue\r\nsend index", mailTo_4.getBody());
        assertNull(mailTo_4.getCc());
        assertNull(mailTo_4.getSubject());
        stringUrl = mailTo_4.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("to=infobot%40example.com&"));
        assertTrue(stringUrl.contains("body=send%20current-issue%0D%0Asend%20index&"));


        assertTrue(MailTo.isMailTo(MAILTOURI_5));
        MailTo mailTo_5 = MailTo.parse(MAILTOURI_5);
        Log.d("Trace", mailTo_5.toString() + mailTo_5.getHeaders().toString()
                + mailTo_5.getHeaders().size());
        assertEquals(3, mailTo_5.getHeaders().size());
        assertEquals("joe@example.com", mailTo_5.getTo());
        assertEquals("bob@example.com", mailTo_5.getCc());
        assertEquals("hello", mailTo_5.getBody());
        assertNull(mailTo_5.getSubject());
        stringUrl = mailTo_5.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("cc=bob%40example.com&"));
        assertTrue(stringUrl.contains("body=hello&"));
        assertTrue(stringUrl.contains("to=joe%40example.com&"));

        assertTrue(MailTo.isMailTo(MAILTOURI_6));
        MailTo mailTo_6 = MailTo.parse(MAILTOURI_6);
        Log.d("Trace", mailTo_6.toString() + mailTo_6.getHeaders().toString()
                + mailTo_6.getHeaders().size());
        assertEquals(3, mailTo_6.getHeaders().size());
        assertEquals(", joe@example.com", mailTo_6.getTo());
        assertEquals("bob@example.com", mailTo_6.getCc());
        assertEquals("hello", mailTo_6.getBody());
        assertNull(mailTo_6.getSubject());
        stringUrl = mailTo_6.toString();
        assertTrue(stringUrl.startsWith("mailto:?"));
        assertTrue(stringUrl.contains("cc=bob%40example.com&"));
        assertTrue(stringUrl.contains("body=hello&"));
        assertTrue(stringUrl.contains("to=%2C%20joe%40example.com&"));
    }
}
