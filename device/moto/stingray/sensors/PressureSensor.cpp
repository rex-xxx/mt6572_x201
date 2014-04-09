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

#include <fcntl.h>
#include <errno.h>
#include <math.h>
#include <poll.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/select.h>

#include <linux/bmp085.h>

#include <cutils/log.h>

#include "PressureSensor.h"

/*****************************************************************************/

PressureSensor::PressureSensor()
    : SensorBase(BAROMETER_DEVICE_NAME, "barometer"),
      mEnabled(0),
      mInputReader(32)
{
    mPendingEvent.version = sizeof(sensors_event_t);
    mPendingEvent.sensor = ID_B;
    mPendingEvent.type = SENSOR_TYPE_PRESSURE;
    memset(mPendingEvent.data, 0x00, sizeof(mPendingEvent.data));

    open_device();

    // read the actual value of all sensors if they're enabled already
    struct input_absinfo absinfo;
    int flags = 0;
    if (!ioctl(dev_fd, BMP085_IOCTL_GET_ENABLE, &flags)) {
        if (flags)  {
            mEnabled = 1;
            if (!ioctl(data_fd, EVIOCGABS(EVENT_TYPE_PRESSURE), &absinfo)) {
                mPendingEvent.pressure = absinfo.value * CONVERT_B;
            }
        }
    }
    if (!mEnabled) {
        close_device();
    }
}

PressureSensor::~PressureSensor() {
}

int PressureSensor::enable(int32_t, int en)
{
    int flags = en ? 1 : 0;
    int err = 0;
    if (flags != mEnabled) {
        if (flags) {
            open_device();
        }
        err = ioctl(dev_fd, BMP085_IOCTL_SET_ENABLE, &flags);
        err = err<0 ? -errno : 0;
        ALOGE_IF(err, "BMP085_IOCTL_SET_ENABLE failed (%s)", strerror(-err));
        if (!err) {
            mEnabled = flags;
        }
        if (!flags) {
            close_device();
        }
    }
    return err;
}

int PressureSensor::setDelay(int32_t handle, int64_t ns)
{
    if (ns < 0)
        return -EINVAL;

    int delay = ns / 1000000;
    if (ioctl(dev_fd, BMP085_IOCTL_SET_DELAY, &delay)) {
        return -errno;
    }
    return 0;
}

int PressureSensor::readEvents(sensors_event_t* data, int count)
{
    if (count < 1)
        return -EINVAL;

    ssize_t n = mInputReader.fill(data_fd);
    if (n < 0)
        return n;
    int numEventReceived = 0;
    input_event const* event;

    while (count && mInputReader.readEvent(&event)) {
        int type = event->type;
        if (type == EV_ABS) {
            processEvent(event->code, event->value);
        } else if (type == EV_SYN) {
            int64_t time = timevalToNano(event->time);
            mPendingEvent.timestamp = time;
            if (mEnabled) {
                *data++ = mPendingEvent;
                count--;
                numEventReceived++;
            }
        } else {
            ALOGE("PressureSensor: unknown event (type=%d, code=%d)",
                    type, event->code);
        }
        mInputReader.next();
    }

    return numEventReceived;
}

void PressureSensor::processEvent(int code, int value)
{
    if (code == EVENT_TYPE_PRESSURE) {
            mPendingEvent.pressure = value * CONVERT_B;
    }
}
