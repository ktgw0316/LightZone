/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateKey;
import com.lightcrafts.utils.xml.XMLException;

import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

class TemplateNamespaceTreeNode implements TreeNode {

    private TreeNode root;
    private String namespace;
    private ArrayList<TemplateTreeNode> children;

    TemplateNamespaceTreeNode(
        TemplateRootNode root, String namespace, List<TemplateKey> all
    ) throws XMLException, TemplateDatabase.TemplateException {
        this.root = root;
        this.namespace = namespace;
        children = new ArrayList<TemplateTreeNode>();
        for (TemplateKey key : all) {
            String ns = key.getNamespace();
            if (ns.equals(namespace)) {
                TemplateTreeNode child = new TemplateTreeNode(this, key);
                children.add(child);
            }
        }
    }

    public String toString() {
        return namespace;
    }

    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    public int getChildCount() {
        return children.size();
    }

    public TreeNode getParent() {
        return root;
    }

    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public Enumeration children() {
        final Iterator<TemplateTreeNode> iterator = children.iterator();
        return new Enumeration() {
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }
            public Object nextElement() {
                return iterator.next();
            }
        };
    }
}
