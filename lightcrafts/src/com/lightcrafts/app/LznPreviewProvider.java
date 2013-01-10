/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.types.LZNImageType;
import com.lightcrafts.model.Engine;
import com.lightcrafts.ui.browser.model.PreviewUpdater;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.jai.JAIContext;

import static com.lightcrafts.app.Locale.LOCALE;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;

class LznPreviewProvider implements PreviewUpdater.Provider {

    // Where the "Previewing..." message goes
    private ComboFrame frame;

    LznPreviewProvider(ComboFrame frame) {
        this.frame = frame;
    }

    public RenderedImage getPreviewImage(File file, int size) {
        ImageInfo info = ImageInfo.getInstanceFor(file);
        try {
            if (info.getImageType() != LZNImageType.INSTANCE) {
                return null;
            }
            Document doc = Application.createDocumentHeadless(file);
            Engine engine = doc.getEngine();
            Dimension dim = new Dimension(size, size);
            RenderedImage preview = engine.getRendering(dim);
            // divorce the preview from the document
            preview = new CachedImage(
                (PlanarImage) preview, JAIContext.fileCache
            );
            doc.dispose();
            return preview;
        }
        catch (Throwable t) {
            // UnknownImageTypeException
            // IOException
            // BadImageFileException
            // ColorProfileException
            // Document.MissingImageFileException
            // (etc.)
            System.err.println(
                "Error generating preview for " + file.getName() + ":"
            );
            t.printStackTrace();
            return null;
        }
    }

    public void previewStarted() {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    frame.showWait(LOCALE.get("PreviewMessage"));
                }
            }
        );
    }

    public void previewEnded() {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    frame.hideWait();
                }
            }
        );
    }
}
