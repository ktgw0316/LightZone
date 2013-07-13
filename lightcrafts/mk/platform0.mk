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
  PLATFORM_DIR:=		$(ROOT)/macosx
endif

##
# Windows
##
ifeq ($(findstring CYGWIN,$(PLATFORM)),CYGWIN)
  PLATFORM:=		Windows
  PLATFORM_DIR:=		$(ROOT)/windows
endif

##
# Linux
##
ifeq ($(PLATFORM),Linux)
  # PLATFORM is OK as-is
  PLATFORM_DIR:=		$(ROOT)/linux
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

# vim:set noet sw=8 ts=8:
