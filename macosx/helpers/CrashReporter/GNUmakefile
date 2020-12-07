##
# CrashReporter: a custom crash reporter.
# GNUmakefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

# Uncomment to compile in debug mode.
#DEBUG:=		true

# Different compilers required for Objective-C sources.
XCODE_PATH:=	$(shell xcode-select -p)
ifeq ($(findstring CommandLineTools,$(XCODE_PATH)),CommandLineTools)
  # Use command line tools.
  XCODE_BIN_DIR=	$(XCODE_PATH)/usr/bin
else
  # Use Xcode.
  XCODE_BIN_DIR=	$(XCODE_PATH)/Toolchains/XcodeDefault.xctoolchain/usr/bin
endif
CC=	$(XCODE_BIN_DIR)/clang
CXX=	$(XCODE_BIN_DIR)/clang++

CFLAGS:=	-g -Os -mdynamic-no-pic
INCLUDES=	$(MACOSX_ISYSROOT)
LDFLAGS=	$(PLATFORM_LDFLAGS) -framework Cocoa

TARGET:=	CrashReporter

########## You shouldn't have to change anything below this line. #############

ifdef DEBUG
DEFINES+=	-DDEBUG
endif

CFLAGS+=	$(DEFINES)

include		$(ROOT)/lightcrafts/mk/sources.mk

##
# Build rules
##

.PHONY: all
all: $(TARGET)

ifeq ($(UNIVERSAL),1)

$(TARGET): $(TARGET)-arm $(TARGET)-x86
	$(LIPO) -create $(TARGET)-arm $(TARGET)-x86 -output $@

$(TARGET)-arm: $(OBJECTS_ARM)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ *-arm.o

$(TARGET)-x86: $(OBJECTS_X86)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ *-x86.o

else	# UNIVERSAL

$(TARGET): $(OBJECTS)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ *.o

endif	# UNIVERSAL

.PHONY: bundle
bundle: $(TARGET)
	$(RM) $(TARGET).app
	$(MKDIR) $(TARGET).app/Contents/MacOS
	cp $(TARGET) $(TARGET).app/Contents/MacOS
	cp -r Resources $(TARGET).app/Contents
	cp Info.plist $(TARGET).app/Contents

include		$(ROOT)/lightcrafts/mk/auto_dep.mk

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean:
	$(RM) *.o .*.d

distclean: clean
	$(RM) $(TARGET) $(TARGET)-arm $(TARGET)-x86 $(TARGET).app

mostlyclean:

# vim:set noet sw=8 ts=8:
