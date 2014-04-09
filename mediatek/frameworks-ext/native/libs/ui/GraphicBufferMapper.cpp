/*
 * Copyright (C) 2007 The Android Open Source Project
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

#define LOG_TAG "GraphicBufferMapper"
#define ATRACE_TAG ATRACE_TAG_GRAPHICS

#include <stdint.h>
#include <errno.h>

#include <utils/Errors.h>
#include <utils/Trace.h>

#include <cutils/xlog.h>

#include <ui/GraphicBufferMapper.h>
#include <ui/Rect.h>

#include <hardware/gralloc.h>


namespace android {
// ---------------------------------------------------------------------------

GraphicBufferMapper::~GraphicBufferMapper()
{
    gralloc_extra_close(mExtraDev);
}

status_t GraphicBufferMapper::getIonFd(buffer_handle_t handle, int *idx, int *num)
{
    ATRACE_CALL();
    status_t err;

    if (!mExtraDev) {
        XLOGE("gralloc extra device is not supported");
        return INVALID_OPERATION;
    }

    err = mExtraDev->getIonFd(mExtraDev, handle, idx, num);

    ALOGW_IF(err, "getIonFd(...) failed %d (%s)", err, strerror(-err));
    return err;
}

// ---------------------------------------------------------------------------
}; // namespace android
