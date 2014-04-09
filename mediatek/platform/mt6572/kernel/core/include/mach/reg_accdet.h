#include <mach/mt_reg_base.h>
//Register address define
#ifndef ACCDET_HW_H
#define ACCDET_HW_H

#define ACCDET_BASE              0x00000000			

//top reset control
#define TOP_RST_ACCDET			 ACCDET_BASE + 0x0114
#define TOP_RST_ACCDET_SET 		 ACCDET_BASE + 0x0116
#define TOP_RST_ACCDET_CLR 		 ACCDET_BASE + 0x0118

//irq enable control
#define INT_CON_ACCDET		 ACCDET_BASE + 0x0166
#define INT_CON_ACCDET_SET		 ACCDET_BASE + 0x0168 
#define INT_CON_ACCDET_CLR       ACCDET_BASE + 0x016A

//clock enable control
#define TOP_CKPDN				 ACCDET_BASE + 0x0108
#define TOP_CKPDN_SET            ACCDET_BASE + 0x010A
#define TOP_CKPDN_CLR            ACCDET_BASE + 0x010C

//accdet register
#define ACCDET_RSV               ACCDET_BASE + 0x077A
#define ACCDET_CTRL              ACCDET_BASE + 0x077C
#define ACCDET_STATE_SWCTRL      ACCDET_BASE + 0x077E
#define ACCDET_WIDTH         	 ACCDET_BASE + 0x0780
#define ACCDET_THRESH        	 ACCDET_BASE + 0x0782
#define ACCDET_EN_DELAY_NUM      ACCDET_BASE + 0x0784
#define ACCDET_DEBOUNCE0         ACCDET_BASE + 0x0786
#define ACCDET_DEBOUNCE1         ACCDET_BASE + 0x0788
#define ACCDET_DEBOUNCE2         ACCDET_BASE + 0x078A
#define ACCDET_DEBOUNCE3         ACCDET_BASE + 0x078C
#define ACCDET_IRQ_STS           ACCDET_BASE + 0x0790
#define ACCDET_CONTROL_RG        ACCDET_BASE + 0x0792
#define ACCDET_STATE_RG          ACCDET_BASE + 0x0794
#define ACCDET_CUR_DEB			 ACCDET_BASE + 0x0796
#define ACCDET_RSV_CON0			 ACCDET_BASE + 0x0798
#define ACCDET_RSV_CON1			 ACCDET_BASE + 0x079A



//for top reset control
#define ACCDET_RESET_SET             (1<<4)
#define ACCDET_RESET_CLR             (1<<4)

//for irq control
#define RG_ACCDET_IRQ_SET        (1<<2)
#define RG_ACCDET_IRQ_CLR        (1<<2)

//for clock control
#define RG_ACCDET_CLK_SET        (1<<9)
#define RG_ACCDET_CLK_CLR        (1<<9)

//for ACCDET_CTRL
#define ACCDET_ENABLE			 (1<<0)
#define ACCDET_DISABLE			 (0<<0)

//for ACCDET_IRQ_STS
#define ACCDET_IRQ_CLR           0x100
#define ACCDET_IRQ			0x0001

//for ACCDET_STATE_SWCTRL
#define ACCDET_SWCTRL_EN         0x07
#define ACCDET_SWCTRL_IDLE_EN    (0x07<<4)

#define ACCDET_1V9_ON 0x1e10
#define ACCDET_1V9_OFF 0x1a10
#define ACCDET_2V8_ON 0x5a20
#define ACCDET_2V8_OFF 0x5a10

/*

#define ACCDET_1V9_ON 0x1e10
#define ACCDET_1V9_OFF 0x1e10
#define ACCDET_2V8_ON 0x5a20
#define ACCDET_2V8_OFF 0x5a20

#define ACCDET_1V9_ON 0x0e10
#define ACCDET_1V9_OFF 0x0a10
#define ACCDET_2V8_ON 0x4a20
#define ACCDET_2V8_OFF 0x4a10
*/

#endif //ACCDET_HW_H

