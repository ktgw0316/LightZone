/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.model.LayerConfig;
import com.lightcrafts.model.LayerMode;
import com.lightcrafts.model.Operation;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.toolkit.LCSliderUI;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lightcrafts.ui.operation.Locale.LOCALE;

/** Manage a combo box and a slider to control LayerConfig settings on
  * an Operation.  This includes initialization from Operation defaults
  * and undo support.
  */

final class LayerControls extends Box {

    private Operation op;
    private JComboBox combo;    // Picks a LayerMode
    private JSlider slider;     // Sets an opacity number

    private List<LayerMode> layerModes;    // Allowed LayerModes

    private Map<String, LayerMode> modeMap =
            new HashMap<String, LayerMode>(); // Localized mode names

    private final PropertyChangeSupport pcs;

    public static final String BLENDING_MODES = "Blending Modes";

    // This classes uses the OpControlUndoSupport and the Operation from
    // its OpControl, and the modes List is for the LayerMode combo box.

    LayerControls(OpControl control, List<LayerMode> modes, PropertyChangeSupport pcs) {
        super(BoxLayout.X_AXIS);

        setBackground(OpControl.Background);

        op = control.getOperation();
        layerModes = modes;

        this.pcs = pcs;

        combo = new JComboBox();
        combo.setBackground(OpControl.Background);
        combo.setFont(OpControl.ControlFont);
        combo.setMaximumRowCount(30);
        // combo.setMaximumSize(combo.getPreferredSize());
        for ( LayerMode mode : layerModes ) {
            String localizedName = getLocalizedName(mode);
            modeMap.put(localizedName, mode);
            combo.addItem(localizedName);
        }
        slider = new JSlider();
        slider.setBackground(OpControl.Background);
        slider.setFont(OpControl.ControlFont);
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);
        slider.setToolTipText(LOCALE.get("OpacityToolTip"));
        slider.setUI(new LCSliderUI(slider));

        final Box blendingModeBox = Box.createHorizontalBox();
        JLabel blendLabel = new JLabel( LOCALE.get( "BlendingModeMenuLabel" ) + ": " );
        blendLabel.setFont(LightZoneSkin.LightZoneFontSet.SmallFont);
        blendingModeBox.add(blendLabel);
        blendingModeBox.add(combo);

        final Box opacityBox = Box.createHorizontalBox();
        JLabel opacityLabel = new JLabel( LOCALE.get( "ToolOpacitySliderLabel" ) + ":  " );
        opacityLabel.setFont(LightZoneSkin.LightZoneFontSet.SmallFont);
        opacityBox.add(opacityLabel);
        opacityBox.add(slider);

        final Box combinedBox = Box.createVerticalBox();
        combinedBox.add(blendingModeBox);
        combinedBox.add(opacityBox);
        blendingModeBox.setAlignmentX( Component.LEFT_ALIGNMENT );
        opacityBox.setAlignmentX( Component.LEFT_ALIGNMENT );

        add(Box.createHorizontalStrut(5));
        add(combinedBox);
        add(Box.createHorizontalStrut(5));

        // Initialize the combo and slider settings from Operation defaults:
        final LayerConfig config = op.getDefaultLayerConfig();
        setMode(config.getMode());
        setOpacity(config.getOpacity());

        // Install the combo and slider behaviors:
        final OpControl.OpControlUndoSupport undoSupport = control.undoSupport;
        combo.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    updateOperation();
                    undoSupport.postEdit(LOCALE.get("BlendEditName"));
                }
            }
        );
        slider.addChangeListener(
            new ChangeListener() {
                public void stateChanged(ChangeEvent event) {
                    updateOperation();
                }
            }
        );
        slider.addMouseListener(
            new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    op.changeBatchStarted();
                }
                public void mouseReleased(MouseEvent event) {
                    op.changeBatchEnded();
                    undoSupport.postEdit(LOCALE.get("OpacityEditName"));
                }
            }
        );
        combo.addMouseWheelListener(e -> {
            JComboBox source = (JComboBox) e.getComponent();
            if (!source.hasFocus()) {
                return;
            }
            final int rot = e.getWheelRotation();
            if (rot == 0) return;
            final int ni = source.getSelectedIndex() + rot;
            if (ni >= 0 && ni < source.getItemCount()) {
                source.setSelectedIndex(ni);
            }
        });
    }

    private final static String ModeTag = "Mode";
    private final static String OpacityTag = "Opacity";

    void save( XmlNode node ) {
        node.setAttribute( ModeTag, getMode().getName() );
        final int opacity = slider.getValue();
        node.setAttribute( OpacityTag, Integer.toString( opacity ) );
    }

    void restore(XmlNode node) throws XMLException {
        try {
            final int value = Integer.parseInt(node.getAttribute(OpacityTag));
            slider.setValue(value);
        }
        catch (NumberFormatException e) {
            throw new XMLException(
                "Value at attribute \"" + OpacityTag + "\" is not a number", e
            );
        }
        final String modeName = node.getAttribute(ModeTag);
        for ( LayerMode mode : layerModes ) {
            if ( modeName.equals( mode.getName() ) ) {
                setMode( mode );
                return;
            }
        }
        throw new XMLException(
            "Value at attribute \"" + ModeTag + "\" is not a valid layer mode"
        );
    }

    void operationChanged(Operation operation) {
        op = operation;
        final LayerConfig config = op.getDefaultLayerConfig();
        setMode(config.getMode());
        setOpacity(config.getOpacity());
    }

    // Get the opacity number from the slider:
    private double getOpacity() {
        return slider.getValue() / 100.;
    }

    private void setOpacity(double opacity) {
        slider.setValue((int) Math.round(100 * opacity));
    }

    // Get the LayerMode from the combo box:
    private LayerMode getMode() {
        String name = (String) combo.getSelectedItem();
        return modeMap.get(name);
    }

    private void setMode(LayerMode mode) {
        String localizedName = getLocalizedName(mode);
        combo.setSelectedItem(localizedName);
    }

    private String getLocalizedName(LayerMode mode) {
        String label = mode.getName().replaceAll(" ", "").concat("Label");
        return LOCALE.get(label);
    }

    private void updateOperation() {
        final LayerMode mode = getMode();
        final double opacity = getOpacity();
        final LayerConfig config = new LayerConfig(mode, opacity);
        op.setLayerConfig(config);
        if (opacity == 1.0 && mode.getName().equals("Normal"))
            pcs.firePropertyChange(BLENDING_MODES, Boolean.TRUE, Boolean.FALSE);
        else
            pcs.firePropertyChange(BLENDING_MODES, Boolean.FALSE, Boolean.TRUE);
    }
}
/* vim:set et sw=4 ts=4: */
