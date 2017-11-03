/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A specification of how metadata should be presented to the user.
 * <p>
 * Metadata is organized into sections for display.  Each section holds an
 * ordered collection of MetadataEntries.  Each field is defined by a paired
 * ImageMetadataDirectory instance and integer tag ID.
 * of
 */
class MetadataPresentation {

    private ArrayList<MetadataSection> sections;

    MetadataPresentation() {
        sections = new ArrayList<MetadataSection>();

        MetadataSection coreSection = new MetadataSection();
        coreSection.addEntry(
            CoreDirectory.class, CoreTags.CORE_FILE_NAME
        );
        coreSection.addEntry(
            CoreDirectory.class, CoreTags.CORE_DIR_NAME
        );
        coreSection.addEntry(new FileTimeMetadataEntry());

        coreSection.addEntry(
            CoreDirectory.class, CoreTags.CORE_FILE_SIZE
        );
        sections.add(coreSection);

        MetadataSection editableSection = new MetadataSection();
        editableSection.addEntry(
            new RatingMetadataEntry()
        );
        editableSection.addEntry(
            new TitleMetadataEntry()
        );
        editableSection.addEntry(
            new CaptionMetadataEntry()
        );
        editableSection.addEntry(
            new CopyrightMetadataEntry()
        );
        editableSection.addEntry(
            new CreatorMetadataEntry()
        );
        editableSection.addEntry(
            new IPTCMetadataEntry(IPTCTags.IPTC_LOCATION)
        );
        sections.add(editableSection);

        MetadataSection miscSection = new MetadataSection();
        miscSection.addEntry(
            new EditSizeMetadataEntry()
        );
        miscSection.addEntry(
            new VersionSizeMetadataEntry()
        );
        miscSection.addEntry(
            new ExposureMetadataEntry()
        );
        miscSection.addEntry(
            new SimpleMetadataEntry(
                CoreDirectory.class, CoreTags.CORE_FOCAL_LENGTH
            )
        );
        miscSection.addEntry(
            new SimpleMetadataEntry(
                CoreDirectory.class, CoreTags.CORE_ISO
            )
        );
        miscSection.addEntry(
            new SimpleMetadataEntry(
                CoreDirectory.class, CoreTags.CORE_FLASH
            )
        );
        miscSection.addEntry(new CaptureTimeMetadataEntry());

        miscSection.addEntry(
            new SimpleMetadataEntry(
                CoreDirectory.class, CoreTags.CORE_CAMERA
            )
        );
        miscSection.addEntry(
            new SimpleMetadataEntry(
                CoreDirectory.class, CoreTags.CORE_LENS
            )
        );
        miscSection.addEntry(
                new GPSMetadataEntry()
        );
        sections.add(miscSection);
    }

    List<MetadataSection> getSections() {
        return sections;
    }
}
