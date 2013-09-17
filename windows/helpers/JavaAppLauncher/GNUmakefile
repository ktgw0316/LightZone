##
# JavaAppLauncher: a simple Java application launcher for Windows.
# GNUmakefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts
include		$(COMMON_DIR)/mk/platform.mk

JARS:=		$(COMMON_DIR)/products/lightcrafts.jar \
		../../products/lightcrafts-windows.jar

# Uncomment to compile in debug mode.
#DEBUG:=		true

CFLAGS:=	-O3
DEFINES:=	-D_WIN32_IE=0x0500 -D_WIN32_WINNT=0x0500 -DNTDDK_VERSION=0x05000200 -DUNICODE
INCLUDES:=	-I"$(JAVA_HOME)/include" -I"$(JAVA_HOME)/include/win32" \
		-I$(COMMON_DIR)/jnisrc/jniutils
LDFLAGS:=	-mwindows -static-libgcc -static-libstdc++
LINK:=		-lshlwapi

TARGET:=	LightZone.exe

########## You shouldn't have to change anything below this line. #############

ifdef DEBUG
  CFLAGS+=	-g
  DEFINES+=	-DDEBUG
  STRIP:=	echo >/dev/null
else
  STRIP:=	strip
endif

CFLAGS+=	$(DEFINES)

RC_TEMPLATES:=	$(wildcard *.rc.template)
RC_SOURCES:=	$(RC_TEMPLATES:.rc.template=.rc)

include		$(COMMON_DIR)/mk/sources.mk

RESOURCES:=	../../resources

##
# Build rules
##

.PHONY: all
all: $(TARGET)

$(TARGET): $(OBJECTS) $(RC_OBJECTS)
	$(CC_LINK) $(CFLAGS) $(LDFLAGS) $(RC_OBJECTS) -o $@ $(OBJECTS) $(LINK)
	$(STRIP) $@

main.cpp: jar_md5_include

jar_md5_include: $(JARS)
	$(COMMON_DIR)/tools/bin/lc-md5-jar -l c $(JARS) > $@

lightzone.res: LightZone.ico local_strings.h

LightZone.ico: $(RESOURCES)/icons/LightZone.ico
	$(call SYMLINK,$^,$@)

%.rc : %.rc.template
	./build-rc.sh $^ $@

include		$(COMMON_DIR)/mk/auto_dep.mk

##
# Utility rules
##

.PHONY: clean distclean mostlyclean

clean:
	$(RM) *.o .*.d *.rc *.res RC* jar_md5_include LightZone.ico

distclean: clean
	$(RM) $(TARGET)

mostlyclean:

# vim:set noet sw=8 ts=8:
