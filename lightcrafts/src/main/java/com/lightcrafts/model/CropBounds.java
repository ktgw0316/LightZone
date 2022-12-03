/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.model;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

/**
 * A CropBounds is an immutable rectangular shape which may be at an
 * arbitrary angle.  It's used in <code>Engine.setCropBounds()</code>.  It
 * is always interpreted in the original image coordinates.
 * <p>
 * A special case is a CropBounds that defines an angle without a rectangle.
 * Such a CropBounds indicates to the Engine that the image should be
 * rotated, without specifying how the image bounds should update.
 */

@EqualsAndHashCode
public class CropBounds implements Cloneable {

    // The counter-clockwise angle to rotate the image:
    @Accessors(chain=true) @Getter @Setter
    private double angle;

    // True if the image is flipped:
    @Getter
    private boolean flippedHorizontally = false;
    @Getter
    private boolean flippedVertically = false;

    // Location of the center of the cropped image, or null if rotate-only:
    private Point2D center;

    // Width and height of the cropped image, or zero if rotate-only:
    @Getter
    private double width;
    @Getter
    private double height;

    @Override
    public CropBounds clone() {
        CropBounds clone = null;
        try {
            clone = (CropBounds) super.clone();
            clone.center = (center != null) ? (Point2D) center.clone() : null;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return clone;
    }

    /**
     * Make a new default CropBounds that represents no rotation or cropping.
     */
    public CropBounds() {
        this(0);
    }

    /**
     * Make a new CropBounds defining a counter-clockwise rotation of the
     * image by the given angle, measured in radians.
     */
    public CropBounds(double angle) {
        this.angle = angle;
    }

    /**
     * Make a new CropBounds representing cropping with no rotation.
     */
    public CropBounds(Rectangle2D rect) {
        this(rect, 0);
    }

    /**
     * Make a new CropBounds by taking the given rectangle and rotating it
     * about its center, clockwise relative to the image, by the given angle.
     * (I.e., the rotation is defined so that the image will appear rotated
     * counter-clockwise when the crop rectangle is oriented.)
     */
    public CropBounds(Rectangle2D rect, double angle) {
        this(
            rect,
            AffineTransform.getRotateInstance(
                -angle,
                rect.getX() + rect.getWidth() / 2,
                rect.getY() + rect.getHeight() / 2
            )
        );
    }

    /** Make the CropBounds from a rectangle given in screen coordinates.
      * @param screenRect The crop rectangle, in screen coordinates.
      * @param xform The current AffineTransform, from image coordinates to
      * screen coordinates, as returned from Engine.getTransform().
      */
    public CropBounds(Rectangle2D screenRect, AffineTransform xform) {
        val ul = new Point2D.Double(
            screenRect.getX(), screenRect.getY()
        );
        val ll = new Point2D.Double(
            screenRect.getX(), screenRect.getY() + screenRect.getHeight()
        );
        val ur = new Point2D.Double(
            screenRect.getX() + screenRect.getWidth(), screenRect.getY()
        );
        if (xform != null) {
            try {
                xform.inverseTransform(ul, ul);
                xform.inverseTransform(ll, ll);
                xform.inverseTransform(ur, ur);
            } catch (NoninvertibleTransformException e) {
                // Engine transforms must always be invertible.
                throw new RuntimeException("Couldn't infer crop bounds", e);
            }
        }

        center = new Point2D.Double(
            (ur.getX() + ll.getX()) / 2, (ur.getY() + ll.getY()) / 2
        );
        val topDx = ur.getX() - ul.getX();
        val topDy = ur.getY() - ul.getY();

        val leftDx = ll.getX() - ul.getX();
        val leftDy = ll.getY() - ul.getY();

        angle = Math.atan2(topDy, topDx);
        width = Math.sqrt(topDx * topDx + topDy * topDy);
        height = Math.sqrt(leftDx * leftDx + leftDy * leftDy);
    }

    /**
     * Make a CropBounds cloned from the given CropBounds but with the
     * new rectangle rotated clockwise about the given CropBounds' center to
     * the given angle.  (The angle parameter is the clockwise value for the
     * rectangle, corresponding to a counter-clockwise angle for the image.)
     */
    public CropBounds(CropBounds bounds, double angle) {
        this(bounds.getCenter(), bounds.getWidth(), bounds.getHeight(), angle);
    }

    /**
     * Construct a CropBounds explicitly from a center, dimensions, and an
     * angle (clockwise value for the rectangle, corresponding to a counter-
     * clockwise angle for the image).
     */
    public CropBounds(
        Point2D center, double width, double height, double angle
    ) {
        this.center = (center != null) ? (Point2D) center.clone() : null;
        this.width = width;
        this.height = height;
        this.angle = angle;
    }

    /**
     * Transform a CropBounds by applying an AffineTransform to it.  This only
     * works if the AffineTransform has no shear.  (Shear would transform
     * a CropBounds into a parallelogram.)
     */
    public static CropBounds transform(
        AffineTransform xform, CropBounds oldBounds
    ) {
        final CropBounds newBounds;
        if (oldBounds.isAngleOnly()) {
            val angle = oldBounds.getAngle();

            val origin = new Point2D.Double(0, 0);
            val unit = new Point2D.Double(Math.cos(angle), Math.sin(angle));

            xform.transform(origin, origin);
            xform.transform(unit, unit);

            val dx = unit.getX() - origin.getX();
            val dy = unit.getY() - origin.getY();

            newBounds = new CropBounds(Math.atan2(dy, dx));
        }
        else {
            val ul = oldBounds.getUpperLeft();
            val ur = oldBounds.getUpperRight();
            val ll = oldBounds.getLowerLeft();
            val lr = oldBounds.getLowerRight();

            xform.transform(ul, ul);
            xform.transform(ur, ur);
            xform.transform(ll, ll);
            xform.transform(lr, lr);

            newBounds = new CropBounds(ul, ur, ll, lr);
        }
        val hFlip = oldBounds.isFlippedHorizontally();
        val vFlip = oldBounds.isFlippedVertically();
        if (hFlip || vFlip) {
            newBounds.flip(hFlip, vFlip);
        }
        return newBounds;
    }

    public boolean isAngleOnly() {
        return (center == null);
    }

    public Point2D getCenter() {
        return isAngleOnly() ? null : (Point2D) center.clone();
    }

    public CropBounds(Point2D ul, Point2D ur, Point2D ll, Point2D lr) {
        val topDx = ur.getX() - ul.getX();
        val topDy = ur.getY() - ul.getY();
//        val bottomDx = lr.getX() - ll.getX();
//        val bottomDy = lr.getY() - ll.getY();

        val leftDx = ll.getX() - ul.getX();
        val leftDy = ll.getY() - ul.getY();
//        val rightDx = lr.getX() - ur.getX();
//        val rightDy = lr.getY() - ur.getY();

        angle = Math.atan2(topDy, topDx);
//        angle = Math.atan2(bottomDy, bottomDx);
//        angle = Math.atan2(rightDx, rightDy);
//        angle = Math.atan2(leftDx, leftDy);

        width = Math.sqrt(topDx * topDx + topDy * topDy);
//        width = Math.sqrt(bottomDx * bottomDx + bottomDy * bottomDy);

        height = Math.sqrt(leftDx * leftDx + leftDy * leftDy);
//        height = Math.sqrt(rightDx * rightDx + rightDy * rightDy);

        val x = (ul.getX() + lr.getX()) / 2;
        val y = (ul.getY() + lr.getY()) / 2;
        center = new Point2D.Double(x, y);
    }

    public Point2D getUpperLeft() {
        val p = new Point2D.Double(
            center.getX() - width / 2, center.getY() - height / 2
        );
        val xform = AffineTransform.getRotateInstance(
            angle, center.getX(), center.getY()
        );
        xform.transform(p, p);
        return p;
    }

    public Point2D getUpperRight() {
        val p = new Point2D.Double(
            center.getX() + width / 2, center.getY() - height / 2
        );
        val xform = AffineTransform.getRotateInstance(
            angle, center.getX(), center.getY()
        );
        xform.transform(p, p);
        return p;
    }

    public Point2D getLowerLeft() {
        val p = new Point2D.Double(
            center.getX() - width / 2, center.getY() + height / 2
        );
        val xform = AffineTransform.getRotateInstance(
            angle, center.getX(), center.getY()
        );
        xform.transform(p, p);
        return p;
    }

    public Point2D getLowerRight() {
        val p = new Point2D.Double(
            center.getX() + width / 2, center.getY() + height / 2
        );
        val xform = AffineTransform.getRotateInstance(
            angle, center.getX(), center.getY()
        );
        xform.transform(p, p);
        return p;
    }

    public CropBounds createInvertedAspect() {
        return new CropBounds(center, height, width, angle);
    }

    public Dimension getDimensionToFit(Dimension bounds) {
        if (width <= 0 || height <= 0) {
            return new Dimension(bounds.width, bounds.height);
        }

        // Fit longer sides
        double scale = (width > height)
                ? bounds.width / width
                : bounds.height / height;
        int newWidth = (int) (scale * width);
        int newHeight = (int) (scale * height);

        // Fit shorter sides only when the longer sides gave a wrong dimension
        if (newWidth > bounds.width || newHeight > bounds.height) {
            scale = (width < height)
                    ? bounds.width / width
                    : bounds.height / height;
            newWidth = (int) (scale * width);
            newHeight = (int) (scale * height);
        }

        return new Dimension(newWidth, newHeight);
    }

    public CropBounds flip(boolean horizontal, boolean vertical) {
        if (horizontal)
            flippedHorizontally = !flippedHorizontally;
        if (vertical)
            flippedVertically = !flippedVertically;
        return this;
    }
}
