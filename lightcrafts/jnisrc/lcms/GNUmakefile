HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1

TARGET_BASE:=		LCMS

# Uncomment to compile in debug mode.
#DEBUG:=			true

##
# I didn't want to figure out how to tweak this EDISON stuff to have it conform
# to the way JNI source is built, so I'll just define the TARGET rule manually.
##
JNI_MANUAL_TARGET:=	1

##
# This is to prevent LCMS from using the MessageBox() function to report errors
# under Windows and just use stderr instead.
##
JNI_WINDOWS_DEFINES:=	-D__CONSOLE__

JAVAH_CLASSES:=		com.lightcrafts.utils.LCMS

ROOT:=			../../..
include			../jni.mk

LCMS_SOURCE:=		$(wildcard lcms/src/*.c) $(wildcard *.c)
LCMS_INCLUDES:=		$(wildcard lcms/src/*.h)
LCMS_LDFLAGS:=		$(filter-out $(PLATFORM_LDFLAGS),$(LDFLAGS))

ifeq ($(UNIVERSAL),1)
$(TARGET_PPC) $(TARGET_X86): $(LCMS_SOURCE) $(LCMS_INCLUDES)
	-$(MKDIR) $(TARGET_DIR)
	$(CC_LINK) $(CFLAGS) $(INCLUDES) $(LCMS_LDFLAGS) -o $@ $(LCMS_SOURCE)
ifeq ($(PLATFORM),MacOSX)
	  cp -p $@ $(TARGET_DIR)
endif
	$(RM) $@.dSYM
else
$(TARGET): $(LCMS_SOURCE) $(LCMS_INCLUDES)
	-$(MKDIR) $(TARGET_DIR)
	$(CC_LINK) $(CFLAGS) $(INCLUDES) $(LCMS_LDFLAGS) -o $@ $(LCMS_SOURCE)
ifeq ($(PLATFORM),MacOSX)
	  cp -p $@ $(TARGET_DIR)
endif
	$(RM) $@.dSYM
endif

# vim:set noet sw=8 ts=8:
