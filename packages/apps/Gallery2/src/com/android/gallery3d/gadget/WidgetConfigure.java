/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.gallery3d.gadget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AlbumPicker;
import com.android.gallery3d.app.CropImage;
import com.android.gallery3d.app.DialogPicker;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.app.Gallery;
import android.widget.Toast;

import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;

public class WidgetConfigure extends Activity {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/WidgetConfigure";

    public static final String KEY_WIDGET_TYPE = "widget-type";
    private static final String KEY_PICKED_ITEM = "picked-item";

    private static final int REQUEST_WIDGET_TYPE = 1;
    private static final int REQUEST_CHOOSE_ALBUM = 2;
    private static final int REQUEST_CROP_IMAGE = 3;
    private static final int REQUEST_GET_PHOTO = 4;

    public static final int RESULT_ERROR = RESULT_FIRST_USER;

    // Scale up the widget size since we only specified the minimized
    // size of the gadget. The real size could be larger.
    // Note: There is also a limit on the size of data that can be
    // passed in Binder's transaction.
    private static float WIDGET_SCALE_FACTOR = 1.5f;
    private static int MAX_WIDGET_SIDE = 360;

    private int mAppWidgetId = -1;
    private Uri mPickedItem;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        if (mAppWidgetId == -1) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }
        
        MtkLog.d(TAG, "onCreate, widget id=" + mAppWidgetId);

        if (savedState == null) {
            if (ApiHelper.HAS_REMOTE_VIEWS_SERVICE) {
                Intent intent = new Intent(this, WidgetTypeChooser.class);
                startActivityForResult(intent, REQUEST_WIDGET_TYPE);
            } else { // Choose the photo type widget
                setWidgetType(new Intent()
                        .putExtra(KEY_WIDGET_TYPE, R.id.widget_type_photo));
            }
        } else {
            mPickedItem = savedState.getParcelable(KEY_PICKED_ITEM);
        }
    }
    protected void onSaveInstanceStates(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PICKED_ITEM, mPickedItem);
    }

    private void updateWidgetAndFinish(WidgetDatabaseHelper.Entry entry) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        RemoteViews views = PhotoAppWidgetProvider.buildWidget(this, mAppWidgetId, entry);
        manager.updateAppWidget(mAppWidgetId, views);
        setResult(RESULT_OK, new Intent().putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            setResult(resultCode, new Intent().putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
            finish();
            return;
        }

        if (requestCode == REQUEST_WIDGET_TYPE) {
            setWidgetType(data);
        } else if (requestCode == REQUEST_CHOOSE_ALBUM) {
            setChoosenAlbum(data);
        } else if (requestCode == REQUEST_GET_PHOTO) {
            setChoosenPhoto(data);
        } else if (requestCode == REQUEST_CROP_IMAGE) {
            setPhotoWidget(data);
        } else {
            throw new AssertionError("unknown request: " + requestCode);
        }
    }

    /// M: For external sdcard ,the URI(//conten://media/external/images/media/id)
    // maybe change, after mount and remount operation. So Use absolute path replace
    // URI(//conten://media/external/images/media/id) @{
    public String getAbsloutePathBaseOfUri (Uri imageUri) {
        Log.d(TAG, "Single Photo mode :imageUri==="+imageUri);
        String absloutePath = null;
        Cursor cursor = null;
        try{
            cursor = getContentResolver().query(
                    imageUri,
                    new String[]{ImageColumns.DATA},
                    null,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                absloutePath = cursor.getString(0);
                Log.d(TAG,"[getAbsloutePathBaseOfUri]get absolute path ="+ absloutePath);
            }
        } catch (Exception e) {
            Log.d(TAG, " [getAbsloutePathBaseOfUri] "+e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return absloutePath;
    }
    ///@}
    private void setPhotoWidget(Intent data) {
        // Store the cropped photo in our database
        Bitmap bitmap = (Bitmap) data.getParcelableExtra("data");
        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(this);
        try {
            // M: we need to check each step for possible failure,
            // e.g. low memory caused gallery process being killed
            // during CropImage
            if (mPickedItem == null) {
                // M: since we've changed default pick->crop flow,
                // we should look into intent data for a possible picked item uri
                /// M: For external sdcard ,the URI(//conten://media/external/images/media/id)
                // maybe change, after unmount and mount operation. So Use absolute path replace
                // URI(//conten://media/external/images/media/id)
                String absolutePath = getAbsloutePathBaseOfUri(data.getData());
                if (absolutePath != null
                        && WidgetDatabaseHelper.EXTERNAL_SDCARD_PATH != null
                        && absolutePath.contains(WidgetDatabaseHelper.EXTERNAL_SDCARD_PATH)) {
                    mPickedItem = Uri.parse(absolutePath);
                    Log.d(TAG, "use absloute path as uri for external sdcard="+mPickedItem);
                } else {
                    mPickedItem = data.getData();
                }
            }
            boolean setPhotoSucceeded = helper.setPhoto(mAppWidgetId, mPickedItem, bitmap);
            if (!setPhotoSucceeded) {
                MtkLog.e(TAG, "setPhoto for widget #" + mAppWidgetId + " uri[" + mPickedItem + "] failed!!");
                Toast.makeText(this, R.string.widget_load_failed, Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            WidgetDatabaseHelper.Entry widgetEntry = helper.getEntry(mAppWidgetId);
            if (widgetEntry != null) {
                updateWidgetAndFinish(widgetEntry);
            } else {
                MtkLog.e(TAG, "getEntry(" + mAppWidgetId + ") failed!!");
                Toast.makeText(this, R.string.widget_load_failed, Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        } finally {
            helper.close();
        }
    }

    private void setChoosenPhoto(Intent data) {
        Resources res = getResources();

        float width = res.getDimension(R.dimen.appwidget_width);
        float height = res.getDimension(R.dimen.appwidget_height);

        // We try to crop a larger image (by scale factor), but there is still
        // a bound on the binder limit.
        float scale = Math.min(WIDGET_SCALE_FACTOR,
                MAX_WIDGET_SIDE / Math.max(width, height));

        int widgetWidth = Math.round(width * scale);
        int widgetHeight = Math.round(height * scale);

        mPickedItem = data.getData();
        MtkLog.d(TAG, "setChoosenPhoto: photo uri=" + mPickedItem);
        Intent request = new Intent(CropImage.ACTION_CROP, mPickedItem)
                .putExtra(CropImage.KEY_OUTPUT_X, widgetWidth)
                .putExtra(CropImage.KEY_OUTPUT_Y, widgetHeight)
                .putExtra(CropImage.KEY_ASPECT_X, widgetWidth)
                .putExtra(CropImage.KEY_ASPECT_Y, widgetHeight)
                .putExtra(CropImage.KEY_SCALE_UP_IF_NEEDED, true)
                .putExtra(CropImage.KEY_SCALE, true)
                .putExtra(CropImage.KEY_RETURN_DATA, true);
        startActivityForResult(request, REQUEST_CROP_IMAGE);
    }

    private void setChoosenAlbum(Intent data) {
        String albumPath = data.getStringExtra(AlbumPicker.KEY_ALBUM_PATH);
        MtkLog.d(TAG, "setChoosenAlbum: album path=" + albumPath);
        WidgetDatabaseHelper helper = new WidgetDatabaseHelper(this);
        try {
            helper.setWidget(mAppWidgetId,
                    WidgetDatabaseHelper.TYPE_ALBUM, albumPath);
            updateWidgetAndFinish(helper.getEntry(mAppWidgetId));
        } finally {
            helper.close();
        }
    }

    private void setWidgetType(Intent data) {
        int widgetType = data.getIntExtra(KEY_WIDGET_TYPE, R.id.widget_type_shuffle);
        if (widgetType == R.id.widget_type_album) {
            MtkLog.d(TAG, "setWidgetType: type=album");
            Intent intent = new Intent(this, AlbumPicker.class);

            // M: set drm inclusion if needed
            setDrmFlLevel(intent);

            startActivityForResult(intent, REQUEST_CHOOSE_ALBUM);
        } else if (widgetType == R.id.widget_type_shuffle) {
            MtkLog.d(TAG, "setWidgetType: type=shuffle");
            WidgetDatabaseHelper helper = new WidgetDatabaseHelper(this);
            try {
                helper.setWidget(mAppWidgetId, WidgetDatabaseHelper.TYPE_SHUFFLE, null);
                updateWidgetAndFinish(helper.getEntry(mAppWidgetId));
            } finally {
                helper.close();
            }
        } else {
            MtkLog.d(TAG, "setWidgetType: type=photo");
            if (startMtkCropFlow()) {
                return;
            }
            // Explicitly send the intent to the DialogPhotoPicker
            Intent request = new Intent(this, DialogPicker.class)
                    .setAction(Intent.ACTION_GET_CONTENT)
                    .setType("image/*");

            // M: set drm inclusion if needed
            setDrmFlLevel(request);
            // M: do not show convert dialog
            if (MediatekFeature.isStereoDisplaySupported()) {
                request.putExtra(StereoHelper.ATTACH_WITHOUT_CONVERSION, true);
            }

            startActivityForResult(request, REQUEST_GET_PHOTO);
        }
    }

    private void setDrmFlLevel(Intent intent) {
        if (null == intent || !MediatekFeature.isDrmSupported()) return;
        //add Drm Feature support, specify that only FL
        //kind drm media can be queried out.
        intent.putExtra(com.mediatek.drm.OmaDrmStore.DrmExtra.EXTRA_DRM_LEVEL,
                        com.mediatek.drm.OmaDrmStore.DrmExtra.DRM_LEVEL_FL);
    }
    
    // M: added for MTK-specific pick-and-crop flow;
    // which allows user to navigate back to AlbumPage
    // and pick another image to crop
    private boolean startMtkCropFlow() {
        // Explicitly send the intent to the DialogPhotoPicker
        Intent request = new Intent(this, DialogPicker.class)
                .setAction(Intent.ACTION_GET_CONTENT)
                .setType("image/*");
        
        // M: calculate and fill in crop related extras
        Resources res = getResources();

        float width = res.getDimension(R.dimen.appwidget_width);
        float height = res.getDimension(R.dimen.appwidget_height);

        // We try to crop a larger image (by scale factor), but there is still
        // a bound on the binder limit.
        float scale = Math.min(WIDGET_SCALE_FACTOR,
                MAX_WIDGET_SIDE / Math.max(width, height));

        int widgetWidth = Math.round(width * scale);
        int widgetHeight = Math.round(height * scale);
        
        request.putExtra(CropImage.KEY_OUTPUT_X, widgetWidth)
                .putExtra(CropImage.KEY_OUTPUT_Y, widgetHeight)
                .putExtra(CropImage.KEY_ASPECT_X, widgetWidth)
                .putExtra(CropImage.KEY_ASPECT_Y, widgetHeight)
                .putExtra(CropImage.KEY_SCALE_UP_IF_NEEDED, true)
                .putExtra(CropImage.KEY_SCALE, true)
                .putExtra(CropImage.KEY_RETURN_DATA, true);
        request.putExtra(Gallery.EXTRA_CROP, "crop");

        // M: set drm inclusion if needed
        setDrmFlLevel(request);
        // M: do not show convert dialog
        if (MediatekFeature.isStereoDisplaySupported()) {
            request.putExtra(StereoHelper.ATTACH_WITHOUT_CONVERSION, true);
        }
        
        startActivityForResult(request, REQUEST_CROP_IMAGE);
        return true;
    }
}
