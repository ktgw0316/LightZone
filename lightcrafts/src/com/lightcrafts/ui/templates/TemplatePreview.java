/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.model.Engine;
import com.lightcrafts.model.EngineFactory;
import com.lightcrafts.model.Preview;
import com.lightcrafts.ui.LightZoneSkin;
import com.lightcrafts.ui.editor.EditorControls;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.LinkedList;
import java.util.List;

/**
 * A preview of an Engine in an Editor that has two modes: one where it holds
 * a Preview component from its given Engine; and another where it holds a
 * different Preview component from another Engine that has been derived from
 * the given one with a style subsequently applied to it.
 */
class TemplatePreview extends JPanel {

    private final static Dimension PreferredSize = new Dimension(240, 180);

    private Engine engine;

    private EditorControls editControls;
    private Engine editEngine;

    // Avoid thrashing template controls at mouse motion
    private XmlNode recentTemplate;

    private List<OpControl> tools;

    // Constructor for a disabled TemplateControl.
    TemplatePreview() {
        setLayout(null);
        setPreferredSize(PreferredSize);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, PreferredSize.height));
        setMinimumSize(new Dimension(1, PreferredSize.height));
        tools = new LinkedList<OpControl>();
        setBackground(LightZoneSkin.Colors.NeutralGray);
    }

    TemplatePreview(Engine engine) {
        this();
        this.engine = engine;
        Preview preview = engine.getPreviews().iterator().next();
        add(preview);
    }

    void showNormalPreview() {
        if (editControls == null) {
            return;
        }
        disposeEditControls();
        Preview preview = engine.getPreviews().iterator().next();
        removeAll();
        add(preview);
        validate();
        repaint();
    }

    // Take a snapshot of the given Preview, wrap it in an Engine, apply the
    // given template to this new Engine, and show its component.
    void showTemplatePreview(XmlNode node) {
        if (engine == null) {
            return;
        }
        if (editControls == null) {
            createEditControls();
            Component comp = editEngine.getComponent();
            removeAll();
            add(comp);
            validate();
            repaint();
        }
        if (node != recentTemplate) {
            editControls.removeControls(tools);
            try {
                tools = editControls.addControls(node);
            }
            catch (Throwable t) {
                // Just leave the old tools removed.
                System.err.println(
                    "Could not preview a template"
                );
                t.printStackTrace();
            }
            recentTemplate = node;
        }
    }

    @Override
    public void doLayout() {
        Dimension size = getSize();
        Component[] comps = getComponents();
        if (comps.length < 1) {
            return;
        }
        Component comp = comps[0];
        if (editControls != null) {
            // The Engine Component needs centering.
            editEngine.setScale(new Rectangle(0, 0, size.width, size.height));
            Dimension naturalSize = editEngine.getNaturalSize();
            double imageRatio = naturalSize.width / (double) naturalSize.height;
            double containerRatio = size.width / (double) size.height;
            int x, y;
            if (imageRatio > containerRatio) {
                x = 0;
                y = (size.height - naturalSize.height) / 2;
            }
            else {
                x = (size.width - naturalSize.width) / 2;
                y = 0;
            }
            comp.setSize(naturalSize);
            comp.setLocation(x, y);
        }
        else {
            // The Engine Preview centers itself.
            comp.setLocation(0, 0);
            comp.setSize(size);
        }
    }

    private void createEditControls() {
        Dimension size = getSize();
        RenderedImage image = engine.getRendering(size);
        editEngine = EngineFactory.createEngine(image);
        editControls = new EditorControls(editEngine);
    }

    private void disposeEditControls() {
        editEngine.dispose();
        editControls = null;
        tools.clear();
        recentTemplate = null;
    }
}
