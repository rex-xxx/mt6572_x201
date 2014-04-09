package com.mediatek.phone.recording;

import com.mediatek.phone.recording.IPhoneRecordStateListener;

interface IPhoneRecorder {
    void listen(IPhoneRecordStateListener callback);
    void remove();
    void startRecord();
    void stopRecord(boolean isMount);
}
