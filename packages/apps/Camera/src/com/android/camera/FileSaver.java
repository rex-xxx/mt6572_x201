package com.android.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;

import com.android.camera.Util.ImageFileNamer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FileSaver extends Thread { //push thumbnail to thumbnail manager
    private static final String TAG = "FileSaver";
    private static final boolean LOG = Log.LOGV;
    
    private static final int QUEUE_LIMIT = 100;
    
    private static final String TEMP_SUFFIX = ".tmp";

    private Camera mContext;
    private ContentResolver mResolver;
    private List<FileSaverListener> mSaverListener = new CopyOnWriteArrayList<FileSaverListener>();
    private ArrayList<SaveRequest> mQueue = new ArrayList<SaveRequest>();
    private HashMap<Integer, ImageFileNamer> mFileNamer;
    private int mQueueLimit = QUEUE_LIMIT;
    private boolean mStoped;
    
    public interface FileSaverListener {
        void onFileSaved(SaveRequest request);
    }
    
    public FileSaver(Camera context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        start();
    }
    
    // Runs in saver thread
    @Override
    public void run() {
        while (true) {
            SaveRequest r;
            synchronized (this) {
                if (mQueue.isEmpty()) {
                    notifyAll();  // notify main thread in waitDone

                    // Note that we can only stop after we saved all images
                    // in the queue.
                    if (mStoped) { break; }

                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        // ignore.
                        Log.w(TAG, "save thread is interrupted.", ex);
                    }
                    continue;
                }
                r = mQueue.get(0);
            }
            //Don't save request if sdcard not ready.
            if (Storage.isStorageReady()) {
                r.saveRequest();
            }
            synchronized (this) {
                mQueue.remove(0);
                notifyAll();  // the main thread may wait in addImage
            }
            r.notifyListener();
            for (FileSaverListener listener : mSaverListener) {
                listener.onFileSaved(r);
            }
            r = null;
        }
    }
    
    // Runs in main thread
    public void waitDone() {
        synchronized (this) {
            while (!mQueue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore.
                    Log.e(TAG, "waitDone()", ex);
                }
            }
        }
    }

    // Runs in main thread
    public void finish() {
        waitDone();
        synchronized (this) {
            mStoped = true;
            notifyAll();
        }
        try {
            join();
        } catch (InterruptedException ex) {
            // ignore.
            Log.e(TAG, "finish()", ex);
        }
    }
    
    // Runs in main thread
    public void finishAfterSaved() {
        if (LOG) {
            Log.v(TAG, "finishAfterSaved()");
        }
        synchronized (this) {
            mStoped = true;
            notifyAll();
        }
    }

    public long getWaitingDataSize() {
        long totalToWrite = 0;
        synchronized (this) {
            for (SaveRequest r : mQueue) {
                totalToWrite += r.getDataSize();
            }
        }
        return totalToWrite;
    }
    
    public int getWaitingCount() {
        int count = 0;
        synchronized (this) {
            count = mQueue.size();
        }
        return count;
    }
    
    public boolean addListener(FileSaverListener l) {
        if (!mSaverListener.contains(l)) {
            return mSaverListener.add(l);
        }
        return false;
    }
    
    public boolean removeListener(FileSaverListener l) {
        return mSaverListener.remove(l);
    }
    
    private void addSaveRequest(SaveRequest request) {
        synchronized (this) {
            while (mQueue.size() >= mQueueLimit) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore.
                    Log.e(TAG, "addSaveRequest(" + request + ")", ex);
                }
            }
            mQueue.add(request);
            notifyAll(); // Tell saver thread there is new work to do.
        }
    }
    
    //record type, location and datetaken
    public SaveRequest preparePhotoRequest(int type, int pictureType) {
        SaveRequest request = null;
        if (type == Storage.FILE_TYPE_PANO) {
            request = new PanoOperator(pictureType);
        } else {
            request = new PhotoOperator();
        }
        request.prepareRequest();
        return request;
    }
    // copy JpegRotation,location ,record type and datetaken
    public SaveRequest copyPhotoRequest(SaveRequest originRequest) {
        SaveRequest request = null;
        if (originRequest instanceof PhotoOperator) {
            request = ((PhotoOperator) originRequest).copyRequest();
        }
        return request;
    }

    //resolution
    public SaveRequest prepareVideoRequest(int outputFileFormat, String resolution) {
        //we should prepare file path for recording, so here we fill lots of info.
        VideoOperator operator = new VideoOperator(outputFileFormat, resolution);
        operator.prepareRequest();
        return operator;
    }
    private abstract class RequestOperator implements SaveRequest {
        int mFileType;
        byte[] mData;//for raw data
        Uri mUri;
        String mTitle;
        Location mLocation;
        int mWidth;
        int mHeight;
        int mOrientation;
        String mDisplayName;
        String mFilePath;//for saved file
        String mMimeType;
        long mDateTaken;
        int mMpoType;//for mpo
        int mStereoType;//for stereo
        String mResolution;//for video resolution
        long mDataSize;
        long mDuration;
        int mTempPictureType;
        int mTempOutputFileFormat;//for video file format
        String mTempFilePath;//for saved file
        int mTempJpegRotation;
        boolean mIgnoreThumbnail;
        FileSaverListener mListener;
        
        @Override
        public void setIgnoreThumbnail(boolean ignore) {
            mIgnoreThumbnail = ignore;
        }
        @Override
        public boolean isIgnoreThumbnail() {
            return mIgnoreThumbnail;
        }
        @Override
        public String getTempFilePath() {
            return mTempFilePath;
        }
        @Override
        public String getFilePath() {
            return mFilePath;
        }
        @Override
        public int getDataSize() {
            if (mData == null) {
                return 0;
            } else {
                return mData.length;
            }
        }
        @Override
        public void setData(byte[] data) {
            mData = data;
        }
        @Override
        public void setDuration(long duration) {
            mDuration = duration;
        }
        @Override
        public Uri getUri() {
            return mUri;
        }
        @Override
        public void setJpegRotation(int jpegRotation) {
            if (LOG) {
                Log.v(TAG, "setJpegRotation(" + jpegRotation + ")");
            }
            mTempJpegRotation = jpegRotation;
        }
        @Override
        public int getJpegRotation() {
            if (LOG) {
                Log.v(TAG, "getJpegRotation mTempJpegRotation=" + mTempJpegRotation);
            }
            return mTempJpegRotation;
        }
        @Override
        public Location getLocation() {
            return mLocation;
        }
        @Override
        public void setLocation(Location loc) {
            mLocation = loc;
        }
        @Override
        public void setTempPath(String path) {
            mTempFilePath = path;
        }
        @Override
        public void setListener(FileSaverListener listener) {
            mListener = listener;
        }
        @Override
        public void notifyListener() {
            if (mListener != null) {
                mListener.onFileSaved(this);
            }
        }
        @Override
        public void updateDataTaken(long time) {
            mDateTaken = time;
        } 
        public void saveImageToDatabase(RequestOperator r) {
            // Insert into MediaStore.
            ContentValues values = new ContentValues(14);
            values.put(ImageColumns.TITLE, r.mTitle);
            values.put(ImageColumns.DISPLAY_NAME, r.mDisplayName);
            values.put(ImageColumns.DATE_TAKEN, r.mDateTaken);
            values.put(ImageColumns.MIME_TYPE, r.mMimeType);
            values.put(ImageColumns.DATA, r.mFilePath);
            values.put(ImageColumns.SIZE, r.mDataSize);  
            values.put(ImageColumns.STEREO_TYPE, r.mStereoType);//should be rechecked
            if (r.mLocation != null) {
                values.put(ImageColumns.LATITUDE, r.mLocation.getLatitude());
                values.put(ImageColumns.LONGITUDE, r.mLocation.getLongitude());
            }
            values.put(ImageColumns.ORIENTATION, r.mOrientation);
            values.put(ImageColumns.WIDTH, r.mWidth);
            values.put(ImageColumns.HEIGHT,r.mHeight); 
            values.put(ImageColumns.MPO_TYPE, r.mMpoType);
            try {
                r.mUri = mResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values);
                if (mContext.isNonePickIntent()) {
                	// picture taken and saved by camera which is launched by 3rd apps will 
                	// be inserted into DB. But do not broadcast "New_Picture" intent, 
                	// otherwise, it will not pass camera CTS test.
                	Util.broadcastNewPicture(mContext, r.mUri);
                }
            } catch (Throwable th)  { //Here we keep google default, don't follow check style
                // This can happen when the external volume is already mounted, but
                // MediaScanner has not notify MediaProvider to add that volume.
                // The picture is still safe and MediaScanner will find it and
                // insert it into MediaProvider. The only problem is that the user
                // cannot click the thumbnail to review the picture.
                Log.e(TAG, "Failed to write MediaStore", th);
            }
        }
        @Override
        public void saveSync() {
            if (mData == null) {
                Log.w(TAG, "saveSync() why mData==null???", new Throwable());
                return;
            }
            FileOutputStream out = null;
            try {
                // Write to a temporary file and rename it to the final name. This
                // avoids other apps reading incomplete data.
                out = new FileOutputStream(mTempFilePath);
                out.write(mData);
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to write image", e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.e(TAG, "saveRequest()", e);
                    }
                }
            }
        }
        
        @Override
        public String toString() {
            return new StringBuilder()
            .append("RequestOperator(mUri=")
            .append(mUri)
            .append(", mTempFilePath=")
            .append(mTempFilePath)
            .append(", mFilePath=")
            .append(mFilePath)
            .append(", mIgnoreThumbnail=")
            .append(mIgnoreThumbnail)
            .append(")")
            .toString();
        }
    }
    private class PhotoOperator extends RequestOperator {
        private PhotoOperator() {
            mTempPictureType = Storage.PICTURE_TYPE_JPG;
        }
        @Override
        public void prepareRequest() {
            mFileType = Storage.FILE_TYPE_PHOTO;
            mDateTaken = System.currentTimeMillis();
            Location loc = mContext.getLocationManager().getCurrentLocation();
            if (loc != null) {
                mLocation = new Location(loc);
            }
        }
        @Override
        public void addRequest() {
            if (mData == null) {
                Log.w(TAG, "addRequest() why mData==null???", new Throwable());
                return;
            }
            //In google default camera, it set picture size when capture and onPictureTaken
            //Here we just set picture size in onPictureTaken.
            Size s = mContext.getParameters().getPictureSize();
            mWidth = s.width;
            mHeight = s.height;
            addSaveRequest(this);
        }

        public PhotoOperator copyRequest() {
            PhotoOperator newRequest = new PhotoOperator();
            newRequest.mFileType = Storage.FILE_TYPE_PHOTO;
            newRequest.mDateTaken = System.currentTimeMillis();
            newRequest.mLocation = this.mLocation;
            newRequest.mTempJpegRotation = this.mTempJpegRotation;
            return newRequest;
        }
        @Override
        public void saveRequest() {
            int orientation = Exif.getOrientation(mData);
            int width;
            int height;
            if ((mTempJpegRotation + orientation) % 180 == 0) {
                width = mWidth;
                height = mHeight;
            } else {
                width = mHeight;
                height = mWidth;
            }
            mWidth = width;
            mHeight = height;
            mOrientation = orientation;
            mDataSize = mData.length;
            mTitle = createName(mFileType, mDateTaken);
            mDisplayName = Storage.generateFileName(mTitle, mTempPictureType);
            mFilePath = Storage.generateFilepath(mDisplayName);
            mTempFilePath = mFilePath + TEMP_SUFFIX;
            FileOutputStream out = null;
            try {
                // Write to a temporary file and rename it to the final name. This
                // avoids other apps reading incomplete data.
                out = new FileOutputStream(mTempFilePath);
                out.write(mData);
                out.close();
                new File(mTempFilePath).renameTo(new File(mFilePath));
            } catch (IOException e) {
                Log.e(TAG, "Failed to write image", e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.e(TAG, "saveRequest()", e);
                    }
                }
            }
            mMimeType = Storage.generateMimetype(mTitle, mTempPictureType);
            mMpoType = Storage.generateMpoType(mTempPictureType);
            saveImageToDatabase(this);
            if (LOG) {
                Log.v(TAG, "saveRequest() mTempJpegRotation=" + mTempJpegRotation
                        + ", mOrientation=" + mOrientation);
            }
        }
        
        @Override
        public Thumbnail createThumbnail(int thumbnailWidth) {
            Thumbnail thumb = null;
            if (mUri != null) {
                // Create a thumbnail whose width is equal or bigger than
                // that of the thumbnail view.
                int ratio = (int) Math.ceil((double) mWidth / thumbnailWidth);
                int inSampleSize = Integer.highestOneBit(ratio);
                thumb = Thumbnail.createThumbnail(mData, mOrientation, inSampleSize, mUri);
            }
            if (LOG) {
                Log.v(TAG, "createThumbnail(" + thumbnailWidth + ") mOrientation=" + mOrientation
                        + ", mUri=" + mUri);
            }
            return thumb;
        }
    }
    
    private class PanoOperator extends RequestOperator {
        private PanoOperator(int pictureType) {
            mTempPictureType = pictureType;
        }
        @Override
        public void prepareRequest() {
            mFileType = Storage.FILE_TYPE_PANO;
            mDateTaken = System.currentTimeMillis();
            Location loc = mContext.getLocationManager().getCurrentLocation();
            if (loc != null) {
                mLocation = new Location(loc);
            }
            mTitle = createName(mFileType, mDateTaken);
            mDisplayName = Storage.generateFileName(mTitle, mTempPictureType);
            mFilePath = Storage.generateFilepath(mDisplayName);
            mTempFilePath = mFilePath + TEMP_SUFFIX;
        }
        @Override
        public void addRequest() {
            addSaveRequest(this);
        }
        @Override
        public void saveRequest() {
            //title, file path, temp file path is ready
            File temp = new File(mTempFilePath);
            if (temp.length() <= 0) {
                temp.delete();
                setIgnoreThumbnail(true);
                Log.w(TAG, "Bad file created by native layer, delete it! " + this);
                return;
            }
            temp.renameTo(new File(mFilePath));
            mDataSize = new File(mFilePath).length();
            try {
                ExifInterface exif = new ExifInterface(mFilePath);
                int orientation = Util.getExifOrientation(exif);
                int width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
                int height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
                mWidth = width;
                mHeight = height;
                mOrientation = orientation;
            } catch (IOException ex) {
                Log.e(TAG, "cannot read exif", ex);
            }
            mMimeType = Storage.generateMimetype(mTitle, mTempPictureType);
            mMpoType = Storage.generateMpoType(mTempPictureType);
            
            saveImageToDatabase(this);
        }
        @Override
        public Thumbnail createThumbnail(int thumbnailWidth) {
            Thumbnail thumb = null;
            if (mUri != null) {
                // Create a thumbnail whose width is equal or bigger than
                // that of the thumbnail view.
                int widthRatio = (int) Math.ceil((double) mWidth / mContext.getPreviewFrameWidth());
                int heightRatio = (int) Math.ceil((double) mWidth / mContext.getPreviewFrameHeight());
                int inSampleSize = Integer.highestOneBit(Math.max(widthRatio, heightRatio));
                thumb = Thumbnail.createThumbnail(mFilePath, mOrientation, inSampleSize, mUri, mStereoType);
            }
            if (LOG) {
                Log.v(TAG, "createThumbnail(" + thumbnailWidth + ") mOrientation=" + mOrientation
                        + ", mUri=" + mUri + ", mFilePath=" + mFilePath + ", return " + thumb);
            }
            return thumb;
        }
    }
    
    private class VideoOperator extends RequestOperator {
        private VideoOperator(int outputFileFormat, String resolution) {
            mTempOutputFileFormat = outputFileFormat;
            mResolution = resolution;
        }
        @Override
        public void prepareRequest() {
            mFileType = Storage.FILE_TYPE_VIDEO;
            mDateTaken = System.currentTimeMillis();
            mTitle = createName(mFileType, mDateTaken);
            mDisplayName = mTitle + convertOutputFormatToFileExt(mTempOutputFileFormat);
            mMimeType = convertOutputFormatToMimeType(mTempOutputFileFormat);
            mFilePath = Storage.generateFilepath(mDisplayName);
        }
        @Override
        public void addRequest() {
            addSaveRequest(this);
        }
        @Override
        public void saveRequest() {
            //need video compute duration
            try {
                File temp = new File(mTempFilePath);
                File file = new File(mFilePath);
                temp.renameTo(file);
                mDataSize = file.length();
                
                ContentValues values = new ContentValues(11);
                values.put(Video.Media.TITLE, mTitle);
                values.put(Video.Media.DISPLAY_NAME, mDisplayName);
                values.put(Video.Media.DATE_TAKEN, mDateTaken);
                values.put(Video.Media.MIME_TYPE, mMimeType);
                values.put(Video.Media.DATA, mFilePath);
                values.put(Video.Media.SIZE, mDataSize);
                values.put(Video.Media.STEREO_TYPE, mStereoType);
                if (mLocation != null) {
                    values.put(Video.Media.LATITUDE, mLocation.getLatitude());
                    values.put(Video.Media.LONGITUDE, mLocation.getLongitude());
                }
                values.put(Video.Media.RESOLUTION, mResolution);
                values.put(Video.Media.DURATION, mDuration);
                mUri = mResolver.insert(Video.Media.EXTERNAL_CONTENT_URI, values);
                mContext.sendBroadcast(new Intent(android.hardware.Camera.ACTION_NEW_VIDEO, mUri));
            } catch (Throwable th)  { //Here we keep google default, don't follow check style
                // This can happen when the external volume is already mounted, but
                // MediaScanner has not notify MediaProvider to add that volume.
                // The picture is still safe and MediaScanner will find it and
                // insert it into MediaProvider. The only problem is that the user
                // cannot click the thumbnail to review the picture.
                Log.e(TAG, "Failed to write MediaStore", th);
            }
        }
        @Override
        public Thumbnail createThumbnail(int thumbnailWidth) {
            Thumbnail thumb = null;
            if (mUri != null) {
                Bitmap videoFrame = Thumbnail.createVideoThumbnailBitmap(mFilePath, thumbnailWidth);
                if (videoFrame != null) {
                    thumb = Thumbnail.createThumbnail(mUri, videoFrame, 0);
                }
            }
            return thumb;
        }
    }
    
    private String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return ".mp4";
        }
        return ".3gp";
    }
    
    private String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            return "video/mp4";
        }
        return "video/3gpp";
    }
    
    private String createName(int fileType, long dateTaken) {
        if (mFileNamer == null) {
            mFileNamer = new HashMap<Integer, ImageFileNamer>();
            ImageFileNamer photo = new ImageFileNamer(
                    mContext.getString(R.string.image_file_name_format));
            mFileNamer.put(Storage.FILE_TYPE_PHOTO, photo);
            //pano_file_name_format changed to image format for UX design.
            mFileNamer.put(Storage.FILE_TYPE_PANO, photo);
            mFileNamer.put(Storage.FILE_TYPE_VIDEO, new ImageFileNamer(
                    mContext.getString(R.string.video_file_name_format)));
        }
        Date date = new Date(dateTaken);
        String name = mFileNamer.get(fileType).generateName(dateTaken);
        if (LOG) {
            Log.v(TAG, "createName(" + fileType + ", " + dateTaken + ")");
        }
        return name;
    }
}
