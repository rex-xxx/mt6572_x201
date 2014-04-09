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

.EQU    num_iteration,        0x180
@L1_MEMORY       EQU     0x89000000
@sync_adr        EQU     0x8A000000
@sync_flag       EQU     0x8A000040
.EQU    COPY_SIZE,            0x00001000
@ 0x00004000 --> 16K L1 memory copy
@ copy only 4K area
.EQU EFUSE_CPU_ADR,       0x10009040 @bit 27:24 is efused for CPU core

.global scu_error_flag
.global scu_test_flag
@.global scu_pc_sync_flag
@.global scu_pc_sync_adr
.EQU    Cache_Line_Size,               0x40

                .ALIGN   5

@*****************************************************************************
@***    start of main test
@*****************************************************************************

.global fp6_scu_start
fp6_scu_start:
                STMDB   sp!, {r1-r12}

                .align   5 @ align to $ line
                @MOV  r0, PC
                @LDR  r1, =0x0000FFFF
                @BIC  r2,r0,r1
                @LDR  r3, =0x0000f000
                @ADD  r3, r3 ,r2
                @LDR  r4, =scu_cp_sync_adr
                @STR  r3, [r4]
                @LDR  r3, =0x0000f040
                @ADD  r3, r3 ,r2
                @LDR  r4, =scu_pc_sync_flag
                @STR  r3, [r4]
                @LDR  r3, =0x00010000
                @ADD  r3, r3 ,r2
                LDR  r4, =PC_sync_L1_MEMORY
                STR  r0, [r4]

                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3          @ Extract CPU ID bits
                LSL   r2, r0, #6

                LDR   r0, =scu_pc_sync_flag
                ADD   r0, r0, r2
                @LDR   r0, [r0]
                @LDR   r0, =sync_adr
                MOV   r1, #0
                STR   r1, [r0]
                STR   r1, [r0,#0x4]
                STR   r1, [r0,#0x8]
                STR   r1, [r0,#0xc]

                LDR   r0, =scu_pc_sync_adr
                @LDR   r0, [r0]
                @LDR   r0, =sync_flag
                MOV   r1, #0
                STR   r1, [r0]
                STR   r1, [r0,#0x4]
                STR   r1, [r0,#0x8]
                STR   r1, [r0,#0xc]

@enable fp6_scu.rtl.run.logMMU
                DSB
@                MRC     p15, 0, r0, c1, c0, 0       @ Read SCTLR - System Control Register
@                ORR     r0, r0, #0x01               @ Set M bit (bit 0)
@                MCR     p15, 0, r0, c1, c0, 0       @ Write SCTLR
@		ISB

                LDR     r11,=num_iteration@ num of iterations round power_loop, it should not be edited by other task

                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3          @ Extract CPU ID bits
                MOV   r8, r0                @ pass extract ID as initlization value

                LDR   r10, =PC_sync_L1_MEMORY
                LDR   r10, [r10]
                @LDR   r10, =L1_MEMORY
                LDR   r9,  =COPY_SIZE
                MUL   r12, r9, r8
                ADD   r12, r10, r12          @ shift address by CPUID * COPY_SIZE , every cpu initilization its own address

                @ turn off d cache to initialize data in outer memory
@D_cache_off:
@                MRC p15,0,r1,c1,c0,0
@                BIC r1,r1,#4
@                MCR p15,0,r1,c1,c0,0
@                ISB

                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3          @ Extract CPU ID bits
                LSL   r2, r0, #6

                LDR   r0, =scu_pc_sync_adr
                ADD   r0, r0, r2
                @LDR   r0, [r0]
                @LDR   r0, =sync_adr
                MOV   r1, #0
                STR   r1, [r0]
                STR   r1, [r0,#0x4]
                STR   r1, [r0,#0x8]
                STR   r1, [r0,#0xc]


                @MEM_SET_64_INC r12, r9, r8
                MOV             r0, r8
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


                @$src, $size, $init --> all variable is not changed
                @r0~r6 is in used

@D_cache_on:
@                @ turn on D cache
@                MOV     r2,#0x4
@                MRC     p15,0,r1,c1,c0,0
@                ORR     r1,r1,r2
@                MCR     p15,0,r1,c1,c0,0
@                ISB

_next_iter_loop:
                LDR   r0, =scu_pc_sync_adr
                @LDR   r0, [r0]
                        @LDR   r0, =sync_adr
                MOV   r1, #0
                STR   r1, [r0]

                LDR      r9, =COPY_SIZE
                LSR      r9, #12
                SUB      r9, r9, #1
                MOV      r10, r12
_next_4k_load:

                @LOAD_NONSEQ_DATA_L1 r10
                LDR     r0, [r10]
                LDR     r1, [r10,#Cache_Line_Size]!
                LDR     r2, [r10,#Cache_Line_Size]!
                LDR     r3, [r10,#Cache_Line_Size]!
                LDR     r4, [r10,#Cache_Line_Size]!
                LDR     r5, [r10,#Cache_Line_Size]!
                LDR     r6, [r10,#Cache_Line_Size]!
                LDR     r7, [r10,#Cache_Line_Size]!
                LDR     r0, [r10,#Cache_Line_Size]!
                LDR     r1, [r10,#Cache_Line_Size]!
                LDR     r2, [r10,#Cache_Line_Size]!
                LDR     r3, [r10,#Cache_Line_Size]!
                LDR     r4, [r10,#Cache_Line_Size]!
                LDR     r5, [r10,#Cache_Line_Size]!
                LDR     r6, [r10,#Cache_Line_Size]!
                LDR     r7, [r10,#Cache_Line_Size]!
                LDR     r0, [r10,#Cache_Line_Size]!
                LDR     r1, [r10,#Cache_Line_Size]!
                LDR     r2, [r10,#Cache_Line_Size]!
                LDR     r3, [r10,#Cache_Line_Size]!
                LDR     r4, [r10,#Cache_Line_Size]!
                LDR     r5, [r10,#Cache_Line_Size]!
                LDR     r6, [r10,#Cache_Line_Size]!
                LDR     r7, [r10,#Cache_Line_Size]!
                LDR     r0, [r10,#Cache_Line_Size]!
                LDR     r1, [r10,#Cache_Line_Size]!
                LDR     r2, [r10,#Cache_Line_Size]!
                LDR     r3, [r10,#Cache_Line_Size]!
                LDR     r4, [r10,#Cache_Line_Size]!
                LDR     r5, [r10,#Cache_Line_Size]!
                LDR     r6, [r10,#Cache_Line_Size]!
                LDR     r7, [r10,#Cache_Line_Size]!
                LDR     r0, [r10,#Cache_Line_Size]!
                LDR     r1, [r10,#Cache_Line_Size]!
                LDR     r2, [r10,#Cache_Line_Size]!
                LDR     r3, [r10,#Cache_Line_Size]!
                LDR     r4, [r10,#Cache_Line_Size]!
                LDR     r5, [r10,#Cache_Line_Size]!
                LDR     r6, [r10,#Cache_Line_Size]!
                LDR     r7, [r10,#Cache_Line_Size]!
                LDR     r0, [r10,#Cache_Line_Size]!
                LDR     r1, [r10,#Cache_Line_Size]!
                LDR     r2, [r10,#Cache_Line_Size]!
                LDR     r3, [r10,#Cache_Line_Size]!
                LDR     r4, [r10,#Cache_Line_Size]!
                LDR     r5, [r10,#Cache_Line_Size]!
                LDR     r6, [r10,#Cache_Line_Size]!
                LDR     r7, [r10,#Cache_Line_Size]!
                LDR     r0, [r10,#Cache_Line_Size]!
                LDR     r1, [r10,#Cache_Line_Size]!
                LDR     r2, [r10,#Cache_Line_Size]!
                LDR     r3, [r10,#Cache_Line_Size]!
                LDR     r4, [r10,#Cache_Line_Size]!
                LDR     r5, [r10,#Cache_Line_Size]!
                LDR     r6, [r10,#Cache_Line_Size]!
                LDR     r7, [r10,#Cache_Line_Size]!
                LDR     r0, [r10,#Cache_Line_Size]!
                LDR     r1, [r10,#Cache_Line_Size]!
                LDR     r2, [r10,#Cache_Line_Size]!
                LDR     r3, [r10,#Cache_Line_Size]!
                LDR     r4, [r10,#Cache_Line_Size]!
                LDR     r5, [r10,#Cache_Line_Size]!
                LDR     r6, [r10,#Cache_Line_Size]!
                LDR     r7, [r10,#Cache_Line_Size]!

                MOV      r7, #0
                MCR      p15,0,r7,c7,c10,4
                @ $src is a variable
                @ r0~r7 is in used
                ADD      r10,r10,#0x40
                SUBS     r9,r9,#1
                BCS      _next_4k_load

                LDR   r9, =COPY_SIZE

                @MEM_CMP3_64_INC r12, r9, r8, test_error
                MOV      r6, r12        @ destination address
                SUB      r7, r9, #0x40    @ address count
                MOV      r4, r8


MEM_CMP3_64_INC:

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
                BCS            MEM_CMP3_64_INC

                @$src, $size, $init, $fail_label --> all variable will not be changed
                @ r0~r7 is in used

@sync here
@spin_lock_1st
@        LDR   r0, =scu_pc_sync_adr
@        @LDR   r0, [r0]
@                @LDR   r0, =sync_adr
@        MOV   r2, #1
@                LDREX r1, [r0]
@        CMP   r1, #0
@        STREXEQ r1, r2, [r0]
@        CMPEQ r1, #0
@        BNE   spin_lock_1st
@
@                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
@                AND   r0, r0, #0x3          @ Extract CPU ID bits
@                LSL   r0, r0, #6
@
@                LDR   r3, =scu_pc_sync_flag
@                @LDR   r3, [r3]
@                ADD   r5, r3, r0 @ offset to every cpu's location
@                @LDR   r3, =sync_flag
@                LDR   r4, =0xf
@                STR   r4, [r5]
@                DMB
@
@                @MOV   r2, #0
@                @DMB
@                @STR   r2, [r0]
@copy start


                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3          @ Extract CPU ID bits
		LSL   r4, r0, #6

                LDR   r3, =scu_pc_sync_flag
                @LDR   r3, [r3]
		ADD   r5, r3, r4 @ offset to every cpu's location
                @LDR   r3, =sync_flag
		LDR   r4, =0xf
                STR   r4, [r5]
		DMB

		CMP   r0, #0
		BNE   sync_1st_0
#if 0
                LDR   r5, =EFUSE_CPU_ADR
		LDR   r4,[r5]
		LSR   r5, r4, #24
		AND   r4, r5, #0xF
#else
        	MOV   r4, #0xC   @set dual core for fpga
#endif
		CMP   r4, #0x0
		BEQ   sync_1st_0 @ quad core efuse will go to normal branch
		CMP   r4, #0x8
		BEQ   test_error @ three core efuse will test error
		CMP   r4, #0xC
		BEQ   dual_core_init@ dual core efuse will go to dual core init
		CMP   r4, #0xE
		BEQ   test_error @ single core efuse will test error
dual_core_init:
                LDR   r3, =scu_pc_sync_flag
                @LDR   r3, [r3]
	        LDR   r4,=0x3
		LSL   r4, r4, #6
		ADD   r5, r3, r4 @ offset to every cpu's location
                @LDR   r3, =sync_flag
		LDR   r4, =0xf
                STR   r4, [r5]
                STR   r4, [r5,#4]
	        LDR   r4,=0x2
		LSL   r4, r4, #6
		ADD   r5, r3, r4 @ offset to every cpu's location
                @LDR   r3, =sync_flag
		LDR   r4, =0xf
                STR   r4, [r5]
                STR   r4, [r5,#4]
		DMB
@ copy end
sync_1st_0:
                LDR   r0,=scu_error_flag
				LDR   r0,[r0]
				cmp   r0,#0xff
				beq   test_pass
                LDR   r4, [r3]
                CMP   r4, #0xf
                BNE   sync_1st_0
                LDR   r4, [r3,#0x40]
                CMP   r4, #0xf
                BNE   sync_1st_0
                LDR   r4, [r3,#0x80]
                CMP   r4, #0xf
                BNE   sync_1st_0
                LDR   r4, [r3,#0xC0]
                CMP   r4, #0xf
                BNE   sync_1st_0
@copy start
#if 0
                LDR   r0, =EFUSE_CPU_ADR
		LDR   r8,[r0]
		LSR   r0, r8, #24
		AND   r8, r0, #0xF
#else
	        MOV   r8, #0xC   @set dual core for fpga
#endif
		CMP   r8, #0x8
		BEQ   test_error @ three core efuse will test error
		CMP   r8, #0xE
		BEQ   test_error @ single core efuse will test error
		CMP   r8, #0xC
		MOVEQ r8, #0x1
		MOVNE r8, #0x3

@copy end
                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3          @ Extract CPU ID bits
                ADD   r0, r0, #0x1          @ increase 1 to test other CPU's content
                AND   r8, r0, r8            @ wrap

                LDR   r10, =PC_sync_L1_MEMORY
                LDR   r10, [r10]
                @LDR   r10, =L1_MEMORY
                LDR   r9,  =COPY_SIZE
                MUL   r12, r9, r8
                ADD   r12, r10, r12          @ shift address by CPUID * COPY_SIZE , every cpu initilization its own address

                @MEM_CMP4_64_INC r12, r9, r8, test_error
                MOV    r6,  r12        @ destination address
                SUB    r7,  r9, #0x40 @ address count
                MOV    r4,  r8

MEM_CMP4_64_INC:

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
                BCS            MEM_CMP4_64_INC
                @$src, $size, $init, $fail_label --> all variable will not be changed
                @ r0~r7 is in used

@spin_lock_2nd
@        LDR   r0, =scu_pc_sync_adr
@        @LDR   r0, [r0]
@                @LDR   r0, =sync_adr
@        MOV   r2, #1
@                LDREX r1, [r0]
@        CMP   r1, #0
@        STREXEQ r1, r2, [r0]
@        CMPEQ r1, #0
@        BNE   spin_lock_2nd

                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3          @ Extract CPU ID bits
                LSL   r0, r0, #6

                LDR   r3, =scu_pc_sync_flag
                ADD   r3, r3, #4 @ choose 2nd word to check
                @LDR   r3, [r3]
                ADD   r5, r3, r0 @ offset to every cpu's location
                @LDR   r3, =sync_flag
                LDR   r4, =0xf
                STR   r4, [r5]
                DMB

sync_1st_1:
                LDR   r0,=scu_error_flag
				LDR   r0,[r0]
				cmp   r0,#0xff
				beq   test_pass
                LDR   r4, [r3]
                CMP   r4, #0xf
                BNE   sync_1st_1
                LDR   r4, [r3,#0x40]
                CMP   r4, #0xf
                BNE   sync_1st_1
                LDR   r4, [r3,#0x80]
                CMP   r4, #0xf
                BNE   sync_1st_1
                LDR   r4, [r3,#0xC0]
                CMP   r4, #0xf
                BNE   sync_1st_1
@copy start
#if 0
                LDR   r3, =EFUSE_CPU_ADR
		LDR   r4,[r3]
		LSR   r3, r4, #24
		AND   r4, r3, #0xF
#else
		MOV   r4, #0xC   @set dual core for fpga
#endif
		CMP   r4, #0xC
		BNE   quad_core_running

		MOVEQ r0, #0x1
		MOVNE r0, #0x3
		ADD   r8, r8, #0x1          @ increase 1 to test other CPU's content
                AND   r8, r8, r0            @ wrap
		MUL   r12, r9, r8
		ADD   r12, r10, r12         @ shift address by CPUID * COPY_SIZE , every cpu initilization its own address
		BEQ   dual_core_inv

quad_core_running:
@copy end

                ADD   r8, r8, #0x1          @ increase 1 to test other CPU's content
                AND   r8, r8, #0x3          @ wrap
                MUL   r12, r9, r8
                ADD   r12, r10, r12          @ shift address by CPUID * COPY_SIZE , every cpu initilization its own address

                @MEM_CMP5_64_INC r12, r9, r8, test_error
                MOV   r6, r12        @ destination address
                SUB   r7, r9, #0x40 @address count
                MOV   r4, r8
MEM_CMP5_64_INC:

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
                BCS            MEM_CMP5_64_INC

                @$src, $size, $init, $fail_label --> all variable will not be changed
                @ r0~r7 is in used

@spin_lock_3rd
@        LDR   r0, =scu_pc_sync_adr
@        @LDR   r0, [r0]
@                @LDR   r0, =sync_adr
@        MOV   r2, #1
@                LDREX r1, [r0]
@        CMP   r1, #0
@        STREXEQ r1, r2, [r0]
@        CMPEQ r1, #0
@        BNE   spin_lock_3rd

                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3          @ Extract CPU ID bits
                LSL   r0, r0, #6

                LDR   r3, =scu_pc_sync_flag
                ADD   r3, r3, #8 @ choose 3rd word to check
                @LDR   r3, [r3]
                ADD   r5, r3, r0 @ offset to every cpu's location
                @LDR   r3, =sync_flag
                LDR   r4, =0xf
                STR   r4, [r5]
                DMB

sync_1st_2:
                LDR   r0,=scu_error_flag
				LDR   r0,[r0]
				cmp   r0,#0xff
				beq   test_pass
                LDR   r4, [r3]
                CMP   r4, #0xf
                BNE   sync_1st_2
                LDR   r4, [r3,#0x40]
                CMP   r4, #0xf
                BNE   sync_1st_2
                LDR   r4, [r3,#0x80]
                CMP   r4, #0xf
                BNE   sync_1st_2
                LDR   r4, [r3,#0xC0]
                CMP   r4, #0xf
                BNE   sync_1st_2

                ADD   r8, r8, #0x1          @ increase 1 to test other CPU's content
                AND   r8, r8, #0x1          @ wrap
                MUL   r12, r9, r8
                ADD   r12, r10, r12          @ shift address by CPUID * COPY_SIZE , every cpu initilization its own address

                @MEM_CMP6_64_INC r12, r9, r8, test_error
                MOV             r6,r12@ destination address
                SUB   r7, r9, #0x40 @ address count
                MOV   r4, r8


MEM_CMP6_64_INC:

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
                BCS            MEM_CMP6_64_INC
                @$src, $size, $init, $fail_label --> all variable will not be changed
                @ r0~r7 is in used

@spin_lock_4th
@        LDR   r0, =scu_pc_sync_adr
@        @LDR   r0, [r0]
@                @LDR   r0, =sync_adr
@        MOV   r2, #1
@                LDREX r1, [r0]
@        CMP   r1, #0
@        STREXEQ r1, r2, [r0]
@        CMPEQ r1, #0
@        BNE   spin_lock_4th

                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3          @ Extract CPU ID bits
                LSL   r0, r0, #6

                LDR   r3, =scu_pc_sync_flag
                ADD   r3, r3, #12 @ choose 4th word to check
                @LDR   r3, [r3]
                ADD   r5, r3, r0 @ offset to every cpu's location
                @LDR   r3, =sync_flag
                LDR   r4, =0xf
                STR   r4, [r5]
                DMB

sync_1st_3:
		LDR   r0,=scu_test_flag
		mov   r4, #0x34
		str   r4, [r0]

	        LDR   r0,=scu_error_flag
				LDR   r0,[r0]
				cmp   r0,#0xff
				beq   test_pass
                LDR   r4, [r3]
                CMP   r4, #0xf
                BNE   sync_1st_3
                LDR   r4, [r3,#0x40]
                CMP   r4, #0xf
                BNE   sync_1st_3
                LDR   r4, [r3,#0x80]
                CMP   r4, #0xf
                BNE   sync_1st_3
                LDR   r4, [r3,#0xC0]
                CMP   r4, #0xf
                BNE   sync_1st_3

                ADD   r8, r8, #0x1          @ increase 1 to test other CPU's content
                AND   r8, r8, #0x1          @ wrap
                MUL   r12, r9, r8
                ADD   r12, r10, r12         @ shift address by CPUID * COPY_SIZE , every cpu initilization its own address
@copy start
dual_core_inv:
@copy end
                LDR     r2, =0x4000
                LDR   r3, =PC_sync_L1_MEMORY
                LDR   r3, [r3]
                @LDR     r3, =L1_MEMORY
                ADD     r2, r2, r3
d_inv_loop:
                MCR    p15,0,r3,c7,c6,1    @ invalidate dcache by set / way
                ADD     r3,r3,#0x40

                CMP     r2,r3
                BNE     d_inv_loop

                MRC   p15, 0, r0, c0, c0, 5 @ Read Multiprocessor Affinity Register
                AND   r0, r0, #0x3          @ Extract CPU ID bits
                LSL   r1, r0, #6

                LDR   r0, =scu_pc_sync_flag
                ADD   r0, r1, r0@ clear by itself cpu
                @LDR   r0, [r0]
                @LDR   r0, =sync_flag
                MOV   r1, #0
                STR   r1, [r0]
                STR   r1, [r0,#0x4]
                STR   r1, [r0,#0x8]
                STR   r1, [r0,#0xc]

                SUBS     r11, r11, #0x01
                BCS      _next_iter_loop

                B       test_pass

test_error:
                MOV  r0, #0xB
				mov  r1, #0xff
				LDR  r2,=scu_error_flag
				str  r1,[r2]
                B    test_end

test_pass :
                MOV  r0, #0xA
                B    test_end

test_end:
                LDMIA  sp!, {r1-r12}
                BX lr

        .align   5
.globl scu_pc_sync_adr
scu_pc_sync_adr:
	.space 0x200


        .align   5
.globl	scu_pc_sync_flag
scu_pc_sync_flag:
	.space 0x200


PC_sync_L1_MEMORY:
                .word 0

                .END
