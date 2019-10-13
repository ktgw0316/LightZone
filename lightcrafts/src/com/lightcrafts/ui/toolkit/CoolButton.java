/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.app.ComboFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.util.Set;
import java.util.HashSet;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.utils.SubstanceConstants;

public class CoolButton extends JButton {

    public enum ButtonStyle { NORMAL, LEFT, CENTER, RIGHT }

    private ButtonStyle style;

    private CoolButtonBackground bkgnd;

    public CoolButton() {
        this(ButtonStyle.NORMAL);
    }

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    public CoolButton(ButtonStyle style) {
        setStyle(style);
    }

    public ComboFrame getComboFrame() {
        return (ComboFrame)SwingUtilities.getAncestorOfClass(
            ComboFrame.class, this
        );
    }

    // Setting the icon on a CoolButton sets its text to null.
    public void setIcon(Icon icon) {
        super.setIcon(icon);

        if (icon != null && icon instanceof ImageIcon) {
            if (style == ButtonStyle.NORMAL)
                putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);

            BufferedImage iconImage = new BufferedImage(icon.getIconWidth(),
                                                        icon.getIconHeight(),
                                                        BufferedImage.TYPE_4BYTE_ABGR);

            Graphics2D g = (Graphics2D) iconImage.getGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();

            float scale = 0.5f;
            RescaleOp rop = new RescaleOp(new float[]{scale, scale, scale, scale},
                                new float[]{0, 0, 0, 0},
                                null);
            BufferedImage image = rop.filter(iconImage, null);

            this.setDisabledIcon(new ImageIcon(image));
        }

        if (icon != null) {
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

    public void setStyle(ButtonStyle style) {
        this.style = style;

        Set<SubstanceConstants.Side> openSides = new HashSet<SubstanceConstants.Side>();

        switch (style) {
            case NORMAL:
                bkgnd = new CoolButtonNormalBackground(this);
                break;

            case LEFT:
                openSides.add(SubstanceConstants.Side.RIGHT);
                putClientProperty(SubstanceLookAndFeel.BUTTON_OPEN_SIDE_PROPERTY, openSides);
                bkgnd = new CoolButtonLeftBackground(this);
                break;

            case CENTER:
	        openSides.add(SubstanceConstants.Side.LEFT);
                openSides.add(SubstanceConstants.Side.RIGHT);
	        putClientProperty(SubstanceLookAndFeel.BUTTON_OPEN_SIDE_PROPERTY, openSides);
                bkgnd = new CoolButtonCenterBackground(this);
                break;

            case RIGHT:
                openSides.add(SubstanceConstants.Side.LEFT);
                putClientProperty(SubstanceLookAndFeel.BUTTON_OPEN_SIDE_PROPERTY, openSides);
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
            System.exit(1);
        }
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());

        Icon icon = new ImageIcon(ImageIO.read(new File(args[0])));

        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(LightZoneSkin.Colors.FrameBackground);

        JButton button = new CoolButton(ButtonStyle.NORMAL);
        button.setText("Cool");
        // ToggleTitleBorder.setBorder(button, "test");
        panel.add(button);

        button = new JButton();
        button.setText("Normal");
        // ToggleTitleBorder.setBorder(button, "test");
        panel.add(button);

        button = new CoolButton(ButtonStyle.NORMAL);
        button.setIcon(icon);
        // ToggleTitleBorder.setBorder(button, "test");
        panel.add(button);

        button = new CoolButton(ButtonStyle.NORMAL);
        button.setIcon(icon);
        button.setEnabled(false);
        panel.add(button);

        button = new CoolButton(ButtonStyle.LEFT);
        button.setIcon(icon);
        panel.add(button);

        button = new CoolButton(ButtonStyle.CENTER);
        button.setIcon(icon);
        panel.add(button);

        button = new CoolButton(ButtonStyle.RIGHT);
        button.setIcon(icon);
        panel.add(button);

        JFrame frame = new JFrame();
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
