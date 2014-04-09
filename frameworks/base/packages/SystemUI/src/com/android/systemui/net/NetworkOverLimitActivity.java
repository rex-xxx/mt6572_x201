/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.net;

import static android.net.NetworkPolicyManager.EXTRA_NETWORK_TEMPLATE;
import static android.net.NetworkTemplate.MATCH_MOBILE_3G_LOWER;
import static android.net.NetworkTemplate.MATCH_MOBILE_4G;
import static android.net.NetworkTemplate.MATCH_MOBILE_ALL;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.INetworkPolicyManager;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import android.view.WindowManager;

import com.android.systemui.R;


import android.content.Intent;

/**
 * Notify user that a {@link NetworkTemplate} is over its
 * {@link NetworkPolicy#limitBytes}, giving them the choice of acknowledging or
 * "snoozing" the limit.
 */
public class NetworkOverLimitActivity extends Activity {
    private static final String TAG = "NetworkOverLimitActivity";
    private boolean isReenabled =false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final NetworkTemplate template = getIntent().getParcelableExtra(EXTRA_NETWORK_TEMPLATE);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getLimitedDialogTitleForTemplate(template));
        builder.setMessage(R.string.data_usage_disabled_dialog);

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(
                R.string.data_usage_disabled_dialog_enable, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        isReenabled=true;
                        snoozePolicy(template);
                    }
                });

        final Dialog dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                ///M: add a broadcast intent for SIM management progress timeout ALPS00552342
                if(isReenabled==false)sendBroadcast();
                finish();
            }
        });

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    private void snoozePolicy(NetworkTemplate template) {
        final INetworkPolicyManager policyService = INetworkPolicyManager.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
        try {
            policyService.snoozeLimit(template);
        } catch (RemoteException e) {
            Slog.w(TAG, "problem snoozing network policy", e);
        }
    }

    private static int getLimitedDialogTitleForTemplate(NetworkTemplate template) {
        switch (template.getMatchRule()) {
            case MATCH_MOBILE_3G_LOWER:
                return R.string.data_usage_disabled_dialog_3g_title;
            case MATCH_MOBILE_4G:
                return R.string.data_usage_disabled_dialog_4g_title;
            case MATCH_MOBILE_ALL:
                return R.string.data_usage_disabled_dialog_mobile_title;
            default:
                return R.string.data_usage_disabled_dialog_title;
        }
    }
///M: the following are MTK added
    private void sendBroadcast() {
        final String ACTION_DATA_USAGE_DISABLED_DIALOG_OK =
            "com.mediatek.systemui.net.action.ACTION_DATA_USAGE_DISABLED_DIALOG_OK"; 
            
        Slog.v(TAG, "sendBroadcast ACTION_DATA_USAGE_DISABLED_DIALOG_OK");
        Intent intent = new Intent(ACTION_DATA_USAGE_DISABLED_DIALOG_OK);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);       
        this.sendBroadcast(intent);
      }
}
