package com.mediatek.contacts.editor;

import android.content.Context;
import android.util.AttributeSet;

import com.android.contacts.R;

public class SimPhotoEditorViewOrange extends SimPhotoEditorView {

    public SimPhotoEditorViewOrange(Context context) {
        super(context);
    }

    public SimPhotoEditorViewOrange(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected int getPhotoImageResource() {
        return R.drawable.ic_contact_picture_sim_contact_orange;
    }
}