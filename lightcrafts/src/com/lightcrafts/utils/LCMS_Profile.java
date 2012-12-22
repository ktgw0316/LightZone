/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.awt.color.ICC_Profile;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Jul 27, 2006
 * Time: 4:51:02 PM
 * To change this template use File | Settings | File Templates.
 */

public class LCMS_Profile {
    private final static class LRUHashMap extends LinkedHashMap {
        LRUHashMap(int initialCapacity, float loadFactor, int max_entries) {
            super(initialCapacity, loadFactor, true);
            this.max_entries = max_entries;
        }

        public LRUHashMap(int initialCapacity, int max_entries) {
            super(initialCapacity);
            this.max_entries = max_entries;
        }

        public LRUHashMap(int max_entries) {
            super();
            this.max_entries = max_entries;
        }

        private final int max_entries;

        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > max_entries;
        }
    }

    private static Map profileCache = new LRUHashMap(20);

    private long cmsProfileHandle = 0;

    public LCMS_Profile(ICC_Profile iccProfile) {
        Long handle = (Long) profileCache.get(iccProfile);
        if (handle == null) {
            byte data[] = iccProfile.getData();
            cmsProfileHandle = LCMS.cmsOpenProfileFromMem(data, data.length);
            profileCache.put(iccProfile, new Long(cmsProfileHandle));
        } else
            cmsProfileHandle = handle.longValue();
    }

    public void dispose() {
        if (cmsProfileHandle != 0) {
            LCMS.cmsCloseProfile(cmsProfileHandle);
            cmsProfileHandle = 0;
        }
    }

    /* public void finalize() {
        dispose();
    } */
}
