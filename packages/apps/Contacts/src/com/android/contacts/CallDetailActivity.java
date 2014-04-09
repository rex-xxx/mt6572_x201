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

package com.android.contacts;

import android.app.ActionBar;
import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.Contacts.Intents.Insert;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.VoicemailContract.Voicemails;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.BackScrollManager.ScrollableHeader;
import com.android.contacts.calllog.CallDetailHistoryAdapter;
import com.android.contacts.calllog.CallLogQuery;
import com.android.contacts.calllog.CallTypeHelper;
import com.android.contacts.calllog.ContactInfo;
import com.android.contacts.calllog.ContactInfoHelper;
import com.android.contacts.calllog.DefaultVoicemailNotifier;
import com.android.contacts.calllog.PhoneNumberHelper;
import com.android.contacts.format.FormatUtils;
import com.android.contacts.util.AsyncTaskExecutor;
import com.android.contacts.util.AsyncTaskExecutors;
import com.android.contacts.util.ClipboardUtils;
import com.android.contacts.util.Constants;
import com.android.contacts.voicemail.VoicemailPlaybackFragment;
import com.android.contacts.voicemail.VoicemailStatusHelper;
import com.android.contacts.voicemail.VoicemailStatusHelper.StatusMessage;
import com.android.contacts.voicemail.VoicemailStatusHelperImpl;

/// The following lines are provided and maintained by Mediatek Inc.
import com.android.contacts.util.PhoneCapabilityTester;
import com.mediatek.contacts.util.LogUtils;
import com.mediatek.contacts.util.VvmUtils;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.util.SetIndicatorUtils;
import com.mediatek.phone.SIMInfoWrapper;
/// The previous lines are provided and maintained by Mediatek Inc.

import java.util.List;

/**
 * Displays the details of a specific call log entry.
 * <p>
 * This activity can be either started with the URI of a single call log entry, or with the
 * {@link #EXTRA_CALL_LOG_IDS} extra to specify a group of call log entries.
 */
public class CallDetailActivity extends Activity implements ProximitySensorAware {
    private static final String TAG = CallDetailActivity.class.getSimpleName();

    /** The time to wait before enabling the blank the screen due to the proximity sensor. */
    private static final long PROXIMITY_BLANK_DELAY_MILLIS = 100;
    /** The time to wait before disabling the blank the screen due to the proximity sensor. */
    private static final long PROXIMITY_UNBLANK_DELAY_MILLIS = 500;

    /** The enumeration of {@link AsyncTask} objects used in this class. */
    public enum Tasks {
        MARK_VOICEMAIL_READ,
        DELETE_VOICEMAIL_AND_FINISH,
        REMOVE_FROM_CALL_LOG_AND_FINISH,
        UPDATE_PHONE_CALL_DETAILS,
    }

    /** A long array extra containing ids of call log entries to display. */
    public static final String EXTRA_CALL_LOG_IDS = "EXTRA_CALL_LOG_IDS";
    /** If we are started with a voicemail, we'll find the uri to play with this extra. */
    public static final String EXTRA_VOICEMAIL_URI = "EXTRA_VOICEMAIL_URI";
    /** If we should immediately start playback of the voicemail, this extra will be set to true. */
    public static final String EXTRA_VOICEMAIL_START_PLAYBACK = "EXTRA_VOICEMAIL_START_PLAYBACK";
    /** If the activity was triggered from a notification. */
    public static final String EXTRA_FROM_NOTIFICATION = "EXTRA_FROM_NOTIFICATION";

    private CallTypeHelper mCallTypeHelper;
    private PhoneNumberHelper mPhoneNumberHelper;
    private PhoneCallDetailsHelper mPhoneCallDetailsHelper;
    private TextView mHeaderTextView;
    private View mHeaderOverlayView;
    private ImageView mMainActionView;
    private ImageButton mMainActionPushLayerView;
    private ImageView mContactBackgroundView;
    private AsyncTaskExecutor mAsyncTaskExecutor;
    private ContactInfoHelper mContactInfoHelper;

    private String mNumber = "";
    private String mDefaultCountryIso;

    /* package */ LayoutInflater mInflater;
    /* package */ Resources mResources;
    /** Helper to load contact photos. */
    private ContactPhotoManager mContactPhotoManager;

    /** M:[VVM] when VVM disabled, the following field will not be used @ { */
    /** Helper to make async queries to content resolver. */
    private CallDetailActivityQueryHandler mAsyncQueryHandler;
    /** Helper to get voicemail status messages. */
    private VoicemailStatusHelper mVoicemailStatusHelper;
    // Views related to voicemail status message.
    private View mStatusMessageView;
    private TextView mStatusMessageText;
    private TextView mStatusMessageAction;
    /** Whether we should show "trash" in the options menu. */
    private boolean mHasTrashOption;
    /** Whether we should show "remove from call log" in the options menu. */
    private boolean mHasRemoveFromCallLogOption;
    /** @} */

    /** Whether we should show "edit number before call" in the options menu. */
    private boolean mHasEditNumberBeforeCallOption;


    private ProximitySensorManager mProximitySensorManager;
    private final ProximitySensorListener mProximitySensorListener = new ProximitySensorListener();

    /**
     * The action mode used when the phone number is selected.  This will be non-null only when the
     * phone number is selected.
     */
    private ActionMode mPhoneNumberActionMode;

    private CharSequence mPhoneNumberLabelToCopy;
    private CharSequence mPhoneNumberToCopy;

    /** Listener to changes in the proximity sensor state. */
    private class ProximitySensorListener implements ProximitySensorManager.Listener {
        /** Used to show a blank view and hide the action bar. */
        private final Runnable mBlankRunnable = new Runnable() {
            @Override
            public void run() {
                View blankView = findViewById(R.id.blank);
                blankView.setVisibility(View.VISIBLE);
                getActionBar().hide();
            }
        };
        /** Used to remove the blank view and show the action bar. */
        private final Runnable mUnblankRunnable = new Runnable() {
            @Override
            public void run() {
                View blankView = findViewById(R.id.blank);
                blankView.setVisibility(View.GONE);
                getActionBar().show();
            }
        };

        @Override
        public synchronized void onNear() {
            clearPendingRequests();
            postDelayed(mBlankRunnable, PROXIMITY_BLANK_DELAY_MILLIS);
        }

        @Override
        public synchronized void onFar() {
            clearPendingRequests();
            postDelayed(mUnblankRunnable, PROXIMITY_UNBLANK_DELAY_MILLIS);
        }

        /** Removed any delayed requests that may be pending. */
        public synchronized void clearPendingRequests() {
            View blankView = findViewById(R.id.blank);
            blankView.removeCallbacks(mBlankRunnable);
            blankView.removeCallbacks(mUnblankRunnable);
        }

        /** Post a {@link Runnable} with a delay on the main thread. */
        private synchronized void postDelayed(Runnable runnable, long delayMillis) {
            // Post these instead of executing immediately so that:
            // - They are guaranteed to be executed on the main thread.
            // - If the sensor values changes rapidly for some time, the UI will not be
            //   updated immediately.
            View blankView = findViewById(R.id.blank);
            blankView.postDelayed(runnable, delayMillis);
        }
    }

    static final String[] CALL_LOG_PROJECTION = new String[] {
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
        CallLog.Calls.NUMBER,
        CallLog.Calls.TYPE,
        CallLog.Calls.COUNTRY_ISO,
        CallLog.Calls.GEOCODED_LOCATION,
        /** M: add @ { */
        CallLog.Calls.SIM_ID,
        CallLog.Calls.VTCALL,
        /** @ }*/
    };

    static final int DATE_COLUMN_INDEX = 0;
    static final int DURATION_COLUMN_INDEX = 1;
    static final int NUMBER_COLUMN_INDEX = 2;
    static final int CALL_TYPE_COLUMN_INDEX = 3;
    static final int COUNTRY_ISO_COLUMN_INDEX = 4;
    static final int GEOCODED_LOCATION_COLUMN_INDEX = 5;

    private final View.OnClickListener mPrimaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (finishPhoneNumerSelectedActionModeIfShown()) {
                return;
            }
            startActivity(((ViewEntry) view.getTag()).primaryIntent
                    /** M: add @ { */
                    .putExtra(Constants.EXTRA_FOLLOW_SIM_MANAGEMENT, true));
        }
    };

    private final View.OnClickListener mSecondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (finishPhoneNumerSelectedActionModeIfShown()) {
                return;
            }
            startActivity(((ViewEntry) view.getTag()).secondaryIntent);
        }
    };

    private final View.OnLongClickListener mPrimaryLongClickListener =
            new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (finishPhoneNumerSelectedActionModeIfShown()) {
                return true;
            }
            startPhoneNumberSelectedActionMode(v);
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        log("CallDetailActivity  onCreate()");
        super.onCreate(icicle);
        /** M: Bug Fix for ALPS00393950 @{ */
        SetIndicatorUtils.getInstance().registerReceiver(this);
        /** @} */
        /** M: [VVM] modify @ { */
        // setContentView(R.layout.call_detail);
        if (VvmUtils.isVvmEnabled()) {
            setContentView(R.layout.call_detail_with_voicemail);
        } else {
            setContentView(R.layout.call_detail_without_voicemail);
        }
        /** @ } */
        mAsyncTaskExecutor = AsyncTaskExecutors.createThreadPoolExecutor();
        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mResources = getResources();

        mCallTypeHelper = new CallTypeHelper(getResources());
        mPhoneNumberHelper = new PhoneNumberHelper(mResources);
        /** M: modify @ { */
        //mPhoneCallDetailsHelper = new PhoneCallDetailsHelper(mResources, mCallTypeHelper,
        //        mPhoneNumberHelper);
        mPhoneCallDetailsHelper = new PhoneCallDetailsHelper(mResources, mCallTypeHelper,
                                                            mPhoneNumberHelper, null, this);
        /** @ }*/
        mHeaderTextView = (TextView) findViewById(R.id.header_text);
        mHeaderOverlayView = findViewById(R.id.photo_text_bar);
        mMainActionView = (ImageView) findViewById(R.id.main_action);
        mMainActionPushLayerView = (ImageButton) findViewById(R.id.main_action_push_layer);
        mContactBackgroundView = (ImageView) findViewById(R.id.contact_background);
        mDefaultCountryIso = ContactsUtils.getCurrentCountryIso(this);
        mContactPhotoManager = ContactPhotoManager.getInstance(this);
        /**M: use ProximityWakeLock to switch screen on/off when sensor states change*/
        createProximityWakeLock();
        //mProximitySensorManager = new ProximitySensorManager(this, mProximitySensorListener);
        /**@}*/
        mContactInfoHelper = new ContactInfoHelper(this, ContactsUtils.getCurrentCountryIso(this));
        configureActionBar();

        /** M:[VVM] to create play back fragment when VVM enabled @ { */
        if (VvmUtils.isVvmEnabled()) {
            mVoicemailStatusHelper = new VoicemailStatusHelperImpl();
            mAsyncQueryHandler = new CallDetailActivityQueryHandler(this);
            mStatusMessageView = findViewById(R.id.voicemail_status);
            mStatusMessageText = (TextView) findViewById(R.id.voicemail_status_message);
            mStatusMessageAction = (TextView) findViewById(R.id.voicemail_status_action);
            Log.i(TAG, "onCreate(): getVoicemailUri = " + getVoicemailUri());
            optionallyHandleVoicemail();
        }
        /** @ } */

        if (getIntent().getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            closeSystemDialogs();
        }
        /** M: add @ { */
        mSimName = (TextView) findViewById(R.id.sim_name);
        SIMInfoWrapper.getDefault().registerForSimInfoUpdate(mHandler, SIM_INFO_UPDATE_MESSAGE, null);
        /** @ } */
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData(getCallLogEntryUris());
        /** M: add @ { */
        log("CallDetailActivity  onResume()");
        mHasSms = PhoneCapabilityTester.isSmsIntentRegistered(getApplicationContext());
        SetIndicatorUtils.getInstance().showIndicator(true, this);
        /** @ }*/
    }

    /**
     * [VVM]Handle voicemail playback or hide voicemail ui.
     * <p>
     * If the Intent used to start this Activity contains the suitable extras, then start voicemail
     * playback.  If it doesn't, then hide the voicemail ui.
     */
    private void optionallyHandleVoicemail() {
        View voicemailContainer = findViewById(R.id.voicemail_container);
        if (hasVoicemail()) {
            // Has voicemail: add the voicemail fragment.  Add suitable arguments to set the uri
            // to play and optionally start the playback.
            // Do a query to fetch the voicemail status messages.
            VoicemailPlaybackFragment playbackFragment = new VoicemailPlaybackFragment();
            Bundle fragmentArguments = new Bundle();
            fragmentArguments.putParcelable(EXTRA_VOICEMAIL_URI, getVoicemailUri());
            if (getIntent().getBooleanExtra(EXTRA_VOICEMAIL_START_PLAYBACK, false)) {
                fragmentArguments.putBoolean(EXTRA_VOICEMAIL_START_PLAYBACK, true);
            }
            playbackFragment.setArguments(fragmentArguments);
            voicemailContainer.setVisibility(View.VISIBLE);
            getFragmentManager().beginTransaction()
                    .add(R.id.voicemail_container, playbackFragment).commitAllowingStateLoss();
            Log.d(TAG,"[optionallyHandleVoicemail] getVoicemailUri()==" + getVoicemailUri().toString());
            mAsyncQueryHandler.startVoicemailStatusQuery(getVoicemailUri());
            markVoicemailAsRead(getVoicemailUri());
        } else {
            // No voicemail uri: hide the status view.
            mStatusMessageView.setVisibility(View.GONE);
            voicemailContainer.setVisibility(View.GONE);
        }
    }

    private boolean hasVoicemail() {
        return getVoicemailUri() != null;
    }

    private Uri getVoicemailUri() {
        return getIntent().getParcelableExtra(EXTRA_VOICEMAIL_URI);
    }

    private void markVoicemailAsRead(final Uri voicemailUri) {
        mAsyncTaskExecutor.submit(Tasks.MARK_VOICEMAIL_READ, new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                ContentValues values = new ContentValues();
                values.put(Voicemails.IS_READ, true);
                getContentResolver().update(voicemailUri, values,
                        Voicemails.IS_READ + " = 0", null);
                /** M: mark the read voice mail as old @ { */
                values.clear();
                values.put(Calls.NEW, 0);
                getContentResolver().update(Calls.CONTENT_URI_WITH_VOICEMAIL, values, Voicemails.IS_READ + " = 1", null);
                // update new voice mail count
                Cursor c = getContentResolver().query(Calls.CONTENT_URI_WITH_VOICEMAIL, new String[] { Voicemails._ID },
                        Calls.NEW + "=1 AND " + Calls.TYPE + "=" + Calls.VOICEMAIL_TYPE, null, null);
                int cnt = 0;
                if (c != null) {
                    cnt = c.getCount();
                    c.close();
                }
                DefaultVoicemailNotifier.getInstance(getApplicationContext()).setNewVoicemailCount(cnt);
                /** @} */
                return null;
            }
        });
    }

    /**
     * Returns the list of URIs to show.
     * <p>
     * There are two ways the URIs can be provided to the activity: as the data on the intent, or as
     * a list of ids in the call log added as an extra on the URI.
     * <p>
     * If both are available, the data on the intent takes precedence.
     */
    private Uri[] getCallLogEntryUris() {
        log("CallDetailActivity getCallLogEntryUris()");
        Uri uri = getIntent().getData();
        if (uri != null) {
            // If there is a data on the intent, it takes precedence over the extra.
            /** M:[VVM] add @ { */
            Uri queryUri = Uri.parse("content://call_log/callsjoindataview");
            long id = ContentUris.parseId(uri);
            uri = ContentUris.withAppendedId(queryUri, id);
            if (VvmUtils.isVvmEnabled()) {
                uri = VvmUtils.buildVvmAllowedUri(uri);
            }
            /** @ } */
            return new Uri[]{ uri };
        }
        long[] ids = getIntent().getLongArrayExtra(EXTRA_CALL_LOG_IDS);
        Uri[] uris = new Uri[ids.length];
        for (int index = 0; index < ids.length; ++index) {
            /** M:[VVM] modify @ { */
           // uris[index] = ContentUris.withAppendedId(Calls.CONTENT_URI_WITH_VOICEMAIL, ids[index]);
            Uri queryUri = Uri.parse("content://call_log/callsjoindataview");
            uris[index] = ContentUris.withAppendedId(queryUri, ids[index]);
            if (VvmUtils.isVvmEnabled()) {
                uris[index] = VvmUtils.buildVvmAllowedUri(uris[index]);
            }
            /** @ }*/
        }
        return uris;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                // Make sure phone isn't already busy before starting direct call
                TelephonyManager tm = (TelephonyManager)
                        getSystemService(Context.TELEPHONY_SERVICE);
                /** M: ALPS00437114 Prevent unnecessary keyEvent "dial" key when the mNumber is null. @{*/
                Log.i(TAG, "Dialing Number: " + mNumber);
                if (mNumber != null && tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                /** @} */
                    startActivity(ContactsUtils.getCallIntent(
                            Uri.fromParts(Constants.SCHEME_TEL, mNumber, null))
                    /** M: add @ { */
                    .setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_BROADCASTER));
                    return true;
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Update user interface with details of given call.
     *
     * @param callUris URIs into {@link CallLog.Calls} of the calls to be displayed
     */
    private void updateData(final Uri... callUris) {
        class UpdateContactDetailsTask extends AsyncTask<Void, Void, PhoneCallDetails[]> {
            @Override
            public PhoneCallDetails[] doInBackground(Void... params) {
                // TODO: All phone calls correspond to the same person, so we can make a single
                // lookup.
                final int numCalls = callUris.length;
                PhoneCallDetails[] details = new PhoneCallDetails[numCalls];
                try {
                    for (int index = 0; index < numCalls; ++index) {
                        details[index] = getPhoneCallDetailsForUri(callUris[index]);
                    }
                    return details;
                } catch (IllegalArgumentException e) {
                    // Something went wrong reading in our primary data.
                    Log.w(TAG, "invalid URI starting call details", e);
                    return null;
                }
            }

            @Override
            public void onPostExecute(PhoneCallDetails[] details) {
                if (isFinishing()) {
                    Log.w(TAG, "[updateData] onPostExecute, activity finished.");
                    return;
                }
                if (details == null) {
                    // Somewhere went wrong: we're going to bail out and show error to users.
                    Toast.makeText(CallDetailActivity.this, R.string.toast_call_detail_error,
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // We know that all calls are from the same number and the same contact, so pick the
                // first.
                PhoneCallDetails firstDetails = details[0];
                mNumber = firstDetails.number.toString();
                final Uri contactUri = firstDetails.contactUri;
                final Uri photoUri = firstDetails.photoUri;

                // Set the details header, based on the first phone call.
                mPhoneCallDetailsHelper.setCallDetailsHeader(mHeaderTextView, firstDetails);

                // Cache the details about the phone number.
                /** M: add @ { */
                final Uri numberCallUri = mPhoneNumberHelper
                        .getCallUri(mNumber, firstDetails.simId);
                /** @ }*/
                final boolean canPlaceCallsTo = mPhoneNumberHelper.canPlaceCallsTo(mNumber);
                final boolean isVoicemailNumber = mPhoneNumberHelper.isVoicemailNumber(mNumber,firstDetails.simId);
                final boolean isSipNumber = mPhoneNumberHelper.isSipNumber(mNumber);

                // Let user view contact details if they exist, otherwise add option to create new
                // contact from this number.
                final Intent mainActionIntent;
                final int mainActionIcon;
                final String mainActionDescription;

                final CharSequence nameOrNumber;
                if (!TextUtils.isEmpty(firstDetails.name)) {
                    nameOrNumber = firstDetails.name;
                } else {
                    nameOrNumber = firstDetails.number;
                }

                LogUtils.d(TAG, "[onPostExecute]contactUri:" + contactUri + ", isVoicemailNumber: " + isVoicemailNumber
                        + ", canPlaceCallsTo: " + canPlaceCallsTo);
                if (contactUri != null) {
                    mainActionIntent = new Intent(Intent.ACTION_VIEW, contactUri);
                    // This will launch People's detail contact screen, so we probably want to
                    // treat it as a separate People task.
                    mainActionIntent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    mainActionIcon = R.drawable.ic_contacts_holo_dark;
                    mainActionDescription =
                            getString(R.string.description_view_contact, nameOrNumber);
                } else if (isVoicemailNumber) {
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                    /** M: delete @ { */
                    // } else if (isSipNumber) {
                    // TODO: This item is currently disabled for SIP addresses, because
                    // the Insert.PHONE extra only works correctly for PSTN numbers.
                    //
                    // To fix this for SIP addresses, we need to:
                    // - define ContactsContract.Intents.Insert.SIP_ADDRESS, and use it here if
                    //   the current number is a SIP address
                    // - update the contacts UI code to handle Insert.SIP_ADDRESS by
                    //   updating the SipAddress field
                    // and then we can remove the "!isSipNumber" check above.
                    // mainActionIntent = null;
                    // mainActionIcon = 0;
                    // mainActionDescription = null;
                    /** @ } */
                } else if (canPlaceCallsTo) {
                    mainActionIntent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                    mainActionIntent.setType(Contacts.CONTENT_ITEM_TYPE);
                    /** M: modify @ { */
                    // mainActionIntent.putExtra(Insert.PHONE, mNumber);
                    if (isSipNumber) {
                        mainActionIntent.putExtra(ContactsContract.Intents.Insert.SIP_ADDRESS,
                                                        mNumber);
                    } else {
                        mainActionIntent.putExtra(Insert.PHONE, mNumber);
                    }
                    /** @ } */
                    mainActionIcon = R.drawable.ic_add_contact_holo_dark;
                    mainActionDescription = getString(R.string.description_add_contact);
                } else {
                    // If we cannot call the number, when we probably cannot add it as a contact either.
                    // This is usually the case of private, unknown, or payphone numbers.
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                }

                LogUtils.d(TAG, "[onPostExecute]mainActionIntent = " + mainActionIntent);
                if (mainActionIntent == null) {
                    mMainActionView.setVisibility(View.INVISIBLE);
                    mMainActionPushLayerView.setVisibility(View.GONE);
                    mHeaderTextView.setVisibility(View.INVISIBLE);
                    mHeaderOverlayView.setVisibility(View.INVISIBLE);
                } else {
                    mMainActionView.setVisibility(View.VISIBLE);
                    mMainActionView.setImageResource(mainActionIcon);
                    mMainActionPushLayerView.setVisibility(View.VISIBLE);
                    mMainActionPushLayerView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(mainActionIntent);
                        }
                    });
                    mMainActionPushLayerView.setContentDescription(mainActionDescription);
                    mHeaderTextView.setVisibility(View.VISIBLE);
                    mHeaderOverlayView.setVisibility(View.VISIBLE);
                }

                // This action allows to call the number that places the call.
                if (canPlaceCallsTo) {
                    final CharSequence displayNumber =
                            mPhoneNumberHelper.getDisplayNumber(
                                    firstDetails.number, firstDetails.formattedNumber);
                    /** M: modify @ { */
                    // ViewEntry entry = new ViewEntry(
                     //       getString(R.string.menu_callNumber,
                    //                FormatUtils.forceLeftToRight(displayNumber)),
                     //               ContactsUtils.getCallIntent(mNumber),
                     //               getString(R.string.description_call, nameOrNumber));
                    boolean isVoicemailUri = PhoneNumberHelper.isVoicemailUri(numberCallUri);
                    int slotId = SIMInfoWrapper.getDefault().getSimSlotById((int) firstDetails.simId);
                    Intent callIntent = ContactsUtils.getCallIntent(mNumber).putExtra(
                            Constants.EXTRA_ORIGINAL_SIM_ID, (long) firstDetails.simId);
                    callIntent.setClassName(Constants.PHONE_PACKAGE, Constants.OUTGOING_CALL_BROADCASTER);
                    if (isVoicemailUri && slotId != -1) {
                        callIntent.putExtra("simId", slotId);
                    }
                    ViewEntry entry = new ViewEntry(getString(R.string.menu_callNumber, FormatUtils
                            .forceLeftToRight(displayNumber)), callIntent, getString(
                            R.string.description_call, nameOrNumber));
                    /** @ } */

                    // Only show a label if the number is shown and it is not a SIP address.
                    if (!TextUtils.isEmpty(firstDetails.name)
                            && !TextUtils.isEmpty(firstDetails.number)
                            && !PhoneNumberUtils.isUriNumber(firstDetails.number.toString())) {

                        /** M: add @ { */
                        if (0 == firstDetails.numberType) {
                            entry.label = mResources.getString(R.string.list_filter_custom);
                        } else {
                            /** @ } */
                            /** M:AAS @ { original code:
                             entry.label = Phone.getTypeLabel(mResources, firstDetails.numberType,
                                    firstDetails.numberLabel); */
                            entry.label = ExtensionManager.getInstance()
                                    .getContactAccountExtension().getTypeLabel(
                                            mResources, firstDetails.numberType,
                                            firstDetails.numberLabel, slotId,
                                            ExtensionManager.COMMD_FOR_AAS);
                            /** @ } */
                        }
                    }

                    // The secondary action allows to send an SMS to the number that placed the
                    // call.
                    /** M: modify @ { */
                    //if (mPhoneNumberHelper.canSendSmsTo(mNumber)) {
                    if (mPhoneNumberHelper.canSendSmsTo(mNumber) && mHasSms) {
                        entry.setSecondaryAction(
                                R.drawable.ic_text_holo_dark,
                                new Intent(Intent.ACTION_SENDTO,
                                           Uri.fromParts("sms", mNumber, null)),
                                getString(R.string.description_send_text_message, nameOrNumber));
                    }

                    /** M: add @ { */
                    // for sim name
                    setSimInfo(firstDetails.simId);

                    // For Video Call.
                    if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                        Intent itThird = new Intent(Intent.ACTION_CALL_PRIVILEGED, numberCallUri)
                                .putExtra(Constants.EXTRA_IS_VIDEO_CALL, true)
                                .putExtra(Constants.EXTRA_ORIGINAL_SIM_ID, (long) firstDetails.simId);
                        itThird.setClassName(Constants.PHONE_PACKAGE,
                                Constants.OUTGOING_CALL_BROADCASTER);
                        if (isVoicemailUri && slotId != -1) {
                            itThird.putExtra("simId", slotId);
                        }
                        entry.setThirdAction(getString(R.string.menu_videocallNumber, FormatUtils
                                .forceLeftToRight(displayNumber)), itThird, getString(
                                R.string.description_call, nameOrNumber));
                    } else {
                        entry.thirdIntent = null;
                    }

                    if (SIMInfoWrapper.getDefault().getInsertedSimCount() != 0) {
                        Intent itFourth = new Intent(Intent.ACTION_CALL_PRIVILEGED, numberCallUri)
                            .putExtra(Constants.EXTRA_ORIGINAL_SIM_ID, (long) firstDetails.simId)
                            .putExtra(Constants.EXTRA_IS_IP_DIAL, true)
                            .putExtra(Constants.EXTRA_ORIGINAL_SIM_ID, (long) firstDetails.simId);
                        itFourth.setClassName(Constants.PHONE_PACKAGE,
                                Constants.OUTGOING_CALL_BROADCASTER);
                    if (isVoicemailUri && slotId != -1) {
                        itFourth.putExtra("simId", slotId);
                    }
                    entry.setFourthAction(getString(R.string.menu_ipcallNumber, FormatUtils
                            .forceLeftToRight(displayNumber)), itFourth, getString(
                                    R.string.description_call, nameOrNumber));
                    } else {
                        entry.fourthIntent = null;
                    }

                    entry.geocode = firstDetails.geocode;
                    /** @ }*/

                    configureCallButton(entry);
                    mPhoneNumberToCopy = displayNumber;
                    mPhoneNumberLabelToCopy = entry.label;
                    /** M: add RCS @ { */
                    String name = null;
                    String number = null;
                    if (null != firstDetails) {
                        number = firstDetails.number.toString();
                        if (null != firstDetails.name) {
                            name = firstDetails.name.toString();
                        }
                    }
                    Log.i(TAG, "updateData name, number : " + name + " , " + number);
                    ExtensionManager.getInstance().getCallDetailExtension().setViewVisibleByActivity(
                            CallDetailActivity.this, name, number, R.id.RCS_container,
                            R.id.separator03, R.id.RCS, R.id.RCS_action, R.id.RCS_text,
                            R.id.RCS_icon, R.id.RCS_divider, ExtensionManager.COMMD_FOR_RCS);
                } else {
                    /** @ } */
                    disableCallButton();
                    mPhoneNumberToCopy = null;
                    mPhoneNumberLabelToCopy = null;
                }

                /** M: [VVM] modify @ { */
                mHasEditNumberBeforeCallOption =
                        canPlaceCallsTo && !isSipNumber && !isVoicemailNumber;
                mHasTrashOption = hasVoicemail();
                mHasRemoveFromCallLogOption = !hasVoicemail();
                /** @ } */
                invalidateOptionsMenu();

                ListView historyList = (ListView) findViewById(R.id.history);

                historyList.setAdapter(
                        new CallDetailHistoryAdapter(CallDetailActivity.this, mInflater,
                                mCallTypeHelper, details, hasVoicemail(), canPlaceCallsTo,
                                findViewById(R.id.controls)));

                BackScrollManager.bind(
                        new ScrollableHeader() {
                            private View mControls = findViewById(R.id.controls);
                            private View mPhoto = findViewById(R.id.contact_background_sizer);
                            private View mHeader = findViewById(R.id.photo_text_bar);
                            private View mSeparator = findViewById(R.id.blue_separator);

                            @Override
                            public void setOffset(int offset) {
                                mControls.setY(-offset);
                            }

                            @Override
                            public int getMaximumScrollableHeaderOffset() {
                                // We can scroll the photo out, but we should keep the header if
                                // present.
                                if (mHeader.getVisibility() == View.VISIBLE) {
                                    return mPhoto.getHeight() - mHeader.getHeight();
                                } else {
                                    // If the header is not present, we should also scroll out the
                                    // separator line.
                                    return mPhoto.getHeight() + mSeparator.getHeight();
                                }
                            }
                        },
                        historyList);
                loadContactPhotos(photoUri);
                findViewById(R.id.call_detail).setVisibility(View.VISIBLE);
            }
        }
        mAsyncTaskExecutor.submit(Tasks.UPDATE_PHONE_CALL_DETAILS, new UpdateContactDetailsTask());
    }

    /** Return the phone call details for a given call log URI. */
    private PhoneCallDetails getPhoneCallDetailsForUri(Uri callUri) {
        ContentResolver resolver = getContentResolver();
        /** M: modify @ { */
        //Cursor callCursor = resolver.query(callUri, CALL_LOG_PROJECTION, null, null, null);
        Cursor callCursor = resolver.query(callUri, CallLogQuery.PROJECTION_CALLS_JOIN_DATAVIEW, null, null, null);
        try {
            if (callCursor == null || !callCursor.moveToFirst()) {
                throw new IllegalArgumentException("Cannot find content: " + callUri);
            }

            /** M: modify @ { */
            /**
             * 
             // Read call log specifics.
            String number = callCursor.getString(NUMBER_COLUMN_INDEX);
            long date = callCursor.getLong(DATE_COLUMN_INDEX);
            long duration = callCursor.getLong(DURATION_COLUMN_INDEX);
            int callType = callCursor.getInt(CALL_TYPE_COLUMN_INDEX);
            String countryIso = callCursor.getString(COUNTRY_ISO_COLUMN_INDEX);
            final String geocode = callCursor.getString(GEOCODED_LOCATION_COLUMN_INDEX);

            if (TextUtils.isEmpty(countryIso)) {
                countryIso = mDefaultCountryIso;
            }

            // Formatted phone number.
            final CharSequence formattedNumber;
            // Read contact specifics.
            final CharSequence nameText;
            final int numberType;
            final CharSequence numberLabel;
            final Uri photoUri;
            final Uri lookupUri;
            // If this is not a regular number, there is no point in looking it up in the contacts.
            ContactInfo info =
                    mPhoneNumberHelper.canPlaceCallsTo(number)
                    && !mPhoneNumberHelper.isVoicemailNumber(number)
                            ? mContactInfoHelper.lookupNumber(number, countryIso)
                            : null;
            if (info == null) {
                formattedNumber = mPhoneNumberHelper.getDisplayNumber(number, null);
                nameText = "";
                numberType = 0;
                numberLabel = "";
                photoUri = null;
                lookupUri = null;
            } else {
                formattedNumber = info.formattedNumber;
                nameText = info.name;
                numberType = info.type;
                numberLabel = info.label;
                photoUri = info.photoUri;
                lookupUri = info.lookupUri;
            }
            return new PhoneCallDetails(number, formattedNumber, countryIso, geocode,
                    new int[]{ callType }, date, duration,
                    nameText, numberType, numberLabel, lookupUri, photoUri);
             */
            
            ContactInfo contactInfo = ContactInfo.fromCursor(callCursor);
            String photo = callCursor.getString(CallLogQuery.CALLS_JOIN_DATA_VIEW_PHOTO_URI);
            Uri photoUri = null;
            if (null != photo) {
                photoUri = Uri.parse(photo);
            } else {
                photoUri = null;
            }
            log("number = " + contactInfo.number);
            if (!mPhoneNumberHelper.canPlaceCallsTo(contactInfo.number)
                    || mPhoneNumberHelper.isVoiceMailNumberForMtk(contactInfo.number,
                            contactInfo.simId)
                    || mPhoneNumberHelper.isEmergencyNumber(contactInfo.number)) {
                contactInfo.formattedNumber = mPhoneNumberHelper.getDisplayNumber(
                        contactInfo.number, null).toString();
                contactInfo.name = "";
                contactInfo.nNumberTypeId = 0;
                contactInfo.label = "";
                photoUri = null;
                contactInfo.lookupUri = null;
            }
            
            return new PhoneCallDetails(contactInfo.number, contactInfo.formattedNumber, 
                    contactInfo.countryIso, contactInfo.geocode,
                    contactInfo.type, contactInfo.date,
                    contactInfo.duration, contactInfo.name,
                    contactInfo.nNumberTypeId, contactInfo.label,
                    contactInfo.lookupUri, photoUri, contactInfo.simId,
                    contactInfo.vtCall, 0, contactInfo.ipPrefix);
            /** @ }*/
        } finally {
            if (callCursor != null) {
                callCursor.close();
            }
        }
    }

    /** Load the contact photos and places them in the corresponding views. */
    private void loadContactPhotos(Uri photoUri) {
        /** M:  modify @ { */
        /**
         * 
        mContactPhotoManager.loadPhoto(mContactBackgroundView, photoUri,
                mContactBackgroundView.getWidth(), true);
         */
        if (photoUri == null) {
            mContactBackgroundView.setImageResource(R.drawable.ic_contact_picture_180_holo_dark);
        } else {
            mContactPhotoManager.loadPhoto(mContactBackgroundView, photoUri, mContactBackgroundView.getWidth(), true);
        }
        /** @ }*/
    }

    static final class ViewEntry {
        public final String text;
        public final Intent primaryIntent;
        /** The description for accessibility of the primary action. */
        public final String primaryDescription;

        public CharSequence label = null;
        /** Icon for the secondary action. */
        public int secondaryIcon = 0;
        /** Intent for the secondary action. If not null, an icon must be defined. */
        public Intent secondaryIntent = null;
        /** The description for accessibility of the secondary action. */
        public String secondaryDescription = null;

        public ViewEntry(String text, Intent intent, String description) {
            this.text = text;
            primaryIntent = intent;
            primaryDescription = description;
        }

        public void setSecondaryAction(int icon, Intent intent, String description) {
            secondaryIcon = icon;
            secondaryIntent = intent;
            secondaryDescription = description;
        }
        
        /** M: add @ { */
        public CharSequence geocode;
        
        /**
         * Intent for the third action-vtCall. If not null, an icon must be
         * defined.
         */
        public Intent thirdIntent;
        public String thirdDescription;
        public String videoText;
        
        public Intent fourthIntent;
        public String fourthDescription;
        public String ipText;
        /** The description for accessibility of the third action. */

        public void setThirdAction(String text, Intent intent,
                String description) {
            videoText = text;
            thirdIntent = intent;
            thirdDescription = description;
        }

        /** The description for accessibility of the fourth action. */

        public void setFourthAction(String text, Intent intent,
                String description) {
            ipText = text;
            fourthIntent = intent;
            fourthDescription = description;
        }

        /** @ }*/
    }

    /** Disables the call button area, e.g., for private numbers. */
    private void disableCallButton() {
        findViewById(R.id.call_and_sms).setVisibility(View.GONE);
        /** M: add @ { */
        findViewById(R.id.separator01).setVisibility(View.GONE);
        findViewById(R.id.separator02).setVisibility(View.GONE);
        findViewById(R.id.video_call).setVisibility(View.GONE);
        findViewById(R.id.ip_call).setVisibility(View.GONE);
        /** M: New Feature RCS */
        findViewById(R.id.RCS).setVisibility(View.GONE);
        findViewById(R.id.separator03).setVisibility(View.GONE);
        /** @ } */
    }

    /** Configures the call button area using the given entry. */
    private void configureCallButton(ViewEntry entry) {
        View convertView = findViewById(R.id.call_and_sms);
        convertView.setVisibility(View.VISIBLE);

        ImageView icon = (ImageView) convertView.findViewById(R.id.call_and_sms_icon);
        View divider = convertView.findViewById(R.id.call_and_sms_divider);
        TextView text = (TextView) convertView.findViewById(R.id.call_and_sms_text);

        View mainAction = convertView.findViewById(R.id.call_and_sms_main_action);
        mainAction.setOnClickListener(mPrimaryActionListener);
        mainAction.setTag(entry);
        mainAction.setContentDescription(entry.primaryDescription);
        mainAction.setOnLongClickListener(mPrimaryLongClickListener);

        if (entry.secondaryIntent != null) {
            icon.setOnClickListener(mSecondaryActionListener);
            icon.setImageResource(entry.secondaryIcon);
            icon.setVisibility(View.VISIBLE);
            icon.setTag(entry);
            icon.setContentDescription(entry.secondaryDescription);
            divider.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        }
        text.setText(entry.text);
        /** M: add @ { */
        text.setPadding(this.getResources().getDimensionPixelSize(R.dimen.call_log_indent_margin), 0, 0, 0);

        TextView label = (TextView) convertView.findViewById(R.id.call_and_sms_label);
        if (TextUtils.isEmpty(entry.label)) {
            label.setVisibility(View.GONE);
        } else {
            label.setText(entry.label);
            label.setVisibility(View.VISIBLE);
            /** M: add @ { */
          label.setPadding(0, 0, this.getResources().getDimensionPixelSize(
                    R.dimen.call_log_inner_margin), 0);
            /** @ } */
        }
        /** M: add @ { */
        //For video call 
        TextView geocode = (TextView) convertView.findViewById(R.id.call_number_geocode);
        View labelAndgeocodeView = (View) convertView.findViewById(R.id.labe_and_geocode_text);
        labelAndgeocodeView.setPadding(this.getResources().getDimensionPixelSize(R.dimen.call_log_indent_margin), 0, 0, 0);

        if (FeatureOption.MTK_PHONE_NUMBER_GEODESCRIPTION) {

            if (TextUtils.isEmpty(entry.geocode)) {
                geocode.setVisibility(View.GONE);
            } else {
                geocode.setText(entry.geocode);
                geocode.setVisibility(View.VISIBLE);
            }

            if (TextUtils.isEmpty(entry.label) && TextUtils.isEmpty(entry.geocode)) {
                labelAndgeocodeView.setVisibility(View.GONE);
            } else {
                labelAndgeocodeView.setVisibility(View.VISIBLE);
            }
        } else {
            geocode.setVisibility(View.GONE);

            if (TextUtils.isEmpty(entry.label)) {
                labelAndgeocodeView.setVisibility(View.GONE);
            } else {
                labelAndgeocodeView.setVisibility(View.VISIBLE);
            }
        }
        
        View separator01 = findViewById(R.id.separator01);
        separator01.setVisibility(View.VISIBLE);
        View separator02 = findViewById(R.id.separator02);
        separator02.setVisibility(View.VISIBLE);
        
        View convertView1 = findViewById(R.id.video_call);
        View videoAction = convertView1.findViewById(R.id.video_call_action);
        if (entry.thirdIntent != null) {
            videoAction.setOnClickListener(mThirdActionListener);
            videoAction.setTag(entry);
            videoAction.setContentDescription(entry.thirdDescription);
            videoAction.setVisibility(View.VISIBLE);
            TextView videoText = (TextView) convertView1.findViewById(R.id.video_call_text);

            videoText.setText(entry.videoText);

            TextView videoLabel = (TextView) convertView1.findViewById(R.id.video_call_label);
            if (TextUtils.isEmpty(entry.label)) {
                videoLabel.setVisibility(View.GONE);
            } else {
                videoLabel.setText(entry.label);
                videoLabel.setVisibility(View.VISIBLE);
            }
        } else {
            separator01.setVisibility(View.GONE);
            convertView1.setVisibility(View.GONE);
        }
        
        //[VVM]For IP call
        if (hasVoicemail()) {
            View convertView2 = findViewById(R.id.ip_call_container);
            convertView2.setVisibility(View.GONE);
        } else {
            View convertView2 = findViewById(R.id.ip_call);
            if (entry.fourthIntent != null) {
                View ipAction = convertView2.findViewById(R.id.ip_call_action);
                ipAction.setOnClickListener(mFourthActionListener);
                ipAction.setTag(entry);
                ipAction.setContentDescription(entry.fourthDescription);
                TextView ipText = (TextView) convertView2.findViewById(R.id.ip_call_text);
                ipText.setText(entry.ipText);
                TextView ipLabel = (TextView) convertView2.findViewById(R.id.ip_call_label);
                if (TextUtils.isEmpty(entry.label)) {
                    ipLabel.setVisibility(View.GONE);
                } else {
                    ipLabel.setText(entry.label);
                    ipLabel.setVisibility(View.VISIBLE);
                }
            } else {
                separator02.setVisibility(View.GONE);
                convertView2.setVisibility(View.GONE);
            }
        }
        /** @ } */
    }

    protected void updateVoicemailStatusMessage(Cursor statusCursor) {
        if (statusCursor == null) {
            mStatusMessageView.setVisibility(View.GONE);
            return;
        }
        final StatusMessage message = getStatusMessage(statusCursor);
        if (message == null || !message.showInCallDetails()) {
            mStatusMessageView.setVisibility(View.GONE);
            return;
        }

        mStatusMessageView.setVisibility(View.VISIBLE);
        mStatusMessageText.setText(message.callDetailsMessageId);
        if (message.actionMessageId != -1) {
            mStatusMessageAction.setText(message.actionMessageId);
        }
        if (message.actionUri != null) {
            mStatusMessageAction.setClickable(true);
            mStatusMessageAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_VIEW, message.actionUri));
                }
            });
        } else {
            mStatusMessageAction.setClickable(false);
        }
    }

    private StatusMessage getStatusMessage(Cursor statusCursor) {
        List<StatusMessage> messages = mVoicemailStatusHelper.getStatusMessages(statusCursor);
        if (messages.size() == 0) {
            return null;
        }
        // There can only be a single status message per source package, so num of messages can
        // at most be 1.
        if (messages.size() > 1) {
            Log.w(TAG, String.format("Expected 1, found (%d) num of status messages." +
                    " Will use the first one.", messages.size()));
        }
        return messages.get(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.call_details_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /** M: [VVM] modify @ { */
        // This action deletes all elements in the group from the call log.
        // We don't have this action for voicemails, because you can just use the trash button.
        menu.findItem(R.id.menu_remove_from_call_log).setVisible(mHasRemoveFromCallLogOption);
        menu.findItem(R.id.menu_edit_number_before_call).setVisible(mHasEditNumberBeforeCallOption);
        menu.findItem(R.id.menu_trash).setVisible(mHasTrashOption);
        /** @ }*/
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onHomeSelected();
                return true;
            }

            // All the options menu items are handled by onMenu... methods.
            default:
                throw new IllegalArgumentException();
        }
    }

    public void onMenuRemoveFromCallLog(MenuItem menuItem) {
        final StringBuilder callIds = new StringBuilder();
        for (Uri callUri : getCallLogEntryUris()) {
            if (callIds.length() != 0) {
                callIds.append(",");
            }
            callIds.append(ContentUris.parseId(callUri));
        }
        mAsyncTaskExecutor.submit(Tasks.REMOVE_FROM_CALL_LOG_AND_FINISH,
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... params) {
                        getContentResolver().delete(Calls.CONTENT_URI_WITH_VOICEMAIL,
                                Calls._ID + " IN (" + callIds + ")", null);
                        return null;
                    }

                    @Override
                    public void onPostExecute(Void result) {
                        finish();
                    }
                });
    }

    public void onMenuEditNumberBeforeCall(MenuItem menuItem) {
        startActivity(new Intent(Intent.ACTION_DIAL, ContactsUtils.getCallUri(mNumber)));
    }

    public void onMenuTrashVoicemail(MenuItem menuItem) {
        final Uri voicemailUri = getVoicemailUri();
        mAsyncTaskExecutor.submit(Tasks.DELETE_VOICEMAIL_AND_FINISH,
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... params) {
                        getContentResolver().delete(voicemailUri, null, null);
                        return null;
                    }
                    @Override
                    public void onPostExecute(Void result) {
                        finish();
                    }
                });
    }

    private void configureActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);
        }
    }

    /** Invoked when the user presses the home button in the action bar. */
    private void onHomeSelected() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Calls.CONTENT_URI);
        // This will open the call log even if the detail view has been opened directly.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        // Immediately stop the proximity sensor.
        disableProximitySensor(false);
        mProximitySensorListener.clearPendingRequests();
        
        /** M: add @ { */
        SetIndicatorUtils.getInstance().showIndicator(false, this);
        /** @ }*/
        super.onPause();
    }

    @Override
    public void enableProximitySensor() {
        /** M: use Proximitywakelock to switch screen on/off, to acquire the sensor @{ */ 
        // mProximitySensorManager.enable();
        Log.d(TAG, "enableProximitySensor");
        if (mProximityWakeLock == null) {
            Log.w(TAG, "mProximityWakeLock = null");
            return;
        }
        synchronized (mProximityWakeLock) {
            if (!mProximityWakeLock.isHeld()) {
                Log.d(TAG, "updateProximitySensorMode: acquiring...");
                mProximityWakeLock.acquire();
            } else {
                Log.d(TAG, "updateProximitySensorMode: lock already held.");
            }
        }
        /**@}*/
    }

    @Override
    public void disableProximitySensor(boolean waitForFarState) {
        /** M: use Proximitywakelock to switch screen on/off, to release the lock @{ */ 
        // mProximitySensorManager.disable(waitForFarState);
        Log.d(TAG, "disableProximitySensor");
        if (mProximityWakeLock == null) {
            Log.w(TAG, "mProximityWakeLock = null");
            return;
        }
        synchronized (mProximityWakeLock) {
            if (mProximityWakeLock.isHeld()) {
                Log.d(TAG, "updateProximitySensorMode: releasing...");
                mProximityWakeLock.release(PowerManager.WAIT_FOR_PROXIMITY_NEGATIVE);
            }
        }
        /**@}*/
    }

    /**
     * If the phone number is selected, unselect it and return {@code true}.
     * Otherwise, just {@code false}.
     */
    private boolean finishPhoneNumerSelectedActionModeIfShown() {
        if (mPhoneNumberActionMode == null) return false;
        mPhoneNumberActionMode.finish();
        return true;
    }

    private void startPhoneNumberSelectedActionMode(View targetView) {
        mPhoneNumberActionMode = startActionMode(new PhoneNumberActionModeCallback(targetView));
    }

    private void closeSystemDialogs() {
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    private class PhoneNumberActionModeCallback implements ActionMode.Callback {
        private final View mTargetView;
        private final Drawable mOriginalViewBackground;

        public PhoneNumberActionModeCallback(View targetView) {
            mTargetView = targetView;

            // Highlight the phone number view.  Remember the old background, and put a new one.
            mOriginalViewBackground = mTargetView.getBackground();
            mTargetView.setBackgroundColor(getResources().getColor(R.color.item_selected));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (TextUtils.isEmpty(mPhoneNumberToCopy)) return false;

            getMenuInflater().inflate(R.menu.call_details_cab, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.copy_phone_number:
                    ClipboardUtils.copyText(CallDetailActivity.this, mPhoneNumberLabelToCopy,
                            mPhoneNumberToCopy, true);
                    mode.finish(); // Close the CAB
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mPhoneNumberActionMode = null;

            // Restore the view background.
            mTargetView.setBackground(mOriginalViewBackground);
        }
    }
        /** M: add @ { */
    private static final int SIM_INFO_UPDATE_MESSAGE = 100;
    static final int CALL_SIMID_COLUMN_INDEX = 6;
    static final int CALL_VT_COLUMN_INDEX = 7;
    
    private TextView mSimName;
    private boolean mHasSms = true;
    
    public StatusBarManager mStatusBarMgr;
    private boolean mShowSimIndicator;

    
    @Override
    protected void onDestroy() {
        log("onDestroy()");
        super.onDestroy();
        SIMInfoWrapper.getDefault().unregisterForSimInfoUpdate(mHandler);
        SetIndicatorUtils.getInstance().unregisterReceiver(this);
    }

    
    //VT Call
    private final View.OnClickListener mThirdActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(((ViewEntry) view.getTag()).thirdIntent);
        }
    };
    //IP Call
    private final View.OnClickListener mFourthActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(((ViewEntry) view.getTag()).fourthIntent);
        }
    };


    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SIM_INFO_UPDATE_MESSAGE:
                    updateData(getCallLogEntryUris());
                    break;
                default:
                    break;
            }
        }
    };




    private void setSimInfo(int simId) {
        //if (FeatureOption.MTK_GEMINI_SUPPORT) {
            int rPadding = this.getResources().getDimensionPixelSize(R.dimen.dialpad_operator_horizontal_padding_right);
            int lPadding = this.getResources().getDimensionPixelSize(R.dimen.dialpad_operator_horizontal_padding_left);
            int tbPadding = 1; //top and bottom padding
            mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
            if (simId == ContactsUtils.CALL_TYPE_SIP) {
                mSimName.setBackgroundResource(R.drawable.sim_dark_internet_call);
                mSimName.setText(R.string.call_sipcall);
                mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
            } else if (ContactsUtils.CALL_TYPE_NONE == simId) {
                mSimName.setVisibility(View.INVISIBLE);
            } else {
                String simName = SIMInfoWrapper.getDefault().getSimDisplayNameById(simId);
                if (null != simName) {
                    mSimName.setText(simName);
                    mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
                } else {
                    mSimName.setVisibility(View.INVISIBLE);
                }
                int color = SIMInfoWrapper.getDefault().getInsertedSimColorById(simId);
                int simColorResId = SIMInfoWrapper.getDefault().getSimBackgroundDarkResByColorId(color);
                if (-1 != color) {
                    mSimName.setBackgroundResource(simColorResId);
                    mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
                } else {
                    mSimName.setBackgroundResource(R.drawable.sim_dark_not_activated);
                    mSimName.setPadding(lPadding, tbPadding, rPadding, tbPadding);
                }
            }
        /*} else {
            mSimName.setVisibility(View.GONE);
        }*/
    }
    private void log(final String msg) {
        if (true) {
            Log.d(TAG, msg);
        }
    }

    /** @ }*/

    /** M: use Proximitywakelock to switch screen on/off when sensor states change @{ */
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mProximityWakeLock;

    private void createProximityWakeLock() {
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // Wake lock used to control proximity sensor behavior.
        if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
        } else {
            Log.w(TAG, "mProximityWakeLock create failed!");
        }
    }
    /**@}*/
}
