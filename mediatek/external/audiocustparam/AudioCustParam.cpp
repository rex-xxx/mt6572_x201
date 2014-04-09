/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2009
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
 * AudioCustParam.cpp
 *
 * Project:
 * --------
 *   Android
 *
 * Description:
 * ------------
 *   This file implements customized parameter handling
 *
 * Author:
 * -------
 *   HP Cheng (mtk01752)
 *
 *------------------------------------------------------------------------------
 * $Revision: #2 $
 * $Modtime:$
 * $Log:$
 *
 * 12 29 2012 donglei.ji
 * [ALPS00425279] [Need Patch] [Volunteer Patch] voice ui and password unlock feature check in
 * voice ui - NVRAM .
 *
 *
 *******************************************************************************/

/*=============================================================================
 *                              Include Files
 *===========================================================================*/
#if defined(PC_EMULATION)
#include "windows.h"
#else
#include "unistd.h"
#include "pthread.h"
#endif

#include <utils/Log.h>
#include <utils/String8.h>

#include "CFG_AUDIO_File.h"
#include "CFG_file_lid.h"//AP_CFG_RESERVED_1 for AudEnh
#include "Custom_NvRam_LID.h"
#include "libnvram.h"
#include "CFG_Audio_Default.h"
#include <cutils/properties.h>

#if defined(MT6582)//Remove when 6589 platform nvram ready
#define USE_DEFAULT_CUST_TABLE    //For BringUp usage
#endif

#ifdef LOG_TAG
#undef LOG_TAG
#endif
#define LOG_TAG "AudioYusuParam"

#define MAX_RETRY_COUNT 20


namespace android
{

/*=============================================================================
 *                             Public Function
 *===========================================================================*/

bool checkNvramReady(void)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return true;
    #endif
    int read_nvram_ready_retry = 0;
    int ret = 0;
    char nvram_init_val[PROPERTY_VALUE_MAX];
    while (read_nvram_ready_retry < MAX_RETRY_COUNT) {
        read_nvram_ready_retry++;
        property_get("nvram_init", nvram_init_val, NULL);
        if (strcmp(nvram_init_val, "Ready") == 0) {
            ret = true;
            break;
        }
        else {
            usleep(500 * 1000);
        }
    }
    ALOGD("Get nvram restore ready retry cc=%d\n", read_nvram_ready_retry);
    if (read_nvram_ready_retry >= MAX_RETRY_COUNT) {
        ALOGW("Get nvram restore ready faild !!!\n");
        ret = false;
    }
    return ret;

}


int getDefaultSpeechParam(AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB)
{
    // only for startup use
    ALOGW("Digi_DL_Speech = %u", speech_custom_default.Digi_DL_Speech);
    ALOGW("uMicbiasVolt = %u", speech_custom_default.uMicbiasVolt);
    ALOGW("sizeof AUDIO_CUSTOM_PARAM_STRUCT = %d", sizeof(AUDIO_CUSTOM_PARAM_STRUCT));
    memcpy((void *)pSphParamNB, (void *)&speech_custom_default, sizeof(AUDIO_CUSTOM_PARAM_STRUCT));
    return sizeof(AUDIO_CUSTOM_PARAM_STRUCT);
}

int getDefaultVer1VolumeParam(AUDIO_VER1_CUSTOM_VOLUME_STRUCT *volume_param)
{
    // only for startup use
    ALOGD("getDefaultVer1VolumeParam");
    memcpy((void *)volume_param, (void *) & (audio_ver1_custom_default), sizeof(AUDIO_VER1_CUSTOM_VOLUME_STRUCT));
    return sizeof(AUDIO_VER1_CUSTOM_VOLUME_STRUCT);
}

// functions
int GetVolumeVer1ParamFromNV(AUDIO_VER1_CUSTOM_VOLUME_STRUCT *pPara)
{
    ALOGD("GetVolumeVer1ParamFromNV ");
    int result = 0;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }

#if defined(USE_DEFAULT_CUST_TABLE)
    // a default value , should disable when NVRAM ready
    result = getDefaultVer1VolumeParam(pPara);
    // get from NV ram and replace the default value
#else
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_VER1_VOLUME_CUSTOM_LID;
    int i = 0, rec_sizem, rec_size, rec_num;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetVolumeVer1ParamFromNV rec_size = %d rec_num = %d", rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
#endif
    return result;

}

int SetVolumeVer1ParamToNV(AUDIO_VER1_CUSTOM_VOLUME_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    // write to NV ram
    F_ID audio_nvram_fd;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_VER1_VOLUME_CUSTOM_LID;
    int i = 0, rec_sizem, rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetVolumeVer1ParamToNV audio_nvram_fd = %d", audio_nvram_fd);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
    return result;
}


int GetNBSpeechParamFromNVRam(AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB)
{
    int result = 0;
    if (checkNvramReady() == false) {
        ALOGE("checkNvramReady fail");
        return 0;
    }

#if defined(USE_DEFAULT_CUST_TABLE)
    // a default value, should disable when NVRAM ready
    result = getDefaultSpeechParam(pSphParamNB);
    // get from NVRam and replace the default value
#else
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_LID;
    int rec_size, rec_num;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetNBSpeechParamFromNVRam audio_nvram_fd = %d", audio_nvram_fd);
    ALOGD("GetNBSpeechParamFromNVRam rec_size = %d rec_num = %d", rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pSphParamNB , rec_size * rec_num);
    ALOGD("GetNBSpeechParamFromNVRam uMicbiasVolt = %d", pSphParamNB->uMicbiasVolt);
    NVM_CloseFileDesc(audio_nvram_fd);
#endif

    return result;
}

int SetNBSpeechParamToNVRam(AUDIO_CUSTOM_PARAM_STRUCT *pSphParamNB)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    // write to NV ram
    F_ID audio_nvram_fd;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetNBSpeechParamToNVRam audio_nvram_fd = %d", audio_nvram_fd);
    result = write(audio_nvram_fd.iFileDesc, pSphParamNB , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}

int getDefaultAudioGainTableParam(AUDIO_GAIN_TABLE_STRUCT *sphParam)
{    
    ALOGW("sizeof AUDIO_GAIN_TABLE_STRUCT = %d",sizeof(AUDIO_GAIN_TABLE_STRUCT));
    memcpy((void*)sphParam,(void*)&(Gain_control_table_default),sizeof(AUDIO_GAIN_TABLE_STRUCT));
    return sizeof(AUDIO_GAIN_TABLE_STRUCT);
}

int GetAudioGainTableParamFromNV(AUDIO_GAIN_TABLE_STRUCT *pPara)
{
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_GAIN_TABLE_LID;
    int rec_size, rec_num, result;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }
    #if defined(USE_DEFAULT_CUST_TABLE)
    result = getDefaultAudioGainTableParam(pPara);
    #else
    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetAudioGainTableParamFromNV audio_nvram_fd = %d rec_size = %d rec_num = %d", audio_nvram_fd, rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
    #endif
    return result;
}

int SetAudioGainTableParamToNV(AUDIO_GAIN_TABLE_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_GAIN_TABLE_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetAudioGainTableParamToNV audio_nvram_fd = %d", audio_nvram_fd);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}


int getDefaultWBSpeechParam(AUDIO_CUSTOM_WB_PARAM_STRUCT *sphParam)
{    
    ALOGW("sizeof AUDIO_CUSTOM_WB_PARAM_STRUCT = %d",sizeof(AUDIO_CUSTOM_WB_PARAM_STRUCT));
    memcpy((void*)sphParam,(void*)&(wb_speech_custom_default),sizeof(AUDIO_CUSTOM_WB_PARAM_STRUCT));
    return sizeof(AUDIO_CUSTOM_WB_PARAM_STRUCT);
}


int GetWBSpeechParamFromNVRam(AUDIO_CUSTOM_WB_PARAM_STRUCT *pSphParamWB)
{
    int result = 0;
    // a default value , should disable when NVRAM ready
    //getDefaultWBSpeechParam(pSphParamWB);
    // get from NV ram and replace the default value


    F_ID audio_nvram_fd;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_WB_PARAM_LID;
    int i = 0, rec_size, rec_num;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }

    #if defined(USE_DEFAULT_CUST_TABLE)
    result = getDefaultWBSpeechParam(pSphParamWB);
    #else

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetWBSpeechParamFromNVRam audio_nvram_fd = %d", audio_nvram_fd);
    ALOGD("GetWBSpeechParamFromNVRam rec_size = %d rec_num = %d", rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pSphParamWB , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
    #endif

    return result;
}

int SetWBSpeechParamToNVRam(AUDIO_CUSTOM_WB_PARAM_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    // write to NV ram
    F_ID audio_nvram_fd;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_WB_PARAM_LID;
    int i = 0, rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetWBSpeechParamToNVRam audio_nvram_fd = %d", audio_nvram_fd);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}

int getDefaultMedParam(AUDIO_PARAM_MED_STRUCT *pPara)
{
    // only for startup use
    ALOGW("sizeof AUDIO_PARAM_MED_STRUCT = %d", sizeof(AUDIO_PARAM_MED_STRUCT));
    memcpy((void *)pPara, (void *) & (audio_param_med_default), sizeof(AUDIO_PARAM_MED_STRUCT));
    return sizeof(AUDIO_PARAM_MED_STRUCT);
}

int GetMedParamFromNV(AUDIO_PARAM_MED_STRUCT *pPara)
{
    int result = 0;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }

#if defined(USE_DEFAULT_CUST_TABLE)
    // a default value , should disable when NVRAM ready
    result = getDefaultMedParam(pPara);
    // get from NV ram and replace the default value
#else
    F_ID audio_nvram_fd;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_PARAM_MED_LID;
    int rec_size, rec_num;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
#endif
    return result;
}

int SetMedParamToNV(AUDIO_PARAM_MED_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    // write to NV ram
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_PARAM_MED_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}

int getDefaultAudioCustomParam(AUDIO_VOLUME_CUSTOM_STRUCT *volParam)
{
    // only for startup use
    ALOGW("sizeof AUDIO_VOLUME_CUSTOM_STRUCT = %d", sizeof(AUDIO_VOLUME_CUSTOM_STRUCT));
    memcpy((void *)volParam, (void *) & (audio_volume_custom_default), sizeof(AUDIO_VOLUME_CUSTOM_STRUCT));
    return sizeof(AUDIO_VOLUME_CUSTOM_STRUCT);
}

// get audio custom parameter from NVRAM
int GetAudioCustomParamFromNV(AUDIO_VOLUME_CUSTOM_STRUCT *pPara)
{
    int result = 0;
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_VOLUME_CUSTOM_LID;
    int rec_size, rec_num;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }
#if defined(USE_DEFAULT_CUST_TABLE)
    // a default value , should disable when NVRAM ready
    result = getDefaultAudioCustomParam(pPara);
    // get from NV ram and replace the default value
#else
    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
#endif
    return result;
}

int SetAudioCustomParamToNV(AUDIO_VOLUME_CUSTOM_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    // write to NV ram
    F_ID audio_nvram_fd;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_VOLUME_CUSTOM_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}



//////////////////////////////////////////////
// Dual Mic Custom Parameter
//////////////////////////////////////////////

int getDefaultDualMicParam(AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *dualMicParam)
{
    ALOGD("sizeof AUDIO_CUSTOM_PARAM_STRUCT = %d", sizeof(AUDIO_CUSTOM_EXTRA_PARAM_STRUCT));
    memcpy((void *)dualMicParam, (void *) & (dual_mic_custom_default), sizeof(AUDIO_CUSTOM_EXTRA_PARAM_STRUCT));
    return sizeof(AUDIO_CUSTOM_EXTRA_PARAM_STRUCT);
}

// Get Dual Mic Custom Parameter from NVRAM
int GetDualMicSpeechParamFromNVRam(AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *pSphParamDualMic)
{
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }

    F_ID dualmic_nvram_fd;
    int file_lid = AP_CFG_RDCL_FILE_DUAL_MIC_CUSTOM_LID;
    int rec_size, rec_num, result;
#if defined(USE_DEFAULT_CUST_TABLE)
    // for test only
    // Get the Dual Mic default parameter, (Disable it when NVRAM ready)
    result = getDefaultDualMicParam(pSphParamDualMic);
    // get from NV ram and replace the default value    
#else  
    result = 0;
    dualmic_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("+GetDualMicSpeechParamFromNVRam audio_nvram_fd = %d", dualmic_nvram_fd);
    ALOGD("GetDualMicSpeechParamFromNVRam, rec_size=%d, rec_num=%d", rec_size, rec_num);
    result = read(dualmic_nvram_fd.iFileDesc, pSphParamDualMic , rec_size * rec_num);
    ALOGD("-GetDualMicSpeechParamFromNVRam");
    NVM_CloseFileDesc(dualmic_nvram_fd);
#endif    
    return result;
}

// Set Dual Mic Custom Parameter from NVRAM
int SetDualMicSpeechParamToNVRam(AUDIO_CUSTOM_EXTRA_PARAM_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    F_ID dualmic_nvram_fd;
    int file_lid = AP_CFG_RDCL_FILE_DUAL_MIC_CUSTOM_LID;
    int rec_size, rec_num, result;
    result = 0;

    dualmic_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("+SetDualMicSpeechParamToNVRam audio_nvram_fd = %d", dualmic_nvram_fd);
    result = write(dualmic_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    ALOGD("-SetDualMicSpeechParamToNVRam");
    NVM_CloseFileDesc(dualmic_nvram_fd);
    return result;
}

//////////////////////////////////////////////
// HD Record Custom Parameter
//////////////////////////////////////////////
#if defined(MTK_AUDIO_HD_REC_SUPPORT)
int getDefaultHdRecordParam(AUDIO_HD_RECORD_PARAM_STRUCT *pPara)
{
    ALOGD("sizeof AUDIO_HD_RECORD_PARAM_STRUCT = %d", sizeof(AUDIO_HD_RECORD_PARAM_STRUCT));
    memcpy((void *)pPara, (void *) & (Hd_Recrod_Par_default), sizeof(AUDIO_HD_RECORD_PARAM_STRUCT));
    return sizeof(AUDIO_HD_RECORD_PARAM_STRUCT);
}
/// Get HD record parameters from NVRAM
int GetHdRecordParamFromNV(AUDIO_HD_RECORD_PARAM_STRUCT *pPara)
{
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_HD_REC_PAR_LID;
    int rec_size, rec_num, result;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }
    #if defined(USE_DEFAULT_CUST_TABLE)
    result = getDefaultHdRecordParam(pPara);
    #else
    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetHdRecordParamFromNV rec_size = %d rec_num = %d", rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
    ALOGD("GetHdRecordParamFromNV result = %d", result);
    if(result!=rec_size)
        return 0;
    #endif
    return result;
}

/// Set HD record parameters to NVRAM
int SetHdRecordParamToNV(AUDIO_HD_RECORD_PARAM_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_HD_REC_PAR_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetHdRecordParamToNV audio_nvram_fd = %d rec_size = %d rec_num = %d", audio_nvram_fd, rec_size, rec_num);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}
int getDefaultHdRecordSceneTable(AUDIO_HD_RECORD_SCENE_TABLE_STRUCT *pPara)
{
    ALOGD("sizeof AUDIO_HD_RECORD_SCENE_TABLE_STRUCT = %d", sizeof(AUDIO_HD_RECORD_SCENE_TABLE_STRUCT));
    memcpy((void *)pPara, (void *) & (Hd_Recrod_Scene_Table_default), sizeof(AUDIO_HD_RECORD_SCENE_TABLE_STRUCT));
    return sizeof(AUDIO_HD_RECORD_SCENE_TABLE_STRUCT);
}
/// Get HD record scene tables from NVRAM
int GetHdRecordSceneTableFromNV(AUDIO_HD_RECORD_SCENE_TABLE_STRUCT *pPara)
{
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_HD_REC_SCENE_LID;
    int rec_size, rec_num, result;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }
    #if defined(USE_DEFAULT_CUST_TABLE)
    result = getDefaultHdRecordSceneTable(pPara);
    #else 
    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetHdRecordSceneTableFromNV rec_size = %d rec_num = %d", rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
    ALOGD("GetHdRecordSceneTableFromNV result = %d", result);
    if(result!=rec_size)
        return 0;
    #endif
    return result;
}

/// Set HD record scene tables to NVRAM
int SetHdRecordSceneTableToNV(AUDIO_HD_RECORD_SCENE_TABLE_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_HD_REC_SCENE_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetHdRecordSceneTableToNV audio_nvram_fd = %d rec_size = %d rec_num = %d", audio_nvram_fd, rec_size, rec_num);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}

int getDefaultHdRecord48kParam(AUDIO_HD_RECORD_48K_PARAM_STRUCT *pPara)
{
    ALOGD("sizeof AUDIO_HD_RECORD_48K_PARAM_STRUCT = %d", sizeof(AUDIO_HD_RECORD_48K_PARAM_STRUCT));
    memcpy((void *)pPara, (void *) & (Hd_Recrod_48k_Par_default), sizeof(AUDIO_HD_RECORD_48K_PARAM_STRUCT));
    return sizeof(AUDIO_HD_RECORD_48K_PARAM_STRUCT);
}


int GetHdRecord48kParamFromNV(AUDIO_HD_RECORD_48K_PARAM_STRUCT *pPara)
{
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_HD_REC_48K_PAR_LID;
    int rec_size, rec_num, result;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }
    #if defined(USE_DEFAULT_CUST_TABLE)
    result = getDefaultHdRecord48kParam(pPara);
    #else 
    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetHdRecord48kParamFromNV rec_size = %d rec_num = %d", rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
    ALOGD("GetHdRecord48kParamFromNV result = %d", result);
    if(result!=rec_size)
        return 0;
    #endif
    return result;
}

int SetHdRecord48kParamToNV(AUDIO_HD_RECORD_48K_PARAM_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_HD_REC_48K_PAR_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetHdRecord48kParamToNV audio_nvram_fd = %d rec_size = %d rec_num = %d", audio_nvram_fd, rec_size, rec_num);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}

#endif

int getDefaultVoiceRecogCustParam(VOICE_RECOGNITION_PARAM_STRUCT *pPara)
{
    ALOGD("sizeof VOICE_RECOGNITION_PARAM_STRUCT = %d", sizeof(VOICE_RECOGNITION_PARAM_STRUCT));
    memcpy((void *)pPara, (void *) & (Voice_Recognize_Par_default), sizeof(VOICE_RECOGNITION_PARAM_STRUCT));
    return sizeof(VOICE_RECOGNITION_PARAM_STRUCT);
}

// Get voice revognition customization parameters
int GetVoiceRecogCustParamFromNV(VOICE_RECOGNITION_PARAM_STRUCT *pPara)
{
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_VOICE_RECOGNIZE_PARAM_LID;
    int rec_size, rec_num, result;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }
    #if defined(USE_DEFAULT_CUST_TABLE)
    result = getDefaultVoiceRecogCustParam(pPara);
    #else 
    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetVoiceRecogCustParamFromNV rec_size = %d rec_num = %d", rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
    #endif
    return result;
}

// Set voice revognition customization parameters
int SetVoiceRecogCustParamToNV(VOICE_RECOGNITION_PARAM_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_VOICE_RECOGNIZE_PARAM_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetVoiceRecogCustParamToNV audio_nvram_fd = %d rec_size = %d rec_num = %d", audio_nvram_fd, rec_size, rec_num);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}

int getDefaultAudEnhControlOptionParam(AUDIO_AUDENH_CONTROL_OPTION_STRUCT *pPara)
{
    ALOGD("sizeof AUDIO_AUDENH_CONTROL_OPTION_STRUCT = %d", sizeof(AUDIO_AUDENH_CONTROL_OPTION_STRUCT));
    memcpy((void *)pPara, (void *) & (AUDENH_Control_Option_Par_default), sizeof(AUDIO_AUDENH_CONTROL_OPTION_STRUCT));
    return sizeof(AUDIO_AUDENH_CONTROL_OPTION_STRUCT);
}

int GetAudEnhControlOptionParamFromNV(AUDIO_AUDENH_CONTROL_OPTION_STRUCT *pPara)
{
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_AUDENH_CONTROL_OPTION_PAR_LID;
    int rec_size, rec_num, result;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }

#if defined(USE_DEFAULT_CUST_TABLE)
    result = getDefaultAudEnhControlOptionParam(pPara);
#else      

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetAudEnhControlOptionParamFromNV rec_size = %d rec_num = %d", rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
#endif    

    return result;
}

int SetAudEnhControlOptionParamToNV(AUDIO_AUDENH_CONTROL_OPTION_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_AUDENH_CONTROL_OPTION_PAR_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetAudEnhControlOptionParamToNV audio_nvram_fd = %d rec_size = %d rec_num = %d", audio_nvram_fd, rec_size, rec_num);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}

int getDefaultDcCalibrationParam(AUDIO_BUFFER_DC_CALIBRATION_STRUCT *pPara)
{
    ALOGD("sizeof AUDIO_BUFFER_DC_CALIBRATION_STRUCT = %d", sizeof(AUDIO_BUFFER_DC_CALIBRATION_STRUCT));
    memcpy((void *)pPara, (void *) & (Audio_Buffer_DC_Calibration_Par_default), sizeof(AUDIO_BUFFER_DC_CALIBRATION_STRUCT));
    return sizeof(AUDIO_BUFFER_DC_CALIBRATION_STRUCT);
}

int GetDcCalibrationParamFromNV(AUDIO_BUFFER_DC_CALIBRATION_STRUCT *pPara)
{
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_BUFFER_DC_CALIBRATION_PAR_LID;
    int rec_size, rec_num, result;
    if (!checkNvramReady()) {
        ALOGW("checkNvramReady fail");
        return 0;
    }

#if defined(USE_DEFAULT_CUST_TABLE)
    result = getDefaultDcCalibrationParam(pPara);
#else      

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISREAD);
    ALOGD("GetDcCalibrationParamFromNV rec_size = %d rec_num = %d", rec_size, rec_num);
    result = read(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);
#endif    

    return result;
}

int SetDcCalibrationParamToNV(AUDIO_BUFFER_DC_CALIBRATION_STRUCT *pPara)
{
    #if defined(USE_DEFAULT_CUST_TABLE)
    return 0;
    #endif
    
    F_ID audio_nvram_fd ;
    int file_lid = AP_CFG_RDCL_FILE_AUDIO_BUFFER_DC_CALIBRATION_PAR_LID;
    int rec_size, rec_num, result;

    audio_nvram_fd = NVM_GetFileDesc(file_lid, &rec_size, &rec_num, ISWRITE);
    ALOGD("SetDcCalibrationParamToNV audio_nvram_fd = %d rec_size = %d rec_num = %d", audio_nvram_fd, rec_size, rec_num);
    result = write(audio_nvram_fd.iFileDesc, pPara , rec_size * rec_num);
    NVM_CloseFileDesc(audio_nvram_fd);

    return result;
}

}; // namespace android
