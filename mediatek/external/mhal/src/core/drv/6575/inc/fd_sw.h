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

#ifndef _APP_FD_SW_PART_H
#define _APP_FD_SW_PART_H

//#define _SVM_LOG_ // On target debug switch
#ifdef WIN32
#define LOGD printf
#endif

#define MAX_FACE_CANDIDATE_NUM_SVM (512) /* HW dependent */
#define MAX_RS_LOOP_NUM (31)         /* HW dependent */
#define FD_SVM_CROP_MAX (32)         /* >(24*1.25)   */

struct fd_post_init_param
{
    int svm_option; // 0:none, 1:single, 2:dual, others: none
};

struct fd_post_param
{
    int real_rs_loop;
    int dest_wd[MAX_RS_LOOP_NUM];
	int dest_ht[MAX_RS_LOOP_NUM];
    unsigned char *pyramid_image_addr[MAX_RS_LOOP_NUM];
};

struct fd_crop_handle
{
    unsigned char *img_crop;
    unsigned char *img_in;
    int in_wd;
    int in_ht;
    int roi_x0;
    int roi_y0;
    int roi_wd;
    int roi_ht;
};

typedef enum 
{
    FDPOST_PRE_MERGE, 
    FDPOST_POST_MERGE,
    FDPOST_MERGE_NUM
} FD_POST_MERGE_OPTION;

typedef enum 
{
    FDPOST_SINGLE_SVM, 
    FDPOST_DUAL_SVM,
    FDPOST_SVM_NUM
} FD_POST_SVM_OPTION;

typedef enum 
{
    FDPOST_SVM_NONE=0, 
    FDPOST_SVM_SINGLE_AFTER_MERGE=1,
    FDPOST_SVM_DUAL_BEFORE_MERGE=2,
    FDPOST_SVM_DUAL_AFTER_MERGE=3,
    FDPOST_SVM_SINGLE_BEFORE_MERGE=4,
    FDPOST_OPTION_NUM
} FD_POST_OPTION;

int FdPostProc(int *p_GFD_NUM, result *p_FD_RESULT, fd_post_param param);
int FdPostInit(fd_post_init_param param);

void Compute_new_cordinate(unsigned char *RGB_Target, unsigned short int *Histogram_Target,  unsigned short int * Histogram_Candidate, unsigned short int width, unsigned short int length, int *Y1_X, int *Y1_Y, float *V1,
								  float axis_X, float axis_Y);
void Compute_Weight_Histogram(unsigned char *RGB_Target, unsigned short int *Histogram, unsigned short int width, unsigned short int length);								  

#endif

