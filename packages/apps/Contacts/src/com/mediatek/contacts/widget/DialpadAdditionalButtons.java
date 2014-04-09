package com.mediatek.contacts.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.android.contacts.R;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;

public class DialpadAdditionalButtons extends FrameLayout {

    private static final String TAG = "DialpadAdditionalButtons";

    private int mButtonWidth;
    private int mButtonHeight;
    private int mDividerHeight;
    private int mDividerWidth;
    private ImageButton mHoloButton;
    private ImageButton mDialButton;
    private ImageButton mMenuButton;
    private View mDivider1;
    private View mDivider2;
    private Drawable mDividerVertical;

    private boolean mLayouted = false;

    public DialpadAdditionalButtons(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources r = getResources();
        mButtonWidth = r.getDimensionPixelSize(R.dimen.dialpad_additional_button_width);
        mButtonHeight = r.getDimensionPixelSize(R.dimen.dialpad_additional_button_height);
        mDividerHeight = r.getDimensionPixelSize(R.dimen.dialpad_divider_height);
        mDividerWidth = r.getDimensionPixelSize(R.dimen.dialpad_divider_width);
    }

    @Override
    protected void onFinishInflate() {
        // TODO Auto-generated method stub
        super.onFinishInflate();

        init();
    }

    protected void init() {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.selectableItemBackground });
        Drawable itemBackground = typedArray.getDrawable(0);

        typedArray = getContext().getTheme().obtainStyledAttributes(new int[] { android.R.attr.dividerVertical });
        mDividerVertical = typedArray.getDrawable(0);

        mHoloButton = new ImageButton(getContext());
        mHoloButton.setImageResource(R.drawable.ic_dialpad_holo_dark);
        mHoloButton.setBackgroundDrawable(itemBackground);
        mHoloButton.setId(R.id.dialpadButton);
        addView(mHoloButton);

        mDivider1 = new View(getContext());
        mDivider1.setBackgroundDrawable(mDividerVertical);
        addView(mDivider1);

        mDialButton = new ImageButton(getContext());
        mDialButton.setImageResource(R.drawable.ic_dial_action_call);
        mDialButton.setBackgroundResource(R.drawable.btn_call);
        mDialButton.setId(R.id.dialButton);
        addView(mDialButton);

        mDivider2 = new View(getContext());
        mDivider2.setBackgroundDrawable(mDividerVertical);
        addView(mDivider2);

        mMenuButton = new ImageButton(getContext());
        mMenuButton.setBackgroundDrawable(itemBackground.getConstantState().newDrawable());
        int id = R.id.overflow_menu;
        int resId = R.drawable.ic_menu_overflow;
        if (ViewConfiguration.get(getContext()).hasPermanentMenuKey()) {
            if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                id = R.id.videoDialButton;
                resId = R.drawable.ic_dial_action_video_call;
            } else {
                id = R.id.addToContact;
                resId = R.drawable.ic_add_contact_holo_dark;
            }
        }
        mMenuButton.setId(id);
        mMenuButton.setImageResource(resId);
        addView(mMenuButton);
        setButtonVisibility();
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mLayouted) {
            return;
        }

        mLayouted = true;

        View child = getChildAt(0);
        child.layout(0, 0, mButtonWidth, mButtonHeight);

        int dividerTop = (mButtonHeight - mDividerHeight) >> 1;
        child = getChildAt(1);
        int child1LeftPosition = calculatePositionByOrientation(mButtonWidth,mDividerHeight);
        int child1RightPosition = calculatePositionByOrientation(mButtonWidth + mDividerWidth, mDividerHeight);
        child.layout(child1LeftPosition, dividerTop, child1RightPosition, dividerTop + mDividerHeight);

        child = getChildAt(2);
        int child2LeftPosition = calculatePositionByOrientation(mButtonWidth, mDividerHeight);
        child.layout(child2LeftPosition, 0, mButtonWidth << 1, mButtonHeight);

        child = getChildAt(3);
        child.layout(mButtonWidth << 1, dividerTop, (mButtonWidth << 1) + mDividerWidth, dividerTop + mDividerHeight);

        child = getChildAt(4);
        child.layout(mButtonWidth << 1, 0, (mButtonWidth << 1) + mButtonWidth, mButtonHeight);
    }

    protected void setButtonVisibility() {
        if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mHoloButton.setVisibility(View.INVISIBLE);
            mMenuButton.setVisibility(View.INVISIBLE);
            mDivider1.setVisibility(View.INVISIBLE);
            mDivider2.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * it
     * @param portraitPosition
     * @param gapPosition
     * @return portraitPosition it current is orientation is portrait,(portraitPosition - gapPostion) when landscape
     */
    private int calculatePositionByOrientation(int portraitPosition,int gapPosition){
        if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            return portraitPosition- gapPosition;
        }
        return portraitPosition;
    }
}
