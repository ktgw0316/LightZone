/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.ui.operation.drag.StackableComponent;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.app.ComboFrame;

import javax.swing.border.Border;
import javax.swing.*;
import java.awt.*;

public class SelectableControl
    extends JLayeredPane implements StackableComponent
{
    public final static Color Background = LightZoneSkin.Colors.ToolsBackground;

    public final static Font ControlFont = LightZoneSkin.fontSet.getSmallFont();

    private final static Border ControlBorder;
    private final static Border SelectedBorder;
    private final static int TitleHeight = 24;

    static {
        Border thinPadding = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        Border thickPadding = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        Border selectedBorder = BorderFactory.createLineBorder(LightZoneSkin.Colors.SelectedToolBorder, 2);
        Border unselectedBorder = BorderFactory.createLineBorder(Color.darkGray, 1);
        SelectedBorder = BorderFactory.createCompoundBorder(thinPadding, selectedBorder);
        ControlBorder = BorderFactory.createCompoundBorder(thickPadding, unselectedBorder);
    }

    SelectableTitle title;
    JComponent content;
    boolean isContentVisible;

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    public SelectableControl() {
        setBackground(Background);

        setContent(new JLabel("Default Control"));
        setShowContent(true);

        title = new SelectableTitle(this);
        title.setBackground(Background);
        title.setFont(ControlFont);
        add(title);

        setOpaque(false);
        setFont(ControlFont);
        setBorder(ControlBorder);

        setTitle("Default Control");

        // Enable mouse events, for the global AWTEventListener in OpStack.
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    }

    public ComboFrame getComboFrame() {
        return (ComboFrame)SwingUtilities.getAncestorOfClass(
            ComboFrame.class, this
        );
    }

    public boolean isFocusCycleRoot() {
        return true;
    }

    public void setTitle(String s) {
        title.setTitleText(s);
    }

    public JComponent getDraggableComponent() {
        return title;
    }

    public boolean isSwappable() {
        return false;
    }

    void setShowContent(boolean visible) {
        if (isContentVisible != visible) {
            isContentVisible = visible;
            if (visible) {
                add(content);
            }
            else {
                remove(content);
            }
            revalidate();
        }
    }

    boolean isContentShown() {
        return isContentVisible;
    }

    /**
     * Derived classes provide the display for an SelectedControl as a
     * JComponent.
     */
    protected void setContent(JComponent c) {
        if (isContentVisible) {
            remove(content);
        }
        content = c;
        if (isContentVisible) {
            add(content);
            content.setBackground(Background);
            content.setFont(ControlFont);
        }
    }

    /**
     * An accessor for the content lets class hierarchies supplement the
     * display from base classes.
     */
    protected JComponent getContent() {
        return content;
    }

    public void setRegionIndicator(boolean hasRegion) {
        title.setRegionIndicator(hasRegion);
    }

    void setSelected(boolean selected) {
        title.setSelected(selected);
        if (selected) {
            setBorder(SelectedBorder);
        }
        else {
            setBorder(ControlBorder);
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Paint the Background Color, but only inside the ControlBorder:

        Insets insets = getInsets();
        Dimension size = getSize();
        Color oldColor = g.getColor();
        g.setColor(Background);
        g.fillRect(
            insets.left,
            insets.top,
            size.width - insets.right - insets.left,
            size.height - insets.bottom - insets.top
        );
        g.setColor(oldColor);
    }

    public Dimension getPreferredSize() {
        int height = TitleHeight;
        if (isContentVisible) {
            Dimension contentSize = content.getPreferredSize();
            height += contentSize.height;
            height += SelectableTitleSeparator.Height;
        }
        Insets insets = getInsets();
        height += insets.top + insets.bottom;
        return new Dimension(Integer.MAX_VALUE, height);
    }

    public void doLayout() {

        Dimension size = getSize();
        Insets insets = getInsets();

        int minX = insets.left;
        int maxX = size.width - insets.right;
        int minY = insets.top;
        int maxY = size.height - insets.bottom;

        int width = maxX - minX;
        int height = maxY - minY;

        // The title gets fixed height and full width:

        title.setLocation(minX, minY);
        title.setSize(width, TitleHeight);

        if (! isContentVisible) {
            return;
        }

        // The content is centered in the remaining height:

        Dimension contentSize = content.getPreferredSize();
        int x = Math.max((width - contentSize.width) / 2, 0) + minX;
        int y = minY + TitleHeight + SelectableTitleSeparator.Height;
        int h = height - TitleHeight - SelectableTitleSeparator.Height;
        int w = Math.min(contentSize.width, width);
        content.setLocation(x, y);
        content.setSize(w, h);
    }

    protected String getHelpTopic() {
        return null;
    }
}
