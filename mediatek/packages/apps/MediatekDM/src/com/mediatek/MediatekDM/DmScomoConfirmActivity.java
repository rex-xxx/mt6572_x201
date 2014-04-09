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
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomoDp;
import com.mediatek.MediatekDM.util.DialogFactory;

public class DmScomoConfirmActivity extends Activity implements OnClickListener {
    private static final int ACTION_DOWNLOAD = 1;
    private static final int ACTION_INSTALL = 2;

    private int mOkAction = 0;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ((NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE))
                .cancel(DmScomoNotification.NOTIFICATION);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        int action = getIntent().getIntExtra("action", -1);
        Log.e(TAG.Scomo, "confirmActivity: action is " + action);
        if (action == DmScomoState.CONFIRM_DOWNLOAD ||
                action == DmScomoState.INSTALL_FAILED ||
                action == DmScomoState.DOWNLOAD_FAILED ||
                action == DmScomoState.INSTALL_OK ||
                action == DmScomoState.GENERIC_ERROR) {
            showDialog(action);
        } else {
            finish();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case DmScomoState.CONFIRM_DOWNLOAD:
                dialog = onConfirmDownload();
                break;
            /*
             * case DmScomoState.CONFIRM_INSTALL: dialog = checkPackageError();
             * if(dialog == null){ dialog = checkSpaceError(); } if(dialog ==
             * null){ dialog = onConfirmInstall(); } break;
             */
            case DmScomoState.INSTALL_FAILED:
                dialog = onInstallFailed();
                break;
            case DmScomoState.DOWNLOAD_FAILED:
                dialog = onDownloadFailed();
                break;
            case DmScomoState.INSTALL_OK:
                dialog = onInstallOk();
                break;
            case DmScomoState.GENERIC_ERROR:
                dialog = onGenericError();
                break;
            default:
                break;
        }
        return dialog;
    }

    private Dialog onGenericError() {
        Log.v(TAG.Scomo, "[DmScomoConfirmActivity] onGenericError");
        return DialogFactory.newAlert(this)
                .setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.unknown_error)
                .setNeutralButton(R.string.ok, this)
                .create();
    }

    private Dialog onConfirmInstall() {
        Log.v(TAG.Scomo, "[DmScomoConfirmActivity] onConfirmInstall");
        DmScomoState scomoState = DmService.getInstance().getScomoState();
        boolean packageInstalled = DmScomoPackageManager.getInstance().isPackageInstalled(
                scomoState.getPackageName());

        StringBuilder sb = new StringBuilder();
        sb.append(this.getString(R.string.download_complete) + "\n");
        sb.append(this.getString(R.string.name));
        sb.append(scomoState.getName() + "\n");
        sb.append(this.getString(R.string.version));
        sb.append(scomoState.getVersion() + "\n");
        sb.append(this.getString(R.string.size));
        sb.append(scomoState.getSize() / 1024 + "KB\n");

        CharSequence text = "";
        if (packageInstalled) {
            sb.append(this.getString(R.string.confirm_upgrade_msg));
            text = getText(R.string.upgrade);
        } else {
            sb.append(this.getString(R.string.confirm_install_msg));
            text = getText(R.string.install);
        }

        mOkAction = ACTION_INSTALL;

        return DialogFactory.newAlert(this)
                .setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(sb.toString())
                .setPositiveButton(text, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.v(TAG.Scomo,
                                "[DmScomoConfirmActivity] positive button clicked, start to install");
                        MdmScomoDp dp = DmService.getInstance().getScomoState().currentDp;
                        try {
                            dp.executeInstall();
                        } catch (Exception e) {

                        }
                    }
                })
                .setNegativeButton(R.string.cancel, this)
                .create();
    }

    private Dialog checkPackageError() {
        Log.v(TAG.Scomo, "[DmScomoConfirmActivity] checkPackageError");
        boolean ret = true;
        DmScomoState scomoState = DmService.getInstance().getScomoState();
        DmScomoPackageManager.ScomoPackageInfo info =
                DmScomoPackageManager.getInstance().getMinimalPackageInfo(
                        scomoState.archiveFilePath);
        ret = (info != null);
        if (!ret) {
            Log.w(TAG.Scomo,
                    "[DmScomoConfirmActivity] checkPackageError: error! ScomoPackageInfo is null");
            return DialogFactory.newAlert(this)
                    .setCancelable(false)
                    .setTitle(R.string.software_update)
                    .setMessage(R.string.package_format_error)
                    .setNeutralButton(R.string.ok, this)
                    .create();
        }
        return null;
    }

    private Dialog checkSpaceError() {
        Log.v(TAG.Scomo, "[DmScomoConfirmActivity] checkSpaceError");
        boolean ret = true;
        DmScomoState scomoState = DmService.getInstance().getScomoState();
        String archiveFilePath = scomoState.archiveFilePath;
        ret = DmScomoPackageManager.getInstance().checkSpace(archiveFilePath);
        if (!ret) {
            Log.w(TAG.Scomo, "[DmScomoConfirmActivity] checkSpaceError: error! checkSpace fail");
            return DialogFactory.newAlert(this)
                    .setCancelable(false)
                    .setTitle(R.string.software_update)
                    .setMessage(R.string.insufficent_storage)
                    .setNeutralButton(R.string.ok, this)
                    .create();
        } else {
            return null;
        }
    }

    private Dialog onDownloadFailed() {
        Log.v(TAG.Scomo, "[DmScomoConfirmActivity] onDownloadFailed");
        return DialogFactory.newAlert(this)
                .setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.download_failed)
                .setNeutralButton(R.string.ok, this)
                .create();
    }

    private Dialog onInstallOk() {
        Log.v(TAG.Scomo, "[DmScomoConfirmActivity] onInstallOk");
        return DialogFactory.newAlert(this)
                .setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.install_complete)
                .setNeutralButton(R.string.ok, this)
                .create();
    }

    private Dialog onInstallFailed() {
        Log.v(TAG.Scomo, "[DmScomoConfirmActivity] onInstallFailed");

        return DialogFactory.newAlert(this)
                .setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.install_failed)
                .setNeutralButton(R.string.ok, this)
                .create();
    }

    private Dialog onConfirmDownload() {
        Log.v(TAG.Scomo, "[DmScomoConfirmActivity] onConfirmDownload");
        mOkAction = ACTION_DOWNLOAD;

        return DialogFactory.newAlert(this)
                .setCancelable(false)
                .setTitle(R.string.software_update)
                .setMessage(R.string.confirm_download_msg)
                .setPositiveButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.v(TAG.Scomo,
                                "[DmScomoConfirmActivity] positive button clicked, start to download");
                        DmService.getInstance().startDlScomoPkg();
                    }
                })
                .setNegativeButton(R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                Log.w(TAG.Scomo, "[DmScomoConfirmActivity] positive button should not be clicked");
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                Log.v(TAG.Scomo,
                        "[DmScomoConfirmActivity] negative button clicked,reset scomo state & cancelDLScomoPkg");
                DmService.getInstance().pauseDlScomoPkg();// cancel DL session
                DmService.getInstance().cancelDlScomoPkg();// trigger report
                                                           // session
            case DialogInterface.BUTTON_NEUTRAL:
                Log.v(TAG.Scomo,
                        "[DmScomoConfirmActivity] neutral button clicked, reset scomo state");
                DmScomoState scomoState = DmService.getInstance().getScomoState();
                scomoState.state = DmScomoState.IDLE;
                DmScomoState.store(DmScomoConfirmActivity.this, scomoState);
                break;
            default:
                break;
        }
        DmScomoConfirmActivity.this.finish();
    }
}
