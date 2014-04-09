/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef ANDROID_SENSORS_H
#define ANDROID_SENSORS_H

#include <stdint.h>
#include <errno.h>
#include <sys/cdefs.h>
#include <sys/types.h>
#include <math.h>

#include <linux/input.h>

#include <hardware/hardware.h>
#include <hardware/sensors.h>

__BEGIN_DECLS

/*****************************************************************************/

int init_nusensors(hw_module_t const* module, hw_device_t** device);

/*****************************************************************************/

#define ARRAY_SIZE(a) (sizeof(a) / sizeof(a[0]))

#define ID_A  (0)
#define ID_M  (1)
#define ID_O  (2)
#define ID_T  (3)
#define ID_P  (4)
#define ID_L  (5)
#define ID_B  (6)
#define ID_G  (7)

/*****************************************************************************/

/*
 * The SENSORS Module
 */

/* the SFH7743 is a binary proximity sensor that triggers around 6 cm on
 * this hardware */
#define PROXIMITY_THRESHOLD_CM  6.0f

/*****************************************************************************/

#define AKM_DEVICE_NAME             "/dev/akm8975_aot"
#define ACCELEROMETER_DEVICE_NAME   "/dev/kxtf9"
#define LIGHTING_DEVICE_NAME        "/dev/max9635"
#define BAROMETER_DEVICE_NAME       "/dev/bmp085"
#define GYROSCOPE_DEVICE_NAME       "/dev/l3g4200d"

#define EVENT_TYPE_ACCEL_X          REL_X
#define EVENT_TYPE_ACCEL_Y          REL_Y
#define EVENT_TYPE_ACCEL_Z          REL_Z

#define EVENT_TYPE_YAW              REL_RX
#define EVENT_TYPE_PITCH            REL_RY
#define EVENT_TYPE_ROLL             REL_RZ
#define EVENT_TYPE_ORIENT_STATUS    REL_HWHEEL

#define EVENT_TYPE_MAGV_X           REL_DIAL
#define EVENT_TYPE_MAGV_Y           REL_WHEEL
#define EVENT_TYPE_MAGV_Z           REL_MISC

#define EVENT_TYPE_LIGHT            MSC_RAW
#define EVENT_TYPE_PRESSURE         ABS_PRESSURE

#define EVENT_TYPE_GYRO_P           REL_RX
#define EVENT_TYPE_GYRO_R           REL_RY
#define EVENT_TYPE_GYRO_Y           REL_RZ

// 1024 LSG = 1G
#define LSG                         (1024.0f)
#define MAX_RANGE_A                 (2*GRAVITY_EARTH)
// conversion of acceleration data to SI units (m/s^2)
#define CONVERT_A                   (GRAVITY_EARTH / LSG)
#define CONVERT_A_X                 (CONVERT_A)
#define CONVERT_A_Y                 (CONVERT_A)
#define CONVERT_A_Z                 (CONVERT_A)

// conversion of magnetic data to uT units
#define CONVERT_M                   (1.0f/16.0f)
#define CONVERT_M_X                 (CONVERT_M)
#define CONVERT_M_Y                 (CONVERT_M)
#define CONVERT_M_Z                 (CONVERT_M)

#define CONVERT_O                   (1.0f/64.0f)
#define CONVERT_O_Y                 (CONVERT_O)
#define CONVERT_O_P                 (CONVERT_O)
#define CONVERT_O_R                 (-CONVERT_O)

// conversion of angular velocity(millidegrees/second) to rad/s
#define MAX_RANGE_G                 (2000.0f * ((float)(M_PI/180.0f)))
#define CONVERT_G                   ((70.0f/1000.0f) * ((float)(M_PI/180.0f)))
#define CONVERT_G_P                 (CONVERT_G)
#define CONVERT_G_R                 (CONVERT_G)
#define CONVERT_G_Y                 (CONVERT_G)

#define CONVERT_B                   (1.0f/100.0f)

#define SENSOR_STATE_MASK           (0x7FFF)

/*****************************************************************************/

__END_DECLS

#endif  // ANDROID_SENSORS_H
