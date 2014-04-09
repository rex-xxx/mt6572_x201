package com.mediatek.calendar.extension;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * M: Interface to extend Calendar month view
 */
public interface IMonthViewExt {

    /**
     * M: Draw additional content in the Month view cell, such as lunar text
     * @param canvas canvas to draw
     * @param monthNumPaint the paint of number in this cell, the extension should be
     * drawn in the similar style(bold, font, size, color)
     * @param numX the number's right-bottom x
     * @param numY the number's right-bottom y
     */
    void drawInCell(Canvas canvas, Paint monthNumPaint, int numX, int numY);
}
