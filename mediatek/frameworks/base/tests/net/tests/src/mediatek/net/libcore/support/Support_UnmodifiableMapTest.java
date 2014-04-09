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
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.support;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;

public class Support_UnmodifiableMapTest extends TestCase {

    Map<String, Integer> map;

    // must be a map containing the string keys "0"-"99" paired with the Integer
    // values Integer(0) to Integer(99)

    public Support_UnmodifiableMapTest(String p1) {
        super(p1);
    }

    public Support_UnmodifiableMapTest(String p1, Map<String, Integer> m) {
        super(p1);
        map = m;
    }

    @Override
    public void runTest() {
        // containsKey
        assertTrue("UnmodifiableMapTest - Should contain the key \"0\"", map
                .containsKey("0"));
        assertTrue("UnmodifiableMapTest - Should contain the key \"50\"", map
                .containsKey("50"));
        assertTrue("UnmodifiableMapTest - Should not contain the key \"100\"",
                !map.containsKey("100"));

        // containsValue
        assertTrue("UnmodifiableMapTest - Should contain the value 0", map
                .containsValue(new Integer(0)));
        assertTrue("UnmodifiableMapTest - Should contain the value 50", map
                .containsValue(new Integer(50)));
        assertTrue("UnmodifiableMapTest - Should not contain value 100", !map
                .containsValue(new Integer(100)));

        // entrySet
        Set<?> entrySet = map.entrySet();
        Iterator<?> entrySetIterator = entrySet.iterator();
        int myCounter = 0;
        while (entrySetIterator.hasNext()) {
            Map.Entry<?, ?> me = (Map.Entry<?, ?>) entrySetIterator.next();
            assertTrue("UnmodifiableMapTest - Incorrect Map.Entry returned",
                    map.get(me.getKey()).equals(me.getValue()));
            myCounter++;
        }
        assertEquals("UnmodifiableMapTest - Incorrect number of map entries returned",
                100, myCounter);

        // get
        assertTrue("UnmodifiableMapTest - getting \"0\" didn't return 0",
                map.get("0").intValue() == 0);
        assertTrue("UnmodifiableMapTest - getting \"50\" didn't return 0",
                map.get("0").intValue() == 0);
        assertNull("UnmodifiableMapTest - getting \"100\" didn't return null",
                map.get("100"));

        // isEmpty
        assertTrue(
                "UnmodifiableMapTest - should have returned false to isEmpty",
                !map.isEmpty());

        // keySet
        Set<?> keySet = map.keySet();
        t_KeySet(keySet);

        // size
        assertTrue("Size should return 100, returned: " + map.size(), map
                .size() == 100);

        // values
        new Support_UnmodifiableCollectionTest("Unmod--from map test", map
                .values());

    }

    void t_KeySet(Set<?> keySet) {
        // keySet should be a set of the strings "0" to "99"

        // contains
        assertTrue("UnmodifiableMapTest - keySetTest - should contain \"0\"",
                keySet.contains("0"));
        assertTrue("UnmodifiableMapTest - keySetTest - should contain \"50\"",
                keySet.contains("50"));
        assertTrue(
                "UnmodifiableMapTest - keySetTest - should not contain \"100\"",
                !keySet.contains("100"));

        // containsAll
        HashSet<String> hs = new HashSet<String>();
        hs.add("0");
        hs.add("25");
        hs.add("99");
        assertTrue(
                "UnmodifiableMapTest - keySetTest - should contain set of \"0\", \"25\", and \"99\"",
                keySet.containsAll(hs));
        hs.add("100");
        assertTrue(
                "UnmodifiableMapTest - keySetTest - should not contain set of \"0\", \"25\", \"99\" and \"100\"",
                !keySet.containsAll(hs));

        // isEmpty
        assertTrue("UnmodifiableMapTest - keySetTest - should not be empty",
                !keySet.isEmpty());

        // iterator
        Iterator<?> it = keySet.iterator();
        while (it.hasNext()) {
            assertTrue(
                    "UnmodifiableMapTest - keySetTest - Iterator returned wrong values",
                    keySet.contains(it.next()));
        }

        // size
        assertTrue(
                "UnmodifiableMapTest - keySetTest - returned wrong size.  Wanted 100, got: "
                        + keySet.size(), keySet.size() == 100);

        // toArray
        Object[] objArray;
        objArray = keySet.toArray();
        for (int counter = 0; it.hasNext(); counter++) {
            assertTrue(
                    "UnmodifiableMapTest - keySetTest - toArray returned incorrect array",
                    objArray[counter] == it.next());
        }

        // toArray (Object[])
        objArray = new Object[100];
        keySet.toArray(objArray);
        for (int counter = 0; it.hasNext(); counter++) {
            assertTrue(
                    "UnmodifiableMapTest - keySetTest - toArray(Object) filled array incorrectly",
                    objArray[counter] == it.next());
        }
    }

}
