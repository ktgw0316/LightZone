##
# Sources makefile
#
# Paul J. Lucas [paul@lightcrafts.com]
##

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

##
# Since we use C, Objective C, C++, and Objective C++ in builds, a single value
# for CC is insufficient.  Therefore, for each type of source file, we override
# the built-in rule and specify the compiler flavor explicitly.
##

%.o : %.c
	$(CC) -c $(CFLAGS) $(INCLUDES) $<

%.o : %.cpp
	$(CXX) -c $(CFLAGS) $(INCLUDES) $<

##
# Note that when compiling Objective-C or Objective-C++, gcc doesn't accept the
# -fast option, so we filter it out.
##
%.o : %.m
	$(CC) -c $(filter-out -fast,$(CFLAGS)) $(INCLUDES) $<

%.o : %.mm
	$(CXX) -c $(filter-out -fast,$(CFLAGS)) $(INCLUDES) $<

##
# As mentioned above, since we use C, Objective C, C++, and Objective C++ in
# builds, a single value for CC is insufficient.  However, only one gcc flavor
# can be used in the final command to link.  We therefore default to using gcc
# and override that to g++ if there were any C++ or Objective C++ source files
# since using at least one of either requires that we link using g++.
##

CC_LINK:=		$(CC)
ifdef CXX_SOURCES
  CC_LINK:=		$(CXX)
endif
ifdef OCXX_SOURCES
  CC_LINK:=		$(CXX)
endif

##
# Windows-specific
##
ifeq ($(PLATFORM),Windows)
  WINDRES:=	windres

%.res : %.rc
	$(WINDRES) $< -o temp.RES
	$(WINDRES) -O coff temp.RES $@
	$(RM) temp.RES
endif

# vim:set noet sw=8 ts=8:
