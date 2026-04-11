/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageDatumType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Actions for changing the orientations of images through the browser.
 */
class RotateActions {

    private static final Logger logger = LoggerFactory.getLogger(RotateActions.class);

    static List<SelectionAction> createAllActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        ArrayList<SelectionAction> actions = new ArrayList<>();
        actions.addAll(createRotateActions(browser, dynamic));
        actions.addAll(createRotateAdvanceActions(browser, dynamic));
        return actions;
    }

    static List<SelectionAction> createRotateAdvanceActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        List<SelectionAction> actions = new ArrayList<>();
        actions.add(createRotateLeftAdvanceAction(browser, dynamic));
        actions.add(createRotateRightAdvanceAction(browser, dynamic));
        return actions;
    }

    static List<SelectionAction> createRotateActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        List<SelectionAction> actions = new ArrayList<>();
        actions.add(createRotateLeftAction(browser, dynamic));
        actions.add(createRotateRightAction(browser, dynamic));
        return actions;
    }

    static SelectionAction createRotateLeftAdvanceAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        return new SelectionAction(
            LOCALE.get("LeftAdvanceMenuItem"),
            browser,
            KeyStroke.getKeyStroke('[', KeyEvent.SHIFT_DOWN_MASK),
            dynamic, true
        ) {
            @Override
            protected SelectionAction clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                rotateLeftAndAdvance(browser);
            }

            @Override
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
    }

    static SelectionAction createRotateRightAdvanceAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        return new SelectionAction(
            LOCALE.get("RightAdvanceMenuItem"),
            browser,
            KeyStroke.getKeyStroke(']', KeyEvent.SHIFT_DOWN_MASK),
            dynamic, true
        ) {
            @Override
            protected SelectionAction clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                rotateRightAndAdvance(browser);
            }

            @Override
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
    }

    static SelectionAction createRotateLeftAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        return new SelectionAction(
            LOCALE.get("LeftMenuItem"),
            browser,
            KeyStroke.getKeyStroke('[', 0),
            dynamic, true
        ) {
            @Override
            protected SelectionAction clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                rotateLeft(browser);
            }

            @Override
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
    }

    static SelectionAction createRotateRightAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        return new SelectionAction(
            LOCALE.get("RightMenuItem"),
            browser,
            KeyStroke.getKeyStroke(']', 0),
            dynamic, true
        ) {
            @Override
            protected SelectionAction clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                rotateRight(browser);
            }

            @Override
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
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
        catch (IOException | BadImageFileException | UnknownImageTypeException e) {
            browser.notifyError(e.getMessage());
            logger.warn("Rotate left failed for {}", datum.getFile(), e);
        }
    }

    static void rotateRight(ImageDatum datum, AbstractImageBrowser browser) {
        try {
            datum.rotateRight();
        }
        catch (IOException | BadImageFileException | UnknownImageTypeException e) {
            browser.notifyError(e.getMessage());
            logger.warn("Rotate right failed for {}", datum.getFile(), e);
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
