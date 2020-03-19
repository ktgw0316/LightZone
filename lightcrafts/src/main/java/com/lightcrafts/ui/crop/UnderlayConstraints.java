/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import com.lightcrafts.model.CropBounds;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Collections;

/**
 * This class holds the geometrical logic that takes a CropBounds input by the
 * user and returns a similar CropBounds that respects the underlay constraint,
 * which is the constraint that requires that crop bounds to be contained
 * inside the image bounds.
 * <p>
 * There are six exposed methods:
 *
 *     translateToUnderlay()       constrain translation changes
 *
 *     sizeToUnderlay()            constrain rotation changes
 *
 *     adjustNorthToUnderlay()     constrain edge adjustments
 *     adjustSouthToUnderlay()
 *     adjustEastToUnderlay()
 *     adjustWestToUnderlay()
 *
 *     adjustNorthWithConstraint() constraint edge adjustments,
 *     adjustSouthWithConstraint()   preserving the aspect ratio
 *     adjustEastWithConstraint()
 *     adjustWestWithConstraint()
 *
 *     adjustNorthEastToUnderlay() constrain corner adjustment gestures,
 *     adjustNorthWestToUnderlay()   always preserving the aspect ratio
 *     adjustSouthEastToUnderlay()
 *     adjustSouthWestToUnderlay()
 *
 * The rest of this class is bookkeeping and geometrical utilities.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class UnderlayConstraints {

    /**
     * Translate the given crop so that it is contained within the underlay
     * bounds, if possible.  Return null if this is not possible.
     */
    static CropBounds translateToUnderlay(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        Rectangle2D oldRect = getCropAsShape(oldCrop).getBounds2D();
        CropBounds newCrop = oldCrop;
        if (oldRect.getX() < underlay.getMinX()) {
            double dx = - (oldRect.getX() - underlay.getMinX());
            double dy = 0;
            newCrop = translateCrop(newCrop, dx, dy);
        }
        if (oldRect.getY() < underlay.getMinY()) {
            double dx = 0;
            double dy = - (oldRect.getY() - underlay.getMinY());
            newCrop = translateCrop(newCrop, dx, dy);
        }
        if (oldRect.getX() + oldRect.getWidth() > underlay.getMaxX()) {
            double dx = - (
                oldRect.getX() + oldRect.getWidth() - underlay.getMaxX()
            );
            double dy = 0;
            newCrop = translateCrop(newCrop, dx, dy);
        }
        if (oldRect.getY() + oldRect.getHeight() > underlay.getMaxY()) {
            double dx = 0;
            double dy = - (
                oldRect.getY() + oldRect.getHeight() - underlay.getMaxY()
            );
            newCrop = translateCrop(newCrop, dx, dy);
        }
        if ((newCrop != null) && underlayContains(newCrop, underlay)) {
            return newCrop;
        }
        return null;
    }

    /**
     * Resize the given crop so that it is contained within the underlay
     * bounds by adjusting its width and height, taking care not to exceed
     * the given limiting width and height or to change the aspect ratio.
     */
    public static CropBounds sizeToUnderlay(
        CropBounds oldCrop,
        Rectangle2D underlay,
        double limitW, double limitH
    ) {
        CropBounds newCrop = new CropBounds(
            oldCrop.getCenter(), limitW, limitH, oldCrop.getAngle()
        );
        if (underlayContains(newCrop, underlay)) {
            return newCrop;
        }
        double ult = ultHeight(oldCrop, underlay);
        double ull = ullHeight(oldCrop, underlay);
        double ulb = ulbHeight(oldCrop, underlay);
        double ulr = ulrHeight(oldCrop, underlay);

        double urt = urtHeight(oldCrop, underlay);
        double url = urlHeight(oldCrop, underlay);
        double urb = urbHeight(oldCrop, underlay);
        double urr = urrHeight(oldCrop, underlay);

        double llt = lltHeight(oldCrop, underlay);
        double lll = lllHeight(oldCrop, underlay);
        double llb = llbHeight(oldCrop, underlay);
        double llr = llrHeight(oldCrop, underlay);

        double lrt = lrtHeight(oldCrop, underlay);
        double lrl = lrlHeight(oldCrop, underlay);
        double lrb = lrbHeight(oldCrop, underlay);
        double lrr = lrrHeight(oldCrop, underlay);

        double aspect = oldCrop.getWidth() / oldCrop.getHeight();

        double h = limitH;

        h = minIgnoreNegative(h, ult);
        h = minIgnoreNegative(h, ull);
        h = minIgnoreNegative(h, ulb);
        h = minIgnoreNegative(h, ulr);
        h = minIgnoreNegative(h, urt);
        h = minIgnoreNegative(h, url);
        h = minIgnoreNegative(h, urb);
        h = minIgnoreNegative(h, urr);
        h = minIgnoreNegative(h, llt);
        h = minIgnoreNegative(h, lll);
        h = minIgnoreNegative(h, llb);
        h = minIgnoreNegative(h, llr);
        h = minIgnoreNegative(h, lrt);
        h = minIgnoreNegative(h, lrl);
        h = minIgnoreNegative(h, lrb);
        h = minIgnoreNegative(h, lrr);

        double w = h * aspect;

        newCrop = new CropBounds(
            oldCrop.getCenter(), w, h, oldCrop.getAngle()
        );
        return newCrop;
    }

    /**
     * Adjust the north edge of the given crop so that it is contained within
     * the underlay bounds.
     */
    public static CropBounds adjustNorthToUnderlay(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Line2D top = getTopUnderlayLine(underlay);
        Line2D left = getLeftUnderlayLine(underlay);
        Line2D bottom = getBottomUnderlayLine(underlay);
        Line2D right = getRightUnderlayLine(underlay);

        Line2D north = getNorthCropLine(oldCrop);
        Line2D south = getSouthCropLine(oldCrop);
        Line2D east = getEastCropLine(oldCrop);
        Line2D west = getWestCropLine(oldCrop);

        Point2D et = getIntersection(east, top);
        Point2D el = getIntersection(east, left);
        Point2D eb = getIntersection(east, bottom);
        Point2D er = getIntersection(east, right);

        Point2D wt = getIntersection(west, top);
        Point2D wl = getIntersection(west, left);
        Point2D wb = getIntersection(west, bottom);
        Point2D wr = getIntersection(west, right);

        double height = oldCrop.getHeight();
        
        if (et != null) {
            height = minIgnoreNegative(height, south.ptLineDist(et));
        }
        if (el != null) {
            height = minIgnoreNegative(height, south.ptLineDist(el));
        }
        if (eb != null) {
            height = minIgnoreNegative(height, south.ptLineDist(eb));
        }
        if (er != null) {
            height = minIgnoreNegative(height, south.ptLineDist(er));
        }
        if (wt != null) {
            height = minIgnoreNegative(height, south.ptLineDist(wt));
        }
        if (wl != null) {
            height = minIgnoreNegative(height, south.ptLineDist(wl));
        }
        if (wb != null) {
            height = minIgnoreNegative(height, south.ptLineDist(wb));
        }
        if (wr != null) {
            height = minIgnoreNegative(height, south.ptLineDist(wr));
        }
        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = oldCrop.getWidth();
        double dh = height - oldCrop.getHeight();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() + (dh / 2) * Math.sin(angle),
            oldCenter.getY() - (dh / 2) * Math.cos(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    /**
     * Adjust the south edge of the given crop so that it is contained within
     * the underlay bounds.
     */
    public static CropBounds adjustSouthToUnderlay(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Line2D top = getTopUnderlayLine(underlay);
        Line2D left = getLeftUnderlayLine(underlay);
        Line2D bottom = getBottomUnderlayLine(underlay);
        Line2D right = getRightUnderlayLine(underlay);

        Line2D north = getNorthCropLine(oldCrop);
        Line2D south = getSouthCropLine(oldCrop);
        Line2D east = getEastCropLine(oldCrop);
        Line2D west = getWestCropLine(oldCrop);

        Point2D et = getIntersection(east, top);
        Point2D el = getIntersection(east, left);
        Point2D eb = getIntersection(east, bottom);
        Point2D er = getIntersection(east, right);

        Point2D wt = getIntersection(west, top);
        Point2D wl = getIntersection(west, left);
        Point2D wb = getIntersection(west, bottom);
        Point2D wr = getIntersection(west, right);

        double height = oldCrop.getHeight();

        if (et != null) {
            height = minIgnoreNegative(height, north.ptLineDist(et));
        }
        if (el != null) {
            height = minIgnoreNegative(height, north.ptLineDist(el));
        }
        if (eb != null) {
            height = minIgnoreNegative(height, north.ptLineDist(eb));
        }
        if (er != null) {
            height = minIgnoreNegative(height, north.ptLineDist(er));
        }
        if (wt != null) {
            height = minIgnoreNegative(height, north.ptLineDist(wt));
        }
        if (wl != null) {
            height = minIgnoreNegative(height, north.ptLineDist(wl));
        }
        if (wb != null) {
            height = minIgnoreNegative(height, north.ptLineDist(wb));
        }
        if (wr != null) {
            height = minIgnoreNegative(height, north.ptLineDist(wr));
        }
        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = oldCrop.getWidth();
        double dh = height - oldCrop.getHeight();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() - (dh / 2) * Math.sin(angle),
            oldCenter.getY() + (dh / 2) * Math.cos(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    /**
     * Adjust the east edge of the given crop so that it is contained within
     * the underlay bounds.
     */
    public static CropBounds adjustEastToUnderlay(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Line2D top = getTopUnderlayLine(underlay);
        Line2D left = getLeftUnderlayLine(underlay);
        Line2D bottom = getBottomUnderlayLine(underlay);
        Line2D right = getRightUnderlayLine(underlay);

        Line2D north = getNorthCropLine(oldCrop);
        Line2D south = getSouthCropLine(oldCrop);
        Line2D east = getEastCropLine(oldCrop);
        Line2D west = getWestCropLine(oldCrop);

        Point2D nt = getIntersection(north, top);
        Point2D nl = getIntersection(north, left);
        Point2D nb = getIntersection(north, bottom);
        Point2D nr = getIntersection(north, right);

        Point2D st = getIntersection(south, top);
        Point2D sl = getIntersection(south, left);
        Point2D sb = getIntersection(south, bottom);
        Point2D sr = getIntersection(south, right);

        double width = oldCrop.getWidth();

        if (nt != null) {
            width = minIgnoreNegative(width, west.ptLineDist(nt));
        }
        if (nl != null) {
            width = minIgnoreNegative(width, west.ptLineDist(nl));
        }
        if (nb != null) {
            width = minIgnoreNegative(width, west.ptLineDist(nb));
        }
        if (nr != null) {
            width = minIgnoreNegative(width, west.ptLineDist(nr));
        }
        if (st != null) {
            width = minIgnoreNegative(width, west.ptLineDist(st));
        }
        if (sl != null) {
            width = minIgnoreNegative(width, west.ptLineDist(sl));
        }
        if (sb != null) {
            width = minIgnoreNegative(width, west.ptLineDist(sb));
        }
        if (sr != null) {
            width = minIgnoreNegative(width, west.ptLineDist(sr));
        }
        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double height = oldCrop.getHeight();
        double dw = width - oldCrop.getWidth();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() + (dw / 2) * Math.cos(angle),
            oldCenter.getY() + (dw / 2) * Math.sin(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    /**
     * Adjust the west edge of the given crop so that it is contained within
     * the underlay bounds.
     */
    public static CropBounds adjustWestToUnderlay(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Line2D top = getTopUnderlayLine(underlay);
        Line2D left = getLeftUnderlayLine(underlay);
        Line2D bottom = getBottomUnderlayLine(underlay);
        Line2D right = getRightUnderlayLine(underlay);

        Line2D north = getNorthCropLine(oldCrop);
        Line2D south = getSouthCropLine(oldCrop);
        Line2D east = getEastCropLine(oldCrop);
        Line2D west = getWestCropLine(oldCrop);

        Point2D nt = getIntersection(north, top);
        Point2D nl = getIntersection(north, left);
        Point2D nb = getIntersection(north, bottom);
        Point2D nr = getIntersection(north, right);

        Point2D st = getIntersection(south, top);
        Point2D sl = getIntersection(south, left);
        Point2D sb = getIntersection(south, bottom);
        Point2D sr = getIntersection(south, right);

        double width = oldCrop.getWidth();

        if (nt != null) {
            width = minIgnoreNegative(width, east.ptLineDist(nt));
        }
        if (nl != null) {
            width = minIgnoreNegative(width, east.ptLineDist(nl));
        }
        if (nb != null) {
            width = minIgnoreNegative(width, east.ptLineDist(nb));
        }
        if (nr != null) {
            width = minIgnoreNegative(width, east.ptLineDist(nr));
        }
        if (st != null) {
            width = minIgnoreNegative(width, east.ptLineDist(st));
        }
        if (sl != null) {
            width = minIgnoreNegative(width, east.ptLineDist(sl));
        }
        if (sb != null) {
            width = minIgnoreNegative(width, east.ptLineDist(sb));
        }
        if (sr != null) {
            width = minIgnoreNegative(width, east.ptLineDist(sr));
        }
        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double height = oldCrop.getHeight();
        double dw = width - oldCrop.getWidth();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() - (dw / 2) * Math.cos(angle),
            oldCenter.getY() - (dw / 2) * Math.sin(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    public static CropBounds adjustNorthWithConstraint(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Line2D south = getSouthCropLine(oldCrop);
        Point2D midSouth = new Point2D.Double(
            (south.getP1().getX() + south.getP2().getX()) / 2,
            (south.getP1().getY() + south.getP2().getY()) / 2
        );
        Line2D neLine = new Line2D.Double(midSouth, oldCrop.getUpperRight());
        Line2D nwLine = new Line2D.Double(midSouth, oldCrop.getUpperLeft());
        Line2D seLine = new Line2D.Double(midSouth, oldCrop.getLowerRight());
        Line2D swLine = new Line2D.Double(midSouth, oldCrop.getLowerLeft());

        LinkedList<Line2D> lines = new LinkedList<Line2D>();
        lines.add(neLine);
        lines.add(nwLine);
        double oldDiagonalScale = midSouth.distance(oldCrop.getUpperLeft());
        double newDiagonalScale = getMinimumDistance(midSouth, lines, underlay);
        if (newDiagonalScale < 0) {
            newDiagonalScale = Double.MAX_VALUE;
        }
        lines.clear();
        lines.add(seLine);
        lines.add(swLine);
        double oldSouthScale = midSouth.distance(oldCrop.getLowerLeft());
        double newSouthScale = getMinimumDistance(midSouth, lines, underlay);
        if (newSouthScale < 0) {
            newSouthScale = Double.MAX_VALUE;
        }
        double scale = Math.min(
            newDiagonalScale / oldDiagonalScale, newSouthScale / oldSouthScale
        );
        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = scale * oldCrop.getWidth();
        double height = scale * oldCrop.getHeight();

        double dh = height - oldCrop.getHeight();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() + (dh / 2) * Math.sin(angle),
            oldCenter.getY() - (dh / 2) * Math.cos(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    public static CropBounds adjustSouthWithConstraint(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Line2D north = getNorthCropLine(oldCrop);
        Point2D midNorth = new Point2D.Double(
            (north.getP1().getX() + north.getP2().getX()) / 2,
            (north.getP1().getY() + north.getP2().getY()) / 2
        );
        Line2D neLine = new Line2D.Double(midNorth, oldCrop.getUpperRight());
        Line2D nwLine = new Line2D.Double(midNorth, oldCrop.getUpperLeft());
        Line2D seLine = new Line2D.Double(midNorth, oldCrop.getLowerRight());
        Line2D swLine = new Line2D.Double(midNorth, oldCrop.getLowerLeft());

        LinkedList<Line2D> lines = new LinkedList<Line2D>();
        lines.add(seLine);
        lines.add(swLine);
        double oldDiagonalScale = midNorth.distance(oldCrop.getLowerLeft());
        double newDiagonalScale = getMinimumDistance(midNorth, lines, underlay);
        if (newDiagonalScale < 0) {
            newDiagonalScale = Double.MAX_VALUE;
        }
        lines.clear();
        lines.add(neLine);
        lines.add(nwLine);
        double oldNorthScale = midNorth.distance(oldCrop.getUpperLeft());
        double newNorthScale = getMinimumDistance(midNorth, lines, underlay);
        if (newNorthScale < 0) {
            newNorthScale = Double.MAX_VALUE;
        }
        double scale = Math.min(
            newDiagonalScale / oldDiagonalScale, newNorthScale / oldNorthScale
        );
        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = scale * oldCrop.getWidth();
        double height = scale * oldCrop.getHeight();

        double dh = height - oldCrop.getHeight();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() - (dh / 2) * Math.sin(angle),
            oldCenter.getY() + (dh / 2) * Math.cos(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    public static CropBounds adjustEastWithConstraint(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Line2D west = getWestCropLine(oldCrop);
        Point2D midWest = new Point2D.Double(
            (west.getP1().getX() + west.getP2().getX()) / 2,
            (west.getP1().getY() + west.getP2().getY()) / 2
        );
        Line2D neLine = new Line2D.Double(midWest, oldCrop.getUpperRight());
        Line2D nwLine = new Line2D.Double(midWest, oldCrop.getUpperLeft());
        Line2D seLine = new Line2D.Double(midWest, oldCrop.getLowerRight());
        Line2D swLine = new Line2D.Double(midWest, oldCrop.getLowerLeft());

        LinkedList<Line2D> lines = new LinkedList<Line2D>();
        lines.add(neLine);
        lines.add(seLine);
        double oldDiagonalScale = midWest.distance(oldCrop.getUpperRight());
        double newDiagonalScale = getMinimumDistance(midWest, lines, underlay);
        if (newDiagonalScale < 0) {
            newDiagonalScale = Double.MAX_VALUE;
        }
        lines.clear();
        lines.add(nwLine);
        lines.add(swLine);
        double oldWestScale = midWest.distance(oldCrop.getUpperLeft());
        double newWestScale = getMinimumDistance(midWest, lines, underlay);
        if (newWestScale < 0) {
            newWestScale = Double.MAX_VALUE;
        }
        double scale = Math.min(
            newDiagonalScale / oldDiagonalScale, newWestScale / oldWestScale
        );
        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = scale * oldCrop.getWidth();
        double height = scale * oldCrop.getHeight();

        double dw = width - oldCrop.getWidth();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() + (dw / 2) * Math.cos(angle),
            oldCenter.getY() + (dw / 2) * Math.sin(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    public static CropBounds adjustWestWithConstraint(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Line2D east = getEastCropLine(oldCrop);
        Point2D midEast = new Point2D.Double(
            (east.getP1().getX() + east.getP2().getX()) / 2,
            (east.getP1().getY() + east.getP2().getY()) / 2
        );
        Line2D neLine = new Line2D.Double(midEast, oldCrop.getUpperRight());
        Line2D nwLine = new Line2D.Double(midEast, oldCrop.getUpperLeft());
        Line2D seLine = new Line2D.Double(midEast, oldCrop.getLowerRight());
        Line2D swLine = new Line2D.Double(midEast, oldCrop.getLowerLeft());

        LinkedList<Line2D> lines = new LinkedList<Line2D>();
        lines.add(nwLine);
        lines.add(swLine);
        double oldDiagonalScale = midEast.distance(oldCrop.getUpperLeft());
        double newDiagonalScale = getMinimumDistance(midEast, lines, underlay);
        if (newDiagonalScale < 0) {
            newDiagonalScale = Double.MAX_VALUE;
        }
        lines.clear();
        lines.add(neLine);
        lines.add(seLine);
        double oldEastScale = midEast.distance(oldCrop.getUpperRight());
        double newEastScale = getMinimumDistance(midEast, lines, underlay);
        if (newEastScale < 0) {
            newEastScale = Double.MAX_VALUE;
        }
        double scale = Math.min(
            newDiagonalScale / oldDiagonalScale, newEastScale / oldEastScale
        );
        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = scale * oldCrop.getWidth();
        double height = scale * oldCrop.getHeight();

        double dw = width - oldCrop.getWidth();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() - (dw / 2) * Math.cos(angle),
            oldCenter.getY() - (dw / 2) * Math.sin(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    public static CropBounds adjustNorthEastToUnderlay(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Point2D sw = oldCrop.getLowerLeft();
        Point2D ne = oldCrop.getUpperRight();
        Line2D diagonal = new Line2D.Double(sw, ne);

        double oldDiag = sw.distance(ne);
        double newDiag = getMinimumDistance(
            sw, Collections.singleton(diagonal), underlay
        );
        double diagScale = newDiag / oldDiag;

        Point2D se = oldCrop.getLowerRight();
        Line2D south = getSouthCropLine(oldCrop);

        double oldSouth = sw.distance(se);
        double newSouth = getMinimumDistance(
            sw, Collections.singleton(south), underlay
        );
        double southScale = newSouth / oldSouth;

        Point2D nw = oldCrop.getUpperLeft();
        Line2D west = getWestCropLine(oldCrop);

        double oldWest = sw.distance(nw);
        double newWest = getMinimumDistance(
            sw, Collections.singleton(west), underlay
        );
        double westScale = newWest / oldWest;
        
        double scale = minIgnoreNegative(1, diagScale);
        scale = minIgnoreNegative(scale, southScale);
        scale = minIgnoreNegative(scale, westScale);
        
        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = scale * oldCrop.getWidth();
        double height = scale * oldCrop.getHeight();

        double dw = width - oldCrop.getWidth();
        double dh = height - oldCrop.getHeight();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() + (dw / 2) * Math.cos(angle) +
                               (dh / 2) * Math.sin(angle),
            oldCenter.getY() - (dh / 2) * Math.cos(angle) +
                               (dw / 2) * Math.sin(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    public static CropBounds adjustNorthWestToUnderlay(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Point2D se = oldCrop.getLowerRight();
        Point2D nw = oldCrop.getUpperLeft();
        Line2D diagonal = new Line2D.Double(se, nw);

        double oldDiag = se.distance(nw);
        double newDiag = getMinimumDistance(
            se, Collections.singleton(diagonal), underlay
        );
        double diagScale = newDiag / oldDiag;

        Point2D sw = oldCrop.getLowerLeft();
        Line2D south = getSouthCropLine(oldCrop);

        double oldSouth = sw.distance(se);
        double newSouth = getMinimumDistance(
            se, Collections.singleton(south), underlay
        );
        double southScale = newSouth / oldSouth;

        Point2D ne = oldCrop.getUpperRight();
        Line2D east = getEastCropLine(oldCrop);

        double oldEast = se.distance(ne);
        double newEast = getMinimumDistance(
            se, Collections.singleton(east), underlay
        );
        double eastScale = newEast / oldEast;

        double scale = minIgnoreNegative(1, diagScale);
        scale = minIgnoreNegative(scale, southScale);
        scale = minIgnoreNegative(scale, eastScale);

        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = scale * oldCrop.getWidth();
        double height = scale * oldCrop.getHeight();

        double dw = width - oldCrop.getWidth();
        double dh = height - oldCrop.getHeight();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() - (dw / 2) * Math.cos(angle) +
                               (dh / 2) * Math.sin(angle),
            oldCenter.getY() - (dh / 2) * Math.cos(angle) -
                               (dw / 2) * Math.sin(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    public static CropBounds adjustSouthEastToUnderlay(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Point2D se = oldCrop.getLowerRight();
        Point2D nw = oldCrop.getUpperLeft();
        Line2D diagonal = new Line2D.Double(se, nw);

        double oldDiag = se.distance(nw);
        double newDiag = getMinimumDistance(
            nw, Collections.singleton(diagonal), underlay
        );
        double diagScale = newDiag / oldDiag;

        Point2D ne = oldCrop.getUpperRight();
        Line2D north = getNorthCropLine(oldCrop);

        double oldNorth = nw.distance(ne);
        double newNorth = getMinimumDistance(
            nw, Collections.singleton(north), underlay
        );
        double northScale = newNorth / oldNorth;

        Point2D sw = oldCrop.getLowerLeft();
        Line2D west = getWestCropLine(oldCrop);

        double oldWest = sw.distance(nw);
        double newWest = getMinimumDistance(
            nw, Collections.singleton(west), underlay
        );
        double westScale = newWest / oldWest;

        double scale = minIgnoreNegative(1, diagScale);
        scale = minIgnoreNegative(scale, northScale);
        scale = minIgnoreNegative(scale, westScale);

        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = scale * oldCrop.getWidth();
        double height = scale * oldCrop.getHeight();

        double dw = width - oldCrop.getWidth();
        double dh = height - oldCrop.getHeight();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() + (dw / 2) * Math.cos(angle) -
                               (dh / 2) * Math.sin(angle),
            oldCenter.getY() + (dh / 2) * Math.cos(angle) +
                               (dw / 2) * Math.sin(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    public static CropBounds adjustSouthWestToUnderlay(
        CropBounds oldCrop, Rectangle2D underlay
    ) {
        if (underlayContains(oldCrop, underlay)) {
            return oldCrop;
        }
        Point2D sw = oldCrop.getLowerLeft();
        Point2D ne = oldCrop.getUpperRight();
        Line2D diagonal = new Line2D.Double(sw, ne);

        double oldDiag = sw.distance(ne);
        double newDiag = getMinimumDistance(
            ne, Collections.singleton(diagonal), underlay
        );
        double diagScale = newDiag / oldDiag;

        Point2D nw = oldCrop.getUpperLeft();
        Line2D north = getNorthCropLine(oldCrop);

        double oldNorth = nw.distance(ne);
        double newNorth = getMinimumDistance(
            ne, Collections.singleton(north), underlay
        );
        double northScale = newNorth / oldNorth;

        Point2D se = oldCrop.getLowerRight();
        Line2D east = getEastCropLine(oldCrop);

        double oldEast = ne.distance(se);
        double newEast = getMinimumDistance(
            ne, Collections.singleton(east), underlay
        );
        double eastScale = newEast / oldEast;

        double scale = minIgnoreNegative(1, diagScale);
        scale = minIgnoreNegative(scale, northScale);
        scale = minIgnoreNegative(scale, eastScale);

        Point2D oldCenter = oldCrop.getCenter();
        double angle = oldCrop.getAngle();
        double width = scale * oldCrop.getWidth();
        double height = scale * oldCrop.getHeight();

        double dw = width - oldCrop.getWidth();
        double dh = height - oldCrop.getHeight();

        Point2D newCenter = new Point2D.Double(
            oldCenter.getX() - (dw / 2) * Math.cos(angle) -
                               (dh / 2) * Math.sin(angle),
            oldCenter.getY() + (dh / 2) * Math.cos(angle) -
                               (dw / 2) * Math.sin(angle)
        );
        CropBounds newCrop = new CropBounds(newCenter, width, height, angle);

        return newCrop;
    }

    // Get the least distance between the given point and any point of
    // intersection between any of the given lines and the underlay rectangle.
    // If none of the lines intersect the underlay, returns -1;
    private static double getMinimumDistance(
        Point2D p, Collection<Line2D> lines, Rectangle2D underlay
    ) {
        Line2D top = getTopUnderlayLine(underlay);
        Line2D left = getLeftUnderlayLine(underlay);
        Line2D bottom = getBottomUnderlayLine(underlay);
        Line2D right = getRightUnderlayLine(underlay);

        LinkedList<Point2D> intersections = new LinkedList<Point2D>();

        for (Line2D line : lines) {

            Point2D t = getIntersection(top, line);
            Point2D l = getIntersection(left, line);
            Point2D b = getIntersection(bottom, line);
            Point2D r = getIntersection(right, line);

            if (t != null) {
                intersections.add(t);
            }
            if (l != null) {
                intersections.add(l);
            }
            if (b != null) {
                intersections.add(b);
            }
            if (r != null) {
                intersections.add(r);
            }
        }
        if (intersections.isEmpty()) {
            return -1;
        }
        double distance = Double.MAX_VALUE;

        for (Point2D q : intersections) {
            distance = Math.min(distance, p.distance(q));
        }
        return distance;
    }

    private static Line2D getNorthCropLine(CropBounds crop) {
        return new Line2D.Double(crop.getUpperLeft(), crop.getUpperRight());
    }

    private static Line2D getSouthCropLine(CropBounds crop) {
        return new Line2D.Double(crop.getLowerRight(), crop.getLowerLeft());
    }

    private static Line2D getEastCropLine(CropBounds crop) {
        return new Line2D.Double(crop.getUpperRight(), crop.getLowerRight());
    }

    private static Line2D getWestCropLine(CropBounds crop) {
        return new Line2D.Double(crop.getLowerLeft(), crop.getUpperLeft());
    }

    private static Line2D getTopUnderlayLine(Rectangle2D underlay) {
        return new Line2D.Double(
            underlay.getMinX(), underlay.getMinY(),
            underlay.getMaxX(), underlay.getMinY()
        );
    }

    private static Line2D getLeftUnderlayLine(Rectangle2D underlay) {
        return new Line2D.Double(
            underlay.getMinX(), underlay.getMinY(),
            underlay.getMinX(), underlay.getMaxY()
        );
    }

    private static Line2D getBottomUnderlayLine(Rectangle2D underlay) {
        return new Line2D.Double(
            underlay.getMinX(), underlay.getMaxY(),
            underlay.getMaxX(), underlay.getMaxY()
        );
    }

    private static Line2D getRightUnderlayLine(Rectangle2D underlay) {
        return new Line2D.Double(
            underlay.getMaxX(), underlay.getMinY(),
            underlay.getMaxX(), underlay.getMaxY()
        );
    }

    // If the two line segments intersect, then return the point of their
    // intersection.  Otherwise return null.
    private static Point2D getIntersection(Line2D line1, Line2D line2) {
        if (line1.intersectsLine(line2)) {

            Point2D p1 = line1.getP1();
            Point2D p2 = line1.getP2();
            Point2D q1 = line2.getP1();
            Point2D q2 = line2.getP2();

            double p1x = p1.getX();
            double p1y = p1.getY();
            double p2x = p2.getX();
            double p2y = p2.getY();
            double q1x = q1.getX();
            double q1y = q1.getY();
            double q2x = q2.getX();
            double q2y = q2.getY();

            double c = ( (q2x - q1x) * (p1y - q1y) - (q2y - q1y) * (p1x - q1x) )
                     / ( (p2x - p1x) * (q2y - q1y) - (p2y - p1y) * (q2x - q1x) )
            ;
            Point2D i = new Point2D.Double(
                p1x + c * (p2x - p1x), p1y + c * (p2y - p1y)
            );
            return i;
        }
        return null;
    }

    // Compute a line segment parallel to the given segment and translated so
    // that the line constructed from the new segment by extrapolation passes
    // through the given point.
    private static Line2D getSegmentThroughPoint(Line2D seg, Point2D p) {
        Point2D p1 = seg.getP1();
        Point2D p2 = seg.getP2();

        double angle = Math.atan2(p2.getY() - p1.getY(), p2.getX() - p1.getX());
        angle += Math.PI / 2;

        double distance = seg.ptLineDist(p);

        int ccw = seg.relativeCCW(p);
        distance *= - ccw;

        double dx = distance * Math.cos(angle);
        double dy = distance * Math.sin(angle);

        p1 = new Point2D.Double(p1.getX() + dx, p1.getY() + dy);
        p2 = new Point2D.Double(p2.getX() + dx, p2.getY() + dy);

        return new Line2D.Double(p1, p2);
    }

    private static double minIgnoreNegative(double current, double next) {
        return (next > 0.001) ? Math.min(current, next) : current;
    }

    // Get the height such that the lower-right crop corner coincides with the
    // right underlay edge.
    private static double lrrHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( uMx - cx ) /
            Math.cos( angle + Math.atan2( - ch, cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the lower-right crop corner coincides with the
    // left underlay edge.
    private static double lrlHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( umx - cx ) /
            Math.cos( angle + Math.atan2( - ch, cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the lower-right crop corner coincides with the
    // top underlay edge.
    private static double lrtHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( uMy - cy ) /
            Math.sin( angle + Math.atan2( - ch, cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the lower-right crop corner coincides with the
    // bottom underlay edge.
    private static double lrbHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( umy - cy ) /
            Math.sin( angle + Math.atan2( - ch, cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the upper-right crop corner coincides with the
    // right underlay edge.
    private static double urrHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( uMx - cx ) /
            Math.cos( angle + Math.atan2( ch, cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the upper-right crop corner coincides with the
    // left underlay edge.
    private static double urlHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( umx - cx ) /
            Math.cos( angle + Math.atan2( ch, cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the upper-right crop corner coincides with the
    // top underlay edge.
    private static double urtHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( uMy - cy ) /
            Math.sin( angle + Math.atan2( ch, cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the upper-right crop corner coincides with the
    // bottom underlay edge.
    private static double urbHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( umy - cy ) /
            Math.sin( angle + Math.atan2( ch, cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the upper-left crop corner coincides with the
    // right underlay edge.
    private static double ulrHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( uMx - cx ) /
            Math.cos( angle + Math.atan2( ch, - cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the upper-left crop corner coincides with the
    // left underlay edge.
    private static double ullHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( umx - cx ) /
            Math.cos( angle + Math.atan2( ch, - cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the upper-left crop corner coincides with the
    // top underlay edge.
    private static double ultHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( uMy - cy ) /
            Math.sin( angle + Math.atan2( ch, - cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the upper-left crop corner coincides with the
    // bottom underlay edge.
    private static double ulbHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( umy - cy ) /
            Math.sin( angle + Math.atan2( ch, - cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the lower-left crop corner coincides with the
    // right underlay edge.
    private static double llrHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( uMx - cx ) /
            Math.cos( angle + Math.atan2( - ch, - cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the lower-left crop corner coincides with the
    // left underlay edge.
    private static double lllHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( umx - cx ) /
            Math.cos( angle + Math.atan2( - ch, - cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the lower-left crop corner coincides with the
    // top underlay edge.
    private static double lltHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( uMy - cy ) /
            Math.sin( angle + Math.atan2( - ch, - cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    // Get the height such that the lower-left crop corner coincides with the
    // bottom underlay edge.
    private static double llbHeight(
        CropBounds crop, Rectangle2D underlay
    ) {
        Point2D c = crop.getCenter();
        double cx = c.getX();
        double cy = c.getY();
        double cw = crop.getWidth();
        double ch = crop.getHeight();
        double uMx = underlay.getMaxX();
        double umx = underlay.getMinX();
        double uMy = underlay.getMaxY();
        double umy = underlay.getMinY();
        double angle = crop.getAngle();
        double aspect = crop.getWidth() / crop.getHeight();
        double constraint =
            ( 2 / Math.sqrt( 1 + aspect * aspect ) ) * ( umy - cy ) /
            Math.sin( angle + Math.atan2( - ch, - cw ) );
        return (! Double.isNaN(constraint)) ? constraint : -1;
    }

    private static CropBounds translateCrop(
        CropBounds oldCrop, double dx, double dy
    ) {
        Point2D center = oldCrop.getCenter();
        double width = oldCrop.getWidth();
        double height = oldCrop.getHeight();
        double angle = oldCrop.getAngle();

        center.setLocation(center.getX() + dx, center.getY() + dy);

        CropBounds newCrop = new CropBounds(center, width, height, angle);
        return newCrop;
    }

    private static final double UnderlayContainsSlop = 1.e-3;

    static boolean underlayContains(
        CropBounds newCrop, Rectangle2D underlay
    ) {
        if (underlay == null) {
            return true;
        }
        // Forgive one pixel when deciding whether a crop is inside the
        // underlay, to allow for roundoff error.
        Rectangle2D outsetRect = new Rectangle2D.Double(
            underlay.getX() - UnderlayContainsSlop,
            underlay.getY() - UnderlayContainsSlop,
            underlay.getWidth() + 2 * UnderlayContainsSlop,
            underlay.getHeight() + 2 * UnderlayContainsSlop
        );
        if (! outsetRect.contains(newCrop.getUpperLeft())) {
            return false;
        }
        if (! outsetRect.contains(newCrop.getUpperRight())) {
            return false;
        }
        if (! outsetRect.contains(newCrop.getLowerLeft())) {
            return false;
        }
        if (! outsetRect.contains(newCrop.getLowerRight())) {
            return false;
        }
        return true;
    }

    private static Shape getCropAsShape(CropBounds crop) {
        if ((crop == null) || crop.isAngleOnly()) {
            return null;
        }
        Point2D ul = crop.getUpperLeft();
        Point2D ur = crop.getUpperRight();
        Point2D ll = crop.getLowerLeft();
        Point2D lr = crop.getLowerRight();

        GeneralPath path = new GeneralPath();
        path.moveTo((float) ul.getX(), (float) ul.getY());
        path.lineTo((float) ur.getX(), (float) ur.getY());
        path.lineTo((float) lr.getX(), (float) lr.getY());
        path.lineTo((float) ll.getX(), (float) ll.getY());
        path.closePath();

        return path;
    }

    // Tell if the crop is consistent with having been derived from the
    // underlay by pure rotation.
    static boolean isRotateDefinedCrop(CropBounds crop, Rectangle2D underlay) {
        Point2D nw = crop.getUpperLeft();
        Point2D ne = crop.getUpperRight();
        Point2D sw = crop.getLowerLeft();
        Point2D se = crop.getLowerRight();

        double nwDist = getMinimumDistance(nw, underlay);
        double neDist = getMinimumDistance(ne, underlay);
        double swDist = getMinimumDistance(sw, underlay);
        double seDist = getMinimumDistance(se, underlay);

        if (
            (nwDist < UnderlayContainsSlop) && (seDist < UnderlayContainsSlop)
        ) {
            return true;
        }
        if (
            (swDist < UnderlayContainsSlop) && (neDist < UnderlayContainsSlop)
        ) {
            return true;
        }
        return false;
    }

    private static double getMinimumDistance(Point2D p, Rectangle2D underlay) {
        Line2D top = getTopUnderlayLine(underlay);
        Line2D bottom = getBottomUnderlayLine(underlay);
        Line2D left = getLeftUnderlayLine(underlay);
        Line2D right = getRightUnderlayLine(underlay);

        double dist = top.ptLineDist(p);
        dist = Math.min(dist, bottom.ptLineDist(p));
        dist = Math.min(dist, left.ptLineDist(p));
        dist = Math.min(dist, right.ptLineDist(p));

        return dist;
    }
}
