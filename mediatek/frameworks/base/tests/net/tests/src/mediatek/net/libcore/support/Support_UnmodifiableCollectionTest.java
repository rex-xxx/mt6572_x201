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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import junit.framework.TestCase;

public class Support_UnmodifiableCollectionTest extends TestCase {

    Collection<Integer> col;

    // must be a collection containing the Integers 0 to 99 (which will iterate
    // in order)

    public Support_UnmodifiableCollectionTest(String p1) {
        super(p1);
    }

    public Support_UnmodifiableCollectionTest(String p1, Collection<Integer> c) {
        super(p1);
        col = c;
    }

    @Override
    public void runTest() {

        // contains
        assertTrue("UnmodifiableCollectionTest - should contain 0", col
                .contains(new Integer(0)));
        assertTrue("UnmodifiableCollectionTest - should contain 50", col
                .contains(new Integer(50)));
        assertTrue("UnmodifiableCollectionTest - should not contain 100", !col
                .contains(new Integer(100)));

        // containsAll
        HashSet<Integer> hs = new HashSet<Integer>();
        hs.add(new Integer(0));
        hs.add(new Integer(25));
        hs.add(new Integer(99));
        assertTrue(
                "UnmodifiableCollectionTest - should contain set of 0, 25, and 99",
                col.containsAll(hs));
        hs.add(new Integer(100));
        assertTrue(
                "UnmodifiableCollectionTest - should not contain set of 0, 25, 99 and 100",
                !col.containsAll(hs));

        // isEmpty
        assertTrue("UnmodifiableCollectionTest - should not be empty", !col
                .isEmpty());

        // iterator
        Iterator<Integer> it = col.iterator();
        SortedSet<Integer> ss = new TreeSet<Integer>();
        while (it.hasNext()) {
            ss.add(it.next());
        }
        it = ss.iterator();
        for (int counter = 0; it.hasNext(); counter++) {
            int nextValue = it.next().intValue();
            assertTrue(
                    "UnmodifiableCollectionTest - Iterator returned wrong value.  Wanted: "
                            + counter + " got: " + nextValue,
                    nextValue == counter);
        }

        // size
        assertTrue(
                "UnmodifiableCollectionTest - returned wrong size.  Wanted 100, got: "
                        + col.size(), col.size() == 100);

        // toArray
        Object[] objArray;
        objArray = col.toArray();
        for (int counter = 0; it.hasNext(); counter++) {
            assertTrue(
                    "UnmodifiableCollectionTest - toArray returned incorrect array",
                    objArray[counter] == it.next());
        }

        // toArray (Object[])
        objArray = new Object[100];
        col.toArray(objArray);
        for (int counter = 0; it.hasNext(); counter++) {
            assertTrue(
                    "UnmodifiableCollectionTest - toArray(Object) filled array incorrectly",
                    objArray[counter] == it.next());
        }

    }

}
