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
#include "harfbuzz-debug.h"

#include <assert.h>
#include <stdio.h>

enum MymrCharClassValues
{
    Mymr_CC_RESERVED       = 0,
    Mymr_CC_CONSONANT      = 1, /* Consonant of type 1, that has conjunct form */
    Mymr_CC_CONSONANT2     = 2, /* Consonant of type 2, that has no conjunct form */
    Mymr_CC_NGA            = 3, /* Consonant NGA */
    Mymr_CC_IND_VOWEL      = 4, /* Independent vowel */
    Mymr_CC_MEDIAL_Y       = 5, /* Medial YA */
    Mymr_CC_MEDIAL_R       = 6, /* Medial RA */
    Mymr_CC_MEDIAL_W       = 7, /* Medial WA */
    Mymr_CC_MEDIAL_H       = 8, /* Medial HA */
    Mymr_CC_VIRAMA         = 9, /* Subscript consonant combining character */
    Mymr_CC_ASAT           = 10, /* Explicit Asat */
    Mymr_CC_PRE_VOWEL      = 11, /* Dependent vowel, prebase (Vowel e) */
    Mymr_CC_ABOVE_VOWEL    = 12, /* Dependent vowel, abovebase (Vowel i, ii, ai) */
    Mymr_CC_BELOW_VOWEL    = 13, /* Dependent vowel, belowbase (Vowel u, uu) */
    Mymr_CC_POST_VOWEL     = 14, /* Dependent vowel, postbase (Vowel aa) */
    Mymr_CC_SIGN_ABOVE     = 15,
    Mymr_CC_SIGN_BELOW     = 16,
    Mymr_CC_SIGN_AFTER     = 17,
    Mymr_CC_COUNT          = 18 /* This is the number of character classes */
};

enum MymrCharClassFlags
{
    Mymr_CF_CLASS_MASK      = 0x0000FFFF,

    Mymr_CF_CONSONANT       = 0x01000000, /* flag to speed up comparing */
    Mymr_CF_MEDIAL          = 0x02000000, /* flag to speed up comparing */
    Mymr_CF_IND_VOWEL       = 0x04000000, /* flag to speed up comparing */
    Mymr_CF_DEP_VOWEL       = 0x08000000, /* flag to speed up comparing */
    Mymr_CF_DOTTED_CIRCLE   = 0x10000000, /* add a dotted circle if a character with this flag is the first in a syllable */
    Mymr_CF_VIRAMA          = 0x20000000, /* flag to speed up comparing */
    Mymr_CF_ASAT            = 0x40000000, /* flag to speed up comparing */

    /* position flags */
    Mymr_CF_POS_BEFORE      = 0x00080000,
    Mymr_CF_POS_BELOW       = 0x00040000,
    Mymr_CF_POS_ABOVE       = 0x00020000,
    Mymr_CF_POS_AFTER       = 0x00010000,
    Mymr_CF_POS_MASK        = 0x000f0000
};

/* Characters that get refrered to by name */
enum MymrChar
{
    Mymr_C_SIGN_ZWNJ      = 0x200C,
    Mymr_C_SIGN_ZWJ       = 0x200D,
    Mymr_C_DOTTED_CIRCLE  = 0x25CC,
    Mymr_C_NGA            = 0x1004,
    Mymr_C_MEDIAL_Y       = 0X103B,
    Mymr_C_MEDIAL_R       = 0X103C,
    Mymr_C_MEDIAL_W       = 0X103D,
    Mymr_C_MEDIAL_H       = 0X103E,
    Mymr_C_VIRAMA         = 0x1039,
    Mymr_C_ASAT           = 0x103A,
    Mymr_C_VOWEL_E        = 0x1031
};

enum
{
    Mymr_xx = Mymr_CC_RESERVED,
    Mymr_c1 = Mymr_CC_CONSONANT | Mymr_CF_CONSONANT | Mymr_CF_POS_BELOW,
    Mymr_c2 = Mymr_CC_CONSONANT2 | Mymr_CF_CONSONANT,
    Mymr_ng = Mymr_CC_NGA | Mymr_CF_CONSONANT,
    Mymr_id = Mymr_CC_IND_VOWEL | Mymr_CF_IND_VOWEL,
    Mymr_my = Mymr_CC_MEDIAL_Y | Mymr_CF_MEDIAL | Mymr_CF_DOTTED_CIRCLE,
    Mymr_mr = Mymr_CC_MEDIAL_R | Mymr_CF_MEDIAL | Mymr_CF_DOTTED_CIRCLE,
    Mymr_mw = Mymr_CC_MEDIAL_W | Mymr_CF_MEDIAL | Mymr_CF_DOTTED_CIRCLE,
    Mymr_mh = Mymr_CC_MEDIAL_H | Mymr_CF_MEDIAL | Mymr_CF_DOTTED_CIRCLE,
    Mymr_vi = Mymr_CC_VIRAMA | Mymr_CF_VIRAMA | Mymr_CF_DOTTED_CIRCLE,
    Mymr_va = Mymr_CC_ASAT | Mymr_CF_ASAT | Mymr_CF_DOTTED_CIRCLE,
    Mymr_dl = Mymr_CC_PRE_VOWEL | Mymr_CF_DEP_VOWEL | Mymr_CF_POS_BEFORE | Mymr_CF_DOTTED_CIRCLE,
    Mymr_da = Mymr_CC_ABOVE_VOWEL | Mymr_CF_DEP_VOWEL | Mymr_CF_POS_ABOVE | Mymr_CF_DOTTED_CIRCLE,
    Mymr_db = Mymr_CC_BELOW_VOWEL | Mymr_CF_DEP_VOWEL | Mymr_CF_POS_BELOW | Mymr_CF_DOTTED_CIRCLE,
    Mymr_dr = Mymr_CC_POST_VOWEL | Mymr_CF_DEP_VOWEL | Mymr_CF_POS_AFTER | Mymr_CF_DOTTED_CIRCLE,
    Mymr_sa = Mymr_CC_SIGN_ABOVE | Mymr_CF_DOTTED_CIRCLE | Mymr_CF_POS_ABOVE,
    Mymr_sb = Mymr_CC_SIGN_BELOW | Mymr_CF_DOTTED_CIRCLE | Mymr_CF_POS_BELOW,
    Mymr_sp = Mymr_CC_SIGN_AFTER | Mymr_CF_POS_AFTER | Mymr_CF_DOTTED_CIRCLE
};


typedef int MymrCharClass;


static const MymrCharClass mymrCharClasses[] =
{
    Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1, Mymr_ng, Mymr_c1, Mymr_c1, Mymr_c1,
    Mymr_c1, Mymr_c1, Mymr_c2, Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1, /* 1000 - 100F */
    Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1,
    Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c1, Mymr_c2, Mymr_c1, Mymr_c1, /* 1010 - 101F */
    Mymr_c2, Mymr_c2, Mymr_xx, Mymr_id, Mymr_id, Mymr_id, Mymr_id, Mymr_id,
    Mymr_xx, Mymr_id, Mymr_id, Mymr_dr, Mymr_dr, Mymr_da, Mymr_da, Mymr_db, /* 1020 - 102F */
    Mymr_db, Mymr_dl, Mymr_da, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_sa, Mymr_sb,
    Mymr_sp, Mymr_vi, Mymr_va, Mymr_my, Mymr_mr, Mymr_mw, Mymr_mh, Mymr_c2, /* 1030 - 103F */
    Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx,
    Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_c2, Mymr_xx, /* 1040 - 104F */
    Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx,
    Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, Mymr_xx, /* 1050 - 105F */
};

static MymrCharClass
getMyanmarCharClass (HB_UChar16 ch)
{
    if (ch == Mymr_C_MEDIAL_Y)
        return Mymr_CC_MEDIAL_Y;

    if (ch == Mymr_C_MEDIAL_R)
        return Mymr_CC_MEDIAL_R;

    if (ch == Mymr_C_MEDIAL_W)
        return Mymr_CC_MEDIAL_W;

    if (ch == Mymr_C_MEDIAL_H)
        return Mymr_CC_MEDIAL_H;

    if (ch == Mymr_C_ASAT)
        return Mymr_CC_ASAT;

    if (ch < 0x1000 || ch > 0x105f)
        return Mymr_CC_RESERVED;

    return mymrCharClasses[ch - 0x1000];
}

static const signed char mymrStateTable[][Mymr_CC_COUNT] =
{
/* xx  c1, c2  ng  id  my  mr  mw  mh  vi  va  dl  da  db  dr  sa  sb  sp  */
    { 1,  3,  3,  2, 16,  7,  7,  8,  9, 23,  4, 10, 11, 12, 13, 15, 24,  1}, /*  0 - ground state */
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, /*  1 - exit state (or sp/sb to the right of the syllable) */
    {-1, 20, 20, 19, -1,  7,  7,  8,  9, -1, 17, 10, 11, 12, 13, 15, 24, -1}, /*  2 - Base NGA */
    {-1, 20, 20, 19, -1,  7,  7,  8,  9,  5,  4, 10, 11, 12, 13, 15, 24, -1}, /*  3 - Base consonant */
    {-1, -1, -1, -1, -1, 22, -1, -1, -1, -1, -1, -1, -1, 21, -1, -1, -1,  1}, /*  4 - Asat */
    {-2,  6, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2}, /*  5 - First virama */
    {-1, 20, 20, -1, -1,  7,  7,  8,  9, -1, -1, 10, 11, 12, 13, 15, -1, -1}, /*  6 - c1 after virama */
    {-1, 20, 20, 19, -1, -1, -1,  8,  9, -1, -1, 10, 11, 12, 13, 15, -1, -1}, /*  7 - First medial */
    {-1, 20, 20, 19, -1, -1, -1, -1,  9, -1, -1, 10, 11, 12, 13, 15, -1, -1}, /*  8 - Second medial */
    {-1, 20, 20, 19, -1, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 15, -1, -1}, /*  9 - Third medial */
    {-1, 20, 20, 19, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 13, 15,  1,  1}, /* 10 - dl, Dependent vowel e */
    {-1, 20, 20, 19, -1, -1, -1, -1, -1, -1, -1, -1, -1, 12, -1,  1,  1,  1}, /* 11 - da, Dependent vowel i,ii,ai */
    {-1, 20, 20, 19, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 15,  1,  1}, /* 12 - db, Dependent vowel u,uu */
    {-1, 20, 20, 19, -1, -1, -1, -1, -1, -1,  4, -1, -1, -1, -1, -1,  1,  1}, /* 13 - dr, Dependent vowel aa */
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, /* 14 - Shayhto */
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  1,  1}, /* 15 - sa, Sign anusvara*/
    {-1, 20, 20, 19, -1, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 15, -1,  1}, /* 16 - Independent vowel */
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, 18, -1, -1, -1, -1, -1, -1, -1,  1}, /* 17 - Asat after NGA */
    {-2,  3,  3,  2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2}, /* 18 - Virama after NGA + Asat */
    {-2, -2, -2, -2, -2, -2, -2, -2, -2, -2, 17, -2, -2, -2, -2, -2, -2, -2}, /* 19 - Next base NGA */
    {-2, -2, -2, -2, -2, -2, -2, -2, -2,  5,  4, -2, -2, -2, -2, -2, -2, -2}, /* 20 - Next base, rest */
    {-2, 20, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2}, /* 21 - Contractions, vowel u after Asat */
    {-2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, -2, 13, -2, -2, -2}, /* 22 - Contractions, Medial ya after virama */
    {-1,  6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, /* 23 - Virama after ground state */
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  1, -1, -1, -1, -1, -1, -1, -1}, /* 24 - Asat after Aukmyit */
/* exit state > -1 is for invalid order of medials and combination of invalids
   with virama where virama should treat as start of next syllable
 */
};



/*#define MYANMAR_DEBUG */
#ifdef MYANMAR_DEBUG
#define MMDEBUG HBDebug
#else
#define MMDEBUG if(0) printf
#endif

/*
//  Given an input string of characters and a location in which to start looking
//  calculate, using the state table, which one is the last character of the syllable
//  that starts in the starting position.
*/
static int myanmar_nextSyllableBoundary(const HB_UChar16 *s, int start, int end, HB_Bool *invalid)
{
    const HB_UChar16 *uc = s + start;
    int state = 0;
    int pos = start;
    *invalid = FALSE;

    while (pos < end) {
        MymrCharClass charClass = getMyanmarCharClass(*uc);
        state = mymrStateTable[state][charClass & Mymr_CF_CLASS_MASK];
        if (pos == start)
            *invalid = (HB_Bool)(charClass & Mymr_CF_DOTTED_CIRCLE);

        MMDEBUG("state[%d]=%d class=%8x (uc=%4x)", pos - start, state, charClass, *uc);

        if (state < 0) {
            if (state < -1)
                pos += (state + 1);
            break;
        }
        ++uc;
        ++pos;
    }
    return pos;
}

#ifndef NO_OPENTYPE
/* ###### might have to change order of above and below forms and substitutions,
   but according to Unicode below comes before above */
static const HB_OpenTypeFeature myanmar_features[] = {
    { HB_MAKE_TAG('p', 'r', 'e', 'f'), PreFormProperty },
    { HB_MAKE_TAG('b', 'l', 'w', 'f'), BelowFormProperty },
    { HB_MAKE_TAG('a', 'b', 'v', 'f'), AboveFormProperty },
    { HB_MAKE_TAG('p', 's', 't', 'f'), PostFormProperty },
    { HB_MAKE_TAG('p', 'r', 'e', 's'), PreSubstProperty },
    { HB_MAKE_TAG('b', 'l', 'w', 's'), BelowSubstProperty },
    { HB_MAKE_TAG('a', 'b', 'v', 's'), AboveSubstProperty },
    { HB_MAKE_TAG('p', 's', 't', 's'), PostSubstProperty },
    { HB_MAKE_TAG('c', 'l', 'i', 'g'), CligProperty },  /* Myanmar1 uses this instead of the other features */
    { 0, 0 }
};
#endif


/*
// Visual order before shaping should be:
//
//    [Vowel Mark E]
//    [Virama + Medial Ra]
//    [Base]
//    [Virama + Consonant]
//    [Nga + Virama] (Kinzi) ### should probably come before post forms (medial ya)
//    [Vowels]
//    [Marks]
//
// This means that we can keep the logical order apart from having to
// move the pre vowel, medial ra and kinzi
*/

/** M: When shape the myanmar string with reordering it, find next base in a syllable @{ */
static int findBase(const HB_UChar16 *chars, int start, int end) {
    MymrCharClass charClass;
    int i;

    for (i = start; i < end; i += 1) {
        charClass = getMyanmarCharClass(chars[i]);
        if (charClass & Mymr_CF_CONSONANT) {
            if (i == start)
                return i;
            else if ((i - 1) >= start) {
                if (!(getMyanmarCharClass(chars[i - 1]) & Mymr_CF_VIRAMA))
                    return i;
            }
        }
    }
    return end;
}
/** @} */

/** M: Shape a syllable with the information in font file @{ */
static HB_Bool myanmar_shape_syllable_with_font(HB_Bool openType, HB_ShaperItem *item, HB_Bool invalid)
{
    /*
//    MMDEBUG("\nsyllable from %d len %d, str='%s'", item->item.pos, item->item.length,
//	    item->string->mid(item->from, item->length).toUtf8().data());
    */

#ifndef NO_OPENTYPE
    const int availableGlyphs = item->num_glyphs;
#endif
    const HB_UChar16 *uc = item->string + item->item.pos;
    int i;
    int len = 0;
    unsigned short reordered[32];
    unsigned char properties[32];
    enum {
	AboveForm = 0x01,
	PreForm = 0x02,
	PostForm = 0x04,
	BelowForm = 0x08
    };

    memset(properties, 0, 32*sizeof(unsigned char));

    /* according to the table the max length of a syllable should be around 14 chars */
    assert(item->item.length < 32);

#ifdef MYANMAR_DEBUG
    printf("original:");
    for (i = 0; i < (int)item->item.length; i++) {
        printf("    %d: %4x", i, uc[i]);
    }
#endif

    len = 0;
    MymrCharClass char_class;
    int basE = 0, cursor = 0;
    while (basE < (int)item->item.length) {
        if (getMyanmarCharClass(uc[cursor]) & Mymr_CF_VIRAMA)
            basE = findBase(uc, cursor, (int)item->item.length);
        else
            basE = findBase(uc, cursor + 1, (int)item->item.length);

        /* shall we add a dotted circle?
         * If in the position in which the base should be (first char in the string) there is
         * a character that has the Dotted circle flag (a character that cannot be a base)
         * then write a dotted circle
         */
        if (getMyanmarCharClass(uc[cursor]) & Mymr_CF_DOTTED_CIRCLE) {
            /* add dotted circle */
            reordered[len] = Mymr_C_DOTTED_CIRCLE;
            len += 1;
        }

        /* copy what is left to the output, skipping before vowels and
         * medial Ra if they are present
         */
        for (i = cursor; i < basE; i += 1) {
            char_class = getMyanmarCharClass(uc[i]);

            switch (char_class & Mymr_CF_POS_MASK) {
            case Mymr_CF_POS_ABOVE:
                reordered[len] = uc[i];
                properties[len] = AboveForm;
                len += 1;
                break;

            case Mymr_CF_POS_AFTER:
                reordered[len] = uc[i];
                properties[len] = PostForm;
                len += 1;
                break;

            case Mymr_CF_POS_BELOW:
                reordered[len] = uc[i];
                properties[len] = BelowForm;
                len += 1;
                break;

            default:
                /* assign the correct flags to Medials */
                if ((char_class & Mymr_CF_MEDIAL) && i + 1 < basE) {
                    if ((char_class & Mymr_CF_CLASS_MASK) == Mymr_CC_MEDIAL_Y) { /* medial ya which possess below and right */
                        reordered[len] = uc[i];
                        properties[len] = PostForm;
                        len += 1;
                        break;
                    } else { /* others wa, ha only below */
                        reordered[len] = uc[i];
                        properties[len] = BelowForm;
                        len += 1;
                        break;
                    }
                }

                if ((char_class & Mymr_CF_VIRAMA) && i + 1 < basE) {
                    if ((getMyanmarCharClass(uc[i + 1]) & Mymr_CF_CLASS_MASK) == Mymr_CC_CONSONANT) { /* subscript consonant */
                        reordered[len] = uc[i];
                        properties[len] = BelowForm;
                        len += 1;
                        break;
                    }
                }

                /* assign the correct flags to consonant with subscript consonant */
                if ((char_class & Mymr_CF_CONSONANT) && i + 2 < basE) {
                    if ((getMyanmarCharClass(uc[i + 1]) & Mymr_CF_VIRAMA)
                            && (getMyanmarCharClass(uc[i + 2]) & Mymr_CF_CLASS_MASK) == Mymr_CC_CONSONANT) {
                        reordered[len] = uc[i];
                        properties[len] = BelowForm;
                        len += 1;
                        break;
                    }
                }

                /* default - any others */
                reordered[len] = uc[i];
                len += 1;
                break;
            }/* switch */
        } /* for: loop for a baseE */
        cursor = basE;
    } /* while: loop for a syllable*/

    if (!item->font->klass->convertStringToGlyphIndices(item->font,
                                                        reordered, len,
                                                        item->glyphs, &item->num_glyphs,
                                                        item->item.bidiLevel % 2))
        return FALSE;

    MMDEBUG("after shaping: len=%d", len);
    for (i = 0; i < len; i++) {
        item->attributes[i].mark = FALSE;
        item->attributes[i].clusterStart = FALSE;
        item->attributes[i].justification = 0;
        item->attributes[i].zeroWidth = FALSE;
        MMDEBUG("    %d: %4x property=%x", i, reordered[i], properties[i]);
    }

    /* now we have the syllable in the right order, and can start running it through open type. */

#ifndef NO_OPENTYPE
    if (openType) {
        hb_uint32 where[32];

        for (i = 0; i < len; ++i) {
            where[i] = ~(PreSubstProperty | BelowSubstProperty
                    | AboveSubstProperty | PostSubstProperty | CligProperty
                    | PositioningProperties);
            if (properties[i] & PreForm)
                where[i] &= ~PreFormProperty;
            if (properties[i] & BelowForm)
                where[i] &= ~BelowFormProperty;
            if (properties[i] & AboveForm)
                where[i] &= ~AboveFormProperty;
            if (properties[i] & PostForm)
                where[i] &= ~PostFormProperty;
        }

        HB_OpenTypeShape(item, where);
        if (!HB_OpenTypePosition(item, availableGlyphs, /*doLogClusters*/FALSE))
            return FALSE;
    } else
#endif
    {
        MMDEBUG("Not using openType");
        HB_HeuristicPosition(item);
    }

    item->attributes[0].clusterStart = TRUE;
    return TRUE;
}
/** @} */

// Porting the pango-myanmar module for reordering a syllable correctly when shape it.
static HB_Bool myanmar_shape_syllable(HB_Bool openType, HB_ShaperItem *item, HB_Bool invalid)
{
    /*
//    MMDEBUG("\nsyllable from %d len %d, str='%s'", item->item.pos, item->item.length,
//      item->string->mid(item->from, item->length).toUtf8().data());
    */

#ifndef NO_OPENTYPE
    const int availableGlyphs = item->num_glyphs;
#endif
    const HB_UChar16 *uc = item->string + item->item.pos;
    int vowel_e = -1;
    int kinzi = -1;
    int medial_ra = -1;
    int i;
    int len = 0;
    unsigned short reordered[32];
    unsigned char properties[32];
    enum {
        AboveForm = 0x01, PreForm = 0x02, PostForm = 0x04, BelowForm = 0x08
    };

    memset(properties, 0, 32*sizeof(unsigned char));

    /* according to the table the max length of a syllable should be around 14 chars */
    assert(item->item.length < 32);

#ifdef MYANMAR_DEBUG
    printf("original:");
    for (i = 0; i < (int)item->item.length; i++) {
        MMDEBUG("    %d: %4x", i, uc[i]);
    }
#endif

    len = 0;
    MymrCharClass char_class;
    int basE = 0, cursor = 0;
    while (basE < (int)item->item.length) {
        medial_ra = -1;  /* There is no Medial Ra, if found this value will change */
        kinzi = -1; /* There is no Kinzi, if found this value will change */
        if (getMyanmarCharClass(uc[cursor]) & Mymr_CF_VIRAMA)
            basE = findBase(uc, cursor, (int)item->item.length);
        else
            basE = findBase(uc, cursor + 1, (int)item->item.length);

        for (i = cursor; i < basE; i += 1) {
            char_class = getMyanmarCharClass(uc[i]);

            /* look for kinzi and remember position */
            if (((char_class & Mymr_CF_CLASS_MASK) == Mymr_CC_NGA) && (i + 2 < basE)
                    && ((getMyanmarCharClass(uc[i + 1]) & Mymr_CF_CLASS_MASK) == Mymr_CC_ASAT)
                    && (getMyanmarCharClass(uc[i + 2]) & Mymr_CF_VIRAMA)) {
                if (i + 3 == (int)item->item.length) {
                    reordered[len] = Mymr_C_DOTTED_CIRCLE;
                    len += 1;
                } else if (getMyanmarCharClass(uc[i + 3]) & Mymr_CF_CONSONANT) {
                    kinzi = i;
                    cursor += 3;
                }
            }

            /* if vowel e, wirte it out */
            if (i > 0 && ((getMyanmarCharClass(uc[i -1]) & Mymr_CF_CLASS_MASK) != Mymr_CC_CONSONANT)
                    && (char_class & Mymr_CF_DEP_VOWEL) && (char_class & Mymr_CF_POS_BEFORE)) {
                reordered[len] = Mymr_C_VOWEL_E;
                properties[len] = PostForm;
                len += 1;
            }

            /* look for medial ra and remember position */
            if ((char_class & Mymr_CF_CLASS_MASK) == Mymr_CC_MEDIAL_R) {
                medial_ra = i;
            }
        }

        /* write medial ra if found  */
        if (medial_ra > -1) {
            reordered[len] = Mymr_C_MEDIAL_R;
            properties[len] = PreForm;
            len += 1;
        }

        /* shall we add a dotted circle?
         * If in the position in which the base should be (first char in the string) there is
         * a character that has the Dotted circle flag (a character that cannot be a base)
         * then write a dotted circle
         */
        if (getMyanmarCharClass(uc[cursor]) & Mymr_CF_DOTTED_CIRCLE) {
            /* add dotted circle */
            reordered[len] = Mymr_C_DOTTED_CIRCLE;
            len += 1;
        }

        /* copy what is left to the output, skipping before vowels and
         * medial Ra if they are present
         */
        for (i = cursor; i < basE; i += 1) {
            char_class = getMyanmarCharClass(uc[i]);

            /* skip vowel e, it was already processed */
            if (i > 0 && ((getMyanmarCharClass(uc[i -1]) & Mymr_CF_CLASS_MASK) != Mymr_CC_CONSONANT)
                    &&(char_class & Mymr_CF_DEP_VOWEL) && (char_class & Mymr_CF_POS_BEFORE)) {
                continue;
            }

            /* skip medial ra, it was already processed */
            if (i == medial_ra) {
                continue;
            }

            switch (char_class & Mymr_CF_POS_MASK) {
            case Mymr_CF_POS_ABOVE:
                reordered[len] = uc[i];
                properties[len] = AboveForm;
                len += 1;
                break;

            case Mymr_CF_POS_AFTER:
                reordered[len] = uc[i];
                properties[len] = PostForm;
                len += 1;
                break;

            case Mymr_CF_POS_BELOW:
                reordered[len] = uc[i];
                properties[len] = BelowForm;
                len += 1;
                break;

            default:
                /* assign the correct flags to Medials */
                if ((char_class & Mymr_CF_MEDIAL) && i + 1 < basE) {
                    if ((char_class & Mymr_CF_CLASS_MASK) == Mymr_CC_MEDIAL_Y) { /* medial ya which possess below and right */
                        reordered[len] = uc[i];
                        properties[len] = PostForm;
                        len += 1;
                        break;
                    } else { /* others wa, ha only below */
                        reordered[len] = uc[i];
                        properties[len] = BelowForm;
                        len += 1;
                        break;
                    }
                }

                if ((char_class & Mymr_CF_VIRAMA) && i + 1 < basE) {
                    if ((getMyanmarCharClass(uc[i + 1]) & Mymr_CF_CLASS_MASK) == Mymr_CC_CONSONANT) { /* subscript consonant */
                        reordered[len] = uc[i];
                        properties[len] = BelowForm;
                        len += 1;
                        break;
                    }
                }

                /* assign the correct flags to consonant with subscript consonant */
                if ((char_class & Mymr_CF_CONSONANT) && i + 2 < basE) {
                    if ((getMyanmarCharClass(uc[i + 1]) & Mymr_CF_VIRAMA)
                            && (getMyanmarCharClass(uc[i + 2]) & Mymr_CF_CLASS_MASK) == Mymr_CC_CONSONANT) {
                        reordered[len] = uc[i];
                        properties[len] = BelowForm;
                        len += 1;
                        break;
                    }
                }

                /* kinzi_to_be */
                if (((char_class & Mymr_CF_CLASS_MASK) == Mymr_CC_NGA)
                        && (getMyanmarCharClass(uc[i + 1]) & Mymr_CF_ASAT)
                        && (getMyanmarCharClass(uc[i + 2]) & Mymr_CF_VIRAMA)
                        ) {
                    reordered[len] = uc[i];
                    i += 1;
                    reordered[len + 1] = uc[i];
                    i += 1;
                    reordered[len + 2] = uc[i];
                    properties[len] = AboveForm;
                    properties[len + 1] = AboveForm;
                    properties[len + 2] = AboveForm;
                    len += 3;
                    break;
                }

                /* default - any others */
                reordered[len] = uc[i];
                len += 1;
                break;
            }/* switch */

            if (kinzi > -1 && i == cursor) {
                reordered[len] = Mymr_C_NGA;
                reordered[len + 1] = Mymr_C_ASAT;
                reordered[len + 2] = Mymr_C_VIRAMA;
                properties[len] = AboveForm;
                properties[len + 1] = AboveForm;
                properties[len + 2] = AboveForm;
                len += 3;
            }
        } /* for: loop for a baseE */
        cursor = basE;
    } /* while: loop for a syllable*/

    if (!item->font->klass->convertStringToGlyphIndices(item->font,
                                                        reordered, len,
                                                        item->glyphs, &item->num_glyphs,
                                                        item->item.bidiLevel % 2))
        return FALSE;

    MMDEBUG("after shaping: len=%d", len);
    for (i = 0; i < len; i++) {
        item->attributes[i].mark = FALSE;
        item->attributes[i].clusterStart = FALSE;
        item->attributes[i].justification = 0;
        item->attributes[i].zeroWidth = FALSE;
        MMDEBUG("    %d: %4x property=%x", i, reordered[i], properties[i]);
    }

    /* now we have the syllable in the right order, and can start running it through open type. */

#ifndef NO_OPENTYPE
    if (openType) {
        hb_uint32 where[32];

        for (i = 0; i < len; ++i) {
            where[i] = ~(PreSubstProperty | BelowSubstProperty
                    | AboveSubstProperty | PostSubstProperty | CligProperty
                    | PositioningProperties);
            if (properties[i] & PreForm)
                where[i] &= ~PreFormProperty;
            if (properties[i] & BelowForm)
                where[i] &= ~BelowFormProperty;
            if (properties[i] & AboveForm)
                where[i] &= ~AboveFormProperty;
            if (properties[i] & PostForm)
                where[i] &= ~PostFormProperty;
        }

        HB_OpenTypeShape(item, where);
        if (!HB_OpenTypePosition(item, availableGlyphs, /*doLogClusters*/FALSE))
            return FALSE;
    } else
#endif
    {
        MMDEBUG("Not using openType");
        HB_HeuristicPosition(item);
    }

    item->attributes[0].clusterStart = TRUE;
    return TRUE;
}

HB_Bool HB_MyanmarShape(HB_ShaperItem *item)
{
    HB_Bool openType = FALSE;
    unsigned short *logClusters = item->log_clusters;

    HB_ShaperItem syllable = *item;
    int first_glyph = 0;

    int sstart = item->item.pos;
    int end = sstart + item->item.length;
    int i = 0;

    assert(item->item.script == HB_Script_Myanmar);
#ifndef NO_OPENTYPE
    openType = HB_SelectScript(item, myanmar_features);
#endif

    MMDEBUG("myanmar_shape: from %d length %d", item->item.pos, item->item.length);
    while (sstart < end) {
        HB_Bool invalid;
        int send = myanmar_nextSyllableBoundary(item->string, sstart, end, &invalid);
        MMDEBUG("syllable from %d, length %d, invalid=%s", sstart, send-sstart,
               invalid ? "TRUE" : "FALSE");
        syllable.item.pos = sstart;
        syllable.item.length = send-sstart;
        syllable.glyphs = item->glyphs + first_glyph;
        syllable.attributes = item->attributes + first_glyph;
        syllable.advances = item->advances + first_glyph;
        syllable.offsets = item->offsets + first_glyph;
        syllable.num_glyphs = item->num_glyphs - first_glyph;
        if (!myanmar_shape_syllable_with_font(openType, &syllable, invalid)) {
            MMDEBUG("syllable shaping failed, syllable requests %d glyphs", syllable.num_glyphs);
            item->num_glyphs += syllable.num_glyphs;
            return FALSE;
        }

        /* fix logcluster array */
        MMDEBUG("syllable:");
        for (i = first_glyph; i < first_glyph + (int)syllable.num_glyphs; ++i)
            MMDEBUG("        %d -> glyph %x", i, item->glyphs[i]);
        MMDEBUG("    logclusters:");
        for (i = sstart; i < send; ++i) {
            MMDEBUG("        %d -> glyph %d", i, first_glyph);
            logClusters[i-item->item.pos] = first_glyph;
        }
        sstart = send;
        first_glyph += syllable.num_glyphs;
    }
    item->num_glyphs = first_glyph;
    return TRUE;
}

void HB_MyanmarAttributes(HB_Script script, const HB_UChar16 *text, hb_uint32 from, hb_uint32 len, HB_CharAttributes *attributes)
{
    int end = from + len;
    const HB_UChar16 *uc = text + from;
    hb_uint32 i = 0;
    HB_UNUSED(script);
    attributes += from;
    while (i < len) {
	HB_Bool invalid;
	hb_uint32 boundary = myanmar_nextSyllableBoundary(text, from+i, end, &invalid) - from;

	attributes[i].charStop = TRUE;
        if (i)
            attributes[i-1].lineBreakType = HB_Break;

	if (boundary > len-1)
            boundary = len;
	i++;
	while (i < boundary) {
	    attributes[i].charStop = FALSE;
	    ++uc;
	    ++i;
	}
	assert(i == boundary);
    }
}

