/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.mode.test;

/** A Point that takes a stream of calls to update(Point) and sometimes
  * responds by changing its location to the given Point, exhibiting a sticky
  * behavior near the boundaries of its Rectangle.
  * <p>
  * @see com.lightcrafts.ui.region.FollowMouseMode
  * @see com.lightcrafts.ui.region.FollowMouseOnceMode
  * @see com.lightcrafts.ui.crop.CropOverlay
  */

import java.awt.*;

public class MagneticPoint extends Point {

    private final static int Range = 20;    // Distance of magnetic behavior

    private Rectangle rect;
    private Rectangle insetRect;
    boolean isStuck;

    public MagneticPoint(Component c) {
        this(new Rectangle(0, 0, c.getSize().width, c.getSize().height));
    }

    public MagneticPoint(Component c, Point p) {
        this(new Rectangle(0, 0, c.getSize().width, c.getSize().height), p);
    }

    public MagneticPoint(Rectangle rect) {
        this.rect = rect;
        insetRect = new Rectangle(
            rect.x + Range,
            rect.y + Range,
            rect.width - 2 * Range,
            rect.height - 2 * Range
        );
    }

    public MagneticPoint(Rectangle rect, Point p) {
        super(p);
        this.rect = rect;
        insetRect = new Rectangle(
            rect.x + Range,
            rect.y + Range,
            rect.width - 2 * Range,
            rect.height - 2 * Range
        );
    }

    public void update(Point p) {
        if (isStuck && rect.contains(p)) {
            if (insetRect.contains(p)) {
                isStuck = false;
            }
            else {
                return;
            }
        }
        if (p.y < rect.y) {
            p = (Point) p.clone();
            p.setLocation(p.x, rect.y);
            isStuck = true;
        }
        if (p.y > rect.y + rect.height) {
            p = (Point) p.clone();
            p.setLocation(p.x, rect.y + rect.height);
            isStuck = true;
        }
        if (p.x < rect.x) {
            p = (Point) p.clone();
            p.setLocation(rect.x,  p.y);
            isStuck = true;
        }
        if (p.x > rect.x + rect.width) {
            p = (Point) p.clone();
            p.setLocation(rect.x + rect.width, p.y);
            isStuck = true;
        }
        move(p.x, p.y);
    }
}
