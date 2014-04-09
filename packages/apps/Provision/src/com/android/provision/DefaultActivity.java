/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.provision;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
/**
 * Application that sets the provisioned bit, like SetupWizard does.
 */
public class DefaultActivity extends Activity {

    private final String mHD720NOPK = "1184x720";
    private final String mWVGAScreenSize = "800x480";
    private final String mQhdScreenSize = "960x540";
    private final String m720P = "1280x720";

    void updateScreenSize() {
        android.view.WindowManager wm = getWindowManager();
        android.view.Display display = wm.getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getSize(size);
        String screenSize = size.y + "x" + size.x;
        if (mHD720NOPK.equals(screenSize)) {
            screenSize = mWVGAScreenSize;
        } else if (mQhdScreenSize.equals(screenSize)) {
            screenSize = mWVGAScreenSize;
        } else if (m720P.equals(screenSize)) {
            screenSize = mWVGAScreenSize;
        }
        Log.i("DefaultActivity", "screen size = " + screenSize);
        android.os.SystemProperties.set("persist.sys.screen.size", screenSize);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        /// M: set the default screen size in system property
        updateScreenSize();

        /// M: For Provison will start ahead of SetupWizard, we need to Check whether SetupWizard exists.
        /// If exist, the provison.apk will not set up Settings.Secure.DEVICE_PROVISIONED
        Boolean setupWizardExists = false;
        PackageManager pm = getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo("com.google.android.setupwizard",PackageManager.GET_ACTIVITIES);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        if (packageInfo != null) {
            ActivityInfo activityInfo = null;
            int len = packageInfo.activities.length;
            for(int i = 0 ; i < len ; i++) {
                String activityName = packageInfo.activities[i].name;
                Log.v("DefaultActivity",activityName);
                if(activityName != null && activityName.equals("com.google.android.setupwizard.SetupWizardActivity")) {
                    setupWizardExists = true;
                    break;
                }
            }
        }

        /// M: SetupWizard exist
        if (!setupWizardExists) {
            // Add a persistent setting to allow other apps to know the device has been provisioned.
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.DEVICE_PROVISIONED, 1);
        }

        // remove this activity from the package manager.
        ComponentName name = new ComponentName(this, DefaultActivity.class);
        pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // terminate the activity.
        finish();
    }
}

