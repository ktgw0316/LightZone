##
# LightZone Mac OS X help makefile
#
# Paul J. Lucs [paul@lightcrafts.com]
##

LANGUAGES:=	English Japanese Spanish

##
# Build rules
##

.PHONY: all
all %:
	@for lang in $(LANGUAGES); \
	do $(MAKE) -f lang.mk --no-print-directory LANG=$$lang $@; \
	done
