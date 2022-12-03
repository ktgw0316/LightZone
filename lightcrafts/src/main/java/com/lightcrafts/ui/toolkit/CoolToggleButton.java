/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.toolkit;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;

public class CoolToggleButton extends JToggleButton {

    private CoolButton.ButtonStyle style;

    private CoolButtonBackground bkgnd;

    public CoolToggleButton() {
        this(CoolButton.ButtonStyle.NORMAL);
    }

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    public CoolToggleButton(CoolButton.ButtonStyle style) {
        setStyle(style);
        setFocusable(false);
        setFocusPainted(false);
    }

    public ComboFrame getComboFrame() {
        return (ComboFrame)SwingUtilities.getAncestorOfClass(
            ComboFrame.class, this
        );
    }

    // Setting the icon on a CoolButton sets its text to null.
    public void setIcon(Icon icon) {
        super.setIcon(icon);

        if (icon != null) {
            BufferedImage iconImage = new BufferedImage(icon.getIconWidth(),
                                                        icon.getIconHeight(),
                                                        BufferedImage.TYPE_4BYTE_ABGR);

            Graphics2D g = (Graphics2D) iconImage.getGraphics();
            icon.paintIcon(this, g, 0, 0);
            g.dispose();

            float scale = 0.5f;
            RescaleOp rop = new RescaleOp(new float[]{scale, scale, scale, scale},
                                new float[]{0, 0, 0, 0},
                                null);
            BufferedImage image = rop.filter(iconImage, null);

            this.setDisabledIcon(new ImageIcon(image));

            super.setText(null);
        }
    }

    // Setting the text on a CoolButton sets its icon to null.
    public void setText(String text) {
        super.setText(text);
        if (text != null) {
            super.setIcon(null);
        }
    }

    // The ButtonStyle effects the background, which determines the order
    // insets on this button.  Setting the style resets the border.
    public void setStyle(CoolButton.ButtonStyle style) {
        this.style = style;

        switch (style) {
            case NORMAL:
                bkgnd = new CoolButtonNormalBackground(this);
                break;
            case LEFT:
                bkgnd = new CoolButtonLeftBackground(this);
                break;
            case CENTER:
                bkgnd = new CoolButtonCenterBackground(this);
                break;
            case RIGHT:
                bkgnd = new CoolButtonRightBackground(this);
                break;
        }
        Insets insets = bkgnd.getInsets();
        Border border = BorderFactory.createEmptyBorder(
            insets.top, insets.left, insets.bottom, insets.right
        );
        setBorder(border);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: (file)");
        }

        LookAndFeel laf = Platform.getPlatform().getLookAndFeel();

        UIManager.setLookAndFeel(laf);

        BufferedImage image = ImageIO.read(new File(args[0]));
        Icon icon = new ImageIcon(image);

        CoolToggleButton button1 = new CoolToggleButton();
        button1.setIcon(icon);

        CoolToggleButton button2 = new CoolToggleButton();
        button2.setText("Some Text");

        CoolToggleButton button3 = new CoolToggleButton();
        button3.setIcon(icon);
        button3.setEnabled(false);

        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(LightZoneSkin.Colors.FrameBackground);
        panel.add(button1);
        panel.add(button2);
        panel.add(button3);

        JFrame frame = new JFrame();
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
