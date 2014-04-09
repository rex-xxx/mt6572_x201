package com.mediatek.contacts.dialpad;

import android.content.Context;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;
import android.widget.EditText;

public class AutoScaleTextSizeWatcher implements TextWatcher {

    private static final String TAG = "AutoScaleTextSizeWatcher";

    protected Context mContext;
    protected EditText mTarget;

    protected int mMaxTextSize;
    protected int mMinTextSize;
    protected int mDeltaTextSize;
    protected int mCurrentTextSize;
    protected int mDigitsWidth;

    protected DisplayMetrics mDisplayMetrics;

    protected Paint mPaint;

    protected int mPreviousDigitsLength;

    public AutoScaleTextSizeWatcher(EditText editText) {
        mContext = editText.getContext();
        mTarget = editText;
        mPaint = new Paint();
        mPaint.set(editText.getPaint());
        mTarget.addTextChangedListener(this);

        mDisplayMetrics = new DisplayMetrics();
        final WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(mDisplayMetrics);
    }

    public void setAutoScaleParameters(int minTextSize, int maxTextSize, int deltaTextSize,
            int digitsWidth) {
        mMaxTextSize = maxTextSize;
        mMinTextSize = minTextSize;
        mDeltaTextSize = deltaTextSize;
        mDigitsWidth = digitsWidth;

        log("digitsWidth = " + digitsWidth);

        mCurrentTextSize = mMaxTextSize;
        mTarget.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCurrentTextSize);
    }

    public void trigger(boolean delete) {
        autoScaleTextSize(true);
    }

    public void afterTextChanged(Editable s) {
        autoScaleTextSize(false);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //
    }

    void log(String msg) {
        Log.d(TAG, msg);
    }

    protected void autoScaleTextSize(boolean delete) {
        final String digits = mTarget.getText().toString();
        final DisplayMetrics dm = mDisplayMetrics;

        if (digits.length() == 0) {
            // digits length is empty, reset to max text size
            mCurrentTextSize = mMaxTextSize;
            mTarget.setTextSize(TypedValue.COMPLEX_UNIT_PX, mMaxTextSize);
            return;
        }

        final int max = mMaxTextSize;
        final int min = mMinTextSize;
        final int digitsWidth = mDigitsWidth;
        final int delta = mDeltaTextSize;
        final Paint paint = mPaint;

        int inputWidth;
        int current = mCurrentTextSize;
        int precurrent = current;
        paint.setTextSize(current);
        inputWidth = (int) paint.measureText(digits);

        log("inputWidth = " + inputWidth + " current = " + current + " digits = " + digits);

        if (!delete) {
            while ((current > min) && inputWidth > digitsWidth) {
                current -= delta;
                if (current < min) {
                    current = min;
                    break;
                }
                paint.setTextSize(current);
                inputWidth = (int) paint.measureText(digits);
            }
        } else {
            while ((inputWidth < digitsWidth) && (current < max)) {
                precurrent = current;
                current += delta;
                if (current > max) {
                    current = max;
                    break;
                }
                // log("5current += delta; inputWidth = "+inputWidth+" current = "+current);
                paint.setTextSize(current);
                inputWidth = (int) paint.measureText(digits);
                // log("gogogocurrent += delta; inputWidth = "+inputWidth+" current = "+current);

            }
            // Cr 235203
            // roll back to precurrent if larger font cause overflow width
            paint.setTextSize(current);
            inputWidth = (int) paint.measureText(digits);
            // log("eeeeecurrent += delta; inputWidth = "+inputWidth+" current = "+current);
            if (inputWidth > digitsWidth) {
                current = precurrent;
                // log("if (inputWidth > digitsWidth) 3scurrent += delta; inputWidth = "+inputWidth+" current = "+current);
            }
        }

        mCurrentTextSize = current;
        mTarget.setTextSize(TypedValue.COMPLEX_UNIT_PX, current);

        // inputWidth = (int)paint.measureText(digits);
        // log("endinputWidth = "+inputWidth+" current = "+current);
    }
}
