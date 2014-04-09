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

package mediatek.net.cts;

import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;

import android.net.NetworkInfo.DetailedState;
import android.test.AndroidTestCase;

@TestTargetClass(DetailedState.class)
public class NetworkInfo_DetailedStateTest extends AndroidTestCase {

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test valueOf(String name).",
        method = "valueOf",
        args = {java.lang.String.class}
    )
    public void testValueOf() {
        assertEquals(DetailedState.AUTHENTICATING, DetailedState.valueOf("AUTHENTICATING"));
        assertEquals(DetailedState.CONNECTED, DetailedState.valueOf("CONNECTED"));
        assertEquals(DetailedState.CONNECTING, DetailedState.valueOf("CONNECTING"));
        assertEquals(DetailedState.DISCONNECTED, DetailedState.valueOf("DISCONNECTED"));
        assertEquals(DetailedState.DISCONNECTING, DetailedState.valueOf("DISCONNECTING"));
        assertEquals(DetailedState.FAILED, DetailedState.valueOf("FAILED"));
        assertEquals(DetailedState.IDLE, DetailedState.valueOf("IDLE"));
        assertEquals(DetailedState.OBTAINING_IPADDR, DetailedState.valueOf("OBTAINING_IPADDR"));
        assertEquals(DetailedState.SCANNING, DetailedState.valueOf("SCANNING"));
        assertEquals(DetailedState.SUSPENDED, DetailedState.valueOf("SUSPENDED"));
    }

    @TestTargetNew(
        level = TestLevel.COMPLETE,
        notes = "Test values().",
        method = "values",
        args = {}
    )
    public void testValues() {
        DetailedState[] expected = DetailedState.values();
        assertEquals(12, expected.length);
        assertEquals(DetailedState.IDLE, expected[0]);
        assertEquals(DetailedState.SCANNING, expected[1]);
        assertEquals(DetailedState.CONNECTING, expected[2]);
        assertEquals(DetailedState.AUTHENTICATING, expected[3]);
        assertEquals(DetailedState.OBTAINING_IPADDR, expected[4]);
        assertEquals(DetailedState.CONNECTED, expected[5]);
        assertEquals(DetailedState.SUSPENDED, expected[6]);
        assertEquals(DetailedState.DISCONNECTING, expected[7]);
        assertEquals(DetailedState.DISCONNECTED, expected[8]);
        assertEquals(DetailedState.FAILED, expected[9]);
        assertEquals(DetailedState.BLOCKED, expected[10]);
        assertEquals(DetailedState.VERIFYING_POOR_LINK, expected[11]);
    }

}
