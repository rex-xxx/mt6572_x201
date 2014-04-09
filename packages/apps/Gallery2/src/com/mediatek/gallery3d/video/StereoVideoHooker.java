package com.mediatek.gallery3d.video;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.android.gallery3d.R;
import com.mediatek.gallery3d.ext.IMovieItem;
import com.mediatek.gallery3d.ext.MtkLog;

public class StereoVideoHooker extends MovieHooker {
    private static final String TAG = "Gallery2/VideoPlayer/StereoVideoHooker";
    private static final boolean LOG = true;

    private static final int UNKNOWN = -1;
    public static final int STEREO_TYPE_2D = MediaStore.Video.Media.STEREO_TYPE_2D;
    public static final int STEREO_TYPE_3D = MediaStore.Video.Media.STEREO_TYPE_SIDE_BY_SIDE;
    private static final String EXTRA_STEREO_TYPE = "mediatek.intent.extra.STEREO_TYPE";
    private static final String COLUMN_STEREO_TYPE = MediaStore.Video.Media.STEREO_TYPE;
    private int mCurrentStereoType;
    private static final int MENU_STEREO_VIDEO = 1;
    private MenuItem mMenuStereoVideo;
    private SurfaceView mVideoSurface;
    
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int stereoType = getIntent().getIntExtra(EXTRA_STEREO_TYPE, UNKNOWN);
        if (stereoType == UNKNOWN) {
            enhanceStereoActionBar(getMovieItem(), stereoType);
        }
        if (stereoType != UNKNOWN) {
            getMovieItem().setStereoType(stereoType);
        }
        initialStereoVideoIcon(getMovieItem().getStereoType());
        setStereoType(getMovieItem().getStereoType());
    }
    
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        mMenuStereoVideo = menu.add(0, getMenuActivityId(MENU_STEREO_VIDEO), 0, R.string.stereo3d_mode_switchto_2d);
        mMenuStereoVideo.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        initialStereoVideoIcon(getMovieItem().getStereoType());
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch(getMenuOriginalId(item.getItemId())) {
        case MENU_STEREO_VIDEO:
            int stereoType = getStereoType();
            if (isStereo3D(stereoType)) {
                stereoType = STEREO_TYPE_2D;
            } else {
                stereoType = STEREO_TYPE_3D;
            }
            setStereoType(stereoType);
            updateStereoVideoIcon();
            return true;
        default:
            return false;
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateStereoVideoIcon();
        return true;
    }
    
    @Override
    public void onMovieItemChanged(final IMovieItem item) {
        super.onMovieItemChanged(item);
        setStereoType(item.getStereoType());
        initialStereoVideoIcon(item.getStereoType());
    }
    
    @Override
    public void setParameter(final String key, final Object value) {
        super.setParameter(key, value);
        if (value instanceof SurfaceView) {
            mVideoSurface = (SurfaceView) value;
            setStereoType(getMovieItem().getStereoType());
        }
    }
    
    public static boolean isStereo3D(final int stereoType) {
        boolean stereo3d = true;
        if (stereoType == UNKNOWN || STEREO_TYPE_2D == stereoType) {
            stereo3d = false;
        }
        if (LOG) {
            MtkLog.v(TAG, "isStereo3D(" + stereoType + ") return " + stereo3d);
        }
        return stereo3d;
    }
    
    public int getStereoType() {
        if (LOG) {
            MtkLog.v(TAG, "getStereoType() return " + mCurrentStereoType);
        }
        return mCurrentStereoType;
    }

    public void setStereoType(final int stereoType) {
        if (LOG) {
            MtkLog.v(TAG, "setStereoType(" + stereoType + ") mVideoSurface=" + mVideoSurface);
        }
        mCurrentStereoType = stereoType;
        final int flag = (isStereo3D(stereoType) ? WindowManager.LayoutParams.LAYOUT3D_SIDE_BY_SIDE
                : WindowManager.LayoutParams.LAYOUT3D_DISABLED);
        if (mVideoSurface != null) {
            mVideoSurface.set3DLayout(flag);
        }
    }
    
    private void updateStereoVideoIcon() {
        if (mMenuStereoVideo != null) {
            final boolean current3D = isStereo3D(getStereoType());
            if (current3D) {
                mMenuStereoVideo.setIcon(R.drawable.ic_switch_to_2d);
                mMenuStereoVideo.setTitle(R.string.stereo3d_mode_switchto_2d);
            } else {
                mMenuStereoVideo.setIcon(R.drawable.ic_switch_to_3d);
                mMenuStereoVideo.setTitle(R.string.stereo3d_mode_switchto_3d);
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "updateStereoVideoIcon() mMenuStereoVideoIcon=" + mMenuStereoVideo);
        }
    }
    
    private void initialStereoVideoIcon(final int stereoType) {
        if (mMenuStereoVideo != null) {
            mMenuStereoVideo.setVisible(isStereo3D(stereoType));
            updateStereoVideoIcon();
        }
        if (LOG) {
            MtkLog.v(TAG, "initialStereoVideoIcon(" + stereoType + ") mSupport3DIcon=" + mMenuStereoVideo);
        }
    }
    
    private void enhanceStereoActionBar(final IMovieItem movieItem, final int stereoType) {
        final String scheme = movieItem.getUri().getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) { //from file manager
            if (stereoType == UNKNOWN) {
                setInfoFromMediaData(movieItem, stereoType);
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            if ("media".equals(movieItem.getUri().getAuthority())) { //from media database
                if (stereoType == UNKNOWN) {
                    setInfoFromMediaUri(movieItem, stereoType);
                }
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "enhanceStereoActionBar() " + movieItem);
        }
    }
    
    private void setInfoFromMediaUri(final IMovieItem movieItem, final int stereoType) {
        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(movieItem.getUri(),
                    new String[]{COLUMN_STEREO_TYPE}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                if (stereoType == UNKNOWN) {
                    movieItem.setStereoType(cursor.getInt(0));
                }
           }
        } catch (final SQLiteException ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "setInfoFromMediaUri() " + movieItem);
        }
    }
    
    private void setInfoFromMediaData(final IMovieItem movieInfo, final int stereoType) {
        Cursor cursor = null;
        try {
            String data = Uri.decode(movieInfo.getUri().toString());
            data = data.replaceAll("'", "''");
            final String where = "_data LIKE '%" + data.replaceFirst("file:///", "") + "'";
            cursor = getContext().getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{COLUMN_STEREO_TYPE}, where, null, null);
            MtkLog.v(TAG, "setInfoFromMediaData() cursor=" + cursor.getCount());
            if (cursor != null && cursor.moveToFirst()) {
                if (stereoType == UNKNOWN) {
                    movieInfo.setStereoType(cursor.getInt(0));
                }
           }
        } catch (final SQLiteException ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (LOG) {
            MtkLog.v(TAG, "setInfoFromMediaData() " + movieInfo);
        }
    }
}
