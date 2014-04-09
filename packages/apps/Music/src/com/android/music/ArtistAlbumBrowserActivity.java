/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.music;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Adapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

import com.android.music.MusicUtils.ServiceToken;


public class ArtistAlbumBrowserActivity extends ExpandableListActivity
        implements View.OnCreateContextMenuListener, MusicUtils.Defs
{
    private static final String TAG = "Artist/Album";
    private String mCurrentArtistId;
    private String mCurrentArtistName;
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    boolean mIsUnknownArtist;
    boolean mIsUnknownAlbum;
    private ArtistAlbumListAdapter mAdapter;
    private boolean mAdapterSent;
    private final static int SEARCH = CHILD_MENU_BASE;
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;

    /// M: Rescan delay time(ms)
    private static final long RESCAN_DELAY_TIME = 1000;
    /// M: It's the delay time(ms) for show emptyview.
    private int EMPTYSHOW_SPEND = 200;
    /// M: Is the sdcard mounted
    private boolean mIsMounted = true;

    /// M: We need to close submenu when sdcard is unmounted
    private SubMenu mSubMenu = null;

    /// M: Album cursor columns
    private final String[] mCursorCols = new String[] {
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
            /// M: add for chinese sorting
            MediaStore.Audio.Artists.ARTIST_PINYIN_KEY
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        MusicLogUtils.d(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (icicle != null) {
            mCurrentAlbumId = icicle.getString("selectedalbum");
            mCurrentAlbumName = icicle.getString("selectedalbumname");
            mCurrentArtistId = icicle.getString("selectedartist");
            mCurrentArtistName = icicle.getString("selectedartistname");
            /// M: ALPS00122166 @{
        } else {
            Intent intent = getIntent();
            mCurrentAlbumId = intent.getStringExtra("selectedalbum");
            mCurrentArtistId = intent.getStringExtra("selectedartist");
            /// @}
        }

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        /// M: add the ScanListener action @{
        f.addAction(Intent.ACTION_MEDIA_MOUNTED);
        /// @}
        f.addDataScheme("file");
        registerReceiver(mScanListener, f);

        setContentView(R.layout.media_picker_activity_expanding);
        ExpandableListView lv = getExpandableListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

        mAdapter = (ArtistAlbumListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            MusicLogUtils.v(TAG, "starting query");
            mAdapter = new ArtistAlbumListAdapter(
                    getApplication(),
                    this,
                    null, // cursor
                    R.layout.track_list_item_group,
                    new String[] {},
                    new int[] {},
                    R.layout.track_list_item_child,
                    new String[] {},
                    new int[] {});
            setListAdapter(mAdapter);
            setTitle(R.string.working_artists);
            getArtistCursor(mAdapter.getQueryHandler(), null);
        } else {
            mAdapter.setActivity(this);
            /// M: reload unknownArtist and unknownAlbum.
            mAdapter.reloadStringOnLocaleChanges();
            setListAdapter(mAdapter);
            mArtistCursor = mAdapter.getCursor();
            if (mArtistCursor != null) {
                init(mArtistCursor);
            } else {
                getArtistCursor(mAdapter.getQueryHandler(), null);
            }
        }
    }

    /*//TODO avoid build fail
    /// M: Add for test launch time --->
    private class PostDrawListener implements android.view.ViewTreeObserver.OnPostDrawListener {

        @Override
        public boolean onPostDraw() {
            // TODO Auto-generated method stub
            MusicLogUtils.i(TAG, "[AppLaunch] Music onPostDraw");
            return true;
        }
        
    }
    
    private PostDrawListener mPostDrawListener = new PostDrawListener();
    
    @Override
    public void onStart() {
        super.onStart();
        MusicLogUtils.d(TAG, "onStart");    
        getWindow().getDecorView().getViewTreeObserver().addOnPostDrawListener(mPostDrawListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        MusicLogUtils.d(TAG, "onStop");    
        getWindow().getDecorView().getViewTreeObserver().removeOnPostDrawListener(mPostDrawListener);
    }
    /// M: <--- Add for test launch time @}
*/   
    @Override
    public Object onRetainNonConfigurationInstance() {
        mAdapterSent = true;
        return mAdapter;
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("selectedalbum", mCurrentAlbumId);
        outcicle.putString("selectedalbumname", mCurrentAlbumName);
        outcicle.putString("selectedartist", mCurrentArtistId);
        outcicle.putString("selectedartistname", mCurrentArtistName);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        MusicLogUtils.d(TAG, "onDestroy");
        ExpandableListView lv = getExpandableListView();
        if (lv != null) {
            mLastListPosCourse = lv.getFirstVisiblePosition();
            View cv = lv.getChildAt(0);
            if (cv != null) {
                mLastListPosFine = cv.getTop();
            }
        }
        
        // If we have an adapter and didn't send it off to another activity yet, we should
        // close its cursor, which we do by assigning a null cursor to it. Doing this
        // instead of closing the cursor directly keeps the framework from accessing
        // the closed cursor later.
        if (!mAdapterSent && mAdapter != null) {
            mAdapter.changeCursor(null);
        }
        // Because we pass the adapter to the next activity, we need to make
        // sure it doesn't keep a reference to this activity. We can do this
        // by clearing its DatasetObservers, which setListAdapter(null) does.
        setListAdapter(null);
        mAdapter = null;
        unregisterReceiver(mScanListener);
        setListAdapter(null);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        MusicLogUtils.d(TAG, "onResume>>>");
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        registerReceiver(mTrackListListener, f);
        mTrackListListener.onReceive(null, null);
      /// M: when the listview's count be 0,show the emptyview. 
        reFreshEmptyView();
        MusicUtils.setSpinnerState(this);
        MusicLogUtils.d(TAG, "onResume<<<");
    }
    /**
     *  M: show the emptyview when the list's count be 0.
     */
    public void reFreshEmptyView() {
        if (getExpandableListView().getAdapter() != null
                && getExpandableListView().getAdapter().getCount() == 0
                && getExpandableListView().getCount() != 0) {
            mListHandler.sendEmptyMessageDelayed(0, EMPTYSHOW_SPEND);
        } else {
            MusicUtils.emptyShow(getExpandableListView(), ArtistAlbumBrowserActivity.this);
        }
    }

    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getExpandableListView().invalidateViews();
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /// M: get the action and current Storage State @{
            String action = intent.getAction();
            String status = Environment.getExternalStorageState();
            MusicLogUtils.d(TAG, "mScanListener.onReceive:" + action + ", status = " + status);

            /// M: Determine whether the action that get from intent match with
             // ACTION_MEDIA_SCANNER_STARTED or ACTION_MEDIA_SCANNER_FINISHED.
            if (Intent.ACTION_MEDIA_SCANNER_STARTED.equals(action) ||
                    Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                MusicUtils.setSpinnerState(ArtistAlbumBrowserActivity.this);
                mReScanHandler.sendEmptyMessage(0);
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                mIsMounted = false;
                mReScanHandler.sendEmptyMessage(0);
                closeContextMenu();
                closeOptionsMenu();
                if (mSubMenu != null) {
                    mSubMenu.close();
                }
                mReScanHandler.sendEmptyMessageDelayed(0, RESCAN_DELAY_TIME);
            } else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
            /// M: If SD card is nalmal mounted, send message to rescan database.
                mIsMounted = true;
                mReScanHandler.sendEmptyMessage(0);
            }
           /// @}
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (mAdapter != null) {
                getArtistCursor(mAdapter.getQueryHandler(), null);
            }
        }
    };
    /**
     *  M: show emptyview  when get the right list'count.
     */
    public Handler mListHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
                reFreshEmptyView();
            }
    };

    @Override
    public void onPause() {
        MusicLogUtils.d(TAG, "onPause");
        unregisterReceiver(mTrackListListener);
        mReScanHandler.removeCallbacksAndMessages(null);
        /// M: ALPS00122166 @{
        Intent intent = new Intent(getIntent());
        intent.putExtra("selectedalbum", mCurrentAlbumId);
        intent.putExtra("selectedartist", mCurrentArtistId);
        setIntent(intent);
        /// @}
        super.onPause();
    }
    
    public void init(Cursor c) {
        if (mAdapter == null) {
            return;
        }
        mAdapter.changeCursor(c); // also sets mArtistCursor

        if (mArtistCursor == null) {
            MusicLogUtils.d(TAG, "mArtistCursor is null");
            MusicUtils.displayDatabaseError(this,mIsMounted);
            closeContextMenu();
            mReScanHandler.sendEmptyMessageDelayed(0, RESCAN_DELAY_TIME);
            return;
        }
        /// M: when Scanning media file show scanning progressbar ,if files exist 
        //   show file list,or show no music @{ 
            MusicUtils.emptyShow(getExpandableListView(),ArtistAlbumBrowserActivity.this);
        /// @}
        // restore previous position
        if (mLastListPosCourse >= 0) {
            ExpandableListView elv = getExpandableListView();
            elv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
            mLastListPosCourse = -1;
        }

        MusicUtils.hideDatabaseError(this);
        setTitle();
    }

    private void setTitle() {
        setTitle(R.string.artists_title);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

        mCurrentAlbumId = Long.valueOf(id).toString();
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", mCurrentAlbumId);
        Cursor c = (Cursor) getExpandableListAdapter().getChild(groupPosition, childPosition);
        String album = c.getString(c.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
        if (album == null || album.equals(MediaStore.UNKNOWN_STRING)) {
            // unknown album, so we should include the artist ID to limit the songs to songs only by that artist
            mArtistCursor.moveToPosition(groupPosition);
            mCurrentArtistId = mArtistCursor.getString(mArtistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
            intent.putExtra("artist", mCurrentArtistId);
        }
        startActivity(intent);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
        mSubMenu = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, mSubMenu);
        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);
        ExpandableListContextMenuInfo mi = (ExpandableListContextMenuInfo) menuInfoIn;
        int itemtype = ExpandableListView.getPackedPositionType(mi.packedPosition);
        int gpos = ExpandableListView.getPackedPositionGroup(mi.packedPosition);
        int cpos = ExpandableListView.getPackedPositionChild(mi.packedPosition);
        if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            if (gpos == -1) {
                // this shouldn't happen
                MusicLogUtils.d(TAG, "no group");
                return;
            }
            gpos = gpos - getExpandableListView().getHeaderViewsCount();
            mArtistCursor.moveToPosition(gpos);
            mCurrentArtistId = mArtistCursor.getString(mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
            mCurrentArtistName = mArtistCursor.getString(mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            mCurrentAlbumId = null;
            mIsUnknownArtist = mCurrentArtistName == null ||
                    mCurrentArtistName.equals(MediaStore.UNKNOWN_STRING);
            mIsUnknownAlbum = true;
            if (mIsUnknownArtist) {
                menu.setHeaderTitle(getString(R.string.unknown_artist_name));
            } else {
                menu.setHeaderTitle(mCurrentArtistName);
                menu.add(0, SEARCH, 0, R.string.search_title);
            }
            return;
        } else if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            if (cpos == -1) {
                // this shouldn't happen
                MusicLogUtils.d(TAG, "no child");
                return;
            }
            Cursor c = (Cursor) getExpandableListAdapter().getChild(gpos, cpos);
            c.moveToPosition(cpos);
            mCurrentArtistId = null;
            mCurrentAlbumId = Long.valueOf(mi.id).toString();
            mCurrentAlbumName = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            gpos = gpos - getExpandableListView().getHeaderViewsCount();
            mArtistCursor.moveToPosition(gpos);
            mCurrentArtistNameForAlbum = mArtistCursor.getString(
                    mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            mIsUnknownArtist = mCurrentArtistNameForAlbum == null ||
                    mCurrentArtistNameForAlbum.equals(MediaStore.UNKNOWN_STRING);
            mIsUnknownAlbum = mCurrentAlbumName == null ||
                    mCurrentAlbumName.equals(MediaStore.UNKNOWN_STRING);
            if (mIsUnknownAlbum) {
                menu.setHeaderTitle(getString(R.string.unknown_album_name));
            } else {
                menu.setHeaderTitle(mCurrentAlbumName);
            }
            if (!mIsUnknownAlbum || !mIsUnknownArtist) {
                menu.add(0, SEARCH, 0, R.string.search_title);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play everything by the selected artist
                long [] list =
                    mCurrentArtistId != null ?
                    MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId))
                    : MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                        
                MusicUtils.playAll(this, list, 0);
                return true;
            }

            case QUEUE: {
                long [] list =
                    mCurrentArtistId != null ?
                    MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId))
                    : MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                MusicUtils.addToCurrentPlaylist(this, list);
                return true;
            }

            case NEW_PLAYLIST: {
                /// M: Return the parent activity if this view is an embedded child.
                Activity parent = getParent();
                Intent intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                /// M: Restore mCurrentArtistId or mCurrentAlbumId in CreatePlaylist, so that when onActivityResult called,
                /// it will return the restored value to avoid mCurrentAlbumId become null when activity has been restarted,
                /// such as switch a language. We use "_selectedartist" and "_selectedalbum" suffix to different artist id
                /// and album id. @{
                if (mCurrentArtistId != null) {
                    intent.putExtra(MusicUtils.ADD_TO_PLAYLIST_ITEM_ID, "selectedartist_" + mCurrentArtistId);
                } else if (mCurrentAlbumId != null) {
                    intent.putExtra(MusicUtils.ADD_TO_PLAYLIST_ITEM_ID, "selectedalbum_" + mCurrentAlbumId);
                }
                /// @}
                if (parent == null) {
                    startActivityForResult(intent, NEW_PLAYLIST);
                /// M: use the object of parent to new Playlist @{
                } else {
                    /// M: Restore start activity tab so that parent MusicBrowserActivity can return the activity result
                    /// to this start activity.
                    intent.putExtra(MusicUtils.START_ACTIVITY_TAB_ID, MusicBrowserActivity.ARTIST_INDEX);
                    parent.startActivityForResult(intent, NEW_PLAYLIST);
                }
                /// @}
                return true;
            }

            case PLAYLIST_SELECTED: {
                long [] list =
                    mCurrentArtistId != null ?
                    MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId))
                    : MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                long playlist = item.getIntent().getLongExtra("playlist", 0);
                MusicUtils.addToPlaylist(this, list, playlist);
                return true;
            }
            
            case DELETE_ITEM: {
                long [] list;
                Bundle b = new Bundle();
                /// M: Get string in DeleteItems Activity to get current language string. @{
                //String desc;
                if (mCurrentArtistId != null) {
                    list = MusicUtils.getSongListForArtist(this, Long.parseLong(mCurrentArtistId));
                    b.putInt(MusicUtils.DELETE_DESC_STRING_ID, R.string.delete_artist_desc);
                    b.putString(MusicUtils.DELETE_DESC_TRACK_INFO, mCurrentArtistName);
                    //String f;
                    //if (android.os.Environment.isExternalStorageRemovable()) {
                    //    f = getString(R.string.delete_artist_desc);
                    //} else {
                    //    f = getString(R.string.delete_artist_desc_nosdcard);
                    //}
                    //desc = String.format(f, mCurrentArtistName);
                } else {
                    list = MusicUtils.getSongListForAlbum(this, Long.parseLong(mCurrentAlbumId));
                    b.putInt(MusicUtils.DELETE_DESC_STRING_ID, R.string.delete_album_desc);
                    b.putString(MusicUtils.DELETE_DESC_TRACK_INFO, mCurrentAlbumName);
                    //String f;
                    //if (android.os.Environment.isExternalStorageRemovable()) {
                    //    f = getString(R.string.delete_album_desc);
                    //} else {
                    //    f = getString(R.string.delete_album_desc_nosdcard);
                    // }
                    //desc = String.format(f, mCurrentAlbumName);
                }
                //b.putString("description", desc);
                /// @}
                b.putLongArray("items", list);
                Intent intent = new Intent();
                intent.setClass(this, DeleteItems.class);
                intent.putExtras(b);
                startActivityForResult(intent, -1);
                return true;
            }

            case SEARCH:
                doSearch();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    void doSearch() {
        CharSequence title = null;
        String query = null;
        
        Intent i = new Intent();
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        if (mCurrentArtistId != null) {
            title = mCurrentArtistName;
            query = mCurrentArtistName;
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistName);
            i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE);
        } else {
            if (mIsUnknownAlbum) {
                title = query = mCurrentArtistNameForAlbum;
            } else {
                title = query = mCurrentAlbumName;
                if (!mIsUnknownArtist) {
                    query = query + " " + mCurrentArtistNameForAlbum;
                }
            }
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
            i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
        }
        title = getString(R.string.mediasearch, title);
        i.putExtra(SearchManager.QUERY, query);

        startActivity(Intent.createChooser(i, title));
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_DONE:
                if (resultCode == RESULT_CANCELED) {
                    finish();
                } else {
                    getArtistCursor(mAdapter.getQueryHandler(), null);
                }
                break;

            case NEW_PLAYLIST:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    /// M: Get selected album id need add to new playlist from intent to avoid mCurrentAlbumId to be null
                    /// when activity restart causing by configaration change. @{
                    String selectItemString = intent.getStringExtra(MusicUtils.ADD_TO_PLAYLIST_ITEM_ID);
                    if (uri != null && selectItemString != null) {
                        long [] list = null;
                        String selectItemId = selectItemString.substring(selectItemString.lastIndexOf("_") + 1);
                        MusicLogUtils.d(TAG, "onActivityResult: selectItemId = " + selectItemId);
                        /// Get playlist for artist or album different by given type.
                        if (selectItemString.startsWith("selectedartist")) {
                            list = MusicUtils.getSongListForArtist(this, Long.parseLong(selectItemId));
                        } else if (selectItemString.startsWith("selectedalbum")) {
                            list = MusicUtils.getSongListForAlbum(this, Long.parseLong(selectItemId));
                        }
                        MusicUtils.addToPlaylist(this, list, Long.parseLong(uri.getLastPathSegment()));
                    }
                    /// @}
                }
                break;
        }
    }

    private Cursor getArtistCursor(AsyncQueryHandler async, String filter) {
        /// M: add for chinese sorting
        String sortOrder = MediaStore.Audio.Artists.ARTIST_PINYIN_KEY;
        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        if (!TextUtils.isEmpty(filter)) {
            uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
        }

        Cursor ret = null;
        if (async != null) {
            async.startQuery(0, null, uri,
                    mCursorCols, null , null, sortOrder);
        } else {
            ret = MusicUtils.query(this, uri,
                    mCursorCols, null , null, sortOrder);
        }
        return ret;
    }

    static class ArtistAlbumListAdapter extends SimpleCursorTreeAdapter implements SectionIndexer {

        private final BitmapDrawable mDefaultAlbumIcon;
        private int mGroupArtistIdIdx;
        private int mGroupArtistIdx;
        private int mGroupAlbumIdx;
        private int mGroupSongIdx;
        private final Context mContext;
        private final Resources mResources;
        private final String mAlbumSongSeparator;
        private String mUnknownAlbum;
        private String mUnknownArtist;
        private final StringBuilder mBuffer = new StringBuilder();
        private final Object[] mFormatArgs = new Object[1];
        private final Object[] mFormatArgs3 = new Object[3];
        private MusicAlphabetIndexer mIndexer;
        private ArtistAlbumBrowserActivity mActivity;
        private AsyncQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        /// M: add for chinese sorting
        private int mGroupArtistPinyinIdx;
        static class ViewHolder {
            TextView line1;
            TextView line2;
            ImageView play_indicator;
            ImageView icon;
        }

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }
            
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
              /// M: Log out query result
                MusicLogUtils.i(TAG, "query complete: "
                        + (cursor == null ? null : cursor.getCount()));
                mActivity.init(cursor);
            }
        }

        ArtistAlbumListAdapter(Context context, ArtistAlbumBrowserActivity currentactivity,
                Cursor cursor, int glayout, String[] gfrom, int[] gto, 
                int clayout, String[] cfrom, int[] cto) {
            super(context, cursor, glayout, gfrom, gto, clayout, cfrom, cto);
            mActivity = currentactivity;
            mQueryHandler = new QueryHandler(context.getContentResolver());

            Resources r = context.getResources();
            mDefaultAlbumIcon = (BitmapDrawable) r.getDrawable(R.drawable.albumart_mp_unknown_list);
            // no filter or dither, it's a lot faster and we can't tell the difference
            mDefaultAlbumIcon.setFilterBitmap(false);
            mDefaultAlbumIcon.setDither(false);
            
            mContext = context;
            getColumnIndices(cursor);
            mResources = context.getResources();
            mAlbumSongSeparator = context.getString(R.string.albumsongseparator);
            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mGroupArtistIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
                mGroupArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
                /// M: add for chinese sorting
                mGroupArtistPinyinIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST_PINYIN_KEY);
                mGroupAlbumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
                mGroupSongIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
                if (mIndexer != null) {
                    mIndexer.setCursor(cursor);
                } else {
                    /// M: add for chinese sorting
                    mIndexer = new MusicAlphabetIndexer(cursor, mGroupArtistPinyinIdx, 
                            mResources.getString(R.string.fast_scroll_alphabet));
                }
            }
        }

        public void setActivity(ArtistAlbumBrowserActivity newactivity) {
            mActivity = newactivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return mQueryHandler;
        }

        @Override
        public View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
            View v = super.newGroupView(context, cursor, isExpanded, parent);
            ImageView iv = (ImageView) v.findViewById(R.id.icon);
            ViewGroup.LayoutParams p = iv.getLayoutParams();
            p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.icon = (ImageView) v.findViewById(R.id.icon);
            vh.icon.setPadding(0, 0, 1, 0);
            v.setTag(vh);
            return v;
        }

        @Override
        public View newChildView(Context context, Cursor cursor, boolean isLastChild,
                ViewGroup parent) {
            View v = super.newChildView(context, cursor, isLastChild, parent);
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.icon = (ImageView) v.findViewById(R.id.icon);
            vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
            vh.icon.setPadding(0, 0, 1, 0);
            v.setTag(vh);
            return v;
        }
        
        @Override
        public void bindGroupView(View view, Context context, Cursor cursor, boolean isexpanded) {

            ViewHolder vh = (ViewHolder) view.getTag();

            String artist = cursor.getString(mGroupArtistIdx);
            String displayartist = artist;
            boolean unknown = artist == null || artist.equals(MediaStore.UNKNOWN_STRING);
            if (unknown) {
                displayartist = mUnknownArtist;
            }
            vh.line1.setText(displayartist);

            int numalbums = cursor.getInt(mGroupAlbumIdx);
            int numsongs = cursor.getInt(mGroupSongIdx);
            
            String songs_albums = MusicUtils.makeAlbumsLabel(context,
                    numalbums, numsongs, unknown);
            
            vh.line2.setText(songs_albums);
            
            long currentartistid = MusicUtils.getCurrentArtistId();
            long artistid = cursor.getLong(mGroupArtistIdIdx);
            if (currentartistid == artistid && !isexpanded) {
                vh.play_indicator.setVisibility(View.VISIBLE);
            } else {
                vh.play_indicator.setVisibility(View.GONE);
            }
        }

        @Override
        public void bindChildView(View view, Context context, Cursor cursor, boolean islast) {

            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            String displayname = name;
            boolean unknown = name == null || name.equals(MediaStore.UNKNOWN_STRING); 
            if (unknown) {
                displayname = mUnknownAlbum;
            }
            vh.line1.setText(displayname);

            int numsongs = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
            int numartistsongs = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST));

            final StringBuilder builder = mBuffer;
            builder.delete(0, builder.length());
            if (unknown) {
                numsongs = numartistsongs;
            }
              
            if (numsongs == 1) {
                builder.append(context.getString(R.string.onesong));
            } else {
                if (numsongs == numartistsongs) {
                    final Object[] args = mFormatArgs;
                    args[0] = numsongs;
                    builder.append(mResources.getQuantityString(R.plurals.Nsongs, numsongs, args));
                } else {
                    final Object[] args = mFormatArgs3;
                    args[0] = numsongs;
                    args[1] = numartistsongs;
                    args[2] = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
                    builder.append(mResources.getQuantityString(R.plurals.Nsongscomp, numsongs, args));
                }
            }
            vh.line2.setText(builder.toString());
            
            ImageView iv = vh.icon;
            // We don't actually need the path to the thumbnail file,
            // we just use it to see if there is album art or not
            String art = cursor.getString(cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Albums.ALBUM_ART));
            if (unknown || art == null || art.length() == 0) {
                iv.setBackgroundDrawable(mDefaultAlbumIcon);
                iv.setImageDrawable(null);
            } else {
                long artIndex = cursor.getLong(0);
                Drawable d = MusicUtils.getCachedArtwork(context, artIndex, mDefaultAlbumIcon);
                iv.setImageDrawable(d);
            }

            long currentalbumid = MusicUtils.getCurrentAlbumId();
            long aid = cursor.getLong(0);
            iv = vh.play_indicator;
            if (currentalbumid == aid) {
                iv.setVisibility(View.VISIBLE);
            } else {
                iv.setVisibility(View.GONE);
            }
        }

        
        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            
            long id = groupCursor.getLong(groupCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
            
            String[] cols = new String[] {
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
                    MediaStore.Audio.Albums.ALBUM_ART
            };
            /// M: add for chinese sorting
            Cursor c = MusicUtils.query(mActivity,
                    MediaStore.Audio.Artists.Albums.getContentUri("external", id),
                    cols, null, null, MediaStore.Audio.Albums.ALBUM_PINYIN_KEY);
            
            class MyCursorWrapper extends CursorWrapper {
                String mArtistName;
                int mMagicColumnIdx;
                MyCursorWrapper(Cursor c, String artist) {
                    super(c);
                    mArtistName = artist;
                    if (mArtistName == null || mArtistName.equals(MediaStore.UNKNOWN_STRING)) {
                        mArtistName = mUnknownArtist;
                    }
                    mMagicColumnIdx = c.getColumnCount();
                }
                
                @Override
                public String getString(int columnIndex) {
                    if (columnIndex != mMagicColumnIdx) {
                        return super.getString(columnIndex);
                    }
                    return mArtistName;
                }
                
                @Override
                public int getColumnIndexOrThrow(String name) {
                    if (MediaStore.Audio.Albums.ARTIST.equals(name)) {
                        return mMagicColumnIdx;
                    }
                    return super.getColumnIndexOrThrow(name); 
                }
                
                @Override
                public String getColumnName(int idx) {
                    if (idx != mMagicColumnIdx) {
                        return super.getColumnName(idx);
                    }
                    return MediaStore.Audio.Albums.ARTIST;
                }
                
                @Override
                public int getColumnCount() {
                    return super.getColumnCount() + 1;
                }
            }
            /// M:if c be null when the database be clear due to the sdcard unmounted,return null 
            /// to avoid Nullpoint JE.
            if (c == null) {
                return null;
            }
            return new MyCursorWrapper(c, groupCursor.getString(mGroupArtistIdx));
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != mActivity.mArtistCursor) {
                mActivity.mArtistCursor = cursor;
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (mConstraintIsValid && (
                    (s == null && mConstraint == null) ||
                    (s != null && s.equals(mConstraint)))) {
                return getCursor();
            }
            Cursor c = mActivity.getArtistCursor(null, s);
            mConstraint = s;
            mConstraintIsValid = true;
            return c;
        }

        /**
         * M: get the Artist Item position.
         * 
         * @param position
         */
        private int getGroupPositon(int position) {
            int expandingChildCount = 0;
            int groupCount = getGroupCount();
            ExpandableListView listview = mActivity.getExpandableListView();
            if (listview == null) {
                MusicLogUtils.e(TAG, "getGroupPositon with ExpandableListView is null");
                return 0;
            }

            for (int i = 0; i < groupCount; i++) {
                if (listview.isGroupExpanded(i)) {
                    int temp = getChildrenCount(i);
                    expandingChildCount += temp;
                }
                if (position - expandingChildCount <= i) {
                    return i;
                }
            }
            return groupCount - 1;
        }
            
        public Object[] getSections() {
            return mIndexer.getSections();
        }
        
        public int getPositionForSection(int sectionIndex) {
            /// M: if the mIndexer is valid,we could get the Position.
            if (mIndexer != null) {
                return mIndexer.getPositionForSection(sectionIndex);
            }
            /// M: default return back 0.
            return 0;
        }
        
        public int getSectionForPosition(int position) {
            /// M: if the mIndexer is valid,we could get the Position.
            if (mIndexer != null) {
                return mIndexer.getSectionForPosition(getGroupPositon(position));
            }
            /// default return back 0.
            return 0;
        }
        
        /**
         * M: when switch language ,call this function to reload unknownArtist and unknownAlbum.
         */
        public void reloadStringOnLocaleChanges() {
            String sUnknownArtist = mActivity.getString(R.string.unknown_artist_name);
            String sUnknownAlbum = mActivity.getString(R.string.unknown_album_name);
            if (mUnknownArtist != null && !mUnknownArtist.equals(sUnknownArtist)) {
                mUnknownArtist = sUnknownArtist;
            }
            if (mUnknownAlbum != null && !mUnknownAlbum.equals(sUnknownAlbum)) {
                mUnknownAlbum = sUnknownAlbum;
            }
        }
    }

    private Cursor mArtistCursor;
    
}

