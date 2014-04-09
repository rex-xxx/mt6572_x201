/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#ifndef _MHAL_INC_CAMERA_BUFMEM_H_
#define _MHAL_INC_CAMERA_BUFMEM_H_


/******************************************************************************
*
*******************************************************************************/


/**
 * @par Enum
 *   MHAL_CAM_MEM_TYPE
 * @par Description
 *   This is the enum for camera buf format 
 */
enum MHAL_CAM_MEM_TYPE
{
    MHAL_CAM_PMEM_TYPE      = 0x0, 
    MHAL_CAM_ASHMEM_TYPE    = 0x1, 
};


/**
 * @par Enum
 *   MHAL_CAM_BUF_MEM_ID
 * @par Description
 *   This is the enum for camera buf ID 
 */
enum MHAL_CAM_BUF_MEM_ID
{
    MHAL_CAM_BUF_CAPTURE    = 0, 
    MHAL_CAM_BUF_POSTVIEW, 
    MHAL_CAM_BUF_PREVIEW, 
    MHAL_CAM_BUF_PREVIEW_ATV, 
    MHAL_CAM_BUF_VIDEO, 
    MHAL_CAM_BUF_VIDEO_BS, 
};


/**
 * @par Structure
 *   mhalCamBufInfo_t
 * @par Description
 *   This is the sturcture for cam buffer info 
 */typedef struct mhalCamBufMemInfo_s {
    MUINT32     bufID;
    MUINT32     frmW;
    MUINT32     frmH;
    MUINT32     camBufCount;
    MUINT32     camBufSize;
    MUINT32     camMemType;
    MUINT32     camBufFmt;
}mhalCamBufMemInfo_t; 


////////////////////////////////////////////////////////////////////////////////
#endif  //  _MHAL_INC_CAMERA_BUFMEM_H_

