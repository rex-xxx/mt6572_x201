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
 

#ifndef _MTK_HDR_H
#define _MTK_HDR_H

#include "MTKHdrType.h"
#include "MTKHdrErrCode.h"

//#define HDR_DEBUG
//#define HDR_MULTI_CORE_OPT
#define HDR_MAX_CORE_NO     (2)

typedef enum
{
    HDR_STATE_NORMALIZE,
    HDR_STATE_SWEIS,
    HDR_STATE_ALIGNMENT,
    HDR_STATE_FUSION,
}HDR_PROC_STATE_ENUM;


typedef enum
{
    HDR_STATE_STANDBY,
    HDR_STATE_INIT,
    HDR_STATE_PROC,
    HDR_STATE_READY,
    HDR_STATE_IDLE,
    HDR_STATE_MAX
}HDR_STATE_ENUM;


typedef struct
{
    MUINT32 BlurRatio;
    MUINT32 Gain[11]; //Gain for top N level

    double BottomFlareRatio;
    double TopFlareRatio;
    MUINT32 BottomFlareBound;
    MUINT32 TopFlareBound;
    MINT32 ThHigh;
    MINT32 ThLow;
    MUINT32 TargetLevelSub;

    // Multi-Core parameters
    MUINT32 CoreNumber;         // given cpu core number
}HDR_TUNING_PARA_STRUCT;


typedef enum
{
    HDR_FEATURE_BEGIN = 0,
    HDR_FEATURE_SET_EIS_INPUT_IMG,
    HDR_FEATURE_GET_EIS_RESULT,
    HDR_FEATURE_GET_GMV,
    HDR_FEATURE_SET_REC_PAIR_INFO,
    HDR_FEATURE_GET_WEIGHTING_TBL,
    HDR_FEATURE_SET_BLUR_WEIGHTING_TBL,
    HDR_FEATURE_GET_RESULT,
    HDR_FEATURE_GET_STATUS,
    HDR_FEATURE_SAVE_LOG,

    HDR_FEATURE_GET_PROC_INFO,
    HDR_FEATURE_SET_PROC_INFO,
    HDR_FEATURE_SET_WORK_BUF_INFO,

    HDR_FEATURE_MAX
}	HDR_FEATURE_ENUM;


typedef struct
{
    HDR_TUNING_PARA_STRUCT hdr_tuning_data;

    MUINT16 image_num;
    MUINT16 ev_gain1;
    MUINT16 ev_gain2; 
    MUINT16 ev_gain3; 
    MUINT16 image_width;
    MUINT16 image_height;
    MUINT32 target_tone;

    MUINT32 image_addr[3]; // input image address


} HDR_SET_ENV_INFO_STRUCT, *P_HDR_SET_ENV_INFO_STRUCT;


typedef struct
{
    MUINT16 small_image_width;
    MUINT16 small_image_height;
    MUINT32 ext_mem_size;
}HDR_GET_PROC_INFO_STRUCT, *P_HDR_GET_PROC_INFO_STRUCT;


typedef struct
{
    MUINT32 small_image_addr[3];
}HDR_SET_PROC_INFO_STRUCT, *P_HDR_SET_PROC_INFO_STRUCT;

typedef struct
{
    MUINT32 ext_mem_size;
    MUINT32 ext_mem_start_addr; //working buffer start address
}HDR_SET_WORK_BUF_INFO_STRUCT, *P_SET_WORK_BUF_INFO_STRUCT;


typedef struct
{
    MUINT16 eis_image_width;
    MUINT16 eis_image_height;
    MUINT32 eis_image_addr;
}EIS_INPUT_IMG_INFO, *P_EIS_INPUT_IMG_INFO;


typedef struct
{
    MUINT32 weight_table_width;
    MUINT32 weight_table_height;
    MUINT8 *weight_table_data; 
}WEIGHT_TBL_INFO, *P_WEIGHT_TBL_INFO;

/*
typedef struct
{
    MUINT16 weight_map_width;
    MUINT16 weight_map_height;
    MUINT32 eis_image_addr;
}WEIGHTING_MAP_INFO, *P_WEIGHTING_MAP_INFO;
*/


typedef struct
{
    MUINT16 output_image_width;
    MUINT16 output_image_height;
    MUINT32 output_image_addr;
}HDR_RESULT_STRUCT;


class MTKHdr {
public:
    static MTKHdr* createInstance();
    virtual void   destroyInstance() = 0;
       
    virtual ~MTKHdr(){};
    // Process Control
    virtual MRESULT HdrInit(void* InitInData, void* InitOutData);
    virtual MRESULT HdrMain(HDR_PROC_STATE_ENUM HdrState);	// START
    virtual MRESULT HdrReset();   //Reset
            
	// Feature Control        
	virtual MRESULT HdrFeatureCtrl(MUINT32 FeatureID, void* pParaIn, void* pParaOut);
private:
    
};


#endif
