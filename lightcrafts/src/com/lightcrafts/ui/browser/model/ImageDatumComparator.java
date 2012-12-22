/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.metadata.CoreDirectory;
import static com.lightcrafts.image.metadata.CoreTags.*;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import static com.lightcrafts.ui.browser.model.Locale.LOCALE;

import java.util.ArrayList;
import java.util.Comparator;

public class ImageDatumComparator implements Comparator<ImageDatum> {

    // A registry of all the ImageDatumComparator instances:
    private static ArrayList<ImageDatumComparator> AllDatums =
        new ArrayList<ImageDatumComparator>();

    public static ImageDatumComparator Name =
        new ImageDatumComparator(
            CORE_FILE_NAME, LOCALE.get("FileNameSort"), false
        );

    public static ImageDatumComparator CaptureTime =
        new ImageDatumComparator(
            CORE_CAPTURE_DATE_TIME, LOCALE.get("CaptureTimeSort"), false
        );

    public static ImageDatumComparator Rating =
        new ImageDatumComparator(
            CORE_RATING, LOCALE.get("RatingSort"), false
        );

    public static ImageDatumComparator ModificationTime =
        new ImageDatumComparator(
            CORE_FILE_DATE_TIME, LOCALE.get("ModTimeSort"), false
        );

    public static ImageDatumComparator Aperture =
        new ImageDatumComparator(
            CORE_APERTURE, LOCALE.get("ApertureSort"), false
        );

    public static ImageDatumComparator Speed =
        new ImageDatumComparator(
            CORE_SHUTTER_SPEED, LOCALE.get("ShutterSort"), false
        );

    public static ImageDatumComparator Size =
        new ImageDatumComparator(
            CORE_FILE_SIZE, LOCALE.get("SizeSort"), false
        );

    public static ImageDatumComparator Lens =
        new ImageDatumComparator(
            CORE_LENS, LOCALE.get("LensSort"), false
        );

    public static ImageDatumComparator FocalLength =
        new ImageDatumComparator(
            CORE_FOCAL_LENGTH, LOCALE.get("FocalLengthSort"), false
        );

    boolean reverse;

    private int tagId;
    private String name;

    private ImageDatumComparator(int tagId, String name, boolean reverse) {
        this.tagId = tagId;
        this.name = name;
        this.reverse = reverse;
        AllDatums.add(this);
    }

    public static ImageDatumComparator[] getAll() {
        return AllDatums.toArray(new ImageDatumComparator[0]);
    }

    public int compare(ImageDatum left, ImageDatum right) {
        // Sorting respects ImageGroups:
        ImageDatum leftLeader = left.getGroup().getLeader();
        ImageDatum rightLeader = right.getGroup().getLeader();

        if (leftLeader != null) {
            left = leftLeader;
        }
        if (rightLeader != null) {
            right = rightLeader;
        }
        ImageMetadata metaLeft = left.getMetadata(true);
        ImageMetadata metaRight = right.getMetadata(true);

        Class<? extends ImageMetadataDirectory> clazz = CoreDirectory.class;
        ImageMetaValue leftValue = metaLeft.getValue(clazz, tagId);
        ImageMetaValue rightValue = metaRight.getValue(clazz, tagId);

        if (leftValue != null && rightValue != null) {
            if (! reverse) {
                int comp = leftValue.compareTo(rightValue);
                if (comp == 0) {
                    comp = compareNames(left, right);
                }
                return comp;
            }
            else {
                int comp = rightValue.compareTo(leftValue);
                if (comp == 0) {
                    comp = compareNames(left, right);
                }
                return comp;
            }
        }
        else if ((leftValue != null) && (rightValue == null)) {
            return reverse ? -1 : +1;
        }
        else if ((leftValue == null) && (rightValue != null)) {
            return reverse ? +1 : -1;
        }
        else {
            return compareNames(left, right);
        }
    }

    void setReversed(boolean reverse) {
        this.reverse = reverse;
    }

    // Used in ImageDatum to know which tags to retain.
    int getTagId() {
        return tagId;
    }

    public String toString() {
        return name;
    }

    // A copy of the compare() method that compares CORE_FILE_NAME.  It works
    // the same as the "Name" ImageDatumComparator class variable, only without
    // recursion.  (There are exotic files where using Name.compare() itself
    // causes stack overflow.)
    private int compareNames(ImageDatum left, ImageDatum right) {
        // Sorting respects ImageGroups:
        ImageDatum leftLeader = left.getGroup().getLeader();
        ImageDatum rightLeader = right.getGroup().getLeader();

        if (leftLeader != null) {
            left = leftLeader;
        }
        if (rightLeader != null) {
            right = rightLeader;
        }
        ImageMetadata metaLeft = left.getMetadata(true);
        ImageMetadata metaRight = right.getMetadata(true);

        Class<? extends ImageMetadataDirectory> clazz = CoreDirectory.class;
        ImageMetaValue leftValue = metaLeft.getValue(clazz, CORE_FILE_NAME);
        ImageMetaValue rightValue = metaRight.getValue(clazz, CORE_FILE_NAME);

        if (leftValue != null && rightValue != null) {
            if (! reverse) {
                int comp = leftValue.compareTo(rightValue);
                return comp;
            }
            else {
                int comp = rightValue.compareTo(leftValue);
                return comp;
            }
        }
        else if ((leftValue != null) && (rightValue == null)) {
            return reverse ? -1 : +1;
        }
        else if ((leftValue == null) && (rightValue != null)) {
            return reverse ? +1 : -1;
        }
        else {
            return 0;
        }
    }
}