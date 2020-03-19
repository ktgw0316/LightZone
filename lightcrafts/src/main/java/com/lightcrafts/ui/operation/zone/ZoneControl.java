/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import com.lightcrafts.model.Operation;
import com.lightcrafts.model.ZoneOperation;
import com.lightcrafts.ui.help.HelpConstants;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.Box;
import java.awt.*;

/** A OpControl widget to manipulate a linear scale in terms of Zone
  * boundaries.  It has two ZoneContainers: one for interaction, and a
  * sterilized one to provide a static reference.
  */

public class ZoneControl extends OpControl {

    // This control is built out of drawing primitives and inherits no
    // preferred size from library components.  So we make one up:

    private final static Dimension PreferredSize = new Dimension(160, 256);

    private ZoneWidget widget;
    private ZoneScaleButtons scales;

    public ZoneControl(ZoneOperation op, OpStack stack) {
        super(op, stack);

        widget = new ZoneWidget(op, undoSupport);
        widget.setBackground(Background);

        scales = new ZoneScaleButtons(op, undoSupport);
        scales.setBackground(Background);
        scales.setFont(ControlFont);

        ZoneWidget reference = new ZoneWidget(op, null);
        reference.setBackground(Background);

        // This freezes the zone layout and removes controls:
        reference.setEnabled(false);

        readyForUndo();

        ZoneContainer zones = new ZoneContainer(widget, reference);
        zones.setBackground(Background);
        zones.setPreferredSize(PreferredSize);

        Box container = Box.createVerticalBox();
        container.add(zones);
        container.add(scales);

        setContent(container);
    }

    protected void operationChanged(Operation operation) {
        super.operationChanged(operation);
        widget.operationChanged((ZoneOperation) operation);
        scales.operationChanged((ZoneOperation) operation);
    }

    public void save(XmlNode node) {
        super.save(node);
        widget.save(node);
        scales.save(node);
    }

    public void restore(XmlNode node) throws XMLException {
        super.restore(node);
        undoSupport.restoreStart();
        widget.restore(node);
        scales.restore(node);
        undoSupport.restoreEnd();
    }

    protected String getHelpTopic() {
        return HelpConstants.HELP_TOOL_ZONEMAPPER;
    }
}
