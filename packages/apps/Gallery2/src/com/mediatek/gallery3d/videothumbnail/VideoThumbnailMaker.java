package com.mediatek.gallery3d.videothumbnail;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.LocalVideo;
import com.mediatek.transcode.VideoTranscode;

import android.graphics.Rect;
import android.os.StatFs;
import android.util.Log;

public final class VideoThumbnailMaker {
    private static final String TAG = "Gallery2/VideoThumbnailMaker";
    private static final int TRANSCODE_CANCEL = 10001;
    private static final int MIN_STORAGE_SPACE = 3 * 1024 * 1024;
    private static final int TRANSCODING_BIT_RATE = 256 * 1024;
    private static final int TRANSCODING_FRAME_RATE = 10;
    private static AtomicLong sCurrentHandle = new AtomicLong(-1);

    private VideoThumbnailMaker() {
        // do nothing
    }

    private static Rect getTargetRect(int srcWidth, int srcHeight, int maxWidth, int maxHeight) {
        if ((srcWidth <= maxWidth) || (srcHeight <= maxHeight)) {
            return new Rect(0, 0, srcWidth, srcHeight);
        }

        float rSrc = (float) srcWidth / srcHeight;
        float rMax = (float) maxWidth / maxHeight;

        int targetWidth;
        int targetHeight;

        // crop and scale
        if (rSrc < rMax) {
            targetWidth = maxWidth;
            targetHeight = targetWidth * srcHeight / srcWidth;
        } else {
            targetHeight = maxHeight;
            targetWidth = targetHeight * srcWidth / srcHeight;
            // width must be the factor of 16, find closest but smallest factor
            if (targetWidth % 16 != 0) {
                targetWidth = (targetWidth - 15) >> 4 << 4;
                targetHeight = targetWidth * srcHeight / srcWidth;
            }
        }

        return new Rect(0, 0, targetWidth, targetHeight);
    }

    private static final int ENCODE_WIDTH = 320;
    private static final int ENCODE_HEIGHT = 240;
    private static final String DYNAMIC_CACHE_FILE_POSTFIX = ".dthumb";
    private static final int MAX_THUMBNAIL_DURATION = 8 * 1000; // 12 seconds at present

    // out file name looks like videoxxx.dthumb
    // TODO: For VideoTranscode may sometimes leave with half-generated files, 
    // to avoid playing such broken files, when the transcoding is in process,
    // use a different file name such as videoxxx.dthumb.temp is a better way
    private static String getDynamicThumbnailPath(String inputName) {
        StringBuilder res = null;
        int i = inputName.lastIndexOf("/");
        if (i == -1) {
            res = new StringBuilder(".dthumb/").append(inputName.substring(i+1).hashCode()).append(DYNAMIC_CACHE_FILE_POSTFIX);
        } else {
            // i-1 can be -1. risk?
            res = new StringBuilder(inputName.substring(0, i+1)).append(".dthumb/").append(inputName.substring(i+1).hashCode())
                .append(DYNAMIC_CACHE_FILE_POSTFIX);
        }
        return res.toString();
    }

    private static int doTranscode(LocalVideo video) {
        Rect srcRect = new Rect(0, 0, video.width, video.height);
        Rect targetRect = getTargetRect(srcRect.width(), srcRect.height(), ENCODE_WIDTH,
                                        ENCODE_HEIGHT);
        Log.v(TAG, "srcRect: " + srcRect + " targetRect: " + targetRect);
        // duration is not so accurate as gotten from meta retriever,
        // but it's already enough (however, I don't know why the googlers don't save the
        // accurate duration with ms to be the unit)
        long duration = video.durationInSec * 1000;
        long startTime = duration / 3;  // eh, magic number?
        long endTime = Math.min(duration, startTime + MAX_THUMBNAIL_DURATION);
        startTime = Math.max(0, endTime - MAX_THUMBNAIL_DURATION);

        if (Thread.currentThread().isInterrupted()) {
            return TRANSCODE_CANCEL;
        }

        String dfilepath = video.filePath;
        String tpath = video.dynamicThumbnailPath;

        String dirPath = dfilepath.substring(0, dfilepath.lastIndexOf('/'));
        if (!isStorageSafeForTranscoding(dirPath)) {
            Log.e(TAG, "storage available in this volume is not enough! stop transcoding");
            return VideoTranscode.ERROR_TRANSCODE_FAIL;
        }

        dirPath = tpath.substring(0, tpath.lastIndexOf('/'));
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdir()) {
            Log.e(TAG, "exception when creating cache container!"); 
            return VideoTranscode.ERROR_TRANSCODE_FAIL;
        }

        if (Thread.currentThread().isInterrupted()) {
            return TRANSCODE_CANCEL;
        }

        long width = (long) targetRect.width();
        long height = (long) targetRect.height();
        
        Log.v(TAG, "start transcoding: " + dfilepath + " to " + tpath + ", target width = " + width + ", target height = " + height);
        Log.v(TAG, "starttime = " + startTime + ", endtime = " + endTime);
        sCurrentHandle.set(VideoTranscode.init());
        int result = VideoTranscode.transcodeAdv(sCurrentHandle.get(), video.filePath, video.dynamicThumbnailPath + ".tmp", (long) targetRect.width(),
                                            (long) targetRect.height(), startTime, endTime, TRANSCODING_BIT_RATE, TRANSCODING_FRAME_RATE);
        Log.v(TAG, "end transcoding: " + dfilepath + " to " + tpath);
        VideoTranscode.deinit(sCurrentHandle.get());
        sCurrentHandle.set(-1);

        if (Thread.currentThread().isInterrupted()) {
            return TRANSCODE_CANCEL;
        }

        return result;
    }

    public static boolean isStorageSafeForTranscoding(String dirPath) {
        try {
            StatFs stat  = new StatFs(dirPath);
            long spaceLeft = (long)(stat.getAvailableBlocks())*stat.getBlockSize();
            Log.v(TAG, "storage available in this volume is: " + spaceLeft);
            if (spaceLeft < MIN_STORAGE_SPACE) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return false;
        }
        return true;
    }

    private static synchronized void handleVideo(LocalVideo video) {
        int result = doTranscode(video);

        Log.e(TAG, "transcode result: " + result);

        if (result == TRANSCODE_CANCEL) {
            video.thumbNailState = LocalVideo.THUMBNAIL_STATE_STATIC;
            return;
        }

        File unCompleteDynThumb = new File(video.dynamicThumbnailPath + ".tmp");
        boolean recrifiedResult = false;
        if (result == VideoTranscode.NO_ERROR) {
            if (unCompleteDynThumb.exists()) {
                recrifiedResult = unCompleteDynThumb.renameTo(new File(video.dynamicThumbnailPath));
            }
        }

        Log.e(TAG, "recrified transcode result: " + result);
        if (recrifiedResult) {
            video.thumbNailState = LocalVideo.THUMBNAIL_STATE_DYNAMIC_GENERATED_SUCCESS;
            sDirector.pumpLiveThumbnails();
            sGalleryActivity.getGLRoot().requestRender();
            Log.e(TAG, "then request render: " + video.dynamicThumbnailPath);
        } else {
            if (unCompleteDynThumb.exists()) {
                unCompleteDynThumb.delete();
            }
            video.thumbNailState = LocalVideo.THUMBNAIL_STATE_DYNAMIC_GENERATED_FAIL;
        }
    }

    // returns true if really need to generate dynamic thumbnail
    // and video.dynamicThumbnailPath contains the path of the thumbnail
    private static boolean needGenDynThumb(LocalVideo video) {
        String inputPath = video.filePath;

        if ((video.isDrm()) || (inputPath == null) || (video.width == 0)
                || (video.height == 0)) {
            video.thumbNailState = LocalVideo.THUMBNAIL_STATE_DYNAMIC_GENERATED_FAIL;
            video.dynamicThumbnailPath = null;
            return false;
        }

        Rect srcRect = new Rect(0, 0, video.width, video.height);

        String outputPath;
        // better use cache file to indicate if the dyn thumb has been generated
        outputPath = getDynamicThumbnailPath(inputPath);
        File dynThumbFile = new File(outputPath);
        if (dynThumbFile.exists()) {
            video.thumbNailState = LocalVideo.THUMBNAIL_STATE_DYNAMIC_GENERATED_SUCCESS;
            video.dynamicThumbnailPath = outputPath;
            return false;
        } else {
            video.thumbNailState = LocalVideo.THUMBNAIL_STATE_DYNAMIC_GENERATING;
            video.dynamicThumbnailPath = outputPath;
            return true;
        }
    }
    
    private static class VideoHandler extends Thread {
        // TODO: use blocking stack is more friendly, to be modified
        private final BlockingQueue<LocalVideo> mVideoQueue;
        private LocalVideo mCurrentVideo;

        public VideoHandler(String threadName) {
            super("DynamicThumbnailRequestHandler-" + threadName);
            mVideoQueue = new LinkedBlockingQueue<LocalVideo>();
        }

        public void run() {
            try {
                LocalVideo currentVideo;
                while(!Thread.currentThread().isInterrupted()) {
                    currentVideo = mVideoQueue.take();                    
                    synchronized (this) {
                        mCurrentVideo = currentVideo;
                    }
                    Log.v(TAG, "handle transcoding request for " + mCurrentVideo.filePath);
                    handleVideo(mCurrentVideo);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Terminating " + getName());
                this.interrupt();
            }
        }

        private void submit(LocalVideo video) {
            if (isAlive()) {
                if (needGenDynThumb(video)) {
                    Log.v(TAG, "submit transcoding request for " + video.filePath);
                    mVideoQueue.add(video);
                }
            } else {
                Log.e(TAG, getName() + " should be started before submitting tasks.");
            }
        }

        private boolean cancelPendingTranscode(LocalVideo video) {
            video.thumbNailState = LocalVideo.THUMBNAIL_STATE_STATIC;
            // video.dynamicThumbnailPath can be cached
            return mVideoQueue.remove(video);
        }
        
        private void cancelCurrentTranscode() {
            if (sCurrentHandle.get() != -1) {
                VideoTranscode.cancel(sCurrentHandle.get());
            }
            LocalVideo currentVideo;
            synchronized (this) {
                currentVideo = mCurrentVideo;
            }
            if (currentVideo != null && currentVideo.dynamicThumbnailPath != null) {
                File unCompleteDynThumb = new File(currentVideo.dynamicThumbnailPath + ".tmp");
                if (unCompleteDynThumb.exists()) {
                    unCompleteDynThumb.delete();
                }
            }
        }

        private void cancelPendingTranscode() {
            // sHandler.interrupt();
            for (LocalVideo video : mVideoQueue) {
                video.thumbNailState = LocalVideo.THUMBNAIL_STATE_STATIC;
                // video.dynamicThumbnailPath can be cached
            }
            mVideoQueue.clear();
            // cancelCurrentTranscode();
        }

        private void cancelAllTranscode() {
            cancelPendingTranscode();
            cancelCurrentTranscode();
            sHandler.interrupt();
        }
    }
    
    private static volatile VideoHandler sHandler = null;

    public static void requestThumbnail(LocalVideo video) {
        if (sHandler != null) {
            sHandler.submit(video);
        }
    }
    
    private static AbstractGalleryActivity sGalleryActivity = null;
    public static void setGalleryActivity(AbstractGalleryActivity galleryActivity) {
        sGalleryActivity = galleryActivity;
    }
    private static VideoThumbnailDirector sDirector = null;
    public static void setDirector(VideoThumbnailDirector director) {
        sDirector = director;
    }
    
    public static void pause() {
        if (VideoThumbnailFeatureOption.OPTION_TRANSCODE_BEFORE_PLAY) {
            if (sHandler != null) {
                sHandler.cancelAllTranscode();
                sHandler = null;
            }
        }
    }

    public static void cancelPendingTranscode() {
        if (sHandler != null) {
            sHandler.cancelPendingTranscode();
        }
    }

    public static void start() {
        if (VideoThumbnailFeatureOption.OPTION_TRANSCODE_BEFORE_PLAY) {
            if (sHandler == null) {
                sHandler = new VideoHandler("transcode proxy");
                sHandler.start();
            }
        }
    }
}
