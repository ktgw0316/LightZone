/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.ui.browser.model.ImageDatum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.lightcrafts.ui.browser.view.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Actions for setting the rating on an image in a browser, including actions
 * that also advance the browser's selection state, with accelerator key
 * properties.
 */
class RatingActions {

    private static final Logger logger = LoggerFactory.getLogger(RatingActions.class);

    static List<SelectionAction> createAllActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        ArrayList<SelectionAction> actions = new ArrayList<>(createRatingActions(browser, dynamic));
        // actions.addAll(createRatingAdvanceActions(browser, dynamic));
        actions.add(createClearRatingAction(browser, dynamic));
        // actions.add(createClearRatingAdvanceAction(browser, dynamic));
        return actions;
    }

    static SelectionAction createClearRatingAction(
        final AbstractImageBrowser browser, boolean dynamic
    ) {
        return new SelectionAction(
            LOCALE.get("ClearRatingAction"),
            browser,
            KeyStroke.getKeyStroke('0', 0),
            dynamic,
            true
        ) {
            @Override
            protected SelectionAction clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                clearRating(browser);
            }
        };
    }

    static List<SelectionAction> createRatingActions(
        AbstractImageBrowser browser, boolean dynamic
    ) {
        List<SelectionAction> actions = new ArrayList<>();
        for (int rating=1; rating<=5; rating++) {
            SelectionAction action =
                createRatingAction(browser, rating, dynamic);
            actions.add(action);
        }
        return actions;
    }

    private static String createStars(int rating) {
        return "★".repeat(Math.max(0, rating));
    }

    static SelectionAction createRatingAction(
        final AbstractImageBrowser browser, final int rating, boolean dynamic
    ) {
        String stars = createStars(rating);
        return new SelectionAction(
            stars, // LOCALE.get("RateAction", stars),
            browser,
            KeyStroke.getKeyStroke('0' + rating, 0),
            dynamic,
            true
        ) {
            @Override
            protected SelectionAction clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException();
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                setRating(browser, rating);
            }
        };
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
            } catch (BadImageFileException | UnknownImageTypeException | IOException e) {
                browser.notifyError(e.getMessage());
                File file = datum.getFile();
                logger.warn("Couldn't set rating on {}", file.getAbsolutePath(), e);
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
        catch (BadImageFileException | UnknownImageTypeException | IOException e) {
            browser.notifyError(e.getMessage());
            File file = datum.getFile();
            logger.warn("Couldn't set rating on {}", file.getAbsolutePath(), e);
            return false;
        }
    }

    static boolean clearRating(ImageDatum datum, AbstractImageBrowser browser) {
        try {
            datum.clearRating();
            return true;
        }
        catch (BadImageFileException | UnknownImageTypeException | IOException e) {
            browser.notifyError(e.getMessage());
            File file = datum.getFile();
            logger.warn("Couldn't clear rating on {}", file.getAbsolutePath(), e);
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
            catch (BadImageFileException | UnknownImageTypeException | IOException e) {
                browser.notifyError(e.getMessage());
                logger.warn("Couldn't clear rating on {}", file.getAbsolutePath(), e);
            }
        }
    }
}
