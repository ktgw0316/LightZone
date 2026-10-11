##
# Automatic dependency generation makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

AUTO_DEP_FLAGS:=	-MM -DAUTO_DEP $(DEFINES)

ifeq ($(PLATFORM),MacOSX)
  AUTO_DEP_FLAGS+=	-D_DARWIN_C_SOURCE -D__DARWIN_C_LEVEL=199506L
endif

AUTO_DEP_FLAGS+=	$(INCLUDES)

ifeq ($(PROCESSOR),powerpc)
  ##
  # When doing dependency generation for Mac OS X universal binaries, -arch
  # parameters are not specified but -DLC_USE_ALTIVEC is.  This causes:
  #
  #	#error Use the "-maltivec" flag to enable PowerPC AltiVec support
  #
  # A way to get rid of this error is to specify -maltivec during dependency
  # generation for PowerPC only.
  ##
  AUTO_DEP_FLAGS+= 	-maltivec
endif

AUTO_DEP_CC:=		$(CC)

MAKEDEPEND:=		$(AUTO_DEP_CC) $(AUTO_DEP_FLAGS)

.%.d : %.c
	$(call MAKE_DEP,$<,$@)

.%.d : %.cpp
	$(call MAKE_DEP,$<,$@)

.%.d : %.m
	$(call MAKE_DEP,$<,$@)

.%.d : %.mm
	$(call MAKE_DEP,$<,$@)

##
# Include the dependency files only if the goals don't contain the word
# "clean".
##
ifneq ($(findstring clean,$(MAKECMDGOALS)),clean)
  -include $(OBJECTS:%.o=.%.d)
endif

# vim:set noet sw=8 ts=8:
