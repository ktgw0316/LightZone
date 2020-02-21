HIGH_PERFORMANCE:=	1

ROOT:=			../../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

TARGET_BASE:=		LCLENSFUN

JNI_EXTRA_CFLAGS:=	-std=c++14
JNI_EXTRA_INCLUDES:=	$(shell $(PKGCFG) --cflags lensfun)
JNI_EXTRA_LINK:=	$(shell $(PKGCFG) --libs-only-l lensfun)
ifneq ($(PLATFORM),MacOSX)
  JNI_EXTRA_LDFLAGS:=	$(shell $(PKGCFG) --libs-only-L lensfun)
endif
JNI_MACOSX_INCLUDES:=	-I/usr/local/include
JNI_MACOSX_LDFLAGS:=	-L/usr/local/lib

JAVAH_CLASSES:=		com.lightcrafts.utils.Lensfun

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
