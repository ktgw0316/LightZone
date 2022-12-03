/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation;

import com.lightcrafts.model.LayerMode;
import com.lightcrafts.model.Operation;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import static com.lightcrafts.ui.LightZoneSkin.Colors.LZOrange;
import static com.lightcrafts.ui.operation.Locale.LOCALE;

class OpFooter extends Box implements PropertyChangeListener {
    private LayerControls layerControls;
    private InvertRegionCheckBox invertRegionSwitch;
    private ColorSelectionControls colorControls;
    private JTabbedPane tabPane;

    public void propertyChange(PropertyChangeEvent evt) {
        if (tabPane != null && evt.getPropertyName().equals(ColorSelectionControls.COLOR_SELECTION)) {
            if (evt.getNewValue() == Boolean.TRUE)
                tabPane.setIconAt(1, getThemeIcon(null, false));
            else
                tabPane.setIconAt(1, getThemeIcon(LZOrange, false));
        }
        if (layerControls != null && evt.getPropertyName().equals(LayerControls.BLENDING_MODES)) {
            if (evt.getNewValue() == Boolean.TRUE)
                tabPane.setIconAt(0, getThemeIcon(null, true));
            else
                tabPane.setIconAt(0, getThemeIcon(LZOrange, true));
        }
    }

    private static Icon getThemeIcon(Color color, boolean square) {
//        int iSize = LightZoneSkin.LightZoneFontSet.TitleFont.getSize() / 2;
        BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) result.getGraphics().create();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                  RenderingHints.VALUE_ANTIALIAS_ON);

        Color color1 = (color == null)
                       ? Color.red
                       : color.darker().darker();
        Color color2 = (color == null)
                       ? Color.green
                       : color.darker();
        Color color3 = (color == null)
                       ? Color.blue
                       : color;

        graphics.setColor(color1);
        if (square)
            graphics.fillRect(5, 2, 6, 6);
        else
            graphics.fillOval(5, 2, 6, 6);
        graphics.setColor(color1.darker());
        if (square)
            graphics.drawRect(5, 2, 6, 6);
        else
            graphics.drawOval(5, 2, 6, 6);

        graphics.setColor(color2);
        if (square)
            graphics.fillRect(1, 9, 6, 6);
        else
            graphics.fillOval(1, 9, 6, 6);
        graphics.setColor(color2.darker());
        if (square)
            graphics.drawRect(1, 9, 6, 6);
        else
            graphics.drawOval(1, 9, 6, 6);

        graphics.setColor(color3);
        if (square)
            graphics.fillRect(9, 9, 6, 6);
        else
            graphics.fillOval(9, 9, 6, 6);
        graphics.setColor(color3.darker());
        if (square)
            graphics.drawRect(9, 9, 6, 6);
        else
            graphics.drawOval(9, 9, 6, 6);

        graphics.dispose();
        return new ImageIcon(result);
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport( this );

    OpFooter(OpControl control, List<LayerMode> layerModes) {
        super(BoxLayout.X_AXIS);

        layerControls = new LayerControls(control, layerModes, pcs);
        invertRegionSwitch = new InvertRegionCheckBox(control, pcs);
        colorControls = new ColorSelectionControls(control, pcs);

        Box blendBox = Box.createVerticalBox();
        blendBox.add(Box.createVerticalStrut(5));
        blendBox.add(layerControls);
        blendBox.add(invertRegionSwitch);
        blendBox.setBackground(LightZoneSkin.Colors.ToolPanesBackground);
        layerControls.setAlignmentX( Component.LEFT_ALIGNMENT );
        invertRegionSwitch.setAlignmentX( Component.LEFT_ALIGNMENT );

        tabPane = new JTabbedPane();
        tabPane.setFont(LightZoneSkin.LightZoneFontSet.SmallFont);
        tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabPane.add(LOCALE.get( "ToolSettingsTabName" ), blendBox);
        tabPane.add(LOCALE.get( "ColorSelectionTabName"), colorControls);

        tabPane.setIconAt(0, getThemeIcon(LZOrange, true));
        tabPane.setIconAt(1, getThemeIcon(LZOrange, false));

        add(tabPane, BorderLayout.NORTH);

        setBackground(LightZoneSkin.Colors.ToolPanesBackground);

        pcs.addPropertyChangeListener( this );
    }

    boolean isRegionsInverted() {
        return invertRegionSwitch.isRegionsInverted();
    }

    void operationChanged(Operation op) {
        layerControls.operationChanged(op);
        colorControls.operationChanged(op);
    }

    private final static String TabIndexTag = "layerControlsIndex";

    void save(XmlNode node) {
        layerControls.save(node);
        invertRegionSwitch.save(node);
        colorControls.save(node);
        node.setAttribute(TabIndexTag, Integer.toString(tabPane.getSelectedIndex()));
    }

    void restore(XmlNode node) throws XMLException {
        layerControls.restore(node);
        invertRegionSwitch.restore(node);
        colorControls.restore(node);
        if (node.hasAttribute(TabIndexTag)) {
            tabPane.setSelectedIndex(Integer.parseInt(node.getAttribute(TabIndexTag)));
        }
    }
}
