/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.ui.ActivityMeter;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.image.RenderedImage;

/**
 * This is a Document editor with all controls disabled.  Whereas active
 * editors are disposed through Document.dispose(), disabled editors have no
 * Documents and so they must be disposed explicitly.  These are vended from
 * Document.createDisabledEditor().
 */
public class DisabledEditor extends Editor {

    public interface Listener {
        void imageClicked(Object key);
    }

    private DisabledImages images;
    private ActivityMeter imagePane;

    // The most recently added image key, which the browser uses sometimes
    // to pick an image to automatically open for editing.
    private Object lastKey;

    DisabledEditor(Listener listener) {
        images = new DisabledImages(listener);
        imagePane = new ActivityMeter(images);

        images.setBorder(LightZoneSkin.getImageBorder());
    }

    /**
     * Though this Editor can not edit, it can still render images.
     */
    public void addImage(Object key, RenderedImage image) {
        images.addImage(key, image);
        images.validate();
        lastKey = key;
    }

    public void updateImage(Object key, RenderedImage image) {
        images.updateImage(key, image);
        images.validate();
    }

    public void removeImages() {
        images.removeAllImages();
        images.repaint();
        lastKey = null;
    }

    public void setImage(Object key, RenderedImage image) {
        removeImages();
        addImage(key, image);
    }

    public boolean hasImage(Object key) {
        return images.hasKey(key);
    }

    /**
     * The most recent key reference passed to addImage(), or null if this key
     * has been removed by removeImages().
     */
    public Object getLastKey() {
        return lastKey;
    }

    /**
     * The Image JComponent shows whatever images have been added in addImage().
     */
    public JComponent getImage() {
        return imagePane;
    }

    /**
     * Add a "Loading..." indication in the ActivityMeter, to show that
     * something is about to happen.
     */
    public void showWait(String text) {
        imagePane.showWait(text);
    }

    /**
     * Remove a "Loading..." indication in the ActivityMeter.
     */
    public void hideWait() {
        imagePane.hideWait();
    }

    /**
     * Update the message shown when there are no preview images.
     */
    public void setDisabledText(String text) {
        images.setDisabledText(text);
    }

    /**
     * Expose the dispose() method from the base class.  To avoid leaks, this
     * method must be called when this DisabledEditor is no longer in use.
     */
    public void dispose() {
        super.dispose();
    }
}
