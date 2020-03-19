/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;
import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Actions for setting the rating on an image in a browser, including actions
 * that also advance the browser's selection state, with accelerator key
 * properties.
 */
class RatingActions {

    static List<SelectionAction> createAllActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        ArrayList<SelectionAction> actions = new ArrayList<SelectionAction>();
        actions.addAll(createRatingActions(browser, dynamic));
        // actions.addAll(createRatingAdvanceActions(browser, dynamic));
        actions.add(createClearRatingAction(browser, dynamic));
        // actions.add(createClearRatingAdvanceAction(browser, dynamic));
        return actions;
    }

//    static SelectionAction createClearRatingAdvanceAction(
//        final AbstractImageBrowser browser, boolean dynamic
//    ) {
//        SelectionAction action = new SelectionAction(
//            LOCALE.get("ClearRatingAdvanceAction"),
//            browser,
//            KeyStroke.getKeyStroke('0', KeyEvent.SHIFT_DOWN_MASK),
//            dynamic
//        ) {
//            public void actionPerformed(ActionEvent e) {
//                clearRating(browser);
//                browser.moveSelectionNext();
//            }
//        };
//        return action;
//    }

    static SelectionAction createClearRatingAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        SelectionAction action = new SelectionAction(
            LOCALE.get("ClearRatingAction"),
            browser,
            KeyStroke.getKeyStroke('0', 0),
            dynamic,
            true
        ) {
            public void actionPerformed(ActionEvent e) {
                clearRating(browser);
            }
        };
        return action;
    }

//    static List<SelectionAction> createRatingAdvanceActions(
//        AbstractImageBrowser browser, boolean dynamic
//    ) {
//        List<SelectionAction> actions = new ArrayList<SelectionAction>();
//        for (int rating=1; rating<=5; rating++) {
//            SelectionAction action =
//                createRatingAdvanceAction(browser, rating, dynamic);
//            actions.add(action);
//        }
//        return actions;
//    }

    static List<SelectionAction> createRatingActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        List<SelectionAction> actions = new ArrayList<SelectionAction>();
        for (int rating=1; rating<=5; rating++) {
            SelectionAction action =
                createRatingAction(browser, rating, dynamic);
            actions.add(action);
        }
        return actions;
    }

//    static SelectionAction createRatingAdvanceAction(
//        final AbstractImageBrowser browser, final int rating, boolean dynamic
//    ) {
//        String stars = createStars(rating);
//        SelectionAction action = new SelectionAction(
//            LOCALE.get("RateAndAdvanceAction", stars),
//            browser,
//            KeyStroke.getKeyStroke('0' + rating, KeyEvent.SHIFT_DOWN_MASK),
//            dynamic
//        ) {
//            public void actionPerformed(ActionEvent e) {
//                setRating(browser, rating);
//                browser.moveSelectionNext();
//            }
//        };
//        return action;
//    }

    private static String createStars(int rating) {
        StringBuffer buffer = new StringBuffer();
        for (int n=0; n<rating; n++) {
            buffer.append('\u2605');    // a unicode star character
        }
        return buffer.toString();
    }

    static SelectionAction createRatingAction(
        final AbstractImageBrowser browser, final int rating, boolean dynamic
    ) {
        String stars = createStars(rating);
        SelectionAction action = new SelectionAction(
            stars, // LOCALE.get("RateAction", stars),
            browser,
            KeyStroke.getKeyStroke('0' + rating, 0),
            dynamic,
            true
        ) {
            public void actionPerformed(ActionEvent e) {
                setRating(browser, rating);
            }
        };
        return action;
    }

    // Get the action that queries the browser selection model for its
    // selected images, assigns the given rating to them, and then maybe
    // advances the lead selection to the next image.
    static void setRating(
        AbstractImageBrowser browser, int rating
    ) {
        List<ImageDatum> datums = browser.getSelectedDatums();
        for (ImageDatum datum : datums) {
            try {
                datum.setRating(rating);
            }
            catch (Throwable t) {
                browser.notifyError(t.getMessage());
                File file = datum.getFile();
                System.err.println(
                    "Couldn't set rating on " + file.getAbsolutePath() + ": "
                );
                t.printStackTrace();
            }
        }
    }

    static boolean setRating(
        ImageDatum datum, int rating, AbstractImageBrowser browser
    ) {
        try {
            datum.setRating(rating);
            return true;
        }
        catch (Throwable t) {
            browser.notifyError(t.getMessage());
            File file = datum.getFile();
            System.err.println(
                "Couldn't set rating on " + file.getAbsolutePath() + ": "
            );
            t.printStackTrace();
            return false;
        }
    }

    static boolean clearRating(ImageDatum datum, AbstractImageBrowser browser) {
        try {
            datum.clearRating();
            return true;
        }
        catch (Throwable t) {
            browser.notifyError(t.getMessage());
            File file = datum.getFile();
            System.err.println(
                "Couldn't clear rating on " + file.getAbsolutePath() + ": "
            );
            t.printStackTrace();
            return false;
        }
    }

    static void clearRating(AbstractImageBrowser browser) {
        List<ImageDatum> datums = browser.getSelectedDatums();
        for (ImageDatum datum : datums) {
            File file = datum.getFile();
            try {
                datum.clearRating();
            }
            catch (Throwable t) {
                browser.notifyError(t.getMessage());
                System.err.println(
                    "Couldn't clear rating on " + file.getAbsolutePath() + ": "
                );
                t.printStackTrace();
            }
        }
    }
}
