/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#include <gui/SurfaceTexture.h>
#include <GLES2/gl2ext.h>
#include <cutils/xlog.h>
#include <math.h>

// Macros for including the SurfaceTexture name in log messages
#define ST_LOGI(x, ...) ALOGI("[%s] "x, mName.string(), ##__VA_ARGS__)
#define ST_LOGE(x, ...) ALOGE("[%s] "x, mName.string(), ##__VA_ARGS__)


namespace android {

// CAUTION: bind texture should in context thread only
status_t SurfaceTexture::bindToAuxSlotLocked() {
    if (EGL_NO_IMAGE_KHR != mBackAuxSlot->eglSlot.mEglImage) {
        AuxSlot *tmp = mBackAuxSlot;
        mBackAuxSlot = mFrontAuxSlot;
        mFrontAuxSlot = tmp;

        glBindTexture(mTexTarget, mTexName);
        glEGLImageTargetTexture2DOES(mTexTarget, (GLeglImageOES)mFrontAuxSlot->eglSlot.mEglImage);

        // insert fence sync object just after new front texture applied
        EGLSyncKHR eglFence = mFrontAuxSlot->eglSlot.mEglFence;
        if (eglFence != EGL_NO_SYNC_KHR) {
            XLOGI("[%s] fence sync already exists in mFrontAuxSlot:%p, destoryed it", __func__, mFrontAuxSlot);
            eglDestroySyncKHR(mEglDisplay, eglFence);
        }

        eglFence = eglCreateSyncKHR(mEglDisplay, EGL_SYNC_FENCE_KHR, NULL);
        if (eglFence == EGL_NO_SYNC_KHR) {
            XLOGE("[%s] error creating fence: %#x", __func__, eglGetError());
        }
        glFlush();
        mFrontAuxSlot->eglSlot.mEglFence = eglFence;
    }
    mAuxSlotDirty = false;

    return NO_ERROR;
}


status_t SurfaceTexture::convertToAuxSlot(bool isForce) {
    Mutex::Autolock l(mMutex);
    return convertToAuxSlotLocked(isForce);
}

status_t SurfaceTexture::bindToAuxSlot() {
    Mutex::Autolock l(mMutex);
    return bindToAuxSlotLocked();
}

status_t SurfaceTexture::forceAuxConversionLocked() {
    status_t err = NO_ERROR;
    ST_LOGI("[%s] mCurrentTexture:%d, mCurrentBuf:%p",
        __func__, mCurrentTexture, mCurrentTextureBuf.get());

    if ((mCurrentTextureBuf != NULL) &&
        (checkPixelFormatSupported(mCurrentTextureBuf) == OK)) {
        err = convertToAuxSlotLocked(true);
    }

    return err;
}

status_t SurfaceTexture::dumpAux() const {
    Mutex::Autolock l(mMutex);

    sp<GraphicBuffer> gb = mFrontAuxSlot->slot.mGraphicBuffer;
    if (gb != NULL) {
        String8 filename = String8::format("/data/SF_dump/AUX%d.RGBA", mTexName);
        
        status_t lockret;
        uint8_t *ptr;

        lockret = gb->lock(LOCK_FOR_SW, (void**)&ptr);        
        if (NO_ERROR != lockret) {
            ST_LOGE("[%s] buffer lock fail: %s (gb:%p, handle:%p)",
                __func__, strerror(lockret), gb.get(), gb->handle);
			return INVALID_OPERATION;
        } else {
            FILE *f = fopen(filename.string(), "wb");
            fwrite(ptr, gb->stride * gb->height * 4, 1, f);
            fclose(f);
        }
        gb->unlock();
    }

    return NO_ERROR;
}

}

