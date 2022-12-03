/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import lombok.Getter;
import lombok.val;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An <code>ElementPrefixFilter</code> is-a {@link NodeTypeFilter} that only
 * accepts XML elements having a particular namespace prefix.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@Getter
public class ElementPrefixFilter extends NodeTypeFilter {

    // The prefix that's being used to filter on.
    private final String prefix;

    /**
     * Construct an <code>ElementPrefixFilter</code>.
     *
     * @param prefix The prefix to accept.
     */
    public ElementPrefixFilter( String prefix ) {
        super( Node.ELEMENT_NODE );
        this.prefix = prefix;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept( Node node ) {
        if ( !super.accept( node ) )
            return false;
        val element = (Element)node;
        return prefix.equals( element.getPrefix() );
    }
}
/* vim:set et sw=4 ts=4: */