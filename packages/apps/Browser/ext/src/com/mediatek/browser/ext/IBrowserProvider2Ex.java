package com.mediatek.browser.ext;

import android.database.sqlite.SQLiteDatabase;

public interface IBrowserProvider2Ex {
    int addDefaultBookmarksHost(SQLiteDatabase db, long parentId,
            CharSequence[] bookmarks, int operatorId, int position);
}