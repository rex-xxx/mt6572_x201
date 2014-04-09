package com.android.settings.deviceinfo;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.settings.R;
import com.mediatek.xlog.Xlog;

public class UsbPreference extends Preference implements View.OnClickListener {
    private static final int TITLE_ID = R.id.usb_preference_title;
    private static final int SUMMARY_ID = R.id.usb_preference_summary;
    private static final int BUTTON_ID = R.id.usb_preference_radiobutton;
    private TextView mPreferenceTitle = null;
    private TextView mPreferenceSummary = null;
    private RadioButton mPreferenceButton = null;
    private CharSequence mTitleValue = "";
    private CharSequence mSummaryValue = "";
    private boolean mChecked = false;

    private static final String TAG = "UsbPreference";

    /**
     * UsbPreference construct
     * 
     * @param context
     *            the preference associated with
     */
    public UsbPreference(Context context) {
        this(context, null);
    }

    /**
     * UsbPreference construct
     * 
     * @param context
     *            the preference associated with
     * @param attrs
     *            the attribute the xml will be inflated to
     */
    public UsbPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * UsbPreference construct
     * 
     * @param context
     *            the preference associated with
     * @param attrs
     *            the attribute the xml will be inflated to
     * @param defStyle
     *            the style which will be apply to the preference
     */
    public UsbPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_usb);

        // get the title from audioprofile_settings.xml
        if (super.getTitle() != null) {
            mTitleValue = super.getTitle().toString();
        }

        // get the summary from audioprofile_settings.xml
        if (super.getSummary() != null) {
            mSummaryValue = super.getSummary().toString();
        }
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);
        Xlog.d(TAG, "getview");
        mPreferenceTitle = (TextView) view.findViewById(TITLE_ID);
        mPreferenceTitle.setText(mTitleValue);
        mPreferenceSummary = (TextView) view.findViewById(SUMMARY_ID);
        mPreferenceSummary.setText(mSummaryValue);
        mPreferenceButton = (RadioButton) view.findViewById(BUTTON_ID);
        mPreferenceButton.setOnClickListener(this);
        mPreferenceButton.setChecked(mChecked);
        return view;
    }

    @Override
    public void setTitle(CharSequence title) {
        if (null == mPreferenceTitle) {
            mTitleValue = title;
        }
        if (!title.equals(mTitleValue)) {
            mTitleValue = title;
            mPreferenceTitle.setText(mTitleValue);
        }
    }

    @Override
    public CharSequence getTitle() {
        return mTitleValue;
    }

    /**
     * set the preference summary
     * 
     * @param summary
     *            the preference summary
     */
    public void setSummary(CharSequence summary) {
        if (null == mPreferenceSummary) {
            mSummaryValue = summary;
        }
        if (!summary.equals(mSummaryValue)) {
            mSummaryValue = summary;
            mPreferenceSummary.setText(mSummaryValue);
        }
    }

    /**
     * get the preference summary
     * 
     * @return the preference summary
     */
    public CharSequence getSummary() {
        return mSummaryValue;
    }

    /**
     * get the preference checked status
     * 
     * @return the checked status
     */
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void onClick(View v) {
        boolean newValue = !isChecked();

        if (!newValue) {
            Xlog.d(TAG, "button.onClick return");
            return;
        }

        if (setChecked(newValue)) {
            callChangeListener(newValue);
            Xlog.d(TAG, "button.onClick");
        }
    }

    @Override
    protected void onClick() {

        super.onClick();

        boolean newValue = !isChecked();

        if (!newValue) {
            Xlog.d(TAG, "preference.onClick return");
            return;
        }

        if (setChecked(newValue)) {
            callChangeListener(newValue);
            Xlog.d(TAG, "preference.onClick");
        }
    }

    /**
     * set the preference checked status
     * 
     * @param checked
     *            the checked status
     * @return set success or fail
     */
    public boolean setChecked(boolean checked) {
        if (null == mPreferenceButton) {
            Xlog.d(TAG, "setChecked return");
            mChecked = checked;
            return false;
        }

        if (mChecked != checked) {
            mPreferenceButton.setChecked(checked);
            mChecked = checked;
            return true;
        }
        return false;
    }
}
