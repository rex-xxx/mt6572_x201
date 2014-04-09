#!/bin/sh

# Copyright (C) 2011 The Android Open Source Project
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

# 221846 = IML40B
# 236517 = IML70C
# 237179 = IML73
# 237867 = IML74B
# 238432 = IML74E
# 238649 = IML74G
# 239410 = IML74K
# 257829 = IMM30B
# 262866 = IMM30D
# 299849 = IMM76D
# end ics-mr1
BRANCH=ics-mr1
if test $BRANCH=ics-mr1
then
  ZIP=tervigon-ota-299849.zip
  BUILD=imm76d
fi # ics-mr1
ROOTDEVICE=stingray
DEVICE=wingray
MANUFACTURER=moto

for COMPANY in nvidia broadcom
do
  echo Processing files from $COMPANY
  rm -rf tmp
  FILEDIR=tmp/vendor/$COMPANY/$DEVICE/proprietary
  mkdir -p $FILEDIR
  mkdir -p tmp/vendor/$MANUFACTURER/$ROOTDEVICE
  case $COMPANY in
  broadcom)
    TO_EXTRACT="\
            system/etc/firmware/bcm4329.hcd \
            system/etc/wifi/bcm4329.cal \
            system/vendor/firmware/fw_bcm4329_mfg.bin
            "
    ;;
  nvidia)
    TO_EXTRACT="\
            system/etc/firmware/nvmm_aacdec.axf \
            system/etc/firmware/nvddk_audiofx_core.axf \
            system/etc/firmware/nvddk_audiofx_transport.axf \
            system/etc/firmware/nvmm_adtsdec.axf \
            system/etc/firmware/nvmm_audiomixer.axf \
            system/etc/firmware/nvmm_h264dec.axf \
            system/etc/firmware/nvmm_jpegdec.axf \
            system/etc/firmware/nvmm_jpegenc.axf \
            system/etc/firmware/nvmm_manager.axf \
            system/etc/firmware/nvmm_mp2dec.axf \
            system/etc/firmware/nvmm_mp3dec.axf \
            system/etc/firmware/nvmm_mpeg4dec.axf \
            system/etc/firmware/nvmm_reference.axf \
            system/etc/firmware/nvmm_service.axf \
            system/etc/firmware/nvmm_sorensondec.axf \
            system/etc/firmware/nvmm_sw_mp3dec.axf \
            system/etc/firmware/nvmm_wavdec.axf \
            system/etc/firmware/nvrm_avp.bin \
            system/lib/egl/libEGL_tegra.so \
            system/lib/egl/libGLESv1_CM_tegra.so \
            system/lib/egl/libGLESv2_tegra.so \
            system/lib/hw/gralloc.tegra.so \
            system/lib/hw/hwcomposer.tegra.so \
            system/lib/libcgdrv.so \
            system/lib/libnvddk_2d.so \
            system/lib/libnvddk_2d_v2.so \
            system/lib/libnvddk_audiofx.so \
            system/lib/libnvdispatch_helper.so \
            system/lib/libnvdispmgr_d.so \
            system/lib/libnvmm.so \
            system/lib/libnvmm_camera.so \
            system/lib/libnvmm_contentpipe.so \
            system/lib/libnvmm_image.so \
            system/lib/libnvmm_manager.so \
            system/lib/libnvmm_service.so \
            system/lib/libnvmm_tracklist.so \
            system/lib/libnvmm_utils.so \
            system/lib/libnvmm_video.so \
            system/lib/libnvomx.so \
            system/lib/libnvomxilclient.so \
            system/lib/libnvos.so \
            system/lib/libnvrm.so \
            system/lib/libnvrm_channel.so \
            system/lib/libnvrm_graphics.so \
            system/lib/libnvsm.so \
            system/lib/libnvwsi.so \
            system/lib/libstagefrighthw.so
            "
    ;;
  esac
  echo \ \ Extracting files from OTA package
  for ONE_FILE in $TO_EXTRACT
  do
    echo \ \ \ \ Extracting $ONE_FILE
    unzip -j -o $ZIP $ONE_FILE -d $FILEDIR > /dev/null || echo \ \ \ \ Error extracting $ONE_FILE
    if test $ONE_FILE = system/vendor/bin/gpsd -o $ONE_FILE = system/vendor/bin/pvrsrvinit
    then
      chmod a+x $FILEDIR/$(basename $ONE_FILE) || echo \ \ \ \ Error chmoding $ONE_FILE
    fi
    if test $(echo $ONE_FILE | grep \\.apk\$ | wc -l) = 1
    then
      echo \ \ \ \ Splitting $ONE_FILE
      mkdir -p $FILEDIR/$(basename $ONE_FILE).parts || echo \ \ \ \ Error making parts dir for $ONE_FILE
      unzip $FILEDIR/$(basename $ONE_FILE) -d $FILEDIR/$(basename $ONE_FILE).parts > /dev/null || echo \ \ \ \ Error unzipping $ONE_FILE
      rm $FILEDIR/$(basename $ONE_FILE) || echo \ \ \ \ Error removing original $ONE_FILE
      rm -rf $FILEDIR/$(basename $ONE_FILE).parts/META-INF || echo \ \ \ \ Error removing META-INF for $ONE_FILE
    fi
  done
  echo \ \ Setting up $COMPANY-specific makefiles
  cp -R $COMPANY/staging/* tmp/vendor/$COMPANY/$DEVICE || echo \ \ \ \ Error copying makefiles
  echo \ \ Setting up shared makefiles
  cp -R root/* tmp/vendor/$MANUFACTURER/$ROOTDEVICE || echo \ \ \ \ Error copying makefiles
  echo \ \ Generating self-extracting script
  SCRIPT=extract-$COMPANY-$DEVICE.sh
  cat PROLOGUE > tmp/$SCRIPT || echo \ \ \ \ Error generating script
  cat $COMPANY/COPYRIGHT >> tmp/$SCRIPT || echo \ \ \ \ Error generating script
  cat PART1 >> tmp/$SCRIPT || echo \ \ \ \ Error generating script
  cat $COMPANY/LICENSE >> tmp/$SCRIPT || echo \ \ \ \ Error generating script
  cat PART2 >> tmp/$SCRIPT || echo \ \ \ \ Error generating script
  echo tail -n +$(expr 2 + $(cat PROLOGUE $COMPANY/COPYRIGHT PART1 $COMPANY/LICENSE PART2 PART3 | wc -l)) \$0 \| tar zxv >> tmp/$SCRIPT || echo \ \ \ \ Error generating script
  cat PART3 >> tmp/$SCRIPT || echo \ \ \ \ Error generating script
  (cd tmp ; tar zc --owner=root --group=root vendor/ >> $SCRIPT || echo \ \ \ \ Error generating embedded tgz)
  chmod a+x tmp/$SCRIPT || echo \ \ \ \ Error generating script
  ARCHIVE=$COMPANY-$DEVICE-$BUILD-$(md5sum < tmp/$SCRIPT | cut -b -8 | tr -d \\n).tgz
  rm -f $ARCHIVE
  echo \ \ Generating final archive
  (cd tmp ; tar --owner=root --group=root -z -c -f ../$ARCHIVE $SCRIPT || echo \ \ \ \ Error archiving script)
  rm -rf tmp
done
