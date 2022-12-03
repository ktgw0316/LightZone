/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.toolkit.ImageOnlyButton;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A button that is rendered as non-opaque text in a big font.
 */
class BigLinkedText extends JToggleButton {
    
    BigLinkedText(String text) {
        setText(null);
        setRolloverEnabled(true);
        setIcon(getNormalIcon(text));
        setPressedIcon(getPressedIcon(text));
        setSelectedIcon(getSelectedIcon(text));
        setRolloverIcon(getRolloverIcon(text));
        ImageOnlyButton.setStyle(this);
    }

    private static Icon getNormalIcon(String text) {
        return getIcon(text, Color.gray);
    }

    private static Icon getPressedIcon(String text) {
        return getIcon(text, Color.white);
    }

    private static Icon getSelectedIcon(String text) {
        return getIcon(text, Color.white);
    }

    private static Icon getRolloverIcon(String text) {
        return getIcon(text, Color.lightGray);
    }

    private static Icon getIcon(String text, Color color) {
        JLabel label = getBigLabel(text, color);
        BufferedImage image = getImage(label);
        Graphics2D g = (Graphics2D) image.getGraphics();
        // Don't color the background, just be transparent
        // g.setColor(Colors.FrameBackground);
        // g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
        );
        label.paint(g);
        g.dispose();
        return new ImageIcon(image);
    }

    private static JLabel getBigLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        Font font = label.getFont();
        font = font.deriveFont(30f);
        label.setFont(font);
        label.setForeground(color);
        Dimension size = label.getPreferredSize();
        label.setSize(size);
        return label;
    }

    private static BufferedImage getImage(JLabel label) {
        Dimension size = label.getSize();
        return new BufferedImage(
            size.width, size.height, BufferedImage.TYPE_INT_ARGB
        );
    }

    public static void main(String[] args) {
        BigLinkedText text = new BigLinkedText("hello");
        JFrame frame = new JFrame("BigLinkedText");
        frame.getContentPane().add(text);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
