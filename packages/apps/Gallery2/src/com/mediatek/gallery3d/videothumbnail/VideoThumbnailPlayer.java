package com.mediatek.gallery3d.videothumbnail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.ui.GLCanvas;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;

public class VideoThumbnailPlayer {
    private static final String TAG = "Gallery2/VideoThumbnailPlayer";
    private static final int INIT_POOL_SIZE = 16;
    private static final int MAX_PLAYER_COUNT = INIT_POOL_SIZE;

    private class VideoThumbnailPlayInfo {
        public final MediaPlayer mediaPlayer = new MediaPlayer();
        public final VideoThumbnail renderTarget = new VideoThumbnail() {
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (VideoThumbnailPlayInfo.this.renderTarget.isWorking) {
                    VideoThumbnailPlayer.this.mOnFrameAvailableListener
                            .render(surfaceTexture);
                    synchronized (renderTarget) {
                        mHasNewFrame = true;
                    }
                    mGalleryActivity.getGLRoot().addOnGLIdleListener(renderTarget);
                } else {
                    VideoThumbnailPlayer.this.mOnFrameAvailableListener
                            .antiRender(surfaceTexture);
                }
            }
        };
        public Surface surface;

        public String thumnailPath = null;

        public VideoThumbnailPlayInfo() {
            renderTarget.acquireSurfaceTexture();
            SurfaceTexture texture = renderTarget.getSurfaceTexture();
            surface = new Surface(texture);
            mediaPlayer.setOnVideoSizeChangedListener(renderTarget);
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0, 0);
            mediaPlayer.setSurface(surface);
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e(TAG, "error happened in video thumbnail's internal player. \n\tmay triggered by video deletion");
                    closeThumbnailByPlayer(mp);
                    return false;
                }
            });
        }

        public void recycle() {
            renderTarget.isWorking = false;
            thumnailPath = null;
            renderTarget.isReadyForRender = false;
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                Log.v(TAG, "thumbnail is released by pausing, give up recycling once again");
            }
        }
    }

    public interface OnFrameAvailableListener {
        public void render(SurfaceTexture surfaceTexture);

        public void antiRender(SurfaceTexture surfaceTexture);
    }

    private AbstractGalleryActivity mGalleryActivity;
    // the following three can be merged (redundant)
    private final List<VideoThumbnailPlayInfo> mPool;
    private final List<VideoThumbnailPlayInfo> mCheckOuts;
    private final Map<MediaPlayer, VideoThumbnailPlayInfo> mMap;
    private final Object mPoolLock = new Object();
    private final Object mCheckOutsLock = new Object();
    private final Object mMapLock = new Object();

    private final OnFrameAvailableListener mOnFrameAvailableListener;
    // do no harm to performance, for I'll remove it later
    private volatile int mPlayerCount;
    private volatile boolean mIsWorking;

    public VideoThumbnailPlayer(int initialSize,
            OnFrameAvailableListener listener, AbstractGalleryActivity activity) {
        mPool = new ArrayList<VideoThumbnailPlayInfo>(initialSize);
        mCheckOuts = new ArrayList<VideoThumbnailPlayInfo>(initialSize);
        mMap = new HashMap<MediaPlayer, VideoThumbnailPlayInfo>(initialSize);
        mOnFrameAvailableListener = listener;
        mGalleryActivity = activity;
    }

    public VideoThumbnailPlayer(OnFrameAvailableListener listener, AbstractGalleryActivity activity) {
        this(INIT_POOL_SIZE, listener, activity);
    }

    public static VideoThumbnailPlayer create(OnFrameAvailableListener listener, AbstractGalleryActivity activity) {
        return new VideoThumbnailPlayer(listener, activity);
    }

    private VideoThumbnailPlayInfo getPlayInfoFromPath(String thumbnailPath) {
        synchronized (mCheckOutsLock) {
            if (mIsWorking) {
                for (VideoThumbnailPlayInfo vtPlayInfo : mCheckOuts) {
                    if (vtPlayInfo.thumnailPath.equals(thumbnailPath)) {
                        return vtPlayInfo;
                    }
                }
            }
        }
        return null;
    }

    public boolean isThumbnailPlaying(final String thumbnailPath) {
        return (getPlayInfoFromPath(thumbnailPath) != null);
    }

    public int getPlayingThumbnailCount() {
        synchronized (mCheckOutsLock) {
            return mCheckOuts.size();
        }
    }

    public void getPlayingThumbnails(List<String> thumbnailPaths) {
        thumbnailPaths.clear();
        synchronized (mCheckOutsLock) {
            for (VideoThumbnailPlayInfo playerInfo : mCheckOuts) {
                thumbnailPaths.add(playerInfo.thumnailPath);
            }
        }
    }

    public boolean renderThumbnail(final String thumbnailPath, GLCanvas canvas, int width,
            int height) {
        VideoThumbnailPlayInfo vtPlayInfo = getPlayInfoFromPath(thumbnailPath);
        if (vtPlayInfo == null) {
            return false;
        }
        try {
            if (!vtPlayInfo.renderTarget.isReadyForRender) {
                return false;
            }
            vtPlayInfo.renderTarget.draw(canvas, width, height);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return true;
    }

    // open a media indicated by its path
    // TODO: use isMediaPlaying() instead when usePlaying = true
    public/* synchronized */boolean openThumbnail(String thumbnailPath) {
        VideoThumbnailPlayInfo vtPlayInfo;

        synchronized (mPoolLock) {
            synchronized (mMapLock) {
                if (!mIsWorking) return true;
                if (mPool.size() > 0) {
                    vtPlayInfo = mPool.remove(0);
                } else {
                    if (mPlayerCount == MAX_PLAYER_COUNT) {
                        return false;
                    }
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "create new player in the pool");
                    }
                    vtPlayInfo = new VideoThumbnailPlayInfo();
                    mMap.put(vtPlayInfo.mediaPlayer, vtPlayInfo);
                    mPlayerCount++;
                }
            }
        }

        assert vtPlayInfo != null;
        vtPlayInfo.thumnailPath = thumbnailPath;
        final MediaPlayer player = vtPlayInfo.mediaPlayer;
        final VideoThumbnailPlayInfo finalPlayInfo = vtPlayInfo;
        try{
            player.reset();
            player.setLooping(true);

            try {
                player.setDataSource(thumbnailPath);
                player.prepare();
            } catch (Exception e) {
                onOpenException(vtPlayInfo, e);
                return true;
            }

            finalPlayInfo.renderTarget.isWorking = true;
        } catch (IllegalStateException e) {
            Log.v(TAG, "thumbnail is released by pausing, give up openning");
            return false;
        }

        // all done, and add to mCheckOuts
        synchronized (mCheckOutsLock) {
            if (mIsWorking) {
                player.start();
                mCheckOuts.add(vtPlayInfo);
            } else {
                releaseThumbnailPlay(finalPlayInfo);
            }
        }
        return true;
    }

    private void onOpenException(final VideoThumbnailPlayInfo vtPlayInfo,
            Exception e) {
        e.printStackTrace();
        vtPlayInfo.mediaPlayer.reset();
        vtPlayInfo.thumnailPath = null;
        synchronized (mPoolLock) {
            mPool.add(vtPlayInfo);
        }
        Log.e(TAG, e.getClass().getName()
                + "happens when openning video thumbnail");
    }

    private/* synchronized */boolean closeThumbnailByPlayer(MediaPlayer player) {
        VideoThumbnailPlayInfo vtPlayInfo;
        synchronized (mMapLock) {
            vtPlayInfo = mMap.get(player);
        }
        if (vtPlayInfo == null
                || getPlayInfoFromPath(vtPlayInfo.thumnailPath) == null) {
            return false;
        }
        synchronized (mCheckOutsLock) {
            mCheckOuts.remove(vtPlayInfo);
        }
        // mPlayerCount--;
        vtPlayInfo.recycle();
        synchronized (mPoolLock) {
             mPool.add(vtPlayInfo);
        }
        return true;
    }

    public/* synchronized */boolean closeThumbnail(String thumbnailPath) {
        VideoThumbnailPlayInfo vtPlayInfo = getPlayInfoFromPath(thumbnailPath);
        if (vtPlayInfo == null) {
            return false;
        }
        return closeThumbnailByPlayer(vtPlayInfo.mediaPlayer);
    }

    public void resume() {
        mIsWorking = true;
    }

    public/* synchronized */void pause() {
        synchronized (mCheckOutsLock) {
            mIsWorking = false;
            mCheckOuts.clear();
        }

        synchronized (mPoolLock) {
            mPool.clear();
        }

        synchronized (mMapLock) {
            for (VideoThumbnailPlayInfo vtPlayInfo : mMap.values()) {
                //playInfos.add(vtPlayInfo);
                releaseThumbnailPlay(vtPlayInfo);
            }
            mMap.clear();
            mPlayerCount = MAX_PLAYER_COUNT;
        }
        System.gc();
    }

    private void releaseThumbnailPlay(VideoThumbnailPlayInfo vtPlayInfo) {
        vtPlayInfo.recycle();
        vtPlayInfo.mediaPlayer.release();
        vtPlayInfo.surface.release();
        vtPlayInfo.renderTarget.releaseSurfaceTexture();
    }
}
