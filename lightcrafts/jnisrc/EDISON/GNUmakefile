HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1

TARGET_BASE:=		Segment

# Uncomment to compile in debug mode.
#DEBUG:=			true

##
# I didn't want to figure out how to tweak this EDISON stuff to have it conform
# to the way JNI source is built, so I'll just define the TARGET rule manually.
##
JNI_MANUAL_TARGET:=	1

JAVAH_CLASSES:=		com.lightcrafts.utils.Segment

ROOT:=			../../..
include			../jni.mk

SEGM_SOURCE:=		$(wildcard segm/*.cpp) $(wildcard *.cpp)
SEGM_INCLUDES:=		$(wildcard segm/*.h)
SEGM_LDFLAGS:=		$(filter-out $(PLATFORM_LDFLAGS),$(LDFLAGS))

ifeq ($(UNIVERSAL),1)
$(TARGET_PPC) $(TARGET_X86): $(SEGM_SOURCE) $(SEGM_INCLUDES)
	-$(MKDIR) $(TARGET_DIR)
	$(CC_LINK) $(CFLAGS) $(INCLUDES) $(SEGM_LDFLAGS) -o $@ $(SEGM_SOURCE)
	$(RM) $@.dSYM
else
$(TARGET): $(SEGM_SOURCE) $(SEGM_INCLUDES)
	-$(MKDIR) $(TARGET_DIR)
	$(CC_LINK) $(CFLAGS) $(INCLUDES) $(SEGM_LDFLAGS) -o $@ $(SEGM_SOURCE)
	$(RM) $@.dSYM
endif

# vim:set noet sw=8 ts=8:
