#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_MTK_PATH:= mediatek/frameworks/base/voicecommand/cfg/

PRODUCT_COPY_FILES += \
       $(LOCAL_MTK_PATH)/1.xml:system/etc/voicecommand/keyword/1.xml \
       $(LOCAL_MTK_PATH)/2.xml:system/etc/voicecommand/keyword/2.xml \
       $(LOCAL_MTK_PATH)/3.xml:system/etc/voicecommand/keyword/3.xml \
       $(LOCAL_MTK_PATH)/64.dat:system/etc/voicecommand/training/ubmfile/64.dat \
       $(LOCAL_MTK_PATH)/128.dat:system/etc/voicecommand/training/ubmfile/128.dat \
       $(LOCAL_MTK_PATH)/voicepattern/Chinese-Mandarin/cmd_pattern:system/etc/voicecommand/voiceui/uipattern/Chinese-Mandarin/cmd_pattern \
       $(LOCAL_MTK_PATH)/voicepattern/Chinese-Taiwan/cmd_pattern:system/etc/voicecommand/voiceui/uipattern/Chinese-Taiwan/cmd_pattern \
       $(LOCAL_MTK_PATH)/voicepattern/English/cmd_pattern:system/etc/voicecommand/voiceui/uipattern/English/cmd_pattern \
       $(LOCAL_MTK_PATH)/modefile/command_3and23.dic:system/etc/voicecommand/voiceui/modefile/command_3and23.dic  \
       $(LOCAL_MTK_PATH)/modefile/command_6and23.dic:system/etc/voicecommand/voiceui/modefile/command_6and23.dic  \
       $(LOCAL_MTK_PATH)/modefile/commandfilr2.dic:system/etc/voicecommand/voiceui/modefile/commandfilr2.dic  \
       $(LOCAL_MTK_PATH)/modefile/commandfilr.dic:system/etc/voicecommand/voiceui/modefile/commandfilr.dic  \
       $(LOCAL_MTK_PATH)/modefile/command_only23.dic:system/etc/voicecommand/voiceui/modefile/command_only23.dic  \
       $(LOCAL_MTK_PATH)/modefile/GMMModel1.bin:system/etc/voicecommand/voiceui/modefile/GMMModel1.bin  \
       $(LOCAL_MTK_PATH)/modefile/GMMModel2.bin:system/etc/voicecommand/voiceui/modefile/GMMModel2.bin  \
       $(LOCAL_MTK_PATH)/modefile/GMMModel3.bin:system/etc/voicecommand/voiceui/modefile/GMMModel3.bin  \
       $(LOCAL_MTK_PATH)/modefile/Model1.bin:system/etc/voicecommand/voiceui/modefile/Model1.bin  \
       $(LOCAL_MTK_PATH)/modefile/Model2.bin:system/etc/voicecommand/voiceui/modefile/Model2.bin  \
       $(LOCAL_MTK_PATH)/modefile/Model3.bin:system/etc/voicecommand/voiceui/modefile/Model3.bin  \
       $(LOCAL_MTK_PATH)/modefile/Model_M_gmmfea39d.dat:system/etc/voicecommand/voiceui/modefile/Model_M_gmmfea39d.dat
