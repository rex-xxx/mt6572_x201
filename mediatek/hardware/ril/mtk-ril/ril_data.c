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

/* //hardware/ril/reference-ril/ril_data.c
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
#include <ril_callbacks.h>
#include <utils/Log.h>

#ifdef __CCMNI_SUPPORT__
#include <cutils/properties.h>
#include <netdb.h>
#include <sys/param.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <linux/if.h>
#include <linux/sockios.h>
#include <linux/route.h>
#endif

#define TC 0
/* pathname returned from RIL_REQUEST_SETUP_DATA_CALL (RIL_REQUEST_SETUP_DEFAULT_PDP) */
#define PPP_TTY_PATH "/dev/omap_csmi_tty1"

#define PROPERTY_RIL_SPECIFIC_SM_CAUSE    "ril.specific.sm_cause"

#define DATA_CHANNEL_CTX getRILChannelCtxFromToken(t)

static RILId s_data_ril_cntx[] = {
    MTK_RIL_SOCKET_1
#ifdef MTK_GEMINI
    , MTK_RIL_SOCKET_2
#if (MTK_GEMINI_SIM_NUM >= 3) /* Gemini plus 3 SIM*/
    , MTK_RIL_SOCKET_3
#endif
#if (MTK_GEMINI_SIM_NUM >= 4) /* Gemini plus 4 SIM*/
    , MTK_RIL_SOCKET_4
#endif
#endif
};

static RILChannelId sCmdChannel4Id[] = {
    RIL_CMD_4
#ifdef MTK_GEMINI
    , RIL_CMD2_4
#if (MTK_GEMINI_SIM_NUM >= 3) /* Gemini plus 3 SIM*/
    , RIL_CMD3_4
#endif
#if (MTK_GEMINI_SIM_NUM >= 4) /* Gemini plus 4 SIM*/
    , RIL_CMD4_4
#endif
#endif
};

#ifdef __CCMNI_SUPPORT__
int sock_fd[MAX_PDP_NUM*MAX_CID_NUM] = {0};
int sock6_fd[MAX_PDP_NUM*MAX_CID_NUM] = {0};
#endif

#ifdef __DELETE_CCMNI_INFO_IN_RIL__
/* Refer to system/core/libnetutils/ifc_utils.c */
extern int ifc_disable(const char *ifname);
extern int ifc_remove_default_route(const char *ifname);
extern int ifc_reset_connections(const char *ifname);
#endif /* __DELETE_CCMNI_INFO_IN_RIL__ */

//Fucntion prototype
void ril_data_ioctl_init(int index);
void ril_data_setflags(int s, struct ifreq *ifr, int set, int clr);
void ril_data_setaddr(int s, struct ifreq *ifr, const char *addr);

int receivedSCRI_RAU = 0;
int sendSCRI_RAU = 0;

#ifdef MTK_IPV6_SUPPORT
extern int current_cid;
extern int cid_status[MAX_PDP_NUM * MAX_CID_NUM];
#endif


//Global variables/strcuture
static int disableFlag = 1;
int gprs_failure_cause = 0;

extern int gcf_test_mode;
extern int s_md_off;

int g_data_call_list[MAX_PDP_NUM*MAX_CID_NUM] = {-1, -1, -1, -1, -1, -1};
int g_pdp_ccmni_list[MAX_PDP_NUM*MAX_CID_NUM] = {-1, -1, -1, -1, -1, -1}; 

char* g_public_dns_server[] = {
    "8.8.8.8",           //Google
    "204.117.214.10",    //SprintLink
    "199.166.31.3",      //QUASAR
    "66.93.87.2",        //SpeakEasy 
    "8.8.4.4"            //Google
};

char g_last_dns_server[4][20] = {0};//[SIM1 last1, SIM1 last2, SIM2 last1, SIM2 last2]


int getAuthType(const char* authType) {
    int request_authType = AUTHTYPE_NOT_SET;
    int req_authType = atoi(authType);

    LOGD("requestSetupDataCall(): request_authType is %d ", req_authType);

    //Application 0->none ; 1->PAP;    2->CHAP; 3->PAP/CHAP;
    //Modem       0->PAP;    1->CHAP; 2->NONE; 3->PAP/CHAP;
    switch(req_authType)
    {
    case 0:
        request_authType = AUTHTYPE_NONE;
        break;
    case 1:
        request_authType = AUTHTYPE_PAP;
        break;
    case 2:
        request_authType = AUTHTYPE_CHAP;
        break;
    case 3:
        request_authType = AUTHTYPE_PAP_CHAP;
        break;
    default:
        request_authType = AUTHTYPE_NOT_SET;
        break;
    }

    return request_authType;
}

int disableIpv6Interface(char* filepath) {

    int fd = open(filepath, O_WRONLY);

    if(fd < 0) {
        LOGE("failed to open file (%s)", strerror(errno));
        return -1;
    }

    if(write(fd, "1", 1) != 1) {
        LOGE("failed to write property file (%s)",strerror(errno));
        close(fd);
        return -1;
    }

    close(fd);
    return 0;
}

#ifdef __ADD_CCMNI_INFO_IN_RIL__
void ril_data_ioctl_init(int index)
{
           
#ifdef MTK_IPV6_SUPPORT
    disableFlag = 0;
#endif

    if(disableFlag || (gcf_test_mode > 0 )) {
        LOGD("Disable IPv6 inteface for CCNMI");
#ifdef MTK_RIL_MD1
        disableIpv6Interface("/proc/sys/net/ipv6/conf/ccmni0/disable_ipv6");
        disableIpv6Interface("/proc/sys/net/ipv6/conf/ccmni1/disable_ipv6");
        disableIpv6Interface("/proc/sys/net/ipv6/conf/ccmni2/disable_ipv6");
#else
        disableIpv6Interface("/proc/sys/net/ipv6/conf/cc2mni0/disable_ipv6");
        disableIpv6Interface("/proc/sys/net/ipv6/conf/cc2mni1/disable_ipv6");
        disableIpv6Interface("/proc/sys/net/ipv6/conf/cc2mni2/disable_ipv6");
#endif
    }
    
        sock_fd[index] = socket(AF_INET, SOCK_DGRAM, 0);
        if (sock_fd[index] < 0)
            LOGD("Couldn't create IP socket: errno=%d", errno);
        else
            LOGD("Allocate sock_fd=%d, for cid=%d", sock_fd[index], index+1);

#ifdef INET6
        sock6_fd[index] = socket(AF_INET6, SOCK_DGRAM, 0);
        if (sock6_fd[index] < 0) {
            sock6_fd[index] = -errno;	/* save errno for later */
            LOGD("Couldn't create IPv6 socket: errno=%d", errno);
        } else {
            LOGD("Allocate sock6_fd=%d, for cid=%d", sock6_fd[index], index+1);
        }
#endif

}

/* For setting IFF_UP: ril_data_setflags(s, &ifr, IFF_UP, 0) */
/* For setting IFF_DOWN: ril_data_setflags(s, &ifr, 0, IFF_UP) */
void ril_data_setflags(int s, struct ifreq *ifr, int set, int clr)
{
    if(ioctl(s, SIOCGIFFLAGS, ifr) < 0)
        goto terminate;
    ifr->ifr_flags = (ifr->ifr_flags & (~clr)) | set;
    if(ioctl(s, SIOCSIFFLAGS, ifr) < 0)
        goto terminate;
    return;
terminate:
    LOGD("Set SIOCSIFFLAGS Error!");
    return;
}


inline void ril_data_init_sockaddr_in(struct sockaddr_in *sin, const char *addr)
{
    sin->sin_family = AF_INET;
    sin->sin_port = 0;
    sin->sin_addr.s_addr = inet_addr(addr);
}

void ril_data_setaddr(int s, struct ifreq *ifr, const char *addr)
{
    LOGD("Configure IPv4 adress :%s", addr);
    ril_data_init_sockaddr_in((struct sockaddr_in *) &ifr->ifr_addr, addr);
    if(ioctl(s, SIOCSIFADDR, ifr) < 0)
        LOGD("Set SIOCSIFADDR Error");
}

static inline int set_address(const char *address, struct sockaddr *sa) {
    return inet_aton(address, &((struct sockaddr_in *)sa)->sin_addr);
}


void ril_data_setdefault_route(int s, char* ifname, char* ipaddr)
{
    struct rtentry rt = {
        .rt_dst = {.sa_family = AF_INET},
        .rt_genmask = {.sa_family = AF_INET},
        .rt_gateway = {.sa_family = AF_INET},
    };
    rt.rt_flags = RTF_UP | RTF_GATEWAY;
    rt.rt_dev = ifname;
    ril_data_init_sockaddr_in((struct sockaddr_in *)&rt.rt_gateway, ipaddr);
    if(ioctl(s, SIOCADDRT, &rt) < 0)
    {
        LOGD("Set SIOCADDRT Error");
    }
}
#if 0
extern int ifc_get_default_route(const char *ifname);
int ril_data_default_gw_exist_check()
{
    int nw_default_gw_check[MAX_PDP_NUM] = {0};
    nw_default_gw_check[0] = ifc_get_default_route("ccmni0");
    nw_default_gw_check[1] = ifc_get_default_route("ccmni1");
    nw_default_gw_check[2] = ifc_get_default_route("ccmni2");
    LOGD("Default GW check: nw_0=%d,nw_1=%d,nw_2=%d",nw_default_gw_check[0],nw_default_gw_check[1],nw_default_gw_check[2]);
    if (nw_default_gw_check[0] != 0 || nw_default_gw_check[1] != 0 || nw_default_gw_check[2] != 0)
    {
        /* It means that one of these network drivers is the default gw */
        /* Return value from the API ifc_get_default_route() is the gw address */
        LOGD("Default GW already exists!");
        return 1;
    }
    else
    {
        LOGD("Default GW not exists!");
        return 0;
    }
}
#endif
#endif /* __ADD_CCMNI_INFO_IN_RIL__ */

/* Change name from requestOrSendPDPContextList to requestOrSendDataCallList */
static void requestOrSendDataCallList(RIL_Token *t, RILId rilid)
{
    ATResponse *p_response;
    ATLine *p_cur;
    int err;
    int n = 0;
    char *out;
    /* Because the RIL_Token* t may be NULL passed due to receive URC: Only t is NULL, 2nd parameter rilid is used */

    RILChannelCtx* rilchnlctx = NULL;
    if (t != NULL)
    {
        rilchnlctx = getRILChannelCtxFromToken(*t);
    }
    else
    {
        rilchnlctx = getChannelCtxbyProxy(rilid);
    }

    if (!isEmulatorRunning()) {
        #ifdef MTK_IPV6_SUPPORT
            requestOrSendDataCallListIpv6(rilchnlctx, t, rilid);
            return;
        #endif
    } else {
        LOGD("request data connection list on EMU, not to use IPv6 handler");
    }

    err = at_send_command_multiline ("AT+CGACT?", "+CGACT:", &p_response, rilchnlctx);
    if (err != 0 || p_response->success == 0) {
        if (t != NULL)
            RIL_onRequestComplete(*t, RIL_E_GENERIC_FAILURE, NULL, 0);
        else
            RIL_onUnsolicitedResponse(RIL_UNSOL_DATA_CALL_LIST_CHANGED,
                                      NULL, 0, rilid);
        return;
    }

    for (p_cur = p_response->p_intermediates; p_cur != NULL;
            p_cur = p_cur->p_next)
        n++;

    LOGD("RIL_Data_Call total: %d", n);

    RIL_Data_Call_Response_v6* responses_v = alloca(n * sizeof(RIL_Data_Call_Response_v6));
    int i;
    for (i = 0; i < n; i++) {
        responses_v[i].status = PDP_FAIL_NONE;
        responses_v[i].suggestedRetryTime = 0;
        responses_v[i].cid = -1;
        responses_v[i].active = 0;
        responses_v[i].type = "IP";
        responses_v[i].ifname = NULL;
        responses_v[i].addresses = NULL;
        responses_v[i].dnses = NULL;
        responses_v[i].gateways = NULL;
    }

    RIL_Data_Call_Response_v6 *response = responses_v;
    for (p_cur = p_response->p_intermediates; p_cur != NULL;
            p_cur = p_cur->p_next) {
        char *line = p_cur->line;

        err = at_tok_start(&line);
        if (err < 0)
            goto error;

        err = at_tok_nextint(&line, &response->cid);
        if (err < 0)
            goto error;

        response->ifname = alloca(sizeof(CCMNI_IFNAME)+2);
        sprintf(response->ifname, "%s%d", CCMNI_IFNAME, response->cid-1);

        err = at_tok_nextint(&line, &response->active);
        if (err < 0)
            goto error;

        if (response->active > 0)
            response->active = 2; // 0=inactive, 1=active/physical link down, 2=active/physical link up

        response++;
    }

    at_response_free(p_response);

    err = at_send_command_multiline ("AT+CGDCONT?", "+CGDCONT:", &p_response, rilchnlctx);
    if (err != 0 || p_response->success == 0) {
        if (t != NULL)
            RIL_onRequestComplete(*t, RIL_E_GENERIC_FAILURE, NULL, 0);
        else
            RIL_onUnsolicitedResponse(RIL_UNSOL_DATA_CALL_LIST_CHANGED,
                                      NULL, 0, rilid);
        return;
    }

    for (p_cur = p_response->p_intermediates; p_cur != NULL;
            p_cur = p_cur->p_next) {
        char *line = p_cur->line;
        int cid;
        char *type;
        char *apn;

        err = at_tok_start(&line);
        if (err < 0)
            goto error;

        err = at_tok_nextint(&line, &cid);
        if (err < 0)
            goto error;

        for (i = 0; i < n; i++) {
            if (responses_v[i].cid == cid)
                break;
        }

        if (i >= n) {
            /* details for a context we didn't hear about in the last request */
            continue;
        }

        err = at_tok_nextstr(&line, &out);
        if (err < 0)
            goto error;

        responses_v[i].type = alloca(strlen(out) + 1);
        strcpy(responses_v[i].type, out);

        err = at_tok_nextstr(&line, &out);
        if (err < 0)
            goto error;

        //ignore, see ril.h
        //responses[i].apn = alloca(strlen(out) + 1);
        //strcpy(responses_v[i].apn, out);

        err = at_tok_nextstr(&line, &out);
        if (err < 0)
            goto error;
        /* For activated PDP context, its address is obtained by AT+CGPADDR=<cid> in the following codes */
        if (responses_v[i].active == 0) {
            LOGD("Allocate deactivated PDP context address with cid=%d,len=%d", responses_v[i].cid, strlen(out)+1);
            responses_v[i].addresses = alloca(strlen(out) + 1);
            strcpy(responses_v[i].addresses, out);
            if (sock_fd[responses_v[i].cid-1] != 0) {
                LOGD("cid=%d is not active: close its sock_fd=%d",responses_v[i].cid, sock_fd[responses_v[i].cid-1]);
                close(sock_fd[responses_v[i].cid-1]);
                sock_fd[responses_v[i].cid-1] = 0;
            }
#ifdef INET6
            if (sock6_fd[responses_v[i].cid-1] != 0) {
                LOGD("cid=%d is not active: close its sock6_fd=%d",responses_v[i].cid, sock6_fd[responses_v[i].cid-1]);
                close(sock6_fd[responses_v[i].cid-1]);
                sock6_fd[responses_v[i].cid-1] = 0;
            }
#endif
        }
    }
    at_response_free(p_response);

    /* For each activated PDP context, it should use AT+CGPADDR to get its activated IP address */
    for (i = 0; i < n; i++) {
        if (responses_v[i].active == 2) {
            char *cmd;
            char *line;
            char *address;
            asprintf(&cmd, "AT+CGPADDR=%d", responses_v[i].cid);
            LOGD("Request activated PDP context address with:%s", cmd);
            err = at_send_command_multiline(cmd, "+CGPADDR:", &p_response, rilchnlctx);
            LOGD("Request activated PDP context address err:%d", err);
            free(cmd);
            if (err != 0 || p_response->success == 0) {
                if (t != NULL)
                    RIL_onRequestComplete(*t, RIL_E_GENERIC_FAILURE, NULL, 0);
                else
                    RIL_onUnsolicitedResponse(RIL_UNSOL_DATA_CALL_LIST_CHANGED, NULL, 0, rilid);
                return;
            }
            /* It should have only one intermediate result: +CGPADDR=<cid>,"x.x.x.x" */
            for (p_cur = p_response->p_intermediates; p_cur != NULL;
                    p_cur = p_cur->p_next) {

                line = p_cur->line;
                err = at_tok_start(&line);
                if (err < 0)
                    goto error;

                err = at_tok_nextint(&line, &response->cid);
                if (err < 0)
                    goto error;

                err = at_tok_nextstr(&line, &out);
                if (err < 0)
                    goto error;

                responses_v[i].addresses = alloca(strlen(out) + 1);
                responses_v[i].gateways = alloca(strlen(out) + 1);
                strcpy(responses_v[i].addresses, out);
                strcpy(responses_v[i].gateways, out);
            }
            at_response_free(p_response);
        }
    }

    err = at_send_command_multiline("AT+CGPRCO?", "+CGPRCO:", &p_response, rilchnlctx);
    if (err < 0 || p_response->success == 0)
        goto error;

    for (p_cur = p_response->p_intermediates; p_cur != NULL; p_cur = p_cur->p_next) {
        char *line = p_cur->line;
        err = at_tok_start(&line);
        if (err < 0)
            goto error;

        int response_cid;
        err = at_tok_nextint(&line, &response_cid);
        if (err < 0)
            goto error;

        int response_length = t!= NULL ? n : 1;
        for (i = 0; i < response_length; i++) {
            if (response_cid == responses_v[i].cid) {
                int dnsLength = 0;
                int dns_count = 0;
                char* dnses[2] = {0};

                for (dns_count=0; dns_count < 2; dns_count++) {
                    err = at_tok_nextstr(&line, &out);
                    if (err < 0)
                        goto error;
                    dnses[dns_count] = alloca(strlen(out) + 1);
                    strcpy(dnses[dns_count], out);
                    dnsLength += strlen(dnses[dns_count]);
                    LOGD("Data list CGPRCO Get DNS Server#%d Addr:%s", dns_count+1, dnses[dns_count]);
                }

                if (dnsLength > 0) {
                    responses_v[i].dnses = alloca(dnsLength + 2); //one space
                    int length = 0;
                    if (dnses[0] != NULL)
                        sprintf(responses_v[i].dnses,"%s ", dnses[0]);
                    if (dnses[1] != NULL)
                        sprintf(responses_v[i].dnses + strlen(responses_v[i].dnses),"%s", dnses[1]);
                }
                LOGD("Data list CGPRCO DNS String %s", responses_v->dnses);
            }
        }
    }
    at_response_free(p_response);

    for (i = 0; i < n; i++) {
        LOGD("Data call list[%d]: status=%d, suggestedRetryTime=%d, cid=%d, active=%d, \
type=%s, ifname=%s, addresses=%s, dnses=%s, gateways=%s", i,
            responses_v[i].status, responses_v[i].suggestedRetryTime,
            responses_v[i].cid, responses_v[i].active, responses_v[i].type,
            responses_v[i].ifname, responses_v[i].addresses,
            responses_v[i].dnses, responses_v[i].gateways);
    }

    if (t != NULL)
        RIL_onRequestComplete(*t, RIL_E_SUCCESS, responses_v,
                              n * sizeof(RIL_Data_Call_Response_v6));
    else
        RIL_onUnsolicitedResponse(RIL_UNSOL_DATA_CALL_LIST_CHANGED,
                                  responses_v,
                                  n * sizeof(RIL_Data_Call_Response_v6),
                                  rilid);

    return;

error:
    if (t != NULL)
        RIL_onRequestComplete(*t, RIL_E_GENERIC_FAILURE, NULL, 0);
    else
        RIL_onUnsolicitedResponse(RIL_UNSOL_DATA_CALL_LIST_CHANGED,
                                  NULL, 0, rilid);

    at_response_free(p_response);
}

/* 27:RIL_REQUEST_SETUP_DATA_CALL/RIL_REQUEST_SETUP_DEFAULT_PDP */
/* ril_commands.h : {RIL_REQUEST_SETUP_DATA_CALL, dispatchStrings, responseStrings} */
/* ril_commands.h : {RIL_REQUEST_SETUP_DEFAULT_PDP, dispatchStrings, responseStrings, RIL_CMD_PROXY_3} */
/* Change name from requestSetupDefaultPDP to requestSetupDataCall */
void requestSetupDataCall(void * data, size_t datalen, RIL_Token t)
{
    const char *apn, *username, *passwd, *authType, *protocol, *profile;
    char *cmd;
#ifdef __CCMNI_SUPPORT__
    char *line = NULL, *out = NULL;
    ATLine *p_cur = NULL;
    char *req_dns1="", *req_dns2="";
#endif
    int err;
    /* request_cid default value is 1, but it should be assigned according to the data[] passed into this function */
    int request_parm_num = (datalen/sizeof(char*));
    int request_cid = 1;
#ifdef MTK_IPV6_SUPPORT    
    int available_cid = INVALID_CID;
#endif    
    int request_authType = 0;	
    int request_protocolType = IPV4;
    ATResponse *p_response = NULL;
    char* dnses[2] = {0};
#ifndef __CCMNI_SUPPORT__
    char *response[2] = { "1", PPP_TTY_PATH };
#else
    int response_cid = 0;
    RIL_Data_Call_Response_v6 *responses_v = alloca(sizeof(RIL_Data_Call_Response_v6));
    responses_v->status = PDP_FAIL_ERROR_UNSPECIFIED;
    responses_v->suggestedRetryTime = 0;
    responses_v->cid = -1;
    responses_v->active = 0;
    responses_v->type = "IP";
    responses_v->ifname = NULL;
    responses_v->addresses = NULL;
    responses_v->dnses = NULL;
    responses_v->gateways = NULL;
#endif

    LOGD("requestSetupData() with datalen=%d,parm_num=%d",datalen,request_parm_num);

#ifdef __CUPCAKE__
    apn = ((const char **)data)[0];
    username = ((const char **)data)[1];
    passwd = ((const char **)data)[2];
#else
    /* In GSM with CDMA version: DOUNT - data[0] is radioType(GSM/UMTS or CDMA), data[1] is profile,
     * data[2] is apn, data[3] is username, data[4] is passwd, data[5] is authType (added by Android2.1)
     * data[6] is cid field added by mtk for Multiple PDP contexts setup support 2010-04
     */
    profile = ((const char **)data)[1];
    apn = ((const char **)data)[2];
    username = ((const char **)data)[3];
    passwd = ((const char **)data)[4];
    authType = ((const char **)data)[5];
    LOGD("requestSetupData(): authType is %s",authType);
    protocol = ((const char **)data)[6];
    LOGD("requestSetupData(): protocol is %s",protocol);

    /* cid field is added by mtk, it should be the last parm of the parmlist: by mtk01411 */
    /* [Note by mtk01411]: In original Android2.1, request_parm_num will be 6 due to no cid field */
    if (request_parm_num == 8)
    {
        request_cid = atoi(((const char **)data)[request_parm_num-1]);
        LOGD("requestSetupData() with request_cid=%d",request_cid);
        if (request_cid < 1 || request_cid > 3)
        {
            LOGD("requestSetupData() with invalid cid");
            goto error;
        }
    }
    else
    {
        /* Last parm is the authType instaed of cid */
        LOGD("requestSetupData() without carrying cid parm");
        goto error;
    }
#endif

#ifdef USE_TI_COMMANDS
    // Config for multislot class 10 (probably default anyway eh?)
    err = at_send_command("AT%CPRIM=\"GMM\",\"CONFIG MULTISLOT_CLASS=<10>\"",
                          NULL, DATA_CHANNEL_CTX);

    err = at_send_command("AT%DATA=2,\"UART\",1,,\"SER\",\"UART\",0", NULL, DATA_CHANNEL_CTX);
#endif /* USE_TI_COMMANDS */

    int fd, qmistatus;
    size_t cur = 0;
    size_t len;
    ssize_t written, rlen;
    char status[32] = {0};
    int retry = 10;

    LOGD("requesting data connection to APN '%s' with username='%s', passwd='%s', authType='%s', profile='%s'", apn,username,passwd,authType,profile);

    fd = open ("/dev/qmi", O_RDWR);
    if (fd >= 0) { /* the device doesn't exist on the emulator */

        LOGD("opened the qmi device\n");
        asprintf(&cmd, "up:%s", apn);
        len = strlen(cmd);

        while (cur < len) {
            do {
                written = write (fd, cmd + cur, len - cur);
            } while (written < 0 && errno == EINTR);

            if (written < 0) {
                LOGE("### ERROR writing to /dev/qmi");
                close(fd);
                goto error;
            }

            cur += written;
        }

        // wait for interface to come online

        do {
            sleep(1);
            do {
                rlen = read(fd, status, 31);
            } while (rlen < 0 && errno == EINTR);

            if (rlen < 0) {
                LOGE("### ERROR reading from /dev/qmi");
                close(fd);
                goto error;
            } else {
                status[rlen] = '\0';
                LOGD("### status: %s", status);
            }
        } while (strncmp(status, "STATE=up", 8) && strcmp(status, "online") && --retry);

        close(fd);

        if (retry == 0) {
            LOGE("### Failed to get data connection up\n");
            goto error;
        }

        qmistatus = system("netcfg rmnet0 dhcp");

        LOGD("netcfg rmnet0 dhcp: status %d\n", qmistatus);

        if (qmistatus < 0) goto error;

    } else {

        //Get the autentication type
        request_authType = getAuthType(authType);
        if (!isEmulatorRunning()) { //since emulator would not send PDN urc, we do not use IPv6 handler
            #ifdef MTK_IPV6_SUPPORT
                //Get the protocol type
                request_protocolType = get_protocol_type(protocol);
                
                //Get the available CID from 1~6
                available_cid = getAvailableCid();
                if(available_cid == INVALID_CID) {
                    LOGE("No available CID to use");
                    goto error;
                }
                
                //Save the current CID for PDN URC
                current_cid = request_cid;
                LOGD("Handle IPv6 and IPv4V6 in V6 handler function");            
                requestSetupDataCallOverIPv6(apn, username, passwd, request_authType, request_protocolType, request_cid, available_cid, profile, t);
                at_response_free(p_response);
                return;
            #endif
        } else {
            LOGD("request data connection on EMU, not to use IPv6 handler");
        }

        asprintf(&cmd, "AT+CGDCONT=%d,\"IP\",\"%s\",,0,0",request_cid, apn);
        //FIXME check for error here
        err = at_send_command(cmd, NULL, DATA_CHANNEL_CTX);
        free(cmd);


        //Add by mtk80372
        request_authType = atoi(authType);
        LOGD("requestSetupDataCall(): request_cid is %d and request_authType is %d ",  request_cid, request_authType);
        //Application 0->none ; 1->PAP;    2->CHAP; 3->PAP/CHAP;
        //Modem       0->PAP;    1->CHAP; 2->NONE; 3->PAP/CHAP;
        switch(request_authType)
        {
        case 0:
            request_authType = AUTHTYPE_NONE;
            break;
        case 1:
            request_authType = AUTHTYPE_PAP;
            break;
        case 2:
            request_authType = AUTHTYPE_CHAP;
            break;
        case 3:
            request_authType = AUTHTYPE_PAP_CHAP;
            break;
        default:
            request_authType = AUTHTYPE_NOT_SET;
            break;
        }
        LOGD("requestSetupDataCall(): after translation,request_cid is %d and request_authType is %d ",  request_cid, request_authType);

        // packet-domain event reporting: +CGEREP=<mode>,<bfr>
        err = at_send_command("AT+CGEREP=1,0", NULL, DATA_CHANNEL_CTX);
#ifndef __CCMNI_SUPPORT__
        // Hangup anything that's happening there now
        err = at_send_command("AT+CGACT=1,0", NULL, DATA_CHANNEL_CTX);

        // Start data on PDP context 1
        if (p_response != NULL) {
            at_response_free(p_response);
            p_response = NULL;
        }        
        err = at_send_command("ATD*99***1#", &p_response, DATA_CHANNEL_CTX);
#else
        if (AUTHTYPE_NOT_SET == request_authType) {
            /* Use AT+CGPRCO=<cid>,<user_name>,<passwd>,<DNS1>,<DNS2> */
            asprintf(&cmd, "AT+CGPRCO=%d,\"%s\",\"%s\",\"%s\",\"%s\"", request_cid, username, passwd, req_dns1, req_dns2);
        } else {
            /* Use AT+CGPRCO=<cid>,<user_name>,<passwd>,<DNS1>,<DNS2> ,<authType>*/
            asprintf(&cmd, "AT+CGPRCO=%d,\"%s\",\"%s\",\"%s\",\"%s\" ,%d", request_cid, username, passwd, req_dns1, req_dns2,request_authType);
        }
        err = at_send_command(cmd, NULL, DATA_CHANNEL_CTX);
        free(cmd);

        // Set modem follow on flag
        err = at_send_command("AT+EGTYPE=3", &p_response, DATA_CHANNEL_CTX);
        at_response_free(p_response);
        p_response = NULL;

        err = at_send_command("AT+EGTYPE=1", &p_response, DATA_CHANNEL_CTX);
        if (err < 0 && p_response->success == 0) {
            LOGD("Fail to set connetion type!!!");
            gprs_failure_cause = 14;
            goto error;
        }
        at_response_free(p_response);
        p_response = NULL;		

        /* Use AT+CGACT=1,cid to activate PDP context indicated via this cid */
        /* Currently, Android only supports one PDP context: cid=1 */
        gprs_failure_cause = 14;
        if (p_response != NULL) {
            at_response_free(p_response);
            p_response = NULL;
        }
        asprintf(&cmd, "AT+CGACT=1,%d", request_cid);
        err = at_send_command(cmd, &p_response, DATA_CHANNEL_CTX);
        free(cmd);
        if (err < 0 || p_response->success == 0){
            if(p_response->success == 0){
               gprs_failure_cause = at_get_cme_error(p_response);
                if (gprs_failure_cause > SM_CAUSE_BASE && gprs_failure_cause < SM_CAUSE_END) {
                    gprs_failure_cause -= SM_CAUSE_BASE;
                    LOGD("AT+CGACT SM CAUSE: %x", gprs_failure_cause);
                } else if (gprs_failure_cause == 132) { // The following are failure cause to activate a context defined in TS 27.007 and convert to 24.008 cause
                    gprs_failure_cause = SM_SERVICE_OPTION_NOT_SUPPORTED;
                    LOGD("AT+CGACT SM CAUSE: %x", gprs_failure_cause);
                } else if (gprs_failure_cause == 133) {
                    gprs_failure_cause = SM_SERVICE_OPTION_NOT_SUBSCRIBED;
                    LOGD("AT+CGACT SM CAUSE: %x", gprs_failure_cause);
                } else if (gprs_failure_cause == 134) {
                    gprs_failure_cause = SM_SERVICE_OPTION_OUT_OF_ORDER;
                    LOGD("AT+CGACT SM CAUSE: %x", gprs_failure_cause);
                } else if (gprs_failure_cause == 149) {
                    gprs_failure_cause = SM_USER_AUTHENTICATION_FAILED;
                    LOGD("AT+CGACT SM CAUSE: %x", gprs_failure_cause);
                } else {
                    LOGD("AT+CGACT err=%d",gprs_failure_cause);
                }
            }
            goto error;
        }

        /* Use AT+CGPADDR=cid to query the ip address assigned to this PDP context indicated via this cid */
        if (p_response != NULL) {
            at_response_free(p_response);
            p_response = NULL;
        }
        asprintf(&cmd, "AT+CGPADDR=%d", request_cid);
        err = at_send_command_singleline(cmd, "+CGPADDR:", &p_response, DATA_CHANNEL_CTX);
        free(cmd);

        /* Parse the response to get the ip address */
        if (err < 0 || p_response->success == 0) {
            goto error;
        }

        line = p_response->p_intermediates->line;
        err = at_tok_start(&line);
        /* line => +CGPADDR:<cid>,"x.x.x.x" */
        if (err < 0)
            goto error;

        /* Get 1st parameter: CID */
        err = at_tok_nextint(&line, &response_cid);
        LOGD("CGPADDR Get CID err=%d,cid=%d", err, response_cid);
        if (err < 0 || response_cid != request_cid)
            goto error;
        responses_v->cid = response_cid;
        LOGD("CGPADDR Get CID:%d", responses_v->cid);

        /* CCMNI interface name: It should a way to find which CCMNI interface name should be filled */
        responses_v->ifname = alloca(sizeof(CCMNI_IFNAME) + 2);
        sprintf(responses_v->ifname,"%s%d", CCMNI_IFNAME, response_cid - 1);
        LOGD("Set Interface Name:%s", responses_v->ifname);

        /* Get 2nd parameter: IPAddr */
        err = at_tok_nextstr(&line, &out);
        if (err < 0)
            goto error;
        responses_v->addresses = alloca(strlen(out) + 1);
        responses_v->gateways = alloca(strlen(out) + 1);
        strcpy(responses_v->addresses, out);
        strcpy(responses_v->gateways, out);
        LOGD("CGPADDR Get IPAddr:%s", responses_v->addresses);
        LOGD("CGPADDR Get Gateway:%s", responses_v->gateways);

        /* Free the p_response for +CGPADDR: */
        if (p_response != NULL) {
            at_response_free(p_response);
            p_response = NULL;
        }

        /* Use AT+CGPRCO? to query each cid's dns server address */
        err = at_send_command_multiline("AT+CGPRCO?", "+CGPRCO:", &p_response, DATA_CHANNEL_CTX);
        if (err < 0 || p_response->success == 0)
            goto error;

        for (p_cur = p_response->p_intermediates; p_cur != NULL; p_cur = p_cur->p_next) {
            line = p_cur->line;
            err = at_tok_start(&line);
            /* line => +CGPRCO:<cid>,"x.x.x.x","y.y.y.y" */
            if (err < 0)
                goto error;

            /* Get 1st parameter: CID */
            err = at_tok_nextint(&line, &response_cid);
            if (err < 0)
                goto error;
            if (response_cid == request_cid)
            {
                int dns_count = 0, ret_val = 0;
                char dns_property_name[PROPERTY_KEY_MAX]= {0};
                char dns_property_value[PROPERTY_VALUE_MAX]= {0};
                struct ifreq ifr;
                int org_uid = -1;

                /* Open the network interface: CCMNI */
                memset(&ifr, 0, sizeof(struct ifreq));
                sprintf(ifr.ifr_name,"%s",responses_v->ifname);
#ifdef __ADD_CCMNI_INFO_IN_RIL__
                ril_data_ioctl_init(response_cid-1);
                ril_data_setflags(sock_fd[response_cid-1],&ifr,IFF_UP,0);
                ril_data_setaddr(sock_fd[response_cid-1],&ifr,responses_v->addresses);
                LOGD("After bringing network interface up and setting its ip,close sock_fd=%d, for cid=%d",sock_fd[response_cid-1], response_cid);
                close(sock_fd[response_cid-1]);
                sock_fd[response_cid-1] = 0;

#if 0
                /* Add the one entry into the routing table as the default gateway */
                /* Check if any default route gw exists or not first */
                /* If MMS APN type is activated first, it will add itself as default gw! */
                /* It should left the decision to Framework or ConnectivityServer to make */

                if(ril_data_default_gw_exist_check() == 0)
                {
                    LOGD("Default GW added in RILD");
                    ril_data_setdefault_route(sock_fd[response_cid-1],ifr.ifr_name,responses_v->addresses);
                    /* Dump Debug Informaiton */
                    ril_data_default_gw_exist_check();
                }
                else
                {
                    LOGD("Default GW already exists!");
                }
#endif
#endif /* __ADD_CCMNI_INFO_IN_RIL__ */

                /* Set net.ccmnix.local-ip property */
                sprintf(dns_property_name,"net.%s%d.local-ip", CCMNI_IFNAME, response_cid-1);
                LOGD("Set net_%s%d_local_ip=%s", CCMNI_IFNAME, response_cid-1, responses_v->addresses);
                ret_val = property_set(dns_property_name, responses_v->addresses);
                memset(dns_property_name,0,sizeof(dns_property_name));

                /* Set net.ccmnix.gw property */
                /* For FW's ConnectivityService: Get gwAddr successfully  */
                sprintf(dns_property_name,"net.%s%d.gw", CCMNI_IFNAME, response_cid-1);
                LOGD("Set net_%s%d_gw=%s", CCMNI_IFNAME, response_cid-1, responses_v->addresses);
                ret_val = property_set(dns_property_name, responses_v->addresses);
                memset(dns_property_name,0,sizeof(dns_property_name));

                /* Get 1st and 2nd DNS Server Address */
                for (dns_count=0; dns_count < 2; dns_count++) {
                    err = at_tok_nextstr(&line, &out);
                    if (err < 0)
                        goto error;

                    char dnsAddr[MAX_IPV4_ADDRESS] = {0};
                    RILId rid = getRILIdByChannelCtx(DATA_CHANNEL_CTX);
                    int dnsServerIndex = rid == MTK_RIL_SOCKET_1 ? 0 : 2;
                    dnsServerIndex += dns_count;

                    if (strcmp(out, NULL_IPV4_ADDRESS) != 0 && strlen(out) > 0) {
                        strcpy(dnsAddr, out);
                        memset(g_last_dns_server[dnsServerIndex], 0, sizeof(g_last_dns_server[dnsServerIndex]));
                        strcpy(g_last_dns_server[dnsServerIndex], dnsAddr);
                    } else {
                        if (strlen(g_last_dns_server[dnsServerIndex]) == 0 || strcmp(g_last_dns_server[dnsServerIndex], NULL_IPV4_ADDRESS) == 0) {
                            LOGD("(v4)No V4 DNS, use public DNS instead [%s]", g_public_dns_server[dns_count]);
                            strcpy(dnsAddr, g_public_dns_server[dns_count]);
                        } else {
                        LOGD("(v4)No V4 DNS, use last DNS instead [%s]", g_last_dns_server[dnsServerIndex]);
                            strcpy(dnsAddr, g_last_dns_server[dnsServerIndex]);
                        }
                    }

                    dnses[dns_count] = alloca(strlen(dnsAddr) + 1);
                    strcpy(dnses[dns_count], dnsAddr);
                    LOGD("CGPRCO Get DNS Server#%d Addr:%s", dns_count+1, dnses[dns_count]);
                }

                int dnsLength = 0;
                for (dns_count=0; dns_count < 2; dns_count++) {
                    if (dnses[dns_count] != NULL) {
                        dnsLength += strlen(dnses[dns_count]);
                        memset(dns_property_name,0,sizeof(dns_property_name));
                        memset(dns_property_value,0,sizeof(dns_property_value));
                        sprintf(dns_property_name,"net.%s%d.dns%d", CCMNI_IFNAME, response_cid-1,dns_count+1);
                        LOGD("CGPRCO DNS Property Name:%s, value=%s", dns_property_name, dnses[dns_count]);
                        ret_val = property_set(dns_property_name, dnses[dns_count]);
                        LOGD("CGPRCO DNS Property Set result=%d", ret_val);
                        property_get(dns_property_name,dns_property_value,NULL);
                        LOGD("CGPRCO DNS Property Get value=%s", dns_property_value);
                    }
                }

                if (dnsLength > 0) {
                    responses_v->dnses = alloca(dnsLength + 2); //one space
                    int length = 0;
                    if (dnses[0] != NULL)
                        sprintf(responses_v->dnses,"%s ", dnses[0]);
                    if (dnses[1] != NULL)
                        sprintf(responses_v->dnses + strlen(responses_v->dnses),"%s", dnses[1]);
                }
                LOGD("CGPRCO DNS String %s", responses_v->dnses);

                memset(dns_property_value,0,sizeof(dns_property_value));
                property_get("net.dnschange",dns_property_value,NULL);
                LOGD("Get dnschange=%s",dns_property_value);
                sprintf(dns_property_value,"%d",atoi(dns_property_value)+1);
                LOGD("Set new dnschange=%s",dns_property_value);
                ret_val = property_set("net.dnschange",dns_property_value);
            }
        }
        /* AT+CGDATA=<L2P>,<cid> */
        asprintf(&cmd, "AT+CGDATA=\"M-UPS\",%d", request_cid);
        responses_v->status = PDP_FAIL_NONE;
        responses_v->active = 2;
        err = at_send_command(cmd, NULL, DATA_CHANNEL_CTX);
        free(cmd);
#endif
        if (err < 0 || p_response->success == 0) {
            goto error;
        }
    }

    LOGD("Data call response: status=%d, suggestedRetryTime=%d, cid=%d, active=%d, \
type=%s, ifname=%s, addresses=%s, dnses=%s, gateways=%s",
            responses_v->status, responses_v->suggestedRetryTime,
            responses_v->cid, responses_v->active, responses_v->type,
            responses_v->ifname, responses_v->addresses,
            responses_v->dnses, responses_v->gateways);

    RIL_onRequestComplete(t, RIL_E_SUCCESS, responses_v, sizeof(RIL_Data_Call_Response_v6));
    /* Free the p_response for ATD* or +CGPRCO: */
    if (p_response != NULL) {
        at_response_free(p_response);
        p_response = NULL;
    }

    return;
error:
#ifdef MTK_IPV6_SUPPORT    
    if(available_cid != INVALID_CID) {
        cid_status[available_cid-1] == INVALID_CID;
    }
#endif    
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    if (p_response != NULL) {
        at_response_free(p_response);
         p_response = NULL;
    }
}

/* 41:RIL_REQUEST_DEACTIVATE_DATA_CALL/RIL_REQUEST_DEACTIVATE_DEFAULT_PDP */
/* ril_commands.h : {RIL_REQUEST_DEACTIVATE_DATA_CALL, dispatchStrings, responseVoid} */
/* ril_commands.h : {RIL_REQUEST_DEACTIVATE_DEFAULT_PDP, dispatchStrings, responseVoid, RIL_CMD_PROXY_3} */
/* Change name from requestDeactiveDefaultPdp to requestDeactiveDataCall */
// TODO: requestDeactiveDataCall/requestDeactiveDefaultPdp
void requestDeactiveDataCall(void * data, size_t datalen, RIL_Token t)
{
    const char *cid;
    char *cmd;
    int err;
    ATResponse *p_response = NULL;
    int i = 0;
#ifdef __CCMNI_SUPPORT__
    struct ifreq ifr;
    int cid_value = 0;
    int result = -1;
    int ccmni_num = -1;
#endif
#ifdef MTK_IPV6_SUPPORT
    int pdpCid = INVALID_CID;
#endif

    cid = ((const char **)data)[0];
    cid_value = atoi(cid);
    ccmni_num = cid_value - 1;

    if (g_pdp_ccmni_list[cid_value-1] != -1) {
        ccmni_num = g_pdp_ccmni_list[cid_value-1];
    }

#ifndef MTK_IPV6_SUPPORT
    // AT+CGACT=<state>,<cid>;  <state>:0-deactivate;1-activate
    asprintf(&cmd, "AT+CGACT=0,%s", cid);
    LOGD("requestDeactiveDefailtPdp():via cmd=%s",cmd);
    err = at_send_command(cmd, &p_response, DATA_CHANNEL_CTX);
    free(cmd);
    if (err < 0 || p_response->success == 0) {
        if (p_response->success == 0 &&
                at_get_cme_error(p_response) == 0x1009) { 
            // L4C error: L4C_CONTEXT_CONFLICT_DEACT_ALREADY_DEACTIVATED(0x1009)
            LOGD("cid %d already deactivated.", i);
        } else {
            goto error;
        }
    }
#else
    for(i = 0; i < MAX_CID_NUM; i++) {
        pdpCid = getActiveCid(cid_value);
        if( pdpCid != INVALID_CID) {
            asprintf(&cmd, "AT+CGACT=0,%d", pdpCid);
            LOGD("requestDeactiveDefailtPdp_v6():via cmd=%s",cmd);
            if (p_response != NULL) {
                at_response_free(p_response);
                p_response = NULL;
            }
            err = at_send_command(cmd, &p_response, DATA_CHANNEL_CTX);
            free(cmd);
            if (err < 0 || p_response->success == 0) {
                if (p_response->success == 0 &&
                        at_get_cme_error(p_response) == 0x1009) { 
                    // L4C error: L4C_CONTEXT_CONFLICT_DEACT_ALREADY_DEACTIVATED(0x1009)
                    LOGD("cid %d already deactivated.", i);
                } else {
                    goto error;
                }
            }
            
            cid_status[pdpCid-1] = INVALID_CID;
        }
    }

    for (i = 0; i<MAX_CID_NUM*MAX_PDP_NUM; i++) {
        if (g_pdp_ccmni_list[i] == ccmni_num) {
            g_pdp_ccmni_list[i] = -1;
        }
    }
#endif

    LOGD("cid_status: %d %d %d %d %d %d", cid_status[0], cid_status[1], cid_status[2], cid_status[3], cid_status[4], cid_status[5]);
    LOGD("pdp_ccmni: %d %d %d %d %d %d", g_pdp_ccmni_list[0], g_pdp_ccmni_list[1], g_pdp_ccmni_list[2], g_pdp_ccmni_list[3], g_pdp_ccmni_list[4], g_pdp_ccmni_list[5]);

#if defined(__CCMNI_SUPPORT__) && defined(__DELETE_CCMNI_INFO_IN_RIL__)
    /* Close the network interface: */
    memset(&ifr, 0, sizeof(struct ifreq));
    sprintf(ifr.ifr_name,"%s%d", CCMNI_IFNAME, ccmni_num);
    LOGD("requestDeactiveDefailtPdp():ifname=%s",ifr.ifr_name);
    //ril_data_setflags(sock_fd[cid_value-1], &ifr, 0, IFF_UP);
    /* Reset related system properties: net.dns1, net.dns2, net.ccmnix.dns1, net.ccmnix.dns2, net.ccmnix.local-ip */
    result = ifc_reset_connections(ifr.ifr_name);
    LOGD("requestDeactiveDefailtPdp():resetconnections result=%d",result);
    result = ifc_remove_default_route(ifr.ifr_name);
    LOGD("requestDeactiveDefailtPdp():remove default route result=%d",result);
    result = ifc_disable(ifr.ifr_name);
    LOGD("requestDeactiveDefailtPdp():disable interface result=%d",result);
#endif /* __CCMNI_SUPPORT__ &&  __DELETE_CCMNI_INFO_IN_RIL__ */

#ifdef __ADD_CCMNI_INFO_IN_RIL__
    if (sock_fd[ccmni_num] != 0) {
        LOGD("requestDeactiveDataCall():close sock_fd=%d, for cid=%d",sock_fd[ccmni_num], cid_value);
        close(sock_fd[ccmni_num]);
        sock_fd[ccmni_num] = 0;
    }
#ifdef INET6
    if (sock6_fd[ccmni_num] != 0) {
        LOGD("requestDeactiveDataCall():close sock6_fd=%d, for cid=%d",sock6_fd[ccmni_num], cid_value);
        close(sock6_fd[ccmni_num]);
        sock6_fd[ccmni_num] = 0;
    }
#endif
#endif /* __ADD_CCMNI_INFO_IN_RIL__ */

    for (i = 0; i < MAX_PDP_NUM*MAX_CID_NUM; i++) {
        if (g_data_call_list[i] == cid_value) {
            g_data_call_list[i] = -1;
            break;
        }
    }
    LOGD("Current data call list: %d %d %d %d %d %d", g_data_call_list[0], g_data_call_list[1], g_data_call_list[2],
        g_data_call_list[3], g_data_call_list[4], g_data_call_list[5]);

    RIL_onRequestComplete(t, RIL_E_SUCCESS, NULL, 0);
    if (p_response != NULL) {
        at_response_free(p_response);
        p_response = NULL;
    }
    return;
error:
    RIL_onRequestComplete(t, RIL_E_GENERIC_FAILURE, NULL, 0);
    if (p_response != NULL) {
        at_response_free(p_response);
        p_response = NULL;
    }
}

int getLastDataCallFailCause()
{
    char value[32] = {'\0'};
    int specific_sm_cause = 0;
    
    property_get(PROPERTY_RIL_SPECIFIC_SM_CAUSE, value, "0");
    specific_sm_cause = atoi(value);

    if (specific_sm_cause != 0 &&
            (gprs_failure_cause == SM_OPERATOR_BARRED ||
            gprs_failure_cause == SM_MBMS_CAPABILITIES_INSUFFICIENT ||
            gprs_failure_cause == SM_LLC_SNDCP_FAILURE ||
            gprs_failure_cause == SM_INSUFFICIENT_RESOURCES ||
            gprs_failure_cause == SM_MISSING_UNKNOWN_APN ||
            gprs_failure_cause == SM_UNKNOWN_PDP_ADDRESS_TYPE ||
            gprs_failure_cause == SM_USER_AUTHENTICATION_FAILED ||
            gprs_failure_cause == SM_ACTIVATION_REJECT_GGSN ||
            gprs_failure_cause == SM_ACTIVATION_REJECT_UNSPECIFIED ||
            gprs_failure_cause == SM_SERVICE_OPTION_NOT_SUPPORTED ||
            gprs_failure_cause == SM_SERVICE_OPTION_NOT_SUBSCRIBED ||
            gprs_failure_cause == SM_SERVICE_OPTION_OUT_OF_ORDER ||
            gprs_failure_cause == SM_NSAPI_IN_USE ||
            gprs_failure_cause == SM_REGULAR_DEACTIVATION ||
            gprs_failure_cause == SM_QOS_NOT_ACCEPTED ||
            gprs_failure_cause == SM_NETWORK_FAILURE ||
            gprs_failure_cause == SM_REACTIVATION_REQUESTED ||
            gprs_failure_cause == SM_FEATURE_NOT_SUPPORTED ||
            gprs_failure_cause == SM_SEMANTIC_ERROR_IN_TFT ||
            gprs_failure_cause == SM_SYNTACTICAL_ERROR_IN_TFT ||
            gprs_failure_cause == SM_UNKNOWN_PDP_CONTEXT ||
            gprs_failure_cause == SM_SEMANTIC_ERROR_IN_PACKET_FILTER ||
            gprs_failure_cause == SM_SYNTACTICAL_ERROR_IN_PACKET_FILTER ||
            gprs_failure_cause == SM_PDP_CONTEXT_WITHOU_TFT_ALREADY_ACTIVATED ||
            gprs_failure_cause == SM_MULTICAST_GROUP_MEMBERSHIP_TIMEOUT ||
            gprs_failure_cause == SM_BCM_VIOLATION ||
            gprs_failure_cause == SM_ONLY_IPV4_ALLOWED ||
            gprs_failure_cause == SM_ONLY_IPV6_ALLOWED ||
            gprs_failure_cause == SM_ONLY_SINGLE_BEARER_ALLOWED ||
            gprs_failure_cause == SM_COLLISION_WITH_NW_INITIATED_REQUEST ||
            gprs_failure_cause == SM_BEARER_HANDLING_NOT_SUPPORT ||
            gprs_failure_cause == SM_MAX_PDP_NUMBER_REACHED ||
            gprs_failure_cause == SM_APN_NOT_SUPPORT_IN_RAT_PLMN ||
            gprs_failure_cause == SM_INVALID_TRANSACTION_ID_VALUE ||
            gprs_failure_cause == SM_SEMENTICALLY_INCORRECT_MESSAGE ||
            gprs_failure_cause == SM_INVALID_MANDATORY_INFO ||
            gprs_failure_cause == SM_MESSAGE_TYPE_NONEXIST_NOT_IMPLEMENTED ||
            gprs_failure_cause == SM_MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE ||
            gprs_failure_cause == SM_INFO_ELEMENT_NONEXIST_NOT_IMPLEMENTED ||
            gprs_failure_cause == SM_CONDITIONAL_IE_ERROR ||
            gprs_failure_cause == SM_MESSAGE_NOT_COMPATIBLE_WITH_PROTOCOL_STATE ||
            gprs_failure_cause == SM_PROTOCOL_ERROR ||
            gprs_failure_cause == SM_APN_RESTRICTION_VALUE_INCOMPATIBLE_WITH_PDP_CONTEXT)) {
        return gprs_failure_cause;
    }else if (gprs_failure_cause == 0x0D19){
        LOGD("getLastDataCallFailCause(): GMM error %X", gprs_failure_cause);
    } else {
       gprs_failure_cause = 14;  //FailCause.Unknown
    }
    
    return gprs_failure_cause;
}

/* 56:RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE/RIL_REQUEST_LAST_PDP_FAIL_CAUSE */
/* ril_commands.h : {RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE, dispatchVoid, responseInts} */
/* ril_commands.h : {RIL_REQUEST_LAST_PDP_FAIL_CAUSE, dispatchVoid, responseInts, RIL_CMD_PROXY_3} */
/* Change name from requestLastPdpFailCause to requestLastDataCallFailCause */
// TODO: requestLastDataCallFailCause/requestLastPdpFailCause
void requestLastDataCallFailCause(void * data, size_t datalen, RIL_Token t)
{
    int lastPdpFailCause = 14;
    lastPdpFailCause = getLastDataCallFailCause();
    RIL_onRequestComplete(t, RIL_E_SUCCESS, &lastPdpFailCause, sizeof(lastPdpFailCause));
}

/* 57:RIL_REQUEST_DATA_CALL_LIST/RIL_REQUEST_PDP_CONTEXT_LIST */
/* ril_commands.h : {RIL_REQUEST_DATA_CALL_LIST, dispatchVoid, responseDataCallList} */
/* ril_commands.h : {RIL_REQUEST_PDP_CONTEXT_LIST, dispatchVoid, responseContexts, RIL_CMD_PROXY_3} */
/* Change name from requestPdpContetList to requestDataCallList */
void requestDataCallList(void * data, size_t datalen, RIL_Token t)
{
    requestOrSendDataCallList(&t, 0);
}

void onGPRSDeatch(char* urc, RILId rid){
    
    LOGD("onGPRSDeatch:%s", urc);
    RIL_onUnsolicitedResponse(RIL_UNSOL_GPRS_DETACH, NULL, 0, rid);
}

/* Change name from onPdpContextListChanged to onDataCallListChanged */
/* It can be called in onUnsolicited() mtk-ril\ril_callbacks.c */
void onDataCallListChanged(void* param)
{
    RILId rilid = *((RILId *) param);
    requestOrSendDataCallList(NULL, rilid);
}


extern int rilDataMain(int request, void *data, size_t datalen, RIL_Token t)
{
    switch (request)
    {
    case RIL_REQUEST_SETUP_DATA_CALL:
        MTK_REQUEST_SETUP_DATA_CALL(data, datalen, t);
        break;
    case RIL_REQUEST_DATA_CALL_LIST:
        MTK_REQUEST_DATA_CALL_LIST(data, datalen, t);
        break;
    case RIL_REQUEST_DEACTIVATE_DATA_CALL:
        MTK_REQUEST_DEACTIVATE_DATA_CALL(data, datalen, t);
        break;
    case RIL_REQUEST_LAST_DATA_CALL_FAIL_CAUSE:
        MTK_REQUEST_LAST_DATA_CALL_FAIL_CAUSE(data, datalen, t);
        break;
    case RIL_REQUEST_SET_SCRI:
        MTK_REQUEST_SET_SCRI(data, datalen, t);
        break;
    //[New R8 modem FD]	
    case RIL_REQUEST_SET_FD_MODE:
         MTK_REQUEST_FD_MODE(data, datalen, t);
        break;
    case RIL_REQUEST_DETACH_PS:
        MTK_REQUEST_DETACH_PS(data, datalen, t);
        break;		 	
    default:
        return 0; /* no matched request */
        break;
    }

    return 1; /* request found and handled */

}

extern int rilDataUnsolicited(const char *s, const char *sms_pdu, RILChannelCtx* p_channel)
{
    int rilid = getRILIdByChannelCtx(p_channel);
    RILChannelCtx* pDataChannel = getChannelCtxbyId(sCmdChannel4Id[rilid]);
    
#ifdef MTK_IPV6_SUPPORT
    if (strStartsWith(s, "+CGEV: NW DEACT")) {
        onPdnDeactResult((char*) s);


        if (s_md_off)	{
            LOGD("rilDataUnsolicited(): modem off!");
            RIL_onUnsolicitedResponse(RIL_UNSOL_DATA_CALL_LIST_CHANGED, NULL, 0, rilid);
         } else {
            RIL_requestProxyTimedCallback (onDataCallListChanged, 
                                                               &s_data_ril_cntx[rilid],
                                                               NULL, 
                                                               pDataChannel->id, 
                                                               "onDataCallListChanged");
         }

        return 1;
    } else if (strStartsWith(s, "+CGEV:")) {
#else
    if (strStartsWith(s, "+CGEV:")) {
#endif

        /* Really, we can ignore NW CLASS and ME CLASS events here,
         * but right now we don't since extranous
         * RIL_UNSOL_DATA_CALL_LIST_CHANGED calls are tolerated
         */
        /* can't issue AT commands here -- call on main thread */
        // + CGEV:DETACH only means modem begin to detach gprs.
        //if(strstr(s, "DETACH")){
            //onGPRSDeatch((char*) s,(RILId) rilid);
        //}else{
        //RIL_requestTimedCallback (onDataCallListChanged, &s_data_ril_cntx[rilid], NULL);
        //}
        if (s_md_off)	{
            LOGD("rilDataUnsolicited(): modem off!");
            RIL_onUnsolicitedResponse(RIL_UNSOL_DATA_CALL_LIST_CHANGED, NULL, 0, rilid);
         } else {
            RIL_requestProxyTimedCallback (onDataCallListChanged, 
                                                               &s_data_ril_cntx[rilid],
                                                               NULL, 
                                                               pDataChannel->id, 
                                                               "onDataCallListChanged");
         }
        
        return 1;

#ifdef WORKAROUND_FAKE_CGEV
    } else if (strStartsWith(s, "+CME ERROR: 150")) {

        if (s_md_off)	{
            LOGD("rilDataUnsolicited(): modem off!");
            RIL_onUnsolicitedResponse(RIL_UNSOL_DATA_CALL_LIST_CHANGED, NULL, 0, rilid);
         } else {
            RIL_requestProxyTimedCallback (onDataCallListChanged, 
                                                               &s_data_ril_cntx[rilid],
                                                               NULL, 
                                                               pDataChannel->id, 
                                                               "onDataCallListChanged");
         }

        return 1;

#endif /* WORKAROUND_FAKE_CGEV */

    } else if(strStartsWith(s, "+ESCRI:")) {
        onScriResult((char*) s,(RILId) rilid);
        return 1;
    }

    return 0;
}


