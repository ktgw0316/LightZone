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
# Miscellaneous stuff.
##

# Flag to indicate that this file has been included
PLATFORM0:=		1

PLATFORM_DIR:=		$(ROOT)/$(shell echo $(PLATFORM) | tr '[A-Z]' '[a-z]')

# vim:set noet sw=8 ts=8:
