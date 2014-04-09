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

package com.android.camera.manager;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.camera.Camera;
import com.android.camera.Log;
import com.android.camera.R;

//import com.mediatek.common.featureoption.FeatureOption;

public class RotateProgress extends ViewManager {
    @SuppressWarnings("unused")
    private static final String TAG = "RotateProgress";
    private static final boolean LOG = Log.LOGV;
    
    private ProgressBar mRotateDialogSpinner;
    private TextView mRotateDialogText;
    
    private String mMessage;

    public RotateProgress(Camera context) {
        super(context, VIEW_LAYER_OVERLAY);
    }
    
    @Override
    protected View getView() {
        View v = getContext().inflate(R.layout.rotate_progress, getViewLayer());
        mRotateDialogSpinner = (ProgressBar) v.findViewById(R.id.rotate_dialog_spinner);
        mRotateDialogText = (TextView) v.findViewById(R.id.rotate_dialog_text);
        return v;
    }

    @Override
    protected void onRefresh() {
        mRotateDialogText.setText(mMessage);
        mRotateDialogText.setVisibility(View.VISIBLE);
        mRotateDialogSpinner.setVisibility(View.VISIBLE);
        if (LOG) {
            Log.v(TAG, "onRefresh() mMessage=" + mMessage);
        }
    }
    
    public void showProgress(String msg) {
        mMessage = msg;
        show();
        if (LOG) {
            Log.v(TAG, "showProgress(" + msg + ")");
        }
    }
}
