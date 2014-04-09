/*! \file
    \brief  Declaration of library functions

    Any definitions in this file will be shared among GLUE Layer and internal Driver Stack.
*/



#ifndef _WMT_PLAT_H_
#define _WMT_PLAT_H_
#include "osal_typedef.h"
#include "osal.h"
#include <mach/mtk_wcn_cmb_stub.h>
#include "mtk_wcn_consys_hw.h"


/*******************************************************************************
*                         C O M P I L E R   F L A G S
********************************************************************************
*/

/*******************************************************************************
*                                 M A C R O S
********************************************************************************
*/

#if 1 /* moved from wmt_exp.h */
#ifndef DFT_TAG
#define DFT_TAG         "[WMT-DFT]"
#endif

#define WMT_LOUD_FUNC(fmt, arg...)   if (gWmtDbgLvl >= WMT_LOG_LOUD) { osal_dbg_print(DFT_TAG "[L]%s:"  fmt, __FUNCTION__ ,##arg);}
#define WMT_INFO_FUNC(fmt, arg...)   if (gWmtDbgLvl >= WMT_LOG_INFO) { osal_info_print(DFT_TAG "[I]%s:"  fmt, __FUNCTION__ ,##arg);}
#define WMT_WARN_FUNC(fmt, arg...)   if (gWmtDbgLvl >= WMT_LOG_WARN) { osal_warn_print(DFT_TAG "[W]%s:"  fmt, __FUNCTION__ ,##arg);}
#define WMT_ERR_FUNC(fmt, arg...)    if (gWmtDbgLvl >= WMT_LOG_ERR) { osal_err_print(DFT_TAG "[E]%s(%d):"  fmt, __FUNCTION__ , __LINE__, ##arg);}
#define WMT_DBG_FUNC(fmt, arg...)    if (gWmtDbgLvl >= WMT_LOG_DBG) { osal_dbg_print(DFT_TAG "[D]%s:"  fmt, __FUNCTION__ ,##arg);}
#define WMT_TRC_FUNC(f)              if (gWmtDbgLvl >= WMT_LOG_DBG) { osal_dbg_print(DFT_TAG "<%s> <%d>\n", __FUNCTION__, __LINE__);}
#endif

#if 1 /* moved from wmt_exp.h */
#define WMT_LOG_LOUD    4
#define WMT_LOG_DBG     3
#define WMT_LOG_INFO    2
#define WMT_LOG_WARN    1
#define WMT_LOG_ERR     0
#endif


#define CFG_WMT_PS_SUPPORT 1 /* moved from wmt_exp.h */
/*******************************************************************************
*                              C O N S T A N T S
********************************************************************************
*/

#if 0 /* [GeorgeKuo] remove COMBO_AUDIO FLAG */
#define COMBO_AUDIO_BT_MASK (0x1UL)
#define COMBO_AUDIO_BT_PCM_ON (0x1UL << 0)
#define COMBO_AUDIO_BT_PCM_OFF (0x0UL << 0)

#define COMBO_AUDIO_FM_MASK (0x2UL)
#define COMBO_AUDIO_FM_LINEIN (0x0UL << 1)
#define COMBO_AUDIO_FM_I2S (0x1UL << 1)

#define COMBO_AUDIO_PIN_MASK     (0x4UL)
#define COMBO_AUDIO_PIN_SHARE    (0x1UL << 2)
#define COMBO_AUDIO_PIN_SEPARATE (0x0UL << 2)
#endif

/*******************************************************************************
*                             D A T A   T Y P E S
********************************************************************************
*/

typedef enum _ENUM_FUNC_STATE_{
    FUNC_ON = 0,
    FUNC_OFF = 1,
    FUNC_RST = 2,
    FUNC_STAT = 3,
    FUNC_CTRL_MAX,
} ENUM_FUNC_STATE, *P_ENUM_FUNC_STATE;

typedef enum _ENUM_PIN_ID_{
    PIN_BGF_EINT = 0,
    PIN_I2S_GRP = 1,
    PIN_GPS_SYNC = 2,
    PIN_GPS_LNA = 3,
    PIN_ID_MAX
} ENUM_PIN_ID, *P_ENUM_PIN_ID;

typedef enum _ENUM_PIN_STATE_{
    PIN_STA_INIT = 0,
    PIN_STA_OUT_L = 1,
    PIN_STA_OUT_H = 2,
    PIN_STA_IN_L = 3,
    PIN_STA_MUX = 4,
    PIN_STA_EINT_EN = 5,
    PIN_STA_EINT_DIS = 6,
    PIN_STA_DEINIT = 7,
    PIN_STA_SHOW = 8,
    PIN_STA_MAX
} ENUM_PIN_STATE, *P_ENUM_PIN_STATE;

typedef enum _CMB_IF_TYPE_{
    CMB_IF_UART = 0,
    CMB_IF_WIFI_SDIO = 1,
    CMB_IF_BGF_SDIO = 2,
    CMB_IF_BGWF_SDIO = 3,
    CMB_IF_TYPE_MAX
} CMB_IF_TYPE, *P_CMB_IF_TYPE;

typedef INT32 (*fp_set_pin)(ENUM_PIN_STATE);

typedef enum _ENUM_WL_OP_{
    WL_OP_GET = 0,
    WL_OP_PUT = 1,
    WL_OP_MAX
} ENUM_WL_OP, *P_ENUM_WL_OP;

typedef enum _ENUM_PALDO_TYPE_{
	BT_PALDO = 0,
	WIFI_PALDO = 1,
	FM_PALDO = 2,
	PALDO_TYPE_MAX
}ENUM_PALDO_TYPE,*P_ENUM_PALDO_TYPE;

typedef enum _ENUM_PALDO_OP_{
	PALDO_OFF = 0,
	PALDO_ON = 1,
	PALDO_OP_MAX	
}ENUM_PALDO_OP,*P_ENUM_PALDO_OP;

typedef struct _BGF_IRQ_BALANCE_{
	UINT32 counter;
	OSAL_UNSLEEPABLE_LOCK lock;
}BGF_IRQ_BALANCE,*P_BGF_IRQ_BALANCE;

typedef VOID (*irq_cb)(VOID);
typedef INT32 (*device_audio_if_cb) (CMB_STUB_AIF_X aif, MTK_WCN_BOOL share);
typedef VOID (*func_ctrl_cb)(UINT32 on,UINT32 type);
typedef LONG (*thermal_query_ctrl_cb)(VOID);
typedef INT32 (*deep_idle_ctrl_cb)(UINT32);

/*******************************************************************************
*                    E X T E R N A L   R E F E R E N C E S
********************************************************************************
*/

/*******************************************************************************
*                            P U B L I C   D A T A
********************************************************************************
*/
extern UINT32 gWmtDbgLvl;


/*******************************************************************************
*                           P R I V A T E   D A T A
********************************************************************************
*/

/*******************************************************************************
*                  F U N C T I O N   D E C L A R A T I O N S
********************************************************************************
*/

INT32
wmt_plat_init (UINT32 co_clock_en);

INT32
wmt_plat_deinit (VOID);


INT32
wmt_plat_pwr_ctrl (
    ENUM_FUNC_STATE state
    );


INT32
wmt_plat_gpio_ctrl (
    ENUM_PIN_ID id,
    ENUM_PIN_STATE state
    );

INT32
wmt_plat_eirq_ctrl (
    ENUM_PIN_ID id,
    ENUM_PIN_STATE state
    );


INT32
wmt_plat_wake_lock_ctrl(
    ENUM_WL_OP opId
    );

VOID wmt_plat_irq_cb_reg (irq_cb bgf_irq_cb);

INT32
wmt_plat_audio_ctrl (
    CMB_STUB_AIF_X state,
    CMB_STUB_AIF_CTRL ctrl
    );

VOID wmt_plat_aif_cb_reg (device_audio_if_cb aif_ctrl_cb);
VOID wmt_plat_func_ctrl_cb_reg(func_ctrl_cb subsys_func_ctrl);
VOID wmt_plat_thermal_ctrl_cb_reg(thermal_query_ctrl_cb thermal_query_ctrl);
VOID wmt_plat_deep_idle_ctrl_cb_reg(deep_idle_ctrl_cb deep_idle_ctrl);

INT32 wmt_plat_soc_paldo_ctrl(ENUM_PALDO_TYPE ePt,ENUM_PALDO_OP ePo);
UINT8 *wmt_plat_get_emi_base_add(ENUM_EMI_BASE_INDEX addr_index);
UINT8 *wmt_plat_get_emi_ctrl_state_base_add(ENUM_EMI_CTRL_STATE_OFFSET ctrl_state_offset);
#if CONSYS_ENALBE_SET_JTAG
UINT32 wmt_plat_jtag_flag_ctrl(UINT32 en);
#endif
#if CONSYS_WMT_REG_SUSPEND_CB_ENABLE
UINT32 wmt_plat_soc_osc_en_ctrl(UINT32 en);
#endif
/*******************************************************************************
*                              F U N C T I O N S
********************************************************************************
*/

#endif /* _WMT_PLAT_H_ */

