/*
 * Copyright (C) 2010 Motorola, Inc.
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

#include <hardware/sensors.h>

#include "nusensors.h"

/*****************************************************************************/

/*
 * The SENSORS Module
 */

static const struct sensor_t sSensorList[] = {
        { "KXTF9 3-axis Accelerometer",
                "Kionix",
                1, SENSORS_HANDLE_BASE+ID_A,
                SENSOR_TYPE_ACCELEROMETER, MAX_RANGE_A, CONVERT_A, 0.57f, 20000, { } },
	{ "Ambient Light sensor",
                "Maxim",
                1, SENSORS_HANDLE_BASE+ID_L,
                SENSOR_TYPE_LIGHT, 208076.8f, 0.05f, 0.003f, 0,  { } },
	{ "AK8975 3-axis Magnetic field sensor",
                "Asahi Kasei",
                1, SENSORS_HANDLE_BASE+ID_M,
                SENSOR_TYPE_MAGNETIC_FIELD, 2000.0f, CONVERT_M, 6.8f, 30000, { } },
	{ "AK8975 Orientation sensor",
                "Asahi Kasei",
                1, SENSORS_HANDLE_BASE+ID_O,
                SENSOR_TYPE_ORIENTATION, 360.0f, CONVERT_O, 7.05f, 30000, { } },
	{ "BMP085 Pressure sensor",
                "Bosch",
                1, SENSORS_HANDLE_BASE+ID_B,
                SENSOR_TYPE_PRESSURE, 110000.0f, 1.0f, 1.0f, 30000, { } },
	{ "L3G4200D Gyroscope sensor",
                "ST Micro",
                1, SENSORS_HANDLE_BASE+ID_G,
                SENSOR_TYPE_GYROSCOPE, MAX_RANGE_G, CONVERT_G, 6.1f, 1250, { } },
};

static int open_sensors(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device);

static int sensors__get_sensors_list(struct sensors_module_t* module,
        struct sensor_t const** list)
{
    *list = sSensorList;
    return ARRAY_SIZE(sSensorList);
}

static struct hw_module_methods_t sensors_module_methods = {
    .open = open_sensors
};

struct sensors_module_t HAL_MODULE_INFO_SYM = {
    .common = {
        .tag = HARDWARE_MODULE_TAG,
        .version_major = 1,
        .version_minor = 0,
        .id = SENSORS_HARDWARE_MODULE_ID,
        .name = "Stingray SENSORS Module",
        .author = "Motorola",
        .methods = &sensors_module_methods,
    },
    .get_sensors_list = sensors__get_sensors_list
};

/*****************************************************************************/

static int open_sensors(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device)
{
    return init_nusensors(module, device);
}
