/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;

/**
 * A <code>WindowsFolderTreeCellRenderer</code> is-a
 * {@link DefaultTreeCellRenderer} that is used for rendering folders
 * (directories) under Windows.
 */
class WindowsFolderTreeCellRenderer extends DefaultTreeCellRenderer {

    /**
     * {@inheritDoc}
     */

    public Color getBackgroundNonSelectionColor()  {
        return LightZoneSkin.Colors.ToolPanesBackground;
    }

    public Component getTreeCellRendererComponent( JTree tree, Object value,
                                                   boolean isSelected,
                                                   boolean isExpanded,
                                                   boolean isLeaf, int row,
                                                   boolean hasFocus ) {
        super.getTreeCellRendererComponent(
            tree, value, isSelected, isExpanded, isLeaf, row, hasFocus
        );
        if ( value instanceof FolderTreeNode ) {
            if ( !tree.isEnabled() ) {
                setEnabled( false );
                if ( isLeaf )
                    setDisabledIcon( getClosedIcon() );
                else
                    setIconFor( (FolderTreeNode)value, isSelected );
            } else {
                setEnabled( true );
                if ( isLeaf )
                    setIcon( getClosedIcon() );
                else
                    setIconFor( (FolderTreeNode)value, isSelected );
            }
        }
        return this;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Set the icon for a given {@link FolderTreeNode}.
     *
     * @param node The {@link FolderTreeNode} to set the icon for.
     * @param isSelected If <code>true</code>, the icon is set to an "open
     * folder" icon; if <code>false</code>, the icon is set to a "closed
     * folder" icon.
     */
    private void setIconFor( FolderTreeNode node, boolean isSelected ) {
        final Icon icon;
        if ( node.hasSpecialIcon() ) {
            final File file = node.getFile();
            icon = Platform.getPlatform().getFileSystemView().getSystemIcon( file );
        } else
            icon = isSelected ? getOpenIcon() : getClosedIcon();
        setIcon( icon );
    }
}
/* vim:set et sw=4 ts=4: */
