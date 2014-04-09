/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

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

#define LOG_TAG "Sensors"

#include <stdio.h>
#include <string.h>
#include <hardware/sensors.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <math.h>
#include <poll.h>
#include <pthread.h>

#include <linux/input.h>
#include <linux/hwmsensor.h>
#include <hwmsen_chip_info.h>


#include <cutils/atomic.h>
#include <cutils/log.h>
#include <cutils/native_handle.h>


 
/*****************************************************************************/

#define SUPPORTED_SENSORS  ((1<<MAX_ANDROID_SENSOR_NUM)-1)

extern struct sensor_t sSensorList[MAX_NUM_SENSOR];

/*****************************************************************************/

struct sensor_delay 
{
   int handle;
   uint32_t delay;
};

struct sensors_data_context_t {
	struct sensors_poll_device_t device;
	int events_fd;
	int device_io_fd;
	uint32_t active_sensors;
	sensors_event_t sensors[MAX_ANDROID_SENSOR_NUM];
	uint32_t pendingSensors;
	int (*activate)(struct sensors_data_context_t *dev,int handle, int enabled);
	int(*set_delay)(struct sensors_data_context_t *dev, int handle, int64_t ns);
	int( *poll)(struct sensors_data_context_t *dev, sensors_event_t* values, int count);
};

	
static int open_sensors(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device);

static int sensors__get_sensors_list(struct sensors_module_t* module,
        struct sensor_t const** list) 
{
    *list = sSensorList;
    return sizeof(sSensorList)/sizeof(sSensorList[0]);
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
        .name = "MTK SENSORS Module",
        .author = "The Android Open Source Project",
        .methods = &sensors_module_methods,
    },
    .get_sensors_list = sensors__get_sensors_list,
};


/*****************************************************************************/

static int open_input(int mode)
{
	/* scan all input drivers and look for HWM_INPUTDEV_NAME */
	int fd = -1;
	const char *dirname = "/dev/input";
	char devname[PATH_MAX];
	char *filename;
	DIR *dir;
	struct dirent *de;
	dir = opendir(dirname);
	if(dir == NULL)
	{
		return -1;
	}

	//LOGD("%s: into open_iput function!\r\n",__func__);
	strcpy(devname, dirname);
	filename = devname + strlen(devname);
	*filename++ = '/';
	while((de = readdir(dir)))
	{
		if(de->d_name[0] == '.' &&(de->d_name[1] == '\0' ||
			(de->d_name[1] == '.' && de->d_name[2] == '\0')))
		{
			continue;
		}
		
		strcpy(filename, de->d_name);
		fd = open(devname, mode);
		if (fd>=0)
		{
			char name[80];
			if (ioctl(fd, EVIOCGNAME(sizeof(name) - 1), &name) < 1)
			{
				name[0] = '\0';
			}
			if (!strcmp(name, HWM_INPUTDEV_NAME))
			{
				//LOGD("%s: using %s (name=%s)", __func__,devname, name);
				break;
			}
				close(fd);
				fd = -1;
		}
	}
	
	closedir(dir);

	if (fd < 0)
	{
		ALOGE("%s: Couldn't find or open '%s' driver (%s)",__func__, HWM_SENSOR_DEV_NAME, strerror(errno));
	}
	return fd;
}



/*****************************************************************************/
static int hwm__activate(struct sensors_data_context_t *dev,
        int handle, int enabled)
{
	//LOGE("+++++++++++++++++++++++++++++++++++++++++hwm__activate in HAL");

	int io_value;
	int err = 0;
	if((handle < SENSORS_HANDLE_BASE) || (handle >= SENSORS_HANDLE_BASE + ID_SENSOR_MAX_HANDLE))
	{
		ALOGE("%s: handle %d para error!",__func__, handle);
		return -1;
	}

	//open_sensor_ctl(dev);
	if(dev->device_io_fd == 0)
	{
		ALOGE("%s: file handle is null!",__func__);
		return -1;
	}
	
	ALOGD("%s: handle %d, enable or disable %d!", __func__, handle, enabled);
	
	uint32_t sensor = (1 << handle);	// new active/inactive sensor

	if(enabled == 1)
	{

		// TODO:  Device IO control to enable sensor
		if(ioctl(dev->device_io_fd, HWM_IO_ENABLE_SENSOR, &handle))
		{
			ALOGE("%s: Enable sensor %d error!",__func__, handle);
			return -1;
		}

		// When start orientation sensor, should start the G and M first.
		if(((dev->active_sensors & SENSOR_ORIENTATION) == 0) 	// hvae no orientation sensor
			&& (sensor & SENSOR_ORIENTATION))					// new orientation sensor start
		{
						
			io_value = ID_ACCELEROMETER;
			if(ioctl(dev->device_io_fd, HWM_IO_ENABLE_SENSOR_NODATA, &io_value))
			{
				ALOGE("%s: Enable ACCELEROMETR sensor error!",__func__);
				return -1;
			}
			

			io_value = ID_MAGNETIC;
			if(ioctl(dev->device_io_fd, HWM_IO_ENABLE_SENSOR_NODATA, &io_value))
			{
				ALOGE("%s: Enable MAGNETIC sensor error!",__func__);
				return -1;
			}
		}
		
		
		dev->active_sensors |= sensor;
	}
	else
	{
		dev->active_sensors &= ~sensor;

		// When stop Orientation, should stop G and M sensor if they are inactive
		if(((dev->active_sensors & SENSOR_ORIENTATION) == 0)
			&& (sensor & SENSOR_ORIENTATION))
		{
			

			io_value = ID_ACCELEROMETER;
			if(ioctl(dev->device_io_fd, HWM_IO_DISABLE_SENSOR_NODATA, &io_value))
			{
				ALOGE("%s: Disable ACCELEROMETR sensor error!",__func__);
				return -1;
			}
			
					
			io_value = ID_MAGNETIC;
			if(ioctl(dev->device_io_fd, HWM_IO_DISABLE_SENSOR_NODATA, &io_value))
			{
				ALOGE("%s: Disable MAGNETIC sensor error!",__func__);
				return -1;
			}
			
		}		

		// TODO: Device IO control disable sensor
		if(ioctl(dev->device_io_fd, HWM_IO_DISABLE_SENSOR, &handle))
		{
			ALOGE("%s: Disable sensor %d error!",__func__, handle);
			return -1;
		}
	}

	return 0;
	
}

//static int control__set_delay(struct sensors_control_context_t *dev, int32_t ms)
static int hwm__set_delay(struct sensors_data_context_t *dev, int handle, int64_t ns)

{
	//LOGE("+++++++++++++++++++++++++++++++++++++++++hwm__set_delay in HAL");
	ALOGD("%s: Set delay %lld ns", __func__,ns);
	struct sensor_delay delayPara;
	delayPara.delay =  ns/1000000;
	delayPara.handle = handle;
	
	if (dev->device_io_fd <= 0)
	{
		return -1;
	}

	if(delayPara.delay < 10)  //set max sampling rate = 50Hz
    {
        delayPara.delay = 10;
        ALOGD("Control set delay %lld ns is too small \n", ns);
    }	

	// TODO: Set delay time to device
	if(ioctl(dev->device_io_fd, HWM_IO_SET_DELAY, &delayPara))
	{
		ALOGE("%s: Set delay %d ms error ", __func__, delayPara.delay);
		return -errno;
	}
	
	
	return 0;
}

static int pick_sensor(struct sensors_data_context_t *dev,sensors_event_t* values, int count)
{
//	LOGD("%s: pick get sensor data", __func__);
	int num = 0;
	uint32_t mask = SUPPORTED_SENSORS;
	while(mask & (num <= count))
	{
		uint32_t i = 31 - __builtin_clz(mask);
		mask &= ~(1<<i);
		if(dev->pendingSensors & (1<<i))
		{
			dev->pendingSensors &= ~(1<<i);
			*values++ = dev->sensors[i];
			
			//LOGD("%s: sensor %d get data [%f, %f, %f]", __func__, i,values->data[0],values->data[1],values->data[2]);
			
			//values++;
			//values->sensor = ;
			
			num++;
		}
	}
	return num;
}

static int hwm__poll(struct sensors_data_context_t *dev, sensors_event_t* values, int count)
{
	//LOGE("+++++++++++++++++++++++++++++++++++++++++hwm__poll in HAL!");

	int res,i, nread;
	hwm_trans_data sensors_data;
	struct input_event event;
	
	int fd_event = dev->events_fd;
	int fd_io = dev->device_io_fd;	
	//LOGD("%s: polling get sensor data, fd %d. \r\n", __func__, fd_event);
	if(fd_event < 0)
	{
		ALOGE("%s: invalid file descriptor, fd=%d",__func__, fd_event);
		return -1;
	}

	// There are pending sensors, return them now..
	if(dev->pendingSensors)
	{
		return pick_sensor(dev, values, count);
	}

	memset(&sensors_data, 0 , sizeof(hwm_trans_data));
	// read the input event	
	nread = read(fd_event, &event, sizeof(event));
	if(nread == sizeof(event))
	{
		uint32_t v;
		//LOGD("%s: event type: %d, code %d, value %d!\r\n", __func__,event.type, event.code, event.value);
		if(event.type == EV_REL)
		{
			if((event.code == EVENT_TYPE_SENSOR) && (event.value != 0))
			{
				// TODO: get the sensor data;				
				sensors_data.date_type = event.value;
				res = ioctl(fd_io, HWM_IO_GET_SENSORS_DATA, &sensors_data);
				if(res != 0)
				{
					ALOGE("%s: Get sensors data error",__func__);
				}
				else
				{
					for(i =0; i < MAX_ANDROID_SENSOR_NUM; i++)
					{
						//LOGD("%s:get sensor value,type: %d, value1 %d, updata %d!\r\n", __func__,
							//sensors_data.data[i].sensor, sensors_data.data[i].values[0], sensors_data.data[i].update);
						if(sensors_data.data[i].update == 0)
						{
							continue;
						}
						
						dev->pendingSensors |= 1 << i;
						dev->sensors[i].sensor = sensors_data.data[i].sensor;
						dev->sensors[i].type = dev->sensors[i].sensor + 1;
						dev->sensors[i].data[0] = (float)sensors_data.data[i].values[0];
						dev->sensors[i].data[1] = (float)sensors_data.data[i].values[1];
						dev->sensors[i].data[2] = (float)sensors_data.data[i].values[2];
						dev->sensors[i].acceleration.status = sensors_data.data[i].status;
						dev->sensors[i].timestamp = sensors_data.data[i].time;
						
						//LOGD("+++++++++++++++++++sensors[%d] timestamp = %ld", i , dev->sensors[i].timestamp);

						if(sensors_data.data[i].value_divide > 1)
						{
							
							dev->sensors[i].data[0] = dev->sensors[i].data[0] / sensors_data.data[i].value_divide;
							dev->sensors[i].data[1] = dev->sensors[i].data[1] / sensors_data.data[i].value_divide;
							dev->sensors[i].data[2] = dev->sensors[i].data[2] / sensors_data.data[i].value_divide;
						/*
							if(dev->sensors[i].sensor <= ID_GYROSCOPE)
							{
								dev->sensors[i].data[0] = dev->sensors[i].data[0] / sensors_data.data[i].value_divide;
								dev->sensors[i].data[1] = dev->sensors[i].data[1] / sensors_data.data[i].value_divide;
								dev->sensors[i].data[2] = dev->sensors[i].data[2] / sensors_data.data[i].value_divide;
							}
							else
							{
								dev->sensors[i].vector.v[0] = dev->sensors[i].data[0] / sensors_data.data[i].value_divide;
							}
						*/	
						}	
					}
				}
			}
			else
			{

			}
		}
	}

	if(dev->pendingSensors)
	{
		return pick_sensor(dev, values, count);
	}

	return 0;
}

/*****************************************************************************/
static int data__close(struct hw_device_t *dev) 
{
	struct sensors_data_context_t* ctx = (struct sensors_data_context_t*)dev;
	if (ctx)
	{
		if (ctx->events_fd > 0)
		{
			ALOGD("%s: about to close fd=%d",__func__, ctx->events_fd);
			close(ctx->events_fd);
		}
		if(ctx->device_io_fd > 0)
		{
			ALOGD("%s: about to close fd=%d",__func__, ctx->device_io_fd);
			close(ctx->device_io_fd);
		}
		free(ctx);
	}
	return 0;
}

static int control__activate(struct sensors_poll_device_t *dev,
        int handle, int enabled) 
{
    struct sensors_data_context_t* ctx = (struct sensors_data_context_t*)dev;
	
    return ctx->activate((struct sensors_data_context_t*)dev,handle, enabled);
}

static int control__setDelay(struct sensors_poll_device_t *dev,
        int handle, int64_t ns) 
{
    struct sensors_data_context_t* ctx = (struct sensors_data_context_t*)dev;
	
    return ctx->set_delay((struct sensors_data_context_t*)dev,handle, ns);
}

static int data__poll(struct sensors_poll_device_t *dev,
        sensors_event_t* data, int count) 
{
    struct sensors_data_context_t* ctx = (struct sensors_data_context_t*)dev;
	
    return ctx->poll((struct sensors_data_context_t*)dev,data, count);
}

/** Open a new instance of a sensor device using name */
static int open_sensors(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device)
{
	int status = -EINVAL;
	ALOGD("%s: name: %s!\r\n", __func__, name);
	
	struct sensors_data_context_t *dev;
	dev = malloc(sizeof(*dev));
	memset(dev, 0, sizeof(*dev));
	dev->events_fd = open_input(O_RDONLY);
	if (dev->events_fd < 0) 
	{
		ALOGE("%s: Open input device error!",__func__);
	    return -1;
	}
	dev->device_io_fd = -1;
	if(dev->device_io_fd <= 0)
	{
		dev->device_io_fd = open(HWM_SENSOR_DEV, O_RDONLY);
		ALOGD("%s: device handle %d",__func__, dev->device_io_fd);
	}
	dev->activate =  hwm__activate;
	dev->set_delay = hwm__set_delay;
	dev->poll = hwm__poll;
	
	dev->device.common.tag = HARDWARE_DEVICE_TAG;
    dev->device.common.version  = 0;
    dev->device.common.module   = (struct hw_module_t*)module;
    dev->device.common.close    = data__close;
    dev->device.activate        = control__activate;
    dev->device.setDelay        = control__setDelay;
    dev->device.poll            = data__poll;

    *device = &dev->device.common;
    status = 0;
	return status;
}

