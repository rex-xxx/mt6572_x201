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

#include <stdlib.h>
#include <stdint.h>
#include <sys/types.h>

#include <ui/GraphicBuffer.h>

#include "LayerScreenshot.h"
#include "Layer.h"
#include "SurfaceFlinger.h"

#include <cutils/xlog.h>
#include <utils/KeyedVector.h>

namespace android {
// ---------------------------------------------------------------------------

struct LayerStatus {
    LayerStatus() : z(0), alpha(0xff), orient(Transform::ROT_0) { }
    uint32_t z;
    uint8_t alpha;
    int32_t orient;
};

DefaultKeyedVector<int, LayerStatus> gLayerStatus;
Mutex gLock;

void LayerScreenshot::setGeometry(const sp<const DisplayDevice>& hw,
        HWComposer::HWCLayerInterface& layer)
{
    LayerBaseClient::setGeometry(hw, layer);

    Mutex::Autolock _l(gLock);

    LayerStatus state;

    const LayerBase::State& s(drawingState());
    const Transform tr(hw->getTransform() * s.transform);
    state.z = s.z;
    state.alpha = s.alpha;
    state.orient = tr.getOrientation();

    gLayerStatus.add(this->getIdentity(), state);

    XLOGI("[%s] add:%d, count:%d", __func__,
        this->getIdentity(), gLayerStatus.size());
}

void LayerScreenshot::clearStatus() {
    Mutex::Autolock _l(gLock);
    for (uint32_t i = 0; i < gLayerStatus.size(); i++) {
        int identity = gLayerStatus.keyAt(i);
        if (this->getIdentity() == (uint32_t)identity) {
            gLayerStatus.removeItem(identity);
            XLOGI("[%s] del:%d, count:%d", __func__,
                identity, gLayerStatus.size());
            break;
        }
    }
}

int LayerScreenshot::getCount() {
    Mutex::Autolock _l(gLock);
    int count = gLayerStatus.size();
    return count;
}

bool LayerScreenshot::isFrozen() {
    Mutex::Autolock _l(gLock);

    int count = gLayerStatus.size();
    if (count <= 0) return false;

    LayerStatus top = gLayerStatus.valueAt(0);
    int identity = gLayerStatus.keyAt(0);
    for (int i = 1; i < count; i++) {
        LayerStatus state = gLayerStatus.valueAt(i);
        if (state.z > top.z) {
            top = state;
            identity = gLayerStatus.keyAt(i);
        }
    }

    XLOGI("[%s] LayerScreenshot top:%d (orient:%d, alpha:%d)",
        __func__, identity, top.orient, top.alpha);

    if ((top.orient & Transform::ROT_INVALID) || (0xff > top.alpha)) {
        XLOGI("    No need to freeze screen...");
        return false;
    }

    XLOGD("    Freeze screen...");
    return true;
}

void LayerScreenshot::setPerFrameData(const sp<const DisplayDevice>& hw,
        HWComposer::HWCLayerInterface& layer) {
    LayerBaseClient::setPerFrameData(hw, layer);
    layer.setBuffer(NULL);
}

void LayerScreenshot::computeGeometryOrient(
    const sp<const DisplayDevice>& hw, LayerMesh* mesh, int orient) const
{
    const Layer::State& s(drawingState());

    int hw_w = hw->getWidth();
    int hw_h = hw->getHeight();

    uint32_t flags = Transform::ROT_0;
    switch ((4 - orient) % 4) {
        case DisplayState::eOrientation90:
            flags = Transform::ROT_90;
            break;
        case DisplayState::eOrientation180:
            flags = Transform::ROT_180;
            break;
        case DisplayState::eOrientation270:
            flags = Transform::ROT_270;
            break;
        default:
            XLOGE("[%s] invalid orientation: %d", __func__, orient);
            break;
    }

    Transform orientTransform;
    if (orient & DisplayState::eOrientationSwapMask) {
        orientTransform.set(flags, hw_h, hw_w);
    } else {
        orientTransform.set(flags, hw_w, hw_h);
    }

    const Transform tr(hw->getTransform() * s.transform * orientTransform);

    if (mesh) {
        tr.transform(mesh->mVertices[0], 0, 0);
        tr.transform(mesh->mVertices[1], 0, hw_h);
        tr.transform(mesh->mVertices[2], hw_w, hw_h);
        tr.transform(mesh->mVertices[3], hw_w, 0);
        for (size_t i=0 ; i<4 ; i++) {
            mesh->mVertices[i][1] = hw_h - mesh->mVertices[i][1];
        }
    }
}

// ---------------------------------------------------------------------------

}; // namespace android
