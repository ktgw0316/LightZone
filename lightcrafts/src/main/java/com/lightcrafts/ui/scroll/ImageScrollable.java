/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.scroll;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.*;

/** An ordinary JPanel that just implements a scrolling policy suitable for
  * navigating images.  Also uses the Scrollable methods to ensure centering
  * of the image in case its preferred size is smaller than the
  * size of this container.
  * <p>
  * In the containment hierarchy of DocPanel, this class provides the added
  * fix of preserving small clips generated during scrolling.  Without an
  * intermediate JPanel between JLayeredPane and RegionOverlay, for some
  * reason, these clips get expanded to the viewport bounds.
  */

class ImageScrollable extends JPanel implements Scrollable {

    // Fractions of the viewport size to scroll by:
    private final static double UnitFraction = .01;
    private final static double BlockFraction = 1.;

    private JComponent image;

    private final Color backgroundColor = LightZoneSkin.Colors.EditorBackground;

    ImageScrollable(JComponent image) {
        this.image = image;
        setLayout(null);
        setBackground(backgroundColor);
        add(image);
    }

    public Dimension getPreferredSize() {
        return image.getPreferredSize();
    }

    public Dimension getPreferredScrollableViewportSize() {
        return image.getPreferredSize();
    }

    public int getScrollableUnitIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        return getIncrement(visibleRect, orientation, UnitFraction);
    }

    public int getScrollableBlockIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        return getIncrement(visibleRect, orientation, BlockFraction);
    }

    public boolean getScrollableTracksViewportWidth() {
        JViewport viewport = (JViewport) getParent();
        Rectangle visible = viewport.getVisibleRect();
        // If the visible rect is wider than the image, then stretch us:
        return visible.width > image.getPreferredSize().width;
    }

    public boolean getScrollableTracksViewportHeight() {
        JViewport viewport = (JViewport) getParent();
        Rectangle visible = viewport.getVisibleRect();
        // If the visible rect is taller than the image, then stretch us:
        return visible.height > image.getPreferredSize().height;
    }

    private int getIncrement(Rectangle visibleRect, int orientation, double x) {
        switch (orientation) {
            case SwingConstants.HORIZONTAL:
                return (int) Math.round(visibleRect.width * x);
            case SwingConstants.VERTICAL:
                return (int) Math.round(visibleRect.height* x);
            default:
                return 0;
        }
    }

    public void doLayout() {
        Dimension size = getSize();
        Dimension imageSize = image.getPreferredSize();

        // If we are bigger than the image, then center the image.
        // Otherwise, fit the image to our bounds.

        int x, y, w, h;

        if (size.width >= imageSize.width) {
            x = (size.width - imageSize.width) / 2;
            w = imageSize.width;
        }
        else {
            x = 0;
            w = size.width;
        }
        if (size.height >= imageSize.height) {
            y = (size.height - imageSize.height) / 2;
            h = imageSize.height;
        }
        else {
            y = 0;
            h = size.height;
        }
        image.setBounds(x, y, w, h);
    }
}
