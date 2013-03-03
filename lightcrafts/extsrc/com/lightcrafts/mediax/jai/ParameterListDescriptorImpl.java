/*
 * $RCSfile: ParameterListDescriptorImpl.java,v $
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import com.lightcrafts.mediax.jai.util.Range;

/**
 * A concrete implementation of the <code>ParameterListDescriptor</code>
 * interface.
 *
 * @see ParameterListDescriptor
 *
 * @since JAI 1.1
 */
public class ParameterListDescriptorImpl
	    implements ParameterListDescriptor, java.io.Serializable {

    /** The number of parameters in the list described by this parameter. */
    private int numParams;

    /** The names of each parameter. */
    private String[] paramNames;

    /** The <code>Class</code> type of each parameter. There is one-to-one
      * mapping between this and <code>paramNames</code>.
      */
    private Class[] paramClasses;

    /**
      * The default values for of each parameter. There is one-to-one
      * mapping between this and <code>paramNames</code>. If there is
      * no default value for a given parameter, it is initialized with
      * <code>ParameterListDescriptor.NO_PARAMETER_DEFAULT</code>
      */
    private Object[] paramDefaults;

    /**
      * Defines the valid parameter values for each parameter.
      */
    private Object[] validParamValues;

    /**
      * A <code>CaselessStringArrayTable</code> mapping the parameter
      * names to their indices in the above arrays in a case-insensitive
      * manner.
      */
    private CaselessStringArrayTable paramIndices;

    /** The <code>Object</code> to reflect upon for enumerated parameters. */
    private Object  descriptor;

    /**
      * Indicates if the <code>validParamValues</code> field has been
      * initialized.
      */
    private boolean validParamsInitialized = false;

    /**
     * Uses reflection to examine "descriptor" for <code>public</code>,
     * <code>static</code> <code>final</code> <code>Field</code>s
     * that are instances of "paramClass".
     *
     * @param descriptor the object to be reflected upon.
     * @param paramClass the parameter class
     * 
     * @return a <code>Set</code> of enumerated values.
     *
     * @throws IllegalArgumentException if descriptor is <code>null</code>
     *		or paramClass is <code>null</code>
     * @throws IllegalArgumentException if "paramClass" is not an instance of
     *      <code>EnumeratedParameter</code>
     */
    public static Set getEnumeratedValues(
				Object descriptor, Class paramClass) {

        if ((descriptor == null) || (paramClass == null))
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	// If not an enumerated parameter, return null
	if (!EnumeratedParameter.class.isAssignableFrom(paramClass))
	    throw new IllegalArgumentException(
		    JaiI18N.formatMsg("ParameterListDescriptorImpl10",
		    new Object[] { paramClass.getName() }));

	Field[] fields = descriptor.getClass().getDeclaredFields();

	if (fields == null)
	    return null;

	// Look for all parameters which are instance of EnumeratedParameter.
	int numFields = fields.length;
	Set valueSet = null;

	// Look for all fields which are static, final
	// instances of the class of this parameter.
	for(int j = 0; j < numFields; j++) {
	    Field field = fields[j];
	    int modifiers = field.getModifiers();
	    if(Modifier.isPublic(modifiers) &&
	       Modifier.isStatic(modifiers) &&
	       Modifier.isFinal(modifiers)) {
		Object fieldValue = null;
		try {
		    fieldValue = field.get(null);
		} catch(Exception e) {
		    // Ignore exception
		}
		if(paramClass.isInstance(fieldValue)) {
		    if(valueSet == null) {
			valueSet = new HashSet();
		    }

		    if(valueSet.contains(fieldValue)) {
			// This error is a coding error
			// which should be caught by the
			// developer the first time the
			// bogus descriptor is loaded.
			throw new UnsupportedOperationException(
			    JaiI18N.getString("ParameterListDescriptorImpl0"));
		    }
		    // Save parameter value in Set.
		    valueSet.add(fieldValue);
		}
	    }
	}

	return valueSet;
    }

    /**
     * A wrapper method to get the valid parameter values for
     * the specified parameter index. This makes sure that the
     * field has been initialized if it wasnt done before.
     */
    private Object getValidParamValue(int index) {

	if (validParamsInitialized)
	    return validParamValues[index];

	synchronized (this) {
	    if (validParamValues == null) {
		validParamValues = new Object[numParams];
	    }

            Class enumeratedClass = EnumeratedParameter.class;

	    for (int i = 0; i < numParams; i++) {
		if (validParamValues[i] != null)
		    continue;

		if (enumeratedClass.isAssignableFrom(paramClasses[i])) {
		    validParamValues[i] =
			getEnumeratedValues(descriptor, paramClasses[i]);
		}
	    }
	}

	validParamsInitialized = true;

	return validParamValues[index];
    }

    /**
     * Constructor for descriptors that dont have any parameters.
     */
    public ParameterListDescriptorImpl() {
	this.numParams        = 0;
	this.paramNames       = null;
	this.paramClasses     = null;
	this.paramDefaults    = null;
	this.paramIndices     = new CaselessStringArrayTable();
	this.validParamValues = null;
    }

    /**
     * Constructor.
     *
     * @param descriptor the object to be reflected upon for enumerated values
     * @param paramNames the names of each parameter. can be <code>null</code>
     *		if there are no parameters.
     * @param paramClasses the <code>Class</code> type of each parameter.
     *		can be <code>null</code> if there are no parameters.
     * @param paramDefaults the default values for each parameter. can be
     *		<code>null</code> if there are no parameters or if
     *		there are no default values, in which case the parameter
     *		defaults are assumed to be <code>
     *		ParameterListDescriptor.NO_PARAMETER_DEFAULT</code>
     * @param validParamValues defines the valid values for each parameter.
     *
     *    <p>Each element of this array can be <code>null</code> (if the parameter 
     *    can take on any value of its class or if it is an enumerated parameter
     *	  whose values are to be auto-detected - see <code>getEnumeratedValues
     *	  </code>), or a <code>Set</code> (for user specified enumerated
     *	  values) or a <code>Range</code> (for parameters that are
     *	  <code>Comparable</code>.)
     *
     *    <p>The valid set of values for an object which is neither an
     *    <code>EnumeratedParameter</code> nor <code>Comparable</code>
     *	  should just be the set of all possible instances of the associated
     *	  class, i.e., the parameter has to be an instance of the specified
     *	  class.
     *
     *    <p>If this array itself is <code>null</code> then it is treated
     *    as an array full of <code>null</code>s as described above.
     *
     * @throws IllegalArgumentException if <code>paramNames</code> is non-null
     *		and the number of <code>paramClasses</code> or a non-null
     *		<code>paramDefaults</code> does not match the length of
     *		<code>paramNames</code>
     * @throws IllegalArgumentException if <code>null</code> is passed in for
     *		<code>validParamValues</code> for a parameter whose
     *		class is of <code>EnumeratedParameter</code> type.
     */
    public ParameterListDescriptorImpl(Object descriptor,
				       String[] paramNames,
				       Class [] paramClasses,
				       Object[] paramDefaults,
				       Object[] validParamValues) {

	int numParams = (paramNames == null) ? 0 : paramNames.length;

	if ((paramDefaults != null) && (paramDefaults.length != numParams))
	   throw new IllegalArgumentException("paramDefaults" +
		  JaiI18N.getString("ParameterListDescriptorImpl1"));

	if ((validParamValues != null) && (validParamValues.length != numParams))
	   throw new IllegalArgumentException("validParamValues" +
		  JaiI18N.getString("ParameterListDescriptorImpl2"));

	this.descriptor = descriptor;

	if (numParams == 0) {

	    if ((paramClasses != null) && (paramClasses.length != 0))
	       throw new IllegalArgumentException("paramClasses" +
                      JaiI18N.getString("ParameterListDescriptorImpl3"));

	    this.numParams        = 0;
	    this.paramNames       = null;
	    this.paramClasses     = null;
	    this.paramDefaults    = null;
	    this.paramIndices     = new CaselessStringArrayTable();
	    this.validParamValues = null;

	} else {

	    if ((paramClasses == null) || (paramClasses.length != numParams))
	       throw new IllegalArgumentException("paramClasses" +
                      JaiI18N.getString("ParameterListDescriptorImpl3"));

	    this.numParams	  = numParams;
	    this.paramNames       = paramNames;
	    this.paramClasses     = paramClasses;
	    this.validParamValues = validParamValues;

	    //
	    // If the defaults are null, fill in NO_PARAMETER_DEFAULT
	    // Else, make sure they belong to the right class.
	    //
	    if (paramDefaults == null) {
		this.paramDefaults = new Object[numParams];

		for (int i = 0; i < numParams; i++)
		    this.paramDefaults[i] =
			    ParameterListDescriptor.NO_PARAMETER_DEFAULT;
	    } else {

		this.paramDefaults = paramDefaults;

		for (int i = 0; i < numParams; i++) {
		    if ((paramDefaults[i] == null) ||
			(paramDefaults[i] ==
			    ParameterListDescriptor.NO_PARAMETER_DEFAULT))
			continue;

		    if (!paramClasses[i].isInstance(paramDefaults[i])) {
			throw new IllegalArgumentException(
			      JaiI18N.formatMsg("ParameterListDescriptorImpl4",
			      new Object[]
				{ paramDefaults[i].getClass().getName(),
				  paramClasses [i].getName(),
				  paramNames[i] }));
		    }
		}
	    }

	    //
	    // Make sure that validParamValues belongs to the right class.
	    //
	    if (validParamValues != null) {

		Class enumeratedClass = EnumeratedParameter.class;

		for (int i = 0; i < numParams; i++) {

		    if (validParamValues[i] == null)
			continue;

		    if (enumeratedClass.isAssignableFrom(paramClasses[i])) {

			// If paramClass[i] is an enumerated parameter, then
			// the validParamValues[i] has to be a Set
			if (!(validParamValues[i] instanceof Set))
			    throw new IllegalArgumentException(
			      JaiI18N.formatMsg("ParameterListDescriptorImpl5",
			      new Object[] { paramNames[i] }));

		    } else if (validParamValues[i] instanceof Range) {

			Range range = (Range)validParamValues[i];

			// If the validParamValues[i] is a Range, then
			// the Range's class must match with paramClass[i]
			if (!paramClasses[i].isAssignableFrom(
					    range.getElementClass()))
			    throw new IllegalArgumentException(
			      JaiI18N.formatMsg("ParameterListDescriptorImpl6",
			      new Object[]
				{ range.getElementClass().getName(),
				  paramClasses[i].getName(),
				  paramNames[i] }));

		    } else {

			// Otherwise, the validParamValues[i] has to be
			// an instance of the paramClasses[i]
			if (!paramClasses[i].isInstance(validParamValues[i]))
			    throw new IllegalArgumentException(
			      JaiI18N.formatMsg("ParameterListDescriptorImpl7",
			      new Object[]
				{ validParamValues[i].getClass().getName(),
				  paramClasses[i].getName(),
				  paramNames[i] }));
		    }
		}
	    }

	    paramIndices = new CaselessStringArrayTable(paramNames);
	}
    }

    /**
     * Returns the total number of parameters.
     */
    public int getNumParameters() {
	return numParams;
    }

    /**
     * Returns an array of <code>Class</code>es that describe the types
     * of parameters.  If there are no parameters, this method returns
     * <code>null</code>.
     */
    public Class[] getParamClasses() {
	return paramClasses;
    }

    /**
     * Returns an array of <code>String</code>s that are the 
     * names of the parameters associated with this descriptor. If there
     * are no parameters, this method returns <code>null</code>.
     */
    public String[] getParamNames() {
	return paramNames;
    }

    /**
     * Returns an array of <code>Object</code>s that define the default
     * values of the parameters.  Default values may be <code>null</code>.
     * The <code>NO_PARAMETER_DEFAULT</code> static <code>Object</code>
     * indicates that a parameter has no default value.  If there are no
     * parameters, this method returns <code>null</code>.
     */
    public Object[] getParamDefaults() {
	return paramDefaults;
    }

    /**
     * Returns the default value of a specified parameter.  The default
     * value may be <code>null</code>.  If a parameter has no default
     * value, this method returns <code>NO_PARAMETER_DEFAULT</code>.
     *
     * @param parameterName  The name of the parameter whose default
     *        value is queried.
     *
     * @throws IllegalArgumentException if <code>parameterName</code> is null
     *		or if the parameter does not exist.
     */
    public Object getParamDefaultValue(String parameterName) {
	return paramDefaults[paramIndices.indexOf(parameterName)];
    }

    /**
     * Returns the <code>Range</code> that represents the range of valid
     * values for the specified parameter. Returns <code>null</code> if
     * the parameter can take on any value or if the valid values are
     * not representable as a Range.
     *
     * @param parameterName The name of the parameter whose valid range
     *		of values is to be determined.
     *
     * @throws IllegalArgumentException if <code>parameterName</code> is null
     *		or if the parameter does not exist.
     */
    public Range getParamValueRange(String parameterName) {

	Object values = getValidParamValue(paramIndices.indexOf(parameterName));

	if ((values == null) || (values instanceof Range))
	    return (Range)values;

	return null;
    }

    /**
     * Return an array of the names of all parameters the type of which is
     * <code>EnumeratedParameter</code>.
     *
     * @return The requested array of names or <code>null</code> if there
     * are no parameters with <code>EnumeratedParameter</code> type.
     */
    public String[] getEnumeratedParameterNames() {

	Vector v = new Vector();

	for (int i = 0; i < numParams; i++) {
	    if (EnumeratedParameter.class.isAssignableFrom(paramClasses[i]))
		v.add(paramNames[i]);
        }

	if (v.size() <= 0)
	    return null;

        return (String[])v.toArray(new String[0]);
    }

    /**
     * Return an array of <code>EnumeratedParameter</code> objects
     * corresponding to the parameter with the specified name.
     *
     * @param parameterName The name of the parameter for which the
     * <code>EnumeratedParameter</code> array is to be returned.
     *
     * @throws IllegalArgumentException if <code>parameterName</code> is null
     *		or if the parameter does not exist.
     * @throws UnsupportedOperationException if there are no enumerated
     * parameters associated with the descriptor.
     * @throws IllegalArgumentException if <code>parameterName</code> is
     * a parameter the class of which is not a subclass of
     * <code>EnumeratedParameter</code>.
     *
     * @return An array of <code>EnumeratedParameter</code> objects
     * representing the range of values for the named parameter.
     */
    public EnumeratedParameter[] getEnumeratedParameterValues(String parameterName) {

	int i = paramIndices.indexOf(parameterName);

	if (!EnumeratedParameter.class.isAssignableFrom(paramClasses[i]))
	    throw new IllegalArgumentException(parameterName + ":" +
		      JaiI18N.getString("ParameterListDescriptorImpl8"));

	Set enumSet = (Set)getValidParamValue(i);

	if (enumSet == null)
	    return null;

	return (EnumeratedParameter[])
		    enumSet.toArray(new EnumeratedParameter[0]);
    }

    /**
     * Checks to see if the specified parameter can take on the specified
     * value.
     *
     * @param parameterName The name of the parameter for which the
     * validity check is to be performed.
     *
     * @throws IllegalArgumentException if <code>parameterName</code> is null
     *		or if the parameter does not exist.
     * @throws IllegalArgumentException  if the class of the object "value"
     *          is not an instance of the class type of parameter
     *          pointed to by the parameterName
     *
     * @return true, if it is valid to pass this value in for this
     *          parameter, false otherwise.
     */
    public boolean isParameterValueValid(String parameterName, Object value) {
	int index = paramIndices.indexOf(parameterName);

	if ((value == null) && (paramDefaults[index] == null)) {
	    return true;
	}

	// Make sure the object belongs to the right class
	if ((value != null) && !paramClasses[index].isInstance(value)) {
	    throw new IllegalArgumentException(
		      JaiI18N.formatMsg("ParameterListDescriptorImpl9",
		      new Object[]
			{ value.getClass().getName(),
			  paramClasses[index].getName(), parameterName }));
	}

	Object validValues = getValidParamValue(index);

	// If validValues is null then any value is acceptable.
	if (validValues == null)
	    return true;
	
	// If validValues is a Range, make sure "value" lies within it.
	if (validValues instanceof Range)
	    return ((Range)validValues).contains((Comparable)value);

	// If validValues is a Set, then make sure that "value" is contained
	// in the Set.
	if (validValues instanceof Set)
	    return ((Set)validValues).contains(value);

	// Otherwise the value must be the same as validValues
	return value == validValues;
    }
}
