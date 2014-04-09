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
#include <signal.h>
#include <time.h>
#include <unistd.h>
#include <hardware_legacy/power.h>
#include <sys/poll.h>

#include "CFG_BT_File.h"
#include "CFG_BT_Default.h"
#include "bt_drv.h"

#include "os_dep.h"
#include "cust_bt.h"


#ifndef BT_DRV_MOD_NAME
#define BT_DRV_MOD_NAME              "bluetooth"
#endif

#define BTWLANEM_DEVNAME             "/dev/btwlan_em"
#define BTWLAN_EM_IOC_MAGIC          0xf6
#define BT_IOCTL_SET_EINT            _IOWR(BTWLAN_EM_IOC_MAGIC, 2, uint32_t)

#ifdef MTK_COMBO_SUPPORT
#define COMBO_IOC_MAGIC              0xb0
#define COMBO_IOCTL_FW_ASSERT        _IOWR(COMBO_IOC_MAGIC, 0, void*)
#endif

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

/* define uart_t.flags */
#define FLOW_CTL_HW     0x0001
#define FLOW_CTL_SW     0x0002
#define FLOW_CTL_NONE   0x0000
#define FLOW_CTL_MASK   0x0003


//===============        Global Variables         =======================

static int   bt_rfkill_id = -1;
static char *bt_rfkill_state_path = NULL;

#define BT_HOST_SLEEP_TIMEOUT      5 //sec
static timer_t  timerid; // host sleep timer id
static unsigned char wake_lock_acquired = 0;
static unsigned char sleep_mode = 0;

static int eint_fd = -1;
static pthread_t eint_thr;


// mtk bt library
static void *glib_handle = NULL;
typedef int (*INIT)(int fd, struct uart_t *u, struct termios *ti);
typedef int (*UNINIT)(int fd);
typedef int (*WRITE)(int fd, unsigned char *buffer, unsigned long len);
typedef int (*READ)(int fd, unsigned char *buffer, unsigned long len);
typedef int (*NVRAM)(unsigned char *ucNvRamData);


INIT    mtk = NULL;
UNINIT  bt_restore = NULL;
WRITE   write_comm_port = NULL;
READ    read_comm_port = NULL;
WRITE   bt_send_data = NULL;
READ    bt_receive_data = NULL;
NVRAM   bt_read_nvram = NULL;

//===============        F U N C T I O N S      =======================

#ifndef MTK_COMBO_SUPPORT
static int bt_init_rfkill(void) 
{
    char path[128];
    char buf[32];
    int fd, id;
    ssize_t sz;
    
    LOG_TRC();
    
    for (id = 0; id < 10 ; id++) {
        snprintf(path, sizeof(path), "/sys/class/rfkill/rfkill%d/type", id);
        fd = open(path, O_RDONLY);
        if (fd < 0) {
            LOG_ERR("Open %s fails: %s(%d)\n", path, strerror(errno), errno);
            return -1;
        }
        sz = read(fd, &buf, sizeof(buf));
        close(fd);
        if (sz >= (ssize_t)strlen(BT_DRV_MOD_NAME) && 
            memcmp(buf, BT_DRV_MOD_NAME, strlen(BT_DRV_MOD_NAME)) == 0) {
            bt_rfkill_id = id;
            break;
        }
    }

    if (id == 10)
        return -1;

    asprintf(&bt_rfkill_state_path, "/sys/class/rfkill/rfkill%d/state", 
        bt_rfkill_id);
    
    return 0;
}

static int bt_set_power(int on) 
{
    int sz;
    int fd = -1;
    int ret = -1;
    const char buf = (on ? '1' : '0');

    LOG_TRC();

    if (bt_rfkill_id == -1) {
        if (bt_init_rfkill()) goto out;
    }

    fd = open(bt_rfkill_state_path, O_WRONLY);
    if (fd < 0) {
        LOG_ERR("Open %s to set BT power fails: %s(%d)\n", bt_rfkill_state_path,
             strerror(errno), errno);
        goto out;
    }
    
    sz = write(fd, &buf, 1);
    if (sz < 0) {
        LOG_ERR("Write %s fails: %s(%d)\n", bt_rfkill_state_path, 
             strerror(errno), errno);
        goto out;
    }
    ret = 0;

out:
    if (fd >= 0) close(fd);
    return ret;
}


static void bt_host_sleep(int, siginfo_t *, void *);

static int bt_register_timer(void)
{
    struct sigevent sev;
    struct sigaction sa;
    int sig_bt = SIGRTMIN; /* user defined signal area min */
    
    LOG_TRC();
    
    sev.sigev_notify = SIGEV_SIGNAL;
    sev.sigev_signo = sig_bt;
    sev.sigev_value.sival_ptr = &timerid;
    if (timer_create(CLOCK_REALTIME, &sev, &timerid) < 0){
        LOG_ERR("Create timer fails errno %d\n", errno);
        return -1;
    }
    
    sa.sa_sigaction = bt_host_sleep;
    sa.sa_flags = SA_SIGINFO;
    if (sigaction(sig_bt, &sa, NULL) < 0){
        LOG_ERR("Register sigaction fails errno %d\n", errno);
        return -1;
    }
    
    return 0;
}

static void bt_unregister_timer(void)
{
    LOG_TRC();
    signal(SIGRTMIN, SIG_DFL);
    timer_delete(timerid);
}

static void bt_arm_timer(unsigned int sec, unsigned int nsec)
{
    struct itimerspec its;
    
    its.it_value.tv_sec = sec;
    its.it_value.tv_nsec = nsec;
    its.it_interval.tv_sec = 0;
    its.it_interval.tv_nsec = 0;
    
    timer_settime(timerid, 0, &its, NULL);
    
    return;
}

static void maskEint(void)
{
    unsigned long bt_eint = 0;
    if (eint_fd >= 0){
        ioctl(eint_fd, BT_IOCTL_SET_EINT, &bt_eint);
    }
}

static void unmaskEint(void)
{
    unsigned long bt_eint = 1;
    if (eint_fd >= 0){
        ioctl(eint_fd, BT_IOCTL_SET_EINT, &bt_eint);
    }
}

static void bt_hold_wake_lock(int hold)
{
    if (hold == 1){
        // acquire wake lock
        if (!wake_lock_acquired){
            acquire_wake_lock(PARTIAL_WAKE_LOCK, "btdrv");
            wake_lock_acquired = 1;
        }
    }
    else if (hold == 0){
        // release wake lock
        if (wake_lock_acquired){
            release_wake_lock("btdrv");
            wake_lock_acquired = 0;
        }
    }
}

static void bt_host_sleep(int signo, siginfo_t *si, void *uc)
{
    LOG_WAN("%s", __func__);
    
    if ((si->si_code == SI_TIMER) && (si->si_value.sival_ptr == &timerid)){
        // timer expires
        // BT host and controller can enter sleep mode
        bt_hold_wake_lock(0);
        sleep_mode = 1;
    }
    
    return;
}

static int bt_host_wake_up(int bt_fd)
{
    UCHAR  HCI_HOST_WAKE_UP[] = {0x01, 0xC1, 0xFC, 0x0};
    
    LOG_WAN("%s", __func__);
    
    if (!glib_handle){
        LOG_ERR("mtk bt library is unloaded!\n");
        return -1;
    }
    
    bt_hold_wake_lock(1);
    bt_arm_timer(BT_HOST_SLEEP_TIMEOUT, 0);
    
    /* Send 0xFCC1 to indicate host has waken up,
       ready to receive data from controller */
    if (bt_send_data(bt_fd, HCI_HOST_WAKE_UP, sizeof(HCI_HOST_WAKE_UP)) < 0){
        return -1;
    }
    
    bt_arm_timer(BT_HOST_SLEEP_TIMEOUT, 0);
    return 0;
}

static int bt_wake_up_chip(int bt_fd)
{
    UCHAR  bMagicNum = 0xFF;
    UCHAR  pAckEvent[7];
    
    LOG_WAN("%s", __func__);
    
    if (!glib_handle){
        LOG_ERR("mtk bt library is unloaded!\n");
        return -1;
    }
    
    bt_arm_timer(BT_HOST_SLEEP_TIMEOUT, 0);
    if (bt_send_data(bt_fd, &bMagicNum, 1) < 0){
        LOG_ERR("Send wake up chip command fails\n");
        return -1;
    }
    
    bt_arm_timer(BT_HOST_SLEEP_TIMEOUT, 0);
    if (bt_receive_data(bt_fd, pAckEvent, 7) < 0){
        LOG_ERR("Can't receive wake up ack event\n");
        return -1;
    }
    
    /* If the return event is not wake up complete event,
       chip wants to send data to host at the very time, how to handle? */
    if ((pAckEvent[4]!=0xC0) || (pAckEvent[5]!=0xFC)){
        LOG_ERR("The receive event is not 0xFCC0!\n");
    }
    
    bt_arm_timer(BT_HOST_SLEEP_TIMEOUT, 0);
    return 0;
}

static void *bt_eint_monitor(void *ptr)
{
    int ret = 0;
    struct pollfd fds[1];
    int bt_fd;

    LOG_TRC();

    bt_fd = (int)ptr;
    eint_fd = open(BTWLANEM_DEVNAME, O_RDWR | O_NOCTTY);
    if (eint_fd < 0){
        LOG_ERR("Can't get %s fd to handle EINT\n", BTWLANEM_DEVNAME);
        return 0;
    }
    
    unmaskEint();
    
    fds[0].fd = eint_fd;
    fds[0].events = POLLIN;

    while(1) {
        ret = poll(fds, 1, -1);
        if(ret > 0){
            if(fds[0].revents & POLLIN){
                LOG_DBG("EINT arrives! Notify host wake up\n");
                sleep_mode = 0;
                if(bt_host_wake_up(bt_fd) < 0){
                    LOG_ERR("Send host wake up command fails\n");
                }
                unmaskEint();
            }
            else if(fds[0].revents & POLLERR){
                LOG_DBG("EINT monitor needs to exit\n");
                goto exit;
            }
        }
        else if ((ret == -1) && (errno == EINTR)){
            LOG_ERR("poll error EINTR\n");
        }
        else{
            LOG_ERR("poll error %s(%d)!\n", strerror(errno), errno);
            goto exit;
        }
    }
exit:
    close(eint_fd);
    eint_fd = -1;
    return 0;
}
#endif

#ifdef MTK_COMBO_SUPPORT
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
#endif

int mtk_bt_enable(int flag, void *func_cb)
{
    const char *errstr;
    struct uart_t u;
    int bt_fd = -1;
    
    LOG_TRC();

#ifndef MTK_COMBO_SUPPORT
    bt_hold_wake_lock(1);

    /* In case BT is powered on before test */
    bt_set_power(0);
    
    if(bt_set_power(1) < 0) {
        LOG_ERR("BT power on fails\n");
        return -1;
    }
#endif

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

#ifndef MTK_COMBO_SUPPORT
    u.flags &= ~FLOW_CTL_MASK;
    u.flags |= FLOW_CTL_SW;
    u.speed = CUST_BT_BAUD_RATE;
#endif

    bt_fd = mtk(-1, &u, NULL);
    if (bt_fd < 0)
        goto error;

    LOG_DBG("BT is enabled success\n");

#ifndef MTK_COMBO_SUPPORT
    bt_register_timer();
    
    /* Create thread to poll EINT event */
    pthread_create(&eint_thr, NULL, bt_eint_monitor, (void*)bt_fd);
    sched_yield();
#endif

    return bt_fd;

error:
    if (glib_handle){
        dlclose(glib_handle);
        glib_handle = NULL;
    }

#ifndef MTK_COMBO_SUPPORT
    bt_set_power(0);
    bt_hold_wake_lock(0);
#endif

    return -1;
}

int mtk_bt_disable(int bt_fd)
{
    LOG_TRC();

    if (!glib_handle){
        LOG_ERR("mtk bt library is unloaded!\n");
        return -1;
    }

#ifndef MTK_COMBO_SUPPORT
    bt_unregister_timer();
    bt_hold_wake_lock(1);
    
    maskEint();
    /* wait until thread exist */
    pthread_join(eint_thr, NULL);
#endif

    bt_restore(bt_fd);
    dlclose(glib_handle);
    glib_handle = NULL;

#ifndef MTK_COMBO_SUPPORT
    bt_set_power(0);
    bt_hold_wake_lock(0);
    sleep_mode = 0;
#endif

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

#ifndef MTK_COMBO_SUPPORT
    bt_hold_wake_lock(1);
    bt_arm_timer(BT_HOST_SLEEP_TIMEOUT, 0);
    
    if (sleep_mode){
        // controller sleeps, host should wake up chip before write data
        if (bt_wake_up_chip(bt_fd) < 0){
            LOG_ERR("Wake up chip fails\n");
            return -1;
        }
        LOG_DBG("Wake up chip success\n");
        sleep_mode = 0;
    }
#endif

    ret_val = write_comm_port(bt_fd, buffer, len);

#ifndef MTK_COMBO_SUPPORT
    if (ret_val > 0)
        bt_arm_timer(BT_HOST_SLEEP_TIMEOUT, 0);
#endif

#ifdef MTK_COMBO_SUPPORT
    if (ret_val < 0 && (ret_val == -88)){
        // whole chip reset, wait it complete (errno 99)
        wait_whole_chip_reset_complete(bt_fd);
        ret_val = -99;
    }
#endif

    return ret_val;
}

int mtk_bt_read(int bt_fd, unsigned char *buffer, unsigned long len)
{
    int ret_val;
    
    LOG_DBG("buffer %x, len %d\n", buffer, len);

    if (!glib_handle) {
        LOG_ERR("mtk bt library is unloaded!\n");
        return -1;
    }

#ifndef MTK_COMBO_SUPPORT
    bt_hold_wake_lock(1);
    bt_arm_timer(BT_HOST_SLEEP_TIMEOUT, 0);
#endif

    ret_val = read_comm_port(bt_fd, buffer, len);

#ifndef MTK_COMBO_SUPPORT
    if (ret_val > 0)
        bt_arm_timer(BT_HOST_SLEEP_TIMEOUT, 0);
#endif

#ifdef MTK_COMBO_SUPPORT
    if (ret_val < 0 && (ret_val == -88)){
        // whole chip reset, wait it complete (errno 99)
        wait_whole_chip_reset_complete(bt_fd);
        ret_val = -99;
    }
#endif

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
          
          LOG_DBG("BT_COLD_OP_GET_ADDR\n");
          
          glib_handle = dlopen("libbluetooth_mtk.so", RTLD_LAZY);
          if (!glib_handle){
              LOG_ERR("%s\n", dlerror());
              return;
          }
          
          dlerror(); /* Clear any existing error */
          
          bt_read_nvram = dlsym(glib_handle, "bt_read_nvram");
          if ((errstr = dlerror()) != NULL){
              LOG_ERR("Can't find function symbols %s\n", errstr);
              dlclose(glib_handle);
              glib_handle = NULL;
              return;
          }
          
          if (bt_read_nvram(nvram) < 0){
              LOG_ERR("Read Nvram data fails\n");
              dlclose(glib_handle);
              glib_handle = NULL;
              return;
          }
          
          result->status = TRUE;
          if (0 == memcmp(nvram, stBtDefault.addr, 6))
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
          
      #ifdef MTK_COMBO_SUPPORT
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
      #else
          LOG_ERR("Operation not supported!\n");
      #endif
          break;
          
      default:
          LOG_DBG("Unknown operation %d\n", req.op);
          break;
    }
    
    return 0;
}