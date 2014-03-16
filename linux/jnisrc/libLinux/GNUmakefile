##
# libLinux Makefile
##

TARGET_BASE:=		Linux

# Uncomment to compile in debug mode.
#DEBUG:=			true

JNI_LINUX_INCLUDES:=	-I/usr/local/include/
JNI_LINUX_LINK:= 	-lLCJNI -lX11
JNI_LINUX_LDFLAGS:=	-L/usr/local/lib/

JAVAH_CLASSES:=	com.lightcrafts.platform.linux.LinuxKeyUtil

ROOT:=		../../..
include		../jni-linux.mk

# vim:set noet sw=8 ts=8:
