/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.toolkit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;

/**
 * A container that holds a single component, but where the component may be
 * updated in nextComponent(), and transitions from one component to the next
 * are animated.
 * <p>
 * The nextComponent() method takes an optional listener, to be notified when
 * a transition completes.
 */
public class FadingContainer extends JPanel {

    // The time for the animated transition to complete, in milliseconds
    public static long Duration = 250;
    // The number of animation paints to perform in this interval
    public static int Steps = 10;

    // The current child component
    private JComponent comp;

    // Animation data
    private BufferedImage before;
    private BufferedImage after;
    private Thread animator;
    private float alpha;  // 0 is before, 1 is after

    private ActionListener listener;

    public FadingContainer(Color background) {
        this(createColorComponent(background));
    }

    public FadingContainer(JComponent comp) {
        this.comp = comp;
        setLayout(new BorderLayout());
        add(comp);
    }

    public JComponent getComponent() {
        return comp;
    }

    // Render a fading transition from the current component to the given one.
    public void nextComponent(JComponent next, ActionListener listener) {
        this.listener = listener;
        Dimension beforeSize = comp.getSize();
        if ((beforeSize.width <= 0) || (beforeSize.height <= 0)) {
            // Transitioning before layout: shortcut to the end, don't render
            // images, don't animate.
            remove(comp);
            comp = next;
            add(comp);
            validate();
            return;
        }
        before = new BufferedImage(
            beforeSize.width, beforeSize.height, BufferedImage.TYPE_INT_ARGB
        );
        Graphics g = before.getGraphics();
        comp.paint(g);
        g.dispose();

        remove(comp);

        comp = next;

        alpha = 1e-3f;   // ensure the very next paint will be blended

        add(comp);
        validate();

        Dimension afterSize = comp.getSize();
        if ((afterSize.width <= 0) || (afterSize.height <= 0)) {
            // Transitioning before layout: don't render images, don't animate.
            return;
        }
        after = new BufferedImage(
            afterSize.width, afterSize.height, BufferedImage.TYPE_INT_ARGB
        );
        g = after.getGraphics();
        comp.paint(g);
        g.dispose();

        this.listener = listener;

        startFade();
    }

    // Render a fading transition to a solid background.
    public void nextComponent(Color background, ActionListener listener) {
        JComponent backComp = createColorComponent(background);
        nextComponent(backComp, listener);
    }

    protected void paintChildren(Graphics graphics) {
        if ((alpha > 0) && (alpha < 1)) {
            Graphics2D g = (Graphics2D) graphics;
            Composite oldComposite = g.getComposite();
            g.setComposite(AlphaComposite.SrcAtop);
            g.drawRenderedImage(before, new AffineTransform());
            AlphaComposite blend = AlphaComposite.getInstance(
                AlphaComposite.SRC_ATOP, alpha
            );
            g.setComposite(blend);
            g.drawRenderedImage(after, new AffineTransform());
            g.setComposite(oldComposite);
        }
        else {
            super.paintChildren(graphics);
        }
    }

    private void startFade() {
        animator = new Thread(
            new Runnable() {
                public void run() {
                    for (int step=1; step<Steps; step++) {
                        try {
                            alpha = step / (float) Steps;
                            repaint();
                            Thread.sleep(Duration / Steps);
                        }
                        catch (InterruptedException e) {
                            // just continue
                        }
                    }
                    alpha = 1;
                    repaint();
                    
                    if (listener != null) {  // see fadeAway()
                        listener.actionPerformed(null);
                    }
                }
            },
            "FadingContainer"
        );
        animator.start();
    }

    private static JComponent createColorComponent(final Color color) {
        JComponent comp = new JComponent() {
            protected void paintComponent(Graphics g) {
                Dimension size = getSize();
                Color oldColor = g.getColor();
                g.setColor(color);
                g.fillRect(0, 0, size.width, size.height);
                g.setColor(oldColor);
            }
        };
        return comp;
    }

    public static void main(String[] args) {
        final JLabel red = new JLabel("red");
        red.setBackground(Color.red);
        red.setOpaque(true);

        final JLabel green = new JLabel("green");
        green.setBackground(Color.green);
        green.setOpaque(true);

        final FadingContainer fader = new FadingContainer(Color.blue);

        final JPanel content = new JPanel(new BorderLayout());
        content.add(fader);

        JButton redButton = new JButton("red");
        redButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    fader.nextComponent(red, null);
                }
            }
        );
        JButton greenButton = new JButton("green");
        greenButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    fader.nextComponent(green, null);
                }
            }
        );
        JButton awayButton = new JButton("away");
        awayButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    fader.nextComponent(Color.blue, null);
                }
            }
        );
        Box buttons = Box.createVerticalBox();
        buttons.add(redButton);
        buttons.add(greenButton);
        buttons.add(awayButton);

        content.add(buttons, BorderLayout.SOUTH);

        JFrame frame = new JFrame("Test");
        frame.setContentPane(content);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
