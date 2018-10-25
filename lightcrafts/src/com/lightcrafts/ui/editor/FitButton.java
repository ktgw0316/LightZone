/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.model.Engine;
import com.lightcrafts.model.Scale;
import static com.lightcrafts.ui.editor.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.CoolToggleButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * An image button that zooms to fit.  Its selected state is used in Editor
 * to maintain zoom-to-fit so long as the button is selected.
 * <p>
 * This class must listen for changes to its ScaleModel, so it can detect
 * scale changes from other sources and deselect itself at that time.
 */

final class FitButton
    extends CoolToggleButton implements ItemListener, ScaleListener
{
    private static Icon Icon =
        IconFactory.createInvertedIcon(FitButton.class, "fit.png");

    private final static String ToolTip = LOCALE.get("ZoomFitToolTip");

    private Editor editor;
    private Engine engine;
    private ScaleModel scale;

    private boolean isChangingScale;    // prevent recursion

    FitButton(Editor editor, Engine engine, ScaleModel scale) {
        this.editor = editor;
        this.engine = engine;
        this.scale = scale;
        setStyle(CoolButton.ButtonStyle.CENTER);
        setIcon(Icon);
        setToolTipText(ToolTip);
        addItemListener(this);
        scale.addScaleListener(this);
    }

    // A disabled button, for the no-Document display mode.

    FitButton() {
        setIcon(Icon);
        setToolTipText(ToolTip);
        setEnabled(false);
    }

    public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            doZoomToFit();
        }
    }

    // This zoom-to-fit logic is copied from Document.zoomToFit().
    void doZoomToFit() {
        isChangingScale = true;
        editor.setScaleToFit();
        isChangingScale = false;
    }

    // If a scale change from another source is detected, exit the
    // zoom-to-fit mode.
    public void scaleChanged(Scale scale) {
        if (! isChangingScale) {
            setSelected(false);
        }
    }
}
