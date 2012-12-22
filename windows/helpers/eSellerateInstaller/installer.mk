##
# eSellerateInstaller
# GNUmakefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

# Uncomment to compile in debug mode.
#DEBUG:=		true

CFLAGS:=	-Os -mno-cygwin
DEFINES:=	-D_WIN32_IE=0x0500 -D_WIN32_WINNT=0x0500 -DUNICODE
LDFLAGS:=	-L .
LINK:=		-leWebClient -lshlwapi -lole32

TARGET:=	eSellerateInstaller.exe

########## You shouldn't have to change anything below this line. #############

ifdef DEBUG
  CFLAGS+=	-g
  DEFINES+=	-DDEBUG
  STRIP:=	echo >/dev/null
else
  STRIP:=	strip
endif

CFLAGS+=	$(DEFINES)

include		$(COMMON_DIR)/mk/sources.mk

##
# Build rules
##

.PHONY: all
all: $(TARGET)

$(TARGET): $(OBJECTS)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ $(OBJECTS) $(LINK)
	$(STRIP) $@

include		$(COMMON_DIR)/mk/auto_dep.mk

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean:
	$(RM) *.o .*.d RC*

distclean: clean
	$(RM) $(TARGET)

mostlyclean:

# vim:set noet sw=8 ts=8:
