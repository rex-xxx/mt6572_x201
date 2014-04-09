/*
 * Copyright © 2009  Red Hat, Inc.
 * Copyright © 2011  Google, Inc.
 *
 *  This is part of HarfBuzz, a text shaping library.
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
 *
 * Red Hat Author(s): Behdad Esfahbod
 * Google Author(s): Behdad Esfahbod
 */

#ifndef HB_ICU_H
#define HB_ICU_H

#include "hb.h"

#include <unicode/uscript.h>

HB_BEGIN_DECLS


hb_script_t
hb_icu_script_to_script (UScriptCode script);

UScriptCode
hb_icu_script_from_script (hb_script_t script);


hb_unicode_funcs_t *
hb_icu_get_unicode_funcs (void);

/** M: Add API to get script @{ */
/** Merge from harfbuzz-old @{ */

static const uint32_t hb_invalid_codepoint = 0xffffffffu;

#define hb_is_high_surrogate(ucs) \
    (((ucs) & 0xfc00) == 0xd800)

#define hb_is_low_surrogate(ucs) \
    (((ucs) & 0xfc00) == 0xdc00)

#define hb_surrogate_to_ucs4(high, low) \
    (((hb_codepoint_t)(high)) <<10 ) + (low) - 0x35fdc00;

// -----------------------------------------------------------------------------
// Find the next script run in a UTF-16 string.
//
// A script run is a subvector of codepoints, all of which are in the same
// script. A run will never cut a surrogate pair in half at either end.
//
// num_code_points: (output, maybe NULL) the number of code points in the run
// output: (output) the @script_run_tart, @script_run_length and @script are set on success
// chars: the UTF-16 string
// len: the length of @chars, in words
// iter: (in/out) the current index into the string. This should be 0 for the
// first call and is updated on exit.
//
// returns: true if a script run was found and returned.
// -----------------------------------------------------------------------------
bool hb_icu_utf16_script_run_next(unsigned *num_code_points, ssize_t &script_run_tart,
        size_t &script_run_length, hb_script_t &script,
        const uint16_t *chars, size_t len, ssize_t *iter);

// -----------------------------------------------------------------------------
// This is the same as above, except that the input is traversed backwards.
// Thus, on the first call, |iter| should be |len| - 1.
// -----------------------------------------------------------------------------
bool hb_icu_utf16_script_run_prev(unsigned *num_code_points, ssize_t &script_run_tart,
        size_t &script_run_length, hb_script_t &script,
        const uint16_t *chars, size_t len, ssize_t *iter);

/** @} */

HB_END_DECLS

#endif /* HB_ICU_H */
