/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.editor;

import android.content.Context;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.editor.Editor.EditorListener;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDelta.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataKind;

import java.util.ArrayList;
import java.util.List;


//The following lines are provided and maintained by Mediatek Inc.
import com.android.contacts.ext.ContactAccountExtension;
import com.android.contacts.ext.ContactDetailExtension;
import com.android.contacts.model.AccountTypeManager;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.extension.aassne.SimUtils;
import com.mediatek.contacts.model.UsimAccountType;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import android.content.res.Resources;
import android.widget.EditText;
import android.util.Log;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.content.ContentValues;
//The previous lines are provided and maintained by Mediatek Inc.
/**
 * Custom view for an entire section of data as segmented by
 * {@link DataKind} around a {@link Data#MIMETYPE}. This view shows a
 * section header and a trigger for adding new {@link Data} rows.
 */
public class KindSectionView extends LinearLayout implements EditorListener {
    private static final String TAG = "KindSectionView";

    private TextView mTitle;
    private ViewGroup mEditors;
    private View mAddFieldFooter;
    private String mTitleString;

    private DataKind mKind;
    private RawContactDelta mState;
    private boolean mReadOnly;

    private ViewIdGenerator mViewIdGenerator;

    private LayoutInflater mInflater;

    private final ArrayList<Runnable> mRunWhenWindowFocused = new ArrayList<Runnable>(1);

    public KindSectionView(Context context) {
        this(context, null);
    }

    public KindSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mEditors != null) {
            int childCount = mEditors.getChildCount();
            for (int i = 0; i < childCount; i++) {
                mEditors.getChildAt(i).setEnabled(enabled);
            }
        }

        if (enabled && !mReadOnly) {
            mAddFieldFooter.setVisibility(View.VISIBLE);
        } else {
            mAddFieldFooter.setVisibility(View.GONE);
        }
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTitle = (TextView) findViewById(R.id.kind_title);
        
        // MTK_THEMEMANAGER_APP
        if (FeatureOption.MTK_THEMEMANAGER_APP) {
            Resources res = mContext.getResources();
            int textColor = res.getThemeMainColor();
            if (textColor != 0) {
                mTitle.setTextColor(textColor);
            }
        }
        // MTK_THEMEMANAGER_APP
        
        mEditors = (ViewGroup) findViewById(R.id.kind_editors);
        mAddFieldFooter = findViewById(R.id.add_field_footer);
        mAddFieldFooter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Setup click listener to add an empty field when the footer is clicked.
                mAddFieldFooter.setVisibility(View.GONE);
                addItem();
            }
        });
    }

    @Override
    public void onDeleteRequested(Editor editor) {
        // If there is only 1 editor in the section, then don't allow the user to delete it.
        // Just clear the fields in the editor.
        final boolean animate;
        /**
         * M:AAS @ { origin code: if (getEditorCount() == 1) {
         */
        ContactAccountExtension cae = ExtensionManager.getInstance().getContactAccountExtension();
        final int slotId = cae.getCurrentSlot(ExtensionManager.COMMD_FOR_AAS);
        final String accountType = SimUtils.getAccountTypeBySlot(slotId);
        if (getEditorCount() == 1 || cae.isFeatureAccount(accountType, ExtensionManager.COMMD_FOR_AAS)) {
            /** M: @ } */
            editor.clearAllFields();
            animate = true;
        } else {
            // Otherwise it's okay to delete this {@link Editor}
            editor.deleteEditor();

            // This is already animated, don't do anything further here
            animate = false;
        }
        updateAddFooterVisible(animate);
    }

    @Override
    public void onRequest(int request) {
        // If a field has become empty or non-empty, then check if another row
        // can be added dynamically.
        if (request == FIELD_TURNED_EMPTY || request == FIELD_TURNED_NON_EMPTY) {
            updateAddFooterVisible(true);
        }
    }

    public void setState(DataKind kind, RawContactDelta state, boolean readOnly, ViewIdGenerator vig) {
        mKind = kind;
        mState = state;
        mReadOnly = readOnly;
        mViewIdGenerator = vig;

        setId(mViewIdGenerator.getId(state, kind, null, ViewIdGenerator.NO_VIEW_INDEX));

        // TODO: handle resources from remote packages
        mTitleString = (kind.titleRes == -1 || kind.titleRes == 0)
                ? ""
                : getResources().getString(kind.titleRes);
        mTitle.setText(mTitleString);

        /** M: Bug Fix for ALPS00557517 @{ */
        String accountType = mState.getAccountType();
        if (AccountType.ACCOUNT_TYPE_USIM.equals(accountType) && mKind.mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
            int slotId = AccountType.getSlotIdBySimAccountName(mState.getAccountName());
            mKind.typeOverallMax = SimCardUtils.getAnrCount(slotId) + 1;
        }
        /** @ } */
        rebuildFromState();
        updateAddFooterVisible(false);
        updateSectionVisible();
    }

    public String getTitle() {
        return mTitleString;
    }

    public void setTitleVisible(boolean visible) {
        findViewById(R.id.kind_title_layout).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Build editors for all current {@link #mState} rows.
     */
    public void rebuildFromState() {
        // Remove any existing editors
        mEditors.removeAllViews();

        /** M:AAS @ { */
        ContactAccountExtension cae = ExtensionManager.getInstance().getContactAccountExtension();
        final String accountType = SimUtils.getAccountTypeBySlot(cae.getCurrentSlot(ExtensionManager.COMMD_FOR_AAS));
        Log.d(TAG, "rebuildFromState() accountType=" + accountType + " mimeType=" + mKind.mimeType);
        if (cae.isFeatureAccount(accountType, ExtensionManager.COMMD_FOR_AAS) && cae.isPhone(mKind.mimeType, ExtensionManager.COMMD_FOR_AAS)) {
            ArrayList<ValuesDelta> values = mState.getMimeEntries(mKind.mimeType);
            if (values != null) {
                Log.d(TAG, "rebuildFromState() values size=" + values.size());
                ArrayList<ValuesDelta> orderedDeltas = new ArrayList<ValuesDelta>();
                for (ValuesDelta entry : values) {
                    if (SimUtils.isAdditionalNumber(entry)) {
                        orderedDeltas.add(entry);
                    } else {
                        // add primary number to first.
                        orderedDeltas.add(0, entry);
                    }
                }
                if (!orderedDeltas.isEmpty()) {
                    for (ValuesDelta entry : orderedDeltas) {
                        if (!entry.isVisible())
                            continue;

                        createEditorView(entry);
                    }
                    return;
                }
            }
        }
        /** M: @ } */

        // Check if we are displaying anything here
        boolean hasEntries = mState.hasMimeEntries(mKind.mimeType);

        if (hasEntries) {
            for (ValuesDelta entry : mState.getMimeEntries(mKind.mimeType)) {
                // Skip entries that aren't visible
                if (!entry.isVisible()) continue;
                if (isEmptyNoop(entry)) continue;

                createEditorView(entry);
            }
        }
    }


    /**
     * Creates an EditorView for the given entry. This function must be used while constructing
     * the views corresponding to the the object-model. The resulting EditorView is also added
     * to the end of mEditors
     */
    private View createEditorView(ValuesDelta entry) {
        final View view;
        try {
            view = mInflater.inflate(mKind.editorLayoutResourceId, mEditors, false);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot allocate editor with layout resource ID " +
                    mKind.editorLayoutResourceId + " for MIME type " + mKind.mimeType +
                    " with error " + e.toString()+" | mEditors : "+mEditors);
        }

        view.setEnabled(isEnabled());

        if (view instanceof Editor) {
            Editor editor = (Editor) view;
            editor.setDeletable(true);
            editor.setValues(mKind, entry, mState, mReadOnly, mViewIdGenerator);
            editor.setEditorListener(this);
        }
        
        mEditors.addView(view);
        return view;
    }

    /**
     * Tests whether the given item has no changes (so it exists in the database) but is empty
     */
    private boolean isEmptyNoop(ValuesDelta item) {
        if (!item.isNoop()) return false;
        final int fieldCount = mKind.fieldList.size();
        for (int i = 0; i < fieldCount; i++) {
            final String column = mKind.fieldList.get(i).column;
            final String value = item.getAsString(column);
            if (!TextUtils.isEmpty(value)) return false;
        }
        return true;
    }

    private void updateSectionVisible() {
        setVisibility(getEditorCount() != 0 ? VISIBLE : GONE);
    }

    protected void updateAddFooterVisible(boolean animate) {
        if (!mReadOnly && (mKind.typeOverallMax != 1)) {
            // First determine whether there are any existing empty editors.
            updateEmptyEditors();
            // If there are no existing empty editors and it's possible to add
            // another field, then make the "add footer" field visible.
            if (!hasEmptyEditor() && RawContactModifier.canInsert(mState, mKind)) {
                /** M:AAS @ { */
                final int slotId = ExtensionManager.getInstance().getContactAccountExtension().getCurrentSlot(ExtensionManager.COMMD_FOR_AAS);
                final String accountType = SimUtils.getAccountTypeBySlot(slotId);
                if (ExtensionManager.getInstance().getContactAccountExtension().isFeatureAccount(accountType,
                        ExtensionManager.COMMD_FOR_AAS)) {
                    ExtensionManager.getInstance().getContactDetailExtension().updateView(mAddFieldFooter, 0,
                            ContactDetailExtension.VIEW_UPDATE_VISIBILITY, ExtensionManager.COMMD_FOR_AAS);
                    return;
                }
                /** M: @ } */
                if (animate) {
                    EditorAnimator.getInstance().showAddFieldFooter(mAddFieldFooter);
                } else {
                    mAddFieldFooter.setVisibility(View.VISIBLE);
                }
                return;
            }
        }
        if (animate) {
            EditorAnimator.getInstance().hideAddFieldFooter(mAddFieldFooter);
        } else {
            mAddFieldFooter.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the editors being displayed to the user removing extra empty
     * {@link Editor}s, so there is only max 1 empty {@link Editor} view at a time.
     */
    private void updateEmptyEditors() {
        List<View> emptyEditors = getEmptyEditors();

        // If there is more than 1 empty editor, then remove it from the list of editors.
        /** M:AAS max default value is 1 @ { */
        int max = ExtensionManager.getInstance().getContactDetailExtension().getMaxEmptyEditors(mKind.mimeType, ExtensionManager.COMMD_FOR_AAS);
        Log.d(TAG, "updateEmptyEditors() max =" + max + " emptyEditors.size()=" + emptyEditors.size() + ", mEditors="
                + mEditors.getChildCount());
        /** M: @ } */

        if (emptyEditors.size() > max) {
            for (View emptyEditorView : emptyEditors) {
                // If no child {@link View}s are being focused on within
                // this {@link View}, then remove this empty editor.
                if (emptyEditorView.findFocus() == null) {
                    mEditors.removeView(emptyEditorView);
                }
            }
        }
    }

    /**
     * Returns a list of empty editor views in this section.
     */
    private List<View> getEmptyEditors() {
        List<View> emptyEditorViews = new ArrayList<View>();
        for (int i = 0; i < mEditors.getChildCount(); i++) {
            View view = mEditors.getChildAt(i);
            if (((Editor) view).isEmpty()) {
                emptyEditorViews.add(view);
            }
        }
        return emptyEditorViews;
    }

    /**
     * Returns true if one of the editors has all of its fields empty, or false
     * otherwise.
     */
    private boolean hasEmptyEditor() {
        return getEmptyEditors().size() > 0;
    }

    /**
     * Returns true if all editors are empty.
     */
    public boolean isEmpty() {
        for (int i = 0; i < mEditors.getChildCount(); i++) {
            View view = mEditors.getChildAt(i);
            if (!((Editor) view).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extends superclass implementation to also run tasks
     * enqueued by {@link #runWhenWindowFocused}.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            for (Runnable r: mRunWhenWindowFocused) {
                r.run();
            }
            mRunWhenWindowFocused.clear();
        }
    }

    /**
     * Depending on whether we are in the currently-focused window, either run
     * the argument immediately, or stash it until our window becomes focused.
     */
    private void runWhenWindowFocused(Runnable r) {
        if (hasWindowFocus()) {
            r.run();
        } else {
            mRunWhenWindowFocused.add(r);
        }
    }

    /**
     * Simple wrapper around {@link #runWhenWindowFocused}
     * to ensure that it runs in the UI thread.
     */
    private void postWhenWindowFocused(final Runnable r) {
        post(new Runnable() {
            @Override
            public void run() {
                runWhenWindowFocused(r);
            }
        });
    }

    public void addItem() {
        ValuesDelta values = null;
        // If this is a list, we can freely add. If not, only allow adding the first.
        if (mKind.typeOverallMax == 1) {
            if (getEditorCount() == 1) {
                return;
            }

            // If we already have an item, just make it visible
            ArrayList<ValuesDelta> entries = mState.getMimeEntries(mKind.mimeType);
            if (entries != null && entries.size() > 0) {
                values = entries.get(0);
            }
        }

        // Insert a new child, create its view and set its focus
        if (values == null) {
            values = RawContactModifier.insertChild(mState, mKind);
        }

        final View newField = createEditorView(values);
        if (newField instanceof Editor) {
            postWhenWindowFocused(new Runnable() {
                @Override
                public void run() {
                    newField.requestFocus();
                    ((Editor)newField).editNewlyAddedField();
                }
            });
        }

        // Hide the "add field" footer because there is now a blank field.
        mAddFieldFooter.setVisibility(View.GONE);

        // Ensure we are visible
        updateSectionVisible();
    }

    public int getEditorCount() {
        return mEditors.getChildCount();
    }

    public DataKind getKind() {
        return mKind;
    }
    
    // / The following lines are provided and maintained by Mediatek Inc.
    /** M: Bug Fix for CR ALPS00328644 @{ */
    public void addSimItem() {
        ValuesDelta values = null;
        if (mKind.typeOverallMax == 1) {
            if (getEditorCount() == 1) {
                return;
            }
            ArrayList<ValuesDelta> entries = mState.getMimeEntries(mKind.mimeType);
            if (entries != null && entries.size() > 0) {
                values = entries.get(0);
            }
        }
        if (values == null) {
            values = RawContactModifier.insertChild(mState, mKind);
        }

        final View newField = createEditorView(values);
        if (newField instanceof Editor) {
            postWhenWindowFocused(new Runnable() {
                @Override
                public void run() {
                    ((Editor) newField).editNewlyAddedField();
                }
            });
        }

        mAddFieldFooter.setVisibility(View.GONE);

        updateSectionVisible();
    }
    /** @} */
    // / The previous lines are provided and maintained by Mediatek Inc.
}
