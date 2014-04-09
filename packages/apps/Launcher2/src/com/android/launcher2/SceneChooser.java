/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.launcher2;

import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import com.mediatek.widget.BookmarkAdapter;
import com.mediatek.widget.BookmarkItem;
import com.mediatek.widget.BookmarkView;
import android.widget.BounceCoverFlow;
import android.widget.TextView;

import com.android.internal.util.XmlUtils;
import com.android.launcher.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class SceneChooser extends Activity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "SceneChooser";
    private static final String TAG_SCENES = "scenes";
    private static final String CONTACT_STRING = "&";
    // Key used in system settings provider.
    private static final String KEY_CURRETN_SCENE_NAME = "current_scene_name";
    
    private static final String PREF_CURRENT_SCENE = "current_scene";
    private static final String PREF_KEY_CURRETN_SCENE_POS = "current_scene_pos";
    private static final String PREF_KEY_CURRETN_SCENE_NAME = "current_scene_name";
    private static final String PREF_KEY_CURRETN_SCENE_TITLE = "current_scene_title";
    private static final String PREF_KEY_CURRETN_SCENE_PREVIEW = "current_scene_preview";
    private static final String PREF_KEY_CURRETN_SCENE_WORKSPACE = "current_scene_workspace";
    private static final String PREF_KEY_CURRETN_SCENE_WALLPAPER = "current_scene_wallpaper";

    private static final float IMAGE_RELECTION = 0.18f; 
    private static final int MAX_ZOOM_OUT = 120;
    
    private BookmarkAdapter mAdapter;
    private BounceCoverFlow mCoverflow;    
    private ArrayList<BookmarkItem> mBookmarkItems;
    private ArrayList<SceneMetaData> mScenesData = new ArrayList<SceneMetaData>();

    private TextView mSceneNameText;    
    private Bitmap mStamp;

    private int mImgHeight;
    private int mImgWidth;
    
    private int mSelectedScenePos;
    private int mCurrentScenePos;
    private String mCurrentSuffix;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scene_chooser);

        loadScenes();
        mCurrentScenePos = getCurrentScenePos(this);

        final Resources resources = getResources();
        mCurrentSuffix = resources.getString(R.string.state);        
        initViews(resources);
    }
    
    @Override
    protected void onDestroy() {
        Bitmap bmp = null;
        for (BookmarkItem item : mBookmarkItems) {
            bmp = item.getContentBitmap();
            if (bmp != null) {
                bmp.recycle();
            }
        }
        
        if (mStamp != null && !mStamp.isRecycled()) {
            mStamp.recycle();
            mStamp = null;
        }
        super.onDestroy();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return true;
    }
    
    private void initViews(final Resources resources) {
        mSceneNameText = (TextView) findViewById(R.id.scene_name);

        mStamp = BitmapFactory.decodeResource(resources, R.drawable.current_scene_stamp);
        mBookmarkItems = new ArrayList<BookmarkItem>();

        mImgWidth = resources.getDimensionPixelSize(R.dimen.thumb_disp_width);
        mImgHeight = resources.getDimensionPixelSize(R.dimen.thumb_disp_height);

        mCoverflow = (BounceCoverFlow) findViewById(R.id.bookmarkCoverflow);
        mCoverflow.setGravity(Gravity.CENTER_VERTICAL);

        addScenesToBookmark(resources);

        mAdapter = new BookmarkAdapter(this, mBookmarkItems);

        mAdapter.setImageDispSize(mImgWidth, mImgHeight);
        mAdapter.setImageReflection(IMAGE_RELECTION);
        mCoverflow.setSpacing(resources.getDimensionPixelSize(R.dimen.coverflow_space));

        mCoverflow.setAdapter(mAdapter);
        mCoverflow.setSelection(mCurrentScenePos);
        mCoverflow.setEmptyView(null);
        mCoverflow.setMaxZoomOut(MAX_ZOOM_OUT);
        mCoverflow.setOnItemSelectedListener(this);
    }

    private void addScenesToBookmark(final Resources resources) {
        final int sceneCount = mScenesData.size();
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "addScenesToBookmark: sceneCount = " + sceneCount + ", mCurrentScenePos = "
                    + mCurrentScenePos);
        }
        
        SceneMetaData scene = null;
        Bitmap previewImage = null;
        String sceneName = null;
        for (int i = 0; i < sceneCount; i++) {
            scene = mScenesData.get(i);
            previewImage = BitmapFactory.decodeResource(resources, scene.previewResId);
            sceneName = resources.getString(scene.sceneTitleResId);
            BookmarkItem item = new BookmarkItem(previewImage, sceneName, null);
            mBookmarkItems.add(item);
        }

        addStampForCurrentScenePreview(mCurrentScenePos);
    }
    
    public void switchSceneAndBack(View v) {
        mCurrentScenePos = mSelectedScenePos;
        final SceneMetaData currentScene = mScenesData.get(mCurrentScenePos);
        Settings.System.putString(getContentResolver(), KEY_CURRETN_SCENE_NAME,
                generateCombinedIdentString(mCurrentScenePos));
        saveSceneSetting(this, currentScene);

        LauncherApplication app = (LauncherApplication) getApplication();
        app.setCurrentScene(currentScene);

        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "switchSceneAndBack: mCurrentScenePos = " + mCurrentScenePos + ", currentScene = "
                    + currentScene);
        }
        
        // Send broadcast to let Launcher activity update database and UI.
        final Intent intent = new Intent(LauncherModel.ACTION_SWITCH_SCENE);
        sendBroadcast(intent);
        
        finish();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "onItemSelected: position = " + position + ", mSelectedScenePos = " + mSelectedScenePos);
        }
        mSelectedScenePos = position;

        if (mSelectedScenePos == mCurrentScenePos) {
            mSceneNameText.setText(getSceneTitle(getResources(), mSelectedScenePos) + "(" + mCurrentSuffix + ")");
        } else {
            mSceneNameText.setText(getSceneTitle(getResources(), mSelectedScenePos));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        mSelectedScenePos = -1;
    }

    /**
     * M: get current scene name.
     * 
     * @return
     */
    public static String getCurrentSceneName(final Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_CURRENT_SCENE, 0);
        final int sceneNameResId = pref.getInt(PREF_KEY_CURRETN_SCENE_NAME, R.string.scene_name_default);
        return context.getResources().getString(sceneNameResId);
    }
    
    /**
     * M: Set wallpaper.
     * 
     * @param resId the resource id of the wallpaper to be set.
     */
    public static void setWallpaper(final Context context, final int resId) {
        WallpaperManager mWallpaperManager = WallpaperManager.getInstance(context);
        try {
            mWallpaperManager.setResource(resId);
        } catch (IOException e) {
            LauncherLog.e(TAG, "Got IOException when setWallpaper: resId = " + resId, e);
        }
    }
    
    /**
     * M: Save Scene after switch scene.
     */
    static void saveSceneSetting(Context context, SceneMetaData scene) {
        SharedPreferences pref = context.getSharedPreferences(PREF_CURRENT_SCENE, 0);
        SharedPreferences.Editor editor = pref.edit();

        editor.putInt(PREF_KEY_CURRETN_SCENE_POS, scene.pos);
        editor.putInt(PREF_KEY_CURRETN_SCENE_NAME, scene.sceneNameResId);
        editor.putInt(PREF_KEY_CURRETN_SCENE_TITLE, scene.sceneTitleResId);
        editor.putInt(PREF_KEY_CURRETN_SCENE_PREVIEW, scene.previewResId);
        editor.putInt(PREF_KEY_CURRETN_SCENE_WORKSPACE, scene.workspaceResId);        
        editor.putInt(PREF_KEY_CURRETN_SCENE_WALLPAPER, scene.sceneWallpaper);

        editor.commit();
    }
    
    static void getSceneSetting(Context context, SceneMetaData scene) {
        SharedPreferences pref = context.getSharedPreferences(PREF_CURRENT_SCENE, 0);
        scene.pos = pref.getInt(PREF_KEY_CURRETN_SCENE_POS, -1);
        scene.sceneNameResId = pref.getInt(PREF_KEY_CURRETN_SCENE_NAME, R.string.scene_name_default);
        scene.sceneTitleResId = pref.getInt(PREF_KEY_CURRETN_SCENE_TITLE, R.string.scene_title_default);
        scene.previewResId = pref.getInt(PREF_KEY_CURRETN_SCENE_POS, R.drawable.scene_default);
        scene.workspaceResId = pref.getInt(PREF_KEY_CURRETN_SCENE_POS, R.xml.default_workspace);
        scene.sceneWallpaper = pref.getInt(PREF_KEY_CURRETN_SCENE_WALLPAPER, R.drawable.wallpaper_03);
    }
    
    /**
     * M: get current scene position.
     * 
     * @param context
     * @return
     */
    private int getCurrentScenePos(final Context context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_CURRENT_SCENE, 0);
        return pref.getInt(PREF_KEY_CURRETN_SCENE_POS, 0);
    }

    /**
     * Generate an identity string which combines package name and resource id of current scene name string.
     * 
     * @param pos
     * @return
     */
    private String generateCombinedIdentString(final int pos) {
        return getPackageName() + CONTACT_STRING + String.valueOf(mScenesData.get(pos).sceneTitleResId);
    }
    
    /**
     * Get the name of the given scene.
     * 
     * @param resources
     * @param pos
     * @return
     */
    private String getSceneTitle(final Resources resources, final int pos) {
        return resources.getString(mScenesData.get(pos).sceneTitleResId);
    }
    
    /**
     * Add current stamp for the current scene preview image.
     * 
     * @param pos
     */
    private void addStampForCurrentScenePreview(int pos) {
        final Bitmap preview = mBookmarkItems.get(pos).getContentBitmap();
        final int previewWidth = preview.getWidth();
        final int previewHeight = preview.getHeight();
        final int stampWidth = mStamp.getWidth();
        final int stampHeight = mStamp.getHeight();

        Bitmap stampBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(stampBitmap);
        canvas.drawBitmap(preview, 0, 0, null);
        canvas.drawBitmap(mStamp, 0, 0, null);

        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();

        mBookmarkItems.get(pos).setContentBitmap(stampBitmap);
    }
    
    /**
     * Load all scenes from xml.
     */
    private void loadScenes() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d(TAG, "loadScenes: this = " + this);
        }

        try {
            XmlResourceParser parser = this.getResources().getXml(R.xml.default_scenes);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            XmlUtils.beginDocument(parser, TAG_SCENES);

            final int depth = parser.getDepth();

            int type = -1;
            while (((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > depth)
                    && type != XmlPullParser.END_DOCUMENT) {

                if (type != XmlPullParser.START_TAG) {
                    continue;
                }

                SceneMetaData scene = new SceneMetaData();
                TypedArray a = this.obtainStyledAttributes(attrs, R.styleable.Scene);
                scene.pos = a.getInteger(R.styleable.Scene_scenePos, -1);
                scene.sceneNameResId = a.getResourceId(R.styleable.Scene_sceneNameResId, -1);
                scene.sceneTitleResId = a.getResourceId(R.styleable.Scene_sceneTitleResId, -1);
                scene.previewResId = a.getResourceId(R.styleable.Scene_previewResId, -1);
                scene.workspaceResId = a.getResourceId(R.styleable.Scene_workspaceResId, -1);
                scene.sceneWallpaper = a.getResourceId(R.styleable.Scene_sceneWallpaper, -1);
                mScenesData.add(scene.pos, scene);
                if (LauncherLog.DEBUG) {
                    LauncherLog.d(TAG, "Load scene: " + scene); 
                }
                a.recycle();
            }
        } catch (XmlPullParserException e) {
            LauncherLog.w(TAG, "Got XmlPullParserException while parsing scenes.", e);
        } catch (IOException e) {
            LauncherLog.w(TAG, "Got IOException while parsing scenes.", e);
        }
    }

    static class SceneMetaData {
        int pos;
        int sceneTitleResId;
        int previewResId;
        int workspaceResId;
        int sceneNameResId;
        int sceneWallpaper;

        @Override
        public String toString() {
            return "Scene{ pos = " + pos + ", sceneNameResId = " + Integer.toHexString(sceneNameResId)
                    + ", sceneTitleResId = " + Integer.toHexString(sceneTitleResId) + ", workspace = "
                    + Integer.toHexString(workspaceResId) + ", preview = " + Integer.toHexString(previewResId)
                    + ", wallpaper = " + Integer.toHexString(sceneWallpaper) + "}";
        }
    }
}
