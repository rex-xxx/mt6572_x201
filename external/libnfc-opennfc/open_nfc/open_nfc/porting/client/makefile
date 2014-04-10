###------------------------------------
###
### makefile: main Target makefile
###
### includes makefile.rules containing all default rules.
###
### ROOT      := root dir of the source tree; -- can be relative to the makefile dir
### TARGETDIR := where the makefile resides; where the binary file is created
### TARGET    := binary file name (no dir)
### SUBTARGETS:= all dirs containing needed C/ASM files -- ALL files from these directories are compiled
### HDIRS     := dir where other needed .H files are -- C/ASM files here will NOT be compiled
### MISCFILES := other needed individual dir/files.c or dir/file.asm
### OTHERFILES:= other files not needed for compiling, but necessary for a full package delivery
###              -- used only with "make tar" or "make tgz" commands.
### MAIN      := (only when building using libraries) source file containing the "main()" function.
###
### ------------------------------------------------------------------------
### Other definable macros, which have default values when not defined here:
### ------------------------------------------------------------------------
###
### TARGET_SYSTEM:= linux or solaris; default: depends on $(OSTYPE) and $(TERM) environment variables
### INCLUDE   := list of include dirs; defaults to dirs listed in SUBTARGETS and HDIRS
### S         := ASM files extension; defaults to "s"
### O         := Obj files extension; defaults to "o"
### OBJDIR    := dir where obj files are put; defaults to ./libs
### A         := Lib files extension; defaults to "a"
### LIBSDIR   := dir where lib files are put; defaults to ./libs
### CCDEP     := C preprocessor command; defaults to $(CC)
###
###	Obj files are put in ./objects
###
###------------------------------------

###
### This makefile creates the LIBRARY file lib/libuseropen_nfclib.a
### to be linked with the Open NFC client applications
###

TARGET_SYSTEM:=linux

ROOT      := ../..
TARGETDIR := client_server/client
TARGET    := userlib

OPEN_NFC_ROOT:=../../../core

# Include definitions specific to cross-compilation and Hardware Target
include $(ROOT)/makefile.settings

OTHERLIBS := 
SUBTARGETS:= client_server/client
SUBTARGETS+= client_server/common

SUBTARGETS+= $(OPEN_NFC_ROOT)/sources
HDIRS     := $(OPEN_NFC_ROOT)/interfaces 

OTHERFILES:= $(ROOT)/$(TARGETDIR)/makefile $(ROOT)/makefile.rules

POSTDIR := $(ROOT)/examples

### This makefile creates a Dynamic library file only
_default: dynlib postcopy_so


### This makefile creates a Dynamic library file only
_default: dynlib 

include $(ROOT)/makefile.rules

#eof
