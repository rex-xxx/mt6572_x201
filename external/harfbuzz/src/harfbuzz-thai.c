/*
 * Copyright (C) 2008 Nokia Corporation and/or its subsidiary(-ies)
 *
 * This is part of HarfBuzz, an OpenType Layout engine library.
 *
 * Permission is hereby granted, without written agreement and without
 * license or royalty fees, to use, copy, modify, and distribute this
 * software and its documentation for any purpose, provided that the
 * above copyright notice and the following two paragraphs appear in
 * all copies of this software.
 *
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF THE COPYRIGHT HOLDER HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 *
 * THE COPYRIGHT HOLDER SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE COPYRIGHT HOLDER HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

#include "harfbuzz-shaper.h"
#include "harfbuzz-shaper-private.h"
#include "harfbuzz-external.h"
#include "harfbuzz-debug.h"

#include <assert.h>
#include <stdio.h>

//#define THAI_DEBUG
#ifdef THAI_DEBUG
#define THDEBUG HBDebug
#else
#define THDEBUG if(0) printf
#endif

enum {
    CH_SPACE        = 0x0020,
    CH_YAMAKKAN     = 0x0E4E,
    CH_MAI_HANAKAT  = 0x0E31,
    CH_SARA_AA      = 0x0E32,
    CH_SARA_AM      = 0x0E33,
    CH_SARA_UEE     = 0x0E37,
    CH_MAITAIKHU    = 0x0E47,
    CH_NIKHAHIT     = 0x0E4D,
    CH_SARA_U       = 0x0E38,
    CH_PHINTHU      = 0x0E3A,
    CH_YO_YING      = 0x0E0D,
    CH_THO_THAN     = 0x0E10,
    CH_DOTTED_CIRCLE = 0x25CC
};

enum {
    // Character classes
    NON =  0,
    CON =  1,
    COA =  2,
    COD =  3,
    LVO =  4,
    FV1 =  5,
    FV2 =  6,
    FV3 =  7,
    BV1 =  8,
    BV2 =  9,
    BDI = 10,
    TON = 11,
    AD1 = 12,
    AD2 = 13,
    AD3 = 14,
    NIK = 15,
    AV1 = 16,
    AV2 = 17,
    AV3 = 18,
    classCount = 19,

    // State Transition actions
    tA  =  0,
    tC  =  1,
    tD  =  2,
    tE  =  3,
    tF  =  4,
    tG  =  5,
    tH  =  6,
    tR  =  7,
    tS  =  8
};

typedef enum{
    THAI_SHAPE_NONE,
    THAI_SHAPE_WIN
} ThaiShapeCategory;

typedef struct ThaiStateTransition_ {
    hb_uint8 nextState;
    hb_uint8 action;
} ThaiStateTransition, *PThaiStateTransition;

static const HB_UChar16 thaiWinProprietary[] =
{
    0xF700, 0xF701, 0xF702, 0xF703, 0xF704, 0xF705, 0xF706, 0xF707,
    0xF708, 0xF709, 0xF70A, 0xF70B, 0xF70C, 0xF70D, 0xF70E, 0xF70F,
    0xF710, 0xF711, 0xF712, 0xF713, 0xF714, 0xF715, 0xF716, 0xF717,
    0xF718, 0xF719, 0xF71A
};

static const hb_uint8 thaiClassTable[] = {
    //       0    1    2    3    4    5    6    7    8    9    A    B    C    D    E    F
    //       -------------------------------------------------------------------------------
    /*0E00*/ NON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, COD, COD, COD,
    /*0E10*/ COD, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, COA, CON, COA, CON, COA,
    /*0E20*/ CON, CON, CON, CON, FV3, CON, FV3, CON, CON, CON, CON, CON, CON, CON, CON, NON,
    /*0E30*/ FV1, AV2, FV1, FV1, AV1, AV3, AV2, AV3, BV1, BV2, BDI, NON, NON, NON, NON, NON,
    /*0E40*/ LVO, LVO, LVO, LVO, LVO, FV2, NON, AD2, TON, TON, TON, TON, AD1, NIK, AD3, NON,
    /*0E50*/ NON, NON, NON, NON, NON, NON, NON, NON, NON, NON, NON, NON
};

static const ThaiStateTransition thaiStateTable[][classCount] = {
    //+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
    //|         N         C         C         C         L         F         F         F         B         B         B         T         A         A         A         N         A         A         A    |
    //|         O         O         O         O         V         V         V         V         V         V         D         O         D         D         D         I         V         V         V    |
    //|         N         N         A         D         O         1         2         3         1         2         I         N         1         2         3         K         1         2         3    |
    //+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
    /*00*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*01*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 2, tC}, { 6, tC}, { 0, tC}, { 8, tE}, { 0, tE}, { 0, tE}, { 0, tC}, { 9, tE}, {11, tC}, {14, tC}, {16, tC}},
    /*02*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 3, tE}, { 0, tE}, { 0, tR}, { 0, tR}, { 4, tE}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*03*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*04*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 5, tC}, { 0, tC}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*05*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*06*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 7, tE}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*07*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*08*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tA}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*09*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {10, tC}, { 0, tC}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*10*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*11*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {12, tC}, { 0, tC}, { 0, tR}, { 0, tR}, {13, tC}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*12*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*13*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*14*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {15, tC}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*15*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*16*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {17, tC}, { 0, tR}, { 0, tC}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*17*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*18*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tA}, { 0, tS}, { 0, tA}, {19, tC}, {23, tC}, { 0, tC}, {25, tF}, { 0, tF}, { 0, tF}, { 0, tD}, {26, tF}, {28, tD}, {31, tD}, {33, tD}},
    /*19*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {20, tF}, { 0, tF}, { 0, tR}, { 0, tR}, {21, tF}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*20*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*21*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {22, tC}, { 0, tC}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*22*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*23*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {24, tF}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*24*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*25*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tA}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*26*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {27, tG}, { 0, tG}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*27*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*28*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {29, tG}, { 0, tG}, { 0, tR}, { 0, tR}, {30, tG}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*29*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*30*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*31*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {32, tG}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*32*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*33*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {34, tG}, { 0, tR}, { 0, tG}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*34*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*35*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tA}, { 0, tS}, { 0, tA}, {36, tH}, {40, tH}, { 0, tH}, {42, tE}, { 0, tE}, { 0, tE}, { 0, tC}, {43, tE}, {45, tC}, {48, tC}, {50, tC}},
    /*36*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {37, tE}, { 0, tE}, { 0, tR}, { 0, tR}, {38, tE}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*37*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*38*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {39, tC}, { 0, tC}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*39*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*40*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {41, tE}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*41*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*42*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tA}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*43*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {44, tC}, { 0, tC}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*44*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*45*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {46, tC}, { 0, tC}, { 0, tR}, { 0, tR}, {47, tC}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*46*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*47*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*48*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {49, tC}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*49*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*50*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tS}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, {51, tC}, { 0, tR}, { 0, tC}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}},
    /*51*/ {{ 0, tA}, { 1, tA}, {18, tA}, {35, tA}, { 0, tA}, { 0, tS}, { 0, tA}, { 0, tA}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}, { 0, tR}}
};

typedef int (*th_brk_def)(const char*, int[], int);
static th_brk_def th_brk = 0;
static int libthai_resolved = 0;

static hb_uint8 thaiGetCharClass(HB_UChar16 ch) {
    hb_uint8 charClass = NON;

    if (ch >= 0x0E00 && ch <= 0x0E5B) {
        charClass = thaiClassTable[ch - 0x0E00];
    }

    return charClass;
}

inline ThaiStateTransition thaiGetTransition(hb_uint8 state, hb_uint8 currClass)
{
    return thaiStateTable[state][currClass];
}

static void resolve_libthai()
{
    if (!th_brk)
        th_brk = (th_brk_def)HB_Library_Resolve("thai", 0, "th_brk");
    libthai_resolved = 1;
}

static void to_tis620(const HB_UChar16 *string, hb_uint32 len, const char *cstr)
{
    hb_uint32 i;
    unsigned char *result = (unsigned char *)cstr;

    for (i = 0; i < len; ++i) {
        if (string[i] <= 0xa0)
            result[i] = (unsigned char)string[i];
        if (string[i] >= 0xe01 && string[i] <= 0xe5b)
            result[i] = (unsigned char)(string[i] - 0xe00 + 0xa0);
        else
            result[i] = '?';
    }

    result[len] = 0;
}

static void thaiWordBreaks(const HB_UChar16 *string, hb_uint32 len, HB_CharAttributes *attributes)
{
    char s[128];
    char *cstr = s;
    int brp[128];
    int *break_positions = brp;
    hb_uint32 numbreaks;
    hb_uint32 i;

    if (!libthai_resolved)
        resolve_libthai();

    if (!th_brk)
        return;

    if (len >= 128)
        cstr = (char *)malloc(len*sizeof(char) + 1);

    to_tis620(string, len, cstr);

    numbreaks = th_brk(cstr, break_positions, 128);
    if (numbreaks > 128) {
        break_positions = (int *)malloc(numbreaks * sizeof(int));
        numbreaks = th_brk(cstr, break_positions, numbreaks);
    }

    for (i = 0; i < len; ++i) {
        attributes[i].lineBreakType = HB_NoBreak;
        attributes[i].wordBoundary = FALSE;
    }

    for (i = 0; i < numbreaks; ++i) {
        if (break_positions[i] > 0) {
            attributes[break_positions[i]-1].lineBreakType = HB_Break;
            attributes[break_positions[i]-1].wordBoundary = TRUE;
        }
    }

    if (break_positions != brp)
        free(break_positions);

    if (len >= 128)
        free(cstr);
}

static HB_UChar16 thaiLeftAboveVowel(HB_UChar16 vowel, ThaiShapeCategory glyphSet)
{
    static const HB_UChar16 leftAboveVowels[][7] = {
        {0x0E31, 0x0E32, 0x0E33, 0x0E34, 0x0E35, 0x0E36, 0x0E37},
        {0xF710, 0x0E32, 0x0E33, 0xF701, 0xF702, 0xF703, 0xF704}
    };

    if (vowel >= CH_MAI_HANAKAT && vowel <= CH_SARA_UEE) {
        return leftAboveVowels[glyphSet][vowel - CH_MAI_HANAKAT];
    }

    if (vowel == CH_YAMAKKAN && glyphSet == 0) {
        return 0x0E7E;
    }

    return vowel;
}

static HB_UChar16 thaiLowerRightTone(HB_UChar16 tone, ThaiShapeCategory glyphSet)
{
    static const HB_UChar16 lowerRightTones[][7] = {
        {0x0E47, 0x0E48, 0x0E49, 0x0E4A, 0x0E4B, 0x0E4C, 0x0E4D},
        {0x0E47, 0xF70A, 0xF70B, 0xF70C, 0xF70D, 0xF70E, 0x0E4D}
    };

    if (tone >= CH_MAITAIKHU && tone <= CH_NIKHAHIT) {
        return lowerRightTones[glyphSet][tone - CH_MAITAIKHU];
    }

    return tone;
}

static HB_UChar16 thaiLowerLeftTone(HB_UChar16 tone, ThaiShapeCategory glyphSet)
{
    static const HB_UChar16 lowerLeftTones[][7] = {
        {0x0E47, 0x0E48, 0x0E49, 0x0E4A, 0x0E4B, 0x0E4C, 0x0E4D},
        {0xF712, 0xF705, 0xF706, 0xF707, 0xF708, 0xF709, 0xF711}
    };

    if (tone >= CH_MAITAIKHU && tone <= CH_NIKHAHIT) {
        return lowerLeftTones[glyphSet][tone - CH_MAITAIKHU];
    }

    return tone;
}

static HB_UChar16 thaiUpperLeftTone(HB_UChar16 tone, ThaiShapeCategory glyphSet)
{
    static const HB_UChar16 upperLeftTones[][7] = {
        {0x0E47, 0x0E48, 0x0E49, 0x0E4A, 0x0E4B, 0x0E4C, 0x0E4D},
        {0xF712, 0xF713, 0xF714, 0xF715, 0xF716, 0xF717, 0xF711}
    };

    if (tone >= CH_MAITAIKHU && tone <= CH_NIKHAHIT) {
        return upperLeftTones[glyphSet][tone - CH_MAITAIKHU];
    }

    return tone;
}

static HB_UChar16 thaiLowerBelowVowel(HB_UChar16 vowel, ThaiShapeCategory glyphSet)
{
    static const HB_UChar16 lowerBelowVowels[][3] = {
        {0x0E38, 0x0E39, 0x0E3A},
        {0xF718, 0xF719, 0xF71A}

    };

    if (vowel >= CH_SARA_U && vowel <= CH_PHINTHU) {
        return lowerBelowVowels[glyphSet][vowel - CH_SARA_U];
    }

    return vowel;
}

static HB_UChar16 thaiNoDescenderCOD(HB_UChar16 cod, ThaiShapeCategory glyphSet)
{
    static const HB_UChar16 noDescenderCODs[][4] = {
        {0x0E0D, 0x0E0E, 0x0E0F, 0x0E10},
        {0xF70F, 0x0E0E, 0x0E0F, 0xF700}
    };

    if (cod >= CH_YO_YING && cod <= CH_THO_THAN) {
        return noDescenderCODs[glyphSet][cod - CH_YO_YING];
    }

    return cod;
}

static hb_uint8 thaiDoTransition (
        ThaiStateTransition transition,
        HB_UChar16 currChar,
        hb_int32 inputIndex,
        ThaiShapeCategory glyphSet,
        HB_UChar16 errorChar,
        HB_UChar16 *outputBuffer,
        hb_int32 *outputIndex,
        hb_int32 *charIndex)
{
    HB_Error success = HB_Err_Ok;

    switch (transition.action) {
    case tA:
        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = currChar;
        break;

    case tC:
        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = currChar;
        break;

    case tD:
        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = thaiLeftAboveVowel(currChar, glyphSet);
        break;

    case tE:
        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = thaiLowerRightTone(currChar, glyphSet);
        break;

    case tF:
        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = thaiLowerLeftTone(currChar, glyphSet);
        break;

    case tG:
        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = thaiUpperLeftTone(currChar, glyphSet);
        break;

    case tH:
    {
        HB_UChar16 cod = outputBuffer[(*outputIndex) - 1];
        HB_UChar16 coa = thaiNoDescenderCOD(cod, glyphSet);

        if (cod != coa) {
            outputBuffer[(*outputIndex) - 1] = coa;

            charIndex[(*outputIndex)] = inputIndex;
            outputBuffer[(*outputIndex)++] = currChar;
            break;
        }

        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = thaiLowerBelowVowel(currChar, glyphSet);
        break;
    }

    case tR:
        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = errorChar;

        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = currChar;
        break;

    case tS:
        if (currChar == CH_SARA_AM) {
            charIndex[(*outputIndex)] = inputIndex;
            outputBuffer[(*outputIndex)++] = errorChar;
        }

        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = currChar;
        break;

    default:
        // FIXME: if we get here, there's an error
        // in the state table!
        charIndex[(*outputIndex)] = inputIndex;
        outputBuffer[(*outputIndex)++] = currChar;
        break;
     }

     return transition.nextState;
}

static HB_Bool thaiIsLegalHere(HB_UChar16 ch, hb_uint8 prevState)
{
    hb_uint8 charClass = thaiGetCharClass(ch);
    ThaiStateTransition transition = thaiGetTransition(prevState, charClass);

    switch (transition.action) {
    case tA:
    case tC:
    case tD:
    case tE:
    case tF:
    case tG:
    case tH:
        return TRUE;

    case tR:
    case tS:
        return FALSE;

    default:
        // FIXME: if we get here, there's an error
        // in the state table!
        return FALSE;
    }
}

static hb_uint8 thaiGetNextState(
        HB_UChar16 ch,
        hb_uint8 prevState,
        hb_int32 inputIndex,
        ThaiShapeCategory glyphSet,
        HB_UChar16 errorChar,
        hb_uint8 *charClass,
        HB_UChar16 *output,
        hb_int32 *outputIndex,
        hb_int32 *charIndex,
        HB_Bool *isClusterStart)
{
    ThaiStateTransition transition;

    *charClass = thaiGetCharClass(ch);
    transition = thaiGetTransition(prevState, *charClass);

    hb_uint8 nextState = thaiDoTransition(transition, ch, inputIndex, glyphSet, errorChar, output, outputIndex, charIndex);

    if (prevState == 0 || (*charClass) == LVO || (*charClass) == CON
            || (*charClass) == COA || (*charClass) == COD) {
        *isClusterStart = TRUE;
    } else {
        *isClusterStart = FALSE;
    }

    return nextState;
}

/*
 * Input:
 *    const HB_UChar16* input, the input string
 *    hb_int32 offset, the beginning position of sub-string that need to be composed
 *    hb_int32 charCount, the number of characters that need to be composed.
 *    ThaiShapeCategory glyphSet, control the substitution action
 *    bool errorChar,
 * Input/Output:
 *    HB_UChar16* output
 *    uhb_int32 *charIndex,
 *    int* clusterCount: the number of cluster
 *    int* clusterStart: the start position for one cluster
 *
*/
static hb_int32 thaiCompose(const HB_UChar16 *input, hb_int32 offset, hb_int32 charCount, ThaiShapeCategory glyphSet,
        HB_UChar16 errorChar, HB_UChar16 *output, hb_int32 *charIndex, int* clusterCount, hb_int32 *clusterStart)
{
    hb_uint8 state = 0;
    hb_int32 inputIndex;
    hb_int32 outputIndex = 0;
    hb_uint8 conState = 0xFF;
    hb_int32 conInput = -1;
    hb_int32 conOutput = -1;
    int j = 0;
    HB_Bool isClusterStart = TRUE;
    int cCount = 0; /* cluster number */

    for (inputIndex = 0; inputIndex < charCount; ++ inputIndex) {
        HB_UChar16 ch = input[inputIndex + offset];
        hb_uint8 charClass;

        // Decompose SARA AM into NIKHAHIT + SARA AA
        if (ch == CH_SARA_AM && thaiIsLegalHere(ch, state)) {
            outputIndex = conOutput;
            state = thaiGetNextState(CH_NIKHAHIT, conState, inputIndex, glyphSet, errorChar, &charClass,
                output, &outputIndex, charIndex, &isClusterStart);

            if (isClusterStart) {
                clusterStart[cCount++] = inputIndex;
            }

            for (j = conInput + 1; j < inputIndex; ++ j) {
                ch = input[j + offset];
                state = thaiGetNextState(ch, state, j, glyphSet, errorChar, &charClass,
                    output, &outputIndex, charIndex, &isClusterStart);
                if (isClusterStart) {
                    clusterStart[cCount++] = j;
                }
            }

            ch = CH_SARA_AA;
        }

        state = thaiGetNextState(ch, state, inputIndex, glyphSet, errorChar, &charClass,
            output, &outputIndex, charIndex, &isClusterStart);
        if (isClusterStart) {
            clusterStart[cCount++] = inputIndex;
        }

        if (charClass >= CON && charClass <= COD) {
            conState = state;
            conInput = inputIndex;
            conOutput = outputIndex;
        }
    }

    *clusterCount = cCount;
    return outputIndex;
}

static ThaiShapeCategory thaiCheckShapeCategory(const HB_ShaperItem* shaper_item)
{
    if(shaper_item->font->klass->canRender(shaper_item->font, thaiWinProprietary, (sizeof(thaiWinProprietary)/sizeof(HB_UChar16)))){
        return THAI_SHAPE_WIN;
    }

    return THAI_SHAPE_NONE;
}

HB_Bool HB_ThaiShape(HB_ShaperItem *item)
{
    HB_Bool openType = FALSE;
    unsigned short *logClusters = item->log_clusters;
    int i;
    int clusterIndex;
    int cStart, cEnd;
    int fristGlyphIndex;
    int prevOutputIndex;
    int outputClusterLength;

    HB_ShaperItem shapedItem = *item;

    THDEBUG("item->item.pos: %d", item->item.pos);

    ThaiShapeCategory glyphSet = thaiCheckShapeCategory(item);

    HB_UChar16 * output = (HB_UChar16 *)malloc(sizeof(HB_UChar16) * item->item.length * 2);
    hb_int32 *charIndex = (hb_int32 *)malloc(sizeof(hb_int32) * item->item.length * 2);
    hb_int32 *clusterStart = (hb_int32 *)malloc(sizeof(hb_int32) * item->item.length * 2);
    int clusterCount = 0;

    hb_int32 strLen = thaiCompose(item->string, item->item.pos, item->item.length, glyphSet,
            CH_DOTTED_CIRCLE, output, charIndex, &clusterCount, clusterStart);
    THDEBUG("reordered strLen: %d", strLen);

#ifdef THAI_DEBUG
    THDEBUG("clusterCount: %d", clusterCount);
    for (i = 0; i < strLen; ++i) {
        THDEBUG("charIndex[%d]: %d, clusterStart[%d]: %d", i, charIndex[i], i, clusterStart[i]);
    }
#endif

    shapedItem.string = output;
    shapedItem.stringLength = strLen;

    fristGlyphIndex = 0;
    prevOutputIndex = 0;
    for (clusterIndex = 0; clusterIndex < clusterCount; ++clusterIndex) {
        cStart = clusterStart[clusterIndex];
        if (clusterIndex == clusterCount - 1) {
            cEnd = item->item.length;
        } else {
            cEnd = clusterStart[clusterIndex + 1];
        }

        outputClusterLength = 0;
        for (i = prevOutputIndex; i < strLen && charIndex[i] < cEnd; ++i) {
            ++outputClusterLength;
        }
        THDEBUG("        outputClusterLength %d", outputClusterLength);

        shapedItem.item.pos = prevOutputIndex;
        shapedItem.item.length = outputClusterLength;
        shapedItem.glyphs = item->glyphs + fristGlyphIndex;
        shapedItem.attributes = item->attributes + fristGlyphIndex;
        shapedItem.advances = item->advances + fristGlyphIndex;
        shapedItem.offsets = item->offsets + fristGlyphIndex;
        shapedItem.num_glyphs = item->num_glyphs - fristGlyphIndex;
        shapedItem.log_clusters = item->log_clusters + cStart;

        HB_BasicShape(&shapedItem);

        for (i = cStart; i < cEnd; ++i) {
            item->log_clusters[i] = fristGlyphIndex;
        }

        prevOutputIndex += outputClusterLength;
        fristGlyphIndex += shapedItem.num_glyphs;
    }

    for (i = item->item.length; i < prevOutputIndex; ++i) {
        item->log_clusters[i] = 0;
    }

    item->num_glyphs = fristGlyphIndex;
    THDEBUG("shapedItem.num_glyphs: %d", item->num_glyphs);

    free(clusterStart);
    free(charIndex);
    free(output);
    return TRUE;
}

void HB_ThaiAttributes(HB_Script script, const HB_UChar16 *text, hb_uint32 from, hb_uint32 len, HB_CharAttributes *attributes)
{
    assert(script == HB_Script_Thai);
    attributes += from;
    thaiWordBreaks(text + from, len, attributes);
}

