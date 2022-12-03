/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.mode.test;

// A Point that takes a stream of calls to update(Point) and sometimes
// responds by changing its location to the given Point, exhibiting a sticky
// behavior near the boundaries of its Rectangle.

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;

class Dot extends JComponent {

    private Point p = new Point();

    void setPoint(Point p) {
        this.p = p;
        repaint();
    }

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        Rectangle r = new Rectangle(p.x - 5, p.y - 5, 10, 10);
        g.fill(r);
    }
}

class MagneticPointTest {

    public static void main(String[] args) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        final Dot dot = new Dot();
        panel.add(dot);

        JFrame frame = new JFrame("Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 400, 400);
        frame.setVisible(true);

        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    Rectangle bounds = panel.getBounds();
                    final MagneticPoint mag = new MagneticPoint(bounds);
                    MouseInputListener listener = new MouseInputAdapter() {
                        public void mouseMoved(MouseEvent event) {
                            mag.update(event.getPoint());
                            dot.setPoint(mag);
                        }
                        public void mouseExited(MouseEvent event) {
                            mag.update(event.getPoint());
                            dot.setPoint(mag);
                        }
                        public void mouseDragged(MouseEvent event) {
                            mag.update(event.getPoint());
                            dot.setPoint(mag);
                        }
                    };
                    dot.addMouseListener(listener);
                    dot.addMouseMotionListener(listener);
                }
            }
        );
    }
}
