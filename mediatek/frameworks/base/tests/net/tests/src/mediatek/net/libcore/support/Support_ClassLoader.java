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
package tests.support;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import dalvik.system.DexClassLoader;

/**
 * Support class for creating a file-based ClassLoader. Delegates to either
 * Dalvik's PathClassLoader or the RI's URLClassLoader, but does so by-name.
 * This allows us to run corresponding tests in both environments.
 */
public abstract class Support_ClassLoader {

    public abstract ClassLoader getClassLoader(URL url, ClassLoader parent);

    public static ClassLoader getInstance(URL url, ClassLoader parent) {
        try {
            Support_ClassLoader factory;

            if ("Dalvik".equals(System.getProperty("java.vm.name"))) {
                factory = (Support_ClassLoader)Class.forName(
                    "tests.support.Support_ClassLoader$Dalvik").newInstance();
            } else {
                factory = (Support_ClassLoader)Class.forName(
                    "tests.support.Support_ClassLoader$RefImpl").newInstance();
            }

            return factory.getClassLoader(url, parent);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to create ClassLoader", ex);
        }
    }

    /**
     * Implementation for Dalvik. Uses the DexClassLoader, so we can write
     * temporary DEX files to a special directory. We don't want to spoil the
     * system's DEX cache with our files. Also, we might not have write access
     * to the system's DEX cache at all (which is the case when we're running
     * CTS).
     */
    static class Dalvik extends Support_ClassLoader {

        private static File tmp;

        static {
            tmp = new File(System.getProperty("java.io.tmpdir"), "dex-cache");
            tmp.mkdirs();
        }

        @Override
        public ClassLoader getClassLoader(URL url, ClassLoader parent) {
            return new DexClassLoader(url.getPath(), tmp.getAbsolutePath(),
                    null, parent);
        }
    }

    /**
     * Implementation for the reference implementation. Nothing interesting to
     * see here. Please get along.
     */
    static class RefImpl extends Support_ClassLoader {
        @Override
        public ClassLoader getClassLoader(URL url, ClassLoader parent) {
            return new URLClassLoader(new URL[] { url }, parent);
        }
    }
}
