/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import java.util.ArrayList;
import java.util.Base64;

import lombok.val;

import org.w3c.dom.*;

import static com.lightcrafts.utils.xml.XMLUtil.asList;
import static com.lightcrafts.utils.xml.XMLUtil.asReversedArray;

/**
 * A limited wrapper around Element, to prevent users from exploring XML
 * features.  The wrapper has just a name, attributes, and children, plus
 * a "version" feature.
 */

public class XmlNode {

    // XmlNodes can have a version number.  See setVersion() and getVersion().
    private final static String VersionTag = "version";

    private Element element;
    private ArrayList<XmlNode> children;

    XmlNode(Element element) {
        this.element = element;
        children = new ArrayList<XmlNode>();
        val nodes = asList(element.getChildNodes());
        for (val node : nodes) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                val child = new XmlNode((Element) node);
                children.add(child);
            }
        }
    }

    /**
     * Hunt up the tree for a version attribute, parse it as an integer,
     * and return it.  If no such attribute is present, return -1.
     */
    public int getVersion() {
        Node e = element;
        while (e instanceof Element) {
            String s = ((Element) e).getAttribute(VersionTag);
            if (s != null) {
                try {
                    return Integer.parseInt(s);
                }
                catch (NumberFormatException ex) {
                    // Keep looking.
                }
            }
            e = e.getParentNode();
        }
        return -1;
    }

    /**
     * Set the version attribute on this node, so that it will be returned
     * by getVersion() on this node and all its descendants, until one of the
     * descendants has its version attribute set.
     */
    public void setVersion(int version) {
        setAttribute(VersionTag, Integer.toString(version));
    }

    public String getName() {
        return element.getTagName();
    }

    /** Test whether this XmlNode has an attribute with the given name.
     */
    public boolean hasAttribute(String key) {
        return element.hasAttribute(key);
    }

    /** Get the value of an attribute under this XmlNode.  If no attribute
      * with the given name is found, it's an XMLException.
      */
    public String getAttribute(String key) throws XMLException {
        if (! hasAttribute(key)) {
            String message =
                "Expected attribute \"" + key +
                "\" under \"" + getName() + "\"";
            throw new XMLException(message);
        }
        return element.getAttribute(key);
    }

    /** Set an attribute key-value pair on this XmlNode.
     */
    public void setAttribute(String key, String value) {
        element.setAttribute(key, value);
    }

    /** Get all attribute keys on this XmlNode.
     */
    public String[] getAttributes() {
        NamedNodeMap map = element.getAttributes();
        int length = map.getLength();
        String[] keys = new String[length];
        for (int n=0; n<length; n++) {
            Node item = map.item(n);
            keys[n] = item.getNodeName();
        }
        return keys;
    }

    /** Create a new XmlNode as a child of this one with the given name.
     */
    public XmlNode addChild(String name) {
        Document doc = element.getOwnerDocument();
        Element e = doc.createElement(name);
        element.appendChild(e);
        XmlNode child = new XmlNode(e);
        children.add(child);
        return child;
    }

    /** Test whether this XmlNode has a child element with the given name.
     */
    public boolean hasChild(String name) {
        for (XmlNode child : children) {
            if (child.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** Get the first child of this XmlNode that has the given name.
     * If no child with this name is found, it's an XMLException.
     */
    public XmlNode getChild(String name) throws XMLException {
        for (XmlNode child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        String message =
            "Expected child \"" + name + "\" under \"" + getName() + "\"";
        throw new XMLException(message);
    }

    /** Get all children of this XmlNode.
     */
    public XmlNode[] getChildren() {
        return children.toArray(new XmlNode[0]);
    }

    /*** Get all children of this XmlNode that have the given name.
     * If none have this name, it's a zero-length array and not an
     * XMLException.
     */
    public XmlNode[] getChildren(String name) {
        final ArrayList<XmlNode> nameChildren = new ArrayList<XmlNode>();
        for (final XmlNode child : children) {
            if (child.getName().equals(name))
                nameChildren.add(child);
        }
        return nameChildren.toArray(new XmlNode[0]);
    }

    public void removeChild(XmlNode child) {
        if (children.contains(child)) {
            children.remove(child);
            element.removeChild(child.element);
        }
    }

    /**
     * Stuff binary data into this XmlNode as a child text node using Base64
     * encoding.  To recover these data, use <code>getData()</code>.
     */
    public void setData(byte[] data) {
        String s = Base64.getEncoder().encodeToString(data);
        clearData();
        Document doc = element.getOwnerDocument();
        Text text = doc.createTextNode(s);
        element.appendChild(text);
    }

    /**
     * Get binary data from this XmlNode that has been previously saved by
     * <code>setData()</code>.  This works by normalizing text node children
     * and then decoding the result as Base64.  If a child text node exists
     * but does not contain valid Base64-encoded data, then this method
     * returns null.
     */
    public byte[] getData() {
        val sb = new StringBuilder();
        val nodes = asList(element.getChildNodes());
        for (val node : nodes) {
            if ( node.getNodeType() == Node.TEXT_NODE ) {
                val text = (Text)node;
                sb.append( text.getData() );
            }
        }
        return Base64.getDecoder().decode(sb.toString());
    }

    /**
     * Remove all text node children of this XmlNode.  This undoes all effects
     * of <code>setData()</code>, so that <code>getData()</code> will return
     * null.
     */
    public void clearData() {
        val nodes = asReversedArray(element.getChildNodes());
        for (val node : nodes) {
            if (node.getNodeType() == Node.TEXT_NODE) {
                element.removeChild(node);
            }
        }
    }
}
/* vim:set et sw=4 ts=4: */
