@-------------------------------------------------------------------------------
@ The confidential and proprietary information contained in this file may
@ only be used by a person authorised under and to the extent permitted
@ by a subsisting licensing agreement from ARM Limited.
@
@            (C) COPYRIGHT 2009-2012  ARM Limited.
@                ALL RIGHTS RESERVED
@
@ This entire notice must be reproduced on all copies of this file
@ and copies of this file may only be made by a person if such person is
@ permitted to do so under the terms of a subsisting license agreement
@ from ARM Limited.
@
@      SVN Information
@
@      Checked In          : $Date: 2009-08-20 11:00:24 +0100 (Thu, 20 Aug 2009) $
@
@      Revision            : $Revision: 115814 $
@
@      Release Information : CORTEX-A7-r0p3-00rel0
@
@-------------------------------------------------------------------------------

@-------------------------------------------------------------------------------
@
@ Description:  Test to execute Advanced SIMD instructions in
@               Cortex-A7 MPCore processor.
@
@-------------------------------------------------------------------------------

@-------------------------------------------------------------------------------
@ Init Code
@-------------------------------------------------------------------------------
                .text
                .align 5

.macro NEON_TEST sFlag, eFlag, i_dval1, i_dval2, i_dval3, i_dval4, i_reg1, i_reg2, i_reg3, i_reg4, e_dval1, e_dval2, e_dval3, e_dval4, e_reg1, e_reg2, e_reg3, e_reg4, e_rval0, e_rval1, e_rval2, instr0, instr1, instr2, instr3, instr4, instr5, instr6, instr7
                STMDB   sp!, {r0-r12}
                .ifnc   "\sFlag", "-"
                    LDR     R0,=\sFlag
                    MSR     CPSR_sf,R0                      @ Set flags
                .else
                    MOV     R0,#0
                    MSR     CPSR_sf,R0                      @ Reset flags
                .endif

        @ Reset ARM registers
                MOV     R0, #0x0
                MOV     R1, #0x0
                MOV     R2, #0x0
                MOV     R3, #0x0
                MOV     R4, #0x0
                MOV     R5, #0x0
                MOV     R6, #0x0
                MOV     R7, #0x0
                MOV     R8, #0x0
                MOV     R9, #0x0
                MOV     R10, #0x0
                MOV     R11, #0x0
                MOV     R12, #0x0


         @RESET_D0_TO_D31
                LDR r0,  =zero
                VLD1.32  { d0[],d1[]}, [r0]
                VLD1.32  { d2[],d3[]}, [r0]
                VLD1.32  { d4[],d5[]}, [r0]
                VLD1.32  { d6[],d7[]}, [r0]
                VLD1.32  { d8[],d9[]}, [r0]
                VLD1.32  {d10[],d11[]}, [r0]
                VLD1.32  {d12[],d13[]}, [r0]
                VLD1.32  {d14[],d15[]}, [r0]
                VLD1.32  {d16[],d17[]}, [r0]
                VLD1.32  {d18[],d19[]}, [r0]
                VLD1.32  {d20[],d21[]}, [r0]
                VLD1.32  {d22[],d23[]}, [r0]
                VLD1.32  {d24[],d25[]}, [r0]
                VLD1.32  {d26[],d27[]}, [r0]
                VLD1.32  {d28[],d29[]}, [r0]
                VLD1.32  {d30[],d31[]}, [r0]

        @ Initialise the value of input register1
        @       [       "$i_reg1" /= "-" :LAND: "$i_dval1" /= "-"
                .ifnc   "\i_reg1", "-"
                    .ifnc   "\i_dval1", "-"
                        ADR r0, i_dval1\@
                        VLDR \i_reg1, [r0]
                    .endif
                .endif
        @       ]

        @ Initialise the value of input register2
                @[       "$i_reg2" /= "-" :LAND: "$i_dval2" /= "-"
                .ifnc   "\i_reg2", "-"
                    .ifnc   "\i_dval2", "-"
                        ADR r0, i_dval2$l
                        VLDR \i_reg2, [r0]
                    .endif
                .endif
                @]

        @ Initialise the value of input register3
                @[       "$i_reg3" /= "-" :LAND: "$i_dval3" /= "-"
                .ifnc   "\i_reg3", "-"
                    .ifnc   "\i_dval3", "-"
                        ADR r0, i_dval3$l
                        VLDR \i_reg3, [r0]
                    .endif
                .endif
                @]

        @ Initialise the value of input register4
                @[       "$i_reg4" /= "-" :LAND: "$i_dval4" /= "-"
                .ifnc   "\i_reg4", "-"
                    .ifnc   "\i_dval4", "-"
                        ADR r0, i_dval4$l
                        VLDR \i_reg4, [r0]
                    .endif
                .endif
                @]


        @ Execute the instruction sequence
0:
                \instr0
1:
                \instr1
2:
                \instr2
3:
                \instr3
4:
                \instr4
5:
                \instr5
6:
                \instr6
7:
                \instr7

                MRS     R12,CPSR                 @ Copy CPSR in R2 just after
                                                 @ executing the IUT

                @ Check CPSR flags
                @.if     \eFlag != '-'
                .ifnc   "\eFlag", "-"
                    LDR     r10,=\eFlag
                    LDR     r11,=CPSR_FLGS_MASK
                    AND     R11,R11,R12
                    TEQ     R10,R11
                    BNE     Neon_test_fail
                .endif

                @ Check value of R0 register
                @[       "$e_rval0" /= "-"
                .ifnc   "\e_rval0", "-"
                LDR     r10,=\e_rval0
                TEQ     r0,r10
                BNE     Neon_test_fail
                @]
                .endif

                @ Check value of R1 register
                @[       "$e_rval1" /= "-"
                .ifnc   "\e_rval1", "-"
                LDR     r10,=\e_rval1
                TEQ     r1,r10
                BNE     Neon_test_fail
                @]
                .endif

                @ Check value of R2 register
                @[       "$e_rval2" /= "-"
                .ifnc   "\e_rval2", "-"
                LDR     r10,=\e_rval2
                TEQ     r2,r10
                BNE     Neon_test_fail
                @]
                .endif

        @ Check the value of the first output dw register
                @.if       \e_dval1 <> '-' && \e_reg1 <> '-'
               .ifnc    "\e_dval1", "-"
                    .ifnc   "\e_reg1", "-"
                    @LDR     R0,=store_DW1\l
                    LDR     R0,=store_DW1\@
                    LDRD    R10, [R0]
                    VMOV    R0, R1, \e_reg1
                    TEQ     R0,R10
                    TEQEQ   R1,R11
                    BNE     Neon_test_fail
                    .endif
                .endif

        @ Check the value of the second output dw register
                @.if       \e_dval2 <> '-' && \e_reg2 <> '-'
                .ifnc   "\e_dval2", "-"
                    .ifnc   "\e_reg2", "-"
                    @LDR     R0,=store_DW2\l
                    LDR     R0,=store_DW2\@
                    LDRD    R10, [R0]
                    VMOV    R0, R1, \e_reg2
                    TEQ     R0,R10
                    TEQEQ   R1,R11
                    BNE     Neon_test_fail
                    .endif
                .endif

        @ Check the value of the third output dw register
                @.if       \e_dval3 <> '-' && \e_reg3 <> '-'
                .ifnc   "\e_dval3", "-"
                    .ifnc   "\e_reg3", "-"
                    @LDR     R0,=store_DW3\l
                    LDR     R0,=store_DW3\@
                    LDRD    R10, [R0]
                    VMOV    R0, R1, \e_reg3
                    TEQ     R0,R10
                    TEQEQ   R1,R11
                    BNE     Neon_test_fail
                    .endif
                .endif

        @ Check the value of the fourth output dw register
                @.if       \e_dval4 <> '-' && \e_reg4 <> '-'
                .ifnc   "\e_dval4", "-"
                    .ifnc   "\e_reg4", "-"
                    @LDR     R0,=store_DW4\l
                    LDR     R0,=store_DW4\@
                    LDRD    R10, [R0]
                    VMOV    R0, R1, \e_reg4
                    TEQ     R0,R10
                    TEQEQ   R1,R11
                    BNE     Neon_test_fail
                    .endif
                .endif


                @B       TestEnd\l
                B       TestEnd\@

                .align

                @.if       \e_dval1 <> '-' && \e_reg1 <> '-'
                .ifnc   "\e_dval1", "-"
                    .ifnc   "\e_reg1", "-"
@store_DW1\l     DCQ     \e_dval1
store_DW1\@:
                .octa   \e_dval1
                    .endif
                .endif
                @.if       \e_dval2 <> '-' && \e_reg2 <> '-'
                .ifnc   "\e_dval2", "-"
                    .ifnc   "\e_reg2", "-"
@store_DW2\l     DCQ     \e_dval2
store_DW2\@:
                .octa   \e_dval2
                    .endif
                .endif
                @.if       \e_dval3 <> '-' && \e_reg3 <> '-'
                .ifnc   "\e_dval3", "-"
                    .ifnc   "\e_reg3", "-"
@store_DW3\l     DCQ     \e_dval3
store_DW3\@:
                .octa \e_dval3
                    .endif
                .endif
                @.if       \e_dval4 <> '-' && \e_reg4 <> '-'
                .ifnc   "\e_dval4", "-"
                    .ifnc   "\e_reg4", "-"
@store_DW4\l     DCQ     \e_dval4
store_DW4\@:
                .octa   \e_dval4
                    .endif
                .endif

                @.if       \i_reg1 <> '-' && \i_dval1 <> '-'
                .ifnc   "\i_reg1", "-"
                    .ifnc   "\i_dval1", "-"
@i_dval1\l     DCQ     \i_dval1
i_dval1\@:
                .octa   \i_dval1
                    .endif
                .endif
                @.if       \i_reg2 <> '-' && \i_dval2 <> '-'
                .ifnc   "\i_reg2", "-"
                    .ifnc   "\i_dval2", "-"
@i_dval2\l     DCQ     \i_dval2
i_dval2\@:
                .octa   \i_dval2
                    .endif
                .endif
                @.if       \i_reg3 <> '-' && \i_dval3 <> '-'
                .ifnc   "\i_reg3", "-"
                    .ifnc   "\i_dval3", "-"
@i_dval3\l     DCQ     \i_dval3
i_dval3\@:
                .octa \i_dval3
                    .endif
                .endif
                @.if       \i_reg4 <> '-' && \i_dval4 <> '-'
                .ifnc   "\i_reg4", "-"
                    .ifnc   "\i_dval4", "-"
@i_dval4\l     DCQ     \i_dval4
i_dval4\@:
                .octa   \i_dval4
                    .endif
                .endif

                @ALIGN
                .align



@Test_pass\l:
@TestDone\l:

@Test_fail\l:
@TestEnd\l:

Test_pass\@:
TestDone\@:

Test_fail\@:
TestEnd\@:
                LDMIA  sp!, {r0-r12}
@               LOCAL_END
    .endm

.macro NEON_INTLK_TEST sFlag, eFlag, e_dval1, e_dval2, e_dval3, e_dval4, e_dval5, e_dval6, e_reg1, e_reg2, e_reg3, e_reg4, e_reg5, e_reg6, e_rval1, e_rval2, e_rval3, e_rval4, e_rreg1, e_rreg2, e_rreg3, e_rreg4, instr0, instr1, instr2, instr3, instr4, instr5, instr6, instr7
                STMDB   sp!, {r0-r12}
                .ifnc   "\sFlag", "-"
                    LDR     R0,=\sFlag
                    MSR     CPSR_sf,R0                      @ Set flags
                .else
                    MOV     R0,#0
                    MSR     CPSR_sf,R0                      @ Reset flags
                .endif

        @ Reset ARM registers
                MOV     R0, #0x0
                MOV     R1, #0x0
                MOV     R2, #0x0
                MOV     R3, #0x0
                MOV     R4, #0x0
                MOV     R5, #0x0
                MOV     R6, #0x0
                MOV     R7, #0x0
                MOV     R8, #0x0
                MOV     R9, #0x0
                MOV     R10, #0x0
                MOV     R11, #0x0
                MOV     R12, #0x0


                @ Initialize neon registers
                @;comment out R0 backup
                @;STR     R0,label0
                LDR R0, =INTL_DATA_ADDR


                VLD1.32 {d0,  d1,  d2,  d3 }, [r0]!
                VLD1.32 {d4,  d5,  d6,  d7 }, [r0]!
                VLD1.32 {d8,  d9,  d10, d11}, [r0]!
                VLD1.32 {d12, d13, d14, d15}, [r0]!
                VLD1.32 {d16, d17, d18, d19}, [r0]!
                VLD1.32 {d20, d21, d22, d23}, [r0]!
                VLD1.32 {d24, d25, d26, d27}, [r0]!
                VLD1.32 {d28, d29, d30, d31}, [r0]!
                @;comment out R0 restore
                @;LDR     R0,label0
                @;add R0 restore
                MOV R0,#0x0

        @ Execute the instruction sequence
0:
                \instr0
1:
                \instr1
2:
                \instr2
3:
                \instr3
4:
                \instr4
5:
                \instr5
6:
                \instr6
7:
                \instr7

                @;comment out R0,R1,R2 backup
                @;STR r0, label0
                @;STR r1, label1
                @;STR r2, label2

                MRS     R2,CPSR                 @ Copy CPSR in R2 just after
                                                @ executing the IUT

            @ Check CPSR flags
                @.if     \eFlag != '-'
                .ifnc   "\eFlag", "-"
                    LDR     R0,=\eFlag
                    LDR     R1,=CPSR_FLGS_MASK
                    AND     R1,R1,R2
                    TEQ     R0,R1
                    BNE     Neon_test_fail
                .endif


            @ Check for the first ARM destination register
                @.if    \e_rreg1 <> '-' && \e_rval1 <> '-'
                .ifnc   "\e_rreg1", "-"
                    .ifnc   "\e_rval1", "-"
                        MOV     R14,\e_rreg1
                        LDR     R13,=\e_rval1
                        LDR     R11, =e_rreg1\@
                        TEQ     R13,R14
                        BNE     Neon_test_fail
                    .endif
                .endif

            @ Check for the second ARM destination register
                @.if    \e_rreg2 <> '-' && \e_rval2 <> '-'
                .ifnc   "\e_rreg2", "-"
                    .ifnc   "\e_rval2", "-"
                        MOV     R14,\e_rreg2
                        LDR     R13,=\e_rval2
                        LDR     R11, =e_rreg2\@
                        TEQ     R13,R14
                        BNE     Neon_test_fail
                    .endif
                .endif

            @ Check for the third ARM destination register
                @.if    \e_rreg3 <> '-' && \e_rval3 <> '-'
                .ifnc   "\e_rreg3", "-"
                    .ifnc   "\e_rval3", "-"
                        MOV     R14,\e_rreg3
                        LDR     R13,=\e_rval3
                        LDR     R11, =e_rreg3\@
                        TEQ     R13,R14
                        BNE     Neon_test_fail
                    .endif
                .endif

            @ Check for the fourth ARM destination register
                @.if    \e_rreg4 <> '-' && \e_rval4 <> '-'
                .ifnc   "\e_rreg4", "-"
                    .ifnc   "\e_rval4", "-"
                        MOV     R14,\e_rreg4
                        LDR     R13,=\e_rval4
                        LDR     R11, =e_rreg4\@
                        TEQ     R13,R14
                        BNE     Neon_test_fail
                    .endif
                .endif

            @ Check the value of the first output dw register
                @.if       \e_dval1 <> '-' && \e_reg1 <> '-'
               .ifnc    "\e_dval1", "-"
                    .ifnc   "\e_reg1", "-"
                        @LDR     R0,=store_DW1\l
                        LDR     R0,=store_DW1\@
                        LDRD    R10, [R0]
                        VMOV    R0, R1, \e_reg1
                        TEQ     R0,R10
                        TEQEQ   R1,R11
                        BNE     Neon_test_fail
                    .endif
                .endif

            @ Check the value of the second output dw register
                @.if       \e_dval2 <> '-' && \e_reg2 <> '-'
                .ifnc   "\e_dval2", "-"
                    .ifnc   "\e_reg2", "-"
                    @LDR     R0,=store_DW2\l
                        LDR     R0,=store_DW2\@
                        LDRD    R10, [R0]
                        VMOV    R0, R1, \e_reg2
                        TEQ     R0,R10
                        TEQEQ   R1,R11
                        BNE     Neon_test_fail
                    .endif
                .endif

            @ Check the value of the third output dw register
                @.if       \e_dval3 <> '-' && \e_reg3 <> '-'
                .ifnc   "\e_dval3", "-"
                    .ifnc   "\e_reg3", "-"
                        @LDR     R0,=store_DW3\l
                        LDR     R0,=store_DW3\@
                        LDRD    R10, [R0]
                        VMOV    R0, R1, \e_reg3
                        TEQ     R0,R10
                        TEQEQ   R1,R11
                        BNE     Neon_test_fail
                    .endif
                .endif

            @ Check the value of the fourth output dw register
                @.if       \e_dval4 <> '-' && \e_reg4 <> '-'
                .ifnc   "\e_dval4", "-"
                    .ifnc   "\e_reg4", "-"
                        @LDR     R0,=store_DW4\l
                        LDR     R0,=store_DW4\@
                        LDRD    R10, [R0]
                        VMOV    R0, R1, \e_reg4
                        TEQ     R0,R10
                        TEQEQ   R1,R11
                        BNE     Neon_test_fail
                    .endif
                .endif

            @ Check the value of the fifth output dw register
                @.if       \e_dval5 <> '-' && \e_reg5 <> '-'
                .ifnc   "\e_dval5", "-"
                    .ifnc   "\e_reg5", "-"
                        @LDR     R0,=store_DW5\l
                        LDR     R0,=store_DW5\@
                        LDRD    R10, [R0]
                        VMOV    R0, R1, \e_reg5
                        TEQ     R0,R10
                        TEQEQ   R1,R11
                        BNE     Neon_test_fail
                    .endif
                .endif
            @ Check the value of the sixth output dw register
                @.if       \e_dval6 <> '-' && \e_reg6 <> '-'
                .ifnc   "\e_dval6", "-"
                    .ifnc   "\e_reg6", "-"
                        @LDR     R0,=store_DW6\l
                        LDR     R0,=store_DW6\@
                        LDRD    R10, [R0]
                        VMOV    R0, R1, \e_reg6
                        TEQ     R0,R10
                        TEQEQ   R1,R11
                        BNE     Neon_test_fail
                    .endif
                .endif

                @B       TestDone\l
                B       TestDone\@

                @ALIGN 4
                .align

                @.if       \e_dval1 <> '-' && \e_reg1 <> '-'
                .ifnc   "\e_dval1", "-"
                    .ifnc   "\e_reg1", "-"
@store_DW1\l     DCQ     \e_dval1
store_DW1\@:
                .octa   \e_dval1
                    .endif
                .endif
                @.if       \e_dval2 <> '-' && \e_reg2 <> '-'
                .ifnc   "\e_dval2", "-"
                    .ifnc   "\e_reg2", "-"
@store_DW2\l     DCQ     \e_dval2
store_DW2\@:
                .octa   \e_dval2
                    .endif
                .endif
                @.if       \e_dval3 <> '-' && \e_reg3 <> '-'
                .ifnc   "\e_dval3", "-"
                    .ifnc   "\e_reg3", "-"
@store_DW3\l     DCQ     \e_dval3
store_DW3\@:
                .octa \e_dval3
                    .endif
                .endif
                @.if       \e_dval4 <> '-' && \e_reg4 <> '-'
                .ifnc   "\e_dval4", "-"
                    .ifnc   "\e_reg4", "-"
@store_DW4\l     DCQ     \e_dval4
store_DW4\@:
                .octa   \e_dval4
                    .endif
                .endif
                @.if       \e_dval5 <> '-' && \e_reg5 <> '-'
                .ifnc   "\e_dval5", "-"
                    .ifnc   "\e_reg5", "-"
@store_DW5\l     DCQ     \$e_dval5
store_DW5\@:
                .octa   \e_dval5
                    .endif
                .endif
                @.if       \e_dval6 <> '-' && \e_reg6 <> '-'
                .ifnc   "\e_dval6", "-"
                    .ifnc   "\e_reg6", "-"
@store_DW6\l     DCQ     \e_dval6
store_DW6\@:
                .octa   \e_dval6
                    .endif
                .endif

                @ALIGN
                .align

@Test_pass\l:
@TestDone\l:

@Test_fail\l:
@TestEnd\l:

Test_pass\@:
TestDone\@:

Test_fail\@:
TestEnd\@:
                LDMIA  sp!, {r0-r12}
@               LOCAL_END
    .endm
@-------------------------------------------------------------------------------
@ Init Code
@-------------------------------------------------------------------------------

       @ Addresses and data used by this test
       @ Note: user may need to modify the address values depending on the
       @       system memory map it run onto.
.EQU GE2Flag,              0x00040000
.EQU NFlag,                0x80000000
.EQU QFlag,                0x08000000
.EQU CPSR_FLGS_MASK,       0xf90f0000
.EQU INTL_DATA_ADDR,       neon_data
.EQU num_iteration,        0x600


                .ALIGN   5

@*****************************************************************************
@***    start of main test
@*****************************************************************************

.global                fp5_adv_simd_start
fp5_adv_simd_start:
                STMDB   sp!, {r1-r12}

@code start here

@-------------------------------------------------------------------------------
@ NEON Test
@-------------------------------------------------------------------------------

neon_test_start:
       @ Enable NEON if present
                @NEON_PRESENT r0
                MOV     r0,#0xf00000               @Write CPACR (Coprocessor Access Control Register)
                MCR     p15,0,r0,c1,c0,2           @to enable coprocessors cp10 & cp11
                MRC     p15,0,r0,c1,c0,2           @Read CPACR;
                CMP     r0, #0x00f00000            @if cp10 & cp11 are enabled and bit 31 is zero then NEON present
                MOVEQ   r0, #1
                MOVNE   r0, #0

                CMP     r0, #1
                BNE     no_neon
                @LDR     r0, =test_skipped
                @MOV     r1, #1
                @STR     r1, [r0]
                MOV     r0, #1<<30
                MCR     p10, #7, r0, c8, c0, 0   @Enable NEON by writing FPEXC
                MOV     r0, #0
                @VLDM    r0!, {d0 -d15}
                @VLDM    r0!, {d16-d31}

                LDR     r10,=num_iteration@ num of iterations round power_loop

neon_loop:
                SUBS     r10, r10, #1

       @ NEON Instructions
                @MESSAGE "- Execute VMOV Instruction\n"
@VMOV.S17_001
                NEON_TEST GE2Flag,GE2Flag, 0x3333fff0f1f3f2f4,-,-,-,d13,-,-,-,-,-,-,-,-,-,-,-,0x3333,-,-,"LDR R4,=0x0a1b2c3d","VMOV.S16 R4, d13[3]","MOV R0,R4","MOV R4,R3"

@VMOV.S17_002
                NEON_TEST GE2Flag,GE2Flag, 0x33ccafb30101f0f4,-,-,-,d14,-,-,-,-,-,-,-,-,-,-,-,0x33cc,-,-,"LDR R5,=0x0a","VMOV.S16 R5, d14[3]","MOV R0,R5"

                @MESSAGE "- Execute VADDW Instruction\n"
@VADD.I16.I16.S8_B2B_003
            NEON_INTLK_TEST QFlag, QFlag, 0x38361d2848d40309, 0x2416de2137570ebc, 0x38991da3489802b5, 0x23f9ddf437d20f1b, 0x38341d424885023a, 0x2386de2537790f31, d6, d7, d4, d5, d0, d1, -, -, -, -, -, -, -, -, "VADDW.S8  q0,q1,d5", "VADDW.S8  q2,q0,d5", "VADDW.S8  q3,q2,d6"

                @MESSAGE "- Execute VSUBW Instruction\n"
@VSUB.I32.I32.U16_B2B_004
            NEON_INTLK_TEST QFlag, QFlag, 0x37cdb49a48719e75, 0x2312d98b371ed5d2, 0x37ce521f4871dac9, 0x2312f6b8371f5b73, 0x37ceb7804871ee44, 0x23136a87371fb55d, d6, d7, d4, d5, d0, d1, -, -, -, -, -, -, -, -, "VSUBW.U16  q0,q1,d5", "VSUBW.U16  q2,q0,d5", "VSUBW.U16  q3,q2,d6"

                @MESSAGE "- Execute VMUL Instruction\n"
@VMUL.I8_B2B_005
            NEON_INTLK_TEST QFlag, QFlag, 0x1c70c090002089ff, 0xfeacb054c028bd5f, 0xf29474d4b834bd61, -, -, -, d2, d1, d0, -, -, -, -, -, -, -, -, -, -, -, "VMUL.I8  d0,d1,d2", "VMUL.I8  d1,d0,d2", "VMUL.I8  d2,d1,d0"

                @MESSAGE "- Execute VMLA Instruction\n"
@VMLA.I32.U16_B2B_006
            NEON_INTLK_TEST QFlag, QFlag, 0xfc299fafef547a9c, 0x79b7d609e2c39f7b, 0x45709189e850c137, 0x3d7008bea9dad9a1, 0x64ae0bb54804d45c, 0xc0a854e81bc4d2b3, d14, d15, d8, d9, d0, d1, -, -, -, -, -, -, -, -, "VMLAL.U16  q0,d1,d2", "VMLAL.U16  q4,d0,d1", "VMLAL.U16  q7,d8,d9"

                @MESSAGE "- Execute VCLS Instruction\n"
@VCLS.S8_B2B_007
            NEON_INTLK_TEST QFlag, QFlag, 0x0100020000020101, 0x0100010000000001, 0x0201000400030200, 0x0003010301010001, -, -, d16, d4, d5, d1, -, -, -, -, -, -, -, -, -, -, "VCLS.S8  d1,d15", "VCLS.S8  q2,q8", "VCLS.S8  d16,d12"

                @MESSAGE "- Execute VMAX Instruction\n"
@VMAX.S32_B2B_008
            NEON_INTLK_TEST QFlag, QFlag, 0x37cf1ce1487201bf, 0x37cf1ce1487201bf, 0x37cf1ce1487201bf, -, -, -, d2, d1, d0, -, -, -, -, -, -, -, -, -, -, -, "VMAX.S32  d0,d1,d2", "VMAX.S32  d1,d0,d2", "VMAX.S32  d2,d1,d0"

                @MESSAGE "- Execute VSHL Instruction\n"
@VSHL.I64_B2B_009
            NEON_INTLK_TEST QFlag, QFlag, 0x369a77ffab5f0b80, 0x3a5b0b433b0a78a8, -, -, -, -, d6, d1, -, -, -, -, -, -, -, -, -, -, -, -, "VSHL.I64  d1,d6,#1", "VSHL.I64  d6,d7,#2", "VSHL.I64  d6,d13,#3"

@VMOV_010
            NEON_TEST NFlag,NFlag,0x5555555555555555,-,-,-,d0,-,-,-,0xaaaaaaaaaaaaaaaa,-,-,-,d0,-,-,-,-,-,-,"LDR R0,=0xaaaaaaaa","LDR R1,=0xaaaaaaaa","VMOV   d0,R0,R1"

                CMP  r10,#0
                BNE  neon_loop

                B    test_pass

no_neon:
                MOV  r0, #0x2
                B    test_error

Neon_test_fail:
                LDMIA  sp!, {r0-r12}
test_error:
                MOV  r0, #0x0
                B    test_end

test_pass:
                MOV  r0, #0x1
                B    test_end

test_end:       LDMIA  sp!, {r1-r12}
                BX lr
@-------------------------------------------------------------------------------
@ Test Data
@-------------------------------------------------------------------------------
                .ALIGN
                .DATA


zero:            .WORD     0

@-------------------------------------------------------------------------------
@ Input Neon Data
@-------------------------------------------------------------------------------
                .ALIGN 5
                .data

neon_data:
                .WORD     0x46B94BFB
                .WORD     0x1C667E81
                .WORD     0xFF6ABDDF
                .WORD     0x9E2CFB54
                .WORD     0x487201BF
                .WORD     0x37CF1CE1
                .WORD     0x37200F47
                .WORD     0x2313DE56
                .WORD     0x359DAA88
                .WORD     0xC6756687
                .WORD     0x6561137B
                .WORD     0x73CF59EA
                .WORD     0x9D853C54
                .WORD     0x1D2D85A1
                .WORD     0xAAEE655B
                .WORD     0xD5502F37
                .WORD     0x3988CCE3
                .WORD     0x3DA10279
                .WORD     0xA5F8D999
                .WORD     0xF1AB768E
                .WORD     0xF368EE43
                .WORD     0x88F51F3F
                .WORD     0x3FAFBEA6
                .WORD     0xF78E4D0D
                .WORD     0x48E2DC29
                .WORD     0xDDA0E486
                .WORD     0xF56BE170
                .WORD     0xC6D34EFF
                .WORD     0x4B135805
                .WORD     0x6206FB8F
                .WORD     0xDDCB53CD
                .WORD     0x690DC509
                .WORD     0x799452D3
                .WORD     0x3AA73B48
                .WORD     0xA9F1EAB5
                .WORD     0x152E9D06
                .WORD     0xB98949B2
                .WORD     0x6C2C0613
                .WORD     0xE3228831
                .WORD     0x00171EAC
                .WORD     0xA95E7136
                .WORD     0x29C75246
                .WORD     0xA3951D16
                .WORD     0x38FD57F6
                .WORD     0x8F5E987C
                .WORD     0xCFA38874
                .WORD     0x0BBD6D71
                .WORD     0xD1765C41
                .WORD     0x2593C701
                .WORD     0xC8EC0F70
                .WORD     0xB3D691C0
                .WORD     0x91BA2F33
                .WORD     0x80D6AB33
                .WORD     0xA1D1A50D
                .WORD     0x58F73EE6
                .WORD     0x78D57E5E
                .WORD     0x53A0B1C3
                .WORD     0x868E74A6
                .WORD     0x0642C7DB
                .WORD     0x493D11A8
                .WORD     0xCE14F3BF
                .WORD     0x6CADE060
                .WORD     0xFA2AE325
                .WORD     0x60970067
                .WORD     0x46B94BFB
                .WORD     0x1C667E81
                .WORD     0xFF6ABDDF
                .WORD     0x9E2CFB54
                .WORD     0x487201BF
                .WORD     0x37CF1CE1
                .WORD     0x37200F47
                .WORD     0x2313DE56
                .WORD     0x359DAA88
                .WORD     0xC6756687
                .WORD     0x6561137B
                .WORD     0x73CF59EA
                .WORD     0x9D853C54
                .WORD     0x1D2D85A1
                .WORD     0xAAEE655B
                .WORD     0xD5502F37
                .WORD     0x3988CCE3
                .WORD     0x3DA10279
                .WORD     0xA5F8D999
                .WORD     0xF1AB768E
                .WORD     0xF368EE43
                .WORD     0x88F51F3F
                .WORD     0x3FAFBEA6
                .WORD     0xF78E4D0D
                .WORD     0x48E2DC29
                .WORD     0xDDA0E486
                .WORD     0xF56BE170
                .WORD     0xC6D34EFF
                .WORD     0x4B135805
                .WORD     0x6206FB8F
                .WORD     0xDDCB53CD
                .WORD     0x690DC509
                .WORD     0x799452D3
                .WORD     0x3AA73B48
                .WORD     0xA9F1EAB5
                .WORD     0x152E9D06
                .WORD     0xB98949B2
                .WORD     0x6C2C0613
                .WORD     0xE3228831
                .WORD     0x00171EAC
                .WORD     0xA95E7136
                .WORD     0x29C75246
                .WORD     0xA3951D16
                .WORD     0x38FD57F6
                .WORD     0x8F5E987C
                .WORD     0xCFA38874
                .WORD     0x0BBD6D71
                .WORD     0xD1765C41
                .WORD     0x2593C701
                .WORD     0xC8EC0F70
                .WORD     0xB3D691C0
                .WORD     0x91BA2F33
                .WORD     0x80D6AB33
                .WORD     0xA1D1A50D
                .WORD     0x58F73EE6
                .WORD     0x78D57E5E
                .WORD     0x53A0B1C3
                .WORD     0x868E74A6
                .WORD     0x0642C7DB
                .WORD     0x493D11A8
                .WORD     0xCE14F3BF
                .WORD     0x6CADE060
                .WORD     0xFA2AE325
                .WORD     0x60970067
                .WORD     0x46B94BFB
                .WORD     0x1C667E81
                .WORD     0xFF6ABDDF
                .WORD     0x9E2CFB54
                .WORD     0x487201BF
                .WORD     0x37CF1CE1
                .WORD     0x37200F47
                .WORD     0x2313DE56
                .WORD     0x359DAA88
                .WORD     0xC6756687
                .WORD     0x6561137B
                .WORD     0x73CF59EA
                .WORD     0x9D853C54
                .WORD     0x1D2D85A1
                .WORD     0xAAEE655B
                .WORD     0xD5502F37
                .WORD     0x3988CCE3
                .WORD     0x3DA10279
                .WORD     0xA5F8D999
                .WORD     0xF1AB768E
                .WORD     0xF368EE43
                .WORD     0x88F51F3F
                .WORD     0x3FAFBEA6
                .WORD     0xF78E4D0D
                .WORD     0x48E2DC29
                .WORD     0xDDA0E486
                .WORD     0xF56BE170
                .WORD     0xC6D34EFF
                .WORD     0x4B135805
                .WORD     0x6206FB8F
                .WORD     0xDDCB53CD
                .WORD     0x690DC509
                .WORD     0x799452D3
                .WORD     0x3AA73B48
                .WORD     0xA9F1EAB5
                .WORD     0x152E9D06
                .WORD     0xB98949B2
                .WORD     0x6C2C0613
                .WORD     0xE3228831
                .WORD     0x00171EAC
                .WORD     0xA95E7136
                .WORD     0x29C75246
                .WORD     0xA3951D16
                .WORD     0x38FD57F6
                .WORD     0x8F5E987C
                .WORD     0xCFA38874
                .WORD     0x0BBD6D71
                .WORD     0xD1765C41
                .WORD     0x2593C701
                .WORD     0xC8EC0F70
                .WORD     0xB3D691C0
                .WORD     0x91BA2F33
                .WORD     0x80D6AB33
                .WORD     0xA1D1A50D
                .WORD     0x58F73EE6
                .WORD     0x78D57E5E
                .WORD     0x53A0B1C3
                .WORD     0x868E74A6
                .WORD     0x0642C7DB
                .WORD     0x493D11A8
                .WORD     0xCE14F3BF
                .WORD     0x6CADE060
                .WORD     0xFA2AE325
                .WORD     0x60970067
                .WORD     0x46B94BFB
                .WORD     0x1C667E81
                .WORD     0xFF6ABDDF
                .WORD     0x9E2CFB54
                .WORD     0x487201BF
                .WORD     0x37CF1CE1
                .WORD     0x37200F47
                .WORD     0x2313DE56
                .WORD     0x359DAA88
                .WORD     0xC6756687
                .WORD     0x6561137B
                .WORD     0x73CF59EA
                .WORD     0x9D853C54
                .WORD     0x1D2D85A1
                .WORD     0xAAEE655B
                .WORD     0xD5502F37
                .WORD     0x3988CCE3
                .WORD     0x3DA10279
                .WORD     0xA5F8D999
                .WORD     0xF1AB768E
                .WORD     0xF368EE43
                .WORD     0x88F51F3F
                .WORD     0x3FAFBEA6
                .WORD     0xF78E4D0D
                .WORD     0x48E2DC29
                .WORD     0xDDA0E486
                .WORD     0xF56BE170
                .WORD     0xC6D34EFF
                .WORD     0x4B135805
                .WORD     0x6206FB8F
                .WORD     0xDDCB53CD
                .WORD     0x690DC509
                .WORD     0x799452D3
                .WORD     0x3AA73B48
                .WORD     0xA9F1EAB5
                .WORD     0x152E9D06
                .WORD     0xB98949B2
                .WORD     0x6C2C0613
                .WORD     0xE3228831
                .WORD     0x00171EAC
                .WORD     0xA95E7136
                .WORD     0x29C75246
                .WORD     0xA3951D16
                .WORD     0x38FD57F6
                .WORD     0x8F5E987C
                .WORD     0xCFA38874
                .WORD     0x0BBD6D71
                .WORD     0xD1765C41
                .WORD     0x2593C701
                .WORD     0xC8EC0F70
                .WORD     0xB3D691C0
                .WORD     0x91BA2F33
                .WORD     0x80D6AB33
                .WORD     0xA1D1A50D
                .WORD     0x58F73EE6
                .WORD     0x78D57E5E
                .WORD     0x53A0B1C3
                .WORD     0x868E74A6
                .WORD     0x0642C7DB
                .WORD     0x493D11A8
                .WORD     0xCE14F3BF
                .WORD     0x6CADE060
                .WORD     0xFA2AE325
                .WORD     0x60970067

                .END
