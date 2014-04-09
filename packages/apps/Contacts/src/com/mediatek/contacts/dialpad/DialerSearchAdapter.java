package com.mediatek.contacts.dialpad;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.DialerSearch;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.CallDetailActivity;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.calllog.PhoneNumberHelper;
import com.android.contacts.ext.ContactPluginDefault;
import com.android.contacts.util.Constants;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.PhoneConstants;

import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.calllog.CallLogSimInfoHelper;
import com.mediatek.contacts.calloption.ContactsCallOptionHandler;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.widget.QuickContactBadgeWithPhoneNumber;
import com.mediatek.phone.HyphonManager;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.telephony.TelephonyManagerEx;

import java.util.ArrayList;
import java.util.HashMap;

public class DialerSearchAdapter extends CursorAdapter implements OnClickListener, View.OnTouchListener {

    public interface Listener {
        void onListViewItemClicked(final String number);
    }

    private static final String TAG = "DialerSearchAdapter";

    public static final int VIEW_TYPE_COUNT = 1;

    public static final int VIEW_TYPE_UNKNOWN = -1;
    public static final int VIEW_TYPE_CONTACT = 0;
    public static final int VIEW_TYPE_CALL_LOG = 1;
    public static final int VIEW_TYPE_CONTACT_CALL_LOG = 2;

    public static final int NUMBER_TYPE_NORMAL = 0;
    public static final int NUMBER_TYPE_UNKNOWN = 1;
    public static final int NUMBER_TYPE_VOICEMAIL = 2;
    public static final int NUMBER_TYPE_PRIVATE = 3;
    public static final int NUMBER_TYPE_PAYPHONE = 4;
    public static final int NUMBER_TYPE_EMERGENCY = 5;

    public static final int NAME_LOOKUP_ID_INDEX        = 0;
    public static final int CONTACT_ID_INDEX            = 1;
    public static final int CALL_LOG_DATE_INDEX         = 2;
    public static final int CALL_LOG_ID_INDEX           = 3;
    public static final int CALL_TYPE_INDEX             = 4;
    public static final int CALL_GEOCODED_LOCATION_INDEX = 5;
    public static final int SIM_ID_INDEX                = 6;
    public static final int VTCALL                      = 7;
    public static final int INDICATE_PHONE_SIM_INDEX    = 8;
    public static final int CONTACT_STARRED_INDEX       = 9;
    public static final int PHOTO_ID_INDEX              = 10;
    public static final int SEARCH_PHONE_TYPE_INDEX     = 11;
    public static final int NAME_INDEX                  = 12;
    public static final int SEARCH_PHONE_NUMBER_INDEX   = 13;
    public static final int CONTACT_NAME_LOOKUP_INDEX   = 14;
    public static final int IS_SDN_CONTACT              = 15;
    public static final int DS_MATCHED_DATA_OFFSETS     = 16;
    public static final int DS_MATCHED_NAME_OFFSETS     = 17;
    
    
    public static final int DS_MATCHED_DATA_DIVIDER     = 3;

    protected Context mContext;

    protected String mVoiceMailNumber;
    protected String mVoiceMailNumber2;

    protected String mEmergency;
    protected String mVoiceMail;
    protected String mUnknownNumber;
    protected String mPrivateNumber;
    protected String mPayphoneNumber;

    protected int mSpanColorFg;
    protected int mSpanColorBg;

    protected ContactPhotoManager mContactPhotoManager;
    protected CallLogSimInfoHelper mCallLogSimInfoHelper;

    protected Drawable[] mCallTypeDrawables = new Drawable[6];
    protected Drawable[] mVideoCallTypeDrawables = new Drawable[6];

    protected HashMap<String, String> mSpecialNumberMaps = new HashMap<String, String>();
    protected HashMap<String, Integer> mNumberTypeMaps = new HashMap<String, Integer>();

    protected Cursor mDialerSearchCursor;
    protected DisplayMetrics mDisplayMetrics;

    protected int mOperatorVerticalPadding;
    protected int mOperatorHorizontalPaddingRight;
    protected int mOperatorHorizontalPaddingLeft;

    HyphonManager mHyphonManager;
    ContactsCallOptionHandler mCallOptionHandler;
    private boolean mNeedClearDigits = false;

    private Listener mListener;
    
    private ImageView mCallView;
    private QuickContactBadgeWithPhoneNumber mQuickView;
    private boolean mHitDownEvent;

    protected PhoneNumberHelper mPhoneNumberHelper;

    public DialerSearchAdapter(Context context, Listener listener) {
        super(context, null, false);
        mContext = context;
        mListener = listener;

        mEmergency = mContext.getResources().getString(R.string.emergencycall);
        mVoiceMail = mContext.getResources().getString(R.string.voicemail);
        mPrivateNumber = mContext.getResources().getString(R.string.private_num);
        mPayphoneNumber = mContext.getResources().getString(R.string.payphone);
        mUnknownNumber = mContext.getResources().getString(R.string.unknown);

        mSpanColorFg = Color.WHITE;
        mSpanColorBg = Color.parseColor("#39caff");
        if (FeatureOption.MTK_THEMEMANAGER_APP) {
            int textColor = mContext.getResources().getThemeMainColor();
            if (textColor != 0) {
                mSpanColorBg = textColor;
            }
        }

        // 1. incoming 2. outgoing 3. missed 4.VVM 5. auto
        // rejected(Calls.AUTOREJECTED_TYPE)
        mCallTypeDrawables[1] = mContext.getResources().getDrawable(
                R.drawable.ic_call_incoming_holo_dark);
        mCallTypeDrawables[2] = mContext.getResources().getDrawable(
                R.drawable.ic_call_outgoing_holo_dark);
        mCallTypeDrawables[3] = mContext.getResources().getDrawable(
                R.drawable.ic_call_missed_holo_dark);
        /**M: [VVM] set voice mail icon color red.*/
        mCallTypeDrawables[4] = mContext.getResources().getDrawable(
                R.drawable.ic_call_voicemail_holo_dark_red);
        mCallTypeDrawables[5] = mContext.getResources().getDrawable(
                R.drawable.ic_call_autorejected_holo_dark);

        mVideoCallTypeDrawables[1] = mContext.getResources().getDrawable(
                R.drawable.ic_video_call_incoming_holo_dark);
        mVideoCallTypeDrawables[2] = mContext.getResources().getDrawable(
                R.drawable.ic_video_call_outgoing_holo_dark);
        mVideoCallTypeDrawables[3] = mContext.getResources().getDrawable(
                R.drawable.ic_video_call_missed_holo_dark);
        /**M: [VVM] set voice mail icon color red.*/
        mVideoCallTypeDrawables[4] = mContext.getResources().getDrawable(
                R.drawable.ic_call_voicemail_holo_dark_red);
        mVideoCallTypeDrawables[5] = mContext.getResources().getDrawable(
                R.drawable.ic_video_call_autorejected_holo_dark);

        mSpecialNumberMaps.put(CallerInfo.UNKNOWN_NUMBER, CallerInfo.UNKNOWN_NUMBER);
        mSpecialNumberMaps.put(CallerInfo.PRIVATE_NUMBER, CallerInfo.PRIVATE_NUMBER);
        mSpecialNumberMaps.put(CallerInfo.PAYPHONE_NUMBER, CallerInfo.PAYPHONE_NUMBER);

        mContactPhotoManager = ContactPhotoManager.getInstance(context);
        mCallLogSimInfoHelper = new CallLogSimInfoHelper(mContext.getResources());

        final Resources r = mContext.getResources();
        mOperatorVerticalPadding = 0;
        mOperatorHorizontalPaddingRight = r
                .getDimensionPixelSize(R.dimen.dialpad_operator_horizontal_padding_right);
        mOperatorHorizontalPaddingLeft = r
                .getDimensionPixelSize(R.dimen.dialpad_operator_horizontal_padding_left);

        mHyphonManager = HyphonManager.getInstance();

        mPhoneNumberHelper = new PhoneNumberHelper(mContext.getResources());
    }

    public DialerSearchAdapter(Context context, Listener listener,
            ContactsCallOptionHandler callOptionHandler) {
        this(context, listener);
        mCallOptionHandler = callOptionHandler;
    }

    public void onPause() {
        //
    }

    public void onResume() {
        SlotUtils.updateVoiceMailNumber();
        // when low memory clear the thumbnails, set the default photo before load complete 
        if (getCursor() != null) {
            notifyDataSetChanged();
        }
    }

    void log(String msg) {
        Log.d(TAG, msg);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        log("getView position: " + position + " convertView: " + convertView + " parent:" + parent);
        if (!mDialerSearchCursor.moveToPosition(position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }
        if (convertView == null) {
            v = newView(mContext, mDialerSearchCursor, parent);
        } else {
            v = convertView;
        }
        bindView(v, mContext, mDialerSearchCursor);
        return v;
    }

    public void bindView(View view, Context context, Cursor cursor) {
        log("+bindView");
        final int viewType = getViewType(cursor);
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        switch (viewType) {
            case VIEW_TYPE_CONTACT:
                bindContactView(view, context, cursor);
                viewHolder.viewType = VIEW_TYPE_CONTACT;
                break;
            case VIEW_TYPE_CALL_LOG:
                bindCallLogView(view, context, cursor);
                viewHolder.viewType = VIEW_TYPE_CALL_LOG;
                break;
            case VIEW_TYPE_CONTACT_CALL_LOG:
                bindContactCallLogView(view, context, cursor);
                viewHolder.viewType = VIEW_TYPE_CONTACT_CALL_LOG;
                break;
            default:
                break;
        }
        view.setOnClickListener(this);
        view.setOnTouchListener(this);
        log("-bindView");
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        log("+newView");
        View view = View.inflate(context, R.layout.dialer_search_item_view, null);

        ViewHolder viewHolder = createViewHolder();
        viewHolder.name = (TextView) view.findViewById(R.id.name);
        viewHolder.labelAndNumber = (TextView) view.findViewById(R.id.labelAndNumber);
        viewHolder.type = (ImageView) view.findViewById(R.id.callType);
        viewHolder.operator = (TextView) view.findViewById(R.id.operator);
        viewHolder.date = (TextView) view.findViewById(R.id.date);
        viewHolder.call = (ImageButton) view.findViewById(R.id.call);
        mCallView = viewHolder.call;
        viewHolder.photo = (QuickContactBadgeWithPhoneNumber) view
                .findViewById(R.id.quick_contact_photo);
        mQuickView = viewHolder.photo;
        viewHolder.divider = (View) view.findViewById(R.id.divider);
        view.setTag(viewHolder);
        log("-newView");

        return view;
    }
    
    public boolean onTouch(View v, MotionEvent event) {
        if ((event.isTouchEvent()) && (MotionEvent.ACTION_DOWN == event.getAction())) {
            int leftSide = -1;
            int rightSide = -1;
            
            float ix = event.getX();
            
            if (mCallView != null) {
                rightSide = mCallView.getLeft();
            }
            
            if (mQuickView != null) {
                leftSide = mQuickView.getRight();
            }
            
            if ((rightSide < 0 || rightSide == 0 || ix < rightSide)
                    && (leftSide < 0 || ix > leftSide)) {
                //return v.onTouchEvent(event);
                mHitDownEvent = true;
                return false;
            } else {
                mHitDownEvent = false;
                return true;
            }
        }
        
        return !mHitDownEvent;
    }

    public void setResultCursor(Cursor cursor) {
        if (mDialerSearchCursor != null) {
            mDialerSearchCursor.close();
        }
        mDialerSearchCursor = cursor;
    }

    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    public int getViewType(Cursor cursor) {
        int retval = VIEW_TYPE_UNKNOWN;
        final int contactId = cursor.getInt(CONTACT_ID_INDEX);
        final int callLogId = cursor.getInt(CALL_LOG_ID_INDEX);

        if (contactId > 0 && callLogId > 0) {
            retval = VIEW_TYPE_CONTACT_CALL_LOG;
        } else if (contactId > 0) {
            retval = VIEW_TYPE_CONTACT;
        } else if (callLogId > 0) {
            retval = VIEW_TYPE_CALL_LOG;
        }

        return retval;
    }

    protected ViewHolder createViewHolder() {
        final ViewHolder viewHolder = new ViewHolder();
        return viewHolder;
    }

    protected String getNumberHighlight(Cursor cursor) {
        final int index = cursor.getColumnIndex(DialerSearch.MATCHED_DATA_OFFSETS);
        return index != -1 ? cursor.getString(index) : null;
    }

    protected String getNameHighlight(Cursor cursor) {
        final int index = cursor.getColumnIndex(DialerSearch.MATCHED_NAME_OFFSETS);
        return index != -1 ? cursor.getString(index) : null;
    }

    /**
     * get the number type based on the number and SIM id
     * @param number
     * @param id
     * @return number type constants
     */
    protected int getNumberType(String number, int id) {
        int type = NUMBER_TYPE_NORMAL;
        if (TextUtils.isEmpty(number)) {
            return type;
        }

        if (number.equals(CallerInfo.UNKNOWN_NUMBER)) {
            type = NUMBER_TYPE_UNKNOWN;
        } else if (number.equals(CallerInfo.PRIVATE_NUMBER)) {
            type = NUMBER_TYPE_PRIVATE;
        } else if (number.equals(CallerInfo.PAYPHONE_NUMBER)) {
            type = NUMBER_TYPE_PAYPHONE;
        } else if (PhoneNumberUtils.isEmergencyNumber(number)) {
            type = NUMBER_TYPE_EMERGENCY;
        } else {
            if (PhoneNumberHelper.isSimVoiceMailNumber(number, id)) {
                return NUMBER_TYPE_VOICEMAIL;
            }
            type = NUMBER_TYPE_NORMAL;
        }
        return type;
    }

    protected boolean isSpecialNumber(int type) {
        return type != NUMBER_TYPE_NORMAL;
    }

    protected SpannableStringBuilder highlightString(String highlight, String target) {
        SpannableStringBuilder style = new SpannableStringBuilder(target);
        int length = highlight.length();
        for (int i = 0; i < length; i += DS_MATCHED_DATA_DIVIDER) {
            if (((int) highlight.charAt(i)) > style.length()
                    || ((int) highlight.charAt(i + 1) + 1) > style.length()) {
                break;
            }
            style.setSpan(new ForegroundColorSpan(mSpanColorBg), (int) highlight.charAt(i),
                    (int) highlight.charAt(i + 1) + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return style;
    }

    protected SpannableStringBuilder highlightHyphon(String highlight, String target, String origin) {
        SpannableStringBuilder style = new SpannableStringBuilder(target);
        ArrayList<Integer> numberHighlightOffset = DialerSearchUtils
                .adjustHighlitePositionForHyphen(target, highlight, origin);        
        if (numberHighlightOffset != null) {
            style.setSpan(new ForegroundColorSpan(mSpanColorBg),
                    numberHighlightOffset.get(0),
                    numberHighlightOffset.get(1) + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return style;
    }

//    protected String formatNumber(String number, int type) {
//        switch (type) {
//            case NUMBER_TYPE_UNKNOWN:
//                return mUnknownNumber;
//            case NUMBER_TYPE_PRIVATE:
//                return mPrivateNumber;
//            case NUMBER_TYPE_PAYPHONE:
//                return mPayphoneNumber;
//            case NUMBER_TYPE_VOICEMAIL:
//                return mVoiceMail;
//        }
//        return PhoneNumberUtils.formatNumber(number);
//    }

//    protected String formatNumber(String number) {
//        if (TextUtils.isEmpty(number))
//            return number;
//
//        String specialNumber = mSpecialNumberMaps.get(number);
//        if (specialNumber != null)
//            return specialNumber;
//        return PhoneNumberUtils.formatNumber(number);
//    }

    protected Intent newItemClickIntent(View view) {
        Intent intent = null;
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        switch (viewHolder.viewType) {
            case VIEW_TYPE_CONTACT_CALL_LOG:
            case VIEW_TYPE_CALL_LOG:
                intent = new Intent(mContext, CallDetailActivity.class);
                intent.setData(ContentUris.withAppendedId(Calls.CONTENT_URI_WITH_VOICEMAIL,
                        viewHolder.callId));

                break;

            case VIEW_TYPE_CONTACT:
                final Uri uri = viewHolder.contactUri;
                if (uri != null) {
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                }

                break;
            default:
                break;
        }
        return intent;
    } 

    protected Uri getContactUri(Cursor cursor) {
        final String lookup = cursor.getString(CONTACT_NAME_LOOKUP_INDEX);
        final int contactId = cursor.getInt(CONTACT_ID_INDEX);
        return Contacts.getLookupUri(contactId, lookup);
    }

    public void bindContactView(View view, Context context, Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.type.setVisibility(View.GONE);
        viewHolder.operator.setVisibility(View.GONE);
        viewHolder.date.setVisibility(View.GONE);
        viewHolder.labelAndNumber.setVisibility(View.VISIBLE);

        final String number = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
        final int labelType = cursor.getInt(SEARCH_PHONE_TYPE_INDEX);
        int indicate = cursor.getInt(INDICATE_PHONE_SIM_INDEX);
        final int simId = cursor.getInt(SIM_ID_INDEX);
        final int numberType = getNumberType(number, simId);
        ///M: to fix number display order problem in Dialpad in Arabic
        final String formatNumber = numberLeftToRight(mHyphonManager.formatNumber(number));
        final String name = cursor.getString(NAME_INDEX);

        CharSequence label = CommonDataKinds.Phone.getTypeLabel(context.getResources(), labelType,
                null);

        /** M:AAS @ } */
        final int slotId = SIMInfoWrapper.getDefault().getSimSlotById(indicate);
        label = ExtensionManager.getInstance().getContactAccountExtension().getTypeLabel(context.getResources(),
                labelType, null, slotId, ExtensionManager.COMMD_FOR_AAS);
        /** M: @ } */

        log("name = " + name + " number = " + number + " label = " + label);

        viewHolder.contactUri = getContactUri(cursor);

        long photoId = cursor.getLong(PHOTO_ID_INDEX);
        if (indicate > 0) {
            int isSdnContact = cursor.getInt(IS_SDN_CONTACT);
            photoId = DialerSearchUtils.getSimType(indicate, isSdnContact);
        }
        if (numberType == NUMBER_TYPE_UNKNOWN) {
            viewHolder.call.setVisibility(View.GONE);
            viewHolder.divider.setVisibility(View.GONE);
        } else {
            viewHolder.call.setVisibility(View.VISIBLE);
            viewHolder.divider.setVisibility(View.VISIBLE);
        }
        if (numberType == NUMBER_TYPE_VOICEMAIL || numberType == NUMBER_TYPE_EMERGENCY) {
            photoId = 0;
            viewHolder.photo.assignPhoneNumber(null, false);
            viewHolder.photo.assignContactUri(null);
        } else {
            viewHolder.photo.assignPhoneNumber(null, false);
            viewHolder.photo.assignContactUri(viewHolder.contactUri);
        }

        mContactPhotoManager.loadThumbnail(viewHolder.photo, photoId, true);

        if (isSpecialNumber(numberType)) {
            if (numberType == NUMBER_TYPE_VOICEMAIL || numberType == NUMBER_TYPE_EMERGENCY) {
                if (numberType == NUMBER_TYPE_VOICEMAIL) {
                    viewHolder.name.setText(mVoiceMail);
                } else {
                    viewHolder.name.setText(mEmergency);
                }

                viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
                String highlight = getNumberHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightHyphon(highlight, formatNumber, number);
                    viewHolder.labelAndNumber.setText(style);
                } else {
                    viewHolder.labelAndNumber.setText(formatNumber);
                }
            } else {
                final String convert = specialNumberToString(numberType);
                if (TextUtils.isEmpty(convert)) {
                    viewHolder.name.setText(convert);
                } else {
                    viewHolder.name.setText(convert);
                }
            }
        } else {
            // empty name ?
            if (!TextUtils.isEmpty(name)) {
                // highlight name
                String highlight = getNameHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightString(highlight, name);
                    viewHolder.name.setText(style);
                } else {
                    viewHolder.name.setText(name);
                }

                // highlight number
                if (!TextUtils.isEmpty(formatNumber)) {
                    highlight = getNumberHighlight(cursor);
                    if (!TextUtils.isEmpty(highlight)) {
                        SpannableStringBuilder style = highlightHyphon(highlight, formatNumber,
                                number);
                        setLabelAndNumber(viewHolder.labelAndNumber, label, style);
                    } else {
                        setLabelAndNumber(viewHolder.labelAndNumber, label, formatNumber);
                    }
                }
            } else {
                viewHolder.labelAndNumber.setVisibility(View.GONE);

                // highlight number and set number to name text view
                if (!TextUtils.isEmpty(formatNumber)) {
                    final String highlight = getNumberHighlight(cursor);
                    if (!TextUtils.isEmpty(highlight)) {
                        SpannableStringBuilder style = highlightHyphon(highlight, formatNumber,
                                number);
                        viewHolder.name.setText(style);
                    } else {
                        viewHolder.name.setText(formatNumber);
                    }
                } else {
                    viewHolder.name.setVisibility(View.GONE);
                }
            }
        }

        CallInfo callInfo = new CallInfo();
        callInfo.id = Settings.System.DEFAULT_SIM_NOT_SET;
        callInfo.number = number;
        viewHolder.call.setTag(callInfo);
        viewHolder.call.setOnClickListener(this);
    }

    public void bindCallLogView(View view, Context context, Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.type.setVisibility(View.VISIBLE);
        viewHolder.operator.setVisibility(View.VISIBLE);
        viewHolder.date.setVisibility(View.VISIBLE);
        viewHolder.labelAndNumber.setVisibility(View.VISIBLE);

        final String number = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
        final int type = cursor.getInt(CALL_TYPE_INDEX);
        final int videoCall = cursor.getInt(VTCALL);
        final int simId = cursor.getInt(SIM_ID_INDEX);
        final int numberType = getNumberType(number, simId);
        long date = cursor.getLong(CALL_LOG_DATE_INDEX);
        int indicate = cursor.getInt(INDICATE_PHONE_SIM_INDEX);
        String geocode = cursor.getString(CALL_GEOCODED_LOCATION_INDEX);

        viewHolder.callId = cursor.getInt(CALL_LOG_ID_INDEX);

        log("type = " + type + " videoCall = " + videoCall + " simId = " + simId + "number = "
                + number + "geocode = " + geocode);

        ///M: to fix number display order problem in Dialpad in Arabic
        final String formatNumber = numberLeftToRight(mHyphonManager.formatNumber(number));

        log("formatNumber = " + formatNumber);

        if (numberType == NUMBER_TYPE_UNKNOWN) {
            viewHolder.call.setVisibility(View.GONE);
            viewHolder.divider.setVisibility(View.GONE);
        } else {
            viewHolder.call.setVisibility(View.VISIBLE);
            viewHolder.divider.setVisibility(View.VISIBLE);
        }

        long photoId = cursor.getLong(PHOTO_ID_INDEX);
        if (indicate > 0) {
            int isSdnContact = cursor.getInt(IS_SDN_CONTACT);
            photoId = DialerSearchUtils.getSimType(indicate, isSdnContact);
        }

        viewHolder.photo.assignContactUri(null);
        viewHolder.photo.assignPhoneNumber(number, mPhoneNumberHelper.isSipNumber(number));
        mContactPhotoManager.loadThumbnail(viewHolder.photo, photoId, true);

        if (TextUtils.isEmpty(geocode)) {
            geocode = mContext.getResources().getString(R.string.call_log_empty_gecode);
        }

        viewHolder.labelAndNumber.setText(geocode);

        String highlight;
        if (isSpecialNumber(numberType)) {
            if (numberType == NUMBER_TYPE_VOICEMAIL || numberType == NUMBER_TYPE_EMERGENCY) {
                if (numberType == NUMBER_TYPE_VOICEMAIL) {
                    viewHolder.name.setText(mVoiceMail);
                } else {
                    viewHolder.name.setText(mEmergency);
                }

                viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
                highlight = getNumberHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightHyphon(highlight, formatNumber, number);
                    viewHolder.labelAndNumber.setText(style);
                } else {
                    viewHolder.labelAndNumber.setText(formatNumber);
                }
            } else {
                final String convert = specialNumberToString(numberType);
                if (TextUtils.isEmpty(convert)) {
                    viewHolder.name.setText(convert);
                } else {
                    viewHolder.name.setText(convert);
                }
            }
        } else {
            if (!TextUtils.isEmpty(formatNumber)) {
                highlight = getNumberHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightHyphon(highlight, formatNumber, number);
                    viewHolder.name.setText(style);
                } else {
                    viewHolder.name.setText(formatNumber);
                }
            }
        }

        java.text.DateFormat dateFormat = DateFormat.getTimeFormat(mContext);
        String dateString = dateFormat.format(date);
        final String display = mCallLogSimInfoHelper.getSimDisplayNameById(simId);
        viewHolder.date.setText(dateString);

        Drawable[] callTypeDrawables = videoCall == 1 ? mVideoCallTypeDrawables
                : mCallTypeDrawables;
         viewHolder.type.setImageDrawable(callTypeDrawables[type]);

        if (!TextUtils.isEmpty(display)) {
            viewHolder.operator.setText(display);
            viewHolder.operator.setBackgroundDrawable(mCallLogSimInfoHelper
                    .getSimColorDrawableById(simId));
            viewHolder.operator.setPadding(mOperatorHorizontalPaddingLeft, mOperatorVerticalPadding,
                    mOperatorHorizontalPaddingRight, mOperatorVerticalPadding);
            viewHolder.operator.setGravity(Gravity.CENTER_VERTICAL);
        } else {
            viewHolder.operator.setVisibility(View.GONE);
        }

        CallInfo callInfo = new CallInfo();
        callInfo.number = number;
        callInfo.id = simId;
        viewHolder.call.setTag(callInfo);
        viewHolder.call.setOnClickListener(this);
    }

    public void bindContactCallLogView(View view, Context context, Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.labelAndNumber.setVisibility(View.VISIBLE);

        // bind : name/number/photo/call
        bindContactView(view, context, cursor);

        // the operator/date/type will be makred invisible
        // in the bindContactView method, so reset their
        // visibility here
        viewHolder.operator.setVisibility(View.VISIBLE);
        viewHolder.date.setVisibility(View.VISIBLE);
        viewHolder.type.setVisibility(View.VISIBLE);

        final int type = cursor.getInt(CALL_TYPE_INDEX);
        final int videoCall = cursor.getInt(VTCALL);
        final int simId = cursor.getInt(SIM_ID_INDEX);
        final String number = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
        long date = cursor.getLong(CALL_LOG_DATE_INDEX);

        viewHolder.callId = cursor.getInt(CALL_LOG_ID_INDEX);

        log("bindContactCallLogView type = " + type + " videoCall = " + videoCall + " simId = "
                + simId);

        // hide the label when it's a uri number
        /*
         * if(PhoneNumberUtils.isUriNumber(number))
         * viewHolder.label.setVisibility(View.GONE);
         */

        java.text.DateFormat dateFormat = DateFormat.getTimeFormat(mContext);
        String dateString = dateFormat.format(date);
        viewHolder.date.setText(dateString);

        // MTK81281 modify for Cr:ALPS00115140 start
        // copy bindCallLogView's solution
        // viewHolder.type.setImageDrawable(mCallTypeDrawables[type]);
        Drawable[] callTypeDrawables = videoCall == 1 ? mVideoCallTypeDrawables
                : mCallTypeDrawables;
        viewHolder.type.setImageDrawable(callTypeDrawables[type]);
        // MTK81281 modify for Cr:ALPS00115140 end

        final String display = mCallLogSimInfoHelper.getSimDisplayNameById(simId);
        if (!TextUtils.isEmpty(display)) {
            viewHolder.operator.setText(display);
            viewHolder.operator.setBackgroundDrawable(mCallLogSimInfoHelper
                    .getSimColorDrawableById(simId));
            viewHolder.operator.setPadding(mOperatorHorizontalPaddingLeft, mOperatorVerticalPadding,
                    mOperatorHorizontalPaddingRight, mOperatorVerticalPadding);
            viewHolder.operator.setGravity(Gravity.CENTER_VERTICAL);
        } else {
            viewHolder.operator.setVisibility(View.GONE);
        }

        CallInfo callInfo = new CallInfo();
        callInfo.id = simId;
        callInfo.number = number;
        viewHolder.call.setTag(callInfo);
        viewHolder.call.setOnClickListener(this);
    }

    String specialNumberToString(int type) {
        switch (type) {
            case NUMBER_TYPE_UNKNOWN:
                return mUnknownNumber;
            case NUMBER_TYPE_PRIVATE:
                return mPrivateNumber;
            case NUMBER_TYPE_PAYPHONE:
                return mPayphoneNumber;
            default:
                break;
        }
        return null;
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            mContactPhotoManager.pause();
        } else {
            mContactPhotoManager.resume();
        }
    }

    protected void setLabelAndNumber(TextView view, CharSequence label, String number) {
        if (PhoneNumberUtils.isUriNumber(number)) {
            view.setText(number);
            return;
        }

        if (TextUtils.isEmpty(label)) {
            view.setText(number);
        } else if (TextUtils.isEmpty(number)) {
            view.setText(label);
        } else {
            view.setText(label + " " + number);
        }
    }

    protected void setLabelAndNumber(TextView view, CharSequence label,
            SpannableStringBuilder number) {
        if (PhoneNumberUtils.isUriNumber(number.toString())) {
            view.setText(number);
            return;
        }

        if (TextUtils.isEmpty(label)) {
            view.setText(number);
        } else if (TextUtils.isEmpty(number)) {
            view.setText(label);
        } else {
            number.insert(0, label + " ");
            view.setText(number);
        }
    }

    public class ViewHolder {
        public QuickContactBadgeWithPhoneNumber photo;
        public ImageButton call;
        public TextView name;
        public TextView labelAndNumber;
        public ImageView type;
        public TextView operator;
        public TextView date;
        public int callId;
        public int viewType;
        public Uri contactUri;
        public View divider;
    }

    public class CallInfo {
        public long id;
        public String number;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.call:
                final CallInfo callInfo = (CallInfo) v.getTag();
                final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri.fromParts(
                        "tel", callInfo.number, null));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Constants.EXTRA_ORIGINAL_SIM_ID, callInfo.id);
                if (mCallOptionHandler != null) {
                    mCallOptionHandler.doCallOptionHandle(intent);
                } else {
                    mContext.startActivity(intent);
                }

                break;

            case R.id.dialer_search_item_view:
                log("onClick(), view id = dialer_search_item_view");

                if (ExtensionManager.getInstance().getDialtactsExtension().startActivity(
                        ContactPluginDefault.COMMD_FOR_OP01)) {
                    mNeedClearDigits = true;
                    Intent intentForSearch = newItemClickIntent(v);
                    if (intentForSearch != null) {
                        mContext.startActivity(intentForSearch);
                    }
                } else {
                    ViewHolder viewHolder = (ViewHolder) v.getTag();
                    final CallInfo callInfoForSearch = (CallInfo) viewHolder.call.getTag();
                    if (null != callInfoForSearch) {
                        if (null != mListener) {
                            log("callinfo number = " + callInfoForSearch.number);
                            mListener.onListViewItemClicked(callInfoForSearch.number);
                        }
                    }
                }

                break;
            default:
                break;
        }
    }

    ///M: to fix number display order problem in Dialpad in Arabic/Hebrew/Urdu
    private String numberLeftToRight(String origin) {
        return TextUtils.isEmpty(origin) ? origin : '\u202D' + origin + '\u202C';
    }
    
    public boolean isDigitsCleared() {
        return mNeedClearDigits;
    }

    public void resetDigitsState() {
        mNeedClearDigits = false;
    }
}
