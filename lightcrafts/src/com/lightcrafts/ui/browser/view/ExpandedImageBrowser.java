/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageDatumType;
import com.lightcrafts.ui.browser.model.ImageGroup;
import com.lightcrafts.ui.browser.model.ImageList;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.utils.awt.geom.HiDpi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * An AbstractImageBrowser where the size of images is fixed, images are
 * displayed in a grid determined by the component width, and extra gaps in
 * the layout are allowed.
 * <p>
 * This makes a browser that has a variable number of rows and columns, and
 * works well with vertical scrolling and horizontal resizing.
 */
public class ExpandedImageBrowser extends AbstractImageBrowser {

    public ExpandedImageBrowser(ImageList list) {
        super(list);
        addComponentListener(
                new ComponentListener() {
                    public void componentResized(ComponentEvent e) {

                    }
                    public void componentMoved(ComponentEvent e) {

                    }
                    public void componentShown(ComponentEvent e) {
                        System.out.println("Browser Shown");
                    }
                    public void componentHidden(ComponentEvent e) {
                        System.out.println("Browser Hidden");
                        justShown = true;
                    }
                }
        );
    }

    protected void paintComponent(Graphics graphics) {
        if (justShown) {
            if (!paintTimer.isRunning())
                paintTimer.start();
            return;
        }

        if (! isWidthInitialized) {
            // Only paint if the component size has been initialized.  Layout
            // jumps are typical the first time this component is displayed,
            // because the preferred height depends on the component width.
            return;
        }
        Graphics2D g = (Graphics2D) graphics;

        // Figure out which ImageDatums fall within the clip bounds.
        final Rectangle clip0 = g.getClipBounds();
        List<ImageDatum> datums = getAllImageData();
        int[] indices = getIndices(datums.size(), clip0);

        HiDpi.resetTransformScaleOf(g);
        final Rectangle clip = g.getClipBounds();

        // Set up context for the ImageGroup highlights.
        Color oldColor = g.getColor();
        g.setColor(LightZoneSkin.Colors.BrowserGroupColor);

        // Iterate backwards through indices, so repaints get enqueued
        // in a visually pleasing order.
        for (int i=indices.length-1; i>=0; i--) {
            int index = indices[i];
            if (index < 0) {
                continue;
            }
            ImageDatum datum = datums.get(index);
            if (datum == null) {
                // A race; the image disappeared during painting.
                continue;
            }
            RenderedImage image = datum.getImage(this);

            // This queue prevents GC of recently painted images:
            recentImages.add(image);

            final Rectangle rect = HiDpi.imageSpaceRectFrom(getBounds(index));
            g.setClip(clip.intersection(rect));

            // If this ImageDatum is a member of a nontrivial ImageGroup,
            // then render a background highlight.
            if (isGroupStart(datums, index)) {
                // paint the start-group
                Shape highlight = new RoundRectangle2D.Double(
                    rect.x + 4, rect.y + 4,
                    2 * rect.width - 8, rect.height - 8,
                    rect.width / 8, rect.height / 8
                );
                g.fill(highlight);
            }
            else if (isGroupEnd(datums, index)) {
                // paint the end-group
                Shape highlight = new RoundRectangle2D.Double(
                    rect.x - rect.width + 4, rect.y + 4,
                    2 * rect.width - 8, rect.height - 8,
                    rect.width / 8, rect.height / 8
                );
                g.fill(highlight);
            }
            else if (datum.getGroup().isNonTrivial()) {
                // paint the mid-group
                Shape highlight = new Rectangle(
                    rect.x, rect.y + 4, rect.width, rect.height - 8
                );
                g.fill(highlight);
            }
            File file = datum.getFile();
            String label = file.getName();
            ImageDatumType type = datum.getType();
            String tag = type.toString();
            ImageMetadata meta = datum.getMetadata(true);
            int rating = meta.getRating();
            boolean selected = selection.isSelected(datum);
            renderer.paint(g, image, label, tag, rating, rect, selected);
        }
        g.setColor(oldColor);
        g.setClip(clip);

        // The control is drawn as an overlay.
        if (controller.isEnabled()) {
            Rectangle ctrlRect = controller.getRect();
            if (ctrlRect != null) {
                if (ctrlRect.intersects(clip)) {
                    controller.paint(g);
                }
            }
        }
    }

    /**
     * Get the ImageDatums rendered in this AbstractImageBrowser: all the
     * ImageDatums in the ImageList.
     */
    ArrayList<ImageDatum> getAllImageData() {
        return list.getAllImageData();
    }

    private boolean isGroupStart(List<ImageDatum> datums, int index) {
        ImageDatum datum = datums.get(index);
        ImageGroup group = datum.getGroup();

        if (! group.isNonTrivial()) {
            return false;
        }
        if (index == 0) {
            return true;
        }
        ImageDatum prev = datums.get(index - 1);
        return group != prev.getGroup();
    }

    private boolean isGroupEnd(List<ImageDatum> datums, int index) {
        ImageDatum datum = datums.get(index);
        ImageGroup group = datum.getGroup();

        if (! group.isNonTrivial()) {
            return false;
        }
        if (index == datums.size() - 1) {
            return true;
        }
        ImageDatum next = datums.get(index + 1);
        return group != next.getGroup();
    }
}
