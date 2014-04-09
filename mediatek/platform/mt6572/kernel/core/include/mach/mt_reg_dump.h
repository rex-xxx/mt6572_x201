#ifndef __MT_REG_DUMP_H
#define __MT_REG_DUMP_H

#include "mt_reg_base.h"

#define CORE0_PC (MCUSYS_CFGREG_BASE + 0x300)
#define CORE0_FP (MCUSYS_CFGREG_BASE + 0x304)
#define CORE0_SP (MCUSYS_CFGREG_BASE + 0x308)
#define CORE1_PC (MCUSYS_CFGREG_BASE + 0x310)
#define CORE1_FP (MCUSYS_CFGREG_BASE + 0x314)
#define CORE1_SP (MCUSYS_CFGREG_BASE + 0x318)


#define CORE_CNT (2)


struct mt_reg_dump {
	unsigned int pc;
	unsigned int fp;
	unsigned int sp;
	unsigned int core_id;
};

extern int mt_reg_dump(char *buf);

#endif

