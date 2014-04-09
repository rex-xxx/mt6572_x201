/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2005
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE. 
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/
/*******************************************************************************
 *
 * Filename:
 * ---------
 * bt_dun_at.h
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is header file of bt_dun_at.c
 *
 * Author:
 * -------
 * Ting Zheng
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision: 
 * $Modtime:
 * $Log: 
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!!
 *==============================================================================
 *******************************************************************************/
#ifndef __BT_DUN_AT_H__
#define __BT_DUN_AT_H__
#include "bt_types.h"

typedef U16 AtResponse;
 
/** This result acknowledges execution of a command. There are no
*  parameters with this command.
*/
#define AT_OK				0x0000

/** This result indicates that connection has been established. There 
*  are no parameters with this command.
*/
#define AT_CONNECT		0x0001

/** This unsolicited result indicates an incoming call signal from network. 
*  There are no parameters with this command.
*/
#define AT_RING			0x0002

/** This result indicates that the connection was terminated, or the attempt
*  to establish a connection failed. There are no parameters with this command.
*/
#define AT_NO_CARRIER		0x0003

/** This result indicates that the received command was not accepted. There 
*  are no parameters with this command.
*/
#define AT_ERROR			0x0004

/** This result indicates that no dial-tone isdetected. There are no parameters
*  with this command.
*/
#define AT_NO_DIALTONE	0x0005

/** This result indicates that busy signal is detected. There are no parameters
*  with this command. 
*/
#define AT_BUSY			0x0006



typedef U16 AtCommand;

/** Determines how ITU-T V.24 circuit 109 (or equivalent) relates to the
 *  detection of received line signal from remote end (recommended default 1
 *  i.e. 109 operation relates to detection of received signal)
 */
#define AT_SIGNAL_DCD		0x0000			

#define AT_SIGNAL_DTR		0x0001

#define AT_SIGNAL_FDC		0x0002

#define AT_SIGNAL_GCAP		0x0003	

#define AT_SIGNAL_GMI		0x0004

#define AT_SIGNAL_GMM		0x0005

#define AT_SIGNAL_GMR		0x0006

#define AT_SIGNAL_ASW		0x0007

#define AT_SIGNAL_DIAL		0x0008

#define AT_SIGNAL_CE		0x0009

#define AT_SIGNAL_HC		0x000a

#define AT_SIGNAL_SL		0x000b

#define AT_SIGNAL_SM		0x000c

#define AT_SIGNAL_OD		0x000d

#define AT_SIGNAL_PD		0x000e

#define AT_SIGNAL_CS		0x000f

#define AT_SIGNAL_AASW		0x0010

#define AT_SIGNAL_ADD		0x0011

#define AT_SIGNAL_CLTC		0x0012

#define AT_SIGNAL_RFC		0x0013

#define AT_SIGNAL_CLEC		0x0014

#define AT_SIGNAL_PBBD		0x0015

#define AT_SIGNAL_CCT		0x0016

#define AT_SIGNAL_CDMT		0x0017

#define AT_SIGNAL_STD		0x0018

#define AT_SIGNAL_RF		0x0019

#define AT_SIGNAL_RCS_CPMC		0x001a

#define AT_SIGNAL_RDC		0x001b

#ifdef __DUN_FOR_GSM__
#define AT_SIGNAL_SET_GPRS_PDP_CONTEXT	0x001c
#endif

AtResponse btmtk_dun_at_decode(U8 *command, U16 len, AtCommand *cmd_out);
BT_BOOL btmtk_dun_at_encode(AtResponse type, U8 *response, U16 *len);

#endif
