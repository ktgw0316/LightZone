/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import org.w3c.dom.Node;

/**
 * A <code>NodeTypeFilter</code> is-an {@link XMLFilter} that matches only
 * nodes of a particular type.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class NodeTypeFilter implements XMLFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>NodeTypeFilter</code>.
     *
     * @param nodeType The type of node to match.
     */
    public NodeTypeFilter( short nodeType ) {
        m_nodeType = nodeType;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept( Node node ) {
        return node.getNodeType() == m_nodeType;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final short m_nodeType;
}
/* vim:set et sw=4 ts=4: */
