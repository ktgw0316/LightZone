/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An <code>ElementPrefixFilter</code> is-a {@link NodeTypeFilter} that only
 * accepts XML elements having a particular namespace prefix.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class ElementPrefixFilter extends NodeTypeFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>ElementPrefixFilter</code>.
     *
     * @param prefix The prefix to accept.
     */
    public ElementPrefixFilter( String prefix ) {
        super( Node.ELEMENT_NODE );
        m_prefix = prefix;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept( Node node ) {
        if ( !super.accept( node ) )
            return false;
        final Element element = (Element)node;
        //
        // Use getTagName() since getPrefix() always returns null.
        //
        // return m_prefix.equals( element.getPrefix() );
        return m_prefix.equals( element.getTagName().replaceAll( ":.*", "" ) );
    }

    /**
     * Gets the prefix that's being used to filter on.
     *
     * @return Returns said prefix.
     */
    public String getPrefix() {
        return m_prefix;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final String m_prefix;
}
/* vim:set et sw=4 ts=4: */