/*
 * $RCSfile: PropertyGeneratorFromSource.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:17 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.util.Vector;
import com.lightcrafts.media.jai.util.PropertyGeneratorImpl;

/**
 * A class that implements the <code>PropertyGenerator</code> interface.
 * This class is used when a property is to be calculated from a particular
 * source.  All properties except the named one are ignored.  If the given
 * source index out of range the property will be undefined, in particular
 * no exception will be thrown.
 *
 */
class PropertyGeneratorFromSource extends PropertyGeneratorImpl {

    int sourceIndex;
    String propertyName;
    
    PropertyGeneratorFromSource(int sourceIndex, String propertyName) {
        super(new String[] {propertyName},
              new Class[] {Object.class}, // could be anything
              new Class[] {OperationNode.class});

        if(propertyName == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	this.sourceIndex = sourceIndex;
	this.propertyName = propertyName;
    }

    public Object getProperty(String name,
			      Object opNode) {
        if(name == null || opNode == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if(sourceIndex >= 0 &&
           opNode instanceof OperationNode &&
           propertyName.equalsIgnoreCase(name)) {
            OperationNode op = (OperationNode)opNode;
            Vector sources = op.getParameterBlock().getSources();
            if(sources != null && sourceIndex < sources.size()) {
                Object src = sources.elementAt(sourceIndex);
                if(src instanceof PropertySource) {
                    return ((PropertySource)src).getProperty(name);
                }
            }
        }

        return java.awt.Image.UndefinedProperty;
    }
}
