/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageGroup;
import com.lightcrafts.ui.browser.model.ImageList;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.RoundRectangle2D;
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
                    @Override
                    public void componentResized(ComponentEvent e) {

                    }
                    @Override
                    public void componentMoved(ComponentEvent e) {

                    }
                    @Override
                    public void componentShown(ComponentEvent e) {
                        System.out.println("Browser Shown");
                    }
                    @Override
                    public void componentHidden(ComponentEvent e) {
                        System.out.println("Browser Hidden");
                        justShown = true;
                    }
                }
        );
    }

    @Override
    protected void renderImageGroup(Graphics2D g, List<ImageDatum> data, int index, ImageDatum datum, Rectangle rect) {
        // If this ImageDatum is a member of a nontrivial ImageGroup,
        // then render a background highlight.
        final ImageGroup group = datum.getGroup();
        final Shape highlight;
        if (group.isGroupStart(data, index)) {
            // paint the start-group
            highlight = new RoundRectangle2D.Double(
                    rect.x + 4, rect.y + 4,
                    2 * rect.width - 8, rect.height - 8,
                    rect.width / 8.0, rect.height / 8.0);
        } else if (group.isGroupEnd(data, index)) {
            // paint the end-group
            highlight = new RoundRectangle2D.Double(
                    rect.x - rect.width + 4, rect.y + 4,
                    2 * rect.width - 8, rect.height - 8,
                    rect.width / 8.0, rect.height / 8.0);
        } else if (group.isNonTrivial()) {
            // paint the mid-group
            highlight = new Rectangle(rect.x, rect.y + 4, rect.width, rect.height - 8);
        } else {
            // Nothing to paint
            return;
        }
        g.fill(highlight);
    }

    /**
     * Get the ImageDatums rendered in this AbstractImageBrowser: all the
     * ImageDatums in the ImageList.
     */
    @Override
    ArrayList<ImageDatum> getAllImageData() {
        return list.getAllImageData();
    }
}
