/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.model;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A CropBounds is an immutable rectangular shape which may be at an
 * arbitrary angle.  It's used in <code>Engine.setCropBounds()</code>.  It
 * is always interpreted in the original image coordinates.
 * <p>
 * A special case is a CropBounds that defines an angle without a rectangle.
 * Such a CropBounds indicates to the Engine that the image should be
 * rotated, without specifying how the image bounds should update.
 */

public class CropBounds {

    // The counter-clockwise angle to rotate the image:
    double angle;

    // Location of the center of the cropped image, or null if rotate-only:
    Point2D center;

    // Width and height of the cropped image, or zero if rotate-only:
    double width;
    double height;

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CropBounds)) return false;

        final CropBounds cropBounds = (CropBounds) o;

        if (angle != cropBounds.angle) return false;
        if (height != cropBounds.height) return false;
        if (width != cropBounds.width) return false;
        if (center != null ? !center.equals(cropBounds.center) : cropBounds.center != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        long temp;
        temp = angle != +0.0d ? Double.doubleToLongBits(angle) : 0l;
        result = (int) (temp ^ (temp >>> 32));
        result = 29 * result + (center != null ? center.hashCode() : 0);
        temp = width != +0.0d ? Double.doubleToLongBits(width) : 0l;
        result = 29 * result + (int) (temp ^ (temp >>> 32));
        temp = height != +0.0d ? Double.doubleToLongBits(height) : 0l;
        result = 29 * result + (int) (temp ^ (temp >>> 32));
        return result;
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
        Point2D ul = new Point2D.Double(
            screenRect.getX(), screenRect.getY()
        );
        Point2D ll = new Point2D.Double(
            screenRect.getX(), screenRect.getY() + screenRect.getHeight()
        );
        Point2D ur = new Point2D.Double(
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
        double topDx = ur.getX() - ul.getX();
        double topDy = ur.getY() - ul.getY();

        double leftDx = ll.getX() - ul.getX();
        double leftDy = ll.getY() - ul.getY();

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
        if (oldBounds.isAngleOnly()) {
            double angle = oldBounds.getAngle();

            Point2D origin = new Point2D.Double(0, 0);
            Point2D unit = new Point2D.Double(Math.cos(angle), Math.sin(angle));

            xform.transform(origin, origin);
            xform.transform(unit, unit);

            double dx = unit.getX() - origin.getX();
            double dy = unit.getY() - origin.getY();
            angle = Math.atan2(dy, dx);

            return new CropBounds(angle);
        }
        else {
            Point2D ul = oldBounds.getUpperLeft();
            Point2D ur = oldBounds.getUpperRight();
            Point2D ll = oldBounds.getLowerLeft();
            Point2D lr = oldBounds.getLowerRight();

            xform.transform(ul, ul);
            xform.transform(ur, ur);
            xform.transform(ll, ll);
            xform.transform(lr, lr);

            return new CropBounds(ul, ur, ll, lr);
        }
    }

    /**
     * The counter-clockwise angle to rotate the image by.
     */
    public double getAngle() {
        return angle;
    }

    public boolean isAngleOnly() {
        return (center == null);
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Point2D getCenter() {
        if (center != null) {
            return (Point2D) center.clone();
        }
        else {
            return null;
        }
    }

    public CropBounds(Point2D ul, Point2D ur, Point2D ll, Point2D lr) {        
        double topDx = ur.getX() - ul.getX();
        double topDy = ur.getY() - ul.getY();
//        double bottomDx = lr.getX() - ll.getX();
//        double bottomDy = lr.getY() - ll.getY();

        double leftDx = ll.getX() - ul.getX();
        double leftDy = ll.getY() - ul.getY();
//        double rightDx = lr.getX() - ur.getX();
//        double rightDy = lr.getY() - ur.getY();

        angle = Math.atan2(topDy, topDx);
//        angle = Math.atan2(bottomDy, bottomDx);
//        angle = Math.atan2(rightDx, rightDy);
//        angle = Math.atan2(leftDx, leftDy);

        width = Math.sqrt(topDx * topDx + topDy * topDy);
//        width = Math.sqrt(bottomDx * bottomDx + bottomDy * bottomDy);

        height = Math.sqrt(leftDx * leftDx + leftDy * leftDy);
//        height = Math.sqrt(rightDx * rightDx + rightDy * rightDy);

        double x = (ul.getX() + lr.getX()) / 2;
        double y = (ul.getY() + lr.getY()) / 2;
        center = new Point2D.Double(x, y);
    }

    public Point2D getUpperLeft() {
        Point2D p = new Point2D.Double(
            center.getX() - width / 2, center.getY() - height / 2
        );
        AffineTransform xform = AffineTransform.getRotateInstance(
            angle, center.getX(), center.getY()
        );
        xform.transform(p, p);
        return p;
    }

    public Point2D getUpperRight() {
        Point2D p = new Point2D.Double(
            center.getX() + width / 2, center.getY() - height / 2
        );
        AffineTransform xform = AffineTransform.getRotateInstance(
            angle, center.getX(), center.getY()
        );
        xform.transform(p, p);
        return p;
    }

    public Point2D getLowerLeft() {
        Point2D p = new Point2D.Double(
            center.getX() - width / 2, center.getY() + height / 2

        );
        AffineTransform xform = AffineTransform.getRotateInstance(
            angle, center.getX(), center.getY()
        );
        xform.transform(p, p);
        return p;
    }

    public Point2D getLowerRight() {
        Point2D p = new Point2D.Double(
            center.getX() + width / 2, center.getY() + height / 2
        );
        AffineTransform xform = AffineTransform.getRotateInstance(
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
}
