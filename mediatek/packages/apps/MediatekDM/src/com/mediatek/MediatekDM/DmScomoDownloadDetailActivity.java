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

package com.mediatek.MediatekDM;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.util.DialogFactory;

public class DmScomoDownloadDetailActivity extends Activity implements DmScomoUpdateListener {
    private Button mPauseButton;
    private Button mCancelButton;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private TextView mDescription;
    private TextView mNewFeature;
    private boolean mIsPaused = false;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setContentView(R.layout.downloading);
        this.setTitle(R.string.scomo_activity_title);
        mPauseButton = (Button) findViewById(R.id.buttonSuspend);
        mCancelButton = (Button) findViewById(R.id.cancellbutton);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbarDownload);
        mProgressText = (TextView) findViewById(R.id.rate);
        mDescription = (TextView) findViewById(R.id.dscrpContentDl);
        mNewFeature = (TextView) findViewById(R.id.featureNotesDl);

        DmService.getInstance().registerScomoListener(this);

        mPauseButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                mPauseButton.setEnabled(false);
                mCancelButton.setEnabled(false);
                Log.i(TAG.Scomo, "detail activity on btn begin");
                DmScomoState scomoState = DmService.getInstance().getScomoState();
                if (scomoState.state == DmScomoState.PAUSED) {
                    DmService.getInstance().resumeDlScomoPkg();
                } else {
                    DmService.getInstance().pauseDlScomoPkg();
                }
                Log.i(TAG.Scomo, "detail activity on btn end");
            }

        });

        mCancelButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                mPauseButton.setEnabled(false);
                mCancelButton.setEnabled(false);
                DmService.getInstance().pauseDlScomoPkg();
                DialogFactory
                        .newAlert(DmScomoDownloadDetailActivity.this)
                        .setTitle(R.string.scomo_activity_title)
                        .setIcon(R.drawable.ic_dialog_info)
                        .setMessage(R.string.scomo_cancel_download_message)
                        .setNegativeButton(R.string.scomo_discard,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        DmService.getInstance().cancelDlScomoPkg();
                                        DmScomoDownloadDetailActivity.this.finish();
                                    }
                                })
                        .setPositiveButton(R.string.scomo_continue,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        mPauseButton.setEnabled(false);
                                        mCancelButton.setEnabled(false);
                                        DmService.getInstance().resumeDlScomoPkg();
                                    }
                                })
                        .show();
            }
        });
    }

    public void onResume() {
        Log.i(TAG.Scomo, "DmScomoDownloadDetailActivity onResume");
        mIsPaused = false;
        super.onResume();
        onScomoUpdated();
    }

    public void onScomoUpdated() {
        if (mIsPaused) {
            return;
        }

        Log.i(TAG.Scomo, "detail on update begin..");

        DmScomoState scomoState = DmService.getInstance().getScomoState();
        if (scomoState == null) {
            return;
        }
        // set description
        mDescription.setText(scomoState.getDescription());
        String version = scomoState.getVersion();
        if (TextUtils.isEmpty(version)) {
            version = getString(R.string.unknown);
        }
        mNewFeature.setText(getString(R.string.featureNotes, version,
                String.valueOf(scomoState.totalSize / 1024) + "KB"));
        // set progress
        String progress = scomoState.currentSize / 1024 + "KB / " + scomoState.totalSize / 1024
                + "KB";
        mProgressText.setText(progress);
        mProgressBar.setMax(scomoState.totalSize);
        mProgressBar.setProgress(scomoState.currentSize);
        //
        Log.i(TAG.Scomo, "state is " + scomoState.state);
        if (scomoState.state == DmScomoState.DOWNLOADING) {
            mPauseButton.setText(R.string.pause);
            mPauseButton.setEnabled(true);
            mCancelButton.setEnabled(true);
        } else if (scomoState.state == DmScomoState.RESUMED) {
            mPauseButton.setText(R.string.pause);
            mPauseButton.setEnabled(false);
            mCancelButton.setEnabled(false);
        } else if (scomoState.state == DmScomoState.DOWNLOADING_STARTED) {
            mPauseButton.setText(R.string.pause);
            mPauseButton.setEnabled(true);
            mCancelButton.setEnabled(true);
        } else if (scomoState.state == DmScomoState.PAUSED) {
            mPauseButton.setText(R.string.resume);
            mPauseButton.setEnabled(true);
            mCancelButton.setEnabled(true);
        } else {
            this.finish();
        }
        Log.i(TAG.Scomo, "detail activity on update done");
    }

    protected void onPause() {
        Log.i(TAG.Scomo, "DmScomoDownloadDetailActivity onPause");
        mIsPaused = true;
        super.onPause();
    }

    public void onDestroy() {
        super.onDestroy();
        DmService.getInstance().removeScomoListener(this);
    }
}
