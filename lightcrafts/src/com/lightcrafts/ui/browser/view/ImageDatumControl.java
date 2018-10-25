/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.utils.awt.geom.HiDpi;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

/**
 * An overlay control for ImageDatums.  It paints, it responds to mouse
 * events, and it alters image data.
 */
class ImageDatumControl {

    private static BufferedImage RotateLeftImage;
    private static BufferedImage RotateRightImage;

    private static BufferedImage RotateLeftHighlightImage;
    private static BufferedImage RotateRightHighlightImage;

    static {
        try {
            RotateLeftImage = ImageIO.read(
                ImageDatumControl.class.getResource(
                    "resources/rotLeftNormal.png"
                )
            );
            RotateRightImage = ImageIO.read(
                ImageDatumControl.class.getResource(
                    "resources/rotRightNormal.png"
                )
            );
            RotateLeftHighlightImage = ImageIO.read(
                ImageDatumControl.class.getResource(
                    "resources/rotLeftHighlight.png"
                )
            );
            RotateRightHighlightImage = ImageIO.read(
                ImageDatumControl.class.getResource(
                    "resources/rotRightHighlight.png"
                )
            );
        }
        catch (IOException e) {
            throw new RuntimeException(
                "Couldn't initialize ImageDatumControl", e
            );
        }
    }

    // The browser where we listen for mouse events and paint ourself
    private AbstractImageBrowser browser;

    // The star slider widget that handles the ratings
    private StarSlider stars;

    // The index of the ImageDatum in the browser where we currently apply
    // (or -1 when there is no current ImageDatum)
    private int index;

    // The current index may point to an ImageDatum that is not an original
    // image, in which case the ImageDatumControl does not respond to mouse
    // events.
    private boolean isEnabled;

    // Flags indicate when the mouse is over each affordance
    private boolean hoverLeft;
    private boolean hoverRight;

    ImageDatumControl(AbstractImageBrowser browser) {
        this.browser = browser;
        stars = new StarSlider(browser);
        index = -1;
    }

    boolean isEnabled() {
        return isEnabled;
    }

    void paint(Graphics2D g) {
        // All painting is relative to the rect:
        Rectangle rect = getRect();

        int x, y;
        AffineTransform xform;
        BufferedImage image;

        // The rotate-left image goes in the bottom left corner:
        x = rect.x;
        y = rect.y + rect.height - RotateLeftImage.getHeight();
        xform = AffineTransform.getTranslateInstance(x, y);
        image = hoverLeft ? RotateLeftHighlightImage : RotateLeftImage;
        g.drawRenderedImage(image, xform);

        // The rotate-right image goes in the bottom right corner:
        x = rect.x + rect.width - RotateRightImage.getWidth();
        y = rect.y + rect.height - RotateRightImage.getHeight();
        xform = AffineTransform.getTranslateInstance(x, y);
        image = hoverRight ? RotateRightHighlightImage : RotateRightImage;
        g.drawRenderedImage(image, xform);

//        stars.paint(g);
    }

    Rectangle getRect() {
        if (index >= 0) {
            Rectangle rect = HiDpi.imageSpaceRectFrom(browser.getBounds(index));
            return rect;
        }
        return null;
    }

    private Rectangle getRotateLeftRect() {
        Rectangle rect = getRect();
        if (rect != null) {
            int x = rect.x;
            int y = rect.y + rect.height - RotateLeftImage.getHeight();
            int w = RotateLeftImage.getWidth();
            int h = RotateLeftImage.getHeight();
            return new Rectangle(x, y, w, h);
        }
        return null;
    }

    private Rectangle getRotateRightRect() {
        Rectangle rect = getRect();
        if (rect != null) {
            int x = rect.x + rect.width - RotateRightImage.getWidth();
            int y = rect.y + rect.height - RotateRightImage.getHeight();
            int w = RotateRightImage.getWidth();
            int h = RotateRightImage.getHeight();
            return new Rectangle(x, y, w, h);
        }
        return null;
    }

    boolean isControllerEvent(MouseEvent event) {
        // Any kind of mouse motion may be relevant:
        if (event.getID() == MouseEvent.MOUSE_MOVED) {
            return true;
        }
        if (! isEnabled) {
            return false;
        }
        // If it's a press on an affordance, return true:
        if (event.getID() == MouseEvent.MOUSE_PRESSED ||
            event.getID() == MouseEvent.MOUSE_RELEASED ||
            event.getID() == MouseEvent.MOUSE_CLICKED
        ) {
            Point p = HiDpi.imageSpacePointFrom(event.getPoint());

            Rectangle leftRect = getRotateLeftRect();
            if (leftRect != null) {
                if (leftRect.contains(p)) {
                    return true;
                }
            }
            Rectangle rightRect = getRotateRightRect();
            if (rightRect != null) {
                if (rightRect.contains(p)) {
                    return true;
                }
            }
        }
//        if (stars.isStarsEvent(event)) {
//            return true;
//        }
        // Nothing else matters for this control:
        return false;
    }

    void handleEvent(MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_MOVED) {
            // Update the current ImageDatum to the current location:
            Point p = event.getPoint();
            int index = browser.getIndex(p);
            setIndex(index);
            if (index < 0) {
                return;
            }
        }
        if (! isEnabled) {
            return;
        }
//        if (stars.isStarsEvent(event)) {
//            stars.handleEvent(event);
//            Rectangle rect = stars.getStarsRect();
//            browser.repaint(rect);
//            return;
//        }
        if (event.getID() == MouseEvent.MOUSE_MOVED) {
            // Update highlights and schedule repaint of the affordance.
            final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
            Rectangle leftRect = getRotateLeftRect();
            boolean wasHoverLeft = hoverLeft;
            hoverLeft = (leftRect != null) && leftRect.contains(p);
            if ((hoverLeft && ! wasHoverLeft) ||
                (! hoverLeft && wasHoverLeft)) {
                browser.repaint(leftRect);
            }
            Rectangle rightRect = getRotateRightRect();
            boolean wasHoverRight = hoverRight;
            hoverRight = (rightRect != null) && rightRect.contains(p);
            if ((hoverRight && ! wasHoverRight) ||
                (! hoverRight && wasHoverRight)) {
                browser.repaint(rightRect);
            }
            boolean changed = stars.updateHighlighted(p);
            if (changed) {
                Rectangle starsRect = stars.getStarsRect();
                browser.repaint(starsRect);
            }
        }
        // Handle clicks on the rotate affordances
        if (event.getID() == MouseEvent.MOUSE_PRESSED) {
            ImageDatum datum = browser.getImageDatum(index);

            // Figure out which affordance, and update the datum.
            final Point p = HiDpi.imageSpacePointFrom(event.getPoint());

            Rectangle leftRect = getRotateLeftRect();
            if (leftRect != null) {
                if (leftRect.contains(p)) {
                    RotateActions.rotateLeft(datum, browser);

                    // Also rotate any other selected images, if the context
                    // datum is also selected:
                    Collection<ImageDatum> datums = browser.getSelectedDatums();
                    if (datums.contains(datum)) {
                        datums.remove(datum);
                        for (ImageDatum d : datums) {
                            RotateActions.rotateLeft(d, browser);
                        }
                    }
                }
            }
            Rectangle rightRect = getRotateRightRect();
            if (rightRect != null) {
                if (rightRect.contains(p)) {
                    RotateActions.rotateRight(datum, browser);

                    // Also rotate any other selected images, if the context
                    // datum is also selected:
                    Collection<ImageDatum> datums = browser.getSelectedDatums();
                    if (datums.contains(datum)) {
                        datums.remove(datum);
                        for (ImageDatum d : datums) {
                            RotateActions.rotateRight(d, browser);
                        }
                    }
                }
            }
        }
    }

    void setIndex(int newIndex) {
        int oldIndex = index;

        if (newIndex != oldIndex) {
            if (oldIndex >= 0) {
                Rectangle oldRect = browser.getBounds(oldIndex);
                browser.repaint(oldRect);
            }
            if (newIndex >= 0) {
                Rectangle newRect = browser.getBounds(newIndex);
                browser.repaint(newRect);
            }
            index = newIndex;
            isEnabled = true;
            if (index >= 0) {
                ImageDatum datum = browser.getImageDatum(index);
                if (datum != null) {
                    Rectangle rect = getRect();
                    stars.setup(datum, rect);
                    if (datum.getType().hasLznData()) {
                        isEnabled = false;
                    }
                }
            }
        }
    }
}
