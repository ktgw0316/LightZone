/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FolderTreeTransferHandler extends TransferHandler {

    private static final Logger logger = LoggerFactory.getLogger(FolderTreeTransferHandler.class);

    private FolderTree tree;

    FolderTreeTransferHandler(FolderTree tree) {
        this.tree = tree;
    }

    public boolean canImport(JComponent comp, DataFlavor[] flavs) {
        return Arrays.stream(flavs)
                .anyMatch((it -> it.equals(DataFlavor.javaFileListFlavor)));
    }

    public boolean importData(JComponent comp, Transferable trans) {
        try {
            //noinspection unchecked
            List<File> files = (List<File>) trans.getTransferData(
                DataFlavor.javaFileListFlavor
            );
            files.stream()
                    .map(File::getAbsolutePath)
                    .forEach(path -> logger.debug("{}", path));
            FolderTreeSelectionModel selection =
                (FolderTreeSelectionModel) tree.getSelectionModel();
            File folder = selection.getDropFolder();
            logger.debug("{}", folder != null ? folder.getAbsolutePath() : null);
            tree.notifyDropAccepted(files, folder);
            return true;
        }
        catch (UnsupportedFlavorException | IOException e) {
            logger.warn("Failed to import dropped folder data", e);
        }
        return false;
    }
}
