/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.scroll;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class PannerOverlay extends JLayeredPane {

    private JScrollPane scroll;
    private Panner panner;

    public PannerOverlay(CenteringScrollPane scroll) {
        this.scroll = scroll;
        panner = new Panner(scroll);
        setLayout(null);
        add(scroll, DEFAULT_LAYER);
        add(panner, PALETTE_LAYER);

        // Add and remove the panner when its enabled state changes,
        // so it won't intercept mouse events directed at the viewport
        // when it is disabled.
        panner.addPropertyChangeListener(
            "enabled",
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    boolean isEnabled = (Boolean) evt.getNewValue();
                    if (isEnabled) {
                        add(panner, PALETTE_LAYER);
                    }
                    else {
                        remove(panner);
                    }
                }
            }
        );
    }

    public Dimension getPreferredSize() {
        Dimension size = scroll.getPreferredSize();
        Insets insets = getInsets();
        return new Dimension(
            size.width + insets.left + insets.right,
            size.height + insets.top + insets.bottom
        );
    }

    public void doLayout() {
        Dimension size = getSize();
        Insets insets = getInsets();
        scroll.setBounds(
            insets.left,
            insets.top,
            size.width - insets.left - insets.right,
            size.height - insets.top - insets.bottom
        );
        Dimension pannerSize = panner.getPreferredSize();
        panner.setLocation(
            size.width - pannerSize.width - insets.right - 20,
            size.height - pannerSize.height - insets.top - 20
        );
        panner.setSize(pannerSize);
    }

    public static void main(String[] args) {
        JComponent comp = new JTree();
        comp.setBackground(Color.gray);
        CenteringScrollPane scroll = new CenteringScrollPane(comp);
        PannerOverlay panner = new PannerOverlay(scroll);

        JFrame frame = new JFrame("PannerOverlay");
        frame.setContentPane(panner);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
