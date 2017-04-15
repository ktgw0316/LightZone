/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.action.ToggleAction;
import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageDatumType;
import static com.lightcrafts.ui.browser.view.Locale.LOCALE;
import com.lightcrafts.utils.file.FileUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class defines all the actions the browser wants to export.  They are
 * used in frame menus.
 */
public class ImageBrowserActions {

    // Copy-paste for tools.  Assigned from the AbstractImageBrowser popup.
    static File TemplateClipboard;

    private AbstractImageBrowser browser;

    private LeadSelectionAction editAction;
    private LeadSelectionAction copyAction;
    private SelectionAction pasteAction;
    private SelectionAction refreshAction;
    private SelectionAction leftAction;
    private SelectionAction rightAction;

    private LeadSelectionAction renameAction;

    private Action selectLatestAction;
    private Action selectAllAction;
    private SelectionAction selectNoneAction;

    private List<SelectionAction> ratingActions;
    // private List<SelectionAction> ratingAdvanceActions;
    private SelectionAction clearRatingAction;
    // private SelectionAction clearRatingAdvanceAction;

    private ToggleAction showHideTypesAction;

    private Action showAction;

    private SelectionAction trashAction;

    ImageBrowserActions(AbstractImageBrowser b) {
        this.browser = b;

        editAction = new LeadSelectionAction("EditMenuItem", browser, true) {
            public void actionPerformed(ActionEvent event) {
                ImageDatum lead = getLeadDatum();
                browser.notifyDoubleClicked(lead);
            }
        };
        copyAction = new LeadSelectionAction(
            "CopyMenuItem", browser, true
        ) {
            public void actionPerformed(ActionEvent event) {
                ImageDatum datum = getLeadDatum();
                TemplateClipboard = datum.getFile();
            }
            void update() {
                super.update();
                if (isEnabled()) {
                    ImageDatum lead = getLeadDatum();
                    ImageDatumType type = lead.getType();
                    boolean hasLzn = type.hasLznData();
                    copyAction.setEnabled(hasLzn);
                }
            }
        };
        pasteAction = new SelectionAction(
            LOCALE.get("PasteMenuItem"), browser, null, true, true
        ) {
            public void actionPerformed(ActionEvent event) {
                List<ImageDatum> selection = getSelection();
                List<File> files = new ArrayList<File>();
                for (ImageDatum datum : selection) {
                    File file = datum.getFile();
                    files.add(file);
                }
                TemplateProvider templates = browser.getTemplateProvider();
                templates.applyTemplate(
                    TemplateClipboard, files.toArray(new File[0])
                );
            }
            void update() {
                super.update();
                if (isEnabled()) {
                    TemplateProvider templates = browser.getTemplateProvider();
                    List<ImageDatum> selection = getSelection();
                    boolean hasTemplates = (templates != null);
                    boolean hasSelection = ! selection.isEmpty();
                    boolean hasClipboard = (TemplateClipboard != null);
                    setEnabled(hasTemplates && hasSelection && hasClipboard);
                }
            }
        };
        refreshAction = new SelectionAction(
            LOCALE.get("RefreshMenuItem"), browser, null, true, true
        ) {
            public void actionPerformed(ActionEvent e) {
                List<ImageDatum> selection = getSelection();
                for (ImageDatum datum : selection) {
                    datum.refresh(false);   // don't use caches
                }
            }
        };
        ratingActions =
            RatingActions.createRatingActions(browser, true);
//        ratingAdvanceActions =
//            RatingActions.createRatingAdvanceActions(browser, true);
        clearRatingAction =
            RatingActions.createClearRatingAction(browser, true);
//        clearRatingAdvanceAction =
//            RatingActions.createClearRatingAdvanceAction(browser, true);

        leftAction = RotateActions.createRotateLeftAction(browser, true);
        rightAction = RotateActions.createRotateRightAction(browser, true);

        renameAction = new LeadSelectionAction(
            "RenameMenuItem", browser, true
        ) {
            public void actionPerformed(ActionEvent event) {
                ImageDatum datum = browser.getLeadSelectedDatum();
                FileActions.renameFile(
                    datum.getFile(), browser
                );
            }
        };
        selectLatestAction = new AbstractAction(
            LOCALE.get("SelectLatestActionName")
        ) {
            public void actionPerformed(ActionEvent event) {
                browser.selectLatest();
            }
        };
        selectLatestAction.setEnabled(true);

        selectAllAction = new AbstractAction(
            LOCALE.get("SelectAllActionName")
        ) {
            public void actionPerformed(ActionEvent event) {
                browser.selectAll();
            }
        };
        selectAllAction.setEnabled(true);

        // This accelerator key setup should be localized and maybe unified
        // with the centralized menu configuration in MenuFactory.
        final String modifier = Platform.isMac() ? "meta" : "ctrl";
        selectAllAction.putValue(
            Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                modifier + " " + "A"
            )
        );
        selectNoneAction = new SelectionAction(
            LOCALE.get("SelectNoneActionName"), browser, null, true, true
        ) {
            public void actionPerformed(ActionEvent e) {
                browser.clearSelection();
            }
        };
        showHideTypesAction = new ToggleAction(
            LOCALE.get("HideImageTypeMenuItem"),
            LOCALE.get("ShowImageTypeMenuItem")
        ) {
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
            public void actionPerformed(ActionEvent event) {
                List<ImageDatum> datums = browser.getSelectedDatums();
                List<File> files = new ArrayList<File>();
                for (ImageDatum datum : datums) {
                    File file = datum.getFile();
                    files.add(file);
                }
                FileActions.deleteFiles(files.toArray(new File[0]), browser);
            }
        };
        showAction = new SelectionAction(
            LOCALE.get("ShowMenuItem"), browser, null, true, true
        ) {
            public void actionPerformed(ActionEvent event) {
                List<ImageDatum> datums = browser.getSelectedDatums();
                List<File> files = new ArrayList<File>();
                for (ImageDatum datum : datums) {
                    File file = datum.getFile();
                    files.add(file);
                }
                Platform platform = Platform.getPlatform();
                for (File file : files) {
                    file = FileUtil.resolveAliasFile(file);
                    String path = file.getAbsolutePath();
                    platform.showFileInFolder(path);
                }
            }
        };
    }

    public Action getEditAction() {
        return editAction;
    }

    public Action getCopyAction() {
        return copyAction;
    }

    public Action getShowFileInFolderAction() {
        return showAction;
    }

    public Action getPasteAction() {
        return pasteAction;
    }

    public Action getRefreshAction() {
        return refreshAction;
    }

    public List<Action> getRatingActions() {
        return new ArrayList<Action>(ratingActions);
    }

//    public List<Action> getRatingAdvanceActions() {
//        return new ArrayList<Action>(ratingAdvanceActions);
//    }

    public Action getClearRatingAction() {
        return clearRatingAction;
    }

//    public Action getClearRatingAdvanceAction() {
//        return clearRatingAdvanceAction;
//    }

    public Action getLeftAction() {
        return leftAction;
    }

    public Action getRightAction() {
        return rightAction;
    }

    public Action getRenameAction() {
        return renameAction;
    }

    public Action getSelectLatestAction() {
        return selectLatestAction;
    }

    public Action getSelectAllAction() {
        return selectAllAction;
    }

    public Action getSelectNoneAction() {
        return selectNoneAction;
    }

    public ToggleAction getShowHideTypesAction() {
        return showHideTypesAction;
    }

    public Action getTrashAction() {
        return trashAction;
    }
}
