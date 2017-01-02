/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;

import javax.swing.*;
import java.awt.event.ActionEvent;

import lombok.val;

import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

/**
 * Actions for changing the orientations of images through the browser.
 */
class FlipActions {

    static SelectionAction createFlipHorizontalAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        return new SelectionAction(
            LOCALE.get("HorizontalMenuItem"),
            browser,
            KeyStroke.getKeyStroke('[', 0),
            dynamic, true
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                flipHorizontal(browser);
            }
            @Override
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
    }

    static SelectionAction createFlipVerticalAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        return new SelectionAction(
            LOCALE.get("VerticalMenuItem"),
            browser,
            KeyStroke.getKeyStroke(']', 0),
            dynamic, true
        ) {
            @Override
            public void actionPerformed(ActionEvent e) {
                flipVertical(browser);
            }
            @Override
            void update() {
                setEnabled(hasNonLznSelection(this));
            }
        };
    }

    static void flipHorizontal(AbstractImageBrowser browser) {
        val datums = browser.getSelectedDatums();
        for (val datum : datums) {
            flipHorizontal(datum, browser);
        }
    }

    static void flipVertical(AbstractImageBrowser browser) {
        val datums = browser.getSelectedDatums();
        for (val datum : datums) {
            flipVertical(datum, browser);
        }
    }

    static void flipHorizontal(ImageDatum datum, AbstractImageBrowser browser) {
        try {
            datum.flipHorizontal();
        }
        catch (Throwable t) {
            browser.notifyError(t.getMessage());
            System.err.println("Flip horizontal failed");
            t.printStackTrace();
        }
    }

    static void flipVertical(ImageDatum datum, AbstractImageBrowser browser) {
        try {
            datum.flipVertical();
        }
        catch (Throwable t) {
            browser.notifyError(t.getMessage());
            System.err.println("Flip vertical failed");
            t.printStackTrace();
        }
    }

    private static boolean hasNonLznSelection(SelectionAction action) {
        val datums = action.getSelection();
        for (val datum : datums) {
            val type = datum.getType();
            if (! type.hasLznData()) {
                return true;
            }
        }
        return false;
    }
}
