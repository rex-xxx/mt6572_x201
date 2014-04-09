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

package com.mediatek.MediatekDM.fumo;

import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class MD5ChecksumTable {
    private static final boolean IS_DEBUG = true;

    private static final HashMap<String, Integer> WEIGHTS;
    private static final Comparator<String> NAME_OPERATOR;

    static {
        WEIGHTS = new HashMap<String, Integer>();
        WEIGHTS.put(FotaDeltaFiles.DELTA_BOOT, 100);
        WEIGHTS.put(FotaDeltaFiles.DELTA_SYSTEM, 10);
        WEIGHTS.put(FotaDeltaFiles.DELTA_RECOVERY, 1);

        NAME_OPERATOR = new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                Integer lhsWeight = WEIGHTS.get(lhs);
                Integer rhsWeight = WEIGHTS.get(rhs);

                int lhsValue = lhsWeight != null ? lhsWeight.intValue() : 0;
                int rhsValue = rhsWeight != null ? rhsWeight.intValue() : 0;

                return rhsValue - lhsValue;
            }
        };
    }

    private HashMap<String, String> mTable;

    private final int mDeltaCount;

    private MD5ChecksumTable(File file, List<String> deltaList) {
        mTable = new HashMap<String, String>();
        mDeltaCount = deltaList.size();

        // sort as boot.delta, system.delta, recovery.delta
        Collections.sort(deltaList, NAME_OPERATOR);

        if (IS_DEBUG) {
            Log.d(TAG.Common, "---- dumping sorted file list ----");
            for (String name : deltaList) {
                Log.d(TAG.Common, "[delta file]:" + name);
            }
        }

        parseChecksumFile(file, deltaList);
    }

    private void parseChecksumFile(File file, List<String> sortedList) {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                if (lineNum < mDeltaCount) {
                    mTable.put(sortedList.get(lineNum), line.trim());
                }
                lineNum++;
            }

            if (IS_DEBUG) {
                Log.d(TAG.Common, "-------- dumping checksum -------");
                for (Entry<String, String> entry : mTable.entrySet()) {
                    Log.d(TAG.Common, entry.getKey() + "\t= " + entry.getValue());
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String query(String fileName) {
        if (fileName.contains(File.separator)) {
            File xFile = new File(fileName);
            fileName = xFile.getName();
        }
        return mTable.get(fileName);
    }

    public static MD5ChecksumTable newTable(String checksumFile, List<String> deltaList) {
        File file = new File(checksumFile);
        return new MD5ChecksumTable(file, deltaList);
    }
}
