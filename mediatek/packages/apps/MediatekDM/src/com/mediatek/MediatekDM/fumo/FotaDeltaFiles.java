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

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class FotaDeltaFiles {
    public static final String DELTA_CHECKSUM = "check.txt";
    public static final String DELTA_BOOT = "boot.delta";
    public static final String DELTA_SYSTEM = "system.delta";
    public static final String DELTA_RECOVERY = "recovery.delta";
    public static final String DELTA_FOLDER = "/data/delta/";

    public static final int DELTA_VERIFY_OK = 0;
    public static final int DELTA_NO_STORAGE = 1;
    public static final int DELTA_INVALID_ZIP = 2;
    public static final int DELTA_CHECKSUM_ERR = 3;

    public static int unpackAndVerify(String updateFile) {
        Log.d(TAG.Common, "[parsing delta]++ start unpacking & verification ++");

        // 1. check remained data storage
        if (!hasEnoughSpace(updateFile)) {
            Log.e(TAG.Common, "[parsing delta]-- storage not enough");
            return DELTA_NO_STORAGE;
        }

        // 2. unzip package
        List<String> files = unZipFile(new File(updateFile), DELTA_FOLDER);
        if (files == null || files.size() == 0) {
            Log.e(TAG.Common, "[parsing delta]-- unzipping failed");
            return DELTA_INVALID_ZIP;
        }

        // 3. check files are correct
        boolean hasChecksum = false;
        for (String name : files) {
            if (name.equals(DELTA_CHECKSUM)) {
                hasChecksum = true;
                Log.d(TAG.Common, "[parsing delta]:got check.txt");
            } else if (name.equals(DELTA_BOOT)) {
                Log.d(TAG.Common, "[parsing delta]:got boot.delta");
            } else if (name.equals(DELTA_SYSTEM)) {
                Log.d(TAG.Common, "[parsing delta]:got system.delta");
            } else if (name.equals(DELTA_RECOVERY)) {
                Log.d(TAG.Common, "[parsing delta]:got recovery.delta");
            } else {
                Log.e(TAG.Common, "[parsing delta]-- unknown file:" + name);
                return DELTA_INVALID_ZIP;
            }
        }
        if (!hasChecksum) {
            Log.e(TAG.Common, "[parsing delta]-- lost check.txt");
            return DELTA_INVALID_ZIP;
        }

        // 4. verify checksum one by one
        files.remove(DELTA_CHECKSUM);

        MD5ChecksumTable checksumTable = MD5ChecksumTable.newTable(DELTA_FOLDER + DELTA_CHECKSUM,
                files);

        for (String delta : files) {
            String filePath = DELTA_FOLDER + delta;
            String checksumComputed = null;
            try {
                checksumComputed = MD5Checksum.getMD5Checksum(filePath);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String checksumStored = checksumTable.query(delta);

            if (checksumComputed == null || !checksumComputed.equals(checksumStored)) {
                Log.e(TAG.Common, "[parsing delta]-- checksum incorrect>>" + delta);
                return DELTA_CHECKSUM_ERR;
            }
        }

        Log.d(TAG.Common, "[parsing delta]++ done!!! ++");
        return DELTA_VERIFY_OK;
    }

    private static List<String> unZipFile(File zipFile, String folderPath) {
        ArrayList<String> files = new ArrayList<String>();

        File desDir = new File(folderPath);
        if (!desDir.exists()) {
            desDir.mkdirs();
        }

        ZipFile zf = null;
        try {
            zf = new ZipFile(zipFile);
            for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                InputStream in = zf.getInputStream(entry);

                files.add(entry.getName());

                File desFile = new File(desDir, entry.getName());

                OutputStream out = new FileOutputStream(desFile);
                byte buffer[] = new byte[2048];
                int realLength = 0;
                while ((realLength = in.read(buffer)) > 0) {
                    out.write(buffer, 0, realLength);
                }
                in.close();
                out.close();
            }
        } catch (ZipException ze) {
            ze.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return files;
    }

    private static boolean hasEnoughSpace(String zipFile) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        final long freeSpace = stat.getBlockSize() * stat.getAvailableBlocks();

        File file = new File(zipFile);
        final long fileSize = file.length();
        final long rawFileSize = (long) (fileSize * 1.5);

        return freeSpace > rawFileSize;
    }

}
