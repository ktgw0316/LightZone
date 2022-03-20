/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.model.RedEyeOperation;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.operation.clone.CloneControl;
import com.lightcrafts.ui.operation.clone.SpotControl;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeSupport;

import static com.lightcrafts.ui.operation.Locale.LOCALE;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Aug 7, 2007
 * Time: 5:12:12 PM
 * To change this template use File | Settings | File Templates.
 */
class InvertRegionCheckBox extends JCheckBox implements ItemListener {
    private OpControl control;
    private OpControl.OpControlUndoSupport undoSupport;

    private final PropertyChangeSupport pcs;

    public static final String BLENDING_MODES = "Blending Modes";

    InvertRegionCheckBox(OpControl control, PropertyChangeSupport pcs) {
        super(LOCALE.get("InvertMaskLabel"));
        this.control = control;
        undoSupport = control.undoSupport;
        setFont(LightZoneSkin.LightZoneFontSet.SmallFont);
        this.pcs = pcs;

        addItemListener(this);
        setFocusable(false);

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
        if (!undoSupport.isRestoring()) {
            undoSupport.postEdit(LOCALE.get("InvertMaskEditName"));
        }
        if (!isRegionsInverted())
            pcs.firePropertyChange(BLENDING_MODES, Boolean.TRUE, Boolean.FALSE);
        else
            pcs.firePropertyChange(BLENDING_MODES, Boolean.FALSE, Boolean.TRUE);
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
