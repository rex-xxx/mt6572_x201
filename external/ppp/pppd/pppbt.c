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
 * pppbt.c
 *
 * Project:
 * --------
 *   BT Project
 *
 * Description:
 * ------------
 *   This file is used to provide data transmission with pppd
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
#if defined (__BTMTK__) && defined (__BT_DUN_PROFILE__)

#include <stdio.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <errno.h>
#include <netdb.h>
#include <linux/if.h>
#include <netinet/in.h>
#include <linux/if_arp.h>

#include "pppd.h"
#include "fsm.h"
#include "ipcp.h"
#include "btdunutils.h"


static int pppbt_setsockname(char **argv) ;
static int pppbt_setipaddr(char **argv);
static int pppbt_connect(void);
static void pppbt_disconnect(void) ;
static void pppbt_check_option(void);
//static void getipaddr(char *ifname, char *ipaddr);


static option_t pppbt_options[] = {
    {"pppbt-setsockname", o_special, pppbt_setsockname, "PPPBT set socket name", OPT_DEVNAM, NULL, 0, 0, NULL, 0, 0},
    {"pppbt-setipaddr", o_special, pppbt_setipaddr, "PPPBT set ip address", OPT_DEVNAM, NULL, 0, 0, NULL, 0, 0},
    {NULL, 0, NULL, NULL, 0, NULL, 0, 0, NULL, 0, 0},
};

static struct channel pppbt_channel = {
    .options = pppbt_options,
    .process_extra_options = NULL,
    .check_options = pppbt_check_option,
    .connect = pppbt_connect,
    .disconnect = pppbt_disconnect,
    .establish_ppp = generic_establish_ppp,
    .disestablish_ppp = generic_disestablish_ppp,
    .send_config = NULL,
    .recv_config = NULL,
    .cleanup = NULL,
    .close = NULL,
};

int pppbt = -1;
static char *pppbt_sock_path;

bool bPppbt = 0;
 
#define PPPBT_IP_ADDR_LEN		15


/* Start argv:
*  pppbt-setsockname [socketname]; pppbt-setipaddr [ip_addr]; ms-dns [dns1_addr]; ms-dns [dns2_addr].
*/

void pppbt_init() 
{
    add_options(pppbt_options);
}

void pppbt_deinit()
{
    bPppbt = 0;
}

/* Set socket path name */
static int pppbt_setsockname(char **argv) 
{
    struct protent *protp= &ipcp_protent;
    printf("pppbt_set argv len: %d\n", strlen(*argv));

    // "bt.ext.adp.spp.data"
    pppbt_sock_path = (char *)malloc(strlen(*argv) + 1);
    if (!pppbt_sock_path)
    {
        return -1;
    }
    memcpy(pppbt_sock_path, *argv, strlen(*argv));
    info("path: %s\n", pppbt_sock_path);

    the_channel = &pppbt_channel;
    bPppbt = 1;

    protp->init(0);
    
    return 1;
}

static int pppbt_setipaddr(char **argv)
{
    //char ipaddr[PPPBT_IP_ADDR_LEN + 1] = "192.168.3.2";
    char ipaddr[PPPBT_IP_ADDR_LEN + 1];
    char *addr;
    char *ch;

    info("pppbt_setipaddr: %s\n", *argv);

    //getipaddr(*argv, ipaddr); 
    memset(ipaddr, 0, sizeof(ipaddr));
    if (ip_dun_create_netdev(*argv, ipaddr) < 0)
    {
        return -1;
    }

    addr = (char *)malloc(strlen(ipaddr) + 2);
    if (addr == NULL)
    {
        return -1;
    }
    memset(addr, 0, strlen(ipaddr) + 2);
    memcpy(addr, ipaddr, strlen(ipaddr));
   
    ch = addr + strlen(ipaddr);
    *ch = ':';

    info("ip addr: %s", addr);	
    return setipaddr(addr, NULL, 1);
}

/* Create Bluetooth DUN socket as client, and connect to server.
*  Return socket handle to ppp daemon to do further read and write.
*/
static int pppbt_connect(void) 
{
    struct sockaddr_un btaddr;
    socklen_t btaddrlen;

    pppbt = socket(AF_LOCAL, SOCK_STREAM, 0);
    if (pppbt < 0)
    {
        info("[pppbt] create bt data socket failed\n");
        return -1;
    }
    
    btaddr.sun_family = AF_LOCAL;
    btaddr.sun_path[0] = 0;	
    strcpy(btaddr.sun_path + 1, pppbt_sock_path);
    btaddrlen = offsetof(struct sockaddr_un, sun_path) + strlen(btaddr.sun_path) + 1;	

    if (connect(pppbt, (struct sockaddr *)&btaddr, btaddrlen) < 0)
    {
        info("[pppbt] connect to bt.ext.adp.spp.data failed: %s(%d)\n", strerror(errno), errno);
        return -1;
    }

    //if (ip_dun_init(pppbt) < 0)
        //return -1;
    ip_dun_setfd(pppbt);

    return pppbt;
}

/* Close Bluetooth DUN socket */
static void pppbt_disconnect(void) 
{
    info("[pppbt] disconnect");
    if (pppbt >= 0) {
        close(pppbt);
        pppbt = -1;
    }
    ip_dun_remove_netdev();
}

/* Set global parameters */
static void pppbt_check_option(void)
{
    nodetach = 1;
}

#endif
