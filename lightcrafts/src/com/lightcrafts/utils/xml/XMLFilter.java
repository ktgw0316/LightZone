/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import org.w3c.dom.Node;

/**
 * An <code>XMLFilter</code> is a filter for XML {@link Node}s.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface XMLFilter {

    /**
     * Tests whether a given {@link Node} should be included.
     *
     * @param node The {@link Node} to consider.
     * @return Returns <code>true</code> only if the {@link Node} is accepted.
     */
    boolean accept( Node node );

}
/* vim:set et sw=4 ts=4: */
