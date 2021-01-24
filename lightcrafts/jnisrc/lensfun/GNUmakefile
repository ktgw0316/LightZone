HIGH_PERFORMANCE:=	1

ROOT:=			../../..
COMMON_DIR:=		$(ROOT)/lightcrafts
include			$(COMMON_DIR)/mk/platform.mk

TARGET_BASE:=		LCLENSFUN

JNI_EXTRA_CFLAGS:=	-std=c++14
JNI_EXTRA_INCLUDES:=	$(shell $(PKGCFG) --cflags lensfun)
JNI_EXTRA_LINK:=	$(shell $(PKGCFG) --libs-only-l lensfun)
JNI_EXTRA_LDFLAGS:=	$(shell $(PKGCFG) --libs-only-L lensfun)

JAVAH_CLASSES:=		com.lightcrafts.utils.Lensfun

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
