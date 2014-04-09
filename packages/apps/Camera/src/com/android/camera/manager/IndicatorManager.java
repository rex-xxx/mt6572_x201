package com.android.camera.manager;

import android.content.res.TypedArray;
import android.hardware.Camera.Parameters;
import android.view.View;

import com.android.camera.Camera;
import com.android.camera.IconListPreference;
import com.android.camera.ListPreference;
import com.android.camera.Log;
import com.android.camera.ParametersHelper;
import com.android.camera.PreferenceGroup;
import com.android.camera.R;
import com.android.camera.SettingChecker;
import com.android.camera.SettingUtils;
import com.android.camera.ui.RotateImageView;

public class IndicatorManager extends ViewManager implements Camera.OnParametersReadyListener,
        Camera.OnPreferenceReadyListener {
    private static final String TAG = "IndicatorManager";
    private static final boolean LOG = Log.LOGV;
    
    private static final int INDICATOR_COUNT = 6;
    private static final int[] VIEW_IDS = new int[] {
            R.id.onscreen_white_balance_indicator,
            R.id.onscreen_scene_indicator,
            R.id.onscreen_exposure_indicator,
            R.id.onscreen_timelapse_indicator,
            R.id.onscreen_selftimer_indicator,
            R.id.onscreen_voice_indicator,
    };
    private static final int[] SETTING_ROWS = new int[] {
            SettingChecker.ROW_SETTING_WHITE_BALANCE,
            SettingChecker.ROW_SETTING_SCENCE_MODE,
            SettingChecker.ROW_SETTING_EXPOSURE,
            SettingChecker.ROW_SETTING_TIME_LAPSE,
            SettingChecker.ROW_SETTING_SELF_TIMER,
            SettingChecker.ROW_SETTING_VOICE,
    };
    private static final boolean[] FROM_PARAMETERS = new boolean[] {
            true,
            true,
            true,
            false,
            false,
            false,
    };
    private static final int ROW_WHITE_BALANCE = 0;
    private static final int ROW_SCENE_MODE = 1;
    private static final int ROW_EXPOSURE = 2;
    private static final int ROW_TIME_LAPSE = 3;
    private static final int ROW_SELF_TIME = 4;
    private static final int ROW_VOICE = 5;
    
    private RotateImageView[] mViews = new RotateImageView[INDICATOR_COUNT];
    private ListPreference[] mPrefs = new ListPreference[INDICATOR_COUNT];
    private String[] mDefaults = new String[INDICATOR_COUNT];
    private String[] mOverrides = new String[INDICATOR_COUNT];
    private boolean mPreferenceReady;
    private View mIndicatorGroup;
    private boolean[] mVisibles;
    
    public IndicatorManager(Camera context) {
        super(context);
        context.addOnParametersReadyListener(this);
        context.addOnPreferenceReadyListener(this);
        //disable animation for cross with remaining.
        setAnimationEnabled(true, false);
    }
    
    @Override
    protected View getView() {
        View view = inflate(R.layout.onscreen_indicators);
        for (int i = 0; i < INDICATOR_COUNT; i++) {
            mViews[i] = (RotateImageView) view.findViewById(VIEW_IDS[i]);
        }
        mIndicatorGroup = view.findViewById(R.id.on_screen_group);
        return view;
    }
    
    public void onPreferenceReady() {
        PreferenceGroup group = getContext().getPreferenceGroup();
        for (int i = 0; i < INDICATOR_COUNT; i++) {
            int row = SETTING_ROWS[i];
            mPrefs[i] = getContext().getListPreference(row);
            mDefaults[i] = getContext().getSettingChecker().getDefaultValue(row);
        }
        mPreferenceReady = true;
    }
    
    public void onCameraParameterReady() {
        if (!mPreferenceReady) {
            throw new RuntimeException("why not preference has not been initialized?");
        }
        refreshModeIndicator(true);
        refresh();
    }
    
    @Override
    public void onRefresh() {
        if (!mPreferenceReady || getContext().isSwitchingCamera()) {
            Log.w(TAG, "onRefresh() why refresh before preference ready? ", new Throwable());
            return;
        }
        refreshModeIndicator(false);
        int showcount = 0;
        for (int i = 0; i < INDICATOR_COUNT; i++) {
            int row = SETTING_ROWS[i];
            String value = null;
            if (mOverrides[i] != null) {
                value = mOverrides[i]; //override value
            } else if (FROM_PARAMETERS[i]) {
                value = getContext().getSettingChecker().getParameterValue(row);
            } else {
                value = getContext().getSettingChecker().getSettingCurrentValue(row);
            }
            //Change HDR to auto for final user.
            if (SettingChecker.ROW_SETTING_SCENCE_MODE == row) {
                if (ParametersHelper.KEY_SCENE_MODE_HDR.equals(value)) {
                    value = Parameters.SCENE_MODE_AUTO;
                }
            }
            if (mPrefs[i] instanceof IconListPreference) {
                if (!mVisibles[i] || value == null || (mDefaults[i] != null && mDefaults[i].equals(value))) {
                    mViews[i].setVisibility(View.GONE);
                } else {
                    mViews[i].setVisibility(View.VISIBLE);
                    IconListPreference iconPref = ((IconListPreference)mPrefs[i]);
                    if (iconPref.getOriginalIconIds() != null) {
                        //we may disable some entry values for unsupported cases.
                        //so here search original value for dynamic cases.
                        int index = SettingUtils.index(iconPref.getOriginalEntryValues(), value);
                        mViews[i].setImageResource(iconPref.getOriginalIconIds()[index]);
                    }
                    showcount++;
                }
            }
            if (LOG) {
                Log.v(TAG, "onRefresh() i=" + i + ", row[" + row + "]=" + value + ", view=" + mViews[i]
                     + ", default=" + mDefaults[i] + ", override=" + mOverrides[i] + ", showcount=" + showcount);
            }
        }
        if (showcount > 0) {
            mIndicatorGroup.setBackgroundResource(R.drawable.bg_indicator_background);
        } else {
            mIndicatorGroup.setBackgroundDrawable(null);
        }
    }
    
    public synchronized void refreshModeIndicator(boolean force) {
        if (LOG) {
            Log.v(TAG, "refreshModeIndicator(" + force + ") mVisibles=" + mVisibles);
        }
        if (mVisibles == null || force) {
            mVisibles = new boolean[INDICATOR_COUNT];
            for (int i = 0; i < INDICATOR_COUNT; i++) {
                boolean visible = true;
                int row = SETTING_ROWS[i];
                if (getContext().isImageCaptureIntent()) {
                    visible = SettingUtils.contains(SettingChecker.SETTING_GROUP_CAMERA_FOR_UI, row);
                } else if (getContext().isVideoMode()) {
                    visible = SettingUtils.contains(SettingChecker.SETTING_GROUP_VIDEO_FOR_UI, row);
                }
                mVisibles[i] = visible;
            }
        }
    }
    
    //AsdActor should save original scene and restore it when release it.
    public void saveSceneMode() {
        if (LOG) {
            Log.v(TAG, "saveSceneMode() mPreferenceReady=" + mPreferenceReady);
        }
        //Clear all overrider values and set it to auto as default.
        getContext().getSettingChecker().setOverrideValues(SETTING_ROWS[ROW_SCENE_MODE], Parameters.SCENE_MODE_AUTO);
    }
    
    public void restoreSceneMode() {
        getContext().getSettingChecker().setOverrideValues(SETTING_ROWS[ROW_SCENE_MODE], null);
        for (int i = 0, len = mOverrides.length; i < len; i++) {
            mOverrides[i] = null;
        }
        if (LOG) {
            Log.v(TAG, "restoreSceneMode() mPreferenceReady=" + mPreferenceReady);
        }
    }
    
    public void onDetectedSceneMode(int scene) {
        //Application shouldn't keep scene[int]-scene[String] mapping.
        //Here we maintain this logic until native FPM correct this bad logic.
        TypedArray asdModeMapping = getContext().getResources().obtainTypedArray(
                R.array.scenemode_native_mapping_entryvalues);
        String sceneMode = asdModeMapping.getString(scene);
        asdModeMapping.recycle();
        //here notify preference changed!
        String localOverride = mOverrides[ROW_SCENE_MODE];
        String preferenceValue = Parameters.SCENE_MODE_AUTO;
        if (!sceneMode.equals(localOverride)) {
            //Set local override value to native detected value.
            mOverrides[ROW_SCENE_MODE] = sceneMode;
            if (getContext().getSettingChecker().isParametersSupportedValue(SETTING_ROWS[ROW_SCENE_MODE], sceneMode)) {
                preferenceValue = sceneMode;
            }
            String checkerOverride = getContext().getSettingChecker().getOverrideValues(SETTING_ROWS[ROW_SCENE_MODE]);
            if (!preferenceValue.equals(checkerOverride)) {
                getContext().getSettingChecker().setOverrideValues(SETTING_ROWS[ROW_SCENE_MODE], preferenceValue);
                getContext().notifyPreferenceChanged();
            } else {
                refresh();
            }
        }
        if (LOG) {
            Log.v(TAG, "onDetectedSceneMode(" + scene + ") override=" + mOverrides[ROW_SCENE_MODE]
                    + ", sceneMode=" + sceneMode + ", preferenceValue=" + preferenceValue
                    + ", local override=" + localOverride);
        }
    }
}