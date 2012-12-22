/*
 * $RCSfile: NegotiableCapability.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:51 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Hashtable;
import java.util.Vector;

import com.lightcrafts.mediax.jai.ParameterList;
import com.lightcrafts.mediax.jai.ParameterListImpl;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.ParameterListDescriptorImpl;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A <code>NegotiableCapability</code> represents the capabilities of an
 * object. These capabilities can be used to negotiate with the capabilities
 * of a similar object. Each <code>NegotiableCapability</code> is
 * characterized by the category it belongs to, as returned by the
 * <code>getCategory()</code> method, by the actual name of the capability
 * it represents, as returned by the <code>getCapabilityName()</code> method
 * and by a list of parameter name-value pairs that define the
 * <code>NegotiableCapability</code>. Every <code>NegotiableCapability</code>
 * object also holds references to a representation of the objects that
 * generated it. These can be accessed via the <code>getGenerators()</code>
 * method. The creator or generator of a <code>NegotiableCapability</code>
 * can supply any representation of itself while constructing the
 * <code>NegotiableCapability</code>. No interpretation is forced by this
 * class, that is left upto the generating class and to the class that
 * utilizes the <code>NegotiableCapability</code> to get the negotiated
 * results. The negotiation is performed by the
 * <code>negotiate()</code> method. Since this method returns a
 * <code>NegotiableCapability</code>, this method can be used repeatedly to
 * perform multiple negotiations. If the negotiation fails, null will be
 * returned from the <code>negotiate()</code> method. Every successful
 * negotiation will add the generator of the <code>NegotiableCapability</code>
 * negotiated with, to the set of generators of the resultant
 * <code>NegotiableCapability</code>. The generators are intended to help
 * the user of <code>NegotiableCapability</code> identify the object that
 * created the <code>NegotiableCapability</code> and therefore the object
 * that can be relied on to be able to handle the parameters agreed on during
 * negotiation. For example, if the negotiation is to be performed to choose
 * a compatible <code>TileEncoder</code>, <code>TileDecoder</code> pair
 * for data compression/decompression, the category would be "tileCodec",
 * the capabilityName would be a specific tile encoding format, say "jpeg"
 * and the generator for the <code>NegotiableCapability</code> could be the
 * <code>TileDecoderFactory</code>/<code>TileEncoderFactory</code> object
 * that generated that <code>NegotiableCapability</code>. After a
 * successful negotiation, the <code>NegotiableCapability</code> that is
 * the result of the negotiation will contain a
 * <code>TileEncoderFactory</code> and a <code>TileDecoderFactory</code>
 * object as the generators for that <code>NegotiableCapability</code>.
 * These two objects can then be retrieved using the
 * <code>getGenerators</code> method and used to do the encoding and
 * decoding and can be relied to be compatible, since the negotiation
 * was successful between their respective
 * <code>NegotiableCapability</code> objects.
 *
 * <p> The number, name, Class type and default values for the parameters in
 * this class is specified by the <code>ParameterListDescriptor</code>
 * returned from <code>getParameterListDescriptor</code> method. Each
 * parameter value in this class must be a class that implements the
 * <code>Negotiable</code> interface. It is for this reason that all of
 * the <code>ParameterList</code> set methods that take primitive data
 * types as arguments and all the <code>ParameterList</code> get methods
 * that return primitive data types are overridden in this class
 * to throw an IllegalArgumentException, as this class only accepts
 * <code>Negotiable</code>'s as parameter values in order to facilitate
 * negotiation on parameters. It may be noted that the implementation of
 * the version of <code>ParameterList.setParameter</code> that takes
 * an <code>Object</code> as the parameter value, in this class
 * throws an <code>IllegalArgumentException</code> if the supplied
 * <code>Object</code> to be set does not implement the
 * <code>Negotiable</code> interface. If no <code>Negotiable</code> value is
 * available as the value for a particular parameter, <code>null</code>
 * should be set as the value. A null value returned from the
 * <code>getNegotiatedValue(String)</code> method is however valid, since
 * the single value result of the negotiation can be null.
 * Similarly the <code>Object</code> returned from the
 * <code>ParameterList.getObjectParameter</code> implementation in this class
 * is always a class that implements the <code>Negotiable</code> interface,
 * and not a wrapper class of a primitive data type, as documented for this
 * method in <code>ParameterList</code>. The
 * <code>getParamValueRange(String parameterName)</code> and the
 * <code>getEnumeratedParameterValues(String parameterName)</code> methods
 * of the <code>ParameterListDescriptor</code> returned from
 * <code>getParameterListDescriptor</code> method of this class should be
 * implemented to return null, since these methods are not meaningful when
 * the parameter values are <code>Negotiable</code>.
 *
 * <p>In order for the negotiation to be successful, the category and the
 * capabilityName of the two <code>NegotiableCapability</code> objects must be
 * the same. In addition, negotiation on each of the parameters must be
 * successful. Since each parameter is represented as a
 * <code>Negotiable</code>, negotiation on it can be performed using the
 * <code>Negotiable.negotiate(Negotiable negotiable)</code> method. The
 * <code>NegotiableCapability</code> returned from the
 * <code>negotiate(NegotiableCapability capability)</code> method
 * contains the same category and capabilityName as that of the
 * <code>NegotiableCapability</code> objects being negotiated as well as
 * including the negotiated values for each parameter. If the negotiation fails
 * for any one parameter, the negotiation for the
 * <code>NegotiableCapability</code>s as a whole is said to fail (unless
 * preference <code>NegotiableCapability</code> objects are involved in
 * the negotiation, as described below) and a null is returned.
 *
 * <p> In order to get a single negotiated value from the set of valid
 * values represented as the <code>Negotiable</code> value for a parameter,
 * the <code>getNegotiatedValue(String parameterName)</code> method can be
 * called. If the negotiation was successful, an <code>Object</code> which
 * is the negotiated result will be returned, otherwise a
 * <code>null</code> (signifying that the negotiation failed) will be
 * returned.
 *
 * <p> <code>NegotiableCapability</code> objects can be classified as being
 * either preferences or non-preferences. A non-preference describes the
 * capabilities of an object completely by specifying <code>Negotiable</code>
 * values for each and every parameter defined in the
 * <code>ParameterListDescriptor</code> returned from
 * <code>getParameterListDescriptor</code> method. A non-preference is allowed
 * to not specify the value of a particular parameter, if a default value
 * for that parameter exists (i.e. the default value is not
 * <code>null</code>). When a non-preference is created, all parameter
 * values are initialized to their default values, and therefore if any
 * parameter value is left unset at the time of the negotiation, the
 * default value that was set at time of initialization will be used for
 * the negotiation. If the default value happened to be <code>null</code>,
 * the negotiation in this case would fail. Note that all references to
 * values in this paragraph, whether default or not, refered to the
 * objects implementing the <code>Negotiable</code> interface that are
 * the values set for a particular parameter name.
 *
 * A preference on the other hand specifies preferences for the selection of
 * a prefered set of (maybe even a single) parameter value from the set of
 * valid ones at negotiation time.
 * A preference is allowed to specify <code>Negotiable</code> parameter
 * values for a subset of parameters, if it so wishes. For those parameters
 * for whom the preference does not specify values, the preference is
 * indicating a don't-care attitude, and the result of the negotiation for
 * such a parameter will be the <code>Negotiable</code> value from the
 * non-preference object the preference is negotiating with. Note that the
 * default value is not substituted for a parameter whose value has not been
 * specified in a preference. A <code>NegotiableCapability</code> which is
 * a preference should return true from the <code>isPreference</code> method,
 * a non-preference object that defines values for all the parameters (or
 * relies on defaults) should return false from this method. As a rule, the
 * result of negotiation between one non-preference and another is a
 * non-preference, between a preference and a non-preference is a
 * non-preference and that between two preferences is a preference, if
 * the negotiation is successful. It may be noted that preferences are
 * not expected to specify their generators, since in general, preferences
 * don't come from objects that can support them. However if generators are
 * specified within a preference, they will be added to the set of generators
 * of the resultant <code>NegotiableCapability</code> in the event of a
 * successful negotiation.
 *
 * <p> Negotiation between a preference and a non-preference
 * <code>NegotiableCapability</code> results in a non-preference
 * <code>NegotiableCapability</code>. For each parameter, if a value is
 * specified (i.e the value is not <code>null</code>)
 * in both the preference and the non-preference, then if these values
 * have a common subset, the negotiation will succeed on this parameter,
 * if there is no commonality, then the negotiation will fail on this
 * parameter and thus also fail as a whole. If the preference doesn't
 * specify a value for a parameter (i.e the value is <code>null</code>),
 * then the value specified by the non-preference for that same parameter
 * is chosen as a result of the successful negotiation on that parameter.
 *
 * <p> Negotiation between two preference <code>NegotiableCapability</code>
 * objects results in a preference <code>NegotiableCapability</code>. For
 * each parameter, if a value is specified (i.e the value is not
 * <code>null</code>) in both the preference objects, the negotiation on
 * that parameter will have a value which is the portion that is common
 * to both. If there is no commonality, negotiation will fail on this
 * parameter (<code>null</code> will be returned) and thus also fail as
 * a whole. If the value for a particular parameter is specified in one
 * preference and not in the other, the negotiated value will be the one
 * specified. If for a particular parameter, no value is specified in
 * either preference, the negotiated value for that parameter will be
 * <code>null</code>, and the negotiation as a whole on the
 * <code>NegotiableCapability</code> will not fail.
 *
 * <p> When a preference <code>NegotiableCapability</code> is constructed,
 * the values of all the parameters defined in the
 * <code>ParameterListDescriptor</code> returned from
 * <code>getParameterListDescriptor</code> method, are initialized to
 * <code>null</code>. <code>null</code> within this class represents a
 * value that has not been specified. Such values are only allowed on
 * a preference <code>NegotiableCapability</code>. On the other hand when
 * a non-preference <code>NegotiableCapability</code> is
 * constructed, all the values are initialized to their default values.
 *
 * <p>All names are treated in a case-retentive and case-insensitive manner.
 *
 * @since JAI 1.1
 */
public class NegotiableCapability extends ParameterListImpl implements Serializable {

    private String category;
    private String capabilityName;
    private List generators;
    private boolean isPreference = false;

    /**
     * Creates a <code>NegotiableCapability</code> with the specified
     * <code>category</code> and <code>capabilityName</code>.
     *
     * @param category        The category this capability belongs to.
     * @param capabilityName  The name of this capability.
     * @param generators      A <code>List</code> containing representations
     *                        of the objects that generated this
     *                        <code>NegotiableCapability</code> or null, if
     *                        there are none.
     * @param descriptor      The descriptor that describes the parameters for
     *                        this class.
     * @param isPreference    Boolean specifying whether this class represents
     *                        a preference or a non-preference.
     *
     * @throws IllegalArgumentException if category is null.
     * @throws IllegalArgumentException if capabilityName is null.
     * @throws IllegalArgumentException if descriptor is null.
     * @throws IllegalArgumentException if any of the default values returned
     * from the supplied descriptor's getParamDefaults() method is
     * ParameterListDescriptor.NO_PARAMETER_DEFAULT. null should be used to
     * represent the absence of a default.
     * @throws IllegalArgumentException if any of the <code>Class</code>
     * types returned from the supplied descriptor's getParamClasses() method
     * does not implement <code>Negotiable</code>.
     */
    public NegotiableCapability(String category,
				String capabilityName,
				List generators,
				ParameterListDescriptor descriptor,
				boolean isPreference) {
	super(descriptor);

	if (category == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability0"));
	}

	if (capabilityName == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability1"));
	}

	ParameterListDescriptor desc = getParameterListDescriptor();
	int numParams = desc.getNumParameters();
	String names[] = desc.getParamNames();
	Class classes[] = desc.getParamClasses();
	Object defaults[] = desc.getParamDefaults();

	for (int i=0; i<numParams; i++) {

	    // Check that all paramClasses implement Negotiable.
	    if (Negotiable.class.isAssignableFrom(classes[i]) == false) {
		throw new IllegalArgumentException(
				   JaiI18N.getString("NegotiableCapability4"));
	    }

	    if (defaults[i] == ParameterListDescriptor.NO_PARAMETER_DEFAULT) {
		throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability5"));
	    }
	}

	this.category = category;
	this.capabilityName = capabilityName;
	this.generators = generators;
	this.isPreference = isPreference;
    }

    /**
     * Returns the category of this <code>NegotiableCapability</code>.
     */
    public String getCategory() {
	return category;
    }

    /**
     * Returns the name of this <code>NegotiableCapability</code>.
     */
    public String getCapabilityName() {
	return capabilityName;
    }

    /**
     * Returns the <code>List</code> containing representations of the
     * objects that generated this <code>NegotiableCapability</code>. This
     * method will return null, if there are no generators for this
     * <code>NegotiableCapability</code>.
     */
    public List getGenerators() {
	return generators;
    }

    /**
     * Set the specified <code>List</code> as the generators for this
     * <code>NegotiableCapability</code>. A generator is a representation
     * of the object that generated this <code>NegotiableCapability</code>.
     *
     * @param generators The <code>List</code> of generators.
     */
    public void setGenerators(List generators) {
	this.generators = generators;
    }

    /**
     * Returns true if this <code>NegotiableCapability</code> is a
     * preference, false otherwise.
     */
    public boolean isPreference() {
	return isPreference;
    }

    /**
     * Returns a single negotiated value from the <code>Negotiable</code> that
     * represents the set of valid values for the given parameter. This
     * method uses the <code>Negotiable.getNegotiatedValue</code> to get
     * the negotiated value for the <code>Negotiable</code> value of the
     * parameter specified by <code>parameterName</code>. If this
     * <code>NegotiableCapability</code> is a non-preference, then a valid
     * <code>Negotiable</code> must be present as the value of the specified
     * parameter, and a single value from that <code>Negotiable</code> will
     * be returned. If this <code>NegotiableCapability</code> is a preference
     * the specified parameter may have a <code>null</code> as its value.
     * In this case, this <code>null</code> will be returned as the
     * negotiated value.
     *
     * @param parameterName The name of parameter to return the negotiated
     *                      value for.
     * @throws IllegalArgumentException if the parameterName is not one of
     * those described by the associated <code>ParameterListDescriptor</code>.
     */
    public Object getNegotiatedValue(String parameterName) {
	Negotiable value = (Negotiable)getObjectParameter(parameterName);
	if (value == null)
	    return null;
	return value.getNegotiatedValue();
    }

    /**
     * Performs negotiation between this <code>NegotiableCapability</code>
     * and the given <code>NegotiableCapability</code>. Returns the common
     * subset supported by this <code>NegotiableCapability</code> and the given
     * <code>NegotiableCapability</code> if the negotiation is successful,
     * null otherwise.
     *
     * <p>In order for the negotiation to be successful, the category and the
     * capabilityName of the supplied <code>NegotiableCapability</code> object
     * must be the same as of this class. In addition, negotiation on each of
     * the parameters must be successful. Since each parameter is represented
     * as a <code>Negotiable</code>, negotiation on it can be performed using
     * the <code>Negotiable.negotiate()</code> method. The
     * <code>NegotiableCapability</code> returned contains the same category,
     * capabilityName as that of this class and also includes the negotiated
     * values for each parameter. If the negotiation fails for any one
     * parameter, the negotiation for the <code>NegotiableCapability</code>s
     * as a whole is said to fail and a null is returned. The result of
     * negotiation between one non-preference and another is a non-preference,
     * between a preference and a non-preference is a non-preference and
     * that between two preferences is a preference, if the negotiation is
     * successful.
     *
     * If this <code>NegotiableCapability</code> is a non-preference, i.e
     * the <code>isPreference()</code> method returns false, and the
     * supplied <code>NegotiableCapability</code> argument is also a
     * non-preference, then the negotiation will fail if the number and
     * <code>Class</code> of parameters in both the
     * <code>NegotiableCapability</code> objects is not the same.
     * If either one of the <code>NegotiableCapability</code> objects is
     * a preference and the other is a non-preference, the number of
     * parameters are not required to match. For those parameters whose names
     * are the same in both the <code>NegotiableCapability</code> objects,
     * the <code>Class</code> types have to match, otherwise the negotiation
     * will fail. Those parameters that exist in the non-preference
     * <code>NegotiableCapability</code> object but not in the preference
     * <code>NegotiableCapability</code> object do not take part in the
     * negotiation, but are directly set on the resultant
     * <code>NegotiableCapability</code> object if the negotiation
     * is successful on the common parameters. Those parameters that
     * exist in the preference <code>NegotiableCapability</code> object but
     * not in the non-preference <code>NegotiableCapability</code> object
     * are ignored, do not take part in the negotiation and are not
     * reflected in the resultant <code>NegotiableCapability</code> in the
     * event of a successful negotiation. If both the
     * <code>NegotiableCapability</code> objects are preferences, then
     * only the common parameters take part in the negotiation and the
     * ones that aren't present in both the <code>NegotiableCapability</code>s
     * are directly set on the resultant <code>NegotiableCapability</code>
     * object if the negotiation is successful on the common parameters.
     * For the common parameters, the <code>Class</code> types have to match,
     * otherwise the negotiation will fail. The check for the compatibility
     * of the <code>ParameterListDescriptor</code> of the supplied
     * <code>NegotiableCapability</code> with the current
     * <code>NegotiableCapability</code>'s <code>ParameterListDescriptor</code>
     * is done using the <code>areParameterListDescriptorsCompatible()</code>
     * method.

     * It may be noted that the <code>ParameterListDescriptor</code> of
     * the <code>NegotiableCapability</code> returned as a result of a
     * successful negotiation will implement the getParamDefaults() and
     * the getParamValueRange() methods in terms of the values returned
     * from the same methods on the <code>ParameterListDescriptor</code>
     * associated with this class, if the negotiation took place between
     * two preferences, or from the same methods on the
     * <code>ParameterListDescriptor</code> associated with the
     * non-preference otherwise.
     *
     * <p> If the supplied <code>NegotiableCapability</code> is null, then
     * the negotiation will fail and null will be returned.
     *
     * @param capability The <code>NegotiableCapability</code> to negotiate
     *                   with.
     * @returns the <code>NegotiableCapability</code> that is the result of a
     * successful negotiation, null if the negotiation failed.
     */
    public NegotiableCapability negotiate(NegotiableCapability capability) {

	if (capability == null) {
	    return null;
	}

	if (capability.getCategory().equalsIgnoreCase(category) == false ||
	    capability.getCapabilityName().equalsIgnoreCase(capabilityName)
	    == false) {
	    // Negotiation failed
	    return null;
	}

	// If the PLD's are not compatible for negotiation, fail the
	// negotiation
	if (areParameterListDescriptorsCompatible(capability) == false) {
	    return null;
	}

	int negStatus;
	if (capability.isPreference() == true) {
	    if (isPreference == true) {
		negStatus = 0;
	    } else {
		negStatus = 1;
	    }
	} else {
	    if (isPreference == true) {
		negStatus = 2;
	    } else {
		negStatus = 3;
	    }
	}

	ParameterListDescriptor pld = getParameterListDescriptor();
	ParameterListDescriptor otherPld =
	    capability.getParameterListDescriptor();
	String thisNames[] = pld.getParamNames();
        if (thisNames == null)
            thisNames = new String[0];
	String otherNames[] = otherPld.getParamNames();
        if (otherNames == null)
            otherNames = new String[0];
	Hashtable thisHash = hashNames(thisNames);
	Hashtable otherHash = hashNames(otherNames);

	Class thisClasses[] = pld.getParamClasses();
	Class otherClasses[] = otherPld.getParamClasses();
	Object thisDefaults[] = pld.getParamDefaults();
	Object otherDefaults[] = otherPld.getParamDefaults();

	NegotiableCapability result = null;
	String currParam;
	Negotiable thisValue, otherValue, resultValue;
	ArrayList resultGenerators = new ArrayList();
	if (generators != null)
	    resultGenerators.addAll(generators);
	if (capability.getGenerators() != null)
	    resultGenerators.addAll(capability.getGenerators());

	switch (negStatus) {

	case 0:

	    Vector commonNames = commonElements(thisHash, otherHash);
	    Hashtable commonHash = hashNames(commonNames);
	    Vector thisExtras = removeAll(thisHash, commonHash);
	    Vector otherExtras = removeAll(otherHash, commonHash);

	    int thisExtraLength = thisExtras.size();
	    int otherExtraLength = otherExtras.size();

	    // Create a new PLD which is the amalgamation of the two
	    // NC's PLD's
	    Vector resultParams = new Vector(commonNames);
	    resultParams.addAll(thisExtras);
	    resultParams.addAll(otherExtras);
	    int resultLength = resultParams.size();
	    String resultNames[] = new String[resultLength];
	    for (int i=0; i<resultLength; i++) {
		resultNames[i] = (String)resultParams.elementAt(i);
	    }

	    Class resultClasses[] = new Class[resultLength];
	    Object resultDefaults[] = new Object[resultLength];
	    Object resultValidValues[] = new Object[resultLength];
	    String name;
	    int count;
	    for (count=0; count<commonNames.size(); count++) {
		name = (String)commonNames.elementAt(count);
		resultClasses[count] = thisClasses[getIndex(thisHash, name)];
		resultDefaults[count] = thisDefaults[getIndex(thisHash, name)];
		resultValidValues[count] = pld.getParamValueRange(name);
	    }
	    for (int i=0; i<thisExtraLength; i++) {
		name = (String)thisExtras.elementAt(i);
		resultClasses[count+i] = thisClasses[getIndex(thisHash, name)];
		resultDefaults[count+i] = thisDefaults[getIndex(thisHash, 
								name)];
		resultValidValues[count+i] = pld.getParamValueRange(name);
	    }
	    count += thisExtraLength;
	    for (int i=0; i<otherExtraLength; i++) {
		name = (String)otherExtras.elementAt(i);
		resultClasses[i+count] = otherClasses[getIndex(otherHash,
							       name)];
		resultDefaults[i+count] = otherDefaults[getIndex(otherHash,
								 name)];
		resultValidValues[i+count] = otherPld.getParamValueRange(name);
	    }

	    ParameterListDescriptorImpl resultPLD =
		new ParameterListDescriptorImpl(null,
						resultNames,
						resultClasses,
						resultDefaults,
						resultValidValues);

	    // Both NC's are preferences
	    result = new NegotiableCapability(category,
					      capabilityName,
					      resultGenerators,
					      resultPLD,
					      true);

	    for (int i=0; i<commonNames.size(); i++) {
		currParam = (String)commonNames.elementAt(i);
		thisValue = (Negotiable)getObjectParameter(currParam);
		otherValue =
		    (Negotiable)capability.getObjectParameter(currParam);

		// If one of the values is null, select the other one, and
		// negotiation succeeds. Note that this also takes care
		// of the scenario when both are null, therefore the result
		// is null, and on a non-pref, this would have failed the
		// negotiation, but on a pref, it doesn't, so we just set
		// null (otherValue) as the result and allow negotiation to
		// succeed.
		if (thisValue == null) {
		    result.setParameter(currParam, otherValue);
		    continue;
		}

		if (otherValue == null) {
		    result.setParameter(currParam, thisValue);
		    continue;
		}

		// Following only gets executed if neither of the two is
		// a null, and therefore both have set values. If negotiation
		// fails, the negotiation as a whole is failed, otherwise
		// set the result on the resultant NC.
		resultValue = thisValue.negotiate(otherValue);
		if (resultValue == null) {
		    return null;
		}

		result.setParameter(currParam, resultValue);
	    }

	    // Copy the extra ones directly into the result
	    for (int i=0; i<thisExtraLength; i++) {
		currParam = (String)thisExtras.elementAt(i);
		result.setParameter(currParam,
				    (Negotiable)getObjectParameter(currParam));
	    }

	    for (int i=0; i<otherExtraLength; i++) {
		currParam = (String)otherExtras.elementAt(i);
		result.setParameter(currParam,
			 (Negotiable)capability.getObjectParameter(currParam));
	    }

	    break;

	case 1:

	    // The given capability is a pref, while this is a non-pref
	    commonNames = commonElements(thisHash, otherHash);
	    commonHash = hashNames(commonNames);
	    thisExtras = removeAll(thisHash, commonHash);

	    // Create a new PLD which is the amalgamation of the two
	    // NC's PLD's
	    resultParams = new Vector(commonNames);
	    resultParams.addAll(thisExtras);
	    resultLength = resultParams.size();
	    resultNames = new String[resultLength];
	    for (int i=0; i<resultLength; i++) {
		resultNames[i] = (String)resultParams.elementAt(i);
	    }

	    resultClasses = new Class[resultLength];
	    resultDefaults = new Object[resultLength];
	    resultValidValues = new Object[resultLength];

	    count = 0;
	    for (count=0; count<commonNames.size(); count++) {
		name = (String)commonNames.elementAt(count);
		resultClasses[count] = thisClasses[getIndex(thisHash, name)];
		resultDefaults[count] = thisDefaults[getIndex(thisHash, name)];
		resultValidValues[count] = pld.getParamValueRange(name);
	    }
	    for (int i=0; i<thisExtras.size(); i++) {
		name = (String)thisExtras.elementAt(i);
		resultClasses[i+count] = thisClasses[getIndex(thisHash, name)];
		resultDefaults[i+count] = thisDefaults[getIndex(thisHash,
								name)];
		resultValidValues[i+count] = pld.getParamValueRange(name);
	    }

	    resultPLD = new ParameterListDescriptorImpl(null,
							resultNames,
							resultClasses,
							resultDefaults,
							resultValidValues);

	    result = new NegotiableCapability(category,
					      capabilityName,
					      resultGenerators,
					      resultPLD,
					      false);

	    for (int i=0; i<commonNames.size(); i++) {
		currParam = (String)commonNames.elementAt(i);
		thisValue = (Negotiable)getObjectParameter(currParam);
		otherValue =
		    (Negotiable)capability.getObjectParameter(currParam);

		if (thisValue == null) {
		    // If non-pref doesn't have value, negotiation fails right
		    // away
		    return null;
		}

		if (otherValue == null) {
		    // If pref value is null, then non-pref's value wins.
		    // This needs to be done separately, since
		    // non-null.negotiate(null) returns null, which is *not*
		    // what we want.
		    result.setParameter(currParam, thisValue);
		} else {
		    // Do the negotiation.
		    resultValue = thisValue.negotiate(otherValue);

		    if (resultValue == null) {
			// Negotiation on one parameter failed, so negotiation
			// on the entire NC has also failed, return null to
			// signify this
			return null;
		    } else {
			result.setParameter(currParam, resultValue);
		    }
		}
	    }

	    // Copy the extra ones directly into the result
	    for (int i=0; i<thisExtras.size(); i++) {
		currParam = (String)thisExtras.elementAt(i);
		resultValue = (Negotiable)getObjectParameter(currParam);
		if (resultValue == null)
		    return null;
		result.setParameter(currParam, resultValue);
	    }

	    break;

	case 2:

	    // The given capability is a non-pref, while this is a pref
	    commonNames = commonElements(thisHash, otherHash);
	    commonHash = hashNames(commonNames);
	    otherExtras = removeAll(otherHash, commonHash);

	    // Create a new PLD which is the amalgamation of the two
	    // NC's PLD's
	    resultParams = new Vector(commonNames);
	    resultParams.addAll(otherExtras);
	    resultLength = resultParams.size();
	    resultNames = new String[resultLength];
	    for (int i=0; i<resultLength; i++) {
		resultNames[i] = (String)resultParams.elementAt(i);
	    }

	    resultClasses = new Class[resultLength];
	    resultDefaults = new Object[resultLength];
	    resultValidValues = new Object[resultLength];
	    count = 0;
	    for (count=0; count<commonNames.size(); count++) {
		name = (String)commonNames.elementAt(count);
		resultClasses[count] = thisClasses[getIndex(thisHash, name)];
		resultDefaults[count] = thisDefaults[getIndex(thisHash, name)];
		resultValidValues[count] = pld.getParamValueRange(name);
	    }

	    for (int i=0; i<otherExtras.size(); i++) {
		name = (String)otherExtras.elementAt(i);
		resultClasses[i+count] = otherClasses[getIndex(otherHash,
							       name)];
		resultDefaults[i+count] = otherDefaults[getIndex(otherHash,
								 name)];
		resultValidValues[i+count] = otherPld.getParamValueRange(name);
	    }

	    resultPLD = new ParameterListDescriptorImpl(null,
							resultNames,
							resultClasses,
							resultDefaults,
							resultValidValues);

	    result = new NegotiableCapability(category,
					      capabilityName,
					      resultGenerators,
					      resultPLD,
					      false);

	    for (int i=0; i<commonNames.size(); i++) {
		currParam = (String)commonNames.elementAt(i);
		thisValue = (Negotiable)getObjectParameter(currParam);
		otherValue =
		    (Negotiable)capability.getObjectParameter(currParam);

		// If non-pref doesn't have value, negotiation fails right
		// away
		if (otherValue == null) {
		    return null;
		}

		if (thisValue == null) {
		    // If pref value is null, then non-pref's value wins.
		    // This needs to be done separately, since
		    // non-null.negotiate(null) returns null, which is *not*
		    // what we want.
		    result.setParameter(currParam, otherValue);
		} else {
		    // Do the negotiation.
		    resultValue = otherValue.negotiate(thisValue);

		    if (resultValue == null) {
			// Negotiation on one parameter failed, so negotiation
			// on the entire NC has also failed, return null to
			// signify this
			return null;
		    } else {
			result.setParameter(currParam, resultValue);
		    }
		}
	    }

	    for (int i=0; i<otherExtras.size(); i++) {
		currParam = (String)otherExtras.elementAt(i);
		resultValue =
		    (Negotiable)capability.getObjectParameter(currParam);
		if (resultValue == null)
		    return null;
		result.setParameter(currParam, resultValue);
	    }

	    break;

	case 3:

	    // Both are non-prefs
	    result = new NegotiableCapability(category, capabilityName,
					      resultGenerators, pld, false);

	    for (int i=0; i<thisNames.length; i++) {
		currParam = thisNames[i];
		thisValue = (Negotiable)getObjectParameter(currParam);
		otherValue =
		    (Negotiable)capability.getObjectParameter(currParam);

		// failed since in nonpref-nonpref negotiation, both
		// Negotiables must have a non-null value
		if (thisValue == null ||
		    otherValue == null) {
		    return null;
		}

		resultValue = thisValue.negotiate(otherValue);

		if (resultValue == null) {
		    // Negotiation on one parameter failed, so negotiation
		    // on the entire NC has also failed, return null to
		    // signify this
		    return null;
		} else {
		    result.setParameter(currParam, resultValue);
		}
	    }

	    break;
	}

	return result;
    }

    /**
     * Returns true if the <code>ParameterListDescriptor</code> of the
     * supplied <code>NegotiableCapability</code> is compatible with the
     * <code>ParameterListDescriptor</code> of this class for negotiation
     * purposes. If both the <code>NegotiableCapability</code> objects are
     * non-preferences, both the number of parameters as well as the
     * <code>Class</code> type of the parameters has to match for this
     * method to return true. If either one or both of the
     * <code>NegotiableCapability</code> objects is a preference, then
     * the <code>Class</code> type of the same named parameters in both
     * the <code>NegotiableCapability</code> object's
     * <code>ParameterListDescriptor</code>s has to match for this
     * method to return true.
     *
     * @param other   The <code>NegotiableCapability</code> to check
     *                compatibility for negotiation purposes for.
     * @throws IllegalArgumentException if other is null.
     */
    public boolean areParameterListDescriptorsCompatible(NegotiableCapability
							 other) {

	if (other == null) {
	    throw new IllegalArgumentException(
				  JaiI18N.getString("NegotiableCapability6"));
	}

	ParameterListDescriptor thisDesc = getParameterListDescriptor();
	ParameterListDescriptor otherDesc = other.getParameterListDescriptor();

	String thisNames[] = thisDesc.getParamNames();
        if (thisNames == null)
            thisNames = new String[0];
	String otherNames[] = otherDesc.getParamNames();
        if (otherNames == null)
            otherNames = new String[0];
	Hashtable thisHash = hashNames(thisNames);
	Hashtable otherHash = hashNames(otherNames);

	if (isPreference == false && other.isPreference() == false) {

	    // The number of parameters must be the same.
	    if (thisDesc.getNumParameters() != otherDesc.getNumParameters())
		return false;

	    // The same names should be present in both in the same order.
	    if (containsAll(thisHash, otherHash) == false)
		return false;

	    Class thisParamClasses[] = thisDesc.getParamClasses();
	    Class otherParamClasses[] = otherDesc.getParamClasses();
	    for (int i=0; i<thisNames.length; i++) {
		if (thisParamClasses[i] !=
                    otherParamClasses[getIndex(otherHash, thisNames[i])])
		    return false;
	    }

	    return true;

	} else {

	    Vector commonNames = commonElements(thisHash, otherHash);

	    Class thisParamClasses[] = thisDesc.getParamClasses();
	    Class otherParamClasses[] = otherDesc.getParamClasses();
	    String currName;
	    for (int i=0; i<commonNames.size(); i++) {
		currName = (String)commonNames.elementAt(i);
		if (thisParamClasses[getIndex(thisHash, currName)] !=
		    otherParamClasses[getIndex(otherHash, currName)])
		    return false;
	    }

	    return true;
	}
    }

    // A case insensitive containsAll for Hashtables containing Strings
    private boolean containsAll(Hashtable thisHash, Hashtable otherHash) {

	CaselessStringKey thisNameKey;
	for (Enumeration i=thisHash.keys(); i.hasMoreElements(); ) {
	    thisNameKey = (CaselessStringKey)i.nextElement();
	    if (otherHash.containsKey(thisNameKey) == false)
		return false;
	}

	return true;
    }

    // Return only those names which exist in thisNames but not in otherNames
    private Vector removeAll(Hashtable thisHash, Hashtable otherHash) {

	Vector v = new Vector();
	CaselessStringKey thisNameKey;
	for (Enumeration i=thisHash.keys(); i.hasMoreElements(); ) {
	    thisNameKey = (CaselessStringKey)i.nextElement();
	    if (otherHash.containsKey(thisNameKey))
		continue;
	    else
		v.add(thisNameKey.toString());
	}

	return v;
    }

    private int getIndex(Hashtable h, String s) {
	return ((Integer)h.get(new CaselessStringKey(s))).intValue();
    }

    private Vector commonElements(Hashtable thisHash,
				  Hashtable otherHash) {

	Vector v = new Vector();
	CaselessStringKey thisNameKey;
	for (Enumeration i=thisHash.keys(); i.hasMoreElements(); ) {
	    thisNameKey = (CaselessStringKey)i.nextElement();
	    if (otherHash.containsKey(thisNameKey))
		v.add(thisNameKey.toString());
	}

	return v;
    }

    private Hashtable hashNames(String paramNames[]) {

	Hashtable h = new Hashtable();
        if (paramNames != null) {
	    for (int i=0; i<paramNames.length; i++) {
	        h.put(new CaselessStringKey(paramNames[i]), new Integer(i));
	    }
        }

        return h;
    }

    private Hashtable hashNames(Vector paramNames) {

	Hashtable h = new Hashtable();
        if (paramNames != null) {
	    for (int i=0; i<paramNames.size(); i++) {
	        h.put(new CaselessStringKey((String)(paramNames.elementAt(i))),
		      new Integer(i));
	    }
        }

        return h;
    }

    /***************** Overridden methods from ParameterList *****************/

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values set on this class
     * must be a <code>Negotiable</code>.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param b a <code>byte</code> value for the parameter.
     *
     * @throws IllegalArgumentException since the value being set is not a
     *         <code>Negotiable</code>.
     */
    public ParameterList setParameter(String paramName, byte b) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability2"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values set on this class
     * must be a <code>Negotiable</code>.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param b a <code>boolean</code> value for the parameter.
     *
     * @throws IllegalArgumentException since the value being set is not a
     *         <code>Negotiable</code>.
     */
    public ParameterList setParameter(String paramName, boolean b) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability2"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values set on this class
     * must be a <code>Negotiable</code>.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param c a <code>char</code> value for the parameter.
     *
     * @throws IllegalArgumentException since the value being set is not a
     *         <code>Negotiable</code>.
     */
    public ParameterList setParameter(String paramName, char c) {
 	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability2"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values set on this class
     * must be a <code>Negotiable</code>.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param s a short value for the parameter.
     *
     * @throws IllegalArgumentException since the value being set is not a
     *         <code>Negotiable</code>.
     */
    public ParameterList setParameter(String paramName, short s) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability2"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values set on this class
     * must be a <code>Negotiable</code>.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param i an <code>int</code> value for the parameter.
     *
     * @throws IllegalArgumentException since the value being set is not a
     *         <code>Negotiable</code>.
     */
    public ParameterList setParameter(String paramName, int i) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability2"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values set on this class
     * must be a <code>Negotiable</code>.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param l a <code>long</code> value for the parameter.
     *
     * @throws IllegalArgumentException since the value being set is not a
     *         <code>Negotiable</code>.
     */
    public ParameterList setParameter(String paramName, long l) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability2"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values set on this class
     * must be a <code>Negotiable</code>.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param f a <code>float</code> value for the parameter.
     *
     * @throws IllegalArgumentException since the value being set is not a
     *         <code>Negotiable</code>.
     */
    public ParameterList setParameter(String paramName, float f) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability2"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values set on this class
     * must be a <code>Negotiable</code>.
     *
     * @param paramName a <code>String</code> naming a parameter.
     * @param d a <code>double</code> value for the parameter.
     *
     * @throws IllegalArgumentException since the value being set is not a
     *         <code>Negotiable</code>.
     */
    public ParameterList setParameter(String paramName, double d) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability2"));
    }

    /**
     * Overrides the superclass method to ensure only a <code>Negotiable</code>
     * object can be added as the value of the parameter.
     *
     * @param paramName A <code>String</code> naming a parameter.
     * @param obj       An Object value for the parameter.
     *
     * @throws IllegalArgumentException if obj is not an instance of
     * <code>Negotiable</code>.
     */
    public ParameterList setParameter(String paramName, Object obj) {

	if (obj != null && !(obj instanceof Negotiable)) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability2"));
	}

	super.setParameter(paramName, obj);
	return this;
    }

    // getObjectParameter method doesn't need to be overridden since it
    // is implemented in ParameterListImpl and can return a Negotiable as
    // an Object

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values in this class
     * are <code>Negotiable</code> and therefore cannot be returned as
     * a primitive data type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException since a <code>Negotiable</code> value
     * cannot be returned as a primitive data type.
     */
    public byte getByteParameter(String paramName) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability3"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values in this class
     * are <code>Negotiable</code> and therefore cannot be returned as
     * a primitive data type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException since a <code>Negotiable</code> value
     * cannot be returned as a primitive data type.
     */
    public boolean getBooleanParameter(String paramName) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability3"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values in this class
     * are <code>Negotiable</code> and therefore cannot be returned as
     * a primitive data type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException since a <code>Negotiable</code> value
     * cannot be returned as a primitive data type.
     */
    public char getCharParameter(String paramName) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability3"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values in this class
     * are <code>Negotiable</code> and therefore cannot be returned as
     * a primitive data type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException since a <code>Negotiable</code> value
     * cannot be returned as a primitive data type.
     */
    public short getShortParameter(String paramName) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability3"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values in this class
     * are <code>Negotiable</code> and therefore cannot be returned as
     * a primitive data type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException since a <code>Negotiable</code> value
     * cannot be returned as a primitive data type.
     */
    public int getIntParameter(String paramName) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability3"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values in this class
     * are <code>Negotiable</code> and therefore cannot be returned as
     * a primitive data type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException since a <code>Negotiable</code> value
     * cannot be returned as a primitive data type.
     */
    public long getLongParameter(String paramName) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability3"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values in this class
     * are <code>Negotiable</code> and therefore cannot be returned as
     * a primitive data type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException since a <code>Negotiable</code> value
     * cannot be returned as a primitive data type.
     */
    public float getFloatParameter(String paramName) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability3"));
    }

    /**
     * Overrides the method in <code>ParameterListImpl</code> to throw
     * an IllegalArgumentException since parameter values in this class
     * are <code>Negotiable</code> and therefore cannot be returned as
     * a primitive data type.
     *
     * @param paramName the name of the parameter to be returned.
     * @throws IllegalArgumentException since a <code>Negotiable</code> value
     * cannot be returned as a primitive data type.
     */
    public double getDoubleParameter(String paramName) {
	throw new IllegalArgumentException(
				JaiI18N.getString("NegotiableCapability3"));
    }
}
