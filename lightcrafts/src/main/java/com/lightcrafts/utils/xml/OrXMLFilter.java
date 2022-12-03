/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import org.w3c.dom.Node;

/**
 * An <code>OrXMLFilter</code> is-an {@link XMLFilter} that matches if any one
 * of the given filters matches.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class OrXMLFilter implements XMLFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>OrXMLFilter</code>.
     *
     * @param filters The filters to use.
     */
    public OrXMLFilter( XMLFilter... filters ) {
        m_filters = filters;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept( Node node ) {
        for ( XMLFilter filter : m_filters )
            if ( filter.accept( node ) )
                return true;
        return false;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final XMLFilter[] m_filters;
}
/* vim:set et sw=4 ts=4: */
