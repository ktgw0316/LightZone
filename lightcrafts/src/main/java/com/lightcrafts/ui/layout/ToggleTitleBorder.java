/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.layout;

import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.*;
import java.util.prefs.Preferences;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.PreferenceChangeEvent;
import java.awt.*;

/**
 * A standard border for toolbar controls with a fixed style and a preference
 * listener so that the borders can be toggled on and off globally.
 * <p>
 * If you just call JComponent.setBorder() with one of these, it will work,
 * but it will not update when the global preference changes.  Use
 * setBorder() instead.
 */
public class ToggleTitleBorder
    extends CompoundBorder implements PreferenceChangeListener
{
    public static final Font font = LightZoneSkin.LightZoneFontSet.TitleFont;

    /**
     * Set the border on the given component to a ToggleTitledBorder that
     * will update dynamically when the global preference changes.
     */
    public static void setBorder(JComponent comp, String title) {
        ToggleTitleBorder border = new ToggleTitleBorder(comp, title);
        Prefs.addPreferenceChangeListener(border);
        comp.setBorder(border);
    }

    /**
     * Reset the border on the given component, removing its global
     * preference change listener and restoring its original border.
     */
    public static void unsetBorder(JComponent comp) {
        Border border = comp.getBorder();
        if (border instanceof ToggleTitleBorder) {
            ToggleTitleBorder ttborder = (ToggleTitleBorder) border;
            Prefs.removePreferenceChangeListener(ttborder);
            comp.setBorder(ttborder.original);
        }
    }

    /**
     * Remove borders recursively on this container and all of its descendants.
     */
    public static void unsetAllBorders(JComponent comp) {
        unsetBorder(comp);
        Component[] children = comp.getComponents();
        for (Component child : children) {
            if (child instanceof JComponent) {
                unsetAllBorders((JComponent) child);
            }
        }
    }

    public static void setShowBorders(boolean show) {
        Prefs.putBoolean(ShowBordersKey, show);
    }

    public static boolean isShowBorders() {
        return Prefs.getBoolean(ShowBordersKey, true);
    }

    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/layout"
    );
    private final static String ShowBordersKey = "ShowToggleTitleBorders";

    static class PlaceholderBorder extends EmptyBorder {
        ToggleTitleBorder ttb;
        PlaceholderBorder(ToggleTitleBorder ttb, Insets insets) {
            super(insets);
            this.ttb = ttb;
        }
    }

    private static final RenderingHints aliasingRenderHints;

    static {
        aliasingRenderHints = new RenderingHints(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        aliasingRenderHints.put(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        );
    }

    static class LZTitleBorder extends TitledBorder {
        ToggleTitleBorder ttb;
        LZTitleBorder(ToggleTitleBorder ttb, String title) {
            super(BorderFactory.createEmptyBorder(), title);
            // super(BorderFactory.createLineBorder(Color.gray), title);
            this.ttb = ttb;
            titleColor = LightZoneSkin.Colors.LabelForeground;
        }

        // On Windows text aliasing is off for some reason...
        public void paintBorder(Component jComponent, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHints(aliasingRenderHints);
            super.paintBorder(jComponent, graphics, x, y, width, height);
        }
    }

    // The component that receives the border
    private JComponent comp;

    // The Border that actually displays the title
    private LZTitleBorder title;

    // A Border to replace the title Border when it is hidden
    private PlaceholderBorder placeholder;

    // The original Border of the component
    private Border original;

    private ToggleTitleBorder(JComponent comp, String text) {
        this.comp = comp;
        title = new LZTitleBorder(this, text);
        title.setTitleFont(font);
        title.setTitleJustification(TitledBorder.CENTER);
        title.setTitlePosition(TitledBorder.BOTTOM);
        Insets insets = getBorderInsets(comp);
        insets.top = 0;
        insets.bottom = 0;
        placeholder = new PlaceholderBorder(this, insets);
        original = comp.getBorder();
        insideBorder = original;
        outsideBorder = isShowBorders() ? title : placeholder;
    }

    public void preferenceChange(PreferenceChangeEvent evt) {
        boolean show = isShowBorders();
        if (show) {
            outsideBorder = title;
        }
        else {
            outsideBorder = placeholder;
        }
        comp.revalidate();
        comp.repaint();
    }
}
