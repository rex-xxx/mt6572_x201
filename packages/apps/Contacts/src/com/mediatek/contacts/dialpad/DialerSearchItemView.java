package com.mediatek.contacts.dialpad;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.android.contacts.R;

public class DialerSearchItemView extends FrameLayout {

    private static final String TAG = "DialerSearchListItem";
    private static final boolean DBG = true;
    private static  int sDialerSearchItemViewHeight;

    protected QuickContactBadge mQuickContactBadge;
    protected TextView mName;
    protected TextView mLabelAndNumber;
    protected ImageView mCallType;
    protected TextView mOperator;
    protected TextView mDate;
    protected View mDivider;
    protected ImageButton mCall;

    // get from @dimen/calllog_list_item_quick_contact_padding_top
    private static int sListItemQuickContactPaddingTop;
    // get from @dimen/calllog_list_item_quick_contact_padding_bottom
    private static int sListItemQuickContactPaddingBottom;

    public DialerSearchItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // use the same padding value as call log list item
        sDialerSearchItemViewHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.dialer_search_item_view_initial_height);
        sListItemQuickContactPaddingTop = mContext.getResources().getDimensionPixelSize(
                R.dimen.calllog_list_item_quick_contact_padding_top);
        sListItemQuickContactPaddingBottom = mContext.getResources().getDimensionPixelSize(
                R.dimen.calllog_list_item_quick_contact_padding_bottom);
    }

    void log(String msg) {
        Log.d(TAG, msg);
    }

    protected void onFinishInflate() {
        mQuickContactBadge = (QuickContactBadge) findViewById(R.id.quick_contact_photo);
        mLabelAndNumber = (TextView) findViewById(R.id.labelAndNumber);
        mName = (TextView) findViewById(R.id.name);
        mCallType = (ImageView) findViewById(R.id.callType);
        mOperator = (TextView) findViewById(R.id.operator);
        mDate = (TextView) findViewById(R.id.date);
        mDivider = findViewById(R.id.divider);
        mCall = (ImageButton) findViewById(R.id.call);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (DBG) {
            log("changed = " + changed + " left = " + left + " top = " + top + " right = " + right
                    + " bottom = " + bottom);
        }
        final int parentLeft = mPaddingLeft;
        final int parentRight = right - left - mPaddingRight;

        final int parentTop = sListItemQuickContactPaddingTop;
        final int parentBottom = bottom - top - sListItemQuickContactPaddingBottom;
        if (DBG) {
            log("mPaddingTop = " + mPaddingTop + " mPaddingBottom = " + mPaddingBottom
                    + " mPaddingLeft = " + mPaddingLeft + " mPaddingRight = " + mPaddingRight);
        }
        if (DBG) {
            log("parentTop = " + parentTop + " parentBottom = " + parentBottom);
        }
        int width;
        int height;
        int childLeft = left;
        int childTop = top;

        int labelTop;

        int heightFirstLine = 0;
        int heightSecondLine = 0;
        int heightThirdLine = 0;
        int maxThreeLineHeight = 0;

        FrameLayout.LayoutParams lp;

        // QuickContactBadge
        // align left and center vertical
        width = mQuickContactBadge.getMeasuredWidth();
        height = mQuickContactBadge.getMeasuredHeight();
        childTop = parentTop + (parentBottom - parentTop - height) / 2;
        childLeft = parentLeft;
        mQuickContactBadge.layout(childLeft, childTop, childLeft + width, childTop + height);
        final int quickContactBadgeRight = childLeft + width;

        // Math.max(sDialerSearchItemViewHeight, height + mPaddingTop +
        // mPaddingBottom )
        sDialerSearchItemViewHeight = height + sListItemQuickContactPaddingTop
                + sListItemQuickContactPaddingBottom;

        // Call button
        // align right
        // CR:ALPS00245549 modify call button width to cover right start
        // width = mCall.getMeasuredWidth();
        width = mCall.getMeasuredWidth() + mPaddingRight;
        // CR:ALPS00245549 modify call button width to cover right end
        height = mCall.getMeasuredHeight();
        // childTop = parentTop - sListItemQuickContactPaddingTop;
        childTop = (sDialerSearchItemViewHeight - height) / 2;
        childLeft = parentRight - width;

        mCall.layout(childLeft, childTop, childLeft + width, childTop + height);

        // Divider
        // left of Call button and center vertical
        width = mDivider.getMeasuredWidth();
        height = mDivider.getMeasuredHeight();
        childTop = parentTop + (parentBottom - parentTop - height) / 2;
        childLeft = childLeft - width;
        mDivider.layout(childLeft, childTop, childLeft + width, childTop + height);

        // name
        // align top and right to mQuickContactBadge
        width = mName.getMeasuredWidth();
        height = mName.getMeasuredHeight();
        lp = (FrameLayout.LayoutParams) mName.getLayoutParams();
        childTop = parentTop;
        childLeft = quickContactBadgeRight + lp.leftMargin;
        mName.layout(childLeft, childTop, childLeft + width, childTop + height);
        final int nameBottom = childTop + height;
        heightFirstLine = height;
        maxThreeLineHeight = maxThreeLineHeight + heightFirstLine;

        final boolean callog = mCallType.getVisibility() == View.VISIBLE;

        // mCallType is visible, it's a call log item
        if (mCallType.getVisibility() == View.VISIBLE) {
            // Call type
            // align parent bottom and right to QuickContactBadge
            final int callTypeRight = onSubLayout(mCallType, quickContactBadgeRight, parentBottom);
            heightThirdLine = getLineMaxHeight(mCallType, heightThirdLine);

            // Operator ( sim indicator )
            // align parent bottom and right to Call type
            int tempTypeRight = callTypeRight;
            if (mOperator.getVisibility() == View.VISIBLE) {
                tempTypeRight = onSubLayout(mOperator, callTypeRight, parentBottom);
                heightThirdLine = getLineMaxHeight(mOperator, heightThirdLine);
            }

            // Date
            final int operatorRight = tempTypeRight;
            if (mDate.getVisibility() == View.VISIBLE) {
                final int tempRight = onSubLayout(mDate, operatorRight, parentBottom);
                heightThirdLine = getLineMaxHeight(mDate, heightThirdLine);
            }
            // get three lines Max-height including the third line
            maxThreeLineHeight = maxThreeLineHeight + heightThirdLine;
        }

        // label and number
        int labelRight = quickContactBadgeRight;
        if (mLabelAndNumber.getVisibility() == View.VISIBLE) {
            width = mLabelAndNumber.getMeasuredWidth();
            height = mLabelAndNumber.getMeasuredHeight();
            lp = (FrameLayout.LayoutParams) mLabelAndNumber.getLayoutParams();
            if (mCallType.getVisibility() == View.VISIBLE) {
                childTop = nameBottom;
            } else {
                childTop = parentBottom - height;
            }
            childLeft = quickContactBadgeRight + lp.leftMargin;
            labelRight = childLeft + width;
            mLabelAndNumber.layout(childLeft, childTop, childLeft + width, childTop + height);
            heightSecondLine = Math.max(heightSecondLine, height);
        }
        // get three lines Max-height including the second line
        maxThreeLineHeight = maxThreeLineHeight + heightSecondLine;
        // if 2nd line or 3rd line is null, 2 and 3 can't be both null
        /*
         * if(heightThirdLine == 0) { //2nd line null , add height same as 3rd
         * line maxThreeLineHeight = maxThreeLineHeight + heightSecondLine;
         * if(DBG) log("(heightThirdLine == 0)"); } else if(heightSecondLine ==
         * 0) { // 3rd line null,add 2nd line height maxThreeLineHeight =
         * maxThreeLineHeight + heightThirdLine; if(DBG)
         * log("(heightSecondLine == 0)"); }
         */
        sDialerSearchItemViewHeight = Math.max(maxThreeLineHeight, sDialerSearchItemViewHeight);
    }

    // @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width;
        int height;
        int measuredWidth = getSuggestedMinimumWidth();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(resolveSizeAndState(measuredWidth, widthMeasureSpec, 0),
                sDialerSearchItemViewHeight);
    }

    /**
     * M: layout sub-view of dialersearchitem
     *
     * @param view
     *            sub-view
     * @param subIconRight
     *            right edge val of last view
     * @param parentBottom
     *            parent view bottom val
     * @return
     */
    private int onSubLayout(View view, final int subIconRight, final int parentBottom) {
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) view.getLayoutParams();
        int childTop;
        if (view instanceof ImageView) {
            /** M: location's offset is 3 when calltype icon */
            childTop = parentBottom - height - 3;
        } else {
            childTop = parentBottom - height;
        }
        int childLeft = subIconRight + lp.leftMargin;
        view.layout(childLeft, childTop, childLeft + width, childTop + height);
        return childLeft + width;
    }

    /**
     * M: get MaxHeight of sub view
     *
     * @param view
     *            sub-view
     * @param heightThirdLine
     *            maxheight of last view
     * @return
     */
    private int getLineMaxHeight(View view, int heightThirdLine) {
        return Math.max(heightThirdLine, view.getMeasuredHeight());
    }
}
