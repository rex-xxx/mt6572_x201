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

/* //hardware/ril/reference-ril/ril_nw.c
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#include <telephony/ril.h>
#include <stdio.h>
#include <assert.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <pthread.h>
#include <alloca.h>
#include "atchannels.h"
#include "at_tok.h"
#include "misc.h"
#include <getopt.h>
#include <sys/socket.h>
#include <cutils/sockets.h>
#include <termios.h>
#include <ctype.h>

/*ADD-BEGIN-JUNGO-20101008-CTZV support */
#include <time.h>

#include <ril_callbacks.h>
#include <ril_nw.h>


#ifdef MTK_RIL_MD1
#define LOG_TAG "RIL"
#else
#define LOG_TAG "RILMD2"
#endif

#include <utils/Log.h>

#include <cutils/properties.h>


/* Global data Begin */

/* NITZ Operator Name */
static pthread_mutex_t ril_nw_nitzName_mutex[RIL_NW_NUM] = RIL_NW_INIT_MUTEX;
static char ril_nw_nitz_oper_code[RIL_NW_NUM][MAX_OPER_NAME_LENGTH] = RIL_NW_INIT_STRING;
static char ril_nw_nitz_oper_lname[RIL_NW_NUM][MAX_OPER_NAME_LENGTH]= RIL_NW_INIT_STRING;
static char ril_nw_nitz_oper_sname[RIL_NW_NUM][MAX_OPER_NAME_LENGTH]= RIL_NW_INIT_STRING;

/* Restrition State */
int ril_nw_cur_state[RIL_NW_NUM] = RIL_NW_INIT_STATE;

// ALPS00353868 START
#define PROPERTY_GSM_CURRENT_COPS_LAC    "gsm.cops.lac"
static int plmn_list_format[RIL_NW_NUM] = RIL_NW_INIT_INT; 
// ALPS00353868 END

char* s_imei[MTK_RIL_SOCKET_NUM] = {0};
char* s_imeisv[MTK_RIL_SOCKET_NUM] = {0};
char* s_basebandVersion[MTK_RIL_SOCKET_NUM] = {0};
char* s_projectFlavor[MTK_RIL_SOCKET_NUM] = {0};
char* s_calData = 0;

int sim_inserted_status = -1;

int gcf_test_mode = 0;

int bPSBEARERSupport = 1;

int s_md_off = 0;
int s_main_loop = 1;

/*RIL Network Static functions */

// ALPS00353868 START
int setPlmnListFormat(RILId rid, int format){
    if(rid < RIL_NW_NUM){
        /* 0: standard +COPS format , 1: standard +COPS format plus <lac> */		
        plmn_list_format[rid] = format; 
    }		
	return 1;
}
// ALPS00353868 END

int getSingnalStrength(char *line, int *rssi, int *ber, int *rssi_qdbm, int *rscp_qdbm, int *ecn0_qdbm)
{
    int err;
    err = at_tok_start(&line);
    if (err < 0) return -1;

    err = at_tok_nextint(&line, rssi);
    if (err < 0) return -1;
    else if( (*rssi < 0  || *rssi > 31) && *rssi != 99)   // check if the value is valid or not
    {
        LOGE("Recevice an invalid value from modem for <rssi>");
        return -1;
    }

    err = at_tok_nextint(&line, ber);
    if (err < 0) return -1;
    else if( (*ber < 0  || *ber > 7) && *ber != 99)   // check if the value is valid or not
    {
        LOGE("Recevice an invalid value from modem for <ber>");
        return -1;
    }
    err = at_tok_nextint(&line, rssi_qdbm);
    if (err < 0) 
	{
	    LOGE("Recevice an invalid value from modem for <rssi_qdbm>");
		return -1;
    }
    err = at_tok_nextint(&line, rscp_qdbm);
    
    // ALPS00506562 
    //if (err < 0) *rscp_qdbm=-1;
    if (err < 0) *rscp_qdbm=0;
    
    err = at_tok_nextint(&line, ecn0_qdbm);    
    
    // ALPS00506562 
    //if (err < 0) *ecn0_qdbm= -1;
    if (err < 0) *ecn0_qdbm= 0;
      
    return 0;

}

static int getOperatorNamesFromNumericCode(
    char *code,
    char *longname,
    char *shortname,
    int max_length,
    RILId rid)
{
    char nitz[PROPERTY_VALUE_MAX];
    char oper_file_path[PROPERTY_VALUE_MAX];
    char oper[128], name[MAX_OPER_NAME_LENGTH];
    char *line, *tmp;
    FILE *list;
    int err;

    char *oper_code, *oper_lname, *oper_sname;

    if (max_length > MAX_OPER_NAME_LENGTH)
    {
        LOGE("The buffer size %d is not valid. We only accept the length less than or equal to %d",
             max_length, MAX_OPER_NAME_LENGTH);
        return -1;
    }

    oper_code = ril_nw_nitz_oper_code[rid];
    oper_lname = ril_nw_nitz_oper_lname[rid];
    oper_sname = ril_nw_nitz_oper_sname[rid];

    longname[0] = '\0';
    shortname[0] = '\0';

    pthread_mutex_lock(&ril_nw_nitzName_mutex[rid]);
    LOGD("Get ril_nw_nitzName_mutex in the getOperatorNamesFromNumericCode");

    /* Check if there is a NITZ name*/
    /* compare if the operator code is the same with <oper>*/
    if(strcmp(code, oper_code) == 0)
    {
        /* there is a NITZ Operator Name*/
        /*get operator code and name*/
        /*set short name with long name when short name is null and long name isn't, and vice versa*/
        int nlnamelen = strlen(oper_lname);
        int nsnamelen = strlen(oper_sname);
        if(nlnamelen != 0 && nsnamelen != 0)
        {
            strncpy(longname,oper_lname, max_length);
            strncpy(shortname, oper_sname, max_length);
        }
        else if(strlen(oper_sname) != 0)
        {
            strncpy(longname, oper_sname, max_length);
            strncpy(shortname, oper_sname, max_length);
        }
        else if(strlen(oper_lname) != 0)
        {
            strncpy(longname, oper_lname, max_length);
            strncpy(shortname, oper_lname, max_length);
        }

        LOGD("Return NITZ Operator Name: %s %s %s, lname length: %d, sname length: %d", oper_code,
                                                                                        oper_lname,
                                                                                        oper_sname,
                                                                                        nlnamelen,
                                                                                        nsnamelen);
    }
    else
    {
        strcpy(longname, code);
        strcpy(shortname, code);
    }
    
#if 0
    /* get the path of operator file*/
    else if( property_get(OPER_FILE_PROPERTY, oper_file_path, NULL) == 0 )
    {
        LOGW("There is no opertor list file, please check the property: %s", OPER_FILE_PROPERTY);
        goto error;
    }
    else
    {
        /* Open operator list file*/
        list = fopen(oper_file_path, "r");
        if (list == NULL)
        {
            LOGE("Can't open the file %s : %d", oper_file_path, errno);
            goto error;
        }

        /* search according to the <oper> code*/
        while( fgets(oper, 128, list) != NULL )
        {
            line = oper;
            /* get <oper> code  */
            err = at_tok_nextstr(&line, &tmp);
            if (err < 0) continue;

            /* compare if the code is the same or not*/
            if(strcmp(code, tmp) == 0)
            {
                /*get long name*/
                err = at_tok_nextstr(&line, &tmp);
                if (err < 0)
                    continue;
                strncpy(longname, tmp, max_length);
                longname[max_length - 1] = '\0';

                /*get short name*/
                err = at_tok_nextstr(&line, &tmp);
                if (err < 0)
                    continue;
                strncpy(shortname, tmp, max_length);
                shortname[max_length - 1] = '\0';

                break;
            }
        }

        fclose(list);
    }
#endif

    pthread_mutex_unlock(&ril_nw_nitzName_mutex[rid]);
    return 0;
error:
    pthread_mutex_unlock(&ril_nw_nitzName_mutex[rid]);
    return -1;
}

void updateNitzOperInfo(RILId rid) {
    if (getMappingSIMByCurrentMode(rid) == GEMINI_SIM_1) {
        property_get(PROPERTY_NITZ_OPER_CODE, ril_nw_nitz_oper_code[rid], NULL);
        property_get(PROPERTY_NITZ_OPER_LNAME, ril_nw_nitz_oper_lname[rid], NULL);
        property_get(PROPERTY_NITZ_OPER_SNAME, ril_nw_nitz_oper_sname[rid], NULL);
#if (MTK_GEMINI_SIM_NUM >= 4)				
    } else if(rid == MTK_RIL_SOCKET_4){
        property_get(PROPERTY_NITZ_OPER_CODE4, ril_nw_nitz_oper_code[rid], NULL);
        property_get(PROPERTY_NITZ_OPER_LNAME4, ril_nw_nitz_oper_lname[rid], NULL);
        property_get(PROPERTY_NITZ_OPER_SNAME4, ril_nw_nitz_oper_sname[rid], NULL);
#endif		
#if (MTK_GEMINI_SIM_NUM >= 3)				
    } else if(rid == MTK_RIL_SOCKET_3){
        property_get(PROPERTY_NITZ_OPER_CODE3, ril_nw_nitz_oper_code[rid], NULL);
        property_get(PROPERTY_NITZ_OPER_LNAME3, ril_nw_nitz_oper_lname[rid], NULL);
        property_get(PROPERTY_NITZ_OPER_SNAME3, ril_nw_nitz_oper_sname[rid], NULL);		
#endif		
    } else {
        property_get(PROPERTY_NITZ_OPER_CODE2, ril_nw_nitz_oper_code[rid], NULL);
        property_get(PROPERTY_NITZ_OPER_LNAME2, ril_nw_nitz_oper_lname[rid], NULL);
        property_get(PROPERTY_NITZ_OPER_SNAME2, ril_nw_nitz_oper_sname[rid], NULL);
    }
    
    LOGD("[RIL%d] NITZ Operator Name : %s %s %s", (getMappingSIMByCurrentMode(rid)+1),
        ril_nw_nitz_oper_code[rid], ril_nw_nitz_oper_lname[rid], ril_nw_nitz_oper_sname[rid]);
}

/*RIL Network functions */

/** returns 1 if on, 0 if off, 4 for airplane mode. and -1 on error */
int queryRadioState(RILId rid)
{
    ATResponse *p_response = NULL;
    int err;
    char *line;
    int ret;

    err = at_send_command_singleline("AT+CFUN?", "+CFUN:", &p_response, getChannelCtxbyProxy(rid));

    if (err < 0 || p_response->success == 0) {
        // assume radio is off
        goto error;
    }

    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);
    if (err < 0) goto error;

    err = at_tok_nextint(&line, &ret);
    if (err < 0) goto error;

    at_response_free(p_response);

    return ret;

error:
    at_response_free(p_response);
    return -1;
}


/*RIL Network functions */

/** returns 1 if on, 0 if off, and -1 on error */
int isRadioOn(RILId rid)
{
    ATResponse *p_response = NULL;
    int err;
    char *line;
    int ret;

    err = at_send_command_singleline("AT+CFUN?", "+CFUN:", &p_response, getChannelCtxbyProxy(MTK_RIL_SOCKET_1));

    if (err < 0 || p_response->success == 0) {
        // assume radio is off
        goto error;
    }

    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);
    if (err < 0) goto error;

    err = at_tok_nextint(&line, &ret);
    if (err < 0) goto error;

    at_response_free(p_response);

    ret = (ret == 4 || ret == 0) ? 0 :    // phone off
          (ret == 1) ? 1 :              // phone on
          -1;                           // invalid value

    return ret;

error:
    at_response_free(p_response);
    return -1;
}

void requestSignalStrength(void * data, size_t datalen, RIL_Token t)
{
    ATResponse *p_response = NULL;
    int err;
    //MTK-START [ALPS00506562][ALPS00516994]
    //int response[12]={0};    
    int response[16] ={0};
    //MTK-START [ALPS00506562][ALPS00516994]
    char *line;

    memset(response, 0, sizeof(response));

    err = at_send_command_singleline("AT+ECSQ", "+ECSQ:", &p_response, NW_CHANNEL_CTX);

    if (err < 0 || p_response->success == 0 ||
            p_response->p_intermediates  == NULL) {
        //RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0); //mtk02514
        goto error;
    }

    line = p_response->p_intermediates->line;
    err = getSingnalStrength(line, &response[0], &response[1], &response[2], &response[3], &response[4]);

    if (err < 0) goto error;

    // 2G sim has only three parameter,3G sim has five parameters
    RIL_onRequestComplete(t, RIL_E_SUCCESS, response, sizeof(response));

    at_response_free(p_response);
    return;

error:
    LOGE("requestSignalStrength must never return an error when radio is on");
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);
}

static unsigned int convertNetworkType(unsigned int uiResponse)
{
    unsigned int uiRet = 0;

    /* mapping */
    switch(uiResponse)
    {
    case 0:     //GSM
    case 1:     //GSM compact
        uiRet = 1;        // GPRS only
        break;
    case 2:     //UTRAN
        uiRet = 3;        // UMTS
        break;
    case 3:     //GSM w/EGPRS
        uiRet = 2;        // EDGE
        break;
    case 4:     //UTRAN w/HSDPA
        uiRet = 9;        // HSDPA
        break;
    case 5:     //UTRAN w/HSUPA
        uiRet = 10;        // HSUPA
        break;
    case 6:     //UTRAN w/HSDPA and HSUPA
        uiRet = 11;        // HSPA
        break;
    default:
        uiRet = 0;        // Unknown
        break;
    }

    return uiRet;
}

static unsigned int convertCellSppedSupport(unsigned int uiResponse)
{
    // Cell speed support is bitwise value of cell capability:
    // bit7 0x80  bit6 0x40  bit5 0x20  bit4 0x10  bit3 0x08  bit2 0x04  bit1 0x02  bit0 0x01
    // Dual-Cell  HSUPA+     HSDPA+     HSUPA      HSDPA      UMTS       EDGE       GPRS
    unsigned int uiRet = 0;

    if ((uiResponse & 0x80) != 0 ||
            (uiResponse & 0x40) != 0 ||
            (uiResponse & 0x20) != 0) {
        uiRet = 15; // ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP
    } else if ((uiResponse & 0x10) != 0 &&
            (uiResponse & 0x08) != 0) {
        uiRet = 11; // ServiceState.RIL_RADIO_TECHNOLOGY_HSPA
    } else if ((uiResponse & 0x10) != 0) {
        uiRet = 10; // ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA
    } else if ((uiResponse & 0x08) != 0) {
        uiRet = 9;  // ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA
    } else if ((uiResponse & 0x04) != 0) {
        uiRet = 3;  // ServiceState.RIL_RADIO_TECHNOLOGY_UMTS
    } else if ((uiResponse & 0x02) != 0) {
        uiRet = 2;  // ServiceState.RIL_RADIO_TECHNOLOGY_EDGE
    } else if ((uiResponse & 0x01) != 0) {
        uiRet = 1;  // ServiceState.RIL_RADIO_TECHNOLOGY_GPRS
    } else {
        uiRet = 0;  // ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN
    }

    return uiRet;
}

static unsigned int convertPSBearerCapability(unsigned int uiResponse)
{
    /*
     *typedef enum
     *{
  *    L4C_NONE_ACTIVATE = 0,
  *    L4C_GPRS_CAPABILITY,
  *    L4C_EDGE_CAPABILITY,
  *    L4C_UMTS_CAPABILITY,
  *    L4C_HSDPA_CAPABILITY, //mac-hs
  *    L4C_HSUPA_CAPABILITY, //mac-e/es
  *    L4C_HSDPA_HSUPA_CAPABILITY, //mac-hs + mac-e/es

  *    L4C_HSDPAP_CAPABILITY, //mac-ehs
  *    L4C_HSDPAP_UPA_CAPABILITY, //mac-ehs + mac-e/es
  *    L4C_HSUPAP_CAPABILITY, //mac-i/is
  *    L4C_HSUPAP_DPA_CAPABILITY, //mac-hs + mac-i/is
  *    L4C_HSPAP_CAPABILITY, //mac-ehs + mac-i/is
  *    L4C_DC_DPA_CAPABILITY, //(DC) mac-hs
  *    L4C_DC_DPA_UPA_CAPABILITY, //(DC) mac-hs + mac-e/es
  *    L4C_DC_HSDPAP_CAPABILITY, //(DC) mac-ehs
  *    L4C_DC_HSDPAP_UPA_CAPABILITY, //(DC) mac-ehs + mac-e/es
  *    L4C_DC_HSUPAP_DPA_CAPABILITY, //(DC) mac-hs + mac-i/is
  *    L4C_DC_HSPAP_CAPABILITY, //(DC) mac-ehs + mac-i/is
     *    } l4c_data_bearer_capablility_enum;
    */

    unsigned int uiRet = 0;

    switch (uiResponse)
    {
    case 1:
        uiRet = 1;  // ServiceState.RIL_RADIO_TECHNOLOGY_GPRS
        break;
    case 2:
        uiRet = 2;  // ServiceState.RIL_RADIO_TECHNOLOGY_EDGE
        break;
    case 3:
        uiRet = 3;  // ServiceState.RIL_RADIO_TECHNOLOGY_UMTS
        break;
    case 4:
        uiRet = 9;  // ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA
        break;
    case 5:
        uiRet = 10; // ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA
        break;
    case 6:
        uiRet = 11; // ServiceState.RIL_RADIO_TECHNOLOGY_HSPA
        break;
    case 7:
    case 8:
    case 9:
    case 10:
    case 11:
    case 12:
    case 13:
    case 14:
    case 15:
    case 16:
    case 17:
        uiRet = 15; // ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP
        break;
    default:
        uiRet = 0;  // ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN
        break;
    }

    return uiRet;
}

static void registrationStateRequest(int request, RIL_Token t)
{
    int err;
    unsigned int response[4];
    char * responseStr[4];
    ATResponse *p_response = NULL;
    const char *cmd;
    const char *prefix;
    char *line, *p;
    int commas;
    int skip;
    int count = 3;
	int is_cs = 0;
	RILId rid = getRILIdByChannelCtx(getRILChannelCtxFromToken(t));

    if (request == RIL_REQUEST_VOICE_REGISTRATION_STATE) {
        cmd = "AT+CREG?";
        prefix = "+CREG:";
		is_cs = 1;
    } else if (request == RIL_REQUEST_DATA_REGISTRATION_STATE) {
        cmd = "AT+CGREG?";
        prefix = "+CGREG:";
		is_cs = 0;
    } else {
        assert(0);
        goto error;
    }

    err = at_send_command_singleline(cmd, prefix, &p_response, NW_CHANNEL_CTX);

    if (err != 0 || p_response->success == 0 ||
            p_response->p_intermediates == NULL) goto error;

    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);
    if (err < 0) goto error;

    /* Ok you have to be careful here
     * The solicited version of the CREG response is
     * +CREG: n, stat, [lac, cid]
     * and the unsolicited version is
     * +CREG: stat, [lac, cid]
     * The <n> parameter is basically "is unsolicited creg on?"
     * which it should always be
     *
     * Now we should normally get the solicited version here,
     * but the unsolicited version could have snuck in
     * so we have to handle both
     *
     * Also since the LAC and CID are only reported when registered,
     * we can have 1, 2, 3, or 4 arguments here
     *
     * finally, a +CGREG: answer may have a fifth value that corresponds
     * to the network type, as in;
     *
     *   +CGREG: n, stat [,lac, cid [,networkType]]
     */

    /***********************************
    * In the mtk solution, we do not need to handle the URC
    * Since the URC will received in the URC channel.
    * So we don't need to follow the above command from google.
    * But we might return <AcT> for +CREG if the stat==2
    * while <lac> is present
    ************************************/

    /* +CREG: <n>, <stat> */
    /* +CREG: <n>, <stat>, <lac>, <cid>, <Act> */
    /* +CGREG: <n>, <stat>, <lac>, <cid>, <Act> */

    /* <n> */
    err = at_tok_nextint(&line, &skip);
    if (err < 0) goto error;

    /* <stat> */
    err = at_tok_nextint(&line, (int*) &response[0]);
    if (err < 0 || response[0] > 5 )
    {
        LOGE("The value in the field <stat> is not valid: %d", response[0] );
        goto error;
    }

    if( at_tok_hasmore(&line) )
    {
    	LOGE("The value in the field <stat> is %d.", response[0]);
        if(response[0] != 1 && response[0] != 5)
        {
            LOGE("The value in the field <stat> is not 1 or 5. <stat>:%d", response[0]);
            goto error;
        }

        /* <lac> */
		LOGE("The value in the field <lac> :%d", response[1]);
        err = at_tok_nexthexint(&line, (int*)&response[1]);
        if ( err < 0 || (response[1] > 0xffff && response[1] != 0xffffffff) )
        {
            LOGE("The value in the field <lac> or <stat> is not valid. <stat>:%d, <lac>:%d",
                 response[0], response[1] );
            goto error;
        }

        /* <cid> */
        err = at_tok_nexthexint(&line, (int*)&response[2]);
        LOGD("cid: %d", response[2] );
        if (err < 0 || (response[2] > 0x0fffffff && response[2] != 0xffffffff) )
        {
            LOGE("The value in the field <cid> is not valid: %d", response[2] );
            goto error;
        }

        //if (request == RIL_REQUEST_GPRS_REGISTRATION_STATE)
        {
            /* <Act> */
            err = at_tok_nextint(&line, (int*)&response[3]);
            LOGE("The value of act: %d", response[3] );
            if (err < 0)
            {
                LOGE("No act in command");
                goto error;
            }
            count = 4;
            if (response[3] > 6)
            {
                LOGE("The value in the act is not valid: %d", response[3] );
                goto error;
            }

            /* mapping */
            response[3] = convertNetworkType(response[3]);
        }
    }
    else
    {
        /* +CREG: <n>, <stat> */
        /* +CGREG: <n>, <stat> */
		LOGE("it is short CREG CGREG");
        response[1] = -1;
        response[2] = -1;
        response[3] = 0;
        //BEGIN mtk03923 [20120119][ALPS00112664]
        count = 4;
        //END   mtk03923 [20120119][ALPS00112664]
    }

    asprintf(&responseStr[0], "%d", response[0]);
    if (response[1] != 0xffffffff)
        asprintf(&responseStr[1], "%x", response[1]);
    else
        asprintf(&responseStr[1], "-1");
    if (response[2] != 0xffffffff)
        asprintf(&responseStr[2], "%x", response[2]);
    else
        asprintf(&responseStr[2], "-1");

    if (count == 4)
    {
        asprintf(&responseStr[3], "%d", response[3]);
        //count = 4;
    }
	LOGE("rid: %d",rid);
	if (response[0] == 1 || response[0] == 5)
    {
    	//LOGE("!!!!in service");
        if (is_cs)
    	{
            //	LOGE("!!!!IN1 service");
            cleanCurrentRestrictionState(RIL_NW_ALL_CS_RESTRICTIONS, rid);
    	}
        else
        {
            //	LOGE("!!!!IN2 service");
            cleanCurrentRestrictionState(RIL_RESTRICTED_STATE_PS_ALL, rid);
        }
    }
    else
    {
        // LOGE("!!!!out service");
        if (is_cs)
        {
            //   LOGE("!!!!out1 service");
            setCurrentRestrictionState(RIL_RESTRICTED_STATE_CS_NORMAL, rid);
        }
        else
        {
            //   LOGE("!!!!ou2t service");
            setCurrentRestrictionState(RIL_RESTRICTED_STATE_PS_ALL, rid);
        }
    }
	onRestrictedStateChanged(rid);
    RIL_onRequestComplete(t, RIL_E_SUCCESS, responseStr, count*sizeof(char*));

    at_response_free(p_response);
    free(responseStr[0]);
    free(responseStr[1]);
    free(responseStr[2]);
    if(count == 4)
        free(responseStr[3]);

    return;

error:
    LOGE("requestRegistrationState must never return an error when radio is on");
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);
}

void requestRegistrationState(void * data, size_t datalen, RIL_Token t)
{
    registrationStateRequest(RIL_REQUEST_VOICE_REGISTRATION_STATE, t);
}

void requestGprsRegistrationState(void * data, size_t datalen, RIL_Token t)
{
    /* 
     * A +CGREG: answer may have a fifth value that corresponds
     * to the network type, as in;
     *
     * +CGREG: n, stat [,lac, cid [,networkType]]
     */

    /***********************************
    * In the mtk solution, we do not need to handle the URC
    * Since the URC will received in the URC channel.
    * So we don't need to follow the above command from google.
    * But we might return <AcT> for +CREG if the stat==2
    * while <lac> is present
    ************************************/

    /* +CGREG: <n>, <stat>, <lac>, <cid>, <Act> */

    int err;
    unsigned int response[6];
    char * responseStr[6] = {NULL, NULL, NULL, NULL, NULL, NULL};
    ATResponse *p_response = NULL;
    const char *cmd;
    const char *prefix;
    char *line, *p;
    int commas;
    int skip, i;
    int count = 3;
    RILId rid = getRILIdByChannelCtx(getRILChannelCtxFromToken(t));

    cmd = "AT+CGREG?";
    prefix = "+CGREG:";

    err = at_send_command_singleline(cmd, prefix, &p_response, NW_CHANNEL_CTX);

    if (err != 0 || p_response->success == 0 ||
            p_response->p_intermediates == NULL) goto error;

    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);
    if (err < 0) goto error;

    /* <n> */
    err = at_tok_nextint(&line, &skip);
    if (err < 0) goto error;

    /* <stat> */
    err = at_tok_nextint(&line, (int*) &response[0]);
    if (err < 0 || response[0] > 5 )
    {
        LOGE("The value in the field <stat> is not valid: %d", response[0] );
        goto error;
    }

    if( at_tok_hasmore(&line) )
    {
    	LOGE("The value in the field <stat> is %d.", response[0]);
        if(response[0] != 1 && response[0] != 5)
        {
            LOGE("The value in the field <stat> is not 1 or 5. <stat>:%d", response[0]);
            goto error;
        }

        /* <lac> */
        LOGE("The value in the field <lac> :%d", response[1]);
        err = at_tok_nexthexint(&line, (int*)&response[1]);
        if ( err < 0 || (response[1] > 0xffff && response[1] != 0xffffffff) )
        {
            LOGE("The value in the field <lac> or <stat> is not valid. <stat>:%d, <lac>:%d",
                 response[0], response[1] );
            goto error;
        }

        /* <cid> */
        err = at_tok_nexthexint(&line, (int*)&response[2]);
        LOGD("cid: %d", response[2] );
        if (err < 0 || (response[2] > 0x0fffffff && response[2] != 0xffffffff) )
        {
            LOGE("The value in the field <cid> is not valid: %d", response[2] );
            goto error;
        }

        /* <Act> */
        err = at_tok_nextint(&line, (int*)&response[3]);
        LOGE("The value of act: %d", response[3] );
        if (err < 0)
        {
            LOGE("No act in command");
            goto error;
        }
        count = 4;
        if (response[3] > 6)
        {
            LOGE("The value in the act is not valid: %d", response[3] );
            goto error;
        }

        /* mapping */
        response[3] = convertNetworkType(response[3]);
    }
    else
    {
        /* +CGREG: <n>, <stat> */
	LOGE("it is short CREG CGREG");
        response[1] = -1;
        response[2] = -1;
        response[3] = 0;
        //BEGIN mtk03923 [20120119][ALPS00112664]
        count = 4;
        //END   mtk03923 [20120119][ALPS00112664]
    }

    if (bPSBEARERSupport == 1) {
        at_response_free(p_response);
        p_response = NULL;
        cmd = "AT+PSBEARER?";
        prefix = "+PSBEARER:";
        
        err = at_send_command_singleline(cmd, prefix, &p_response, NW_CHANNEL_CTX);

        if (err != 0 || p_response->success == 0 ||
                p_response->p_intermediates == NULL) goto skipR7R8;
        
        line = p_response->p_intermediates->line;

        err = at_tok_start(&line);
        if (err < 0) goto skipR7R8;

        /* <cell_data_speed_support> */
        err = at_tok_nextint(&line, (int*)&response[4]);
        if (err < 0) goto skipR7R8;
        count = 5;
        response[4] = convertCellSppedSupport(response[4]);

        /* <max_data_bearer_capability> */
        err = at_tok_nextint(&line, (int*)&response[5]);
        if (err < 0) goto skipR7R8;
        count = 6;
        response[5] = convertPSBearerCapability(response[5]);
    }


skipR7R8:

    asprintf(&responseStr[0], "%d", response[0]);
    if (response[1] != 0xffffffff)
        asprintf(&responseStr[1], "%x", response[1]);
    else
        asprintf(&responseStr[1], "-1");
    if (response[2] != 0xffffffff)
        asprintf(&responseStr[2], "%x", response[2]);
    else
        asprintf(&responseStr[2], "-1");

    if (count == 6) {
        asprintf(&responseStr[3], "%d", (response[4] > response[5])? response[4] : response[5]);
        asprintf(&responseStr[4], "%d", response[4]);
        asprintf(&responseStr[5], "%d", response[5]);        
    } else {
        asprintf(&responseStr[3], "%d", response[3]);
    }
    
    LOGE("rid: %d",rid);
    if (response[0] == 1 || response[0] == 5) {
        cleanCurrentRestrictionState(RIL_RESTRICTED_STATE_PS_ALL, rid);
    } else {
        setCurrentRestrictionState(RIL_RESTRICTED_STATE_PS_ALL, rid);
    }
    onRestrictedStateChanged(rid);

    RIL_onRequestComplete(t, RIL_E_SUCCESS, responseStr, count*sizeof(char*));

    at_response_free(p_response);
    for (i=0; i<count; ++i) {
        if (responseStr[i] != NULL)
        free(responseStr[i]);
    }

    return;

error:
    LOGE("requestRegistrationState must never return an error when radio is on");
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);
}

void requestOperator(void * data, size_t datalen, RIL_Token t)
{
    int err;
    int skip;
    char *response[3];
    char *line;
    char longname[MAX_OPER_NAME_LENGTH], shortname[MAX_OPER_NAME_LENGTH];
    const RILId rid = getRILIdByChannelCtx(NW_CHANNEL_CTX);
    ATResponse *p_response = NULL;

    memset(response, 0, sizeof(response));

    err = at_send_command_singleline(
              "AT+COPS=3,2;+COPS?",
              "+COPS:", &p_response, NW_CHANNEL_CTX);

    /* we expect 1 lines here:
     * +COPS: 0,2,"310170"
     */

    if (err != 0 || p_response->success == 0 ||
            p_response->p_intermediates == NULL) goto error;

    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);
    if (err < 0) goto error;

    /* <mode> */
    err = at_tok_nextint(&line, &skip);
    if (err < 0 || skip < 0 || skip > 4)
    {
        LOGE("The <mode> is an invalid value!!!");
        goto error;
    }
    else if(skip == 2)  // deregister
    {
        LOGW("The <mode> is 2 so we ignore the follwoing fields!!!");
    }
    else if (!at_tok_hasmore(&line))
    {
        // If we're unregistered, we may just get
        // a "+COPS: 0" response
    }
    else
    {
        /* <format> */
        err = at_tok_nextint(&line, &skip);
        if (err < 0 || skip != 2)
        {
            LOGW("The <format> is incorrect: expect 2, receive %d", skip);
            goto error;
        }

        // a "+COPS: 0, n" response is also possible
        if (!at_tok_hasmore(&line)) {
            ;
        }
        else
        {
            /* <oper> */
            err = at_tok_nextstr(&line, &(response[2]));
            if (err < 0) goto error;

            LOGD("Get operator code %s", response[2]);

            err = getOperatorNamesFromNumericCode(
                      response[2], longname, shortname, MAX_OPER_NAME_LENGTH, rid);

            if(err>=0)
            {
                response[0] = longname;
                response[1] = shortname;
            }

        }
    }

    RIL_onRequestComplete(t, RIL_E_SUCCESS, response, sizeof(response));
    at_response_free(p_response);

    return;
error:
    LOGE("requestOperator must not return error when radio is on");
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);
}

void requestGetImei(void * data, size_t datalen, RIL_Token t)
{
    /*RILChannelCtx* p_channel = getRILChannelCtxFromToken(t);
    RILId rid = getRILIdByChannelCtx(p_channel);

    if (s_imei[rid] == NULL) {
        ATResponse *p_response = NULL;
        int err;

        err = at_send_command_numeric("AT+CGSN", &p_response, NW_CHANNEL_CTX);

        if (err < 0 || p_response->success == 0) {
            RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
        } else {
            err = asprintf(s_imei[rid], "%s", p_response->p_intermediates->line);
            if(err >= 0)
                RIL_onRequestComplete(t, RIL_E_SUCCESS, p_response->p_intermediates->line, sizeof(char *));
        }
        at_response_free(p_response);
    } else {
        RIL_onRequestComplete(t, RIL_E_SUCCESS, s_imei[rid], sizeof(char *));
    }*/

    RILChannelCtx* p_channel = getRILChannelCtxFromToken(t);
    RILId rid = getRILIdByChannelCtx(p_channel);
    if (s_imei[rid] == NULL)
        RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    else
        RIL_onRequestComplete(t, RIL_E_SUCCESS, s_imei[rid], sizeof(char *));

}

void requestGetImeisv(void * data, size_t datalen, RIL_Token t)
{
    /*ATResponse *p_response = NULL;
    int err;
    char *line, *sv;

    err = at_send_command_singleline("AT+EGMR=0,9", "+EGMR:",&p_response, NW_CHANNEL_CTX);

    if (err < 0 || p_response->success == 0) {
        goto error;
    } else {
        line = p_response->p_intermediates->line;

        err = at_tok_start(&line);
        if(err < 0) goto error;

        err = at_tok_nextstr(&line, &sv);
        if(err < 0) goto error;

        RIL_onRequestComplete(t, RIL_E_SUCCESS,
                              sv, sizeof(char *));
    }
    at_response_free(p_response);
    return;
error:
    at_response_free(p_response);
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);*/

    RILChannelCtx* p_channel = getRILChannelCtxFromToken(t);
    RILId rid = getRILIdByChannelCtx(p_channel);
    if (s_imeisv[rid] == NULL)
        RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    else
        RIL_onRequestComplete(t, RIL_E_SUCCESS, s_imeisv[rid], sizeof(char *));
}

void requestQueryNetworkSelectionMode(void * data, size_t datalen, RIL_Token t)
{
    int err;
    ATResponse *p_response = NULL;
    int response = 0;
    char *line;

    err = at_send_command_singleline("AT+COPS?", "+COPS:", &p_response, NW_CHANNEL_CTX);

    if (err < 0 || p_response->success == 0) {
        goto error;
    }

    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);

    if (err < 0)
    {
        goto error;
    }

    err = at_tok_nextint(&line, &response);

    if ( err < 0 || ( response < 0 || response > 4|| response == 3) )
    {
        goto error;
    }

    RIL_onRequestComplete(t, RIL_E_SUCCESS, &response, sizeof(int));
    at_response_free(p_response);
    return;
error:
    at_response_free(p_response);
    LOGE("requestQueryNetworkSelectionMode must never return error when radio is on");
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
}

void requestSetNetworkSelectionAutomatic(void * data, size_t datalen, RIL_Token t)
{
    int err;
    ATResponse *p_response = NULL;
    RIL_Errno ril_errno;

    err = at_send_command("AT+COPS=0", &p_response, NW_CHANNEL_CTX);

    if (err < 0 || p_response->success == 0)
    {
        ril_errno = RIL_E_GENERIC_FAILURE;
    }
    else
    {
        ril_errno = RIL_E_SUCCESS;
    }
    RIL_onRequestComplete(t, ril_errno, NULL, 0);
    at_response_free(p_response);
}

void requestSetNetworkSelectionManual(void * data, size_t datalen, RIL_Token t)
{
    int err;
    char * cmd, *numeric_code;
    ATResponse *p_response = NULL;
    RIL_Errno ril_errno = RIL_E_GENERIC_FAILURE;
    int len, i;

    numeric_code = (char *)data;
    len = strlen(numeric_code);
    if (datalen == sizeof (char*) &&
            (len == 5 || len == 6)  )
    {
        // check if the numeric code is valid digit or not
        for(i = 0; i < len ; i++)
        {
            if( numeric_code[i] < '0' || numeric_code[i] > '9')
                break;
        }

        if( i == len)
        {

            err = asprintf(&cmd, "AT+COPS=1, 2, \"%s\"", numeric_code);
            if(err >= 0)
            {
                err = at_send_command(cmd, &p_response, NW_CHANNEL_CTX);

                free(cmd);

                if ( !(err < 0 || p_response->success == 0) )
                {
                    ril_errno = RIL_E_SUCCESS;
                }
            }
        }
        else
        {
            LOGE("the numeric code contains invalid digits");
        }
    }
    else
    {
        LOGE("the data length is invalid for Manual Selection");
    }

    RIL_onRequestComplete(t, ril_errno, NULL, 0);
    at_response_free(p_response);
}

void requestQueryAvailableNetworks(void * data, size_t datalen, RIL_Token t)
{
    int err, len, i, j, num;
    ATResponse *p_response = NULL;
    char *line;
    char **response = NULL;
    char *tmp, *block_p = NULL;
    const RILId rid = getRILIdByChannelCtx(NW_CHANNEL_CTX);
    char *lacStr = NULL;

    err = at_send_command_singleline("AT+COPS=?", "+COPS:",&p_response, NW_CHANNEL_CTX);

    if (err < 0 || p_response->success == 0)
    {
        goto error;
    }

    line = p_response->p_intermediates->line;

    // count the number of operator
    len = strlen(line);
    for(i = 0, num = 0; i < len ; i++ )
    {
        // here we assume that there is no nested ()
        if (line[i] == '(')
        {
            num++;
        }
        else if (line[i] == ',' && line[i+1] == ',')
        {
            break;
        }
    }

    // +COPS: (2,"Far EasTone","FET","46601",0),(...),...,,(0, 1, 3),(0-2)

    err = at_tok_start(&line);
    if (err < 0) goto error;

    response = (char **) malloc( sizeof(char*) * num *5); // for string, each one is 25 bytes
    block_p = (char *) malloc(num* sizeof(char)*5*MAX_OPER_NAME_LENGTH);
    lacStr = (char *) malloc(num* sizeof(char)*4+1);
    memset(lacStr,0,num* sizeof(char)*4+1);

    for(i = 0, j=0 ; i < num ; i++, j+=5)
    {
        /* get "(<stat>" */
        err = at_tok_nextstr(&line, &tmp);
        if (err < 0) goto error;

        response[j+0] = &block_p[(j+0)*MAX_OPER_NAME_LENGTH];
        response[j+1] = &block_p[(j+1)*MAX_OPER_NAME_LENGTH];
        response[j+2] = &block_p[(j+2)*MAX_OPER_NAME_LENGTH];
        response[j+3] = &block_p[(j+3)*MAX_OPER_NAME_LENGTH];
        response[j+4] = &block_p[(j+4)*MAX_OPER_NAME_LENGTH];

        switch(tmp[1])
        {
        case '0':
            sprintf(response[j+3], "unknown");
            break;
        case '1':
            sprintf(response[j+3], "available");
            break;
        case '2':
            sprintf(response[j+3], "current");
            break;
        case '3':
            sprintf(response[j+3], "forbidden");
            break;
        default:
            LOGE("The %d-th <stat> is an invalid value!!!  : %d", i, tmp[1]);
            goto error;
        }

        /* skip long name*/
        err = at_tok_nextstr(&line, &tmp);
        if (err < 0) goto error;

        /* skip short name*/
        err = at_tok_nextstr(&line, &tmp);
        if (err < 0) goto error;

        /* get <oper> numeric code*/
        err = at_tok_nextstr(&line, &tmp);
        if (err < 0) goto error;
        sprintf(response[j+2], "%s", tmp);

        len = strlen(response[j+2]);
        if (len == 5 || len == 6)
        {
            err = getOperatorNamesFromNumericCode(
                      response[j+2], response[j+0], response[j+1], MAX_OPER_NAME_LENGTH, rid);
            if(err < 0) goto error;
        }
        else
        {
            LOGE("The length of the numeric code is incorrect");
            goto error;
        }

        // ALPS00353868 START
        /*plmn_list_format.  0: standard +COPS format , 1: standard +COPS format plus <lac> */		        
        if(plmn_list_format[rid] == 1){
            /* get <lac> numeric code*/
            err = at_tok_nextstr(&line, &tmp);
            if (err < 0){
                LOGE("No <lac> in +COPS response");
				goto error;
            }				
            memcpy(&(lacStr[i*4]),tmp,4);         
        }			
        // ALPS00353868 END
		

        /* get <AcT> 0 is "2G", 2 is "3G"*/
        err = at_tok_nextstr(&line, &tmp);
        if (err < 0) goto error;

        switch(tmp[0])
        {
        case '0':
            sprintf(response[j+4], "2G");
            break;
        case '2':
            sprintf(response[j+4], "3G");
            break;
        default:
            LOGE("The %d-th <Act> is an invalid value!!!  : %d", i, tmp[1]);
            goto error;
        }
    }

    // ALPS00353868 START : save <lac1><lac2><lac3>.. in the property
    if(plmn_list_format[rid] == 1){    
        LOGD("Set lacStr %s to property",lacStr);			
        property_set(PROPERTY_GSM_CURRENT_COPS_LAC, lacStr); 
    }	
    // ALPS00353868 END

    RIL_onRequestComplete(t, RIL_E_SUCCESS, response, sizeof(char*)*num*5);
    at_response_free(p_response);
    free(response);
    free(block_p);
    free(lacStr);
    return;
error:
    at_response_free(p_response);
    if(response)
    {
        LOGD("FREE!!");
        free(block_p);
        free(response);
        free(lacStr);		
    }
    LOGE("requestQueryAvailableNetworks must never return error when radio is on");
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
}

void requestBasebandVersion(void * data, size_t datalen, RIL_Token t)
{
    /*ATResponse *p_response = NULL;
    int err, i, len;
    char *line, *ver, null;

    ver = &null;
    ver[0] = '\0';

    err = at_send_command_multiline("AT+CGMR", "+CGMR:",&p_response, NW_CHANNEL_CTX);

    if (err < 0 || p_response->success == 0)
    {
        goto error;
    }
    else if (p_response->p_intermediates != NULL)
    {
        line = p_response->p_intermediates->line;

        err = at_tok_start(&line);
        if(err < 0) goto error;

        //remove the white space from the end
        len = strlen(line);
        while( len > 0 && isspace(line[len-1]) )
            len --;
        line[len] = '\0';

        //remove the white space from the beginning
        while( (*line) != '\0' &&  isspace(*line) )
            line++;

        ver = line;
    }
    else
    {
        // ALPS00295957 : To handle AT+CGMR without +CGMR prefix response
        at_response_free(p_response);
        p_response = NULL;

        LOGE("Retry AT+CGMR without expecting +CGMR prefix");		

        err = at_send_command_raw("AT+CGMR", &p_response, NW_CHANNEL_CTX);
		
        if (err < 0) {        
            LOGE("Retry AT+CGMR ,fail");		
            goto error;
    	}		

        if(p_response->p_intermediates != NULL)
        {
            line = p_response->p_intermediates->line;
        
            LOGD("retry CGMR response = %s", line);		

            //remove the white space from the end
            len = strlen(line);
            while( len > 0 && isspace(line[len-1]) )
                len --;
            line[len] = '\0';

            //remove the white space from the beginning
            while( (*line) != '\0' &&  isspace(*line) )
                line++;

            ver = line;
        }			
    }
    RIL_onRequestComplete(t, RIL_E_SUCCESS, ver, sizeof(char *));
    at_response_free(p_response);
    return;
error:
    at_response_free(p_response);
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);*/

    RILChannelCtx* p_channel = getRILChannelCtxFromToken(t);
    RILId rid = getRILIdByChannelCtx(p_channel);
    if (s_basebandVersion[rid] == NULL)
        RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    else
        RIL_onRequestComplete(t, RIL_E_SUCCESS, s_basebandVersion[rid], sizeof(char *));
}


void requestSetBandMode(void * data, size_t datalen, RIL_Token t)
{
    ATResponse *p_response = NULL;
    int req, err, gsm_band, umts_band;
    char *cmd;
    RIL_Errno ril_errno = RIL_E_GENERIC_FAILURE;

    req = ((int *)data)[0];

    switch(req)
    {
    case BM_AUTO_MODE: //"unspecified" (selected by baseband automatically)
        gsm_band = 0xff;
        umts_band = 0xffff;
        break;
    case BM_EURO_MODE: //"EURO band" (GSM-900 / DCS-1800 / WCDMA-IMT-2000)
        gsm_band = GSM_BAND_900 | GSM_BAND_1800;
        umts_band = UMTS_BAND_I;
        break;
    case BM_US_MODE: //"US band" (GSM-850 / PCS-1900 / WCDMA-850 / WCDMA-PCS-1900)
        gsm_band = GSM_BAND_850 | GSM_BAND_1900;
        umts_band = UMTS_BAND_II | UMTS_BAND_V;
        break;
    case BM_JPN_MODE: //"JPN band" (WCDMA-800 / WCDMA-IMT-2000)
        gsm_band = 0;
        umts_band = UMTS_BAND_I | UMTS_BAND_VI;
        break;
    case BM_AUS_MODE: //"AUS band" (GSM-900 / DCS-1800 / WCDMA-850 / WCDMA-IMT-2000)
        gsm_band = GSM_BAND_900 | GSM_BAND_1800;
        umts_band = UMTS_BAND_I | UMTS_BAND_V;
        break;
    case BM_AUS2_MODE: //"AUS band 2" (GSM-900 / DCS-1800 / WCDMA-850)
        gsm_band = GSM_BAND_900 | GSM_BAND_1800;
        umts_band = UMTS_BAND_V;
        break;
    case BM_CELLULAR_MODE: //"Cellular (800-MHz Band)"
    case BM_PCS_MODE: //"PCS (1900-MHz Band)"
    case BM_CLASS_3: //"Band Class 3 (JTACS Band)"
    case BM_CLASS_4: //"Band Class 4 (Korean PCS Band)"
    case BM_CLASS_5: //"Band Class 5 (450-MHz Band)"
    case BM_CLASS_6: // "Band Class 6 (2-GMHz IMT2000 Band)"
    case BM_CLASS_7: //"Band Class 7 (Upper 700-MHz Band)"
    case BM_CLASS_8: //"Band Class 8 (1800-MHz Band)"
    case BM_CLASS_9: //"Band Class 9 (900-MHz Band)"
    case BM_CLASS_10: //"Band Class 10 (Secondary 800-MHz Band)"
    case BM_CLASS_11: //"Band Class 11 (400-MHz European PAMR Band)"
    case BM_CLASS_15: //"Band Class 15 (AWS Band)"
    case BM_CLASS_16: //"Band Class 16 (US 2.5-GHz Band)"
    default:
        gsm_band = -1;
        umts_band = -1;
        break;
    }

    if (gsm_band != -1 && umts_band != -1)
    {
        /******************************************************
        * If the modem doesn't support certain group of bands, ex. GSM or UMTS
        * It might just ignore the parameter.
        *******************************************************/
        err = asprintf(&cmd, "AT+EPBSE=%d, %d", gsm_band, umts_band);
        if(err >= 0)
        {
            err = at_send_command(cmd, &p_response, NW_CHANNEL_CTX);

            free(cmd);

            if(err >= 0 && p_response->success != 0)
            {
                ril_errno = RIL_E_SUCCESS;
            }
        }
    }

    RIL_onRequestComplete(t, ril_errno, NULL, 0);
    at_response_free(p_response);

    return;

}

void requestQueryAvailableBandMode(void * data, size_t datalen, RIL_Token t)
{
    ATResponse *p_response = NULL;
    int err, gsm_band, umts_band;
    char *cmd, *line;
    int band_mode[10], index=1;

    err = at_send_command_singleline("AT+EPBSE=?", "+EPBSE:", &p_response, NW_CHANNEL_CTX);

    if (err < 0 || p_response->success == 0)
        goto error;


    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);
    if(err < 0) goto error;

    // get supported GSM bands
    err = at_tok_nextint(&line, &gsm_band);
    if(err < 0) goto error;

    // get supported UMTS bands
    err = at_tok_nextint(&line, &umts_band);
    if(err < 0) goto error;

    //0 for "unspecified" (selected by baseband automatically)
    band_mode[index++] = BM_AUTO_MODE;

    if(gsm_band !=0 || umts_band != 0)
    {
        // 1 for "EURO band" (GSM-900 / DCS-1800 / WCDMA-IMT-2000)
        if ((gsm_band == 0 || (gsm_band | GSM_BAND_900 | GSM_BAND_1800)==gsm_band) &&
            (umts_band == 0 || (umts_band | UMTS_BAND_I)==umts_band))
        {
            band_mode[index++] = BM_EURO_MODE;
        }

        // 2 for "US band" (GSM-850 / PCS-1900 / WCDMA-850 / WCDMA-PCS-1900)
        if ( (gsm_band == 0 ||  (gsm_band | GSM_BAND_850 | GSM_BAND_1900)==gsm_band) &&
             (umts_band == 0 ||  (umts_band | UMTS_BAND_II | UMTS_BAND_V)==umts_band) )
        {
            band_mode[index++] = BM_US_MODE;
        }

        // 3 for "JPN band" (WCDMA-800 / WCDMA-IMT-2000)
        if ( (umts_band | UMTS_BAND_I | UMTS_BAND_VI)==umts_band)
        {
            band_mode[index++] = BM_JPN_MODE;
        }

        // 4 for "AUS band" (GSM-900 / DCS-1800 / WCDMA-850 / WCDMA-IMT-2000)
        if ( (gsm_band == 0 ||  (gsm_band | GSM_BAND_900 | GSM_BAND_1800)==gsm_band) &&
                (umts_band == 0 ||  (umts_band | UMTS_BAND_I | UMTS_BAND_V)==umts_band) )
        {
            band_mode[index++] = BM_AUS_MODE;
        }

        // 5 for "AUS band 2" (GSM-900 / DCS-1800 / WCDMA-850)
        if ( (gsm_band == 0 ||  (gsm_band | GSM_BAND_900 | GSM_BAND_1800)==gsm_band) &&
                (umts_band == 0 ||  (umts_band | UMTS_BAND_V)==umts_band) )
        {
            band_mode[index++] = BM_AUS2_MODE;
        }

    }

    band_mode[0] = index;

    RIL_onRequestComplete(t, RIL_E_SUCCESS, band_mode, sizeof(int)*index);
    at_response_free(p_response);
    return;

error:
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);
}

void requestSetPreferredNetworkType(void * data, size_t datalen, RIL_Token t)
{
    ATResponse *p_response = NULL;
    int req_type, err, rat, rat1;
    char *cmd;
    RIL_Errno ril_errno = RIL_E_MODE_NOT_SUPPORTED;

    /*FIXME: **************************************
    * we set all error as RIL_E_MODE_NOT_SUPPORTED
    * Since modem can't tell us if we can support the mode or not.
    * Normally, it should be correct.
    * But it would be better if we can query it or get CME ERROR.
    * Note : Framework might not be ready.
    ****************************************/

    req_type = ((int *)data)[0];
    rat1= 0;
    switch(req_type)
    {
    case NT_NO_PREFERRED_RAT_ONLY_TYPE:
    case NT_WCDMA_PREFERRED_TYPE:		
        rat = 2;    // WCDMA Prefer
        rat1= 2;
        break;
    case NT_GSM_RAT_ONLY_TYPE:
    case NT_GSM_TYPE:
        rat = 0;    // GSM
        rat1= 0;
        break;
    case NT_WCDMA_RAT_ONLY_TYPE:
    case NT_WCDMA_TYPE:
        rat = 1;    // UMTS
        rat1= 0;
        break;
    case NT_AUTO_RAT_ONLY_TYPE:
    case NT_AUTO_TYPE:
        rat = 2;    // GSM_UMTS
        rat1= 0;
        break;
    default:
        rat = -1;
        break;
    }
    if(rat >= 0)
    {
        if(req_type >= NT_RAT_ONLY_TYPE)//ALPS00282643
            err = asprintf(&cmd,  "AT+ERAT=%d", rat);
        else
            err = asprintf(&cmd,  "AT+ERAT=%d,%d", rat, rat1);
        if(err >= 0)
        {
            err = at_send_command(cmd, &p_response, getRILChannelCtxFromToken(t));

            free(cmd);

            if(err >= 0 && p_response->success != 0)
            {
                ril_errno = RIL_E_SUCCESS;
            }
        }
    }

    RIL_onRequestComplete(t, ril_errno, NULL, 0);
    at_response_free(p_response);

}

void requestGetPreferredNetworkType(void * data, size_t datalen, RIL_Token t)
{
    ATResponse *p_response = NULL;
    int err, skip, nt_type , prefer_type, return_type;
    char *cmd, *line;

    err = at_send_command_singleline("AT+ERAT?", "+ERAT:", &p_response, getRILChannelCtxFromToken(t));

    if (err < 0 || p_response->success == 0)
        goto error;

    line = p_response->p_intermediates->line;

		prefer_type=0;
    err = at_tok_start(&line);
    if(err < 0) goto error;

    //skip <curr_rat>
    err = at_tok_nextint(&line, &skip);
    if(err < 0) goto error;

    //skip <gprs_status>
    err = at_tok_nextint(&line, &skip);
    if(err < 0) goto error;

    //get <rat>
    err = at_tok_nextint(&line, &nt_type);
    if(err < 0) goto error;

    err = at_tok_nextint(&line, &prefer_type);
    if(err < 0) goto error;
    
    if (nt_type == 0) {
        return_type = NT_GSM_TYPE;
    } else if(nt_type == 1) {
        return_type = NT_WCDMA_TYPE;
    } else if(nt_type == 2 && prefer_type == 0) {
        return_type = NT_AUTO_TYPE;
    } else if(nt_type == 2 && prefer_type == 1) {
        LOGE("Dual mode but GSM prefer, mount to AUTO mode");
    	return_type = NT_AUTO_TYPE;
    } else if(nt_type == 2 && prefer_type == 2) {
    	return_type = NT_WCDMA_PREFERRED_TYPE;
    } else {
        goto error;
    }

    RIL_onRequestComplete(t, RIL_E_SUCCESS, &return_type, sizeof(int) );
    at_response_free(p_response);
    return;

error:
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);

}

void requestGetNeighboringCellIds(void * data, size_t datalen, RIL_Token t)
{
    ATResponse *p_response = NULL;
    int err, skip, nt_type;
    char *cmd, *line;
    ATLine *p_line;

    int rat,rssi,ci,lac,psc;
    int i = 0;
    int j = 0; 
    RIL_NeighboringCell nbr[6];
    RIL_NeighboringCell *p_nbr[6];

    err = at_send_command_multiline("AT+ENBR", "+ENBR:", &p_response, NW_CHANNEL_CTX);

    if (err < 0 || p_response->success == 0)
        goto error;

    p_line = p_response->p_intermediates;

    while(p_line != NULL)
    {
        line = p_line->line;

        err = at_tok_start(&line);
        if(err < 0) goto error;

        err = at_tok_nextint(&line, &rat);
        if(err < 0) goto error;

        err = at_tok_nextint(&line, &rssi);
        if(err < 0) goto error;

        if(( (rat == 1) && (rssi < 0  || rssi > 31) && (rssi != 99) )
                || ( (rat == 2) && (rssi < 0 || rssi > 91) ))
        {
            LOGE("The rssi of the %d-th is invalid: %d", i, rssi );
            goto error;
        }

        nbr[i].rssi = rssi;

        if (rat == 1)
        {
            err = at_tok_nextint(&line, &ci);
            if(err < 0) goto error;

            err = at_tok_nextint(&line, &lac);
            if(err < 0) goto error;

            err = asprintf(&nbr[i].cid, "%04X%04X", lac, ci);
            if(err < 0)
            {
                LOGE("Using asprintf and getting ERROR");
                goto error;
            }

            //ALPS00269882 : to bring 'rat' info without changing the interface between RILJ (for backward compatibility concern)
            property_set(PROPERTY_GSM_CURRENT_ENBR_RAT, "1"); //NETWORK_TYPE_GPRS = 1
            LOGD("CURRENT_ENBR_RAT:1");
			
            LOGD("NC[%d], rssi:%d, cid:%s", i, nbr[i].rssi, nbr[i].cid);
        }
        else if (rat == 2)
        {
            err = at_tok_nextint(&line, &psc);
            if(err < 0) goto error;

            err = asprintf(&nbr[i].cid, "%08X", psc);
            if(err < 0)
            {
                LOGE("Using asprintf and getting ERROR");
                goto error;
            }

            //ALPS00269882 : to bring 'rat' info without changing the interface between RILJ (for backward compatibility concern)
            property_set(PROPERTY_GSM_CURRENT_ENBR_RAT, "3"); //NETWORK_TYPE_UMTS = 3
            LOGD("CURRENT_ENBR_RAT:3");
			
            LOGD("NC[%d], rssi:%d, psc:%d", i, rssi, psc);
        }
        else
            goto error;

        p_nbr[i] = &nbr[i];
        i++;
        p_line = p_line->p_next;
    }

    RIL_onRequestComplete(t, RIL_E_SUCCESS, p_nbr, sizeof(RIL_NeighboringCell*) * i );
    at_response_free(p_response);
    for(j=0;j<i;j++)
        free(nbr[j].cid);  
    return;

error:
    LOGE("requestGetNeighboringCellIds has error occur!!");
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    at_response_free(p_response);
    for(j=0;j<i;j++)
        free(nbr[j].cid);
}

void requestSetLocationUpdates(void * data, size_t datalen, RIL_Token t)
{
    ATResponse *p_response = NULL;
    int err, enabled;
    RIL_Errno err_no = RIL_E_GENERIC_FAILURE;
    char *cmd;

    if (datalen == sizeof(int*))
    {
        enabled = ((int*)data)[0];

        if (enabled == 1 || enabled == 0)
        {
            err = asprintf(&cmd, "AT+CREG=%d", enabled ? 2 : 1);
            if( err >= 0 )
            {
                err = at_send_command(cmd, &p_response, NW_CHANNEL_CTX);
                free(cmd);

                if ( err >= 0 && p_response->success > 0)
                {
                    err_no = RIL_E_SUCCESS;
                }
            }
        }
    }
    RIL_onRequestComplete(t, err_no, NULL, 0);
    at_response_free(p_response);

}

RIL_RadioState getNewRadioState(RIL_RadioState state)
{
    return (state > RADIO_STATE_UNAVAILABLE? state : RADIO_STATE_SIM_NOT_READY);
}

void requestGetPacketSwitchBearer(RILId rid)
{
    ATResponse *p_response = NULL;
    int err;
    char *line;
    int ret;
    char *bearer = NULL;

    err = at_send_command_singleline("AT+EPSB?", "+EPSB:", &p_response, getChannelCtxbyProxy(rid));

    if (err < 0 || p_response->success == 0) {
        // assume radio is off
        goto error;
    }

    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);
    if (err < 0) goto error;

    err = at_tok_nextint(&line, &ret);
    if (err < 0) goto error;

    asprintf(&bearer, "%d", ret);

    if (isDualTalkMode()) {
        if (rid == MTK_RIL_SOCKET_1) {
            /* ALPS00430285 : For certain dual talk project, modem will run in 2G mode only.  */			
            int telephonyMode = getTelephonyMode();

            #ifdef MTK_RIL_MD1
            if((telephonyMode == 6) || (telephonyMode == 8)){ 
                LOGD("Dual talk MD1 only run in 2G mode only, set capability to 2G");				
                if (bearer != NULL) {
                    free(bearer);
                    bearer = NULL;
                }
                asprintf(&bearer, "%d", 3);
            }
            #endif

            #ifdef MTK_RIL_MD2
            if((telephonyMode == 7) || (telephonyMode == 8)){ 
                LOGD("Dual talk MD2 only run in 2G mode only, set capability to 2G");								
                if (bearer != NULL) {
                    free(bearer);
                    bearer = NULL;
                }
                asprintf(&bearer, "%d", 3);
            }
            #endif
            /* ALPS00430285 END */			
		
            upadteSystemPropertyByCurrentMode(rid, PROPERTY_GSM_BASEBAND_CAPABILITY, PROPERTY_GSM_BASEBAND_CAPABILITY2, bearer);
        }
    } else {
        /* For W/G GEMINI modem , SIM1 will return +EPSB capability WCDMA. SIM2 will return capability is GSM only */    
        upadteSystemPropertyByCurrentMode(rid, PROPERTY_GSM_BASEBAND_CAPABILITY, PROPERTY_GSM_BASEBAND_CAPABILITY2, bearer);
    }
    
    free(bearer);

    LOGD("[RIL%d] AT+EPSB return %d", rid+1, ret);
    at_response_free(p_response);

    return;

error:
    LOGE("[RIL%d] AT+EPSB return ERROR", rid+1);
    at_response_free(p_response);
}

void requestSN(RILId rid) {
    ATResponse *p_response = NULL;
    int err;
    char *line, *sv;
    // type 5: Serial Number

    err = at_send_command_singleline("AT+EGMR=0,5" , "+EGMR:",&p_response, getChannelCtxbyProxy(rid));
	if (err < 0 || p_response->success == 0) {
        goto error;
    }

    line = p_response->p_intermediates->line;

    err = at_tok_start(&line);
    if(err < 0) goto error;

    err = at_tok_nextstr(&line, &sv);
    if(err < 0) goto error;

    property_set(PROPERTY_SERIAL_NUMBER, sv);
    LOGD("[RIL%d] Get serial number: %s", rid+1, sv);

    at_response_free(p_response);
    return;
error:
    at_response_free(p_response);
}

/* Please refer to the setRadioState*/
void onRadioState(char* t, const RILId rid)
{}

void onNetworkStateChanged(char *urc, const RILId rid)
{
    int err, is_cs, is_r8 = 0;
    int stat;
    unsigned int response[4] = {0};
    char* responseStr[5];
    int i = 0;

    if (strStartsWith(urc,"+CREG:")) {
        is_cs = 1;
        is_r8 = 0;
    } else if (strStartsWith(urc,"+CGREG:")) {
        is_cs = 0;
        is_r8 = 0;
    } else if (strStartsWith(urc,"+PSBEARER:")) {
        is_cs = 0;
        is_r8 = 1;
    } else {
        LOGE("The URC is wrong: %s", urc);
        return;
    }

    err = at_tok_start(&urc);
    if (err < 0) goto error;

    err = at_tok_nextint(&urc, &stat);
    if (err < 0) goto error;
    LOGD("The URC is %s", urc);
    //CGREG:4 means the GPRS is detached
    if (is_r8 != 1 && is_cs == 0 && stat == 4) {
        RIL_onUnsolicitedResponse (RIL_UNSOL_GPRS_DETACH, NULL, 0, rid);
    }

    LOGD("onNetworkStateChanged() , stat=%d , is_cs=%d",stat,is_cs);

    //ALPS00283717
    if(is_cs == 1)
    {    
        /* Note: There might not be full +CREG URC info when screen off     
                   Full URC format: +CREG: <n>, <stat>, <lac>, <cid>, <Act>,<cause> */
		
        // set "-1" to indicate "the field is not present"
        for(i=1;i<5;i++)
            asprintf(&responseStr[i], "-1");

        // fill in <state>
        asprintf(&responseStr[0], "%d", stat);

		//get <lac>
        err = at_tok_nexthexint(&urc,(int*) &response[0]);
        if(err >= 0)
        {
            free(responseStr[1]);        
            asprintf(&responseStr[1], "%x", response[0]);
        
            //get <ci>
            err = at_tok_nexthexint(&urc, (int*) &response[1]);
            if(err >= 0)
            {   
                LOGD("+creg urc <ci>: %d", response[1]);
                free(responseStr[2]);				
                asprintf(&responseStr[2], "%x", response[1]);         
				
                //get <Act>
                err = at_tok_nextint(&urc, (int*) &response[2]);
                if(err >= 0)
                {
                    free(responseStr[3]);                
                    asprintf(&responseStr[3], "%d", response[2]);                         
					
                    //get <cause>
                    err = at_tok_nextint(&urc,(int*) &response[3]);
                    if(err >= 0)
                    {
                        LOGD("now +creg urc <cause>: %d", response[3]);                    
                        free(responseStr[4]);						
                        asprintf(&responseStr[4], "%d", response[3]);                             						
                    }					
                }
            }						
        }
    }	

    /* 
    if (stat == 1 || stat == 5)
    {
        if (is_cs)
            cleanCurrentRestrictionState(RIL_NW_ALL_CS_RESTRICTIONS, rid);
        else
            cleanCurrentRestrictionState(RIL_RESTRICTED_STATE_PS_ALL, rid);
    }
    else
    {
        if (is_cs)
        {
            setCurrentRestrictionState(RIL_RESTRICTED_STATE_CS_NORMAL, rid);
        }
        else
        {
            setCurrentRestrictionState(RIL_RESTRICTED_STATE_PS_ALL, rid);
        }
    }
    */
    if (is_cs == 1) 
    {
        RIL_onUnsolicitedResponse (RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED, responseStr,  sizeof(responseStr), rid);
        for(i=0;i<5;i++)
            free(responseStr[i]);
    } else {
        RIL_onUnsolicitedResponse(RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED, NULL, 0, rid);
    }

   // onRestrictedStateChanged(rid);

    return;

error:
    LOGE("There is something wrong with the URC");
}

void onNitzTimeReceived(char* urc, const RILId rid)
{
    int err;
    int length, i, id;
    char nitz_string[30];
    char *line;
    /*+CIEV: 9, <time>, <tz>[, <dt>]
    <time>: "yy/mm/dd, hh:mm:ss"
    <tz>: [+,-]<value>, <value> is quarters of an hour between local time and GMT
    <dt>: day_light_saving value*/

    /* Final format :  "yy/mm/dd,hh:mm:ss(+/-)tz[,dt]" */

    err = at_tok_start(&urc);
    if (err < 0) return;

    err = at_tok_nextint(&urc, &id);
    if (err < 0 || id != 9) return;

    if (!at_tok_hasmore(&urc))
    {
        LOGE("There is no NITZ data");
        return;
    }

    length = strlen(urc);
    if (length >= (int) sizeof(nitz_string))
    {
        LOGE("The length of the NITZ URC is too long: %d", length);
        return;
    }
    nitz_string[0] = '\0';

    // eliminate the white space first
    line = urc;
    for (i = 0; i<length ; i++)
    {
        if(urc[i] != ' ')
        {
            *(line++) = urc[i];
        }
    }
    *line = '\0';

    length = strlen(urc);
    if(length == 0)
    {
        LOGE("Receiving an empty NITZ");
        return;
    }

    // get <time>
    err = at_tok_nextstr(&urc, &line);
    if (err < 0)
    {
        LOGE("ERROR occurs when parsing <time> of the NITZ URC");
        return;
    }
    strcat(nitz_string, line);

    // concatenate the remaining with <time>
    if (strlen(urc) > 0) {
        strcat(nitz_string, urc);
    } else {
        LOGE("There is no remaining data, said <tz>[,<dt>]");
        return;
    }

    RIL_onUnsolicitedResponse (RIL_UNSOL_NITZ_TIME_RECEIVED, nitz_string, sizeof(char *), rid);
}

/* Add-BY-JUNGO-20101008-CTZV SUPPORT*/
void onNitzTzReceived(char *urc, const RILId rid)
{
    int err, i = 0, length = 0;
    int response[2];
    char nitz_string[30];
    time_t calendar_time;
    struct tm *t_info = NULL;
    char *line = NULL;
    /* Final format :  "yy/mm/dd,hh:mm:ss(+/-)tz[,dt]" */

    err = at_tok_start(&urc);
    if(err < 0) return;

    if(strlen(urc) > 0)
    {

        length = strlen(urc);
        // eliminate the white space first
        line = urc;
        for (i = 0; i<length ; i++)
        {
            if(urc[i] != ' ')
            {
                *(line++) = urc[i];
            }
        }
        *line = '\0';

        //get the system time to fullfit the NITZ string format
        calendar_time = time(NULL);
        if(-1 == calendar_time) return;

        t_info = gmtime(&calendar_time);
        if(NULL == t_info) return;

        memset(nitz_string, 0, sizeof(nitz_string));
        sprintf(nitz_string, "%02d/%02d/%02d,%02d:%02d:%02d%s",  //"yy/mm/dd,hh:mm:ss"
                (t_info->tm_year)%100,
                t_info->tm_mon+1,
                t_info->tm_mday,
                t_info->tm_hour,
                t_info->tm_min,
                t_info->tm_sec,
                urc);

        LOGD("NITZ:%s", nitz_string);

        RIL_onUnsolicitedResponse (RIL_UNSOL_NITZ_TIME_RECEIVED, nitz_string, sizeof(char *), rid);
    }
    else
    {
        LOGE("There is no remaining data, said <tz>[,<dt>]");
        return;
    }
}

void onNitzOperNameReceived(char* urc, const RILId rid)
{
    int err;
    int length, i, id;
    char nitz_string[101];
    char *line;
    char *oper_code;
    char *oper_lname;
    char *oper_sname;
    int is_lname_hex_str=0;
    int is_sname_hex_str=0;	
    char temp_oper_name[MAX_OPER_NAME_LENGTH]={0};    

    /* +CIEV: 10,"PLMN","long_name","short_name" */

    err = at_tok_start(&urc);
    if (err < 0) return;

    err = at_tok_nextint(&urc, &id);
    if (err < 0 || id != 10) return;

    if (!at_tok_hasmore(&urc))
    {
        LOGE("There is no NITZ data");
        return;
    }

    oper_code   = ril_nw_nitz_oper_code[rid];
    oper_lname  = ril_nw_nitz_oper_lname[rid];
    oper_sname  = ril_nw_nitz_oper_sname[rid];

    /* FIXME: it is more good to transfer the OPERATOR NAME to the Telephony Framework directly */

    pthread_mutex_lock(&ril_nw_nitzName_mutex[rid]);
    LOGD("Get ril_nw_nitzName_mutex in the onNitzOperNameReceived");

    err = at_tok_nextstr(&urc, &line);
    if (err < 0) goto error;
    strncpy(oper_code, line, MAX_OPER_NAME_LENGTH);
    oper_code[MAX_OPER_NAME_LENGTH-1] = '\0';

    err = at_tok_nextstr(&urc, &line);
    if (err < 0) goto error;
    strncpy(oper_lname, line, MAX_OPER_NAME_LENGTH);
    oper_lname[MAX_OPER_NAME_LENGTH-1] = '\0';

    err = at_tok_nextstr(&urc, &line);
    if (err < 0) goto error;
    strncpy(oper_sname, line, MAX_OPER_NAME_LENGTH);
    oper_sname[MAX_OPER_NAME_LENGTH-1] = '\0';

    /* ALPS00262905 start 
       +CIEV: 10, <plmn_str>,<full_name_str>,<short_name_str>,<is_full_name_hex_str>,<is_short_name_hex_str> for UCS2 string */
    err = at_tok_nextint(&urc, &is_lname_hex_str);
    if (err >= 0)
    {
        LOGD("is_lname_hex_str=%d",is_lname_hex_str);    
		
        if (is_lname_hex_str == 1)
        {    
            /* ALPS00273663 Add specific prefix "uCs2" to identify this operator name is UCS2 format.  prefix + hex string ex: "uCs2806F767C79D1"  */        
            memset(temp_oper_name, 0, sizeof(temp_oper_name));   
            strncpy(temp_oper_name, "uCs2", 4);        		
            strncpy(&(temp_oper_name[4]), oper_lname, MAX_OPER_NAME_LENGTH-4);        					
            memset(oper_lname, 0, MAX_OPER_NAME_LENGTH);   		
            strncpy(oper_lname, temp_oper_name, MAX_OPER_NAME_LENGTH);        		
            LOGD("lname add prefix uCs2");    				
        }
		
        err = at_tok_nextint(&urc, &is_sname_hex_str);

        LOGD("is_sname_hex_str=%d",is_sname_hex_str);    
		
        if ((err >= 0) && (is_sname_hex_str == 1))
        {
            /* ALPS00273663 Add specific prefix "uCs2" to identify this operator name is UCS2 format.  prefix + hex string ex: "uCs2806F767C79D1"  */        
            memset(temp_oper_name, 0, sizeof(temp_oper_name));   
            strncpy(temp_oper_name, "uCs2", 4);        		
            strncpy(&(temp_oper_name[4]), oper_sname, MAX_OPER_NAME_LENGTH-4);        					
            memset(oper_sname, 0, MAX_OPER_NAME_LENGTH);   		
            strncpy(oper_sname, temp_oper_name, MAX_OPER_NAME_LENGTH);        		
            LOGD("sname Add prefix uCs2");    				
        }		
    }		
    /* ALPS00262905 end */

    LOGD("Get NITZ Operator Name of RIL %d: %s %s %s", rid+1, oper_code, oper_lname, oper_sname);

    if (getMappingSIMByCurrentMode(rid) == GEMINI_SIM_1) {
        property_set(PROPERTY_NITZ_OPER_CODE,   oper_code);
        property_set(PROPERTY_NITZ_OPER_LNAME,  oper_lname);
        property_set(PROPERTY_NITZ_OPER_SNAME,  oper_sname);
#if (MTK_GEMINI_SIM_NUM >= 4)		
    } else if(rid == MTK_RIL_SOCKET_4){
        property_set(PROPERTY_NITZ_OPER_CODE4,   oper_code);
        property_set(PROPERTY_NITZ_OPER_LNAME4,  oper_lname);
        property_set(PROPERTY_NITZ_OPER_SNAME4,  oper_sname);		
#endif		
#if (MTK_GEMINI_SIM_NUM >= 3)		
    } else if(rid == MTK_RIL_SOCKET_3){
        property_set(PROPERTY_NITZ_OPER_CODE3,   oper_code);
        property_set(PROPERTY_NITZ_OPER_LNAME3,  oper_lname);
        property_set(PROPERTY_NITZ_OPER_SNAME3,  oper_sname);				
#endif		
    } else {
        property_set(PROPERTY_NITZ_OPER_CODE2,  oper_code);
        property_set(PROPERTY_NITZ_OPER_LNAME2, oper_lname);
        property_set(PROPERTY_NITZ_OPER_SNAME2, oper_sname);
    }

    if (RADIO_STATE_SIM_READY == getRadioState(rid)) {
        RIL_onUnsolicitedResponse (RIL_UNSOL_RESPONSE_VOICE_NETWORK_STATE_CHANGED, NULL, 0, rid);
    }

error:
    pthread_mutex_unlock(&ril_nw_nitzName_mutex[rid]);
    return;
}

void onRestrictedStateChanged(const RILId rid)
{
    int state[1];
    int simId3G = RIL_get3GSIM()-1;

    state[0] = ril_nw_cur_state[rid] & RIL_NW_ALL_RESTRICTIONS;
    LOGD("RestrictedStateChanged: %x", state[0]);

    if (RIL_is3GSwitched() && ((rid == (MTK_RIL_SOCKET_1 +simId3G)) ||(rid == MTK_RIL_SOCKET_1))) {		
        if (((rid == (MTK_RIL_SOCKET_1 +simId3G)) && (sim_inserted_status & SIM1_INSERTED )) ||
            ((rid == MTK_RIL_SOCKET_1) && (sim_inserted_status & (SIM1_INSERTED <<simId3G))))
        {
            RIL_onUnsolicitedResponse (RIL_UNSOL_RESTRICTED_STATE_CHANGED, state, sizeof(state), rid);
            return;
        }
    } else {
        if ((rid == MTK_RIL_SOCKET_1 && (sim_inserted_status & SIM1_INSERTED))
#ifdef MTK_GEMINI
            || (rid == MTK_RIL_SOCKET_2 && (sim_inserted_status & SIM2_INSERTED))
#if (MTK_GEMINI_SIM_NUM >= 3)
            || (rid == MTK_RIL_SOCKET_3 && (sim_inserted_status & SIM3_INSERTED))
#endif
#if (MTK_GEMINI_SIM_NUM >= 4)
            || (rid == MTK_RIL_SOCKET_4 && (sim_inserted_status & SIM4_INSERTED))
#endif
#endif
            ) 
        {
            RIL_onUnsolicitedResponse (RIL_UNSOL_RESTRICTED_STATE_CHANGED, state, sizeof(state), rid);
        }
    }
}


#ifdef MTK_GEMINI
int isSimInserted(RILId rid) {
    int simId3G = RIL_get3GSIM();
    int pivot = 1;
    int pivotSim = rid;
    int simInserted = 0;

    LOGD("isSimInserted(): simId3G = %d, rid: %d, sim_inserted_status: %d", simId3G, rid, sim_inserted_status);
    
    // 3G switch, 1 -> N, N->1
    if(simId3G != CAPABILITY_3G_SIM1) {
        if (rid == MTK_RIL_SOCKET_1) {
            pivotSim = pivot << (simId3G-1);
        } else if (rid == (simId3G-1)) {
            pivotSim = pivot;
        } else {
            pivotSim = pivot << rid; 
        }
        if ((sim_inserted_status & pivotSim) == pivotSim) {
                simInserted = 1;
            }
        } else {
        pivotSim = pivot << rid;

        LOGD("isSimInserted(): pivotSim = %d ,%d", pivotSim ,(sim_inserted_status & pivotSim));
	
        if ((sim_inserted_status & pivotSim) == pivotSim ) {
                simInserted = 1;
            }
        }

    LOGD("isSimInserted(): simInserted = %d", simInserted);

    return simInserted;
}
#endif

extern int rilNwMain(int request, void *data, size_t datalen, RIL_Token t)
{
    switch (request)
    {
    case RIL_REQUEST_SIGNAL_STRENGTH:
        requestSignalStrength(data, datalen, t);
        break;
    case RIL_REQUEST_VOICE_REGISTRATION_STATE:
        requestRegistrationState(data, datalen, t);
        break;
    case RIL_REQUEST_DATA_REGISTRATION_STATE:
        requestGprsRegistrationState(data, datalen, t);
        break;
    case RIL_REQUEST_OPERATOR:
        requestOperator(data, datalen, t);
        break;
    case RIL_REQUEST_RADIO_POWER:
        requestRadioPower(data, datalen, t);
        break;
    case RIL_REQUEST_RADIO_POWEROFF:
        requestRadioPowerOff(data, datalen, t);
        break;
    case RIL_REQUEST_RADIO_POWERON:
        requestRadioPowerOn(data, datalen, t);
        break;
    case RIL_REQUEST_GET_IMEI:
        requestGetImei(data, datalen, t);
        break;
    case RIL_REQUEST_GET_IMEISV:
        requestGetImeisv(data, datalen, t);
        break;
    case RIL_REQUEST_SET_NETWORK_SELECTION_AUTOMATIC:
        requestSetNetworkSelectionAutomatic(data, datalen, t);
        break;
    case RIL_REQUEST_QUERY_NETWORK_SELECTION_MODE:
        requestQueryNetworkSelectionMode(data, datalen, t);
        break;
    case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL:
        requestSetNetworkSelectionManual(data, datalen, t);
        break;
    case RIL_REQUEST_QUERY_AVAILABLE_NETWORKS:
        requestQueryAvailableNetworks(data, datalen, t);
        break;
    case RIL_REQUEST_BASEBAND_VERSION:
        requestBasebandVersion(data, datalen, t);
        break;
    case RIL_REQUEST_SET_BAND_MODE:
        requestSetBandMode(data, datalen, t);
        break;
    case RIL_REQUEST_QUERY_AVAILABLE_BAND_MODE:
        requestQueryAvailableBandMode(data, datalen, t);
        break;
    case RIL_REQUEST_SET_PREFERRED_NETWORK_TYPE:
        requestSetPreferredNetworkType(data, datalen, t);
        break;
    case RIL_REQUEST_GET_PREFERRED_NETWORK_TYPE:
        requestGetPreferredNetworkType(data, datalen, t);
        break;
    case RIL_REQUEST_GET_NEIGHBORING_CELL_IDS:
        requestGetNeighboringCellIds(data, datalen, t);
        break;
    case RIL_REQUEST_SET_LOCATION_UPDATES:
        requestSetLocationUpdates(data, datalen, t);
        break;
    case RIL_REQUEST_SET_GPRS_CONNECT_TYPE:
        requestSetGprsConnectType(data, datalen, t);
        break;
    case RIL_REQUEST_SET_GPRS_TRANSFER_TYPE:
        requestSetGprsTransferType(data, datalen, t);
        break;
#ifdef  MTK_GEMINI
    case RIL_REQUEST_DUAL_SIM_MODE_SWITCH:
        requestRadioMode(data, datalen, t);
        break;
#endif  /* MTK_GEMINI */
    case RIL_REQUEST_MOBILEREVISION_AND_IMEI: //Add by mtk80372 for Barcode Number
        requestMobileRevisionAndIMEI(data,datalen,t);
        break;
    case RIL_REQUEST_SET_NETWORK_SELECTION_MANUAL_WITH_ACT:
        requestSetNetworkSelectionManualWithAct(data, datalen, t);
        break;
	case RIL_REQUEST_SET_SIM_RECOVERY_ON:
        requestSetSimRecoveryOn(data, datalen, t);
        break;
	case RIL_REQUEST_GET_SIM_RECOVERY_ON:
        requestGetSimRecoveryOn(data, datalen, t);
        break;
    //ALPS00248788 START
    case RIL_REQUEST_SET_TRM:
        LOGD("handle RIL_REQUEST_SET_TRM");	  	
        requestSetTRM(data, datalen, t);
        break;
    // ALPS00248788 END	
//MTK-START [mtk80950][120410][ALPS00266631]check whether download calibration data or not
    case RIL_REQUEST_GET_CALIBRATION_DATA:
        requestGetCalibrationData(data, datalen, t);
        break;
//MTK-END [mtk80950][120410][ALPS00266631]check whether download calibration data or not
    case RIL_REQUEST_VOICE_RADIO_TECH:
        requestVoiceRadioTech(data, datalen, t);
        break;
    default:
        return 0; /* no matched requests */
        break;
    }

    return 1; /* request find and handled */
}

int rilNwUnsolicited(const char *s, const char *sms_pdu, RILChannelCtx* p_channel)
{
    RILId rid = getRILIdByChannelCtx(p_channel);

	LOGD("Nw URC:%s", s);

    if (strStartsWith(s, "+CREG:") || strStartsWith(s, "+CGREG:") || strStartsWith(s, "+PSBEARER:"))
    {
        onNetworkStateChanged((char*) s,rid);
        return 1;
    }
    else if (strStartsWith(s, "+ECSQ:"))
    {
        onSignalStrenth((char*) s,rid);
        return 1;
    }
    else if (strStartsWith(s, "+ECELLINFO:"))
    {
        onNeighboringCellInfo((char*) s,rid);
        return 1;
    }
    else if (strStartsWith(s, "+ENWINFO:"))
    {
        onNetworkInfo((char*) s,rid);
        return 1;
    }
    else if (strStartsWith(s, "+CIEV: 9"))
    {
        onNitzTimeReceived((char*) s,rid);
        return 1;
    }
    else if (strStartsWith(s, "+CIEV: 10"))
    {
        onNitzOperNameReceived((char*) s,rid);
        return 1;
    }
    else if (strStartsWith(s, "+CTZV:")) //Add by Jugno 20101008
    {
        onNitzTzReceived((char*) s, rid);
        return 1;
    }
	else if (strStartsWith(s, "+ESIMS:"))
	{
		onSimInsertChanged(s,rid);
        return 1;
	}
    else if (strStartsWith(s, "+EIND: 16")) //ALPS00248788
    {
        onInvalidSimInfo((char*) s, rid);
        return 1;		
    }
    else if (strStartsWith(s, "+EACMT:"))
    {
        onACMT((char*) s,rid);
        return 1;
    }
    else if (strStartsWith(s, "+EMMRRS:")) //ALPS00368272
    {
        onMMRRStatusChanged((char*) s, rid);
        return 1;		
    }
    
    return 0;
}

