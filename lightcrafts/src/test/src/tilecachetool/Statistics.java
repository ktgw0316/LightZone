/* @(#)Statistics.java	1.2 02/10/24 21:03:24 */
package tilecachetool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * <p>Title: Tile Cache Monitoring Tool</p>
 * <p>Description: Monitors and displays JAI Tile Cache activity.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>    All Rights Reserved</p>
 * <p>Company: Virtual Visions Software, Inc.</p>
 *
 * @author Dennis Sigel
 * @version 1.01
 *
 * Purpose:  Diplsay various statistics about the Tile Cache.
 */

public final class Statistics extends JPanel {

    private long[] stats = null;
    private int percent  = 0;

    private static final Color DARK_BLUE   = new Color(10, 10, 120);
    private static final Color DARK_RED    = new Color(240, 40, 40);
    private static final Color BACKGROUND  = new Color(180, 180, 220);
    private static final Font DEFAULT_FONT = new Font("monospaced",
                                                      Font.BOLD,
                                                      14);
    private static final String[] LABELS = {
        "     Tile Count: ",
        "     Cache Hits: ",
        "   Cache Misses: ",
        "       Add Tile: ",
        "    Remove Tile: ",
        "  Remove(flush): ",
        "Remove(control): ",
        "     Remove(gc): ",
        "Update(addTile): ",
        "Update(getTile): ",
        " Used Memory(%): "
    };


    public Statistics() {
        setLayout(null);
        setPreferredSize( new Dimension(270, 200) );

        EmptyBorder emptyBorder = new EmptyBorder(5, 5, 5, 5);
        LineBorder lineBorder   = new LineBorder(Color.gray, 1);

        TitledBorder titledBorder = BorderFactory.createTitledBorder(lineBorder,
                                                                     "Statistics",
                                                                     TitledBorder.LEFT,
                                                                     TitledBorder.TOP,
                                                                     DEFAULT_FONT,
                                                                     Color.black);

        CompoundBorder compoundBorder = new CompoundBorder(emptyBorder,
                                                           titledBorder);

        setBorder(compoundBorder);

        stats = new long[LABELS.length];
    }

    public void set(long tileCount,
                    long cacheHits,
                    long cacheMisses,
                    long addTile,
                    long removeTile,
                    long removeFlushed,
                    long removeMemoryControl,
                    long removeGC,
                    long updateAddTile,
                    long updateGetTile,
                    int percent) {

        stats[0] = tileCount;
        stats[1] = cacheHits;
        stats[2] = cacheMisses;
        stats[3] = addTile;
        stats[4] = removeTile;
        stats[5] = removeFlushed;
        stats[6] = removeMemoryControl;
        stats[7] = removeGC;
        stats[8] = updateAddTile;
        stats[9] = updateGetTile;
        stats[10] = (long) percent;

        this.percent = percent;
        repaint();
    };

    public void set(int percent) {
        this.percent = percent;
        stats[10] = (long) percent;
        repaint();
    }

    public synchronized void clear() {
        for ( int i = 0; i < stats.length; i++ ) {
            stats[i] = (long)0;
        }

        percent = (int)0;
        repaint();
    }

    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2d = (Graphics2D) g;
        Insets insets  = getInsets();

        // clear drawing area
        g2d.setColor(BACKGROUND);
        g2d.fillRect(insets.left,
                     insets.top,
                     getWidth() - insets.left - insets.right,
                     getHeight() - insets.top - insets.bottom);

        int x1 = insets.left + 10;
        int y1 = insets.top  + 10;
        int x2 = x1 + 20;
        int y2 = getHeight() - insets.bottom - 5;

        int w = 20;
        int h = y2 - y1 + 1;

        // clear the percentage bar graph
        g2d.setColor(DARK_BLUE);
        g2d.fillRect(x1, y1, w, h);

        // draw a border
        g2d.setColor(Color.white);
        g2d.drawRect(x1-1, y1-1, w+1, h+1);

        // fill tile cache percentage graph
        g2d.setColor(DARK_RED);
        int p = (int)((float)h * ((float)percent / 100.0F));

        if ( p > 0 ) {
            g2d.fillRect(x1, y2-p, w, p);
        };

        // draw text
        int x = insets.left + 50;
        int y = insets.top  + 20;

        g2d.setColor(Color.black);
        g2d.setFont(DEFAULT_FONT);

        int fontHeight = getFontMetrics(DEFAULT_FONT).getAscent();

        for ( int i = 0; i < LABELS.length; i++ ) {
            g2d.drawString(LABELS[i] + stats[i], x, y);
            y += fontHeight;
        }
    }
}

