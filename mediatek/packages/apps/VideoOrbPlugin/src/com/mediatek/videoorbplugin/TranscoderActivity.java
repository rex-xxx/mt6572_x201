package com.mediatek.videoorbplugin;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.mediatek.videoorbplugin.transcode.ProgressDialogFragment;
import com.mediatek.transcode.VideoTranscode;
import com.mediatek.videoorbplugin.transcode.PromptDialogFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TranscoderActivity extends Activity
        implements ProgressDialogFragment.DialogActionListener {

    public static final String TAG = "vo.transcoder";
    public static final int UPDATE_INTERVAL = 200;
    public static final String TRANSCODE_PATH_BASE = Environment.getExternalStorageDirectory().toString();
    public static final String TRANSCODE_PATH = TRANSCODE_PATH_BASE + "/.voplugin/";
    public static final String READ_VER_PATH = TRANSCODE_PATH + ".ver";

    public static final String TRANSCODE_TEMP_PATH = TRANSCODE_PATH_BASE + "/.voplugin_tmp/";
    public static final String TRANSCODE_TEMP2_PATH = TRANSCODE_PATH_BASE + "/.voplugin_tmp2/";
    public static final String WRITE_VER_PATH = TRANSCODE_TEMP_PATH + ".ver";

    public static final int MSG_UPDATE_PROGRESS = 1;
    public static final int MSG_CANCEL = 2;
    public static final int MSG_SUCCESS = 3;
    public static final int MSG_FAILURE = 4;

    private TranscoderTask mTranscoderTask;
    private DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
            switch (whichButton) {
                case DialogInterface.BUTTON_POSITIVE:
                    mProgressDlg = ProgressDialogFragment.newInstance(
                            R.string.transcoding_prompt);
                    mProgressDlg.show(getFragmentManager(), "progressDialog");

                    if (TranscodedMediaSource.hasTranscoded(getContentResolver(), getVer())) {
                        mHandler.sendEmptyMessage(MSG_SUCCESS);
                    } else {
                        removeFolders();
                        final int[] measurement = LayoutManager.getIntArray(getResources(), R.array.video_transcode_target, false);
                        Log.v(TAG, "Transcode target width : " + measurement[0] + ", height : " + measurement[1]);
                        mTranscoderTask = new TranscoderTask(measurement[0], measurement[1]);
                        mTranscoderTask.execute(getNeedTranscodeVideoUris());
                    }
                break;
                case DialogInterface.BUTTON_NEGATIVE:
                    Log.d(TAG, "Transcoding is cancelled");
                    setResult(RESULT_CANCELED);
                    finish();
                break;
            }
        }
    };

    private ShutdownReceiver mReceiver;
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutManager.setDisplayMetrics(this);
        PromptDialogFragment prompt = PromptDialogFragment.newInstance(
                R.string.transcoding_prompt,
                R.string.transcoding_prompt_description,
                mClickListener);
        prompt.show(getFragmentManager(), "alertDialog");
    }

    private volatile boolean mIsFinished;
    private boolean isFinished() {
        return mIsFinished;
    }

    private void setResultAndFinish(int result) {
        mIsFinished = true;
        setResult(result);
        finish();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SUCCESS:
                    Log.v(TAG, "Transcoder succeeded");
                    dismissProgressDlg(100);
                    mHandler.removeMessages(MSG_UPDATE_PROGRESS);
                    setResultAndFinish(RESULT_OK);
                    break;
                case MSG_UPDATE_PROGRESS:
                    if (mProgressDlg != null) {
                        int progress = mTranscoderTask.getProgress();
                        mProgressDlg.setProgress(mTranscoderTask.getProgress());
                        if (progress < 100) {
                            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, UPDATE_INTERVAL);
                        }
                    }
                    break;
                case MSG_CANCEL:
                case MSG_FAILURE:
                    Log.d(TAG, "Transcoder was cancelled or failed : " + msg.what);
                    removeFolders();
                    mHandler.removeMessages(MSG_UPDATE_PROGRESS);
                    setResultAndFinish(RESULT_CANCELED);
                    break;
                default:
                    break;
            }
        }
    };

    public int getVer() {
        File f = new File(READ_VER_PATH);
        int ver = 0;
        if (f.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                String buffer = reader.readLine();
                if (buffer != null) {
                    ver = Integer.valueOf(buffer);
                    Log.v(TAG, "Read current ver : " + ver);
                }
            } catch (IOException e) {
                Log.v(TAG, "Failed to read ver : " + e);
            }
        }
        Log.v(TAG, "Read final ver : " + ver);
        return ver;
    }

    public boolean writeVer(String path, String newVer) {
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(newVer);
            writer.close();
            Log.v(TAG, "Write path : " + path + ", ver : " + newVer);
            return true;
        } catch (IOException e) {
            Log.v(TAG, "Can't write ver : " + e);
        }
        return false;
    }

    public ProgressDialogFragment mProgressDlg;
    private void dismissProgressDlg(int progress) {
        if (mProgressDlg == null) {
            return;
        }

        if (progress != -1) {
            mProgressDlg.setProgress(progress);
        }

        mProgressDlg.dismiss();
        mProgressDlg = null;
    }

    private void cancelTranscode() {
        if(isFinished()) {
            return;
        }

        dismissProgressDlg(-1);
        if (mTranscoderTask != null) {
            mTranscoderTask.cancelTranscode();
        }
        mHandler.removeMessages(MSG_UPDATE_PROGRESS);
        mHandler.sendEmptyMessageDelayed(MSG_CANCEL, 100);
    }

    /**
     * Implementation of ProgressDialogFragment.DialogActionListener
     * Called from ProgressDialogFragment.
     */
    public void onCancel() {
        cancelTranscode();
    }

    class TranscoderTask extends AsyncTask<ArrayList<Uri>, Integer, ArrayList<String>> {
        private static final int RESULT_OK = 0;
        private static final int RESULT_RESOULTION_TOO_HIGH = -1;
        private static final int RESULT_INVALID_VIDEO = -2;
        private long mTranscodeHandle;
        private int mProgress;
        private int mResult;
        private ArrayList<Uri> mSrc;
        private volatile int current;
        private volatile boolean mIsCancelled;
        private int mTargetWidth;
        private int mTargetHeight;
        private ArrayList<Uri> mNotTranscoded = new ArrayList<Uri>();
        private ArrayList<Uri> mTranscodedSrc = new ArrayList<Uri>();
        private ArrayList<String> mTranscodedDst = new ArrayList<String>();

        public TranscoderTask(int targetWidth, int targetHeight) {
            mTargetWidth = targetWidth;
            mTargetHeight = targetHeight;
        }

        protected void onPreExecute() {
            mTranscodeHandle = VideoTranscode.init();
            Log.v(TAG, "Before transcoding handle : " + mTranscodeHandle);
            mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, UPDATE_INTERVAL);
            mResult = RESULT_OK;
        }

        protected ArrayList<String> doInBackground(ArrayList<Uri>... uris) {
            mSrc = uris[0];
            ArrayList<String> output = new ArrayList<String>();
            int count = mSrc.size();
            prepareFolder(TRANSCODE_TEMP_PATH); // always generate it first.

            for(int i = 0; i < count; ++i) {
                if (mNotTranscoded.size() + output.size() == 8) {
                    mResult = RESULT_OK;
                    break;
                }

                current = i;
                Uri uri = mSrc.get(i);
                int option = getTranscodeOption(uri);
                if (option == INVALID_CLIP) {
                    Log.v(TAG, "Processing source : " + uri + " invalid clip.");
                    continue;
                }

                if (option == NO_NEED_TRANSCODE) {
                    mNotTranscoded.add(uri);
                    continue;
                }

                long dateTaken = System.currentTimeMillis();
                String source = getPathFromUri(getContentResolver(), uri);
                String destination = generateTempOutputPath(source, dateTaken);
                Log.v(TAG, "Transcoding source : " + source + ", destination : " + destination);
                int result = VideoTranscode.transcode(
                        mTranscodeHandle, source, destination, mTargetWidth, mTargetHeight, 0l, 10l * 1000);
                if (result != VideoTranscode.NO_ERROR) {
                    Log.d(TAG, "Transcoding failed : " + source + ", reason : " + result);
                    mResult = (result == VideoTranscode.ERROR_UNSUPPORTED_VIDEO) ?
                            RESULT_RESOULTION_TOO_HIGH : RESULT_INVALID_VIDEO;
                } else {
                    String finalDestination = generateOutputPath(source, dateTaken);
                    Log.v(TAG, "Transcoding succeed , source : " + source + ", final destination : " + finalDestination);
                    output.add(finalDestination);
                    mTranscodedSrc.add(uri);
                    mTranscodedDst.add(destination);
                    mResult = RESULT_OK;
                }

                if (mIsCancelled) {
                    mResult = RESULT_CANCELED;
                    break;
                }
            }
            return output;
        }

        protected void onPostExecute(ArrayList<String> destination) {
            if (mTranscodeHandle != 0) {
                VideoTranscode.deinit(mTranscodeHandle);
            }
            Log.v(TAG, "Tmp size : " + mTranscodedDst.size() + ", final size : " + destination.size());
            int prevVer = getVer();
            int ver = (prevVer+1);
            for (int i = 0 ; i < mTranscodedDst.size() && (!mIsCancelled); ++i) {
                ContentValues v = new ContentValues(7);
                long fileLength = new File(mTranscodedDst.get(i)).length();
                if (fileLength == 0) {
                    // TODO : Error handling
                    return;
                }

                v.put(MediaStore.Video.Media.DATE_TAKEN, getSrcDateTaken(mTranscodedSrc.get(i)));
                v.put(MediaStore.Video.Media.TAGS, TranscodedMediaSource.TRANSCODED_TAG_ID);
                v.put(MediaStore.Video.Media.DISPLAY_NAME, destination.get(i));
                v.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                v.put(MediaStore.Video.Media.SIZE, fileLength);
                v.put(MediaStore.Video.Media.DATA, destination.get(i));
                v.put(MediaStore.Video.Media.CATEGORY, ver);
                Log.v(TAG, "onPostExecute : i " + i + "Name : " + destination.get(i) + ", Category : " + ver);

                Uri videoTable = Uri.parse("content://media/external/video/media");
                Uri inserted = getContentResolver().insert(videoTable, v);
                if (inserted == null) {
                    Log.w(TAG, "Media data insert failed : " + videoTable);
                } else {
                    Log.v(TAG, "Media data insertion successfully : " + videoTable);
                }
            }

            if (mNotTranscoded != null && mNotTranscoded.size() > 0) {
                for (Uri uri : mNotTranscoded) {
                    if (mIsCancelled) {
                        break;
                    }
                    ContentValues v = new ContentValues(2);
                    v.put(MediaStore.Video.Media.TAGS, TranscodedMediaSource.INCLUDED_TAG_ID);
                    v.put(MediaStore.Video.Media.CATEGORY, ver);
                    String source = getPathFromUri(getContentResolver(), uri);
                    Log.w(TAG, "Media data path : " + source + ", ver : " + ver);
                    int updateRow = getContentResolver().update(uri, v, null, null);
                    if (updateRow == 0) {
                        Log.w(TAG, "Media data update failed : " + uri);
                    } else {
                        Log.v(TAG, "Media data update successfully : " + uri);
                    }
                }

                mNotTranscoded.clear();
                mNotTranscoded = null;
            }

            mHandler.sendEmptyMessageDelayed(MSG_SUCCESS, 200);
            if (!mIsCancelled && writeVer(WRITE_VER_PATH, String.valueOf(ver))) {
                if (mTranscodedDst.size() > 0) {
                    swapFolders();
                } else {
                    clearTranscoded();
                }
            }
        }

        public void cancelTranscode() {
            mIsCancelled = true;
            if (mTranscodeHandle != 0) {
                VideoTranscode.cancel(mTranscodeHandle);
            }
        }

        public int getProgress() {
                if (mTranscodeHandle == 0 || mSrc == null)
                return 0;

            int total = mSrc.size();
            int currentProgress = VideoTranscode.getProgress(mTranscodeHandle);
            if (currentProgress == 100) {
                currentProgress = 0;
            }
            int progress = (int)(((float)current * 100 / total) + ((float)currentProgress / total));
            if (mProgress < progress)
                mProgress = progress;
            return mProgress;
        }

        private final String[] PROJECTION =
                new String [] { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_TAKEN};

        private long getSrcDateTaken(Uri uri) {
            Cursor c = ExternalMediaSource.query(getContentResolver(), uri, PROJECTION);
            String dateTaken = "0";
            if (c.moveToFirst()) {
                dateTaken = c.getString(1); // DateTaken
                Log.v(TAG, "source dateTaken : " + dateTaken);
            }
            c.close();
            return Long.valueOf(dateTaken);
        }
    }

    private final static int INVALID_CLIP = 0;
    private final static int NEED_TRANSCODE = 1;
    private final static int NO_NEED_TRANSCODE = 2;

    public int getTranscodeOption(Uri uri) {
        String strWidth = null;
        String strHeight = null;
        Bitmap thumbnail = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, uri);
            String hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
            if (hasVideo == null) {
                Log.w(TAG, "getSourceVideoRect, no videoTrack");
                return INVALID_CLIP;
            }
            strWidth = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            strHeight = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            thumbnail = retriever.getFrameAtTime();
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
        } finally {
            retriever.release();
        }

        if (strWidth == null || strHeight == null) {
            Log.w(TAG, "invalid video width/height : " + uri);
            return INVALID_CLIP;
        }

        if (thumbnail == null) {
            Log.w(TAG, "invalid thumbnail : " + uri);
            return INVALID_CLIP;
        }

        int width = Integer.decode(strWidth).intValue();
        int height = Integer.decode(strHeight).intValue();
        if (width == 0 || height == 0) {
            Log.w(TAG, "video width/height is 0 : " + uri);
            return INVALID_CLIP;
        }

        return (width > CRITERIA_WIDTH || height > CRITERIA_HEIGHT) ? NEED_TRANSCODE : NO_NEED_TRANSCODE;
    }

    static final private int CRITERIA_WIDTH = 320;
    static final private int CRITERIA_HEIGHT = 240;

    ArrayList<Uri> getNeedTranscodeVideoUris() {
        ArrayList<Uri> src = new ArrayList<Uri>();
        IMediaSource source = MediaSourceFactory.getTranscodeSource(getContentResolver());
        int counts = source.getMediaCount();
        for (int i = 0; i < counts; ++i) {
            Uri videoUri = source.getMediaUri(this, i);
            Log.v(TAG, "Traverse : " + videoUri);
            src.add(videoUri);
        }
        return src;
    }

    /**
     * File system operations
     */
    public static String getPathFromUri(ContentResolver cr, Uri uri) {
        String [] proj = {MediaStore.Video.Media.DATA};
        Cursor c = cr.query(uri, proj, null, null, null);
        int colIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        c.moveToFirst();
        String realPath = c.getString(colIdx);
        c.close();
        return realPath;
    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat("_yyyyMMdd_HHmmss", Locale.US);
        return dateFormat.format(date);
    }

    private void prepareFolder(String path) {
        File f = new File(path);
        if (f.exists()) {
            return;
        }

        if (!f.mkdirs()) {
            Log.w(TAG, "folder creation failed!");
        }
    }

    // has complete all
    private void swapFolders() {
        File prevFolder = new File(TRANSCODE_PATH);
        File renameFolder = new File(TRANSCODE_TEMP2_PATH);

        if (!prevFolder.exists()) {
            File tempFolder = new File(TRANSCODE_TEMP_PATH);
            File finalFolder = new File(TRANSCODE_PATH);
            if (tempFolder.renameTo(finalFolder)) {
                // now, the version was placed in correct position.
                clearCached();
            } else {
                Log.w(TAG, "Temp folder can't be renamed to " + TRANSCODE_PATH);
            }
        } else if (prevFolder.renameTo(renameFolder)) {
            File tempFolder = new File(TRANSCODE_TEMP_PATH);
            File finalFolder = new File(TRANSCODE_PATH);
            if (tempFolder.renameTo(finalFolder)) {
                // now, the version was placed in correct position.
                clearCached();
            } else {
                Log.w(TAG, "Temp folder can't be renamed to " + TRANSCODE_PATH);
            }
        } else {
            Log.w(TAG, "Previous folder can't be renamed to " + TRANSCODE_TEMP2_PATH);
        }
    }

    private void removeFolders() {
        removeFolder(TRANSCODE_TEMP_PATH);
        removeFolder(TRANSCODE_TEMP2_PATH);
    }

    private void clearCached() {
        removeFolders();
        TranscodedMediaSource.removeNonCurVersionData(getContentResolver(), getVer());
    }

    private void clearTranscoded() {
        Log.v(TAG, "clearTranscoded : " + TRANSCODE_PATH);
        removeFolder(TRANSCODE_PATH);
        removeFolder(TRANSCODE_TEMP_PATH);
        TranscodedMediaSource.removeNonCurVersionData(getContentResolver(), getVer());
    }

    private void removeFolder(String path) {
        File folder = new File(path);
        if (!folder.exists()) {
            Log.i(TAG, "folder doesn't exist! : " + path);
            return;
        }

        File [] files = folder.listFiles();
        for (File file : files) {
            Log.w(TAG, "Remove file : " + file.getPath());
            if (!file.delete()) {
                Log.w(TAG, "file deletion failed! : " + file);
            }
        }

        if (!folder.delete()) {
            Log.w(TAG, "folder deletion failed! : " + folder);
        }
    }

    private String generateOutputPath(String source, long dateTaken) {
        //long dateTaken = System.currentTimeMillis();
        String postfix = createName(dateTaken);
        File inputFile = new File(source);

        prepareFolder(TRANSCODE_PATH);
        StringBuilder sb = new StringBuilder(TRANSCODE_PATH);
        sb.append(inputFile.getName());
        int i = sb.lastIndexOf(".");
        if (i == -1) {
            sb.append(postfix);
        } else {
            sb.insert(i, postfix);
        }
        return sb.toString();
    }

    private String generateTempOutputPath(String source, long dateTaken) {
        //long dateTaken = System.currentTimeMillis();
        String postfix = createName(dateTaken);
        File inputFile = new File(source);

        StringBuilder sb = new StringBuilder(TRANSCODE_TEMP_PATH);
        sb.append(inputFile.getName());
        int i = sb.lastIndexOf(".");
        if (i == -1) {
            sb.append(postfix);
        } else {
            sb.insert(i, postfix);
        }
        return sb.toString();
    }

    public void onResume() {
        super.onResume();
        mReceiver = new ShutdownReceiver(this);
        registerReceiver(mReceiver, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"));
        Log.v(TAG, "onResume is called.");
    }

    public void onPause() {
        super.onPause();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        cancelTranscode();
        Log.v(TAG, "onPause is called.");
    }

    public class ShutdownReceiver extends BroadcastReceiver {
        TranscoderActivity mParent;
        public ShutdownReceiver(TranscoderActivity activity) {
            mParent = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mParent == null)
                return;
            if (intent.getAction().equals("android.intent.action.ACTION_SHUTDOWN")) {
                mParent.cancelTranscode();
                Log.v(TAG, "ShutdownReceiver.onReceive is called.");
            }
        }
    }
}