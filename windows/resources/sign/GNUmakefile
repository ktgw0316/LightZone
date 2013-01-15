DESCRIPTION:=		LightZone
DESCRIPTION_URL:=	http://lightzoneproject.org/

CERT_BASENAME:=		LightZone
CERT_PASSWORD:=		photoshopsucks

TIMESTAMP_URL:=		http://timestamp.comodoca.com/authenticode

PATH:=			$(PATH):/cygdrive/c/Program\ Files/Microsoft\ SDKs/Windows/v6.0/Bin

########## You shouldn't have to change anything below this line. #############

##
# Insist that EXE_TO_SIGN has been defined unless the goals contains the word
# "clean".
##
ifneq ($(findstring clean,$(MAKECMDGOALS)),clean)
  ifndef EXE_TO_SIGN
    $(error EXE_TO_SIGN must be defined)
  endif

  WIN_EXE_TO_SIGN:=	$(shell cygpath -w "$(EXE_TO_SIGN)")
  UNIX_EXE_TO_SIGN:=	$(shell cygpath -u "$(EXE_TO_SIGN)")
endif

PVK_FILE:=		$(CERT_BASENAME).pvk
SPC_FILE:=		$(CERT_BASENAME).spc
PFX_FILE:=		$(CERT_BASENAME).pfx

all: $(UNIX_EXE_TO_SIGN)

#/*$(PFX_FILE): $(PVK_FILE) $(SPC_FILE)*/
#/*	pvk2pfx -pvk $(PVK_FILE) -pi $(CERT_PASSWORD) -spc $(SPC_FILE) -pfx $@*/
#/*	chmod -x $@*/

$(UNIX_EXE_TO_SIGN): $(PFX_FILE) FORCE
	signtool sign /f $(PFX_FILE) /p "$(CERT_PASSWORD)" \
	/d "$(DESCRIPTION)" /du "$(DESCRIPTION_URL)" /t "$(TIMESTAMP_URL)" \
	"$(WIN_EXE_TO_SIGN)"

.PHONY: FORCE
FORCE:

clean distclean mostlyclean:
#/*	rm -f $(PFX_FILE)*/
