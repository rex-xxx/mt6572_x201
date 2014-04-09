/*
 * Copyright (C) 2011 MediaTek, Inc.
 *
 * Author: Pupa Chen <pupa.chen@mediatek.com>
 *
 * This software is licensed under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation, and
 * may be copied, distributed, and modified under those terms.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

#ifndef __MT_FREQHOPPING_H__
#define __MT_FREQHOPPING_H__

#include "mach/mt_typedefs.h"
#include "mach/mt_fhreg.h"

#define MT_FREQHOP_DEFAULT_ON

#ifdef __MT_FREQHOPPING_C__
#define FREQHOP_EXTERN
#else
#define FREQHOP_EXTERN extern
#endif

#define FHCTL_SUCCESS		0
#define FHCTL_FAIL			1

#define FHDMA_MODE_IDENTICAL	0
#define FHDMA_MODE_SEPARATED	1

#define FHCTL_SM				(1 << 0)
#define FHCTL_SSC				(1 << 1)
#define FHCTL_HOPPING			(1 << 2)
#define FHCTL_SSC_HP_MASK		(0x6)
#define FHCTL_MODE_MASK			(0x7)

#define FHCTL_SR_LSB			0
#define FHCTL_SR_MSB			1

#define FHCTL_NO_PAUSE			0
#define FHCTL_PAUSE				1

#define FHCTLx_EN				0
#define FRDDSx_EN				1 //free-run
#define SFSTRx_EN				2 //soft-start
#define SFSTRx_BP				4
#define FHCTLx_SRHMODE			5
#define FHCTLx_PAUSE			8
#define FRDDSx_DTS				16
#define FRDDSx_DYS				20 

#define SWITCH_FHCTL2PLLCON	0
#define SWITCH_PLLCON2FHCTL	1

#define FREQHOP_PLLID2SRAMOFFSET(pLLID)		(pLLID * SRAM_TABLE_SIZE_BY_PLL)

//- PLL Index
enum FHCTL_PLL_ID
{
    ARMPLL_ID		= 0,
    MAINPLL_ID		= 1,    
    NUM_OF_PLL_ID,
    
    MT658X_FH_ARM_PLL	= ARMPLL_ID,
    MT658X_FH_MAIN_PLL	= MAINPLL_ID,
};

//- default on section

#define FHCTL_ARMPLL_SSC_ON			(1 << ARMPLL_ID)
#define FHCTL_MAINPLL_SSC_ON		(1 << MAINPLL_ID)

enum FHCTL_RF_ID
{
	RF_2G1_ID = 0,
	RF_2G2_ID,
	RF_INTGMD_ID,
	RF_EXTMD_ID,
	RF_BT_ID,
	RF_WF_ID,
	RF_FM_ID,
	NUM_OF_RF_ID
};

#define WORD_L	0
#define WORD_H	1
typedef struct
{
	U64		map64[NUM_OF_PLL_ID];
}FHCTL_dds_dram;


//-Registers Setting
typedef struct 
{
	UINT32	target_vco_freq;
	UINT32	dt;
	UINT32	df;
	UINT32	uplimit_percent_10;
	UINT32	downlimit_percent_10;
	UINT32	uplimit;
	UINT32	downlimit;
	UINT32	dds_val;	
} FREQHOP_PLLSettings;
#define FREQHOP_PLLSETTINGS_MAXNUMBER 10

typedef struct
{
	char	name[32];
	UINT32	addr;
} FREQHOP_REG_MAP;


#ifdef __FHCTL_CTP__
FREQHOP_EXTERN INT32 freqhop_init(UINT32 option);
FREQHOP_EXTERN const char rfid_to_rfname[NUM_OF_RF_ID][4];
FREQHOP_EXTERN const char pllid_to_pllname[NUM_OF_PLL_ID][8];
FREQHOP_EXTERN FREQHOP_PLLSettings freqhop_pll_settings[NUM_OF_PLL_ID][FREQHOP_PLLSETTINGS_MAXNUMBER];

FREQHOP_EXTERN void freqhop_delay_task(UINT32 time_in_ticks);
FREQHOP_EXTERN void freqhop_sram_blkcpy(UINT32 pll_id, UINT32 *pDDS);
FREQHOP_EXTERN void freqhop_rf_src_hopping_enable(UINT32 rf_id, UINT32 enable);
FREQHOP_EXTERN void freqhop_set_priority(UINT32 order, UINT32 order_md);
FREQHOP_EXTERN void freqhop_set_dma_mode(UINT32 mode);
FREQHOP_EXTERN void freqhop_fhctlx_set_DVFS(UINT32 pll_id, UINT32 dds);
FREQHOP_EXTERN void freqhop_rf_src_trigger_ch(UINT32 rf_id, UINT32 channel);
FREQHOP_EXTERN UINT32 freqhop_get_pll_mon_dss(UINT32 pll_id);
FREQHOP_EXTERN UINT32 freqhop_get_pll_fhctlx_dss(UINT32 pll_id);
FREQHOP_EXTERN void freqhop_sram_init(UINT32 *pDDS);
FREQHOP_EXTERN void freqhop_sram_blkcpy(UINT32 pll_id, UINT32 *pDDS);
FREQHOP_EXTERN void freqhop_setbit_FHCTLx_cfg(UINT32 pll_id, UINT32 field, UINT32 mode);
FREQHOP_EXTERN void freqhop_set_fhctlx_updnlmt(UINT32 pll_id, UINT32 uplimit, UINT32 downlimit);
FREQHOP_EXTERN void freqhop_set_fhctlx_slope(UINT32 pll_id, UINT32 dts, UINT32 dys);
#else	//- __FHCTL_CTP__
FREQHOP_EXTERN void mt_freqhopping_init(void);
FREQHOP_EXTERN int freqhopping_config(UINT32 pll_id, UINT32 vco_freq, UINT32 enable);
FREQHOP_EXTERN void mt_fh_popod_save(void);
FREQHOP_EXTERN void mt_fh_popod_restore(void);
FREQHOP_EXTERN void mt_fh_query_SSC_boundary (UINT32 pll_id, UINT32* uplmt_10, UINT32* dnlmt_10);
#endif //- !__FHCTL_CTP__


#endif/* !__MT_FREQHOPPING_H__ */

