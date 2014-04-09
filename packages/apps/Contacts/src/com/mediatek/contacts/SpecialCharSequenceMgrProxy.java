package com.mediatek.contacts;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.SpecialCharSequenceMgr;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.util.ContactsSettingsUtils;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.contacts.widget.SimPickerDialog;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.telephony.TelephonyManagerEx;

public class SpecialCharSequenceMgrProxy {

    private static final String TAG = "SpecialCharSequenceMgrProxy";

    private static final String MMI_IMEI_DISPLAY = "*#06#";

    private static final String ADN_PHONE_NUMBER_COLUMN_NAME = "number";
    private static final String ADN_NAME_COLUMN_NAME = "name";
    private static final String ADN_INDEX_COLUMN_NAME = "index";

    /**
     * M: [Gemini+] once a slot is ready, it would be put in this list and waiting for information query
     * after this list is empty, it means all slot information has been retrieved.
     */
    private static LinkedList<Integer> sStateReadySlotList = new LinkedList<Integer>();

    private static SimContactQueryCookie sCookie = null;

    private static boolean sStopProgress = false;


    private SpecialCharSequenceMgrProxy() {
    }

    public static boolean handleChars(Context context, String input,
            EditText textField) {
        if (SlotUtils.isGeminiEnabled()) {
            return handleChars(context, input, false, textField);
        } else {
            return SpecialCharSequenceMgr.handleChars(context, input, false,
                    textField);
        }
    }

    static boolean handleChars(Context context, String input) {
        if (SlotUtils.isGeminiEnabled()) {
            return handleChars(context, input, false, null);
        } else {
            return SpecialCharSequenceMgr.handleChars(context, input, false,
                    null);
        }
    }

    static boolean handleChars(Context context, String input, boolean useSystemWindow,
            EditText textField) {
        Log.d(TAG, "handleChars() dialString:" + input);
        if (SlotUtils.isGeminiEnabled()) {
            String dialString = PhoneNumberUtils.stripSeparators(input);
            if (handleIMEIDisplay(context, dialString, useSystemWindow)
                    || handlePinEntry(context, dialString)
                    || handleAdnEntry(context, dialString, textField)
                    || handleSecretCode(context, dialString)) {
                return true;
            }
            return false;
        } else {
            return SpecialCharSequenceMgr.handleChars(context, input, useSystemWindow, textField);
        }
    }

    static boolean handleIMEIDisplay(Context context, String input, boolean useSystemWindow) {
        if (SlotUtils.isGeminiEnabled()) {
           if (input.equals(MMI_IMEI_DISPLAY)) {
                showIMEIPanel(context, useSystemWindow);
                return true;
            }
            return false;
        } else {
            return SpecialCharSequenceMgr.handleIMEIDisplay(context, input,
                    useSystemWindow);
        }
    }

    /**
     * M: [Gemini+] handle pin entry of the input if it's gemini phone
     * @param context
     * @param input
     * @return
     */
    static boolean handlePinEntry(Context context, String input) {
        if (SlotUtils.isGeminiEnabled()) {
            if ((input.startsWith("**04") || input.startsWith("**05")) && input.endsWith("#")) {
                final String innerInput = input;
                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //convert the click item to slot id
                        final AlertDialog alert = (AlertDialog) dialog;
                        final ListAdapter listAdapter = alert.getListView().getAdapter();
                        final int slot = ((Integer)listAdapter.getItem(which)).intValue();
                        SimCardUtils.handlePinMmi(innerInput, slot);

                        dialog.dismiss();
                    }
                };

                final long defaultSim = ContactsSettingsUtils.getDefaultSIMForVoiceCall();
                Log.i(TAG, "[handlePinEntry]default sim is " + defaultSim);

                if (defaultSim == ContactsSettingsUtils.DEFAULT_SIM_NOT_SET
                        || defaultSim == ContactsSettingsUtils.VOICE_CALL_SIM_SETTING_INTERNET) {
                    Log.i(TAG, "[handlePinEntry] default sim not set or is internet sim");
                    return false;
                }

                final SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getDefault();
                int simCount = simInfoWrapper.getInsertedSimCount();

                if (defaultSim == ContactsSettingsUtils.DEFAULT_SIM_SETTING_ALWAYS_ASK
                        && simCount > 1) {
                    AlertDialog dialog = SimPickerDialog.create(context,
                            context.getResources().getString(
                                    R.string.call_pin_dialog_title), false,
                            onClickListener);
                    dialog.show();
                    return true;
                } else {
                    // default sim is internet, nothing to do
                    if (defaultSim == ContactsSettingsUtils.VOICE_CALL_SIM_SETTING_INTERNET) {
                        Log.i(TAG, "defaultSim is internet");
                        return false;
                    }

                    // default sim is always ask but sim count < 2
                    if (defaultSim == ContactsSettingsUtils.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
                        if (simInfoWrapper.getInsertedSimInfoList() != null
                                && simInfoWrapper.getInsertedSimCount() > 0) {
                            return SimCardUtils.handlePinMmi(innerInput,
                                    simInfoWrapper.getInsertedSimInfoList().get(0).mSlot);
                        } else {
                            Log.i(TAG, "[handlePinEntry]insert sim info list is null or empty");
                            return false;
                        }
                    }

                    final int slot = simInfoWrapper.getSimSlotById((int)defaultSim);
                    return SimCardUtils.handlePinMmi(innerInput, slot);
                }
            }
            Log.d(TAG, "[handlePinEntry]not handled otherwise");
            return false;
        } else {
            return SpecialCharSequenceMgr.handlePinEntry(context, input);
        }
    }

    static void showIMEIPanel(Context context, boolean useSystemWindow) {
        if (SlotUtils.isGeminiEnabled()) {
            List<String> imeis = new ArrayList<String>();
            String imei_invalid = context.getResources().getString(R.string.imei_invalid);
            for (int slotId : SlotUtils.getAllSlotIds()) {
                String imei = TelephonyManagerEx.getDefault().getDeviceId(slotId);
                imeis.add(TextUtils.isEmpty(imei) ? imei_invalid : imei);
            }

            AlertDialog alert = new AlertDialog.Builder(context).setTitle(R.string.imei)
                    .setItems(imeis.toArray(new String[imeis.size()]), null).setPositiveButton(android.R.string.ok, null)
                    .setCancelable(false).create();
            alert.show();
        } else {
            final TelephonyManager telephonyManager = ((TelephonyManager)context.getSystemService(
                    Context.TELEPHONY_SERVICE));
            SpecialCharSequenceMgr.showIMEIPanel(context, useSystemWindow, telephonyManager);
        }
    }

    static boolean handleAdnEntry(Context context, String input, EditText textField) {
        log("handleAdnEntry, input = " + input);
        if (SlotUtils.isGeminiEnabled()) {
            KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager.inKeyguardRestrictedInputMode()) {
                Log.d(TAG, "[handleAdnEntry]keyguard restricted input mode");
                return false;
            }

            int len = input.length();
            if ((len > 1) && (len < 5) && (input.endsWith("#"))) {
                sStopProgress = false;
                try {
                    // get the ordinal number of the sim contact
                    int index = -1;
                    try {
                        index = Integer.parseInt(input.substring(0, len - 1));
                        if (index <= 0) {
                            Log.d(TAG, "[handleAdnEntry]index <= 0 for input" + input);
                            return false;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "[handleAdnEntry]parse int failed for input: " + input);
                        return false;
                    }
                    

                    // The original code that navigated to a SIM Contacts list view did not
                    // highlight the requested contact correctly, a requirement for PTCRB
                    // certification.  This behaviour is consistent with the UI paradigm
                    // for touch-enabled lists, so it does not make sense to try to work
                    // around it.  Instead we fill in the the requested phone number into
                    // the dialer text field.

                    // create the async query handler
                    QueryHandler handler = new QueryHandler(context.getContentResolver());

                    // create the cookie object
                    // index in SIM
                    SimContactQueryCookie sc = new SimContactQueryCookie(index, handler, 0);
                    sCookie = sc;
                    // setup the cookie fields
                    sc.contactNum = index;
                    sc.setTextField(textField);
                    if (null != textField) {
                        sc.text = textField.getText().toString();
                    } else {
                        sc.text = null;
                    }
                    log("index = " + index);

                    // create the progress dialog
                    if (null == sc.progressDialog) {
                        sc.progressDialog = new ProgressDialog(context);
                    }
                    sc.progressDialog.setTitle(R.string.simContacts_title);
                    sc.progressDialog.setMessage(context.getText(R.string.simContacts_emptyLoading));
                    sc.progressDialog.setIndeterminate(true);
                    sc.progressDialog.setCancelable(true);
                    sc.progressDialog.setOnCancelListener(sc);
                    sc.progressDialog.setOnShowListener(sc);
                    sc.progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                    sc.context = context;

                    final long defaultSim = Settings.System.getLong(context
                            .getContentResolver(),
                            Settings.System.VOICE_CALL_SIM_SETTING, -3);
                    Log.i(TAG, "[handleAdnEntry]defaultSim is " + defaultSim);

                    if (defaultSim == ContactsSettingsUtils.VOICE_CALL_SIM_SETTING_INTERNET
                            || defaultSim == ContactsSettingsUtils.DEFAULT_SIM_NOT_SET) {
                        Log.d(TAG, "[handleAdnEntry]defaultSim is internet or not set default sim");
                        return false;
                    }

                    final SIMInfoWrapper simInfoWrapper = SIMInfoWrapper.getDefault();

                    int slot = SlotUtils.getNonSlotId();
                    if (defaultSim == ContactsSettingsUtils.DEFAULT_SIM_SETTING_ALWAYS_ASK) {
                        SparseBooleanArray isRadioOn = new SparseBooleanArray();
                        sStateReadySlotList.clear();
                        boolean isAnyRadioOn = false;
                        for (int slotId : SlotUtils.getAllSlotIds()) {
                            if (SimCardUtils.isSetRadioOn(context.getContentResolver(), slotId)) {
                                isAnyRadioOn = true;
                            }
                            if (SimCardUtils.isSimStateReady(slotId)) {
                                sStateReadySlotList.add(slotId);
                            }
                        }
                        ///M: no slot radio is on
                        if (!isAnyRadioOn) {
                            log("radio power off, bail out");
                            return false;
                        }

                        ///M: no slot sim state is ready
                        if (sStateReadySlotList.isEmpty()) {
                            log("sim not ready, bail out");
                            return false;
                        }

                        sc.mIsSingleQuery = (sStateReadySlotList.size() == 1);
                        slot = sStateReadySlotList.poll();

                        log("start query slot is: " + slot + ", isSingleQuery = " + sc.mIsSingleQuery);
                    } else {
                        slot = simInfoWrapper.getSimSlotById((int)defaultSim);
                        sc.mIsSingleQuery = true; //in this case, only do a single query, set true;

                        if (!SimCardUtils.isSetRadioOn(context.getContentResolver(), (int) slot)
                                || !SimCardUtils.isSimStateReady((int) slot)) {
                            log("radio power off or sim not ready, bail out");
                            return false;
                        }
                    }

                    Uri uri = SimCardUtils.SimUri.getSimUri(slot);
                    log("slot = " + slot + " uri = " + uri);
                    if (null != sc.progressDialog && !sc.progressDialog.isShowing()) {
                        Log.d(TAG, "handleAdnEntry() sc.progressDialog.show()");
                        sc.progressDialog.show();
                    }
                    handler.startQuery(slot, sc, uri, new String[] { ADN_PHONE_NUMBER_COLUMN_NAME,ADN_INDEX_COLUMN_NAME },
                            null, null, null);
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage());
                }
                return true;
            }
            Log.d(TAG, "[handleAdnEntry] not handled");
            return false;
        } else {
            return SpecialCharSequenceMgr.handleAdnEntry(context, input,
                    textField);
        }
    }

    static boolean handleSecretCode(Context context, String input) {
        // Secret codes are in the form *#*#<code>#*#*
        return SpecialCharSequenceMgr.handleSecretCode(context, input);
    }

    private static class SimContactQueryCookie implements
            DialogInterface.OnCancelListener, DialogInterface.OnShowListener {
        public ProgressDialog progressDialog;
        public int contactNum;
        /**
         * M: [Gemini+] single query means only one slot need to be queried
         */
        public boolean mIsSingleQuery;

        // Used to identify the query request.
        private int mToken;
        private QueryHandler mHandler;

        // The text field we're going to update
        private EditText mTextField;
        public String text;

        public Context context;
        ///M: [Gemini+] decoupling slot id with number 0, 1 ... 
        /// key is slotId, value is the information @{
        public SparseArray<String> mSimNumberForSlot = new SparseArray<String>();
        public SparseArray<String> mSimNameForSlot = new SparseArray<String>();
        public SparseBooleanArray mFoundForSlot = new SparseBooleanArray();
        ///@}

        public SimContactQueryCookie(int number, QueryHandler handler, int token) {
            contactNum = number;
            mHandler = handler;
            mToken = token;
        }

        /**
         * Synchronized getter for the EditText.
         */
        public synchronized EditText getTextField() {
            return mTextField;
        }

        public synchronized QueryHandler getQueryHandler() {
            return mHandler;
        }
        
        /**
         * Synchronized setter for the EditText.
         */
        public synchronized void setTextField(EditText editText) {
            mTextField = editText;
        }

        /**
         * Cancel the ADN query by stopping the operation and signaling
         * the cookie that a cancel request is made.
         */
        public synchronized void onCancel(DialogInterface dialog) {
            // close the progress dialog
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            // setting the textfield to null ensures that the UI does NOT get
            // updated.
            mTextField = null;
            mHandler.shouldDismissSimPicker(true);

            // Cancel the operation if possible.
            mHandler.cancelOperation(mToken);
        }

        @Override
        public synchronized void onShow(DialogInterface dialog) {
            mHandler.shouldDismissSimPicker(false);
        }
    }

    private static class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        /**
         * M: [ALPS00612118]should dismiss the SimPickerDialog? if the progress
         * dialog is canceled, the SimPickerDialog should be canceled too.
         * 
         * @param should
         *            true if should dismiss
         */
        public void shouldDismissSimPicker(boolean should) {
            mShouldDismissSimPickerDialog = should;
            if (should) {
                LogUtils.d(TAG, "[shouldDismissSimPicker]should dismiss");
                if (alert != null && alert.isShowing()) {
                    LogUtils.d(TAG, "[shouldDismissSimPicker]cancel the dialog");
                    alert.cancel();
                }
            }
        }

        protected void showToast(Context context, SimContactQueryCookie sc, String name, String number) {
            final EditText text = sc.mTextField;
            int len = number != null ? number.length() : 0;
            
            if (sc.text.equals(number)) {
                Toast.makeText(context,
                        context.getString(R.string.non_phone_caption) + "\n"
                                + number, Toast.LENGTH_LONG).show();
            } else if ((len > 1) && (len < 5) && (number.endsWith("#"))) {
                Toast.makeText(context,
                        context.getString(R.string.non_phone_caption) + "\n"
                                + number, Toast.LENGTH_LONG).show();
            } else {
                // fill the text in.
                text.setText(number);
                text.setSelection(text.getText().length());

                // display the name as a toast
                name = context.getString(R.string.menu_callNumber, name);
                Toast.makeText(context, name, Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Override basic onQueryComplete to fill in the textfield when
         * we're handed the ADN cursor.
         * M: [Gemini+]token is the query slotId
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            log("onQueryComplete token = " + token);
            sc = (SimContactQueryCookie) cookie;
            log("onQueryComplete sc = " + sc);
            if (sc == null || sStopProgress) {
                log("onQueryComplete sc = " + sc + "sStopProgress "
                        + sStopProgress);
                if (c != null) {
                    c.close();
                }
                return;
            }
            final Context context = sc.progressDialog.getContext();
            EditText text = sc.getTextField();

            String name = null;
            String number = null;

            if (c != null && text != null) {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    if (fdnRequest(token)) {
                        log("fdnRequest for " + token);
                        break;
                    }

                    if (c.getInt(c.getColumnIndexOrThrow(ADN_INDEX_COLUMN_NAME)) == sc.contactNum) {
                        name = c.getString(c
                                .getColumnIndexOrThrow(ADN_NAME_COLUMN_NAME));
                        number = c
                                .getString(c
                                        .getColumnIndexOrThrow(ADN_PHONE_NUMBER_COLUMN_NAME));
                        sc.mFoundForSlot.put(token, true);
                        break;
                    }
                }
                c.close();
            }

            log("sc.mFoundForSlot[" + token + "] " + sc.mFoundForSlot.get(token));

            sc.mSimNameForSlot.put(token, name);
            sc.mSimNumberForSlot.put(token, number);
            log("name = " + name + " number = " + number);

            if (sc.mIsSingleQuery) {
               if (sc.progressDialog != null && sc.progressDialog.isShowing()) {
                    sc.progressDialog.dismiss();
                    sc.progressDialog = null;
                }

                if (sc.mFoundForSlot.get(token)) {
                    showToast(context, sc, name, number);
                } // findFlag
            } else {
                if (sStateReadySlotList.isEmpty()) {
                    if (sc.progressDialog != null
                            && sc.progressDialog.isShowing()) {
                        sc.progressDialog.dismiss();
                        sc.progressDialog = null;
                    }

                    /**
                     * M: [ALPS00612118] if the progress dialog is canceled, the
                     * SimPickerDialog should not be shown then. @{
                     */
                    if (mShouldDismissSimPickerDialog) {
                        LogUtils.w(TAG, "[onQueryComplete]should dismiss, not show sim select");
                        return;
                    }
                    /**
                     * @}
                     */

                    DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                final AlertDialog alert = (AlertDialog) dialog;
                                final ListAdapter listAdapter = alert.getListView().getAdapter();
                                final int slot = ((Integer)listAdapter.getItem(which)).intValue();
                                log("onClick slot = " + slot + "  dialog = " + dialog);
                                /** M: Fixed CR ALPS00567927 */
                                dialog.dismiss();
                                if (sc.mFoundForSlot.get(slot)) {
                                    showToast(context, sc, sc.mSimNameForSlot.get(slot),
                                            sc.mSimNumberForSlot.get(slot));
                                }
                                log("onClick dismiss dialog = " + dialog);
                            } catch (Exception e) {
                                Log.d(TAG, "exception : " + e.getMessage());
                                Log.d(TAG, "exception : " + e);
                            }
                        }
                    };

                    alert = SimPickerDialog.create(context,
                            context.getString(R.string.call_pin_dialog_title),
                            false, onClickListener);
                    alert.show();
                    log("onquerycomplete: show the selector dialog = " + alert);
              } else {
                  QueryHandler handler = sc.getQueryHandler();
                  int nextQuerySlotId = sStateReadySlotList.poll();
                  Uri uri = SimCardUtils.SimUri.getSimUri(nextQuerySlotId);
                  handler.startQuery(nextQuerySlotId, sc, uri,
                            new String[] { ADN_PHONE_NUMBER_COLUMN_NAME,
                                    ADN_INDEX_COLUMN_NAME }, null, null, null);
                  log("[onQueryComplete]next slot to query is: " + nextQuerySlotId);
              }
           }
        }

        /**
         * M: [ALPS00612118] change the local vars to fields @{
         */
        private AlertDialog alert;
        private SimContactQueryCookie sc;
        private boolean mShouldDismissSimPickerDialog = false;
        /**
         * @}
         */
    }

    /*
     * public static boolean fdnRequest(int slot) {
     * 
     * Phone phone = PhoneFactory.getDefaultPhone(); if (null == phone) {
     * Log.e(TAG, "fdnRequest phone is null"); return false; } IccCard iccCard;
     * if (true == FeatureOption.MTK_GEMINI_SUPPORT) { iccCard = ((GeminiPhone)
     * phone).getIccCardGemini(slot); } else { iccCard = phone.getIccCard(); }
     * 
     * return iccCard.getIccFdnEnabled(); }
     */
    static boolean fdnRequest(int slot) {
        return SimCardUtils.isFdnEnabed(slot);
    }

    static void log(String msg) {
        Log.d(TAG, msg);
    }

    public static void dismissDialog() {
        Log.d(TAG, "dismissProgressDialog");
        sStopProgress = true;
        Log.d(TAG, "sStopProgress " + sStopProgress);
        if (sCookie != null && sCookie.progressDialog != null
                && sCookie.progressDialog.isShowing()) {
            sCookie.progressDialog.dismiss();
        }
    }
}
