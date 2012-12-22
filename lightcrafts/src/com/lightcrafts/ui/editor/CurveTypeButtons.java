/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.region.CurveFactory;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.CoolToggleButton;
import com.lightcrafts.ui.toolkit.IconFactory;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.color.ColorScheme;

/**
 * A group of three toggle buttons that control the Curve implementation
 * setting in the RegionManager.
 */

public class CurveTypeButtons extends Box {

    public static final Icon PolygonIcon =
        IconFactory.createInvertedIcon(CurveTypeButtons.class, "polygon.png");

    public static final Icon BasisIcon =
        IconFactory.createInvertedIcon(CurveTypeButtons.class, "basis.png");
                
    public static final Icon BezierIcon =
        IconFactory.createInvertedIcon(CurveTypeButtons.class, "bezier.png");

    public static final Icon RegionGenericIcon =
        IconFactory.createInvertedIcon(CurveTypeButtons.class, "regiongeneric.png");

    private static String PolygonToolTip = LOCALE.get("PolygonToolTip");
    private static String BezierToolTip = LOCALE.get("BezierToolTip");
    private static String BasisToolTip = LOCALE.get("BasisToolTip");

    private RegionManager regions;

    private CoolToggleButton polygon;
    private CoolToggleButton bezier;
    private CoolToggleButton basis;

    private boolean isUpdating; // prevent update loops with RegionManager

    CurveTypeButtons(RegionManager regions) {
        super(BoxLayout.X_AXIS);

        this.regions = regions;

        initButtons();
        updateFromFactory();

        ButtonGroup group = new ButtonGroup();

        polygon.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if (event.getStateChange() == ItemEvent.SELECTED) {
                        RegionManager regions =
                            CurveTypeButtons.this.regions;
                        isUpdating = true;
                        regions.setCurveType(CurveFactory.Polygon);
                        isUpdating = false;
                    }
                }
            }
        );
        group.add(polygon);
        add(polygon);

        basis.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if (event.getStateChange() == ItemEvent.SELECTED) {
                        RegionManager regions =
                            CurveTypeButtons.this.regions;
                        isUpdating = true;
                        regions.setCurveType(CurveFactory.CubicBasis);
                        isUpdating = false;
                    }
                }
            }
        );
        group.add(basis);
        add(basis);

        bezier.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if (event.getStateChange() == ItemEvent.SELECTED) {
                        RegionManager regions =
                            CurveTypeButtons.this.regions;
                        isUpdating = true;
                        regions.setCurveType(CurveFactory.CubicBezier);
                        isUpdating = false;
                    }
                }
            }
        );
        group.add(bezier);
        add(bezier);
    }

    // Create disabled buttons for the no-Document display mode:

    CurveTypeButtons() {
        super(BoxLayout.X_AXIS);
        initButtons();
        polygon.setEnabled(false);
        add(polygon);
        basis.setEnabled(false);
        add(basis);
        bezier.setEnabled(false);
        add(bezier);
    }

    void updateFromFactory() {
        if (! isUpdating) {
            switch (regions.getCurveType()) {
                case CurveFactory.Polygon:
                    polygon.setSelected(true);
                    break;
                case CurveFactory.CubicBezier:
                    bezier.setSelected(true);
                    break;
                case CurveFactory.CubicBasis:
                    basis.setSelected(true);
                    break;
            }
        }
    }

    ColorScheme orangeScheme = new LightZoneSkin.CustomColorScheme(LightZoneSkin.Colors.LZOrange);

    private void initButtons() {
        polygon = new CoolToggleButton(CoolButton.ButtonStyle.LEFT);
        polygon.setIcon(PolygonIcon);
        basis = new CoolToggleButton(CoolButton.ButtonStyle.CENTER);
        basis.setIcon(BasisIcon);
        bezier = new CoolToggleButton(CoolButton.ButtonStyle.RIGHT);
        bezier.setIcon(BezierIcon);

        // bezier.putClientProperty(SubstanceLookAndFeel.FLAT_PROPERTY, Boolean.TRUE);
        polygon.putClientProperty(SubstanceLookAndFeel.THEME_PROPERTY, LightZoneSkin.orangeTheme);
        basis.putClientProperty(SubstanceLookAndFeel.THEME_PROPERTY, LightZoneSkin.orangeTheme);
        bezier.putClientProperty(SubstanceLookAndFeel.THEME_PROPERTY, LightZoneSkin.orangeTheme);

        polygon.setToolTipText(PolygonToolTip);
        basis.setToolTipText(BasisToolTip);
        bezier.setToolTipText(BezierToolTip);

    }
}
