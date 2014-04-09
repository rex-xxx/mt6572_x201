package com.android.camera.manager;

import android.graphics.Bitmap;
import android.hardware.Camera.CameraInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.android.camera.Camera;
import com.android.camera.CameraHolder;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.Thumbnail;
import com.android.camera.Util;
import com.android.camera.ui.RotateImageView;

import java.io.FileDescriptor;

public class ReviewManager extends ViewManager implements View.OnClickListener {
    private static final String TAG = "ReviewManager";
    private static final boolean LOG = Log.LOGV;

    private ImageView mReviewImage;
    private RotateImageView mRetakeView;
    private RotateImageView mPlayView;
    private FileDescriptor mFileDescriptor;
    private String mFilePath;
    private int mOrientationCompensation;
    private Bitmap mReviewBitmap;

    public ReviewManager(Camera context) {
        super(context, VIEW_LAYER_BOTTOM);
    }

    @Override
    protected View getView() {
        View view = inflate(R.layout.review_layout);
        mPlayView = (RotateImageView) view.findViewById(R.id.btn_play);
        mRetakeView = (RotateImageView) view.findViewById(R.id.btn_retake);
        mReviewImage = (ImageView) view.findViewById(R.id.review_image);
        if (mReviewImage != null && getContext().isImageCaptureIntent()) {
            mReviewImage.setVisibility(View.GONE);
        }
        if (mPlayView != null && getContext().isImageCaptureIntent()) {
            mPlayView.setVisibility(View.GONE);
        } else {
            mPlayView.setVisibility(View.VISIBLE);
        }
        mRetakeView.setOnClickListener(this);
        mPlayView.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View view) {
        OnClickListener listener = null;
        if (mRetakeView == view) {
            listener = getContext().getCameraActor().getRetakeListener();
        } else {
            listener = getContext().getCameraActor().getPlayListener();
        }
        if (listener != null) {
            listener.onClick(view);
        }
        if (LOG) {
            Log.v(TAG, "onClick(" + view + ") listener=" + listener);
        }
    }

    @Override
    protected void onRefresh() {
        if (LOG) {
            Log.v(TAG, "onRefresh() mFileDescriptor=" + mFileDescriptor + ", mFilePath=" + mFilePath
                    + ", OrientationCompensation=" + mOrientationCompensation + ", mReviewBitmap=" + mReviewBitmap);
        }
        if (mReviewBitmap == null) {
            if (mFileDescriptor != null) {
                mReviewBitmap = Thumbnail.createVideoThumbnailBitmap(mFileDescriptor, getContext().getPreviewFrameWidth());
            } else if (mFilePath != null) {
                mReviewBitmap = Thumbnail.createVideoThumbnailBitmap(mFilePath, getContext().getPreviewFrameWidth());
            }
        }
        if (mReviewBitmap != null && mReviewImage != null) {
            // MetadataRetriever already rotates the thumbnail. We should rotate
            // it to match the UI orientation (and mirror if it is front-facing camera).
            CameraInfo[] info = CameraHolder.instance().getCameraInfo();
            boolean mirror = (info[getContext().getCameraId()].facing == CameraInfo.CAMERA_FACING_FRONT);
            mReviewBitmap = Util.rotateAndMirror(mReviewBitmap, -mOrientationCompensation, mirror);
            mReviewImage.setImageBitmap(mReviewBitmap);
            mReviewImage.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    protected void onRelease() {
        super.onRelease();
        if (mReviewImage != null) {
            mReviewImage.setImageBitmap(null);
        }
    }

    public void show(FileDescriptor fd) {
        if (LOG) {
            Log.v(TAG, "show(" + fd + ") mReviewBitmap=" + mReviewBitmap);
        }
        mFileDescriptor = fd;
        mReviewBitmap = null;
        show();
    }

    public void show(String filePath) {
        if (LOG) {
            Log.v(TAG, "show(" + filePath + ") mReviewBitmap=" + mReviewBitmap);
        }
        mFilePath = filePath;
        mReviewBitmap = null;
        show();
    }

    public void setOrientationCompensation(int orientationCompensation) {
        if (LOG) {
            Log.v(TAG, "setOrientationCompensation(" + orientationCompensation + ")");
        }
        mOrientationCompensation = orientationCompensation;
    }
}
