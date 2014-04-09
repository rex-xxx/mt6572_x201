#!/bin/sh

#WORKSPACE=$(dirname $(pwd -P))

WORKSPACE=/proj/mtk03449/Perforce/ALPS_SW/TRUNK/ALPS.JB/
FINDBUGS_EXE=~/local/findbugs/bin/findbugs

# Run FindBugs
SRC_DIR=$WORKSPACE/alps/mediatek/packages/wallpapers/Stereo3DWallpaper
FRAMEWORK_JAR=$WORKSPACE/alps/out/target/common/obj/JAVA_LIBRARIES/framework_intermediates/classes.jar
DALVIK_JAR=$WORKSPACE/alps/out/target/common/obj/JAVA_LIBRARIES/core_intermediates/classes.jar
EMMA_JAR=$WORKSPACE/alps/out/target/common/obj/JAVA_LIBRARIES/emma_intermediates/classes.jar
NGIN3D_JAR=$WORKSPACE/alps/out/target/common/obj/JAVA_LIBRARIES/com.mediatek.ngin3d_intermediates/classes.jar
OUTPUT_DIR=$WORKSPACE/alps/out/target/common/obj/APPS/Stereo3DWallpaper_intermediates

~/local/findbugs/bin/findbugs -textui -xml -auxclasspath $FRAMEWORK_JAR:$DALVIK_JAR:$EMMA_JAR:$NGIN3D_JAR -sourcepath $SRC_DIR/src -exclude $SRC_DIR/.findbugs-exclude/exclude.xml -output $SRC_DIR/findbugs-stereo3dwallpaper.xml $OUTPUT_DIR/noproguard.classes.jar
