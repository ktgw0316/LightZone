HIGH_PERFORMANCE:=	1
USE_ICC_HERE:=		1

ROOT:=			../../..
include			$(ROOT)/lightcrafts/mk/platform.mk

TARGET_BASE:=		DCRaw

# Uncomment to compile in debug mode.
#DEBUG:=			true

ifeq ($(PLATFORM),MacOSX)
  JNI_EXTRA_LINK:=      -lstdc++
endif

JNI_EXTRA_CFLAGS+=	-fopenmp

JNI_EXTRA_DEFINES:=	-DNO_JPEG -DNO_LCMS
JNI_LINUX_DEFINES:=	-Dfgetc=getc_unlocked
JNI_MACOSX_DEFINES:=	$(JNI_LINUX_DEFINES)

JNI_WINDOWS_LINK:=      -lgomp.dll
JNI_MACOSX_LINK:=	-single_module

##
# Altivec stuff is specified in JNI_PPC_DEFINES rather than JNI_PPC_CFLAGS
# because otherwise gcc complains about it during dependency generation.
##
JNI_PPC_DEFINES=	$(ALTIVEC_CFLAGS)

##
# Manually specify our own C sources because dcraw.c is #include'd into
# DCRawjnilib.c; therefore dcraw.c must not be built into dcraw.o.
##
C_SOURCES:=		dcrawUtils.c

JAVAH_CLASSES:=		com.lightcrafts.utils.DCRaw

include			../jni.mk

# vim:set noet sw=8 ts=8:
