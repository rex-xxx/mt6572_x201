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

package org.bouncycastle.jce.provider;

import org.bouncycastle.asn1.x509.ReasonFlags;

/**
 * This class helps to handle CRL revocation reasons mask. Each CRL handles a
 * certain set of revocation reasons.
 */
class ReasonsMask
{
    private int _reasons;

    /**
     * Constructs are reason mask with the reasons.
     * 
     * @param reasons The reasons.
     */
    ReasonsMask(int reasons)
    {
        _reasons = reasons;
    }

    /**
     * A reason mask with no reason.
     * 
     */
    ReasonsMask()
    {
        this(0);
    }

    /**
     * A mask with all revocation reasons.
     */
    static final ReasonsMask allReasons = new ReasonsMask(ReasonFlags.aACompromise
            | ReasonFlags.affiliationChanged | ReasonFlags.cACompromise
            | ReasonFlags.certificateHold | ReasonFlags.cessationOfOperation
            | ReasonFlags.keyCompromise | ReasonFlags.privilegeWithdrawn
            | ReasonFlags.unused | ReasonFlags.superseded);

    /**
     * Adds all reasons from the reasons mask to this mask.
     * 
     * @param mask The reasons mask to add.
     */
    void addReasons(ReasonsMask mask)
    {
        _reasons = _reasons | mask.getReasons();
    }

    /**
     * Returns <code>true</code> if this reasons mask contains all possible
     * reasons.
     * 
     * @return <code>true</code> if this reasons mask contains all possible
     *         reasons.
     */
    boolean isAllReasons()
    {
        return _reasons == allReasons._reasons ? true : false;
    }

    /**
     * Intersects this mask with the given reasons mask.
     * 
     * @param mask The mask to intersect with.
     * @return The intersection of this and teh given mask.
     */
    ReasonsMask intersect(ReasonsMask mask)
    {
        ReasonsMask _mask = new ReasonsMask();
        _mask.addReasons(new ReasonsMask(_reasons & mask.getReasons()));
        return _mask;
    }

    /**
     * Returns <code>true</code> if the passed reasons mask has new reasons.
     * 
     * @param mask The reasons mask which should be tested for new reasons.
     * @return <code>true</code> if the passed reasons mask has new reasons.
     */
    boolean hasNewReasons(ReasonsMask mask)
    {
        return ((_reasons | mask.getReasons() ^ _reasons) != 0);
    }

    /**
     * Returns the reasons in this mask.
     * 
     * @return Returns the reasons.
     */
    int getReasons()
    {
        return _reasons;
    }
}
