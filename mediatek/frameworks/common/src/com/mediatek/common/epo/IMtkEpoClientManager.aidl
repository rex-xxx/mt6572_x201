package com.mediatek.common.epo;

import android.os.Bundle;
import com.mediatek.common.epo.IMtkEpoStatusListener;
import com.mediatek.common.epo.MtkEpoFileInfo;

interface IMtkEpoClientManager {
    void enable();
    void disable();
    boolean getStatus();
    int getProgress();
    
    void enableAutoDownload(boolean enable);
    boolean getAutoDownloadStatus();
    
    void addStatusListener(in IMtkEpoStatusListener listener);
    void removeStatusListener(in IMtkEpoStatusListener listener);
    
    int startDownload();
    void stopDownload();
    MtkEpoFileInfo getEpoFileInfo();

    void setUpdatePeriod(long interval); //milliseconds
    long getUpdatePeriod(); //milliseconds
    void setTimeout(int timeout); //seconds for socket connect/read
    void setRetryTimes(int times);
    void setProfile(String addr, int port, String userName, String password);
    int extraCommand(String cmd, in Bundle extra);
}

