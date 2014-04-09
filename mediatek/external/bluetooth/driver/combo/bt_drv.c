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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <termios.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <unistd.h>

#include "CFG_BT_File.h"
#include "CFG_BT_Default.h"
#include "bt_drv.h"
#include "os_dep.h"


#define COMBO_IOC_MAGIC              0xb0
#define COMBO_IOCTL_FW_ASSERT        _IOWR(COMBO_IOC_MAGIC, 0, void*)

struct uart_t {
    char *type;
    int  m_id;
    int  p_id;
    int  proto;
    int  init_speed;
    int  speed;
    int  flags;
    int  pm;
    char *bdaddr;
    int  (*init) (int fd, struct uart_t *u, struct termios *ti);
    int  (*post) (int fd, struct uart_t *u, struct termios *ti);
};

//===============        Global Variables         =======================

// mtk bt library
static void *glib_handle = NULL;
typedef int (*INIT)(int fd, struct uart_t *u, struct termios *ti);
typedef int (*UNINIT)(int fd);
typedef int (*WRITE)(int fd, unsigned char *buffer, unsigned long len);
typedef int (*READ)(int fd, unsigned char *buffer, unsigned long len);
typedef int (*NVRAM)(unsigned char *ucNvRamData);
typedef int (*GETID)(int *pChipId);


INIT    mtk = NULL;
UNINIT  bt_restore = NULL;
WRITE   write_comm_port = NULL;
READ    read_comm_port = NULL;
WRITE   bt_send_data = NULL;
READ    bt_receive_data = NULL;
NVRAM   bt_read_nvram = NULL;
GETID   bt_get_combo_id = NULL;

//===============        F U N C T I O N S      =======================

static void wait_whole_chip_reset_complete(int bt_fd)
{
    UCHAR temp;
    int   res;
    
    do {
        res = read(bt_fd, &temp, 1);
        if (res < 0){
            if (errno == 88)
                usleep(100000);
            else if (errno == 99)
                break;
            else if (errno != EINTR && errno != EAGAIN)
                break;
        }
        else{
            break; // impossible case
        }
	} while(1);
}

int mtk_bt_enable(int flag, void *func_cb)
{
    const char *errstr;
    struct uart_t u;
    int bt_fd = -1;
    
    LOG_TRC();
    
    glib_handle = dlopen("libbluetooth_mtk.so", RTLD_LAZY);
    if (!glib_handle){
        LOG_ERR("%s\n", dlerror());
        goto error;
    }
    
    dlerror(); /* Clear any existing error */
    
    mtk = dlsym(glib_handle, "mtk");
    bt_restore = dlsym(glib_handle, "bt_restore");
    write_comm_port = dlsym(glib_handle, "write_comm_port");
    read_comm_port = dlsym(glib_handle, "read_comm_port");
    bt_send_data = dlsym(glib_handle, "bt_send_data");
    bt_receive_data = dlsym(glib_handle, "bt_receive_data");

    if ((errstr = dlerror()) != NULL){
        LOG_ERR("Can't find function symbols %s\n", errstr);
        goto error;
    }
    
    bt_fd = mtk(-1, &u, NULL);
    if (bt_fd < 0)
        goto error;

    LOG_DBG("BT is enabled success\n");
    
    return bt_fd;

error:
    if (glib_handle){
        dlclose(glib_handle);
        glib_handle = NULL;
    }
    return -1;
}

int mtk_bt_disable(int bt_fd)
{
    LOG_TRC();

    if (!glib_handle){
        LOG_ERR("mtk bt library is unloaded!\n");
        return -1;
    }
    
    bt_restore(bt_fd);
    dlclose(glib_handle);
    glib_handle = NULL;
    
    return 0;
}

int mtk_bt_write(int bt_fd, unsigned char *buffer, unsigned long len)
{
    int ret_val;
    
    LOG_DBG("buffer %x, len %d\n", buffer, len);

    if (!glib_handle){
        LOG_ERR("mtk bt library is unloaded!\n");
        return -1;
    }
    
    ret_val = write_comm_port(bt_fd, buffer, len);
    
    if (ret_val < 0 && (ret_val == -88)){
        // whole chip reset, wait it complete (errno 99)
        wait_whole_chip_reset_complete(bt_fd);
        ret_val = -99;
    }
    
    return ret_val;
}

int mtk_bt_read(int bt_fd, unsigned char *buffer, unsigned long len)
{
    int ret_val;
    
    LOG_DBG("buffer %x, len %d\n", buffer, len);

    if (!glib_handle){
        LOG_ERR("mtk bt library is unloaded!\n");
        return -1;
    }
    
    ret_val = read_comm_port(bt_fd, buffer, len);
    
    if (ret_val < 0 && (ret_val == -88)){
        // whole chip reset, wait it complete (errno 99)
        wait_whole_chip_reset_complete(bt_fd);
        ret_val = -99;
    }
    
    return ret_val;	
}

void mtk_bt_op(BT_REQ req, BT_RESULT *result)
{    
    result->status = FALSE;
    
    switch(req.op)
    {
      case BT_COLD_OP_GET_ADDR:
      {
          const char *errstr;
          unsigned char nvram[sizeof(ap_nvram_btradio_mt6610_struct)];
          unsigned char ucDefaultAddr[6] = {0};
          int chipId;
          
          LOG_DBG("BT_COLD_OP_GET_ADDR\n");
          
          glib_handle = dlopen("libbluetooth_mtk.so", RTLD_LAZY);
          if (!glib_handle){
              LOG_ERR("%s\n", dlerror());
              return;
          }
          
          dlerror(); /* Clear any existing error */
          
          bt_read_nvram = dlsym(glib_handle, "bt_read_nvram");
          bt_get_combo_id = dlsym(glib_handle, "bt_get_combo_id");
          
          if ((errstr = dlerror()) != NULL){
              LOG_ERR("Can't find function symbols %s\n", errstr);
              dlclose(glib_handle);
              glib_handle = NULL;
              return;
          }
          
          if(bt_read_nvram(nvram) < 0){
              LOG_ERR("Read Nvram data fails\n");
              dlclose(glib_handle);
              glib_handle = NULL;
              return;
          }
          
          /* Get combo chip id */
          if(bt_get_combo_id(&chipId) < 0){
              LOG_ERR("Unknown combo chip id\n");
              dlclose(glib_handle);
              glib_handle = NULL;
              return;
          }
          
        #ifdef MTK_MT6620
          if(chipId == 0x6620){
              memcpy(ucDefaultAddr, stBtDefault_6620.addr, 6);
          }
        #endif

        #ifdef MTK_MT6628
          if(chipId == 0x6628){
              memcpy(ucDefaultAddr, stBtDefault_6628.addr, 6);
          }
        #endif

        #ifdef MTK_CONSYS_MT6572
          if(chipId == 0x6572){
              memcpy(ucDefaultAddr, stBtDefault_6572.addr, 6);
          }
        #endif

          result->status = TRUE;
          if (0 == memcmp(nvram, ucDefaultAddr, 6))
          {
              LOG_DBG("Nvram BD address default value\n");
              result->param.addr[0] = 0;  //default address
              memcpy(&result->param.addr[1], nvram, 6);
          }
          else {
              LOG_DBG("Nvram BD address has valid value\n");
              result->param.addr[0] = 1;  //valid address
              memcpy(&result->param.addr[1], nvram, 6);
          }
          
          dlclose(glib_handle);
          glib_handle = NULL;
          break;
      }
      case BT_HOT_OP_SET_FWASSERT:
          LOG_DBG("BT_HOT_OP_SET_FWASSERT\n");
          
          // req.param.fd should be the fd returned by mtk_bt_enable
          if (req.param.fd < 0){
              LOG_ERR("Invalid bt fd!\n");
              return;
          }
          
          if (ioctl(req.param.fd, COMBO_IOCTL_FW_ASSERT, NULL) < 0){
              LOG_ERR("Set COMBO FW ASSERT fails\n");
              return;
          }
          
          result->status = TRUE;
          break;
          
      default:
          LOG_DBG("Unknown operation %d\n", req.op);
          break;
    }
    
    return 0;
}
