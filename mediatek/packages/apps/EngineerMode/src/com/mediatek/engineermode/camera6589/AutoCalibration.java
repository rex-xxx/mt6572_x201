/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.engineermode.camera6589;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;

import java.util.Arrays;

public class AutoCalibration extends Activity implements OnItemClickListener {

    public static final String PREFERENCE_KEY = "camera_inter_settings";
    private static final String TAG = "EM/AutoCalibration";
    private static final int DIALOG_ISO_SPEED = 5;
    private static final String[] ISO_STRS_ARRAY = { "0", "100", "150", "200", "300", "400", "600", "800", "1200",
            "1600", "2400", "3200" };
    // components
    private RadioButton mNormalMode;
    private LinearLayout mNormalCaptureLayout;
    private Spinner mNormalCaptureSize;
    private Spinner mNormalCaptureType;
    private Spinner mNormalCaptureNum;
    private RadioButton mMulFrameMode;
    private LinearLayout mMulFrameLayout;
    private Spinner mMulFrameCaptureNum;
    private RadioButton mVideoCliplMode;
    private LinearLayout mVideoClipLayout;
    private Spinner mVideoClipResolution;
    private RadioButton mAfAuto;
    private RadioButton mAfFullScan;
    private RadioButton mAfBracket;
    private RadioButton mAfThrough;
    private LinearLayout mAfBracketLayout;
    private EditText mAfBracketRange;
    private Spinner mAfBracketInterval;
    private LinearLayout mAfThroughLayout;
    private EditText mAfThroughInterval;
    private Spinner mAfThroughDirec;
    private ListView mIsoListView;
    private ListView mCaptureListView;
    // Use for 2nd MP(JB2)
    // private Spinner mPreFlashSpinner;
    // private Spinner mMainFlashSpinner;
    private Spinner mStrobeModeSpinner;
    private Spinner mFlickerSpinner;
    // Through focus: Manual configure
    private LinearLayout mThroughFocusStart;
    private LinearLayout mThroughFocusEnd;
    private EditText mThroughFocsuStartPos;
    private EditText mThroughFocsuEndPos;

    private boolean[] mMulISOFlags = { true, false, true, false, false, false, false, false, false, false, false,
            false, false, false };
    private boolean mAfModeStatus = true; // auto is true, others is false
    private int mAfSpecialIso = 0;
    private String mIsoValueStr;
    private int mCaptureMode = 0;
    private int mAfMode = 0;
    // HDR debug
    private static final String HDR_KEY = "mediatek.hdr.debug";
    private Spinner mHdrSpinner;
    private CompoundButton.OnCheckedChangeListener mRadioListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Elog.d(TAG, "Button is " + buttonView.getId() + "isChecked " + isChecked);
            if (isChecked) {
                if (buttonView == mNormalMode) {
                    int[] layouts = { View.VISIBLE, View.GONE, View.GONE };
                    RadioButton[] buttons = { mVideoCliplMode, mMulFrameMode };
                    mCaptureMode = 0;
                    setCaptureLayout(layouts, buttons);
                    // set af status
                    setAfModeAccessble(true);
                } else if (buttonView == mMulFrameMode) {
                    int[] layouts = { View.GONE, View.VISIBLE, View.GONE };
                    RadioButton[] buttons = { mVideoCliplMode, mNormalMode };
                    mCaptureMode = 1;
                    setCaptureLayout(layouts, buttons);
                    // set af status
                    setAfModeAccessble(true);
                } else if (buttonView == mVideoCliplMode) {
                    int[] layouts = { View.GONE, View.GONE, View.VISIBLE };
                    RadioButton[] buttons = { mNormalMode, mMulFrameMode };
                    mCaptureMode = 2;
                    setCaptureLayout(layouts, buttons);
                    // set af status
                    mAfAuto.setChecked(true);
                    setAfModeAccessble(false);
                } else if (buttonView == mAfAuto) {
                    int[] layouts = { View.GONE, View.GONE };
                    RadioButton[] buttons = { mAfBracket, mAfFullScan, mAfThrough };
                    mAfMode = 0;
                    setAfLayout(layouts, buttons);
                } else if (buttonView == mAfBracket) {
                    int[] layouts = { View.VISIBLE, View.GONE };
                    RadioButton[] buttons = { mAfAuto, mAfFullScan, mAfThrough };
                    mAfMode = 1;
                    setAfLayout(layouts, buttons);
                } else if (buttonView == mAfFullScan) {
                    int[] layouts = { View.GONE, View.GONE };
                    RadioButton[] buttons = { mAfBracket, mAfAuto, mAfThrough };
                    mAfMode = 2;
                    setAfLayout(layouts, buttons);
                } else if (buttonView == mAfThrough) {
                    int[] layouts = { View.GONE, View.VISIBLE };
                    RadioButton[] buttons = { mAfBracket, mAfFullScan, mAfAuto };
                    mAfMode = 3;
                    setAfLayout(layouts, buttons);
                }
                if (mAfMode == 0) {
                    if (!mAfModeStatus) {
                        statusChangesByAf(true);
                        mAfModeStatus = true;
                    }
                } else {
                    if (mAfModeStatus) {
                        statusChangesByAf(false);
                        mAfModeStatus = false;
                    }
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Elog.i(TAG, "onCreate.");
        setContentView(R.layout.mt6589_auto_calibration);
        inintComponents();
        setStatusTodefault();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Elog.i(TAG, "onItemClick view: " + parent.getId() + " position: " + position);
        if (parent.getId() == mIsoListView.getId()) {
            if (position == 0) { // set ISO speed
                showDialog(DIALOG_ISO_SPEED);
            }
        } else {
            if (position == 0) {
                if (!putValuesToPreference()) {
                    return;
                }
                Intent captureIntent = new Intent();
                captureIntent.setClass(this, Camera.class);
                this.startActivity(captureIntent);
                Elog.i(TAG, "Start captureIntent!");
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Elog.i(TAG, "onCreateDialog id: " + id + "AF mode: " + mAfMode);
        Dialog dialog = null;
        Builder builder = null;
        if (DIALOG_ISO_SPEED == id) {
            builder = new AlertDialog.Builder(AutoCalibration.this);
            builder.setTitle(R.string.auto_clibr_iso_setting);
            if (mAfMode == 0) {
                builder.setMultiChoiceItems(R.array.auto_calib_iso, mMulISOFlags,
                        new DialogInterface.OnMultiChoiceClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                mMulISOFlags[which] = isChecked;
                                if (which == 0) {
                                    if (isChecked) {
                                        for (int i = 1; i < mMulISOFlags.length; i++) {
                                            if (i == 2) {
                                                mMulISOFlags[i] = true;
                                                ((AlertDialog) dialog).getListView().setItemChecked(i, true);
                                            } else {
                                                mMulISOFlags[i] = false;
                                                ((AlertDialog) dialog).getListView().setItemChecked(i, false);
                                            }
                                        }
                                    }
                                } else if (which == 1) {
                                    if (isChecked) {
                                        mMulISOFlags[0] = false;
                                        ((AlertDialog) dialog).getListView().setItemChecked(0, false);
                                        for (int i = 2; i < mMulISOFlags.length; i++) {
                                            mMulISOFlags[i] = true;
                                            ((AlertDialog) dialog).getListView().setItemChecked(i, true);
                                        }
                                    }
                                } else {
                                    if (isChecked) {
                                        mMulISOFlags[0] = false;
                                        ((AlertDialog) dialog).getListView().setItemChecked(0, false);
                                    } else {
                                        mMulISOFlags[1] = false;
                                        ((AlertDialog) dialog).getListView().setItemChecked(1, false);
                                    }
                                }
                            }
                        });
            } else {
                builder.setSingleChoiceItems(R.array.auto_calib_special_iso, mAfSpecialIso,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mAfSpecialIso = whichButton;
                                mIsoValueStr = ISO_STRS_ARRAY[mAfSpecialIso];
                            }
                        });
            }
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mAfMode == 0) {
                        putStrInPreference(R.string.auto_clibr_key_iso_speed, getArrayValue(mMulISOFlags));
                    } else {
                        putStrInPreference(R.string.auto_clibr_key_iso_speed, mIsoValueStr);
                    }
                    removeDialog(DIALOG_ISO_SPEED);
                    if (mVideoCliplMode.isChecked()) {
                        Toast.makeText(AutoCalibration.this, R.string.auto_clibr_video_dump_tips, Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }
        if (builder != null) {
            dialog = builder.create();
            dialog.setCancelable(false);
        }
        return dialog;
    }

    private void setCaptureLayout(int[] visis, RadioButton[] buttons) {
        mNormalCaptureLayout.setVisibility(visis[0]);
        mMulFrameLayout.setVisibility(visis[1]);
        mVideoClipLayout.setVisibility(visis[2]);
        for (RadioButton radioButton : buttons) {
            radioButton.setChecked(false);
        }
    }

    private void setAfLayout(int[] visis, RadioButton[] buttons) {
        mAfBracketLayout.setVisibility(visis[0]);
        mAfThroughLayout.setVisibility(visis[1]);
        for (RadioButton radioButton : buttons) {
            radioButton.setChecked(false);
        }
    }

    private void setAfModeAccessble(boolean access) {
        RadioButton[] buttons = { mAfBracket, mAfFullScan, mAfThrough };
        for (RadioButton radioButton : buttons) {
            radioButton.setEnabled(access);
        }
    }

    private void inintComponents() {
        // Capture mode
        mNormalMode = (RadioButton) findViewById(R.id.raido_capture_normal);
        mNormalMode.setOnCheckedChangeListener(mRadioListener);
        mMulFrameMode = (RadioButton) findViewById(R.id.raido_capture_mult_frame);
        mMulFrameMode.setOnCheckedChangeListener(mRadioListener);
        mMulFrameMode.setVisibility(View.GONE); // 1st MP does not support Multi
                                                // Frame mode
                                                // mCaptureModeGroup.addView(mMulFrameMode);
        mVideoCliplMode = (RadioButton) findViewById(R.id.raido_capture_video);
        mVideoCliplMode.setOnCheckedChangeListener(mRadioListener);
        mNormalCaptureLayout = (LinearLayout) findViewById(R.id.normal_capture_set);
        mNormalCaptureSize = (Spinner) findViewById(R.id.normal_capture_size);
        mNormalCaptureType = (Spinner) findViewById(R.id.normal_capture_type);
        mNormalCaptureNum = (Spinner) findViewById(R.id.normal_capture_number);
        mMulFrameLayout = (LinearLayout) findViewById(R.id.mult_frame_capture_set);
        mMulFrameLayout.setVisibility(View.GONE); // 1st MP does not support
                                                  // Multi Frame mode
        mMulFrameCaptureNum = (Spinner) findViewById(R.id.mult_capture_number);
        mVideoClipLayout = (LinearLayout) findViewById(R.id.video_capture_set);
        mVideoClipResolution = (Spinner) findViewById(R.id.video_capture_resolution);
        // Af mode
        mAfAuto = (RadioButton) findViewById(R.id.raido_af_auto);
        mAfAuto.setOnCheckedChangeListener(mRadioListener);
        mAfFullScan = (RadioButton) findViewById(R.id.raido_af_full);
        mAfFullScan.setOnCheckedChangeListener(mRadioListener);
        mAfBracket = (RadioButton) findViewById(R.id.raido_af_bracket);
        mAfBracket.setOnCheckedChangeListener(mRadioListener);
        mAfThrough = (RadioButton) findViewById(R.id.raido_af_through);
        mAfThrough.setOnCheckedChangeListener(mRadioListener);
        mAfBracketLayout = (LinearLayout) findViewById(R.id.af_bracket_set);
        mAfBracketInterval = (Spinner) findViewById(R.id.af_bracket_interval);
        mAfBracketRange = (EditText) findViewById(R.id.af_bracket_range);
        mAfThroughLayout = (LinearLayout) findViewById(R.id.af_through_set);
        mAfThroughDirec = (Spinner) findViewById(R.id.af_through_dirct);
        // If Manual configure(2) has been selected, show start/stop pos
        // editors, else let them gone.
        mAfThroughDirec.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 2) {
                    mThroughFocusStart.setVisibility(View.VISIBLE);
                    mThroughFocusEnd.setVisibility(View.VISIBLE);
                } else {
                    mThroughFocusStart.setVisibility(View.GONE);
                    mThroughFocusEnd.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Elog.d(TAG, "select nothing.");
            }

        });
        mAfThroughInterval = (EditText) findViewById(R.id.af_through_interval);
        // Through focus: Manual configure
        mThroughFocusStart = (LinearLayout) findViewById(R.id.through_focus_start_set);
        mThroughFocusEnd = (LinearLayout) findViewById(R.id.through_focus_end_set);
        mThroughFocsuStartPos = (EditText) findViewById(R.id.af_through_manual_start);
        mThroughFocsuEndPos = (EditText) findViewById(R.id.af_through_manual_end);
        // ISO list view
        mIsoListView = (ListView) findViewById(R.id.listview_iso);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                new String[] { getString(R.string.auto_clibr_iso_speed) });
        mIsoListView.setAdapter(adapter);
        setLvHeight(mIsoListView);
        mIsoListView.setOnItemClickListener(this);
        // init spinners
        // mPreFlashSpinner = (Spinner) findViewById(R.id.pre_flash_lev);
        // mMainFlashSpinner = (Spinner) findViewById(R.id.main_flash_lev);
        mFlickerSpinner = (Spinner) findViewById(R.id.flicker);
        mStrobeModeSpinner = (Spinner) findViewById(R.id.led_flash);
        // Capture lis view
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Arrays.asList(getResources()
                .getStringArray(R.array.auto_calib_capture)));
        mCaptureListView = (ListView) findViewById(R.id.listview_capture);
        mCaptureListView.setAdapter(adapter);
        setLvHeight(mCaptureListView);
        mCaptureListView.setOnItemClickListener(this);
        // HDR debug
        mHdrSpinner = (Spinner) findViewById(R.id.hdr_debug);
        if ("1".equals(SystemProperties.get(HDR_KEY))) {
            mHdrSpinner.setSelection(1);
        } else {
            mHdrSpinner.setSelection(0);
        }
        mHdrSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                SystemProperties.set(HDR_KEY, String.valueOf(arg2));
                Elog.i(TAG, "hdrValue : " + SystemProperties.get(HDR_KEY));
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                Elog.d(TAG, "select nothing.");
            }
        });
    }

    private void setStatusTodefault() {
        Elog.v(TAG, "setStatusTodefault()");
        mCaptureMode = 0;
        mNormalMode.setChecked(true);
        mMulISOFlags[0] = true;
        mMulISOFlags[1] = false;
        mMulISOFlags[2] = true;
        for (int i = 3; i < mMulISOFlags.length; i++) {
            mMulISOFlags[i] = false;
        }
        mAfMode = 0;
        mAfAuto.setChecked(true);
        mAfModeStatus = true;
        mAfBracketRange.setText("0");
        mAfThroughInterval.setText("1");
        final SharedPreferences preferences = getSharedPreferences(PREFERENCE_KEY, android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.auto_clibr_key_capture_mode), mCaptureMode);
        editor.putInt(getString(R.string.auto_clibr_key_capture_size), 0);
        editor.putInt(getString(R.string.auto_clibr_key_capture_type), 0);
        editor.putInt(getString(R.string.auto_clibr_key_capture_number), 0);
        editor.putInt(getString(R.string.auto_clibr_key_flicker), 0);
        editor.putInt(getString(R.string.auto_clibr_key_led_flash), 0);
        editor.putInt(getString(R.string.auto_clibr_key_pre_flash), 1);
        editor.putInt(getString(R.string.auto_clibr_key_main_flash), 1);
        editor.putString(getString(R.string.auto_clibr_key_iso_speed), getArrayValue(mMulISOFlags));
        editor.putInt(getString(R.string.auto_clibr_key_af_mode), mAfMode);
        editor.commit();
        mThroughFocsuStartPos.setText(String.valueOf(preferences.getInt(
                getString(R.string.auto_clibr_key_through_manual_start_pos), 0)));
        mThroughFocsuEndPos.setText(String.valueOf(preferences.getInt(
                getString(R.string.auto_clibr_key_through_manual_end_pos), 1023)));
        mThroughFocusStart.setVisibility(View.GONE);
        mThroughFocusEnd.setVisibility(View.GONE);
    }

    private boolean putValuesToPreference() {
        final SharedPreferences preferences = getSharedPreferences(PREFERENCE_KEY, android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.auto_clibr_key_capture_mode), mCaptureMode);
        if (mCaptureMode == 0) {
            editor.putInt(getString(R.string.auto_clibr_key_capture_size), mNormalCaptureSize.getSelectedItemPosition());
            editor.putInt(getString(R.string.auto_clibr_key_capture_type), mNormalCaptureType.getSelectedItemPosition());
            editor.putInt(getString(R.string.auto_clibr_key_capture_number),
                    mNormalCaptureNum.getSelectedItemPosition());
        } else if (mCaptureMode == 1) {
            editor.putInt(getString(R.string.auto_clibr_key_capture_number),
                    mMulFrameCaptureNum.getSelectedItemPosition());
        } else {
            editor.putInt(getString(R.string.auto_clibr_key_capture_resolution),
                    mVideoClipResolution.getSelectedItemPosition());
        }
        editor.putInt(getString(R.string.auto_clibr_key_flicker), mFlickerSpinner.getSelectedItemPosition());
        editor.putInt(getString(R.string.auto_clibr_key_led_flash), mStrobeModeSpinner.getSelectedItemPosition());
        // editor.putInt(getString(R.string.auto_clibr_key_pre_flash),
        // mPreFlashSpinner.getSelectedItemPosition() + 1);
        // editor.putInt(getString(R.string.auto_clibr_key_main_flash),
        // mMainFlashSpinner.getSelectedItemPosition() + 1);
        editor.putInt(getString(R.string.auto_clibr_key_af_mode), mAfMode);
        editor.putInt(getString(R.string.auto_clibr_key_branket_interval),
                Integer.valueOf(mAfBracketInterval.getSelectedItem().toString()));
        int value = Integer.valueOf(mAfBracketRange.getText().toString());
        if (value < 0 || value > 511) {
            Toast.makeText(this, R.string.auto_clibr_af_bracket_range_tip, Toast.LENGTH_LONG).show();
            return false;
        }
        editor.putInt(getString(R.string.auto_clibr_key_branket_range), value);
        editor.putInt(getString(R.string.auto_clibr_key_through_focus_dirct), mAfThroughDirec.getSelectedItemPosition());
        if (mAfThroughDirec.getSelectedItemPosition() == 2) {
            editor.putInt(getString(R.string.auto_clibr_key_through_manual_start_pos),
                    Integer.valueOf(mThroughFocsuStartPos.getText().toString()));
            editor.putInt(getString(R.string.auto_clibr_key_through_manual_end_pos),
                    Integer.valueOf(mThroughFocsuEndPos.getText().toString()));
        }
        value = Integer.valueOf(mAfThroughInterval.getText().toString());
        if (value < 1 || value > 511) {
            Toast.makeText(this, R.string.auto_clibr_af_through_interval_tip, Toast.LENGTH_LONG).show();
            return false;
        }
        editor.putInt(getString(R.string.auto_clibr_key_through_focus_interval), value);
        editor.commit();
        return true;
    }

    private String getArrayValue(boolean[] array) {
        String result = "";
        for (int i = 2; i < array.length; i++) {
            if (array[i]) {
                result += ISO_STRS_ARRAY[i - 2] + ",";
            }
        }
        if (result.length() == 0) {
            Toast.makeText(this, R.string.auto_clibr_iso_tips, Toast.LENGTH_LONG);
            return "0,";
        }
        return result;
    }

    private void statusChangesByAf(boolean afStatus) {
        // mPreFlashSpinner.setEnabled(afStatus);
        // mMainFlashSpinner.setEnabled(afStatus);
        mStrobeModeSpinner.setSelection(2); // off
        mStrobeModeSpinner.setEnabled(afStatus);
        mMulFrameMode.setEnabled(afStatus);
        mVideoCliplMode.setEnabled(afStatus);
        mNormalCaptureNum.setEnabled(afStatus);
        if (mCaptureMode != 0) {
            mNormalMode.setChecked(true);
        }
        if (afStatus) {
            mMulISOFlags[0] = true;
            mMulISOFlags[1] = false;
            mMulISOFlags[2] = true;
            for (int i = 3; i < mMulISOFlags.length; i++) {
                mMulISOFlags[i] = false;
            }
            putStrInPreference(R.string.auto_clibr_key_iso_speed, getArrayValue(mMulISOFlags));
        } else {
            mAfSpecialIso = 0;
            mIsoValueStr = "0";
            putStrInPreference(R.string.auto_clibr_key_iso_speed, mIsoValueStr);
        }
    }

    private void putInPreference(int keyId, int value) {
        String key = getString(keyId);
        Elog.i(TAG, "putInPreference key: " + key + ",value: " + value);
        try {
            final SharedPreferences preferences = getSharedPreferences(PREFERENCE_KEY,
                    android.content.Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(key, value);
            editor.commit();
        } catch (NullPointerException ne) {
            Elog.i(TAG, ne.getMessage());
        }
    }

    private void putStrInPreference(int keyId, String value) {
        String key = getString(keyId);
        Elog.i(TAG, "putInPreference key: " + key + ",value: " + value);
        try {
            final SharedPreferences preferences = getSharedPreferences(PREFERENCE_KEY,
                    android.content.Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(key, value);
            editor.commit();
        } catch (NullPointerException ne) {
            Elog.i(TAG, ne.getMessage());
        }
    }

    private void setLvHeight(ListView listView) {
        ListAdapter adapter = listView.getAdapter();
        if (adapter == null) {
            Elog.d(TAG, "no data in ListView");
            return;
        }
        int totalHeight = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View itemView = adapter.getView(i, null, listView);
            itemView.measure(0, 0);
            totalHeight += itemView.getMeasuredHeight();
        }
        ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
        layoutParams.height = totalHeight + (listView.getDividerHeight() * (adapter.getCount() - 1));
        listView.setLayoutParams(layoutParams);
    }
}
