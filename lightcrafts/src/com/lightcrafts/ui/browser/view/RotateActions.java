/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageDatumType;
import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Actions for changing the orientations of images through the browser.
 */
class RotateActions {

    static List<SelectionAction> createAllActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        ArrayList<SelectionAction> actions = new ArrayList<SelectionAction>();
        actions.addAll(createRotateActions(browser, dynamic));
        actions.addAll(createRotateAdvanceActions(browser, dynamic));
        return actions;
    }

    static List<SelectionAction> createRotateAdvanceActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        List<SelectionAction> actions = new ArrayList<SelectionAction>();
        actions.add(createRotateLeftAdvanceAction(browser, dynamic));
        actions.add(createRotateRightAdvanceAction(browser, dynamic));
        return actions;
    }

    static List<SelectionAction> createRotateActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        List<SelectionAction> actions = new ArrayList<SelectionAction>();
        actions.add(createRotateLeftAction(browser, dynamic));
        actions.add(createRotateRightAction(browser, dynamic));
        return actions;
    }

    static SelectionAction createRotateLeftAdvanceAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        SelectionAction action = new SelectionAction(
            LOCALE.get("LeftAdvanceMenuItem"),
            browser,
            KeyStroke.getKeyStroke('[', KeyEvent.SHIFT_DOWN_MASK),
            dynamic, true
        ) {
            public void actionPerformed(ActionEvent e) {
                rotateLeftAndAdvance(browser);
            }
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
        return action;
    }

    static SelectionAction createRotateRightAdvanceAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        SelectionAction action = new SelectionAction(
            LOCALE.get("RightAdvanceMenuItem"),
            browser,
            KeyStroke.getKeyStroke(']', KeyEvent.SHIFT_DOWN_MASK),
            dynamic, true
        ) {
            public void actionPerformed(ActionEvent e) {
                rotateRightAndAdvance(browser);
            }
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
        return action;
    }

    static SelectionAction createRotateLeftAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        SelectionAction action = new SelectionAction(
            LOCALE.get("LeftMenuItem"),
            browser,
            KeyStroke.getKeyStroke('[', 0),
            dynamic, true
        ) {
            public void actionPerformed(ActionEvent e) {
                rotateLeft(browser);
            }
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
        return action;
    }

    static SelectionAction createRotateRightAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        SelectionAction action = new SelectionAction(
            LOCALE.get("RightMenuItem"),
            browser,
            KeyStroke.getKeyStroke(']', 0),
            dynamic, true
        ) {
            public void actionPerformed(ActionEvent e) {
                rotateRight(browser);
            }
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
        return action;
    }

    static void rotateLeftAndAdvance(AbstractImageBrowser browser) {
        rotateLeft(browser);
        browser.moveSelectionNext();
    }

    static void rotateRightAndAdvance(AbstractImageBrowser browser) {
        rotateRight(browser);
        browser.moveSelectionNext();
    }

    static void rotateLeft(AbstractImageBrowser browser) {
        List<ImageDatum> datums = browser.getSelectedDatums();
        for (ImageDatum datum : datums) {
            rotateLeft(datum, browser);
        }
    }

    static void rotateRight(AbstractImageBrowser browser) {
        List<ImageDatum> datums = browser.getSelectedDatums();
        for (ImageDatum datum : datums) {
            rotateRight(datum, browser);
        }
    }

    static void rotateLeft(ImageDatum datum, AbstractImageBrowser browser) {
        try {
            datum.rotateLeft();
        }
        catch (Throwable t) {
            browser.notifyError(t.getMessage());
            System.err.println("Rotate left failed");
            t.printStackTrace();
        }
    }

    static void rotateRight(ImageDatum datum, AbstractImageBrowser browser) {
        try {
            datum.rotateRight();
        }
        catch (Throwable t) {
            browser.notifyError(t.getMessage());
            System.err.println("Rotate right failed");
            t.printStackTrace();
        }
    }

    private static boolean hasNonLznSelection(SelectionAction action) {
        List<ImageDatum> datums = action.getSelection();
        for (ImageDatum datum : datums) {
            ImageDatumType type = datum.getType();
            if (! type.hasLznData()) {
                return true;
            }
        }
        return false;
    }
}
