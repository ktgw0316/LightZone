##
# Mac OS X .dmg file makefile.
#
# Paul J. Lucas [paul@lightcrafts.com]
##

APP_NAME:=		LightZone

# The location of the "common" directory.
COMMON:=		photokina

# The location of the top-level "macosx" directory.
MACOSX:=		..

# The location of the top-level "windows" directory.
WINDOWS:=		../../windows

# The name of the LightZone Windows installer
WIN_INSTALLER:=		$(APP_NAME)-Installer.exe

DMG_BKGRND:=		dmg_background.gif

MAC_FOLDER:=		$(APP_NAME) for Macintosh
MAC_DOC_FOLDER:=	$(MAC_FOLDER)/Documentation
WIN_FOLDER:=		$(APP_NAME) for Windows

########## You shouldn't have to change anything below this line. #############

# The .dmg filesystem type.
DMG_FS:=	HFS+

# This is a temporary size, not the final .dmg size, so just make it big.
DMG_SIZE:=	150m

ASR:=		/usr/sbin/asr
MKDIR:=		mkdir -p
RM:=		rm -fr
SETFILE:=	/Developer/Tools/SetFile

HDIUTIL:=	hdiutil
DMGCREATE:=	$(HDIUTIL) create -ov
DMGATTACH:=	$(HDIUTIL) attach
DMGDETACH:=	$(HDIUTIL) detach
DMGCOMPRESS:=	$(HDIUTIL) convert -format UDZO -imagekey zlib-level=9 -ov
HYBRIDCREATE:=	$(HDIUTIL) makehybrid

PROTOTYPE:=	$(APP_NAME)-prototype
PROTOTYPE_DMG:=	$(PROTOTYPE).dmg
PROTOTYPE_VOL:=	/Volumes/$(PROTOTYPE)

MASTER:=	$(APP_NAME)
MASTER_DMG:=	$(MASTER)-master.dmg
MASTER_VOL:=	/Volumes/$(MASTER)

USER:=		$(APP_NAME)
USER_DMG:=	$(USER).dmg
USER_ISO:=	$(USER).iso

# The location of the TheApp.app/Contents directory.
APP_CONTENTS:=	$(MACOSX)/release/$(APP_NAME).app/Contents

MAC_DOC_ORIG_DIR:=	$(MACOSX)/documentation
WIN_DOC_ORIG_DIR:=	$(WINDOWS)/documentation

.PHONY: default prototype master user
default:
	@echo
	@echo "Make one of: prototype, master, user, iso"
	@echo

prototype: $(PROTOTYPE_DMG)
master: $(MASTER_DMG)
user: $(USER_DMG)
iso: $(USER_ISO)

###############################################################################
# Prototype .dmg
###############################################################################

$(PROTOTYPE_DMG): $(MASTER_VOL)
	$(DMGCREATE) -fs $(DMG_FS) -size $(DMG_SIZE) -volname $(PROTOTYPE) -attach $@
	$(MKDIR) "$(PROTOTYPE_VOL)/$(MAC_FOLDER)/$(APP_NAME).app"
	$(MKDIR) "$(PROTOTYPE_VOL)/$(MAC_DOC_FOLDER)"
	$(MKDIR) "$(PROTOTYPE_VOL)/$(WIN_FOLDER)"
	$(MKDIR) "$(MASTER_VOL)/$(MAC_FOLDER)"
	cp $(DMG_BKGRND) "$(MASTER_VOL)/$(MAC_FOLDER)"
	cp $(DMG_BKGRND) "$(PROTOTYPE_VOL)/$(MAC_FOLDER)"
	$(SETFILE) -a V "$(PROTOTYPE_VOL)/$(MAC_FOLDER)/$(DMG_BKGRND)"
	open $(PROTOTYPE_VOL)
	@echo
	@echo "Now customize the folder/icon layout:"
	@echo "+ No toolbar"
	@echo "+ Position folder window near upper-left corner of screen"
	@echo "+ Icon view/size: 128x128"
	@echo "+ Text size: 12pt"
	@echo "+ Label position: bottom"
	@echo "+ No arrangement"
	@echo "+ Background picture: $(MASTER_VOL)/$(MAC_FOLDER)/$(DMG_BKGRND)"
	@echo
	@echo "When done: make master"
	@echo

$(MASTER_VOL): clean_master
	$(MKDIR) $(MASTER_VOL); touch $(MASTER_VOL)/prototype_flag

###############################################################################
# Master .dmg
###############################################################################

$(MASTER_DMG): $(PROTOTYPE_VOL)
	$(DMGCREATE) -srcfolder $(PROTOTYPE_VOL) -volname $(MASTER) $@
	if [ -f $(MASTER_VOL)/prototype_flag ]; then $(RM) $(MASTER_VOL); fi
	$(DMGDETACH) $(PROTOTYPE_VOL)
	@echo
	@echo "You may now:"
	@echo "+ Delete $(PROTOTYPE_DMG)"
	@echo

$(PROTOTYPE_VOL):
	$(DMGATTACH) $(PROTOTYPE_DMG)

###############################################################################
# User .dmg
###############################################################################

TEMP:=		$(APP_NAME)-temp
TEMP_VOL:=	/Volumes/$(TEMP)
TEMP_DMG:=	$(TEMP).dmg

$(USER_DMG): $(APP_CONTENTS) clean_master clean_temp
	$(DMGCREATE) -fs $(DMG_FS) -size $(DMG_SIZE) -volname $(TEMP) -attach $(TEMP_DMG)
	$(DMGATTACH) $(MASTER_DMG)
	$(ASR) -source $(MASTER_VOL) -target $(TEMP_VOL) -erase -noprompt
	$(DMGDETACH) $(MASTER_VOL)
	cp -r $(APP_CONTENTS) "$(MASTER_VOL) 1/$(MAC_FOLDER)/$(APP_NAME).app"
	cp $(COMMON)/* "$(MASTER_VOL) 1"
	cp $(MAC_DOC_ORIG_DIR)/* "$(MASTER_VOL) 1/$(MAC_DOC_FOLDER)"
	$(DMGDETACH) $(MASTER_VOL)\ 1
	$(DMGCOMPRESS) $(TEMP_DMG) -o $@
	$(RM) $(TEMP_DMG)

$(APP_CONTENTS):
	cd $(MACOSX) && ant -DUNIVERSAL=1 bundle-tb

###############################################################################
# Hybrid .iso
###############################################################################

$(USER_ISO): $(APP_CONTENTS) $(WIN_INSTALLER) clean_master clean_temp
	$(DMGCREATE) -fs $(DMG_FS) -size $(DMG_SIZE) -volname $(TEMP) -attach $(TEMP_DMG)
	$(DMGATTACH) $(MASTER_DMG)
	$(ASR) -source $(MASTER_VOL) -target $(TEMP_VOL) -erase -noprompt
	$(DMGDETACH) $(MASTER_VOL)
	cp -r $(APP_CONTENTS) "$(MASTER_VOL) 1/$(MAC_FOLDER)/$(APP_NAME).app"
	cp -pR $(COMMON)/* "$(MASTER_VOL) 1"
	cp $(MAC_DOC_ORIG_DIR)/* "$(MASTER_VOL) 1/$(MAC_DOC_FOLDER)"
	cp $(WIN_DOC_ORIG_DIR)/* "$(MASTER_VOL) 1/$(WIN_FOLDER)"
	cp $(WIN_INSTALLER) "$(MASTER_VOL) 1/$(WIN_FOLDER)"
	cp $(WINDOWS)/resources/cd/* $(MASTER_VOL)\ 1
	$(RM) $(MASTER_VOL)\ 1/Autorun.inf
	$(DMGDETACH) $(MASTER_VOL)\ 1
	$(DMGCOMPRESS) $(TEMP_DMG) -o $(USER_DMG)
	$(RM) $(TEMP_DMG)
	$(HYBRIDCREATE) -o $@ $(USER_DMG) -hfs -iso -joliet \
		-abstract-file abstract.txt \
		-copyright-file cpyright.txt \
		-application "$(APP_NAME)" \
		-publisher "Light Crafts, Inc." \
		-default-volume-name "$(APP_NAME)" \
		-only-joliet '*{ico,inf,txt}'

$(WIN_INSTALLER):
	@echo
	@echo You must build $@ under Windows
	@echo and copy it to this directory.
	@echo
	@exit 1

###############################################################################
# Utility rules
###############################################################################

clean_master:
	if [ -f $(MASTER_VOL)/prototype_flag ]; then $(RM) $(MASTER_VOL); fi
	if [ -d $(MASTER_VOL) ]; then $(DMGDETACH) $(MASTER_VOL); fi

clean_prototype:
	if [ -d $(PROTOTYPE_VOL) ]; then $(DMGDETACH) $(PROTOTYPE_VOL); fi
	$(RM) $(PROTOTYPE_DMG)

clean_temp:
	if [ -d $(TEMP_VOL) ]; then $(DMGDETACH) $(TEMP_VOL); fi
	$(RM) $(TEMP_DMG)

clean: clean_prototype clean_master clean_temp
	if [ -d $(MASTER_VOL)\ 1 ]; then $(DMGDETACH) $(MASTER_VOL)\ 1; fi

distclean mostlyclean: clean
	$(RM) $(USER_DMG) $(USER_ISO)

# vim:set noet sw=8 ts=8:
