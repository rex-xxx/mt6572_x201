package com.mediatek.gallery3d.videothumbnail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.LocalVideo;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.ui.AlbumSlidingWindow;
import com.android.gallery3d.ui.AlbumSlotRenderer;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLRoot;
//import com.mediatek.gallery3d.videothumbnail.LoadingMonitor.LoadingListener;

import android.graphics.SurfaceTexture;
import android.util.Log;

public class VideoThumbnailDirector {
    private static final String TAG = "Gallery2/VideoThumbnailDirector";
    private final int HANDLER_CONCURRENCY = 1;

    private VideoThumbnailPlayer mVideoThumbnailPlayer;
    private AbstractGalleryActivity mGalleryActivity;
    private AlbumSlidingWindow mDataWindow;
    private AlbumSlotRenderer mSlotRenderer;

    private DirectorSecretary mSecretary;
    private Object mLockSecretaryBeauty = new Object();
    private Object mLockStarterIndex = new Object();
    private Object mLockStoperIndex = new Object();
    private int mCurrentStarterIndex;
    private int mCurrentStoperIndex;
    private volatile int mActiveStart = 0;
    private volatile int mActiveEnd = 0;
//    private volatile int mContentStart = 0;
//    private volatile int mContentEnd = 0;
    private volatile boolean mIsStageUpdated;
    private PlayerHandler[] mThumbnailStarters = new PlayerHandler[HANDLER_CONCURRENCY];
    private PlayerHandler[] mThumbnailStopers = new PlayerHandler[HANDLER_CONCURRENCY];

    /* comment out this section for emma coverage purpose
     * this section is loading handling related but never used at present
    private LoadingListener mLoadingListener = new LoadingListener() {
        public void loadingChanged(int count) {
            Log.v(TAG, "loading changed, and it's " + count);
            adjustThumbnails(Math.max(count, 0));
        }
    };
    */

    public VideoThumbnailDirector(AbstractGalleryActivity activity, AlbumSlotRenderer albumSlotRenderer) {
        assert activity != null;
        mGalleryActivity = activity;
        mSlotRenderer = albumSlotRenderer;
        VideoThumbnailMaker.setGalleryActivity(mGalleryActivity);
        VideoThumbnailMaker.setDirector(this);
    }

    public void resume(AlbumSlidingWindow dataWindow) {
        pumpLiveThumbnails();
        mDataWindow = dataWindow;
        VideoThumbnailMaker.start();
        mVideoThumbnailPlayer = VideoThumbnailPlayer.create(new VideoThumbnailPlayer.OnFrameAvailableListener() {
            public void render(SurfaceTexture surfaceTexture) {
                GLRoot renderContext = mGalleryActivity.getGLRoot();
                if (renderContext != null) {
                    renderContext.requestRender();
                }
            }

            public void antiRender(SurfaceTexture surfaceTexture) {
                // TODO Auto-generated method stub
            }
        }, mGalleryActivity);
        mVideoThumbnailPlayer.resume();
        for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
            mThumbnailStarters[i] = new PlayerHandler();
            mThumbnailStopers[i] = new PlayerHandler();
            mThumbnailStarters[i].start();
            mThumbnailStopers[i].start();            
        }
        mSecretary = new DirectorSecretary();
        mSecretary.start();
        /* comment out this section for emma coverage purpose
         * this section is loading handling related but never used at present
        if (VideoThumbnailFeatureOption.OPTION_MONITOR_LOADING) {
            // Note: LoadingMonitor can cause memory leakage
            // Never enable it without modification
            LoadingMonitor loading = new LoadingMonitor(mLoadingListener);
            loading.setInervalTime(8);
            loading.startCheck();
        }
        */
    }

    public void pause() {
        mSecretary.interrupt();
        mSecretary = null;
        // unpumpLiveThumbnails();
        new Thread() {
            public void run() {
                setName("thumbnail player pauser");
                mVideoThumbnailPlayer.pause();
            }
        }.start();
        VideoThumbnailMaker.pause();
        for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
            if (mThumbnailStarters[i] != null) {
                mThumbnailStarters[i].interrupt();
            }
            if (mThumbnailStopers[i] != null) {
                mThumbnailStopers[i].interrupt();
            }
        }
        /* comment out this class for emma coverage purpose
         * this section is loading handling related but never used at present
        if (VideoThumbnailFeatureOption.OPTION_MONITOR_LOADING) {
            // Note: LoadingMonitor can cause memory leakage
            // Never enable it without modification
            // At least, the following function should be implemented
            // StopLoadingMonitor();
        }
        */
        // mDataWindow = null;
    }

    // in GL thread
    public boolean renderThumbnail(
            final AlbumSlidingWindow.AlbumEntry entry, final GLCanvas canvas,
            final int width, final int height) {
        if (entry.mediaType != MediaObject.MEDIA_TYPE_VIDEO) {
            return false;
        }
        LocalVideo video = (LocalVideo) entry.item;
        if (video != null) {
            if (!VideoThumbnailFeatureOption.OPTION_TRANSCODE_BEFORE_PLAY) {
                return renderThumbnailByPath(video.filePath, canvas, width,
                        height);
            }
            if (video.thumbNailState == LocalVideo.THUMBNAIL_STATE_DYNAMIC_GENERATED_SUCCESS) {
                return renderThumbnailByPath(video.dynamicThumbnailPath, canvas,
                        width, height);
            }
        }
        return false;
    }

    // in main thread
    public void updateStage() {
        mActiveStart = mDataWindow.getActiveStart();
        mActiveEnd = mDataWindow.getActiveEnd();
//        mContentStart = mDataWindow.getContentStart();
//        mContentEnd = mDataWindow.getContentEnd();
        pumpLiveThumbnails();
    }

    public void pumpLiveThumbnails() {
        synchronized (mLockSecretaryBeauty) {
            mIsStageUpdated = true;
            mLockSecretaryBeauty.notifyAll();
        }
    }

    /* comment out this section for emma coverage purpose
     * this section is loading handling related but never used at present
    public void dumbLiveThumbnails() {
        synchronized (mLockSecretaryBeauty) {
            mIsStageUpdated = false;
        }
    }
    */

    private boolean renderThumbnailByPath(final String thumbnailPath,
            final GLCanvas canvas, final int width, final int height) {
        return mVideoThumbnailPlayer.renderThumbnail(thumbnailPath, canvas, width, height);
    }

    private boolean isThumbnailInStage(final String thumbnailPath) {
        AlbumSlidingWindow.AlbumEntry entry;
        boolean isInStage = false;
        LocalVideo video;
        for (int i = mActiveStart; i < mActiveEnd; i++) {
            if (mDataWindow == null) {
                break;
            }
            // following should be done in UI thread
            if (!mDataWindow.isActiveSlot(i)) {
                break;
            }
            try {
                entry = mDataWindow.get(i);
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                break;
            }
            // above should be done in UI thread
            if (entry == null) {
                break;
            }
            if (entry.mediaType == MediaObject.MEDIA_TYPE_VIDEO) {
                video = (LocalVideo)(entry.item);
                if (video == null) {
                    break;
                }
                if (VideoThumbnailFeatureOption.OPTION_TRANSCODE_BEFORE_PLAY) {
                    if (thumbnailPath.equals(video.dynamicThumbnailPath)) {
                        isInStage = true;
                        break;
                    }
                } else {
                    if (thumbnailPath.equals(video.filePath)) {
                        isInStage = true;
                        break;
                    }
                }
            }
        }
        return isInStage;
    }

    // in GL thread
    private void respondToStageUpdate() {
        if (!mIsStageUpdated) {
            return;
        }

        // make sure all static thumbnails go first
        if (!/*isStaticThumbnailDone()*/mDataWindow.isAllActiveSlotsStaticThumbnailReady()) {
            return;
        }
        
        List<String> thumbnailPaths = new ArrayList<String>();
        mVideoThumbnailPlayer.getPlayingThumbnails(thumbnailPaths);
        if (!scrollThumbnailsOutOfScene(thumbnailPaths)) {
            return;
        }

        List<LocalVideo> thumbnailsOnStage = new ArrayList<LocalVideo>();
        this.collectAllThumbnailsOnStage(thumbnailsOnStage);
        shuffleThumbnails();

        if (VideoThumbnailFeatureOption.OPTION_TRANSCODE_BEFORE_PLAY) {
            this.requestThumbnails(thumbnailsOnStage);        
            // may we can put some background requests here
        }

        // start new visible videos (enqueue for starting)
        this.selectPreparedThumbnails(thumbnailsOnStage, thumbnailPaths);
        if (!scrollThumbnailsIntoScene(thumbnailPaths)) {
            return;        
        }
 
        mIsStageUpdated = false;
    }

    private void requestThumbnails(List<LocalVideo> thumbnails) {
        VideoThumbnailMaker.cancelPendingTranscode();
        for (LocalVideo video : thumbnails) {
            if (video.thumbNailState == LocalVideo.THUMBNAIL_STATE_STATIC) {
                VideoThumbnailMaker.requestThumbnail(video);
            }
        }
    }

    // returning false: without real starting
    private boolean scrollThumbnailsIntoScene(final List<String> thumbnailPaths) {
        for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
            if (mThumbnailStarters[i] != null) {
                mThumbnailStarters[i].cancelPendingRunnables();
            }
        }
        if (mSlotRenderer.isStageChanging()) {
            return false;
        }
        return startThumbnails(thumbnailPaths);
    }

    private boolean startThumbnails(final List<String> thumbnailPaths) {
        int currentStarterIndex;
        for (final String path : thumbnailPaths) {
            currentStarterIndex = mCurrentStarterIndex;
            currentStarterIndex %= HANDLER_CONCURRENCY;
            mCurrentStarterIndex ++;
            mCurrentStarterIndex %= HANDLER_CONCURRENCY;
            final PlayerHandler playerStarter = mThumbnailStarters[currentStarterIndex];
            if (playerStarter != null) {
                playerStarter.submit(new Runnable() {
                    public void run() {
                        if (!mVideoThumbnailPlayer.isThumbnailPlaying(path)
                                && isThumbnailInStage(path)) {
                            if (mSlotRenderer.isStageChanging()
                                    || !mVideoThumbnailPlayer.openThumbnail(path)) {
                                // hey, retry
                                playerStarter.submit(this);
                            }
                        }
                    }
                });
            }
        }
        return true;
    }

    // returning false: without real stopping
    private boolean scrollThumbnailsOutOfScene(final List<String> thumbnailPaths) {
        for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
            if (mThumbnailStopers[i] != null) {
                mThumbnailStopers[i].cancelPendingRunnables();
            }
        }
        if (mSlotRenderer.isStageChanging()) {
            return false;
        }
        return stopThumbnails(thumbnailPaths, false);
    }

    // returning false: without real stopping
    private boolean stopThumbnails(final List<String> thumbnailPaths, final boolean forceClose) {
        int currentStoperIndex;
        for (final String path : thumbnailPaths) {
            currentStoperIndex = mCurrentStoperIndex;
            currentStoperIndex %= HANDLER_CONCURRENCY;
            mCurrentStoperIndex ++;
            mCurrentStoperIndex %= HANDLER_CONCURRENCY;
            final PlayerHandler playerStoper = mThumbnailStopers[currentStoperIndex];
            if (playerStoper != null) {
                playerStoper.submit(
                    new Runnable() {
                        public void run() {
                            boolean shouldDelete = forceClose ? true : !isThumbnailInStage(path);
                            if (shouldDelete) {
                                mVideoThumbnailPlayer.closeThumbnail(path);
                            } else {
                                try {
                                    playerStoper.mRunnableQueue.take().run();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                );
            }
        }
        return true;
    }

    private void collectAllThumbnailsOnStage(List<LocalVideo> videos) {
        videos.clear();
        AlbumSlidingWindow.AlbumEntry entry;
        LocalVideo video;
        for (int i = mActiveStart; i < mActiveEnd; i++) {
            // TODO: move the below outside of for
            if (mDataWindow == null) {
                break;
            }
            // following should be done in UI thread
            if (!mDataWindow.isActiveSlot(i)) {
                break;
            }
            try {
                entry = mDataWindow.get(i);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                break;
            }
            // above should be done in UI thread
            if (entry == null) {
                break;
            }
            if (entry.mediaType == MediaObject.MEDIA_TYPE_VIDEO) {
                video = (LocalVideo)(entry.item);
                if (video != null) {
                    videos.add(video);
                }
            }
        }
    }

    private void selectPreparedThumbnails(final List<LocalVideo> candidateThumbnails, List<String> preparedThumbnails) {
        preparedThumbnails.clear();
        if (VideoThumbnailFeatureOption.OPTION_TRANSCODE_BEFORE_PLAY) {
            for (LocalVideo video : candidateThumbnails) {
                if (video.thumbNailState == LocalVideo.THUMBNAIL_STATE_DYNAMIC_GENERATED_SUCCESS) {
                    preparedThumbnails.add(video.dynamicThumbnailPath);
                }
            }
        } else {
            for (LocalVideo video : candidateThumbnails) {
                preparedThumbnails.add(video.filePath);
            }
        }
    }

    private void shuffleThumbnails() {
        // place holder
        // for the situation where all videos must be played, it's useless
    }

    private static class PlayerHandler extends Thread {
        // TODO: use blocking stack is more friendly, to be modified
        public final BlockingQueue<Runnable> mRunnableQueue;
        private Runnable mCurrentRunnable;

        public PlayerHandler() {
            super("Player handler");
            mRunnableQueue = new LinkedBlockingQueue<Runnable>();
        }

        public void run() {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                while(!Thread.currentThread().isInterrupted()) {
                    synchronized (this) {
                        while (mPausing) {
                            wait();
                        }
                        mCurrentRunnable = mRunnableQueue.take();                    
                    }
                    mCurrentRunnable.run();
//                    try {
//                        Thread.sleep(300);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
                }
            } catch (InterruptedException e) {
                this.interrupt();
            }
        }

        public void submit(Runnable runnable) {
            if (isAlive()) {
                mRunnableQueue.add(runnable);
            } else {
                Log.e(TAG, getName() + " should be started before submitting tasks.");
            }
        }

        public void pause() {
            mPausing = true;
        }

        public void reGo() {
            synchronized (this) {
                mPausing = false;
                notifyAll();
            }
        }

        private volatile boolean mPausing;

        public void cancelPendingRunnables() {
            mRunnableQueue.clear();
        }
    }

    private class DirectorSecretary extends Thread {
        public void run() {
            setName("pretty secretary");
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (mLockSecretaryBeauty) {
                    while (!mIsStageUpdated) {
                        try {
                            mLockSecretaryBeauty.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            return;
                        }
                    }
                }
                if (mSlotRenderer.isStageFixed()) {
                    respondToStageUpdate();
                    try {
                        sleep(80);  // not good
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }
    }

    /* comment out this section for emma coverage purpose
     * this section is loading handling related but never used at present
    private int getMaxThumbnailCountAtRuntime(float loadingFactor) {
        if (loadingFactor < 80) {
            return 100;
        }
        // TODO: you know
        int maxThumbnailCountAtRuntime = (int) Math.floor(-0.075*loadingFactor + 7.5);
        return maxThumbnailCountAtRuntime;
    }

    private void adjustThumbnails(float loadingFactor) {
        final int maxThumbnailCountAtRuntime = getMaxThumbnailCountAtRuntime(loadingFactor);
        final int playingThumbnailCount = mVideoThumbnailPlayer.getPlayingThumbnailCount();
        if (maxThumbnailCountAtRuntime > playingThumbnailCount) {
            // resume starters
            for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
                if (mThumbnailStarters[i] != null) {
                    mThumbnailStarters[i].reGo();
                }
            }
        } else if (maxThumbnailCountAtRuntime < playingThumbnailCount) {
            // pause starter
            for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
                if (mThumbnailStarters[i] != null) {
                    mThumbnailStarters[i].pause();
                }
            }
            List<String> thumbnailPaths = new ArrayList<String>();
            mVideoThumbnailPlayer.getPlayingThumbnails(thumbnailPaths);
            thumbnailPaths = thumbnailPaths.subList(0, Math.min(thumbnailPaths.size(), playingThumbnailCount - maxThumbnailCountAtRuntime));
            stopThumbnails(thumbnailPaths, true);
            // wait chances to re-start
            startThumbnails(thumbnailPaths);
        }
    }
    */
}
