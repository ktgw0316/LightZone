/*
 * $RCSfile: ParameterListImpl.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:15 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.CaselessStringArrayTable;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * A concrete implementation of the <code>ParameterList</code>
 * interface. The number, names, Class types and default values are
 * specified via the associated <code>ParameterListDescriptor</code>
 * which should be supplied at construction time. This
 * default implementation should be sufficient for most
 * <code>ParameterList</code>s and normally need not be sub-classed.
 *
 * @see ParameterList
 * @see ParameterListDescriptor
 * @see ParameterListDescriptorImpl
 *
 * @since JAI 1.1
 */
public class ParameterListImpl
	    implements ParameterList, java.io.Serializable {

    private ParameterListDescriptor pld;

    /**
     * A <code>CaselessStringArrayTable</code> of parameter indices hashed by
     * <code>CaselessStringKey</code> versions of the names.
     */
    private CaselessStringArrayTable paramIndices;

    /**
     * Something to hold the parameter values.
     */
    private Object[] paramValues;

    /**
     * The parameter classes obtained from <code>ParameterListDescriptor</code>
     */
    private Class[] paramClasses;

    /**
     * Creates a <code>ParameterListImpl</code> using the specified
     * <code>ParameterListDescriptor</code>. Initializes the
     * parameters to the defaults (could be 
     * <code>ParameterListDescriptor.NO_PARAMETER_DEFAULT</code>)
     * specified by <code>descriptor</code>
     *
     * @param descriptor a <code>ParameterListDescriptor</code> describing
     *			 the parameter names, defaults etc.
     *
     * @throws IllegalArgumentException if descriptor is <code>null</code>
     */
    public ParameterListImpl(ParameterListDescriptor descriptor) {

	if (descriptor == null)
	    throw new
		IllegalArgumentException(JaiI18N.getString("Generic0"));

	this.pld = descriptor;

	int numParams = pld.getNumParameters();

	if (numParams > 0) {
	    // Fill in the parameter defaults.
	    Object[] paramDefaults = pld.getParamDefaults();

	    paramClasses = pld.getParamClasses();
	    paramIndices = new CaselessStringArrayTable(pld.getParamNames());
	    paramValues  = new Object[numParams];

	    for (int i = 0; i < numParams; i++) {
		paramValues[i] = paramDefaults[i];
	    }

	} else {
	    paramClasses = null;
	    paramIndices = null;
	    paramValues  = null;
	}
    }

    /**
     * Returns the associated <code>ParameterListDescriptor</code>.
     */
    public ParameterListDescriptor getParameterListDescriptor() {
	return pld;
    }
    
    /**
     * A private method (so that it may get inlined) which sets
     * the value of the specified parameter.
     *
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
     */
    private ParameterList setParameter0(String paramName, Object obj) {

	int index = paramIndices.indexOf(paramName);

	if ((obj != null) && !paramClasses[index].isInstance(obj))
	    throw new IllegalArgumentException(formatMsg(
		  JaiI18N.getString("ParameterListImpl0"),
		  new Object[]
		    { obj.getClass().getName(),
		      paramClasses[index].getName(), paramName }));

	if (!pld.isParameterValueValid(paramName, obj))
	    throw new IllegalArgumentException(paramName + ":" + 
                      JaiI18N.getString("ParameterListImpl1"));

	paramValues[index] = obj;

	return this;
    }

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
     * @param s a <code>short<code> value for the parameter. 
     * 
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the class type of parameter
     *		pointed to by the paramName is not a <code>Short</code>
     * @throws IllegalArgumentException if the parameter value is invalid.
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
     */
    public ParameterList setParameter(String paramName, double d) {
        return setParameter0(paramName, new Double(d));
    }

    /**
     * Sets a named parameter to an <code>Object</code> value.
     * Checks are made to verify that the parameter is of the right
     * <code>Class</code> type and that the value is valid.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param obj an <code>Object</code> value for the parameter.
     *
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     *          specified name.
     * @throws IllegalArgumentException if the parameter value is invalid.
     */
    public ParameterList setParameter(String paramName, Object obj) {
        return setParameter0(paramName, obj);
    }

    /**
     * A private method (so that it can be inlined) to get the
     * value associated with the specified parameter.
     *
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    private Object getObjectParameter0(String paramName) {

	Object obj = paramValues[paramIndices.indexOf(paramName)];

	if (obj == ParameterListDescriptor.NO_PARAMETER_DEFAULT)
	    throw new IllegalStateException(paramName + ":" +
			JaiI18N.getString("ParameterListImpl2"));

	return obj;
    }

    /**
     * Gets a named parameter as an <code>Object</code>. Parameters
     * belonging to a primitive type, such as int, will be returned as a
     * member of the corresponding wrapper class, such as
     * <code>Integer</code>
     *
     * @param paramName the name of the parameter to be returned.
     *
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public Object getObjectParameter(String paramName) {
	return getObjectParameter0(paramName);
    }

    /**
     * A convenience method to return a parameter as a <code>byte</code>.
     *
     * @param paramName the name of the parameter to be returned.
     *
     * @throws IllegalArgumentException if paramName is null.
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
     * A convenience method to return a parameter as a <code>boolean</code>.
     *
     * @param paramName the name of the parameter to be returned.
     *
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public boolean getBooleanParameter(String paramName) {
	return ((Boolean)getObjectParameter0(paramName)).booleanValue();
    }

    /**
     * A convenience method to return a parameter as a <code>char</code>.
     *
     * @param paramName the name of the parameter to be returned.
     *
     * @throws IllegalArgumentException if paramName is null.
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
     * A convenience method to return a parameter as a <code>short</code>.
     *
     * @param paramName the name of the parameter to be returned.
     *
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public short getShortParameter(String paramName) {
	return ((Short)getObjectParameter0(paramName)).shortValue();
    }

    /**
     * A convenience method to return a parameter as an <code>int</code>.
     *
     * @param paramName the name of the parameter to be returned.
     *
     * @throws IllegalArgumentException if paramName is null.
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
     * A convenience method to return a parameter as a <code>long</code>.
     *
     * @param paramName the name of the parameter to be returned.
     *
     * @throws IllegalArgumentException if paramName is null.
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
     * A convenience method to return a parameter as a <code>float</code>.
     *
     * @param paramName the name of the parameter to be returned.
     *
     * @throws IllegalArgumentException if paramName is null.
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
     * A convenience method to return a parameter as a <code>double</code>.
     *
     * @param paramName the name of the parameter to be returned.
     *
     * @throws IllegalArgumentException if paramName is null.
     * @throws IllegalArgumentException if there is no parameter with the
     * specified name.
     * @throws ClassCastException if the parameter is of a different type.
     * @throws IllegalStateException if the parameter value is still
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT
     */
    public double getDoubleParameter(String paramName) {
	return ((Double)getObjectParameter0(paramName)).doubleValue();
    }

    /**
     * Creates a <code>MessageFormat</code> object and set the
     * <code>Locale</code> to default.
     */
    private String formatMsg(String key, Object[] args) {
        MessageFormat mf = new MessageFormat(key);
        mf.setLocale(Locale.getDefault());

        return mf.format(args);
    }
}
