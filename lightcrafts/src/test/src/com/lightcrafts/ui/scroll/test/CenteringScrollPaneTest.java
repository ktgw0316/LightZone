/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.scroll.test;

import com.lightcrafts.ui.mode.Mode;
import com.lightcrafts.ui.mode.ModeOverlay;
import com.lightcrafts.ui.scroll.CenteringScrollPane;
import com.lightcrafts.ui.scroll.ScrollMode;

import javax.swing.*;
import java.awt.*;

public class CenteringScrollPaneTest {

    static int x;

    private static void createAndShowGUI() {
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createLineBorder(Color.red));
        panel.setPreferredSize(new Dimension(x, x));

        JButton inButton = new JButton("Zoom In");
        inButton.addActionListener(event -> {
            x *= 2;
            panel.setPreferredSize(new Dimension(x, x));
            panel.revalidate();
        });

        JButton outButton = new JButton("Zoom Out");
        outButton.addActionListener(event -> {
            x /= 2;
            panel.setPreferredSize(new Dimension(x, x));
            panel.revalidate();
        });

        Box buttons = Box.createHorizontalBox();
        buttons.add(inButton);
        buttons.add(outButton);

        ModeOverlay overlay = new ModeOverlay(panel);

        CenteringScrollPane scroll = new CenteringScrollPane(overlay);

        Mode mode = new ScrollMode(scroll);

        overlay.pushMode(mode);

        overlay.addMouseWheelListener(e -> System.out.println("wheel moved " + e.getWheelRotation()));

        JFrame frame = new JFrame("CenteringScrollPaneTest");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(scroll);
        frame.getContentPane().add(buttons, BorderLayout.NORTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 400, 400);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        x = 1000;
        SwingUtilities.invokeLater(CenteringScrollPaneTest::createAndShowGUI);
    }
}
