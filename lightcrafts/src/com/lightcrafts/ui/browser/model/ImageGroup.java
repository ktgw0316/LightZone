/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import java.util.*;

/**
 * ImageDatums can be clustered into groups.  Every ImageGroup has one
 * ImageDatum that is its group leader.  See ImageDatum.getGroup(),
 * ImageDatum.setGroup(), and ImageDatumComparator.
 */
public class ImageGroup {

    private final List<ImageDatum> datums;

    private ImageDatum leader;

    public List<ImageDatum> getImageDatums() {
        return new LinkedList<>(datums);
    }

    public boolean isNonTrivial() {
        return datums.size() > 1;
    }

    ImageGroup(ImageDatum leader) {
        this.leader = leader;
        datums = new LinkedList<>();
        datums.add(leader);
    }

    void addImageDatum(ImageDatum datum) {
        datums.add(datum);
    }

    void removeImageDatum(ImageDatum datum) {
        datums.remove(datum);
        if (leader == datum) {
            leader = null;
        }
    }

    ImageDatum getLeader() {
        return leader;
    }

    /**
     * For ImageGroupProviders, this provides a static utility to check the
     * internal consistency of ImageGroup assignments for a Collection of
     * ImageDatums.
     * <p>
     * Consistency means that among all ImageGroups bound to the given
     * ImageDatums, no ImageDatum occurs in more than one group, and that
     * among all the ImageGroup members, there is not an ImageDatum that is
     * not included in the given Collection.
     */
    public static boolean checkConsistency(Collection<ImageDatum> datums) {
        // Identify the distinct ImageGroups:
        Set<ImageGroup> groups = new HashSet<>();
        for (ImageDatum datum : datums) {
            ImageGroup group = datum.getGroup();
            groups.add(group);
        }
        // Tally all ImageDatums in the groups:
        Set<ImageDatum> seen = new HashSet<>();
        for (ImageGroup group : groups) {
            List<ImageDatum> members = group.getImageDatums();
            for (ImageDatum member : members) {
                if (seen.contains(member)) {
                    System.err.println("ImageDatum in multiple ImageGroups");
                    return false;
                }
                if (! datums.contains(member)) {
                    System.err.println("Unexpected ImageDatum in ImageGroup");
                    return false;
                }
                seen.add(member);
            }
        }
        return true;
    }

    public boolean isGroupStart(List<ImageDatum> data, int index) {
        if (! isNonTrivial()) {
            return false;
        }
        if (index == 0) {
            return true;
        }
        ImageDatum prev = data.get(index - 1);
        return this != prev.getGroup();
    }

    public boolean isGroupEnd(List<ImageDatum> data, int index) {
        if (! isNonTrivial()) {
            return false;
        }
        if (index == data.size() - 1) {
            return true;
        }
        ImageDatum next = data.get(index + 1);
        return this != next.getGroup();
    }
}
