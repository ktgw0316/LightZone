/*
 * $RCSfile: ParameterBlockJAI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:14 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.CaselessStringArrayTable;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

/**
 * A convenience subclass of <code>ParameterBlock</code> that
 * allows the use of default parameter values and getting/setting
 * sources and parameters by name. A <code>ParameterBlockJAI</code> is
 * constructed using either an <code>OperationDescriptor</code>,
 * or an operation name (that will be looked up in the appropriate
 * default <code>OperationRegistry</code>) and a mode which should
 * be in <code>OperationDescriptor.getSupportedModes()</code> (such
 * as rendered, renderable, collection or renderableCollection). If
 * the mode is not specified <code>ParameterBlockJAI</code> will by
 * default work with the first mode in the array of <code>String</code>s
 * returned by <code>OperationDescriptor.getSupportedModes()</code>.
 *
 * <p> Once constructed, a <code>ParameterBlockJAI</code> appears to
 * have no sources. It contains all the parameters required by its
 * <code>OperationDescriptor</code> for a specified mode, each having
 * its default value as given by the <code>OperationDescriptor</code>.
 * Such a <code>ParameterBlockJAI</code> may not yet be usable, its
 * sources (if any) are not set, and some or all of its parameters may
 * have inapproriate values. The <code>addSource</code> methods of
 * <code>ParameterBlock</code> may be used to initialize the source values,
 * and the <code>set(value, index)</code> methods may be used to modify
 * new parameter values. The preferred way of setting parameter values
 * is the <code>setParameter(name, value)</code> described below. The
 * <code>add()</code> methods should not be used since the parameter
 * list is already long enough to hold all of the parameters required by
 * the <code>OperationDescriptor</code>.
 *
 * <p> Additionally, <code>ParameterBlockJAI</code> offers
 * <code>setParameter(name, value)</code> methods that take a
 * parameter name; the index of the parameter is determined from the
 * <code>OperationDescriptor</code> and the corresponding parameter
 * is set. (users are strongly recommended to use this method
 * instead of the equivalent <code>set(value, index)</code> or
 * the deprecated <code>set(value, name)</code> methods). As in
 * <code>ParameterBlock</code>, all parameters are stored internally
 * as subclasses of Object and all get/set methods that take or return
 * values of primitive types are simply convenience methods that transform
 * values between the primitive types and their corresponding wrapper
 * classes.
 *
 * <p> The <code>OperationDescriptor</code> that is used to initialize
 * a <code>ParameterBlockJAI</code> at construction is not
 * serializable and thus cannot be serialized using the default
 * serialization mechanism. The operation name is serialized instead and
 * included in the serialized <code>ParameterBlockJAI</code> stream.
 * During de-serialization, the operation name is de-serialized and then
 * looked up in the default <code>OperationRegistry</code> available at
 * the time of de-serialization. If no <code>OperationDescriptor</code>
 * has been registered with this <code>OperationRegistry</code>
 * under the given operation name, a NotSerializableException will
 * be thrown. The serialization of <code>ParameterBlockJAI</code>
 * works correctly only if the <code>OperationDescriptor</code>
 * registered for the operation name in question is identical to the
 * <code>OperationDescriptor</code> that was registered with the
 * <code>OperationRegistry</code> available at serialization time.
 *
 * <p> All parameter names are treated in a case-insensitive but
 * retentive manner.
 *
 * <p> <strong>Warning:</strong> Serialized objects of this class will
 * not be compatible with future releases. The current serialization
 * support is appropriate for short term storage or RMI between
 * applications running the same version of JAI. A future release of JAI
 * will provide support for long term persistence.
 */
public class ParameterBlockJAI extends ParameterBlock
                               implements ParameterList {
    
    /** 
     * The <code>OperationDescriptor</code> associated with this
     * <code>ParameterBlockJAI</code>. 
     */
    private transient OperationDescriptor odesc;

    /**
     * The operation mode.
     */
    private String modeName;

    /**
     * The ParameterListDescriptor for the specific mode for the operator.
     */
    private ParameterListDescriptor pld;

    /**
     * A <code>CaselessStringArrayTable</code> of parameter indices hashed by
     * <code>CaselessStringKey</code> versions of the parameter names.
     */
    private CaselessStringArrayTable paramIndices;

    /**
     * A <code>CaselessStringArrayTable</code> source indices hashed by
     * <code>CaselessStringKey</code> versions of the source names.
     */
    private CaselessStringArrayTable sourceIndices;

    /** The number of parameters. Cached for convenience. */
    private int numParameters;

    /** The names of the parameters. Cached for convenience. */
    private String[] paramNames;

    /** The Class types of the parameters. */
    private Class[] paramClasses;

    /** The Class types of the sources. */
    private Class[] sourceClasses;

    private static String getDefaultMode(OperationDescriptor odesc) {

	if (odesc == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	return odesc.getSupportedModes()[0];
    }

    /**
     * Constructs a <code>ParameterBlockJAI</code> for
     * use with an operation described by a particular
     * <code>OperationDescriptor</code>. It uses the first
     * mode in the array of <code>String</code>s returned by
     * <code>OperationDescriptor.getSupportedModes()</code>
     * to get the <code>ParameterListDescriptor</code> from
     * <code>OperationDescriptor</code>. The default values of the
     * parameters are filled in.
     *
     * @param odesc the OperationDescriptor describing the parameters
     *		to be managed.
     *
     * @throws IllegalArgumentException if odesc is null
     */
    public ParameterBlockJAI(OperationDescriptor odesc) {
	this(odesc, getDefaultMode(odesc));
    }

    /**
     * Constructs a <code>ParameterBlockJAI</code> for a particular
     * operation by name. The <code>OperationRegistry</code> associated
     * with the default instance of the <code>JAI</code> class is used
     * to locate the <code>OperationDescriptor</code> associated with
     * the operation name.
     *
     * It uses the first mode in the array of <code>String</code>s
     * returned by <code>OperationDescriptor.getSupportedModes()</code>
     * to get the <code>ParameterListDescriptor</code> from
     * <code>OperationDescriptor</code>. The default values of the
     * parameters are filled in.
     *
     * @param operationName a <code>String</code> giving the name of the operation.
     *
     * @throws IllegalArgumentException if operationName is null.
     */
    public ParameterBlockJAI(String operationName) {
	this((OperationDescriptor)
	     JAI.getDefaultInstance().getOperationRegistry().
             getDescriptor(OperationDescriptor.class, operationName));
    }

    /**
     * Constructs a <code>ParameterBlockJAI</code> for
     * use with an operation described by a particular
     * <code>OperationDescriptor</code> and a registry mode. The default
     * values of the parameters are filled in.
     *
     * @param odesc the OperationDescriptor describing the parameters
     *		to be managed.
     * @param modeName the operation mode whose paramters are to be managed.
     *
     * @throws IllegalArgumentException if modeName is null or odesc is null
     *
     * @since JAI 1.1
     */
    public ParameterBlockJAI(OperationDescriptor odesc, String modeName) {

	if ((odesc == null) || (modeName == null))
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	this.odesc = odesc;
	this.modeName = modeName;

	pld = odesc.getParameterListDescriptor(modeName);

        numParameters = pld.getNumParameters();
        paramNames = pld.getParamNames();

	paramIndices  = new CaselessStringArrayTable(pld.getParamNames());
	sourceIndices = new CaselessStringArrayTable(odesc.getSourceNames());

	paramClasses  = pld.getParamClasses();
	sourceClasses = odesc.getSourceClasses(modeName);

	Object[] defaults = pld.getParamDefaults();

        parameters = new Vector(numParameters);

	for (int i = 0; i < numParameters; i++) {
	    parameters.addElement(defaults[i]);
	}
    }

    /**
     * Constructs a <code>ParameterBlockJAI</code> for a
     * particular operation by name and a registry mode. The
     * <code>OperationRegistry</code> associated with the default
     * instance of the <code>JAI</code> class is used to locate the
     * <code>OperationDescriptor</code> associated with the operation
     * name. The default values of the parameters are filled in.
     *
     * @param operationName a <code>String</code> giving the name of the
     *        operation.
     * @param modeName the operation mode whose paramters are to be managed.
     *
     * @throws IllegalArgumentException if operationName or modeName is null
     *
     * @since JAI 1.1
     */
    public ParameterBlockJAI(String operationName, String modeName) {
	this((OperationDescriptor)
	     JAI.getDefaultInstance().getOperationRegistry().
             getDescriptor(modeName, operationName), modeName);
    }

    /**
     * Returns the zero-relative index of a named source within the list of
     * sources.
     *
     * @param sourceName a <code>String</code> containing the parameter name.
     * @throws IllegalArgumentException if source is null or if there is
     *		no source with the specified name.
     *
     * @since JAI 1.1
     */
    public int indexOfSource(String sourceName) {
	return sourceIndices.indexOf(sourceName);
    }

    /**
     * Returns the zero-relative index of a named parameter within the list of
     * parameters.
     *
     * @param paramName a <code>String</code> containing the parameter name.
     *
     * @throws IllegalArgumentException if paramName is null or if there is
     *		no parameter with the specified name.
     *
     * @since JAI 1.1
     */
    public int indexOfParam(String paramName) {
	return paramIndices.indexOf(paramName);
    }

    /**
     * Returns the <code>OperationDescriptor</code> associated with this
     * <code>ParameterBlockJAI</code>.
     */
    public OperationDescriptor getOperationDescriptor() {
        return odesc;
    }

    /**
     * Returns the <code>ParameterListDescriptor</code> that provides
     * descriptions of the parameters associated with the operator
     * and mode.
     *
     * @since JAI 1.1
     */
    public ParameterListDescriptor getParameterListDescriptor() {
	return pld;
    }

    /**
     * Get the operation mode used to determine parameter names,
     * classes and default values.
     *
     * @since JAI 1.1
     */
    public String getMode() {
	return modeName;
    }

    /**
     * Sets a named source to a given <code>Object</code> value.
     *
     * @param sourceName a <code>String</code> naming a source.
     * @param source an <code>Object</code> value for the source.
     * 
     * @throws IllegalArgumentException if <code>source</code> is null.
     * @throws IllegalArgumentException if <code>sourceName</code> is null.
     * @throws IllegalArgumentException if <code>source</code> is not
     *                                  an instance of (any of) the
     *                                  expected class(es).
     * @throws IllegalArgumentException if the associated operation has
     *                                  no source with the supplied name.
     *
     * @since JAI 1.1
     */
    public ParameterBlockJAI setSource(String sourceName, Object source) {
        if ((source == null) || (sourceName == null)) {
            throw new IllegalArgumentException(
			  JaiI18N.getString("Generic0"));
        }

        int index = indexOfSource(sourceName);

        if (!sourceClasses[index].isInstance(source)) {
            throw new IllegalArgumentException(
                      JaiI18N.getString("ParameterBlockJAI4"));
        }

        if (index >= odesc.getNumSources()) {
            addSource(source);
        } else {
            setSource(source, index);
        }

        return this;
    }

    /**
     * Returns an array of <code>Class</code> objects describing the types
     * of the parameters.  This is a more efficient implementation than that
     * of the superclass as the parameter classes are known a priori.
     *
     * @since JAI 1.1
     */
    public Class [] getParamClasses() {
        // Just return the Class array obtained from the OD's PLD.
        return paramClasses;
    }

    /**
     * Gets the named parameter as an <code>Object</code>.
     *
     * @throws IllegalStateException if the param value is
     *	    <code>ParameterListDescriptor.NO_PARAMETER_DEFAULT</code>
     */
    private Object getObjectParameter0(String paramName) {

        Object obj = getObjectParameter(indexOfParam(paramName));

	if (obj == ParameterListDescriptor.NO_PARAMETER_DEFAULT)
	    throw new IllegalStateException(paramName + ":" +
			JaiI18N.getString("ParameterBlockJAI6"));

	return obj;
    }

    /**
     * Gets a named parameter as an Object. Parameters belonging to a
     * primitive type, such as <code>int</code>, will be returned as a
     * member of the corresponding <code>Number</code> subclass, such as
     * <code>Integer</code>.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public Object getObjectParameter(String paramName) {
        return getObjectParameter0(paramName);
    }

    /**
     * A convenience method to return a parameter as a <code>byte</code>.  An
     * exception will be thrown if the parameter is of a different
     * type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public byte getByteParameter(String paramName) {
        return ((Byte)getObjectParameter0(paramName)).byteValue();
    }

    /**
     * A convenience method to return a parameter as a <code>boolean</code>. An
     * exception will be thrown if the parameter is of a different
     * type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     *
     * @since JAI 1.1
     */
    public boolean getBooleanParameter(String paramName) {
      return ((Boolean)getObjectParameter0(paramName)).booleanValue();
    }

    /**
     * A convenience method to return a parameter as a <code>char</code>.  An
     * exception will be thrown if the parameter is of a different
     * type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public char getCharParameter(String paramName) {
        return ((Character)getObjectParameter0(paramName)).charValue();
    }

    /**
     * A convenience method to return a parameter as an <code>short</code>.  An
     * exception will be thrown if the parameter is of a different
     * type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     *
     * @since JAI 1.1
     */
    public short getShortParameter(String paramName) {
        return ((Short)getObjectParameter0(paramName)).shortValue();
    }

    /**
     * A convenience method to return a parameter as an <code>int</code>.  An
     * exception will be thrown if the parameter is of a different
     * type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public int getIntParameter(String paramName) {
        return ((Integer)getObjectParameter0(paramName)).intValue();
    }

    /**
     * A convenience method to return a parameter as a <code>long</code>.  An
     * exception will be thrown if the parameter is of a different
     * type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public long getLongParameter(String paramName) {
        return ((Long)getObjectParameter0(paramName)).longValue();
    }

    /**
     * A convenience method to return a parameter as a <code>float</code>.  An
     * exception will be thrown if the parameter is of a different
     * type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public float getFloatParameter(String paramName) {
        return ((Float)getObjectParameter0(paramName)).floatValue();
    }

    /**
     * A convenience method to return a parameter as a <code>double</code>.  An
     * exception will be thrown if the parameter is of a different
     * type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public double getDoubleParameter(String paramName) {
        return ((Double)getObjectParameter0(paramName)).doubleValue();
    }

    // NEW ParameterList methods.

    /**
     * Sets a named parameter to a <code>byte</code> value.
     * Checks are made to verify that the parameter is of the right
     * <code>Class</code> type and that the value is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param b a <code>byte</code> value for the parameter.
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the class type of parameter
     *		pointed to by the paramName is not a <code>Byte</code>
     * @throws IllegalArgumentException if the parameter value is invalid.
     *
     * @since JAI 1.1
     */
    public ParameterList setParameter(String paramName, byte b) {
        return setParameter0(paramName, new Byte(b));
    }

    /**
     * Sets a named parameter to a <code>boolean</code> value.
     * Checks are made to verify that the parameter is of the right
     * <code>Class</code> type and that the value is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param b a <code>boolean</code> value for the parameter.
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the class type of parameter
     *		pointed to by the paramName is not a <code>Boolean</code>
     * @throws IllegalArgumentException if the parameter value is invalid.
     *
     * @since JAI 1.1
     */  
    public ParameterList setParameter(String paramName, boolean b) {
        return setParameter0(paramName, new Boolean(b));
    }

    /**
     * Sets a named parameter to a <code>char</code> value.
     * Checks are made to verify that the parameter is of the right
     * <code>Class</code> type and that the value is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param c a <code>char</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the class type of parameter
     *		pointed to by the paramName is not a <code>Character</code>
     * @throws IllegalArgumentException if the parameter value is invalid.
     *
     * @since JAI 1.1
     */
    public ParameterList setParameter(String paramName, char c) {
        return setParameter0(paramName, new Character(c));
    }

    /**
     * Sets a named parameter to a <code>short</code> value.
     * Checks are made to verify that the parameter is of the right
     * <code>Class</code> type and that the value is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param s a <code>short</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the class type of parameter
     *		pointed to by the paramName is not a <code>Short</code>
     * @throws IllegalArgumentException if the parameter value is invalid.
     *
     * @since JAI 1.1
     */
    public ParameterList setParameter(String paramName, short s) {
        return setParameter0(paramName, new Short(s));
    }

    /**
     * Sets a named parameter to an <code>int</code> value.
     * Checks are made to verify that the parameter is of the right
     * <code>Class</code> type and that the value is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param i an <code>int</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the class type of parameter
     *		pointed to by the paramName is not a <code>Integer</code>
     * @throws IllegalArgumentException if the parameter value is invalid.
     *
     * @since JAI 1.1
     */
    public ParameterList setParameter(String paramName, int i) {
        return setParameter0(paramName, new Integer(i));
    }

    /**
     * Sets a named parameter to a <code>long</code> value.
     * Checks are made to verify that the parameter is of the right
     * <code>Class</code> type and that the value is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param l a <code>long</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the class type of parameter
     *		pointed to by the paramName is not a <code>Long</code>
     * @throws IllegalArgumentException if the parameter value is invalid.
     *
     * @since JAI 1.1
     */
    public ParameterList setParameter(String paramName, long l) {
        return setParameter0(paramName, new Long(l));
    }

    /**
     * Sets a named parameter to a <code>float</code> value.
     * Checks are made to verify that the parameter is of the right
     * <code>Class</code> type and that the value is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param f a <code>float</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the class type of parameter
     *		pointed to by the paramName is not a <code>Float</code>
     * @throws IllegalArgumentException if the parameter value is invalid.
     *
     * @since JAI 1.1
     */
    public ParameterList setParameter(String paramName, float f) {
        return setParameter0(paramName, new Float(f));
    }

    /**
     * Sets a named parameter to a <code>double</code> value.
     * Checks are made to verify that the parameter is of the right
     * <code>Class</code> type and that the value is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param d a <code>double</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the class type of parameter
     *		pointed to by the paramName is not a <code>Double</code>
     * @throws IllegalArgumentException if the parameter value is invalid.
     *
     * @since JAI 1.1
     */
    public ParameterList setParameter(String paramName, double d) {
        return setParameter0(paramName, new Double(d));
    }

    /**
     * Sets a named parameter to an <code>Object</code> value.  The value
     * may be <code>null</code>, an instance of the class expected for this
     * parameter, or a <code>DeferredData</code> instance the
     * <code>getDataClass()</code> method of which returns the
     * expected class.  If the object is a <code>DeferredData</code> instance,
     * then its wrapped data value is checked for validity if and only if
     * its <code>isValid()</code> method returns <code>true</code>.  If the
     * object is not a <code>DeferredData</code> instance, then it is
     * always checked for validity.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param obj an <code>Object</code> value for the parameter.
     *
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the parameter value is invalid.
     *
     * @since JAI 1.1
     */
    public ParameterList setParameter(String paramName, Object obj) {
        return setParameter0(paramName, obj);
    }

    /**
     * Checks to see if the specified parameter is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param obj an Object value for the parameter.
     *
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if <code>obj</code> is
     *         non-<code>null</code> and not an instance of the class
     *         expected for the indicated parameter or if <code>obj</code>
     *         is an invalid value for the indicated parameter.
     *
     * @return the index of the parameter
     */
    private int checkParameter(String paramName, Object obj) {
        int index = indexOfParam(paramName);

        if(obj != null) {

	    if (obj == ParameterListDescriptor.NO_PARAMETER_DEFAULT) {
		throw new IllegalArgumentException(paramName + ":" +
			       JaiI18N.getString("ParameterBlockJAI8"));
	    }

            if (obj instanceof DeferredData) {
                DeferredData dd = (DeferredData)obj;
                if (!paramClasses[index].isAssignableFrom(dd.getDataClass())) {
                    throw new IllegalArgumentException(paramName + ":" +
			       JaiI18N.getString("ParameterBlockJAI0"));
                }

                if (dd.isValid() &&
                    !pld.isParameterValueValid(paramName, dd.getData())) {
                    throw new IllegalArgumentException(paramName + ":" +
			       JaiI18N.getString("ParameterBlockJAI2"));
                }
            } else if (!paramClasses[index].isInstance(obj)) {
                throw new IllegalArgumentException(paramName + ":" +
			       JaiI18N.getString("ParameterBlockJAI0"));
            }
        }

        if(obj == null || !(obj instanceof DeferredData)) {
            if (!pld.isParameterValueValid(paramName, obj)) {
                throw new IllegalArgumentException(paramName + ":" +
                                                   JaiI18N.getString("ParameterBlockJAI2"));
            }
        }

	return index;
    }

    /**
     * Sets a named parameter to an Object value.  The value may be
     * <code>null</code>, an instance of the class expected for this
     * parameter, or a <code>DeferredData</code> instance the
     * <code>getDataClass()</code> method of which returns the
     * expected class.  If the object is a <code>DeferredData</code> instance,
     * then its wrapped data value is checked for validity if and only if
     * its <code>isValid()</code> method returns <code>true</code>.
     * If the object is not a <code>DeferredData</code> instance, then it is
     * always checked for validity.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param obj an Object value for the parameter.
     *
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if <code>obj</code> is
     *         non-<code>null</code> and not an instance of the class
     *         expected for the indicated parameter or if <code>obj</code>
     *         is an invalid value for the indicated parameter.
     */
    private ParameterList setParameter0(String paramName, Object obj) {

        int index = checkParameter(paramName, obj);

        parameters.setElementAt(obj, index);
	return this;
    }

    /* ----- Superclass methods overridden for consistent behavior. ----- */

    /** 
     * Adds an object to the list of parameters. 
     * 
     * This method always throws an <code>IllegalStateException</code>
     * because the <code>ParameterBlockJAI</code> constructor initializes
     * all parameters with their default values.
     * 
     * @throws IllegalStateException if parameters are added to an already 
     * initialized ParameterBlockJAI
     *
     * @since JAI 1.1
     */
    
    public ParameterBlock add(Object obj) {
      throw new IllegalStateException(JaiI18N.getString("ParameterBlockJAI5"));
    }
  
    /**
     * Replaces an Object in the list of parameters.
     *
     * @param obj The new value of the parameter.
     * @param index The zero-relative index of the parameter.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>index</code>
     *         is negative or not less than the number of parameters
     *         expected for the associated operation.
     * @throws IllegalArgumentException if <code>obj</code> is
     *         non-<code>null</code> and not an instance of the class
     *         expected for the indicated parameter or if <code>obj</code>
     *         is an invalid value for the indicated parameter.
     *
     * @since JAI 1.1
     */
    public ParameterBlock set(Object obj, int index) {
        if(index < 0 || index >= pld.getNumParameters()) {
            throw new ArrayIndexOutOfBoundsException();
        }

        // Not the most efficient implementation but has minimum duplication.
        setParameter0(paramNames[index], obj);

        return this;
    }

    /**
     * Sets the entire <code>Vector</code> of parameters to a given
     * <code>Vector</code>.  The <code>Vector</code> is saved by reference.
     *
     * @throws IllegalArgumentException if the size of the supplied
     *         <code>Vector</code> does not equal the number of parameters
     *         of the associated operation.
     * @throws IllegalArgumentException if a non-<code>null</code>,
     *         non-<code>DeferredData</code> value is not an instance of
     *         the class expected for the indicated parameter or if
     *         <code>obj</code> is an invalid value for the indicated
     *         parameter.
     * @throws IllegalArgumentException if a non-<code>null</code>,
     *         <code>DeferredData</code> value does not wrap an instance of
     *         the class expected for the indicated parameter or if it is
     *         valid but its wrapped value is invalid for the indicated
     *         parameter.
     *
     * @since JAI 1.1
     */
    public void setParameters(Vector parameters) {
        if (parameters == null || parameters.size() != numParameters) {
            throw new IllegalArgumentException(JaiI18N.getString("ParameterBlockJAI7"));
        }

        for(int i = 0; i < numParameters; i++) {
	    checkParameter(paramNames[i], parameters.get(i));
        }

        this.parameters = parameters;
    }

    /********************** DEPRECATED METHODS *************************/

    /**
     * Returns the zero-relative index of a named parameter within the list of
     * parameters.
     *
     * @param paramName a <code>String</code> containing the parameter name.
     *
     * @throws IllegalArgumentException if paramName is null or if there is
     *		no parameter with the specified name.
     *
     * @deprecated as of JAI 1.1 - use "indexOfParam" instead.
     *
     * @see #indexOfParam
     */
    public int indexOf(String paramName) {
	return indexOfParam(paramName);
    }

    /**
     * Sets a named parameter to a <code>byte</code> value.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param b a <code>byte</code> value for the parameter.
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     *
     * @deprecated as of JAI 1.1 - use <code>setParameter</code> instead.
     *
     * @see #setParameter(String, byte)
     */
    public ParameterBlock set(byte b, String paramName) {
        return set(new Byte(b), paramName);
    }

    /**
     * Sets a named parameter to a <code>char</code> value.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param c a <code>char</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     *
     * @deprecated as of JAI 1.1 - use <code>setParameter</code> instead.
     *
     * @see #setParameter(String, char)
     */
    public ParameterBlock set(char c, String paramName) {
        return set(new Character(c), paramName);
    }

    /**
     * Sets a named parameter to a short value.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param s a short value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     *
     * @deprecated as of JAI 1.1 - use <code>setParameter</code> instead.
     *
     * @see #setParameter(String, short)
     */
    public ParameterBlock set(short s, String paramName) {
        return set(new Short(s), paramName);
    }

    /**
     * Sets a named parameter to an <code>int</code> value.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param i an <code>int</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     *
     * @deprecated as of JAI 1.1 - use <code>setParameter</code> instead.
     *
     * @see #setParameter(String, int)
     */
    public ParameterBlock set(int i, String paramName) {
        return set(new Integer(i), paramName);
    }

    /**
     * Sets a named parameter to a <code>long</code> value.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param l a <code>long</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     *
     * @deprecated as of JAI 1.1 - use <code>setParameter</code> instead.
     *
     * @see #setParameter(String, long)
     */
    public ParameterBlock set(long l, String paramName) {
        return set(new Long(l), paramName);
    }

    /**
     * Sets a named parameter to a <code>float</code> value.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param f a <code>float</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     *
     * @deprecated as of JAI 1.1 - use <code>setParameter</code> instead.
     *
     * @see #setParameter(String, float)
     */
    public ParameterBlock set(float f, String paramName) {
        return set(new Float(f), paramName);
    }

    /**
     * Sets a named parameter to a <code>double</code> value.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param d a <code>double</code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     *
     * @deprecated as of JAI 1.1 - use <code>setParameter</code> instead.
     *
     * @see #setParameter(String, double)
     */
    public ParameterBlock set(double d, String paramName) {
        return set(new Double(d), paramName);
    }

    /**
     * Sets a named parameter to an Object value.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param obj an Object value for the parameter.
     *
     * @throws IllegalArgumentException if obj is null, or if the class
     *         type of obj does not match the class type of parameter
     *         pointed to by the paramName.
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     *
     * @deprecated as of JAI 1.1 - use <code>setParameter</code> instead.
     *
     * @see #setParameter(String, Object)
     */
    public ParameterBlock set(Object obj, String paramName) {
	setParameter0(paramName, obj);
        return this;
    }

    // [De]serialization methods.

    /**
     * Serialize the <code>ParameterBlockJAI</code>.
     * @throws IOException 
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Write the non-static and non-transient fields.
        out.defaultWriteObject();
        
	// Write out the operation name of the OperationDescriptor.
	out.writeObject(odesc.getName());
    }

    /**
     * Deserialize the <code>ParameterBlockJAI</code>.
     *
     * @throws IOException 
     * @throws NotSerializableException if no OperationDescriptor is
     *         registered with the current OperationRegistry under the
     *         deserialized operation name.
     */
    private void readObject(ObjectInputStream in) throws IOException,
	ClassNotFoundException {
        
	// Read the non-static and non-transient fields.
        in.defaultReadObject();

	// Read the operation name
	String operationName = (String)in.readObject();

	// Try to get the OperationDescriptor registered under this name
	odesc = (OperationDescriptor)JAI.getDefaultInstance().
		    getOperationRegistry().
			getDescriptor(modeName, operationName);

	if (odesc == null) {
	    throw new NotSerializableException(operationName + " " + 
			    JaiI18N.getString("ParameterBlockJAI1"));
	}
    }    
    
    /**
     * Creates a copy of a <code>ParameterBlockJAI</code>. The source
     * and parameter Vectors are cloned, but the actual sources and
     * parameters are copied by reference. This allows modifications to
     * the order and number of sources and parameters in the clone to be
     * invisible to the original <code>ParameterBlockJAI</code>. Changes
     * to the shared sources or parameters themselves will still be
     * visible.
     *
     * @return an Object clone of the <code>ParameterBlockJAI</code>.
     *
     * @since JAI 1.1
     */
    public Object clone() {
        ParameterBlockJAI theClone = (ParameterBlockJAI)shallowClone();

        if (sources != null) {
            theClone.setSources((Vector)sources.clone());
        }

        if (parameters != null) {
	    // Clone the parameter Vector without doing the parameter
	    // validity checks.
            theClone.parameters = (Vector)parameters.clone();
        }
        return (Object) theClone;
    }

}
