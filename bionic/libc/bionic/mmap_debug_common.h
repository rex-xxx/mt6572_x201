/*
 * Copyright (C) 2009 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

/*
 * Contains declarations of types and constants used by malloc leak
 * detection code in both, libc and libc_malloc_debug libraries.
 */
#ifndef MMAP_DEBUG_COMMON_H
#define MMAP_DEBUG_COMMON_H

// =============================================================================
// function pointer definition
// =============================================================================
typedef int (*MmapDebugInit)();
typedef void (*Mmap_Bt)(void *,size_t);
typedef void (*Munmap_Bt)(void *);


Mmap_Bt mmap_bt ;//= NULL;
Munmap_Bt munmap_bt ;//= NULL;
// =============================================================================
// log functions
// =============================================================================
#define mmap_debug_log(format, ...)  \
    __libc_android_log_print(ANDROID_LOG_DEBUG, "MMAP_CHECK", (format), ##__VA_ARGS__ )
#define mmap_error_log(format, ...)  \
    __libc_android_log_print(ANDROID_LOG_ERROR, "MMAP_CHECK", (format), ##__VA_ARGS__ )
#define mmap_info_log(format,...)  \
    __libc_android_log_print(ANDROID_LOG_INFO, "MMAP_CHECK", (format), ##__VA_ARGS__ )
#endif  // MMAP_DEBUG_COMMON_H