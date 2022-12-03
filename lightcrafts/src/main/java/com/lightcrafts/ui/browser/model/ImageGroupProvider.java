/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import java.util.List;

/**
 * An interface to plug into AbstractImageBrowser.setClusterProvider(), for
 * defining ImageGroups of ImageDatums.  Implementations should use
 * ImageDatum.newGroup(), ImageDatum.getGroup() and ImageDatum.setGroup().
 */
public interface ImageGroupProvider {

    void cluster(List<ImageDatum> datums);
}
