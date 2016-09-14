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

MACOSX_MINOR_VERSION:=	$(shell sw_vers -productVersion | cut -d. -f2-2)

# Different compilers required for Objective-C sources.
ifeq ($(MACOSX_MINOR_VERSION),6) # Snow Leopard
  CC=	gcc
  CXX=	g++
else
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
endif

ifeq ($(UNIVERSAL),1)
  ##
  # We need to build the PowerPC part with gcc/g++ 3.3 so the launcher will run
  # on older versions of Mac OS X.
  ##
  CC_PPC:=	$(CC) -arch ppc
  CXX_PPC:=	$(CXX) -arch ppc
  CC_X86:=	$(CC) -arch i386
  CXX_X86:=	$(CXX) -arch i386

  #SDKROOT_PPC:=	/Developer/SDKs/MacOSX10.3.9.sdk
  #SDKROOT_X86:=	/Developer/SDKs/MacOSX10.4u.sdk
else
  ifeq ($(PROCESSOR),powerpc)
    ##
    # For PowerPC-only, we want to build against 10.3.9 to support Panther.
    # (Intel-only requires 10.4.)
    ##
    SDKROOT:=	/Developer/SDKs/MacOSX10.3.9.sdk
  endif
endif

CFLAGS:=	-g -Os -mdynamic-no-pic
INCLUDES=	$(MACOSX_ISYSROOT)
LDFLAGS=	$(PLATFORM_LDFLAGS) -framework Cocoa

TARGET:=	CrashReporter

########## You shouldn't have to change anything below this line. #############

export MACOSX_DEPLOYMENT_TARGET_ppc:=	10.3
export MACOSX_DEPLOYMENT_TARGET_i386:=	10.5

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

$(TARGET): $(TARGET)-ppc $(TARGET)-x86
	$(LIPO) -create $(TARGET)-ppc $(TARGET)-x86 -output $@

$(TARGET)-ppc: $(OBJECTS_PPC)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ *-ppc.o

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
	$(RM) $(TARGET) $(TARGET)-ppc $(TARGET)-x86 $(TARGET).app

mostlyclean:

# vim:set noet sw=8 ts=8:
