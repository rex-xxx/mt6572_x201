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

package com.android.gallery3d.data;

import android.content.Context;
import android.net.Uri;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.app.Log;
import com.mediatek.gallery3d.util.MtkLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class ClusterAlbumSet extends MediaSet implements ContentListener {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/ClusterAlbumSet";
    private GalleryApp mApplication;
    private MediaSet mBaseSet;
    private int mKind;
    private ArrayList<ClusterAlbum> mAlbums = new ArrayList<ClusterAlbum>();
    private boolean mFirstReloadDone;
    public  int currentIndexOfSet;
    private static final int MAX_LOAD_COUNT_CLUSTER_ALBUM = 64;
    /// M: while Cluster object delete operation. should set isDeleteOperation = true in delete operation Thread.
    private static boolean isDeleteOperation = false;
    public ClusterAlbumSet(Path path, GalleryApp application,
            MediaSet baseSet, int kind) {
        super(path, INVALID_DATA_VERSION);
        mApplication = application;
        mBaseSet = baseSet;
        mKind = kind;
        baseSet.addContentListener(this);
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return mAlbums.size();
    }

    @Override
    public String getName() {
        return mBaseSet.getName();
    }

    @Override
    public long reload() {
        /// M: if mBaseSet is instance of ComboAlbumSet and is not first reload, should use function synchronizedAlbumData @{
        boolean needSyncAlbum = (mFirstReloadDone)
                       && (mBaseSet instanceof LocalAlbumSet
                               || mBaseSet instanceof MtpDeviceSet
                               || mBaseSet instanceof ComboAlbumSet);
       if (mBaseSet instanceof ClusterAlbum) {
           /// M: mBaseSet is instance of ClusterAlbum,should offsetInStack +1
           mBaseSet.offsetInStack = offsetInStack + 1;
       }
       /// M: if offsetInStack%2 == 1, mean the reload is start by ClusterAlbum, in this case ,should call synchronizedAlbumData
       // if offsetInStack%2 != 1, mean the reload start by ClusterAlbumSet, should call reload of mBaseSet.
       long mLastDataVersion =((offsetInStack%2 == 1) && needSyncAlbum) ? mBaseSet.synchronizedAlbumData() : mBaseSet.reload();
       /// @}
        if (mLastDataVersion > mDataVersion) {
            Log.d(TAG, "total media item count: " + mBaseSet.getTotalMediaItemCount());
            if (mFirstReloadDone) {
                if (!isDeleteOperation) {
                    updateClustersContents();
                } else {
                   /// M: add for Cluster delete operation.
                   updateClustersContentsForDeleteOperation();
                }
            } else {
                updateClusters();
                mFirstReloadDone = true;
            }
            mDataVersion = nextVersionNumber();
        } else {
            Log.d(TAG, "ClusterAlbumSet: mBaseSet.reload() <= mDataVersion");
        }
        /// M: reassign for next time reload. @{
        mCurrentClusterAlbum = null;
        offsetInStack = 0;
        currentIndexOfSet = -1;
        /// @}
        return mDataVersion;
    }

    /// M: synchronous reload to avoid loading item before it's got from db @{
    @Override
    public long reloadForSlideShow() {
        /// M: if mBaseSet is instance of ComboAlbumSet and is not first reload, should use function synchronizedAlbumData @{
        boolean needSyncAlbum = (mFirstReloadDone)
                       && (mBaseSet instanceof LocalAlbumSet
                               || mBaseSet instanceof MtpDeviceSet
                               || mBaseSet instanceof ComboAlbumSet);
       if (mBaseSet instanceof ClusterAlbum) {
           /// M: mBaseSet is instance of ClusterAlbum,should offsetInStack +1
           mBaseSet.offsetInStack = offsetInStack + 1;
       }
       /// M: if offsetInStack%2 == 1, mean the reload is start by ClusterAlbum, in this case ,should call synchronizedAlbumData
       // if offsetInStack%2 != 1, mean the reload start by ClusterAlbumSet, should call reload of mBaseSet.
       long mLastDataVersion =((offsetInStack%2 == 1) && needSyncAlbum) ? mBaseSet.synchronizedAlbumData() : mBaseSet.reloadForSlideShow();
       /// @}
        if (mLastDataVersion > mDataVersion) {
            Log.d(TAG, "total media item count: " + mBaseSet.getTotalMediaItemCount());
            if (mFirstReloadDone) {
                if (!isDeleteOperation) {
                    updateClustersContents();
                } else {
                   /// M: add for Cluster delete operation.
                   updateClustersContentsForDeleteOperation();
                }
            } else {
                updateClusters();
                mFirstReloadDone = true;
            }
            mDataVersion = nextVersionNumber();
        } else {
            Log.d(TAG, "ClusterAlbumSet: mBaseSet.reload() <= mDataVersion");
        }
        /// M: reassign for next time reload. @{
        mCurrentClusterAlbum = null;
        offsetInStack = 0;
        currentIndexOfSet = -1;
        /// @}
        return mDataVersion;
    }
    /// @}

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    private void updateClusters() {
        Log.d(TAG, "updateClusters");
        mAlbums.clear();
        Clustering clustering;
        Context context = mApplication.getAndroidContext();
        switch (mKind) {
            case ClusterSource.CLUSTER_ALBUMSET_TIME:
                clustering = new TimeClustering(context);
                break;
            case ClusterSource.CLUSTER_ALBUMSET_LOCATION:
                clustering = new LocationClustering(context);
                break;
            case ClusterSource.CLUSTER_ALBUMSET_TAG:
                clustering = new TagClustering(context);
                break;
            case ClusterSource.CLUSTER_ALBUMSET_FACE:
                clustering = new FaceClustering(context);
                break;
            default: /* CLUSTER_ALBUMSET_SIZE */
                clustering = new SizeClustering(context);
                break;
        }

        clustering.run(mBaseSet);
        int n = clustering.getNumberOfClusters();
        Log.d(TAG, "number of clusters: " + n);
        DataManager dataManager = mApplication.getDataManager();
        for (int i = 0; i < n; i++) {
            Path childPath;
            String childName = clustering.getClusterName(i);
            if (mKind == ClusterSource.CLUSTER_ALBUMSET_TAG) {
                childPath = mPath.getChild(Uri.encode(childName));
            } else if (mKind == ClusterSource.CLUSTER_ALBUMSET_SIZE) {
                long minSize = ((SizeClustering) clustering).getMinSize(i);
                childPath = mPath.getChild(minSize);
            } else {
                childPath = mPath.getChild(i);
            }
            ClusterAlbum album = (ClusterAlbum) dataManager.peekMediaObject(
                        childPath);
            if (album == null) {
                album = new ClusterAlbum(childPath, dataManager, this);
            }
            album.setMediaItems(clustering.getCluster(i));
            /// M: do updateClusters, means not in cluster album delete operation, so should set isDeleteOperation = false.
            // and set mNumberOfDeletedImage = 0;
            isDeleteOperation = false;
            album.setNumberOfDeletedImage(0);
            album.setName(childName);
            album.setCoverMediaItem(clustering.getClusterCover(i));
            mAlbums.add(album);
        }
    }

    private void updateClustersContentsForDeleteOperation() {
        int n = mAlbums.size();
        for (int i = n - 1; i >= 0; i--) {
            /// M : If the number of deleted image equal with albums size ,means there is not image in this album.
            // So should delete the album.
           if  (mAlbums.get(i).getNumberOfDeletedImage() == mAlbums.get(i).getMediaItemCount()) {
               mAlbums.get(i).setNumberOfDeletedImage(0);
               mAlbums.remove(i);
           }
        }
    }

    private void updateClustersContents() {
        Log.d(TAG, "updateClusterContents");
        final HashSet<Path> existing = new HashSet<Path>();
        /// M: record MediaItem.
        final HashMap<Path, MediaItem> existingMediaItem = new HashMap<Path, MediaItem>();
        mBaseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            public void consume(int index, MediaItem item) {
                existing.add(item.getPath());
                existingMediaItem.put(item.getPath(), item);
            }
        });
        /// M:note for oldPath @{
        HashSet<Path> oldPathHashSet = new HashSet<Path>();
        int deletedItem = 0;
        /// @}
        int n = mAlbums.size();
        // The loop goes backwards because we may remove empty albums from
        // mAlbums.
        for (int i = n - 1; i >= 0; i--) {
            ArrayList<Path> oldPaths = mAlbums.get(i).getMediaItems();
          //  Log.d(TAG, "oldPaths.size==="+oldPaths.size());
            ArrayList<Path> newPaths = new ArrayList<Path>();

            int m = oldPaths.size();
            for (int j = 0; j < m; j++) {
                Path p = oldPaths.get(j);
                /// M: add all path to hashSet
                oldPathHashSet.add(p);
                if (existing.contains(p)) {
                    newPaths.add(p);
                } else {
                    deletedItem++;
                }
            }
            mAlbums.get(i).setMediaItems(newPaths);
            //mAlbums.get(i).nextVersion();
            if (newPaths.isEmpty()) {
                mAlbums.remove(i);
            }
        }
        /// M: addedPath is used to save added paths.@{
        if ((existing.size() + deletedItem > oldPathHashSet.size())) {
            Log.d(TAG," offsetOFStack=="+offsetInStack +" currentIndexOfSet="+currentIndexOfSet);
            if ( offsetInStack >= 1 ) {
                ArrayList<Path> addedPath = new ArrayList<Path>();
                /// M: transfer HashSet to ArrayList.
                ArrayList<Path> mNewPath = new ArrayList<Path>(existing);
                int sizeOfExistingPath = mNewPath.size();
                for (int i = 0; i < sizeOfExistingPath; i ++) {
                    if (!oldPathHashSet.contains(mNewPath.get(i))) {
                        addedPath.add(mNewPath.get(i));
                        Log.d(TAG," addedPath=="+mNewPath.get(i).toString());
                    }
                }
                /// M: there some new path, so should updateClusters again;
                setCurrentIndexOfSet();
                updateAlbumInClusters(addedPath, existingMediaItem);
            } else {
                updateClusters();
                /// M: modify mDataVersion of all album.
                for (int i = mAlbums.size()-1; i >= 0; i--) {
                    mAlbums.get(i).nextVersion();
                }
          }
        }
        /// @}
    }

    /// M: set mCurrentClusterAlbum. @{
    public void setCurrentIndexOfSet () {
        int albumSize = mAlbums.size();
        if (mCurrentClusterAlbum != null) {
            boolean hasFindSet = false;
            for (int i = 0; i < albumSize; i++) {
                if (mAlbums.get(i) == mCurrentClusterAlbum) {
                    currentIndexOfSet = i;
                    hasFindSet = true;
                    break;
                }
            }
            if (!hasFindSet) {
                currentIndexOfSet = 0;
                Log.d(TAG, "[setCurrentIndexOfSet]: has not find set");
            }
        }
    }
    ///@}

    /// M: update one Album, instant of doing updateClusters.
    public void updateAlbumInClusters(ArrayList<Path> paths, HashMap<Path, MediaItem> exisingMediaItem) {
        if (mAlbums != null) {
            if (currentIndexOfSet < mAlbums.size() && currentIndexOfSet >= 0) {
                try {
                    /// M: get mediaSet according mPath.
                    MediaSet mMediaSet = mApplication.getDataManager().getMediaSet(mAlbums.get(currentIndexOfSet).mPath);
                    ArrayList<MediaItem> mOldItem = mMediaSet.getMediaItem(0, mMediaSet.getMediaItemCount());
                    int sizeOfOldMediaItems = mOldItem.size();
                    int sizeofAddPaths = paths.size();
                    /// M: get each Path that need to add in old path list.
                    for (int j = 0; j < sizeofAddPaths; j++) {
                        MediaItem item = exisingMediaItem.get(paths.get(j));
                        int k = 0;
                        for (; k < sizeOfOldMediaItems; k++) {
                            if (item.getDateInMs() == mOldItem.get(k).getDateInMs()) {
                                mAlbums.get(currentIndexOfSet).addMediaItems(paths.get(j), k);
                                Log.d(TAG,"add Path::"+paths.get(j).toString()+"  index:::::"+k);
                                break;
                            }
                        }
                        /// M: if not find the item ,should insert the item in index 0
                        if (k == sizeOfOldMediaItems) {
                            mAlbums.get(currentIndexOfSet).addMediaItems(paths.get(j), 0);
                            Log.d(TAG,"add Path::"+paths.get(j).toString()+" the end index:::::"+k);
                        }
                    }
                }  catch (OutOfMemoryError e) {
                    Log.w(TAG," maybe sizeOldMediaItems is too big:" + e);
                }
            }
            Log.d(TAG, "currentIndexOfSet=="+currentIndexOfSet);
        }
    }
    /// @}

    /// M: synchronizedAlbumData should be started by ClusterAlbum. Using reload should not update Albums in time.
    public long synchronizedAlbumData() {
        if (mBaseSet.synchronizedAlbumData() > mDataVersion ) {
            updateClustersContents();
            mDataVersion = nextVersionNumber();
        }
        return mDataVersion;
    }
    /// @}

    /// M: set and get ClusterDeleteOperation.
    public static void setClusterDeleteOperation(boolean deleteOperation) {
        MtkLog.d(TAG, "setClusterDeleteOperation isDeleteOperation: " + deleteOperation);
        isDeleteOperation = deleteOperation;
    }

    public static boolean getClusterDeleteOperation() {
        MtkLog.d(TAG, "getClusterDeleteOperation isDeleteOperation: " + isDeleteOperation);
        return isDeleteOperation;
    }
    /// @}
}
