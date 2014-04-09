/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.mediatek.phone.provider;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import com.mediatek.phone.provider.CallHistory.Calls;

public class CallHistoryAsync {

    private static final String TAG = "CallHistoryAsync";

    public static class DeleteCallArgs {

        public DeleteCallArgs(Context context, String number) {
            mContext = context;
            mNumber = number;
        }

        public final Context mContext;
        public final String  mNumber;
    }

    public AsyncTask deleteCall(DeleteCallArgs args) {
        assertUiThread();
        return new DeleteCallTask().execute(args);
    }

    /**
     * AsyncTask to save calls in the DB.
     */
    private class DeleteCallTask extends AsyncTask<DeleteCallArgs, Void, Integer[]> {
        @Override
        protected Integer[] doInBackground(DeleteCallArgs... numberList) {
            int count = numberList.length;
            Integer[] result = new Integer[count];
            for (int i = 0; i < count; i++) {
                DeleteCallArgs c = numberList[i];

                try {
                    result[i] = Calls.deleteNumber(c.mContext, c.mNumber);
                } catch (Exception e) {
                    Log.e(TAG, "Exception raised during delete call entry: " + e);
                    result[i] = null;
                }
            }
            return result;
        }
    }

    public static class AddCallArgs {

        public AddCallArgs(Context context, String number, String countryISO,
                           long start, int slotId, boolean isMultiSim) {
            this.mContext = context;
            this.mNumber = number;
            this.mCountryISO = countryISO;
            this.mStart = start;
            this.mSlotId = slotId;
            this.mIsMultiSim = isMultiSim;
        }

        public final Context mContext;
        public final String mNumber;
        public final String mCountryISO;
        public final long mStart;
        public final int mSlotId;
        public final boolean mIsMultiSim;
    }

    public AsyncTask addCall(AddCallArgs args) {
        assertUiThread();
        return new AddCallTask().execute(args);
    }

    /**
     * AsyncTask to save calls in the DB.
     */
    private class AddCallTask extends AsyncTask<AddCallArgs, Void, Void> {
        //Uri[]> {
        @Override
        protected Void doInBackground(AddCallArgs... numberList) {
            int count = numberList.length;
            for (int i = 0; i < count; i++) {
                AddCallArgs c = numberList[i];

                try {
                    Calls.addCallNumber(c.mContext, c.mNumber, c.mCountryISO,
                                        c.mStart, c.mSlotId, c.mIsMultiSim);
                } catch (Exception e) {
                    Log.e(TAG, "Exception raised during adding CallLog entry: " + e);
                    //result[i] = null;
                }
            }
            return null;
        }
    }

    public static class UpdateConfirmFlagArgs {

        public UpdateConfirmFlagArgs(Context context, String number, long confirm) {

            mContext = context;
            mNumber = number;
            mConfirm = confirm;
        }

        public final Context mContext;
        public final String mNumber;
        public final long mConfirm;
    }

    public AsyncTask updateConfirmFlag(UpdateConfirmFlagArgs args) {
        assertUiThread();
        return new UpdateConfirmFlagTask().execute(args);
    }

    /**
     * AsyncTask to save calls in the DB.
     */
    private class UpdateConfirmFlagTask extends AsyncTask<UpdateConfirmFlagArgs, Void, Integer[]> {
        @Override
        protected Integer[] doInBackground(UpdateConfirmFlagArgs... numberList) {
            int count = numberList.length;
            Integer[] result = new Integer[count];
            for (int i = 0; i < count; i++) {
                UpdateConfirmFlagArgs c = numberList[i];

                try {
                    result[i] = Calls.updateConfirmFlag(c.mContext, c.mNumber, c.mConfirm);
                } catch (Exception e) {
                    Log.e(TAG, "Exception raised during update confirm flag entry: " + e);
                    result[i] = null;
                }
            }
            return result;
        }
    }

    private void assertUiThread() {
        if (!Looper.getMainLooper().equals(Looper.myLooper())) {
            throw new RuntimeException("Not on the UI thread!");
        }
    }
}
