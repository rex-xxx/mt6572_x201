/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.calloption;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.phone.Constants;
import com.mediatek.calloption.InternationalDialogHandler.PrefixInfo;
import com.mediatek.phone.GeminiConstants;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.phone.provider.CallHistory.Calls;
import com.mediatek.phone.provider.CallHistoryAsync;
import com.mediatek.telephony.PhoneNumberUtilsEx;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class InternationalCallOptionHandler extends CallOptionBaseHandler
                                                     implements DialogInterface.OnClickListener,
                                                                DialogInterface.OnDismissListener,
                                                                DialogInterface.OnCancelListener {

    private static final String TAG = "InternationalCallOptionHandler";

    private static final String INTERNATIONAL_DIALING_PREFERENCE_KEY = "international_dialing_key";

    private static final String COUNTRY_ISO_TAIWAN = "TW";
    private static final String COUNTRY_ISO_USA = "US";
    private static final String COUNTRY_ISO_CHINA = "CN";

    private Request mRequest;
    protected InternationalDialogHandler mDialogHandler;

    @Override
    public void handleRequest(final Request request) {
        log("handleRequest()");

        mRequest = request;

        String number = CallOptionUtils.getInitialNumber(request.getApplicationContext(), request.getIntent());

        // Check the number begin with "+"
        // Check smart dialing feature switch off or not
        int internationalDialingSetting = Settings.System.getInt(request.getApplicationContext().getContentResolver(),
                                                                 Settings.System.INTER_DIAL_SETTING, 0);
        log("internationalDialingSetting = " + internationalDialingSetting);
        int internationalDialOption = request.getIntent().getIntExtra(Constants.EXTRA_INTERNATIONAL_DIAL_OPTION,
                                                                      Constants.INTERNATIONAL_DIAL_OPTION_NORMAL);
        log("internationalDialOption = " + internationalDialOption);
        if (TextUtils.isEmpty(number) || number.startsWith("+") || number.endsWith("#")
                || request.getIntent().getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false)
                || Constants.INTERNATIONAL_DIAL_OPTION_IGNORE == internationalDialOption
                || (1 != internationalDialingSetting
                        && Constants.INTERNATIONAL_DIAL_OPTION_WITH_COUNTRY_CODE != internationalDialOption)
                || 0 == SIMInfoWrapper.getDefault().getInsertedSimCount()
                || PhoneNumberUtils.isEmergencyNumber(number)) {
            log("number beging with + or international dialing setting is off or some other conditions, dial directly");
            if (null != mSuccessor) {
                mSuccessor.handleRequest(request);
            }
            return;
        }
        boolean isOffhook = false;
        try {
            isOffhook = request.getTelephonyInterface().isOffhook();
        } catch (RemoteException e) {
            log("Remote exception happen when call ITelephony::isOffhook()");
        }
        if (isOffhook) {
            log("is offhook");
            if (PhoneNumberUtilsEx.ID_VALID_WHEN_CALL_EXIST == PhoneNumberUtilsEx.isValidNumber(
                    CallOptionUtils.getCurrentCountryISO(request.getActivityContext()), number)) {
                log("current phone state is offhook and number is valid when call exists, so call directly");
                if (null != mSuccessor) {
                    mSuccessor.handleRequest(request);
                }
                return;
            }
        }
        // below is for inner class to use, must be final
        String currentCountryISO = CallOptionUtils.getCurrentCountryISO(request.getApplicationContext());
        final String number2 = number;
        log("CallHistory getCallInfo start, number2 = " + number2);
        Calls.CallInfo callInfo = Calls.getCallInfo(request.getApplicationContext(), number2);
        if (Constants.INTERNATIONAL_DIAL_OPTION_WITH_COUNTRY_CODE != internationalDialOption && null != callInfo) {
            log("CallHistory getCallInfo end, get same call history!");
            log("number = " + number + ", number2 = " + number2 + ", country iso = " + callInfo.mCountryISO + "," +
                    " area code = " + callInfo.mAreaCode + ", confirm = " + callInfo.mConfirm);
            final String countryISORecorded = callInfo.mCountryISO;
            final String areaCodeRecorded = callInfo.mAreaCode;
            final long confirm = callInfo.mConfirm;

            // Check local country ISO with recorded country ISO
            if (countryISORecorded.equals(currentCountryISO)) {
                // local country ISO is same as recorded country ISO, dial directly
                log("find same call history, and call history's country iso is same as current one, dial directly");
                if (null != mSuccessor) {
                    mSuccessor.handleRequest(request);
                }
                return;
            } else {
                log("The current country ISO is different with recorded one");
                // Check the number whether with international prefix with recorded country ISO,
                // for example "00" for China mainland
                String internationlPrefixRecorded = PhoneNumberUtilsEx.getInternationalPrefix(countryISORecorded);
                Pattern pattern = Pattern.compile(internationlPrefixRecorded);
                Matcher matcher = pattern.matcher(number2);
                if (!matcher.matches() && matcher.lookingAt()) {
                    // check whether the international prefix based on current country ISO
                    // is same as that based on recorded
                    log("The dialed number starts with recorded country ISO's international prefix");
                    String internationlPrefixCurrent = PhoneNumberUtilsEx.getInternationalPrefix(currentCountryISO);
                    if (!internationlPrefixCurrent.equals(internationlPrefixRecorded)) {
                        // different
                        log("Current country ISO's international prefix is different with that of recorded one");
                        String formatNumber = matcher.replaceFirst("+");
                        // no need pop up dialog, currently for bluetooth case
                        if (1 == confirm && !request.getIntent().getBooleanExtra(Constants.EXTRA_IS_FORBIDE_DIALOG, false)) {
                            log("confirm is 1, pop up internation prefix confirm dialog"); // !!! 2
                            // pop up international prefix confirm dialog
                            // below need use current country ISO because the message in alert dialog is for "from"
                            Locale locale = new Locale(Locale.getDefault().getLanguage(), currentCountryISO);
                            PrefixInfo prefixInfo = new PrefixInfo(null, locale.getDisplayCountry(Locale.getDefault()),
                                                                   countryISORecorded, currentCountryISO, null, null,
                                                                   false, number, formatNumber, null, null, null);
                            showDialog(request.getActivityContext(),
                                    InternationalDialogHandler.DIALOG_TYPE_INTERNATIONAL_PREFIX_CONFIRM,
                                    internationalDialOption, prefixInfo, this, this, this);
                            new CallHistoryAsync().updateConfirmFlag(
                                    new CallHistoryAsync.UpdateConfirmFlagArgs(request.getApplicationContext(),
                                                                               number, 0L));
                            return;
                        } else {
                            // change international prefix and dial directly
                            request.getIntent().putExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL, formatNumber);
                        }
                    }
                    log("Current country ISO's international prefix is same as that of recorded one, dial directly");
                    if (null != mSuccessor) {
                        mSuccessor.handleRequest(request);
                    }
                    return;
                }
                log("number2 = " + number2 + ", countryISORecorded = " + countryISORecorded);
                // no need pop up dialog, currently for bluetooth case
                if (1 == confirm && !request.getIntent().getBooleanExtra(Constants.EXTRA_IS_FORBIDE_DIALOG, false)) {
                    log("confirm == 1, show confirm dialog");
                    Locale locale = new Locale(Locale.getDefault().getLanguage(), countryISORecorded);
                    String countryCode =
                            String.valueOf(PhoneNumberUtil.getInstance().getCountryCodeForRegion(countryISORecorded));
                    String defaultCountryName = locale.getDisplayCountry(Locale.getDefault());

                    CallOptionUtils.NumberInfo numberInfo
                            = CallOptionUtils.getNumberInfo(number2, countryISORecorded);
                    if (!PhoneNumberUtilsEx.isAreaCodeNeeded(countryISORecorded, number2)) {
                        // The number with recorded country prefix can be dialed directly
                        log("The number with recorded country prefix can be dialed directly," +
                                " pop up country single select dialog"); // !!! 2
                        PrefixInfo prefixInfo = new PrefixInfo(countryCode, defaultCountryName,
                                countryISORecorded, currentCountryISO, numberInfo.mAreaCode, null,
                                false, number2, null, numberInfo.mSubscriber, numberInfo.mAreaCodePrefix, null);
                        showDialog(request.getActivityContext(),
                                InternationalDialogHandler.DIALOG_TYPE_COUNTRY_SINGLE_SELECT,
                                internationalDialOption, prefixInfo, this, this, this);
                        return;
                    }
                    // whether area code is stored or not
                    if (!TextUtils.isEmpty(areaCodeRecorded)) {
                        log("area code recorded is not empty"); // !!! 2
                        // if already store area code in call history, just use it
                        String geoDescriptionOfCity =
                                CallOptionUtils.getCityGeoDescription(areaCodeRecorded + number2,
                                                                      countryISORecorded, locale);
                        PrefixInfo prefixInfo = new PrefixInfo(countryCode, defaultCountryName,
                                countryISORecorded, currentCountryISO, areaCodeRecorded, geoDescriptionOfCity,
                                false, number2, null, number2, numberInfo.mAreaCodePrefix, null);
                        showDialog(request.getActivityContext(),
                                InternationalDialogHandler.DIALOG_TYPE_COUNTRY_AREA_SINGLE_SELECT,
                                internationalDialOption, prefixInfo, this, this, this);
                    } else {
                        log("area code recorded is empty");
                        // if not store area code in call history before,
                        // see whether number already have this information
                        if (null != numberInfo && !TextUtils.isEmpty(numberInfo.mAreaCode)) {
                            log("The number already has area code");
                            // !!! This logic may can be here because this branch is that number needs area code
                            // dialed number already has area code info
                            // can get area code from original number, just use it
                            String geoDescriptionOfCity =
                                CallOptionUtils.getCityGeoDescription(number2,
                                                                      countryISORecorded, locale);
                            log("area name is " + geoDescriptionOfCity);
                            PrefixInfo prefixInfo = new PrefixInfo(countryCode, defaultCountryName,
                                    countryISORecorded, currentCountryISO, numberInfo.mAreaCode, geoDescriptionOfCity,
                                    false, number2, null, numberInfo.mSubscriber, null, null);
                            showDialog(request.getActivityContext(),
                                    InternationalDialogHandler.DIALOG_TYPE_COUNTRY_AREA_SINGLE_SELECT,
                                    internationalDialOption, prefixInfo, this, this, this);
                        } else {
                            log("The number do not include area code");
                            String latestAreaCode = Calls.getLatestAreaCode(mRequest.getApplicationContext(),
                                                                            countryISORecorded);
                            if (!TextUtils.isEmpty(latestAreaCode)) {
                                log("can get latest area code from other recorded number"); // !!! 2
                                String geoDescriptionOfCity =
                                    CallOptionUtils.getCityGeoDescription(latestAreaCode + number,
                                                                          countryISORecorded, locale);
                                PrefixInfo prefixInfo = new PrefixInfo(countryCode, defaultCountryName,
                                        countryISORecorded, currentCountryISO, latestAreaCode, geoDescriptionOfCity,
                                        true, number, null, numberInfo.mSubscriber, numberInfo.mAreaCodePrefix, null);
                                showDialog(request.getActivityContext(),
                                        InternationalDialogHandler.DIALOG_TYPE_DEFAULT_AREA_INPUT_SINGLE_SELECT,
                                        internationalDialOption, prefixInfo, this, this, this);
                            } else {
                                log("can NOT get latest area code from other recorded number"); // !!! 2
                                PrefixInfo prefixInfo = new PrefixInfo(countryCode, defaultCountryName,
                                        countryISORecorded, currentCountryISO, null, null, true, number,
                                        null, numberInfo.mSubscriber, numberInfo.mAreaCodePrefix, null);
                                showDialog(request.getActivityContext(),
                                        InternationalDialogHandler.DIALOG_TYPE_AREA_INPUT_SINGLE_SELECT,
                                        internationalDialOption, prefixInfo, this, this, this);
                            }
                        }
                    }
                    return;
                } else {
                    String finalNumber = PhoneNumberUtils.formatNumberToE164(areaCodeRecorded + number2, countryISORecorded);
                    if (finalNumber == null) {
                        request.getIntent().putExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL, number2);
                    } else {
                        request.getIntent().putExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL, finalNumber);
                    }
                }
            }
        } else {
            // There is no recorded same number call history
            // find whether valid country ISO exist based on the sequence
            log("CallHistory getCallInfo end, do not find same call history!");
            String preferCountryISO = "";
            String deniedCountryISO = "";
            if (Constants.INTERNATIONAL_DIAL_OPTION_WITH_COUNTRY_CODE == internationalDialOption) {
                if (null != callInfo) {
                    preferCountryISO = callInfo.mCountryISO;
                }
                deniedCountryISO = currentCountryISO;
            }
            // !!! need to check below findValidCountryISO() performance for MO performance
            ArrayList<String> validCountryISOList = findValidCountryISO(request.getApplicationContext(), number,
                        preferCountryISO, deniedCountryISO, true); // true means get the first valid one
            String validCountryISO =
                    (validCountryISOList == null || validCountryISOList.isEmpty()) ? "" : validCountryISOList.get(0);

            if (Constants.INTERNATIONAL_DIAL_OPTION_WITH_COUNTRY_CODE != internationalDialOption) {
                if (TextUtils.isEmpty(validCountryISO)) {
                    log("can not find valid country iso, show invalid number dialog"); // !!!
                    showInvalidNumberDialog(request.getActivityContext(), this, this, this);
                    return;
                }

                if (currentCountryISO.equals(validCountryISO)) {
                    log("The number is valid for current country ISO, dial directly");
                    // the number dialed is valid based on current country ISO
                    if (null != mSuccessor) {
                        mSuccessor.handleRequest(request);
                    }
                    return;
                }
                log("The number is NOT valid for current country ISO");
            }

            if (Constants.INTERNATIONAL_DIAL_OPTION_WITH_COUNTRY_CODE == internationalDialOption
                    && TextUtils.isEmpty(validCountryISO)) {
                log("can not find valid country iso, set default country ISO");
                String defaultValidCountryISO =
                        findValidCountryISOFromDefaultCountry(request.getApplicationContext(), number);
                if (TextUtils.isEmpty(defaultValidCountryISO)) {
                    log("default valid country ISO is null");
                } else {
                    validCountryISOList.add(defaultValidCountryISO);
                    validCountryISO = defaultValidCountryISO;
                }
            } else {
                validCountryISOList = findValidCountryISO(request.getApplicationContext(), number,
                        preferCountryISO, deniedCountryISO, false); // false means get all valid ones
            }

            // the number dialed is invalid based on current country ISO
            CallOptionUtils.NumberInfo numberInfo
                    = CallOptionUtils.getNumberInfo(number, validCountryISO);
            log("valid country ISO = " + validCountryISO);
            log("number info = " + numberInfo);
            // pop up select dialog for user to confirm national prefix
            Locale locale = new Locale(Locale.getDefault().getLanguage(), validCountryISO);
            String countryCode =
                    String.valueOf(PhoneNumberUtil.getInstance().getCountryCodeForRegion(validCountryISO));
            String countryName = locale.getDisplayCountry(Locale.getDefault());
            if (!PhoneNumberUtilsEx.isAreaCodeNeeded(validCountryISO, number)) {
            //if (false) {
                log("the number can be dialed directly, just show dialog for country selection"); // !!! 2
                // the number can be added country code directly according to valid country ISO
                PrefixInfo prefixInfo = new PrefixInfo(countryCode, countryName,
                        validCountryISO, currentCountryISO, numberInfo.mAreaCode, null,
                        false, number, null, numberInfo.mSubscriber, numberInfo.mAreaCodePrefix, validCountryISOList);
                showDialog(request.getActivityContext(),
                        InternationalDialogHandler.DIALOG_TYPE_COUNTRY_SELECT,
                        internationalDialOption, prefixInfo, this, this, this);
            } else {
                String latestAreaCode = Calls.getLatestAreaCode(mRequest.getApplicationContext(), validCountryISO);
                log("latest area code = " + latestAreaCode);
                if (!TextUtils.isEmpty(latestAreaCode)) {
                    log("can get latest area code from records"); // !!! 2
                    String geoDescriptionOfCity =
                        CallOptionUtils.getCityGeoDescription(latestAreaCode + number, validCountryISO, locale);
                    PrefixInfo prefixInfo = new PrefixInfo(countryCode, countryName,
                            validCountryISO, currentCountryISO, latestAreaCode, geoDescriptionOfCity, true,
                            number, null, number, numberInfo.mAreaCodePrefix, validCountryISOList);
                    showDialog(request.getActivityContext(),
                            InternationalDialogHandler.DIALOG_TYPE_COUNTRY_SELECT_DEFAULT_AREA_INPUT,
                            internationalDialOption, prefixInfo, this, this, this);
                } else {
                    log("can NOT get latest area code from records"); // !!! 2
                    PrefixInfo prefixInfo = new PrefixInfo(countryCode, countryName,
                            validCountryISO, currentCountryISO, null, null, true, number, null,
                            number, numberInfo.mAreaCodePrefix, validCountryISOList);
                    showDialog(request.getActivityContext(),
                            InternationalDialogHandler.DIALOG_TYPE_COUNTRY_SELECT_AREA_INPUT,
                            internationalDialOption, prefixInfo, this, this, this);
                }
            }
            return;
        }
        if (null != mSuccessor) {
            mSuccessor.handleRequest(request);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (null == mDialogHandler) {
                // For case of invalid number dialog
                if (null != mSuccessor) {
                    mSuccessor.handleRequest(mRequest);
                }
                return;
            }
            String numberSelect = mDialogHandler.getSelectResult();
            if (null != numberSelect) {
                String number = CallOptionUtils.getInitialNumber(mRequest.getApplicationContext(),
                                                                 mRequest.getIntent());
                if (PhoneNumberUtils.stripSeparators(number).equals(
                        PhoneNumberUtils.stripSeparators(numberSelect))) {
                    // Delete old call history
                    new CallHistoryAsync().deleteCall(new CallHistoryAsync.DeleteCallArgs(mRequest.getApplicationContext(),
                            number));
                } else {
                    mRequest.getIntent().putExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL, numberSelect);
                }
            } else {
                log("no select from alert dialog, something wrong");
            }
            if (null != mSuccessor) {
                mSuccessor.handleRequest(mRequest);
            }
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            mDialog.cancel();
        }
    }

    public void onDismiss(DialogInterface dialog) {
        log("onDismiss()");
    }

    public void onCancel(DialogInterface dialog) {
        log("onCancel()");
        mRequest.getResultHandler().onHandlingFinish();
    }

    protected abstract void showDialog(final Context context, final int dialogType,
                                       final int internationalDialOption, final PrefixInfo prefixInfo,
                                       DialogInterface.OnClickListener clickListener,
                                       DialogInterface.OnDismissListener dismissListener,
                                       DialogInterface.OnCancelListener cancelListener);

    protected abstract void showInvalidNumberDialog(final Context context,
                                           DialogInterface.OnClickListener clickListener,
                                           DialogInterface.OnDismissListener dismissListener,
                                           DialogInterface.OnCancelListener cancelListener);

    private ArrayList<String> findValidCountryISO(Context context, String number, String preferCountryISO,
                                                  String deniedCountryISO, boolean onlyFirstOne) {
        ArrayList<String> validCountryISOList = new ArrayList<String>();
        if (!TextUtils.isEmpty(preferCountryISO)) {
            if (CallOptionUtils.isValidNumberForCountryISO(context, number, preferCountryISO)) {
                validCountryISOList.add(preferCountryISO);
                if (onlyFirstOne) {
                    return validCountryISOList;
                }
            }
        }
        String currentCountryISO = CallOptionUtils.getCurrentCountryISO(context);
        if (TextUtils.isEmpty(deniedCountryISO) || !deniedCountryISO.equals(currentCountryISO)) {
            if (CallOptionUtils.isValidNumberForCountryISO(context, number, currentCountryISO)) {
                if (onlyFirstOne) {
                    validCountryISOList.add(currentCountryISO);
                    return validCountryISOList;
                } else {
                    if (!validCountryISOList.contains(currentCountryISO)) {
                        validCountryISOList.add(currentCountryISO);
                    }
                }
            }
        }
        TelephonyManager telephonyManager = TelephonyManager.getDefault();
        if (mRequest.isMultipleSim()) {
            for (int slot: GeminiConstants.SLOTS) {
                String sim1CountryISO = telephonyManager.getSimCountryIsoGemini(slot).toUpperCase();
                if (!TextUtils.isEmpty(sim1CountryISO)
                        && (TextUtils.isEmpty(deniedCountryISO) || !deniedCountryISO.equals(sim1CountryISO))) {
                    if (CallOptionUtils.isValidNumberForCountryISO(context, number, sim1CountryISO)) {
                        if (onlyFirstOne) {
                            validCountryISOList.add(sim1CountryISO);
                            return validCountryISOList;
                        } else {
                            if (!validCountryISOList.contains(sim1CountryISO)) {
                                validCountryISOList.add(sim1CountryISO);
                            }
                        }
                    }
                }
            }
        } else {
            String simCountryISO = telephonyManager.getSimCountryIso().toUpperCase();
            if (!TextUtils.isEmpty(simCountryISO)
                    && (TextUtils.isEmpty(deniedCountryISO) || !deniedCountryISO.equals(simCountryISO))) {
                if (CallOptionUtils.isValidNumberForCountryISO(context, number, simCountryISO)) {
                    if (onlyFirstOne) {
                        validCountryISOList.add(simCountryISO);
                        return validCountryISOList;
                    } else {
                        if (!validCountryISOList.contains(simCountryISO)) {
                            validCountryISOList.add(simCountryISO);
                        }
                    }
                }
            }
        }
        addValidCountryISOFromCallHistory(context, number, validCountryISOList, onlyFirstOne, deniedCountryISO);
        return validCountryISOList;
    }

    private void addValidCountryISOFromCallHistory(Context context, String number,
                                                   ArrayList<String> validCountryISOList,
                                                   boolean onlyFirstOne, String deniedCountryISO) {
        Cursor cursor = Calls.getAllCountryISO(context);
        String countryISO = null;
        if (null != cursor) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                countryISO = cursor.getString(0);
                if (TextUtils.isEmpty(deniedCountryISO) || !deniedCountryISO.equals(countryISO)) {
                    if (CallOptionUtils.isValidNumberForCountryISO(context, number, countryISO)) {
                        if (onlyFirstOne) {
                            validCountryISOList.add(countryISO);
                            cursor.close();
                            return;
                        } else {
                            if (!validCountryISOList.contains(countryISO)) {
                                validCountryISOList.add(countryISO);
                            }
                        }
                    }
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
    }

    private String findValidCountryISOFromDefaultCountry(Context context, String number) {
        String validCountryISO = "";
        if (!CallOptionUtils.getCurrentCountryISO(context).equals(COUNTRY_ISO_CHINA) && 
                CallOptionUtils.isValidNumberForCountryISO(context, number, COUNTRY_ISO_CHINA)) {
            validCountryISO = COUNTRY_ISO_CHINA;
        } else {
            if (!CallOptionUtils.getCurrentCountryISO(context).equals(COUNTRY_ISO_TAIWAN) &&
                    CallOptionUtils.isValidNumberForCountryISO(context, number, COUNTRY_ISO_TAIWAN)) {
                validCountryISO = COUNTRY_ISO_TAIWAN;
            } else {
                if (!CallOptionUtils.getCurrentCountryISO(context).equals(COUNTRY_ISO_USA) && 
                        CallOptionUtils.isValidNumberForCountryISO(context, number, COUNTRY_ISO_USA)) {
                    validCountryISO = COUNTRY_ISO_USA;
                }
            }
        }
        return validCountryISO;
    }

    public void dismissDialog() {
        log("dismissDialog()");
        if (null != mDialogHandler) {
            mDialogHandler.onHandledDialogDismiss();
        }
        super.dismissDialog();
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
