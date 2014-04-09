package com.mediatek.contacts.calloption;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.phone.Constants;
import com.mediatek.calloption.InternationalDialogHandler;

public class ContactsInternationalDialogHandler extends InternationalDialogHandler {

    private static final String TAG = "ContactsInternationalDialogHandler";

    public ContactsInternationalDialogHandler(Context context, int type, int internationalDialOption,
                                              PrefixInfo prefixInfo) {
        super(context, type, internationalDialOption, prefixInfo);
        mCountrySelectDialogHandler = new ContactsInternationalCountrySelectDialogHandler(mContext, this);
    }

    protected View createPrefixConfirmDialogItems() {
        String description = mContext.getString(
                R.string.international_dialing_change_country_prefix,
                mPrefixInfo.mCountryName);
        String messageYes = mContext.getString(R.string.international_dialing_yes);
        String messageNo = mContext.getString(R.string.international_dialing_no);
        String numberYes = PhoneNumberUtils.formatNumber(mPrefixInfo.mNumberReplaceInterPrefix,
                                                         mPrefixInfo.mSuggestCountryISO);
        if (TextUtils.isEmpty(numberYes)) {
            numberYes = mPrefixInfo.mNumberReplaceInterPrefix;
        }
        String numberNo = PhoneNumberUtils.formatNumber(mPrefixInfo.mNumberOrigin, mPrefixInfo.mCurrentCountryISO);
        if (TextUtils.isEmpty(numberNo)) {
            numberNo = mPrefixInfo.mNumberOrigin;
        }
        return createSingleSelectItems(description, messageYes, numberYes, messageNo, numberNo);
    }

    protected View createCountryAreaSingleSelectDialogItems() {
        String description = mContext.getString(
                R.string.international_dialing_add_country_code_and_area_code,
                mPrefixInfo.mAreaName, mPrefixInfo.mCountryName, mPrefixInfo.mCountryCode + " " + mPrefixInfo.mAreaCode);
        String messageYes = mContext.getString(R.string.international_dialing_yes);
        String messageNo = mContext.getString(R.string.international_dialing_no);
        String numberYes = PhoneNumberUtils.formatNumber(
                           "+" + mPrefixInfo.mCountryCode + mPrefixInfo.mAreaCode + mPrefixInfo.mNumberSubscriber,
                           mPrefixInfo.mSuggestCountryISO);
        if (TextUtils.isEmpty(numberYes)) {
            numberYes = "+" + mPrefixInfo.mCountryCode + mPrefixInfo.mAreaCode + mPrefixInfo.mNumberSubscriber;
        }
        String numberNo = PhoneNumberUtils.formatNumber(mPrefixInfo.mNumberOrigin, mPrefixInfo.mCurrentCountryISO);
        if (TextUtils.isEmpty(numberNo)) {
            numberNo = mPrefixInfo.mNumberOrigin;
        }
        return createSingleSelectItems(description, messageYes, numberYes, messageNo, numberNo);
    }

    protected View createCountrySingleSelectDialogItems() {
        String description = mContext.getString(
                R.string.international_dialing_add_country_code,
                mPrefixInfo.mCountryName, mPrefixInfo.mCountryCode);
        String messageYes = mContext.getString(R.string.international_dialing_yes);
        String messageNo = mContext.getString(R.string.international_dialing_no);
        String numberYes = PhoneNumberUtils.formatNumber(
                               "+" + mPrefixInfo.mCountryCode + mPrefixInfo.mAreaCode + mPrefixInfo.mNumberSubscriber,
                               mPrefixInfo.mSuggestCountryISO);
        if (TextUtils.isEmpty(numberYes)) {
            numberYes = "+" + mPrefixInfo.mCountryCode + mPrefixInfo.mAreaCode + mPrefixInfo.mNumberSubscriber;
        }
        String numberNo = PhoneNumberUtils.formatNumber(mPrefixInfo.mNumberOrigin, mPrefixInfo.mCurrentCountryISO);
        if (TextUtils.isEmpty(numberNo)) {
            numberNo = mPrefixInfo.mNumberOrigin;
        }
        return createSingleSelectItems(description, messageYes, numberYes, messageNo, numberNo);
    }

    protected View createCountrySelectDialogItems(int internationalDialOption) {
        String description = "";
        if (Constants.INTERNATIONAL_DIAL_OPTION_WITH_COUNTRY_CODE == internationalDialOption) {
            description = mContext.getString(R.string.international_dialing_click_below_button);
        } else {
            description = mContext.getString(R.string.international_dialing_select_country);
        }
        String messageButton = mPrefixInfo.mCountryName + "(+" + mPrefixInfo.mCountryCode + ")";
        String editTextMessage = mContext.getString(R.string.international_dialing_need_area_code);
        String editTextAreaCode = mContext.getString(R.string.international_dialing_input_area_code);
        String numberMessage = mContext.getString(R.string.international_dialing_dialed_number);
        String numberDisplay
            = PhoneNumberUtils.formatNumber(
                    "+" + mPrefixInfo.mCountryCode + mPrefixInfo.mAreaCode + mPrefixInfo.mNumberSubscriber,
                    mPrefixInfo.mSuggestCountryISO);
        if (TextUtils.isEmpty(numberDisplay)) {
            numberDisplay = "+" + mPrefixInfo.mCountryCode + mPrefixInfo.mAreaCode + mPrefixInfo.mNumberSubscriber;
        }
        return createButtonEditTextItems(description, messageButton, editTextMessage, editTextAreaCode,
                                         false, numberMessage, numberDisplay,
                                         mContext.getString(R.string.international_dialing_area_code),
                                         mContext.getString(R.string.international_dialing_input_area_code));
    }

    protected View createCountrySelectAreaInputDialogItems(int internationalDialOption) {
        String description = "";
        if (Constants.INTERNATIONAL_DIAL_OPTION_WITH_COUNTRY_CODE == internationalDialOption) {
            description = mContext.getString(R.string.international_dialing_click_below_button);
        } else {
            description = mContext.getString(R.string.international_dialing_select_country);
        }
        String messageButton = mPrefixInfo.mCountryName + "(+" + mPrefixInfo.mCountryCode + ")";
        String editTextMessage = mContext.getString(R.string.international_dialing_need_area_code);
        String editTextAreaCode = mContext.getString(R.string.international_dialing_input_area_code);
        String numberMessage = mContext.getString(R.string.international_dialing_dialed_number);
        String numberDisplay = "+" + mPrefixInfo.mCountryCode + " ("
                               + mContext.getString(R.string.international_dialing_area_code)
                               + ") " + mPrefixInfo.mNumberOrigin;
        return createButtonEditTextItems(description, messageButton,
                                         editTextMessage, editTextAreaCode, true, numberMessage,
                                         numberDisplay, mContext.getString(R.string.international_dialing_area_code),
                                         mContext.getString(R.string.international_dialing_input_area_code));
    }

    protected View createCountrySelectDefaultAreaInputDialogItems(int internationalDialOption) {
        String description = "";
        if (Constants.INTERNATIONAL_DIAL_OPTION_WITH_COUNTRY_CODE == internationalDialOption) {
            description = mContext.getString(R.string.international_dialing_click_below_button);
        } else {
            description = mContext.getString(R.string.international_dialing_select_country);
        }
        String messageButton = mPrefixInfo.mCountryName + " (+" + mPrefixInfo.mCountryCode + ") ";
        String editTextMessage = mContext.getString(
                R.string.international_dialing_confirm_area_code,
                mPrefixInfo.mAreaName);
        String editTextAreaCode = mPrefixInfo.mAreaCode;
        if (!TextUtils.isEmpty(mPrefixInfo.mAreaName)) {
            editTextAreaCode += " (" + mPrefixInfo.mAreaName + ") ";
        }
        String numberMessage = mContext.getString(R.string.international_dialing_dialed_number);
        String numberDisplay = PhoneNumberUtils.formatNumber(
                "+" + mPrefixInfo.mCountryCode + " " + mPrefixInfo.mAreaCode + " "
                + mPrefixInfo.mNumberOrigin, mPrefixInfo.mSuggestCountryISO);
        if (TextUtils.isEmpty(numberDisplay)) {
            numberDisplay = "+" + mPrefixInfo.mCountryCode  + mPrefixInfo.mAreaCode + mPrefixInfo.mNumberOrigin;
        }
        return createButtonEditTextItems(description, messageButton,
                                         editTextMessage, editTextAreaCode, true, numberMessage,
                                         numberDisplay, mContext.getString(R.string.international_dialing_area_code),
                                         mContext.getString(R.string.international_dialing_input_area_code));
    }

    protected View createAreaInputSingleSelectDialogItems() {
        String description1 = mContext.getString(
                R.string.international_dialing_add_country_code_input_area_code,
                mPrefixInfo.mCountryName);
        String editText = mContext.getString(R.string.international_dialing_input_area_code);
        String description2 =
                mContext.getString(R.string.international_dialing_add_country_code_and_area_code_without_prefix);
        String messageYes = mContext.getString(R.string.international_dialing_yes);
        String numberYes = "+" + mPrefixInfo.mCountryCode + " ("
                + mContext.getString(R.string.international_dialing_area_code)
                + ") " + mPrefixInfo.mNumberOrigin;
        String messageNo = mContext.getString(R.string.international_dialing_no);
        String numberNo = PhoneNumberUtils.formatNumber(mPrefixInfo.mNumberOrigin, mPrefixInfo.mCurrentCountryISO);
        if (TextUtils.isEmpty(numberNo)) {
            numberNo = mPrefixInfo.mNumberOrigin;
        }
        return createEditTextSingleSelectItems(description1, editText, description2, messageYes, numberYes,
                                               messageNo, numberNo);
    }

    protected View createDefaultAreaInputSingleSelectDialogItems() {
        String description1 = mContext.getString(
                R.string.international_dialing_add_country_code_default_input_area_code,
                mPrefixInfo.mCountryName, mPrefixInfo.mAreaName);
        String editText = mPrefixInfo.mAreaCode;
        if (!TextUtils.isEmpty(mPrefixInfo.mAreaName)) {
            editText += " (" + mPrefixInfo.mAreaName + ") ";
        }
        String description2 =
                mContext.getString(R.string.international_dialing_add_country_code_and_area_code_without_prefix);
        String messageYes = mContext.getString(R.string.international_dialing_yes);
        String numberYes =
            PhoneNumberUtils.formatNumber("+" + mPrefixInfo.mCountryCode + mPrefixInfo.mAreaCode
                    + mPrefixInfo.mNumberOrigin, mPrefixInfo.mSuggestCountryISO);
        String messageNo = mContext.getString(R.string.international_dialing_no);
        String numberNo = PhoneNumberUtils.formatNumber(mPrefixInfo.mNumberOrigin, mPrefixInfo.mCurrentCountryISO);
        if (TextUtils.isEmpty(numberNo)) {
            numberNo = mPrefixInfo.mNumberOrigin;
        }
        return createEditTextSingleSelectItems(description1, editText, description2, messageYes, numberYes,
                                               messageNo, numberNo);
    }

    protected View createButtonEditTextItems(final String description1, final String buttonText,
                                             final String description2, final String editText,
                                             final boolean isEditShow, final String numberMessage,
                                             final String number, final String textAreaCode,
                                             final String textInputCodeHere) {

        mOriginSuggestNumber = number;
        mTextAreaCode = textAreaCode;
        mTextInputCodeHere = textInputCodeHere;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.international_number_prefix_text_button_text_edit_text_text, null);

        mDescriptionText = (TextView) dialogView.findViewById(R.id.text1);
        mDescriptionText.setText(description1);

        mCountrySelectButton = (Button) dialogView.findViewById(R.id.button);
        mCountrySelectButton.setText(buttonText);
        mCountrySelectButton.setOnClickListener(this);

        mAreaCodeDescription = (TextView) dialogView.findViewById(R.id.text2);
        mAreaCodeDescription.setText(description2);

        mInputAreaEditText = (EditText) dialogView.findViewById(R.id.edit);
        mInputAreaEditText.setHint(editText);
        mInputAreaEditText.addTextChangedListener(this);

        if (isEditShow) {
            mAreaCodeDescription.setVisibility(View.VISIBLE);
            mInputAreaEditText.setVisibility(View.VISIBLE);
        } else {
            mAreaCodeDescription.setVisibility(View.GONE);
            mInputAreaEditText.setVisibility(View.GONE);
        }

        mMessageYesText = (TextView) dialogView.findViewById(R.id.text3);
        mMessageYesText.setText(numberMessage);

        mSuggestNumberText = (TextView) dialogView.findViewById(R.id.text4);
        mSuggestNumberText.setText(number);

        return dialogView;
    }

    protected View createEditTextSingleSelectItems(final String description1, final String editText,
                                                   final String description2, final String messageYes,
                                                   final String numberYes, final String messageNo,
                                                   final String numberNo) {

        mOriginSuggestNumber = numberYes;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.international_number_prefix_text_edit_text_single_single, null);

        mDescriptionText = (TextView) dialogView.findViewById(R.id.text1);
        mDescriptionText.setText(description1);

        mInputAreaEditText = (EditText) dialogView.findViewById(R.id.edit);
        mInputAreaEditText.setHint(editText);
        mInputAreaEditText.addTextChangedListener(this);

        mAreaCodeDescription = (TextView) dialogView.findViewById(R.id.text2);
        mAreaCodeDescription.setText(description2);

        mSingleGroup1 = (ViewGroup) dialogView.findViewById(R.id.single1);
        mMessageYesText = (TextView) mSingleGroup1.findViewById(R.id.text1);
        mMessageYesText.setText(messageYes);
        mSuggestNumberText = (TextView) mSingleGroup1.findViewById(R.id.text2);
        mSuggestNumberText.setText(numberYes);
        mRadioButton1 = (RadioButton) mSingleGroup1.findViewById(R.id.select);
        mRadioButton1.setChecked(true);
        mSingleGroup1.setOnClickListener(this);

        mSingleGroup2 = (ViewGroup) dialogView.findViewById(R.id.single2);
        mMessageNoText = (TextView) mSingleGroup2.findViewById(R.id.text1);
        mMessageNoText.setText(messageNo);
        mOriginNumberText = (TextView) mSingleGroup2.findViewById(R.id.text2);
        mOriginNumberText.setText(numberNo);
        mRadioButton2 = (RadioButton) mSingleGroup2.findViewById(R.id.select);
        mSingleGroup2.setOnClickListener(this);

        return dialogView;
    }

    protected View createSingleSelectItems(final String description, final String messageYes,
                                           final String numberYes, final String messageNo,
                                           final String numberNo) {
        mOriginSuggestNumber = numberYes;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.international_number_prefix_text_single_single, null);

        mDescriptionText = (TextView) dialogView.findViewById(R.id.top_text);
        mDescriptionText.setText(description);

        mSingleGroup1 = (ViewGroup) dialogView.findViewById(R.id.single1);
        mMessageYesText = (TextView) mSingleGroup1.findViewById(R.id.text1);
        mMessageYesText.setText(messageYes);
        mSuggestNumberText = (TextView) mSingleGroup1.findViewById(R.id.text2);
        mSuggestNumberText.setText(numberYes);
        mRadioButton1 = (RadioButton) mSingleGroup1.findViewById(R.id.select);
        mRadioButton1.setChecked(true);
        mSingleGroup1.setOnClickListener(this);

        mSingleGroup2 = (ViewGroup) dialogView.findViewById(R.id.single2);
        mMessageNoText = (TextView) mSingleGroup2.findViewById(R.id.text1);
        mMessageNoText.setText(messageNo);
        mOriginNumberText = (TextView) mSingleGroup2.findViewById(R.id.text2);
        mOriginNumberText.setText(numberNo);
        mRadioButton2 = (RadioButton) mSingleGroup2.findViewById(R.id.select);
        mSingleGroup2.setOnClickListener(this);

        return dialogView;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
