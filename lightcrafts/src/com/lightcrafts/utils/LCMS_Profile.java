/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

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

    private static Map<ICC_Profile, Long> profileCache = new LRUHashMap<ICC_Profile, Long>(20);

    private long cmsProfileHandle = 0;

    public LCMS_Profile(ICC_Profile iccProfile) {
        Long handle = profileCache.get(iccProfile);
        if (handle == null) {
            byte[] data = iccProfile.getData();
            cmsProfileHandle = LCMSNative.cmsOpenProfileFromMem(data, data.length);
            profileCache.put(iccProfile, cmsProfileHandle);
        } else
            cmsProfileHandle = handle;
    }

    public void dispose() {
        if (cmsProfileHandle != 0) {
            LCMSNative.cmsCloseProfile(cmsProfileHandle);
            cmsProfileHandle = 0;
        }
    }

}
