package com.mediatek.gallery3d.ext;

public class MovieStrategy implements IMovieStrategy {
    @Override
    public boolean shouldEnableNMP(IMovieItem item) {
        return false;
    }

    @Override
    public boolean shouldEnableCheckLongSleep() {
        return true;
    }

    @Override
    public boolean shouldEnableServerTimeout() {
        return false;
    }
    
    @Override
    public boolean shouldEnableRewindAndForward() {
        return false;
    }
}
