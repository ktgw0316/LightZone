/* @(#)MemoryChart.java	1.2 02/10/24 21:03:23 */
package tilecachetool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import javax.swing.JComponent;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 * <p>Title: Tile Cache Monitoring Tool</p>
 * <p>Description: Monitors and displays JAI Tile Cache activity.</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>     All Rights Reserved</p>
 * <p>Company: Virtual Visions Software, Inc.</p>
 *
 * @author Dennis Sigel
 * @version 1.01
 *
 * Monitors the JVM memory usage like a Strip Chart recorder
 */

public final class MemoryChart extends JComponent
                               implements Runnable,
                                          ComponentListener {

    private Thread thread;
    private long delay = 500;  // default value
    private double free_memory;
    private double total_memory;
    private RingBuffer rbuf = new RingBuffer(500);
    private Insets insets = null;
    private int data_length = -1;
    private int percent;
    private int x1 = 0;
    private int y1 = 0;
    private int x2 = 0;
    private int y2 = 0;
    private int w  = 0;
    private int h  = 0;

    private static final StringBuffer buf1 = new StringBuffer(32);
    private static final StringBuffer buf2 = new StringBuffer(32);
    private static final StringBuffer buf3 = new StringBuffer(32);

    private static final int BAR_WIDTH     = 5;
    private static final Runtime RUNTIME   = Runtime.getRuntime();
    private static final Color BLUE        = new Color(100, 150, 220);
    private static final Color DARK_BLUE   = new Color(20, 20, 120);
    private static final Rectangle SBOX    = new Rectangle(130, 20, 85, 45);
    private static final Font DEFAULT_FONT = new Font("monospaced",
                                                      Font.BOLD,
                                                      14);


    /**
     * Default constructor
     */
    public MemoryChart() {
        setBackground(Color.gray);
        setPreferredSize( new Dimension(480, 140) );

        EmptyBorder b2 = new EmptyBorder(10, 10, 10, 10);
        BevelBorder b1 = new BevelBorder(BevelBorder.LOWERED);
        setBorder( new CompoundBorder(b1, b2) );

        addComponentListener(this);

        insets = getInsets();

        buf1.append("Alloc Memory: ");
        buf2.append(" Used Memory: ");
        buf3.append("Percent Used: ");
    }

    public void componentResized(ComponentEvent e) {
        int width = getWidth() - BAR_WIDTH;

        // prevent unnecessary repeat allocations
        if ( data_length != width ) {
            data_length = width;
            //rbuf.setSize(data_length);
        }
    }

    public void componentShown(ComponentEvent e) {
        data_length = getWidth() - BAR_WIDTH;
    }

    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}

    public void start() {
        thread = new Thread(this);
        thread.setName("MemoryChartThread");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /**
     * Run in a thread.
     */
    public void run() {
        Thread current_thread = Thread.currentThread();

        while( (thread == current_thread && !isShowing()) ||
               (getSize().width < 5 || getSize().height < 5) ) {
            try {
                thread.sleep(delay);
            } catch( InterruptedException e ) {
                return;
            }
        }

        while( thread == current_thread && isShowing() ) {
            repaint();  //automatically double buffered

            try {
                thread.sleep(delay);
            } catch( InterruptedException e ) {
                break;
            }
        }

        thread = null;
    }

    /**
     * Alter the delay between memory updates
     * @param interval Delay between time updates for JVM memory plot
     */
    public void setDelay(long interval) {
        if ( interval > 0 ) {
            delay = interval;
        } else {
            delay = 1;
        }
    }

    /**
     * Stop the thread
     */
    public synchronized void stop() {
        if ( thread != null ) {
            thread.interrupt();
        }

        thread = null;
        notifyAll();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        int i;

        // bounds of graph area
        x1 = insets.left;
        y1 = insets.top;
        x2 = getWidth() - insets.right - BAR_WIDTH - 1;
        y2 = getHeight() - insets.bottom - 1;

        w = x2 - x1 + 1;
        h = y2 - y1 + 1;

        g2d.setBackground(Color.darkGray);
        g2d.clearRect(x1, y1, w, h);

        free_memory  = RUNTIME.freeMemory();
        total_memory = RUNTIME.totalMemory();

        // percentage of memory used
        float v = 100.0F * (float)((total_memory - free_memory) / total_memory);
        rbuf.write(v);

        // draw percent bar graph
        int bh = (int)((float) h * rbuf.read(rbuf.getCount()-1) / 100.0F);
        drawBarGraph(g2d, x2, y2, bh);

        // draw grids
        drawGrids(g2d, x1, y1, w, h);

        //draw legend (memory statistics)
        drawLegend(g2d);

        // draw graph data
        drawData(g2d);
    }

    // draw graph grid lines
    private void drawGrids(Graphics2D g2d, int x1, int y1, int w, int h) {
        g2d.setColor(Color.cyan);
        g2d.drawRect(x1, y1, w + BAR_WIDTH - 1, h);

        float slope = (float)(y2 - y1) / 10.0F;

        g2d.setColor(BLUE);
        for ( int i = 1; i < 10; i++ ) {
            int y = (int) ((float)i * slope) + y1;
            g2d.drawLine(x1, y, x2-1, y);
        }
    };

    // draw a vertical bar graph on right side of plot
    private void drawBarGraph(Graphics2D g2d, int x2, int y2, int v) {
        g2d.setColor(BLUE);
        g2d.fillRect(x2, y1, BAR_WIDTH, h);

        g2d.setColor(Color.yellow);
        g2d.fillRect(x2, y2 - v, BAR_WIDTH, v+1);
    }

    // draw statistics data
    private void drawLegend(Graphics2D g2d) {
        percent = (int)(100.0F * (float)((total_memory - free_memory)/total_memory));

        //erase the background
        g2d.setColor(DARK_BLUE);
        g2d.fillRect(x1+5, y1+5, 200, 50);

        g2d.setColor(Color.white);
        g2d.setFont(DEFAULT_FONT);

        buf1.delete(14, 31);
        buf1.append((long)total_memory);
        g2d.drawString(buf1.toString(), insets.left + 10, insets.top + 20);

        buf2.delete(14, 31);
        buf2.append((long)(total_memory - free_memory));
        g2d.drawString(buf2.toString(), insets.left + 10, insets.top + 35);

        buf3.delete(14, 31);
        buf3.append(percent + "%");
        g2d.drawString(buf3.toString(), insets.left + 10, insets.top + 50);
    }

    // plot jvm memory usage
    private void drawData(Graphics2D g2d) {
        g2d.setColor(Color.yellow);

        float slope_x = (float)(w - BAR_WIDTH + 3) / ((float)rbuf.getSize() - 1.0F);
        float slope_y = (float)h / 100.0F;

        for ( int i = 1; i < rbuf.getCount(); i++ ) {
            float p1 = rbuf.read(i-1) / 100.0F;
            float p2 = rbuf.read( i ) / 100.0F;

            int px1 = x1 + (int)((float)(i-1) * slope_x);
            int py1 = y2 - (int) (h * p1);
            int px2 = x1 + (int)((float)(i) * slope_x);
            int py2 = y2 - (int) (h * p2);

            g2d.drawLine(px1, py1, px2, py2);
        }
    }
}

