TARGET_BASE:=		LCLCMS

# Uncomment to compile in debug mode.
#DEBUG:=			true

JNI_EXTRA_PKGCFG:=	lcms2

##
# This is to prevent LCMS from using the MessageBox() function to report errors
# under Windows and just use stderr instead.
##
JNI_WINDOWS_DEFINES:=	-D__CONSOLE__

ROOT:=			../../..
include			../jni.mk

# vim:set noet sw=8 ts=8:
