package com.android.camera.manager;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.camera.Camera;
import com.android.camera.Log;
import com.android.camera.R;

import java.util.Locale;

public class RecordingView extends ViewManager implements OnClickListener {
    private static final String TAG = "RecordingView";
    private static final boolean LOG = Log.LOGV;
    
    private TextView mRecordingTime;
    private ImageView mPauseResume;
    private boolean mRecordinging;
    private String mTimeText;
    private OnClickListener mListener;
    
    private static final int PROGRESS_MAX = 100;
    
    private TextView mRecordingSizeTotal;
    private TextView mRecordingSizeCurrent;
    private SeekBar mRecrodingSizeProgress;
    private View mRecordingSizeGroup;
    private long mCurrent;
    private long mTotal;
    private int mProgress;
    private int mMax = PROGRESS_MAX;
    
    private boolean mRecordingSizeVisible;
    private boolean mTimeVisible;
    private boolean mPauseResumeVisible;
    
    public RecordingView(Camera context) {
        super(context);
    }

    @Override
    protected View getView() {
        View view = inflate(R.layout.recording);
        mRecordingTime = (TextView)view.findViewById(R.id.recording_time);
        mPauseResume = (ImageView)view.findViewById(R.id.btn_pause_resume);
        mPauseResume.setOnClickListener(this);
        
        mRecordingSizeGroup = view.findViewById(R.id.recording_size_group);
        mRecordingSizeCurrent = (TextView)view.findViewById(R.id.recording_current);
        mRecordingSizeTotal = (TextView)view.findViewById(R.id.recording_total);
        mRecrodingSizeProgress = (SeekBar)view.findViewById(R.id.recording_progress);
        if (mRecrodingSizeProgress != null) {
            mRecrodingSizeProgress.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motion) {
                    //disable seek
                    return true;
                }
            });
            mRecrodingSizeProgress.setMax(mMax);
            mRecrodingSizeProgress.setProgress(mProgress);
        }
        return view;
    }

    @Override
    protected void onRefresh() {
        if (LOG) {
            Log.v(TAG, "onRefresh() mCurrent=" + mCurrent + ", mTotal=" + mTotal
                    + ", mProgress=" + mProgress + ", mMax=" + mMax + ", mRecordinging=" + mRecordinging);
        }
        int recordingId = R.drawable.ic_recording_indicator_pause;
        int playpauseid = R.drawable.ic_recording_play;
        if (mRecordinging) {
            recordingId = R.drawable.ic_recording_indicator_play;
            playpauseid = R.drawable.ic_recording_pause;
        }
        mPauseResume.setImageResource(playpauseid);
        mRecordingTime.setCompoundDrawablesWithIntrinsicBounds(
                getContext().getResources().getDrawable(recordingId),
                null,
                null,
                null);
        mRecordingTime.setText(mTimeText);
        
        if (mRecordingSizeCurrent != null) {
            mRecordingSizeCurrent.setText(getFileSize(mCurrent));
        }
        if (mRecordingSizeTotal != null) {
            mRecordingSizeTotal.setText(getFileSize(mTotal));
        }
        if (mRecrodingSizeProgress != null) {
            mRecrodingSizeProgress.setMax(mMax);
            mRecrodingSizeProgress.setProgress(mProgress);
        }
        setTimeVisible(mTimeVisible);
        setPauseResumeVisible(mPauseResumeVisible);
        setRecordingSizeVisible(mRecordingSizeVisible);
    }
    
    @Override
    public void hide() {
        super.hide();
        setSizeProgress(0);//reset progress to 0
    }

    @Override
    public void onClick(View view) {
        if (mListener != null && mPauseResume == view) {
            mListener.onClick(mPauseResume);
        }
    }
    
    public void setListener(OnClickListener l) {
        mListener = l;
    }
    
    public void setRecordingIndicator(boolean recording) {
        if (LOG) {
            Log.v(TAG, "setRecordingIndicator(" + recording + ")");
        }
        mRecordinging = recording;
        refresh();
    }
    
    public boolean getRecording() {
        return mRecordinging;
    }
    
    public void showTime(long millis, boolean showMillis) {
        mTimeText = formatTime(millis, showMillis);
        if (mRecordingTime != null) {
            mRecordingTime.setText(mTimeText);
        }
    }
    
    private String formatTime(long millis, boolean showMillis) {
        final int totalSeconds = (int) millis / 1000;
        final int millionSeconds = (int) (millis % 1000) / 10;
        final int seconds = totalSeconds % 60;
        final int minutes = (totalSeconds / 60) % 60;
        final int hours = totalSeconds / 3600;
        String text = null;
        if (showMillis) {
            if (hours > 0) {
                text = String.format(Locale.ENGLISH, "%d:%02d:%02d.%02d", hours, minutes, seconds, millionSeconds);
            } else {
                text = String.format(Locale.ENGLISH, "%02d:%02d.%02d", minutes, seconds, millionSeconds);
            }
        } else {
            if (hours > 0) {
                text = String.format(Locale.ENGLISH, "%d:%02d:%02d", hours, minutes, seconds);
            } else {
                text = String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds);
            }
        }
        if (LOG) {
            Log.v(TAG, "formatTime(" + millis + ", " + showMillis + ") return " + text);
        }
        return text;
    }
    
    public void setPauseResumeVisible(boolean visible) {
        if (LOG) {
            Log.v(TAG, "setPauseResumeVisible(" + visible + ") mPauseResumeVisible=" + mPauseResumeVisible);
        }
        mPauseResumeVisible = visible;
        if (mPauseResume != null) {
            mPauseResume.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    public void setTimeVisible(boolean visible) {
        if (LOG) {
            Log.v(TAG, "setTimeVisible(" + visible + ") mTimeVisible=" + mTimeVisible);
        }
        mTimeVisible = visible;
        if (mRecordingTime != null) {
            mRecordingTime.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }
    
    public void setRecordingSizeVisible(boolean visible) {
        if (LOG) {
            Log.v(TAG, "setRecordingSizeVisible(" + visible + ") mRecordingSizeVisible=" + mRecordingSizeVisible);
        }
        mRecordingSizeVisible = visible;
        if (mRecordingSizeGroup != null) {
            mRecordingSizeGroup.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
    
    public void setCurrentSize(long size) {
        if (LOG) {
            Log.v(TAG, "setCurrentSize(" + size + ")");
        }
        mCurrent = size;
        if (mRecordingSizeCurrent != null) {
            mRecordingSizeCurrent.setText(getFileSize(mCurrent));
        }
    }
    
    public void setTotalSize(long size) {
        if (LOG) {
            Log.v(TAG, "setTotalSize(" + size + ")");
        }
        mTotal = size;
        if (mRecordingSizeTotal != null) {
            mRecordingSizeTotal.setText(getFileSize(mTotal));
        }
    }
    
    public void setMaxSize(int max) {
        if (LOG) {
            Log.v(TAG, "setMaxSize(" + max + ")");
        }
        mMax = max;
        if (mRecrodingSizeProgress != null) {
            mRecrodingSizeProgress.setMax(mMax);
        }
    }
    
    public void setSizeProgress(int progress) {
        if (LOG) {
            Log.v(TAG, "setSizeProgress(" + progress + ")");
        }
        mProgress = progress;
        if (mRecrodingSizeProgress != null) {
            mRecrodingSizeProgress.setProgress(mProgress);
        }
    }
    
    private String getFileSize(long size) {
        long kb = size / 1024;
        return kb + "K";//Formatter.formatFileSize(getContext(), size);
    }
}
