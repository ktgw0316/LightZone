/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import com.lightcrafts.ui.toolkit.IconFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * A primitive container that shows some dialog buttons in front of an image.
 * The image will either be a random choice among the configured backgrounds,
 * or the special "expired" image.
 */
class DialogBackgroundContainer extends JPanel {

    static Dimension BackgroundSize = new Dimension(640, 480);

    // The background image
    private JComponent background;

    // Container for the buttons
    private Box buttonBox;

    // Text that used to be the JOptionPane message
    private JLabel messageLabel;

    DialogBackgroundContainer(
        String message, List<JComponent> buttons, boolean expired
    ) {
        setLayout(null);

        messageLabel = new JLabel(message);
        messageLabel.setForeground(Color.white);
        add(messageLabel);

        buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(Box.createHorizontalStrut(8));
        for (JComponent button : buttons) {
            buttonBox.add(button);
            buttonBox.add(Box.createHorizontalStrut(8));
        }
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buttonBox);

        BufferedImage image = expired ?
            getSpecificBackground() : getRandomBackground();
        final BufferedImage scaledImage =
            IconFactory.getScaledImage(image, BackgroundSize.width);
        background = new JComponent() {
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics;
                g.drawRenderedImage(scaledImage, new AffineTransform());
            }
            public Dimension getPreferredSize() {
                return BackgroundSize;
            }
        };
        add(background);
    }

    public void doLayout() {
        Dimension size = getSize();

        Dimension buttonSize = buttonBox.getPreferredSize();
        buttonBox.setLocation(0, size.height - buttonSize.height);
        buttonBox.setSize(size.width, buttonSize.height);

        background.setLocation(0, 0);
        background.setSize(size.width, size.height);

        Dimension messageSize = messageLabel.getPreferredSize();
        messageLabel.setLocation(
            (size.width - messageSize.width) / 2,
            size.height - buttonSize.height - messageSize.height
        );
        messageLabel.setSize(messageSize);
    }

    public Dimension getPreferredSize() {
        return BackgroundSize;
    }

    private static BufferedImage getRandomBackground() {
        Random rand = new Random(System.currentTimeMillis());
        int i = rand.nextInt(3) + 1;
        try {
            Class clazz = eSellerateLicenseDialogs.class;
            URL url = clazz.getResource("resources/" + i + ".jpg");
            BufferedImage image = ImageIO.read(url);
            image = IconFactory.getScaledImage(image, 640);
            return image;
        }
        catch (Throwable t) {   // IOException, null URL
            t.printStackTrace();
            throw new RuntimeException(
                "Couldn't read license image \"" + i + ".jpg\""
            );
        }
    }

    private static BufferedImage getSpecificBackground() {
        try {
            Class clazz = eSellerateLicenseDialogs.class;
            URL url = clazz.getResource("resources/4.jpg");
            BufferedImage image = ImageIO.read(url);
            image = IconFactory.getScaledImage(image, 640);
            return image;
        }
        catch (Throwable t) {   // IOException, null URL
            t.printStackTrace();
            throw new RuntimeException(
                "Couldn't read license image \"4.jpg\""
            );
        }
    }

    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        List<JComponent> buttons = new LinkedList<JComponent>();
        String[] texts = new String[] {
            "Continue Trial", "Buy Now", "Enter Serial #"
        };
        for (String text : texts) {
            JButton button = new JButton(text);
            button.setContentAreaFilled(false);
            buttons.add(button);
        }
        JFrame frame = new JFrame();
        frame.getContentPane().add(
            new DialogBackgroundContainer("Trial will expire", buttons, false)
        );
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
