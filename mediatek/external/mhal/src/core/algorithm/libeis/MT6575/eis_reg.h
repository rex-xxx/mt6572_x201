/********************************************************************************************
 *     LEGAL DISCLAIMER
 *
 *     (Header of MediaTek Software/Firmware Release or Documentation)
 *
 *     BY OPENING OR USING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 *     THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE") RECEIVED
 *     FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON AN "AS-IS" BASIS
 *     ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES, EXPRESS OR IMPLIED,
 *     INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
 *     A PARTICULAR PURPOSE OR NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY
 *     WHATSOEVER WITH RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 *     INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK
 *     ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
 *     NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S SPECIFICATION
 *     OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
 *
 *     BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE LIABILITY WITH
 *     RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION,
TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE
 *     FEES OR SERVICE CHARGE PAID BY BUYER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 *     THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE WITH THE LAWS
 *     OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF LAWS PRINCIPLES.
 ************************************************************************************************/
#ifndef _EIS_REG_H_
#define _EIS_REG_H_

/*******************************************************************************
*
********************************************************************************/
#define EIS_BITS(RegBase, RegName, FieldName)  (RegBase->RegName.u.bits.FieldName)
#define EIS_REG(RegBase, RegName) (RegBase->RegName.u.reg)
//
#define EIS_BASE_HW     0xC2096000
#define EIS_BASE_RANGE  0x002C
//

#define EIS_WIN_NUM 16

#define FALSE 0
#define TRUE 1
#define EIS_NO_ERROR 0
#define EIS_INVALID_DRIVER 1

typedef void                MVOID;
typedef int                 MBOOL;
typedef signed char         MINT8;
typedef signed short        MINT16;
typedef signed int          MINT32;
typedef unsigned char       MUINT8;
typedef unsigned short      MUINT16;
typedef unsigned int        MUINT32;
typedef unsigned long long  MUINT64;

/*******************************************************************************
*
********************************************************************************/
//
typedef volatile struct EIS_0000 {
    union {
        struct {
            MUINT32 PREP_FIR_H       : 1;  //[0]
            MUINT32 PREP_GAIN_FIR_H  : 1;  //[1]
            MUINT32 PREP_DS_IIR_H    : 1;  //[2]
            MUINT32 PREP_GAIN_H      : 1;  //[3]
            MUINT32 PREP_DS_IIR_V    : 2;  //[4:5]
            MUINT32 PREP_GAIN_IIR_V  : 2;  //[6:7]
            MUINT32 ME_NUM_HRP       : 4;  //[8:11]
            MUINT32 ME_AD_CLIP       : 4;  //[12:15]
            MUINT32 ME_AD_KNEE       : 4;  //[16:19]
            MUINT32 EIS_SRAM_RB      : 1;  //[20]
            MUINT32 EIS_SRAM_PD      : 1;  //[21]
            MUINT32 RESERVED         : 8;  //[22:29]
            MUINT32 EIS_SRC_SEL      : 1;  //[30]
            MUINT32 CE               : 1;  //[31]
        } bits;
        MUINT32 reg;
    } u;
} eis_prep_me_ctrl_t;
//
typedef volatile struct EIS_0004 {
    union {
        struct {
            MUINT32 LMV_TH_Y_SURROUND : 8; //[0:7]
            MUINT32 LMV_TH_Y_CENTER   : 8; //[8:15]
            MUINT32 LMV_TH_X_SURROUND : 8; //[16:23]
            MUINT32 LMV_TH_X_CENTER   : 8; //[24:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_lmv_th_t;
//
typedef volatile struct EIS_0008 {
    union {
        struct {
            MUINT32 FL_OFFSET_V      : 12; //[0:11]
            MUINT32 RESERVED0        : 4;  //[12:15]
            MUINT32 FL_OFFSET_H      : 12; //[16:27]
            MUINT32 RESERVED1        : 4;  //[28:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_fl_offset_t;
//
typedef volatile struct EIS_000C {
    union {
        struct {
            MUINT32 MB_OFFSET_V      : 12; //[0:11]
            MUINT32 RESERVED0        : 4;  //[12:15]
            MUINT32 MB_OFFSET_H      : 12; //[16:27]
            MUINT32 RESERVED1        : 4;  //[28:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_mb_offset_t;
//
typedef volatile struct EIS_0010 {
    union {
        struct {
            MUINT32 MB_INTERVAL_V    : 12; //[0:11]
            MUINT32 RESERVED0        : 4;  //[12:15]
            MUINT32 MB_INTERVAL_H    : 12; //[16:27]
            MUINT32 RESERVED1        : 4;  //[28:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_mb_interval_t;
//
typedef volatile struct EIS_0014 {
    union {
        struct {
            MUINT32 GMV_Y            : 12; //[0:11]
            MUINT32 RESERVED0        : 4;  //[12:15]
            MUINT32 GMV_X            : 12; //[16:27]
            MUINT32 RESERVED1        : 4;  //[28:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_gmv_t;
//
typedef volatile struct EIS_0018 {
    union {
        struct {
            MUINT32 RESERVED           : 32;  //[0:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_dbg_out_t;
//
typedef volatile struct EIS_001C {
    union {
        struct {
            MUINT32 EIS_DBG_SEL      : 5;  //[0:4]
            MUINT32 RESERVED0        : 3;  //[5:7]
            MUINT32 SRAM_DELSEL      : 2;  //[8:9]
            MUINT32 RESERVED1        : 6;  //[10:15]
            MUINT32 EIS_SWRST_B      : 1;  //[16]
            MUINT32 RESERVED2        : 3;  //[17:19]
            MUINT32 EIS_IRQ          : 1;  //[20]
            MUINT32 EIS_IRQ_ENABLE   : 1;  //[21]
            MUINT32 RESERVED3        : 2;  //[22:23]
            MUINT32 EIS_IRQ_WR_CLR   : 1;  //[24]
            MUINT32 RESERVED4        : 7;  //[25:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_dbg_ctrl_t;
//
typedef volatile struct EIS_0020 {
    union {
        struct {
            MUINT32 RESERVED           : 32;  //[0:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_reserved0_t;
//
typedef volatile struct EIS_0024 {
    union {
        struct {
            MUINT32 RESERVED           : 32;  //[0:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_reserved1_t;
//
typedef volatile struct EIS_0028 {
    union {
        struct {
            MUINT32 ADDR               : 32;  //[0:31]
        } bits;
        MUINT32 reg;
    } u;
} eis_base_addr_t;


//
typedef volatile struct _eis_reg_t {
    eis_prep_me_ctrl_t      EIS_PREP_ME_CTRL;   // 0x00
    eis_lmv_th_t            EIS_LMV_TH;
    eis_fl_offset_t         EIS_FL_OFFSET;
    eis_mb_offset_t         EIS_MB_OFFSET;
    eis_mb_interval_t       EIS_MB_INTERVAL;    // 0x10
    eis_gmv_t               EIS_GMV;
    eis_dbg_out_t           EIS_DBG_OUT;
    eis_dbg_ctrl_t          EIS_DBG_CTRL;
    eis_reserved0_t         EIS_RESERVED0;      // 0x20
    eis_reserved1_t         EIS_RESERVED1;
    eis_base_addr_t         EIS_BASE_ADDR;
    
} eis_reg_t;

#define LMV_DIGIT 12
#define LMV2_DIGIT 4

typedef struct EIS_Stat_MB {
    union {
        struct {
            MUINT64 LMV_X_2ND        : 4;  //[0:3]
            MUINT64 LMV_Y_2ND        : 4;  //[4:7]
            MUINT64 SAD_2ND          : 9;  //[8:16]
            MUINT64 TRUST_X          : 7;  //[17:23]
            MUINT64 TRUST_Y          : 7;  //[24:30]
            MUINT64 LMV_X            : 12; //[31:42]
            MUINT64 LMV_Y            : 12; //[43:54]
            MUINT64 SAD              : 9;  //[55:63]
        } bits;
        MUINT64 reg;
    } u;
} eis_stat_mb_t;

typedef struct EIS_Stat_AvgSad0 {
    union {
        struct {
            MUINT64 AVGSAD12         : 9;  //[0:8]
            MUINT64 AVGSAD11         : 9;  //[9:17]
            MUINT64 AVGSAD10         : 9;  //[18:26]
            MUINT64 AVGSAD03         : 9;  //[27:35]
            MUINT64 AVGSAD02         : 9;  //[36:44]
            MUINT64 AVGSAD01         : 9;  //[45:53]
            MUINT64 AVGSAD00         : 9;  //[54:62]
            MUINT64 RESERVED         : 1;  //[63]
        } bits;
        MUINT64 reg;
    } u;
} eis_stat_avgsad0_t;

typedef struct EIS_Stat_AvgSad1 {
    union {
        struct {
            MUINT64 AVGSAD31         : 9;  //[0:8]
            MUINT64 AVGSAD30         : 9;  //[9:17]
            MUINT64 AVGSAD23         : 9;  //[18:26]
            MUINT64 AVGSAD22         : 9;  //[27:35]
            MUINT64 AVGSAD21         : 9;  //[36:44]
            MUINT64 AVGSAD20         : 9;  //[45:53]
            MUINT64 AVGSAD13         : 9;  //[54:62]
            MUINT64 RESERVED         : 1;  //[63]
        } bits;
        MUINT64 reg;
    } u;
} eis_stat_avgsad1_t;

typedef struct EIS_Stat_AvgSad2 {
    union {
        struct {
            MUINT64 AVGSAD33         : 9;  //[0:8]
            MUINT64 AVGSAD32         : 9;  //[9:17]
            MUINT64 RESERVED0        : 14; //[18:31]
            MUINT64 GMV_X            : 12; //[32:43]
            MUINT64 RESERVED1        : 4;  //[44:47]            
            MUINT64 GMV_Y            : 12; //[48:59]
            MUINT64 RESERVED2        : 4;  //[60:63]            
        } bits;
        MUINT64 reg;
    } u;
} eis_stat_avgsad2_t;

typedef struct EIS_Stat_NewTrust {
    union {
        struct {
            MUINT64 NEW_TRUST_Y0     : 7;  //[0:6]
            MUINT64 RESERVED0        : 1;  //[7]            
            MUINT64 NEW_TRUST_X0     : 7;  //[8:14]
            MUINT64 RESERVED1        : 1;  //[15]            
            MUINT64 NEW_TRUST_Y1     : 7;  //[16:22]
            MUINT64 RESERVED2        : 1;  //[23]                        
            MUINT64 NEW_TRUST_X1     : 7;  //[24:30]
            MUINT64 RESERVED3        : 1;  //[31]                        
            MUINT64 NEW_TRUST_Y2     : 7;  //[32:38]            
            MUINT64 RESERVED4        : 1;  //[39]                        
            MUINT64 NEW_TRUST_X2     : 7;  //[40:46]
            MUINT64 RESERVED5        : 1;  //[47]                        
            MUINT64 NEW_TRUST_Y3     : 7;  //[48:54]     
            MUINT64 RESERVED6        : 1;  //[55]                        
            MUINT64 NEW_TRUST_X3     : 7;  //[56:62]
            MUINT64 RESERVED7        : 1;  //[63]                        
        } bits;
        MUINT64 reg;
    } u;
} eis_stat_newtrust_t;

typedef struct _eis_ori_stat_t {

    eis_stat_mb_t         EIS_STAT_MB[EIS_WIN_NUM];
    eis_stat_avgsad0_t    EIS_STAT_AVGSAD0;
    eis_stat_avgsad1_t    EIS_STAT_AVGSAD1;
    eis_stat_avgsad2_t    EIS_STAT_AVGSAD2;
    eis_stat_newtrust_t   EIS_STAT_NEWTRUST[4];
    
} eis_ori_stat_t;

typedef struct _eis_stat_t {

    MINT32 i4LMV_X[EIS_WIN_NUM];
    MINT32 i4LMV_Y[EIS_WIN_NUM];

    MINT32 i4LMV_X2[EIS_WIN_NUM];
    MINT32 i4LMV_Y2[EIS_WIN_NUM];

    MINT32 i4Trust_X[EIS_WIN_NUM];
    MINT32 i4Trust_Y[EIS_WIN_NUM];

    MINT32 i4NewTrust_X[EIS_WIN_NUM];
    MINT32 i4NewTrust_Y[EIS_WIN_NUM];

    MINT32 i4SAD[EIS_WIN_NUM];
    MINT32 i4SAD2[EIS_WIN_NUM];    
    MINT32 i4AVG[EIS_WIN_NUM];
    
} eis_stat_t;

#endif // _EIS_REG_H_

