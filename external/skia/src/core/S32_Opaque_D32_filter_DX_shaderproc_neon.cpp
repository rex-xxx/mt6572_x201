/*
 * Copyright (c) 2010, Code Aurora Forum. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

void S32_Opaque_D32_filter_DX_shaderproc_neon(const unsigned int* image0, const unsigned int* image1,
                                        SkFixed fx, unsigned int maxX, unsigned int subY,
                                         unsigned int* colors,
                                         SkFixed dx, int count) {

    asm volatile(
            "mov r3, %[count]    \n\t"    //r3 = count

            "mov r5, %[fx] \n\t"        //r5 = x = fx
            "cmp r3, #0 \n\t"
            "beq endloop \n\t"

            "vdup.8         d17, %[subY]                \n\t"   // duplicate y into d17
            "vmov.u8        d16, #16                \n\t"   // set up constant in d16
            "vsub.u8        d18, d16, d17             \n\t"   // d18 = 16-y

            "vmov.u16       d16, #16                \n\t"   // set up constant in d16,int 16bit

#define UNROLL8
#define UNROLL2
#ifdef UNROLL8
            "cmp r3, #8 \n\t"
            "blt initloop2 \n\t"
            ///////////////loop2 in x
            "beginloop8:    \n\t"

                /////////////////pixel 1////////////////////////////////////
                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18              \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17              \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d22, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d22, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d22, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d22, d0, d20              \n\t"   // d4 += a10 * (16-x)

                //////////////// end bilinear interp

                "add r5, r5, %[dx] \n\t"    //r5 = x += dx

                /////////////////pixel 2////////////////////////////////////
                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18              \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17              \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d24, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d24, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d24, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d24, d0, d20              \n\t"   // d4 += a10 * (16-x)

                //////////////// end bilinear interp

                "add r5, r5, %[dx] \n\t"    //r5 = x += dx

                /////////////////pixel 3////////////////////////////////
                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18               \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17               \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d26, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d26, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d26, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d26, d0, d20              \n\t"   // d4 += a10 * (16-x)

                //////////////// end bilinear interp

                "add r5, r5, %[dx] \n\t"    //r5 = x += dx

                /////////////////pixel 4////////////////////////////////
                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18               \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17               \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d28, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d28, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d28, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d28, d0, d20              \n\t"   // d4 += a10 * (16-x)

                //////////////// end bilinear interp

                "add r5, r5, %[dx] \n\t"    //r5 = x += dx

                /////////////////pixel 5////////////////////////////////////
                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18               \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17               \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d23, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d23, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d23, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d23, d0, d20              \n\t"   // d4 += a10 * (16-x)

                //////////////// end bilinear interp

                "add r5, r5, %[dx] \n\t"    //r5 = x += dx

                /////////////////pixel 6////////////////////////////////////
                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18               \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17               \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d25, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d25, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d25, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d25, d0, d20              \n\t"   // d4 += a10 * (16-x)

                //////////////// end bilinear interp

                "add r5, r5, %[dx] \n\t"    //r5 = x += dx

                /////////////////pixel 7////////////////////////////////
                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18               \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17               \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d27, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d27, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d27, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d27, d0, d20              \n\t"   // d4 += a10 * (16-x)

                //////////////// end bilinear interp

                "add r5, r5, %[dx] \n\t"    //r5 = x += dx

                /////////////////pixel 8////////////////////////////////
                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18               \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17               \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d29, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d29, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d29, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d29, d0, d20              \n\t"   // d4 += a10 * (16-x)

                //////////////// Store results///////////////////

                "vshrn.i16      d0, q11, #8              \n\t"   // shift down result by 8
                "vshrn.i16      d1, q12, #8              \n\t"   // shift down result by 8
                "vshrn.i16      d2, q13, #8              \n\t"   // shift down result by 8
                "vshrn.i16      d3, q14, #8              \n\t"   // shift down result by 8

                "vst4.u32        {d0, d1, d2, d3}, [%[colors]]!       \n\t"   // store result

                //////////////// end bilinear interp

                "sub r3, r3, #8    \n\t"    //num -=8
                "add r5, r5, %[dx] \n\t"    //r5 = x += dx
                "cmp r3, #7 \n\t"

                "bgt        beginloop8  \n\t"

            "endloop8:    \n\t"
            ////////////////end loop in x
#endif    //UNROLL8



#ifdef UNROLL2
            "initloop2: \n\t"
            "cmp r3, #2 \n\t"
            "blt initloop \n\t"
            ///////////////loop2 in x
            "beginloop2:    \n\t"


                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18               \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17               \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d22, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d22, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d22, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d22, d0, d20              \n\t"   // d4 += a10 * (16-x)

                //////////////// end bilinear interp

                "add r5, r5, %[dx] \n\t"    //r5 = x += dx

                /////////////////second half////////////////////////////////
                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18               \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17               \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19             \n\t"   // d20 = 16-x

                "vmul.i16       d23, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d23, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d23, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d23, d0, d20              \n\t"   // d4 += a10 * (16-x)
                "vshrn.i16      d0, q11, #8               \n\t"   // shift down result by 8

                "vst1.u32        {d0}, [%[colors]]!       \n\t"   // store result

                //////////////// end bilinear interp

                "sub r3, r3, #2    \n\t"    //num -=2
                "add r5, r5, %[dx] \n\t"    //r5 = x += dx
                "cmp r3, #1 \n\t"

                "bgt        beginloop2  \n\t"

            "endloop2:    \n\t"
            ////////////////end loop in x
#endif    //UNROLL2

#if defined (UNROLL2) || defined (UNROLL8)
            "initloop: \n\t"
            "cmp r3, #0 \n\t"
            "ble endloop \n\t"
#endif    //defined (UNROLL2) || defined (UNROLL8)

            ///////////////loop in x
            "beginloop:    \n\t"


                //x0 = SkClampMax((fx) >> 16, max)
                "asr r4, r5, #16 \n\t"

                "lsl r4, r4, #2 \n\t"
                "add r6, r4, %[image0] \n\t"
                "vldr.32 d4, [r6] \n\t"
                "add r6, r4, %[image1] \n\t"
                "vldr.32 d5, [r6] \n\t"

                //(((fx) >> 12) & 0xF)
                "lsr r4, r5, #12 \n\t"
                "and r4, r4, #15 \n\t"
                "vdup.16        d19, r4                \n\t"   // duplicate x into d19


                ////////////bilinear interp

                "vmull.u8       q3, d4, d18              \n\t"   // q3 = [a01|a00] * (16-y)
                "vmull.u8       q0, d5, d17              \n\t"   // q0 = [a11|a10] * y

                "vsub.u16       d20, d16, d19            \n\t"   // d20 = 16-x

                "vmul.i16       d4, d7, d19              \n\t"   // d4  = a01 * x
                "vmla.i16       d4, d1, d19              \n\t"   // d4 += a11 * x
                "vmla.i16       d4, d6, d20              \n\t"   // d4 += a00 * (16-x)
                "vmla.i16       d4, d0, d20              \n\t"   // d4 += a10 * (16-x)
                "vshrn.i16      d0, q2, #8               \n\t"   // shift down result by 8

                "vst1.u32        {d0[0]}, [%[colors]]!       \n\t"   // store result

                //////////////// end bilinear interp

                "sub r3, r3, #1    \n\t"    //num -=1
                "add r5, r5, %[dx] \n\t"    //r5 = x += dx
                "cmp r3, #0 \n\t"
                "bgt        beginloop  \n\t"

            "endloop:    \n\t"
            ////////////////end loop in x
            : [colors] "+r" (colors)
            : [image0] "r" (image0), [image1] "r" (image1), [fx] "r" (fx), [maxX] "r" (maxX), [subY] "r" (subY),
             [dx] "r" (dx), [count] "r" (count)
            : "cc", "memory", "r3", "r4", "r5", "r6", "d0", "d1", "d2", "d3", "d4", "d5", "d6", "d7", "d16", "d17", "d18", "d19", "d20", "d21", "d22", "d23", "d24", "d25", "d26", "d27", "d28", "d29"
            );


}
