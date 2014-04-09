
package com.mediatek.contacts.util;

import android.net.Uri;
import android.util.Log;

public class SimContactPhotoUtils {
    private static final String TAG = "SimContactPhotoUtils";

    public interface SimPhotoIdAndUri {

        int DEFAULT_SIM_PHOTO_ID = -1;

        int SIM_PHOTO_ID_BLUE_SDN = -5;
        int SIM_PHOTO_ID_ORANGE_SDN = -6;
        int SIM_PHOTO_ID_GREEN_SDN = -7;
        int SIM_PHOTO_ID_PURPLE_SDN = -8;

        int DEFAULT_SIM_PHOTO_ID_SDN = -9;

        int SIM_PHOTO_ID_BLUE = -10;
        int SIM_PHOTO_ID_ORANGE = -11;
        int SIM_PHOTO_ID_GREEN = -12;
        int SIM_PHOTO_ID_PURPLE = -13;

        String DEFAULT_SIM_PHOTO_URI = "content://sim";

        String SIM_PHOTO_URI_BLUE_SDN = "content://sdn-5";
        String SIM_PHOTO_URI_ORANGE_SDN = "content://sdn-6";
        String SIM_PHOTO_URI_GREEN_SDN = "content://sdn-7";
        String SIM_PHOTO_URI_PURPLE_SDN = "content://sdn-8";

        String DEFAULT_SIM_PHOTO_URI_SDN = "content://sdn";

        String SIM_PHOTO_URI_BLUE = "content://sim-10";
        String SIM_PHOTO_URI_ORANGE = "content://sim-11";
        String SIM_PHOTO_URI_GREEN = "content://sim-12";
        String SIM_PHOTO_URI_PURPLE = "content://sim-13";
    }
    
    public interface SimPhotoColors {
        int BLUE = 0;
        int ORANGE = 1;
        int GREEN = 2;
        int PURPLE = 3;
    }

    public static long getPhotoIdByPhotoUri(Uri uri) {
        long id = 0;

        if (uri == null) {
            Log.e(TAG, "getPhotoIdByPhotoUri uri is null");
            return id;
        }

        String photoUri = uri.toString();
        Log.i(TAG, "getPhotoIdByPhotoUri uri : " + photoUri);

        if (SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI.equals(photoUri)) {
            id = SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID;
        } else if (SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID_SDN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE_SDN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE_SDN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN_SDN;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE;
        } else if (SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE_SDN.equals(photoUri)) {
            id = SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE_SDN;
        }
        Log.i(TAG, "getPhotoIdByPhotoUri id : " + id);
        return id;
    }

    public static boolean isSimPhotoUri(Uri uri) {
        if (null == uri) {
            Log.e(TAG, "isSimPhotoUri uri is null");
            return false;
        }

        String photoUri = uri.toString();
        Log.i(TAG, "isSimPhotoUri uri : " + photoUri);

        if (SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI.equals(photoUri)
                || SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI_SDN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE_SDN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE_SDN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN_SDN.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE.equals(photoUri)
                || SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE_SDN.equals(photoUri)) {
            return true;
        }
        
        return false;
    }

    public static boolean isSimPhotoId(long photoId) {
        Log.i(TAG, "isSimPhotoId photoId : " + photoId);
        if (photoId == SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID
                || photoId == SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID_SDN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE_SDN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN_SDN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE_SDN
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE
                || photoId == SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE_SDN) {
            return true;
        }
        
        return false;
    }

    public String getPhotoUri(int isSdnContact, int colorId) {
        String photoUri = null;

        boolean isSdn = (isSdnContact > 0);
        Log.i(TAG, "[onLoadFinished] i = " + colorId + " | isSdn : " + isSdn);

        switch (colorId) {
            case SimContactPhotoUtils.SimPhotoColors.BLUE:
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_BLUE;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.ORANGE:
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_ORANGE;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.GREEN:
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_GREEN;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.PURPLE:
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_URI_PURPLE;
                }
                break;
            default:
                Log.i(TAG, "no match color");
                if (isSdn) {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI_SDN;
                } else {
                    photoUri = SimContactPhotoUtils.SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_URI;
                }
                break;
        }

        return photoUri;
    }

    public long getPhotoId(int isSdnContact, int colorId) {
        long photoId = 0;
        boolean isSdn = (isSdnContact > 0);
        Log.i(TAG, "[getSimType] i = " + colorId + " | isSdn : " + isSdn);
        switch (colorId) {
            case SimContactPhotoUtils.SimPhotoColors.BLUE:
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_BLUE;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.ORANGE:
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_ORANGE;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.GREEN:
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_GREEN;
                }
                break;
            case SimContactPhotoUtils.SimPhotoColors.PURPLE:
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.SIM_PHOTO_ID_PURPLE;
                }
                break;
            default:
                Log.i(TAG, "no match color");
                if (isSdn) {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID_SDN;
                } else {
                    photoId = SimContactPhotoUtils.SimPhotoIdAndUri.DEFAULT_SIM_PHOTO_ID;
                }
                break;
        }
        return photoId;
    }

}
