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

package com.mediatek.contacts.widget;
import com.mediatek.storage.StorageManagerEx;

import android.widget.ImageView;

import android.widget.RadioButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.graphics.drawable.Drawable;
import com.android.contacts.ContactsApplication;

import com.android.contacts.R;

public class ImportExportItem extends LinearLayout {

    private static final String TAG = ImportExportItem.class.getSimpleName();

    private TextView mAccountUserName;
    private ImageView mIcon;
    private RadioButton mRadioButton;

    public ImportExportItem(Context context) {
        super(context);
    }

    public ImportExportItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setActivated(boolean activated) {
        super.setActivated(activated);
        if (mRadioButton != null) {
            mRadioButton.setChecked(activated);
        } else {
            Log.wtf(TAG, "radio-button cannot be activated because it is null");
        }
    }

    public void bindView(Drawable icon, String text, String path) {
        mAccountUserName = (TextView) findViewById(R.id.accountUserName);
        mIcon = (ImageView) findViewById(R.id.icon);
        mRadioButton = (RadioButton) findViewById(R.id.radioButton);

        if (icon != null && path == null) {
            mIcon.setImageDrawable(icon);
        } else if (path != null) {
            String internal = StorageManagerEx.getInternalStoragePath();
            Log.d(TAG, "internal: " + internal);
            if (path.equals(internal)) {
                mIcon.setImageResource(R.drawable.contact_phone_storage);
            } else {
                mIcon.setImageResource(R.drawable.contact_sd_card_icon);

            }
        } else {
            mIcon.setImageResource(R.drawable.unknown_source);
        }

        mAccountUserName.setText(text);
    }
}
