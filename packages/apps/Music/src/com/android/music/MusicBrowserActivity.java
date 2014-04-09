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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.app.SearchManager;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;

import com.mediatek.music.ext.Extensions;
import com.mediatek.music.ext.IMusicTrackBrowser;

public class MusicBrowserActivity extends TabActivity implements MusicUtils.Defs, ServiceConnection, OnTabChangeListener,
        ViewPager.OnPageChangeListener {
    private static final String TAG = "MusicBrowser";

    private static final String ARTIST = "Artist";
    private static final String ALBUM = "Album";
    private static final String SONG = "Song";
    private static final String PLAYLIST = "Playlist";
    private static final String PLAYBACK = "Playback";
    private static final String SAVE_TAB = "activetab";
    static final int ARTIST_INDEX = 0;
    static final int ALBUM_INDEX = 1;
    static final int SONG_INDEX = 2;
    static final int PLAYLIST_INDEX = 3;
    static final int PLAYBACK_INDEX = 4;
    private static final int PLAY_ALL = CHILD_MENU_BASE + 3;

    private static final HashMap<String, Integer> TAB_MAP = new HashMap<String, Integer>(PLAYBACK_INDEX + 1);
    private LocalActivityManager mActivityManager;
    private ViewPager mViewPager;
    private TabHost mTabHost;
    private ArrayList<View> mPagers = new ArrayList<View>(PLAYBACK_INDEX);
    private int mTabCount;
    private int mCurrentTab;
    private MusicUtils.ServiceToken mToken;
    private IMediaPlaybackService mService = null;
    private int mOrientaiton;

    /// M: FakeMenu mFakeMenu;
    private View mOverflowMenuButton;
    private PopupMenu mPopupMenu = null;
    private boolean mPopupMenuShowing = false;
    private boolean mHasMenukey = true;
    private int mOverflowMenuButtonId;

    /// M: Whether sdcard is mounted
    private boolean mIsSdcardMounted = true;

    /// M: Add search button in actionbar when nowplaying not exist
    MenuItem mSearchItem;
    ImageButton mSearchButton;

    /// M: Initial tab map hashmap
    static {
        TAB_MAP.put(ARTIST, ARTIST_INDEX);
        TAB_MAP.put(ALBUM, ALBUM_INDEX);
        TAB_MAP.put(SONG, SONG_INDEX);
        TAB_MAP.put(PLAYLIST, PLAYLIST_INDEX);
        TAB_MAP.put(PLAYBACK, PLAYBACK_INDEX);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MusicLogUtils.d(TAG, "onCreate");
        setContentView(R.layout.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mToken = MusicUtils.bindToService(this, this);
        mHasMenukey = ViewConfiguration.get(this).hasPermanentMenuKey();
        mActivityManager = new LocalActivityManager(this, false);
        mActivityManager.dispatchCreate(savedInstanceState);

        mTabHost = getTabHost();
        initTab();
        mCurrentTab = MusicUtils.getIntPref(this, SAVE_TAB, ARTIST_INDEX);
        MusicLogUtils.d(TAG, "onCreate mCurrentTab: " + mCurrentTab);
        if ((mCurrentTab < 0) || (mCurrentTab >= mTabCount)) {
            mCurrentTab = ARTIST_INDEX;
        }
        /// M: reset the defalt tab value
        if (mCurrentTab == ARTIST_INDEX) {
            mTabHost.setCurrentTab(ALBUM_INDEX);
        }
        mTabHost.setOnTabChangedListener(this);

        initPager();
        mViewPager = (ViewPager) findViewById(R.id.viewpage);
        mViewPager.setAdapter(new MusicPagerAdapter());
        mViewPager.setOnPageChangeListener(this);

        IntentFilter f = new IntentFilter();
        f.addAction(MusicUtils.SDCARD_STATUS_UPDATE);
        registerReceiver(mSdcardstatustListener, f);

        createFakeMenu();

        /// M: Init search button click listener in nowplaying.
        initSearchButton();
    }

    @Override
    public void onResume() {
        super.onResume();
        MusicLogUtils.d(TAG, "onResume>>>");
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        registerReceiver(mTrackListListener, f);
        mTabHost.setCurrentTab(mCurrentTab);
        mActivityManager.dispatchResume();
        MusicLogUtils.d(TAG, "onResume<<<");
    }

    @Override
    public void onPause() {
        MusicLogUtils.d(TAG, "onPause");
        unregisterReceiver(mTrackListListener);
        mActivityManager.dispatchPause(false);
        MusicUtils.setIntPref(this, SAVE_TAB, mCurrentTab);
        super.onPause();
    }

    @Override
    public void onStop() {
        mActivityManager.dispatchStop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        MusicLogUtils.d(TAG, "onDestroy");
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
            mService = null;
        }
        unregisterReceiver(mSdcardstatustListener);
        mActivityManager.dispatchDestroy(false);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /// M: Get the start activity tab index from intent which set by start activity, so that we can return
        /// result to right activity.
        int startActivityTab = mCurrentTab;
        if (data != null) {
            startActivityTab = data.getIntExtra(MusicUtils.START_ACTIVITY_TAB_ID, mCurrentTab);
        }
        MusicLogUtils.d(TAG, "onActivityResult: startActivityTab = " + startActivityTab);
        Activity startActivity = mActivityManager.getActivity(getStringId(startActivityTab));
        if (startActivity == null) {
            return;
        }
        switch (startActivityTab) {
            case ARTIST_INDEX:
                ((ArtistAlbumBrowserActivity) startActivity).onActivityResult(requestCode, resultCode, data);
                break;

            case ALBUM_INDEX:
                ((AlbumBrowserActivity) startActivity).onActivityResult(requestCode, resultCode, data);
                break;

            case SONG_INDEX:
                ((TrackBrowserActivity) startActivity).onActivityResult(requestCode, resultCode, data);
                break;

            case PLAYLIST_INDEX:
                ((PlaylistBrowserActivity) startActivity).onActivityResult(requestCode, resultCode, data);
                break;

            default:
                MusicLogUtils.d(TAG, "default");
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MusicLogUtils.d(TAG, "onConfigurationChanged>>");
        TabWidget tabWidgetTemp = mTabHost.getTabWidget();
        View tabView;
        Activity activity;
        int viewStatusForTab = View.GONE;

        mOrientaiton = newConfig.orientation;
        if (mOrientaiton == Configuration.ORIENTATION_LANDSCAPE) {
            MusicLogUtils.d(TAG, "onConfigurationChanged--LandScape");
            viewStatusForTab = View.VISIBLE;
        }
        if (mService != null) {
            MusicUtils.updateNowPlaying(MusicBrowserActivity.this, mOrientaiton);
            updatePlaybackTab();
        }
        /// M: load tab which is alive only for Landscape;
        for (int i = PLAYBACK_INDEX; i < mTabCount; i++) {
            tabView = tabWidgetTemp.getChildTabViewAt(i);
            if (tabView != null) {
                tabView.setVisibility(viewStatusForTab);
            }
        }
        /// M: notify sub Activity for configuration changed;
        for (int i = 0; i < PLAYBACK_INDEX; i++) {
            activity = mActivityManager.getActivity(getStringId(i));
            if (activity != null) {
                activity.onConfigurationChanged(newConfig);
            }
        }

        if (!mHasMenukey) {
            boolean popupMenuShowing = mPopupMenuShowing;
            if (popupMenuShowing && mPopupMenu != null) {
                mPopupMenu.dismiss();
                MusicLogUtils.d(TAG, "changeFakeMenu:mPopupMenu.dismiss()");
            }
            MusicLogUtils.d(TAG, "changeFakeMenu:popupMenuShowing=" + popupMenuShowing);
            createFakeMenu();
            if (popupMenuShowing && mOverflowMenuButton != null) {
                mOverflowMenuButton.performClick();
                MusicLogUtils.d(TAG, "changeFakeMenu:performClick()");
            }
        }
        MusicLogUtils.d(TAG, "onConfigurationChanged<<");
    }

    /**
     * M: Create fake menu.
     */
    private void createFakeMenu() {
        if (mHasMenukey) {
            MusicLogUtils.d(TAG, "createFakeMenu Quit when there has Menu Key");
            return;
        }
        if (mOrientaiton == Configuration.ORIENTATION_LANDSCAPE) {
            mOverflowMenuButtonId = R.id.overflow_menu;
            mOverflowMenuButton = findViewById(R.id.overflow_menu);
        } else {
            mOverflowMenuButtonId = R.id.overflow_menu_nowplaying;
            mOverflowMenuButton = findViewById(R.id.overflow_menu_nowplaying);
            View parent = (View) mOverflowMenuButton.getParent();
            if (parent != null) {
                parent.setVisibility(View.VISIBLE);
            }
        }
        mOverflowMenuButton.setVisibility(View.VISIBLE);
        mOverflowMenuButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MusicLogUtils.d(TAG, "createFakeMenu:onClick()");
                if (v.getId() == mOverflowMenuButtonId) {
                    final PopupMenu popupMenu = new PopupMenu(MusicBrowserActivity.this, mOverflowMenuButton);
                    mPopupMenu = popupMenu;
                    final Menu menu = popupMenu.getMenu();
                    onCreateOptionsMenu(menu);
                    popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            return onOptionsItemSelected(item);
                        }
                    });
                    popupMenu.setOnDismissListener(new OnDismissListener() {
                        public void onDismiss(PopupMenu menu) {
                            mPopupMenuShowing = false;
                            MusicLogUtils.d(TAG, "createFakeMenu:onDismiss() called");
                            return;
                        }
                    });
                    onPrepareOptionsMenu(menu);
                    mPopupMenuShowing = true;
                    if (popupMenu != null) {
                        MusicLogUtils.d(TAG, "createFakeMenu:popupMenu.show()");
                        popupMenu.show();
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, PLAY_ALL, 0, R.string.play_all);
        menu.add(0, PARTY_SHUFFLE, 0, R.string.party_shuffle);
        menu.add(0, SHUFFLE_ALL, 0, R.string.shuffle_all);
        menu.add(0, EFFECTS_PANEL, 0, R.string.effects_list_title);
        /// M: Add search view
        mSearchItem = MusicUtils.addSearchView(this, menu, mQueryTextListener);
        /// M: Add folder to play
        menu.add(0, TrackBrowserActivity.ADD_FOLDER_TO_PLAY, 0, R.string.add_folder_to_play);
        /// M: Add folder as a playlist
        menu.add(0, PlaylistBrowserActivity.ADD_FOLDER_AS_PLAYLIST, 0, R.string.add_folder_as_playlist);
        /// M: add a file from filemanager to play all audio in the same folder.
        menu.add(0, TrackBrowserActivity.ADD_SONG_TO_PLAY, 0, R.string.add_song_to_play);
        return true;
    }

    /**
     * M: When edit Text ,do query follow the message of the query
     */
    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        public boolean onQueryTextSubmit(String query) {
            Intent intent = new Intent();
            intent.setClass(MusicBrowserActivity.this, QueryBrowserActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            return true;
        }

        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MusicUtils.setPartyShuffleMenuIcon(menu);
        super.onPrepareOptionsMenu(menu);
        if (!mIsSdcardMounted) {
            MusicLogUtils.w(TAG, "Sdcard is not mounted, don't show option menu!");
            return false;
        }
        /// M: Only show play all in song activity.
        menu.findItem(PLAY_ALL).setVisible(mCurrentTab == SONG_INDEX);
        /// M: Show shuffle all in all activity except playlist activity.
        menu.findItem(SHUFFLE_ALL).setVisible(mCurrentTab != PLAYLIST_INDEX);
        /// M: Only show effect menu when effect class is enable.
        MusicUtils.setEffectPanelMenu(getApplicationContext(), menu);
        /// M: Search button can only show on one of place between nowplaying and action bar, when action bar exist,
        /// it should show on action bar, otherwise show on nowplaying, if nowplaying not exist(such as landscape in
        /// MusicBrowserActivity), show it in option menu.
        mSearchItem.setVisible(mOrientaiton == Configuration.ORIENTATION_LANDSCAPE);
        /// M: Use plugin to enable need feature. {@
        IMusicTrackBrowser musicPlugin = Extensions.getPluginObject(getApplicationContext());
        /// Only show add folder to play in song activity
        menu.findItem(TrackBrowserActivity.ADD_FOLDER_TO_PLAY).setVisible(
                mCurrentTab == SONG_INDEX && musicPlugin.enableAddFolderToPlayMenu());
        /// Only show add folder as playlist in playlist activity
        menu.findItem(PlaylistBrowserActivity.ADD_FOLDER_AS_PLAYLIST).setVisible(
                mCurrentTab == PLAYLIST_INDEX && musicPlugin.enableAddFolderAsPlaylistMenu());
        /// Only show add folder to play in song activity
        menu.findItem(TrackBrowserActivity.ADD_SONG_TO_PLAY).setVisible(
                mCurrentTab == SONG_INDEX && musicPlugin.enableAddSongToPlayMenu());
        /// @}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Cursor cursor;
        Intent intent;
        switch (item.getItemId()) {
            case PLAY_ALL:
                cursor = MusicUtils.query(this,
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Media._ID },
                        MediaStore.Audio.Media.IS_MUSIC + "=1",
                        null,
                        /// M: add for chinese sorting
                        MediaStore.Audio.Media.TITLE_PINYIN_KEY);
                if (cursor != null) {
                    MusicUtils.playAll(this, cursor);
                    cursor.close();
                }
                return true;

            case PARTY_SHUFFLE:
                MusicUtils.togglePartyShuffle();
                return true;

            case SHUFFLE_ALL:
                cursor = MusicUtils.query(this,
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Media._ID },
                        MediaStore.Audio.Media.IS_MUSIC + "=1",
                        null,
                        /// M: add for chinese sorting
                        MediaStore.Audio.Media.TITLE_PINYIN_KEY);
                if (cursor != null) {
                    MusicUtils.shuffleAll(this, cursor);
                    cursor.close();
                }
                return true;

            case EFFECTS_PANEL:
                return MusicUtils.startEffectPanel(this);

            case R.id.search:
                onSearchRequested();
                return true;

            case TrackBrowserActivity.ADD_FOLDER_TO_PLAY:
                intent = new Intent("com.mediatek.filemanager.DOWNLOAD_LOCATION");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                this.startActivityForResult(intent, TrackBrowserActivity.ADD_FOLDER_TO_PLAY);
                return true;

            case PlaylistBrowserActivity.ADD_FOLDER_AS_PLAYLIST:
                intent = new Intent("com.mediatek.filemanager.DOWNLOAD_LOCATION");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                this.startActivityForResult(intent, PlaylistBrowserActivity.ADD_FOLDER_AS_PLAYLIST);
                return true;

            case TrackBrowserActivity.ADD_SONG_TO_PLAY:
                intent = new Intent("com.mediatek.filemanager.ADD_FILE");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                this.startActivityForResult(intent, TrackBrowserActivity.ADD_SONG_TO_PLAY);
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * M: Implements receive track ListListener broadcast
     */
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicLogUtils.d(TAG, "mTrackListListener");
            if (mService != null) {
                MusicUtils.updateNowPlaying(MusicBrowserActivity.this, mOrientaiton);
                updatePlaybackTab();
            }
        }
    };

    /**
     * M: Implements receive SDCard status broadcast
     */
    private BroadcastReceiver mSdcardstatustListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsSdcardMounted = intent.getBooleanExtra(MusicUtils.SDCARD_STATUS_ONOFF, false);

            View view;
            if (mIsSdcardMounted) {
                MusicLogUtils.d(TAG, "Sdcard normal");
                view = findViewById(R.id.normal_view);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
                view = findViewById(R.id.sd_message);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
                view = findViewById(R.id.sd_icon);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
                view = findViewById(R.id.sd_error);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
                /// M: update nowplaying when sdcard mounted
                if (mService != null) {
                    MusicUtils.updateNowPlaying(MusicBrowserActivity.this, mOrientaiton);
                }
            } else {
                MusicLogUtils.d(TAG, "Sdcard error");
                view = findViewById(R.id.normal_view);
                if (view != null) {
                    view.setVisibility(View.GONE);
                }
                view = findViewById(R.id.sd_icon);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
                TextView testview = (TextView) findViewById(R.id.sd_message);
                if (testview != null) {
                    testview.setVisibility(View.VISIBLE);
                    int message = intent.getIntExtra(MusicUtils.SDCARD_STATUS_MESSAGE, R.string.sdcard_error_message);
                    testview.setText(message);
                }
                view = findViewById(R.id.sd_error);
                if (view != null) {
                    view.setVisibility(View.VISIBLE);
                }
            }
        }
    };

    /**
     * get current tab id though index
     * 
     * @param index
     * @return
     */
    private String getStringId(int index) {
        String tabStr = ARTIST;
        switch (index) {
            case ALBUM_INDEX:
                tabStr = ALBUM;
                break;
            case SONG_INDEX:
                tabStr = SONG;
                break;
            case PLAYLIST_INDEX:
                tabStr = PLAYLIST;
                break;
            case PLAYBACK_INDEX:
                tabStr = PLAYBACK;
                break;
            case ARTIST_INDEX:
            default:
                MusicLogUtils.d(TAG, "ARTIST_INDEX or default");
                break;
        }
        return tabStr;
    }

    /**
     * initial tab host
     */
    private void initTab() {
        MusicLogUtils.d(TAG, "initTab>>");
        final TabWidget tabWidget = (TabWidget) getLayoutInflater().inflate(R.layout.buttonbar, null);
        mOrientaiton = getResources().getConfiguration().orientation;
        mTabCount = tabWidget.getChildCount();
        View tabView;
        /// M:remove fake menu
        if (mHasMenukey) {
            mTabCount--;
        }
        for (int i = 0; i < mTabCount; i++) {
            tabView = tabWidget.getChildAt(0);
            if (tabView != null) {
                tabWidget.removeView(tabView);
            }
            MusicLogUtils.d(TAG, "addTab:" + i);
            mTabHost.addTab(mTabHost.newTabSpec(getStringId(i)).setIndicator(tabView).setContent(android.R.id.tabcontent));
        }
        if (mOrientaiton == Configuration.ORIENTATION_PORTRAIT) {
            TabWidget tabWidgetTemp = mTabHost.getTabWidget();
            for (int i = PLAYBACK_INDEX; i < mTabCount; i++) {
                tabView = tabWidgetTemp.getChildTabViewAt(i);
                if (tabView != null) {
                    tabView.setVisibility(View.GONE);
                }
                MusicLogUtils.d(TAG, "set tab gone:" + i);
            }
        }
        MusicLogUtils.d(TAG, "initTab<<");
    }

    /**
     * get current view
     * 
     * @param index
     * @return View
     */
    private View getView(int index) {
        MusicLogUtils.d(TAG, "getView>>>index = " + index);
        View view = null;
        Intent intent = new Intent(Intent.ACTION_PICK);
        switch (index) {
            case ARTIST_INDEX:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/artistalbum");
                break;
            case ALBUM_INDEX:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
                break;
            case SONG_INDEX:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
                break;
            case PLAYLIST_INDEX:
                intent.setDataAndType(Uri.EMPTY, MediaStore.Audio.Playlists.CONTENT_TYPE);
                break;
            default:
                MusicLogUtils.d(TAG, "default");
                return null;
        }
        intent.putExtra("withtabs", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        view = mActivityManager.startActivity(getStringId(index), intent).getDecorView();
        MusicLogUtils.d(TAG, "getView<<<");
        return view;
    }

    /**
     * initial view pager
     */
    private void initPager() {
        mPagers.clear();
        View view = null;
        for (int i = 0; i <= PLAYLIST_INDEX; i++) {
            view = (i == mCurrentTab) ? getView(i) : null;
            mPagers.add(view);
        }
    }

    /**
     * update play back tab info
     */
    private void updatePlaybackTab() {
        final int drawalbeTopPostion = 1;
        final int opaqueFull = 255; // 100%
        final int opaqueHalf = 128; // 50%
        TabWidget tabWidgetTemp = mTabHost.getTabWidget();
        TextView tabView = (TextView) tabWidgetTemp.getChildTabViewAt(PLAYBACK_INDEX);
        boolean enable = true;
        long id = -1;
        Drawable[] drawables;
        Drawable drawableTop = null;
        int drawableTopAlpha = opaqueFull;

        if (tabView == null) {
            return;
        }
        try {
            if (mService != null) {
                id = mService.getAudioId();
            }
        }
        catch (RemoteException ex) {
            MusicLogUtils.e(TAG, "updatePlaybackTab getAudioId remote excption:" + ex);
        }
        if (id == -1) {
            enable = false;
            drawableTopAlpha = opaqueHalf;
        }
        tabView.setEnabled(enable);
        drawables = tabView.getCompoundDrawables();
        drawableTop = drawables[drawalbeTopPostion];
        if (drawableTop != null) {
            drawableTop.setAlpha(drawableTopAlpha);
        }
        MusicLogUtils.d(TAG, "updatePlaybackTab:" + enable);
    }

    /**
     * for service connect
     */
    public void onServiceConnected(ComponentName className, IBinder service) {
        mService = IMediaPlaybackService.Stub.asInterface(service);
        String shuf = getIntent().getStringExtra("autoshuffle");
        if (mService != null) {
            if (Boolean.valueOf(shuf).booleanValue()) {
                try {
                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_AUTO);
                }
                catch (RemoteException ex) {
                    MusicLogUtils.e(TAG, "onServiceConnected setShuffleMode remote excption:" + ex);
                }
            }
            MusicUtils.updateNowPlaying(MusicBrowserActivity.this, mOrientaiton);
            updatePlaybackTab();
        }
    }

    public void onServiceDisconnected(ComponentName className) {
        mService = null;
        finish();
    }

    /**
     * OnTabChangeListener for TabHost
     * 
     * @param tabId
     */
    public void onTabChanged(String tabId) {
        int tabIndex = TAB_MAP.get(tabId);
        MusicLogUtils.d(TAG, "onTabChanged-tabId:" + tabId);
        // MusicLogUtils.d(TAG, "onTabChanged-tabIndex:" + tabIndex);
        if ((tabIndex >= ARTIST_INDEX) && (tabIndex <= PLAYLIST_INDEX)) {
            mViewPager.setCurrentItem(tabIndex);
            mCurrentTab = tabIndex;
        } else if (tabIndex == PLAYBACK_INDEX) {
            Intent intent = new Intent(this, MediaPlaybackActivity.class);
            startActivity(intent);
        }
    }

    /**
     * OnPageChangeListener for ViewPager
     * 
     * @param position
     */
    public void onPageSelected(int position) {
        MusicLogUtils.d(TAG, "onPageSelected-position:" + position);
        mTabHost.setCurrentTab(position);
    }

    /**
     * onPageScrolled
     * 
     * @param position
     * @param positionOffset
     * @param positionOffsetPixels
     */
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    /**
     * onPageScrollStateChanged
     * 
     * @param state
     */
    public void onPageScrollStateChanged(int state) {
    }

    /**
     * MusicPagerAdapter for scroll page
     */
    private class MusicPagerAdapter extends PagerAdapter {
        @Override
        public void destroyItem(View container, int position, Object object) {
            ViewPager viewPager = ((ViewPager) container);
            // MusicLogUtils.d(TAG, "destroyItem-position:" + position);
            viewPager.removeView(mPagers.get(position));
        }

        @Override
        public Object instantiateItem(View container, int position) {
            ViewPager viewPager = ((ViewPager) container);
            View view = mPagers.get(position);
            MusicLogUtils.d(TAG, "instantiateItem-position:" + position);
            if (view == null) {
                view = getView(position);
                mPagers.remove(position);
                mPagers.add(position, view);
                mActivityManager.dispatchResume();
            }
            viewPager.addView(view);
            return mPagers.get(position);
        }

        public int getCount() {
            // MusicLogUtils.d(TAG, "getCount:" + mPagers.size());
            return mPagers.size();
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == null ? false : view.equals(object);
        }
    }

    /**
     * M: init search button, set on click listener and search dialog on dismiss listener, disable search button
     * when search dialog has shown and enable it after dismiss search dialog.
     */
    private void initSearchButton() {
        mSearchButton = (ImageButton)findViewById(R.id.search_menu_nowplaying);
        final View blankView = this.findViewById(R.id.blank_between_search_and_overflow);
        final View nowPlayingView = this.findViewById(R.id.nowplaying);
        if (mSearchButton != null) {
            mSearchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSearchRequested();
                    mSearchButton.setVisibility(View.GONE);
                    if (blankView.getVisibility() == View.VISIBLE) {
                        blankView.setVisibility(View.GONE);
                    }
                }
            });
            SearchManager searchManager = (SearchManager) this.getSystemService(Context.SEARCH_SERVICE);
            searchManager.setOnDismissListener(new SearchManager.OnDismissListener() {
                @Override
                public void onDismiss() {
                    mSearchButton.setVisibility(View.VISIBLE);
                    if (nowPlayingView.getVisibility() != View.VISIBLE && !mHasMenukey) {
                        blankView.setVisibility(View.VISIBLE);
                    }
                    MusicLogUtils.d(TAG, "Search dialog on dismiss, enalbe search button");
                }
            });
        }
    }
}
