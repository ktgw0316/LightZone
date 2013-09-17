##
# Automatic dependency generation makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

AUTO_DEP_FLAGS:=	-MM -DAUTO_DEP $(DEFINES)

ifeq ($(UNIVERSAL),1)
  ##
  # We need to use an architecture-specific INCLUDES, but since dependencies
  # are generated once regardless of the number of architectures, we have to
  # pick one, so we pick PPC.  Strictly speaking, this isn't the right thing do
  # do since it means the X86 compile will depend on PPC includes, but in
  # practice it's OK because this is only for dependency generation, not code
  # generation.
  ##
  AUTO_DEP_FLAGS+=	$(INCLUDES_PPC)
else
  AUTO_DEP_FLAGS+=	$(INCLUDES)
endif

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

ifeq ($(findstring MacOSX10.2.8,$(AUTO_DEP_FLAGS)),MacOSX10.2.8)
  ##
  # There aren't gcc 4.0 headers for the 10.2.8 SDK, so use gcc 3.3 to generate
  # the dependencies.
  ##
  AUTO_DEP_CC:=		gcc-3.3
else
  AUTO_DEP_CC:=		$(CC)
endif

MAKEDEPEND:=		$(AUTO_DEP_CC) $(AUTO_DEP_FLAGS)

# Must not use := here!
define MAKE_DEP
  $(MAKEDEPEND) $1 | sed "s!^\([^ :]*\):!\1 $2 : !" | sed 's/\(\w\):/\/cygdrive\/\L\1/g' > $2; [ -s $2 ]  || $(RM) $2
endef

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
