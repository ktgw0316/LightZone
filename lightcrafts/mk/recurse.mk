##
# Intermediate/recursive directory makefile
#
# This makefile is included by all makefiles that are in "intermediate"
# directories, e.g., jnisrc, so as to recurse into all subdirectories and
# perform a "make" there.
#
# Paul J. Lucas [paul@lightcrafts.com]
# Masahiro Kitagawa [arctica0316@gmail.com]
##

ifndef SUBDIRS
  SUBDIRS:=	$(dir $(wildcard */*akefile))
endif

##
# Build rules
##

# We do our own echo of the directory because "make --print-directory" is
# overly verbose.

default: all

.PHONY: $(SUBDIRS)

$(SUBDIRS)::
	@echo "-----> Entering $@"
	$(MAKE) --no-print-directory -C $@ $(MAKECMDGOALS) || exit 1
	@echo "-----> Leaving $@"

all clean distclean mostlyclean: $(SUBDIRS)

# vim:set noet sw=8 ts=8:
