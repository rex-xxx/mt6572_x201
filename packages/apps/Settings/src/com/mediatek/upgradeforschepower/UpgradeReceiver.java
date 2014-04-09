package com.mediatek.upgradeforschepower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mediatek.xlog.Xlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class UpgradeReceiver extends BroadcastReceiver {

    private static final String SCHPWRS_DB_PATH = "data/data/com.android.settings/databases/schpwrs.db";
    private static final String TEMP_DB_PATH = "/data/schpwrs.db";
    private static final String TAG = "UpgradeReceiver";
    private File mSettingSchPwrsDbFile;

    @Override
    public void onReceive(Context context, Intent intent) {
        Xlog.v(TAG, "onReceive = " + intent.getAction());
        mSettingSchPwrsDbFile = new File(SCHPWRS_DB_PATH);
        if (mSettingSchPwrsDbFile.exists()) {
            copyDbFileToPhoneStorage();
            // make sure don't copy db file when next time receive the
            // PRE_BOOT_COMPLETED intent but not for MOTA
            if (!mSettingSchPwrsDbFile.delete()) {
                Xlog.w(TAG, "delete settings db file failed.");
            }
        } else {
            Xlog.w(TAG, "com.android.settings/databases/schpwrs.db does not exist");
        }
    }

    private void copyDbFileToPhoneStorage() {
        Xlog.v(TAG, "copyDbFileToPhoneStorage()");
        File tempDbFile = new File(TEMP_DB_PATH);
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(mSettingSchPwrsDbFile);
            fos = new FileOutputStream(tempDbFile);
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } catch (FileNotFoundException e) {
            Xlog.w(TAG, "FileNotFoundException " + e.getMessage());
            return;
        } catch (IOException e) {
            Xlog.w(TAG, "IOException " + e.getMessage());
            return;
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e2) {
                Xlog.w(TAG, "IOException " + e2.getMessage());
                return;
            }
        }
        Xlog.v(TAG, "Copy done return true");
    }
}
