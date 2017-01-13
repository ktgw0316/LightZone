##
# Platform0 Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

PLATFORM:=		$(shell uname -s)

# Mac OS X
ifeq ($(PLATFORM),Darwin)
  PLATFORM:=		MacOSX

# Windows
else ifeq ($(findstring CYGWIN,$(PLATFORM)),CYGWIN)
  PLATFORM:=		Windows
  CYGWIN:=		1
else ifeq ($(findstring MINGW,$(PLATFORM)),MINGW)
  PLATFORM:=		Windows
else ifeq ($(findstring MSYS,$(PLATFORM)),MSYS)
  PLATFORM:=		Windows

# Debian GNU/kFreeBSD
else ifeq ($(PLATFORM),GNU/kFreeBSD)
  PLATFORM:=		Linux

# Linux, SunOS, and other BSDs
else
  # Keep PLATFORM as-is.
  PLATFORM_DIR:=	${ROOT}/linux
endif

##
# Miscellaneous stuff.
##

# Flag to indicate that this file has been included
PLATFORM0:=		1

PLATFORM_DIR?=		$(ROOT)/$(shell echo $(PLATFORM) | tr '[A-Z]' '[a-z]')

# vim:set noet sw=8 ts=8:
