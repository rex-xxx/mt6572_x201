package com.mediatek.calloption;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.mediatek.phone.provider.CallHistory.Calls;
import com.mediatek.telephony.PhoneNumberUtilsEx;

import java.util.ArrayList;

public abstract class InternationalDialogHandler implements View.OnClickListener, TextWatcher,
                                                   DialogInterface.OnShowListener,
                                                   InternationalCountrySelectDialogHandler.OnCountrySelectListener {

    private static final String TAG = "InternationalDialogHandler";

    public static final int DIALOG_TYPE_INTERNATIONAL_PREFIX_CONFIRM      = 0;
    public static final int DIALOG_TYPE_COUNTRY_AREA_SINGLE_SELECT        = 1;
    public static final int DIALOG_TYPE_COUNTRY_SINGLE_SELECT             = 2;
    public static final int DIALOG_TYPE_COUNTRY_SELECT                    = 3;
    public static final int DIALOG_TYPE_COUNTRY_SELECT_AREA_INPUT         = 4;
    public static final int DIALOG_TYPE_COUNTRY_SELECT_DEFAULT_AREA_INPUT = 5;
    public static final int DIALOG_TYPE_AREA_INPUT_SINGLE_SELECT          = 6;
    public static final int DIALOG_TYPE_DEFAULT_AREA_INPUT_SINGLE_SELECT  = 7;

    public static class PrefixInfo {
        public String mCountryCode;
        public String mCountryName;
        public String mSuggestCountryISO;
        public String mCurrentCountryISO;
        public String mAreaCode;
        public String mAreaName;
        public boolean mIsAreaCodeNeeded;
        public String mNumberOrigin;
        public String mNumberReplaceInterPrefix;
        public String mNumberSubscriber;
        public String mPossibleAreaPrefix;
        public ArrayList<String> mValidCountryISOList;

        public PrefixInfo(String countryCode, String countryName,
                          String suggestCountryISO, String currentCountryISO,
                          String areaCode, String areaName,
                          boolean isAreaCodeNeeded, String numberOrigin,
                          String numberReplaceInterPrefix,
                          String numberSubscriber, String possibleAreaPrefix,
                          ArrayList<String> validCountryISOList) {
            mCountryCode = countryCode;
            mCountryName = countryName;
            mSuggestCountryISO = suggestCountryISO;
            mCurrentCountryISO = currentCountryISO;
            mAreaCode = areaCode;
            mAreaName = areaName;
            mIsAreaCodeNeeded = isAreaCodeNeeded;
            mNumberOrigin = numberOrigin;
            mNumberReplaceInterPrefix = numberReplaceInterPrefix;
            mNumberSubscriber = numberSubscriber;
            mPossibleAreaPrefix = possibleAreaPrefix;
            mValidCountryISOList = validCountryISOList;
        }

        public String toString() {
            return "mCountryCode = " + mCountryCode + ", mCountryName = " + mCountryName +
                   ", mSuggestCountryISO = " + mSuggestCountryISO + ", mCurrentCountryISO = " + mCurrentCountryISO +
                   ", mAreaCode = " + mAreaCode + ", mAreaName = " + mAreaName +
                   ", mIsAreaCodeNeeded = " + mIsAreaCodeNeeded + ", mNumberOrigin = " + mNumberOrigin +
                   ", mNumberReplaceInterPrefix = " + mNumberReplaceInterPrefix +
                   ", mNumberSubscriber = " + mNumberSubscriber +
                   ", mPossibleAreaPrefix = " + mPossibleAreaPrefix;
        }
    }

    protected Context mContext;
    protected int mType;
    protected int mInternationalDialOption;
    protected PrefixInfo mPrefixInfo;

    protected TextView mDescriptionText;
    protected TextView mMessageYesText;
    protected TextView mMessageNoText;
    protected TextView mSuggestNumberText;
    protected TextView mOriginNumberText;
    protected TextView mAreaCodeDescription;
    protected EditText mInputAreaEditText;
    protected Button   mCountrySelectButton;
    protected RadioButton mRadioButton1;
    protected RadioButton mRadioButton2;
    protected ViewGroup mSingleGroup1;
    protected ViewGroup mSingleGroup2;
    protected AlertDialog mAlertDialog;

    protected String mOriginSuggestNumber;
    // "(area code)"
    protected String mTextAreaCode;
    // "Input area code here"
    protected String mTextInputCodeHere;

    protected InternationalCountrySelectDialogHandler mCountrySelectDialogHandler;

    public InternationalDialogHandler(Context context, int type, int internationalDialOption,
                                      PrefixInfo prefixInfo) {
        mContext = context;
        mType = type;
        mInternationalDialOption = internationalDialOption;
        mPrefixInfo = prefixInfo;
    }

    public View createDialogView() {
        switch (mType) {
            case DIALOG_TYPE_INTERNATIONAL_PREFIX_CONFIRM:
                return createPrefixConfirmDialogItems();
            case DIALOG_TYPE_COUNTRY_AREA_SINGLE_SELECT:
                return createCountryAreaSingleSelectDialogItems();
            case DIALOG_TYPE_COUNTRY_SINGLE_SELECT:
                return createCountrySingleSelectDialogItems();
            case DIALOG_TYPE_COUNTRY_SELECT:
                return createCountrySelectDialogItems(mInternationalDialOption);
            case DIALOG_TYPE_COUNTRY_SELECT_AREA_INPUT:
                return createCountrySelectAreaInputDialogItems(mInternationalDialOption);
            case DIALOG_TYPE_COUNTRY_SELECT_DEFAULT_AREA_INPUT:
                return createCountrySelectDefaultAreaInputDialogItems(mInternationalDialOption);
            case DIALOG_TYPE_AREA_INPUT_SINGLE_SELECT:
                return createAreaInputSingleSelectDialogItems();
            case DIALOG_TYPE_DEFAULT_AREA_INPUT_SINGLE_SELECT:
                return createDefaultAreaInputSingleSelectDialogItems();
            default:
                break;
        }
        return null;
    }

    protected abstract View createPrefixConfirmDialogItems();

    protected abstract View createCountryAreaSingleSelectDialogItems();

    protected abstract View createCountrySingleSelectDialogItems();

    protected abstract View createCountrySelectDialogItems(int internationalDialOption);

    protected abstract View createCountrySelectAreaInputDialogItems(int internationalDialOption);

    protected abstract View createCountrySelectDefaultAreaInputDialogItems(int internationalDialOption);

    protected abstract View createAreaInputSingleSelectDialogItems();

    protected abstract View createDefaultAreaInputSingleSelectDialogItems();

    protected abstract View createButtonEditTextItems(final String description1, final String buttonText,
                                                      final String description2, final String editText,
                                                      final boolean isEditShow, final String numberMessage,
                                                      final String number, final String textAreaCode,
                                                      final String textInputCodeHere);

    protected abstract View createEditTextSingleSelectItems(final String description1,
            final String editText, final String description2,
            final String messageYes, final String numberYes,
            final String messageNo, final String numberNo);

    protected abstract View createSingleSelectItems(final String description,
            final String messageYes, final String numberYes,
            final String messageNo, final String numberNo);

    @Override
    public void onClick(View view) {
        // show country code select dialog
        if (view == mSingleGroup1 || view == mSingleGroup2) {
            if (view == mSingleGroup1) {
                mRadioButton1.setChecked(true);
                mRadioButton2.setChecked(false);
            }
            if (view == mSingleGroup2) {
                mRadioButton2.setChecked(true);
                mRadioButton1.setChecked(false);
            }
            Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (null != button) {
                if (DIALOG_TYPE_AREA_INPUT_SINGLE_SELECT == mType
                        && mRadioButton1.isChecked() && TextUtils.isEmpty(mInputAreaEditText.getText())) {
                    log("onClick(), disable button");
                    button.setEnabled(false);
                } else {
                    log("onClick(), enable button");
                    button.setEnabled(true);
                }
            }
        } else if (view == mCountrySelectButton) {
            // pop up country selection list dialog
            mCountrySelectDialogHandler.showCountrySelectDialog(mPrefixInfo.mValidCountryISOList);
        }
    }

    @Override
    public void onCountrySelected(String countryISO, String countryCode, String countryName) {
        log("onCountrySelected(), countryISO = " + countryISO + ", countryCode = "
                + countryCode + ", countryName = " + countryName);
        mPrefixInfo.mSuggestCountryISO = countryISO;
        mPrefixInfo.mCountryCode = countryCode;
        mPrefixInfo.mCountryName = countryName;
        CallOptionUtils.NumberInfo numberInfo
                = CallOptionUtils.getNumberInfo(mPrefixInfo.mNumberOrigin, mPrefixInfo.mSuggestCountryISO);
        mPrefixInfo.mAreaCode = numberInfo.mAreaCode;
        mPrefixInfo.mNumberSubscriber = numberInfo.mSubscriber;
        mPrefixInfo.mPossibleAreaPrefix = numberInfo.mAreaCodePrefix;
        if (TextUtils.isEmpty(mPrefixInfo.mAreaCode)) {
            if (CallOptionUtils.isValidNumberForCountryISO(mContext, mPrefixInfo.mNumberOrigin,
                    mPrefixInfo.mSuggestCountryISO)) {
                mPrefixInfo.mIsAreaCodeNeeded
                        = PhoneNumberUtilsEx.isAreaCodeNeeded(mPrefixInfo.mSuggestCountryISO, mPrefixInfo.mNumberOrigin);
            }
        } else {
            mPrefixInfo.mIsAreaCodeNeeded = false;
        }
        log("mPrefixInfo = " + mPrefixInfo);

        if (null != mCountrySelectButton) {
            mCountrySelectButton.setText(mPrefixInfo.mCountryName + "(+" + mPrefixInfo.mCountryCode + ")");
        }
        if (mPrefixInfo.mIsAreaCodeNeeded) {
            mAreaCodeDescription.setVisibility(View.VISIBLE);
            mInputAreaEditText.setVisibility(View.VISIBLE);
            String latestAreaCode = Calls.getLatestAreaCode(mContext.getApplicationContext(),
                                                            mPrefixInfo.mSuggestCountryISO);
            if (!TextUtils.isEmpty(latestAreaCode)) {
                mInputAreaEditText.setText("");
                mInputAreaEditText.setHint(latestAreaCode);
                mOriginSuggestNumber = PhoneNumberUtils.formatNumber(
                            "+" + mPrefixInfo.mCountryCode + latestAreaCode + " " + mPrefixInfo.mNumberOrigin,
                            mPrefixInfo.mSuggestCountryISO);
                if (TextUtils.isEmpty(mOriginSuggestNumber)) {
                    mOriginSuggestNumber = "+" + mPrefixInfo.mCountryCode + latestAreaCode + mPrefixInfo.mNumberOrigin;
                }
                mSuggestNumberText.setText(mOriginSuggestNumber);
                mType = DIALOG_TYPE_COUNTRY_SELECT_AREA_INPUT;
                Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (null != button) {
                    log("onCountrySelected(), use latest area code, enable button");
                    button.setEnabled(true);
                }
            } else {
                mInputAreaEditText.setText("");
                mInputAreaEditText.setHint(mTextInputCodeHere);
                mOriginSuggestNumber = "+" + mPrefixInfo.mCountryCode + " (" + mTextAreaCode + ")"
                                       + mPrefixInfo.mNumberOrigin;
                mSuggestNumberText.setText(mOriginSuggestNumber);
                mType = DIALOG_TYPE_COUNTRY_SELECT_DEFAULT_AREA_INPUT;
                Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (null != button) {
                    log("onCountrySelected(), no area code, disable button");
                    button.setEnabled(false);
                }
            }
        } else {
            String suggestNumber = PhoneNumberUtils.formatNumber(
                    "+" + mPrefixInfo.mCountryCode + mPrefixInfo.mAreaCode + mPrefixInfo.mNumberSubscriber,
                    mPrefixInfo.mSuggestCountryISO);
            if (TextUtils.isEmpty(suggestNumber)) {
                suggestNumber = "+" + mPrefixInfo.mCountryCode + mPrefixInfo.mAreaCode + mPrefixInfo.mNumberSubscriber;
            }
            mSuggestNumberText.setText(suggestNumber);
            mAreaCodeDescription.setVisibility(View.GONE);
            mInputAreaEditText.setVisibility(View.GONE);
            mType = DIALOG_TYPE_COUNTRY_SELECT;
            Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (null != button) {
                log("onCountrySelected(), no need area code, enable button");
                button.setEnabled(true);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable input) {
        if (TextUtils.isEmpty(input)) {
            log("afterTextChanged(), input is null");
            mSuggestNumberText.setText(mOriginSuggestNumber);
            if (DIALOG_TYPE_COUNTRY_SELECT_AREA_INPUT == mType ||
                    DIALOG_TYPE_COUNTRY_SELECT_DEFAULT_AREA_INPUT == mType ||
                    (DIALOG_TYPE_AREA_INPUT_SINGLE_SELECT == mType && mRadioButton1.isChecked())) {
                Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (null != button) {
                    log("afterTextChanged(), disable button");
                    button.setEnabled(false);
                }
            }
        } else {
            log("afterTextChanged(), input is NOT null");
            String inputAreaCode = input.toString();
            log("mPrefixInfo.mPossibleAreaPrefix = " + mPrefixInfo.mPossibleAreaPrefix);
            log("inputAreaCode = " + inputAreaCode);
            if (!TextUtils.isEmpty(mPrefixInfo.mPossibleAreaPrefix) && 
                    inputAreaCode.startsWith(mPrefixInfo.mPossibleAreaPrefix)) {
                inputAreaCode = inputAreaCode.replaceFirst(mPrefixInfo.mPossibleAreaPrefix, "");
            }
            String suggestNumber = PhoneNumberUtils.formatNumber("+" + mPrefixInfo.mCountryCode +
                                   inputAreaCode + mPrefixInfo.mNumberSubscriber, mPrefixInfo.mSuggestCountryISO);
            if (TextUtils.isEmpty(suggestNumber)) {
                suggestNumber = "+" + mPrefixInfo.mCountryCode + inputAreaCode + mPrefixInfo.mNumberSubscriber;
            }
            mSuggestNumberText.setText(suggestNumber);
            if (DIALOG_TYPE_COUNTRY_SELECT_AREA_INPUT == mType ||
                    DIALOG_TYPE_COUNTRY_SELECT_DEFAULT_AREA_INPUT == mType ||
                    DIALOG_TYPE_AREA_INPUT_SINGLE_SELECT == mType) {
                Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (null != button) {
                    log("afterTextChanged(), enable button");
                    button.setEnabled(true);
                }
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
    }

    public String getSelectResult() {
        CharSequence result = "";

        switch (mType) {

            case DIALOG_TYPE_INTERNATIONAL_PREFIX_CONFIRM:
            case DIALOG_TYPE_COUNTRY_AREA_SINGLE_SELECT:
            case DIALOG_TYPE_COUNTRY_SINGLE_SELECT:
            case DIALOG_TYPE_AREA_INPUT_SINGLE_SELECT:
            case DIALOG_TYPE_DEFAULT_AREA_INPUT_SINGLE_SELECT:
                if (mRadioButton1.isChecked()) {
                    result = mSuggestNumberText.getText();
                } else if (mRadioButton2.isChecked()) {
                    result = mOriginNumberText.getText();
                }
                break;

            case DIALOG_TYPE_COUNTRY_SELECT:
            case DIALOG_TYPE_COUNTRY_SELECT_AREA_INPUT:
            case DIALOG_TYPE_COUNTRY_SELECT_DEFAULT_AREA_INPUT:
                result = mSuggestNumberText.getText();
                break;

            default:
                break;
        }
        return result.toString();
    }

    public String getCountryCode() {
        return mPrefixInfo.mCountryCode;
    }

    public void setAlertDialog(AlertDialog alertDialog) {
        mAlertDialog = alertDialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        Button button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (null == button) {
            return;
        }
        if (DIALOG_TYPE_COUNTRY_SELECT_AREA_INPUT == mType ||
                (DIALOG_TYPE_AREA_INPUT_SINGLE_SELECT == mType && mRadioButton1.isChecked())) {
            log("onShow(), disable button");
            button.setEnabled(false);
        } else {
            log("onShow(), enable button");
            button.setEnabled(true);
        }
    }

    public void onHandledDialogDismiss() {
        if (null != mCountrySelectDialogHandler) {
            mCountrySelectDialogHandler.dismissHandledDialog();
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
