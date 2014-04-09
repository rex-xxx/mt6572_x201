package com.mediatek.qsb.ext;

import android.content.Context;

public interface IWebSearchHandler {
    boolean handleSearchInternal(Context context, String searchEngineName, String searchUri);
}
