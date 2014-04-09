package com.android.email.activity.setup;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ListPopupWindow;
import android.widget.MultiAutoCompleteTextView;

import com.android.emailcommon.utility.TextUtilities;

/** An editable text view, extending {@link MultiAutoCompleteTextView}, that
 *  can filter data by EmailAccountTokenizer (separate string by '@'), and auto-fill
 *  EditText when the filter count was only one.
 */
public class EmailAccountAutoCompleteTextView extends MultiAutoCompleteTextView {
    private boolean mIMEFullScreenMode = false;
    // Record the last input text length, and don't do the auto-replace
    // if the user was deleting chars.
    private int mLastTextLength = 0;

    ListPopupWindow mPopup;
    Tokenizer mAccountTokenizer = new EmailAccountTokenizer();
    Tokenizer mCommaTokenizer = new MultiAutoCompleteTextView.CommaTokenizer();

    public EmailAccountAutoCompleteTextView(Context context) {
        super(context);
    }

    public EmailAccountAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setThreshold(1);
        setTokenizer(new EmailAccountTokenizer());
        // Disable auto-replace on only one matched item when IME was in fullscreen mode
        //, cause the IME's own bug.
    /*  Configuration cf = context.getResources().getConfiguration();
        if (cf.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mIMEFullScreenMode = true;
        } else {
            mIMEFullScreenMode = false;
        }*/
    }

    public EmailAccountAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void replaceTextAndHighLight(CharSequence text) {
        clearComposingText();
        int end = getSelectionEnd();
        Editable editable = getText();
        DropdownAccountsArrayAdapter<String> adapter = (DropdownAccountsArrayAdapter<String>) getAdapter();
        Filter filter = adapter.getFilter();

        int start = 0;
        String filteringString = null;
        // Account filter and Address filter used different tokenizer.
        if (filter instanceof DropdownAddressFilter) {
            filteringString = ((DropdownAddressFilter)filter).getFilterString();
            start = mCommaTokenizer.findTokenStart(editable, end);
        } else {
            filteringString = ((DropdownAccountsFilter)filter).getFilterString();
            start = mAccountTokenizer.findTokenStart(editable, end);
        }
        String currentToFilterString = editable.subSequence(start, end).toString();
        // Cause the filtering was an asynchronous operation, if the current text was not yet the filtered text.
        // we never replace the text.
        if (TextUtilities.stringOrNullEquals(filteringString, currentToFilterString)) {
            // false, not trigger filter once more.
            setText(text, false);
            Editable replaced = getText();
            Selection.setSelection(replaced, end, replaced.length());
        }
    }

    @Override
    protected void replaceText(CharSequence text) {
        clearComposingText();

        setText(text);
        // make sure we keep the caret at the end of the text view
        Editable spannable = getText();
        Selection.setSelection(spannable, spannable.length());
        mLastTextLength = getEditTextLength();
    }

    @Override
    public void onFilterComplete(int count) {
        super.onFilterComplete(count);
        int length = getEditTextLength();
        if (length > mLastTextLength) {
            replacePartTextIfNeed();
        }
        mLastTextLength = length;
    }

    /* 
     * Obtain the last edited text to avoid miss the mLastHostNameLength
     * due to screen orientation.
     */
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        mLastTextLength = getEditTextLength();
    }

    private int getEditTextLength() {
        String text = getText().toString();
        return text.length();
    }

    public void replacePartTextIfNeed() {
        BaseAdapter adapter = (BaseAdapter) getAdapter();
        if (adapter.getCount() == 0) {
            return;
        }
        String text = getText().toString();
        CharSequence replaceText = convertSelectionToString(adapter.getItem(0));
        if (replaceText.equals(text)) {
            dismissDropDown();
            return;
        }
        int itemCount = adapter.getCount();
        // if the matched account was only one, we auto fill the EditText with it.
        // Don't replace text when the IME was in full screen mode.
        if (itemCount == 1 && !mIMEFullScreenMode) {
            replaceTextAndHighLight(replaceText);
            dismissDropDown();
        }
    }

    /**
     * This email account Tokenizer can be used for lists where the items are
     * separated by '@' and one or more spaces.
     */
    public static class EmailAccountTokenizer implements Tokenizer {
        public int findTokenStart(CharSequence text, int cursor) {
            int i = 0;

            while (i < cursor && text.charAt(i) != '@') {
                i++;
            }
            // Don't perform filtering when there was no '@' input.
            if (i == cursor) {
                return cursor;
            }
            return i + 1;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();

            while (i < len) {
                if (text.charAt(i) == '@') {
                    return i;
                } else {
                    i++;
                }
            }
            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            // Highlight the replacing text part.
            SpannableString highlightSpan = new SpannableString(text);
            highlightSpan.setSpan(new BackgroundColorSpan(TextUtilities.HIGHLIGHT_COLOR_INT), 0,
                    highlightSpan.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return highlightSpan;
        }
    }
}
