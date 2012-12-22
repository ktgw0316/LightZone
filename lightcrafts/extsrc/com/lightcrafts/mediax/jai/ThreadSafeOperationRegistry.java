/*
 * $RCSfile: ThreadSafeOperationRegistry.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:21 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.RWLock;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * A wrapper class on <code>OperationRegistry</code> which is
 * thread safe. Every method is wrapped with an appropriate read
 * or a write lock. Exceptions are caught and the lock is released
 * before the exception is re-thrown.
 *
 * @since JAI 1.1
 */
final class ThreadSafeOperationRegistry extends OperationRegistry {

    /** The reader/writer lock for this class. */
    private RWLock lock;

    public ThreadSafeOperationRegistry() {
	super();

	// Create an upgradable reader/writer lock.
	lock = new RWLock(true);
    }

    public String toString() {
	try {
	    lock.forReading();
	    String t = super.toString();
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void writeToStream(OutputStream out) throws IOException {
	try {
	    lock.forReading();
	    super.writeToStream(out);
	    lock.release();
	} catch (IOException ioe) {
	    lock.release();
	    throw ioe;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void initializeFromStream(InputStream in) throws IOException {
	try {
	    lock.forWriting();
	    super.initializeFromStream(in);
	    lock.release();
	} catch (IOException ioe) {
	    lock.release();
	    throw ioe;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void updateFromStream(InputStream in) throws IOException {
	try {
	    lock.forWriting();
	    super.updateFromStream(in);
	    lock.release();
	} catch (IOException ioe) {
	    lock.release();
	    throw ioe;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void readExternal(ObjectInput in)
	    throws IOException, ClassNotFoundException {

	try {
	    lock.forWriting();
	    super.readExternal(in);
	    lock.release();
	} catch (IOException ioe) {
	    lock.release();
	    throw ioe;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void writeExternal(ObjectOutput out) throws IOException {
	try {
	    lock.forReading();
	    super.writeExternal(out);
	    lock.release();
	} catch (IOException ioe) {
	    lock.release();
	    throw ioe;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    /********************** NEW JAI 1.1 methods *************************/

    public void removeRegistryMode(String modeName) {
	try {
	    lock.forWriting();
	    super.removeRegistryMode(modeName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public String[] getRegistryModes() {
	try {
	    lock.forReading();
	    String[] t = super.getRegistryModes();
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void registerDescriptor(RegistryElementDescriptor descriptor) {
	try {
	    lock.forWriting();
	    super.registerDescriptor(descriptor);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void unregisterDescriptor(RegistryElementDescriptor descriptor) {
	try {
	    lock.forWriting();
	    super.unregisterDescriptor(descriptor);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public RegistryElementDescriptor getDescriptor(
		    Class descriptorClass, String descriptorName) {
	try {
	    lock.forReading();
	    RegistryElementDescriptor t = super.getDescriptor(descriptorClass, descriptorName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public List getDescriptors(Class descriptorClass) {
	try {
	    lock.forReading();
	    List t = super.getDescriptors(descriptorClass);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public String[] getDescriptorNames(Class descriptorClass) {
	try {
	    lock.forReading();
	    String[] t = super.getDescriptorNames(descriptorClass);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public RegistryElementDescriptor getDescriptor(String modeName,
					    String descriptorName) {
	try {
	    lock.forReading();
	    RegistryElementDescriptor t = super.getDescriptor(modeName, descriptorName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public List getDescriptors(String modeName) {

	try {
	    lock.forReading();
	    List t = super.getDescriptors(modeName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public String[] getDescriptorNames(String modeName) {
	try {
	    lock.forReading();
	    String[] t = super.getDescriptorNames(modeName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void setProductPreference(String modeName,
				     String descriptorName,
				     String preferredProductName,
				     String otherProductName) {
	try {
	    lock.forWriting();
	    super.setProductPreference(modeName,
				       descriptorName,
				       preferredProductName,
				       otherProductName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void unsetProductPreference(String modeName,
				       String descriptorName,
				       String preferredProductName,
				       String otherProductName) {
	try {
	    lock.forWriting();
	    super.unsetProductPreference(modeName,
					 descriptorName,
					 preferredProductName,
					 otherProductName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void clearProductPreferences(String modeName,
				       String descriptorName) {
	try {
	    lock.forWriting();
	    super.clearProductPreferences(modeName, descriptorName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public String[][] getProductPreferences(String modeName,
					    String descriptorName) {
	try {
	    lock.forReading();
	    String[][] t = super.getProductPreferences(modeName, descriptorName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }


    public Vector getOrderedProductList(String modeName,
					String descriptorName) {
	try {
	    lock.forReading();
	    Vector t = super.getOrderedProductList(modeName, descriptorName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }


    public void registerFactory(String modeName,
				String descriptorName,
				String productName,
				Object factory) {
	try {
	    lock.forWriting();
	    super.registerFactory(modeName,
				  descriptorName,
				  productName,
				  factory);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void unregisterFactory(String modeName,
				  String descriptorName,
				  String productName,
				  Object factory) {
	try {
	    lock.forWriting();
	    super.unregisterFactory(modeName,
				    descriptorName,
				    productName,
				    factory);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void setFactoryPreference(String modeName,
				     String descriptorName,
				     String productName,
				     Object preferredOp,
				     Object otherOp) {
	try {
	    lock.forWriting();
	    super.setFactoryPreference(modeName,
				       descriptorName,
				       productName,
				       preferredOp,
				       otherOp);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void unsetFactoryPreference(String modeName,
				       String descriptorName,
				       String productName,
				       Object preferredOp,
				       Object otherOp) {
	try {
	    lock.forWriting();
	    super.unsetFactoryPreference(modeName,
					 descriptorName,
					 productName,
					 preferredOp,
					 otherOp);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void clearFactoryPreferences(String modeName,
					String descriptorName,
					String productName) {
	try {
	    lock.forWriting();
	    super.clearFactoryPreferences(modeName,
					  descriptorName,
					  productName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public Object[][] getFactoryPreferences(String modeName,
					    String descriptorName,
					    String productName) {
	try {
	    lock.forReading();
	    Object[][] t = super.getFactoryPreferences(
		    modeName, descriptorName, productName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public List getOrderedFactoryList(String modeName,
				      String descriptorName,
				      String productName) {
	try {
	    lock.forReading();
	    List t = super.getOrderedFactoryList(modeName,
					descriptorName,
					productName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public Iterator getFactoryIterator(String modeName,
				       String descriptorName) {
	try {
	    lock.forReading();
	    Iterator t = super.getFactoryIterator(modeName, descriptorName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public Object getFactory(String modeName, String descriptorName) {
	try {
	    lock.forReading();
	    Object t = super.getFactory(modeName, descriptorName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }


    public Object invokeFactory(String modeName,
				String descriptorName,
				Object[] args) {
	try {
	    lock.forReading();
	    Object t = super.invokeFactory(modeName, descriptorName, args);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }


    public void addPropertyGenerator(String modeName,
				     String descriptorName,
				     PropertyGenerator generator) {
	try {
	    lock.forWriting();
	    super.addPropertyGenerator(modeName,
				       descriptorName,
				       generator);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void removePropertyGenerator(String modeName,
					String descriptorName,
					PropertyGenerator generator) {
	try {
	    lock.forWriting();
	    super.removePropertyGenerator(modeName,
					  descriptorName,
					  generator);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void copyPropertyFromSource(String modeName,
				       String descriptorName,
				       String propertyName,
				       int sourceIndex) {
	try {
	    lock.forWriting();
	    super.copyPropertyFromSource(modeName,
					 descriptorName,
					 propertyName,
					 sourceIndex);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void suppressProperty(String modeName,
				 String descriptorName,
				 String propertyName) {
	try {
	    lock.forWriting();
	    super.suppressProperty(modeName,
				   descriptorName,
				   propertyName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void suppressAllProperties(String modeName,
				      String descriptorName) {
	try {
	    lock.forWriting();
	    super.suppressAllProperties(modeName, descriptorName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void clearPropertyState(String modeName) {
	try {
	    lock.forWriting();
	    super.clearPropertyState(modeName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public String[] getGeneratedPropertyNames(String modeName,
					      String descriptorName) {
	try {
	    lock.forReading();
	    String[] t = super.getGeneratedPropertyNames(modeName, descriptorName);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public PropertySource getPropertySource(String modeName,
					    String descriptorName,
					    Object op,
					    Vector sources) {
	try {
	    lock.forReading();
	    PropertySource t = super.getPropertySource(
		modeName, descriptorName, op, sources);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public PropertySource getPropertySource(OperationNode op) {
	try {
	    lock.forReading();
	    PropertySource t = super.getPropertySource(op);
	    lock.release();
	    return t;
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    public void registerServices(ClassLoader cl) throws IOException {
	try {
	    lock.forWriting();
	    super.registerServices(cl);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }

    /********************** DEPRECATED METHODS *************************/

    public void unregisterOperationDescriptor(String operationName) {
	try {
	    lock.forWriting();
	    super.unregisterOperationDescriptor(operationName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }
	
    public void clearOperationPreferences(String operationName,
					  String productName) {
	try {
	    lock.forWriting();
	    super.clearOperationPreferences(operationName, productName);
	    lock.release();
	} catch (RuntimeException e) {
	    lock.release();
	    throw e;
	}
    }
}
