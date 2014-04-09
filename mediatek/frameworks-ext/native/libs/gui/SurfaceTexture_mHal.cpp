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

#define ATRACE_TAG ATRACE_TAG_GRAPHICS

#include <gui/BufferQueue.h>
#include <gui/SurfaceTexture.h>
#include <GLES2/gl2ext.h>
#include <cutils/xlog.h>

#include <dlfcn.h>
#include <assert.h>

#include "MediaHal.h"

#include <utils/Trace.h>

namespace android {

#define LOCK_FOR_MDP (GRALLOC_USAGE_SW_READ_RARELY | GRALLOC_USAGE_SW_WRITE_NEVER | GRALLOC_USAGE_HW_TEXTURE)
#define ALIGN(x,a)  (((x) + (a) - 1L) & ~((a) - 1L))

typedef int (*TYPE_registerLoopMemory)(mHalMVALOOPMEM_CLIENT, mHalRegisterLoopMemory_t*, mHalRegisterLoopMemoryObj_t*) ;
typedef int (*TYPE_unregisterLoopMemory)(mHalMVALOOPMEM_CLIENT, mHalRegisterLoopMemoryObj_t*);
typedef int (*TYPE_ipcBitBlt)(mHalBltParam_t*);
TYPE_registerLoopMemory registerLoopMemory = NULL;
TYPE_unregisterLoopMemory unregisterLoopMemory = NULL;
TYPE_ipcBitBlt ipcBitBlt = NULL;

void *libmhalmdp = NULL;
uint32_t gMvaCnt = 0;
Mutex gMvaLock;


// MVA operations, internal use
status_t deinitMDPLocked() {
    XLOGI("[%s] close libmhalmdp.so, handle:%p", __func__, libmhalmdp);

    dlclose(libmhalmdp);
    libmhalmdp = NULL;
    registerLoopMemory = NULL;
    unregisterLoopMemory = NULL;
    ipcBitBlt = NULL;

    return NO_ERROR;
}

status_t initMDPLocked() {
    libmhalmdp = dlopen("/system/lib/libmhalmdp.so", RTLD_LAZY);
    if (NULL == libmhalmdp) {
        XLOGE("[%s] open libmhalmdp.so FAILED", __func__);
        deinitMDPLocked();
        return NAME_NOT_FOUND;
    } else {
        XLOGI("[%s] open libmhalmdp.so success, handle:%p", __func__, libmhalmdp);
    }

    registerLoopMemory = (TYPE_registerLoopMemory)dlsym(libmhalmdp, "mHalMdp_RegisterLoopMemory");
    unregisterLoopMemory = (TYPE_unregisterLoopMemory)dlsym(libmhalmdp, "mHalMdp_UnRegisterLoopMemory");
    ipcBitBlt = (TYPE_ipcBitBlt)dlsym(libmhalmdp, "mHalMdpIpc_BitBlt");
    if ((registerLoopMemory == NULL) || (unregisterLoopMemory == NULL) || (ipcBitBlt == NULL)) {
        XLOGE("    open libmhalmdp symbols FAILED");
        deinitMDPLocked();
        return NAME_NOT_FOUND;
    } else {
        XLOGI("    open libmhalmdp symbols success");
        XLOGI("        registerLoopMemory:%p", registerLoopMemory);
        XLOGI("        unregisterLoopMemory:%p", unregisterLoopMemory);
        XLOGI("        ipcBitBlt:%p", ipcBitBlt);
    }

    return NO_ERROR;
}

mHalRegisterLoopMemoryObj_t *registerMva(sp<GraphicBuffer> gb) {
    if ((HAL_PIXEL_FORMAT_YV12 != gb->format) && (HAL_PIXEL_FORMAT_RGBA_8888 != gb->format)) {
        XLOGW("[%s] only for YV12/RGBA_8888 now", __func__);
        return NULL;
    }

    void *ptr;
    mHalRegisterLoopMemory_t para;
    mHalRegisterLoopMemoryObj_t *ret;

    gb->lock(LOCK_FOR_MDP, &ptr);          // get VA
    gb->unlock();

    if (HAL_PIXEL_FORMAT_YV12 == gb->format) {
        para.mhal_color = MHAL_FORMAT_IMG_YV12;
        para.buffer_size = gb->stride * gb->height * 3 / 2;
    } else if (HAL_PIXEL_FORMAT_RGBA_8888 == gb->format) {
        para.mhal_color = MHAL_FORMAT_ABGR_8888;
        para.buffer_size = gb->stride * gb->height * 4;
    }
    para.mem_type = MHAL_MEM_TYPE_OUTPUT;
    para.addr     = (uint32_t)ptr;
    para.img_size = mHalMdpSize(gb->stride, gb->height);
    para.img_roi  = mHalMdpRect(0, 0, gb->stride, gb->height);
    para.rotate   = MHAL_BITBLT_ROT_0;

    // lock here for manipulate MVA and MDP
    Mutex::Autolock l(gMvaLock);

    // if first time to use MDP/MVA, init these utilities
    if (0 == gMvaCnt) {
        initMDPLocked();
    }
    assert((NULL != libmhalmdp) && (NULL != registerLoopMemory));

    ret = (new mHalRegisterLoopMemoryObj_t());
    if (0 <= registerLoopMemory(MHAL_MLM_CLIENT_SFTEX, &para, ret)) {
        gMvaCnt += 1;
        XLOGI("    register MVA success, srcImgYAddr:%p, Aux MVA count:%d", ret->calc_addr->y, gMvaCnt);
    } else {
        delete ret;
        ret = NULL;
        XLOGW("    register MVA FAILED, maybe M4U permission deny");
    }

    return ret;
}

status_t unregisterMva(mHalRegisterLoopMemoryObj_t *mva) {
    if (NULL == mva) {
        XLOGW("[%s] NULL mva object, cannot unregister it", __func__);
        return INVALID_OPERATION;
    }

    // lock here for manipulate MVA and MDP
    Mutex::Autolock l(gMvaLock);

    assert((NULL != libmhalmdp) && (NULL != unregisterLoopMemory));

    status_t ret = NO_ERROR;
    if (0 <= unregisterLoopMemory(MHAL_MLM_CLIENT_SFTEX, mva)) {
        gMvaCnt -= 1;
        delete mva;
        XLOGI("    unregister MVA success, srcImgYAddr:%p, Aux MVA count:%d", mva->calc_addr->y, gMvaCnt);
        ret = NO_ERROR;
    } else {
        XLOGE("    unregister MVA FAILED, srcImgYAddr:%p", mva->calc_addr->y);
        ret = INVALID_OPERATION;
    }

    // if all MVA unregistered, think as no MDP functions required
    if (0 == gMvaCnt) {
        deinitMDPLocked();
    }

    return ret;
}

status_t SurfaceTexture::checkPixelFormatSupported(
    sp<GraphicBuffer> graphicBuffer) const {

    if (graphicBuffer != NULL) {
        PixelFormat format = graphicBuffer->format;
        if (HAL_PIXEL_FORMAT_I420 == format) {
            return OK;
        }
    }
    return INVALID_OPERATION;
}

status_t SurfaceTexture::freeAuxSlotLocked(AuxSlot &bs) {
    if (EGL_NO_IMAGE_KHR != bs.eglSlot.mEglImage ||
        bs.slot.mGraphicBuffer != NULL) {
        XLOGI("[%s]", __func__);

        if (NO_ERROR == unregisterMva((mHalRegisterLoopMemoryObj_t *)bs.mMva)) {
            bs.mMva = NULL;
        }

        if (EGL_NO_SYNC_KHR != bs.eglSlot.mEglFence) {
            eglDestroySyncKHR(mEglDisplay, bs.eglSlot.mEglFence);
            bs.eglSlot.mEglFence = EGL_NO_SYNC_KHR;
        }

        bs.slot.mGraphicBuffer = NULL;
        eglDestroyImageKHR(mEglDisplay, bs.eglSlot.mEglImage);
        bs.eglSlot.mEglImage = EGL_NO_IMAGE_KHR;
    }

    return NO_ERROR;
}

// conversion function should format by format, chip by chip
// currently input is MTK_I420, and output is IMG_YV12/ABGR
status_t SurfaceTexture::convertToAuxSlotLocked(bool isForce) {
    // check invalid buffer
    if (BufferQueue::INVALID_BUFFER_SLOT == mCurrentTexture) {
        mAuxSlotConvert = false;
        return INVALID_OPERATION;
    }

    ATRACE_CALL();

    // 1) normal BufferQueue needs conversion now
    // 2) SurfaceTextureLayer neesd conversion after HWC
    bool isNeedConversionNow =
        (BufferQueue::TYPE_BufferQueue == mBufferQueue->getType()) ||
        ((true == isForce) && (BufferQueue::TYPE_SurfaceTextureLayer == mBufferQueue->getType()));

    //if ((true == isNeedConversionNow) && (BufferQueue::NO_CONNECTED_API != getConnectedApi())) {
    if (true == isNeedConversionNow) {
        XLOGI("do convertToAuxSlot...");

        Slot &src = mSlots[mCurrentTexture];
        AuxSlot &dst = *mBackAuxSlot;

        // fence sync here for buffer not used by G3D
        EGLSyncKHR fence = mFrontAuxSlot->eglSlot.mEglFence;
        if (fence != EGL_NO_SYNC_KHR) {
            EGLint result = eglClientWaitSyncKHR(mEglDisplay, fence, 0, 1000000000);
            if (result == EGL_FALSE) {
                XLOGW("[%s] FAILED waiting for front fence: %#x, tearing risk", __func__, eglGetError());
            } else if (result == EGL_TIMEOUT_EXPIRED_KHR) {
                XLOGW("[%s] TIMEOUT waiting for front fence, tearing risk", __func__);
            }
            eglDestroySyncKHR(mEglDisplay, fence);
            mFrontAuxSlot->eglSlot.mEglFence = EGL_NO_SYNC_KHR;
        }

#ifdef USE_MDP
        uint32_t hal_out_fmt;
        uint32_t mdp_in_fmt;
        uint32_t mdp_out_fmt;

        //if (NATIVE_WINDOW_API_CAMERA == getConnectedApi()) {
            hal_out_fmt = HAL_PIXEL_FORMAT_RGBA_8888;
            // camera path needs RGBA for MDP resource
            mdp_out_fmt = MHAL_FORMAT_ABGR_8888;
        //} else {
        //    hal_out_fmt = HAL_PIXEL_FORMAT_YV12;
        //    mdp_out_fmt = MHAL_FORMAT_IMG_YV12;
        //}
        // !!! only convert for I420 now !!!
        mdp_in_fmt = MHAL_FORMAT_YUV_420;

        // source graphic buffer
        sp<GraphicBuffer> sg = src.mGraphicBuffer;

        // destination graphic buffer
        sp<GraphicBuffer> dg = dst.slot.mGraphicBuffer;

        // free if current aux slot exist and not fit
        if ((EGL_NO_IMAGE_KHR != dst.eglSlot.mEglImage && dg != NULL) &&
            ((sg->width != dg->width) || (sg->height != dg->height) || (hal_out_fmt != (uint32_t)dg->format))) {

            XLOGI("[%s] free old aux slot ", __func__);
            XLOGI("    src[w:%d, h:%d, f:0x%x] dst[w:%d, h:%d, f:0x%x] required format:0x%x",
                sg->width, sg->height, sg->format,
                dg->width, dg->height, dg->format,
                hal_out_fmt);

            freeAuxSlotLocked(dst);
        }

        // create aux buffer if current is NULL
        if ((EGL_NO_IMAGE_KHR == dst.eglSlot.mEglImage) && (dst.slot.mGraphicBuffer == NULL)) {
            XLOGI("[%s] create dst buffer and image", __func__);

            XLOGI("    before create new aux buffer: %p", __func__, dg.get());
            dg = dst.slot.mGraphicBuffer = new GraphicBuffer(sg->width,
                                                        sg->height,
                                                        hal_out_fmt,
                                                        sg->usage);
            if (dg == NULL) {
                XLOGE("    create aux GraphicBuffer FAILED", __func__);
                freeAuxSlotLocked(dst);
                return BAD_VALUE;
            } else {
                XLOGI("    create aux GraphicBuffer: %p", __func__, dg.get());
            }

            dst.eglSlot.mEglImage = createImage(mEglDisplay, dg);
            if (EGL_NO_IMAGE_KHR == dst.eglSlot.mEglImage) {
                XLOGE("[%s] create aux eglImage FAILED", __func__);
                freeAuxSlotLocked(dst);
                return BAD_VALUE;
            }

            XLOGI("[%s] create aux slot success", __func__);
            XLOGI("    src[w:%d, h:%d, f:0x%x], dst[w:%d, h:%d, f:0x%x]",
                sg->width, sg->height, sg->format,
                dg->width, dg->height, dg->format);

            dst.mMva = registerMva(dg);
        }

        status_t lockret;
        uint8_t *src_yp, *dst_yp;

        lockret = sg->lock(LOCK_FOR_MDP, (void**)&src_yp);
        if (NO_ERROR != lockret) {
            XLOGE("[%s] buffer lock fail: %s", __func__, strerror(lockret));
            return INVALID_OPERATION;
        }
        lockret = dg->lock(LOCK_FOR_MDP, (void**)&dst_yp);
        if (NO_ERROR != lockret) {
            XLOGE("[%s] buffer lock fail: %s", __func__, strerror(lockret));
            return INVALID_OPERATION;
        }
        {
            mHalBltParam_t bltParam;
            memset(&bltParam, 0, sizeof(bltParam));

            bltParam.srcAddr    = (MUINT32)src_yp;
            bltParam.srcX       = 0;
            bltParam.srcY       = 0;
            bltParam.srcW       = sg->width;
            // !!! I420 content is forced 16 align !!!
            bltParam.srcWStride = ALIGN(sg->width, 16);
            bltParam.srcH       = sg->height;
            bltParam.srcHStride = sg->height;
            bltParam.srcFormat  = mdp_in_fmt;

            bltParam.dstAddr   = (MUINT32)dst_yp;
            bltParam.dstW      = dg->width;
            // already 32 align
            bltParam.pitch     = dg->stride;
            bltParam.dstH      = dg->height;
            bltParam.dstFormat = mdp_out_fmt;

#ifdef MTK_75DISPLAY_ENHANCEMENT_SUPPORT
            bltParam.doImageProcess = (NATIVE_WINDOW_API_MEDIA == getConnectedApi()) ? 1 : 0;
#endif

            // mdp bitblt and check
            if (MHAL_NO_ERROR != ipcBitBlt(&bltParam)) {
                if (1 == bltParam.doImageProcess) {
                    XLOGW("[%s] bitblt FAILED with PQ, disable and try again", __func__);
                    bltParam.doImageProcess = 0;
                    if (MHAL_NO_ERROR != ipcBitBlt(&bltParam)) {
                        XLOGE("[%s] bitblt FAILED, unlock buffer and return", __func__);
                        dst.slot.mGraphicBuffer->unlock();
                        src.mGraphicBuffer->unlock();
                        return INVALID_OPERATION;
                    }
                } else {
                    XLOGE("[%s] bitblt FAILED, unlock buffer and return", __func__);
                    dst.slot.mGraphicBuffer->unlock();
                    src.mGraphicBuffer->unlock();
                    return INVALID_OPERATION;
                }
            } else {
                // for drawing debug line
                if (true == mLine) {
                    int _stride = bltParam.pitch;
                    uint8_t *_ptr = (uint8_t*)bltParam.dstAddr;
                    static uint32_t offset = bltParam.dstH / 4;
                    //ST_XLOGI("!!!!! draw line, ptr: %p, offset: %d, stride: %d, height: %d", _ptr, offset, _stride, bltParam.dstH);
                    if (NULL != _ptr) {
                        memset((void*)(_ptr + offset * _stride * 3 / 2), 0xFF, _stride * 20 * 3 / 2);
                    }
                    offset += 20;
                    if (offset >= bltParam.dstH * 3 / 4)
                      offset = bltParam.dstH / 4;
                }
            }
        }
        dg->unlock();
        sg->unlock();

#else // ! USE_MDP
        status_t err = swConversionLocked(src, dst);
        if (NO_ERROR != err)
            return err;
#endif // USE_MDP

        mAuxSlotConvert = false;
        mAuxSlotDirty = true;
    }

    return NO_ERROR;
}


}

