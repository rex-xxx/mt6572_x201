package com.mediatek.contacts.editor;

import android.content.Context;
import android.util.AttributeSet;

import com.android.contacts.R;

public class UsimPhotoEditorView extends SimPhotoEditorView {

    public UsimPhotoEditorView(Context context) {
        super(context);
    }

    public UsimPhotoEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected int getPhotoImageResource() {
        return R.drawable.ic_contact_picture_usim_contact;
    }

    protected int onInflatePhotoImageId() {
        return R.id.usim_photo;
    }

}