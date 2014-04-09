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
import java.util.TreeSet;

/**
 * java.util.Collection
 */
public class Support_CollectionTest extends junit.framework.TestCase {
    Collection<Integer> col; // must contain the Integers 0 to 99

    public Support_CollectionTest(String p1) {
        super(p1);
    }

    public Support_CollectionTest(String p1, Collection<Integer> c) {
        super(p1);
        col = c;
    }

    @Override
    public void runTest() {
        new Support_UnmodifiableCollectionTest("", col).runTest();

        // setup
        Collection<Integer> myCollection = new TreeSet<Integer>();
        myCollection.add(new Integer(101));
        myCollection.add(new Integer(102));
        myCollection.add(new Integer(103));

        // add
        assertTrue("CollectionTest - a) add did not work", col.add(new Integer(
                101)));
        assertTrue("CollectionTest - b) add did not work", col
                .contains(new Integer(101)));

        // remove
        assertTrue("CollectionTest - a) remove did not work", col
                .remove(new Integer(101)));
        assertTrue("CollectionTest - b) remove did not work", !col
                .contains(new Integer(101)));

        // addAll
        assertTrue("CollectionTest - a) addAll failed", col
                .addAll(myCollection));
        assertTrue("CollectionTest - b) addAll failed", col
                .containsAll(myCollection));

        // containsAll
        assertTrue("CollectionTest - a) containsAll failed", col
                .containsAll(myCollection));
        col.remove(new Integer(101));
        assertTrue("CollectionTest - b) containsAll failed", !col
                .containsAll(myCollection));

        // removeAll
        assertTrue("CollectionTest - a) removeAll failed", col
                .removeAll(myCollection));
        assertTrue("CollectionTest - b) removeAll failed", !col
                .removeAll(myCollection)); // should not change the colletion
                                            // the 2nd time around
        assertTrue("CollectionTest - c) removeAll failed", !col
                .contains(new Integer(102)));
        assertTrue("CollectionTest - d) removeAll failed", !col
                .contains(new Integer(103)));

        // retianAll
        col.addAll(myCollection);
        assertTrue("CollectionTest - a) retainAll failed", col
                .retainAll(myCollection));
        assertTrue("CollectionTest - b) retainAll failed", !col
                .retainAll(myCollection)); // should not change the colletion
                                            // the 2nd time around
        assertTrue("CollectionTest - c) retainAll failed", col
                .containsAll(myCollection));
        assertTrue("CollectionTest - d) retainAll failed", !col
                .contains(new Integer(0)));
        assertTrue("CollectionTest - e) retainAll failed", !col
                .contains(new Integer(50)));

        // clear
        col.clear();
        assertTrue("CollectionTest - a) clear failed", col.isEmpty());
        assertTrue("CollectionTest - b) clear failed", !col
                .contains(new Integer(101)));

    }

}
