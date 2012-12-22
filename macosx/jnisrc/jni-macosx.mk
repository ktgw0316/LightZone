##
# JNI Mac OS X Makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

JNI_MACOSX_LDFLAGS+=	-framework Cocoa

##
# ROOT is defined by the makefile including this one.
##
include 		$(ROOT)/lightcrafts/jnisrc/jni.mk

# vim:set noet sw=8 ts=8:
