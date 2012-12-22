/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.templates;

import com.lightcrafts.templates.TemplateDatabase;
import com.lightcrafts.templates.TemplateKey;

import javax.swing.tree.TreeNode;
import java.util.*;

/**
 * An invisibile, universal root node to hold template namepsace nodes.
 */
class TemplateRootNode implements TreeNode {

    private ArrayList<TemplateNamespaceTreeNode> children;

    TemplateRootNode() throws TemplateDatabase.TemplateException {
        List<TemplateKey> keys = TemplateDatabase.getTemplateKeys();
        LinkedHashSet<String> namespaces = new LinkedHashSet<String>();
        for (TemplateKey key : keys) {
            String namespace = key.getNamespace();
            namespaces.add(namespace);
        }
        children = new ArrayList<TemplateNamespaceTreeNode>();
        for (String namespace : namespaces) {
            try {
                TemplateNamespaceTreeNode child = new TemplateNamespaceTreeNode(
                    this, namespace, keys
                );
                children.add(child);
            }
            catch (Throwable t) {
                // TemplateDatabase.TemplateException, XMLException
                t.printStackTrace();
                // Continue, just a malformed template
            }
        }
    }

    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    public int getChildCount() {
        return children.size();
    }

    public TreeNode getParent() {
        return null;
    }

    public int getIndex(TreeNode node) {
        return children.indexOf(node);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public boolean isLeaf() {
        return false;
    }

    public Enumeration children() {
        final Iterator iterator = children.iterator();
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
