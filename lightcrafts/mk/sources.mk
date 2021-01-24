##
# Sources makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ifeq ($(UNIVERSAL),1)

  ##
  # If architecture-specific versions of CC, CXX, and CFLAGS aren't set, just
  # copy them from the architecture-neutral values.
  ##
  ifndef CC_ARM
    CC_ARM:=		$(CC)
  endif
  ifndef CC_X86
    CC_X86:=		$(CC)
  endif
  ifndef CXX_ARM
    CXX_ARM:=		$(CXX)
  endif
  ifndef CXX_X86
    CXX_X86:=		$(CXX)
  endif

  ifndef CFLAGS_ARM
    CFLAGS_ARM=		$(CFLAGS)
  endif
  ifndef CFLAGS_X86
    CFLAGS_X86=		$(CFLAGS)
  endif

  ifndef INCLUDES_ARM
    INCLUDES_ARM=	$(INCLUDES)
  endif
  ifndef INCLUDES_X86
    INCLUDES_X86=	$(INCLUDES)
  endif

endif	# UNIVERSAL

##
# Ordinarily, source files are automatically determined.  If this needs to be
# overridden, a makefile can define:
#
#	C_SOURCES	C sources.
#	CXX_SOURCES	C++ sources.
#	OC_SOURCES	Objective C sources.
#	OCXX_SOURCES	Objective C++ sources.
#	RC_SOURCES	Microsoft Windows resource file sources.
##
ifndef C_SOURCES
  C_SOURCES:=		$(wildcard *.c)
endif
ifndef CXX_SOURCES
  CXX_SOURCES:=		$(wildcard *.cpp)
endif
ifndef OC_SOURCES
  OC_SOURCES:=		$(wildcard *.m)
endif
ifndef OCXX_SOURCES
  OCXX_SOURCES:=	$(wildcard *.mm)
endif
ifndef RC_SOURCES
  RC_SOURCES:=		$(wildcard *.rc)
endif

##
# The object files are derived from all source files.
##
C_OBJECTS:=		$(C_SOURCES:.c=.o)
CXX_OBJECTS:=		$(CXX_SOURCES:.cpp=.o)
OC_OBJECTS:=		$(OC_SOURCES:.m=.o)
OCXX_OBJECTS:=		$(OCXX_SOURCES:.mm=.o)
RC_OBJECTS:=		$(RC_SOURCES:.rc=.res)

OBJECTS:=		$(C_OBJECTS) $(OC_OBJECTS) \
			$(CXX_OBJECTS) $(OCXX_OBJECTS)
ifeq ($(UNIVERSAL),1)
  C_OBJECTS_ARM:=	$(C_SOURCES:.c=-arm.o)
  C_OBJECTS_X86:=	$(C_SOURCES:.c=-x86.o)
  CXX_OBJECTS_ARM:=	$(CXX_SOURCES:.cpp=-arm.o)
  CXX_OBJECTS_X86:=	$(CXX_SOURCES:.cpp=-x86.o)
  OC_OBJECTS_ARM:=	$(OC_SOURCES:.m=-arm.o)
  OC_OBJECTS_X86:=	$(OC_SOURCES:.m=-x86.o)
  OCXX_OBJECTS_ARM:=	$(OCXX_SOURCES:.mm=-arm.o)
  OCXX_OBJECTS_X86:=	$(OCXX_SOURCES:.mm=-x86.o)

  OBJECTS_ARM:=		$(C_OBJECTS_ARM) $(OC_OBJECTS_ARM) \
			$(CXX_OBJECTS_ARM) $(OCXX_OBJECTS_ARM)
  OBJECTS_X86:=		$(C_OBJECTS_X86) $(OC_OBJECTS_X86) \
			$(CXX_OBJECTS_X86) $(OCXX_OBJECTS_X86)
endif

##
# If Universal, override SDKROOT per architecture if specifically defined for a
# given architecture.
##
ifeq ($(UNIVERSAL),1)
  ifdef SDKROOT_ARM
    %-arm.o : SDKROOT:= $(SDKROOT_ARM)
    %-arm   : SDKROOT:= $(SDKROOT_ARM)
  endif
  ifdef SDKROOT_X86
    %-x86.o : SDKROOT:= $(SDKROOT_X86)
    %-x86   : SDKROOT:= $(SDKROOT_X86)
  endif
endif

##
# Since we use C, Objective C, C++, and Objective C++ in builds, a single value
# for CC is insufficient.  Therefore, for each type of source file, we override
# the built-in rule and specify the compiler flavor explicitly.
##

ifeq ($(UNIVERSAL),1)

%-arm.o : %.c
	$(CC_ARM) -c $(CFLAGS_ARM) $(INCLUDES_ARM) -o $@ $<

%-arm.o : %.cpp
	$(CXX_ARM) -c $(CFLAGS_ARM) $(INCLUDES_ARM) -o $@ $<

%-x86.o : %.c
	$(CC_X86) -c $(CFLAGS_X86) $(INCLUDES_X86) -o $@ $<

%-x86.o : %.cpp
	$(CXX_X86) -c $(CFLAGS_X86) $(INCLUDES_X86) -o $@ $<
else	# UNIVERSAL
%.o : %.c
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

%.o : %.cpp
	$(CXX) -c $(CFLAGS) $(INCLUDES) $<
endif	# UNIVERSAL

##
# Note that when compiling Objective-C or Objective-C++, gcc doesn't accept the
# -fast option, so we filter it out.
##
ifeq ($(UNIVERSAL),1)

%-arm.o : %.m
	$(CC_ARM) -c $(filter-out -fast,$(CFLAGS_ARM)) $(INCLUDES_ARM) -o $@ $<

%-arm.o : %.mm
	$(CXX_ARM) -c $(filter-out -fast,$(CFLAGS_ARM)) $(INCLUDES_ARM) -o $@ $<

%-x86.o : %.m
	$(CC_X86) -c $(filter-out -fast,$(CFLAGS_X86)) $(INCLUDES_X86) -o $@ $<

%-x86.o : %.mm
	$(CXX_X86) -c $(filter-out -fast,$(CFLAGS_X86)) $(INCLUDES_X86) -o $@ $<
else	# UNIVERSAL
%.o : %.m
	$(CC) -c $(filter-out -fast,$(CFLAGS)) $(INCLUDES) $<

%.o : %.mm
	$(CXX) -c $(filter-out -fast,$(CFLAGS)) $(INCLUDES) $<
endif	# UNIVERSAL

##
# As mentioned above, since we use C, Objective C, C++, and Objective C++ in
# builds, a single value for CC is insufficient.  However, only one gcc flavor
# can be used in the final command to link.  We therefore default to using gcc
# and override that to g++ if there were any C++ or Objective C++ source files
# since using at least one of either requires that we link using g++.
##
ifeq ($(UNIVERSAL),1)

  CC_LINK_ARM:=		$(CC_ARM)
  CC_LINK_X86:=		$(CC_X86)
  ifdef CXX_SOURCES
    CC_LINK_ARM:=	$(CXX_ARM)
    CC_LINK_X86:=	$(CXX_X86)
  endif
  ifdef OCXX_SOURCES
    CC_LINK_ARM:=	$(CXX_ARM)
    CC_LINK_X86:=	$(CXX_X86)
  endif

  %-arm	: CFLAGS:= 	$(CFLAGS_ARM)
  %-x86 : CFLAGS:= 	$(CFLAGS_X86)

  %-arm	: CC_LINK:= 	$(CC_LINK_ARM)
  %-x86 : CC_LINK:= 	$(CC_LINK_X86)

  %-arm$(JNILIB_EXT) : CFLAGS:= $(CFLAGS_ARM)
  %-x86$(JNILIB_EXT) : CFLAGS:= $(CFLAGS_X86)

  %-arm$(JNILIB_EXT) : CC_LINK:= $(CC_LINK_ARM)
  %-x86$(JNILIB_EXT) : CC_LINK:= $(CC_LINK_X86)

else	# UNIVERSAL

  CC_LINK:=		$(CC)
  ifdef CXX_SOURCES
    CC_LINK:=		$(CXX)
  endif
  ifdef OCXX_SOURCES
    CC_LINK:=		$(CXX)
  endif

endif	# UNIVERSAL

##
# Windows-specific
##
ifeq ($(PLATFORM),Windows)
  ifeq ($(CYGWIN),1)
    WINDRES:=	$(shell echo $(CC) | sed 's/gcc/windres/')
  else
    WINDRES:=	windres
  endif

%.res : %.rc
	$(WINDRES) $< -o temp.RES
	$(WINDRES) -O coff temp.RES $@
	$(RM) temp.RES
endif

# vim:set noet sw=8 ts=8:
