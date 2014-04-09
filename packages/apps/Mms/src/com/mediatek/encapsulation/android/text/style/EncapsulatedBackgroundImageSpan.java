package com.mediatek.encapsulation.android.text.style;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.text.ParcelableSpan;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ReplacementSpan;

import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.text.style.BackgroundImageSpan;

public class EncapsulatedBackgroundImageSpan extends ReplacementSpan implements ParcelableSpan {
    private Drawable mDrawable;
    private int mImageId;
    private int mWidth = -1;

    /** M: MTK ADD */
    private BackgroundImageSpan mBackgroundImageSpan;

    /** M: MTK ADD
     * new BackgroundImageSpan use resource id and Drawable
     * @param id the drawable resource id
     * @param drawable Drawable related to the id
     */
    public EncapsulatedBackgroundImageSpan(int id, Drawable drawable) {
        mBackgroundImageSpan = new BackgroundImageSpan(id, drawable);
        mImageId = id;
        mDrawable = drawable;
    }

    public EncapsulatedBackgroundImageSpan(Parcel src) {
        mBackgroundImageSpan = new BackgroundImageSpan(src);
        mImageId = src.readInt();
    }

    public void draw(Canvas canvas, int width,float x,int top, int y, int bottom, Paint paint) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mBackgroundImageSpan.draw(canvas, width, x, top, y, bottom, paint);
        } else {
            if (mDrawable == null) {//if no backgroundImage just don't do any draw
                throw new IllegalStateException("should call convertToDrawable() first");
            }
            Drawable drawable = mDrawable;
            canvas.save();

            canvas.translate(x, top); // translate to the left top point
            mDrawable.setBounds(0, 0, width, (bottom - top));
            drawable.draw(canvas);

            canvas.restore();
        }
    }

    public void updateDrawState(TextPaint tp) {
    }

    public int getSpanTypeId() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mBackgroundImageSpan.getSpanTypeId();
        } else {
            return 0;
        }
    }

    public int describeContents() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mBackgroundImageSpan.describeContents();
        } else {
            return 0;
        }
    }

    public void writeToParcel(Parcel dest, int flags) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mBackgroundImageSpan.writeToParcel(dest, flags);
        } else {
            dest.writeInt(mImageId);
        }
    }

    public void convertToDrawable(Context context) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mBackgroundImageSpan.convertToDrawable(context);
        } else {
            if (mDrawable == null) {
                mDrawable = context.getResources().getDrawable(mImageId);
            }
        }
    }

    /**
     * M: convert a style text that contain BackgroundImageSpan, Parcek only pass resource id,
     * after Parcel, we need to convert resource id to Drawable.
     */
    public static void convert(CharSequence text , Context context) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            BackgroundImageSpan.convert(text, context);
        } else {
            if (!(text instanceof SpannableStringBuilder)) {
                return;
            }

            SpannableStringBuilder builder = (SpannableStringBuilder)text;

            BackgroundImageSpan[] spans = builder.getSpans(0, text.length(), BackgroundImageSpan.class);
            if (spans == null || spans.length == 0) {
                return;
            }

            for (int i = 0; i < spans.length; i++) {
                spans[i].convertToDrawable(context);
            }
        }
    }

    public void draw(Canvas canvas, CharSequence text, int start, int end,
            float x, int top, int y, int bottom, Paint paint) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            mBackgroundImageSpan.draw(canvas, text, start, end, x, top, y, bottom, paint);
        } else {
            // draw image
            draw(canvas, mWidth,x,top, y, bottom, paint);
            // draw text
            // the paint is already updated
            canvas.drawText(text,start,end, x,y, paint);
        }
    }

    public int getSize(Paint paint, CharSequence text, int start, int end,
            FontMetricsInt fm) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mBackgroundImageSpan.getSize(paint, text, start, end, fm);
        } else {
            float size = paint.measureText(text, start, end);
            if (fm != null && paint != null) {
                paint.getFontMetricsInt(fm);
            }
            mWidth = (int)size;
            return mWidth;
        }
    }
}
