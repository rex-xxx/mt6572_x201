/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

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

package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.CatLog;

import com.mediatek.common.telephony.OtherAddress;
import com.mediatek.common.telephony.TransportProtocol;
import com.mediatek.common.telephony.BipUtils;
import com.mediatek.common.telephony.ErrorValue;
import com.mediatek.common.telephony.ChannelStatus;
import com.mediatek.common.telephony.BearerDesc;
import com.mediatek.common.featureoption.FeatureOption;

class BipChannelManager {
    final public static int MAXPSCID = FeatureOption.MTK_BIP_SCWS == true ? 5 : 1;
    final public static int MAXUICCSERVIER = FeatureOption.MTK_BIP_SCWS == true ? 2 : 1;
    final public static int MAXCHANNELID = FeatureOption.MTK_BIP_SCWS == true ? 7 : 1;
    private byte mChannelIdPool = (byte)0;
    private Channel[] mChannels = null;
    private byte mCurrentOccupiedPSCh = (byte)0;
    private byte mCurrentOccupiedUICCSerCh = (byte)0;
    private int[] mBipChannelStatus = null;
        
    public BipChannelManager(){
        this.mBipChannelStatus = new int[MAXCHANNELID];        
        this.mChannels = new Channel[MAXCHANNELID];
        for(int i=0; i<MAXCHANNELID; i++) {
            this.mChannels[i] = null;
            this.mBipChannelStatus[i] = BipUtils.CHANNEL_STATUS_UNKNOWN;
        }
    }
    public boolean isChannelIdOccupied(int cId)
    {
        CatLog.d("[BIP]", "isChannelIdOccupied, mChannelIdPool "+mChannelIdPool+":"+cId);
        return (0 < (mChannelIdPool & (0x01 << (cId-1))));
    }
    public int getFreeChannelId(){
        int i = 0;

        for(i = 0; i < MAXCHANNELID; i++){
            if(0 == (mChannelIdPool & (0x01 << i))){
                return i+1;
            }
        }
        return 0;//All channels are occupied. No channelId is available.
    }

    public int acquireChannelId(int protocolType){
        byte i = 0;
        CatLog.d("[BIP]", "acquireChannelId, protocolType "+protocolType+",occupied "+mCurrentOccupiedPSCh+","+mCurrentOccupiedUICCSerCh);
        if((BipUtils.TRANSPORT_PROTOCOL_SERVER == protocolType && MAXUICCSERVIER <= mCurrentOccupiedUICCSerCh) ||
            ((BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE == protocolType || BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE == protocolType) && 
            MAXPSCID <= mCurrentOccupiedPSCh))
            return 0;

        for(i = 0; i < MAXCHANNELID; i++){
            if(0 == (mChannelIdPool & (0x01 << i))){
                mChannelIdPool |= (byte)((byte)0x01 << i);
                if(BipUtils.TRANSPORT_PROTOCOL_SERVER == protocolType)
                    mCurrentOccupiedUICCSerCh++;
                else if(BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE == protocolType||
                    BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE == protocolType)
                    mCurrentOccupiedPSCh++;
                CatLog.d("[BIP]", "acquireChannelId, mChannelIdPool "+mChannelIdPool+":"+(i+1));
                return i+1;
            }
        }
        return 0;//All channels are occupied. No channelId is available.
    }
    public void releaseChannelId(int cId, int protocolType){       
        try {        
            if(0 == (mChannelIdPool & (0x01 << (byte)(cId-1)))){
                CatLog.e("[BIP]", "releaseChannelId, cId:"+cId+" has been released.");
                return;
            }
            if(0 >= cId){
                CatLog.e("[BIP]", "cId==0, it's no valid cid.");
                return;
            }
        
            if(BipUtils.TRANSPORT_PROTOCOL_SERVER == protocolType && 0 <= mCurrentOccupiedUICCSerCh)
                mCurrentOccupiedUICCSerCh--;
            else if((BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE == protocolType || BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE == protocolType) && 
                0 <= mCurrentOccupiedPSCh)
                mCurrentOccupiedPSCh--;
            else
                CatLog.e("[BIP]", "releaseChannelId, bad parameters.cId:"+cId+":"+mChannelIdPool);
        
            mChannelIdPool &= (byte)(~(0x01 << (byte)(cId-1)));
            CatLog.d("[BIP]", "releaseChannelId, cId "+cId+",protocolType "+protocolType+",occupied "+mCurrentOccupiedPSCh+","+mCurrentOccupiedUICCSerCh+":"+mChannelIdPool);
            
        } catch (IndexOutOfBoundsException e){
            CatLog.e("[BIP]", "IndexOutOfBoundsException releaseChannelId cId="+cId+":"+mChannelIdPool);
        }
    }
    
    public void releaseChannelId(int cId){
        int protocolType = -1;
        
        try {    
            if(0 == (mChannelIdPool & (0x01 << (byte)(cId-1)))){
                CatLog.e("[BIP]", "releaseChannelId, cId:"+cId+" has been released.");
                return;
            }
            if(0 >= cId){
                CatLog.e("[BIP]", "cId==0, it's no valid cid.");
                return;
            }
            if(null != mChannels[cId-1])
                protocolType = mChannels[cId-1].mProtocolType;
            else {
                CatLog.e("[BIP]", "channel object is null.");
                return;
            }
        
            if(BipUtils.TRANSPORT_PROTOCOL_SERVER == protocolType && 0 < mCurrentOccupiedUICCSerCh)
                mCurrentOccupiedUICCSerCh--;
            else if((BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE == protocolType || BipUtils.TRANSPORT_PROTOCOL_TCP_REMOTE == protocolType) && 
                0 < mCurrentOccupiedPSCh)
                mCurrentOccupiedPSCh--;
            else
                CatLog.e("[BIP]", "releaseChannelId, bad parameters.cId:"+cId+":"+mChannelIdPool);
        
            mChannelIdPool &= (byte)(~(0x01 << (byte)(cId-1)));
            CatLog.d("[BIP]", "releaseChannelId, cId "+cId+",protocolType"+protocolType+",occupied "+mCurrentOccupiedPSCh+","+mCurrentOccupiedUICCSerCh+":"+mChannelIdPool);
            
        } catch (IndexOutOfBoundsException e){
            CatLog.e("[BIP]", "IndexOutOfBoundsException releaseChannelId cId="+cId+":"+mChannelIdPool);
        }
    }
   
    public int addChannel(int cId, Channel ch){
//        int freeCId = 0;

//        freeCId = acquireChannelId();
        CatLog.d("[BIP]", "BCM-addChannel:"+cId);
        if(0 < cId){
            try {
            mChannels[cId-1] = ch;
            mBipChannelStatus[cId-1]=BipUtils.CHANNEL_STATUS_OPEN;
            } catch (IndexOutOfBoundsException e){
                CatLog.e("[BIP]", "IndexOutOfBoundsException addChannel cId="+cId);
                return -1;
            }
        } else {
            CatLog.e("[BIP]", "No free channel id.");
        }
        return cId;
    }
    
    public Channel getChannel(int cId){
        try {
            return mChannels[cId-1];
        } catch (IndexOutOfBoundsException e){
            CatLog.e("[BIP]", "IndexOutOfBoundsException getChannel cId="+cId);
        }
        return null;
    }
	
    public int getBipChannelStatus(int cId){
        return mBipChannelStatus[cId-1];
    }

    public void setBipChannelStatus(int cId, int status){
        try {
            mBipChannelStatus[cId-1] = status;
        } catch (IndexOutOfBoundsException e) {
            CatLog.e("[BIP]", "IndexOutOfBoundsException setBipChannelStatus cId="+cId);
        }
    }
    public int removeChannel(int cId){
        CatLog.d("[BIP]", "BCM-removeChannel:"+cId);
        try {
            releaseChannelId(cId);
            mChannels[cId-1] = null;
            mBipChannelStatus[cId-1]=BipUtils.CHANNEL_STATUS_CLOSE;
            return 1;
        } catch (IndexOutOfBoundsException e){
            CatLog.e("[BIP]", "IndexOutOfBoundsException removeChannel cId="+cId);
        } catch (NullPointerException e1) {
            CatLog.e("[BIP]", "removeChannel channel:"+cId+" is null");
        }
        return 0;
    }

    public boolean isClientChannelOpened() {
        int i = 0;
        try {
            for (i = 0; i < MAXCHANNELID; i++) {
                if(null != mChannels && null != mChannels[i] && 
               0 != ((BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE | BipUtils.TRANSPORT_PROTOCOL_UDP_REMOTE) & mChannels[i].mProtocolType))
               return true;
            }
        } catch (NullPointerException e) {
            CatLog.e("[BIP]", "isClientChannelOpened channel:"+i+" is null");
        }
        return false;
    }
    public void updateBipChannelStatus(int cId, int chStatus){
        try {
            mChannels[cId].mChannelStatus = chStatus;
        } catch (IndexOutOfBoundsException e){
            CatLog.e("[BIP]", "IndexOutOfBoundsException updateBipChannelStatus cId="+cId);
        } catch (NullPointerException e1) {
            CatLog.e("[BIP]", "updateBipChannelStatus id:"+cId+" is null");
        }
    }

    public void updateChannelStatus(int cId, int chStatus){
        try {
            mChannels[cId].mChannelStatusData.mChannelStatus = chStatus;
        } catch (IndexOutOfBoundsException e){
            CatLog.e("[BIP]", "IndexOutOfBoundsException updateChannelStatus cId="+cId);
        } catch (NullPointerException e1) {
            CatLog.e("[BIP]", "updateChannelStatus id:"+cId+" is null");
        }
    }
    public void updateChannelStatusInfo(int cId, int chStatusInfo){
        try {
            mChannels[cId].mChannelStatusData.mChannelStatusInfo = chStatusInfo;
        } catch (IndexOutOfBoundsException e){
            CatLog.e("[BIP]", "IndexOutOfBoundsException updateChannelStatusInfo cId="+cId);
        } catch (NullPointerException e1) {
            CatLog.e("[BIP]", "updateChannelStatusInfo id:"+cId+" is null");
        }        
    }
	
}
