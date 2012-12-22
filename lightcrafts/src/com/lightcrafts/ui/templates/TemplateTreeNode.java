/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.ui.operation.OpControl;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.List;

class TemplateTreeNode implements TreeNode {

    TemplateKey key;

    XmlNode node;

    List<OpControl> opControls;

    private TemplateNamespaceTreeNode parent;

    private boolean isPreviewed;

    TemplateTreeNode(
        TemplateNamespaceTreeNode parent, TemplateKey key
    ) throws TemplateDatabase.TemplateException, XMLException {
        this.key = key;
        this.parent = parent;
        this.key = key;
        XmlDocument xml = TemplateDatabase.getTemplateDocument(key);
        XmlNode root = xml.getRoot();
        // Tag name copied from Document.ControlTag:
        node = root.getChild("Controls");
    }

    public TreeNode getChildAt(int childIndex) {
        return null;
    }

    public String toString() {
        // Used by the JTree cell renderer.
        return key.getName();
    }

    public TemplateKey getTemplateKey() {
        return key;
    }

    public int getChildCount() {
        return 0;
    }

    public TreeNode getParent() {
        return parent;
    }

    public int getIndex(TreeNode node) {
        return 0;
    }

    public boolean getAllowsChildren() {
        return false;
    }

    public boolean isLeaf() {
        return true;
    }

    public Enumeration children() {
        return EmptyEnumeration;
    }

    void setPreviewed(boolean previewed) {
        isPreviewed = previewed;
    }

    boolean isPreviewed() {
        return isPreviewed;
    }

    private static Enumeration EmptyEnumeration = new Enumeration() {
        public boolean hasMoreElements() {
            return false;
        }
        public Object nextElement() {
            return null;
        }
    };
}
