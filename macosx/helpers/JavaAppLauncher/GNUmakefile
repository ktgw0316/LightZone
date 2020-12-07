##
# JavaAppLauncher: a simple Java application launcher for Mac OS X.
# GNUmakefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
MACOSX_DIR:=	../..
include		$(COMMON_DIR)/mk/platform.mk

JARS:=		$(COMMON_DIR)/products/lightcrafts.jar \
		$(MACOSX_DIR)/products/lightcrafts-apple.jar

# Uncomment to compile in debug mode.
#DEBUG:=		true

# Different compilers required for Objective-C sources.
MACOSX_DEPLOYMENT_TARGET:=	$(shell sw_vers -productVersion | cut -d. -f-2)
SDKROOT:=	$(shell xcodebuild -version -sdk macosx${MACOSX_DEPLOYMENT_TARGET} | sed -n '/^Path:/p' | sed 's/^Path: //')

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

CFLAGS:=	-Os -mdynamic-no-pic -fobjc-exceptions
INCLUDES=	$(MACOSX_ISYSROOT) $(JAVA_INCLUDES) \
		-I$(COMMON_DIR)/jnisrc/jniutils
LDFLAGS=	$(PLATFORM_LDFLAGS) -framework JavaVM -framework Cocoa

TARGET:=	JavaAppLauncher

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

ifeq ($(UNIVERSAL),1)

$(TARGET): $(TARGET)-arm $(TARGET)-x86
	$(LIPO) -create $(TARGET)-arm $(TARGET)-x86 -output $@

$(TARGET)-arm: $(OBJECTS_ARM)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ *-arm.o $(LINK)
	$(STRIP) $@

$(TARGET)-x86: $(OBJECTS_X86)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ *-x86.o $(LINK)
	$(STRIP) $@

else	# UNIVERSAL

$(TARGET): $(OBJECTS)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) -o $@ *.o $(LINK)
	$(STRIP) $@

endif	# UNIVERSAL

include		$(COMMON_DIR)/mk/auto_dep.mk

main.o main-arm.o main-x86.o: jar_md5_include

jar_md5_include: $(JARS)
	$(COMMON_DIR)/tools/bin/lc-md5-jar -l c $(JARS) > $@

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean:
	$(RM) *.o .*.d jar_md5_include

distclean: clean
	$(RM) $(TARGET) $(TARGET)-arm $(TARGET)-x86

mostlyclean:

# vim:set noet sw=8 ts=8:
