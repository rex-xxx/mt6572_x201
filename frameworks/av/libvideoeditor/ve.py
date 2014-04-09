#!/usr/bin/env python
#
# Written by demon.deng@mediatek.com
# Any problems, please ocs/email me, thanks

import sys
import os
import platform
import time
import xml.dom.minidom
import hashlib
import subprocess

version = sys.version_info[0]
VERSION2 = version == 2
VIDEO_EDITOR_LEAK_DIRECTORY = "/sdcard/videoeditor/leak"

def detectSDCARD():
    cmd = "adb shell getprop persist.sys.sd.defaultpath"
    fs = os.popen(cmd).readlines()
    p = "/mnt/sdcard"
    if fs[0].endswith("\r\n"):
        p = fs[0][:-2]
    else:
        p = fs[0][:-1]
    if p.find("sdcard") < 0:
        p = "/mnt/sdcard"
    print( "sdcard path = " + p)
    return p

win = platform.system() == "Windows"
if win == True:
    ADBDUMMY = " 1>NUL 2>&1"
else:
    ADBDUMMY = " 1>/dev/null 2>&1"
CODING = sys.stdout.encoding
    
APName = "com.android.videoeditor"
SDCARD = detectSDCARD()
APDataPath = SDCARD + "/Android/data/" + APName
WORKSPACE = time.strftime("%Y%m%d%H%M%S", time.localtime())
EXTERNALS = "externals"

def generateName(filename):
    basename = os.path.basename(filename)
    dirname = os.path.dirname(filename)
    m = hashlib.md5()
    m.update(dirname.encode("utf-8"))
    name = m.hexdigest() + "_" + basename
    return name

def processItem(item, exPath):
    prefix = "... "
    filename = item.getAttribute("filename")
    if filename.find(APName + "/" + EXTERNALS) >= 0:
        bName = os.path.basename(filename)
    elif filename.find(APName) >= 0:
        print( prefix + "skip internal file: " + filename)
        return
    else:
        bName = generateName(filename)
    exFile = exPath + "/" + bName
    if (os.path.exists(exFile)):
        print( prefix + "skip pulled external file: " + filename)
        if filename != bName:
            item.setAttribute("filename", APDataPath + "/" + EXTERNALS + "/" + bName)
        return
    
    cmd = "adb pull %s %s" % (filename.encode("utf-8").decode(CODING), exFile) + ADBDUMMY
    if VERSION2:
      cmd = cmd.encode(CODING)
    #print(cmd)
    os.system(cmd)
    print( prefix + "store external file %s to %s" % (filename, exFile))
    item.setAttribute("filename", APDataPath + "/" + EXTERNALS + "/" + bName)

def processItems(items, exPath):
    for item in items:
        processItem(item, exPath)

def processXML(xmlFile, exPath):
    x = xml.dom.minidom.parse(xmlFile)
    items = []
    items.extend(x.getElementsByTagName("media_item"))
    items.extend(x.getElementsByTagName("audio_track"))
    processItems(items, exPath)
    f = open(xmlFile, 'w')
    f.truncate(0)
    x.writexml(f)
    f.close()

def prepareSingleDir(argv):
    sub = argv[0]
    subDir = WORKSPACE + "/files/" + sub
    os.mkdir(WORKSPACE + "/files/")
    os.mkdir(subDir)
    os.system("adb pull %s/files/%s %s" % (APDataPath, sub, subDir) + ADBDUMMY)

def prepareDir(argv):
    os.mkdir(WORKSPACE)
    if (len(argv) > 0):
        prepareSingleDir(argv)
    else:
        os.system("adb pull %s %s" % (APDataPath, WORKSPACE) + ADBDUMMY)
    exPath = WORKSPACE + "/" + EXTERNALS
    if (not os.path.exists(exPath)):
        os.mkdir(exPath)

def processMetaData(xmlFile):
    x = xml.dom.minidom.parse(xmlFile)
    return x.getElementsByTagName("project")[0].getAttribute("name")

def processDir(exPath):
    p = WORKSPACE + "/files"
    if (not os.path.exists(p)):
        print( "No Projects Found")
        return

    directories = os.listdir(p)
    for directory in directories:
        if directory == ".nomedia":
            continue
        meta = p + "/" + directory + "/metadata.xml"
        if (not os.path.exists(meta)):
            print( "Warning!!! no %s found" % (directory))
            continue
        pName = processMetaData(p + "/" + directory + "/metadata.xml")
        print( "Directory %s ====> Project %s" % (directory, pName))
        xmlFile = p + "/" + directory + "/videoeditor.xml"
        processXML(xmlFile, exPath)
        print( "")

def pullFun(argv):
    print( "Backup file to " + WORKSPACE)
    prepareDir(argv)
    exPath = WORKSPACE + "/" + EXTERNALS
    processDir(exPath)

def rewriteXML(xmlFile):
    f = open(xmlFile, "r")
    lines = f.readlines()
    f.close()

    if SDCARD == "/mnt/sdcard":
        oldAPDataPath = "/mnt/sdcard2" + "/Android/data/" + APName
        newAPDataPath = "/mnt/sdcard" + "/Android/data/" + APName
    else:
        newAPDataPath = "/mnt/sdcard2" + "/Android/data/" + APName
        oldAPDataPath = "/mnt/sdcard" + "/Android/data/" + APName

    newlines = []
    for line in lines:
        newlines.append(line.replace(oldAPDataPath, newAPDataPath))

    f = open(xmlFile, "w+")
    f.writelines(newlines)
    f.close()

def rewriteXMLs(p):
    p = p + "/files"
    directories = os.listdir(p)
    for directory in directories:
        if directory == "." or directory == ".." or directory == EXTERNALS:
            continue
        xmlFile = p + "/" + directory + "/videoeditor.xml"
        if os.path.exists(xmlFile):
            rewriteXML(xmlFile)

def pushFun(argv):
    rewriteXMLs(argv[0])
    os.system("adb push %s %s" % (argv[0], APDataPath))

def getNameFromMeta(meta):
    prefix = "<project name=\""
    l = len(prefix)
    start = meta.find(prefix)
    end = meta.find("\"", start + l)
    return meta[start + l:end]

def listFun(argv):
    directories = os.popen("adb shell ls %s/files" % (APDataPath)).readlines()
    if len(directories) == 0 or directories[0].find("No such file") >= 0:
        print( "No Projects Found")
        return
    for directory in directories:
        if directory.endswith("\r\n"):
            d = directory[:-2]
        else:
            d = directory[:-1]
        if (len(d)) == 0:
           continue    
        
        p = subprocess.Popen("adb shell cat %s/files/%s/metadata.xml" % (APDataPath, d), stdout = subprocess.PIPE, shell=True)
        meta = p.stdout.readlines()
        #meta = os.popen("adb shell cat %s/files/%s/metadata.xml" % (APDataPath, d)).readlines()
        pName = getNameFromMeta(meta[0].decode("utf-8"))
        print( "Directory %s ====> Project %s" % (d, pName))

def cleanFun(argv):
    os.system("adb shell rm -r %s/*" % (APDataPath))

def clean2Fun(argv):
    os.system("adb shell rm -r %s/Movies/movie_*" % SDCARD)

def startleakFun(argv):
    os.system("adb shell rm -r %s" % (VIDEO_EDITOR_LEAK_DIRECTORY))
    os.system("adb shell setprop libc.debug.malloc 1")
    os.system("adb shell setprop media.videoeditor.leak 1")
    os.system("adb shell stop")
    os.system("adb shell start")

def pullleakFun(argv):
    os.mkdir(WORKSPACE)
    os.mkdir(WORKSPACE + "/ve")
    os.mkdir(WORKSPACE + "/ve/ve_leak_2012")
    os.mkdir(WORKSPACE + "/ve/ve_leak_2012/memstatus")
    d = WORKSPACE + "/ve/ve_leak_2012/memstatus"
    f = WORKSPACE + "/ve/ve_leak_2012/result"
    fp = open(f, "w")
    fp.writelines(["pass\n"])
    fp.close()
    os.system("adb pull %s %s" % (VIDEO_EDITOR_LEAK_DIRECTORY, d))

def main(argv):
    funs = { 
            "pull"  : pullFun,
            "push"  : pushFun,
            "list"  : listFun,
            "clean"  : cleanFun,
            "clean2"  : clean2Fun,
            "startleak" : startleakFun,
            "pullleak"  : pullleakFun,
           }
    fun = argv[0]
    if fun in funs:
        argv.pop(0)
        funs[fun](argv)
    else:
        usage()

def usage():
    print( """\
Usage:
    ./ve.py pull
        pull all projects
    ./ve.py pull directory
        pull one project if directory is provided
    ./ve.py push backupdirectory
        push files in backupdirectory to phone
    ./ve.py list
        list all projects with directories
    ./ve.py clean
        clean all projects in phone
    ./ve.py clean2
        clean all outputs in /mnt/sdcard/Movies
    ./ve.py startleak
        set properties for memory leak check
    ./ve.py pullleak
        pull memstatus files for memory leak
        then you can use ./do.py check 2012xxxxxxxx to check memory leak, do.py is under alps/mediatek/source/external/StressTestBench
    """)

if __name__ == "__main__":
    if len(sys.argv) == 1:
        usage()
    else:
        sys.argv.pop(0)
        main(sys.argv)

"""
functional:
adb shell am instrument -e class com.android.mediaframeworktest.functional.videoeditor.MediaItemThumbnailTest -w com.android.mediaframeworktest/.MediaFrameworkTestRunner 
adb shell am instrument -e class com.android.mediaframeworktest.functional.videoeditor.MediaPropertiesTest -w com.android.mediaframeworktest/.MediaFrameworkTestRunner 
adb shell am instrument -e class com.android.mediaframeworktest.functional.videoeditor.VideoEditorAPITest -w com.android.mediaframeworktest/.MediaFrameworkTestRunner 
adb shell am instrument -e class com.android.mediaframeworktest.functional.videoeditor.VideoEditorExportTest -w com.android.mediaframeworktest/.MediaFrameworkTestRunner 
adb shell am instrument -e class com.android.mediaframeworktest.functional.videoeditor.VideoEditorPreviewTest -w com.android.mediaframeworktest/.MediaFrameworkTestRunner 

stress:
adb shell am instrument -e class com.android.mediaframeworktest.stress.VideoEditorStressTest -w com.android.mediaframeworktest/.MediaPlayerStressTestRunner 

performance:
adb shell am instrument -e class com.android.mediaframeworktest.performance.VideoEditorPerformance -w com.android.mediaframeworktest/.MediaFrameworkPerfTestRunner
"""
