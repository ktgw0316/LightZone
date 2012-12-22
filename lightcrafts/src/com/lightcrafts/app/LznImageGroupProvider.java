/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.browser.model.ImageDatum;
import com.lightcrafts.ui.browser.model.ImageGroup;
import com.lightcrafts.ui.browser.model.ImageGroupProvider;
import com.lightcrafts.ui.editor.assoc.DocumentDatabase;
import com.lightcrafts.ui.editor.DocumentReader;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An ImageGroupProvider for an AbstractImageBrowser that clusters ImageDatums
 * based on LZN-image associations defined in the DocumentDatabase.
 */
class LznImageGroupProvider implements ImageGroupProvider {

    public void cluster(List<ImageDatum> datums) {

        // Reset any preexisting group assignments:
        for (ImageDatum datum : datums) {
            datum.newGroup();
        }
        // The DocumentDatabase uses Files, so prepare a fast map from Files
        // back to ImageDatums:
        Map<File, ImageDatum> fileDatumMap = getFileDatumMap(datums);

        // Find image ImageDatums for original images, and then look up their
        // LZNs in the database:
        for (ImageDatum datum : datums) {

            File file = datum.getFile();

            // "Readable" files (containing LZN data) can't be group leaders:
            if (! DocumentReader.isReadable(file)) {

                // Use the DocumentDatabase to find the versions of this image:
                List<File> docs = DocumentDatabase.getDocumentsForImage(file);

                ImageGroup group = datum.newGroup();
                for (File doc : docs) {
                    ImageDatum docDatum = fileDatumMap.get(doc);
                    if (docDatum != null) {
                        // The DocumentDatabase can get stale, and we can't
                        // have inconsistencies in the group structure:
                        if (DocumentReader.isReadable(doc)) {
                            docDatum.setGroup(group);
                        }
                    }
                }
            }
        }
    }

    static Map<File, ImageDatum> getFileDatumMap(
        Collection<ImageDatum> datums
    ) {
        Map<File, ImageDatum> map = new HashMap<File, ImageDatum>();
        for (ImageDatum datum : datums) {
            File file = datum.getFile();
            map.put(file, datum);
        }
        return map;
    }
}
