package com.android.camera.manager;

import android.view.View;

import com.android.camera.Camera;
import com.android.camera.FeatureSwitcher;
import com.android.camera.Log;
import com.android.camera.ModeChecker;
import com.android.camera.R;
import com.android.camera.ui.RotateImageView;

public class ModePicker extends ViewManager implements View.OnClickListener,
        View.OnLongClickListener, Camera.OnFullScreenChangedListener {
    private static final String TAG = "ModePicker";
    private static final boolean LOG = Log.LOGV;
    
    public interface OnModeChangedListener {
        void onModeChanged(int newMode);
    }
    
    public static final int MODE_PHOTO = 0;
    public static final int MODE_HDR = 1;
    public static final int MODE_FACE_BEAUTY = 2;
    public static final int MODE_PANORAMA = 3;
    public static final int MODE_MAV = 4;
    public static final int MODE_ASD = 5;
    public static final int MODE_SMILE_SHOT = 6;
    public static final int MODE_BEST = 7;
    public static final int MODE_EV = 8;
    public static final int MODE_VIDEO = 9;
    
    public static final int MODE_NUM_ICON = 10;
    public static final int MODE_NUM_ALL = 12;
    
    private static final int OFFSET = 100;
    private static final int OFFSET_STEREO_PREVIEW = OFFSET;
    private static final int OFFSET_STEREO_SINGLE = OFFSET * 2;
    
    public static final int MODE_PHOTO_3D = OFFSET_STEREO_PREVIEW + MODE_PHOTO;
    public static final int MODE_VIDEO_3D = OFFSET_STEREO_PREVIEW + MODE_VIDEO;
    
    public static final int MODE_PHOTO_SGINLE_3D = OFFSET_STEREO_SINGLE + MODE_PHOTO;
    public static final int MODE_PANORAMA_SINGLE_3D = OFFSET_STEREO_SINGLE + MODE_PANORAMA;
    
    private static final int DELAY_MSG_HIDE_MS = 3000; //3s
    
    private static final int[] MODE_ICONS_HIGHTLIGHT = new int[]{
        R.drawable.ic_mode_photo_focus,
        R.drawable.ic_mode_hdr_focus,
        R.drawable.ic_mode_facebeauty_focus,
        R.drawable.ic_mode_panorama_focus,
        R.drawable.ic_mode_mav_focus,
        R.drawable.ic_mode_asd_focus,
        R.drawable.ic_mode_smile_shot_focus,
        R.drawable.ic_mode_best_shot_focus,
        R.drawable.ic_mode_ev_bracket_shot_focus,
    };
    private static final int[] MODE_ICONS_NORMAL = new int[]{
        R.drawable.ic_mode_photo_normal,
        R.drawable.ic_mode_hdr_normal,
        R.drawable.ic_mode_facebeauty_normal,
        R.drawable.ic_mode_panorama_normal,
        R.drawable.ic_mode_mav_normal,
        R.drawable.ic_mode_asd_normal,
        R.drawable.ic_mode_smile_shot_normal,
        R.drawable.ic_mode_best_shot_normal,
        R.drawable.ic_mode_ev_bracket_shot_normal,
    };
    
    private final RotateImageView[] mModeViews = new RotateImageView[MODE_NUM_ICON];
    private View mScrollView;
    private int mCurrentMode = -1;
    private OnModeChangedListener mModeChangeListener;
    private OnScreenToast mModeToast;
    
    public ModePicker(Camera context) {
        super(context);
        context.addOnFullScreenChangedListener(this);
    }
    
    public int getCurrentMode() {
        return mCurrentMode;
    }
    
    private void setRealMode(int mode) {
        if (LOG) {
            Log.v(TAG, "setRealMode(" + mode + ") mCurrentMode=" + mCurrentMode);
        }
        if (mCurrentMode != mode) {
            mCurrentMode = mode;
            highlightCurrentMode();
            notifyModeChanged();
            if (mModeToast != null) {
                mModeToast.cancel();
            }
        }
    }
    
    public void setCurrentMode(int mode) {
        int realmode = getModeIndex(mode);
        if (getContext().isStereoMode()) {
            if (FeatureSwitcher.isStereoSingle3d()) {
                realmode += OFFSET_STEREO_SINGLE;
            } else {
                realmode += OFFSET_STEREO_PREVIEW;
            }
        }
        if (LOG) {
            Log.v(TAG, "setCurrentMode(" + mode + ") realmode=" + realmode);
        }
        setRealMode(realmode);
    }

    private void highlightCurrentMode() {
        int index = getModeIndex(mCurrentMode);
        for (int i = 0; i < MODE_NUM_ICON; i++) {
            if (mModeViews[i] != null) {
                if (i == index) {
                    mModeViews[i].setImageResource(MODE_ICONS_HIGHTLIGHT[i]);
                } else {
                    mModeViews[i].setImageResource(MODE_ICONS_NORMAL[i]);
                }
            }
        }
    }
    
    public int getModeIndex(int mode) {
        int index = mode % OFFSET;
        if (LOG) {
            Log.v(TAG, "getModeIndex(" + mode + ") return " + index);
        }
        return index;
    }
    
    public void setListener(OnModeChangedListener l) {
        mModeChangeListener = l;
    }

    @Override
    protected View getView() {
        clearListener();
        View view = inflate(R.layout.mode_picker);
        mScrollView = view.findViewById(R.id.mode_picker_scroller);
        mModeViews[MODE_PHOTO] = (RotateImageView) view.findViewById(R.id.mode_photo);
        mModeViews[MODE_HDR] = (RotateImageView) view.findViewById(R.id.mode_hdr);
        mModeViews[MODE_FACE_BEAUTY] = (RotateImageView) view.findViewById(R.id.mode_face_beauty);
        mModeViews[MODE_PANORAMA] = (RotateImageView) view.findViewById(R.id.mode_panorama);
        mModeViews[MODE_MAV] = (RotateImageView) view.findViewById(R.id.mode_mav);
        mModeViews[MODE_ASD] = (RotateImageView) view.findViewById(R.id.mode_asd);
        mModeViews[MODE_SMILE_SHOT] = (RotateImageView) view.findViewById(R.id.mode_smile);
        mModeViews[MODE_BEST] = (RotateImageView) view.findViewById(R.id.mode_best);
        mModeViews[MODE_EV] = (RotateImageView) view.findViewById(R.id.mode_ev);
        applyListener();
        highlightCurrentMode();
        return view;
    }

    private void applyListener() {
        for (int i = 0; i < MODE_NUM_ICON; i++) {
            if (mModeViews[i] != null) {
                mModeViews[i].setOnClickListener(this);
                mModeViews[i].setOnLongClickListener(this);
            }
        }
    }

    private void clearListener() {
        for (int i = 0; i < MODE_NUM_ICON; i++) {
            if (mModeViews[i] != null) {
                mModeViews[i].setOnClickListener(null);
                mModeViews[i].setOnLongClickListener(null);
                mModeViews[i] = null;
            }
        }
    }
    
    @Override
    public void onClick(View view) {
        if (LOG) {
            Log.v(TAG, "onClick(" + view + ") isEnabled()=" + isEnabled()
                    + ", view.isEnabled()=" + view.isEnabled()
                    + ", getContext().isFullScreen()=" + getContext().isFullScreen());
        }
        if (getContext().isFullScreen()) {
            for (int i = 0; i < MODE_NUM_ICON; i++) {
                if (mModeViews[i] == view) {
                    setCurrentMode(i);
                    break;
                }
            }
        }
    }
    
    private void notifyModeChanged() {
        if (mModeChangeListener != null) {
            mModeChangeListener.onModeChanged(getCurrentMode());
        }
    }
    
    public void onRefresh() {
        if (LOG) {
            Log.v(TAG, "onRefresh() mCurrentMode=" + mCurrentMode);
        }
        int visibleCount = 0;
        for (int i = 0; i < MODE_NUM_ICON; i++) {
            if (mModeViews[i] != null) {
                boolean visible = ModeChecker.getModePickerVisible(getContext(), getContext().getCameraId(), i);
                mModeViews[i].setVisibility(visible ? View.VISIBLE : View.GONE);
                if (visible) {
                    visibleCount++; 
                }
            }
        }
        if (visibleCount <= 1) { //to enable/disable background
            mScrollView.setVisibility(View.GONE);
        } else {
            mScrollView.setVisibility(View.VISIBLE);
        }
        highlightCurrentMode();
    }
    
    @Override
    public boolean onLongClick(View view) {
        if (LOG) {
            Log.v(TAG, "onLongClick(" + view + ")");
        }
        if (view.getContentDescription() != null) {
            if (mModeToast == null) {
                mModeToast = OnScreenToast.makeText(getContext(), view.getContentDescription());
            } else {
                mModeToast.setText(view.getContentDescription());
            }
            mModeToast.showToast();
        }
        //don't consume long click event
        return false;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mScrollView != null) {
            mScrollView.setEnabled(enabled);
        }
        for (int i = 0; i < MODE_NUM_ICON; i++) {
            if (mModeViews[i] != null) {
                mModeViews[i].setEnabled(enabled);
                mModeViews[i].setClickable(enabled);
            }
        }
    }
    
    @Override
    protected void onRelease() {
        super.onRelease();
        mModeToast = null;
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        if (LOG) {
            Log.v(TAG, "onFullScreenChanged(" + full + ") mModeToast=" + mModeToast);
        }
        if (mModeToast != null && !full) {
            mModeToast.cancel();
        }
    }
}
