package com.lightcrafts.ui.browser.view;

import java.awt.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;

public class ImageDatumUrgencyRenderer {

    // Inset from the left edge
    static int HInset = 4;

    // Inset from the bottom edge
    static int VInset = 28;

    // Roundness of the roundrect background
    static int ArcRadius = 10;

    // Characteristic size of the little star
    static int LabelSize = 15;

    static int LeftMargin = 40;

    static void paint(Graphics2D g, Rectangle2D rect, int urgency) {
        final var origin = getTextOrigin(rect, g, "");

        final var label = new RoundRectangle2D.Double(
                origin.getX() - 0.5 * ArcRadius + LeftMargin,
                origin.getY() - 0.5 * ArcRadius - LabelSize,
                LabelSize + ArcRadius,
                LabelSize + ArcRadius,
                ArcRadius, ArcRadius
        );
        final var oldPaint = g.getPaint();
        final var color = valueToColorMap.get(urgency);
        g.setPaint(color);
        g.fill(label);
        g.setPaint(oldPaint);
    }

    // Get coordinates where the text origin should go.
    // This is the start of the basline, suitable for g.drawString().
    private static Point2D getTextOrigin(
            Rectangle2D rect, Graphics2D g, String text
    ) {
        Point2D ll = getLowerLeft(rect);
        FontMetrics metrics = g.getFontMetrics();
        Rectangle2D bounds = metrics.getStringBounds(text, g);
        double x = ll.getX() + bounds.getX();
        double y = ll.getY() - bounds.getHeight() - bounds.getY();
        return new Point2D.Double(x, y);
    }

    // Get coordinates where the lower-left corner of the rating number's
    // bounding rectangle should go.
    static Point2D getLowerLeft(Rectangle2D rect) {
        double x = rect.getX() + HInset + ArcRadius;
        double y = rect.getY() + rect.getHeight() - VInset - ArcRadius;
        return new Point2D.Double(x, y);
    }

    private static final Map<Integer, Color> valueToColorMap = new HashMap<>() {{
        // cf. https://jfly.uni-koeln.de/colorset/CUD_color_set_GuideBook_2018_for_print_cs4.pdf
        put(0, new Color(0, 0, 0, 0)); // transparent
        put(1, new Color(255, 75, 0)); // red
        put(2, new Color(246, 170, 0)); // orange
        put(3, new Color(255, 241, 0)); // yellow
        put(4, new Color(3, 175, 122)); // green
        put(5, new Color(77, 196, 255)); // blue
        put(6, new Color(153, 0, 153)); // purple
        put(7, new Color(132, 145, 158)); // gray
        put(8, Color.BLACK);
    }};
}
