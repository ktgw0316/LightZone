/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;

import java.util.ArrayList;

/**
 * A jig for inspecting and Template XML in TemplateToolSelector.
 */
class TemplateJig {

    private XmlDocument xml;

    TemplateJig(XmlDocument xml) throws XMLException {
        this.xml = xml;
        validate();
    }

    private void validate() throws XMLException {
        // Explore the structure of the XmlDocument enough to make sure the
        // other methods of this class will not encounter XMLExceptions.
        XmlNode controls = getControlNode();
        if (controls == null) {
            throw new XMLException("No Control node");
        }
        XmlNode[] tools = getToolNodes();
        XmlNode[] regions = getRegionNodes();
        if (tools.length != regions.length) {
            throw new XMLException("Tool tags do not match region tags");
        }
    }

    String[] getToolNames() {
        XmlNode[] nodes = getToolNodes();
        ArrayList<String> names = new ArrayList<String>();
        for (XmlNode node : nodes) {
            String name = getToolName(node);
            names.add(name);
        }
        return names.toArray(new String[0]);
    }

    void removeTool(int index) {
        XmlNode controls = getControlNode();
        XmlNode tool = getToolNode(index);
        XmlNode region = getRegionNode(index);
        controls.removeChild(tool);
        controls.removeChild(region);
    }

    private XmlNode getToolNode(int index) {
        return getToolNodes()[index];
    }

    private XmlNode getRegionNode(int index) {
        return getRegionNodes()[index];
    }

    private XmlNode[] getToolNodes() {
        XmlNode controls = getControlNode();
        XmlNode[] nodes = controls.getChildren();
        ArrayList<XmlNode> tools = new ArrayList<XmlNode>();
        for (XmlNode node : nodes) {
            if (isToolNode(node)) {
                tools.add(node);
            }
        }
        return tools.toArray(new XmlNode[0]);
    }

    private static boolean isToolNode(XmlNode node) {
        return node.hasAttribute("Name");
    }

    private static String getToolName(XmlNode node) {
        assert isToolNode(node);
        try {
            return node.getAttribute("Name");
        }
        catch (XMLException e) {
            // Should never happen; guarded by the assertion.
            return null;
        }
    }

    private XmlNode[] getRegionNodes() {
        XmlNode controls = getControlNode();
        XmlNode[] nodes = controls.getChildren();
        ArrayList<XmlNode> tools = new ArrayList<XmlNode>();
        for (XmlNode node : nodes) {
            if (isRegionNode(node)) {
                tools.add(node);
            }
        }
        return tools.toArray(new XmlNode[0]);
    }

    private boolean isRegionNode(XmlNode node) {
        return node.getName().equals("Region");
    }

    private XmlNode getControlNode() {
        try {
            XmlNode root = xml.getRoot();
            XmlNode node = root.getChild("Controls");
            return node;
        }
        catch (XMLException e) {
            // Can't happen if validate() returned.
            return null;
        }
    }
}
