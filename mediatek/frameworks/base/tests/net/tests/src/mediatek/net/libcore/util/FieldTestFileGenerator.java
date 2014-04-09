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
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package tests.util;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import tests.support.Support_GetPutFields;
import tests.support.Support_GetPutFieldsDeprecated;
import tests.support.Support_GetPutFieldsDefaulted;

/**
 * Writes three test files that are used as reference in
 * {@code tests.api.java.io.ObjectInputStreamGetFieldTest} and
 * {@code tests.api.java.io.ObjectOutputStreamPutFieldTest}.
 * These files must be moved to
 * {@code $ANDROID_BUILD_TOP/dalvik/libcore/luni/src/test/resources/tests/api/java/io}
 * to be included at the correct location in the core tests package.
 * <p>
 * <strong>Important:</strong> Before executing this class, the contents of class
 * {@code tests.support.Support_GetPutFieldsDefaulted} must be commented out. See the
 * description there for further information.
 */
public class FieldTestFileGenerator {

    public static void main(String[] args) throws IOException {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        Support_GetPutFields toSerialize = new Support_GetPutFields();
        Support_GetPutFieldsDeprecated toSerializeDeprecated =
                new Support_GetPutFieldsDeprecated();
        Support_GetPutFieldsDefaulted toSerializeDefaulted =
                new Support_GetPutFieldsDefaulted();
        boolean success = true;

        toSerialize.initTestValues();
        toSerializeDeprecated.initTestValues();
        toSerializeDefaulted.initTestValues();

        System.out.println("Trying to write the test file 'testFields.ser'...");
        try {
            fos = new FileOutputStream("testFields.ser");
            oos = new ObjectOutputStream(fos);
            oos.writeObject(toSerialize);
            oos.close();
        }
        catch (Exception e) {
            System.out.println("Exception occured while writing the file: " + e);
            success = false;
        }
        finally {
            if (fos != null) fos.close();
        }

        System.out.println("Trying to write the test file 'testFieldsDeprecated.ser'...");
        try {
            fos = new FileOutputStream("testFieldsDeprecated.ser");
            oos = new ObjectOutputStream(fos);
            oos.writeObject(toSerializeDeprecated);
            oos.close();
        }
        catch (Exception e) {
            System.out.println("Exception occured while writing the file: " + e);
            success = false;
        }
        finally {
            if (fos != null) fos.close();
        }

        System.out.println("Trying to write the test file 'testFieldsDefaulted.ser'...");
        try {
            fos = new FileOutputStream("testFieldsDefaulted.ser");
            oos = new ObjectOutputStream(fos);
            oos.writeObject(toSerializeDefaulted);
            oos.close();
        }
        catch (Exception e) {
            System.out.println("Exception occured while writing the file: " + e);
            success = false;
        }
        finally {
            if (fos != null) fos.close();
        }

        if (success) {
            System.out.println("Success!");
        } else {
            System.out.println("Failure!");
        }

    }
 }
