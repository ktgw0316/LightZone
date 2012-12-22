/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An <code>ElementFilter</code> is-a {@link NodeTypeFilter} that matches only
 * {@link Element} nodes having a particular name and, optionally, attributes
 * having a particular name and/or value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class ElementFilter extends NodeTypeFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>ElementFilter</code>.
     *
     * @param elementName The name of the element(s) to match.
     */
    public ElementFilter( String elementName ) {
        this( elementName, (String)null, (String)null );
    }

    /**
     * Construct an <code>ElementFilter</code>.
     *
     * @param elementName The name of the element(s) to match.
     * @param attributeName If not <code>null</code>, only matches elements
     * that have an attribute with the given name.
     * @param attributeValue If not <code>null</code>, only matches elements
     * that have an attribute with the given value.
     */
    public ElementFilter( String elementName, String attributeName,
                          String attributeValue ) {
        this(
            elementName,
            attributeName  != null ? new String[]{ attributeName  } : null,
            attributeValue != null ? new String[]{ attributeValue } : null
        );
    }

    /**
     * Construct an <code>ElementFilter</code>.
     *
     * @param elementName The name of the element(s) to match.
     * @param attributeNames If not <code>null</code>, only matches elements
     * that have all the attributes with the given names.
     * @param attributeValues If not <code>null</code>, only matches elements
     * that have attributes with the given values.  If a value is
     * <code>null</code>, then any value matches.
     */
    public ElementFilter( String elementName, String[] attributeNames,
                          String[] attributeValues ) {
        super( Node.ELEMENT_NODE );
        m_elementName = elementName;
        m_attNames = attributeNames;
        m_attValues = attributeValues;
    }

    /**
     * {@inheritDoc}
     */
    public boolean accept( Node node ) {
        if ( !super.accept( node ) )
            return false;
        final Element element = (Element)node;
        if ( !element.getTagName().equals( m_elementName ) )
            return false;
        if ( m_attNames != null ) {
            for ( int i = 0; i < m_attNames.length; ++i ) {
                if ( !element.hasAttribute( m_attNames[i] ) )
                    return false;
                if ( m_attValues != null && m_attNames[i] != null ) {
                    final String attValue =
                        element.getAttribute( m_attNames[i] );
                    if ( !attValue.equals( m_attValues[i] ) )
                        return false;
                }
            }
        }
        return true;
    }

    ////////// protected //////////////////////////////////////////////////////

    protected final String m_elementName;
    protected final String[] m_attNames;
    protected final String[] m_attValues;
}
/* vim:set et sw=4 ts=4: */
