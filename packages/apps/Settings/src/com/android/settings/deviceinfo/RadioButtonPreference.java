package com.android.settings.deviceinfo;

import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.settings.R;
import com.mediatek.xlog.Xlog;

public class RadioButtonPreference extends Preference implements
        View.OnClickListener {
    private static final int TITLE_ID = R.id.preference_title;
    private static final int BUTTON_ID = R.id.preference_radiobutton;
    private TextView mPreferenceTitle = null;
    private RadioButton mPreferenceButton = null;
    private CharSequence mTitleValue = "";
    private boolean mChecked = false;
    private String mMountPath;

    private static final String TAG = "RadioButtonPreference";

    /**
     * RadioButtonPreference construct
     * 
     * @param context
     *            the preference associated with
     */
    public RadioButtonPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference_radiobutton);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);
        Xlog.d(TAG, "getview");
        mPreferenceTitle = (TextView) view.findViewById(TITLE_ID);
        mPreferenceTitle.setText(mTitleValue);
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
     * set the preferce checked or unchecked
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

    /**
     * Set the storage preference path
     * 
     * @param path
     *            the storage path
     */
    public void setPath(String path) {
        mMountPath = path;
    }

    /**
     * get the storage preference path
     * 
     * @return the storage path
     */
    public String getPath() {
        return mMountPath;
    }
}
