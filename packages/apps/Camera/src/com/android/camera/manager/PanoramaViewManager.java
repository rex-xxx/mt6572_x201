package com.android.camera.manager;

import android.graphics.Matrix;
import android.view.View;
import android.view.ViewGroup;

import com.android.camera.AnimationController;
import com.android.camera.Camera;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.ui.NaviLineImageView;
import com.android.camera.ui.ProgressIndicator;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.RotateLayout;
import com.android.camera.ui.RotateLayout.OnSizeChangedListener;

public class PanoramaViewManager extends ViewManager {
    private static final String TAG = "PanoramaViewManager";
    private static final boolean LOG = Log.LOGV;

    private View mRootView;
    private View mPanoView;
    private RotateLayout mScreenProgressLayout;
    private ViewGroup mDirectionSigns[] = new ViewGroup[4];// up,down,left,right
    private ViewGroup mCenterIndicator;
    private NaviLineImageView mNaviLine;
    private ProgressIndicator mProgressIndicator;
    private ViewGroup mCollimatedArrowsDrawable;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private boolean mS3DMode = false;
    /** package */
    static final boolean ANIMATION = true;
    private Matrix mSensorMatrix[];
    private Matrix mDisplayMatrix = new Matrix();

    private static final int DIRECTION_RIGHT = 0;
    private static final int DIRECTION_LEFT = 1;
    private static final int DIRECTION_UP = 2;
    private static final int DIRECTION_DOWN = 3;
    private static final int DIRECTION_UNKNOWN = 4;

    private int mSensorDirection = DIRECTION_UNKNOWN;
    private int mDisplayDirection = DIRECTION_UNKNOWN;
    private int mDisplayOrientaion;
    private int mDisplayRotation;

    private static final int TARGET_DISTANCE_HORIZONTAL = 160;
    private static final int TARGET_DISTANCE_VERTICAL = 120;
    private static final int PANO_3D_OVERLAP_DISTANCE = 32;
    private static final int NONE_ORIENTATION = -1;

    public static final int PANORAMA_VIEW = 0;
    public static final int MAV_VIEW = 1;

    private int mHalfArrowHeight = 0;
    private int mHalfArrowLength = 0;
    private AnimationController mAnimation;
    private boolean mNeedInitialize = true;
    private ViewChangeListener mViewChangedListener;
    private int mViewCategory;
    private int mHoldOrientation = NONE_ORIENTATION;

    private OnSizeChangedListener mOnSizeChangedListener = new OnSizeChangedListener() {
        @Override
        public void onSizeChanged(int width, int height) {
            if (LOG) {
                Log.v(TAG, "onSizeChanged width=" + width + " height=" + height);
            }
            mPreviewWidth = Math.max(width, height);
            mPreviewHeight = Math.min(width, height);
        }
    };

    public PanoramaViewManager(Camera context, int viewCategory) {
        super(context);
        mViewCategory = viewCategory;
    }

    public void setViewChangedListener(ViewChangeListener viewChangedListener) {
        mViewChangedListener = viewChangedListener;
    }

    public void show() {
        super.show();
        if (mNeedInitialize) {
            initializeViewManager();
            mNeedInitialize = false;
        }
        showCaptureView();
    }

    /**
     * will be called if app want to show current view which hasn't been created.
     * @return
     */
    @Override
    protected View getView() {
        View view = inflate(R.layout.pano_preview);
        mRootView = view.findViewById(R.id.pano_frame_layout);
        return view;
    }

    private void initializeViewManager() {
        mPanoView = mRootView.findViewById(R.id.pano_view);

        mScreenProgressLayout = (RotateLayout) mRootView.findViewById(R.id.on_screen_progress);
        mCenterIndicator = (ViewGroup) mRootView.findViewById(R.id.center_indicator);
        mDirectionSigns[DIRECTION_RIGHT] = (ViewGroup) mRootView.findViewById(R.id.pano_right);
        mDirectionSigns[DIRECTION_LEFT] = (ViewGroup) mRootView.findViewById(R.id.pano_left);
        mDirectionSigns[DIRECTION_UP] = (ViewGroup) mRootView.findViewById(R.id.pano_up);
        mDirectionSigns[DIRECTION_DOWN] = (ViewGroup) mRootView.findViewById(R.id.pano_down);
        mAnimation = new AnimationController(mDirectionSigns, (ViewGroup) mCenterIndicator.getChildAt(0));

        if (mViewCategory == PANORAMA_VIEW) {
            mNaviLine = (NaviLineImageView) mRootView.findViewById(R.id.navi_line);
            mCollimatedArrowsDrawable = (ViewGroup) mRootView.findViewById(R.id.static_center_indicator);

            mProgressIndicator = new ProgressIndicator(getContext(), ProgressIndicator.TYPE_PANO);
            mProgressIndicator.setVisibility(View.GONE);
            mProgressIndicator.setOrientation(getOrientation());

            prepareSensorMatrix();
        } else if (mViewCategory == MAV_VIEW) {
            mProgressIndicator = new ProgressIndicator(getContext(), ProgressIndicator.TYPE_MAV);
            mProgressIndicator.setVisibility(View.GONE);
            mProgressIndicator.setOrientation(getOrientation());
        }

        mDisplayOrientaion = getContext().getDisplayOrientation();
        mDisplayRotation = getContext().getDisplayRotation();
        mScreenProgressLayout.setOnSizeChangedListener(mOnSizeChangedListener);
    }

    /**
     * will be called when app call release() to unload views from view hierarchy.
     */
    protected void onRelease() {
        mNeedInitialize = true;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (LOG) {
            Log.v(TAG, "onOrientationChanged mContext.getCameraState()=" + getContext().getCameraState()
                    + " orientation=" + orientation);
        }
        if (getContext().getCameraState() != Camera.STATE_SNAPSHOT_IN_PROGRESS) {
            super.onOrientationChanged(orientation);
            if (mProgressIndicator != null) {
                mProgressIndicator.setOrientation(orientation);
            }
            mHoldOrientation = NONE_ORIENTATION;
        } else {
            mHoldOrientation = orientation;
        }
    }

    public void showCaptureView() {
        // reset orientation,since camera state is snapinprogress at last time. 
        if (mHoldOrientation != NONE_ORIENTATION) {
            onOrientationChanged(mHoldOrientation);
        }
        if (mViewCategory == MAV_VIEW) {
            for (int i = 0; i < 4; i++) {
                mDirectionSigns[i].setVisibility(View.INVISIBLE);
            }
            mCenterIndicator.setVisibility(View.VISIBLE);
            mAnimation.startCenterAnimation();
        } else {
            mCenterIndicator.setVisibility(View.GONE);
        }
        mPanoView.setVisibility(View.VISIBLE);
        mProgressIndicator.setVisibility(View.VISIBLE);
    }

    private void updateDirection(int direction) {
        if (mDisplayOrientaion == 90) {
            // Rotate sensor direction to Display direction
            if (direction == DIRECTION_LEFT || direction == DIRECTION_RIGHT) {
                direction = 3 - direction;
            } else {
                direction -= 2;
            }
        }
        if (LOG) {
            Log.v(TAG, "updateDirection mDirection: " + mSensorDirection + " direction: " + direction);
        }
        if (mSensorDirection != direction) {
            mSensorDirection = direction;
            if (mSensorDirection != DIRECTION_UNKNOWN) {
                mViewChangedListener.onCaptureBegin();
                setOrientationIndicator(direction);
                mCenterIndicator.setVisibility(View.VISIBLE);
                mAnimation.startCenterAnimation();
                for (int i = 0; i < 4; i++) {
                    mDirectionSigns[i].setVisibility(View.INVISIBLE);
                }
            } else {
                mCenterIndicator.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void setOrientationIndicator(int direction) {
        if (direction == DIRECTION_RIGHT) {
            ((Rotatable) mCollimatedArrowsDrawable).setOrientation(0, ANIMATION);
            ((Rotatable) mCenterIndicator).setOrientation(0, ANIMATION);
            mNaviLine.setRotation(-90);
        } else if (direction == DIRECTION_LEFT) {
            ((Rotatable) mCollimatedArrowsDrawable).setOrientation(180, ANIMATION);
            ((Rotatable) mCenterIndicator).setOrientation(180, ANIMATION);
            mNaviLine.setRotation(90);
        } else if (direction == DIRECTION_UP) {
            ((Rotatable) mCollimatedArrowsDrawable).setOrientation(90, ANIMATION);
            ((Rotatable) mCenterIndicator).setOrientation(90, ANIMATION);
            mNaviLine.setRotation(180);
        } else if (direction == DIRECTION_DOWN) {
            ((Rotatable) mCollimatedArrowsDrawable).setOrientation(270, ANIMATION);
            ((Rotatable) mCenterIndicator).setOrientation(270, ANIMATION);
            mNaviLine.setRotation(0);
        }
    }

    private void prepareSensorMatrix() {
        mSensorMatrix = new Matrix[4];

        mSensorMatrix[DIRECTION_LEFT] = new Matrix();
        mSensorMatrix[DIRECTION_LEFT].setScale(-1, -1);
        mSensorMatrix[DIRECTION_LEFT].postTranslate(0, TARGET_DISTANCE_VERTICAL);

        mSensorMatrix[DIRECTION_RIGHT] = new Matrix();
        mSensorMatrix[DIRECTION_RIGHT].setScale(-1, -1);
        mSensorMatrix[DIRECTION_RIGHT].postTranslate(TARGET_DISTANCE_HORIZONTAL * 2, TARGET_DISTANCE_VERTICAL);

        mSensorMatrix[DIRECTION_UP] = new Matrix();
        mSensorMatrix[DIRECTION_UP].setScale(-1, -1);
        mSensorMatrix[DIRECTION_UP].postTranslate(TARGET_DISTANCE_HORIZONTAL, 0);

        mSensorMatrix[DIRECTION_DOWN] = new Matrix();
        mSensorMatrix[DIRECTION_DOWN].setScale(-1, -1);
        mSensorMatrix[DIRECTION_DOWN].postTranslate(TARGET_DISTANCE_HORIZONTAL, TARGET_DISTANCE_VERTICAL * 2);
    }

    private void prepareTransformMatrix(int direction) {
        mDisplayMatrix.reset();
        int halfPrewWidth = mPreviewWidth >> 1;
        int halfPrewHeight = mPreviewHeight >> 1;

        // Determine the length / height of the arrow.
        getArrowHL();

        // For simplified calculation of view rectangle, clip arrow length
        // for both view width and height.
        // Arrow may look like this "--------------->"
        float halfViewWidth = (float) halfPrewWidth - mHalfArrowLength;
        float halfViewHeight = (float) halfPrewHeight - mHalfArrowLength;

        mDisplayMatrix.postScale(halfViewWidth / TARGET_DISTANCE_HORIZONTAL, halfViewHeight / TARGET_DISTANCE_VERTICAL);
        switch (mDisplayOrientaion) {
        case 0:
            break;
        case 90:
            mDisplayMatrix.postTranslate(0, -halfViewHeight * 2);
            mDisplayMatrix.postRotate(90);
            break;
        case 180:
            mDisplayMatrix.postTranslate(-halfViewWidth * 2, -halfViewHeight * 2);
            mDisplayMatrix.postRotate(180);
            break;
        case 270:
            mDisplayMatrix.postTranslate(-halfViewHeight * 2, 0);
            mDisplayMatrix.postRotate(-90);
            break;
        default:
            break;
        }
        mDisplayMatrix.postTranslate(mHalfArrowLength, mHalfArrowLength);
    }

    private void getArrowHL() {
        if (mHalfArrowHeight == 0) {
            int naviWidth = mNaviLine.getWidth();
            int naviHeight = mNaviLine.getHeight();
            if (naviWidth > naviHeight) {
                mHalfArrowLength = naviWidth >> 1;
                mHalfArrowHeight = naviHeight >> 1;
            } else {
                mHalfArrowHeight = naviWidth >> 1;
                mHalfArrowLength = naviHeight >> 1;
            }
        }
    }

    private void updateUIShowingMatrix(int x, int y, int direction) {
        // Be sure it's called in onFrame.
        float[] pts = { x, y };
        mSensorMatrix[direction].mapPoints(pts);
        if (LOG) {
            Log.i(TAG, "Matrix x = " + pts[0] + " y = " + pts[1]);
        }

        prepareTransformMatrix(direction);
        mDisplayMatrix.mapPoints(pts);
        if (LOG) {
            Log.i(TAG, "DisplayMatrix x = " + pts[0] + " y = " + pts[1]);
        }

        int fx = (int) pts[0];
        int fy = (int) pts[1];

        mNaviLine.setLayoutPosition(fx - mHalfArrowHeight, fy - mHalfArrowLength,
                fx + mHalfArrowHeight, fy + mHalfArrowLength);

        updateDirection(direction);
        mNaviLine.setVisibility(View.VISIBLE);
    }

    public void setViewsForNext(int imageNum) {
        if (!filterViewCategory(PANORAMA_VIEW)) {
            return;
        }
        mProgressIndicator.setProgress(imageNum + 1);
        if (imageNum == 0) {
            mAnimation.startDirectionAnimation();
        } else {
            mNaviLine.setVisibility(View.INVISIBLE);
            mAnimation.stopCenterAnimation();
            mCenterIndicator.setVisibility(View.GONE);
            mCollimatedArrowsDrawable.setVisibility(View.VISIBLE);
        }
    }

    public void setProgress(int num) {
        if (mProgressIndicator != null) {
            mProgressIndicator.setProgress(num);
        }
    }

    public void startCenterAnimation() {
        mCollimatedArrowsDrawable.setVisibility(View.GONE);
        mAnimation.startCenterAnimation();
        mCenterIndicator.setVisibility(View.VISIBLE);
    }

    public void updateMovingUI(int xy, int direction, boolean shown) {
        if (!filterViewCategory(PANORAMA_VIEW)) {
            return;
        }
        // direction means sensor towards.
        if (direction == DIRECTION_UNKNOWN || shown || 0 == mNaviLine.getWidth()
                || 0 == mNaviLine.getHeight()) {
            //if NaviLine's size is 0, draw NaviLine position will be wrong.
            mNaviLine.setVisibility(View.INVISIBLE);
            return;
        }
        short x = (short) ((xy & 0xFFFF0000) >> 16);
        short y = (short) (xy & 0x0000FFFF);

        updateUIShowingMatrix(x, y, direction);
    }

    private boolean filterViewCategory(int requestCategory) {
        if (mViewCategory != requestCategory) {
            if (LOG) {
                Log.v(TAG, "Only panorama could call this method. mViewCategory=" + mViewCategory);
            }
            return false;
        }
        return true;
    }

    public void set3DMode(boolean panoramaMode) {
        mS3DMode = panoramaMode;
    }

    public void resetController() {
        if (LOG) {
            Log.v(TAG, "resetController mViewCategory=" + mViewCategory);
        }
        mPanoView.setVisibility(View.GONE);
        mProgressIndicator.setProgress(0);// reset to 0
        mProgressIndicator.setVisibility(View.GONE);
        mAnimation.stopCenterAnimation();
        mCenterIndicator.setVisibility(View.GONE);

        if (mViewCategory == PANORAMA_VIEW) {
            mSensorDirection = DIRECTION_UNKNOWN;
            mNaviLine.setVisibility(View.GONE);
            mCollimatedArrowsDrawable.setVisibility(View.GONE);
            for (int i = 0; i < 4; i++) {
                mDirectionSigns[i].setSelected(false);
                mDirectionSigns[i].setVisibility(View.VISIBLE);
            }
        }
    }

    public interface ViewChangeListener {
        void onCaptureBegin();
    }
}
