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
  ifndef CC_PPC
    CC_PPC:=		$(CC)
  endif
  ifndef CC_X86
    CC_X86:=		$(CC)
  endif
  ifndef CXX_PPC
    CXX_PPC:=		$(CXX)
  endif
  ifndef CXX_X86
    CXX_X86:=		$(CXX)
  endif

  ifndef CFLAGS_PPC
    CFLAGS_PPC=		$(CFLAGS)
  endif
  ifndef CFLAGS_X86
    CFLAGS_X86=		$(CFLAGS)
  endif

  ifndef INCLUDES_PPC
    INCLUDES_PPC=	$(INCLUDES)
  endif
  ifndef INCLUDES_X86
    INCLUDES_X86=	$(INCLUDES)
  endif

  ##
  # gcc-3.3 doesn't permit specifying -o with -c so we have to let it generate
  # the default .o and then rename it.  We set this variable to know whether
  # we're dealing with gcc-3.3 and thus have to deal with this case.  (We only
  # need to use gcc-3.3 for the PowerPC-half of Mac OS X Universal builds.)
  ##
  GCC_33_PPC:=		$(findstring 3.3,$(CC_PPC))

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
  C_OBJECTS_PPC:=	$(C_SOURCES:.c=-ppc.o)
  C_OBJECTS_X86:=	$(C_SOURCES:.c=-x86.o)
  CXX_OBJECTS_PPC:=	$(CXX_SOURCES:.cpp=-ppc.o)
  CXX_OBJECTS_X86:=	$(CXX_SOURCES:.cpp=-x86.o)
  OC_OBJECTS_PPC:=	$(OC_SOURCES:.m=-ppc.o)
  OC_OBJECTS_X86:=	$(OC_SOURCES:.m=-x86.o)
  OCXX_OBJECTS_PPC:=	$(OCXX_SOURCES:.mm=-ppc.o)
  OCXX_OBJECTS_X86:=	$(OCXX_SOURCES:.mm=-x86.o)

  OBJECTS_PPC:=		$(C_OBJECTS_PPC) $(OC_OBJECTS_PPC) \
			$(CXX_OBJECTS_PPC) $(OCXX_OBJECTS_PPC)
  OBJECTS_X86:=		$(C_OBJECTS_X86) $(OC_OBJECTS_X86) \
			$(CXX_OBJECTS_X86) $(OCXX_OBJECTS_X86)
endif

##
# If Universal, override SDKROOT per architecture if specifically defined for a
# given architecture.
##
ifeq ($(UNIVERSAL),1)
  ifdef SDKROOT_PPC
    %-ppc.o : SDKROOT:= $(SDKROOT_PPC)
    %-ppc   : SDKROOT:= $(SDKROOT_PPC)
    ifdef GCC_33_PPC
      %-ppc.o : export NEXT_ROOT:= $(SDKROOT_PPC)
      %-ppc   : export NEXT_ROOT:= $(SDKROOT_PPC)
      %-ppc.o : MACOSX_ISYSROOT:=
      %-ppc   : MACOSX_ISYSROOT:=
      %-ppc.o : MACOSX_SYSLIBROOT:=
      %-ppc   : MACOSX_SYSLIBROOT:=
    endif
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

ifdef GCC_33_PPC
%-ppc.o : %.c
	$(CC_PPC) -c $(CFLAGS_PPC) $(INCLUDES_PPC) $< && mv $*.o $@
else
%-ppc.o : %.c
	$(CC_PPC) -c $(CFLAGS_PPC) $(INCLUDES_PPC) -o $@ $<
endif

ifdef GCC_33_PPC
%-ppc.o : %.cpp
	$(CXX_PPC) -c $(CFLAGS_PPC) $(INCLUDES_PPC) $< && mv $*.o $@
else
%-ppc.o : %.cpp
	$(CXX_PPC) -c $(CFLAGS_PPC) $(INCLUDES_PPC) -o $@ $<
endif

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

ifdef GCC_33_PPC
%-ppc.o : %.m
	$(CC_PPC) -c $(filter-out -fast,$(CFLAGS_PPC)) $(INCLUDES_PPC) $< && mv $*.o $@
else
%-ppc.o : %.m
	$(CC_PPC) -c $(filter-out -fast,$(CFLAGS_PPC)) $(INCLUDES_PPC) -o $@ $<
endif

ifdef GCC_33_PPC
%-ppc.o : %.mm
	$(CXX_PPC) -c $(filter-out -fast,$(CFLAGS_PPC)) $(INCLUDES_PPC) $< && mv $*.o $@
else
%-ppc.o : %.mm
	$(CXX_PPC) -c $(filter-out -fast,$(CFLAGS_PPC)) $(INCLUDES_PPC) -o $@ $<
endif

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

  CC_LINK_PPC:=		$(CC_PPC)
  CC_LINK_X86:=		$(CC_X86)
  ifdef CXX_SOURCES
    CC_LINK_PPC:=	$(CXX_PPC)
    CC_LINK_X86:=	$(CXX_X86)
  endif 
  ifdef OCXX_SOURCES
    CC_LINK_PPC:=	$(CXX_PPC)
    CC_LINK_X86:=	$(CXX_X86)
  endif

  %-ppc	: CFLAGS:= 	$(CFLAGS_PPC)
  %-x86 : CFLAGS:= 	$(CFLAGS_X86)

  %-ppc	: CC_LINK:= 	$(CC_LINK_PPC)
  %-x86 : CC_LINK:= 	$(CC_LINK_X86)

  %-ppc$(JNILIB_EXT) : CFLAGS:= $(CFLAGS_PPC)
  %-x86$(JNILIB_EXT) : CFLAGS:= $(CFLAGS_X86)

  %-ppc$(JNILIB_EXT) : CC_LINK:= $(CC_LINK_PPC)
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
  WINDRES:=	$(shell echo $(CC) | sed 's/gcc/windres/')

%.res : %.rc
	$(WINDRES) $< -o temp.RES
	$(WINDRES) -O coff temp.RES $@
	$(RM) temp.RES
endif

# vim:set noet sw=8 ts=8:
