/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016 Marinna Cole */
/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.ui.crop;

import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.operator.RotateDescriptor;
import com.lightcrafts.model.CropBounds;
import com.lightcrafts.platform.Platform;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputListener;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.LinkedList;
import java.util.List;
import java.lang.*;

// This is a translucent white overlay in the shape of the complement of
// a rectangle that can be dragged, turned, and resized with the mouse.
//
// It works in screen coordinates (no AffineTransform).

class CropOverlay extends JComponent implements MouseInputListener, MouseWheelListener {

    private final static Stroke RectStroke = new BasicStroke(20f);
    private final static Color RectColor = new Color(0, 0, 0, 128);
    private final static Color GridColor = new Color(100, 100, 100, 128);
    private final static Color RectBorderColor = Color.white;

    // Basic crop cursor:
    private final static Cursor CropCursor;

    // Drag-the-crop cursors:
    private final static Cursor MoveCursor;
    private final static Cursor MovingCursor;

    // Rotate cursor:
    private final static Cursor RotateCursor;
    private final static Cursor RotatingCursor;

    // Hot point coordinates for all the cursors:
    private final static ResourceBundle HotPointResources =
        ResourceBundle.getBundle("com/lightcrafts/ui/crop/resources/HotPoints");

    static {
        CropCursor = getCursor("crop_cursor");
        MoveCursor = getCursor("move_curve"); // Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR); // getCursor("move_curve");
        MovingCursor = getCursor("moving_curve");
        RotateCursor = getCursor("rotate");
        RotatingCursor = getCursor("rotating");
    }
    private final static int MinDragDistance = 10;

    private CropBounds crop;    // the crop bounds, in screen coordinates

    private Rectangle2D underlayRect; // underlay bounds, in screen coordinates

    private AspectConstraint constraint;    // the aspect ratio constraint

    // Rotation poll position normalized with underlayRect
    private Point2D relativePoll;

    private Cursor cursor;

    private Point dragStart;

    private boolean isRotating;
    private double rotateAngleStart;
    private Point rotateMouseStart;
    private double rotateWidthLimit;
    private double rotateHeightLimit;

    private boolean isMoving;
    private Point2D moveCenterStart;
    private Point moveMouseStart;

    private boolean adjustingNorth;
    private boolean adjustingSouth;
    private boolean adjustingEast;
    private boolean adjustingWest;

    private boolean hasHighlight;

    private boolean isRotateOnly;           // only allow rotations

    private enum GridStyle {
        THIRD() {
            @Override
            public GridStyle next() {
                return TRIANGLE;
            }
        },
        TRIANGLE() {
            @Override
            public GridStyle next() {
                return GOLDEN;
            }
        },
        GOLDEN() {
            @Override
            public GridStyle next() {
                return FIBONACCI;
            }
        },
        FIBONACCI() {
            @Override
            public GridStyle next() {
                return DIAGONAL;
            }
        },
        DIAGONAL() {
            @Override
            public GridStyle next() {
                return THIRD;
            }
        };

        abstract GridStyle next();
    }

    private static GridStyle CropGridStyle = GridStyle.THIRD;

    private static int GridOrientation = 0;

    // The members carried here are for painting different grid pattern
    private Point2D ul;
    private Point2D ur;
    private Point2D ll;
    private Point2D lr;
    private Point2D center;
    private double Width;
    private double Height;
    private double DiagonalDistance;
    private double RotateAngle;
    private double DiagonalAngle;

    CropOverlay(boolean isRotateOnly) {
        this.isRotateOnly = isRotateOnly;
        cursor = isRotateOnly ? RotateCursor : CropCursor;
        setCursor(cursor);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addRotateKeyListener();
    }

    CropBounds getCrop() {
        return crop;
    }

    // Accept a CropBounds that may violate constraints, mutate it into a
    // constrained CropBounds, and apply it.
    void setCropWithConstraints(CropBounds newCrop) {
        newCrop = constraint.adjust(newCrop);
        newCrop = UnderlayConstraints.sizeToUnderlay(
            newCrop, underlayRect, Integer.MAX_VALUE, Integer.MAX_VALUE
        );
        if (UnderlayConstraints.underlayContains(newCrop, underlayRect)) {
            setCrop(newCrop);
        }
    }

    void setCrop(CropBounds newCrop) {
        if ((newCrop == null) || newCrop.isAngleOnly()) {
            double angle = 0;
            if (newCrop != null) {
                angle = newCrop.getAngle();
            }
            newCrop = new CropBounds(underlayRect, angle);
        }
        crop = newCrop;
        repaint();
    }

    void setUnderlayRect(Rectangle bounds) {
        underlayRect = bounds;
    }

    void setAspectConstraint(AspectConstraint constraint) {
        this.constraint = constraint;
        if (crop != null) {
            CropBounds newCrop = constraint.adjust(crop);
            newCrop = UnderlayConstraints.translateToUnderlay(newCrop, underlayRect);
            // The crop may be null, if the constraints could not be enforced
            // within the bounds of the image.
            setCrop(newCrop);
        }
    }

    void modeEntered() {
        // At mode start, take a look at the current underlay and the current
        // crop, and decide whether rotate drag gestures should use the
        // underlay dimensions or the initial crop dimensions as the limit
        // for resizing during rotation.
        updateRotateConstraints();
    }

    Dimension2D getRotateLimitDimensions() {
        return new RotateLimitDimension(rotateWidthLimit, rotateHeightLimit);
    }

    void setRotateLimitDimensions(Dimension2D limit) {
        rotateWidthLimit = limit.getWidth();
        rotateHeightLimit = limit.getHeight();
    }

    // See modeEntered() and mousePressed().
    private void updateRotateConstraints() {
        boolean isRotateConstrained = UnderlayConstraints.isRotateDefinedCrop(crop, underlayRect);
        rotateWidthLimit = isRotateConstrained ?
            underlayRect.getWidth() : crop.getWidth();
        rotateHeightLimit = isRotateConstrained ?
            underlayRect.getHeight() : crop.getHeight();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Shape shape = getCropAsShape();
        if (shape != null) {
            Graphics2D g = (Graphics2D) graphics;
            Color oldColor = g.getColor();
            Stroke oldStroke = g.getStroke();
            RenderingHints oldHints = g.getRenderingHints();

            g.setColor(RectColor);
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );
            Area complement = new Area(getBounds());
            complement.subtract(new Area(shape));
            g.fill(complement);

            g.setColor(GridColor);
            g.setStroke(new BasicStroke(3));
            paintGrid(g);

            Stroke borderStroke = new BasicStroke(hasHighlight ? 2f : 1f);
            g.setColor(RectBorderColor);
            g.setStroke(borderStroke);
            g.draw(shape);

            g.setStroke(oldStroke);
            g.setColor(oldColor);
            g.setRenderingHints(oldHints);
        }
    }

    private void paintGrid(Graphics2D g) {
        ul = crop.getUpperLeft();
        ur = crop.getUpperRight();
        ll = crop.getLowerLeft();
        lr = crop.getLowerRight();
        center = crop.getCenter();

        if (isRotateOnly) {
            paintRotateGrid(g);
        }
        else {
            paintCropGrid(g);
        }
    }

    void changeOrientation() {
        GridOrientation++;
        GridOrientation %= 4;
        repaint();
    }

    private void paintCropGrid(Graphics2D g) {
        Width = crop.getWidth();
        Height = crop.getHeight();
        DiagonalDistance = Math.sqrt(Height*Height+Width*Width);
        RotateAngle = crop.getAngle();
        DiagonalAngle = Math.atan(Height/Width);

        switch (CropGridStyle) {
            case THIRD:
                paintGridThird(g);
                break;
            case TRIANGLE:
                paintGridTriangle(g);
                break;
            case GOLDEN:
                paintGridGolden(g);
                break;
            case FIBONACCI:
                paintGridFibonacci(g);
                break;
            case DIAGONAL:
                paintGridDiagonal(g);
                break;
        }
    }

    private void paintGridThird(Graphics2D g) {
        int gridCount = 3;
        for (int x = 1; x< gridCount; x++) {
            Point2D p = new Point2D.Double(
                (x * ul.getX() + (gridCount - x) * lr.getX()) / gridCount,
                (x * ul.getY() + (gridCount - x) * lr.getY()) / gridCount
            );
            Line2D hLine = new Line2D.Double(ul, ur);
            hLine = getSegmentThroughPoint(hLine, p);
            g.draw(hLine);

            Line2D vLine = new Line2D.Double(ul, ll);
            vLine = getSegmentThroughPoint(vLine, p);
            g.draw(vLine);
        }
    }

    private void paintGridTriangle(Graphics2D g) {
        final double ul_x = ul.getX();
        final double ul_y = ul.getY();
        final double ur_x = ur.getX();
        final double ur_y = ur.getY();
        final double ll_x = ll.getX();
        final double ll_y = ll.getY();
        final double lr_x = lr.getX();
        final double lr_y = lr.getY();

        // Determine the shortest distance between the corner to diagonal line
        // d = | (Xp-X1) * (Y2-Y1) - (Yp-Y1) * (X2-X1) | / Diagonal_Length
        final double d = Math.abs((lr_x - ll_x) * (ur_y - ll_y) - (lr_x - ll_y) * (ur_x-ll_x)) / DiagonalDistance;

        if ( (GridOrientation % 2) == 0 ) {
            g.draw( new Line2D.Double(ur_x, ur_y, ll_x, ll_y) );
            g.draw( new Line2D.Double(
                    lr_x - d * Math.sin(DiagonalAngle - RotateAngle),
                    lr_y - d * Math.cos(DiagonalAngle - RotateAngle), lr_x, lr_y) );
            g.draw( new Line2D.Double(
                    ul_x + d * Math.sin(DiagonalAngle - RotateAngle),
                    ul_y + d * Math.cos(DiagonalAngle - RotateAngle), ul_x, ul_y) );
        }
        else {
            g.draw( new Line2D.Double(ul_x, ul_y, lr_x, lr_y) );
            g.draw( new Line2D.Double(
                    ur_x - d * Math.sin(DiagonalAngle + RotateAngle),
                    ur_y + d * Math.cos(DiagonalAngle + RotateAngle), ur_x, ur_y) );
            g.draw( new Line2D.Double(
                    ll_x + d * Math.sin(DiagonalAngle + RotateAngle),
                    ll_y - d * Math.cos(DiagonalAngle + RotateAngle), ll_x, ll_y) );
        }
    }

    private void paintGridGolden(Graphics2D g) {
        Line2D hLine = new Line2D.Double(ul, ur);
        Line2D vLine = new Line2D.Double(ul, ll);

        // 0.118 the ratio of golden to center point => 0.5 - 1/2.618 = 0.118
        Point2D p = new Point2D.Double(
            center.getX() - 0.118 * DiagonalDistance * Math.cos(DiagonalAngle+RotateAngle),
            center.getY() - 0.118 * DiagonalDistance * Math.sin(DiagonalAngle+RotateAngle)
        );
        hLine = getSegmentThroughPoint(hLine, p);
        g.draw(hLine);
        vLine = getSegmentThroughPoint(vLine, p);
        g.draw(vLine);

        p = new Point2D.Double(
            center.getX() + 0.118 * DiagonalDistance * Math.cos(DiagonalAngle+RotateAngle),
            center.getY() + 0.118 * DiagonalDistance * Math.sin(DiagonalAngle+RotateAngle)
        );
        hLine = getSegmentThroughPoint(hLine, p);
        g.draw(hLine);
        vLine = getSegmentThroughPoint(vLine, p);
        g.draw(vLine);
    }

    private void paintGridFibonacci(Graphics2D g) {
        g.rotate(RotateAngle, center.getX(), center.getY());
        double angle;
        double arcWidth = Width / 1.618;
        double arcHeight = Height;

        switch (GridOrientation) {
        case 0:
            {
                angle = 180;

                Point2D Center = new Point2D.Double(center.getX()+0.118*Width, center.getY()+Height/2);

                g.drawArc(
                    (int)(Center.getX() - arcWidth),
                    (int)(Center.getY() - arcHeight),
                    (int)arcWidth*2,
                    (int)arcHeight*2,
                    (int)angle,
                    -90
                );

                for(int i=0; i<10; i++) {
                    angle -= 90;
                    arcWidth = arcWidth / 1.618;
                    arcHeight = arcHeight / 1.618;
                    Center.setLocation(
                        Center.getX() + Math.sin( Math.toRadians(  90 - angle ) ) * arcWidth  / 1.618,
                        Center.getY() - Math.sin( Math.toRadians( 180 - angle ) ) * arcHeight / 1.618
                    );
                    g.drawArc(
                        (int)(Center.getX() - arcWidth),
                        (int)(Center.getY() - arcHeight),
                        (int)arcWidth*2,
                        (int)arcHeight*2,
                        (int)angle,
                        -90
                    );
                }
            }
            break;
        case 1:
            {
                angle = 180;

                Point2D Center = new Point2D.Double(center.getX()+0.118*Width, center.getY()-Height/2);

                g.drawArc(
                    (int)(Center.getX() - arcWidth),
                    (int)(Center.getY() - arcHeight),
                    (int)arcWidth*2,
                    (int)arcHeight*2,
                    (int)angle,
                    90
                );

                for(int i=0; i<10; i++) {
                    angle += 90;
                    arcWidth = arcWidth / 1.618;
                    arcHeight = arcHeight / 1.618;
                    Center.setLocation(
                        Center.getX() - Math.sin( Math.toRadians(  -90 + angle ) ) * arcWidth  / 1.618,
                        Center.getY() + Math.sin( Math.toRadians( -180 + angle ) ) * arcHeight / 1.618
                    );
                    g.drawArc(
                        (int)(Center.getX() - arcWidth),
                        (int)(Center.getY() - arcHeight),
                        (int)arcWidth*2,
                        (int)arcHeight*2,
                        (int)angle,
                        90
                    );
                }
            }
            break;
        case 2:
            {
                angle = 0;

                Point2D Center = new Point2D.Double(center.getX()-0.118*Width, center.getY()+Height/2);

                g.drawArc(
                    (int)(Center.getX() - arcWidth),
                    (int)(Center.getY() - arcHeight),
                    (int)arcWidth*2,
                    (int)arcHeight*2,
                    (int)angle,
                    90
                );

                for(int i=0; i<10; i++) {
                    angle += 90;
                    arcWidth = arcWidth / 1.618;
                    arcHeight = arcHeight / 1.618;
                    Center.setLocation(
                            Center.getX() + Math.sin( Math.toRadians( 90 + angle ) ) * arcWidth  / 1.618,
                            Center.getY() - Math.sin( Math.toRadians(  0 + angle ) ) * arcHeight / 1.618
                    );
                    g.drawArc(
                        (int)(Center.getX() - arcWidth),
                        (int)(Center.getY() - arcHeight),
                        (int)arcWidth*2,
                        (int)arcHeight*2,
                        (int)angle,
                        90
                    );
                }
            }
            break;
        case 3:
            {
                angle = 0;

                Point2D Center = new Point2D.Double(center.getX() - 0.118 * Width, center.getY() - Height / 2);

                g.drawArc(
                    (int)(Center.getX() - arcWidth),
                    (int)(Center.getY() - arcHeight),
                    (int)arcWidth*2,
                    (int)arcHeight*2,
                    (int)angle,
                    -90
                );

                for(int i=0; i<10; i++) {
                    angle -= 90;
                    arcWidth = arcWidth / 1.618;
                    arcHeight = arcHeight / 1.618;
                    Center.setLocation(
                            Center.getX() + Math.sin( Math.toRadians( 90 - angle ) ) * arcWidth  / 1.618,
                            Center.getY() + Math.sin( Math.toRadians(  0 - angle ) ) * arcHeight / 1.618
                    );
                    g.drawArc(
                        (int)(Center.getX() - arcWidth),
                        (int)(Center.getY() - arcHeight),
                        (int)arcWidth*2,
                        (int)arcHeight*2,
                        (int)angle,
                        -90
                    );
                }
            }
            break;
        }
        g.rotate(-1*RotateAngle, center.getX(), center.getY());
    }

    private void paintGridDiagonal(Graphics2D g) {
        boolean ShorterSide = (Width > Height);

        if (ShorterSide) {
            g.draw( new Line2D.Double(ul.getX(), ul.getY(), ll.getX()+Height*Math.cos(-1*RotateAngle), ll.getY()-Height*Math.sin(-1*RotateAngle)) );
            g.draw( new Line2D.Double(ur.getX(), ur.getY(), lr.getX()-Height*Math.cos(-1*RotateAngle), lr.getY()+Height*Math.sin(-1*RotateAngle)) );
            g.draw( new Line2D.Double(ll.getX(), ll.getY(), ul.getX()+Height*Math.cos(-1*RotateAngle), ul.getY()-Height*Math.sin(-1*RotateAngle)) );
            g.draw( new Line2D.Double(lr.getX(), lr.getY(), ur.getX()-Height*Math.cos(-1*RotateAngle), ur.getY()+Height*Math.sin(-1*RotateAngle)) );
        }
        else {
            g.draw( new Line2D.Double(ul.getX(), ul.getY(), ur.getX()+Width*Math.sin(-1*RotateAngle), ur.getY()+Width*Math.cos(-1*RotateAngle)) );
            g.draw( new Line2D.Double(ur.getX(), ur.getY(), ul.getX()+Width*Math.sin(-1*RotateAngle), ul.getY()+Width*Math.cos(-1*RotateAngle)) );
            g.draw( new Line2D.Double(ll.getX(), ll.getY(), lr.getX()-Width*Math.sin(-1*RotateAngle), lr.getY()-Width*Math.cos(-1*RotateAngle)) );
            g.draw( new Line2D.Double(lr.getX(), lr.getY(), ll.getX()-Width*Math.sin(-1*RotateAngle), ll.getY()-Width*Math.cos(-1*RotateAngle)) );
        }
    }

    private void paintRotateGrid(Graphics2D g) {
        Point2D midLeft   = getMidPoint(ul, ll);
        Point2D midTop    = getMidPoint(ul, ur);
        Point2D midRight  = getMidPoint(ur, lr);
        Point2D midBottom = getMidPoint(ll, lr);

        Line2D hMidLine = new Line2D.Double(midLeft, midRight);
        Line2D vMidLine = new Line2D.Double(midTop, midBottom);

        Point2D poll = updatePoll();
        Line2D hPollLine = getSegmentThroughPoint(hMidLine, poll);
        Line2D vPollLine = getSegmentThroughPoint(vMidLine, poll);
        Point2D hMidPoint = getMidPoint(hPollLine.getP1(), hPollLine.getP2());
        Point2D vMidPoint = getMidPoint(vPollLine.getP1(), vPollLine.getP2());

        final int gridSpacing = 30;
        List<Point2D> upPts    = getPointsBetween(hMidPoint, midTop,    gridSpacing);
        List<Point2D> downPts  = getPointsBetween(hMidPoint, midBottom, gridSpacing);
        List<Point2D> rightPts = getPointsBetween(vMidPoint, midRight,  gridSpacing);
        List<Point2D> leftPts  = getPointsBetween(vMidPoint, midLeft,   gridSpacing);

        if (isInRect(poll)) {
            g.draw(hPollLine);
            g.draw(vPollLine);
            final double r = gridSpacing / 3;
            g.draw(new Ellipse2D.Double(poll.getX() - r, poll.getY() - r,
                                        2 * r, 2 * r));
        }
        paintLines(g, hMidLine,    upPts);
        paintLines(g, hMidLine,  downPts);
        paintLines(g, vMidLine, rightPts);
        paintLines(g, vMidLine,  leftPts);
    }

    private void paintLines(Graphics2D g, Line2D refLine, List<Point2D> Pts) {
        //boolean isFirstLine = true;

        for (Point2D p : Pts) {
            if (! isInRect(p))
                continue;
            /*
            // When painting all these lines, we take care not to paint the center
            // lines twice, which would make them appear darker than the other
            // lines because of the compositing.

            if (isFirstLine) {
                isFirstLine = false;
                continue;
            }
            */
            Line2D line = getSegmentThroughPoint(refLine, p);
            g.draw(line);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();

        if (isInRect(p)) {
            if (isRotateOnly) {
                setPoll(p);
                return;
            }

            // Cycle through different overlays when the crop hasn't been rotated.
            // Does it make sense to expand this for the rotated case?
            CropGridStyle = CropGridStyle.next();
            /*
            switch (CropGridStyle) {
                case THIRD:
                     CropGridStyle = GridStyle.TRIANGLE;
                     break;
                case TRIANGLE:
                     CropGridStyle = GridStyle.GOLDEN;
                     break;
                case GOLDEN:
                     CropGridStyle = GridStyle.FIBONACCI;
                     break;
                case FIBONACCI:
                     CropGridStyle = GridStyle.DIAGONAL;
                     break;
                case DIAGONAL:
                     CropGridStyle = GridStyle.THIRD;
                     break;
            }
            */
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            return;
        }
        Point p = e.getPoint();

        if (isRotateOnly) {
            if (crop == null) {
                setCrop(new CropBounds(underlayRect));
            }
            isRotating = true;
            rotateAngleStart = crop.getAngle();
            rotateMouseStart = p;
        }
        else {
            if (isOnRect(p)) {
                if (isOnNorth(p)) {
                    adjustingNorth = true;
                }
                if (isOnSouth(p)) {
                    adjustingSouth = true;
                }
                if (isOnEast(p)) {
                    adjustingEast = true;
                }
                if (isOnWest(p)) {
                    adjustingWest = true;
                }
                updateRotateConstraints();
            }
            else if (isInRect(p)) {
                if (hasRotateModifier(e)) {
                    isRotating = true;
                    rotateAngleStart = crop.getAngle();
                    rotateMouseStart = p;
                }
                else {
                    isMoving = true;
                    moveMouseStart = p;
                    moveCenterStart = crop.getCenter();
                }
                updateRotateConstraints();
            }
            else if (crop != null && (! crop.isAngleOnly())) {
                isRotating = true;
                rotateAngleStart = crop.getAngle();
                rotateMouseStart = p;
            }
            else {
                if (hasRotateModifier(e)) {
                    setCrop(new CropBounds(underlayRect));
                    isRotating = true;
                    rotateAngleStart = crop.getAngle();
                    rotateMouseStart = p;
                }
                else {
                    if (! underlayRect.contains(p)) {
                        // Use the closest point inside the underlay instead.
                        Point2D closest = getClosestUnderlayPoint(p);
                        p = new Point(
                            (int) Math.round(closest.getX()),
                            (int) Math.round(closest.getY())
                        );
                    }
                    dragStart = p;
                }
            }
        }
        updateCursor(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            return;
        }
        isMoving = false;
        isRotating = false;
        dragStart = null;
        rotateMouseStart = null;
        moveMouseStart = null;
        moveCenterStart = null;
        adjustingNorth = false;
        adjustingSouth = false;
        adjustingEast = false;
        adjustingWest = false;
        updateCursor(e);
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updateHighlight(e);

        Point p = e.getPoint();

        // Backup the current crop, so we can back out changes if it turns
        // out they exceed the underlay constraint.
        CropBounds oldCrop = crop;

        if ((dragStart != null) &&
            ((Math.abs(dragStart.x - p.x) < MinDragDistance) ||
             (Math.abs(dragStart.y - p.y) < MinDragDistance))) {
            return;
        }
        if (dragStart != null) {
            if (! underlayRect.contains(p)) {
                return;
            }
            double x = Math.min(p.x, dragStart.x);
            double y = Math.min(p.y, dragStart.y);
            double w = Math.abs(p.x - dragStart.x);
            double h = Math.abs(p.y - dragStart.y);
            Rectangle2D rect = new Rectangle2D.Double(x, y, w, h);
            CropBounds newCrop = new CropBounds(rect);
            setCrop(newCrop);
            if (isOnNorth(p)) {
                adjustingNorth = true;
            }
            else {
                adjustingSouth = true;
            }
            if (isOnWest(p)) {
                adjustingWest = true;
            }
            else {
                adjustingEast = true;
            }
            dragStart = null;
        }
        else if (isMoving) {
            int dx = p.x - moveMouseStart.x;
            int dy = p.y - moveMouseStart.y;
            Point2D.Double center = new Point2D.Double(
                moveCenterStart.getX() + dx,
                moveCenterStart.getY() + dy
            );
            double width = crop.getWidth();
            double height = crop.getHeight();
            double angle = crop.getAngle();
            CropBounds newCrop = new CropBounds(center, width, height, angle);
            newCrop = UnderlayConstraints.translateToUnderlay(newCrop, underlayRect);
            if (newCrop != null) {
                setCrop(newCrop);
            }
        }
        else if (isRotating) {
            Point2D poll = updatePoll();
            Line2D start = new Line2D.Double(poll, rotateMouseStart);
            Line2D end = new Line2D.Double(poll, p);
            double startAngle = Math.atan2(
                start.getY2() - start.getY1(), start.getX2() - start.getX1()
            );
            double endAngle = Math.atan2(
                end.getY2() - end.getY1(), end.getX2() - end.getX1()
            );
            double angle = rotateAngleStart + endAngle - startAngle;
            CropBounds newCrop = new CropBounds(crop, angle);
            updateRotateConstraints();
            newCrop = UnderlayConstraints.sizeToUnderlay(
                newCrop, underlayRect, rotateWidthLimit, rotateHeightLimit
            );
            if (newCrop != null) {
                setCrop(newCrop);
            }

            return;
        }
        else if ((crop != null) && (! crop.isAngleOnly())) {
            // This MouseEvent is a regular mid-drag event.

            // If an edge is adjusting, then preserve the opposite edge.
            if (isEdgeAdjusting()) {
                if (adjustingNorth) {
                    setNorth(p);
                    crop = constraint.adjustWidth(crop);
                    if (constraint.isNoConstraint()) {
                        crop = UnderlayConstraints.adjustNorthToUnderlay(crop, underlayRect);
                    }
                    else {
                        crop = UnderlayConstraints.adjustNorthWithConstraint(crop, underlayRect);
                    }
                }
                else if (adjustingSouth) {
                    setSouth(p);
                    crop = constraint.adjustWidth(crop);
                    if (constraint.isNoConstraint()) {
                        crop = UnderlayConstraints.adjustSouthToUnderlay(crop, underlayRect);
                    }
                    else {
                        crop = UnderlayConstraints.adjustSouthWithConstraint(crop, underlayRect);
                    }
                }
                if (adjustingEast) {
                    setEast(p);
                    crop = constraint.adjustHeight(crop);
                    if (constraint.isNoConstraint()) {
                        crop = UnderlayConstraints.adjustEastToUnderlay(crop, underlayRect);
                    }
                    else {
                        crop = UnderlayConstraints.adjustEastWithConstraint(crop, underlayRect);
                    }
                }
                else if (adjustingWest) {
                    setWest(p);
                    crop = constraint.adjustHeight(crop);
                    if (constraint.isNoConstraint()) {
                        crop = UnderlayConstraints.adjustWestToUnderlay(crop, underlayRect);
                    }
                    else {
                        crop = UnderlayConstraints.adjustWestWithConstraint(crop, underlayRect);
                    }
                }
                if (! UnderlayConstraints.underlayContains(crop, underlayRect)) {
                    setCrop(oldCrop);
                }
                updateRotateConstraints();
            }
            // If a corner is adjusting, then preserve the opposite corner.
            else if (isCornerAdjusting()) {

                p = projectOntoUnderlay(p);

                Point2D center = crop.getCenter();
                Point2D ul = crop.getUpperLeft();
                Point2D ur = crop.getUpperRight();
                Point2D ll = crop.getLowerLeft();
                Point2D lr = crop.getLowerRight();

                if (adjustingSouth) {
                    setSouth(p);
                    if (adjustingWest) {
                        setWest(p);
                        Line2D diag = new Line2D.Double(center, ll);
                        if (diag.relativeCCW(p) > 0) {
                            crop = constraint.adjustBottom(crop);
                        }
                        else {
                            crop = constraint.adjustLeft(crop);
                        }
                        crop = UnderlayConstraints.adjustSouthWestToUnderlay(
                            crop, underlayRect
                        );
                    }
                    else {
                        setEast(p);
                        Line2D diag = new Line2D.Double(center, lr);
                        if (diag.relativeCCW(p) > 0) {
                            crop = constraint.adjustBottom(crop);
                        }
                        else {
                            crop = constraint.adjustRight(crop);
                        }
                        crop = UnderlayConstraints.adjustSouthEastToUnderlay(
                            crop, underlayRect
                        );
                    }
                }
                else {
                    setNorth(p);
                    if (adjustingEast) {
                        setEast(p);
                        Line2D diag = new Line2D.Double(center, ur);
                        if (diag.relativeCCW(p) > 0) {
                            crop = constraint.adjustTop(crop);
                        }
                        else {
                            crop = constraint.adjustRight(crop);
                        }
                        crop = UnderlayConstraints.adjustNorthEastToUnderlay(
                            crop, underlayRect
                        );
                    }
                    else {
                        setWest(p);
                        Line2D diag = new Line2D.Double(center, ul);
                        if (diag.relativeCCW(p) > 0) {
                            crop = constraint.adjustTop(crop);
                        }
                        else {
                            crop = constraint.adjustLeft(crop);
                        }
                        crop = UnderlayConstraints.adjustNorthWestToUnderlay(
                            crop, underlayRect
                        );
                    }
                    updateRotateConstraints();
                }
            }
        }
        if (! UnderlayConstraints.underlayContains(crop, underlayRect)) {
            setCrop(oldCrop);
        }
    }

    private Point projectOntoUnderlay(Point p) {
        Point q = (Point) p.clone();
        Rectangle under = underlayRect.getBounds();
        final int outcode = underlayRect.outcode( p );
        if ( (outcode & Rectangle2D.OUT_TOP) != 0 ) {
            q = new Point(q.x, under.y);
        }
        if ( (outcode & Rectangle2D.OUT_LEFT) != 0 ) {
            q = new Point(under.x, q.y);
        }
        if ( (outcode & Rectangle2D.OUT_BOTTOM) != 0 ) {
            q = new Point(q.x, under.y + under.height);
        }
        if ( (outcode & Rectangle2D.OUT_RIGHT) != 0 ) {
            q = new Point(under.x + under.width, q.y);
        }
        return q;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateCursor(e);
        updateHighlight(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int count = e.getWheelRotation();

        rotateAngleStart = crop.getAngle();
        double deg = rotateAngleStart * 180 / Math.PI;
        deg = Math.round(100 * deg) * 0.01d;

        final double STEPS_PER_DEG = 50;
        double angle = Math.round(STEPS_PER_DEG * deg + count) / STEPS_PER_DEG * Math.PI / 180;

        CropBounds newCrop = new CropBounds(crop, angle);
        updateRotateConstraints();
        newCrop = UnderlayConstraints.sizeToUnderlay(
                newCrop, underlayRect, rotateWidthLimit, rotateHeightLimit
                );
        if (newCrop != null)
            setCrop(newCrop);
    }

    private boolean isEdgeAdjusting() {
        return (
            (adjustingNorth && ! (adjustingEast || adjustingWest)) ||
            (adjustingSouth && ! (adjustingEast || adjustingWest)) ||
            (adjustingEast && ! (adjustingNorth || adjustingSouth)) ||
            (adjustingWest && ! (adjustingNorth || adjustingSouth))
        );
    }

    private boolean isCornerAdjusting() {
        return (
            (adjustingNorth && (adjustingEast || adjustingWest)) ||
            (adjustingSouth && (adjustingEast || adjustingWest))
        );
    }

    private void updateCursor(MouseEvent e) {
        Cursor newCursor = getCursor(e);
        if (cursor != newCursor) {
            cursor = newCursor;
            setCursor(cursor);
        }
    }

    private void updateHighlight(MouseEvent e) {
        Point p = e.getPoint();
        boolean hadHighlight = hasHighlight;
        hasHighlight = isAdjusting() || isInRect(p) || isOnRect(p);
        if (hadHighlight != hasHighlight) {
            repaint();
        }
    }

    private Cursor getCursor(MouseEvent e) {
        if ((crop == null) || crop.isAngleOnly()) {
            if (hasRotateModifier(e) || isRotateOnly) {
                return RotateCursor;
            }
            else {
                return CropCursor;
            }
        }
        Point p = e.getPoint();
        if (! isRotateOnly) {
            double angle = crop.getAngle();
            if (isOnNorth(p)) {
                if (isOnEast(p)) {
                    return getResizeCursor(- Math.PI / 4 + angle);
                }
                if (isOnWest(p)) {
                    return getResizeCursor(- 3 * Math.PI / 4 + angle);
                }
                return getResizeCursor(- Math.PI / 2 + angle);
            }
            if (isOnSouth(p)) {
                if (isOnEast(p)) {
                    return getResizeCursor(+ Math.PI / 4 + angle);
                }
                if (isOnWest(p)) {
                    return getResizeCursor(+ 3 * Math.PI / 4 + angle);
                }
                return getResizeCursor(+ Math.PI / 2 + angle);
            }
            if (isOnEast(p)) {
                return getResizeCursor(+ angle);
            }
            if (isOnWest(p)) {
                return getResizeCursor(+ Math.PI + angle);
            }
            if (isInRect(p)) {
                if (isRotating) {
                    return RotatingCursor;
                }
                else if (isMoving) {
                    return MovingCursor;
                }
                else if (
                    hasRotateModifier(e)
                    ) {
                    return RotateCursor;
                }
                else {
                    return MoveCursor;
                }
            }
        }
        if ((crop != null) || isRotateOnly) {
            if (isRotating) {
                return RotatingCursor;
            }
            else {
                return RotateCursor;
            }
        }
        return CropCursor;
    }

    private boolean isAdjusting() {
        return adjustingNorth ||
               adjustingSouth ||
               adjustingEast  ||
               adjustingWest  ||
               isRotating ||
               isMoving;
    }

    private boolean isInRect(Point2D p) {
        Shape shape = getCropAsShape();
        return (shape != null) && shape.contains(p);
    }

    private boolean isOnRect(Point p) {
        Shape shape = getCropAsShape();
        if (shape == null) {
            return false;
        }
        Shape thickRect = createThickShape(shape);
        return thickRect.contains(p);
    }

    private void setNorth(Point p) {
        Line2D north = getNorthLine();
        north = getSegmentThroughPoint(north, p);

        Point2D center = crop.getCenter();
        if (north.relativeCCW(center) != -1) {
            return;
        }
        Point2D ul = north.getP1();
        Point2D ur = north.getP2();
        Point2D ll = crop.getLowerLeft();
        Point2D lr = crop.getLowerRight();

        CropBounds newCrop = new CropBounds(ul, ur, ll, lr);
        setCrop(newCrop);
    }

    private void setSouth(Point p) {
        Line2D south = getSouthLine();
        south = getSegmentThroughPoint(south, p);

        Point2D center = crop.getCenter();
        if (south.relativeCCW(center) != -1) {
            return;
        }
        Point2D ul = crop.getUpperLeft();
        Point2D ur = crop.getUpperRight();
        Point2D ll = south.getP2();
        Point2D lr = south.getP1();

        CropBounds newCrop = new CropBounds(ul, ur, ll, lr);
        setCrop(newCrop);
    }

    private void setEast(Point p) {
        Line2D east = getEastLine();
        east = getSegmentThroughPoint(east, p);

        Point2D center = crop.getCenter();
        if (east.relativeCCW(center) != -1) {
            return;
        }
        Point2D ul = crop.getUpperLeft();
        Point2D ur = east.getP1();
        Point2D ll = crop.getLowerLeft();
        Point2D lr = east.getP2();

        CropBounds newCrop = new CropBounds(ul, ur, ll, lr);
        setCrop(newCrop);
    }

    private void setWest(Point p) {
        Line2D west = getWestLine();
        west = getSegmentThroughPoint(west, p);

        Point2D center = crop.getCenter();
        if (west.relativeCCW(center) != -1) {
            return;
        }
        Point2D ul = west.getP2();
        Point2D ur = crop.getUpperRight();
        Point2D ll = west.getP1();
        Point2D lr = crop.getLowerRight();

        CropBounds newCrop = new CropBounds(ul, ur, ll, lr);
        setCrop(newCrop);
    }

    private Line2D getNorthLine() {
        return new Line2D.Double(crop.getUpperLeft(), crop.getUpperRight());
    }

    private Line2D getEastLine() {
        return new Line2D.Double(crop.getUpperRight(), crop.getLowerRight());
    }

    private Line2D getSouthLine() {
        return new Line2D.Double(crop.getLowerRight(), crop.getLowerLeft());
    }

    private Line2D getWestLine() {
        return new Line2D.Double(crop.getLowerLeft(), crop.getUpperLeft());
    }

    private boolean isOnNorth(Point p) {
        return crop != null && createThickShape(getNorthLine()).contains(p);
    }

    private boolean isOnSouth(Point p) {
        return crop != null && createThickShape(getSouthLine()).contains(p);
    }

    private boolean isOnEast(Point p) {
        return crop != null && createThickShape(getEastLine()).contains(p);
    }

    private boolean isOnWest(Point p) {
        return crop != null && createThickShape(getWestLine()).contains(p);
    }

    private Point2D getClosestUnderlayPoint(Point2D p) {
        if (underlayRect.contains(p)) {
            return p;
        }
        if (p.getX() < underlayRect.getMinX()) {
            p = new Point2D.Double(underlayRect.getMinX(), p.getY());
        }
        if (p.getY() < underlayRect.getMinY()) {
            p = new Point2D.Double(p.getX(), underlayRect.getMinY());
        }
        if (p.getX() > underlayRect.getMaxX()) {
            p = new Point2D.Double(underlayRect.getMaxX(), p.getY());
        }
        if (p.getY() > underlayRect.getMaxY()) {
            p = new Point2D.Double(p.getX(), underlayRect.getMaxY());
        }
        return p;
    }

//    // Repaint this overlay when the CropBounds udpates, marking dirty as
//    // little of the component as possible to minimize thrash painting of
//    // the underlay.
//    private void repaint(CropBounds oldCrop, CropBounds newCrop) {
//        Shape oldShape = getCropAsShape(oldCrop);
//        Shape newShape = getCropAsShape(newCrop);
//        if ((oldShape == null) || (newShape == null)) {
//            repaint();
//            return;
//        }
//        Rectangle oldRect = oldShape.getBounds();
//        Rectangle newRect = newShape.getBounds();
//        if (! newRect.intersects(oldRect)) {
//            repaint(oldRect);
//            repaint(newRect);
//            return;
//        }
//        Area xor = new Area(oldShape);
//        xor.exclusiveOr(new Area(newShape));
//
//        Set<Shape> shapes = getConnectedComponents(xor);
//        for (Shape shape : shapes) {
//            Rectangle rect = shape.getBounds();
//            repaint(rect);
//        }
//    }

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

    private static Point2D getMidPoint(Point2D p, Point2D q) {
        return new Point2D.Double(
            (p.getX() + q.getX()) / 2, (p.getY() + q.getY()) / 2
        );
    }

    private static List<Point2D> getPointsBetween(
        Point2D start, Point2D end, double spacing
    ) {
        double angle = Math.atan2(
            end.getY() - start.getY(), end.getX() - start.getX()
        );
        double dist = start.distance(end);
        int nPts = (int) Math.floor(dist / spacing);
        LinkedList<Point2D> points = new LinkedList<Point2D>();
        Point2D p = start;
        for (int n = 0; n <= nPts; n++) {
            points.add(p);
            p = new Point2D.Double(
                p.getX() + spacing * Math.cos(angle),
                p.getY() + spacing * Math.sin(angle)
            );
        }
        return points;
    }

    private Shape getCropAsShape() {
        if (crop == null || crop.isAngleOnly()) {
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

    private static Shape createThickShape(Shape shape) {
        return RectStroke.createStrokedShape(shape);
    }

//    // Return a Set of Shapes, comprising the connected components of
//    // a given Shape.  Used for the optimized repaint() method.
//    private static Set<Shape> getConnectedComponents(Shape shape) {
//        PathIterator i = shape.getPathIterator(new AffineTransform());
//        HashSet<Shape> components = new HashSet<Shape>();
//        if (i.isDone()) {
//            return components;
//        }
//        float[] pts = new float[6];
//        GeneralPath component = new GeneralPath();
//        int type = i.currentSegment(pts);
//        assert type == PathIterator.SEG_MOVETO;
//        component.moveTo(pts[0], pts[1]);
//        i.next();
//        while (! i.isDone()) {
//            type = i.currentSegment(pts);
//            if (type == PathIterator.SEG_MOVETO) {
//                components.add(component);
//                component = new GeneralPath();
//                component.moveTo(pts[0], pts[1]);
//            }
//            else if (type == PathIterator.SEG_LINETO) {
//                component.lineTo(pts[0], pts[1]);
//            }
//            else if (type == PathIterator.SEG_QUADTO) {
//                component.quadTo(pts[0], pts[1], pts[2], pts[3]);
//            }
//            else if (type == PathIterator.SEG_CUBICTO) {
//                component.curveTo(
//                    pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]
//                );
//            }
//            else if (type == PathIterator.SEG_CLOSE) {
//                component.closePath();
//            }
//            i.next();
//        }
//        components.add(component);
//
//        return components;
//    }

    // Access one of the predefined cursors from resources.
    private static Cursor getCursor(String name) {
        try {
            String path = "resources/" + name + ".png";
            URL url = CropOverlay.class.getResource(path);
            BufferedImage image = ImageIO.read(url);
            int x = Integer.parseInt(HotPointResources.getString(name + "X"));
            int y = Integer.parseInt(HotPointResources.getString(name + "Y"));
            Point hot = new Point(x, y);
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            return toolkit.createCustomCursor(image, hot, name);
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static double RecentCursorAngle;
    private static Cursor RecentCursor;

    // Custom-render a new resize cursor that looks right for resizing an edge
    // at the given angle.
    private static Cursor getResizeCursor(double angle) {
        // Don't thrash the cursor rendering: if the requested angle matches
        // the most recent angle within roundoff, then send back the recent
        // cursor.

        if (Platform.isWindows()) {
            int cursorType = Cursor.DEFAULT_CURSOR;
            double a = Math.PI/4;
            if (Math.abs(angle) < a)
                cursorType = Cursor.E_RESIZE_CURSOR;
            if (Math.abs(angle - Math.PI/4) < a)
                cursorType = Cursor.SE_RESIZE_CURSOR;
            if (Math.abs(angle - Math.PI/2) < a)
                cursorType = Cursor.S_RESIZE_CURSOR;
            if (Math.abs(angle - 3 * Math.PI/4) < a)
                cursorType = Cursor.SW_RESIZE_CURSOR;
            if (Math.abs(angle - Math.PI) < a)
                cursorType = Cursor.W_RESIZE_CURSOR;
            if (Math.abs(angle + 3 * Math.PI/4) < a)
                cursorType = Cursor.NW_RESIZE_CURSOR;
            if (Math.abs(angle + Math.PI/2) < a)
                cursorType = Cursor.N_RESIZE_CURSOR;
            if (Math.abs(angle + Math.PI/4) < a)
                cursorType = Cursor.NE_RESIZE_CURSOR;

            return Cursor.getPredefinedCursor(cursorType);
        }

        double diff = Math.abs(angle - RecentCursorAngle);
        if ((diff < 0.001) && (RecentCursor != null)) {
            return RecentCursor;
        }
        try {
            String path = "resources/resize.png";
            URL url = CropOverlay.class.getResource(path);
            RenderedImage resourceImage = ImageIO.read(url);
            int cx = resourceImage.getWidth() / 2;
            int cy = resourceImage.getHeight() / 2;
            RenderedOp rotatedImage = RotateDescriptor.create(
                resourceImage,
                (float) cx, (float) cy, (float) angle,
                Interpolation.getInstance(Interpolation.INTERP_BICUBIC),
                null, null
            );
            Point hot = new Point(cx, cy);
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            BufferedImage buffer = rotatedImage.getAsBufferedImage();
            Cursor cursor = toolkit.createCustomCursor(buffer, hot, "resize");

            RecentCursorAngle = angle;
            RecentCursor = cursor;

            return cursor;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean hasRotateModifier(MouseEvent event) {
        Platform.Type type = Platform.getType();
        switch (type) {
            case MacOSX:
                return event.isMetaDown();
            default:
                return event.isControlDown();
        }
    }

    private void addRotateKeyListener() {
        KeyboardFocusManager focus =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focus.addKeyEventPostProcessor(rotateKeyProcessor);
    }

    public void dispose() {
        KeyboardFocusManager focus =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focus.removeKeyEventPostProcessor(rotateKeyProcessor);
    }

    // Listen for keystrokes so we can detect when the user presses and
    // releases the modifier key that allows rotation from inside the crop
    // bounds, so we can update the cursor.
    private KeyEventPostProcessor rotateKeyProcessor =

        new KeyEventPostProcessor() {

            int RotateKeyCode = Platform.isMac()
                    ? KeyEvent.VK_META
                    : KeyEvent.VK_CONTROL;

            int RotateModifierMask = Platform.isMac()
                    ? MouseEvent.META_DOWN_MASK
                    : MouseEvent.CTRL_DOWN_MASK;

            @Override
            public boolean postProcessKeyEvent(KeyEvent e) {
                if (e.getKeyCode() == RotateKeyCode) {

                    boolean isPressed = e.getID() == KeyEvent.KEY_PRESSED;
                    boolean isReleased = e.getID() == KeyEvent.KEY_RELEASED;

                    if (isPressed || isReleased) {

                        PointerInfo pointer = MouseInfo.getPointerInfo();
                        Point p = pointer.getLocation();
                        SwingUtilities.convertPointFromScreen(
                            p, CropOverlay.this
                        );
                        MouseEvent syntheticEvent = new MouseEvent(
                            CropOverlay.this,
                            0, 0,
                            isPressed ? RotateModifierMask : 0,
                            p.x, p.y, 0, false
                        );
                        updateCursor(syntheticEvent);
                    }
                }
                return false;   // these key events have other interpretations
            }
        };

    private Point2D updatePoll() {
        if (relativePoll == null) {
            Point2D p = crop.getCenter();
            setPoll(p);
            return p;
        }

        return new Point2D.Double(
                relativePoll.getX() * underlayRect.getWidth()  + underlayRect.getX(),
                relativePoll.getY() * underlayRect.getHeight() + underlayRect.getY());
    }

    private void setPoll(Point2D p) {
        if (p == null) {
            p = crop.getCenter();
        }
        relativePoll = new Point2D.Double(
                (p.getX() - underlayRect.getX()) / underlayRect.getWidth(),
                (p.getY() - underlayRect.getY()) / underlayRect.getHeight());
    }
}
