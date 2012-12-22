ROOT:=		../../..
COMMON_DIR:=	$(ROOT)/lightcrafts

TARGET:=	trial

include		$(ROOT)/lightcrafts/mk/sources.mk

##
# Build rules
##

.PHONY: all
all: $(TARGET)

$(TARGET): $(OBJECTS)
	$(CC_LINK) -o $@ $(OBJECTS)

include 	$(COMMON_DIR)/mk/auto_dep.mk

##
# Utility rules
##

.PHONY: clean distclean

clean:
	$(RM) *.o .*.d core

distclean: clean
	$(RM) $(TARGET)

# vim:set noet sw=8 ts=8:
