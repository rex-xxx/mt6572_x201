@-----------------------------------------------------------------------------
@ The confidential and proprietary information contained in this file may
@ only be used by a person authorised under and to the extent permitted
@ by a subsisting licensing agreement from ARM Limited.
@
@            (C) COPYRIGHT 2007-2010 ARM Limited.
@                ALL RIGHTS RESERVED
@
@ This entire notice must be reproduced on all copies of this file
@ and copies of this file may only be made by a person if such person is
@ permitted to do so under the terms of a subsisting license agreement
@ from ARM Limited.
@
@      RCS Information
@
@      RCS Filename        : $RCSfile$
@
@      Checked In          : $Date$
@
@      Revision            : $Revision: 73865 $
@
@      Release Information : r3p0-00rel0
@-----------------------------------------------------------------------------
@
@@-----------------------------------------------------------------------------
@@  Find configuration of CPU
@@  Variable PERIPH_BASE needs to be updated according to real mapping
@@-----------------------------------------------------------------------------

@-------------------------------------------------------------------------------
@ Init Code
@-------------------------------------------------------------------------------
                .text
                .align 5

.EQU    num_iteration,     	  0x60
.EQU    num_init,             0x0

.EQU    COPY_SIZE,            0x00002000

@ 0x00004000 --> 16K L1 memory copy
@ lower 16K memory is copied to upper 16K memory

.EQU    Cache_Line_Size,      0x40

                .ALIGN   5

@*****************************************************************************
@***    start of main test
@*****************************************************************************

.global fp2_memcpyL1_start
fp2_memcpyL1_start:
                STMDB   sp!, {r1-r12}
                .ALIGN 5 @ align to $ line

                @MOV  r0, PC
				@LDR  r1, =0x0000FFFF
                @BIC  r2,r0,r1
                @LDR  r3, =0x00010000
                @ADD  r3, r3 ,r2
                LDR  r4, =PC_sync_L1_MEMORY
                STR  r0, [r4]


                LDR     r11,=num_iteration            @ num of iterations round power_loop, it should not be edited by other task

                MRC   p15, 0, r0, c0, c0, 5         @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3                  @ Extract CPU ID bits
                LSL   r0, #14
                LDR   r10, =PC_sync_L1_MEMORY
                LDR   r10, [r10]
                @LDR   r10, =L1_MEMORY
                LDR   r9, =COPY_SIZE
                ADD   r12, r10, r0                  @ shift address by CPUID, every cpu access different address space

                @ turn off d cache to initialize data in outer memory
@D_cache_off:
                @MRC p15,0,r1,c1,c0,0
                @BIC r1,r1,#4
                @MCR p15,0,r1,c1,c0,0
                @ISB

                LDR      r10, =num_init
                @MEM_SET_64_INC r12, r9, r10
                MOV             r0, r10
                MOV             r6, r12
                SUB             r5, r9, #0x40
MEM_SET_64_INC:
                ADD             r1, r0, #1
                ADD             r2, r0, #2
                ADD             r3, r0, #3

                STMIA           r6!,{r0, r1, r2, r3}
                STMIA           r6!,{r0, r1, r2, r3}
                STMIA           r6!,{r0, r1, r2, r3}
                STMIA           r6!,{r0, r1, r2, r3}

                ADD             r0, r0, #4

                SUBS           r5, r5, #0x40
                BCS            MEM_SET_64_INC

	@flush dcache
                stmfd          sp!, {r0-r5, r7, r9-r11, lr}
                bl             v7_flush_dcache_all
                ldmfd          sp!, {r0-r5, r7, r9-r11, lr}


                @r8, $size, $init --> all variable is not changed
                @r0~r6 is in used

@D_cache_on:
@                @ turn on D cache
@                MOV     r2,#0x4
@                MRC     p15,0,r1,c1,c0,0
@                ORR     r1,r1,r2
@                MCR     p15,0,r1,c1,c0,0
@                ISB

                LDR      r9, =COPY_SIZE
                LSR      r9, #12
                SUB      r9, r9, #1
                MOV      r8, r12
_next_4k_load:

                @LOAD_NONSEQ_DATA_L1 r8
                LDR     r0, [r8]
                LDR     r1, [r8,#Cache_Line_Size]!
                LDR     r2, [r8,#Cache_Line_Size]!
                LDR     r3, [r8,#Cache_Line_Size]!
                LDR     r4, [r8,#Cache_Line_Size]!
                LDR     r5, [r8,#Cache_Line_Size]!
                LDR     r6, [r8,#Cache_Line_Size]!
                LDR     r7, [r8,#Cache_Line_Size]!
                LDR     r0, [r8,#Cache_Line_Size]!
                LDR     r1, [r8,#Cache_Line_Size]!
                LDR     r2, [r8,#Cache_Line_Size]!
                LDR     r3, [r8,#Cache_Line_Size]!
                LDR     r4, [r8,#Cache_Line_Size]!
                LDR     r5, [r8,#Cache_Line_Size]!
                LDR     r6, [r8,#Cache_Line_Size]!
                LDR     r7, [r8,#Cache_Line_Size]!
                LDR     r0, [r8,#Cache_Line_Size]!
                LDR     r1, [r8,#Cache_Line_Size]!
                LDR     r2, [r8,#Cache_Line_Size]!
                LDR     r3, [r8,#Cache_Line_Size]!
                LDR     r4, [r8,#Cache_Line_Size]!
                LDR     r5, [r8,#Cache_Line_Size]!
                LDR     r6, [r8,#Cache_Line_Size]!
                LDR     r7, [r8,#Cache_Line_Size]!
                LDR     r0, [r8,#Cache_Line_Size]!
                LDR     r1, [r8,#Cache_Line_Size]!
                LDR     r2, [r8,#Cache_Line_Size]!
                LDR     r3, [r8,#Cache_Line_Size]!
                LDR     r4, [r8,#Cache_Line_Size]!
                LDR     r5, [r8,#Cache_Line_Size]!
                LDR     r6, [r8,#Cache_Line_Size]!
                LDR     r7, [r8,#Cache_Line_Size]!
                LDR     r0, [r8,#Cache_Line_Size]!
                LDR     r1, [r8,#Cache_Line_Size]!
                LDR     r2, [r8,#Cache_Line_Size]!
                LDR     r3, [r8,#Cache_Line_Size]!
                LDR     r4, [r8,#Cache_Line_Size]!
                LDR     r5, [r8,#Cache_Line_Size]!
                LDR     r6, [r8,#Cache_Line_Size]!
                LDR     r7, [r8,#Cache_Line_Size]!
                LDR     r0, [r8,#Cache_Line_Size]!
                LDR     r1, [r8,#Cache_Line_Size]!
                LDR     r2, [r8,#Cache_Line_Size]!
                LDR     r3, [r8,#Cache_Line_Size]!
                LDR     r4, [r8,#Cache_Line_Size]!
                LDR     r5, [r8,#Cache_Line_Size]!
                LDR     r6, [r8,#Cache_Line_Size]!
                LDR     r7, [r8,#Cache_Line_Size]!
                LDR     r0, [r8,#Cache_Line_Size]!
                LDR     r1, [r8,#Cache_Line_Size]!
                LDR     r2, [r8,#Cache_Line_Size]!
                LDR     r3, [r8,#Cache_Line_Size]!
                LDR     r4, [r8,#Cache_Line_Size]!
                LDR     r5, [r8,#Cache_Line_Size]!
                LDR     r6, [r8,#Cache_Line_Size]!
                LDR     r7, [r8,#Cache_Line_Size]!
                LDR     r0, [r8,#Cache_Line_Size]!
                LDR     r1, [r8,#Cache_Line_Size]!
                LDR     r2, [r8,#Cache_Line_Size]!
                LDR     r3, [r8,#Cache_Line_Size]!
                LDR     r4, [r8,#Cache_Line_Size]!
                LDR     r5, [r8,#Cache_Line_Size]!
                LDR     r6, [r8,#Cache_Line_Size]!
                LDR     r7, [r8,#Cache_Line_Size]!

                MOV      r7, #0
                MCR      p15,0,r7,c7,c10,4

        @ r8 is a variable
        @ r0~r7 is in used
                ADD      r8,r8,#0x40
                SUBS     r9,r9,#1
                BCS      _next_4k_load

_next_iter_loop:
                MOV   r6, r12
                LDR   r7, =COPY_SIZE
                ADD   r8, r12, r7

                @MEM_COPY_DW_64  r6, r8, r7
                SUB         r5, r7, #0x40
MEM_COPY_DW_64:

                LDRD          r0, [r6,#0x0]
                STRD          r0, [r8,#0x0]
                LDRD          r2, [r6,#0x8]
                STRD          r2, [r8,#0x8]

                LDRD          r0, [r6,#0x10]
                STRD          r0, [r8,#0x10]
                LDRD          r2, [r6,#0x18]
                STRD          r2, [r8,#0x18]

                LDRD          r0, [r6,#0x20]
                STRD          r0, [r8,#0x20]
                LDRD          r2, [r6,#0x28]
                STRD          r2, [r8,#0x28]

                LDRD          r0, [r6,#0x30]
                STRD          r0, [r8,#0x30]
                LDRD          r2, [r6,#0x38]
                STRD          r2, [r8,#0x38]

                ADD             r6, r6, #0x40
                ADD             r8, r8, #0x40
                SUBS            r5, r5, #0x40
                BCS             MEM_COPY_DW_64

                @r0~r5 is in used
                @pass 3 register as r8, $dst, $size
                @r8, $dst will be changed

                MOV   r8, r12
                LDR   r9, =COPY_SIZE

                @MEM_CMP2_64_INC r8, r9, r10, test_error
                ADD             r6, r8, r9 @destination address
                SUB             r7, r9, #0x40 @ address count
                MOV           r4, r10
MEM_CMP2_64_INC:
                LDMIA         r6! ,{r0, r1, r2, r3}
                ADD           r5, r4, #0
                CMP           r5, r0
                BNE           test_error
                ADD           r5, r4, #1
                CMP           r5, r1
                BNE           test_error
                ADD           r5, r4, #2
                CMP           r5, r2
                BNE           test_error
                ADD           r5, r4, #3
                CMP           r5, r3
                BNE           test_error

                LDMIA         r6! ,{r0, r1, r2, r3}
                ADD           r5, r4, #0
                CMP           r5, r0
                BNE           test_error
                ADD           r5, r4, #1
                CMP           r5, r1
                BNE           test_error
                ADD           r5, r4, #2
                CMP           r5, r2
                BNE           test_error
                ADD           r5, r4, #3
                CMP           r5, r3
                BNE           test_error

                LDMIA         r6! ,{r0, r1, r2, r3}
                ADD           r5, r4, #0
                CMP           r5, r0
                BNE           test_error
                ADD           r5, r4, #1
                CMP           r5, r1
                BNE           test_error
                ADD           r5, r4, #2
                CMP           r5, r2
                BNE           test_error
                ADD           r5, r4, #3
                CMP           r5, r3
                BNE           test_error

                LDMIA         r6! ,{r0, r1, r2, r3}
                ADD           r5, r4, #0
                CMP           r5, r0
                BNE           test_error
                ADD           r5, r4, #1
                CMP           r5, r1
                BNE           test_error
                ADD           r5, r4, #2
                CMP           r5, r2
                BNE           test_error
                ADD           r5, r4, #3
                CMP           r5, r3
                BNE           test_error

                ADD           r4, r4, #4

                SUBS           r7, r7, #0x40
                BCS            MEM_CMP2_64_INC

                @r8, $size, $init, $fail_label --> all variable will not be changed
                @ r0~r7 is in used

                SUBS     r11, r11, #0x01
                BCS      _next_iter_loop

                B       test_pass

test_error:
                MOV  r0, #0x0
                B    test_end

test_pass:
                MOV  r0, #0x1
                B    test_end

test_end:
                LDMIA  sp!, {r1-r12}
                BX lr
                .ALIGN   5
PC_sync_L1_MEMORY:
                .word 0

                .END
