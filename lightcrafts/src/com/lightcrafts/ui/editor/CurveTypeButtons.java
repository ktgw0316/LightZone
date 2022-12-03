/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.editor;

import com.lightcrafts.ui.region.CurveFactory;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.CoolToggleButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.event.ItemEvent;

import static com.lightcrafts.ui.editor.Locale.LOCALE;

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

    private static final String PolygonToolTip = LOCALE.get("PolygonToolTip");
    private static final String BezierToolTip = LOCALE.get("BezierToolTip");
    private static final String BasisToolTip = LOCALE.get("BasisToolTip");

    private final RegionManager regions;

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

        polygon.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                RegionManager regions1 = CurveTypeButtons.this.regions;
                isUpdating = true;
                regions1.setCurveType(CurveFactory.Polygon);
                isUpdating = false;
            }
        });
        group.add(polygon);
        add(polygon);

        basis.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                RegionManager regions12 = CurveTypeButtons.this.regions;
                isUpdating = true;
                regions12.setCurveType(CurveFactory.CubicBasis);
                isUpdating = false;
            }
        });
        group.add(basis);
        add(basis);

        bezier.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                RegionManager regions13 = CurveTypeButtons.this.regions;
                isUpdating = true;
                regions13.setCurveType(CurveFactory.CubicBezier);
                isUpdating = false;
            }
        });
        group.add(bezier);
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

    private void initButtons() {
        polygon = new CoolToggleButton(CoolButton.ButtonStyle.LEFT);
        polygon.setIcon(PolygonIcon);
        basis = new CoolToggleButton(CoolButton.ButtonStyle.CENTER);
        basis.setIcon(BasisIcon);
        bezier = new CoolToggleButton(CoolButton.ButtonStyle.RIGHT);
        bezier.setIcon(BezierIcon);

        polygon.setToolTipText(PolygonToolTip);
        basis.setToolTipText(BasisToolTip);
        bezier.setToolTipText(BezierToolTip);
    }
}
