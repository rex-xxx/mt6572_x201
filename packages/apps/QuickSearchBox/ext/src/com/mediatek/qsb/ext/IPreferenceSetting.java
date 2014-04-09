package com.mediatek.qsb.ext;

import android.content.Context;

import com.mediatek.common.search.SearchEngineInfo;

public interface IPreferenceSetting {
    SearchEngineInfo getDefaultSearchEngine(Context context);
}
