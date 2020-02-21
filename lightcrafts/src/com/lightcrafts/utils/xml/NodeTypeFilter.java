/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import lombok.RequiredArgsConstructor;
import org.w3c.dom.Node;

/**
 * A <code>NodeTypeFilter</code> is-an {@link XMLFilter} that matches only
 * nodes of a particular type.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@RequiredArgsConstructor
public class NodeTypeFilter implements XMLFilter {

    // The type of node to match.
    private final short nodeType;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accept( Node node ) {
        return node.getNodeType() == nodeType;
    }
}
/* vim:set et sw=4 ts=4: */
