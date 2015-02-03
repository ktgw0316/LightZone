/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.layout;

import com.lightcrafts.ui.toolkit.CoolToggleButton;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;

class VerticalToggleButton extends CoolToggleButton {

    VerticalToggleButton(String text, FadingTabbedPanel.Orientation orient) {
        Icon icon = new RotatedTextIcon(orient, text);
        setIcon(icon);
        // putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.FALSE);
        Dimension preferredSize = getPreferredSize();
        preferredSize.height = 120;
        setPreferredSize(preferredSize);
    }

    class RotatedTextIcon implements Icon {
        private FadingTabbedPanel.Orientation orient;

        private GlyphVector glyphs;
        private float width;
        private float height;
        private float ascent;

        RotatedTextIcon(FadingTabbedPanel.Orientation orient, String text) {
            this.orient = orient;

            Font font = getFont();
            FontRenderContext fcr = new FontRenderContext(null, true, true);
            glyphs = font.createGlyphVector(fcr, text);
            width = (int) glyphs.getLogicalBounds().getWidth() + 4;
            height = (int) glyphs.getLogicalBounds().getHeight();

            LineMetrics lineMetrics = font.getLineMetrics(text, fcr);
            ascent = lineMetrics.getAscent();
            height = (int) lineMetrics.getHeight();
        }

        public int getIconWidth() {
            return (int) height;
        }

        public int getIconHeight() {
            return (int) width;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g;
            Font font = getFont();
            g2d.setFont(font);
            AffineTransform oldTransform = g2d.getTransform();

            g2d.setColor(c.getForeground());

            AffineTransform trans = new AffineTransform();
            switch (orient) {
                case Up:
                    trans.concatenate(oldTransform);
                    trans.translate(x, y - 2);
                    trans.rotate(
                        Math.PI * 3 / 2, height / 2, width / 2);
                    g2d.setTransform(trans);
                    g2d.drawGlyphVector(
                        glyphs,
                        (height - width) / 2,
                        (width - height) / 2 + ascent
                    );
                    break;
                case Down:
                    trans.concatenate(oldTransform);
                    trans.translate(x, y + 2);
                    trans.rotate(
                        Math.PI / 2, height / 2, width / 2
                    );
                    g2d.setTransform(trans);
                    g2d.drawGlyphVector(
                        glyphs,
                        (height - width) / 2,
                        (width - height) / 2 + ascent
                    );
            }
            g2d.setTransform(oldTransform);
        }
    }

    public static void main(String[] args) {
        VerticalToggleButton up = new VerticalToggleButton(
            "hello", FadingTabbedPanel.Orientation.Up
        );
        VerticalToggleButton down = new VerticalToggleButton(
            "world", FadingTabbedPanel.Orientation.Down
        );
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        
        panel.add(up);
        panel.add(down);

        JFrame frame = new JFrame("VerticalToggleButton");
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
