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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.MediatekDM.DmConst.TAG;

public class DmScomoActivity extends Activity implements DmScomoUpdateListener {
    private DmService dmService = null;

    RelativeLayout mDownloadingLayout;
    RelativeLayout mInstallingLayout;
    LinearLayout mEmptyLayout;
    private boolean mIsPaused = false;

    // private boolean mIsServiceBound=false;
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG.Scomo, "---mConnection onServiceConnected---");
            // mIsServiceBound=true;
            if (DmService.getInstance() == null) {
                Log.e(TAG.Scomo, "weired 1, null");
            }
            DmService.getInstance().registerScomoListener(DmScomoActivity.this);

            // if(!Options.UseDirectInternet){
            // try {
            // Log.e(TAG.Scomo,"startDmDataConnectivity...");
            // DmDataConnection.getInstance(DmScomoActivity.this).startDmDataConnectivity();
            // } catch (Exception e) {
            // e.printStackTrace ();
            // }
            // }

            onScomoUpdated();
        }

        public void onServiceDisconnected(ComponentName name) {
            // mIsServiceBound=false;
        }
    };

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.setContentView(R.layout.scomo);
        this.setTitle(R.string.scomo_activity_title);

        mDownloadingLayout = (RelativeLayout) this.findViewById(R.id.LayoutDownloading);
        mInstallingLayout = (RelativeLayout) this.findViewById(R.id.LayoutInstalling);
        mEmptyLayout = (LinearLayout) this.findViewById(R.id.LayoutEmpty);

        mDownloadingLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(DmScomoActivity.this,
                        DmScomoDownloadDetailActivity.class);
                intent.setAction("download_detail");
                startActivity(intent);
            }
        });

        Intent serviceIntent = new Intent(this, DmService.class);
        serviceIntent.setAction("com.mediatek.MediatekDM.DMSERVE");
        startService(serviceIntent);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        // if (DmService.getInstance()==null) {
        // Intent serviceIntent = new Intent(this, DmService.class);
        // serviceIntent.setAction("com.mediatek.MediatekDM.DMSERVE");
        // startService(serviceIntent);
        // bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
        // } else {
        // dmService=DmService.getInstance();
        // dmService.registerScomoListener(DmScomoActivity.this);
        // }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG.Scomo, "DmScomoActivity is onResume");
        mIsPaused = false;
        onScomoUpdated();
    }

    protected void onPause() {
        Log.d(TAG.Scomo, "DmScomoActivity is onPause");
        mIsPaused = true;
        super.onPause();
    }

    public void onScomoUpdated() {
        if (mIsPaused) {
            return;
        }

        if (DmService.getInstance() == null) {
            return;
        }

        int registerSimId = DmCommonFunction.getRegisteredSimId(this);
        if (registerSimId == -1) {
            Log.w(TAG.Scomo, "The sim card is not register to DM server, show Empty View");
            showEmpty();
            return;
        }
        DmScomoState scomoState = DmService.getInstance().getScomoState();

        if (scomoState == null) {
            Log.e(TAG.Scomo, "scomoState is null, wait to load ScomoState");
            showLoading();
            return;
        }

        hideDownloading();
        hideInstalling();
        hideEmpty();

        if (!scomoState.mVerbose || scomoState.state == DmScomoState.IDLE
                || scomoState.state == DmScomoState.ABORTED) {
            showEmpty();
        } else if (scomoState.state == DmScomoState.DOWNLOADING) {
            showDownloading(getString(R.string.downloading_scomo));
        } else if (scomoState.state == DmScomoState.DOWNLOADING_STARTED
                || scomoState.state == DmScomoState.RESUMED) {
            showDownloading(getString(R.string.connecting));
        } else if (scomoState.state == DmScomoState.INSTALLING) {
            showInstalling(getString(R.string.installing_scomo));
        } else if (scomoState.state == DmScomoState.UPDATING) {
            showInstalling(getString(R.string.upgrading));
        } else if (scomoState.state == DmScomoState.PAUSED) {
            showDownloading(getString(R.string.paused));
        } else if (scomoState.state == DmScomoState.CONFIRM_DOWNLOAD
                || scomoState.state == DmScomoState.INSTALL_FAILED
                || scomoState.state == DmScomoState.DOWNLOAD_FAILED
                || scomoState.state == DmScomoState.INSTALL_OK
                || scomoState.state == DmScomoState.GENERIC_ERROR) {
            displayDialog(scomoState.state);
        }
    }

    private void showEmpty() {
        mDownloadingLayout.setVisibility(View.GONE);
        mInstallingLayout.setVisibility(View.GONE);
        mEmptyLayout.setVisibility(View.VISIBLE);
        ((ProgressBar) mEmptyLayout.findViewById(R.id.progressLoading)).setVisibility(View.GONE);
        ((TextView) mEmptyLayout.findViewById(R.id.TextViewEmpty)).setText(R.string.no_activity);
    }

    private void showLoading() {
        mDownloadingLayout.setVisibility(View.GONE);
        mInstallingLayout.setVisibility(View.GONE);
        mEmptyLayout.setVisibility(View.VISIBLE);
        ((ProgressBar) mEmptyLayout.findViewById(R.id.progressLoading)).setVisibility(View.VISIBLE);
        ((TextView) mEmptyLayout.findViewById(R.id.TextViewEmpty)).setText(R.string.loading);
    }

    private void hideEmpty() {
        mEmptyLayout.setVisibility(View.GONE);
    }

    private void hideInstalling() {
        mInstallingLayout.setVisibility(View.GONE);
    }

    private void hideDownloading() {
        mDownloadingLayout.setVisibility(View.GONE);
    }

    private void displayDialog(int action) {
        Intent intent = new Intent(this, DmScomoConfirmActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("action", action);
        startActivity(intent);
    }

    private void showInstalling(String state) {
        mInstallingLayout.setVisibility(View.VISIBLE);
        mDownloadingLayout.setVisibility(View.GONE);
        mEmptyLayout.setVisibility(View.GONE);
        DmScomoState scomoState = DmService.getInstance().getScomoState();

        ((TextView) mInstallingLayout.findViewById(R.id.title)).setText(state);
        ((ImageView) mInstallingLayout.findViewById(R.id.icon)).setImageDrawable(scomoState
                .getIcon());
        ((TextView) mInstallingLayout.findViewById(R.id.name)).setText(scomoState.getName());
        ((TextView) mInstallingLayout.findViewById(R.id.version)).setText(scomoState.getVersion()
                + "  "
                + scomoState.getSize() / 1024 + "KB");

    }

    private void showDownloading(String state) {
        mInstallingLayout.setVisibility(View.GONE);
        mEmptyLayout.setVisibility(View.GONE);
        mDownloadingLayout.setVisibility(View.VISIBLE);

        DmScomoState scomoState = DmService.getInstance().getScomoState();
        ((TextView) mDownloadingLayout.findViewById(R.id.title)).setText(state);
        ((ImageView) mDownloadingLayout.findViewById(R.id.downloadingIcon))
                .setImageDrawable(scomoState.getIcon());
        ((TextView) mDownloadingLayout.findViewById(R.id.TextViewName)).setText(scomoState
                .getName());
        String ratio = scomoState.currentSize / 1024 + "KB/" + scomoState.totalSize / 1024 + "KB";
        ((TextView) mDownloadingLayout.findViewById(R.id.TextViewSize)).setText(ratio);
        ((ProgressBar) mDownloadingLayout.findViewById(R.id.ProgressBarProgress))
                .setMax(scomoState.totalSize);
        ((ProgressBar) mDownloadingLayout.findViewById(R.id.ProgressBarProgress))
                .setProgress(scomoState.currentSize);
    }

    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        if (DmService.getInstance() != null) {
            DmService.getInstance().removeScomoListener(this);
        }
        unbindService(mConnection);
        super.onDestroy();
    }
}
