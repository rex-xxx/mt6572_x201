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

package com.android.ex.chips;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.text.style.ImageSpan;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Patterns;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

/// M:
import android.view.Gravity;
import android.telephony.PhoneNumberUtils;
import android.text.SpanWatcher;
import android.content.res.Configuration;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.text.BoringLayout;
import java.lang.Enum;
import android.os.Parcel;
import android.os.Parcelable;
import android.net.Uri;

/**
 * RecipientEditTextView is an auto complete text view for use with applications
 * that use the new Chips UI for addressing a message to recipients.
 */
public class MTKRecipientEditTextView extends MultiAutoCompleteTextView implements
        OnItemClickListener, Callback, RecipientAlternatesAdapter.OnCheckedItemChangedListener,
        GestureDetector.OnGestureListener, OnDismissListener, OnClickListener,
        TextView.OnEditorActionListener {

    private static final char COMMIT_CHAR_COMMA = ',';

    private static final char NAME_WRAPPER_CHAR = '"';

    private static final char COMMIT_CHAR_SEMICOLON = ';';

    private static final char COMMIT_CHAR_CHINESE_COMMA = '\uFF0C';  /// M: Support chinese comma as seperator

    private static final char COMMIT_CHAR_CHINESE_SEMICOLON = '\uFF1B';  /// M: Support chinese semicolon as seperator

    private static final char COMMIT_CHAR_SPACE = ' ';

    private static final String TAG = "RecipientEditTextView";

    private static int DISMISS = "dismiss".hashCode();

    private static final long DISMISS_DELAY = 300;

    // TODO: get correct number/ algorithm from with UX.
    // Visible for testing.
    /*package*/ static final int CHIP_LIMIT = 2;

    private static final int MAX_CHIPS_PARSED = 100;

    private static int sSelectedTextColor = -1;

    // Resources for displaying chips.
    private Drawable mChipBackground = null;

    private Drawable mChipDelete = null;

    private Drawable mInvalidChipBackground;

    private Drawable mChipBackgroundPressed;

    private float mChipHeight;

    private float mChipFontSize;

    private float mLineSpacingExtra;

    private int mChipPadding;

    private Tokenizer mTokenizer;

    private Validator mValidator;

    private RecipientChip mSelectedChip;

    private int mAlternatesLayout;

    private Bitmap mDefaultContactPhoto;

    private ImageSpan mMoreChip;

    private TextView mMoreItem;

    private final ArrayList<String> mPendingChips = new ArrayList<String>();

    private Handler mHandler;

    private int mPendingChipsCount = 0;

    private boolean mNoChips = false;

    private ListPopupWindow mAlternatesPopup;

    private ListPopupWindow mAddressPopup;

    private ArrayList<RecipientChip> mTemporaryRecipients;

    private ArrayList<RecipientChip> mRemovedSpans;

    private boolean mShouldShrink = true;

    // Chip copy fields.
    private GestureDetector mGestureDetector;

    private Dialog mCopyDialog;

    private String mCopyAddress;

    /**
     * Used with {@link #mAlternatesPopup}. Handles clicks to alternate addresses for a
     * selected chip.
     */
    private OnItemClickListener mAlternatesListener;

    private int mCheckedItem;

    private TextWatcher mTextWatcher;

    // Obtain the enclosing scroll view, if it exists, so that the view can be
    // scrolled to show the last line of chips content.
    private ScrollView mScrollView;

    private boolean mTriedGettingScrollView;

    private boolean mDragEnabled = false;

    private final Runnable mAddTextWatcher = new Runnable() {
        @Override
        public void run() {
            if (mTextWatcher == null) {
                mTextWatcher = new RecipientTextWatcher();
                addTextChangedListener(mTextWatcher);
            }
        }
    };

    private IndividualReplacementTask mIndividualReplacements;

    private Runnable mHandlePendingChips = new Runnable() {

        @Override
        public void run() {
            handlePendingChips();
        }

    };

    private Runnable mDelayedShrink = new Runnable() {

        @Override
        public void run() {
            shrink();
        }

    };

    private int mMaxLines;

    public MTKRecipientEditTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChipDimensions(context, attrs);
        if (sSelectedTextColor == -1) {
            sSelectedTextColor = context.getResources().getColor(android.R.color.white);
        }
        mDefaultTextSize = getPaint().getTextSize();  // M: Save default size of text
        mAlternatesPopup = new ListPopupWindow(context);
        mAddressPopup = new ListPopupWindow(context);
        /// M: Get default vertical offset of AutoCompleteTextView which is used for adjusting position of popup window. @{
        TypedArray a =
            context.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.AutoCompleteTextView, com.android.internal.R.attr.autoCompleteTextViewStyle, 0);
        mDefaultVerticalOffset = (int)a.getDimension(com.android.internal.R.styleable.AutoCompleteTextView_dropDownVerticalOffset, 0.0f);
        /// @}
        mCopyDialog = new Dialog(context);
        mAlternatesListener = new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView,View view, int position,
                    long rowId) {
                setDisableBringPointIntoView(true);  /// M: Don't scroll view when click on the item in drop-down list 
                mAlternatesPopup.setOnItemClickListener(null);
                replaceChip(mSelectedChip, ((RecipientAlternatesAdapter) adapterView.getAdapter())
                        .getRecipientEntry(position));
                Message delayed = Message.obtain(mHandler, DISMISS);
                delayed.obj = mAlternatesPopup;
                mHandler.sendMessageDelayed(delayed, DISMISS_DELAY);
                clearComposingText();
            }
        };
        setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setOnItemClickListener(this);
        setCustomSelectionActionModeCallback(this);
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == DISMISS) {
                    ((ListPopupWindow) msg.obj).dismiss();
                    return;
                }
                super.handleMessage(msg);
            }
        };
        mTextWatcher = new RecipientTextWatcher();
        addTextChangedListener(mTextWatcher);
        mGestureDetector = new GestureDetector(context, this);
        setOnEditorActionListener(this);
        mMaxLines = getLineCount();
    }

    @Override
    public boolean onEditorAction(TextView view, int action, KeyEvent keyEvent) {
        if (action == EditorInfo.IME_ACTION_DONE) {
            if (commitDefault()) {
                return true;
            }
            if (mSelectedChip != null) {
                clearSelectedChip();
                return true;
            } else if (focusNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);
        int imeActions = outAttrs.imeOptions&EditorInfo.IME_MASK_ACTION;
        if ((imeActions&EditorInfo.IME_ACTION_DONE) != 0) {
            // clear the existing action
            outAttrs.imeOptions ^= imeActions;
            // set the DONE action
            outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
        }
        if ((outAttrs.imeOptions&EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }
        outAttrs.actionLabel = getContext().getString(R.string.done);
        return connection;
    }

    /*package*/ RecipientChip getLastChip() {
        RecipientChip last = null;
        RecipientChip[] chips = getSortedRecipients();
        if (chips != null && chips.length > 0) {
            last = chips[chips.length - 1];
        }
        return last;
    }

    @Override
    public void onSelectionChanged(int start, int end) {
        // When selection changes, see if it is inside the chips area.
        // If so, move the cursor back after the chips again.
        RecipientChip last = getLastChip();
        if (last != null && start < getSpannable().getSpanEnd(last)) {
            // Grab the last chip and set the cursor to after it.
            setSelection(Math.min(getSpannable().getSpanEnd(last) + 1, getText().length()));
        }
        /// M: Set selection to the end of whole text when chips to be parsed is over MAX_CHIPS_PARSED now or before & RecipientEditTextView is just been expanded. @{
        if (mNoChips && mJustExpanded) {
            Editable text = getText();
            setSelection(text != null && text.length() > 0 ? text.length() : 0);
            mJustExpanded = false;
        }
        /// @}
        super.onSelectionChanged(start, end);
    }

    /**
     * M: User interface state that is stored by RecipientEditTextView for implementing View.onSaveInstanceState.
     * @hide
     */
    public static class RecipientSavedState extends BaseSavedState {
        boolean frozenWithFocus;

        RecipientSavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(frozenWithFocus ? 1 : 0);
        }

        @Override
        public String toString() {
            String str = "RecipientEditTextView.RecipientSavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " frozenWithFocus=" + frozenWithFocus + ")";
            return str;
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<RecipientSavedState> CREATOR
                = new Parcelable.Creator<RecipientSavedState>() {
            public RecipientSavedState createFromParcel(Parcel in) {
                return new RecipientSavedState(in);
            }

            public RecipientSavedState[] newArray(int size) {
                return new RecipientSavedState[size];
            }
        };

        private RecipientSavedState(Parcel in) {
            super(in);
            frozenWithFocus = (in.readInt() != 0);
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Log.d(TAG,"[onRestoreInstanceState]");
        /// M: Modify to remove text content and append all addresses back. 
        ///    Because original onRestoreInstance didn't recover mTemporaryRecipients/mRemovedSpans/.... @{
        RecipientSavedState ss = (RecipientSavedState)state;
        boolean hasFocus = ss.frozenWithFocus;

        if (!TextUtils.isEmpty(getText())) {
            super.onRestoreInstanceState(null);
        } else {
            super.onRestoreInstanceState(ss.getSuperState());
        }

        Log.d(TAG,"[onRestore] Text->" + getText());

        /// M: System help us restore RecipientChip, we don't need to restore it by ourselves. @{
        boolean doRestore = true;
        if (hasFocus) {
            RecipientChip lastChip = getLastChip();
            if (lastChip != null) {
                doRestore = false;
            }
        }
        /// @}

        if (!TextUtils.isEmpty(getText()) && doRestore) {
            Log.d(TAG,"[onRestore] Do restore process");
            if (mTextWatcher != null) {
                removeTextChangedListener(mTextWatcher);
            }
            /// M: Process text content. @{
            String text = getText().toString();
            int textLen = text.length();
            getText().delete(0, textLen);
            MTKRecipientList recipientList = new MTKRecipientList();
            int x=0;
            int tokenStart = 0;
            int tokenEnd = 0;
            while((tokenEnd = mTokenizer.findTokenEnd(text, tokenStart)) < text.length()) {
                String destination = text.substring(tokenStart, tokenEnd);
                tokenStart = tokenEnd + 2;
                recipientList.addRecipient(tokenizeName(destination), isPhoneNumber(destination) ? destination : tokenizeAddress(destination));
                x++;
            }

            appendList(recipientList);

            if (tokenStart < tokenEnd) {
                String lastToken = text.substring(tokenStart, tokenEnd);
                if (recipientList.getRecipientCount() != 0) {
                    mStringToBeRestore = lastToken; /// M: Restore the text later
                } else {
                    getText().append(lastToken); /// M: Restore the text now
                }
            }
            /// @}
            mHandler.post(mAddTextWatcher);
        }
        /// @}
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Log.d(TAG,"[onSaveInstanceState]");
        // If the user changes orientation while they are editing, just roll back the selection.
        clearSelectedChip();
        /// M: Save the state whether RecipientEditTextView has focus now. @{
        Parcelable superState = super.onSaveInstanceState();
        RecipientSavedState ss = new RecipientSavedState(superState);
        if (isFocused()) {
            ss.frozenWithFocus = true;
        } else {
            ss.frozenWithFocus = false;
        }
        Log.d(TAG,"[onSave] Text ->" + getText());
        return ss;
        /// @}
    }

    /**
     * Convenience method: Append the specified text slice to the TextView's
     * display buffer, upgrading it to BufferType.EDITABLE if it was
     * not already editable. Commas are excluded as they are added automatically
     * by the view.
     */
    @Override
    public void append(CharSequence text, int start, int end) {
        /// M: Only do append instead of other procedures during batch processing of appending strings. @{
        if (mDuringAppendStrings) {
            Log.d(TAG, "[append] (mDuringAppendStrings) " + text);
            super.append(text, start, end);
            return;
        }
        Log.d(TAG, "[append] " + text);
        /// @}
        // We don't care about watching text changes while appending.
        if (mTextWatcher != null) {
            removeTextChangedListener(mTextWatcher);
        }
        /// M: Add to pendingStrings list to process in batch later
        mPendingStrings.add(text.toString());

        if (!TextUtils.isEmpty(text) && TextUtils.getTrimmedLength(text) > 0) {
            String displayString = text.toString();
            int separatorPos = displayString.lastIndexOf(COMMIT_CHAR_COMMA);
            // Verify that the separator pos is not within ""; if it is, look
            // past the closing quote. If there is no comma past ", this string
            // will resolve to an error chip.
            if (separatorPos > -1) {
                String parseDisplayString = displayString.substring(separatorPos);
                int endQuotedTextPos = parseDisplayString.indexOf(NAME_WRAPPER_CHAR);
                if (endQuotedTextPos > separatorPos) {
                    separatorPos = parseDisplayString.lastIndexOf(COMMIT_CHAR_COMMA,
                            endQuotedTextPos);
                }
            }
            if (!TextUtils.isEmpty(displayString)
                    && TextUtils.getTrimmedLength(displayString) > 0) {
                mPendingChipsCount++;
                mPendingChips.add(text.toString());
            }
        }
        // Put a message on the queue to make sure we ALWAYS handle pending
        // chips.
        if (mPendingChipsCount > 0) {
            postHandlePendingChips();
        }
        mHandler.post(mAddTextWatcher);
    }

    /// M:
    public void appendList(MTKRecipientList recipientList) {
        if ((recipientList == null) || (recipientList.getRecipientCount() <= 0)) {
            return;
        }

        int recipientCnt = recipientList.getRecipientCount();
        String str = "";
        for (int x=0; x<recipientCnt; x++) {
            MTKRecipient recipient = recipientList.getRecipient(x);
            str += recipient.getFormatString();
        }

        // We don't care about watching text changes while appending.
        if (mTextWatcher != null) {
            removeTextChangedListener(mTextWatcher);
        }

        mDuringAppendStrings = true;
        append(str, 0, str.length());
        mDuringAppendStrings = false;

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            for (int x=0; x<recipientCnt; x++) {
                MTKRecipient recipient = recipientList.getRecipient(x);
                Log.d(TAG,"[appendList] Recipient -> Name = " + recipient.getDisplayName() + " & Dest = " + recipient.getDestination());
            }
        }

        for (int x=0; x<recipientCnt; x++) {
            /// Original manipulation in append. @{
            MTKRecipient recipient = recipientList.getRecipient(x);
            String text = recipient.getFormatString();
            if (!TextUtils.isEmpty(text) && TextUtils.getTrimmedLength(text) > 0) {
                Log.d(TAG,"[appendList] perText= "+text);
                String displayString = text.toString();
                int separatorPos = displayString.lastIndexOf(COMMIT_CHAR_COMMA);
                // Verify that the separator pos is not within ""; if it is, look
                // past the closing quote. If there is no comma past ", this string
                // will resolve to an error chip.
                if (separatorPos > -1) {
                    String parseDisplayString = displayString.substring(separatorPos);
                    int endQuotedTextPos = parseDisplayString.indexOf(NAME_WRAPPER_CHAR);
                    if (endQuotedTextPos > separatorPos) {
                        separatorPos = parseDisplayString.lastIndexOf(COMMIT_CHAR_COMMA,
                                endQuotedTextPos);
                    }
                }
                if (!TextUtils.isEmpty(displayString)
                        && TextUtils.getTrimmedLength(displayString) > 0) {
                    mPendingChipsCount++;
                    mPendingChips.add(text.toString());
                }
            }
            /// @}
        }
        // Put a message on the queue to make sure we ALWAYS handle pending
        // chips.
        if (mPendingChipsCount > 0) {
            postHandlePendingChips();
        }
        mHandler.post(mAddTextWatcher);
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);
        if (!hasFocus) {
            shrink();
        } else {
            expand();
        }
    }

    @Override
    public void performValidation() {
        // Do nothing. Chips handles its own validation.
    }

    private void shrink() {
        if (mTokenizer == null) {
            return;
        }
        /// M: When chips to be parsed is over MAX_CHIPS_PARSED now or before, we just need to create moreChip (+XX) instead of executing other procedures. @{
        if (mNoChips) {
            createMoreChip();
            return;
        }
        /// @}
        long contactId = mSelectedChip != null ? mSelectedChip.getEntry().getContactId() : -1;
        if (mSelectedChip != null && contactId != RecipientEntry.INVALID_CONTACT
                && ((!isPhoneQuery() && contactId != RecipientEntry.GENERATED_CONTACT) || isPhoneQuery())) { /// M: When shrink, also clear selectedChip in phoneQuery.
            clearSelectedChip();
        } else {
            if (getWidth() <= 0) {
                // We don't have the width yet which means the view hasn't been drawn yet
                // and there is no reason to attempt to commit chips yet.
                // This focus lost must be the result of an orientation change
                // or an initial rendering.
                // Re-post the shrink for later.
                mHandler.removeCallbacks(mDelayedShrink);
                mHandler.post(mDelayedShrink);
                return;
            }
            // Reset any pending chips as they would have been handled
            // when the field lost focus.
            if (mPendingChipsCount > 0) {
                postHandlePendingChips();
            } else {
                Editable editable = getText();
                /// M: To judge wheather text just have blank.
                boolean textIsAllBlank = textIsAllBlank(editable);
                
                int end = getSelectionEnd();
                int start = mTokenizer.findTokenStart(editable, end);
                RecipientChip[] chips = getSpannable().getSpans(start, end, RecipientChip.class);
                if ((chips == null || chips.length == 0)) {
                    Editable text = getText();
                    int whatEnd = mTokenizer.findTokenEnd(text, start);
                    // This token was already tokenized, so skip past the ending token.
                    if (whatEnd < text.length() && text.charAt(whatEnd) == ',') {
                        whatEnd++;
                    }
                    // In the middle of chip; treat this as an edit
                    // and commit the whole token.
                    int selEnd = getSelectionEnd();
                    if (whatEnd != selEnd && !textIsAllBlank) { /// M: To judge wheather text just have blank,if not construct chips.
                        handleEdit(start, whatEnd);
                    } else {
                        commitChip(start, end, editable);
                    }
                }
            }
            mHandler.post(mAddTextWatcher);
        }
        createMoreChip();
    }
    
    /// M: To judge wheather text just have blank.
    private boolean textIsAllBlank(Editable e) {
        if (e != null) {
            for (int i = 0; i < e.length(); i++) {
                if (e.charAt(i) != ' ') {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    private void expand() {
        if (mShouldShrink && !isPhoneQuery()) { /// M: Phone has limited number of lines
            setMaxLines(Integer.MAX_VALUE);
        }
        /// M: Replace the first chip if needed. @{
        if(isPhoneQuery()) {
            RecipientChip[] recipients = getSortedRecipients();
            if (recipients != null && recipients.length > 0 && mHasEllipsizedFirstChip) {
                replaceChipOnSameTextRange(recipients[0], -1);
                mHasEllipsizedFirstChip = false;
            }
        }
        /// @}
        removeMoreChip();
        setCursorVisible(true);
        Editable text = getText();
        setSelection(text != null && text.length() > 0 ? text.length() : 0);
        // If there are any temporary chips, try replacing them now that the user
        // has expanded the field.
        if (mTemporaryRecipients != null && mTemporaryRecipients.size() > 0) {
            new RecipientReplacementTask().execute();
            mTemporaryRecipients = null;
        }
        /// M: For indicating RecipientEditTextView is just been expanded.
        if (mNoChips) {
            mJustExpanded = true;
        }
        /// @}
    }

    private CharSequence ellipsizeText(CharSequence text, TextPaint paint, float maxWidth) {
        paint.setTextSize(mChipFontSize);
        if (maxWidth <= 0 && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Max width is negative: " + maxWidth);
        }
        return TextUtils.ellipsize(text, paint, maxWidth,
                TextUtils.TruncateAt.END);
    }

    private Bitmap createSelectedChip(RecipientEntry contact, TextPaint paint, Layout layout) {
        // Ellipsize the text so that it takes AT MOST the entire width of the
        // autocomplete text entry area. Make sure to leave space for padding
        // on the sides.
        int height = (int) mChipHeight;
        int deleteWidth = height;
        float[] widths = new float[1];
        paint.getTextWidths(" ", widths);
        CharSequence ellipsizedText = ellipsizeText(createChipDisplayText(contact), paint,
                calculateAvailableWidth(true) - deleteWidth - widths[0]);

        // Make sure there is a minimum chip width so the user can ALWAYS
        // tap a chip without difficulty.
        int width = Math.max(deleteWidth * 2, (int) Math.floor(paint.measureText(ellipsizedText, 0,
                ellipsizedText.length()))
                + (mChipPadding * 2) + deleteWidth);

        // Create the background of the chip.
        Bitmap tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tmpBitmap);
        if (mChipBackgroundPressed != null) {
            mChipBackgroundPressed.setBounds(0, 0, width, height);
            mChipBackgroundPressed.draw(canvas);
            paint.setColor(sSelectedTextColor);
            // Vertically center the text in the chip.
            canvas.drawText(ellipsizedText, 0, ellipsizedText.length(), mChipPadding,
                    getTextYOffset((String) ellipsizedText, paint, height), paint);
            // Make the delete a square.
            Rect backgroundPadding = new Rect();
            mChipBackgroundPressed.getPadding(backgroundPadding);
            mChipDelete.setBounds(width - deleteWidth + backgroundPadding.left,
                    0 + backgroundPadding.top,
                    width - backgroundPadding.right,
                    height - backgroundPadding.bottom);
            mChipDelete.draw(canvas);
        } else {
            Log.w(TAG, "Unable to draw a background for the chips as it was never set");
        }
        return tmpBitmap;
    }


    private Bitmap createUnselectedChip(RecipientEntry contact, TextPaint paint, Layout layout,
            boolean leaveBlankIconSpacer) {
        // Ellipsize the text so that it takes AT MOST the entire width of the
        // autocomplete text entry area. Make sure to leave space for padding
        // on the sides.
        int height = (int) mChipHeight;
        int iconWidth = height;
        float[] widths = new float[1];
        paint.getTextWidths(" ", widths);
        /// M: Limit ellipsizedText in some case (ex. moreChip)
        CharSequence ellipsizedText = ellipsizeText(createChipDisplayText(contact), paint,
                (mLimitedWidthForSpan == -1) ? (calculateAvailableWidth(false) - iconWidth - widths[0]) : (mLimitedWidthForSpan - iconWidth - widths[0]));
        // Make sure there is a minimum chip width so the user can ALWAYS
        // tap a chip without difficulty.
        
        /// M: Only leave space if icon exists. @{
        boolean hasIcon = false;
        int ellipsizedTextWidth = (int) Math.floor(paint.measureText(ellipsizedText, 0, ellipsizedText.length()));
        int width = ellipsizedTextWidth + (mChipPadding * 2);
        /// @}

        // Create the background of the chip.
        Bitmap tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Drawable background = getChipBackground(contact);
        if (background != null) {
            Canvas canvas = null; /// M: Only leave space if icon exists	

            // Don't draw photos for recipients that have been typed in OR generated on the fly.
            long contactId = contact.getContactId();
            boolean drawPhotos = isPhoneQuery() ?
                    contactId != RecipientEntry.INVALID_CONTACT
                    : (contactId != RecipientEntry.INVALID_CONTACT
                            && (contactId != RecipientEntry.GENERATED_CONTACT &&
                                    !TextUtils.isEmpty(contact.getDisplayName())));
            if (drawPhotos) {
                byte[] photoBytes = contact.getPhotoBytes();
                // There may not be a photo yet if anything but the first contact address
                // was selected.
                if (photoBytes == null && contact.getPhotoThumbnailUri() != null) {
                    // TODO: cache this in the recipient entry?
                    ((BaseRecipientAdapter) getAdapter()).fetchPhoto(contact, contact
                            .getPhotoThumbnailUri());
                    photoBytes = contact.getPhotoBytes();
                }

                Bitmap photo;
                if (photoBytes != null) {
                    photo = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                } else {
                    // TODO: can the scaled down default photo be cached?
                    photo = mDefaultContactPhoto;
                }
                // Draw the photo on the left side.
                if (photo != null) {
                    /// M: Only leave space if icon exists. @{
                    hasIcon = true;
                    width = ellipsizedTextWidth + (mChipPadding * 2) + iconWidth; 
                    /// @}
                    RectF src = new RectF(0, 0, photo.getWidth(), photo.getHeight());
                    Rect backgroundPadding = new Rect();
                    mChipBackground.getPadding(backgroundPadding);
                    RectF dst = new RectF(width - iconWidth + backgroundPadding.left,
                            0 + backgroundPadding.top,
                            width - backgroundPadding.right,
                            height - backgroundPadding.bottom);
                    Matrix matrix = new Matrix();
                    matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
                    /// M: Only leave space if icon exists.  @{
                    tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(tmpBitmap);
                    /// @}
                    canvas.drawBitmap(photo, matrix, paint);
                }
            } else if (!leaveBlankIconSpacer || isPhoneQuery()) {
                iconWidth = 0;
            }
            /// M: Only leave space if icon exists. @{
            if (canvas == null) {
                canvas = new Canvas(tmpBitmap);
            }

            background.setBounds(0, 0, width, height);
            background.draw(canvas);
            /// @}
            
            paint.setColor(getContext().getResources().getColor(android.R.color.black));
            // Vertically center the text in the chip.
            int xPositionOfText = hasIcon ? mChipPadding : (mChipPadding + (width - mChipPadding*2 - ellipsizedTextWidth)/2); /// M: Horizontally center the text in the chip
            canvas.drawText(ellipsizedText, 0, ellipsizedText.length(), xPositionOfText,
                    getTextYOffset((String)ellipsizedText, paint, height), paint);
        } else {
            Log.w(TAG, "Unable to draw a background for the chips as it was never set");
        }
        return tmpBitmap;
    }

    /**
     * Get the background drawable for a RecipientChip.
     */
    // Visible for testing.
    /*package*/ Drawable getChipBackground(RecipientEntry contact) {
        return (mValidator != null && mValidator.isValid(contact.getDestination())) ?
                mChipBackground : mInvalidChipBackground;
    }

    private float getTextYOffset(String text, TextPaint paint, int height) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int textHeight = bounds.bottom - bounds.top - (int)paint.descent(); /// M: Vertically center the text in the chip (rollback to ICS2)
        return height - ((height - textHeight) / 2);
    }

    private RecipientChip constructChipSpan(RecipientEntry contact, int offset, boolean pressed,
            boolean leaveIconSpace) throws NullPointerException {
        if (mChipBackground == null) {
            throw new NullPointerException(
                    "Unable to render any chips as setChipDimensions was not called.");
        }
        Layout layout = getLayout();

        TextPaint paint = getPaint();
        float defaultSize = paint.getTextSize();
        int defaultColor = paint.getColor();

        Bitmap tmpBitmap;
        if (pressed) {
            tmpBitmap = createSelectedChip(contact, paint, layout);

        } else {
            tmpBitmap = createUnselectedChip(contact, paint, layout, leaveIconSpace);
        }

        // Pass the full text, un-ellipsized, to the chip.
        Drawable result = new BitmapDrawable(getResources(), tmpBitmap);
        result.setBounds(0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight());
        RecipientChip recipientChip = new RecipientChip(result, contact, offset);
        // Return text to the original size.
        paint.setTextSize(defaultSize);
        paint.setColor(defaultColor);
        return recipientChip;
    }

    /**
     * Calculate the bottom of the line the chip will be located on using:
     * 1) which line the chip appears on
     * 2) the height of a chip
     * 3) padding built into the edit text view
     */
    private int calculateOffsetFromBottom(int line) {
        // Line offsets start at zero.
        int actualLine = getLineCount() - (line + 1);
        return -((actualLine * ((int) mChipHeight) + getPaddingBottom()) + getPaddingTop())
                + getDropDownVerticalOffset();
    }

    /**
     * Get the max amount of space a chip can take up. The formula takes into
     * account the width of the EditTextView, any view padding, and padding
     * that will be added to the chip.
     */
    private float calculateAvailableWidth(boolean pressed) {
        return getWidth() - getPaddingLeft() - getPaddingRight() - (mChipPadding * 2);
    }


    private void setChipDimensions(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecipientEditTextView, 0,
                0);
        Resources r = getContext().getResources();
        mChipBackground = a.getDrawable(R.styleable.RecipientEditTextView_chipBackground);
        if (mChipBackground == null) {
            mChipBackground = r.getDrawable(R.drawable.chip_background);
        }
        mChipBackgroundPressed = a
                .getDrawable(R.styleable.RecipientEditTextView_chipBackgroundPressed);
        if (mChipBackgroundPressed == null) {
            mChipBackgroundPressed = r.getDrawable(R.drawable.chip_background_selected);
        }
        mChipDelete = a.getDrawable(R.styleable.RecipientEditTextView_chipDelete);
        if (mChipDelete == null) {
            mChipDelete = r.getDrawable(R.drawable.chip_delete);
        }
        mChipPadding = a.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipPadding, -1);
        if (mChipPadding == -1) {
            mChipPadding = (int) r.getDimension(R.dimen.chip_padding);
        }
        mAlternatesLayout = a.getResourceId(R.styleable.RecipientEditTextView_chipAlternatesLayout,
                -1);
        if (mAlternatesLayout == -1) {
            mAlternatesLayout = R.layout.chips_alternate_item;
        }

        mDefaultContactPhoto = BitmapFactory.decodeResource(r, R.drawable.ic_contact_picture);

        mMoreItem = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.more_item, null);

        mChipHeight = a.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipHeight, -1);
        if (mChipHeight == -1) {
            mChipHeight = r.getDimension(R.dimen.chip_height);
        }
        mChipFontSize = a.getDimensionPixelSize(R.styleable.RecipientEditTextView_chipFontSize, -1);
        if (mChipFontSize == -1) {
            mChipFontSize = r.getDimension(R.dimen.chip_text_size);
        }
        mInvalidChipBackground = a
                .getDrawable(R.styleable.RecipientEditTextView_invalidChipBackground);
        if (mInvalidChipBackground == null) {
            mInvalidChipBackground = r.getDrawable(R.drawable.chip_background_invalid);
        }
        mLineSpacingExtra =  context.getResources().getDimension(R.dimen.line_spacing_extra);
        a.recycle();
    }

    // Visible for testing.
    /* package */ void setMoreItem(TextView moreItem) {
        mMoreItem = moreItem;
    }


    // Visible for testing.
    /* package */ void setChipBackground(Drawable chipBackground) {
        mChipBackground = chipBackground;
    }

    // Visible for testing.
    /* package */ void setChipHeight(int height) {
        mChipHeight = height;
    }

    /**
     * Set whether to shrink the recipients field such that at most
     * one line of recipients chips are shown when the field loses
     * focus. By default, the number of displayed recipients will be
     * limited and a "more" chip will be shown when focus is lost.
     * @param shrink
     */
    public void setOnFocusListShrinkRecipients(boolean shrink) {
        mShouldShrink = shrink;
    }

    @Override
    public void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);
        if (width != 0 && height != 0) {
            if (mPendingChipsCount > 0) {
                postHandlePendingChips();
            } else {
                checkChipWidths();
            }
        }
        // Try to find the scroll view parent, if it exists.
        if (mScrollView == null && !mTriedGettingScrollView) {
            ViewParent parent = getParent();
            while (parent != null && !(parent instanceof ScrollView)) {
                parent = parent.getParent();
            }
            if (parent != null) {
                mScrollView = (ScrollView) parent;
            }
            mTriedGettingScrollView = true;
        }
    }

    private void postHandlePendingChips() {
        mHandler.removeCallbacks(mHandlePendingChips);
        mHandler.post(mHandlePendingChips);
    }

    private void checkChipWidths() {
        // Check the widths of the associated chips.
        RecipientChip[] chips = getSortedRecipients();
        if (chips != null) {
            Rect bounds;
            for (RecipientChip chip : chips) {
                bounds = chip.getDrawable().getBounds();
                if (getWidth() > 0 && bounds.right - bounds.left > (getWidth() - mPaddingLeft - mPaddingRight)) { /// M: Modified to take padding into consideration
                    // Need to redraw that chip.
                    replaceChip(chip, chip.getEntry());
                }
            }
        }
    }

    // Visible for testing.
    /*package*/ void handlePendingChips() {
        Log.d(TAG,"[Debug] handlePendingChips-start");  /// M: TempDebugLog
        /// M: Append strings in batch processing
        appendPendingStrings();

        if (getViewWidth() <= 0) {
            // The widget has not been sized yet.
            // This will be called as a result of onSizeChanged
            // at a later point.
            return;
        }
        if (mPendingChipsCount <= 0) {
            return;
        }

        synchronized (mPendingChips) {
            Editable editable = getText();
            int prevTokenEnd = 0; /// M: Record previous tokenEnd as starting of next substring
            // Tokenize!
            if (mPendingChipsCount <= MAX_CHIPS_PARSED) {
                /// M: We don't care about watching text and span changes while in the middle of handling pending chips. @{
                watcherProcessor wp = null;
                wp = new watcherProcessor();
                wp.initWatcherProcessor();
                wp.removeSpanWatchers();
                /// @}
                for (int i = 0; i < mPendingChips.size(); i++) {
                    /// M: Add text and span watchers back before handling last chip. @{
                    if (i == mPendingChips.size() - 1) {           
                        if(wp != null) {
                            wp.addSpanWatchers();
                        }
                        /// M: Let text and span watchers work corectly by reset selection and layout. @{
                        setSelection(getText().length());
                        requestLayout();
                        /// @}
                    }
                    /// @}
                    String current = mPendingChips.get(i);
                    int tokenStart = editable.toString().indexOf(current, prevTokenEnd); /// M: Get substring from previous tokenEnd to end of current string
                    int tokenEnd = tokenStart + current.length();
                    if (tokenStart >= 0) {
                        // When we have a valid token, include it with the token
                        // to the left.
                        if (tokenEnd < editable.length() - 2
                                && editable.charAt(tokenEnd) == COMMIT_CHAR_COMMA) {
                            tokenEnd++;
                        }
                        createReplacementChip(tokenStart, tokenEnd, editable);
                    }
                    /// M: Get previous tokenEnd as starting of next substring. @{
                    RecipientChip[] chips = getSpannable().getSpans(tokenStart, editable.length(), RecipientChip.class);
                    if ((chips != null && chips.length > 0)) {
                        boolean prevTokenEndSet = false;
                        for(int x = 0; x < chips.length; x++) {
                            if (getChipStart(chips[x]) == tokenStart) {
                                prevTokenEnd = getChipEnd(chips[x]);
                                prevTokenEndSet = true;
                                break;
                            }
                        }
                        if (!prevTokenEndSet) {
                            prevTokenEnd = 0;
                        }
                    } else {
                        prevTokenEnd = 0;
                    }
                    /// @}
                    mPendingChipsCount--;
                }
                sanitizeEnd();
                /// M: Let text and span watchers work corectly by reset selection and layout. @{
                recoverLayout();
                /// @}
            } else {
                mNoChips = true;
            }

            /// M: Restore part of text after handling chips in case it be sanitized. @{
            if (mStringToBeRestore != null) {
                Log.d(TAG,"[handlePendingChips] Restore text ->" + mStringToBeRestore);
                getText().append(mStringToBeRestore);
                mStringToBeRestore = null;
            }
            /// @}

            if (mTemporaryRecipients != null && mTemporaryRecipients.size() > 0
                    && mTemporaryRecipients.size() <= RecipientAlternatesAdapter.MAX_LOOKUPS) {
                if (hasFocus() || mTemporaryRecipients.size() < CHIP_LIMIT) {
                    new RecipientReplacementTask().execute();
                    mTemporaryRecipients = null;
                } else {
                    // Create the "more" chip
                    /// M: Calculate how many chips can be accommodated. @{
                    int numChipsCanShow = 0;
                    if (isPhoneQuery()) {
                        numChipsCanShow = calculateNumChipsCanShow();
                    } else {
                        numChipsCanShow = CHIP_LIMIT;
                    }
                    /// @}
                    mIndividualReplacements = new IndividualReplacementTask();
                    mIndividualReplacements.execute(new ArrayList<RecipientChip>(
                            mTemporaryRecipients.subList(0, numChipsCanShow)));
                    if (mTemporaryRecipients.size() > numChipsCanShow) {
                        mTemporaryRecipients = new ArrayList<RecipientChip>(
                                mTemporaryRecipients.subList(numChipsCanShow,
                                        mTemporaryRecipients.size()));
                    } else {
                        mTemporaryRecipients = null;
                    }
                    createMoreChip();
                }
            } else {
                // There are too many recipients to look up, so just fall back
                // to showing addresses for all of them.
                mTemporaryRecipients = null;
                /// M: Only create moreChip (+XX) when RecipientEditTextView lost focus. @{
                if (!hasFocus()) {
                    createMoreChip();
                }
                /// @}
            }
            mPendingChipsCount = 0;
            mPendingChips.clear();
        }
        Log.d(TAG,"[Debug] handlePendingChips-end");  /// M: TempDebugLog
    }

    // Visible for testing.
    /*package*/ int getViewWidth() {
        return getWidth();
    }

    /**
     * Remove any characters after the last valid chip.
     */
    // Visible for testing.
    /*package*/ void sanitizeEnd() {
        // Don't sanitize while we are waiting for pending chips to complete.
        if (mPendingChipsCount > 0) {
            return;
        }
        // Find the last chip; eliminate any commit characters after it.
        RecipientChip[] chips = getSortedRecipients();
        if (chips != null && chips.length > 0) {
            int end;
            ImageSpan lastSpan;
            mMoreChip = getMoreChip();
            if (mMoreChip != null) {
                lastSpan = mMoreChip;
            } else {
                lastSpan = getLastChip();
            }
            end = getSpannable().getSpanEnd(lastSpan);
            Editable editable = getText();
            int length = editable.length();
            if (length > end) {
                // See what characters occur after that and eliminate them.
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "There were extra characters after the last tokenizable entry."
                            + editable);
                }
                editable.delete(end + 1, length);
            }
        }
    }

    /**
     * Create a chip that represents just the email address of a recipient. At some later
     * point, this chip will be attached to a real contact entry, if one exists.
     */
    private void createReplacementChip(int tokenStart, int tokenEnd, Editable editable) {
        if (alreadyHasChip(tokenStart, tokenEnd)) {
            // There is already a chip present at this location.
            // Don't recreate it.
            return;
        }
        String token = editable.toString().substring(tokenStart, tokenEnd);
        int commitCharIndex = token.trim().lastIndexOf(COMMIT_CHAR_COMMA);
        if (commitCharIndex == token.length() - 1) {
            token = token.substring(0, token.length() - 1);
        }
        RecipientEntry entry = createTokenizedEntry(token);
        if (entry != null) {
            String destText = createAddressText(entry);
            // Always leave a blank space at the end of a chip.
            int textLength = destText.length() - 1;
            SpannableString chipText = new SpannableString(destText);
            int end = getSelectionEnd();
            int start = mTokenizer != null ? mTokenizer.findTokenStart(getText(), end) : 0;
            RecipientChip chip = null;
            try {
                if (!mNoChips) {
                    /* leave space for the contact icon if this is not just an email address */
                    chip = constructChipSpan(
                            entry,
                            start,
                            false,
                            TextUtils.isEmpty(entry.getDisplayName())
                                    || TextUtils.equals(entry.getDisplayName(),
                                            entry.getDestination()));
                    chipText.setSpan(chip, 0, textLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            editable.replace(tokenStart, tokenEnd, chipText);
            // Add this chip to the list of entries "to replace"
            if (chip != null) {
                if (mTemporaryRecipients == null) {
                    mTemporaryRecipients = new ArrayList<RecipientChip>();
                }
                chip.setOriginalText(chipText.toString());
                mTemporaryRecipients.add(chip);
            }
        }
    }

    private static boolean isPhoneNumber(String number) {
        // TODO: replace this function with libphonenumber's isPossibleNumber (see
        // PhoneNumberUtil). One complication is that it requires the sender's region which
        // comes from the CurrentCountryIso. For now, let's just do this simple match.
        if (TextUtils.isEmpty(number)) {
            return false;
        }

        Matcher match = Patterns.PHONE.matcher(number);
        return match.matches();
    }

    private RecipientEntry createTokenizedEntry(String token) {
        if (TextUtils.isEmpty(token)) {
            return null;
        }
        if (isPhoneQuery() && isPhoneNumber(token)) {
            return RecipientEntry
                    .constructFakeEntry(token);
        }
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(token);
        String display = null;
        if (isValid(token) && tokens != null && tokens.length > 0) {
            // If we can get a name from tokenizing, then generate an entry from
            // this.
            display = tokens[0].getName();
            if (!TextUtils.isEmpty(display)) {
                if (!isPhoneQuery()) {
                    if (!TextUtils.isEmpty(token)) {
                        token = token.trim();
                    }
                    char charAt = token.charAt(token.length() - 1);
                    if (charAt == COMMIT_CHAR_COMMA || charAt == COMMIT_CHAR_SEMICOLON || charAt == COMMIT_CHAR_CHINESE_COMMA || charAt == COMMIT_CHAR_CHINESE_SEMICOLON) {
                        token = token.substring(0, token.length() - 1);
                    }
                }
                return RecipientEntry.constructGeneratedEntry(display, token);
            } else {
                display = tokens[0].getAddress();
                if (!TextUtils.isEmpty(display)) {
                    return RecipientEntry.constructFakeEntry(display);
                }
            }
        }
        // Unable to validate the token or to create a valid token from it.
        // Just create a chip the user can edit.
        String validatedToken = null;
        if (mValidator != null && !mValidator.isValid(token)) {
            // Try fixing up the entry using the validator.
            validatedToken = mValidator.fixText(token).toString();
            if (!TextUtils.isEmpty(validatedToken)) {
                if (validatedToken.contains(token)) {
                    // protect against the case of a validator with a null domain,
                    // which doesn't add a domain to the token
                    Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(validatedToken);
                    if (tokenized.length > 0) {
                        validatedToken = tokenized[0].getAddress();
                    }
                } else {
                    // We ran into a case where the token was invalid and removed
                    // by the validator. In this case, just use the original token
                    // and let the user sort out the error chip.
                    validatedToken = null;
                }
            }
        }
        // Otherwise, fallback to just creating an editable email address chip.
        return RecipientEntry
                .constructFakeEntry(!TextUtils.isEmpty(validatedToken) ? validatedToken : token);
    }

    private boolean isValid(String text) {
        return mValidator == null ? true : mValidator.isValid(text);
    }

    /**
     * M: Get name after parsing destination by Rfc822Tokenizer. If it's null, return whole destination instead.
     */
    private String tokenizeName(String destination) {
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(destination);
        if (tokens != null && tokens.length > 0) {
            return tokens[0].getName();
        }
        return destination;
    }

    private String tokenizeAddress(String destination) {
        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(destination);
        if (tokens != null && tokens.length > 0) {
            return tokens[0].getAddress();
        }
        return destination;
    }

    @Override
    public void setTokenizer(Tokenizer tokenizer) {
        mTokenizer = tokenizer;
        super.setTokenizer(mTokenizer);
    }

    @Override
    public void setValidator(Validator validator) {
        mValidator = validator;
        super.setValidator(validator);
    }

    /**
     * We cannot use the default mechanism for replaceText. Instead,
     * we override onItemClickListener so we can get all the associated
     * contact information including display text, address, and id.
     */
    @Override
    protected void replaceText(CharSequence text) {
        return;
    }

    /**
     * Dismiss any selected chips when the back key is pressed.
     */
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mSelectedChip != null) {
            clearSelectedChip();
            return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    /**
     * Monitor key presses in this view to see if the user types
     * any commit keys, which consist of ENTER, TAB, or DPAD_CENTER.
     * If the user has entered text that has contact matches and types
     * a commit key, create a chip from the topmost matching contact.
     * If the user has entered text that has no contact matches and types
     * a commit key, then create a chip from the text they have entered.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.hasNoModifiers()) {
                    if (commitDefault()) {
                        return true;
                    }
                    if (mSelectedChip != null) {
                        clearSelectedChip();
                        return true;
                    } else if (focusNext()) {
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_TAB:
                if (event.hasNoModifiers()) {
                    if (mSelectedChip != null) {
                        clearSelectedChip();
                    } else {
                        commitDefault();
                    }
                    if (focusNext()) {
                        return true;
                    }
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean focusNext() {
        View next = focusSearch(View.FOCUS_DOWN);
        if (next != null) {
            next.requestFocus();
            return true;
        }
        return false;
    }

    /**
     * Create a chip from the default selection. If the popup is showing, the
     * default is the first item in the popup suggestions list. Otherwise, it is
     * whatever the user had typed in. End represents where the the tokenizer
     * should search for a token to turn into a chip.
     * @return If a chip was created from a real contact.
     */
    private boolean commitDefault() {
        // If there is no tokenizer, don't try to commit.
        if (mTokenizer == null) {
            return false;
        }
        Editable editable = getText();
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(editable, end);

        if (shouldCreateChip(start, end)) {
            setDisableBringPointIntoView(true);  /// M: Don't scroll view when commitDefault chip
            int whatEnd = mTokenizer.findTokenEnd(getText(), start);
            // In the middle of chip; treat this as an edit
            // and commit the whole token.
            if (whatEnd != getSelectionEnd()) {
                handleEdit(start, whatEnd);
                return true;
            }
            return commitChip(start, end , editable);
        }
        return false;
    }

    private void commitByCharacter() {
        // We can't possibly commit by character if we can't tokenize.
        if (mTokenizer == null) {
            return;
        }
        Editable editable = getText();
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(editable, end);
        if (shouldCreateChip(start, end)) {
            commitChip(start, end, editable);
        }
        setSelection(getText().length());
    }

    private boolean commitChip(int start, int end, Editable editable) {
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter.getCount() > 0 && enoughToFilter()
                && end == getSelectionEnd() && !isPhoneQuery()) {
            // choose the first entry.
            submitItemAtPosition(0);
            dismissDropDown();
            return true;
        } else {
            int tokenEnd = mTokenizer.findTokenEnd(editable, start);
            if (editable.length() > tokenEnd + 1) {
                char charAt = editable.charAt(tokenEnd + 1);
                if (charAt == COMMIT_CHAR_COMMA || charAt == COMMIT_CHAR_SEMICOLON || charAt == COMMIT_CHAR_CHINESE_COMMA || charAt == COMMIT_CHAR_CHINESE_SEMICOLON) {
                    tokenEnd++;
                }
            }
            String text = editable.toString().substring(start, tokenEnd).trim();
            /// M: Auto choose first match contact. @{
            if (isPhoneQuery() && adapter != null && adapter.getCount() > 0 && enoughToFilter() && end == getSelectionEnd()){
                int adapterCount = getAdapter().getCount();
                /// M: Check if there's fully match displayName or destination.
                for (int itemCnt = 0; itemCnt < adapterCount; itemCnt++){
                    RecipientEntry entry = (RecipientEntry)getAdapter().getItem(itemCnt);
                    String displayName = entry.getDisplayName().toLowerCase();
                    String destination = entry.getDestination(); 
                    if (text.equals(destination) || text.toLowerCase().equals(displayName)){
                        submitItemAtPosition(itemCnt);
                        dismissDropDown();
                        return true;
                    }
                }
                /// M: Check if there's match normanlized destination.
                for (int itemCnt = 0; itemCnt < adapterCount; itemCnt++){
                    RecipientEntry entry = (RecipientEntry)getAdapter().getItem(itemCnt);
                    String displayName = entry.getDisplayName().toLowerCase();
                    String destination = entry.getDestination();
                    if (entry.getDestinationKind() == RecipientEntry.ENTRY_KIND_PHONE) {
                        String currentNumber = PhoneNumberUtils.normalizeNumber(text);
                        String queryNumber = PhoneNumberUtils.normalizeNumber(destination);
                        if (PhoneNumberUtils.compare(currentNumber, queryNumber)) {
                            submitItemAtPosition(itemCnt);
                            dismissDropDown();
                            return true;
                        }
                     }
                }
            }
            /// @}
            clearComposingText();
            if (text != null && text.length() > 0 && !text.equals(" ")) {
                RecipientEntry entry = createTokenizedEntry(text);
                if (entry != null) {
                    QwertyKeyListener.markAsReplaced(editable, start, end, "");
                    CharSequence chipText = createChip(entry, false);
                    if (chipText != null && start > -1 && end > -1) {
                        editable.replace(start, end, chipText);
                    }
                }
                // Only dismiss the dropdown if it is related to the text we
                // just committed.
                // For paste, it may not be as there are possibly multiple
                // tokens being added.
                if (end == getSelectionEnd()) {
                    dismissDropDown();
                }
                sanitizeBetween();
                return true;
            }
        }
        return false;
    }

    // Visible for testing.
    /* package */ void sanitizeBetween() {
        // Don't sanitize while we are waiting for content to chipify.
        if (mPendingChipsCount > 0) {
            return;
        }
        // Find the last chip.
        RecipientChip[] recips = getSortedRecipients();
        if (recips != null && recips.length > 0) {
            RecipientChip last = recips[recips.length - 1];
            RecipientChip beforeLast = null;
            if (recips.length > 1) {
                beforeLast = recips[recips.length - 2];
            }
            int startLooking = 0;
            int end = getSpannable().getSpanStart(last);
            if (beforeLast != null) {
                startLooking = getSpannable().getSpanEnd(beforeLast);
                Editable text = getText();
                if (startLooking == -1 || startLooking > text.length() - 1) {
                    // There is nothing after this chip.
                    return;
                }
                if (text.charAt(startLooking) == ' ') {
                    startLooking++;
                }
            }
            if (startLooking >= 0 && end >= 0 && startLooking < end) {
                getText().delete(startLooking, end);
            }
        }
    }

    private boolean shouldCreateChip(int start, int end) {
        return !mNoChips && hasFocus() && enoughToFilter() && !alreadyHasChip(start, end);
    }

    private boolean alreadyHasChip(int start, int end) {
        if (mNoChips) {
            return true;
        }
        RecipientChip[] chips = getSpannable().getSpans(start, end, RecipientChip.class);
        if ((chips == null || chips.length == 0)) {
            return false;
        }
        return true;
    }

    private void handleEdit(int start, int end) {
        if (start == -1 || end == -1) {
            // This chip no longer exists in the field.
            dismissDropDown();
            return;
        }
        // This is in the middle of a chip, so select out the whole chip
        // and commit it.
        Editable editable = getText();
        setSelection(end);
        String text = getText().toString().substring(start, end);
        if (!TextUtils.isEmpty(text)) {
            RecipientEntry entry = RecipientEntry.constructFakeEntry(text);
            QwertyKeyListener.markAsReplaced(editable, start, end, "");
            CharSequence chipText = createChip(entry, false);
            int selEnd = getSelectionEnd();
            if (chipText != null && start > -1 && selEnd > -1) {
                editable.replace(start, selEnd, chipText);
            }
        }
        dismissDropDown();
    }

    /**
     * If there is a selected chip, delegate the key events
     * to the selected chip.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.hasNoModifiers()) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // Visible for testing.
    /* package */ Spannable getSpannable() {
        return getText();
    }

    private int getChipStart(RecipientChip chip) {
        return getSpannable().getSpanStart(chip);
    }

    private int getChipEnd(RecipientChip chip) {
        return getSpannable().getSpanEnd(chip);
    }

    /**
     * Instead of filtering on the entire contents of the edit box,
     * this subclass method filters on the range from
     * {@link Tokenizer#findTokenStart} to {@link #getSelectionEnd}
     * if the length of that range meets or exceeds {@link #getThreshold}
     * and makes sure that the range is not already a Chip.
     */
    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        if (enoughToFilter() && !isCompletedToken(text)) {
            int end = getSelectionEnd();
            int start = mTokenizer.findTokenStart(text, end);
            // If this is a RecipientChip, don't filter
            // on its contents.
            Spannable span = getSpannable();
            RecipientChip[] chips = span.getSpans(start, end, RecipientChip.class);
            if (chips != null && chips.length > 0) {
                return;
            }
        }
        super.performFiltering(text, keyCode);
    }

    /// M: When touch after paste,dismiss filter popup. @{
    public void onFilterComplete(int count) {
        if (!bTouchedAfterPasted) {
            super.onFilterComplete(count);
        }
        bPasted = false;
        bTouchedAfterPasted = false;
    }
    /// M: }@

    // Visible for testing.
    /*package*/ boolean isCompletedToken(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        // Check to see if this is a completed token before filtering.
        int end = text.length();
        int start = mTokenizer.findTokenStart(text, end);
        String token = text.toString().substring(start, end).trim();
        if (!TextUtils.isEmpty(token)) {
            char atEnd = token.charAt(token.length() - 1);
            return atEnd == COMMIT_CHAR_COMMA || atEnd == COMMIT_CHAR_SEMICOLON || atEnd == COMMIT_CHAR_CHINESE_COMMA || atEnd == COMMIT_CHAR_CHINESE_SEMICOLON;
        }
        return false;
    }

    private void clearSelectedChip() {
        if (mSelectedChip != null) {
            unselectChip(mSelectedChip);
            mSelectedChip = null;
        }
        setCursorVisible(true);
    }

    /**
     * Monitor touch events in the RecipientEditTextView.
     * If the view does not have focus, any tap on the view
     * will just focus the view. If the view has focus, determine
     * if the touch target is a recipient chip. If it is and the chip
     * is not selected, select it and clear any other selected chips.
     * If it isn't, then select that chip.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isFocused()) {
            // Ignore any chip taps until this view is focused.
            return super.onTouchEvent(event);
        }

        /// M: When touch after paste, don't care filter popup.
        if (bPasted) {
            bTouchedAfterPasted = true;
            dismissDropDown();
        }

        /// M: Get currentChip if touch point is in chip. Only show soft input when currentChip is INVALID_CONTACT or GENERATED_CONTACT. @{
        float x = -1;
        float y = -1;
        int offset = -1;
        RecipientChip currentChip = null;
        
        boolean shouldShowSoftInput = true;
        if (mCopyAddress == null && event.getAction() == MotionEvent.ACTION_UP) {
            x = event.getX();
            y = event.getY();
            offset = putOffsetInRange(getOffsetForPosition(x, y));
            /// M: Fix misrecognize a touch event is located in a chip while it's actually out side of the chip.
            currentChip = (isTouchPointInChip(x,y)) ? (findChip(offset)) : (null); 
            
            if (currentChip!=null) {
                shouldShowSoftInput = shouldShowEditableText(currentChip);
                if (!shouldShowSoftInput) {
                    super.setShowSoftInputOnFocus(false);
                }
            }
        }
        /// @}
        
        boolean handled = super.onTouchEvent(event);
        int action = event.getAction();
        boolean chipWasSelected = false;
        if (mSelectedChip == null) {
            mGestureDetector.onTouchEvent(event);
        }

        /// M: Don't handle the release after a long press, because it will
        /// move the selection away from whatever the menu action was
        /// trying to affect.@{ 
        if (getEnableDiscardNextActionUp() && action == MotionEvent.ACTION_UP) {
            setEnableDiscardNextActionUp(false);
            /// M: Reset ShowSoftInputOnFocus in TextView. @{
            if (!shouldShowSoftInput) {
                super.setShowSoftInputOnFocus(true);
            }
            /// @}
            return handled;
        }
        /// @} 

        if (mCopyAddress == null && action == MotionEvent.ACTION_UP) {
            /// M: Do nothing after scrolling in case trigger scroll view to the end of text afterwards. @{
            if (isPhoneQuery() && mMoveCursorToVisible) {
                mMoveCursorToVisible = false;
                /// M: Reset ShowSoftInputOnFocus in TextView. @{
                if (!shouldShowSoftInput) {
                    super.setShowSoftInputOnFocus(true);
                }
                /// @}
                return true;
            }
            /// @}
            
            if (currentChip != null) {
                if (action == MotionEvent.ACTION_UP) {
                    if (mSelectedChip != null && mSelectedChip != currentChip) {
                        clearSelectedChip();
                        mSelectedChip = selectChip(currentChip);
                    } else if (mSelectedChip == null) {
                        setSelection(getText().length());
                        commitDefault();
                        mSelectedChip = selectChip(currentChip);
                    } else {
                        onClick(mSelectedChip, offset, x, y);
                    }
                }
                chipWasSelected = true;
                handled = true;
            } else if (mSelectedChip != null && shouldShowEditableText(mSelectedChip)) {
                chipWasSelected = true;
            }
        }
        if (action == MotionEvent.ACTION_UP && !chipWasSelected) {
            clearSelectedChip();
        }
        /// M: Reset ShowSoftInputOnFocus in TextView. @{
        if (!shouldShowSoftInput) {
            super.setShowSoftInputOnFocus(true);
        }
        /// @}
        return handled;
    }

    private void scrollLineIntoView(int line) {
        if (mScrollView != null) {
            mScrollView.scrollBy(0, calculateOffsetFromBottom(line));
        }
    }

    private void showAlternates(RecipientChip currentChip, ListPopupWindow alternatesPopup,
            int width, Context context) {
        int line = getLayout().getLineForOffset(getChipStart(currentChip));
        int bottom = getOffsetFromBottom(line); /// M: Locate drop-down list at proper position
        // Align the alternates popup with the left side of the View,
        // regardless of the position of the chip tapped.
        alternatesPopup.setWidth(width);
        alternatesPopup.setAnchorView(this);
        alternatesPopup.setVerticalOffset(bottom + mDefaultVerticalOffset); /// M: Adjust position of popup window
        alternatesPopup.setAdapter(createAlternatesAdapter(currentChip));
        alternatesPopup.setOnItemClickListener(mAlternatesListener);
        // Clear the checked item.
        mCheckedItem = -1;
        alternatesPopup.show();
        ListView listView = alternatesPopup.getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        // Checked item would be -1 if the adapter has not
        // loaded the view that should be checked yet. The
        // variable will be set correctly when onCheckedItemChanged
        // is called in a separate thread.
        if (mCheckedItem != -1) {
            listView.setItemChecked(mCheckedItem, true);
            mCheckedItem = -1;
        }
    }

    private ListAdapter createAlternatesAdapter(RecipientChip chip) {
        if (isPhoneQuery()) {
            /// M: Show phone number and email simutaneously when select chip in phoneQuery
            RecipientAlternatesAdapter adapter = new RecipientAlternatesAdapter(getContext(), chip.getContactId(), chip.getDataId(),
                mAlternatesLayout, ((BaseRecipientAdapter)getAdapter()).getQueryType(), this, ((BaseRecipientAdapter)getAdapter()).getShowPhoneAndEmail());
            return adapter;
        } else { 
            return new RecipientAlternatesAdapter(getContext(), chip.getContactId(), chip.getDataId(),
                mAlternatesLayout, ((BaseRecipientAdapter)getAdapter()).getQueryType(), this);    
        }
    }

    private ListAdapter createSingleAddressAdapter(RecipientChip currentChip) {
        return new SingleRecipientArrayAdapter(getContext(), mAlternatesLayout, currentChip
                .getEntry());
    }

    @Override
    public void onCheckedItemChanged(int position) {
        ListView listView = mAlternatesPopup.getListView();
        if (listView != null && listView.getCheckedItemCount() == 0) {
            listView.setItemChecked(position, true);
        }
        mCheckedItem = position;
    }

    // TODO: This algorithm will need a lot of tweaking after more people have used
    // the chips ui. This attempts to be "forgiving" to fat finger touches by favoring
    // what comes before the finger.
    private int putOffsetInRange(int o) {
        int offset = o;
        Editable text = getText();
        int length = text.length();
        // Remove whitespace from end to find "real end"
        int realLength = length;
        for (int i = length - 1; i >= 0; i--) {
            if (text.charAt(i) == ' ') {
                realLength--;
            } else {
                break;
            }
        }

        // If the offset is beyond or at the end of the text,
        // leave it alone.
        if (offset >= realLength) {
            return offset;
        }
        Editable editable = getText();
        while (offset >= 0 && findText(editable, offset) == -1 && findChip(offset) == null) {
            // Keep walking backward!
            offset--;
        }
        return offset;
    }

    private int findText(Editable text, int offset) {
        if (text.charAt(offset) != ' ') {
            return offset;
        }
        return -1;
    }

    private RecipientChip findChip(int offset) {
        RecipientChip[] chips = getSpannable().getSpans(0, getText().length(), RecipientChip.class);
        // Find the chip that contains this offset.
        for (int i = 0; i < chips.length; i++) {
            RecipientChip chip = chips[i];
            int start = getChipStart(chip);
            int end = getChipEnd(chip);
            if (offset >= start && offset <= end) {
                return chip;
            }
        }
        return null;
    }

    // Visible for testing.
    // Use this method to generate text to add to the list of addresses.
    /* package */String createAddressText(RecipientEntry entry) {
        String display = entry.getDisplayName();
        String address = entry.getDestination();
        if (TextUtils.isEmpty(display) || TextUtils.equals(display, address)) {
            display = null;
        }
        String trimmedDisplayText;
        if (isPhoneQuery() && isPhoneNumber(address)) {
            trimmedDisplayText = address.trim();
        } else {
            if (address != null) {
                // Tokenize out the address in case the address already
                // contained the username as well.
                Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(address);
                if (tokenized != null && tokenized.length > 0) {
                    address = tokenized[0].getAddress();
                }
            }
            Rfc822Token token = new Rfc822Token(display, address, null);
            trimmedDisplayText = token.toString().trim();
        }
        int index = trimmedDisplayText.indexOf(",");
        return mTokenizer != null && !TextUtils.isEmpty(trimmedDisplayText)
                && index < trimmedDisplayText.length() - 1 ? (String) mTokenizer
                .terminateToken(trimmedDisplayText) : trimmedDisplayText;
    }

    // Visible for testing.
    // Use this method to generate text to display in a chip.
    /*package*/ String createChipDisplayText(RecipientEntry entry) {
        String display = entry.getDisplayName();
        String address = entry.getDestination();
        if (TextUtils.isEmpty(display) || TextUtils.equals(display, address)) {
            display = null;
        }
        if (address != null && !(isPhoneQuery() && isPhoneNumber(address))) {
            // Tokenize out the address in case the address already
            // contained the username as well.
            Rfc822Token[] tokenized = Rfc822Tokenizer.tokenize(address);
            if (tokenized != null && tokenized.length > 0) {
                address = tokenized[0].getAddress();
            }
        }
        if (!TextUtils.isEmpty(display)) {
            return display;
        } else if (!TextUtils.isEmpty(address)){
            return address;
        } else {
            return new Rfc822Token(display, address, null).toString();
        }
    }

    private CharSequence createChip(RecipientEntry entry, boolean pressed) {
        String displayText = createAddressText(entry);
        if (TextUtils.isEmpty(displayText)) {
            return null;
        }
        SpannableString chipText = null;
        // Always leave a blank space at the end of a chip.
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(getText(), end);
        int textLength = displayText.length()-1;
        chipText = new SpannableString(displayText);
        if (!mNoChips) {
            try {
                RecipientChip chip = constructChipSpan(entry, start, pressed,
                        false /* leave space for contact icon */);
                chipText.setSpan(chip, 0, textLength,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                chip.setOriginalText(chipText.toString());
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage(), e);
                return null;
            }
        }
        return chipText;
    }

    /**
     * When an item in the suggestions list has been clicked, create a chip from the
     * contact information of the selected item.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        submitItemAtPosition(position);
    }

    private void submitItemAtPosition(int position) {
        RecipientEntry entry = createValidatedEntry(
                (RecipientEntry)getAdapter().getItem(position));
        if (entry == null) {
            return;
        }
        clearComposingText();

        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(getText(), end);

        Editable editable = getText();
        QwertyKeyListener.markAsReplaced(editable, start, end, "");
        CharSequence chip = createChip(entry, false);
        if (chip != null && start >= 0 && end >= 0) {
            editable.replace(start, end, chip);
        }
        sanitizeBetween();
    }

    private RecipientEntry createValidatedEntry(RecipientEntry item) {
        if (item == null) {
            return null;
        }
        final RecipientEntry entry;
        // If the display name and the address are the same, or if this is a
        // valid contact, but the destination is invalid, then make this a fake
        // recipient that is editable.
        String destination = item.getDestination();
        if (item.getContactId() == RecipientEntry.GENERATED_CONTACT) { /// M: Let phone also can constructGeneratedEntry
            entry = RecipientEntry.constructGeneratedEntry(item.getDisplayName(),
                    destination);
        } else if (RecipientEntry.isCreatedRecipient(item.getContactId())
                && (TextUtils.isEmpty(item.getDisplayName())
                        || TextUtils.equals(item.getDisplayName(), destination)
                        || (mValidator != null && !mValidator.isValid(destination)))) {
            entry = RecipientEntry.constructFakeEntry(destination);
        } else {
            entry = item;
        }
        return entry;
    }

    /** Returns a collection of contact Id for each chip inside this View. */
    /* package */ Collection<Long> getContactIds() {
        final Set<Long> result = new HashSet<Long>();
        RecipientChip[] chips = getSortedRecipients();
        if (chips != null) {
            for (RecipientChip chip : chips) {
                result.add(chip.getContactId());
            }
        }
        return result;
    }


    /** Returns a collection of data Id for each chip inside this View. May be null. */
    /* package */ Collection<Long> getDataIds() {
        final Set<Long> result = new HashSet<Long>();
        RecipientChip [] chips = getSortedRecipients();
        if (chips != null) {
            for (RecipientChip chip : chips) {
                result.add(chip.getDataId());
            }
        }
        return result;
    }

    // Visible for testing.
    /* package */RecipientChip[] getSortedRecipients() {
        /// M: For print TempDebugLog. @{
        Object[] recipientsObj = getSpannable()
                .getSpans(0, getText().length(), RecipientChip.class);
        boolean printLog = false;
        for (Object currObj : recipientsObj) {
            if(!(currObj instanceof RecipientChip)) {
                printLog = true;
            }
        }
        if (printLog) {
            for (Object currObj : recipientsObj) {
                tempLogPrint("getSortedRecipients",currObj);
            }
            printLog = false;
        }
        /// @}
        RecipientChip[] recips = getSpannable()
                .getSpans(0, getText().length(), RecipientChip.class);
        ArrayList<RecipientChip> recipientsList = new ArrayList<RecipientChip>(Arrays
                .asList(recips));
        final Spannable spannable = getSpannable();
        Collections.sort(recipientsList, new Comparator<RecipientChip>() {

            @Override
            public int compare(RecipientChip first, RecipientChip second) {
                int firstStart = spannable.getSpanStart(first);
                int secondStart = spannable.getSpanStart(second);
                if (firstStart < secondStart) {
                    return -1;
                } else if (firstStart > secondStart) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return recipientsList.toArray(new RecipientChip[recipientsList.size()]);
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * No chips are selectable.
     */
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    // Visible for testing.
    /* package */ImageSpan getMoreChip() {
        MoreImageSpan[] moreSpans = getSpannable().getSpans(0, getText().length(),
                MoreImageSpan.class);
        return moreSpans != null && moreSpans.length > 0 ? moreSpans[0] : null;
    }

    private MoreImageSpan createMoreSpan(int count) {
        String moreText = String.format(mMoreItem.getText().toString(), count);
        TextPaint morePaint = new TextPaint(getPaint());
        morePaint.setTextSize(mMoreItem.getTextSize());
        morePaint.setColor(mMoreItem.getCurrentTextColor());
        int width = (int)morePaint.measureText(moreText) + mMoreItem.getPaddingLeft()
                + mMoreItem.getPaddingRight();
        /// M: Save current textSize and set textSize to defaultSize in case bitmap size of MoreChip is incorrect. @{
        float TempTextSize = getPaint().getTextSize();
        getPaint().setTextSize(mDefaultTextSize);
        /// @}
        int height = getLineHeight();
        Bitmap drawable = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(drawable);
        int adjustedHeight = height;
        Layout layout = getLayout();
        if (layout != null) {
            adjustedHeight -= layout.getLineDescent(0);
        }
        canvas.drawText(moreText, 0, moreText.length(), 0, adjustedHeight, morePaint);

        Drawable result = new BitmapDrawable(getResources(), drawable);
        result.setBounds(0, 0, width, height);
        getPaint().setTextSize(TempTextSize);  /// M: Reset textSize back.
        return new MoreImageSpan(result);
    }

    // Visible for testing.
    /*package*/ void createMoreChipPlainText() {
        // Take the first <= CHIP_LIMIT addresses and get to the end of the second one.
        Editable text = getText();
        int start = 0;
        int end = start;
        for (int i = 0; i < CHIP_LIMIT; i++) {
            end = movePastTerminators(mTokenizer.findTokenEnd(text, start));
            start = end; // move to the next token and get its end.
        }
        // Now, count total addresses.
        start = 0;
        int tokenCount = countTokens(text);
        MoreImageSpan moreSpan = createMoreSpan(tokenCount - CHIP_LIMIT);
        SpannableString chipText = new SpannableString(text.subSequence(end, text.length()));
        chipText.setSpan(moreSpan, 0, chipText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.replace(end, text.length(), chipText);
        mMoreChip = moreSpan;
    }

    // Visible for testing.
    /* package */int countTokens(Editable text) {
        int tokenCount = 0;
        int start = 0;
        while (start < text.length()) {
            start = movePastTerminators(mTokenizer.findTokenEnd(text, start));
            tokenCount++;
            if (start >= text.length()) {
                break;
            }
        }
        return tokenCount;
    }

    /**
     * Create the more chip. The more chip is text that replaces any chips that
     * do not fit in the pre-defined available space when the
     * RecipientEditTextView loses focus.
     */
    // Visible for testing.
    /* package */ void createMoreChip() {
        if (mNoChips) {
            createMoreChipPlainText();
            return;
        }

        if (!mShouldShrink) {
            return;
        }

        ImageSpan[] tempMore = getSpannable().getSpans(0, getText().length(), MoreImageSpan.class);
        if (tempMore.length > 0) {
            getSpannable().removeSpan(tempMore[0]);
        }
        RecipientChip[] recipients = getSortedRecipients();
        /// M: There's different criterion for phoneQuery & non-phoneQuery.
        if (recipients == null || (!isPhoneQuery() && recipients.length <= CHIP_LIMIT) || (isPhoneQuery() && recipients.length <= 1) ) {
            mMoreChip = null;
            return;
        }
        Spannable spannable = getSpannable();
        int numRecipients = recipients.length;
        /// M: Calculate overage. @{
        int overage = 0;
        if (isPhoneQuery()) {
            overage = numRecipients - calculateNumChipsCanShow() ;
            if (overage <= 0) {
                mMoreChip = null;
                return;
            }
        } else {
            overage = numRecipients - CHIP_LIMIT;
        }
        /// @}
        MoreImageSpan moreSpan = createMoreSpan(overage);
        mRemovedSpans = new ArrayList<RecipientChip>();
        int totalReplaceStart = 0;
        int totalReplaceEnd = 0;
        Editable text = getText();
        /// M: Remove watchers. @{
        watcherProcessor wp = null;
        wp = new watcherProcessor();
        wp.initWatcherProcessor();
        wp.removeSpanWatchers();
        /// @}
        for (int i = numRecipients - overage; i < recipients.length; i++) {
            mRemovedSpans.add(recipients[i]);
            if (i == numRecipients - overage) {
                totalReplaceStart = spannable.getSpanStart(recipients[i]);
            }
            if (i == recipients.length - 1) {
                totalReplaceEnd = spannable.getSpanEnd(recipients[i]);
            }
            if (mTemporaryRecipients == null || !mTemporaryRecipients.contains(recipients[i])) {
                int spanStart = spannable.getSpanStart(recipients[i]);
                int spanEnd = spannable.getSpanEnd(recipients[i]);
                recipients[i].setOriginalText(text.toString().substring(spanStart, spanEnd));
            }
            spannable.removeSpan(recipients[i]);
        }
        /// M: Add watchers back. @{
        if(wp != null) {
            wp.addSpanWatchers();
        }
        recoverLayout();
        /// @}
        if (totalReplaceEnd < text.length()) {
            totalReplaceEnd = text.length();
        }
        int end = Math.max(totalReplaceStart, totalReplaceEnd);
        int start = Math.min(totalReplaceStart, totalReplaceEnd);
        SpannableString chipText = new SpannableString(text.subSequence(start, end));
        chipText.setSpan(moreSpan, 0, chipText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.replace(start, end, chipText);
        mMoreChip = moreSpan;
        // If adding the +more chip goes over the limit, resize accordingly.
        if (!isPhoneQuery() && getLineCount() > mMaxLines) {
            setMaxLines(getLineCount());
        }
    }

    /**
     * Replace the more chip, if it exists, with all of the recipient chips it had
     * replaced when the RecipientEditTextView gains focus.
     */
    // Visible for testing.
    /*package*/ void removeMoreChip() {
        if (mMoreChip != null) {
            Spannable span = getSpannable();
            span.removeSpan(mMoreChip);
            mMoreChip = null;
            // Re-add the spans that were removed.
            if (mRemovedSpans != null && mRemovedSpans.size() > 0) {
                // Recreate each removed span.
                RecipientChip[] recipients = getSortedRecipients();
                // Start the search for tokens after the last currently visible
                // chip.
                if (recipients == null || recipients.length == 0) {
                    return;
                }
                int end = span.getSpanEnd(recipients[recipients.length - 1]);
                Editable editable = getText();
                /// M: Remove watchers. @{
                watcherProcessor wp = null;
                wp = new watcherProcessor();
                wp.initWatcherProcessor();
                wp.removeSpanWatchers();
                /// @}
                for (RecipientChip chip : mRemovedSpans) {
                    int chipStart;
                    int chipEnd;
                    String token;
                    // Need to find the location of the chip, again.
                    token = (String) chip.getOriginalText();
                    // As we find the matching recipient for the remove spans,
                    // reduce the size of the string we need to search.
                    // That way, if there are duplicates, we always find the correct
                    // recipient.
                    chipStart = editable.toString().indexOf(token, end);
                    end = chipEnd = Math.min(editable.length(), chipStart + token.length());
                    // Only set the span if we found a matching token.
                    if (chipStart != -1) {
                        editable.setSpan(chip, chipStart, chipEnd,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                /// M: Add watchers back. @{
                if(wp != null) {
                    wp.addSpanWatchers();
                }
                recoverLayout();
                /// @}
                mRemovedSpans.clear();
            }
        }
    }

    /**
     * Show specified chip as selected. If the RecipientChip is just an email address,
     * selecting the chip will take the contents of the chip and place it at
     * the end of the RecipientEditTextView for inline editing. If the
     * RecipientChip is a complete contact, then selecting the chip
     * will change the background color of the chip, show the delete icon,
     * and a popup window with the address in use highlighted and any other
     * alternate addresses for the contact.
     * @param currentChip Chip to select.
     * @return A RecipientChip in the selected state or null if the chip
     * just contained an email address.
     */
    private RecipientChip selectChip(RecipientChip currentChip) {
        if (shouldShowEditableText(currentChip)) {
            CharSequence text = currentChip.getValue();
            Editable editable = getText();
            removeChip(currentChip);
            editable.append(text);
            setCursorVisible(true);
            setSelection(editable.length());
            setDisableBringPointIntoView(false); /// M: Scroll view when select INVALID_CONTACT or GENERATED_CONTACT
            return new RecipientChip(null, RecipientEntry.constructFakeEntry((String) text), -1);
        } else if (currentChip.getContactId() == RecipientEntry.GENERATED_CONTACT) {
            int start = getChipStart(currentChip);
            int end = getChipEnd(currentChip);
            getSpannable().removeSpan(currentChip);
            RecipientChip newChip;
            try {
                if (mNoChips) {
                    return null;
                }
                newChip = constructChipSpan(currentChip.getEntry(), start, true, false);
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage(), e);
                return null;
            }
            Editable editable = getText();
            QwertyKeyListener.markAsReplaced(editable, start, end, "");
            if (start == -1 || end == -1) {
                Log.d(TAG, "The chip being selected no longer exists but should.");
            } else {
                editable.setSpan(newChip, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            newChip.setSelected(true);
            if (shouldShowEditableText(newChip)) {
                scrollLineIntoView(getLayout().getLineForOffset(getChipStart(newChip)));
            }
            showAddress(newChip, mAddressPopup, getWidth(), getContext());
            setCursorVisible(false);
            setDisableBringPointIntoView(true);  /// M: Don't scroll view when select chip
            return newChip;
        } else {
            int start = getChipStart(currentChip);
            int end = getChipEnd(currentChip);
            getSpannable().removeSpan(currentChip);
            RecipientChip newChip;
            try {
                newChip = constructChipSpan(currentChip.getEntry(), start, true, false);
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage(), e);
                return null;
            }
            Editable editable = getText();
            QwertyKeyListener.markAsReplaced(editable, start, end, "");
            if (start == -1 || end == -1) {
                Log.d(TAG, "The chip being selected no longer exists but should.");
            } else {
                editable.setSpan(newChip, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            newChip.setSelected(true);
            if (shouldShowEditableText(newChip)) {
                scrollLineIntoView(getLayout().getLineForOffset(getChipStart(newChip)));
            }
            showAlternates(newChip, mAlternatesPopup, getWidth(), getContext());
            setCursorVisible(false);
            setDisableBringPointIntoView(true);  /// M: Don't scroll view when select chip
            return newChip;
        }
    }

    private boolean shouldShowEditableText(RecipientChip currentChip) {
        long contactId = currentChip.getContactId();
        return contactId == RecipientEntry.INVALID_CONTACT
                || (!isPhoneQuery() && contactId == RecipientEntry.GENERATED_CONTACT);
    }

    private void showAddress(final RecipientChip currentChip, final ListPopupWindow popup,
            int width, Context context) {
        int line = getLayout().getLineForOffset(getChipStart(currentChip));
        int bottom = getOffsetFromBottom(line); /// M: Locate drop-down list at proper position
        // Align the alternates popup with the left side of the View,
        // regardless of the position of the chip tapped.
        popup.setWidth(width);
        popup.setAnchorView(this);
        popup.setVerticalOffset(bottom + mDefaultVerticalOffset); /// M: Adjust position of popup window
        popup.setAdapter(createSingleAddressAdapter(currentChip));
        popup.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                unselectChip(currentChip);
                popup.dismiss();
            }
        });
        popup.show();
        ListView listView = popup.getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setItemChecked(0, true);
    }

    /**
     * Remove selection from this chip. Unselecting a RecipientChip will render
     * the chip without a delete icon and with an unfocused background. This is
     * called when the RecipientChip no longer has focus.
     */
    private void unselectChip(RecipientChip chip) {
        int start = getChipStart(chip);
        int end = getChipEnd(chip);
        Editable editable = getText();
        mSelectedChip = null;
        if (start == -1 || end == -1) {
            Log.w(TAG, "The chip doesn't exist or may be a chip a user was editing");
            setSelection(editable.length());
            commitDefault();
        } else {
            getSpannable().removeSpan(chip);
            QwertyKeyListener.markAsReplaced(editable, start, end, "");
            editable.removeSpan(chip);
            try {
                if (!mNoChips) {
                    editable.setSpan(constructChipSpan(chip.getEntry(), start, false, false),
                            start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        setCursorVisible(true);
        setSelection(editable.length());
        if (mAlternatesPopup != null && mAlternatesPopup.isShowing()) {
            mAlternatesPopup.dismiss();
        }
        /// M: Dismiss mAddressPopup. @{
        if (mAddressPopup != null && mAddressPopup.isShowing()) {
            mAddressPopup.dismiss();
        }
        /// @}
    }

    /**
     * Return whether a touch event was inside the delete target of
     * a selected chip. It is in the delete target if:
     * 1) the x and y points of the event are within the
     * delete assset.
     * 2) the point tapped would have caused a cursor to appear
     * right after the selected chip.
     * @return boolean
     */
    private boolean isInDelete(RecipientChip chip, int offset, float x, float y) {
        // Figure out the bounds of this chip and whether or not
        // the user clicked in the X portion.
        return chip.isSelected() && offset == getChipEnd(chip);
    }

    /**
     * Remove the chip and any text associated with it from the RecipientEditTextView.
     */
    // Visible for testing.
    /*pacakge*/ void removeChip(RecipientChip chip) {
        Spannable spannable = getSpannable();
        int spanStart = spannable.getSpanStart(chip);
        int spanEnd = spannable.getSpanEnd(chip);
        Editable text = getText();
        int toDelete = spanEnd;
        boolean wasSelected = chip == mSelectedChip;
        // Clear that there is a selected chip before updating any text.
        if (wasSelected) {
            mSelectedChip = null;
        }
        // Always remove trailing spaces when removing a chip.
        while (toDelete >= 0 && toDelete < text.length() && text.charAt(toDelete) == ' ') {
            toDelete++;
        }
        /// M: No need to adopt accelerate mechanism if removed last chip. @{
        boolean needAccelerate = true;
        RecipientChip lastChip = getLastChip();
        if ((getChipStart(chip) == getChipStart(lastChip)) && (getChipEnd(chip) == getChipEnd(lastChip))) {
            needAccelerate = false;
        }
        /// @}
        spannable.removeSpan(chip);
        /// M: No need to adopt acclerate mechanism if no chip left. @{
        RecipientChip[] chips = getSortedRecipients();
        if ((chips == null || chips.length == 0)) {
            needAccelerate = false;
        }
        /// @}
        /// M: Temporarily remove all the chips after removedChip, and save them for later use. @{
        RecipientChipProcessor rcp = new RecipientChipProcessor();
        if (needAccelerate) {
            /// M: Get start index of removed chips
            int index = 0;
            for (index = 0; index < chips.length; index++) {
                if (getChipStart(chips[index]) >= spanStart) {
                    break;
                }
            }
            rcp.removeChipsWithoutNotification(index, chips.length);
            mDuringAccelerateRemoveChip = true;
        }
        /// @}
        if (spanStart >= 0 && toDelete > 0) {
            text.delete(spanStart, toDelete);
        }
        /// M: Add all the temporarily removed chips back. @{
        if (needAccelerate) {
            mDuringAccelerateRemoveChip = false;
            rcp.addChipsBackWithoutNotification(spanEnd - spanStart + 1);
        }

        /// @}
        if (wasSelected) {
            clearSelectedChip();
        }
    }

    /**
     * Replace this currently selected chip with a new chip
     * that uses the contact data provided.
     */
    // Visible for testing.
    /*package*/ void replaceChip(RecipientChip chip, RecipientEntry entry) {
        boolean wasSelected = chip == mSelectedChip;
        if (wasSelected) {
            mSelectedChip = null;
        }
        int start = getChipStart(chip);
        int end = getChipEnd(chip);
        getSpannable().removeSpan(chip);
        Editable editable = getText();
        CharSequence chipText = createChip(entry, false);
        /// M: No need to adopt accelerate mechanism if replaced last chip. @{
        boolean needAccelerate = true;
        RecipientChip lastChip = getLastChip();
        if ((getChipStart(chip) == getChipStart(lastChip)) && (getChipEnd(chip) == getChipEnd(lastChip))) {
            needAccelerate = false;
        }
        /// @}
        /// M: Temporarily remove all the chips after replacedChip, and save them for later use. @{
        RecipientChipProcessor rcp = new RecipientChipProcessor();
        RecipientChip[] currChips = getSortedRecipients();
        Spannable spannable = getSpannable();
        if (needAccelerate) {
            /// M: Get start index of removed chips
            int index = 0;
            for (index = 0; index < currChips.length; index++) {
                if (getChipStart(currChips[index]) >= start) {
                    break;
                }
            }
            rcp.removeChipsWithoutNotification(index, currChips.length);
            mDuringAccelerateRemoveChip = true;
        }
        /// @}
        if (chipText != null) {
            if (start == -1 || end == -1) {
                Log.e(TAG, "The chip to replace does not exist but should.");
                editable.insert(0, chipText);
            } else {
                if (!TextUtils.isEmpty(chipText)) {
                    // There may be a space to replace with this chip's new
                    // associated space. Check for it
                    int toReplace = end;
                    while (toReplace >= 0 && toReplace < editable.length()
                            && editable.charAt(toReplace) == ' ') {
                        toReplace++;
                    }
                    editable.replace(start, toReplace, chipText);
                }
            }
        }
        /// M: Add all the temporarily removed chips back. @{
        if (needAccelerate) {
            mDuringAccelerateRemoveChip = false;
            rcp.addChipsBackWithoutNotification((end - start + 1) - chipText.length());
        }
        /// @}
        setCursorVisible(true);
        if (wasSelected) {
            clearSelectedChip();
        }
    }

    /**
     * Handle click events for a chip. When a selected chip receives a click
     * event, see if that event was in the delete icon. If so, delete it.
     * Otherwise, unselect the chip.
     */
    public void onClick(RecipientChip chip, int offset, float x, float y) {
        if (chip.isSelected()) {
            if (isInDelete(chip, offset, x, y)) {
                /// M: Scroll view when needed. @{
                if (isPhoneQuery()) {
                    int lineCount = getLineCount();
                    int maxLine = getMaxLines();
                    if ((maxLine != 1) && (lineCount > maxLine)) {
                        Layout layout = getLayout();
                        int offsetToBottom = layout.getLineTop(Math.max(lineCount-(maxLine-1), 0));
                        int offsetY = getScrollY() + layout.getLineTop(Math.min(lineCount, maxLine));
                        if (offsetY <= offsetToBottom) {
                            setDisableBringPointIntoView(true);
                        }
                    }
                } else {
                    setDisableBringPointIntoView(true);  /// M: Don't scroll view when remove a chip
                }
                /// @}
                removeChip(chip);
            } else {
                clearSelectedChip();
            }
        }
    }

    private boolean chipsPending() {
        return mPendingChipsCount > 0 || (mRemovedSpans != null && mRemovedSpans.size() > 0);
    }

    @Override
    public void removeTextChangedListener(TextWatcher watcher) {
        mTextWatcher = null;
        super.removeTextChangedListener(watcher);
    }

    private class RecipientTextWatcher implements TextWatcher {

        @Override
        public void afterTextChanged(Editable s) {
            /// M: Always set cursor to true if there's any text changed in case cursor disappear.
            setCursorVisible(true);
            // If the text has been set to null or empty, make sure we remove
            // all the spans we applied.
            if (TextUtils.isEmpty(s)) {
                // Remove all the chips spans.
                Spannable spannable = getSpannable();
                RecipientChip[] chips = spannable.getSpans(0, getText().length(),
                        RecipientChip.class);
                for (RecipientChip chip : chips) {
                    spannable.removeSpan(chip);
                }
                if (mMoreChip != null) {
                    spannable.removeSpan(mMoreChip);
                }
                return;
            }
            // Get whether there are any recipients pending addition to the
            // view. If there are, don't do anything in the text watcher.
            if (chipsPending()) {
                return;
            }
            // If the user is editing a chip, don't clear it.
            if (mSelectedChip != null
                    && mSelectedChip.getContactId() != RecipientEntry.INVALID_CONTACT) { /// M: Rollback to JB 4.1 
                setSelection(getText().length());
                clearSelectedChip();
            }
            /// M:
            if (mDuringAccelerateRemoveChip) {
                return;
            }
            /// @}
            int length = s.length();
            // Make sure there is content there to parse and that it is
            // not just the commit character.
            if (length > 1) {
                char last;
                int end = getSelectionEnd() == 0 ? 0 : getSelectionEnd() - 1;
                int len = length() - 1;
                if (end != len) {
                    last = s.charAt(end);
                } else {
                    last = s.charAt(len);
                }
                if (last == COMMIT_CHAR_SEMICOLON || last == COMMIT_CHAR_COMMA || last == COMMIT_CHAR_CHINESE_COMMA || last == COMMIT_CHAR_CHINESE_SEMICOLON) {
                    /// M: Replace chinese comma or semiconlon to english one. @{
                    if (last == COMMIT_CHAR_CHINESE_COMMA) {
                        getText().replace(end, end + 1, Character.toString(COMMIT_CHAR_COMMA));
                        return;
                    }
                    else if (last == COMMIT_CHAR_CHINESE_SEMICOLON) {
                        getText().replace(end, end + 1, Character.toString(COMMIT_CHAR_SEMICOLON));
                        return;
                    }
                    /// @}
                    commitByCharacter();
                } else if (last == COMMIT_CHAR_SPACE) {
                    if (!isPhoneQuery()) {
                        // Check if this is a valid email address. If it is,
                        // commit it.
                        String text = getText().toString();
                        int tokenStart = mTokenizer.findTokenStart(text, getSelectionEnd());
                        String sub = text.substring(tokenStart, mTokenizer.findTokenEnd(text,
                                tokenStart));
                        if (!TextUtils.isEmpty(sub) && mValidator != null &&
                                mValidator.isValid(sub)) {
                            commitByCharacter();
                        }
                    }
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // This is a delete; check to see if the insertion point is on a space
            // following a chip.
            if (before > count) {
                // If the item deleted is a space, and the thing before the
                // space is a chip, delete the entire span.
                int selStart = getSelectionStart();
                RecipientChip[] repl = getSpannable().getSpans(selStart, selStart,
                        RecipientChip.class);
                if (repl.length > 0) {
                    // There is a chip there! Just remove it.
                    Editable editable = getText();
                    // Add the separator token.
                    int tokenStart = mTokenizer.findTokenStart(editable, selStart);
                    int tokenEnd = mTokenizer.findTokenEnd(editable, tokenStart);
                    tokenEnd = tokenEnd + 1;
                    if (tokenEnd > editable.length()) {
                        tokenEnd = editable.length();
                    }
                    /// M: Dismiss drop-down list & clear slected chip if necessary. @{
                    if (mSelectedChip != null) {
                        if (tokenStart == getChipStart(mSelectedChip) && tokenEnd == getChipEnd(mSelectedChip)) {
                            if (mAlternatesPopup != null && mAlternatesPopup.isShowing()) {
                                mAlternatesPopup.dismiss();
                            }
                            if (mAddressPopup != null && mAddressPopup.isShowing()) {
                                mAddressPopup.dismiss();
                            }
                            mSelectedChip = null;
                        }
                    }
                    /// @}
                    editable.delete(tokenStart, tokenEnd);
                    getSpannable().removeSpan(repl[0]);
                }
            } else if (count > before && !mDisableBringPointIntoView) { /// M: scroll when enable.
                // Only scroll when the user is adding text, not clearing text.
                scrollBottomIntoView();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Do nothing.
        }
    }

    private void scrollBottomIntoView() {
        if (mScrollView != null) {
            mScrollView.scrollBy(0, (int)(getLineCount() * mChipHeight));
        }
    }

    /**
     * Handles pasting a {@link ClipData} to this {@link RecipientEditTextView}.
     */
    private void handlePasteClip(ClipData clip) {
        removeTextChangedListener(mTextWatcher);
        /// M: Remove white spaces at the end of text. @{
        do {
            int index = 0;
            RecipientChip lastChip = getLastChip();
            if (lastChip != null) {
                index = getChipEnd(lastChip) + 1;
            }
            int selEnd = getSelectionEnd();
            if (selEnd == index || selEnd == 0) {
                /// M: No extra space
                break;
            }
            int x = selEnd;
            String text = getText().toString();
            while (x > index && text.charAt(x - 1) == ' ') {
                x--;
            }
            if ((x - 1) > index && x < (text.length() - 1) && text.charAt(x) == ' ') {
                /// M: Leave one space if needed
                x++;
            }
            getText().delete(x, selEnd);
        } while (false);
        /// @}

        if (clip != null && clip.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)){
            for (int i = 0; i < clip.getItemCount(); i++) {
                CharSequence paste = clip.getItemAt(i).getText();
                if (paste != null) {
                    paste = filterInvalidCharacter(paste);  /// M: replace the invalid character refer to android2.3 Email Chips.          
                    int start = getSelectionStart();
                    int end = getSelectionEnd();
                    Editable editable = getText();
                    if (start >= 0 && end >= 0 && start != end) {
                        /// M: replace the selected text. set cursor to the end. @{                         
                        editable.replace(start, end, paste);                        
                        setSelection(editable.length());                        
                        /// @}
                    } else {
                        editable.insert(end, paste);
                    }
                    handlePasteAndReplace();
                }
            }
        }

        mHandler.post(mAddTextWatcher);
    }

    /** M: filter invalid character from the string.
     * replace '\n' to ' '
     * replace the one or more ' '(white space) in the beginning of a string to ""
     * A string contains "0 or more ' '(white space) following a ','(comma)" repeat one or more will be replaced to a ','
     * @param source string.
     * @return the processed string.
     */
    private CharSequence filterInvalidCharacter(CharSequence source) {
        String result = source.toString();
        /// M: The '\n' in the middle of the span which cause IndexOutOfBoundsException.
        result = result.replaceAll("\n", " ");
        /// M: String contains chinese comma and semicolon will be replaced to a ','
        result = result.replace(COMMIT_CHAR_CHINESE_COMMA, COMMIT_CHAR_COMMA);
        /// M: String contains chinese semicolon will be replaced to a ';'
        result = result.replace(COMMIT_CHAR_CHINESE_SEMICOLON, COMMIT_CHAR_SEMICOLON);
        /// M: Replace the "0 or more ' '(white space) following a ','(comma)" repeat one or more in the beginning of a string to ""
        result = result.replaceAll("^( *,)+", "");
        /// M: String contains "0 or more ' '(white space) following a ','(comma)" repeat one or more will be replaced to a ','
        result = result.replaceAll("( *,)+", ",");
        result = result.replaceAll("(, *)+", ", ");
        /// M: Replace the "0 or more ' '(white space) following a ';'(semicolon)" repeat one or more in the beginning of a string to ""
        result = result.replaceAll("^( *;)+", "");
        /// M: String contains "0 or more ' '(white space) following a ';'(semicolon)" repeat one or more will be replaced to a ';'
        result = result.replaceAll("( *;)+", ";");
        result = result.replaceAll("(; *)+", "; ");
        /// M: Trim white spaces at the beginning of string
        result = result.replaceAll("^\\s+","");
        return result;
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == android.R.id.paste) {
            /// M: When touch after paste, don't care filter popup.
            this.bPasted = true;

            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                    Context.CLIPBOARD_SERVICE);
            handlePasteClip(clipboard.getPrimaryClip());
            return true;
        }
        return super.onTextContextMenuItem(id);
    }

    private void handlePasteAndReplace() {
        ArrayList<RecipientChip> created = handlePaste();
        if (created != null && created.size() > 0) {
            // Perform reverse lookups on the pasted contacts.
            IndividualReplacementTask replace = new IndividualReplacementTask();
            replace.execute(created);
        }
    }

    // Visible for testing.
    /* package */ArrayList<RecipientChip> handlePaste() {
        String text = getText().toString();
        int originalTokenStart = mTokenizer.findTokenStart(text, getSelectionEnd());
        String lastAddress = text.substring(originalTokenStart);
        int tokenStart = originalTokenStart;
        int prevTokenStart = tokenStart;
        RecipientChip findChip = null;
        ArrayList<RecipientChip> created = new ArrayList<RecipientChip>();
        if (tokenStart != 0) {
            // There are things before this!
            while (tokenStart != 0 && findChip == null) {
                prevTokenStart = tokenStart;
                tokenStart = mTokenizer.findTokenStart(text, tokenStart);
                findChip = findChip(tokenStart);
                /// M: Stop searching for tokenStart. @{
                if (prevTokenStart == tokenStart) {
                    break;
                }
                /// @}
            }
            if (tokenStart != originalTokenStart) {
                if (findChip != null) {
                    tokenStart = prevTokenStart;
                }
                int tokenEnd;
                RecipientChip createdChip;
                /// M: Fix lost chip problem (strings stay in string state instead of becoming chips) @{
                int parseEnd = originalTokenStart;
                int offsetFromLastString = text.length() - originalTokenStart;
                /// @}
                while (tokenStart < parseEnd) { 
                    tokenEnd = movePastTerminators(mTokenizer.findTokenEnd(getText().toString(),
                            tokenStart));
                    commitChip(tokenStart, tokenEnd, getText());
                    createdChip = findChip(tokenStart);
                    if (createdChip == null) {
                        break;
                    }
                    // +1 for the space at the end.
                    tokenStart = getSpannable().getSpanEnd(createdChip) + 1;
                    created.add(createdChip);
                    /// M: Fix lost chip problem (strings stay in string state instead of becoming chips) 
                    parseEnd = getText().length() - offsetFromLastString;
                }
            }
        }
        // Take a look at the last token. If the token has been completed with a
        // commit character, create a chip.
        if (isCompletedToken(lastAddress)) {
            Editable editable = getText();
            tokenStart = editable.toString().indexOf(lastAddress, tokenStart); /// M: Use tokenStart after text processing in case wrong index value lead to JE.
            commitChip(tokenStart, editable.length(), editable);
            created.add(findChip(tokenStart));
        }
        return created;
    }

    // Visible for testing.
    /* package */int movePastTerminators(int tokenEnd) {
        if (tokenEnd >= length()) {
            return tokenEnd;
        }
        char atEnd = getText().toString().charAt(tokenEnd);
        if (atEnd == COMMIT_CHAR_COMMA || atEnd == COMMIT_CHAR_SEMICOLON) {
            tokenEnd++;
        }
        // This token had not only an end token character, but also a space
        // separating it from the next token.
        if (tokenEnd < length() && getText().toString().charAt(tokenEnd) == ' ') {
            tokenEnd++;
        }
        return tokenEnd;
    }

    private class RecipientReplacementTask extends AsyncTask<Void, Void, Void> {
        private RecipientChip createFreeChip(RecipientEntry entry) {
            try {
                if (mNoChips) {
                    return null;
                }
                return constructChipSpan(entry, -1, false,
                        false /*leave space for contact icon */);
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG,"[Debug] RecipientReplacementTask,inBG-start");  /// M: TempDebugLog
            if (mIndividualReplacements != null) {
                mIndividualReplacements.cancel(true);
            }
            // For each chip in the list, look up the matching contact.
            // If there is a match, replace that chip with the matching
            // chip.
            final ArrayList<RecipientChip> originalRecipients = new ArrayList<RecipientChip>();
            RecipientChip[] existingChips = getSortedRecipients();
            for (int i = 0; i < existingChips.length; i++) {
                originalRecipients.add(existingChips[i]);
            }
            if (mRemovedSpans != null) {
                originalRecipients.addAll(mRemovedSpans);
            }
            ArrayList<String> addresses = new ArrayList<String>();
            RecipientChip chip;
            for (int i = 0; i < originalRecipients.size(); i++) {
                chip = originalRecipients.get(i);
                if (chip != null) {
                    addresses.add(createAddressText(chip.getEntry()));
                }
            }
            HashMap<String, RecipientEntry> entries = RecipientAlternatesAdapter
                    .getMatchingRecipients(getContext(), addresses);
            final ArrayList<RecipientChip> replacements = new ArrayList<RecipientChip>();
            for (final RecipientChip temp : originalRecipients) {
                RecipientEntry entry = null;
                if (RecipientEntry.isCreatedRecipient(temp.getEntry().getContactId())
                        && getSpannable().getSpanStart(temp) != -1) {
                    // Replace this.
                    /// M: Query with normalized number again if there's no fully matched number. @{
                    if (isPhoneQuery()) {
                        String tokenizedAddress = tokenizeAddress(temp.getEntry().getDestination());
                        entry = entries.get(tokenizedAddress);
                        if (entry == null) {
                            entry = RecipientAlternatesAdapter.getRecipientEntryByPhoneNumber(getContext(), tokenizedAddress);
                        }
                    } else { /// @}
                        entry = createValidatedEntry(entries.get(tokenizeAddress(temp.getEntry()
                                .getDestination())));
                    }
                }
                if (entry != null) {
                    replacements.add(createFreeChip(entry));
                } else {
                    replacements.add(temp);
                }
            }
            if (replacements != null && replacements.size() > 0) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,"[Debug] RecipientReplacementTask,run-start");  /// M: TempDebugLog
                        Editable oldText = getText();
                        int start, end;
                        int i = 0;
                        /// M: We don't care about watching text and span changes while in the middle of handling pending chips. @{
                        int totalChips = replacements.size();
                        watcherProcessor wp = null;
                        if (totalChips > 0) {
                            wp = new watcherProcessor();
                            wp.initWatcherProcessor();
                            wp.removeSpanWatchers();
                        }
                        /// @}
                        for (RecipientChip chip : originalRecipients) {
                            /// M: Add text and span watchers back before handling last chip. @{
                            if (i == (totalChips - 1)) {
                                if(wp != null) {
                                    wp.addSpanWatchers();
                                }
                                /// M: Let text and span watchers work corectly by reset selection and layout. @{
                                setSelection(getText().length());
                                requestLayout();
                                /// @}
                            }
                            /// @}
                            // Find the location of the chip in the text currently shown.
                            start = oldText.getSpanStart(chip);
                            if (start != -1) {
                                end = oldText.getSpanEnd(chip);
                                oldText.removeSpan(chip);
                                RecipientChip replacement = replacements.get(i);
                                // Trim any whitespace, as we will already have
                                // it added if these are replacement chips.
                                SpannableString displayText = new SpannableString(
                                        createAddressText(replacement.getEntry()).trim());
                                displayText.setSpan(replacement, 0, displayText.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                // Replace the old text we found with with the new display text,
                                // which now may also contain the display name of the recipient.
                                oldText.replace(start, end, displayText);
                                replacement.setOriginalText(displayText.toString());
                            } else if (!hasFocus()) {
                                /// M: Fix part of chips didn't correctly be replaced to the chip with contact info. @{
                                RecipientChip replacement = replacements.get(i);
                                if (replacement != null) {
                                    if (mTemporaryRecipients == null) {
                                        mTemporaryRecipients = new ArrayList<RecipientChip>();
                                    }
                                    mTemporaryRecipients.add(replacement);
                                }
                                /// @}
                            }
                            i++;
                        }
                        originalRecipients.clear();
                        /// M: Let text and span watchers work corectly by reset selection and layout. @{
                        if (totalChips > 0) {
                            recoverLayout();
                        }
                        /// @}
                        Log.d(TAG,"[Debug] RecipientReplacementTask,run-end");  /// M: TempDebugLog
                    }
                });
            }
            Log.d(TAG,"[Debug] RecipientReplacementTask,inBG-end");  /// M: TempDebugLog
            return null;
        }
    }

    private class IndividualReplacementTask extends AsyncTask<Object, Void, Void> {
        @SuppressWarnings("unchecked")
        @Override
        protected Void doInBackground(Object... params) {
            Log.d(TAG,"[Debug] IndividualReplacementTask,inBG-start");  /// M: TempDebugLog
            // For each chip in the list, look up the matching contact.
            // If there is a match, replace that chip with the matching
            // chip.
            final ArrayList<RecipientChip> originalRecipients =
                    (ArrayList<RecipientChip>) params[0];
            ArrayList<String> addresses = new ArrayList<String>();
            RecipientChip chip;
            for (int i = 0; i < originalRecipients.size(); i++) {
                chip = originalRecipients.get(i);
                if (chip != null) {
                    addresses.add(createAddressText(chip.getEntry()));
                }
            }
            HashMap<String, RecipientEntry> entries = RecipientAlternatesAdapter
                    .getMatchingRecipients(getContext(), addresses);
            for (final RecipientChip temp : originalRecipients) {
                if (RecipientEntry.isCreatedRecipient(temp.getEntry().getContactId())
                        && getSpannable().getSpanStart(temp) != -1) {
                    // Replace this.
                    /// M: If destination is a normal phone number (which means without "<",...), use it directly. @{
                    String destination = temp.getEntry().getDestination();
                    RecipientEntry entry = null;
                    if (isPhoneNumber(destination)) {
                        entry = createValidatedEntry(entries.get(destination));
                    } else {
                        entry = createValidatedEntry(entries.get(tokenizeAddress(destination.toLowerCase())));
                    }
                    ///@}
                    // If we don't have a validated contact match, just use the
                    // entry as it existed before.
                    if (entry == null && !isPhoneQuery()) {
                        entry = temp.getEntry();
                    }
                    final RecipientEntry tempEntry = entry;
                    if (tempEntry != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                replaceChip(temp, tempEntry);
                            }
                        });
                    }
                }
            }
            Log.d(TAG,"[Debug] IndividualReplacementTask,inBG-end");  /// M: TempDebugLog
            return null;
        }
    }


    /**
     * MoreImageSpan is a simple class created for tracking the existence of a
     * more chip across activity restarts/
     */
    private class MoreImageSpan extends ImageSpan {
        public MoreImageSpan(Drawable b) {
            super(b);
        }
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // Do nothing.
        return false;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        if (mSelectedChip != null) {
            return;
        }
        float x = event.getX();
        float y = event.getY();
        int offset = putOffsetInRange(getOffsetForPosition(x, y));
        /// M: Fix misrecognize a touch event is located in a chip while it's actually out side of the chip.
        RecipientChip currentChip = (isTouchPointInChip(x,y)) ? (findChip(offset)) : (null); 
        if (currentChip != null) {
            if (mDragEnabled) {
                // Start drag-and-drop for the selected chip.
                startDrag(currentChip);
            } else {
                // Copy the selected chip email address.
                showCopyDialog(currentChip.getEntry().getDestination());
            }
        }
    }

    /**
     * Enables drag-and-drop for chips.
     */
    public void enableDrag() {
        mDragEnabled = true;
    }

    /**
     * Starts drag-and-drop for the selected chip.
     */
    private void startDrag(RecipientChip currentChip) {
        String address = currentChip.getEntry().getDestination();
        ClipData data = ClipData.newPlainText(address, address + COMMIT_CHAR_COMMA);

        // Start drag mode.
        startDrag(data, new RecipientChipShadow(currentChip), null, 0);

        // Remove the current chip, so drag-and-drop will result in a move.
        // TODO (phamm): consider readd this chip if it's dropped outside a target.
        removeChip(currentChip);
    }

    /**
     * Handles drag event.
     */
    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Only handle plain text drag and drop.
                return event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
            case DragEvent.ACTION_DRAG_ENTERED:
                requestFocus();
                return true;
            case DragEvent.ACTION_DROP:
                handlePasteClip(event.getClipData());
                return true;
        }
        return false;
    }

    /**
     * Drag shadow for a {@link RecipientChip}.
     */
    private final class RecipientChipShadow extends DragShadowBuilder {
        private final RecipientChip mChip;

        public RecipientChipShadow(RecipientChip chip) {
            mChip = chip;
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            Rect rect = mChip.getDrawable().getBounds();
            shadowSize.set(rect.width(), rect.height());
            shadowTouchPoint.set(rect.centerX(), rect.centerY());
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            mChip.getDrawable().draw(canvas);
        }
    }

    private void showCopyDialog(final String address) {
        mCopyAddress = address;
        mCopyDialog.setTitle(address);
        mCopyDialog.setContentView(R.layout.copy_chip_dialog_layout);
        mCopyDialog.setCancelable(true);
        mCopyDialog.setCanceledOnTouchOutside(true);
        Button button = (Button)mCopyDialog.findViewById(android.R.id.button1);
        button.setOnClickListener(this);
        int btnTitleId;
        if (isPhoneQuery()) {
            btnTitleId = R.string.copy_number;
        } else {
            btnTitleId = R.string.copy_email;
        }
        String buttonTitle = getContext().getResources().getString(btnTitleId);
        button.setText(buttonTitle);
        mCopyDialog.setOnDismissListener(this);
        mCopyDialog.show();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        // Do nothing.
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // Do nothing.
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // Do nothing.
        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mCopyAddress = null;
    }

    @Override
    public void onClick(View v) {
        // Copy this to the clipboard.
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("", mCopyAddress));
        mCopyDialog.dismiss();
    }

    protected boolean isPhoneQuery() {
        return getAdapter() != null
                && ((BaseRecipientAdapter) getAdapter()).getQueryType()
                    == BaseRecipientAdapter.QUERY_TYPE_PHONE;
    }

    /**
     * M: set whether to scroll when adding text
     * @param enable whether to scroll when adding text
     * @hide
     */
    public void setScrollAddText(boolean enable) {
        mEnableScrollAddText = enable;
    }
    
    /**
     * M: whether enable to scroll when adding text
     * @hide
     */
    public boolean isScrollAddText() {
        return mEnableScrollAddText;
    }
    
    /// M: whether enable scroll when adding text.
    private boolean mEnableScrollAddText = true;
    
    /**
     * M: Set whether discard next action up.
     * @param enable true is enable; false is disable.
     * @hide
     */
    protected void setEnableDiscardNextActionUp(boolean enable) {
        mRETVDiscardNextActionUp = enable;
    }
    
    /**
     * M: Get whether discard next action up.
     * @hide
     */
    protected boolean getEnableDiscardNextActionUp() {
        return mRETVDiscardNextActionUp;
    }
    

    /// M: Whether discard next action up.
    private boolean mRETVDiscardNextActionUp =false;

    /// M: When touch after paste, don't care filter popup.
    private boolean bPasted = false;
    private boolean bTouchedAfterPasted = false;


    /// M: Limit width for construct chip span
    private int mLimitedWidthForSpan = -1;
    /// M: Whether first chip has been ellipsized before
    private boolean mHasEllipsizedFirstChip = false;
    
    /**
     * M: Get width of the chip.
     * @param chip 
     * @hide
     */
    private int getChipWidth(RecipientChip chip){
        return chip.getDrawable().getBounds().width();
    }

    /**
     * M: Get interval between chips.
     * @hide
     */
    private int getChipInterval() {
        float[] widths = new float[1];
        TextPaint paint = getPaint();
        paint.getTextWidths(" ", widths);
        return (int)(widths[0]);
    }

    /**
     * M: Get width of moreSpan approximately.
     * @param count how many chip is in moreChip
     * @hide
     */
    private int getMeasuredMoreSpanWidth(int count){
        String moreText = String.format(mMoreItem.getText().toString(), count);
        TextPaint morePaint = new TextPaint(getPaint());
        morePaint.setTextSize(mMoreItem.getTextSize());
        return (int)morePaint.measureText(moreText) + mMoreItem.getPaddingLeft()+ mMoreItem.getPaddingRight();
    }

    /**
     * M: Replace currentChip from its start position to its end position with same contact but new chip.
     * @param currentChip chip to be replaced
     * @param newChipWidth the width of new chip if need to do replacement
     * @hide
     */
    private void replaceChipOnSameTextRange(RecipientChip currentChip, int newChipWidth){
        int start = getChipStart(currentChip);
        int end = getChipEnd(currentChip);
        mLimitedWidthForSpan = newChipWidth;
        RecipientEntry entry = currentChip.getEntry();
        RecipientChip ellipsizeRecipient = constructChipSpan(currentChip.getEntry(), start, false, 
                                                             TextUtils.isEmpty(entry.getDisplayName()) || 
                                                             TextUtils.equals(entry.getDisplayName(),entry.getDestination()));
        mLimitedWidthForSpan = -1;
        getSpannable().removeSpan(currentChip);
        Editable text = getText();
        QwertyKeyListener.markAsReplaced(text, start, end, "");
        text.setSpan(ellipsizeRecipient, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * M: Calculate how many chips can be accommodated in one line (need to take moreChip into consideration).
     */
    private int calculateNumChipsCanShow() {
        RecipientChip[] recipients = getSortedRecipients();
        if (recipients == null || recipients.length == 0) {
            return 0;
        }

        int innerWidth = (int) calculateAvailableWidth(false);
        int numRecipients = recipients.length;
        int overage = 0;

        /// M: Get chip interval
        int chipInterval = getChipInterval();

        boolean canShowAll = true;
        int occupiedSpace = 0;

        int index=0;
        for (index=0; index < numRecipients; index++) {
            occupiedSpace += getChipWidth(recipients[index]) + chipInterval;

            if (occupiedSpace > innerWidth) {
                canShowAll = false;
                break;
            }
        }

        if (canShowAll) {
            return numRecipients;
        }

        if ((index == numRecipients) && !canShowAll) {
            index--;
        }

        int moreSpanWidth = getMeasuredMoreSpanWidth(numRecipients); /// M: For measuring approximate moreSpan width
        int chipsSpace = innerWidth - moreSpanWidth;

        int j=0;
        for (j=index; j>=0; j--) {
            occupiedSpace -= getChipWidth(recipients[j]) - chipInterval;

            if (occupiedSpace < chipsSpace) {
                break;
            }
        }

        if (j==0) {
            if (getChipWidth(recipients[0]) > chipsSpace) {
                /// M: need to ellipsize 1st chip becauese space is not enough, then replace the original one
                replaceChipOnSameTextRange(recipients[0],chipsSpace);
                mHasEllipsizedFirstChip = true;
            }
            return 1;
        } else {
            return j;
        }
    }

    /// M: Whether to bring point into view when manipulating
    private boolean mMoveCursorToVisible = false;
    private boolean mDisableBringPointIntoView = false;

    /**
     * M: Overide to disable moveCursorToVisibleOffset in certain case.
     */
    @Override
    public boolean moveCursorToVisibleOffset() {
        if (isPhoneQuery() && !mMoveCursorToVisible) {
            mMoveCursorToVisible = true;
            return false;
        } else {
            return super.moveCursorToVisibleOffset();
        }
        
    }

    /**
     * M: Set whether to disable bringPointIntoView.
     * @param disableBringPointIntoView 
     */  
    private void setDisableBringPointIntoView(boolean disableBringPointIntoView) {
        mDisableBringPointIntoView = disableBringPointIntoView;
    }
    
    /**
     * M: Overide to let bringPointIntoView only be triggered in certain cases.
     * @param offset 
     */
    @Override
    public boolean bringPointIntoView(int offset) {
        if (!mDisableBringPointIntoView ) {
            return super.bringPointIntoView(offset);
        } else {
            return false;
        }
    }

    /**
     * M: Override to reset settings of bringPointIntoView.
     */
    @Override
    public boolean onPreDraw() {
        boolean changed = super.onPreDraw();  
        setDisableBringPointIntoView(false);  /// M: After one manipulation, reset it to false.
        return changed;
    }

    /// M: For appending strings in batch processing
    private ArrayList<String> mPendingStrings = new ArrayList<String>();
    private boolean mDuringAppendStrings = false;

    /**
     * M: Append strings in batch processing.
     */
    private void appendPendingStrings() {
        Log.d(TAG,"[Debug] appendPendingStrings-start");  /// M: TempDebugLog
        int pendingStringsCount = (mPendingStrings != null)? (mPendingStrings.size()): 0;
        if (pendingStringsCount <= 0) {
            Log.d(TAG,"[Debug] appendPendingStrings-end (null)");  /// M: TempDebugLog
            return;
        }
        mDuringAppendStrings = true;
        String str = "";
        for (int x=0; x<pendingStringsCount ; x++) {
            str += mPendingStrings.get(x);
        }
        append(str, 0, str.length());
        mPendingStrings.clear();
        mDuringAppendStrings = false; 
        Log.d(TAG,"[Debug] appendPendingStrings-end");  /// M: TempDebugLog
    }

    /**
     * M: Manipulate removing and adding span watchers for improving performance.
     */
    private class watcherProcessor{
        private SpanWatcher[] mSpanWatchers;
        private int[] mSpanFlags;
        private int mSpanWatchersNum;

        public watcherProcessor(){
            mSpanWatchers = null;    
            mSpanFlags = null;
            mSpanWatchersNum = 0;
        }

        public void initWatcherProcessor(){
            mSpanWatchers = getSpannable().getSpans(0, getText().length(), SpanWatcher.class);
            mSpanWatchersNum = mSpanWatchers.length; 
            mSpanFlags = new int[mSpanWatchersNum];
        }

        public void removeSpanWatchers(){
            for (int x=0; x<mSpanWatchersNum ; x++) {
                tempLogPrint("removeSpanWatchers",mSpanWatchers[x]);  /// M: TempDebugLog
                mSpanFlags[x] = getSpannable().getSpanFlags(mSpanWatchers[x]);
                if (mSpanWatchers[x] instanceof TextWatcher) {
                    tempLogPrint("removeSpanWatchers, remove - ",mSpanWatchers[x]);  /// M: TempDebugLog
                    getSpannable().removeSpan(mSpanWatchers[x]);
                } 
            }
        }

        public void addSpanWatchers(){
            for (int x=0; x<mSpanWatchersNum ; x++) {
                if (mSpanWatchers[x] instanceof TextWatcher) {
                    getSpannable().setSpan(mSpanWatchers[x], 0, getText().length(),mSpanFlags[x]);
                    tempLogPrint("addSpanWatchers, add - ",mSpanWatchers[x]);  /// M: TempDebugLog
                }
            } 
        }        
    }

    /**
     * M: Get offset from bottom to show drop-down list in proper position.
     * @param line The line in which selected chip located
     */
    private int getOffsetFromBottom(int line) {
        int bottom;
        if (line == getLineCount() -1) {
            bottom = 0;
        } else {
            int offsetFromTop = getPaddingTop() + getLayout().getLineTop(line+1);
            bottom = - (getHeight() - (offsetFromTop - getScrollY()));
        }
        return bottom;
    }

    /**
     * M: Check whether cursor is after last chip or not.
     */
    private boolean isEndChip() {
        CharSequence text = getText();
        int end = getSelectionEnd();
        int i = end - 1;
        char c;
        
        while (i > 0 && ((c = text.charAt(i)) != ',' && c != ';')) {
            --i;
        }

        if ((end - i) <= 2 && i != 0 ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * M: Override to avoid showing unnecessary drop-down list.
     */
    @Override
    public void showDropDown() {
        if (isPhoneQuery()) {
            if (!isEndChip()) {
                super.showDropDown();
            } else {
                dismissDropDown();
            }
        } else {
            super.showDropDown();
        }
    }

    /**
     * M: Get whether the touch point is located in chip.
     * @param posX Touch position in x coordinate
     * @param posY Touch position in y coordinate
     * @hide
     */
    protected boolean isTouchPointInChip(float posX, float posY) {
        boolean isInChip = true;
        Layout layout = getLayout();
        if (layout != null)
        {
            int offsetForPosition = getOffsetForPosition(posX, posY);
            int line = layout.getLineForOffset(offsetForPosition);
            float maxX = layout.getPrimaryHorizontal(layout.getLineEnd(line) - 1);
            float currentX = posX - getTotalPaddingLeft();
            currentX = Math.max(0.0f, currentX);
            currentX = Math.min(getWidth() - getTotalPaddingRight() - 1, currentX);
            currentX += getScrollX();
            if(currentX > maxX)
            {
                isInChip = false;
            }
        }
        return isInChip;
    }
    
    /// M: For dealing with configuration changed
    private OnGlobalLayoutListener mGlobalLayoutListener = null;
    private int mPreviousWidth = 0;

    /**
     * M: When configuration changed, if focused, clear selectedChip and dismiss drop-down list if focused.
     *    When configuration changed, if unfocused and during phone query, update moreChip.
     * @hide
     */
    @Override 
    protected void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "[onConfigurationChanged] current view width="+ getWidth() +", height="+ getHeight() +", line count="+ getLineCount());

        if (isFocused()) {
            if (mSelectedChip != null && !shouldShowEditableText(mSelectedChip)) {
                clearSelectedChip();
            }
            dismissDropDown();
        } else {
            if (isPhoneQuery()) {
                registerGlobalLayoutListener();
                mPreviousWidth = getWidth();
            }
        }
    }

    /**
     * M: RegisterGlobalLayoutListener to deal with moreChip when configuration changed.
     */
    private void registerGlobalLayoutListener() {
        ViewTreeObserver viewTreeObs = getViewTreeObserver();
        if( mGlobalLayoutListener == null ){
            mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    Log.d(TAG, "[onGlobalLayout] current view width="+ getWidth() +", height="+ getHeight() +", line count="+ getLineCount());

                    if (mPreviousWidth == getWidth()) {
                        /// M: Width of view haven't been updated
                        return;
                    }

                    boolean isPortrait = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
                    if (isPortrait) {
                        rotateToPortrait();
                    } else {
                        rotateToLandscape();
                    }
                    requestLayout();
                    unRegisterGlobalLayoutListener();
                }
            };
            viewTreeObs.addOnGlobalLayoutListener(mGlobalLayoutListener);
        }
    }

    /**
     * M: UnRegisterGlobalLayoutListener.
     */
    private void unRegisterGlobalLayoutListener() {
        if (mGlobalLayoutListener != null) {
            ViewTreeObserver viewTreeObs = getViewTreeObserver();
            viewTreeObs.removeGlobalOnLayoutListener(mGlobalLayoutListener);
            mGlobalLayoutListener = null;
        }
    }

    /**
     * M: Configuration changed from landscape mode to portrait mode.
     */
    private void rotateToPortrait() {
        RecipientChip[] recipients = getSortedRecipients();
        int numRecipients = recipients.length;

        if (recipients == null || numRecipients== 0) {
            return;
        }

        if (mMoreChip == null) {
            createMoreChip();
            return;
        }

        int innerWidth = (int) calculateAvailableWidth(false);
        int moreSpanWidth = mMoreChip.getDrawable().getBounds().width();
        int chipInterval = getChipInterval();
        int availableSpace = innerWidth - moreSpanWidth - chipInterval;
        int leftSpace = availableSpace;

        int currIndex = 0;
        /// M: Get how many chips can be accommodated in one line
        for (currIndex = 0; currIndex < numRecipients; currIndex++) {
            leftSpace -= (getChipWidth(recipients[currIndex]) + chipInterval);
            if (leftSpace <= 0) {
                break;
            }
        }

        if (currIndex == numRecipients) {
            if (leftSpace >= 0) {
                /// M: Remain same layout as landscape mode
                return;
            } else {
                currIndex -= 1;
            }
        }

        if (numRecipients == 1) {
            if ((currIndex == 0) && (leftSpace < 0)) {
                /// M: Need to ellipsize 1st chip becauese space is not enough, then replace the original one
                replaceChipOnSameTextRange(recipients[0],availableSpace);
                mHasEllipsizedFirstChip = true;
            }
            return;
        } else {
            if (currIndex == 0) {
                currIndex ++;
                if (leftSpace < 0) {
                    /// M: need to ellipsize 1st chip becauese space is not enough, then replace the original one
                    replaceChipOnSameTextRange(recipients[0],availableSpace);
                    mHasEllipsizedFirstChip = true;
                }
            }

            /// M: Update mMoreChip
            Spannable spannable = getSpannable();
            Editable text = getText();
            int recipientSpanStart = spannable.getSpanStart(recipients[currIndex]);
            int moreSpanEnd = spannable.getSpanEnd(mMoreChip);
            int j = 0;
            for (int i = currIndex; i < numRecipients; i++) {
                mRemovedSpans.add(j++, recipients[i]);
                if (mTemporaryRecipients == null || !mTemporaryRecipients.contains(recipients[i])) {
                    int spanStart = spannable.getSpanStart(recipients[i]);
                    int spanEnd = spannable.getSpanEnd(recipients[i]);
                    recipients[i].setOriginalText(text.toString().substring(spanStart, spanEnd));
                }
                spannable.removeSpan(recipients[i]);
            }
            spannable.removeSpan(mMoreChip);
            MoreImageSpan moreSpan = createMoreSpan(mRemovedSpans.size());
            SpannableString chipText = new SpannableString(text.subSequence(recipientSpanStart, moreSpanEnd));
            chipText.setSpan(moreSpan, 0, chipText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.replace(recipientSpanStart, moreSpanEnd, chipText);
            mMoreChip = moreSpan;
        }
    }

    /**
     * M: Configuration changed from portrait mode to landscape mode.
     */
    private void rotateToLandscape() {
        RecipientChip[] recipients = getSortedRecipients();
        int numRecipients = recipients.length;

        if (recipients == null || numRecipients == 0) {
            return;
        }

        /// M: Expand first chip
        replaceChipOnSameTextRange(recipients[0], -1);
        mHasEllipsizedFirstChip = false;
        recipients = getSortedRecipients(); /// M: Get newest recipients

        if (mMoreChip == null) {
            return;
        }

        int innerWidth = (int) calculateAvailableWidth(false);
        int moreSpanWidth = mMoreChip.getDrawable().getBounds().width();
        int chipInterval = getChipInterval();
        int availableSpace = innerWidth; /// M: Don't minus mMoreChip size now
        int leftSpace = availableSpace;

        if (numRecipients == 1) {
            availableSpace -= (moreSpanWidth + chipInterval);
            if ((availableSpace - getChipWidth(recipients[0])) < 0) {
                replaceChipOnSameTextRange(recipients[0], availableSpace);
                mHasEllipsizedFirstChip = true;
                return;
            }
        }

        int currIndex = 0;
        /// M: Minus all existing chip's width
        for (currIndex = 0; currIndex < numRecipients; currIndex++) {
            leftSpace -= (getChipWidth(recipients[0]) + chipInterval);
            if (leftSpace <= 0) {
                break;
            }
        }

        /// M: Check if left space can accommodate all chips in mRemovedSpans
        int i = 0;
        for (i = 0; i < mRemovedSpans.size(); i++) {
            leftSpace -= (getChipWidth(mRemovedSpans.get(i)) + chipInterval);
            if (leftSpace <= 0) {
                break;
            }
        }

        if (i == mRemovedSpans.size()) {
            if (leftSpace >= 0) {
                /// M: All the chips can be shown
                expand();
                return;
            } else {
                i--;
            }
        }

        /// M: Get how many chips can be accommodated in one line (including mMoreChip)
        leftSpace -= moreSpanWidth;
        int j = 0;
        for (j = i; j >= 0; j--) {
            leftSpace += (getChipWidth(mRemovedSpans.get(j)) + chipInterval);
            if (leftSpace >= 0) {
                break;
            }
        }  

        /// M: Add removedSpan back & remove mMoreChip
        Spannable spannable = getSpannable();
        Editable editable = getText();
        int moreSpanStart = spannable.getSpanStart(mMoreChip);
        int moreSpanEnd = spannable.getSpanEnd(mMoreChip);
        spannable.removeSpan(mMoreChip);
        int end = spannable.getSpanEnd(recipients[numRecipients- 1]);
        int chipStart = 0;;
        int chipEnd = end; /// M: Starts from the end of current last recipientChip (the chip just before mMoreChip)
        String token;
        RecipientChip chip = null;
        for (int iteration = 0;  iteration < j; iteration++) {
            chip = mRemovedSpans.get(0); /// M: Always get first removedSpan
            // Need to find the location of the chip, again.
            token = (String) chip.getOriginalText();
            // As we find the matching recipient for the remove spans,
            // reduce the size of the string we need to search.
            // That way, if there are duplicates, we always find the correct
            // recipient.
            chipStart = editable.toString().indexOf(token, chipEnd);
            chipEnd = chipStart + token.length();
            // Only set the span if we found a matching token.
            if (chipStart != -1) {
                editable.setSpan(chip, chipStart, chipEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            mRemovedSpans.remove(0); /// M: Always remove first removedSpan
        }

        /// M: Update mMoreChip
        MoreImageSpan moreSpan = createMoreSpan(mRemovedSpans.size());
        SpannableString chipText = new SpannableString(getText().subSequence(chipEnd + 1, moreSpanEnd));
        chipText.setSpan(moreSpan, 0, chipText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        getText().replace(chipEnd + 1, moreSpanEnd, chipText);
        mMoreChip = moreSpan;
    }
    
    /**
     * M: Construct chip for long pressed at last normal text.
     * @hide
     */
    public void constructPressedChip() {
        Editable editable = getText();
        setSelection(editable != null && editable.length() > 0 ? editable.length() : 0);
        boolean textIsAllBlank = textIsAllBlank(editable);
        
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(editable, end);
        RecipientChip[] chips = getSpannable().getSpans(start, end, RecipientChip.class);
        if ((chips == null || chips.length == 0)) {
            Editable text = getText();
            int whatEnd = mTokenizer.findTokenEnd(text, start);
            if (whatEnd < text.length() && text.charAt(whatEnd) == ',') {
                whatEnd++;
            }
            int selEnd = getSelectionEnd();
            if (whatEnd != selEnd && !textIsAllBlank) {
                handleEdit(start, whatEnd);
            } else {
                commitChip(start, end, editable);
            }
        }
    }
    
    /**
     * M: UpdatePressedChipType for deciding how to update pressed chip.
     * @hide
     */
    public enum UpdatePressedChipType{
        ADD_CONTACT, UPDATE_CONTACT, DELETE_CONTACT
    }

    /**
     * M: Update pressed chip which just added into contact.
     * @param posX Touch position of pressed chip in x coordinate
     * @param posY Touch position of pressed chip in y coordinate
     * @param updateType How to update pressed chip
     * @hide
     */
    public void updatePressedChip(float posX, float posY, UpdatePressedChipType updateType) {
        int offset = putOffsetInRange(getOffsetForPosition(posX, posY));
        final RecipientChip currentChip = (isTouchPointInChip(posX, posY)) ? (findChip(offset)) : (null);
        ArrayList<String> addresses = new ArrayList<String>();
        if (currentChip == null) {
            return;
        }

        if (updateType == UpdatePressedChipType.DELETE_CONTACT) {
            RecipientEntry entry = createTokenizedEntry(currentChip.getValue().toString());
            if (entry != null) {
                Editable editable = getText();
                int start = getChipStart(currentChip);
                int end = getChipEnd(currentChip);
                QwertyKeyListener.markAsReplaced(editable, start, end, "");
                CharSequence chipText = createChip(entry, false);
                if (chipText != null && start > -1 && end > -1) {
                    editable.replace(start, end + 1, chipText);
                }
            }
            return;
        }

        addresses.add(createAddressText(currentChip.getEntry()));
        HashMap<String, RecipientEntry> entries = RecipientAlternatesAdapter
                .getMatchingRecipients(getContext(), addresses);
        if ((RecipientEntry.isCreatedRecipient(currentChip.getEntry().getContactId()) || (updateType == UpdatePressedChipType.UPDATE_CONTACT))
                && getSpannable().getSpanStart(currentChip) != -1) {
            String destination = currentChip.getEntry().getDestination();
            RecipientEntry entry = null;
            if (isPhoneNumber(destination)) {
                entry = createValidatedEntry(entries.get(destination));
            } else {
                entry = createValidatedEntry(entries.get(tokenizeAddress(destination.toLowerCase())));
            }
            if (entry == null && !isPhoneQuery()) {
                entry = currentChip.getEntry();
            }
            final RecipientEntry tempEntry = entry;
            if (tempEntry != null) {
                /// M: Update photo cache map
                Uri photoThumbnailUri = entry.getPhotoThumbnailUri();
                if (photoThumbnailUri != null) {
                    ((BaseRecipientAdapter) getAdapter()).updatePhotoCacheByUri(photoThumbnailUri);
                }
                /// @}
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        replaceChip(currentChip, tempEntry);
                    }
                });
            }
        }
    }

    /// M: Default size of text
    private float mDefaultTextSize = 0;

    /// M: For indicating RecipientEditTextView is just been expanded, especially used when chips to be parsed is over MAX_CHIPS_PARSED.
    private boolean mJustExpanded = false;

    /**
     * M: Handle pending strings immediately to let getText() get all text including pending strings.
     *    This API is to prevent getting wrong content due to getText is called before handlePendingStrings.
     * @hide
     */
    public Editable handleAndGetText() {
        appendPendingStrings();
        return getText();
    }

    private void tempLogPrint(String logTitle, Object obj) {
        int spanStart = getSpannable().getSpanStart(obj);
        int spanEnd = getSpannable().getSpanEnd(obj);
        int spanFlag = getSpannable().getSpanFlags(obj);
        String spanName = obj.getClass().getName();
        int spanID = obj.hashCode();
        Log.d(TAG, "[Debug] "+logTitle+ " ---> spanStart=" + spanStart + ", spanEnd="+ spanEnd + ", spanFlag=" + spanFlag + 
                   ", spanID=" + spanID + ", spanName=" + spanName);            
    }
    
    /// M: To indicate it's during accelrattion of removing chip, preventing unnecessary afterTextChanged action.
    private boolean mDuringAccelerateRemoveChip = false;

    /*
     * M: Let text and span watchers work corectly by reset selection and layout.
     */
    private void recoverLayout() {
        setSelection(getText().length());
        int hintWant = getLayout() == null ? 0 : getLayout().getWidth();
        makeNewLayout(getLayout().getWidth(), hintWant, new BoringLayout.Metrics(), new BoringLayout.Metrics(),
                      getWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight(),
                      false);
        requestLayout();
        invalidate();
    }

    private class RecipientChipProcessor{
        private ArrayList<RecipientChip> mChips;
        private ArrayList<Integer> mSpanStart;
        private ArrayList<Integer> mSpanEnd;
        private ArrayList<Integer> mSpanFlags;

        public RecipientChipProcessor() {
            mChips = new ArrayList<RecipientChip>();
            mSpanStart = new ArrayList<Integer>();
            mSpanEnd = new ArrayList<Integer>();
            mSpanFlags = new ArrayList<Integer>();
        }

        public void removeChipsWithoutNotification(int startIndex, int endIndex) {
            RecipientChip[] chips = getSortedRecipients();
            if ((chips == null || chips.length == 0)) {
                return;
            }
            Log.e(TAG, "[removeChipsWithoutNotification]");
            /// M: Remove watchers
            watcherProcessor wp = null;
            wp = new watcherProcessor();
            wp.initWatcherProcessor();
            wp.removeSpanWatchers();
            /// M: Remove chip spans
            for (int x = startIndex; x < endIndex; x++) {
                mChips.add(chips[x]);
                mSpanStart.add(getChipStart(chips[x]));
                mSpanEnd.add(getChipEnd(chips[x]));
                mSpanFlags.add(getSpannable().getSpanFlags(chips[x]));
                getSpannable().removeSpan(chips[x]);
            }
            /// M: Add watchers back
            if(wp != null) {
                wp.addSpanWatchers();
            }
            recoverLayout();
        }

        public void addChipsBackWithoutNotification(int offset) {
            if (mChips == null || mChips.size() == 0) {
                return;
            }
            Log.d(TAG, "[addChipsBackWithoutNotification]");
            /// M: Remove watchers
            watcherProcessor wp = null;
            wp = new watcherProcessor();
            wp.initWatcherProcessor();
            wp.removeSpanWatchers();
            /// M: Add chip spans back
            int chipsLen = mChips.size();
            for (int x=0; x< chipsLen; x++) {
                if (x == (chipsLen -1)) {
                    /// M: Add watchers back
                    if(wp != null) {
                        wp.addSpanWatchers();
                    }
                    /// M: Let text and span watchers work corectly by reset selection and layout. @{
                    setSelection(getText().length());
                    requestLayout();
                    /// @}
                }
                int chipStart = mSpanStart.get(x) - offset;
                int chipEnd = mSpanEnd.get(x) - offset;
                if (!alreadyHasChip(chipStart, chipEnd)) {
                    getSpannable().setSpan(mChips.get(x), chipStart, chipEnd, mSpanFlags.get(x));
                }
            }
            recoverLayout();
        }
    }

    /// M: Default vertical offset of AutoCompleteTextView which is used for adjusting position of popup window
    private int mDefaultVerticalOffset = 0;

    /// M: Text to be restore later instead of restoring at onRestoreInstanceState phase
    private String mStringToBeRestore = null;
}