# Copyright (C) 2009 The Android Open Source Project
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

"""Emit commands needed for Motorola devices during OTA installation
(installing the MBM, CDT, LBL and BP images)."""

import common

def FullOTA_InstallEnd(info):
  try:
    WriteBPUpdate(info,info.input_zip.read("RADIO/rdl.bin"),
                info.input_zip.read("RADIO/bp.img"))
  except KeyError:
    print ("warning: rdl.bin and/or bp.img not in input target_files; "
           "skipping BP update")
  if info.extras.get("mbm", None) == "consumer":
    try:
      info.input_zip.getinfo("RADIO/mbm_consumer.bin")
      Write2FullOTAPackage(info, "mbm", "mbm_consumer.bin")
    except KeyError, e:
      print ("warning: mbm_consumer.bin not in input target_files; "
             "skipping MBM update")
  else:
    Write2FullOTAPackage(info, "mbm", "mbm.bin")
  Write2FullOTAPackage(info, "lbl", "lbl")
  Write2FullOTAPackage(info, "cdt", "cdt.bin")

def IncrementalOTA_InstallEnd(info):
  WriteBP2IncrementalPackage(info)
  if info.extras.get("mbm", None) == "consumer":
    try:
      info.target_zip.getinfo("RADIO/mbm_consumer.bin")
      Write2IncrementalPackage(info, "mbm", "mbm_consumer.bin")
    except KeyError, e:
      print ("warning: mbm_consumer.bin not in input target_files; "
             "skipping MBM update")
  else:
    Write2IncrementalPackage(info, "mbm", "mbm.bin")
  Write2IncrementalPackage(info, "lbl", "lbl")
  Write2IncrementalPackage(info, "cdt", "cdt.bin")

#Append BP image and BP update agent(RDL, run in BP side) to package
def WriteBPUpdate(info, rdl_bin, bp_bin):
  common.ZipWriteStr(info.output_zip, "rdl.bin",
                     rdl_bin)
  common.ZipWriteStr(info.output_zip, "bp.img",
                     bp_bin)
  # this only works with edify; motorola devices never use amend.
  info.script.AppendExtra('''assert(package_extract_file("bp.img", "/tmp/bp.img"),
       package_extract_file("rdl.bin", "/tmp/rdl.bin"),
       moto.update_cdma_bp("/tmp/rdl.bin", "/tmp/bp.img"),
       delete("/tmp/bp.img", "/tmp/rdl.bin"));''')

#Append BP image and RDL to incremental package
def WriteBP2IncrementalPackage(info):
  try:
    target_rdl = info.target_zip.read("RADIO/rdl.bin")
    target_bp = info.target_zip.read("RADIO/bp.img")
    try:
      source_bp = info.source_zip.read("RADIO/bp.img")
      if source_bp == target_bp:
        print("BP images unchanged; skipping")
      else:
        print("BP image changed; including")
        info.script.Print("Writing RDL/BP image...")
        WriteBPUpdate(info,target_rdl,target_bp)
    except KeyError:
      print("warning: no rdl.bin and/or bp.img in source_files; just use target")
      info.script.Print("Writing RDL/BP image...")
      WriteBPUpdate(info,target_rdl,target_bp)
  except KeyError:
    print("warning: no rdl.bin and/or bp.img in target_files; not flashing")

#Append raw image update to package
def Write2FullOTAPackage(info, dev_name, bin_name):
  try:
    common.ZipWriteStr(info.output_zip, bin_name,
                       info.input_zip.read("RADIO/"+bin_name))
    info.script.WriteRawImage(dev_name, bin_name)
  except KeyError:
    print ("warning: no "+ bin_name +" in input target_files; not flashing")

#Append raw image update to incremental package
def Write2IncrementalPackage(info, dev_name, bin_name):
  try:
    file_name = "RADIO/" + bin_name;
    target = info.target_zip.read(file_name);
    try:
      source = info.source_zip.read(file_name);
      if source == target:
        print(dev_name + " image unchanged; skipping")
      else:
        print(dev_name + " image changed; including")
        common.ZipWriteStr(info.output_zip, bin_name, target)
        info.script.WriteRawImage(dev_name, bin_name)
    except KeyError:
      print("warning: no "+ bin_name +" in source_files; just use target")
      common.ZipWriteStr(info.output_zip, bin_name, target)
      info.script.WriteRawImage(dev_name, bin_name)
  except KeyError:
    print("warning: no "+ bin_name +" in target_files; not flashing")
