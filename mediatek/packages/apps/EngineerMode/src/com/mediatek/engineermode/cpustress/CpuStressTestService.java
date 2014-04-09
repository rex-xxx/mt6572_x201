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

package com.mediatek.engineermode.cpustress;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.widget.Toast;

import com.mediatek.engineermode.ChipSupport;
import com.mediatek.engineermode.R;
import com.mediatek.engineermode.emsvr.AFMFunctionCallEx;
import com.mediatek.engineermode.emsvr.FunctionReturn;
import com.mediatek.xlog.Xlog;

import java.io.File;

public class CpuStressTestService extends Service {

    private static final String TAG = "EM/CpuStressTestService";

    protected static final String THERMAL_ETC_FILE = "/etc/.tp/.ht120.mtc";
    private static final int INDEX_TEST_APMCU = 1;
    private static final int INDEX_TEST_VIDEOCODEC = 2;
    private static final int INDEX_TEST_BACKUP = 3;
    private static final int INDEX_TEST_RESTORE = 4;
    private static final String HANDLER_THREAD_NAME_APMCU = "ApMcu";
    private static final String HANDLER_THREAD_NAME_VIDEO = "VideoCodec";
    private static final String HANDLER_THREAD_NAME_BACKUPRESTORE = "BackupRestore";

    public static final String VALUE_RUN = "run";
    public static final String VALUE_LOOPCOUNT = "loopcount";
    public static final String VALUE_ITERATION = "iteration";
    public static final String VALUE_MASK = "mask";
    public static final String VALUE_RESULT = "result";
    public static final String RESULT_NEON = "result_neon";
    public static final String RESULT_PASS_NEON = "result_pass_neon";
    public static final String RESULT_CA9 = "result_ca9";
    public static final String RESULT_PASS_CA9 = "result_pass_ca9";
    public static final String RESULT_DHRY = "result_dhry";
    public static final String RESULT_PASS_DHRY = "result_pass_dhry";
    public static final String RESULT_MEMCPY = "result_memcpy";
    public static final String RESULT_PASS_MEMCPY = "result_pass_memcpy";
    public static final String RESULT_FDCT = "result_fdct";
    public static final String RESULT_PASS_FDCT = "result_pass_fdct";
    public static final String RESULT_IMDCT = "result_imdct";
    public static final String RESULT_PASS_IMDCT = "result_pass_imdct";
    public static final String RESULT_VIDEOCODEC = "result_video_codec";
    public static final String RESULT_PASS_VIDEOCODEC = "result_pass_video_codec";

    private static final String PASS = "PASS";
    private static final String FAIL = "FAIL";
    private static final String SKIP = "is powered off";
    private static final String PASS_89 = "Frame #1950";
    private static final String SKIP_89 = "Frame #";
    private static final String RESULT_ERROR = "ERROR";

    private static final int TIME_DELAYED = 100;
    private static final long LOOPCOUNT_DEFAULT_VALUE = 99999999;
    protected static final int CORE_NUM_MASK = 28;
    private static final String CPU_1_ONLINE_PATH = "/sys/devices/system/cpu/cpu1/online";
    private static final String CPU_3_ONLINE_PATH = "/sys/devices/system/cpu/cpu3/online";

    private static final String RESULT_SEPARATE = ";";

    public static final int CORE_NUMBER_4 = 4;
    public static final int CORE_NUMBER_2 = 2;
    public static final int CORE_NUMBER_1 = 1;

    public static int sCoreNumber = 0;
    protected static boolean sIsThermalSupport = false;
    protected static boolean sIsThermalDisabled = false;
    protected static int sIndexMode = 0;

    private long mLoopCountApMcu = LOOPCOUNT_DEFAULT_VALUE;
    private int mTestApMcuMask = 0;
    private int mResultApMcu = 0;
    private boolean mTestApMcuRunning = false;
    private int mResultPassL2C = 0;
    private int mResultTotalL2C = 0;
    private int mResultPassNeon = 0;
    private int mResultTotalNeon = 0;
    private int mResultPassCa9 = 0;
    private int mResultTotalCa9 = 0;
    private int mResultPassDhry = 0;
    private int mResultTotalDhry = 0;
    private int mResultPassMemcpy = 0;
    private int mResultTotalMemcpy = 0;
    private int mResultPassFdct = 0;
    private int mResultTotalFdct = 0;
    private int mResultPassImdct = 0;
    private int mResultTotalImdct = 0;

    private long mLoopCountVideoCodec = LOOPCOUNT_DEFAULT_VALUE;
    private int mIterationVideoCodec = 0;
    private int mResultVideoCodec = 0;
    private boolean mTestVideoCodecRunning = false;
    private int mResultPassVideoCodec = 0;
    private int mResultTotalVideoCodec = 0;

    private boolean mTestClockSwitchRunning = false;

    protected boolean mWantStopApmcu = false;
    protected boolean mWantStopSwCodec = false;
    protected ICpuStressTestComplete mTestClass = null;

    private WakeLock mWakeLock = null;

    private final StressTestBinder mBinder = new StressTestBinder();
    private final HandlerThread mHandlerThreadApMcu = new HandlerThread(
            HANDLER_THREAD_NAME_APMCU);
    private final HandlerThread mHandlerThreadVideoCodec = new HandlerThread(
            HANDLER_THREAD_NAME_VIDEO);
    private final HandlerThread mHandlerThreadBackupRestore = new HandlerThread(
            HANDLER_THREAD_NAME_BACKUPRESTORE);
    private HandlerApMcu mHandlerApMcu = null;
    private HandlerVideoCodec mHandlerVideoCodec = null;
    private HandlerBackupRestore mHandlerBackupRestore = null;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        int coreNumber = coreNum();
        sCoreNumber = coreNumber;
        mResultApMcu |= coreNumber << CORE_NUM_MASK;
        mResultVideoCodec |= coreNumber << CORE_NUM_MASK;
        sIsThermalSupport = new File(THERMAL_ETC_FILE).exists();
        mWakeLock = new WakeLock();
        try {
            mHandlerThreadApMcu.start();
            mHandlerThreadVideoCodec.start();
            mHandlerThreadBackupRestore.start();
            mHandlerApMcu = new HandlerApMcu(mHandlerThreadApMcu.getLooper());
            mHandlerVideoCodec = new HandlerVideoCodec(mHandlerThreadVideoCodec
                    .getLooper());
            mHandlerBackupRestore = new HandlerBackupRestore(
                    mHandlerThreadBackupRestore.getLooper());
        } catch (IllegalThreadStateException e) {
            Xlog.w(TAG, "Handler thread IllegalThreadStateException: "
                    + e.getMessage());
            Toast.makeText(this,
                    R.string.hqa_cpustress_test_toast_threadhandler_error,
                    Toast.LENGTH_LONG).show();
        }
        Xlog.i(TAG, "Core Number: " + coreNumber);
    }

    @Override
    public void onDestroy() {
        restore(sIndexMode);
        mHandlerThreadApMcu.quit();
        mHandlerThreadVideoCodec.quit();
        mHandlerThreadBackupRestore.quit();
        mWakeLock.release();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Xlog.v(TAG, "Enter onStartCommand");
        return START_NOT_STICKY;
    }

    /**
     * Start test use configuration data
     * 
     * @param data
     *            Test configuration data
     */
    public void startTest(Bundle data) {
        Xlog.v(TAG, "Enter startTest");
        if (mTestClass instanceof ApMcu) {
            Xlog.v(TAG, "startTest for ApMcu");
            if (mTestApMcuRunning) {
                Xlog.v(TAG, "ApMpu test is running");
                return;
            }
            mLoopCountApMcu = data.getLong(VALUE_LOOPCOUNT);
            mTestApMcuMask = data.getInt(VALUE_MASK);
            mTestApMcuRunning = true;
            mWantStopApmcu = false;
            mResultApMcu = 0;
            mResultApMcu |= sCoreNumber << CORE_NUM_MASK;
            mResultTotalL2C = 0;
            mResultPassL2C = 0;
            mResultTotalNeon = 0;
            mResultPassNeon = 0;
            mResultTotalCa9 = 0;
            mResultPassCa9 = 0;
            mResultPassDhry = 0;
            mResultTotalDhry = 0;
            mResultPassMemcpy = 0;
            mResultTotalMemcpy = 0;
            mResultPassFdct = 0;
            mResultTotalFdct = 0;
            mResultPassImdct = 0;
            mResultTotalImdct = 0;
            updateWakeLock();
            mHandlerApMcu.sendEmptyMessage(INDEX_TEST_APMCU);
        } else if (mTestClass instanceof SwVideoCodec) {
            Xlog.v(TAG, "startTest for SwVideoCodec");
            if (mTestVideoCodecRunning) {
                Xlog.v(TAG, "VideoCodec test is running");
                return;
            }
            mLoopCountVideoCodec = data.getLong(VALUE_LOOPCOUNT);
            mIterationVideoCodec = data.getInt(VALUE_ITERATION);
            mTestVideoCodecRunning = true;
            mWantStopSwCodec = false;
            mResultVideoCodec = 0;
            mResultVideoCodec |= sCoreNumber << CORE_NUM_MASK;
            mResultPassVideoCodec = 0;
            mResultTotalVideoCodec = 0;
            updateWakeLock();
            mHandlerVideoCodec.sendEmptyMessage(INDEX_TEST_VIDEOCODEC);
        } else if (mTestClass instanceof ClockSwitch) {
            Xlog.v(TAG, "startTest for ClockSwitch");
            mTestClockSwitchRunning = true;
            updateWakeLock();
        }
    }

    /**
     * Invoked when press "stop" button to stop the test
     */
    public void stopTest() {
        Xlog.v(TAG, "Enter stopTest, testObject is: " + mTestClass);
        if (mTestClass instanceof ApMcu) {
            Xlog.v(TAG, "stopTest for ApMcu");
            // bRunApMcu = false;
            mWantStopApmcu = true;
        } else if (mTestClass instanceof SwVideoCodec) {
            Xlog.v(TAG, "stopTest for SwVideoCodec");
            // bRunVideoCodec = false;
            mWantStopSwCodec = true;
        } else if (mTestClass instanceof ClockSwitch) {
            Xlog.v(TAG, "stopTest for ClockSwitch");
            mTestClockSwitchRunning = false;
        }
        // updateWakeLock();
    }

    /**
     * Update test data. If param is null, return test result, or just update
     * test data
     * 
     * @param data
     *            Contains test data need to update
     * @return If param is null, it return test result, or return null
     */
    public Bundle updateData(Bundle data) {
        Xlog.v(TAG, "updateData, data is null ? " + (data == null));
        if (null == data) {
            if (mTestClass instanceof ApMcu) {
                return dataGenerator(INDEX_TEST_APMCU);
            } else if (mTestClass instanceof SwVideoCodec) {
                return dataGenerator(INDEX_TEST_VIDEOCODEC);
            }
        } else {
            if (mTestClass instanceof ApMcu) {
                mTestApMcuMask = data.getInt(VALUE_MASK);
            } else if (mTestClass instanceof SwVideoCodec) {
                Xlog.v(TAG, "VideoCodec test not need to update config");
            }
        }
        return null;
    }

    /**
     * Get test result data
     * 
     * @return Test result data
     */
    public Bundle getData() {
        return updateData(null);
    }

    /**
     * Do backup and restore
     * 
     * @param index
     *            Index to do different branch
     */
    private void doBackupRestore(int index) {
        Xlog.v(TAG, "Enter doBackupRestore: " + index);
        String result = runCmdInNative(
                AFMFunctionCallEx.FUNCTION_EM_CPU_STRESS_TEST_BACKUP, 1, index);
        Xlog.v(TAG, "doBackupRestore: " + result);
    }

    /**
     * Do ApMcu test, invoked in #HandlerThread
     */
    private void doApMcuTest() {
        Xlog.v(TAG, "enter doApMpuTest");
        if (CORE_NUMBER_2 == sCoreNumber) {
            switch (sIndexMode) {
            case CpuStressTest.INDEX_SINGLE:
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_NEON_0)) {
                    doApMcuTest(ApMcu.MASK_NEON_0);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_CA9_0)) {
                    doApMcuTest(ApMcu.MASK_CA9_0);
                }
            	if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_DHRY_0)) {
                    doApMcuTest(ApMcu.MASK_DHRY_0);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_MEMCPY_0)) {
                    doApMcuTest(ApMcu.MASK_MEMCPY_0);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_FDCT_0)) {
                    doApMcuTest(ApMcu.MASK_FDCT_0);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_IMDCT_0)) {
                    doApMcuTest(ApMcu.MASK_IMDCT_0);
                }
                break;
            case CpuStressTest.INDEX_TEST:
            case CpuStressTest.INDEX_DUAL:
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_NEON_1)) {
                    doApMcuTest(ApMcu.MASK_NEON_1);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_CA9_1)) {
                    doApMcuTest(ApMcu.MASK_CA9_1);
                }
            	if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_DHRY_1)) {
                    doApMcuTest(ApMcu.MASK_DHRY_1);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_MEMCPY_1)) {
                    doApMcuTest(ApMcu.MASK_MEMCPY_1);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_FDCT_1)) {
                    doApMcuTest(ApMcu.MASK_FDCT_1);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_IMDCT_1)) {
                    doApMcuTest(ApMcu.MASK_IMDCT_1);
                }
                break;
            default:
                break;
            }
        } else if (CORE_NUMBER_4 == sCoreNumber) {
            switch (sIndexMode) {
            case CpuStressTest.INDEX_SINGLE:
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_NEON_0)) {
                    doApMcuTest(ApMcu.MASK_NEON_0);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_CA9_0)) {
                    doApMcuTest(ApMcu.MASK_CA9_0);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_DHRY_0)) {
                    doApMcuTest(ApMcu.MASK_DHRY_0);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_MEMCPY_0)) {
                    doApMcuTest(ApMcu.MASK_MEMCPY_0);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_FDCT_0)) {
                    doApMcuTest(ApMcu.MASK_FDCT_0);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_IMDCT_0)) {
                    doApMcuTest(ApMcu.MASK_IMDCT_0);
                }
                break;
            case CpuStressTest.INDEX_DUAL:
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_NEON_1)) {
                    doApMcuTest(ApMcu.MASK_NEON_1);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_CA9_1)) {
                    doApMcuTest(ApMcu.MASK_CA9_1);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_DHRY_1)) {
                    doApMcuTest(ApMcu.MASK_DHRY_1);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_MEMCPY_1)) {
                    doApMcuTest(ApMcu.MASK_MEMCPY_1);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_FDCT_1)) {
                    doApMcuTest(ApMcu.MASK_FDCT_1);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_IMDCT_1)) {
                    doApMcuTest(ApMcu.MASK_IMDCT_1);
                }
                break;
            case CpuStressTest.INDEX_TRIPLE:
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_NEON_2)) {
                    doApMcuTest(ApMcu.MASK_NEON_2);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_CA9_2)) {
                    doApMcuTest(ApMcu.MASK_CA9_2);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_DHRY_2)) {
                    doApMcuTest(ApMcu.MASK_DHRY_2);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_MEMCPY_2)) {
                    doApMcuTest(ApMcu.MASK_MEMCPY_2);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_FDCT_2)) {
                    doApMcuTest(ApMcu.MASK_FDCT_2);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_IMDCT_2)) {
                    doApMcuTest(ApMcu.MASK_IMDCT_2);
                }
                break;
            case CpuStressTest.INDEX_TEST:
            case CpuStressTest.INDEX_QUAD:
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_NEON_3)) {
                    doApMcuTest(ApMcu.MASK_NEON_3);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_CA9_3)) {
                    doApMcuTest(ApMcu.MASK_CA9_3);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_DHRY_3)) {
                    doApMcuTest(ApMcu.MASK_DHRY_3);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_MEMCPY_3)) {
                    doApMcuTest(ApMcu.MASK_MEMCPY_3);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_FDCT_3)) {
                    doApMcuTest(ApMcu.MASK_FDCT_3);
                }
                if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_IMDCT_3)) {
                    doApMcuTest(ApMcu.MASK_IMDCT_3);
                }
                break;
            default:
                break;
            }
        } else {
            if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_NEON_0)) {
                doApMcuTest(ApMcu.MASK_NEON_0);
            }
            if (0 != (mTestApMcuMask & 1 << ApMcu.MASK_CA9_0)) {
                doApMcuTest(ApMcu.MASK_CA9_0);
            }
        }
        Xlog.v(TAG, "iResultApMpu is 0x" + Integer.toHexString(mResultApMcu));
    }

    /**
     * Do ApMcu test
     * 
     * @param index
     *            Different test branch
     */
    private void doApMcuTest(int index) {
        Xlog.v(TAG, "doApMpuTest index is: " + index);
        String response = runCmdInNative(
                AFMFunctionCallEx.FUNCTION_EM_CPU_STRESS_TEST_APMCU, 1, index);
        Xlog.v(TAG, "doApMcuTest response: " + response);
        if (null == response) {
            return;
        }
        switch (index) {
        case ApMcu.MASK_NEON_0:
        case ApMcu.MASK_NEON_1:
        case ApMcu.MASK_NEON_2:
        case ApMcu.MASK_NEON_3:
            mResultTotalNeon++;
            String[] neonResult = response.split(RESULT_SEPARATE);
            boolean bNeonPass = true;
            for (int i = 0; i < neonResult.length; i++) {
                if (neonResult[i].contains(PASS)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_NEON_0 + i));
                } else if (neonResult[i].contains(SKIP)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_NEON_0 + i));
                    Xlog.d(TAG, "NEON test, CPU" + i + " OFFLINE");
                } else {
                    bNeonPass = false;
                    mResultApMcu &= ~(1 << (ApMcu.MASK_NEON_0 + i));
                }
            }
            if (bNeonPass) {
                mResultPassNeon++;
            }
            break;
        case ApMcu.MASK_CA9_0:
        case ApMcu.MASK_CA9_1:
        case ApMcu.MASK_CA9_2:
        case ApMcu.MASK_CA9_3:
            mResultTotalCa9++;
            String[] ca9Result = response.split(RESULT_SEPARATE);
            boolean bCa9Pass = true;
            for (int i = 0; i < ca9Result.length; i++) {
                if (ca9Result[i].contains(PASS)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_CA9_0 + i));
                } else if (ca9Result[i].contains(SKIP)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_CA9_0 + i));
                    Xlog.d(TAG, "CA9 test, CPU" + i + " OFFLINE");
                } else {
                    bCa9Pass = false;
                    mResultApMcu &= ~(1 << (ApMcu.MASK_CA9_0 + i));
                }
            }
            if (bCa9Pass) {
                mResultPassCa9++;
            }
            break;
        case ApMcu.MASK_DHRY_0:
        case ApMcu.MASK_DHRY_1:
        case ApMcu.MASK_DHRY_2:
        case ApMcu.MASK_DHRY_3:
            mResultTotalDhry++;
            String[] dhryResult = response.split(RESULT_SEPARATE);
            boolean bDhryPass = true;
            for (int i = 0; i < dhryResult.length; i++) {
                if (dhryResult[i].contains(PASS)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_DHRY_0 + i));
                } else if (dhryResult[i].contains(SKIP)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_DHRY_0 + i));
                    Xlog.d(TAG, "DHRY test, CPU" + i + " OFFLINE");
                } else {
                    bDhryPass = false;
                    mResultApMcu &= ~(1 << (ApMcu.MASK_DHRY_0 + i));
                }
            }
            if (bDhryPass) {
                mResultPassDhry++;
            }
            break;
        case ApMcu.MASK_MEMCPY_0:
        case ApMcu.MASK_MEMCPY_1:
        case ApMcu.MASK_MEMCPY_2:
        case ApMcu.MASK_MEMCPY_3:
            mResultTotalMemcpy++;
            String[] memcpyResult = response.split(RESULT_SEPARATE);
            boolean bMemcpyPass = true;
            for (int i = 0; i < memcpyResult.length; i++) {
                if (memcpyResult[i].contains(PASS)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_MEMCPY_0 + i));
                } else if (memcpyResult[i].contains(SKIP)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_MEMCPY_0 + i));
                    Xlog.d(TAG, "MEMCPY test, CPU" + i + " OFFLINE");
                } else {
                    bMemcpyPass = false;
                    mResultApMcu &= ~(1 << (ApMcu.MASK_MEMCPY_0 + i));
                }
            }
            if (bMemcpyPass) {
                mResultPassMemcpy++;
            }
            break;
        case ApMcu.MASK_FDCT_0:
        case ApMcu.MASK_FDCT_1:
        case ApMcu.MASK_FDCT_2:
        case ApMcu.MASK_FDCT_3:
            mResultTotalFdct++;
            String[] fdctResult = response.split(RESULT_SEPARATE);
            boolean bFdctPass = true;
            for (int i = 0; i < fdctResult.length; i++) {
                if (fdctResult[i].contains(PASS)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_FDCT_0 + i));
                } else if (fdctResult[i].contains(SKIP)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_FDCT_0 + i));
                    Xlog.d(TAG, "FDCT test, CPU" + i + " OFFLINE");
                } else {
                    bFdctPass = false;
                    mResultApMcu &= ~(1 << (ApMcu.MASK_FDCT_0 + i));
                }
            }
            if (bFdctPass) {
                mResultPassFdct++;
            }
            break;
        case ApMcu.MASK_IMDCT_0:
        case ApMcu.MASK_IMDCT_1:
        case ApMcu.MASK_IMDCT_2:
        case ApMcu.MASK_IMDCT_3:
            mResultTotalImdct++;
            String[] imdctResult = response.split(RESULT_SEPARATE);
            boolean bImdctPass = true;
            for (int i = 0; i < imdctResult.length; i++) {
                if (imdctResult[i].contains(PASS)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_IMDCT_0 + i));
                } else if (imdctResult[i].contains(SKIP)) {
                    mResultApMcu |= (1 << (ApMcu.MASK_IMDCT_0 + i));
                    Xlog.d(TAG, "IMDCT test, CPU" + i + " OFFLINE");
                } else {
                    bImdctPass = false;
                    mResultApMcu &= ~(1 << (ApMcu.MASK_IMDCT_0 + i));
                }
            }
            if (bImdctPass) {
                mResultPassImdct++;
            }
            break;
        default:
            break;
        }
    }

    private String runCmdInNative(int index, int paramNum, int... param) {
        StringBuilder build = new StringBuilder();
        AFMFunctionCallEx functionCall = new AFMFunctionCallEx();
        boolean result = functionCall.startCallFunctionStringReturn(index);
        functionCall.writeParamNo(paramNum);
        for (int i : param) {
            functionCall.writeParamInt(i);
        }
        if (result) {
            FunctionReturn r;
            do {
                r = functionCall.getNextResult();
                if (r.mReturnString.isEmpty()) {
                    break;
                }
                build.append(r.mReturnString);
            } while (r.mReturnCode == AFMFunctionCallEx.RESULT_CONTINUE);
            if (r.mReturnCode == AFMFunctionCallEx.RESULT_IO_ERR) {
                Xlog.d(TAG, "AFMFunctionCallEx: RESULT_IO_ERR");
                build.replace(0, build.length(), RESULT_ERROR);
            }
        } else {
            Xlog.d(TAG, "AFMFunctionCallEx return false");
            build.append(RESULT_ERROR);
        }
        return build.toString();
    }

    /**
     * Do video codec test
     */
    private void doVideoCodecTest() {
        Xlog.v(TAG, "enter doVideoCodecTest");
        String response = "";
        int testIndex = -1;
        if (CORE_NUMBER_2 == sCoreNumber) {
            switch (CpuStressTestService.sIndexMode) {
            case CpuStressTest.INDEX_SINGLE:
                testIndex = SwVideoCodec.SWCODEC_MASK_CORE_0;
                break;
            case CpuStressTest.INDEX_TEST:
            case CpuStressTest.INDEX_DUAL:
                testIndex = SwVideoCodec.SWCODEC_MASK_CORE_1;
                break;
            default:
                break;
            }
        } else if (CORE_NUMBER_4 == sCoreNumber) {
            switch (CpuStressTestService.sIndexMode) {
            case CpuStressTest.INDEX_SINGLE:
                testIndex = SwVideoCodec.SWCODEC_MASK_CORE_0;
                break;
            case CpuStressTest.INDEX_DUAL:
                testIndex = SwVideoCodec.SWCODEC_MASK_CORE_1;
                break;
            case CpuStressTest.INDEX_TRIPLE:
                testIndex = SwVideoCodec.SWCODEC_MASK_CORE_2;
                break;
            case CpuStressTest.INDEX_QUAD:
            case CpuStressTest.INDEX_TEST:
                testIndex = SwVideoCodec.SWCODEC_MASK_CORE_3;
                break;
            default:
                break;
            }
        } else {
            testIndex = SwVideoCodec.SWCODEC_MASK_CORE_0;
        }
        if (-1 == testIndex) {
            return;
        }
        response = runCmdInNative(
                AFMFunctionCallEx.FUNCTION_EM_CPU_STRESS_TEST_SWCODEC, 2,
                testIndex, 1);
        Xlog.v(TAG, "doVideoCodecTest response: " + response);
        if (null == response) {
            return;
        }
        mResultTotalVideoCodec++;
        String[] resultArray = response.split(RESULT_SEPARATE);
        boolean bPass = true;
        if (resultArray.length > 0) {
            if (CORE_NUMBER_4 == sCoreNumber) {
                for (int i = 0; i < resultArray.length; i++) {
                    if (resultArray[i].contains(PASS_89)) {
                        mResultVideoCodec |= (1 << (SwVideoCodec.SWCODEC_MASK_CORE_0 + i));
                    } else if (resultArray[i].contains(SKIP_89)) {
                        mResultVideoCodec |= (1 << (SwVideoCodec.SWCODEC_MASK_CORE_0 + i));
                        Xlog.d(TAG, "VideoCodec test, CPU" + i + " OFFLINE");
                    } else {
                        mResultVideoCodec &= ~(1 << (SwVideoCodec.SWCODEC_MASK_CORE_0 + i));
                        bPass = false;
                    }
                }
            } else {
                String passTag = PASS_89;
                String skipTag = SKIP_89;
                if (!ChipSupport.isCurrentChipHigher(ChipSupport.MTK_6589_SUPPORT, true)) {
                    passTag = PASS;
                    skipTag = SKIP;
                }
                for (int i = 0; i < resultArray.length; i++) {
                    if (resultArray[i].contains(passTag)) {
                        mResultVideoCodec |= (1 << (SwVideoCodec.SWCODEC_MASK_CORE_0 + i));
                    } else if (resultArray[i].contains(skipTag)) {
                        mResultVideoCodec |= (1 << (SwVideoCodec.SWCODEC_MASK_CORE_0 + i));
                        Xlog.d(TAG, "VideoCodec test, CPU" + i + " OFFLINE");
                    } else {
                        mResultVideoCodec &= ~(1 << (SwVideoCodec.SWCODEC_MASK_CORE_0 + i));
                        bPass = false;
                    }
                }
            }
        } else {
            mResultVideoCodec = 0;
            mResultVideoCodec |= sCoreNumber << CORE_NUM_MASK;
            bPass = false;
        }
        if (bPass) {
            mResultPassVideoCodec++;
        }
    }

    /**
     * Get test status data and packaged to #Bundle
     * 
     * @param index
     *            Test index ID
     * @return #Bundle contains test status
     */
    private Bundle dataGenerator(int index) {
        Xlog.v(TAG, "dataGenerator index is " + index);
        Bundle data = new Bundle();
        switch (index) {
        case INDEX_TEST_APMCU:
            data.putBoolean(VALUE_RUN, mTestApMcuRunning);
            data.putLong(VALUE_LOOPCOUNT, mLoopCountApMcu);
            data.putInt(VALUE_MASK, mTestApMcuMask);
            data.putInt(VALUE_RESULT, mResultApMcu);
            data.putInt(RESULT_NEON, mResultTotalNeon);
            data.putInt(RESULT_PASS_NEON, mResultPassNeon);
            data.putInt(RESULT_CA9, mResultTotalCa9);
            data.putInt(RESULT_PASS_CA9, mResultPassCa9);
            data.putInt(RESULT_DHRY, mResultTotalDhry);
            data.putInt(RESULT_PASS_DHRY, mResultPassDhry);
            data.putInt(RESULT_MEMCPY, mResultTotalMemcpy);
            data.putInt(RESULT_PASS_MEMCPY, mResultPassMemcpy);
            data.putInt(RESULT_FDCT, mResultTotalFdct);
            data.putInt(RESULT_PASS_FDCT, mResultPassFdct);
            data.putInt(RESULT_IMDCT, mResultTotalImdct);
            data.putInt(RESULT_PASS_IMDCT, mResultPassImdct);
            break;
        case INDEX_TEST_VIDEOCODEC:
            data.putBoolean(VALUE_RUN, mTestVideoCodecRunning);
            data.putLong(VALUE_LOOPCOUNT, mLoopCountVideoCodec);
            data.putInt(VALUE_ITERATION, mIterationVideoCodec);
            data.putInt(VALUE_RESULT, mResultVideoCodec);
            data.putInt(RESULT_VIDEOCODEC, mResultTotalVideoCodec);
            data.putInt(RESULT_PASS_VIDEOCODEC, mResultPassVideoCodec);
            break;
        default:
            break;
        }
        return data;
    }

    /**
     * Check whether has test running
     * 
     * @return True if there is test running
     */
    public boolean isTestRun() {
        return mTestApMcuRunning || mTestClockSwitchRunning
                || mTestVideoCodecRunning;
    }

    /**
     * Check whether clock switch test is running
     * 
     * @return True if clock switch test is running
     */
    public boolean isClockSwitchRun() {
        return mTestClockSwitchRunning;
    }

    /**
     * Get CPU core numbers
     * 
     * @return CPU core number
     */
    private int coreNum() {
        if (new File(CPU_3_ONLINE_PATH).exists()) {
            return CORE_NUMBER_4;
        } else if (new File(CPU_1_ONLINE_PATH).exists()) {
            return CORE_NUMBER_2;
        } else {
            return CORE_NUMBER_1;
        }
    }

    public class StressTestBinder extends Binder {

        /**
         * Binder
         * 
         * @return #CpuStressTestService
         */
        CpuStressTestService getService() {
            return CpuStressTestService.this;
        }
    }

    public interface ICpuStressTestComplete {

        /**
         * Invoked when need to update test result
         */
        void onUpdateTestResult();
    }

    /**
     * Backup/restore CPU status
     * 
     * @param indexDefault
     *            Test mode index
     */
    public void setIndexMode(int indexDefault) {
        Xlog.v(TAG, "setIndexMode: " + indexDefault + " sIndexMode: "
                + sIndexMode);
        if (indexDefault == sIndexMode) {
            return;
        }
        if (0 == sIndexMode) {
            backup(indexDefault);
        } else if (0 == indexDefault) {
            restore(sIndexMode);
        } else {
            restore(sIndexMode);
            backup(indexDefault);
        }
        synchronized (this) {
            sIndexMode = indexDefault;
        }
    }

    /**
     * Backup CPU status
     * 
     * @param index
     *            Test mode index
     */
    private void backup(int index) {
        Xlog.v(TAG, "Enter backup: " + index);
        Message m = mHandlerBackupRestore.obtainMessage(INDEX_TEST_BACKUP);
        m.arg1 = index + CpuStressTest.TEST_BACKUP;
        mHandlerBackupRestore.sendMessage(m);
    }

    /**
     * Restore CPU status
     * 
     * @param index
     *            Test mode index
     */
    private void restore(int index) {
        Xlog.v(TAG, "Enter restore: " + index);
        Message m = mHandlerBackupRestore.obtainMessage(INDEX_TEST_RESTORE);
        m.arg1 = index + CpuStressTest.TEST_RESTORE;
        mHandlerBackupRestore.sendMessage(m);
    }

    /**
     * Update wake lock, auto acquire or release lock by test running
     */
    public void updateWakeLock() {
        synchronized (this) {
            if (isTestRun()) {
                mWakeLock.acquire(this);
            } else {
                mWakeLock.release();
            }
        }
    }

    class HandlerApMcu extends Handler {

        HandlerApMcu(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Xlog.v(TAG, "mTestHandlerApMcu receive msg: " + msg.what);
            if (INDEX_TEST_APMCU == msg.what) {
                if (mLoopCountApMcu <= 0) {
                    mLoopCountApMcu = 0;
                    mTestApMcuRunning = false;
                    updateWakeLock();
                    removeMessages(INDEX_TEST_APMCU);
                } else {
                    if (mWantStopApmcu) {
                        mTestApMcuRunning = false;
                        mWantStopApmcu = false;
                        removeMessages(INDEX_TEST_APMCU);
                        updateWakeLock();
                    } else {
                        // lLoopCountApMcu--;
                        doApMcuTest();
                        sendEmptyMessageDelayed(INDEX_TEST_APMCU, TIME_DELAYED);
                    }
                }
            }
            if (mLoopCountApMcu > 0 && mTestApMcuRunning) {
                mLoopCountApMcu--;
            }
            if (null != mTestClass) {
                if (mTestClass instanceof ApMcu
                        || mTestClass instanceof CpuStressTest) {
                    mTestClass.onUpdateTestResult();
                }
            }
            super.handleMessage(msg);
        }
    };

    class HandlerVideoCodec extends Handler {

        HandlerVideoCodec(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Xlog.v(TAG, "mTestHandlerVideoCodec receive msg: " + msg.what);
            if (INDEX_TEST_VIDEOCODEC == msg.what) {
                if (mLoopCountVideoCodec <= 0) {
                    mLoopCountVideoCodec = 0;
                    mTestVideoCodecRunning = false;
                    updateWakeLock();
                    removeMessages(INDEX_TEST_VIDEOCODEC);
                } else {
                    if (mWantStopSwCodec) {
                        mTestVideoCodecRunning = false;
                        mWantStopSwCodec = false;
                        removeMessages(INDEX_TEST_VIDEOCODEC);
                        updateWakeLock();
                    } else {
                        // lLoopCountVideoCodec--;
                        doVideoCodecTest();
                        sendEmptyMessageDelayed(INDEX_TEST_VIDEOCODEC,
                                TIME_DELAYED);
                    }
                }
            }
            if (mLoopCountVideoCodec > 0 && mTestVideoCodecRunning) {
                mLoopCountVideoCodec--;
            }
            if (null != mTestClass) {
                if (mTestClass instanceof SwVideoCodec
                        || mTestClass instanceof CpuStressTest) {
                    mTestClass.onUpdateTestResult();
                }
            }
            super.handleMessage(msg);
        }
    };

    class HandlerBackupRestore extends Handler {

        HandlerBackupRestore(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Xlog.v(TAG, "mTestHandlerBackupRestore receive msg: " + msg.what);
            switch (msg.what) {
            case INDEX_TEST_BACKUP:
            case INDEX_TEST_RESTORE:
                doBackupRestore(msg.arg1);
                break;
            default:
                break;
            }
            if (null != mTestClass) {
                mTestClass.onUpdateTestResult();
            }
            super.handleMessage(msg);
        }
    };

    static class WakeLock {
        private PowerManager.WakeLock mScreenWakeLock = null;
        private PowerManager.WakeLock mCpuWakeLock = null;

        /**
         * Acquire CPU wake lock
         * 
         * @param context
         *            Global information about an application environment
         */
        void acquireCpuWakeLock(Context context) {
            Xlog.v(TAG, "Acquiring cpu wake lock");
            if (mCpuWakeLock != null) {
                return;
            }

            PowerManager pm = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);

            mCpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
            // | PowerManager.ON_AFTER_RELEASE, TAG);
            mCpuWakeLock.acquire();
        }

        /**
         * Acquire screen wake lock
         * 
         * @param context
         *            Global information about an application environment
         */
        void acquireScreenWakeLock(Context context) {
            Xlog.v(TAG, "Acquiring screen wake lock");
            if (mScreenWakeLock != null) {
                return;
            }

            PowerManager pm = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);

            mScreenWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
            // | PowerManager.ON_AFTER_RELEASE, TAG);
            mScreenWakeLock.acquire();
        }

        /**
         * Acquire wake lock
         * 
         * @param context
         *            Global information about an application environment
         */
        void acquire(Context context) {
            acquireScreenWakeLock(context);
            // acquireCpuWakeLock(context);
        }

        /**
         * Release wake lock
         */
        void release() {
            Xlog.v(TAG, "Releasing wake lock");
            if (mCpuWakeLock != null) {
                mCpuWakeLock.release();
                mCpuWakeLock = null;
            }
            if (mScreenWakeLock != null) {
                mScreenWakeLock.release();
                mScreenWakeLock = null;
            }
        }
    }
}
