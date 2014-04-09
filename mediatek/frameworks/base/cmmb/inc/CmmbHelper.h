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

#ifndef CMMB_HELPER_H
#define CMMB_HELPER_H

// CmmbHelper.h
//
#include <utils/Log.h>
#include "../osal/OSAL_def.h"
#include "CmmbSPCommon.h"
#include "CmmbSpDefs.h"
#include "CmmbReadBits.h"

/********************/
/* service provider log  */
/********************/
#define debughelper() SP_LOGE("%s,%d", __func__, __LINE__)

#ifdef LOG_TAG
#undef LOG_TAG
#endif
#define LOG_TAG "CMMB_SP"
#define INNOADR_ENABLE_LOGS

#ifdef INNOADR_ENABLE_LOGS
#define SP_LOGE(fmt, args...) ALOGE( "cmmbSP - %s(): " fmt "\n", __FUNCTION__, ##args)
#define SP_LOGW(fmt, args...) ALOGW( "cmmbSP - %s(): " fmt "\n", __FUNCTION__, ##args)
#define SP_LOGI(fmt, args...) ALOGI( "cmmbSP - %s(): " fmt "\n", __FUNCTION__, ##args)
#define SP_LOGD(fmt, args...) ALOGD( "cmmbSP - %s(): " fmt "\n", __FUNCTION__, ##args)
#else
#define SP_LOGE(fmt, args...) 
#define SP_LOGW(fmt, args...)
#define SP_LOGI(fmt, args...)
#define SP_LOGD(fmt, args...)
#endif

/**************************************************
				macro  and enum 
***************************************************/

#define MAX_FREQUENCY_NUM 		36
#define MAX_SERVICE_NUM_PER_FREQ	15
#define MAX_MPX_FRAME_SIZE		(640*1024)
#define MAX_MPX_FRAME_COUNT		3

// internal event ID
#define INTER_START_ESG			20
#define INTER_STOP_ESG				21
#define INTER_UPDATE_SIGNAL		22
#define INTER_PARSE_TS0        		24

#define CMMB_CMD_RES_MAX_TIME   	5000       //2500 Note: change to 5s due to tune timeout
#define SERVICE_HANDLE_TS0			1
#define MAX_TS0_SIZE				(64*1024)

enum ECurrentRequest
{
	REQ_NONE = 0,
	REQ_UAM_INIT,
	REQ_UAM_SEND,
	REQ_TUNE,
	REQ_START_TS0,
	REQ_STOP_TS0,
	REQ_START_SERVICE,
	REQ_STOP_SERVICE,
	REQ_GET_STAT,
};

// service provider state
enum ECmmbSPState
{
	SP_STATE_NONE = 0,
	SP_STATE_READY,
	SP_STATE_CONNECTED,
	SP_STATE_RECEIVING,
	SP_STATE_ERROR,
};

typedef enum _TCmmbChipType
{
	CMMB_NONE=-1,
        CMMB_SIANO=0,
	CMMB_INNOFIDEI	 
}CmmbChipTypeStruct;
/**************************************************
				struct
***************************************************/

struct CmmbService
{
	UINT32 serviceId;	// service ID
	UINT32 subFrmIdx;	// sub-frame index
	UINT32 serviceHdl;	// service handle
};

typedef struct esg_update_info_t
{
	UINT32 freq;
	UINT8 usn;

}esg_update_info;

/**************************************************
				class
***************************************************/

class CMutex
{
public:
	CMutex()
		: mutex_()
	{
		UINT32 ret;
		ret = OSAL_MutexCreate(&mutex_);
		assert(ret == OSAL_OK);
	}

	~CMutex()
	{
		OSAL_MutexDelete(&mutex_);
	}

	bool Get()
	{
		return (OSAL_MutexGet(&mutex_) == OSAL_OK);
	}

	bool Put()
	{
		return (OSAL_MutexPut(&mutex_) == OSAL_OK);
	}

private:
	pthread_mutex_t	mutex_;
};

class CAutoLock
{
public:
	CAutoLock(CMutex& mutex)
		: mutex_(mutex)
	{
		mutex_.Get();
	}

	~CAutoLock()
	{
		mutex_.Put();
	}

private:
	CMutex& mutex_;
};

class CEvent
{
public:
	CEvent()
		: event_()
	{
		UINT32 ret;
		ret = OSAL_EventCreate(&event_);
		assert(ret == OSAL_OK);
	}

	~CEvent()
	{
		OSAL_EventDelete(&event_);
	}

	bool Set()
	{
		return (OSAL_EventSet(&event_) == OSAL_OK);
	}

	bool Clear()
	{
		return (OSAL_EventClear(&event_) == OSAL_OK);
	}

	// OSAL_WAIT_FOREVER for unlimited wait
	CmmbResult Wait(UINT32 timeout);

private:
	OSAL_Event	event_;
};

class CMessageQueue
{
public:
	typedef UINT32 (*MessageProc)(UINT32 msgId, UINT8* payload, UINT32 payloadLen);

	CMessageQueue(MessageProc msgProc)
		: msgProc_(msgProc)
		, headPos_(0)
		, tailPos_(0) 
		, taskId_(null) 
		, exitFlag_(false) 
		, fullFlag_(false) 
	{
		//InitTask();
	}

	~CMessageQueue()
	{
		//TermTask();
	}

	void Clear();			// clear all existent messages

	void PostMessage(UINT32 msgId, UINT8* payload, UINT32 payloadLen);

	bool InitTask();		// initialize task
	void TermTask();		// terminate task

private:

	void ProcMessages();	// process messages
	void IncPtr(UINT32& ptr);

	bool QueueIsEmpty()
	{
		return (headPos_ == tailPos_
			&& !fullFlag_);
	}

	static UINT32 TaskProc(UINT32 param);

	static const UINT32 MAX_QUEUE_SIZE = 20;

	struct CMsg
	{
		UINT32 msgId;
		UINT8* payload;
		UINT32 payloadLen;

		CMsg()
			: msgId(0)
			, payload(null)
			, payloadLen(0) {}
	};

	MessageProc msgProc_;
	CMutex		mutex_;		// queue read/write lock
	CEvent		event_;		// queue message proc event
	CMsg		queue_[MAX_QUEUE_SIZE];
	UINT32		headPos_;	// queue head position
	UINT32		tailPos_;	// queue tail position
	void*	taskId_;
	bool		exitFlag_;	// let task exit
	bool		fullFlag_;	// queue is full
};


template<int MaxBufSize, int MaxBufCount>	
class CBufferQueue
{
public:
	CBufferQueue()
		: count_(0) {}

    bool Append(UINT8* buffer, UINT32 bufLen)
    {
        assert(buffer != null);
        assert(bufLen <= MAX_MPX_FRAME_SIZE);

        Buffer* buf = null;
        bool bRet = true;

        if (QueueIsFull())
        {
            // no empty item, erase the first one
        #ifndef WIN32
            SP_LOGE("cmmbSP - no buffer, drop a Mpx Frame");
        #endif
            bRet = false;
            buf = GetBuffer(0);
            assert(buf != null);

            buf->index = MaxBufCount-1;

            // adjust index of other items
            for (int i = 0; i < MaxBufCount; i++)
            {
                if (&array_[i] != buf)
                    array_[i].index--;
            }
        }
        else
        {
            assert(count_ < MaxBufCount);

            // find a empty item
            buf = GetBuffer(-1);
            assert(buf != null);

            buf->index = count_;
            count_++;
        }

        memcpy(buf->buffer, buffer, bufLen);
        buf->bufLen = bufLen;

        return bRet;
    }

	UINT8* GetFront(UINT32& bufLen)
	{
		if (count_ == 0)
			return null;

		Buffer* buf = GetBuffer(0);
		assert(buf != null);

		bufLen = buf->bufLen;
		return buf->buffer;
	}

	void PopFront()
	{
		assert(count_ > 0);

		Buffer* buf = GetBuffer(0);
		assert(buf != null);

		// adjust index of other items
		for (int i = 0; i < MaxBufCount; i++)
		{
			if (&array_[i] != buf
				&& array_[i].index != -1)
			{
				array_[i].index--;
			}
		}

		buf->Clear();
		count_--;
	}

	CMutex& GetMutex()
	{
		return mutex_;
	}

	void Clear()
	{
		CAutoLock lock(mutex_);

		for (int i = 0; i < MaxBufCount; i++)
		{
			array_[i].Clear();
		}

		count_ = 0;
	}

	int GetCount()
	{
		return count_;
	}

private:
	struct Buffer
	{
		UINT8	buffer[MaxBufSize];
		UINT32	bufLen;
		int		index;

		Buffer()
		{
			Clear();
		}

		void Clear()
		{
			bufLen = 0;
			index = -1;
		}
	};

	Buffer* GetBuffer(int index)
	{
		assert(-1 <= index);
		assert(index < count_);

		for (int i = 0; i < MaxBufCount; i++)
		{
			if (array_[i].index == index)
			{
				return &array_[i];
			}
		}

		assert(!"CBufferQueue internal error");
		return null;
	}

	bool QueueIsFull()
	{
		return (count_ == MaxBufCount);
	}

	Buffer	array_[MaxBufCount];
	CMutex	mutex_;
	int		count_;		// buffer count in queue
};

class ESGUpdateEngine
{
public:
	ESGUpdateEngine()
	{
		strcpy(filename, "/data/misc/cmmb/cmmb_esginfo.dat");
		freqindex=0;
	}
	
	void InitESGUpdateInfo()
	{
		CAutoLock lock(mutex_);	
		file_ = fopen(filename, "rb");
		UINT32 len=0;
		if(file_!=null)
		{
			fseek(file_,0L,SEEK_END);
			len=ftell(file_);
			if(len==0)//the first time lanch the ESG mode, or the esf info file has been deleted
			{			
				memset(ESGUpdateInfo,0xff,sizeof(ESGUpdateInfo));	
				SP_LOGE("InitESGUpdateInfo()");
			}	
			else
			{
				rewind(file_);
				for(i=0;i<MAX_FREQUENCY_NUM;i++)
				{
					fread(&ESGUpdateInfo[i].freq,1,4,file_);
					fread(&ESGUpdateInfo[i].usn,1,1,file_);
					SP_LOGE("ESGUpdateInfo[%d].freq=%d,ESGUpdateInfo[%d].usn=%d",i,ESGUpdateInfo[i].freq,i,ESGUpdateInfo[i].usn);
				}
			}			
			fclose(file_);
			file_ = null;	
		}
		
	}

	UINT8 getusn(UINT32 freq)
	{
		CAutoLock lock(mutex_);	
		for(i=0;i<MAX_FREQUENCY_NUM;i++)
		{	
			if(freq==ESGUpdateInfo[i].freq)
				break;
		}
		if(i==MAX_FREQUENCY_NUM)
		{	
			SP_LOGE("freq=%d, usn=0xff",freq);
			return 0xff;
		}
		else
		{
			SP_LOGE("freq=%d, usn=%d",freq,ESGUpdateInfo[i].usn);
			return ESGUpdateInfo[i].usn;			
		}
	}

	void updateusn(UINT32 freq, UINT8 usn)
	{		
		CAutoLock lock(mutex_);	
		for(i=0;i<MAX_FREQUENCY_NUM;i++)
		{	
			if(freq==ESGUpdateInfo[i].freq)
				break;
		}
		if(i==MAX_FREQUENCY_NUM)//no find freq,set the info first time
		{
			ESGUpdateInfo[freqindex].freq = freq;
			ESGUpdateInfo[freqindex].usn = usn;
			SP_LOGE("ESGUpdateInfo[%d].freq=%d,ESGUpdateInfo[%d].usn=%d",freqindex,ESGUpdateInfo[freqindex].freq,freqindex,ESGUpdateInfo[freqindex].usn);
			freqindex++;			
		}
		else
		{
			ESGUpdateInfo[i].usn=usn;
			SP_LOGE("freq=%d, usn=%d",freq,ESGUpdateInfo[i].usn);
		}
	}	

	void FlushESGUpdateInfo()
	{
		CAutoLock lock(mutex_);	
		file_ = fopen(filename, "wb");	
		if (file_ != null)
		{
			for(i=0;i<MAX_FREQUENCY_NUM;i++)
			{
				fwrite(&ESGUpdateInfo[i].freq,1,4,file_);
				fwrite(&ESGUpdateInfo[i].usn,1,1,file_);
				SP_LOGE("ESGUpdateInfo[%d].freq=%d,ESGUpdateInfo[%d].usn=%d",i,ESGUpdateInfo[i].freq,i,ESGUpdateInfo[i].usn);
			}
			fclose(file_);
			file_ = null;			
		}
	}

private:
	FILE*	file_;
	char        filename[80];
	int           i;
	UINT8 	freqindex;
	CMutex	mutex_;
	esg_update_info ESGUpdateInfo[MAX_FREQUENCY_NUM];	
};


#ifdef CMMB_TRACE_MFS

UINT32 WriterThread(UINT32 param);

extern void * g_hWriter;

class CmmbFileWriter
{
public:
	CmmbFileWriter()
		: file_(null) 
		, num_(0)
	{
		strcpy(name_, "/data/misc/cmmb/cmmb_test%d.mfs");
	}

	~CmmbFileWriter()
	{
		close();

		if (g_hWriter != null)
		{
			OSAL_TaskCleanup(g_hWriter);
			g_hWriter = null;
		}
	}

	void open()
	{
		close();
		char name[260];
		sprintf(name, "/data/misc/cmmb/cmmb_test%d.mfs", ++num_);
	

		file_ = fopen(name, "wb");
	}

	void write(UINT8* buffer, UINT32 bufSize)
	{
		CAutoLock lock(mutex_);
		buffer_ = buffer;
		bufSize_ = bufSize;
		write_i();		
		//event_.Set();
	}

	void close()
	{
		if (file_ != null)
		{
			fclose(file_);
			file_ = null;
			name_[0] = 0;
		}
	}
	
	void write_i()
	{
		if (file_ != null)
		{
			CAutoLock lock(mutex_);
			fwrite(buffer_, 1, bufSize_, file_);
		}
	}

	int fileSize()
	{
		if (file_ != null)
		{
			return ftell(file_);
		}

		return 0;
	}

private:
	friend UINT32 WriterThread(UINT32 param);
	FILE*	file_;
	char	name_[260];
	int		num_;
	UINT8*	buffer_;
	UINT32	bufSize_;
	CEvent	event_;
	CMutex	mutex_;
};

const int MAX_MFS_FILE_SIZE = 50000000;

extern CmmbFileWriter mfsWriter;
#endif // CMMB_TRACE_MFS


/**************************************************
				function
***************************************************/
typedef int (*CMMB_AUTOSCAN_CALLBACK)(int freq);

CmmbResult CmmbFtAutoScan();

void SetResponseEvent();

void UserDataCallback_Innofidei(UINT32 serviceHdl, UINT8* buffer, UINT32 bufSize);

void Siano_UserDataCallback(UINT32 serviceHdl, UINT8* buffer, UINT32 bufSize);

void PostEventMessage(UINT32 msgId, UINT8* payload, UINT32 payloadLen);

UINT32 GetCmmbSystemTime();

void StopAllEsgServices();

CmmbResult ParseTs0();

CmmbResult ParseESG();

CmmbResult EsgParserStart();

CmmbResult EsgParserStop();

int FindService(UINT32 serviceHdl);

const CmmbService& GetServiceInfo(int index);

CmmbResult StartEsgService(UINT32 serviceId);

bool IsStreamService(int index);

void CopyMpxFrame(UINT8* buffer, UINT32 bufLen);

void SendUamErrorEvent(UINT32 errMsg);

CmmbResult GetErrorCode();

void SetErrorCode(CmmbResult errCode);

#define zero_struct(item) memset(&(item), 0, sizeof(item))

#endif // CMMB_HELPER_H

