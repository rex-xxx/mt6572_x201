package com.mediatek.contacts.editor;

import android.content.Context;
import android.util.AttributeSet;

import com.android.contacts.R;

public class SimPhotoEditorViewGreen extends SimPhotoEditorView {

    public SimPhotoEditorViewGreen(Context context) {
        super(context);
    }

    public SimPhotoEditorViewGreen(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected int getPhotoImageResource() {
        return R.drawable.ic_contact_picture_sim_contact_green;
    }

}