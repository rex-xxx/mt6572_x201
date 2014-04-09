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

#include "rsContext.h"
#include "rsProgramStore.h"

using namespace android;
using namespace android::renderscript;

#define USE_KEYMAP 1

ProgramStore::ProgramStore(Context *rsc,
                           bool colorMaskR, bool colorMaskG, bool colorMaskB, bool colorMaskA,
                           bool depthMask, bool ditherEnable,
                           RsBlendSrcFunc srcFunc, RsBlendDstFunc destFunc,
                           RsDepthFunc depthFunc) : ProgramBase(rsc) {
    memset(&mHal, 0, sizeof(mHal));

    mHal.state.ditherEnable = ditherEnable;

    mHal.state.colorRWriteEnable = colorMaskR;
    mHal.state.colorGWriteEnable = colorMaskG;
    mHal.state.colorBWriteEnable = colorMaskB;
    mHal.state.colorAWriteEnable = colorMaskA;
    mHal.state.blendSrc = srcFunc;
    mHal.state.blendDst = destFunc;

    mHal.state.depthWriteEnable = depthMask;
    mHal.state.depthFunc = depthFunc;
}

void ProgramStore::preDestroy() const {
    for (uint32_t ct = 0; ct < mRSC->mStateFragmentStore.mStorePrograms.size(); ct++) {
        if (mRSC->mStateFragmentStore.mStorePrograms[ct] == this) {
            mRSC->mStateFragmentStore.mStorePrograms.removeAt(ct);
            mRSC->mStateFragmentStore.mStoreProgramsKeyMaps.removeItem(mProgramStoreKey);
            break;
        }
    }
}

ProgramStore::~ProgramStore() {
    mRSC->mHal.funcs.store.destroy(mRSC, this);
}

void ProgramStore::setup(const Context *rsc, ProgramStoreState *state) {
    if (state->mLast.get() == this) {
        return;
    }
    state->mLast.set(this);

    rsc->mHal.funcs.store.setActive(rsc, this);
}

void ProgramStore::serialize(Context *rsc, OStream *stream) const {
}

ProgramStore *ProgramStore::createFromStream(Context *rsc, IStream *stream) {
    return NULL;
}

void ProgramStore::init() {
    mRSC->mHal.funcs.store.init(mRSC, this);
}

ProgramStoreState::ProgramStoreState() {
}

ProgramStoreState::~ProgramStoreState() {
}

ObjectBaseRef<ProgramStore> ProgramStore::getProgramStore(Context *rsc,
                                                          bool colorMaskR,
                                                          bool colorMaskG,
                                                          bool colorMaskB,
                                                          bool colorMaskA,
                                                          bool depthMask, bool ditherEnable,
                                                          RsBlendSrcFunc srcFunc,
                                                          RsBlendDstFunc destFunc,
                                                          RsDepthFunc depthFunc) {
    ObjectBaseRef<ProgramStore> returnRef;
    ObjectBase::asyncLock();


    static int cnt = 0;

    ALOGD_IF(++cnt % 3000 == 0,
        "cnt: %d, size:%d", cnt, rsc->mStateFragmentStore.mStorePrograms.size());
    
#if !USE_KEYMAP    
    for (uint32_t ct = 0; ct < rsc->mStateFragmentStore.mStorePrograms.size(); ct++) {
        ProgramStore *existing = rsc->mStateFragmentStore.mStorePrograms[ct];
        if (existing->mHal.state.ditherEnable != ditherEnable) continue;
        if (existing->mHal.state.colorRWriteEnable != colorMaskR) continue;
        if (existing->mHal.state.colorGWriteEnable != colorMaskG) continue;
        if (existing->mHal.state.colorBWriteEnable != colorMaskB) continue;
        if (existing->mHal.state.colorAWriteEnable != colorMaskA) continue;
        if (existing->mHal.state.blendSrc != srcFunc) continue;
        if (existing->mHal.state.blendDst != destFunc) continue;
        if (existing->mHal.state.depthWriteEnable != depthMask) continue;
        if (existing->mHal.state.depthFunc != depthFunc) continue;

        ALOGD("[NO KEYMAP] find already create programstore, index=%d, cnt=%d)", ct, cnt);
        
        returnRef.set(existing);
        ObjectBase::asyncUnlock();
        return returnRef;
    }
#else    

    // CTS failed issue
    uint32_t programStoreKey =  ditherEnable |
                        colorMaskR << 1 | colorMaskG << 2 | colorMaskB << 3 | colorMaskA << 4 |
                        depthMask << 5 |
                        srcFunc << 6 | destFunc << 13 | depthFunc << 20;

    //ALOGD("[USE KEYMAP] mStoreProgramsKeyMaps size=%d, key=%d", rsc->mStateFragmentStore.mStoreProgramsKeyMaps.size(), programStoreKey);
    
    //if (rsc->mStateFragmentStore.mStoreProgramsKeyMaps.size() != 0)
    {        
        uint32_t index = rsc->mStateFragmentStore.mStoreProgramsKeyMaps.valueFor(programStoreKey);
        if (index != 0)
        {
            ALOGD("[USE KEYMAP] find already create programstore, index=%d, cnt=%d", index, cnt);
            
            ProgramStore *existing = rsc->mStateFragmentStore.mStorePrograms[index];

        returnRef.set(existing);
        ObjectBase::asyncUnlock();
        return returnRef;
    }
    }
#endif    
    ObjectBase::asyncUnlock();

    ProgramStore *pfs = new ProgramStore(rsc,
                                         colorMaskR, colorMaskG, colorMaskB, colorMaskA,
                                         depthMask, ditherEnable,
                                         srcFunc, destFunc, depthFunc);

#if USE_KEYMAP    
    pfs->mProgramStoreKey = programStoreKey;
#endif

    returnRef.set(pfs);

    pfs->init();

    ObjectBase::asyncLock();
    rsc->mStateFragmentStore.mStorePrograms.push(pfs);
    ObjectBase::asyncUnlock();

#if USE_KEYMAP
    rsc->mStateFragmentStore.mStoreProgramsKeyMaps.add(programStoreKey, rsc->mStateFragmentStore.mStorePrograms.size() - 1);
#endif

    return returnRef;
}



void ProgramStoreState::init(Context *rsc) {
    mDefault.set(ProgramStore::getProgramStore(rsc,
                                               true, true, true, true,
                                               true, true,
                                               RS_BLEND_SRC_ONE, RS_BLEND_DST_ZERO,
                                               RS_DEPTH_FUNC_LESS).get());
}

void ProgramStoreState::deinit(Context *rsc) {
    mDefault.clear();
    mLast.clear();
}


namespace android {
namespace renderscript {

RsProgramStore rsi_ProgramStoreCreate(Context *rsc,
                                      bool colorMaskR, bool colorMaskG, bool colorMaskB, bool colorMaskA,
                                      bool depthMask, bool ditherEnable,
                                      RsBlendSrcFunc srcFunc, RsBlendDstFunc destFunc,
                                      RsDepthFunc depthFunc) {

    ObjectBaseRef<ProgramStore> ps = ProgramStore::getProgramStore(rsc,
                                                                   colorMaskR, colorMaskG,
                                                                   colorMaskB, colorMaskA,
                                                                   depthMask, ditherEnable,
                                                                   srcFunc, destFunc, depthFunc);
    ps->incUserRef();
    return ps.get();
}

}
}
