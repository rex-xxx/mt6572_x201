package com.android.camera.manager;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.camera.Camera;
import com.android.camera.FileSaver;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SaveRequest;
import com.android.camera.Storage;
import com.android.camera.Thumbnail;
import com.android.camera.Util;
import com.android.camera.ui.RotateImageView;

import java.io.File;

public class ThumbnailManager extends ViewManager implements OnClickListener,
        FileSaver.FileSaverListener,
        Camera.Resumable,
        Camera.OnFullScreenChangedListener {
    private static final String TAG = "ThumbnailManager";
    private static final boolean LOG = Log.LOGV;
    
    private static final String ACTION_DELETE_PICTURE =
        "com.android.gallery3d.action.DELETE_PICTURE";
    // An image view showing the last captured picture thumbnail.
    private RotateImageView mThumbnailView;
    private boolean mUpdateThumbnailDelayed;
    private AsyncTask<Void, Void, Thumbnail> mLoadThumbnailTask;
    private Thumbnail mThumbnail;
    private WorkerHandler mWorkerHandler;
    private long mRefreshInterval = 0;
    private long mLastRefreshTime;
    private SaveRequest mLastSaveRequest;
    private SaveRequest mCurrentSaveRequest;
    private boolean mResumed;
    
    private static final int MSG_SAVE_THUMBNAIL = 0;
    private static final int MSG_UPDATE_THUMBNAIL = 1;
    private static final int MSG_CHECK_THUMBNAIL = 2;
    
    private IntentFilter mDeletePictureFilter = new IntentFilter(ACTION_DELETE_PICTURE);    
    private BroadcastReceiver mDeletePictureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOG) {
                Log.v(TAG, "mDeletePictureReceiver.onReceive(" + intent + ")");
            }
            if (isShowing()) {
                getLastThumbnailUncached();
            } else {
                mUpdateThumbnailDelayed = true;
            }
        }
    };
    
    private static final String ACTION_IPO_SHUTDOWN =
        "android.intent.action.ACTION_SHUTDOWN_IPO";
    private IntentFilter mIpoShutdownFilter = new IntentFilter(ACTION_IPO_SHUTDOWN);
    private BroadcastReceiver mIpoShutdownReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LOG) {
                Log.v(TAG, "mIpoShutdownReceiver.onReceive(" + intent + ")");
            }
            saveThumbnailToFile();
        }
    };
    
    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_UPDATE_THUMBNAIL:
                updateThumbnailView();
                break;
            default:
                break;
            }
        }
    };
    
    public ThumbnailManager(Camera context) {
        super(context);
        setFileter(false);
        context.addResumable(this);
        context.addOnFullScreenChangedListener(this);
    }
    
    public void setFileSaver(FileSaver saver) {
        if (saver != null) {
            saver.addListener(this);
        }
    }
    
    public void setRefreshInterval(int ms) {
        if (LOG) {
            Log.v(TAG, "setRefreshInterval(" + ms + ")");
        }
        mRefreshInterval = ms;
        mLastRefreshTime = System.currentTimeMillis();
    }

    @Override
    public void begin() {
        if (mWorkerHandler == null) {
            HandlerThread t = new HandlerThread("thumbnail-creation-thread");
            t.start();
            mWorkerHandler = new WorkerHandler(t.getLooper());
        }
    }
    
    @Override
    public void resume() {
        if (LOG) {
            Log.v(TAG, "resume() mResumed=" + mResumed);
        }
        if (!mResumed) {
            LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getContext());
            manager.registerReceiver(mDeletePictureReceiver, mDeletePictureFilter);
            getContext().registerReceiver(mIpoShutdownReceiver, mIpoShutdownFilter);
            if (isShowing()) {
                // if the ThumbnailView is not showed, do not get last thumbnail.
                getLastThumbnail();
            }
            mResumed = true;
        }
    }
    
    @Override
    public void pause() {
        if (LOG) {
            Log.v(TAG, "pause() mResumed=" + mResumed);
        }
        if (mResumed) {
            LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getContext());
            manager.unregisterReceiver(mDeletePictureReceiver);
            getContext().unregisterReceiver(mIpoShutdownReceiver);
            if (mLoadThumbnailTask != null) {
                mLoadThumbnailTask.cancel(true);
                mLoadThumbnailTask = null;
            }
            saveThumbnailToFile();
            mResumed = false;
        }
    }
    @Override
    public void setEnabled(boolean enabled) {
        if (LOG) {
            Log.v(TAG, "setEnabled " + enabled + " isenable=" + isEnabled());
        }
        super.setEnabled(enabled);
        if (mThumbnailView != null) {
            mThumbnailView.setEnabled(enabled);
            mThumbnailView.setClickable(enabled);
        }
    }
    
    @Override
    public void finish() {
        if (mWorkerHandler != null) {
            mWorkerHandler.getLooper().quit();
        }
    }

    @Override
    protected View getView() {
        View view = inflate(R.layout.thumbnail);
        mThumbnailView = (RotateImageView) view.findViewById(R.id.thumbnail);
        mThumbnailView.setOnClickListener(this);
        return view;
    }
    
    @Override
    protected void onRefresh() {
        updateThumbnailView();
    }
    
    private void updateThumbnailView() {
        if (LOG) {
            Log.v(TAG, "updateThumbnailView() mThumbnailView=" + mThumbnailView
                    + ", mThumbnail=" + mThumbnail + ", isShowing=" + isShowing());
        }
        if (mThumbnailView != null) {
            if (isShowing()) {
                if (mThumbnail != null && mThumbnail.getBitmap() != null) {
                    mThumbnailView.setBitmap(mThumbnail.getBitmap());
                    mThumbnailView.setVisibility(View.VISIBLE);
                } else {
                    mThumbnailView.setBitmap(null);
                    mThumbnailView.setVisibility(View.INVISIBLE);
                }
            } else {
                mThumbnailView.setVisibility(View.INVISIBLE);
            }
            if (LOG) {
                Log.v(TAG, "updateThumbnailView() " + mThumbnailView.getVisibility());
            }
        }
    };
    
    private class LoadThumbnailTask extends AsyncTask<Void, Void, Thumbnail> {
        private boolean mLookAtCache;

        public LoadThumbnailTask(boolean lookAtCache) {
            mLookAtCache = lookAtCache;
        }

        @Override
        protected Thumbnail doInBackground(Void... params) {
            if (LOG) {
                Log.v(TAG, "doInBackground() begin.");
            }
            // Load the thumbnail from the file.
            ContentResolver resolver = getContext().getContentResolver();
            Thumbnail t = null;
            if (mLookAtCache) {
                t = Thumbnail.getLastThumbnailFromFile(getContext().getFilesDir(), resolver);
            }
            if (LOG) {
                Log.v(TAG, "doInBackground() get from thumbnail. thumbnail=" + t + ", isCancelled()=" + isCancelled());
            }
            if (isCancelled()) { return null; }

            if (t == null && Storage.isStorageReady()) {
                Thumbnail result[] = new Thumbnail[1];
                // Load the thumbnail from the media provider.
                int code = Thumbnail.getLastThumbnailFromContentResolver(resolver, result);
                if (LOG) {
                    Log.v(TAG, "getLastThumbnailFromContentResolver code = " + code);
                }
                switch (code) {
                case Thumbnail.THUMBNAIL_FOUND:
                    return result[0];
                case Thumbnail.THUMBNAIL_NOT_FOUND:
                    return null;
                case Thumbnail.THUMBNAIL_DELETED:
                    cancel(true);
                    return null;
                default:
                    return null;
                }
            } else {
                if (LOG) {
                    Log.v(TAG, "getLastThumbnailFromFile = true");
                }
            }
            return t;
        }

        @Override
        protected void onPostExecute(Thumbnail thumbnail) {
            if (LOG) {
                Log.v(TAG, "onPostExecute() thumbnail=" + thumbnail + ", isCancelled()=" + isCancelled());
            }
            if (isCancelled()) { return; }
            mThumbnail = thumbnail;
            updateThumbnailView();
        }
    }
    
    private void getLastThumbnail() {
        /* we should check here.
        //mThumbnail = ThumbnailHolder.getLastThumbnail(getContext().getContentResolver());
        // Suppose users tap the thumbnail view, go to the gallery, delete the
        // image, and coming back to the camera. Thumbnail file will be invalid.
        // Since the new thumbnail will be loaded in another thread later, the
        // view should be set to gone to prevent from opening the invalid image.
         */
        //Here we remove switch mode keeping thumbnail function and refetch the thumbnail.
        //If user delete some uri, mUpdateThumbnailDelayed flag will be used in onRefresh().
        mThumbnail = null;
        updateThumbnailView();
        mLoadThumbnailTask = new LoadThumbnailTask(true).execute();
        if (LOG) {
            Log.v(TAG, "getLastThumbnail() mThumbnail=" + mThumbnail);
        }
    }

    private void getLastThumbnailUncached() {
        if (mLoadThumbnailTask != null) {
            mLoadThumbnailTask.cancel(true);
        }
        mLoadThumbnailTask = new LoadThumbnailTask(false).execute();
    }

    private void saveThumbnailToFile() {
        if (LOG) {
            Log.v(TAG, "saveThumbnailToFile() mThumbnail=" + mThumbnail);
        }
        if (mThumbnail != null && !mThumbnail.fromFile()) {
            new SaveThumbnailTask().execute(mThumbnail);
        }
    }

    private class SaveThumbnailTask extends AsyncTask<Thumbnail, Void, Void> {
        @Override
        protected Void doInBackground(Thumbnail... params) {
            final int n = params.length;
            final File filesDir = getContext().getFilesDir();
            for (int i = 0; i < n; i++) {
                params[i].saveLastThumbnailToFile(filesDir);
            }
            return null;
        }
    }
    
    @Override
    public void onFileSaved(SaveRequest request) {
        if (LOG) {
            Log.v(TAG, "onFileSaved(" + request + ") ignore=" + request.isIgnoreThumbnail());
        }
        //If current URI is not valid, don't create thumbnail.
        if (!request.isIgnoreThumbnail() && request.getUri() != null) {
            mCurrentSaveRequest = request;
            mWorkerHandler.removeMessages(MSG_SAVE_THUMBNAIL);
            mWorkerHandler.sendEmptyMessage(MSG_SAVE_THUMBNAIL);
        }
    }
    
    private void sendUpdateThumbnail() {
        mMainHandler.removeMessages(MSG_UPDATE_THUMBNAIL);
        Message msg = mMainHandler.obtainMessage(MSG_UPDATE_THUMBNAIL, mThumbnail);
        msg.sendToTarget();
    }
    
    private class WorkerHandler extends Handler {
        public WorkerHandler(Looper looper) {
            super(looper);
        }
        
        @Override
        public void handleMessage(Message msg) {
            if (LOG) {
                Log.v(TAG, "handleMessage(" + msg + ")");
            }
            long now = System.currentTimeMillis();
            switch (msg.what) {
            case MSG_SAVE_THUMBNAIL:
                SaveRequest curRequest = mCurrentSaveRequest;
                // M: initialize the ThumbnaiView to create thumbnail when camera 
                // launched by 3rd apps.@{
                if (mThumbnailView == null) {
                    getView();
                } 
                // @}
                if (curRequest != mLastSaveRequest && mThumbnailView != null) {
                    if (mRefreshInterval != 0 && (now - mLastRefreshTime < mRefreshInterval)) {
                        long delay = mRefreshInterval - (now - mLastRefreshTime);
                        sendEmptyMessageDelayed(MSG_SAVE_THUMBNAIL, delay);
                    } else {
                        mLastRefreshTime = now;
                        int thumbnailWidth = mThumbnailView.getLayoutParams().width;
                        Thumbnail thumb = curRequest.createThumbnail(thumbnailWidth);
                        if (thumb != null) { //just update when thumbnail valid
                            mThumbnail = thumb;
                        } else {
                            Log.w(TAG, "Why doesn't create thumbnail success???" + curRequest);
                        }
                        if (!mResumed) {
                            //Here save thumbnail to cache, so after resumed, thumbnail will be right. 
                            saveThumbnailToFile();
                        } else {
                            sendUpdateThumbnail();
                        }
                    }
                }
                break;
            case MSG_CHECK_THUMBNAIL:
                if (mThumbnail != null) {
                    boolean valid = Util.isUriValid(mThumbnail.getUri(), getContext().getContentResolver());
                    if (!valid) {
                        mThumbnail = null;
                        sendUpdateThumbnail();
                    }
                    if (LOG) {
                        Log.v(TAG, "handleMessage() mThumbnail=" + mThumbnail + ", valid=" + valid);
                    }
                }
                break;
            default:
                break;
            }
            if (LOG) {
                Log.v(TAG, "handleMessage() diff=" + (now - mLastRefreshTime)
                        + ", mRefreshInterval=" + mRefreshInterval);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (LOG) {
            Log.v(TAG, "onClick() mThumbnail=" + mThumbnail + " really=" + getContext().isFullScreen());
        }
        if (getContext().isFullScreen() && getContext().isCameraIdle() && mThumbnail != null) {
            if (getContext().getFileSaver() != null) {
                getContext().getFileSaver().waitDone();
            }
            getContext().gotoGallery();
        }
    }

    @Override
    public void onFullScreenChanged(boolean full) {
        if (full && mUpdateThumbnailDelayed) {
            getLastThumbnailUncached();
            mUpdateThumbnailDelayed = false;
        } else {
            updateThumbnailView();
            if (mWorkerHandler != null) {
                mWorkerHandler.sendEmptyMessage(MSG_CHECK_THUMBNAIL);
            }
        }
    }
    
    public void forceUpdate() {
        getLastThumbnail();
    }
}