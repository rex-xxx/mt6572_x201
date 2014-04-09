package com.android.internal.telephony;

public class CellBroadcastConfigInfo {
    /*
    * 0 --> CB config is activated
    * 1 --> CB config is deactivated
    */
    public int mode = 1;
    
    public String channelConfigInfo = null;
    
    public String languageConfigInfo = null;
    
    public boolean isAllLanguageOn = false;
    
    CellBroadcastConfigInfo(int mode, String channels, String languages, boolean allOn) {
        this.mode = mode;
        this.channelConfigInfo = channels;
        this.languageConfigInfo = languages;
        this.isAllLanguageOn = allOn;
    }
    
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("CellBroadcastConfigInfo: mode = ");
        ret.append(mode);
        ret.append(", channel = ");
        ret.append(channelConfigInfo);
        ret.append(", language = ");
        if (!isAllLanguageOn) {
            ret.append(languageConfigInfo);
        } else {
            ret.append("all");
        }
        return ret.toString();
    }
}