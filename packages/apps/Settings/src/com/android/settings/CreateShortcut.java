/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import android.app.LauncherActivity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.mediatek.common.featureoption.FeatureOption;

import java.util.List;

public class CreateShortcut extends LauncherActivity {
    ///M: add tag and audio profile class name @{
    private static final String TAG = "CreateShortcut";
    private static final String AUDIOPROFILE = "com.android.settings.Settings$AudioProfileSettingsActivity";
    private static final String SOUND = "com.android.settings.Settings$SoundSettingsActivity";
    /// @}
    ///M: ALPS00601692 add DreamSettings class name @{
    private static final String DREAM_SETTINGS = "com.android.settings.Settings$DreamSettingsActivity";
    /// @}

    @Override
    protected Intent getTargetIntent() {
        Intent targetIntent = new Intent(Intent.ACTION_MAIN, null);
        targetIntent.addCategory("com.android.settings.SHORTCUT");
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return targetIntent;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent shortcutIntent = intentForPosition(position);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher_settings));
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, itemForPosition(position).label);
        setResult(RESULT_OK, intent);
        finish();
    }

    // /M: option of mtk audio profile and google default sound @{
    @Override
    public List<ListItem> makeListItems() {
        List<ListItem> list = super.makeListItems();
        int listSize = list.size();
        if (FeatureOption.MTK_AUDIO_PROFILES) {
            for (int i = 0; i < listSize; i++) {
                if (SOUND.equals(list.get(i).className)) {
                    Log.d(TAG, "Not support google sound ,remove it");
                    list.remove(i);
                    break;
                }
            }
        } else {
            for (int i = 0; i < listSize; i++) {
                if (AUDIOPROFILE.equals(list.get(i).className)) {
                    Log.d(TAG, "Not support mtk audio profle ,remove it");
                    list.remove(i);
                    break;
                }
            }
        }
        ///M: ALPS00601692 remove DreamSettings when dreamsSupported is false @{
        if (!getResources().getBoolean(com.android.internal.R.bool.config_dreamsSupported)) {
            listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                if (DREAM_SETTINGS.equals(list.get(i).className)) {
                    Log.d(TAG, "Not support dream settings, remove it");
                    list.remove(i);
                    break;
                }
            }
        }
        /// @}
        return list;
    }
    // / @}
    
    @Override
    protected boolean onEvaluateShowIcons() {
        return false;
    }
}
