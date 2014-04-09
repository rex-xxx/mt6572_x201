package com.mediatek.mms.ext;
import android.app.Activity;

public interface IAppGuideExt {
    /**
     * Called when the app want to show application guide
     * @param activity: The parent activity
     * @param type: The app type, such as "PHONE/CONTACTS/MMS/CAMERA"
     */
    public void showAppGuide(String type);

}
