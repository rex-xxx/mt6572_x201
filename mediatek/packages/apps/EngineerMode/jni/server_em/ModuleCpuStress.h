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

#ifndef __AFM_MODULE_CPUSTRESS__
#define __AFM_MODULE_CPUSTRESS__

#define ERROR "ERROR"
#define CPUTEST_RESULT_SIZE 1024

#define INDEX_TEST_NEON_0 0
#define INDEX_TEST_NEON_1 1
#define INDEX_TEST_NEON_2 2
#define INDEX_TEST_NEON_3 3
#define INDEX_TEST_CA9_0 4
#define INDEX_TEST_CA9_1 5
#define INDEX_TEST_CA9_2 6
#define INDEX_TEST_CA9_3 7
#define INDEX_TEST_DHRY_0 8
#define INDEX_TEST_DHRY_1 9
#define INDEX_TEST_DHRY_2 10
#define INDEX_TEST_DHRY_3 11
#define INDEX_TEST_MEMCPY_0 12
#define INDEX_TEST_MEMCPY_1 13
#define INDEX_TEST_MEMCPY_2 14
#define INDEX_TEST_MEMCPY_3 15
#define INDEX_TEST_FDCT_0 16
#define INDEX_TEST_FDCT_1 17
#define INDEX_TEST_FDCT_2 18
#define INDEX_TEST_FDCT_3 19
#define INDEX_TEST_IMDCT_0 20
#define INDEX_TEST_IMDCT_1 21
#define INDEX_TEST_IMDCT_2 22
#define INDEX_TEST_IMDCT_3 23


#define INDEX_TEST_BACKUP 20
#define INDEX_TEST_BACKUP_TEST (INDEX_TEST_BACKUP + 1)
#define INDEX_TEST_BACKUP_SINGLE (INDEX_TEST_BACKUP + 2)
#define INDEX_TEST_BACKUP_DUAL (INDEX_TEST_BACKUP + 3)
#define INDEX_TEST_BACKUP_TRIPLE (INDEX_TEST_BACKUP + 4)
#define INDEX_TEST_BACKUP_QUAD (INDEX_TEST_BACKUP + 5)

#define INDEX_TEST_RESTORE 40
#define INDEX_TEST_RESTORE_TEST (INDEX_TEST_RESTORE + 1)
#define INDEX_TEST_RESTORE_SINGLE (INDEX_TEST_RESTORE + 2)
#define INDEX_TEST_RESTORE_DUAL (INDEX_TEST_RESTORE + 3)
#define INDEX_TEST_RESTORE_TRIPLE (INDEX_TEST_RESTORE + 4)
#define INDEX_TEST_RESTORE_QUAD (INDEX_TEST_RESTORE + 5)

#if defined(MT6589) || defined(MT6572)
#define FILE_NEON_0 "/sys/bus/platform/drivers/slt_cpu0_vfp/slt_cpu0_vfp"
#define FILE_NEON_1 "/sys/bus/platform/drivers/slt_cpu1_vfp/slt_cpu1_vfp"
#define FILE_NEON_2 "/sys/bus/platform/drivers/slt_cpu2_vfp/slt_cpu2_vfp"
#define FILE_NEON_3 "/sys/bus/platform/drivers/slt_cpu3_vfp/slt_cpu3_vfp"
#else
#define FILE_NEON_0 "/sys/bus/platform/drivers/cpu0_ca9_neon/cpu0_slt_ca9_neon"
#define FILE_NEON_1 "/sys/bus/platform/drivers/cpu1_ca9_neon/cpu1_slt_ca9_neon"
#define FILE_NEON_2 "/sys/bus/platform/drivers/cpu2_ca9_neon/cpu2_slt_ca9_neon"
#define FILE_NEON_3 "/sys/bus/platform/drivers/cpu3_ca9_neon/cpu3_slt_ca9_neon"
#endif
#if defined(MT6589) || defined(MT6572)
#define FILE_CA9_0 "/sys/bus/platform/drivers/slt_cpu0_maxpower/slt_cpu0_maxpower"
#define FILE_CA9_1 "/sys/bus/platform/drivers/slt_cpu1_maxpower/slt_cpu1_maxpower"
#define FILE_CA9_2 "/sys/bus/platform/drivers/slt_cpu2_maxpower/slt_cpu2_maxpower"
#define FILE_CA9_3 "/sys/bus/platform/drivers/slt_cpu3_maxpower/slt_cpu3_maxpower"
#else
#define FILE_CA9_0 "/sys/bus/platform/drivers/cpu0_ca9_max_power/cpu0_slt_ca9_max_power"
#define FILE_CA9_1 "/sys/bus/platform/drivers/cpu1_ca9_max_power/cpu1_slt_ca9_max_power"
#define FILE_CA9_2 "/sys/bus/platform/drivers/cpu2_ca9_max_power/cpu2_slt_ca9_max_power"
#define FILE_CA9_3 "/sys/bus/platform/drivers/cpu3_ca9_max_power/cpu3_slt_ca9_max_power"
#endif

#define FILE_DHRY_0 "/sys/bus/platform/drivers/slt_cpu0_dhry/slt_cpu0_dhry"
#define FILE_DHRY_1 "/sys/bus/platform/drivers/slt_cpu1_dhry/slt_cpu1_dhry"
#define FILE_DHRY_2 "/sys/bus/platform/drivers/slt_cpu2_dhry/slt_cpu2_dhry"
#define FILE_DHRY_3 "/sys/bus/platform/drivers/slt_cpu3_dhry/slt_cpu3_dhry"

#define FILE_MEMCPY_0 "/sys/bus/platform/drivers/slt_cpu0_memcpyL2/slt_cpu0_memcpyL2"
#define FILE_MEMCPY_1 "/sys/bus/platform/drivers/slt_cpu1_memcpyL2/slt_cpu1_memcpyL2"
#define FILE_MEMCPY_2 "/sys/bus/platform/drivers/slt_cpu2_memcpyL2/slt_cpu2_memcpyL2"
#define FILE_MEMCPY_3 "/sys/bus/platform/drivers/slt_cpu3_memcpyL2/slt_cpu3_memcpyL2"

#define FILE_FDCT_0 "/sys/bus/platform/drivers/slt_cpu0_fdct/slt_cpu0_fdct"
#define FILE_FDCT_1 "/sys/bus/platform/drivers/slt_cpu1_fdct/slt_cpu1_fdct"
#define FILE_FDCT_2 "/sys/bus/platform/drivers/slt_cpu2_fdct/slt_cpu2_fdct"
#define FILE_FDCT_3 "/sys/bus/platform/drivers/slt_cpu3_fdct/slt_cpu3_fdct"

#define FILE_IMDCT_0 "/sys/bus/platform/drivers/slt_cpu0_imdct/slt_cpu0_imdct"
#define FILE_IMDCT_1 "/sys/bus/platform/drivers/slt_cpu1_imdct/slt_cpu1_imdct"
#define FILE_IMDCT_2 "/sys/bus/platform/drivers/slt_cpu2_imdct/slt_cpu2_imdct"
#define FILE_IMDCT_3 "/sys/bus/platform/drivers/slt_cpu3_imdct/slt_cpu3_imdct"

#define FILE_CPU0_SCAL "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
#define FILE_CPU1_SCAL "/sys/devices/system/cpu/cpu1/cpufreq/scaling_governor"
#define FILE_CPU2_SCAL "/sys/devices/system/cpu/cpu2/cpufreq/scaling_governor"
#define FILE_CPU3_SCAL "/sys/devices/system/cpu/cpu3/cpufreq/scaling_governor"
#define FILE_CPU0_ONLINE "/sys/devices/system/cpu/cpu0/online"
#define FILE_CPU1_ONLINE "/sys/devices/system/cpu/cpu1/online"
#define FILE_CPU2_ONLINE "/sys/devices/system/cpu/cpu2/online"
#define FILE_CPU3_ONLINE "/sys/devices/system/cpu/cpu3/online"

#define FILE_HOTPLUG "/proc/mtk_hotplug/enable"

#define INDEX_SWCODEC_TEST_SINGLE 0
#define INDEX_SWCODEC_TEST_DUAL 1
#define INDEX_SWCODEC_TEST_TRIPLE 2
#define INDEX_SWCODEC_TEST_QUAD 3

#if defined(MT6575)
#define COMMAND_SWCODEC_TEST_SINGLE "/data/mfv_ut_75 EM 1"
#define COMMAND_SWCODEC_TEST_DUAL_0 ""
#define COMMAND_SWCODEC_TEST_DUAL_1 ""
#elif defined(MT6577)
#define COMMAND_SWCODEC_TEST_SINGLE "/data/mfv_ut_77_cpu0 EM 1"
#define COMMAND_SWCODEC_TEST_DUAL_0 "/data/mfv_ut_77_cpu0 EM 1"
#define COMMAND_SWCODEC_TEST_DUAL_1 "/data/mfv_ut_77_cpu1 EM 1"
#else
#define COMMAND_SWCODEC_TEST_SINGLE "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 0"
#define COMMAND_SWCODEC_TEST_DUAL_0 "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 0"
#define COMMAND_SWCODEC_TEST_DUAL_1 "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 1"
#endif

#define COMMAND_SWCODEC_TEST_TRIPLE_0 "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 0"
#define COMMAND_SWCODEC_TEST_TRIPLE_1 "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 1"
#define COMMAND_SWCODEC_TEST_TRIPLE_2 "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 2"
#define COMMAND_SWCODEC_TEST_QUAD_0 "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 0"
#define COMMAND_SWCODEC_TEST_QUAD_1 "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 1"
#define COMMAND_SWCODEC_TEST_QUAD_2 "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 2"
#define COMMAND_SWCODEC_TEST_QUAD_3 "/system/bin/mp4dec_ut_stress /data/352x288_30_512.cmp 1 3"

#define COMMAND_HOTPLUG_DISABLE "echo 0 > /sys/module/mt_hotplug_mechanism/parameters/g_enable"
#define COMMAND_HOTPLUG_ENABLE "echo 1 > /sys/module/mt_hotplug_mechanism/parameters/g_enable"


#define INDEX_THERMAL_DISABLE 0
#define INDEX_THERMAL_ENABLE 1
#define THERMAL_DISABLE_COMMAND "/system/bin/thermal_manager /etc/.tp/.ht120.mtc"
#define THERMAL_ENABLE_COMMAND "/system/bin/thermal_manager /etc/.tp/thermal.conf"

struct thread_params_t {
	char file[CPUTEST_RESULT_SIZE >> 1];
	char result[CPUTEST_RESULT_SIZE >> 2];
};

struct thread_status_t {
	pthread_t pid;
	int create_result;
	struct thread_params_t param;
};

static char backup_first[CPUTEST_RESULT_SIZE>>1] = {0};
static char backup_second[CPUTEST_RESULT_SIZE>>1] = {0};
static char backup_third[CPUTEST_RESULT_SIZE>>1] = {0};
static char backup_fourth[CPUTEST_RESULT_SIZE>>1] = {0};
static pthread_mutex_t lock;

class RPCClient;

class ModuleCpuStress {
public:
	ModuleCpuStress();
	virtual ~ModuleCpuStress();
	static int ApMcu(RPCClient* msgSender);
	static int SwCodec(RPCClient* msgSender);
	static int BackupRestore(RPCClient* msgSender);
	static int ThermalUpdate(RPCClient* msgSender);
};

#endif	
