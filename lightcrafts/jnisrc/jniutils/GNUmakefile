TARGET_BASE:=		LCJNI

# Uncomment to compile in debug mode.
#DEBUG:=		true

JNI_MACOSX_DYLIB:=	1
JNI_WINDOWS_IMPLIB:=	1
ifeq ($(PROCESSOR),x86_64)
  JNI_FREEBSD_DEFINES:=	-DAJ_FreeBSD64
endif

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
