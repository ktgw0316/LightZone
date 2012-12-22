/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.test;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.*;

class ComponentTreeNode implements TreeNode {

    private Component comp;

    private ComponentTreeNode parent;

    private ArrayList<ComponentTreeNode> children;

    ComponentTreeNode(Component comp, ComponentTreeNode parent) {
        this.comp = comp;
        this.parent = parent;
        children = new ArrayList<ComponentTreeNode>();
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                ComponentTreeNode node = new ComponentTreeNode(child, this);
                children.add(node);
            }
        }
    }

    Component getComponent() {
        return comp;
    }

    public String toString() {
        return comp.getClass().getName();
    }

    public TreeNode getChildAt(int childIndex) {
        return children.get(childIndex);
    }

    public int getChildCount() {
        return children.size();
    }

    public TreeNode getParent() {
        return parent;
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
        final Iterator i = new ArrayList(children).iterator();
        return new Enumeration() {
            public boolean hasMoreElements() {
                return i.hasNext();
            }
            public Object nextElement() {
                return i.next();
            }
        };
    }
}
