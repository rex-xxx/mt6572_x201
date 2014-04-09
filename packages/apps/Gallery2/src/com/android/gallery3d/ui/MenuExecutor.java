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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumSetPage;
import com.android.gallery3d.app.CropImage;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.ClusterAlbum;
import com.android.gallery3d.data.ClusterAlbumSet;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

import java.util.ArrayList;

//mediatek imports
import android.net.Uri;
import android.widget.Toast;

import com.android.gallery3d.data.MediaSet;

import com.mediatek.gallery3d.drm.DrmHelper;
import com.mediatek.gallery3d.stereo.StereoConvertor;
import com.mediatek.gallery3d.stereo.StereoHelper;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

public class MenuExecutor {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/MenuExecutor";

    private static final int MSG_TASK_COMPLETE = 1;
    private static final int MSG_TASK_UPDATE = 2;
    private static final int MSG_TASK_START = 3;
    private static final int MSG_DO_SHARE = 4;

    public static final int EXECUTION_RESULT_SUCCESS = 1;
    public static final int EXECUTION_RESULT_FAIL = 2;
    public static final int EXECUTION_RESULT_CANCEL = 3;

    private static final boolean mIsStereoDisplaySupported =
                                          MediatekFeature.isStereoDisplaySupported();
    private static final boolean mIsDisplay2dAs3dSupported =
            MediatekFeature.isDisplay2dAs3dSupported();

    private ProgressDialog mDialog;
    private Future<?> mTask;
    // wait the operation to finish when we want to stop it.
    private boolean mWaitOnStop;
    private volatile boolean isMultiOperation ;
    private volatile boolean hasCancelMultiOperation ;
    private final AbstractGalleryActivity mActivity;
    private final SelectionManager mSelectionManager;
    private final Handler mHandler;

    private ProgressDialog createProgressDialog(
            Context context, int titleId, int progressMax) {
        ProgressDialog dialog = new ProgressDialog(context);
        if (R.string.stereo3d_convert2d_dialog_title != titleId) {
            dialog.setTitle(titleId);
        }
        dialog.setMax(progressMax);
        dialog.setCancelable(false);
        dialog.setIndeterminate(false);
        /// M: while isMultiOperation is true,should add "cancel" button for stopping the operation.
        if (isMultiOperation) {
            dialog.setButton(context.getString(R.string.cancel), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    hasCancelMultiOperation = true;
                }
            });
        }
        if (progressMax > 1) {
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        }
        return dialog;
    }

    public interface ProgressListener {
        public void onConfirmDialogShown();
        public void onConfirmDialogDismissed(boolean confirmed);
        public void onProgressStart();
        public void onProgressUpdate(int index);
        public void onProgressComplete(int result);
    }

    public MenuExecutor(
            AbstractGalleryActivity activity, SelectionManager selectionManager) {
        mActivity = Utils.checkNotNull(activity);
        mSelectionManager = Utils.checkNotNull(selectionManager);
        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_TASK_START: {
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressStart();
                        }
                        break;
                    }
                    case MSG_TASK_COMPLETE: {
                        stopTaskAndDismissDialog();
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressComplete(message.arg1);
                        }
                        mSelectionManager.leaveSelectionMode();
                        break;
                    }
                    case MSG_TASK_UPDATE: {
                        if (mDialog != null) mDialog.setProgress(message.arg1);
                        if (message.obj != null) {
                            ProgressListener listener = (ProgressListener) message.obj;
                            listener.onProgressUpdate(message.arg1);
                        }
                        break;
                    }
                    case MSG_DO_SHARE: {
                        ((Activity) mActivity).startActivity((Intent) message.obj);
                        break;
                    }
                }
            }
        };
    }

    private void stopTaskAndDismissDialog() {
        /// M: if isMultiOperation == true ,should not stop the task. so after press
        // home key. The task still run on background.
        if (isMultiOperation == true) return;
        if (mTask != null) {
            if (!mWaitOnStop) mTask.cancel();
            mTask.waitDone();
            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }
            mTask = null;
        }
    }

    public void pause() {
        stopTaskAndDismissDialog();
    }

    private void onProgressUpdate(int index, ProgressListener listener) {
        mHandler.sendMessage(
                mHandler.obtainMessage(MSG_TASK_UPDATE, index, 0, listener));
    }

    private void onProgressStart(ProgressListener listener) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TASK_START, listener));
    }

    private void onProgressComplete(int result, ProgressListener listener) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TASK_COMPLETE, result, 0, listener));
    }

    public static void updateMenuOperation(Menu menu, int supported) {
        boolean supportDelete = (supported & MediaObject.SUPPORT_DELETE) != 0;
        boolean supportRotate = (supported & MediaObject.SUPPORT_ROTATE) != 0;
        boolean supportCrop = (supported & MediaObject.SUPPORT_CROP) != 0;
        boolean supportTrim = (supported & MediaObject.SUPPORT_TRIM) != 0;
        boolean supportShare = (supported & MediaObject.SUPPORT_SHARE) != 0;
        boolean supportSetAs = (supported & MediaObject.SUPPORT_SETAS) != 0;
        boolean supportShowOnMap = (supported & MediaObject.SUPPORT_SHOW_ON_MAP) != 0;
        boolean supportCache = (supported & MediaObject.SUPPORT_CACHE) != 0;
        boolean supportEdit = (supported & MediaObject.SUPPORT_EDIT) != 0;
        boolean supportInfo = (supported & MediaObject.SUPPORT_INFO) != 0;
        boolean supportImport = (supported & MediaObject.SUPPORT_IMPORT) != 0;

        //add for Bluetooth Print feature
        boolean supportPrint = ((supported & MediaObject.SUPPORT_PRINT) != 0) &&
                               MediatekFeature.isBluetoothPrintSupported();
        //add fro drm protection info
        boolean supportDrmInfo = (supported & MediaObject.SUPPORT_DRM_INFO) != 0;
        // added for stereo 3D display
        boolean supportStereoMode = (supported & MediaObject.SUPPORT_STEREO_DISPLAY) != 0;
        // add for PQ Tuning feature
        boolean supportPQTuning = (supported & MediaObject.SUPPORT_PQ) != 0;

        setMenuItemVisible(menu, R.id.action_delete, supportDelete);
        setMenuItemVisible(menu, R.id.action_rotate_ccw, supportRotate);
        setMenuItemVisible(menu, R.id.action_rotate_cw, supportRotate);
        setMenuItemVisible(menu, R.id.action_crop, supportCrop);
        setMenuItemVisible(menu, R.id.action_trim, supportTrim);
        // Hide panorama until call to updateMenuForPanorama corrects it
        setMenuItemVisible(menu, R.id.action_share_panorama, false);
        setMenuItemVisible(menu, R.id.action_share, supportShare);
        setMenuItemVisible(menu, R.id.action_setas, supportSetAs);
        setMenuItemVisible(menu, R.id.action_show_on_map, supportShowOnMap);
        setMenuItemVisible(menu, R.id.action_edit, supportEdit);
        setMenuItemVisible(menu, R.id.action_details, supportInfo);
        setMenuItemVisible(menu, R.id.action_import, supportImport);

        // add for pqTuning feature
        setMenuItemVisible(menu, R.id.action_picture_quality, supportPQTuning);
        //add for Bluetooth Print feature
        setMenuItemVisible(menu, R.id.action_print, supportPrint);
        //add for drm pro Print feature
        setMenuItemVisible(menu, R.id.action_protect_info, supportDrmInfo);
        // added for stereo 3D display
        setMenuItemVisible(menu, R.id.action_switch_stereo_mode, supportStereoMode);
        updateStereoMenu(menu, supported);
    }

    public static void updateMenuForPanorama(Menu menu, boolean shareAsPanorama360,
            boolean disablePanorama360Options) {
        setMenuItemVisible(menu, R.id.action_share_panorama, shareAsPanorama360);
        if (disablePanorama360Options) {
            setMenuItemVisible(menu, R.id.action_rotate_ccw, false);
            setMenuItemVisible(menu, R.id.action_rotate_cw, false);
        }
    }

    private static void setMenuItemVisible(Menu menu, int itemId, boolean visible) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) item.setVisible(visible);
    }

    private Path getSingleSelectedPath() {
        ArrayList<Path> ids = mSelectionManager.getSelected(true);
        Utils.assertTrue(ids.size() == 1);
        return ids.get(0);
    }

    private Intent getIntentBySingleSelectedPath(String action) {
        DataManager manager = mActivity.getDataManager();
        Path path = getSingleSelectedPath();
        String mimeType = getMimeType(manager.getMediaType(path));
        return new Intent(action).setDataAndType(manager.getContentUri(path), mimeType);
    }

    private void onMenuClicked(int action, ProgressListener listener) {
        onMenuClicked(action, listener, false, true);
    }

    public void onMenuClicked(int action, ProgressListener listener,
            boolean waitOnStop, boolean showDialog) {
        int title;
        switch (action) {
            case R.id.action_select_all:
                if (mSelectionManager.inSelectAllMode()) {
                    mSelectionManager.deSelectAll();
                } else {
                    mSelectionManager.selectAll();
                }
                return;
            case R.id.action_crop: {
                DataManager manager = mActivity.getDataManager();
                Path path = getSingleSelectedPath();
                MediaObject obj = manager.getMediaObject(path);
                if (MediatekFeature.isStereoImage(obj)) {
                    String crop = ((Activity) mActivity).getString(R.string.crop_action);
                    String convertCrop = ((Activity) mActivity).getString(
                                     R.string.stereo3d_convert2d_dialog_text,crop);
                    clickStereoPhoto(action, listener, convertCrop);
                    return;
                }
                Intent intent = getIntentBySingleSelectedPath(FilterShowActivity.CROP_ACTION)
                        .setClass((Activity) mActivity, FilterShowActivity.class);
                ((Activity) mActivity).startActivity(intent);
                return;
            }
            case R.id.action_edit: {
                //we shift original Google code here to let 3D convert to 2D
                //process run in the thread pool
                DataManager manager = mActivity.getDataManager();
                Path path = getSingleSelectedPath();
                MediaObject obj = manager.getMediaObject(path);
                Log.i(TAG,"onItemSelected:obj="+obj);
                Log.i(TAG,"onItemSelected:MediatekFeature.isStereoImage(obj)="+MediatekFeature.isStereoImage(obj));
                if (MediatekFeature.isStereoImage(obj)) {
                    String edit = ((Activity) mActivity).getString(R.string.edit);
                    String convertEdit = ((Activity) mActivity).getString(
                                     R.string.stereo3d_convert2d_dialog_text,edit);
                    clickStereoPhoto(action, listener, convertEdit);
                    return;
                }
                Intent intent = getIntentBySingleSelectedPath(Intent.ACTION_EDIT)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ((Activity) mActivity).startActivity(Intent.createChooser(intent, null));
                return;
            }
            case R.id.action_setas: {
                Intent intent = getIntentBySingleSelectedPath(Intent.ACTION_ATTACH_DATA)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra("mimeType", intent.getType());
                Activity activity = mActivity;
                activity.startActivity(Intent.createChooser(
                        intent, activity.getString(R.string.set_as)));
                return;
            }
            case R.id.action_delete:
                title = R.string.delete;
                break;
            case R.id.action_rotate_cw:
                title = R.string.rotate_right;
                break;
            case R.id.action_rotate_ccw:
                title = R.string.rotate_left;
                break;
            case R.id.action_show_on_map:
                title = R.string.show_on_map;
                break;
            case R.id.action_import:
                title = R.string.Import;
                break;
            case R.id.action_protect_info: {
                //title = com.mediatek.internal.R.string.drm_protectioninfo_title;
                //break;
                //add for drm protection info
                if (!MediatekFeature.isDrmSupported()) return;
                DataManager manager = mActivity.getDataManager();
                Path path = getSingleSelectedPath();
                Uri uri = manager.getContentUri(path);
                DrmHelper.showProtectInfo((Activity) mActivity, uri);
                //onProgressComplete(EXECUTION_RESULT_SUCCESS, listener);
                return;
            }
            case R.id.action_print: {
                title = R.string.camera_print;
                DataManager manager = mActivity.getDataManager();
                Path path = getSingleSelectedPath();
                MediaObject obj = manager.getMediaObject(path);
                if (MediatekFeature.isStereoImage(obj)) {
                    String print = ((Activity) mActivity).getString(R.string.camera_print);
                    String convertPrint = ((Activity) mActivity).getString(
                                     R.string.stereo3d_convert2d_dialog_text,print);
                    clickStereoPhoto(action, listener, convertPrint);
                    return;
                }
                //add for Bluetooth Print
                Activity activity = (Activity) mActivity;
                String mimeType;
                Log.v(TAG, "Print for " + path);
                int type = manager.getMediaType(path);
                if(type != MediaObject.MEDIA_TYPE_IMAGE) {
                    break;
                } else {
                    mimeType = "image/*";
                }
                Intent intent = new Intent();
                intent.setAction("mediatek.intent.action.PRINT");
                intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM, manager.getContentUri(path));
                try {
                    activity.startActivity(Intent.createChooser(intent,
                                           activity.getText(R.string.printFile)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(activity, R.string.no_way_to_print,
                                   Toast.LENGTH_SHORT).show();
                }
                return;
            }
            default:
                return;
        }
        startAction(action, title, listener, waitOnStop, showDialog);
    }

    private class ConfirmDialogListener implements OnClickListener, OnCancelListener {
        private final int mActionId;
        private final ProgressListener mListener;

        public ConfirmDialogListener(int actionId, ProgressListener listener) {
            mActionId = actionId;
            mListener = listener;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mListener != null) {
                    mListener.onConfirmDialogDismissed(true);
                }
                onMenuClicked(mActionId, mListener);
            } else {
                if (mListener != null) {
                    mListener.onConfirmDialogDismissed(false);
                }
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            if (mListener != null) {
                mListener.onConfirmDialogDismissed(false);
            }
        }
    }

    public void onMenuClicked(MenuItem menuItem, String confirmMsg,
            final ProgressListener listener) {
        final int action = menuItem.getItemId();

        if (confirmMsg != null) {
            if (listener != null) listener.onConfirmDialogShown();
            ConfirmDialogListener cdl = new ConfirmDialogListener(action, listener);
            new AlertDialog.Builder(mActivity.getAndroidContext())
                    .setMessage(confirmMsg)
                    .setOnCancelListener(cdl)
                    .setPositiveButton(R.string.ok, cdl)
                    .setNegativeButton(R.string.cancel, cdl)
                    .create().show();
        } else {
            onMenuClicked(action, listener);
        }
    }

    public void startAction(int action, int title, ProgressListener listener) {
        startAction(action, title, listener, false, true);
    }

    public void startAction(int action, int title, ProgressListener listener,
            boolean waitOnStop, boolean showDialog) {
        ArrayList<Path> ids = mSelectionManager.getSelected(false);
        stopTaskAndDismissDialog();
        /// M: if ids.size() > 1, this is a multi operation.
        isMultiOperation = (ids.size() > 1)? true : false;
        Activity activity = mActivity;
        mDialog = createProgressDialog(activity, title, ids.size());

        // M: append message for single id
        appendMessageForSingleId(mDialog, ids);

        if (showDialog) {
            mDialog.show();
        }
        MediaOperation operation = new MediaOperation(action, ids, listener);
        mTask = mActivity.getThreadPool().submit(operation, null);
        mWaitOnStop = waitOnStop;
    }

    public static String getMimeType(int type) {
        switch (type) {
            case MediaObject.MEDIA_TYPE_IMAGE :
                return GalleryUtils.MIME_TYPE_IMAGE;
            case MediaObject.MEDIA_TYPE_VIDEO :
                return GalleryUtils.MIME_TYPE_VIDEO;
            default: return GalleryUtils.MIME_TYPE_ALL;
        }
    }

    private boolean execute(
            DataManager manager, JobContext jc, int cmd, Path path) {
        boolean result = true;
        Log.v(TAG, "Execute cmd: " + cmd + " for " + path);
        long startTime = System.currentTimeMillis();

        switch (cmd) {
            case R.id.action_delete:
                manager.delete(path);
                break;
            case R.id.action_rotate_cw:
                manager.rotate(path, 90);
                break;
            case R.id.action_rotate_ccw:
                manager.rotate(path, -90);
                break;
            case R.id.action_toggle_full_caching: {
                MediaObject obj = manager.getMediaObject(path);
                int cacheFlag = obj.getCacheFlag();
                if (cacheFlag == MediaObject.CACHE_FLAG_FULL) {
                    cacheFlag = MediaObject.CACHE_FLAG_SCREENNAIL;
                } else {
                    cacheFlag = MediaObject.CACHE_FLAG_FULL;
                }
                obj.cache(cacheFlag);
                break;
            }
            case R.id.action_show_on_map: {
                MediaItem item = (MediaItem) manager.getMediaObject(path);
                double latlng[] = new double[2];
                item.getLatLong(latlng);
                if (GalleryUtils.isValidLocation(latlng[0], latlng[1])) {
                    GalleryUtils.showOnMap(mActivity, latlng[0], latlng[1]);
                }
                break;
            }
            case R.id.action_import: {
                MediaObject obj = manager.getMediaObject(path);
                result = obj.Import();
                break;
            }
            /// M: Migration, onMenuclicked has its function, why add here? so remove it
            /*
            case R.id.action_crop: {
                Activity activity = (Activity) mActivity;
                MediaItem item = (MediaItem) manager.getMediaObject(path);
                //this operation may be time consuming.
                String imageMimeType = item.getMimeType();
                Uri imageUri = StereoConvertor.convertSingle(jc, (Context)mActivity,
                                     manager.getContentUri(path), imageMimeType);
                Log.d(TAG,"execute:crop:got new uri:"+imageUri);
                try {
                    Intent intent = new Intent(CropImage.ACTION_CROP)
                            .setDataAndType(manager.getContentUri(path), imageMimeType);
                    ((Activity) mActivity).startActivity(intent);
                } catch (Throwable t) {
                    Log.w(TAG, "failed to start crop activity: ", t);
                    Toast.makeText(activity,
                            activity.getString(R.string.activity_not_found),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.action_edit: {
                Activity activity = (Activity) mActivity;
                MediaItem item = (MediaItem) manager.getMediaObject(path);
                //this operation may be time consuming.
                String imageMimeType = item.getMimeType();
                Uri originUri = manager.getContentUri(path);
                Uri imageUri = StereoConvertor.convertSingle(jc, (Context)mActivity,
                                     originUri, imageMimeType);
                Log.d(TAG,"execute:edit:got new uri:"+imageUri);

                try {
                    activity.startActivity(Intent.createChooser(
                        new Intent(Intent.ACTION_EDIT)
                            .setDataAndType(imageUri, "image/jpeg")
                            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .putExtra(MtkUtils.URI_FOR_SAVING, originUri.toString()),
                        null));
                } catch (Throwable t) {
                    Log.w(TAG, "failed to start edit activity: ", t);
                    Toast.makeText(activity,
                            activity.getString(R.string.activity_not_found),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.action_print: {
                //add for 3D to 2D conversion
                Activity activity = (Activity) mActivity;
                String mimeType;
                Log.v(TAG, "Print for " + path);
                int type = manager.getMediaType(path);
                if(type != MediaObject.MEDIA_TYPE_IMAGE) {
                    break;
                } else {
                    mimeType = "image/*";
                }
                Intent intent = new Intent();
                intent.setAction("mediatek.intent.action.PRINT");
                intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                intent.setType(mimeType);

                //this operation may be time consuming.
                MediaItem item = (MediaItem) manager.getMediaObject(path);
                String imageMimeType = item.getMimeType();
                Uri imageUri = StereoConvertor.convertSingle(jc, (Context)mActivity,
                                     manager.getContentUri(path), imageMimeType);
                Log.d(TAG,"execute:print:got new uri:"+imageUri);

                intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                try {
                    activity.startActivity(Intent.createChooser(intent,
                                           activity.getText(R.string.printFile)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(activity, R.string.no_way_to_print,
                                   Toast.LENGTH_SHORT).show();
                }
                break;
            }
            */
            default:
                throw new AssertionError();
        }
        Log.v(TAG, "It takes " + (System.currentTimeMillis() - startTime) +
                " ms to execute cmd for " + path);
        return result;
    }

    private class MediaOperation implements Job<Void> {
        private final ArrayList<Path> mItems;
        private final int mOperation;
        private final ProgressListener mListener;

        public MediaOperation(int operation, ArrayList<Path> items,
                ProgressListener listener) {
            mOperation = operation;
            mItems = items;
            mListener = listener;
        }

        @Override
        public Void run(JobContext jc) {
            int index = 0;
            DataManager manager = mActivity.getDataManager();
            int result = EXECUTION_RESULT_SUCCESS;
            boolean isDelete = (mOperation == R.id.action_delete);
            try {
                onProgressStart(mListener);
                for (Path id : mItems) {
                    /// M: if hasCancelMultiOperation is true, should break the operation.
                    if (jc.isCancelled() || hasCancelMultiOperation) {
                        result = EXECUTION_RESULT_CANCEL;
                        break;
                    }
                    if (isDelete) {
                        if ("cluster".equals(id.getPrefix())) {
                            /// M: this is cluster object, use special logic during delete operation
                            // to avoid delete fail issue
                            MtkLog.w(TAG, "deleting cluster, use special logic for culster object!");
                            ClusterAlbumSet.setClusterDeleteOperation(true);
                        }
                    }
                    if (!execute(manager, jc, mOperation, id)) {
                        result = EXECUTION_RESULT_FAIL;
                    }
                    onProgressUpdate(++index, mListener);
                }
            } catch (Throwable th) {
                Log.e(TAG, "failed to execute operation " + mOperation
                        + " : " + th);
            } finally {
               if (isDelete) {
                   /// M: For cluster object delete operation. should refreshAll at last.
                   boolean isDeleteOperation = ClusterAlbumSet.getClusterDeleteOperation();
                   ClusterAlbumSet.setClusterDeleteOperation(false);
                   if (isDeleteOperation) {
                       MtkLog.w(TAG, "deleting cluster complete, force reload all!");
                       manager.forceRefreshAll();
                   }
               }
                /// M: while the multi-operation finish,and Gallery have paused at the time.
                //     set AlbumSetPage.shouldHideToast as true.
                if (mActivity.hasPausedActivity() && isMultiOperation) {
                    mActivity.setHideToast(true);
                }
                hasCancelMultiOperation = false;
                isMultiOperation = false;
               onProgressComplete(result, mListener);
            }
            return null;
        }
    }


    // added for stereo 3D menu switching
    private static void updateStereoMenu(Menu menu, int supported) {
        if (!MediatekFeature.isStereoDisplaySupported()) return;
        MenuItem item = menu.findItem(R.id.action_switch_stereo_mode);
        if (item == null) {
            return;
        }
        if ((supported & MediaObject.SUPPORT_SWITCHTO_2D) != 0) {
            item.setTitle(R.string.stereo3d_mode_switchto_2d);
            item.setIcon(R.drawable.ic_switch_to_2d);
        } else if ((supported & MediaObject.SUPPORT_SWITCHTO_3D) != 0) {
            item.setTitle(R.string.stereo3d_mode_switchto_3d);
            item.setIcon(R.drawable.ic_switch_to_3d);
        }
    }

    private void clickStereoPhoto(int action, ProgressListener listener,
                                  String message) {
        //special process for stereo photo
        final int menuTitle = R.string.stereo3d_convert2d_dialog_title;
        final ProgressListener menuListener = listener;
        final int menuAction = action;

        final AlertDialog.Builder builder =
                            new AlertDialog.Builder((Context)mActivity);
        DialogInterface.OnClickListener clickListener =
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (DialogInterface.BUTTON_POSITIVE == which) {
                        //if crop is clickde, show dialog, but do not
                        //convert to 2d
                        if (R.id.action_crop == menuAction) {
                            startCropIntent();
                            return;
                        }
                        startAction(menuAction, menuTitle, menuListener);
                    }
                    dialog.dismiss();
                }
            };
        builder.setPositiveButton(android.R.string.ok, clickListener);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setTitle(menuTitle)
               .setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startCropIntent() {
        DataManager manager = mActivity.getDataManager();
        Path path = getSingleSelectedPath();
        MediaObject obj = manager.getMediaObject(path);
        String mimeType = getMimeType(manager.getMediaType(path));
        Intent intent = new Intent(CropImage.ACTION_CROP)
                .setDataAndType(manager.getContentUri(path), mimeType);
        ((Activity) mActivity).startActivity(intent);
    }

    private static void setMenuItemEnable(
            Menu menu, int id, boolean enabled) {
        MenuItem item = menu.findItem(id);
        if (item != null) item.setEnabled(enabled);
    }

    public static void updateSupportedMenuEnabled(Menu menu, int supported, boolean enabled) {
        boolean supportDelete = (supported & MediaObject.SUPPORT_DELETE) != 0;
        boolean supportRotate = (supported & MediaObject.SUPPORT_ROTATE) != 0;
        boolean supportCrop = (supported & MediaObject.SUPPORT_CROP) != 0;
        boolean supportShare = (supported & MediaObject.SUPPORT_SHARE) != 0;
        boolean supportSetAs = (supported & MediaObject.SUPPORT_SETAS) != 0;
        boolean supportShowOnMap = (supported & MediaObject.SUPPORT_SHOW_ON_MAP) != 0;
        boolean supportCache = (supported & MediaObject.SUPPORT_CACHE) != 0;
        boolean supportEdit = (supported & MediaObject.SUPPORT_EDIT) != 0;
        boolean supportInfo = (supported & MediaObject.SUPPORT_INFO) != 0;
        boolean supportImport = (supported & MediaObject.SUPPORT_IMPORT) != 0;
        //add for Bluetooth Print feature
        boolean supportPrint = ((supported & MediaObject.SUPPORT_PRINT) != 0) &&
                               MediatekFeature.isBluetoothPrintSupported();
        //add fro drm protection info
        boolean supportDrmInfo = (supported & MediaObject.SUPPORT_DRM_INFO) != 0;

        if (supportDelete) {
        	setMenuItemEnable(menu, R.id.action_delete, enabled);
        }
        if (supportRotate) {
        	setMenuItemEnable(menu, R.id.action_rotate_ccw, enabled);
        	setMenuItemEnable(menu, R.id.action_rotate_cw, enabled);
        }
        if (supportCrop) {
        	setMenuItemEnable(menu, R.id.action_crop, enabled);
        }
        if (supportShare) {
        	setMenuItemEnable(menu, R.id.action_share, enabled);
        }
        if (supportSetAs) {
        	setMenuItemEnable(menu, R.id.action_setas, enabled);
        }
        if (supportShowOnMap) {
        	setMenuItemEnable(menu, R.id.action_show_on_map, enabled);
        }
        if (supportEdit) {
        	setMenuItemEnable(menu, R.id.action_edit, enabled);
        }
        if (supportInfo) {
        	setMenuItemEnable(menu, R.id.action_details, enabled);
        }
        if (supportImport) {
        	setMenuItemEnable(menu, R.id.action_import, enabled);
        }
        //add for Bluetooth Print feature
        if (supportPrint) {
        	setMenuItemEnable(menu, R.id.action_print, enabled);
        }
        //add for drm protection feature
        if (supportDrmInfo) {
        	setMenuItemEnable(menu, R.id.action_protect_info, enabled);
        }
    }

    private void appendMessageForSingleId(ProgressDialog dialog, ArrayList<Path> ids) {
        if (ids.size() == 1) {
            String message = null;
            MediaObject obj = mActivity.getDataManager().getMediaObject(ids.get(0));
            if (obj == null) {
                return;
            }
            if (obj instanceof MediaItem) {
                message = ((MediaItem) obj).getName();
            } else if (obj instanceof MediaSet) {
                message = ((MediaSet) obj).getName();
            }
            if (message != null) {
                mDialog.setMessage(message);
            }
        }
    }
}
