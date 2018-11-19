HIGH_PERFORMANCE:=	1
#USE_ICC_HERE:=		1

TARGET_BASE:=		dcraw_lz

# Uncomment to compile in debug mode.
#DEBUG:=		true

EXEC_EXTRA_CFLAGS:=	-fopenmp -Wno-unused-result
EXEC_WINDOWS_CFLAGS:=	-static
EXEC_EXTRA_DEFINES:=	-DNODEPS -DLIGHTZONE
EXEC_LINUX_DEFINES:=	-Dfgetc=getc_unlocked
EXEC_MACOSX_DEFINES:=	$(EXEC_LINUX_DEFINES)
EXEC_WINDOWS_DEFINES:=	-DDJGPP
EXEC_WINDOWS_LINK:=	-lmingwex -lws2_32
EXEC_LINUX_LINK:=	-lm

ROOT:=		../../..
include		$(ROOT)/lightcrafts/mk/executable.mk

# vim:set noet sw=8 ts=8:
