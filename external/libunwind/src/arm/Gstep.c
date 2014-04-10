/* libunwind - a platform-independent unwind library
   Copyright (C) 2008 CodeSourcery
   Copyright 2011 Linaro Limited

This file is part of libunwind.

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.  */

#include "unwind_i.h"
#include "offsets.h"
#include "ex_tables.h"

#include <signal.h>

#define arm_exidx_step	UNW_OBJ(arm_exidx_step)

typedef struct exidx_decode_cache_tag
{
  unw_word_t cache_cfa;
  unw_word_t cache_ip;
  dwarf_loc_t loc[DWARF_NUM_PRESERVED_REGS];
} exidx_decode_cache;

typedef struct exidx_decode_cache_record_tag
{
  uint8_t buf[32];
  exidx_decode_cache before_decode;
  exidx_decode_cache after_decode;
  int hit_count;
} exidx_decode_cache_record;

#define MAX_CACHE_RECORD_CNT    16
static exidx_decode_cache_record _gCacheRecord[MAX_CACHE_RECORD_CNT];
static int _gCacheRecordCnt;

#if 1
#define FIND_PROC_INFO_CCNT (0)
#define EXIDX_EXTRACT_CCNT  (0)
#define EXIDX_DECODE_CCNT   (0)

#define u32 unsigned int
static u32 read_ccnt(void)
{
    u32 val;
    asm volatile("mrc p15, 0, %0, c9, c13, 0" : "=r" (val));            
    return val;
}

// Global PMU enable
// On ARM11 this enables the PMU, and the counters start immediately
// On Cortex this enables the PMU, there are individual enables for the counters
static void enable_pmu(void)
{
    u32 val;

/*
    EXPORT enable_pmu
    ; Global PMU enable
    ; void enable_pmu(void)
    enable_pmu  PROC
    MRC     p15, 0, r0, c9, c12, 0  ; Read PMNC
    ORR     r0, r0, #0x01           ; Set E bit
    MCR     p15, 0, r0, c9, c12, 0  ; Write PMNC
*/
    asm volatile("mrc p15, 0, %0, c9, c12, 0" : "=r" (val));    
    val |= 0x1;
    asm volatile("mcr p15, 0, %0, c9, c12, 0" : : "r" (val));
}

static void enable_ccnt(void)
{
    u32 val;

/*    
    EXPORT enable_ccnt
    ; Enable the CCNT
    ; void enable_ccnt(void)

    MOV     r0, #0x80000000         ; Set C bit
    MCR     p15, 0, r0, c9, c12, 1  ; Write CNTENS Register
*/
    val = 0x80000000;
    asm volatile("mcr p15, 0, %0, c9, c12, 1" : : "r" (val));
}

static void reset_ccnt(void)
{
    u32 val;

/*
    EXPORT  reset_ccnt
    ; Resets the CCNT
    ; void reset_ccnt(void)
    MRC     p15, 0, r0, c9, c12, 0  ; Read PMNC
    ORR     r0, r0, #0x4            ; Set C bit (Cycle counter reset)
    MCR     p15, 0, r0, c9, c12, 0  ; Write PMNC

*/
    asm volatile("mrc p15, 0, %0, c9, c12, 0" : "=r" (val));
    val |= 0x4;
    asm volatile("mcr p15, 0, %0, c9, c12, 0" : : "r" (val));
}

static void disable_ccnt(void)
{
    u32 val;

/*
    EXPORT disable_ccnt
    ; Disable the CCNT
    ; void disable_ccnt(void)
    MOV     r0, #0x80000000         ; Set C bit
    MCR     p15, 0, r0, c9, c12, 2  ; Write CNTENC Register
*/
    val = 0x80000000;
    asm volatile("mcr p15, 0, %0, c9, c12, 2" : : "r" (val));
}

#if 0
void set_cpu_affinity(void)
{
    cpu_set_t cmask;
    cpu_set_t get; 
    int num = sysconf(_SC_NPROCESSORS_CONF);     
    unsigned long len = sizeof(cmask);
    int i;
    
    CPU_ZERO(&cmask);
    CPU_SET(BIND_CPU, &cmask);
      
    /* bind process to processor 0 */
    if (sched_setaffinity(0, len, &cmask) == -1)
    {
        printf("sched_setaffinity error\n");
    }

    /* confirm the process is bind to processor 0 */
    while (1) 
    {     
        CPU_ZERO(&get); 
        if (sched_getaffinity(0, sizeof(get), &get) == -1)
        { 
            printf("sched_getaffinity error\n"); 
        } 

        for (i = 0; i < num; i++) 
        { 
            if (CPU_ISSET(i, &get)) 
            { 
                printf("this process %d is running processor : %d, total processor: %d\n", getpid(), i, num);
                if (i == BIND_CPU)
                {
                    return ;
                }
            } 
        } 
    } 
}
#endif
#endif

static void print_dwarf_cursor(struct dwarf_cursor *c)
{
//  Debug(99, "c->as_arg = %x\n", c->as_arg);    
//  Debug(99, "c->as = %x\n", c->as);    
  Debug(99, "c->cfa = %x\n", c->cfa);    
  Debug(99, "c->ip = %x\n", c->ip);    
//  Debug(99, "c->args_size= %x\n", c->args_size);    
//  Debug(99, "c->ret_addr_column = %x\n", c->ret_addr_column);    

#if 0
  Debug(99, "c->loc[UNW_ARM_R0] = %x\n", c->loc[UNW_ARM_R0].val);
  Debug(99, "c->loc[UNW_ARM_R1] = %x\n", c->loc[UNW_ARM_R1].val);
  Debug(99, "c->loc[UNW_ARM_R2] = %x\n", c->loc[UNW_ARM_R2].val);
  Debug(99, "c->loc[UNW_ARM_R3] = %x\n", c->loc[UNW_ARM_R3].val);
  Debug(99, "c->loc[UNW_ARM_R4] = %x\n", c->loc[UNW_ARM_R4].val);
  Debug(99, "c->loc[UNW_ARM_R5] = %x\n", c->loc[UNW_ARM_R5].val);
  Debug(99, "c->loc[UNW_ARM_R6] = %x\n", c->loc[UNW_ARM_R6].val);
  Debug(99, "c->loc[UNW_ARM_R7] = %x\n", c->loc[UNW_ARM_R7].val);
  Debug(99, "c->loc[UNW_ARM_R8] = %x\n", c->loc[UNW_ARM_R8].val);
  Debug(99, "c->loc[UNW_ARM_R9] = %x\n", c->loc[UNW_ARM_R9].val);
  Debug(99, "c->loc[UNW_ARM_R10] = %x\n", c->loc[UNW_ARM_R10].val);
  Debug(99, "c->loc[UNW_ARM_R11] = %x\n", c->loc[UNW_ARM_R11].val);
  Debug(99, "c->loc[UNW_ARM_R12] = %x\n", c->loc[UNW_ARM_R12].val);
#endif  
  Debug(99, "c->loc[UNW_ARM_R13] val %x\n", c->loc[UNW_ARM_R13].val);                                      
  Debug(99, "c->loc[UNW_ARM_R14] val %x\n", c->loc[UNW_ARM_R14].val);                                      
  Debug(99, "c->loc[UNW_ARM_R15] val %x\n", c->loc[UNW_ARM_R15].val);                                    

}

static void print_buf(uint8_t *pBuf)
{
  int i = 0;
  for (i = 0; i < 32 && (pBuf[i] != 0xb0); i++)
  {
    Debug(99, "buf[%d] = %d\n", i, pBuf[i]);
  }
}

static inline int
arm_exidx_step (struct cursor *c)
{
  unw_word_t old_ip, old_cfa;
  uint8_t buf[32];
  int ret;
  int cache_hit = 0;
  int cache_index = 0;
  int min_hit_cache_index = 0;
  int max_hit_cache_index = 0;
  exidx_decode_cache_record local_cache_record;
  u32 valstart;
  u32 valend;

  Debug (1, "arm_exidx_step\n");

  old_ip = c->dwarf.ip;
  old_cfa = c->dwarf.cfa;

  /* mark PC unsaved */
  c->dwarf.loc[UNW_ARM_R15] = DWARF_NULL_LOC;

//  enable_ccnt();
#if 0
  for (cache_index = 0; cache_index < MAX_CACHE_RECORD_CNT; cache_index++)
  {
    if (
       (_gCacheRecord[cache_index].before_decode.cache_cfa == c->dwarf.cfa) &&
       (_gCacheRecord[cache_index].before_decode.cache_ip == c->dwarf.ip) &&
       (_gCacheRecord[cache_index].before_decode.loc[0].val == c->dwarf.loc[UNW_ARM_R13].val) &&
       (_gCacheRecord[cache_index].before_decode.loc[1].val == c->dwarf.loc[UNW_ARM_R14].val)
       )
    {
      cache_hit = 1;
      memcpy(&local_cache_record, &_gCacheRecord[cache_index], sizeof(exidx_decode_cache_record));
      _gCacheRecord[cache_index].hit_count++;
      Debug (99, "index %d hit_count %d\n", cache_index, _gCacheRecord[cache_index].hit_count);
      break;
    }
    else
    {      
      if (_gCacheRecord[cache_index].hit_count == 0)
      {
        min_hit_cache_index = cache_index;
        break;
      }

      if ((cache_index != min_hit_cache_index) &&
         (_gCacheRecord[cache_index].hit_count <= _gCacheRecord[min_hit_cache_index].hit_count))
      {
        min_hit_cache_index = cache_index;
      }
      else
      {
        max_hit_cache_index = cache_index;
      }     
    }
  }

  if (1)//!cache_hit)
  {
    _gCacheRecord[min_hit_cache_index].before_decode.cache_cfa = c->dwarf.cfa;
    _gCacheRecord[min_hit_cache_index].before_decode.cache_ip = c->dwarf.ip;
    _gCacheRecord[min_hit_cache_index].before_decode.loc[0] = c->dwarf.loc[UNW_ARM_R13];
    _gCacheRecord[min_hit_cache_index].before_decode.loc[1] = c->dwarf.loc[UNW_ARM_R14];

    if ((ret = tdep_find_proc_info (&c->dwarf, c->dwarf.ip, 1)) < 0)
    {    
      Debug (1, "tdep_find_proc_info (&c->dwarf, c->dwarf.ip, 1) < 0\n");
      return ret;
    }
  
    if (c->dwarf.pi.format != UNW_INFO_FORMAT_ARM_EXIDX)
    {
      Debug (1, "(c->dwarf.pi.format != UNW_INFO_FORMAT_ARM_EXIDX)\n");
      return -UNW_ENOINFO;
    }
  
    ret = arm_exidx_extract (&c->dwarf, buf);
    Debug (1, "arm_exidx_extract ret %d\n", ret);  
    if (ret == -UNW_ESTOPUNWIND)
    {
      return 0;
    }
    else if (ret < 0)
    {
      return ret;
    }


    ret = arm_exidx_decode (buf, ret, &c->dwarf);
    Debug (1, "arm_exidx_decode ret %d\n", ret);
    if (ret < 0)
    {
      return ret;
    }

    ret = arm_exidx_decode (buf, ret, &c->dwarf);
    Debug (1, "arm_exidx_decode ret %d\n", ret);
    if (ret < 0)
    {
      return ret;
    }

    _gCacheRecord[min_hit_cache_index].after_decode.cache_cfa = c->dwarf.cfa;
    _gCacheRecord[min_hit_cache_index].after_decode.loc[0] = c->dwarf.loc[UNW_ARM_R13];
    _gCacheRecord[min_hit_cache_index].after_decode.loc[1] = c->dwarf.loc[UNW_ARM_R14];
    _gCacheRecord[min_hit_cache_index].after_decode.loc[2] = c->dwarf.loc[UNW_ARM_R15];
    _gCacheRecord[min_hit_cache_index].hit_count = 1;

    if (cache_hit)
    {
      unw_word_t ip;
      ip = *(unw_word_t *) DWARF_GET_LOC (local_cache_record.after_decode.loc[2]);
      if (
      (c->dwarf.cfa != local_cache_record.after_decode.cache_cfa) ||
      (c->dwarf.ip != ip) ||
      (c->dwarf.loc[UNW_ARM_R13].val != local_cache_record.after_decode.loc[0].val) ||
      (c->dwarf.loc[UNW_ARM_R14].val != local_cache_record.after_decode.loc[1].val) ||
      (c->dwarf.loc[UNW_ARM_R15].val != local_cache_record.after_decode.loc[2].val))
      {
        Debug (99, "cache error\n");
        Debug (99, "cache_cfa = %x\n", local_cache_record.after_decode.cache_cfa);
        Debug (99, "UNW_ARM_R13 val %x\n", local_cache_record.after_decode.loc[0].val);
        Debug (99, "UNW_ARM_R14 val %x\n", local_cache_record.after_decode.loc[1].val);
        Debug (99, "UNW_ARM_R15 val %x\n", local_cache_record.after_decode.loc[2].val);
        
        print_dwarf_cursor(&c->dwarf);
    
 }    
    }
  }
#else
  if ((ret = tdep_find_proc_info (&c->dwarf, c->dwarf.ip, 1)) < 0)
  {    
    return ret;
  }
  
  if (c->dwarf.pi.format != UNW_INFO_FORMAT_ARM_EXIDX)
  {
    return -UNW_ENOINFO;
  }
  
  ret = arm_exidx_extract (&c->dwarf, buf);
  if (ret == -UNW_ESTOPUNWIND)
  {
    return 0;
  }
  else if (ret < 0)
  {
    return ret;
  }
    
  ret = arm_exidx_decode (buf, ret, &c->dwarf);
  if (ret < 0)
  {
    return ret;
  }  
#endif

  if (c->dwarf.ip == old_ip && c->dwarf.cfa == old_cfa)
  {
    Debug (1, "%s: ip and cfa unchanged; stopping here (ip=0x%lx)\n",
            __FUNCTION__, (long) c->dwarf.ip);
    Dprintf ("%s: ip and cfa unchanged; stopping here (ip=0x%lx)\n",
            __FUNCTION__, (long) c->dwarf.ip);
    return -UNW_EBADFRAME;
  }

  Debug (1, "c->dwarf.ip = %x\n", c->dwarf.ip);

  return (c->dwarf.ip == 0) ? 0 : 1;
}

PROTECTED int
unw_handle_signal_frame (unw_cursor_t *cursor)
{
  struct cursor *c = (struct cursor *) cursor;
  int ret;
  unw_word_t sc_addr, sp, sp_addr = c->dwarf.cfa;
  struct dwarf_loc sp_loc = DWARF_LOC (sp_addr, 0);

  if ((ret = dwarf_get (&c->dwarf, sp_loc, &sp)) < 0)
    return -UNW_EUNSPEC;

  /* Obtain signal frame type (non-RT or RT). */
  ret = unw_is_signal_frame (cursor);

  /* Save the SP and PC to be able to return execution at this point
     later in time (unw_resume).  */
  c->sigcontext_sp = c->dwarf.cfa;
  c->sigcontext_pc = c->dwarf.ip;

  /* Since kernel version 2.6.18 the non-RT signal frame starts with a
     ucontext while the RT signal frame starts with a siginfo, followed
     by a sigframe whose first element is an ucontext.
     Prior 2.6.18 the non-RT signal frame starts with a sigcontext while
     the RT signal frame starts with two pointers followed by a siginfo
     and an ucontext. The first pointer points to the start of the siginfo
     structure and the second one to the ucontext structure.  */

  if (ret == 1)
    {
      /* Handle non-RT signal frames. Check if the first word on the stack
	 is the magic number.  */
      if (sp == 0x5ac3c35a)
	{
	  c->sigcontext_format = ARM_SCF_LINUX_SIGFRAME;
	  sc_addr = sp_addr + LINUX_UC_MCONTEXT_OFF;
	}
      else
	{
	  c->sigcontext_format = ARM_SCF_LINUX_OLD_SIGFRAME;
	  sc_addr = sp_addr;
	}
      c->sigcontext_addr = sp_addr;
    }
  else if (ret == 2)
    {
      /* Handle RT signal frames. Check if the first word on the stack is a
	 pointer to the siginfo structure.  */
      if (sp == sp_addr + 8)
	{
	  c->sigcontext_format = ARM_SCF_LINUX_OLD_RT_SIGFRAME;
	  c->sigcontext_addr = sp_addr + 8 + sizeof (siginfo_t); 
	}
      else
	{
	  c->sigcontext_format = ARM_SCF_LINUX_RT_SIGFRAME;
	  c->sigcontext_addr = sp_addr + sizeof (siginfo_t);
	}
      sc_addr = c->sigcontext_addr + LINUX_UC_MCONTEXT_OFF;
    }
  else
    return -UNW_EUNSPEC;

  /* Update the dwarf cursor.
     Set the location of the registers to the corresponding addresses of the
     uc_mcontext / sigcontext structure contents.  */
  c->dwarf.loc[UNW_ARM_R0] = DWARF_LOC (sc_addr + LINUX_SC_R0_OFF, 0);
  c->dwarf.loc[UNW_ARM_R1] = DWARF_LOC (sc_addr + LINUX_SC_R1_OFF, 0);
  c->dwarf.loc[UNW_ARM_R2] = DWARF_LOC (sc_addr + LINUX_SC_R2_OFF, 0);
  c->dwarf.loc[UNW_ARM_R3] = DWARF_LOC (sc_addr + LINUX_SC_R3_OFF, 0);
  c->dwarf.loc[UNW_ARM_R4] = DWARF_LOC (sc_addr + LINUX_SC_R4_OFF, 0);
  c->dwarf.loc[UNW_ARM_R5] = DWARF_LOC (sc_addr + LINUX_SC_R5_OFF, 0);
  c->dwarf.loc[UNW_ARM_R6] = DWARF_LOC (sc_addr + LINUX_SC_R6_OFF, 0);
  c->dwarf.loc[UNW_ARM_R7] = DWARF_LOC (sc_addr + LINUX_SC_R7_OFF, 0);
  c->dwarf.loc[UNW_ARM_R8] = DWARF_LOC (sc_addr + LINUX_SC_R8_OFF, 0);
  c->dwarf.loc[UNW_ARM_R9] = DWARF_LOC (sc_addr + LINUX_SC_R9_OFF, 0);
  c->dwarf.loc[UNW_ARM_R10] = DWARF_LOC (sc_addr + LINUX_SC_R10_OFF, 0);
  c->dwarf.loc[UNW_ARM_R11] = DWARF_LOC (sc_addr + LINUX_SC_FP_OFF, 0);
  c->dwarf.loc[UNW_ARM_R12] = DWARF_LOC (sc_addr + LINUX_SC_IP_OFF, 0);
  c->dwarf.loc[UNW_ARM_R13] = DWARF_LOC (sc_addr + LINUX_SC_SP_OFF, 0);
  c->dwarf.loc[UNW_ARM_R14] = DWARF_LOC (sc_addr + LINUX_SC_LR_OFF, 0);
  c->dwarf.loc[UNW_ARM_R15] = DWARF_LOC (sc_addr + LINUX_SC_PC_OFF, 0);

  /* Set SP/CFA and PC/IP.  */
  dwarf_get (&c->dwarf, c->dwarf.loc[UNW_ARM_R13], &c->dwarf.cfa);
  dwarf_get (&c->dwarf, c->dwarf.loc[UNW_ARM_R15], &c->dwarf.ip);

  return 1;
}

PROTECTED int
unw_step (unw_cursor_t *cursor)
{
  struct cursor *c = (struct cursor *) cursor;
  int ret = -UNW_EUNSPEC;

  Debug (1, "(cursor=%p)\n", c);

  /* Check if this is a signal frame. */
  if (unw_is_signal_frame (cursor))
  {
     Debug (1, "(unw_is_signal_frame (cursor)\n");
     return unw_handle_signal_frame (cursor);
  }

#ifdef CONFIG_DEBUG_FRAME
  Debug (1, "CONFIG_DEBUG_FRAME\n");
  /* First, try DWARF-based unwinding. */
  if (UNW_TRY_METHOD(UNW_ARM_METHOD_DWARF))
  {
      Debug (1, "(UNW_TRY_METHOD(UNW_ARM_METHOD_DWARF))\n");
      ret = dwarf_step (&c->dwarf);
      Debug(1, "dwarf_step()=%d\n", ret);

      if (likely (ret > 0))
	return 1;
      else if (unlikely (ret == -UNW_ESTOPUNWIND))
	return ret;

    if (ret < 0 && ret != -UNW_ENOINFO)
      {
        Debug (2, "returning %d\n", ret);
        return ret;
      }
    }
#endif /* CONFIG_DEBUG_FRAME */

  /* Next, try extbl-based unwinding. */
  if (UNW_TRY_METHOD (UNW_ARM_METHOD_EXIDX))
  {
      Debug (1, "UNW_TRY_METHOD (UNW_ARM_METHOD_EXIDX)\n");

      ret = arm_exidx_step (c);
      Debug (1, "arm_exidx_step ret %d\n", ret);
      if (ret > 0)
	      return 1;
      if (ret == -UNW_ESTOPUNWIND || ret == 0)
	      return ret;
  }

  /* Fall back on APCS frame parsing.
     Note: This won't work in case the ARM EABI is used. */
  if (unlikely (ret < 0))
    {
      if (UNW_TRY_METHOD(UNW_ARM_METHOD_FRAME))
        {
          ret = UNW_ESUCCESS;
          /* DWARF unwinding failed, try to follow APCS/optimized APCS frame chain */
          unw_word_t instr, i;
          Debug (13, "dwarf_step() failed (ret=%d), trying frame-chain\n", ret);
          dwarf_loc_t ip_loc, fp_loc;
          unw_word_t frame;
          /* Mark all registers unsaved, since we don't know where
             they are saved (if at all), except for the EBP and
             EIP.  */
          if (dwarf_get(&c->dwarf, c->dwarf.loc[UNW_ARM_R11], &frame) < 0)
            {
              return 0;
            }
          for (i = 0; i < DWARF_NUM_PRESERVED_REGS; ++i) {
            c->dwarf.loc[i] = DWARF_NULL_LOC;
          }
          if (frame)
            {
              if (dwarf_get(&c->dwarf, DWARF_LOC(frame, 0), &instr) < 0)
                {
                  return 0;
                }
              instr -= 8;
              if (dwarf_get(&c->dwarf, DWARF_LOC(instr, 0), &instr) < 0)
                {
                  return 0;
                }
              if ((instr & 0xFFFFD800) == 0xE92DD800)
                {
                  /* Standard APCS frame. */
                  ip_loc = DWARF_LOC(frame - 4, 0);
                  fp_loc = DWARF_LOC(frame - 12, 0);
                }
              else
                {
                  /* Codesourcery optimized normal frame. */
                  ip_loc = DWARF_LOC(frame, 0);
                  fp_loc = DWARF_LOC(frame - 4, 0);
                }
              if (dwarf_get(&c->dwarf, ip_loc, &c->dwarf.ip) < 0)
                {
                  return 0;
                }
              c->dwarf.loc[UNW_ARM_R12] = ip_loc;
              c->dwarf.loc[UNW_ARM_R11] = fp_loc;
              Debug(15, "ip=%lx\n", c->dwarf.ip);
            }
          else
            {
              ret = -UNW_ENOINFO;
            }
        }
    }
  return ret == -UNW_ENOINFO ? 0 : 1;
}
