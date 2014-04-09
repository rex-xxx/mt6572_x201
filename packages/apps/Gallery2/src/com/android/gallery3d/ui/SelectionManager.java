/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.gallery3d.ui;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.ThreadPool.JobContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SelectionManager {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/SelectionManager";

    public static final int ENTER_SELECTION_MODE = 1;
    public static final int LEAVE_SELECTION_MODE = 2;
    public static final int SELECT_ALL_MODE = 3;
    // M: when click deselect all in menu, not leave selection mode
    public static final int DESELECT_ALL_MODE = 4;

    private Set<Path> mClickedSet;
    private MediaSet mSourceMediaSet;
    private SelectionListener mListener;
    private DataManager mDataManager;
    private boolean mInverseSelection;
    private boolean mIsAlbumSet;
    private boolean mInSelectionMode;
    private boolean mAutoLeave = true;
    private int mTotal;
    private ArrayList<Path> mSelection = null;

    public interface SelectionListener {
        public void onSelectionModeChange(int mode);
        public void onSelectionChange(Path path, boolean selected);
    }

    public SelectionManager(AbstractGalleryActivity activity, boolean isAlbumSet) {
        mDataManager = activity.getDataManager();
        mClickedSet = new HashSet<Path>();
        mIsAlbumSet = isAlbumSet;
        mTotal = -1;
    }

    // Whether we will leave selection mode automatically once the number of
    // selected items is down to zero.
    public void setAutoLeaveSelectionMode(boolean enable) {
        mAutoLeave = enable;
    }

    public void setSelectionListener(SelectionListener listener) {
        mListener = listener;
    }

    public void selectAll() {
        mInverseSelection = true;
        mClickedSet.clear();
        enterSelectionMode();
        if (mListener != null) mListener.onSelectionModeChange(SELECT_ALL_MODE);
    }

    public void deSelectAll() {
        // M: when click deselect all in menu, not leave selection mode
        //leaveSelectionMode();
        mInverseSelection = false;
        mClickedSet.clear();
        // M: when click deselect all in menu, not leave selection mode
        if (mListener != null) {
            mListener.onSelectionModeChange(DESELECT_ALL_MODE);
        }
    }

    public boolean inSelectAllMode() {
        ///M: Not in select all mode, if not all items are selected now @{
        return getTotalCount() == getSelectedCount();
        ///@}
        //return mInverseSelection;
    }

    public boolean inSelectionMode() {
        return mInSelectionMode;
    }

    public void enterSelectionMode() {
        if (mInSelectionMode) return;
        Log.i(TAG, "<enterSelectionMode>");
        mInSelectionMode = true;
        if (mListener != null) mListener.onSelectionModeChange(ENTER_SELECTION_MODE);
    }

    public void leaveSelectionMode() {
        if (!mInSelectionMode) return;

        Log.i(TAG, "<leaveSelectionMode>");
        mInSelectionMode = false;
        mInverseSelection = false;
        mClickedSet.clear();
        // M: clear mTotal so that it will be re-calculated
        // next time user enters selection mode
        mTotal = -1;
        if (mListener != null) mListener.onSelectionModeChange(LEAVE_SELECTION_MODE);
    }

    public boolean isItemSelected(Path itemId) {
        return mInverseSelection ^ mClickedSet.contains(itemId);
    }

    private int getTotalCount() {
        if (mSourceMediaSet == null) return -1;

        if (mTotal < 0) {
            mTotal = mIsAlbumSet
                    ? mSourceMediaSet.getSubMediaSetCount()
                    : mSourceMediaSet.getMediaItemCount();
        }
        return mTotal;
    }

    public int getSelectedCount() {
        int count = mClickedSet.size();
        if (mInverseSelection) {
            count = getTotalCount() - count;
        }
        return count;
    }

    public void toggle(Path path) {
        Log.i(TAG, "<toggle> path = " + path);
        if (mClickedSet.contains(path)) {
            mClickedSet.remove(path);
        } else {
            enterSelectionMode();
            mClickedSet.add(path);
        }

        // Convert to inverse selection mode if everything is selected.
        int count = getSelectedCount();
        if (count == getTotalCount()) {
            selectAll();
        }

        if (mListener != null) mListener.onSelectionChange(path, isItemSelected(path));
        if (count == 0 && mAutoLeave) {
            leaveSelectionMode();
        }
    }

    private static void expandMediaSet(ArrayList<Path> items, MediaSet set) {
        int subCount = set.getSubMediaSetCount();
        for (int i = 0; i < subCount; i++) {
            expandMediaSet(items, set.getSubMediaSet(i));
        }
        int total = set.getMediaItemCount();
        int batch = 50;
        int index = 0;

        while (index < total) {
            int count = index + batch < total
                    ? batch
                    : total - index;
            ArrayList<MediaItem> list = set.getMediaItem(index, count);
            for (MediaItem item : list) {
                items.add(item.getPath());
            }
            index += batch;
        }
    }

    public ArrayList<Path> getSelected(boolean expandSet) {
        final ArrayList<Path> selected = new ArrayList<Path>();
        if (mIsAlbumSet) {
            if (mInverseSelection) {
                int total = getTotalCount();
                for (int i = 0; i < total; i++) {
                    MediaSet set = mSourceMediaSet.getSubMediaSet(i);
                    /// M: if set is null, should continue and return directly.
                    if (set == null) {
                        continue;
                    }
                    Path id = set.getPath();
                    if (!mClickedSet.contains(id)) {
                        if (expandSet) {
                            expandMediaSet(selected, set);
                        } else {
                            selected.add(id);
                        }
                    }
                }
            } else {
                for (Path id : mClickedSet) {
                    if (expandSet) {
                        expandMediaSet(selected, mDataManager.getMediaSet(id));
                    } else {
                        selected.add(id);
                    }
                }
            }
        } else {
            if (mInverseSelection) {
                int total = getTotalCount();
                int index = 0;
                while (index < total) {
                    int count = Math.min(total - index, MediaSet.MEDIAITEM_BATCH_FETCH_COUNT);
                    ArrayList<MediaItem> list = mSourceMediaSet.getMediaItem(index, count);
                    for (MediaItem item : list) {
                        Path id = item.getPath();
                        if (!mClickedSet.contains(id)) selected.add(id);
                    }
                    index += count;
                }
            } else {
                /// M:  we check if items in click set are still in mSourceMediaSet,
                // if not, we do not add it to selected list.
                ArrayList<Path> selectedPathTemple = new ArrayList<Path>();
                selectedPathTemple.addAll(mClickedSet);
                mDataManager.mapMediaItems(selectedPathTemple, new MediaSet.ItemConsumer() {
                    public void consume(int index, MediaItem item) {
                        selected.add(item.getPath());
                    }
                }, 0);
                /// @}
            }
        }
        return selected;
    }
    
    /// M: used by ActionModeHandler computeShareIntent @{
    public ArrayList<Path> getSelected(JobContext jc, boolean expandSet) {
        final ArrayList<Path> selected = new ArrayList<Path>();
        if (mIsAlbumSet) {
            if (mInverseSelection) {
                int total = getTotalCount();
                for (int i = 0; i < total; i++) {
                    if (jc.isCancelled()) {
                        Log.i(TAG, "<getSelected> jc.isCancelled() - 1");
                        return null;
                    }
                    MediaSet set = mSourceMediaSet.getSubMediaSet(i);
                    /// M: if set is null, should continue and return directly.
                    if (set == null) {
                        continue;
                    }
                    Path id = set.getPath();
                    if (!mClickedSet.contains(id)) {
                        if (expandSet) {
                            expandMediaSet(jc, selected, set);
                        } else {
                            selected.add(id);
                        }
                    }
                }
            } else {
                for (Path id : mClickedSet) {
                    if (jc.isCancelled()) {
                        Log.i(TAG, "<getSelected> jc.isCancelled() - 2");
                        return null;
                    }
                    if (expandSet) {
                        expandMediaSet(jc, selected, mDataManager.getMediaSet(id));
                    } else {
                        selected.add(id);
                    }
                }
            }
        } else {
            if (mInverseSelection) {
                int total = getTotalCount();
                int index = 0;
                while (index < total) {
                    int count = Math.min(total - index, MediaSet.MEDIAITEM_BATCH_FETCH_COUNT);
                    ArrayList<MediaItem> list = mSourceMediaSet.getMediaItem(index, count);
                    for (MediaItem item : list) {
                        if (jc.isCancelled()) {
                            Log.i(TAG, "<getSelected> jc.isCancelled() - 3");
                            return null;
                        }
                        Path id = item.getPath();
                        if (!mClickedSet.contains(id)) selected.add(id);
                    }
                    index += count;
                }
            } else {
                /// M:  we check if items in click set are still in mSourceMediaSet,
                // if not, we do not add it to selected list.
                ArrayList<Path> selectedPathTemple = new ArrayList<Path>();
                selectedPathTemple.addAll(mClickedSet);
                mDataManager.mapMediaItems(selectedPathTemple, new MediaSet.ItemConsumer() {
                    public void consume(int index, MediaItem item) {
                        selected.add(item.getPath());
                    }
                }, 0);
                /// @}
            }
        }
        return selected;
    }

    private static void expandMediaSet(JobContext jc, ArrayList<Path> items, MediaSet set) {
        if (jc.isCancelled()) {
            Log.i(TAG, "<expandMediaSet> jc.isCancelled() - 1");
            return;
        }
        int subCount = set.getSubMediaSetCount();
        for (int i = 0; i < subCount; i++) {
            if (jc.isCancelled()) {
                Log.i(TAG, "<expandMediaSet> jc.isCancelled() - 2");
                return;
            }
            expandMediaSet(jc, items, set.getSubMediaSet(i));
        }
        int total = set.getMediaItemCount();
        int batch = 50;
        int index = 0;

        while (index < total) {
            if (jc.isCancelled()) {
                Log.i(TAG, "<expandMediaSet> jc.isCancelled() - 3");
                return;
            }
            int count = index + batch < total
                    ? batch
                    : total - index;
            ArrayList<MediaItem> list = set.getMediaItem(index, count);
            for (MediaItem item : list) {
                if (jc.isCancelled()) {
                    Log.i(TAG, "<expandMediaSet> jc.isCancelled() - 4");
                    return;
                }
                items.add(item.getPath());
            }
            index += batch;
        }
    }
    /// @}

    public void setSourceMediaSet(MediaSet set) {
        mSourceMediaSet = set;
        mTotal = -1;
    }

    public void saveSelection() {
        if (mSelection != null) {
            mSelection.clear();
        }
        try {
            mSelection  = getSelected(false);
        } catch (Exception e) {
            // this probably means that the actual items are changing
            // while fetching selected items, so we do not save selection
            // under this situation
            /// TODO: find more suitable method to protect this part
            mSelection = null;
        }
    }

    public void restoreSelection() {
        Log.d(TAG, "restoreSelection");
        if (mSourceMediaSet == null || mSelection == null) {
            return;
        }
        mTotal = mIsAlbumSet
                   ? mSourceMediaSet.getSubMediaSetCount()
                   : mSourceMediaSet.getMediaItemCount();
        Path id = null;
        ArrayList<Path> availablePaths = new ArrayList<Path>();
        // remove dirty entry
        if (mIsAlbumSet) {
            MediaSet set = null;
            for (int i = 0; i < mTotal; ++i) {
                set = mSourceMediaSet.getSubMediaSet(i);
                if (set != null) {
                    id = set.getPath();
                    if (mSelection.contains(id)) {
                        availablePaths.add(id);
                    }
                }
            }
        } else {
            ArrayList<MediaItem> items = mSourceMediaSet.getMediaItem(0, mTotal);
            if (items != null && items.size() > 0) {
                for (MediaItem item : items) {
                    id = item.getPath();
                    if (mSelection.contains(id)) {
                        availablePaths.add(id);
                    }
                }
            }
        }
        int newCnt = availablePaths.size();
        if (newCnt == mTotal && mTotal != 0) {
            // in select all mode
            selectAll();
        } else {
            // leave select all mode and set clicked set
            mInverseSelection = false;
            mClickedSet.clear();
            for (int i = 0; i < newCnt; ++i) {
                mClickedSet.add(availablePaths.get(i));
            }
            // M: if no item selected when restore, we do not leave selection
            // mode
            // Log.d("wjx", "mClickedSet.size"+mClickedSet.size());
            // if (mClickedSet.size() == 0) {
            // leaveSelectionMode();
            // }
        }

        // clear saved selection when done
        mSelection.clear();
        mSelection = null;
    }

    public void onSourceContentChanged() {
        // M: reset and reload total count since source set data has changed
        mTotal = -1;
        //getTotalCount();
        int count = getTotalCount();
        Log.d(TAG, "onSourceContentChanged, new total=" + count);
        if (count == 0) {
            leaveSelectionMode();
        }
    }
}
