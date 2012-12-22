/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata.test;

import javax.swing.tree.TreeNode;
import javax.swing.*;
import java.io.File;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.file.FileUtil;

/** A simple TreeNode implementation based on File, to make JTrees out of
  * file system trees.
  */

class FileNode implements TreeNode {

    File file_;
    ArrayList children_;
    FileNode parent_;

    FileNode(File file) {
        file_ = file;
//        getChildren();
    }

    ArrayList getChildren() {
        if (children_ != null) {
            return children_;
        }
        children_ = new ArrayList();
        File[] files = FileUtil.listFiles( file_ );
        if (files == null) {
            return children_;
        }
        for (int n=0; n<files.length; n++) {
            FileNode child = new FileNode(files[n]);
            child.parent_ = this;
            children_.add(child);
        }
        return children_;
    }

    public File getFile() {
        return file_;
    }

    public String toString() {
        return file_.getName();
    }

    public int getChildCount() {
        return getChildren().size();
    }

    public boolean getAllowsChildren() {
        return getChildCount() > 0;
    }

    public boolean isLeaf() {
        return isLeaf( file_ );
    }

    private static boolean isLeaf( File file ) {
        file = Platform.getPlatform().isSpecialFile( file );
        return file.isFile();
    }

    public Enumeration children() {
        final Iterator i = getChildren().iterator();
        return new Enumeration() {
            public boolean hasMoreElements() {
                return i.hasNext();
            }
            public Object nextElement() {
                return i.next();
            }
        };
    }

    public TreeNode getParent() {
        return parent_;
    }

    public TreeNode getChildAt(int childIndex) {
        return (TreeNode) getChildren().get(childIndex);
    }

    public int getIndex(TreeNode node) {
        return getChildren().indexOf(node);
    }

    public static void main(String[] args) {
        String dir = System.getProperty("user.dir");
        File file = new File(dir);
        FileNode node = new FileNode(file);
        JTree tree = new JTree(node);

        JFrame frame = new JFrame("Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(tree);
        frame.addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            }
        );
        frame.pack();
        frame.setVisible(true);
    }
}
