##
# Intermediate/recursive directory makefile
#
# This makefile is included by all makefiles that are in "intermediate"
# directories, e.g., jnisrc, so as to recurse into all subdirectories and
# perform a "make" there.
#
# Paul J. Lucas [paul@lightcrafts.com]
##

ifndef SUBDIRS
  SUBDIRS:=	*
endif

##
# Build rules
##

# We do our own echo of the directory because "make --print-directory" is
# overly verbose.

.PHONY: all
all:
	@for dir in $(SUBDIRS); \
	do \
	    if [ -f $$dir/*akefile ]; \
	    then \
		echo "-----> Entering $$dir"; \
	        $(MAKE) --no-print-directory -C $$dir $@ || exit 1; \
		echo "-----> Leaving $$dir"; \
	    fi; \
	done

%:
	@for dir in $(SUBDIRS); \
	do \
	    if [ -f $$dir/*akefile ]; \
	    then \
		echo "-----> Entering $$dir"; \
	        $(MAKE) --no-print-directory -C $$dir $@ || exit 1; \
		echo "-----> Leaving $$dir"; \
	    fi; \
	done

# vim:set noet sw=8 ts=8:
