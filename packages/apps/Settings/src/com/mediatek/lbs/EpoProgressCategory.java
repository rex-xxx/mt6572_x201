/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.lbs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.settings.ProgressCategoryBase;
import com.android.settings.R;

public class EpoProgressCategory extends ProgressCategoryBase {

    private boolean mProgress = false;
    private String mText;

    /**
     * Epo Progress Category
     * 
     * @param context
     *            Context
     * @param attrs
     *            AttributeSet
     */
    public EpoProgressCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_progress_category_for_lbs);
        mText = context.getResources().getString(R.string.progress_scanning);
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        TextView textView = (TextView) view.findViewById(R.id.scanning_text);
        View progressBar = view.findViewById(R.id.scanning_progress);

        int visibility = mProgress ? View.VISIBLE : View.INVISIBLE;
        textView.setVisibility(visibility);
        textView.setText(mText);
        progressBar.setVisibility(visibility);
    }

    /**
     * Turn on/off the progress indicator and text on the right.
     * 
     * @param progressOn
     *            whether or not the progress should be displayed
     */
    public void setProgress(boolean progressOn) {
        mProgress = progressOn;
        notifyChanged();
    }

    /**
     * set the text to be shown in info area
     * 
     * @param text
     *            String
     */
    public void setText(String text) {
        mText = text;
    }
}
