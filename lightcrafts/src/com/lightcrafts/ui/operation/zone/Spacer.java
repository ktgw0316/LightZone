/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation.zone;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;

/** A fixed-height spacer JComponent that is useful between GradientFills.
  * It can be moved in response to mouse drag events, it has a control to
  * stick and unstick, and it updates a ZoneModel as it moves.
  */

class Spacer extends JPanel
    implements ZoneModelListener, FocusListener, MouseListener
{
    final static int SpacerHeight = 18;
    final static int SpacerOutcrop = 22;

    private final static Cursor SliderCursor =
        Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);

    private final static Cursor ClickCursor =
        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

    private final ZoneModel model;
    private final int index;
    private final JButton unstickButton;  // If stuck, a button to unstick.

    // The set of spacers in a ZoneWidget share knobs.
    static class SpacerHandle {
        Spacer spacer;
    }
    private final SpacerHandle knobHandle;
    private final KnobPainter knobPainter;

    Spacer(
        ZoneModel model, int index, SpacerHandle knobHandle
    ) {
        this.model = model;
        this.index = index;
        this.knobHandle = knobHandle;
        
        knobPainter = new KnobPainter(this);

        setOpaque(false);
        setCursor(SliderCursor);

        initKeyMaps();

        Icon icon = getIcon("unstick");
        Icon pressedIcon = getIcon("unstickPressed");
        Icon highlightIcon = getIcon("unstickHighlight");
        unstickButton = new JButton(icon);
        unstickButton.setSize(
            icon.getIconWidth(), icon.getIconHeight()
        );
        unstickButton.setPressedIcon(pressedIcon);
        unstickButton.setRolloverEnabled(true);
        unstickButton.setRolloverIcon(highlightIcon);
        unstickButton.setBorder(null);
        unstickButton.setBorderPainted(false);
        unstickButton.setRolloverEnabled(true);
        unstickButton.setOpaque(false);
        unstickButton.setCursor(ClickCursor);
        unstickButton.setFocusable(false);
        unstickButton.addActionListener(event -> Spacer.this.model.removePoint(Spacer.this.index));
        if (model.containsPoint(index)) {
            add(unstickButton);
        }
        model.addZoneModelListener(this);

        setFocusable(true);
        addFocusListener(this);
        addMouseListener(this);
    }

    int getIndex() {
        return index;
    }

    int getOutcrop() {
        if (isStuck()) {
            return SpacerOutcrop;
        }
        return 0;
    }

    @Override
    public void doLayout() {
        if (isStuck()) {
            Dimension size = getSize();
            Dimension unStickSize = unstickButton.getPreferredSize();
            int x = size.width - unStickSize.width;
            int y = size.height / 2 - unStickSize.height / 2;
            unstickButton.setSize(unStickSize);
            unstickButton.setLocation(x, y);
        }
    }

    @Override
    public void zoneModelBatchStart(ZoneModelEvent event) {
    }

    @Override
    public void zoneModelChanged(ZoneModelEvent event) {
        if ((! isStuck()) && model.containsPoint(index)) {
            add(unstickButton);
            revalidate();
            repaint();
        }
        else if (isStuck() && ! model.containsPoint(index)) {
            remove(unstickButton);
            revalidate();
            repaint();
        }
    }

    @Override
    public void zoneModelBatchEnd(ZoneModelEvent event) {
    }

    @Override
    public void focusGained(FocusEvent e) {
        setFocusedKnob();
    }

    @Override
    public void focusLost(FocusEvent e) {
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (knobHandle.spacer != null) {
            knobHandle.spacer.knobPainter.knobOff(false);
        }
        knobHandle.spacer = this;
        knobPainter.knobOn(false);
    }

    @Override
    public void mouseExited(MouseEvent e) {
//        if (knobHandle.spacer == this) {
//            knobPainter.knobOff(false);
//        }
//        knobHandle.spacer = null;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics;

        // If we are disabled, we don't draw our arrow, focus, and unstick
        // decorations:
        if (! isEnabled()) {
            return;
        }
        Color oldColor = g.getColor();
        RenderingHints oldHints = g.getRenderingHints();
        g.setColor(Color.black);
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON
        );
        if (isStuck()) {
            paintHorizontalLine(g);
        }
        g.setRenderingHints(oldHints);
        g.setColor(oldColor);

        knobPainter.paint(g);
    }

    void moveTo(int newY) {
        Point p = getLocation();
        setLocation(p.x, newY);
        try {
            updateModel();
        }
        catch (IllegalArgumentException e) {
            // Can't move Spacers past each other:
            setLocation(p);
        }
    }

    void updateModel() {
        double value = ComponentScaler.componentToScale(this, SpacerHeight / 2);
        model.setPoint(index, value);
    }

    void setFocusedKnob() {
        if (knobHandle.spacer != null) {
            knobHandle.spacer.knobPainter.knobOff(true);
        }
        knobHandle.spacer = this;
        knobPainter.knobOn(true);
    }

    private boolean isStuck() {
        return (unstickButton.getParent() != null);
    }

    private void paintHorizontalLine(Graphics g) {
        Dimension size = getSize();
        int y = size.height / 2;
        g.drawLine(0, y, size.width, y);
    }

    private void initKeyMaps() {
        // Down-arrow nudges this Spacer downward:
        registerKeyboardAction(
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    Point p = getLocation();
                    moveTo(p.y + 1);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
            WHEN_FOCUSED
        );
        // Up-arrow nudges this Spacer upward:
        registerKeyboardAction(
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    Point p = getLocation();
                    moveTo(p.y - 1);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
            WHEN_FOCUSED
        );
        // Space sticks this Spacer where it is:
        registerKeyboardAction(
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    Point p = getLocation();
                    moveTo(p.y);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_X, 0),
            WHEN_FOCUSED
        );
        // Delete unsticks this Spacer:
        registerKeyboardAction(
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    model.removePoint(index);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            WHEN_FOCUSED
        );
        // Backspace is same as Delete:
        registerKeyboardAction(
            new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    model.removePoint(index);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0),
            WHEN_FOCUSED
        );
    }

    // Loading resources over and over again turns out to be really expensive
    // at document initialization, use a cache.
    private static final Map<String, Icon> IconCache = new WeakHashMap<>();

    private static Icon getIcon(String name) {
        String path = "resources/" + name + ".png";
        synchronized (IconCache) {
            Icon theIcon = IconCache.get(path);
            if (theIcon == null) {
                URL url = Spacer.class.getResource(path);
                if (url != null) {
                    theIcon = new ImageIcon(url);
                    IconCache.put(path, theIcon);
                }
            }
            return theIcon;
        }
    }
}
