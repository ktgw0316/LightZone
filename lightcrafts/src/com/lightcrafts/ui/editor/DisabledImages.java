/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.RenderedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

/**
 * This is a Container for DisabledImageComponents.  It serves as a
 * repository for layout logic, and can also display a label when
 * it holds no images.
 */
class DisabledImages extends JPanel implements Scrollable {

    private final static int Gap = 8;   // space between images

    private final static int MaxImages = 5; // Limit on disabled images

    // Track images by keys, so they can be updated and expired:
    private LinkedHashMap<Object, DisabledImageComponent> componentMap;

    // Show this, if asked, when there are no images:
    private DisabledLabel label;

    private EllipticLabel ellipsis;

    private MouseListener mouseListener;

    DisabledImages(final DisabledEditor.Listener listener) {
        setLayout(new GridLayout());
        setOpaque(true);
        setBackground(LightZoneSkin.Colors.EditorBackground);
        componentMap = new LinkedHashMap<Object, DisabledImageComponent>();
        mouseListener = new MouseAdapter() {
            // Don't use mouseClicked() here because it doesn't work
            // on Oracle's Java 8 on Mac OS X
            @Override
            public void mouseReleased(MouseEvent event) {
                Component comp = event.getComponent();
                if (!comp.contains(event.getPoint())) {
                    return;
                }
                Object key = getKeyForComponent(comp);
                if (key != null) {
                    listener.imageClicked(key);
                }
            }
        };
        ellipsis = new EllipticLabel();
    }

    private boolean firstTime = true;

    final Timer paintTimer = new Timer(300, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            firstTime = false;
            paintTimer.stop();
            repaint();
        }
    });

    protected void paintChildren(Graphics graphics) {
        if (firstTime) {
            if (!paintTimer.isRunning())
                paintTimer.start();
            return;
        }
        super.paintChildren(graphics);
    }

    private Object getKeyForComponent(Component comp) {
        Set<Map.Entry<Object, DisabledImageComponent>> set =
            componentMap.entrySet();
        for (Map.Entry<Object, DisabledImageComponent> entry : set) {
            if (entry.getValue() == comp) {
                return entry.getKey();
            }
        }
        return null;
    }

    void addImage(Object key, RenderedImage image) {
        DisabledImageComponent comp = new DisabledImageComponent(image);
        comp.addMouseListener(mouseListener);
        componentMap.put(key, comp);
        add(comp);

        // Limit the number of simultaneous previews.
        while (componentMap.size() > MaxImages) {
            Iterator<Map.Entry<Object, DisabledImageComponent>> i =
                componentMap.entrySet().iterator();
            Map.Entry<Object, DisabledImageComponent> entry = i.next();
            DisabledImageComponent oldComp = entry.getValue();
            oldComp.removeMouseListener(mouseListener);
            remove(oldComp);
            i.remove();
            ellipsis.increment();
        }
        if (ellipsis.getCount() > 0) {
            remove(ellipsis);
            add(ellipsis);
        }
        int count = getComponentCount();
        int cols = getColumnCount();
        int rows = (int) Math.ceil(count / (double) cols);

        if (label != null) {
            remove(label);
        }
        setLayout(new GridLayout(rows, cols, Gap, Gap));
    }

    void updateImage(Object key, RenderedImage image) {
        DisabledImageComponent comp = componentMap.get(key);
        if (comp != null) {
            comp.setImage(image);
        }
    }

    void removeAllImages() {
        removeAll();
        for (DisabledImageComponent comp : componentMap.values()) {
            comp.removeMouseListener(mouseListener);
        }
        componentMap.clear();
        setLayout(new GridLayout());

        if (label != null) {
            setLayout(new BorderLayout());
            add(label);
        }
        ellipsis.reset();
    }

    boolean hasKey(Object key) {
        return componentMap.containsKey(key);
    }

    void setDisabledText(String text) {
        if (label != null) {
            remove(label);
            label = null;
        }
        if (text != null) {
            label = new DisabledLabel(text);
            if (getComponentCount() == 0) {
                setLayout(new BorderLayout());
                add(label);
                revalidate();
            }
        }
    }

    // Here is the layout magic: get a column count for the current image count.
    private int getColumnCount() {
        int count = getComponentCount();
        if (count == Math.round(Math.sqrt(count))) {
            return (int) Math.round(Math.sqrt(count));
        }
        else {
            return (int) Math.ceil(Math.sqrt(count));
        }
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        return 0;
    }

    public int getScrollableBlockIncrement(
        Rectangle visibleRect, int orientation, int direction
    ) {
        return 0;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return true;
    }
}
