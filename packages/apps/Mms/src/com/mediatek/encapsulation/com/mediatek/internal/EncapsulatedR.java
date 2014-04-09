
package com.mediatek.encapsulation.com.mediatek.internal;

import com.mediatek.encapsulation.EncapsulationConstant;

import com.android.mms.R;

/*** M: MTK ADD */
public class EncapsulatedR {

    public static final int DEFAULTVALUE = 0;

    public static class drawable {

        public static final int drm_red_lock;

        public static final int drm_green_lock;

        public static final int sim_locked;

        public static final int sim_radio_off;

        public static final int sim_invalid;

        public static final int sim_searching;

        public static final int sim_roaming;

        public static final int sim_connected;

        public static final int sim_roaming_connected;

        public static final int sim_background_locked;

        static {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                drm_red_lock = com.mediatek.internal.R.drawable.drm_red_lock;
                drm_green_lock = com.mediatek.internal.R.drawable.drm_green_lock;
                sim_locked = com.mediatek.internal.R.drawable.sim_locked;
                sim_radio_off = com.mediatek.internal.R.drawable.sim_radio_off;
                sim_invalid = com.mediatek.internal.R.drawable.sim_invalid;
                sim_searching = com.mediatek.internal.R.drawable.sim_searching;
                sim_roaming = com.mediatek.internal.R.drawable.sim_roaming;
                sim_connected = com.mediatek.internal.R.drawable.sim_connected;
                sim_roaming_connected = com.mediatek.internal.R.drawable.sim_roaming_connected;
                sim_background_locked = R.drawable.sim_background_locked;

            } else {
                /** M: Can not complete for this branch. */
                drm_red_lock = DEFAULTVALUE;
                drm_green_lock = DEFAULTVALUE;
                sim_locked = DEFAULTVALUE;
                sim_radio_off = DEFAULTVALUE;
                sim_invalid = DEFAULTVALUE;
                sim_searching = DEFAULTVALUE;
                sim_roaming = DEFAULTVALUE;
                sim_connected = DEFAULTVALUE;
                sim_roaming_connected = DEFAULTVALUE;
                sim_background_locked = R.drawable.sim_background_locked;
            }

        }
    }

    public static class string {

        public static final int url_dialog_choice_title;

        public static final int url_dialog_choice_message;

        static {
            if (EncapsulationConstant.USE_MTK_PLATFORM) {
                url_dialog_choice_message = com.mediatek.internal.R.string.url_dialog_choice_message;
                url_dialog_choice_title = com.mediatek.internal.R.string.url_dialog_choice_title;
            } else {
                /** M: Can not complete for this branch. */
                url_dialog_choice_message = DEFAULTVALUE;
                url_dialog_choice_title = DEFAULTVALUE;
            }

        }

    }

}
