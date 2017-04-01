/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.whitepoint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;

import com.lightcrafts.model.WhitePointOperation;
import com.lightcrafts.ui.help.HelpConstants;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.ui.toolkit.DropperButton;
import com.lightcrafts.ui.mode.DropperMode;
import com.lightcrafts.ui.swing.ColorSwatch;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import static com.lightcrafts.ui.operation.whitepoint.Locale.LOCALE;
import com.lightcrafts.ui.editor.EditorMode;

/**
 * An OpControl to hold a WhitePointOperation in an OpStack.
 */

public class WhitePointControl extends OpControl {

    private DropperButton dropperButton;
    private ColorSwatch swatch;
    private ColorText text;
    private DropperMode dropperMode;

    // Flag dropper button state changes that just synchronize the button
    // when the dropper Mode is externally cancelled, so OpControlModeListener
    // notifications won't fire:
    private boolean isDropperModeCancelling;

    public WhitePointControl(WhitePointOperation op, OpStack stack) {
        super(op, stack);

        // Make the button that enters and exits the dropper Mode:
        dropperButton = new DropperButton();
        dropperButton.setAlignmentX(1f);
        dropperButton.addItemListener(
            new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    boolean selected =
                        (event.getStateChange() == ItemEvent.SELECTED);
                    if (selected) {
                        getComboFrame().getEditor().setMode( EditorMode.ARROW );
                        notifyListenersEnterMode(dropperMode);
                    }
                    else {
                        if (! isDropperModeCancelling) {
                            notifyListenersExitMode(dropperMode);
                        }
                    }
                }
            }
        );
        // Initialize the dropper Mode, and let it exit itself:
        dropperMode = new DropperMode( this );
        dropperMode.addListener(
            new DropperMode.Listener() {
                public void pointSelected(Point2D p) {
                    setWhitePoint(p);
//                    // Trigger Mode-exit via the toggle button:
//                    dropperButton.setSelected(false);
                }
                public void modeCancelled() {
                    // Reset the toggle button, without firing notifications:
                    isDropperModeCancelling = true;
                    dropperButton.setSelected(false);
                    isDropperModeCancelling = false;
                }
            }
        );
        Color color = op.getWhitePoint();
        swatch = new ColorSwatch(color);
        text = new ColorText(color);

        Box content = Box.createHorizontalBox();
        content.add(swatch);
        content.add(Box.createHorizontalStrut(8));
        content.add(text);
        content.add(Box.createHorizontalStrut(32));
        content.add(dropperButton);

        content.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        setContent(content);

        readyForUndo();
    }

    private void setWhitePoint(Color color) {
        ((WhitePointOperation) getOperation()).setWhitePoint(color);
        updateWhitePoint();
        if (! undoSupport.isRestoring()) {
            undoSupport.postEdit(LOCALE.get("SetWhitePointEditName"));
        }
    }

    private void setWhitePoint(Point2D p) {
        ((WhitePointOperation) getOperation()).setWhitePoint(p);
        updateWhitePoint();
        if (! undoSupport.isRestoring()) {
            undoSupport.postEdit(LOCALE.get("DropperEditName"));
        }
    }

    private Color getWhitePoint() {
        return ((WhitePointOperation) getOperation()).getWhitePoint();
    }

    private void updateWhitePoint() {
        Color color = getWhitePoint();
        swatch.setColor(color);
        text.setColor(color);
    }

    private final static String ColorTag = "Color";

    public void save(XmlNode node) {
        super.save(node);
        Color color = getWhitePoint();
        XmlNode whiteNode = node.addChild(ColorTag);
        whiteNode.setAttribute("r", Integer.toString(color.getRed()));
        whiteNode.setAttribute("g", Integer.toString(color.getGreen()));
        whiteNode.setAttribute("b", Integer.toString(color.getBlue()));
    }

    public void restore(XmlNode node) throws XMLException {
        super.restore(node);
        XmlNode whiteNode = node.getChild(ColorTag);
        int r = Integer.parseInt(whiteNode.getAttribute("r"));
        int g = Integer.parseInt(whiteNode.getAttribute("g"));
        int b = Integer.parseInt(whiteNode.getAttribute("b"));
        Color color = new Color(r, g, b);
        undoSupport.restoreStart();
        setWhitePoint(color);
        if (dropperButton.isSelected()) {
            dropperButton.setSelected(false);
        }
        undoSupport.restoreEnd();
    }

    protected String getHelpTopic() {
        return HelpConstants.HELP_TOOL_WHITE_BALANCE;
    }
}
