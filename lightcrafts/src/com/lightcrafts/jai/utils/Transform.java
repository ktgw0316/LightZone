/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.utils;

import javax.media.jai.PlanarImage;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 7, 2005
 * Time: 7:50:33 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Transform {
    public abstract void setSource(Object source);
    public abstract PlanarImage render();
    public abstract PlanarImage update();
    public abstract void dispose();
}
