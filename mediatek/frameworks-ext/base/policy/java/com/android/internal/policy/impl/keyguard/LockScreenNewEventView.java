package com.android.internal.policy.impl.keyguard;

import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.Gravity;

import com.mediatek.internal.R;
import com.mediatek.xlog.Xlog;

public class LockScreenNewEventView extends RelativeLayout {
	private static final String TAG = "NewEventView";
	private static final boolean DEBUG = true;

    private static final int MAX_COUNT = 99;
	private static final String MAX_COUNT_STRING = "99+";
    private Context mContext;
    private int mResourceId;
    private int mCount = 0;
    boolean mAttachedToWindow = false;
    
    private ViewGroup mTopParentView;
    
	private String mNumberText;
	
	private ImageView mUnReadImageView;
	private TextView mUnReadTextView;
	
	private UnReadObserver mEventChangeObserver;
    
    Runnable mSetNumberRunnable = new Runnable() {
         @Override
         public void run() {
            setNumberImp(mCount);
         }
     };

    public LockScreenNewEventView(Context context) {
        this(context, null);
    }
    
    public LockScreenNewEventView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LockScreenNewEventView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setDrawingCacheEnabled(true);
    }
    
    public void onFinishInflate() {
        super.onFinishInflate();
        mUnReadTextView = (TextView)findViewById(R.id.event_app_unread);
        mUnReadImageView = (ImageView)findViewById(R.id.event_app_icon);
    }

    
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
    }
    
    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mSetNumberRunnable);
        mAttachedToWindow = false;
    }
  

    public void init(int drawableId){
        Xlog.i(TAG, "init");
        mResourceId = drawableId;
        Drawable drawable = mContext.getResources().getDrawable(drawableId);
        mUnReadImageView.setImageDrawable(drawable);
        setPivotY(getMeasuredHeight() * 0.5f);
        setPivotX(getMeasuredWidth() * 0.5f);
    }
    
    public int getResourceId() {
        return mResourceId;
    }
    
    public void setNumber(int count) {
        if(mAttachedToWindow == true) {
            mCount = count;
            this.post(mSetNumberRunnable);
        }
    }
    
    private final void setNumberImp(int count) {
    	if (DEBUG) Xlog.d(TAG, "setNumber count=" + count);
        
        //check this is valid number or not
		if (count <= 0) {
		    setViewVisibility(View.INVISIBLE);
            return;
		}
        
    	setViewVisibility(View.VISIBLE);
    	// If new number is bigger than 99 and current numberText is 99+, then just return 
    	if (count > MAX_COUNT) {
    	    mNumberText = MAX_COUNT_STRING;
    	} else {
    	    mNumberText = Integer.toString(count);
    	}
    	mUnReadTextView.setText(mNumberText);
    }
    
    public void setTopParent(ViewGroup parentView) {
        mTopParentView = parentView;
    }
    
    public void setViewVisibility(int visibility) {
        mTopParentView.setVisibility(visibility);
    }
    
    public ImageView getNewEventImageView() {
        return mUnReadImageView;
    }
    
    public int getNewEventBitmapWidth() {
        if (mUnReadImageView != null && mUnReadImageView.getDrawable() != null) {
            return mUnReadImageView.getDrawable().getIntrinsicWidth();
        } else {
            return 0;
        }
    }
    
    public int getNewEventBitmapHeight() {
        if (mUnReadImageView != null && mUnReadImageView.getDrawable() != null) {
            return mUnReadImageView.getDrawable().getIntrinsicHeight();
        } else {
            return 0;
        }
    }
    
    // Return the center of mUnReadImageView
    public int getFakeCenterX() {
        return mUnReadImageView.getWidth()/2;
    }
    
    // Return the center of mUnReadImageView
    public int getFakeCenterY() {
        return mUnReadImageView.getHeight()/2;
    }
    
    public void registerForQueryObserver(Uri uri, UnReadObserver unReadObserver) {
        if (unReadObserver != null) {
            mEventChangeObserver = unReadObserver;
            getContext().getContentResolver().registerContentObserver(
                    uri, true, mEventChangeObserver);
        }
    }
    
    public void unRegisterNewEventObserver() {
        if (mEventChangeObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(mEventChangeObserver);
        }
    }
    
    public void updateQueryBaseTime(long newBaseTime) {
        mEventChangeObserver.updateQueryBaseTime(newBaseTime);
    }
}
