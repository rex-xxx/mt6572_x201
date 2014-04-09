

#define LOG_TAG "UIBCMessage"
#include <utils/Log.h>

#include "UibcMessage.h"
#include "WifiDisplayUibcType.h"

#include <media/IRemoteDisplayClient.h>

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/foundation/hexdump.h>


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <netinet/in.h>

namespace android {

static const char *UIBC_INPUT_DEVICE_KPD = "/dev/input/event5";
static const char *UIBC_INPUT_DEVICE_TPD = "/dev/input/event6";

#define UIBC_KEYCODE_UNKNOWN KEY_UNKNOWN

#define BTN_TOUCH   0x14a

#define WFD_UIBC_HIDC_USAGE_REPORT_INPUT        0x00
#define WFD_UIBC_HIDC_USAGE_REPORT_DESCRIPTOR   0x01

#define HID_LEFTSHIFT_BIT                       0x02
#define HID_RIGHTSHIFT_BIT                      0x20


static ASCII_KEYCODE_MAPPING_STRUCT s_keyCodeMapping[] = {
    {0x30, KEY_0}, // '0'
    {0x31, KEY_1},
    {0x32, KEY_2},
    {0x33, KEY_3},
    {0x34, KEY_4},
    {0x35, KEY_5},
    {0x36, KEY_6},
    {0x37, KEY_7},
    {0x38, KEY_8},
    {0x39, KEY_9},
    {0x41, KEY_A}, // 'A'
    {0x61, KEY_A}, // 'a'
    {0x42, KEY_B},
    {0x62, KEY_B},
    {0x43, KEY_C},
    {0x63, KEY_C},
    {0x44, KEY_D},
    {0x64, KEY_D},
    {0x45, KEY_E},
    {0x65, KEY_E},
    {0x46, KEY_F},
    {0x66, KEY_F},
    {0x47, KEY_G},
    {0x67, KEY_G},
    {0x48, KEY_H},
    {0x68, KEY_H},
    {0x49, KEY_I},
    {0x69, KEY_I},
    {0x4a, KEY_J},
    {0x6a, KEY_J},
    {0x4b, KEY_K},
    {0x6b, KEY_K},
    {0x4c, KEY_L},
    {0x6c, KEY_L},
    {0x4d, KEY_M},
    {0x6d, KEY_M},
    {0x4e, KEY_N},
    {0x6e, KEY_N},
    {0x4f, KEY_O},
    {0x6f, KEY_O},
    {0x50, KEY_P},
    {0x70, KEY_P},
    {0x51, KEY_Q},
    {0x71, KEY_Q},
    {0x52, KEY_R},
    {0x72, KEY_R},
    {0x53, KEY_S},
    {0x73, KEY_S},
    {0x54, KEY_T},
    {0x74, KEY_T},
    {0x55, KEY_U},
    {0x75, KEY_U},
    {0x56, KEY_V},
    {0x76, KEY_V},
    {0x57, KEY_W},
    {0x77, KEY_W},
    {0x58, KEY_X},
    {0x78, KEY_X},
    {0x59, KEY_Y},
    {0x79, KEY_Y},
    {0x5a, KEY_Z},
    {0x7a, KEY_Z},
    {0x08, KEY_BACKSPACE},
    {0x09, KEY_TAB},
    {0x0d, KEY_ENTER},    
    {0x12, KEY_KBDILLUMDOWN}, // MENU    
    {0x1b, KEY_BACK}, // (Escape ->) BACK
    {0x20, KEY_SPACE}, //Space
    {0x23, KEY_END}, // END
    {0x24, KEY_HOME}, // HOME     
    {0x2d, KEY_SEND}, // (Insert ->) CALL    
    {0x13, KEY_VOLUMEUP},
    {0x14, KEY_VOLUMEDOWN},
};

static const int s_keyCodeMappingNum = sizeof(s_keyCodeMapping) / sizeof(s_keyCodeMapping[0]);

UibcMessage::UibcMessage(const sp<IRemoteDisplayClient> &client)
    : mKeyFd(-1),
    mTpdFd(-1),
    mClient(client){
}

UibcMessage::~UibcMessage() {
    
}

status_t UibcMessage::init() {
    int version;
           
    return OK;
}

status_t UibcMessage::destroy() {

    if(mKeyFd >= 0){
        close(mKeyFd);
        mKeyFd = -1;
    }

    if(mTpdFd >= 0){
        close(mTpdFd);
        mTpdFd = -1;
    }
    
    return OK;
}

status_t UibcMessage::handleUIBCMessage(const sp<ABuffer> &buffer) {
    size_t size = buffer->size();
    size_t payloadOffset = 0;
    UIBCInputCategoryCode  inputCategory = WFD_UIBC_INPUT_CATEGORY_UNKNOWN;

    if(size < UIBC_HEADER_SIZE){
       ALOGE("The size of UIBC message is less than header size");
       return ERROR_MALFORMED;
    }
    
    const uint8_t *data = buffer->data();

#if 1
            ALOGD("in with");
            hexdump(data, size);
#endif


    int testCmd = data[0];
    ALOGD("testCmd:%d", testCmd);
    
    if(testCmd >= 0x80){  //Enter test command                
        switch(testCmd){
            case 0x80:
                {
                    for(int j = 0; j < 1; j++){
                        sendKeyEvent(30+j, 1);
                        //sendKeyEvent(30+j, 0);
                    }
                    break;
                }
            case 0x81:
                {
                    int testCode = data[1];
                    sendKeyEvent(testCode, 1);
                    sendKeyEvent(testCode, 0);
                    break;
                }
            case 0x82:
                {
                    sendTouchEvent(320,320, WFD_UIBC_GENERIC_IE_ID_LMOUSE_TOUCH_DOWN);
                    sendTouchEvent(340,320, WFD_UIBC_GENERIC_IE_ID_MOUSE_TOUCH_MOVE);
                    sendTouchEvent(360,320, WFD_UIBC_GENERIC_IE_ID_MOUSE_TOUCH_MOVE);
                    sendTouchEvent(600,320, WFD_UIBC_GENERIC_IE_ID_LMOUSE_TOUCH_UP);
                }
                break;
            }

        return OK;
    }
    
    //Check UIBC version
#if 0    
    if ((data[0] >> 6) != 0) {
        // Unsupported version.
        return ERROR_UNSUPPORTED;
    }
#endif

    //Skip the timestamp
    bool hasTimeStamp = data[0] & UIBC_TIMESTAMP_MASK;
    if(hasTimeStamp){
        payloadOffset = UIBC_HEADER_SIZE + UIBC_TIMESTAMP_SIZE;
    }else{
        payloadOffset = UIBC_HEADER_SIZE;
    }

    if (size < payloadOffset) {
        // Not enough data to fit the basic header
        ALOGE("Not enough data to fit the basic header");
        return ERROR_MALFORMED;
    }

    buffer->setRange(payloadOffset, size - payloadOffset);
    
    inputCategory = (UIBCInputCategoryCode) (data[1] & UIBC_INPUT_CATEGORY_MASK);
    
    switch(inputCategory){
        case WFD_UIBC_INPUT_CATEGORY_GENERIC:
            handleGenericInput(buffer); 
            break;
        case WFD_UIBC_INPUT_CATEGORY_HIDC:
            handleHIDCInput(buffer);
            break;
        default:
            ALOGE("Uknown input category:%d", inputCategory);
            break;
    }

    return OK;
}

status_t UibcMessage::handleGenericInput(const sp<ABuffer> &buffer){
    size_t size = buffer->size();
    WFD_UIBC_GENERIC_BODY_FORMAT_HDR *pHdr = (WFD_UIBC_GENERIC_BODY_FORMAT_HDR*) buffer->data();
    UINT16 bodyLength = ntohs(pHdr->length);
    
    if (size < bodyLength) {
        ALOGE("Error: not enough space for a complete generic body:%d", bodyLength);
        return ERROR;
    }
    
    ALOGV("handleGenericInput with IE:%d", pHdr->ieID);
    
    switch (pHdr->ieID) {
        case WFD_UIBC_GENERIC_IE_ID_LMOUSE_TOUCH_DOWN:
        case WFD_UIBC_GENERIC_IE_ID_LMOUSE_TOUCH_UP:
        case WFD_UIBC_GENERIC_IE_ID_MOUSE_TOUCH_MOVE:
        {
            WFD_UIBC_GENERIC_BODY_MOUSE_TOUCH *pBody = (WFD_UIBC_GENERIC_BODY_MOUSE_TOUCH*)pHdr;
            ALOGI("(Move,Down,Up) ieID: %d, numptr: %d",
                            pBody->ieID,
                            pBody->numPointers);
            for(int i = 0; i < pBody->numPointers && i < MAX_NUM_COORDINATE; i++){
                ALOGD("%dth: pointerID:%d x:%d y:%d", i, pBody->coordinates[i].pointerID, 
                    ntohs(pBody->coordinates[i].x), ntohs(pBody->coordinates[i].y));
                sendTouchEvent(ntohs(pBody->coordinates[i].x), ntohs(pBody->coordinates[i].y), pHdr->ieID);
            }
            break;
        }
        case WFD_UIBC_GENERIC_IE_ID_KEY_DOWN:
        case WFD_UIBC_GENERIC_IE_ID_KEY_UP:
        {
            if((sizeof(WFD_UIBC_GENERIC_BODY_FORMAT_HDR) + bodyLength)
                            == sizeof(WFD_UIBC_GENERIC_BODY_KEY)){
                    WFD_UIBC_GENERIC_BODY_KEY *pBody = (WFD_UIBC_GENERIC_BODY_KEY*)pHdr;
                    ALOGD("(Key,Down,Up) ieID: %d, code1: %d, code2: %d",
                                    pBody->ieID,
                                    ntohs(pBody->code1),
                                    ntohs(pBody->code2));
                    int isDown =   ( (pHdr->ieID == WFD_UIBC_GENERIC_IE_ID_KEY_DOWN) ? 1 : 0  );
                    if( ntohs(pBody->code1) > 0){
                        sendKeyEvent(ntohs(pBody->code1), isDown);
                    }

                    if(ntohs(pBody->code2) > 0){
                        sendKeyEvent(ntohs(pBody->code2), isDown);
                    }
            }
            break;
        }
        case WFD_UIBC_GENERIC_IE_ID_ZOOM:
            if((sizeof(WFD_UIBC_GENERIC_BODY_FORMAT_HDR) + bodyLength)
                        == sizeof(WFD_UIBC_GENERIC_BODY_ZOOM)){
                WFD_UIBC_GENERIC_BODY_ZOOM *pBody = (WFD_UIBC_GENERIC_BODY_ZOOM*)pHdr;
                ALOGD("(ZOOM) ieID: %d, x: %d, y: %d, itimes: %d, ftimes: %d",
                                pBody->ieID,
                                pBody->x,
                                pBody->y,
                                pBody->intTimes,
                                pBody->fractTimes);
            }
            break;
        case WFD_UIBC_GENERIC_IE_ID_VSCROLL:
        case WFD_UIBC_GENERIC_IE_ID_HSCROLL:
            if((sizeof(WFD_UIBC_GENERIC_BODY_FORMAT_HDR) + bodyLength)
                        == sizeof(WFD_UIBC_GENERIC_BODY_SCROLL)){
                WFD_UIBC_GENERIC_BODY_SCROLL *pBody = (WFD_UIBC_GENERIC_BODY_SCROLL*)pHdr;
                ALOGD("(SCROLL V/H) ieID: %d, amount: %d",
                                pBody->ieID,
                                pBody->amount);
            }
            break;
        case WFD_UIBC_GENERIC_IE_ID_ROTATE:
            if((sizeof(WFD_UIBC_GENERIC_BODY_FORMAT_HDR) + bodyLength)
                        == sizeof(WFD_UIBC_GENERIC_BODY_ROTATE)){
                WFD_UIBC_GENERIC_BODY_ROTATE *pBody = (WFD_UIBC_GENERIC_BODY_ROTATE*)pHdr;
                ALOGD("(ROTATE V/H) ieID: %d, iamount: %d, famount: %d",
                                pBody->ieID,
                                pBody->intAmount,
                                pBody->fractAmount);
                break;                                
            }
        default:
            ALOGE("Unknown User input for generic type");
            break;        
    }
    
    return OK;
}

status_t UibcMessage::handleHIDCInput(const sp<ABuffer> &buffer){
    size_t bufferSize = buffer->size();
    size_t payloadOffset = 0;
    
    if (bufferSize < sizeof(WFD_UIBC_HIDC_BODY_FORMAT_HDR)) {
        ALOGE("Error: not enough space for a complete HIDC header:%d", bufferSize);
        return ERROR;
    }

    WFD_UIBC_HIDC_BODY_FORMAT_HDR *pHdr = (WFD_UIBC_HIDC_BODY_FORMAT_HDR*) buffer->data();
    UINT16 bodyLength = ntohs(pHdr->length);

    if (bufferSize < bodyLength + sizeof(WFD_UIBC_HIDC_BODY_FORMAT_HDR)) {
        ALOGE("Error: not enough space for a complete HIDC body:%d", bufferSize);
        return ERROR;
    }
    
    ALOGI("handleHIDCInput with Info:(%d:%d:%d)", pHdr->inputPath, pHdr->hidType, pHdr->usage);

    payloadOffset = sizeof(WFD_UIBC_HIDC_BODY_FORMAT_HDR);
    buffer->setRange(payloadOffset, bufferSize - payloadOffset); // Move the buffer to the HIDC body part
    uint8_t* pBuffer = buffer->data();

    switch (pHdr->usage) {
            case WFD_UIBC_HIDC_USAGE_REPORT_INPUT:
                {
                    if (pHdr->inputPath == WFD_UIBC_HIDC_IE_ID_USB){
                        switch(pHdr->hidType){
                            case WFD_UIBC_HIDCTYPE_IE_ID_KEYBOARD:
                                if(bodyLength == 8){
                                    if(pBuffer[2] != 0) {
                                        //Check the shift key
                                        if((pBuffer[0] & HID_LEFTSHIFT_BIT) || (pBuffer[0] & HID_RIGHTSHIFT_BIT)){
                                            //sendKeyEvent(AKEYCODE_SHIFT_LEFT, 1);
                                            //sendKeyEvent(AKEYCODE_SHIFT_LEFT, 0);
                                        }
                                        sendKeyEvent(pBuffer[2], 1);
                                    }else{
                                        sendKeyEvent(pBuffer[2], 0);
                                    }
                                }
                                break;
                            case WFD_UIBC_HIDCTYPE_IE_ID_MOUSE:
                                if(bodyLength >= 3) {
                                    int xOffs, yOffs;
                                    xOffs = (char) pBuffer[1];
                                    yOffs = (char) pBuffer[2];
                                    if (xOffs >= 0x80) xOffs -= 0x80;
                                    if (yOffs >= 0x80) yOffs -= 0x80;
                                    sendTouchEvent(xOffs, yOffs, WFD_UIBC_GENERIC_IE_ID_LMOUSE_TOUCH_DOWN);
                                    sendTouchEvent(xOffs, yOffs, WFD_UIBC_GENERIC_IE_ID_MOUSE_TOUCH_MOVE);
                                    sendTouchEvent(xOffs, yOffs, WFD_UIBC_GENERIC_IE_ID_LMOUSE_TOUCH_UP);
                                }
                                break;
                        }
                    }                    
                    break;
                }
            case WFD_UIBC_HIDC_USAGE_REPORT_DESCRIPTOR:
                {
                    
                    break;
                }
    }
        
    return OK;
}

short UibcMessage::mapKeyCode(UINT16 uibcCode) {
    short ret = UIBC_KEYCODE_UNKNOWN;

    ALOGI("uibcCode: %d", uibcCode);

    
    for (unsigned int i = 0; i < (sizeof(s_keyCodeMapping) / sizeof(s_keyCodeMapping[0])); i++) {
        if (uibcCode == s_keyCodeMapping[i].asciiCode) {
            ret = s_keyCodeMapping[i].keyCode;
            break;
        }
    }

    ALOGI("mapKeyCode: %d", ret);
    return ret;
}

status_t UibcMessage::sendEvent(int fd, struct input_event* event){

        
#if 0    
    int ret = 0;
    int size = (int) sizeof(*event);
        
    ret = write(mKeyFd, event, size);
    if(ret < size) {
        ALOGE("write event failed, %s\n", strerror(errno));
        return -1;
    }
#endif    

    return OK;
}

status_t UibcMessage::sendKeyEvent(UINT16 code, int isDown){

    if(mClient != NULL){
        ALOGD("sendKeyEvent:%d/%d", code, isDown);
        mClient->onDisplayKeyEvent(code, isDown);
    }

#if 0    
    struct input_event keyEvent;
    int ret = 0;
    
    keyEvent.type = EV_KEY;
    keyEvent.value = (isDown == true) ? 1 : 0;

    //Find the key code
    ret = mapKeyCode(code);
    if(ret == UIBC_KEYCODE_UNKNOWN){
        ALOGE("Can't find key code");
        return -1;
    }
    keyEvent.code = ret;
    sendEvent(mKeyFd, &keyEvent);

    keyEvent.type = EV_SYN;
    keyEvent.code = 0;
    keyEvent.value = 0;
    sendEvent(mKeyFd, &keyEvent);
#endif

    return OK;
}

status_t UibcMessage::sendTouchEvent(UINT16 x, UINT16 y, UINT16 type){

    if(mClient != NULL){
        mClient->onDisplayTouchEvent(x, y, type);
    }


#if 0    
    struct input_event tpdEvent;
    int ret = 0;
    
    switch(type){
        case WFD_UIBC_GENERIC_IE_ID_LMOUSE_TOUCH_DOWN:
            {
                tpdEvent.type = EV_KEY;
                tpdEvent.code = BTN_TOUCH;
                tpdEvent.value = 1;             //1:Down 0: Up
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x30;
                tpdEvent.value = 0x14;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x35;
                tpdEvent.value = x;
                sendEvent(mTpdFd, &tpdEvent);
                
                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x36;
                tpdEvent.value = y;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x39;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_SYN;
                tpdEvent.code = 0x02;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_SYN;
                tpdEvent.code = 0x00;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);                
                break;
            }
        case WFD_UIBC_GENERIC_IE_ID_LMOUSE_TOUCH_UP:
            {
                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x30;
                tpdEvent.value = 0x14;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x35;
                tpdEvent.value = x;
                sendEvent(mTpdFd, &tpdEvent);
                
                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x36;
                tpdEvent.value = y;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x39;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_SYN;
                tpdEvent.code = 0x02;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_SYN;
                tpdEvent.code = 0x00;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_KEY;
                tpdEvent.code = BTN_TOUCH;
                tpdEvent.value = 0;             //1:Down 0: Up
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_SYN;
                tpdEvent.code = 0x02;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_SYN;
                tpdEvent.code = 0x00;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);                
                break;
            }            
        case WFD_UIBC_GENERIC_IE_ID_MOUSE_TOUCH_MOVE:
            {
                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x30;
                tpdEvent.value = 0x14;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x35;
                tpdEvent.value = x;
                sendEvent(mTpdFd, &tpdEvent);
                
                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x36;
                tpdEvent.value = y;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_ABS;
                tpdEvent.code = 0x39;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_SYN;
                tpdEvent.code = 0x02;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);

                tpdEvent.type = EV_SYN;
                tpdEvent.code = 0x00;
                tpdEvent.value = 0;
                sendEvent(mTpdFd, &tpdEvent);                
                break;
            }
        default:
            ALOGE("Uknown type for mouse/touch event");
            break;
    } 
#endif
    
    return OK;
}

}
