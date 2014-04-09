package com.android.camera;

import android.location.Location;
import android.net.Uri;

public interface SaveRequest {
    void prepareRequest();
    void addRequest();
    void saveRequest();
    Thumbnail createThumbnail(int thumbnailWidth);
    
    void setIgnoreThumbnail(boolean ignore);
    boolean isIgnoreThumbnail();
    String getTempFilePath();
    String getFilePath();
    int getDataSize();
    void setData(byte[] data);
    void setDuration(long duration);
    Uri getUri();
    void setJpegRotation(int rotation);
    Location getLocation();
    void setListener(FileSaver.FileSaverListener listener);
    void notifyListener();
    void setLocation(Location loc);
    void setTempPath(String path);
    void updateDataTaken(long time);
    void saveSync();
    int getJpegRotation();
}
