/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.dreams.phototable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.lang.reflect.Field;

public class PreviewStubActivity extends Activity {

    private static final String TAG = PreviewStubActivity.class.getSimpleName();
    private PhotoTable mTable;
    private PhotoCarousel mCarousel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String dreamType = intent.getStringExtra("dream");
        boolean showEmpty = intent.getBooleanExtra("empty", false);
        if (null == dreamType || "flip".equals(dreamType)) {
            AlbumSettings settings = AlbumSettings
                    .getAlbumSettings(getSharedPreferences(
                            FlipperDreamSettings.PREFS_NAME, 0));
            if (settings.isConfigured() && !showEmpty) {
                setContentView(R.layout.carousel);
                PhotoCarousel carousel = (PhotoCarousel) findViewById(R.id.carousel);
                mCarousel = carousel;
                setFieldValue(PhotoCarousel.class, carousel, "mDropPeriod",
                        3000);
                setFieldValue(PhotoCarousel.class, carousel, "mFlipDuration",
                        1000);
                int limit = intent.getIntExtra("badlimit", 10);
                PhotoSource photoSource = (PhotoSource) getObjectValue(carousel
                        .getClass(), "mPhotoSource", carousel);
                setFieldValue(PhotoSource.class, photoSource,
                        "mBadImageSkipLimit", limit);
            } else {
                setContentView(R.layout.bummer);
                BummerView bummer = (BummerView) findViewById(R.id.bummer);
                setFieldValue(BummerView.class, bummer, "mDelay", 3000);
                setFieldValue(BummerView.class, bummer, "mAnimTime", 1000);
            }
        } else if ("table".equals(dreamType)) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            AlbumSettings settings = AlbumSettings
                    .getAlbumSettings(getSharedPreferences(
                            PhotoTableDreamSettings.PREFS_NAME, 0));
            ViewGroup view = null;
            if (settings.isConfigured() && !showEmpty) {
                view = (ViewGroup) inflater.inflate(R.layout.table, null);
                PhotoTable table = (PhotoTable) view.findViewById(R.id.table);
                table.setDream(null);
                mTable = table;
                int fast = intent.getIntExtra("fast", 4000);
                setFieldValue(PhotoTable.class, table, "mFastDropPeriod", fast);
                int limit = intent.getIntExtra("badlimit", 10);
                PhotoSource photoSource = (PhotoSource) getObjectValue(
                        PhotoTable.class, "mPhotoSource", table);
                setFieldValue(PhotoSource.class, photoSource,
                        "mBadImageSkipLimit", limit);
            } else {
                Resources resources = getResources();
                view = (ViewGroup) inflater.inflate(R.layout.bummer, null);
                BummerView bummer = (BummerView) view.findViewById(R.id.bummer);
                bummer.setAnimationParams(true, 3000, 1000);
            }
            setContentView(view);
        }
    }

    @Override
    protected void onDestroy() {
        PhotoCarousel carousel = (PhotoCarousel) findViewById(R.id.carousel);
        if (null != carousel) {
            carousel.removeTasks();
        }
        PhotoTable table = (PhotoTable) findViewById(R.id.table);
        if (null != table) {
            table.removeTasks();
        }
        mTable = null;
        mCarousel = null;
        super.onDestroy();
    }

    private static void setFieldValue(Class cls, Object obj, String fieldName,
            Object value) {
        Field field = null;
        try {
            field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "setFieldValue NoSuchFieldException: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "setFieldValue IllegalArgumentException: "
                    + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(TAG, "setFieldValue IllegalAccessException: "
                    + e.getMessage());
        }
    }

    public static Object getObjectValue(Class cls, String fieldName,
            Object targetObject) {
        Field field = null;
        Object result = null;
        try {
            field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            result = field.get(targetObject);
        } catch (NoSuchFieldException e) {
            Log
                    .e(TAG, "getObjectValue NoSuchFieldException: "
                            + e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getObjectValue IllegalArgumentException: "
                    + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(TAG, "getObjectValue IllegalAccessException: "
                    + e.getMessage());
        }
        return result;
    }

}
