/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import com.lightcrafts.model.ZoneOperation;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import static com.lightcrafts.ui.operation.zone.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.*;

// A radio button group to control the scale property of ZoneOperations.

class ZoneScaleButtons extends JPanel implements ItemListener {

    private final static String RgbLabel = LOCALE.get("RgbLabel");
    private final static String RgbToolTip = LOCALE.get("RgbToolTip");

    private final static String LuminosityLabel = LOCALE.get("LuminosityLabel");
    private final static String LuminosityToolTip =
        LOCALE.get("LuminosityToolTip");

    private ZoneOperation op;

    private JRadioButton rgbButton;
    private JRadioButton luminosityButton;

    private OpControl.OpControlUndoSupport undoSupport;

    ZoneScaleButtons(
        ZoneOperation op, OpControl.OpControlUndoSupport undoSupport
    ) {
        this.op = op;
        this.undoSupport = undoSupport;

        rgbButton = new JRadioButton(RgbLabel);
        rgbButton.setToolTipText(RgbToolTip);
        rgbButton.addItemListener(this);
        rgbButton.setFocusable(false);

        luminosityButton = new JRadioButton(LuminosityLabel);
        luminosityButton.setToolTipText(LuminosityToolTip);
        luminosityButton.addItemListener(this);
        luminosityButton.setFocusable(false);
        luminosityButton.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(luminosityButton);
        group.add(rgbButton);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(luminosityButton);
        add(rgbButton);
    }

    public void itemStateChanged(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            setScale();
            if (! ZoneScaleButtons.this.undoSupport.isRestoring()) {
                undoSupport.postEdit(LOCALE.get("ZoneTypeEditName"));
            }
        }
    }

    public void setBackground(Color color) {
        super.setBackground(color);
        if (rgbButton != null) {
            // After the base class constructor is done
            rgbButton.setBackground(color);
            luminosityButton.setBackground(color);
        }
    }

    public void setFont(Font font) {
        super.setFont(font);
        if (rgbButton != null) {
            // After the base class constructor is done
            rgbButton.setFont(font);
            luminosityButton.setFont(font);
        }
    }

    void operationChanged(ZoneOperation op) {
        this.op = op;
        setScale();
    }

    private void setScale() {
        if (rgbButton.isSelected()) {
            op.setScale(ZoneOperation.RgbScale);
        }
        else {
            op.setScale(ZoneOperation.LuminosityScale);
        }
    }

    private final static String ScaleTag = "scale";

    void save(XmlNode node) {
        int value;
        if (rgbButton.isSelected()) {
            value = ZoneOperation.RgbScale;
        }
        else {
            value = ZoneOperation.LuminosityScale;
        }
        node.setAttribute(ScaleTag, Integer.toString(value));
    }

    void restore(XmlNode node) throws XMLException {
        if (node.hasAttribute(ScaleTag)) {
            int value = Integer.parseInt(node.getAttribute(ScaleTag));
            if (value == ZoneOperation.RgbScale) {
                rgbButton.setSelected(true);
            }
            else  {
                luminosityButton.setSelected(true);
            }
        }
    }
}
