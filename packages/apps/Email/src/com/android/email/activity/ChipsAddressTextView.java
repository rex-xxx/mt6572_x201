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

package com.android.email.activity;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.Filter;

import com.android.emailcommon.Logging;
import com.android.ex.chips.MTKRecipientEditTextView;

/**
 * This is a MultiAutoCompleteTextView which has a custom validator.
 */
class ChipsAddressTextView extends MTKRecipientEditTextView {
    /**
     *  Set the search address threshold value as 1
     *  The default threshold length is 2.
     */
    public static final int AUTO_SEARCH_THRESHOLD_LENGTH = 1;
    private static final long DELETE_KEY_POST_DELAY = 500L;
    private static final long ADD_POST_DELAY = 300L;
    /** A noop validator that does not munge invalid texts. */
    private static class ForwardValidator implements Validator {
        private Validator mValidator = null;

        public CharSequence fixText(CharSequence invalidText) {
            return invalidText;
        }

        public boolean isValid(CharSequence text) {
            return mValidator != null ? mValidator.isValid(text) : true;
        }

        public void setValidator(Validator validator) {
            mValidator = validator;
        }
    }

    private final ForwardValidator mInternalValidator = new ForwardValidator();

    public ChipsAddressTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setValidator(mInternalValidator);
        //set search address threshold length as 1
        setThreshold(AUTO_SEARCH_THRESHOLD_LENGTH);
    }

    @Override
    public void setValidator(Validator validator) {
        mInternalValidator.setValidator(validator);
    }

    public void setGalSearchDelayer() {
        Filter filter = getFilter();
        /** M: MTK Dependence @{ */
        if (filter != null) {
            filter.setDelayer(new Filter.Delayer() {

                private int mPreviousLength = 0;

                public long getPostingDelay(CharSequence constraint) {
                    if (constraint == null) {
                        return 0;
                    }

                    long delay = constraint.length() < mPreviousLength 
                            ? DELETE_KEY_POST_DELAY : ADD_POST_DELAY;
                    mPreviousLength = constraint.length();
                    return delay;
                }
            });
        }
        /** @} */
    }

    /**
     * M: Not perform GAL searching if current selection ending with a completed address.
     * (ending with ", " or "; ").
     * By default, if current text is "xxx@126.com, t" and user delete the last char 't',
     * it will auto perform searching 'xxx@126.com, ', which will cause UI abnormal
     * and list popup window showing for a long time.
     * After fixed, it will not search 'xxx@126.com, ' when delete the last char 't'.
     */
    @Override
    public boolean enoughToFilter() {
        Editable s = getText();
        if (s != null) {
            int end = s.length();
            if (end > 2 && s.charAt(end - 1) == ' '
                    && (s.charAt(end - 2) == ',' || s.charAt(end - 2) == ';')) {
                Logging.d("Not perfom GAL search for current text is " + s);
                return false;
            }
        }
        return super.enoughToFilter();
    }
}
