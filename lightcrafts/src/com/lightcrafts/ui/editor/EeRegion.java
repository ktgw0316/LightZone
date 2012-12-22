/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.Region;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

class EeRegion {

    static class ShapeRegion implements Region {

        private Shape shape;

        ShapeRegion(Shape shape) {
            this.shape = shape;
        }

        public Shape getOuterShape() {
            return shape;
        }

        public float getWidth() {
            return 0;
        }

        public Point2D getTranslation() {
            return null;
        }

        public Collection getContours() {
            return Collections.singleton(this);
        }
    }

    private static ArrayList<String> Names = new ArrayList<String>();

    static {
        ResourceBundle resources = ResourceBundle.getBundle(
            "com/lightcrafts/ui/editor/resources/EeRegion"
        );
        String Team = resources.getString("Team");
        StringTokenizer tokens = new StringTokenizer(Team, ",");
        while (tokens.hasMoreTokens()) {
            Names.add(tokens.nextToken());
        }
    }

    // Translate a shape so the upper left corner of its bounds is at (0, 0):
    private static Shape justify(Shape shape) {
        Rectangle2D bounds = shape.getBounds();
        AffineTransform xform = AffineTransform.getTranslateInstance(
            - bounds.getX(), - bounds.getY()
        );
        return xform.createTransformedShape(shape);
    }

    private static Shape stringToShape(Graphics2D g, String s, float size) {
        FontRenderContext context = g.getFontRenderContext();
        Font font = g.getFont();
        font = font.deriveFont(size);
        TextLayout text = new TextLayout(s, font, context);
        Shape shape = text.getOutline(null);
        shape = justify(shape);
        return shape;
    }

    static Region create(Graphics2D g, Dimension size) {
        int count = Names.size();
        Shape[] shapes = new Shape[count];
        double[] widths = new double[count];
        double[] heights = new double[count];
        float fontSize = 32f * size.height / 400;

        for (int n=0; n<count; n++) {
            String name = Names.get(n);
            Shape shape = stringToShape(g, name, fontSize);
            Rectangle2D bounds = shape.getBounds();
            shapes[n] = shape;
            widths[n] = bounds.getWidth();
            heights[n] = bounds.getHeight();
        }
        // Find the maximum height
        double maxHeight = 0;
        for (int n=0; n<count; n++) {
            maxHeight = Math.max(maxHeight, heights[n]);
        }
        maxHeight *= 1.3;   // extra vertical space is nice

        // Pad with empty space above and below
        double inset = (size.height - count * maxHeight) / 2.;

        Area area = new Area();
        for (int n=0; n<count; n++) {
            // Distribute vertically:
            double h = inset + n * maxHeight;
            // Center horizontally:
            double w = (size.width - widths[n]) / 2.;
            AffineTransform xform = AffineTransform.getTranslateInstance(w, h);
            shapes[n] = xform.createTransformedShape(shapes[n]);
            area.add(new Area(shapes[n]));
        }
        ShapeRegion region = new ShapeRegion(area);
        return region;
    }
}
