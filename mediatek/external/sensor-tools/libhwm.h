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

#ifndef __LIBHWM_H__
#define __LIBHWM_H__
/*---------------------------------------------------------------------------*/
#include <math.h>

#define	GSENSOR_NAME "/dev/gsensor"
#define GSENSOR_ATTR_SELFTEST "/sys/bus/platform/drivers/gsensor/selftest"

#define GYROSCOPE_NAME	"/dev/gyroscope"
/*---------------------------------------------------------------------------*/
// hardware\libhardware\include\hardware\sensors.h
#define LIBHWM_GRAVITY_EARTH            (9.80665f)  
/*---------------------------------------------------------------------------*/
#define LIBHWM_ACC_NVRAM_SENSITIVITY    (65536)  /*16bits : 1g*/
/*---------------------------------------------------------------------------*/
#define LSB_TO_GRA(X)                   ((X*LIBHWM_GRAVITY_EARTH)/LIBHWM_ACC_NVRAM_SENSITIVITY)
#define GRA_TO_LSB(X)                   (round((X*LIBHWM_ACC_NVRAM_SENSITIVITY)/LIBHWM_GRAVITY_EARTH))
/*---------------------------------------------------------------------------*/
#define LIBHWM_INVAL_FD                 (-1)
/*---------------------------------------------------------------------------*/
#define LIBHWM_IS_INVAL_FD(fd)          (fd == LIBHWM_INVAL_FD)
// Gyroscope sensor sensitivity 1000
#define LIBHWM_GYRO_NVRAM_SENSITIVITY	1000
/*---------------------------------------------------------------------------*/ 
#define ABSDIF(X,Y) ((X > Y) ? (Y - X) : (X - Y))
#define ABS(X)      ((X > 0) ? (X) : (-X))
/*---------------------------------------------------------------------------*/
typedef enum {
    HWM_TYPE_NONE = 0,
    HWM_TYPE_ACC = 1,
    HWM_TYPE_MAG = 2,
    HWM_TYPE_PRO = 3,
    HWM_TYPE_LIG = 4,    
} HwmType;
/*---------------------------------------------------------------------------*/
typedef struct {
    HwmType  type;
    char        *ctl;
    char        *dat;
    int         ctl_fd;
    int         dat_fd;    
} HwmDev;
/*---------------------------------------------------------------------------*/
typedef union{
    struct {    /*raw data*/
        int rx;
        int ry;
        int rz;
    };
    struct {    
        float x;
        float y;
        float z;
    };
    struct {
        float azimuth;
        float pitch;
        float roll;
    };    
} HwmData;
/*---------------------------------------------------------------------------*/
typedef struct {
    void *ptr;
    int   len;
} HwmPrivate;
/*---------------------------------------------------------------------------*/
extern int gsensor_calibration(int fd, int period, int count, int tolerance, int trace, HwmData *cali);
extern int gsensor_write_nvram(HwmData *dat);
extern int gsensor_read_nvram(HwmData *dat);
extern int gsensor_rst_cali(int fd);
extern int gsensor_set_cali(int fd, HwmData *dat);
extern int gsensor_get_cali(int fd, HwmData *dat);
extern int gsensor_read(int fd, HwmData *dat); 
extern int gsensor_close(int fd);
extern int gsensor_open(int *fd);
extern int gyroscope_calibration(int fd, int period, int count, int tolerance, int trace, HwmData *cali);
extern int gyroscope_write_nvram(HwmData *dat);
extern int gyroscope_read_nvram(HwmData *dat);
extern int gyroscope_rst_cali(int fd);
extern int gyroscope_set_cali(int fd, HwmData *dat);
extern int gyroscope_get_cali(int fd, HwmData *dat);
extern int gyroscope_read(int fd, HwmData *dat); 
extern int gyroscope_close(int fd);
extern int gyroscope_open(int *fd);


/*---------------------------------------------------------------------------*/
#endif 
