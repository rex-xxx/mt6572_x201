package com.mediatek.calendar.extension;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.text.format.Time;

import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.lunar.LunarUtil;

/**
 * M: Lunar Extension for MonthView
 * this extension would draw lunar text in the cell
 */
public class LunarMonthViewExt implements IMonthViewExt {

    private static final String TAG = "LunarMonthViewExt";

    private static final int PADDING_LUNAR_OFFSET = 2;
    private static int sLunarOffsetXFromMonthNumber = -10;
    private static int sLunarTextSize = 11;
    private static boolean sIsScaled = false;

    private final int mOrientation;
    private final Paint mLunarTextPaint;
    private final IMonthViewForExt mMonthWeekEventsView;
    private final float mScale;
    private final boolean mCanShowLunar;
    private final LunarUtil mLunarUtil;

    /**
     * M: init this extension, prepare some global properties
     * @param context the context of the view
     * @param view need the MonthWeekEventsView to fetch some information
     */
    public LunarMonthViewExt(Context context, IMonthViewForExt view) {
        mMonthWeekEventsView = view;
        mOrientation = context.getResources().getConfiguration().orientation;
        mScale = context.getResources().getDisplayMetrics().density;
        mLunarTextPaint = new Paint();
        mLunarUtil = LunarUtil.getInstance(context);
        mCanShowLunar = mLunarUtil.canShowLunarCalendar();
        initDimens();
    }

    /**
     * M: init the dimens, so that the lunar text size and location
     * would self-suit to different screen sizes
     */
    private void initDimens() {
        if (!sIsScaled) {
            sLunarTextSize *= mScale;
            sLunarOffsetXFromMonthNumber *= mScale;

            sIsScaled = true;
        }
    }

    @Override
    public void drawInCell(Canvas canvas, Paint monthNumPaint, int numX,
            int numY) {
        if (mCanShowLunar) {
            drawLunar(canvas, monthNumPaint, numX, numY);
        }
    }

    /** M: #Lunar# draw lunar in the box(x ,y) if needed.
     * Actually, this function only retrieve the Lunar string with specific dilemma(;)
     * 
     * @param canvas canvas to draw
     * @param monthNumPaint the paint of the month number
     * @param x the x of the number's right-bottom
     * @param y the y of the number's right-bottom
     */
    private void drawLunar(Canvas canvas, Paint monthNumPaint, int x, int y) {
        Time weekDayTime = mMonthWeekEventsView.getTimeFromLocation(x, y);
        if (weekDayTime == null) {
            LogUtil.e(TAG, "drawWeekNums(),getDayFromLocation("
                    + x + ") return null,for loop continue");
            return;
        }

        String lunarText = mLunarUtil.getLunarFestivalChineseString(
                weekDayTime.year, weekDayTime.month + 1, weekDayTime.monthDay);
        doDrawLunar(lunarText, canvas, monthNumPaint, x, y);
    }

    /** M: #Lunar# do the drawing of lunar string, with the consideration of current orientation.
     * if it's port, the dilemma separated String will be drawn in several lines.
     * if it's land, the string will be separated by a space
     * 
     * @param lunarText the text with dilemma
     * @param canvas canvas to draw
     * @param monthNumPaint the paint of the month number need it to init the lunar Paint
     * @param x the x of the number's right-bottom
     * @param y the y of the number's right-bottom
     */
    private void doDrawLunar(String lunarText, Canvas canvas, Paint monthNumPaint, int x, int y) {
        mLunarTextPaint.set(monthNumPaint);
        mLunarTextPaint.setTextSize(sLunarTextSize);

        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            mLunarTextPaint.setTextAlign(Align.CENTER);
            String[] words = lunarText.split(LunarUtil.DELIM);
            int wordX = x + sLunarOffsetXFromMonthNumber;
            int wordY = y + sLunarTextSize;
            for (String word : words) {
                canvas.drawText(word, wordX, wordY, mLunarTextPaint);
                wordY += (sLunarTextSize + PADDING_LUNAR_OFFSET);
            }
        } else {
            final String landDelim = " ";
            mLunarTextPaint.setTextAlign(Align.RIGHT);
            canvas.drawText(lunarText.replace(LunarUtil.DELIM, landDelim).trim(),
                    x + PADDING_LUNAR_OFFSET,
                    y + sLunarTextSize, mLunarTextPaint);
        }
    }

}
