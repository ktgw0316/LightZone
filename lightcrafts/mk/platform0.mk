##
# Platform0 Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

PLATFORM:=		$(shell uname)

##
# Mac OS X
##
ifeq ($(PLATFORM),Darwin)
  PLATFORM:=		MacOSX
endif

##
# Windows
##
ifeq ($(findstring CYGWIN,$(PLATFORM)),CYGWIN)
  PLATFORM:=		Windows
endif

##
# Linux
##
ifeq ($(PLATFORM),Linux)
  # PLATFORM is OK as-is
endif

##
# GNU/kFreeBSD
##
ifeq ($(PLATFORM),GNU/kFreeBSD)
  PLATFORM:=		Linux
endif

##
# FreeBSD
##
ifeq ($(PLATFORM),FreeBSD)
  # PLATFORM is OK as-is
  PLATFORM_DIR:=	${ROOT}/linux
endif

##
# OpenIndiana
##
ifeq ($(PLATFORM),SunOS)
  # PLATFORM is OK as-is
  PLATFORM_DIR:=		$(ROOT)/linux
endif

##
# Miscellaneous stuff.
##

# Flag to indicate that this file has been included
PLATFORM0:=		1

PLATFORM_DIR?=		$(ROOT)/$(shell echo $(PLATFORM) | tr '[A-Z]' '[a-z]')

# vim:set noet sw=8 ts=8:
