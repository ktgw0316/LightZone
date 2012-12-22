/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.model.RedEyeOperation;
import static com.lightcrafts.ui.operation.Locale.LOCALE;
import com.lightcrafts.ui.operation.OpControl.OpControlUndoSupport;
import com.lightcrafts.ui.operation.clone.CloneControl;
import com.lightcrafts.ui.operation.clone.SpotControl;
import com.lightcrafts.ui.toolkit.ImageOnlyButton;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

class RegionSwitch extends JToggleButton implements ItemListener{

    private final static Icon NormalIcon;
    private final static Icon InvertedIcon;

    static {
        try {
            NormalIcon = new ImageIcon(
                ImageIO.read(
                    RegionSwitch.class.getResource(
                        "resources/normal_region.png"
                    )
                )
            );
            InvertedIcon = new ImageIcon(
                ImageIO.read(
                    RegionSwitch.class.getResource(
                        "resources/inverted_region.png"
                    )
                )
            );
        }
        catch (IOException e) {
            throw new RuntimeException("Couldn't initialize RegionSwitch", e);
        }
    }

    private OpControl control;
    private OpControlUndoSupport undoSupport;

    RegionSwitch(OpControl control) {
        this.control = control;
        setIcon(NormalIcon);
        setSelectedIcon(InvertedIcon);
        undoSupport = control.undoSupport;
        setToolTipText(LOCALE.get("InvertMaskToolTip"));

        ImageOnlyButton.setStyle(this);

        addItemListener(this);

        // Special cases: CloneControl and SpotControl need this switch
        // disabled, because no one would ever want to invert their regions.
        if ((control instanceof CloneControl) ||
            (control instanceof SpotControl) ||
            (control.getOperation() instanceof RedEyeOperation)) {
            setEnabled(false);
        }
    }

    public void itemStateChanged(ItemEvent event) {
        boolean selected = (event.getStateChange() == ItemEvent.SELECTED);
        control.setRegionInverted(selected);
        if (! undoSupport.isRestoring()) {
            undoSupport.postEdit(LOCALE.get("InvertMaskEditName"));
        }
    }

    boolean isRegionsInverted() {
        return isSelected();
    }

    private final static String InvertedTag = "regionsInverted";

    void save(XmlNode node) {
        node.setAttribute(InvertedTag, Boolean.toString(isRegionsInverted()));
    }

    void restore(XmlNode node) throws XMLException {
        // Backwards compatibility: this attribute added for LZN version 4.
        if (node.hasAttribute(InvertedTag)) {
            String value = node.getAttribute(InvertedTag);
            boolean isInverted = Boolean.valueOf(value);
            setSelected(isInverted);
        }
    }
}
