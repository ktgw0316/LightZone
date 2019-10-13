/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.action.ToggleAction;
import com.lightcrafts.utils.file.FileUtil;
import lombok.Getter;
import lombok.val;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

/**
 * This class defines all the actions the browser wants to export.  They are
 * used in frame menus.
 */
public class ImageBrowserActions {

    // Copy-paste for tools.  Assigned from the AbstractImageBrowser popup.
    static File TemplateClipboard;

    private AbstractImageBrowser browser;

    @Getter
    private LeadSelectionAction editAction;

    @Getter
    private LeadSelectionAction copyAction;

    @Getter
    private SelectionAction pasteAction;

    @Getter
    private SelectionAction refreshAction;

    @Getter
    private SelectionAction leftAction;

    @Getter
    private SelectionAction rightAction;

    @Getter
    private SelectionAction horizontalAction;

    @Getter
    private SelectionAction verticalAction;

    @Getter
    private LeadSelectionAction renameAction;

    @Getter
    private Action selectLatestAction;

    @Getter
    private Action selectAllAction;

    @Getter
    private SelectionAction selectNoneAction;

    private List<SelectionAction> ratingActions;

    // @Getter
    // private List<SelectionAction> ratingAdvanceActions;

    @Getter
    private SelectionAction clearRatingAction;

    // @Getter
    // private SelectionAction clearRatingAdvanceAction;

    @Getter
    private ToggleAction showHideTypesAction;

    @Getter
    private Action showFileInFolderAction;

    @Getter
    private SelectionAction trashAction;

    ImageBrowserActions(AbstractImageBrowser b) {
        this.browser = b;

        editAction = new LeadSelectionAction("EditMenuItem", browser, true) {
            @Override
            public void actionPerformed(ActionEvent event) {
                val lead = getLeadDatum();
                browser.notifyDoubleClicked(lead);
            }
        };

        copyAction = new LeadSelectionAction("CopyMenuItem", browser, true) {
            @Override
            public void actionPerformed(ActionEvent event) {
                val datum = getLeadDatum();
                TemplateClipboard = datum.getFile();
            }
            @Override
            void update() {
                super.update();
                if (isEnabled()) {
                    val lead = getLeadDatum();
                    val type = lead.getType();
                    val hasLzn = type.hasLznData();
                    copyAction.setEnabled(hasLzn);
                }
            }
        };
        pasteAction = new SelectionAction(
            LOCALE.get("PasteMenuItem"), browser, null, true, true
        ) {
            @Override
            public void actionPerformed(ActionEvent event) {
                val selection = getSelection();
                val files = new ArrayList<File>();
                for (val datum : selection) {
                    val file = datum.getFile();
                    files.add(file);
                }
                val templates = browser.getTemplateProvider();
                templates.applyTemplate(
                    TemplateClipboard, files.toArray(new File[0])
                );
            }
            @Override
            void update() {
                super.update();
                if (isEnabled()) {
                    val templates = browser.getTemplateProvider();
                    val selection = getSelection();
                    val hasTemplates = (templates != null);
                    val hasSelection = ! selection.isEmpty();
                    val hasClipboard = (TemplateClipboard != null);
                    setEnabled(hasTemplates && hasSelection && hasClipboard);
                }
            }
        };
        refreshAction = new SelectionAction(
            LOCALE.get("RefreshMenuItem"), browser, null, true, true
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                val selection = getSelection();
                for (val datum : selection) {
                    datum.refresh(false);   // don't use caches
                }
            }
        };
        ratingActions = RatingActions.createRatingActions(browser, true);
//        ratingAdvanceActions = RatingActions.createRatingAdvanceActions(browser, true);
        clearRatingAction = RatingActions.createClearRatingAction(browser, true);
//        clearRatingAdvanceAction = RatingActions.createClearRatingAdvanceAction(browser, true);

        leftAction = RotateActions.createRotateLeftAction(browser, true);
        rightAction = RotateActions.createRotateRightAction(browser, true);

        horizontalAction = FlipActions.createFlipHorizontalAction(browser, true);
        verticalAction = FlipActions.createFlipVerticalAction(browser, true);

        renameAction = new LeadSelectionAction("RenameMenuItem", browser, true) {
            @Override
            public void actionPerformed(ActionEvent event) {
                val datum = browser.getLeadSelectedDatum();
                FileActions.renameFile(
                    datum.getFile(), browser
                );
            }
        };
        selectLatestAction = new AbstractAction(LOCALE.get("SelectLatestActionName")) {
            @Override
            public void actionPerformed(ActionEvent event) {
                browser.selectLatest();
            }
        };
        selectLatestAction.setEnabled(true);

        selectAllAction = new AbstractAction(LOCALE.get("SelectAllActionName")) {
            @Override
            public void actionPerformed(ActionEvent event) {
                browser.selectAll();
            }
        };
        selectAllAction.setEnabled(true);

        // This accelerator key setup should be localized and maybe unified
        // with the centralized menu configuration in MenuFactory.
        val modifier = Platform.isMac() ? "meta" : "ctrl";
        selectAllAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                modifier + " " + "A"
        ));
        selectNoneAction = new SelectionAction(
            LOCALE.get("SelectNoneActionName"), browser, null, true, true
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                browser.clearSelection();
            }
        };
        showHideTypesAction = new ToggleAction(
            LOCALE.get("HideImageTypeMenuItem"),
            LOCALE.get("ShowImageTypeMenuItem")
        ) {
            @Override
            protected void onActionPerformed(ActionEvent event) {
                ImageDatumRenderer.setShowImageTypes(true);
                browser.repaint();
            }
            protected void offActionPerformed(ActionEvent event) {
                ImageDatumRenderer.setShowImageTypes(false);
                browser.repaint();
            }
        };
        showHideTypesAction.setState(ImageDatumRenderer.doesShowImageTypes());

        // The trash action does get a key binding, defined in
        // AbstractImageBrowser instead of here.
        trashAction = new SelectionAction(
            LOCALE.get("TrashMenuItem"), browser, null, true, true
        ) {
            @Override
            public void actionPerformed(ActionEvent event) {
                val datums = browser.getSelectedDatums();
                val files = new ArrayList<File>();
                for (val datum : datums) {
                    val file = datum.getFile();
                    files.add(file);
                }
                FileActions.deleteFiles(files.toArray(new File[0]), browser);
            }
        };
        showFileInFolderAction = new SelectionAction(
            LOCALE.get("ShowMenuItem"), browser, null, true, true
        ) {
            @Override
            public void actionPerformed(ActionEvent event) {
                val datums = browser.getSelectedDatums();
                val files = new ArrayList<File>();
                for (val datum : datums) {
                    val file = datum.getFile();
                    files.add(file);
                }
                val platform = Platform.getPlatform();
                for (val f : files) {
                    val file = FileUtil.resolveAliasFile(f);
                    val path = file.getAbsolutePath();
                    platform.showFileInFolder(path);
                }
            }
        };
    }

    public List<Action> getRatingActions() {
        return new ArrayList<Action>(ratingActions);
    }
}
