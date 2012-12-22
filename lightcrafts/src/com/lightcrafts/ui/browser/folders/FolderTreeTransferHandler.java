/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

class FolderTreeTransferHandler extends TransferHandler {

    private FolderTree tree;

    FolderTreeTransferHandler(FolderTree tree) {
        this.tree = tree;
    }

    public boolean canImport(JComponent comp, DataFlavor[] flavs) {
        for (DataFlavor flav : flavs) {
            if (flav.equals(DataFlavor.javaFileListFlavor)) {
                return true;
            }
        }
        return false;
    }

    public boolean importData(JComponent comp, Transferable trans) {
        try {
            //noinspection unchecked
            List<File> files = (List<File>) trans.getTransferData(
                DataFlavor.javaFileListFlavor
            );
            for (Object o : files) {
                File file = (File) o;
                System.out.println(file.getAbsolutePath());
            }
            FolderTreeSelectionModel selection =
                (FolderTreeSelectionModel) tree.getSelectionModel();
            File folder = selection.getDropFolder();
            System.out.println(
                folder != null ? folder.getAbsolutePath() : null
            );
            tree.notifyDropAccepted(files, folder);
            return true;
        }
        catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
