package com.mediatek.gallery3d.video;

import com.mediatek.gallery3d.ext.ActivityHooker;
import com.mediatek.gallery3d.ext.IMovieItem;
import com.mediatek.gallery3d.ext.IMoviePlayer;
import com.mediatek.gallery3d.ext.MtkLog;

public class MovieHooker extends ActivityHooker {
    private static final String TAG = "Gallery2/VideoPlayer/MovieHooker";
    private static final boolean LOG = true;
    private IMovieItem mMovieItem;
    private IMoviePlayer mPlayer;
    
    @Override
    public void setParameter(final String key, final Object value) {
        super.setParameter(key, value);
        if (LOG) {
            MtkLog.v(TAG, "setParameter(" + key + ", " + value + ")");
        }
        if (value instanceof IMovieItem) {
            mMovieItem = (IMovieItem) value;
            onMovieItemChanged(mMovieItem);
        } else if (value instanceof IMoviePlayer) {
            mPlayer = (IMoviePlayer) value;
            onMoviePlayerChanged(mPlayer);
        }
    }
    
    public IMovieItem getMovieItem() {
        return mMovieItem;
    }
    public IMoviePlayer getPlayer() {
        return mPlayer;
    }
    
    public void onMovieItemChanged(final IMovieItem item){}
    public void onMoviePlayerChanged(final IMoviePlayer player){}
}
