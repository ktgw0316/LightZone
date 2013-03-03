/*
 * $RCSfile: OperationDescriptorImpl.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:12 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.CaselessStringArrayTable;
import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;
import com.lightcrafts.mediax.jai.util.Range;

/**
 * This class provides an abstract implementation of the
 * <code>OperationDescriptor</code> interface that is suitable for
 * subclassing.
 *
 * @see OperationDescriptor
 * @see RegistryElementDescriptor
 *
 */
public abstract class OperationDescriptorImpl
	    implements OperationDescriptor, java.io.Serializable {

    private boolean deprecated = false;

    /**
     * The resource tags and their corresponding data, stored as an
     * two-dimensional <code>String</code> array.
     *
     * @since JAI 1.1
     */
    protected final String[][] resources;

    /**
     * An array of operation modes supported by this operator.
     * Must be a non-empty subset of "rendered", "renderable",
     * "collection" and "renderableCollection" or other image
     * operator modes to be defined later.
     *
     * @since JAI 1.1
     */
    protected final String[] supportedModes;

    /**
      * A <code>CaselessStringArrayTable</code> mapping the mode names to
      * their indices in the above arrays in a case-insensitive manner.
      */
    private CaselessStringArrayTable modeIndices;

    /**
     * An array of <code>String</code>s that are the names of the
     * sources of this operation.  The names must be listed in the
     * order corresponding to the source <code>Class</code>es.
     *
     * @since JAI 1.1
     */
    protected final String[] sourceNames;

    /**
     * A 2D array of source classes for each source for each mode.
     * sourceClasses[m][i] specifies the <code>Class</code> for
     * supportedModes[m] and sourceNames[i].
     */
    private Class[][] sourceClasses;

    /**
      * A <code>CaselessStringArrayTable</code> mapping the source names to
      * their indices in the above arrays in a case-insensitive manner.
      */
    private CaselessStringArrayTable sourceIndices;

    /**
     * An array of <code>ParameterListDescriptor</code> for each mode.
     */
    private ParameterListDescriptor[] paramListDescriptors;

    /**
     * The array of parameter names. We need this because
     * ParameterListDescriptor works only with parameter names and not
     * parameter indices. But many of the deprecated methods and
     * the validation of ParameterBlock has to happen through
     * parameter indices.
     */
    String[] paramNames;

    /** The global name of this operation. */
    private String name = null;

    // Constructors...

    /**
     * Do some of the initialization common to all constructors.
     */
    private String[] checkSources(String[][] resources,
				  String[]   supportedModes,
				  String[]   sourceNames,
				  Class [][] sourceClasses) {

	if ((resources == null) || (resources.length == 0))
            throw new IllegalArgumentException(
		    "resources: " + JaiI18N.getString("Generic2"));

	if ((supportedModes == null) || (supportedModes.length == 0))
            throw new IllegalArgumentException(
		    "supportedModes: " + JaiI18N.getString("Generic2"));

	// Validate source related arguments.

	int numModes = supportedModes.length;

	if (sourceClasses != null) {

	   if (sourceClasses.length != numModes)
	      throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationDescriptorImpl0",
		    new Object[] {"sourceClasses", new Integer(numModes)}));

	    int numSources = (sourceClasses[0] == null) ? 0 :
			      sourceClasses[0].length;

	    if (sourceNames == null) {
		sourceNames = getDefaultSourceNames(numSources);

	    } else if (sourceNames.length != numSources) {

	      throw new IllegalArgumentException(
			    JaiI18N.formatMsg("OperationDescriptorImpl1",
			    new Object[] 
				{ new Integer(sourceNames.length),
				  new Integer(numSources)
				}));
	    }

	    for (int i = 0; i < sourceClasses.length; i++) {
		int ns = (sourceClasses[i] == null) ?
				    0 : sourceClasses[i].length;

		if (numSources != ns) {
		    throw new IllegalArgumentException(
			JaiI18N.formatMsg("OperationDescriptorImpl2",
			new Object[]
			    { new Integer(ns),
			      new Integer(numSources),
			      supportedModes[i]
			    }));
		}
	    }

	} else if ((sourceNames != null) && (sourceNames.length != 0)) {
            throw new IllegalArgumentException(
			JaiI18N.formatMsg("OperationDescriptorImpl1",
			new Object[]
			    { new Integer(sourceNames.length),
			      new Integer(0)
			    }));
	}

	return sourceNames;
    }
    
    /**
     * Constructor. Note that <code>sourceClasses[m][i]</code>
     * corresponds to the mode <code>supportedModes[m]</code>
     * and the source <code>sourceNames[i]</code>. Similarly
     * <code>paramClasses[m][i]</code> corresponds to the
     * mode <code>supportedModes[m]</code> and the parameter
     * <code> paramNames[i]</code>. The same holds true for
     * <code>paramDefaults</code> and <code>validParamValues</code>
     *
     * @param resources  The resource tags and their corresponding data.
     * @param supportedModes  The modes that this operator supports.
     *        maybe one or more of "rendered", "renderable", "collection",
     *        and "renderableCollection" (or other image operation related
     *	      modes that maybe defined later). Must support at least one mode.
     * @param sourceNames  The source names.  It may be <code>null</code>
     *        if this operation has no sources or if the default source
     *        naming convention ("source0", "source1", etc.) is to be used.
     * @param sourceClasses  The source types required by this operation
     *        for each of the above supported modes. can be null
     *        if this operation has no sources. The number of
     *	      sources for each mode must be the same.
     * @param paramNames  The localized parameter names.  It may be
     *        <code>null</code> if this operation has no parameters.
     * @param paramClasses  The parameter types required by this operation.
     *        for each mode. It may be <code>null</code> if this operation
     *        has no parameters. The number of parameters for each mode
     *	      must be the same.
     * @param paramDefaults  The parameter default values for each parameter
     *         for each mode. It may be <code>null</code> if this
     *         operation has no parameters, or none of the parameters has
     *         a default value for any mode. The parameter defaults for an
     *         individual mode may be null, if there are no defaults for
     *         that mode.
     * @param validParamValues defines the valid values for each parameter
     *		for each mode.  this can be <code>null</code> if the operation
     *		has no parameters. Otherwise each element can be filled in as
     *		defined in {@link
     *		ParameterListDescriptorImpl#ParameterListDescriptorImpl(
     *		    Object, String[], Class[], Object[], Object[])}
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if supportedModes is <code>null</code>
     * @throws IllegalArgumentException if the number of <code>sourceClasses</code>
     *	       for each mode is not the same or is not equal to the number
     *	       of sourceNames (if non-null).
     * @throws IllegalArgumentException if this operation has parameters and
     *         <code>paramClasses</code> or <code>paramNames</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>sourceNames</code>
     *         is non-<code>null</code> and its length does not equal
     *         the number of sources of this operation.
     * @throws IllegalArgumentException if this operation has parameters
     *         and <code>paramClasses</code>, <code>paramNames</code>,
     *         and <code>paramDefaults</code> (if all are not
     *         <code>null</code>) do not all have the same number of elements.
     *
     * @since JAI 1.1
     */
    public OperationDescriptorImpl(String[][] resources,
                                   String[]   supportedModes,
                                   String[]   sourceNames,
                                   Class [][] sourceClasses,
                                   String[]   paramNames,
                                   Class [][] paramClasses,
                                   Object[][] paramDefaults,
                                   Object[][] validParamValues) {

	sourceNames = checkSources(resources, supportedModes,
					sourceNames, sourceClasses);

	this.resources	    = resources;
	this.supportedModes = supportedModes;
	this.sourceNames    = sourceNames;
	this.sourceClasses  = sourceClasses;
	this.paramNames     = paramNames;

	this.modeIndices   = new CaselessStringArrayTable(supportedModes);
	this.sourceIndices = new CaselessStringArrayTable(sourceNames);

	// Validate parameter related arguments.

	int numParams = (paramNames == null) ? 0 : paramNames.length;
	int numModes = supportedModes.length;

	if (numParams == 0) {
	    if ((paramClasses != null) && (paramClasses.length != numModes))
	       throw new IllegalArgumentException(
                      JaiI18N.formatMsg("OperationDescriptorImpl0",
		      new Object[] {"paramClasses", new Integer(numModes)}));

	} else {

	    if ((paramClasses == null) || (paramClasses.length != numModes))
	       throw new IllegalArgumentException(
                      JaiI18N.formatMsg("OperationDescriptorImpl0",
		      new Object[] {"paramClasses", new Integer(numModes)}));
	}

	if ((paramDefaults != null) && (paramDefaults.length != numModes))
	   throw new IllegalArgumentException(
		  JaiI18N.formatMsg("OperationDescriptorImpl0",
		  new Object[] {"paramDefaults", new Integer(numModes)}));

	if ((validParamValues != null) && (validParamValues.length != numModes))
	   throw new IllegalArgumentException(
		  JaiI18N.formatMsg("OperationDescriptorImpl0",
		  new Object[] {"validParamValues", new Integer(numModes)}));

	// Create the ParameterListDescriptor-s for each mode.

	paramListDescriptors =
		new ParameterListDescriptor[numModes];

	for (int i = 0; i < numModes; i++) {
	    paramListDescriptors[i] =
		new ParameterListDescriptorImpl(this,
			paramNames, paramClasses[i],
			paramDefaults == null ? null : paramDefaults[i],
			validParamValues == null ? null : validParamValues[i]);
	}
    }

    /**
     * Constructor. This assumes that all modes have the same
     * set of parameter classes, defaults and valid values. Note
     * that <code>sourceClasses[m][i]</code> corresponds to
     * the mode <code>supportedModes[m]</code> and the source
     * <code>sourceNames[i]</code>.
     *
     * @param resources  The resource tags and their corresponding data.
     * @param supportedModes  The modes that this operator supports.
     *        maybe one or more of "rendered", "renderable", "collection",
     *        and "renderableCollection". Must support at least one mode.
     * @param sourceNames  The source names.  It may be <code>null</code>
     *        if this operation has no sources or if the default source
     *        naming convention ("source0", "source1", etc.) is to be used.
     * @param sourceClasses  The source types required by this operation
     *        for each of the above supported modes. can be null
     *        if this operation has no sources. The number of
     *	      sources for each mode must be the same.
     * @param paramNames  The localized parameter names.  It may be
     *        <code>null</code> if this operation has no parameters.
     * @param paramClasses  The parameter types required by this operation.
     *        It may be <code>null</code> if this operation has no parameters.
     * @param paramDefaults  The parameter default values for each parameter
     *         It may be <code>null</code> if this operation has no
     *	       parameters, or none of the parameters has a default value.
     * @param validParamValues defines the valid values for each parameter
     *		for all modes.  this can be <code>null</code> if the operation
     *		has no parameters. Otherwise it can be filled in as
     *		defined in {@link
     *		ParameterListDescriptorImpl#ParameterListDescriptorImpl(
     *		    Object, String[], Class[], Object[], Object[])}
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if supportedModes is <code>null</code>
     * @throws IllegalArgumentException if the number of <code>sourceClasses</code>
     *	       for each mode is not the same or is not equal to the number
     *	       of sourceNames (if non-null).
     * @throws IllegalArgumentException if this operation has parameters and
     *         <code>paramClasses</code> or <code>paramNames</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if <code>sourceNames</code>
     *         is non-<code>null</code> and its length does not equal
     *         the number of sources of this operation.
     * @throws IllegalArgumentException if this operation has parameters
     *         and <code>paramClasses</code>, <code>paramNames</code>,
     *         and <code>paramDefaults</code> (if all are not
     *         <code>null</code>) do not all have the same number of elements.
     *
     * @since JAI 1.1
     */
    public OperationDescriptorImpl(String[][] resources,
                                   String[]   supportedModes,
                                   String[]   sourceNames,
                                   Class [][] sourceClasses,
                                   String[]   paramNames,
                                   Class []   paramClasses,
                                   Object[]   paramDefaults,
                                   Object[]   validParamValues) {

	sourceNames = checkSources(resources, supportedModes,
					sourceNames, sourceClasses);

	this.resources	    = resources;
	this.supportedModes = supportedModes;
	this.sourceNames    = sourceNames;
	this.sourceClasses  = sourceClasses;
	this.paramNames     = paramNames;

	this.modeIndices   = new CaselessStringArrayTable(supportedModes);
	this.sourceIndices = new CaselessStringArrayTable(sourceNames);

	// Create one ParameterListDescriptor and use the same for each mode.

	ParameterListDescriptor pld = new ParameterListDescriptorImpl(
		this, paramNames, paramClasses, paramDefaults, validParamValues);

	paramListDescriptors =
		new ParameterListDescriptor[supportedModes.length];

	for (int i = 0; i < supportedModes.length; i++) {
	    paramListDescriptors[i] = pld;
	}
    }

    /**
     * Constructor. This assumes that all modes have the same
     * set of parameter classes, defaults and valid values. The
     * source names are automatically generated using the default
     * source naming convertion ("source0", "source1", etc.). The
     * source class list is automatically generated using
     * <code>makeDefaultSourceClassList()</code> from <code>numSources</code>
     * and <code>supportedModes</code>
     *
     * @param resources  The resource tags and their corresponding data.
     * @param supportedModes  The modes that this operator supports.
     *        maybe one or more of "rendered", "renderable", "collection",
     *        and "renderableCollection". Must support at least one mode.
     * @param numSources  The number of sources.
     * @param paramNames  The localized parameter names.  It may be
     *        <code>null</code> if this operation has no parameters.
     * @param paramClasses  The parameter types required by this operation.
     *        It may be <code>null</code> if this operation has no parameters.
     * @param paramDefaults  The parameter default values for each parameter
     *         It may be <code>null</code> if this operation has no
     *	       parameters, or none of the parameters has a default value.
     * @param validParamValues defines the valid values for each parameter
     *		for all modes.  this can be <code>null</code> if the operation
     *		has no parameters. Otherwise it can be filled in as
     *		defined in {@link
     *		ParameterListDescriptorImpl#ParameterListDescriptorImpl(
     *		    Object, String[], Class[], Object[], Object[])}
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if supportedModes is <code>null</code>
     * @throws IllegalArgumentException if this operation has parameters and
     *         <code>paramClasses</code> or <code>paramNames</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if this operation has parameters
     *         and <code>paramClasses</code>, <code>paramNames</code>,
     *         and <code>paramDefaults</code> (if all are not
     *         <code>null</code>) do not all have the same number of elements.
     *
     * @since JAI 1.1
     */
    public OperationDescriptorImpl(String[][] resources,
                                   String[]   supportedModes,
                                   int        numSources,
                                   String[]   paramNames,
                                   Class []   paramClasses,
                                   Object[]   paramDefaults,
                                   Object[]   validParamValues) {

	Class[][] sourceClasses =
		    makeDefaultSourceClassList(supportedModes, numSources);

	String[] sourceNames = checkSources(resources, supportedModes,
					null, sourceClasses);

	this.resources	    = resources;
	this.supportedModes = supportedModes;
	this.sourceNames    = sourceNames;
	this.sourceClasses  = sourceClasses;
	this.paramNames     = paramNames;

	this.modeIndices   = new CaselessStringArrayTable(supportedModes);
	this.sourceIndices = new CaselessStringArrayTable(sourceNames);

	// Create one ParameterListDescriptor and use the same for each mode.

	ParameterListDescriptor pld = new ParameterListDescriptorImpl(
		this, paramNames, paramClasses, paramDefaults, validParamValues);

	paramListDescriptors =
		new ParameterListDescriptor[supportedModes.length];

	for (int i = 0; i < supportedModes.length; i++) {
	    paramListDescriptors[i] = pld;
	}
    }

    /**
     * Constructor which accepts a <code>ParameterListDescriptor</code>
     * to describe the parameters for each mode. Note
     * that <code>sourceClasses[m][i]</code> corresponds to
     * the mode <code>supportedModes[m]</code> and the source
     * <code>sourceNames[i]</code>.
     *
     * @param resources  The resource tags and their corresponding data.
     * @param supportedModes  The modes that this operator supports.
     *        maybe one or more of "rendered", "renderable", "collection",
     *        and "renderableCollection". Must support at least one mode.
     * @param sourceNames  The source names.  It may be <code>null</code>
     *        if this operation has no sources or if the default source
     *        naming convention ("source0", "source1", etc.) is to be used.
     * @param sourceClasses  The source types required by this operation
     *        for each of the above supported modes. can be null
     *        if this operation has no sources. The number of
     *	      sources for each mode must be the same.
     * @param pld the parameter list descriptor for each mode.
     *		Can be <code>null</code> if there are no parameters.
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if supportedModes is <code>null</code>
     * @throws IllegalArgumentException if the number of <code>sourceClasses</code>
     *	       for each mode is not the same or is not equal to the number
     *	       of sourceNames (if non-null).
     * @throws IllegalArgumentException if <code>sourceNames</code>
     *         is non-<code>null</code> and its length does not equal
     *         the number of sources of this operation.
     *
     * @since JAI 1.1
     */
    public OperationDescriptorImpl(String[][] resources,
                                   String[]   supportedModes,
                                   String[]   sourceNames,
                                   Class [][] sourceClasses,
                                   ParameterListDescriptor[] pld) {

	sourceNames = checkSources(resources, supportedModes,
					sourceNames, sourceClasses);

	this.resources	    = resources;
	this.supportedModes = supportedModes;
	this.sourceNames    = sourceNames;
	this.sourceClasses  = sourceClasses;

	this.modeIndices   = new CaselessStringArrayTable(supportedModes);
	this.sourceIndices = new CaselessStringArrayTable(sourceNames);

	if ((pld != null) && (pld.length != supportedModes.length)) {
	   throw new IllegalArgumentException(
		  JaiI18N.formatMsg("OperationDescriptorImpl0",
		  new Object[]
		    { "ParameterListDescriptor's",
		      new Integer(supportedModes.length)
		    }));
	}

	if (pld == null) {

	    ParameterListDescriptor tpld = new ParameterListDescriptorImpl();

	    paramListDescriptors =
		new ParameterListDescriptor[supportedModes.length];

	    for (int i = 0; i < supportedModes.length; i++)
		paramListDescriptors[i] = tpld;

	    this.paramNames = null;
	} else {
	    paramListDescriptors = pld;
	    this.paramNames      = paramListDescriptors[0].getParamNames();
	}
    }

    /**
     * Constructor. This assumes that all modes use the same
     * <code>ParameterListDescriptor</code>. Note
     * that <code>sourceClasses[m][i]</code> corresponds to
     * the mode <code>supportedModes[m]</code> and the source
     * <code>sourceNames[i]</code>.
     *
     * @param resources  The resource tags and their corresponding data.
     * @param supportedModes  The modes that this operator supports.
     *        maybe one or more of "rendered", "renderable", "collection",
     *        and "renderableCollection". Must support at least one mode.
     * @param sourceNames  The source names.  It may be <code>null</code>
     *        if this operation has no sources or if the default source
     *        naming convention ("source0", "source1", etc.) is to be used.
     * @param sourceClasses  The source types required by this operation
     *        for each of the above supported modes. can be null
     *        if this operation has no sources. The number of
     *	      sources for each mode must be the same.
     * @param pld the parameter list descriptor for all modes.
     *		Can be <code>null</code> if there are no parameters.
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if supportedModes is <code>null</code>
     * @throws IllegalArgumentException if the number of <code>sourceClasses</code>
     *	       for each mode is not the same or is not equal to the number
     *	       of sourceNames (if non-null).
     * @throws IllegalArgumentException if <code>sourceNames</code>
     *         is non-<code>null</code> and its length does not equal
     *         the number of sources of this operation.
     *
     * @since JAI 1.1
     */
    public OperationDescriptorImpl(String[][] resources,
                                   String[]   supportedModes,
                                   String[]   sourceNames,
                                   Class [][] sourceClasses,
                                   ParameterListDescriptor pld) {

	sourceNames = checkSources(resources, supportedModes,
					sourceNames, sourceClasses);

	this.resources	    = resources;
	this.supportedModes = supportedModes;
	this.sourceNames    = sourceNames;
	this.sourceClasses  = sourceClasses;

	this.modeIndices   = new CaselessStringArrayTable(supportedModes);
	this.sourceIndices = new CaselessStringArrayTable(sourceNames);

	if (pld == null)
	   pld = new ParameterListDescriptorImpl();

	this.paramNames    = pld.getParamNames();

	paramListDescriptors =
		new ParameterListDescriptor[supportedModes.length];

	for (int i = 0; i < supportedModes.length; i++) {
	    paramListDescriptors[i] = pld;
	}
    }


    /** Gets the default source names. */
    private String[] getDefaultSourceNames(int numSources) {

        String[] defaultSourceNames = new String[numSources];
        for(int i = 0; i < numSources; i++) {
            defaultSourceNames[i] = "source"+i;
        }
        return defaultSourceNames;
    }

    // BEGIN : RegistryElementDescriptor methods

    /**
     * Returns the name of this operation; this is the same as the
     * <code>GlobalName</code> value in the resources and is visible
     * to all. This is also descriptor name under which it is registered
     * in the <code>OperationRegistry</code>.
     *
     * @return  A <code>String</code> representing the operation's
     *          global name.
     *
     * @throws MissingResourceException if the <code>GlobalName</code>
     *         resource value is not supplied in the <code>resources</code>.
     */
    public String getName() {
        if (name == null) {
            name = (String)getResourceBundle(
                       Locale.getDefault()).getObject("GlobalName");
        }
        return name;
    }

    /**
     * The registry modes supported by this descriptor. Known modes
     * include those returned by <code>RegistryMode.getModes()</code>
     *
     * @return an array of <code>String</code>s specifying the supported modes.
     *
     * @see RegistryMode
     * @see RegistryElementDescriptor
     *
     * @since JAI 1.1
     */
    public String[] getSupportedModes() {
	return supportedModes;
    }

    /**
     * Does this descriptor support the specified registry mode ?.
     * The <code>modeName</code>s are treated in a case-insensitive
     * (but retentive) manner.
     *
     * @param modeName the registry mode name
     *
     * @return true, if the implementation of this descriptor supports
     *	       the specified mode. false otherwise.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null
     *
     * @see RegistryElementDescriptor
     *
     * @since JAI 1.1
     */
    public boolean isModeSupported(String modeName) {

	if (modeName == null)
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	return modeIndices.contains(modeName);
    }

    /**
     * Does this descriptor support properties ?
     *
     * @return The default implementation in this class returns
     * <code>true</code> so that properties are always supported.
     * Operations that do not wish to support properties must
     * override this implementation.
     *
     * @see PropertyGenerator
     * @see RegistryElementDescriptor
     *
     * @since JAI 1.1
     */
    public boolean arePropertiesSupported() {
	return true;
    }

    /**
     * Returns an array of <code>PropertyGenerator</code>s implementing
     * the property inheritance for this operator that may be used as
     * a basis for the operation's property management. The default
     * implementation returns <code>null</code>, indicating that source
     * properties are simply copied. Subclasses should override this
     * method if they wish to produce inherited properties.
     *
     * <p><strong> For the sake of backward compatibilty, if a
     * deprecated constructor was used to create this object,
     * then this method simply calls its deprecated equivalent,
     * <code>getPropertyGenerators()</code>, if the modeName is either
     * "rendered" or "renderable". </strong>
     *
     * @param modeName the registry mode name
     *
     * @return  An array of <code>PropertyGenerator</code>s, or
     *          <code>null</code> if this operation does not have any of
     *          its own <code>PropertyGenerator</code>s.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null
     *		or if it is not one of the supported modes.
     * @throws UnsupportedOperationException if <code>arePropertiesSupported</code>
     *          returns <code>false</code>
     *
     * @see RegistryElementDescriptor
     *
     * @since JAI 1.1
     */
    public PropertyGenerator[] getPropertyGenerators(String modeName) {

	if (modeName == null)
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	if (deprecated) {
	    if (modeName.equalsIgnoreCase("rendered") ||
		modeName.equalsIgnoreCase("renderable"))
		return getPropertyGenerators();
	}

	if (!arePropertiesSupported()) {
	    throw new UnsupportedOperationException(
		       JaiI18N.formatMsg("OperationDescriptorImpl3",
		       new Object[] { modeName }));
	}

	return null;
    }

    /**
     * Returns the <code>ParameterListDescriptor</code> that describes
     * the associated parameters (NOT sources). This method returns
     * null if there are no parameters for the specified modeName.
     * If the specified modeName supports parameters but the
     * implementing class does not have parameters, then this method
     * returns a non-null <code>ParameterListDescriptor</code> whose
     * <code>getNumParameters()</code> returns 0.
     *
     * @param modeName the registry mode name.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null
     *		or if it is not one of the supported modes.
     *
     * @see RegistryElementDescriptor
     *
     * @since JAI 1.1
     */
    public ParameterListDescriptor getParameterListDescriptor(String modeName) {
	return paramListDescriptors[modeIndices.indexOf(modeName)];
    }

    // END : RegistryElementDescriptor methods

    /**
     * Returns the resource data for this operation.  It must contain
     * <code>String</code> data for the following tags: "GlobalName",
     * "LocalName", "Vendor", "Description", "DocURL", and "Version".
     * Additional resources should be supplied when appropriate.
     *
     * <p> The default implementation simply returns a reference to
     * the local "resources" variable, which should be supplied by
     * each subclass by way of the superclass constructor.  It also
     * ignores the <code>Locale</code> argument, and always returns
     * the <code>String</code>s in the default <code>Locale</code>.
     *
     * @param locale  The <code>Locale</code> in which to localize the
     *        resource data.
     *
     * @return  A two-dimensional array of <code>String</code>s containing
     *          the mandatory and optional resource tags and their
     *          corresponding resource data. (String[i][0] is
     *	        the tag for the i-th resource and String[i][1] is the
     *	        corresponding data)
     */
    public String[][] getResources(Locale locale) {
        return resources;
    }

    /**
     * Returns the resource data for this operation in a
     * <code>ResourceBundle</code>.  The resource data are taken from the
     * <code>getResources()</code> method.
     *
     * <p> The default implementation ignores the <code>Locale</code>
     * argument, and always returns the resources in the default
     * <code>Locale</code>.
     *
     * @param locale  The <code>Locale</code> in which to localize the
     *        resource data.
     *
     * @return A <code>ResourceBundle</code> containing mandatory and
     *         optional resource information.
     */
    public ResourceBundle getResourceBundle(Locale locale) {
	final Locale l = locale;
        return new ListResourceBundle() {
            public Object[][] getContents() {
                return getResources(l);
            }
        }; // from return statement
    }

    /**
      * Returns the number of sources required by this operation.
      * All modes have the same number of sources.
      */
    public int getNumSources() {
	return sourceNames.length;
    }

    /**
     * Returns an array of <code>Class</code>es that describe the types
     * of sources required by this operation for the specified mode.
     * If this operation has no sources, this method returns <code>null</code>.
     *
     * @param modeName the operation mode name
     *
     * @throws IllegalArgumentException if modeName is <code>null</code>
     *		or if it is not one of the supported modes.
     *
     * @since JAI 1.1
     */
    public Class[] getSourceClasses(String modeName) {
	checkModeName(modeName);

	Class[] sc = sourceClasses[modeIndices.indexOf(modeName)];

	if ((sc != null) && (sc.length <= 0))
	    return null;

	return sc;
    }

    /**
     * Returns an array of <code>String</code>s that are the names
     * of the sources of this operation.  If this operation has no
     * sources, this method returns <code>null</code>.  If this
     * operation has sources but their names were not provided at
     * construction time, then the returned names will be of the
     * form "source0", "source1", etc.
     *
     * @since JAI 1.1
     */
    public String[] getSourceNames() {

	if ((sourceNames == null) || (sourceNames.length <= 0))
	    return null;

        return sourceNames;
    }


    /**
     * Returns a <code>Class</code> that describes the type of
     * destination this operation produces for the specified mode.
     *
     * <p><strong> For the sake of backward compatibilty, if a
     * deprecated constructor was used to create this object, then this
     * method simply calls its deprecated equivalent, if the modeName is
     * either "rendered" or "renderable". </strong>
     *
     * @param modeName the operation mode name
     *
     * @throws IllegalArgumentException if modeName is <code>null</code>
     *		or if it is not one of the supported modes.
     *
     * @since JAI 1.1
     */
    public Class getDestClass(String modeName) {

	checkModeName(modeName);

	if (deprecated) {

	    if (modeName.equalsIgnoreCase("rendered"))
		return getDestClass();

	    if (modeName.equalsIgnoreCase("renderable"))
		return getRenderableDestClass();
	}

	return RegistryMode.getMode(modeName).getProductClass();
    }

    /**
     * Returns <code>true</code> if this operation supports the
     * specified mode, and is capable of handling the given input
     * source(s) for the specified mode. The default implementation
     * ensures that the <code>ParameterBlock</code> has at least
     * the required number of sources. It also verifies the class type
     * of the first <code>getNumSources()</code> and makes sure
     * that none of them are <code>null</code>. Any extra sources in
     * the <code>ParameterBlock</code> are ignored. Subclasses should
     * override this implementation if their requirement on the
     * sources are different from the default. This method is used by
     * <code>validateArguments</code> to validate the sources.
     *
     * <p><strong> For the sake of backward compatibilty, if a
     * deprecated constructor was used to create this object, then this
     * method simply calls its deprecated equivalent, if the <code>modeName</code> is
     * either "rendered" or "renderable". </strong>
     *
     * @param modeName the operation mode name
     * @param args  a <code>ParameterBlock</code> that has the sources
     * @param msg  A string that may contain error messages.
     *
     * @throws IllegalArgumentException if any of the input parameters are <code>null</code>.
     *
     * @since JAI 1.1
     *
     * @see #validateArguments
     */
    protected boolean validateSources(String modeName,
				      ParameterBlock args,
                                      StringBuffer msg) {
	if (modeName == null)
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	if (deprecated) {

	    if (modeName.equalsIgnoreCase("rendered"))
		return validateSources(args, msg);

	    if (modeName.equalsIgnoreCase("renderable"))
		return validateRenderableSources(args, msg);
	}

	return validateSources(getSourceClasses(modeName), args, msg);
    }

    /**
     * Returns <code>true</code> if this operation is capable of
     * handling the input parameters for the specified mode. The default
     * implementation validates the number of parameters, the class type
     * of each parameter, and null parameter. For non-null parameters,
     * it also checks to see if the parameter value is valid. Subclasses
     * should override this implementation if their requirement on the
     * parameter objects are different from the default. This is used by
     * <code>validateArguments</code> to validate the parameters.
     *
     * <p> JAI allows unspecified tailing parameters if these parameters
     * have default values.  This method automatically sets these unspecified
     * parameters to their default values.  However, if a parameter, which
     * has a default value, is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>; else this method returns
     * <code>false</code>.
     *
     * <p> This method sets all the undefined parameters in the
     * <code>ParameterBlock</code> to their default values, if the default
     * values are specified.
     *
     * <p> Note that <code>DeferredData</code> parameters will not be
     * recognized as valid unless the parameter is defined to have class
     * <code>DeferredData.class</code>.
     *
     * <p><strong> For the sake of backward compatibilty, if a
     * deprecated constructor was used to create this object, then this
     * method simply calls its deprecated equivalent, if the <code>modeName</code> is
     * either "rendered" or "renderable". </strong>
     *
     * @throws IllegalArgumentException if any of the input parameters are <code>null</code>.
     *
     * @since JAI 1.1
     *
     * @see #validateArguments
     * @see ParameterListDescriptorImpl#isParameterValueValid
     */
    protected boolean validateParameters(String modeName, ParameterBlock args,
                                         StringBuffer msg) {
	if (modeName == null)
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	if (deprecated) {
	    if (modeName.equalsIgnoreCase("rendered") ||
		modeName.equalsIgnoreCase("renderable"))
		return validateParameters(args, msg);
	}

	return validateParameters(
		    getParameterListDescriptor(modeName), args, msg);
    }

    /**
     * Returns <code>true</code> if this operation/mode is capable of
     * handling the input source(s) and/or parameter(s)
     * specified in the <code>ParameterBlock</code>, or
     * <code>false</code> otherwise, in which case an explanatory
     * message may be appended to the <code>StringBuffer</code>.
     *
     * <p> This method is the standard place where input arguments are
     * validated against this operation's specification for the specified
     * mode.  It is called by <code>JAI.create()</code> as a part of its
     * validation process.  Thus it is strongly recommended that the
     * application programs use the <code>JAI.create()</code> methods to
     * instantiate all the rendered operations.
     *
     * <p> The default implementation of this method makes sure that
     * this operator supports the specified mode and then calls
     * <code>validateSources</code> and <code>validateParameters</code>.
     *
     * <p> This method sets all the undefined parameters in the
     * <code>ParameterBlock</code> to their default values, if the default
     * values are specified.
     *
     * <p> Note that <code>DeferredData</code> parameters will not be
     * recognized as valid unless the parameter is defined to have class
     * <code>DeferredData.class</code>.
     *
     * @param modeName the operation mode name
     * @param args  Input arguments, including source(s) and/or parameter(s).
     * @param msg  A string that may contain error messages.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is <code>null</code>
     *
     * @since JAI 1.1
     *
     * @see #validateSources
     * @see #validateParameters
     */
    public boolean validateArguments(String modeName, ParameterBlock args, StringBuffer msg) {
	return (isModeSupported(modeName) &&
		validateSources(modeName, args, msg) &&
	        validateParameters(modeName, args, msg));
    }

    /**
     * Returns <code>true</code> if the operation should be computed
     * immediately for all supported modes of this operation during
     * the call to <code>JAI.create()</code>; that is, the operation
     * is placed in immediate mode. If <code>true</code>, and
     * the computation fails, <code>null</code> will be returned
     * from <code>JAI.create()</code>. If <code>false</code>,
     * <code>JAI.create()</code> will return an instance of the
     * appropriate destination class that may be asked to compute itself
     * at a later time; this computation may fail at that time.
     *
     * <p> Operations that rely on an external resource, such as
     * a source file, or that produce externally-visible side
     * effects, such as writing to an output file, should return
     * <code>true</code> from this method. Operations that rely
     * only on their sources and parameters usually wish to return
     * <code>false</code> in order to defer rendering as long as
     * possible.
     *
     * <p> The default implementation in this class returns
     * <code>false</code> so that deferred execution is invoked.
     * Operations that wish to be placed in the immediate mode must
     * override this implementation.
     */
    public boolean isImmediate() {
        return false;
    }

    /**
     * Calculates the region over which two distinct renderings
     * of an operation may be expected to differ.
     *
     * <p> The class of the returned object will vary as a function of
     * the mode of the operation.  For rendered and renderable two-
     * dimensional images this will be an instance of a class which
     * implements <code>java.awt.Shape</code>.
     *
     * @param modeName The name of the mode.
     * @param oldParamBlock The previous sources and parameters.
     * @param oldHints The previous hints.
     * @param newParamBlock The current sources and parameters.
     * @param newHints The current hints.
     * @param node The affected node in the processing chain.
     *
     * @return <code>null</code> to indicate that there is no
     *         common region of validity.
     *
     * @throws IllegalArgumentException if <code>modeName</code>
     *         is <code>null</code> or if the operation requires either
     *         sources or parameters and either <code>oldParamBlock</code>
     *         or <code>newParamBlock</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>oldParamBlock</code> or
     *         <code>newParamBlock</code> do not contain sufficient sources
     *         or parameters for the operation in question.
     *
     * @since JAI 1.1
     */
    public Object getInvalidRegion(String modeName,
                                   ParameterBlock oldParamBlock,
                                   RenderingHints oldHints,
                                   ParameterBlock newParamBlock,
                                   RenderingHints newHints,
                                   OperationNode node) {
	if (modeName == null)
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        return null;
    }

    /**
     * Get the default source class for the supported mode.
     *
     * "rendered"   - java.awt.image.RenderedImage.class
     * "renderable" - java.awt.image.renderable.RenderableImage.class
     * "collection" - java.util.Collection.class
     * "renderableCollection" - java.util.Collection.class
     *
     * @since JAI 1.1
     */
    protected static Class getDefaultSourceClass(String modeName) {

	if ("rendered".equalsIgnoreCase(modeName))
	    return java.awt.image.RenderedImage.class;

	if ("renderable".equalsIgnoreCase(modeName))
	    return java.awt.image.renderable.RenderableImage.class;

	if ("collection".equalsIgnoreCase(modeName))
	    return java.util.Collection.class;

	if ("renderableCollection".equalsIgnoreCase(modeName))
	    return java.util.Collection.class;

	return null;
    }

    /**
     * Create a list of per mode source classes for each supported mode
     * which can then be passed on to the constructor. Uses
     * <code>getDefaultSourceClass(modeName)</code> to construct this
     * list.
     *
     * @since JAI 1.1
     */
    protected static Class[][] makeDefaultSourceClassList(
			String[] supportedModes, int numSources) {
	
	if ((supportedModes == null) || (supportedModes.length == 0))
	    return null;

	int count = supportedModes.length;

	Class classes[][] = new Class[count][numSources];

	for (int i = 0; i < count; i++) {

	    Class sourceClass = getDefaultSourceClass(supportedModes[i]);

	    for (int j = 0; j < numSources; j++)
		classes[i][j] = sourceClass;
	}

	return classes;
    }
    /********************** DEPRECATED Constructors ********************/

    /**
     * Create a list of supported modes using the deprecated is*Supported
     * methods.
     */
    private String[] makeSupportedModeList() {

	int count = 0;

	if (isRenderedSupported())   count++;
	if (isRenderableSupported()) count++;

	String modes[] = new String[count];

	count = 0;

	if (isRenderedSupported())   modes[count++] = "rendered";
	if (isRenderableSupported()) modes[count++] = "renderable";

	return modes;
    }

    /**
     * Create a list of per mode source classes for use by deprecated
     * constructors.
     */
    private Class[][] makeSourceClassList(Class[] sourceClasses,
					Class[] renderableSourceClasses) {
	int count = 0;

	if (isRenderedSupported())   count++;
	if (isRenderableSupported()) count++;

	Class classes[][] = new Class[count][];

	count = 0;

	if (isRenderedSupported())   classes[count++] = sourceClasses;
	if (isRenderableSupported()) classes[count++] = renderableSourceClasses;

	return classes;
    }

    /**
     * Create a list of valid parameter values, one for each param.
     */
    private Object[] makeValidParamValueList(Class[] paramClasses) {

	if (paramClasses == null)
	    return null;

	int numParams = paramClasses.length;

	Object validValues[] = null;

	for (int i = 0; i < numParams; i++) {
	    Number min = getParamMinValue(i);
	    Number max = getParamMaxValue(i);

	    if ((min == null) && (max == null))
		continue;

	    if (validValues == null)
		validValues = new Object[numParams];

	    validValues[i] = new Range(
		    min.getClass(), (Comparable)min, (Comparable)max);
	}

	return validValues;
    }

    /**
     * Constructor.
     *
     * @param resources  The resource tags and their corresponding data.
     * @param sourceClasses  The source types required by this operation
     *        in the rendered mode.  It may be <code>null</code>
     *        if this operation does not support the rendered mode, or
     *        if it has no sources.
     * @param renderableSourceClasses  The source types required by this
     *        operation in the renderable mode.  It may be
     *        <code>null</code> if this operation does not support the
     *        renderable mode, or if it has no sources.
     * @param paramClasses  The parameter types required by this operation.
     *        It may be <code>null</code> if this operation has no
     *        parameters.
     * @param paramNames  The localized parameter names.  It may be
     *        <code>null</code> if this operation has no parameters.
     * @param paramDefaults  The parameter default values.  It may be
     *        <code>null</code> if this operation has no parameters,
     *        or none of the parameters has a default value.
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if this operation supports the
     *         rendered mode, and it has sources, and
     *         <code>sourceClasses</code> is <code>null</code>.
     * @throws IllegalArgumentException if this operation supports the
     *         renderable mode, and it has sources, and
     *         <code>renderableSourceClasses</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>sourceClasses</code>
     *         and <code>renderableSourceClasses</code> (if both are not
     *         <code>null</code>) do not have the same number of elements.
     * @throws IllegalArgumentException if this operation has parameters and
     *         <code>paramClasses</code> or <code>paramNames</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if this operation has parameters
     *         and <code>paramClasses</code>, <code>paramNames</code>,
     *         and <code>paramDefaults</code> (if all are not
     *         <code>null</code>) do not all have the same number of elements.
     *
     * @deprecated as of JAI 1.1 in favor of
     * constructors where supported modes are explicitly specified.
     * Uses <code>isRenderedSupported()</code> and <code>
     * isRenderableSupported()</code> to figure out the supported modes.
     *
     * @see OperationDescriptorImpl#OperationDescriptorImpl(String[][],
     *	    String[], String[], Class [][], String[], Class [], Object[], Object[])
     */
    public OperationDescriptorImpl(String[][] resources,
                                   Class[] sourceClasses,
                                   Class[] renderableSourceClasses,
                                   Class[] paramClasses,
                                   String[] paramNames,
                                   Object[] paramDefaults) {
	this.deprecated	= true;

	String[] supportedModes = makeSupportedModeList();
	Class[][] sourceClassList = makeSourceClassList(sourceClasses,
					    renderableSourceClasses);

	String[] sourceNames = checkSources(resources, supportedModes,
					null, sourceClassList);

	Object[] validParamValues = makeValidParamValueList(paramClasses);

	this.resources	    = resources;
	this.supportedModes = supportedModes;
	this.sourceNames    = sourceNames;
	this.sourceClasses  = sourceClassList;
	this.paramNames     = paramNames;

	this.modeIndices   = new CaselessStringArrayTable(supportedModes);
	this.sourceIndices = new CaselessStringArrayTable(sourceNames);

	// Create one ParameterListDescriptor and use the same for each mode.

	ParameterListDescriptor pld = new ParameterListDescriptorImpl(
		this, paramNames, paramClasses, paramDefaults, validParamValues);

	paramListDescriptors =
		new ParameterListDescriptor[supportedModes.length];

	for (int i = 0; i < supportedModes.length; i++) {
	    paramListDescriptors[i] = pld;
	}
    }

    /**
     * Constructor for operations that support either the rendered
     * or the renderable or both modes.  The class type for all the
     * source(s) of the rendered mode (if supported) is set to
     * <code>java.awt.image.RenderedImage.class</code>.
     * The class type for all the source(s) of the renderable mode (if
     * supported) is set to
     * <code>java.awt.image.renderable.RenderableImage</code>.
     *
     * @param resources  The resource tags and their corresponding data.
     * @param numSources  The number of sources required by this operation.
     *        It should not be negative.  A negative value indicates this
     *        operation has no sources.
     * @param paramClasses  The parameter types required by this operation.
     *        It may be <code>null</code> if this operation has no
     *        parameters.
     * @param paramNames  The localized parameter names.  It may be
     *        <code>null</code> if this operation has no parameters.
     * @param paramDefaults  The parameter default values.  It may be
     *        <code>null</code> if this operation has no parameters,
     *        or none of the parameters has a default value.
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if this operation has parameters and
     *         <code>paramClasses</code> or <code>paramNames</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if this operation has parameters
     *         and <code>paramClasses</code>, <code>paramNames</code>,
     *         and <code>paramDefaults</code> (if not <code>null</code>)
     *         do not all have the same number of elements.
     *
     * @deprecated as of JAI 1.1 in favor of
     * constructors where supported modes are explicitly specified.
     * Uses <code>isRenderedSupported()</code> and <code>
     * isRenderableSupported()</code> to figure out the supported modes.
     *
     * @see OperationDescriptorImpl#OperationDescriptorImpl(String[][],
     *	    String[], int, String[], Class [], Object[], Object[])
     */
    public OperationDescriptorImpl(String[][] resources,
                                   int numSources,
                                   Class[] paramClasses,
                                   String[] paramNames,
                                   Object[] paramDefaults) {
	this.deprecated	= true;

	String[] supportedModes = makeSupportedModeList();
	Class[][] sourceClassList =
		makeDefaultSourceClassList(supportedModes, numSources);

	String[] sourceNames = checkSources(resources, supportedModes,
					null, sourceClassList);

	Object[] validParamValues = makeValidParamValueList(paramClasses);

	this.resources	    = resources;
	this.supportedModes = supportedModes;
	this.sourceNames    = sourceNames;
	this.sourceClasses  = sourceClassList;
	this.paramNames     = paramNames;

	this.modeIndices   = new CaselessStringArrayTable(supportedModes);
	this.sourceIndices = new CaselessStringArrayTable(sourceNames);

	// Create one ParameterListDescriptor and use the same for each mode.

	ParameterListDescriptor pld = new ParameterListDescriptorImpl(
		this, paramNames, paramClasses, paramDefaults, validParamValues);

	paramListDescriptors =
		new ParameterListDescriptor[supportedModes.length];

	for (int i = 0; i < supportedModes.length; i++) {
	    paramListDescriptors[i] = pld;
	}
    }

    /**
     * Constructor for operations that supports only the rendered mode
     * and requires no parameters.
     *
     * @param resources  The resource tags and their corresponding data.
     * @param sourceClasses  The source types required by this operation
     *        in the rendered mode.  It may be <code>null</code>
     *        if this operation has no sources.
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of constructors where the mode
     * specfic information is explicitly specified.
     *
     * @see OperationDescriptorImpl#OperationDescriptorImpl(String[][],
     *	    String[], String[], Class [][], String[], Class [], Object[], Object[])
     */
    public OperationDescriptorImpl(String[][] resources,
                                   Class[] sourceClasses) {
        this(resources, sourceClasses, null, null, null, null);
    }

    /**
     * Constructor for operations that supports either the rendered
     * or the renderable or both modes and requires no parameters.
     *
     * @param resources  The resource tags and their corresponding data.
     * @param sourceClasses  The source types required by this operation
     *        in the rendered mode.  It may be <code>null</code>
     *        if this operation does not support the rendered mode, or
     *        if it has no sources.
     * @param renderableSourceClasses  The source types required by this
     *        operation in the renderable mode.  It may be
     *        <code>null</code> if this operation does not support the
     *        renderable mode, or if it has no sources.
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if this operation supports the
     *         rendered mode, and it has sources, and
     *         <code>sourceClasses</code> is <code>null</code>.
     * @throws IllegalArgumentException if this operation supports the
     *         renderable mode, and it has sources, and
     *         <code>renderableSourceClasses</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>sourceClasses</code>
     *         and <code>renderableSourceClasses</code> (if both are not
     *         <code>null</code>) do not have the same number of elements.
     *
     * @deprecated as of JAI 1.1 in favor of constructors where the mode
     * specfic information is explicitly specified.
     *
     * @see OperationDescriptorImpl#OperationDescriptorImpl(String[][],
     *	    String[], String[], Class [][], String[], Class [], Object[], Object[])
     */
    public OperationDescriptorImpl(String[][] resources,
                                   Class[] sourceClasses,
                                   Class[] renderableSourceClasses) {
        this(resources,
             sourceClasses, renderableSourceClasses, null, null, null);
    }

    /**
     * Constructor for operations that supports either the rendered
     * or the renderable or both modes and requires no sources.
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if this operation has parameters and
     *         <code>paramClasses</code> or <code>paramNames</code> is
     *         <code>null</code>.
     * @throws IllegalArgumentException if this operation has parameters
     *         and <code>paramClasses</code>, <code>paramNames</code>,
     *         and <code>paramDefaults</code> (if not <code>null</code>)
     *         do not all have the same number of elements.
     *
     * @deprecated as of JAI 1.1 in favor of constructors where the mode
     * specfic information is explicitly specified.
     *
     * @see OperationDescriptorImpl#OperationDescriptorImpl(String[][],
     *	    String[], int, String[], Class [], Object[], Object[])
     */
    public OperationDescriptorImpl(String[][] resources,
                                   Class[] paramClasses,
                                   String[] paramNames,
                                   Object[] paramDefaults) {
        this(resources, null, null,
             paramClasses, paramNames, paramDefaults);
    }

    /**
     * Constructor for operations that support the rendered mode and
     * possibly the renderable mode and require no parameters.  The
     * class type for all the source(s) of the rendered mode is set to
     * <code>java.awt.image.RenderedImage.class</code>.
     * The class type for all the source(s) of the renderable mode (if
     * supported) is set to
     * <code>java.awt.image.renderable.RenderableImage</code>.
     *
     * @param resources  The resource tags and their corresponding data.
     * @param numSources  The number of sources required by this operation.
     *        It should not be negative.  A negative value indicates this
     *        operation has no sources.
     *
     * @throws IllegalArgumentException if <code>resources</code> is
     *         <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of constructors where the mode
     * specfic information is explicitly specified.
     *
     * @see OperationDescriptorImpl#OperationDescriptorImpl(String[][],
     *	    String[], int, String[], Class [], Object[], Object[])
     */
    public OperationDescriptorImpl(String[][] resources,
                                   int numSources) {
        this(resources, numSources, null, null, null);
    }

    /********************** DEPRECATED METHODS *************************/

    /**
     * Returns an array of <code>PropertyGenerator</code>s implementing
     * the property inheritance for this operation.  The default
     * implementation returns <code>null</code>, indicating that source
     * properties are simply copied.  Subclasses should override
     * this method if they wish to produce inherited properties.
     *
     * @deprecated as of JAI 1.1 in favor of the equivalent method
     *	that specifies the mode name.
     *
     * @see #getPropertyGenerators
     */
    public PropertyGenerator[] getPropertyGenerators() {
        return deprecated ? null : getPropertyGenerators("rendered");
    }

    /**
     * Returns <code>true</code> if this operation supports the
     * rendered mode.  The default implementation in this
     * class returns <code>true</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>isModeSupported("rendered")</code>
     *
     * @see #isModeSupported
     */
    public boolean isRenderedSupported() {
        return deprecated ? true : isModeSupported("rendered");
    }

    /**
     * Returns the source class types of this operation for the rendered
     * mode.  If this operation has no sources, this method returns <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getSourceClasses("rendered")</code>
     *
     * @throws IllegalArgumentException if the <code>rendered</code> mode
     *		    is not supported.
     *
     * @see #getSourceClasses
     */
    public Class[] getSourceClasses() {
        return getSourceClasses("rendered");
    }

    /**
     * Returns the destination class type of this operation for the
     * rendered mode.  The default implementation in this class returns
     * <code>java.awt.image.RenderedImage.class</code> if this operation
     * supports the rendered mode, or <code>null</code> otherwise.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getDestClass("rendered")</code>
     *
     * @see #getDestClass
     */
    public Class getDestClass() {
	if (deprecated) {
	    return (isRenderedSupported() ?
		    java.awt.image.RenderedImage.class : null);
	} else {
	    return getDestClass("rendered");
	}
    }

    /**
     * Returns <code>true</code> if this operation supports the rendered
     * mode, and is capable of handling the input arguments for the
     * rendered mode.  The default implementation validates both the
     * source(s) and the parameter(s).
     *
     * <p> Note that <code>DeferredData</code> parameters will not be
     * recognized as valid unless the parameter is defined to have class
     * <code>DeferredData.class</code>.
     *
     * <p> Additional validations should be added by each individual
     * operation based on its specification.
     *
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>msg</code> is <code>null</code>
     *         and the validation fails.
     *
     * @deprecated as of JAI 1.1 in favor of <code>validateArguments("rendered", ...)</code>
     *
     * @see #validateArguments
     */
    public boolean validateArguments(ParameterBlock args,
                                     StringBuffer msg) {
	if (deprecated) {
	    return (validateSources(args, msg) &&
		    validateParameters(args, msg));
	} else {
	    return validateArguments("rendered", args, msg);
	}
    }

    /********************* Renderable Mode Methods (deprecated) ********/

    /**
     * Returns <code>true</code> if this operation supports the
     * renderable mode.  The default implementation in this
     * class returns <code>false</code>.  Operations that support
     * the renderable mode must override this implementation.
     *
     * @deprecated as of JAI 1.1 in favor of <code>isModeSupported("renderable")</code>
     *
     * @see #isModeSupported
     */
    public boolean isRenderableSupported() {
        return deprecated ? false : isModeSupported("renderable");
    }

    /**
     * Returns the source class types of this operation for the
     * renderable mode. If this operation has no sources this method
     * returns <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getSourceClasses("renderable")</code>
     *
     * @throws IllegalArgumentException if the <code>renderable</code> mode
     *		    is not supported.
     *
     * @see #getSourceClasses
     */
    public Class[] getRenderableSourceClasses() {
        return getSourceClasses("renderable");
    }

    /**
     * Returns the destination class type of this operation for the
     * renderable mode.  The default implementation in this class returns
     * <code>java.awt.image.renderable.RenderableImage.class</code> if
     * this operation supports the renderable mode, or <code>null</code>
     * otherwise.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getDestClass("renderable")</code>
     *
     * @see #getDestClass
     */
    public Class getRenderableDestClass() {
	if (deprecated) {
	    return (isRenderableSupported() ?
		    java.awt.image.renderable.RenderableImage.class : null);
	} else {
	    return getDestClass("renderable");
	}
    }

    /**
     * Returns <code>true</code> if this operation supports the
     * renderable mode, and is capable of handling the input
     * arguments for the renderable mode.  The default implementation
     * validates both the source(s) and the parameter(s).
     *
     * <p> If this operation does not support the renderable mode,
     * this method returns <code>false</code> regardless of the input
     * arguments.
     *
     * <p> Note that <code>DeferredData</code> parameters will not be
     * recognized as valid unless the parameter is defined to have class
     * <code>DeferredData.class</code>.
     *
     * <p> Additional validations should be added by each individual
     * operation based on its specification.
     *
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>msg</code> is <code>null</code>
     *         and the validation fails.
     *
     * @deprecated as of JAI 1.1 in favor of <code>validateArguments("renderable", ...)</code>
     *
     * @see #validateArguments
     */
    public boolean validateRenderableArguments(ParameterBlock args,
                                               StringBuffer msg) {
	if (deprecated) {
	    return (validateRenderableSources(args, msg) &&
		    validateParameters(args, msg));
	} else {
	    return validateArguments("renderable", args, msg);
	}
    }

    /************************ Parameter Methods (deprecated) ***********/

    /**
     * The ParameterListDescriptor for the first supported mode. Used
     * by deprecated methods where modeName is not specified.
     */
    private ParameterListDescriptor getDefaultPLD() {
	return getParameterListDescriptor(getSupportedModes()[0]);
    }

    /**
     * Returns the number of parameters (not including sources)
     * required by this operation.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getNumParameters()</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     *
     * @see ParameterListDescriptor#getNumParameters
     */
    public int getNumParameters() {
        return getDefaultPLD().getNumParameters();
    }

    /**
     * Returns the parameter class types of this operation.
     * If this operation has no parameters, this method returns
     * <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamClasses()</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     *
     * @see ParameterListDescriptor#getParamClasses
     */
    public Class[] getParamClasses() {
        return getDefaultPLD().getParamClasses();
    }

    /**
     * Returns the localized parameter names of this operation.
     * If this operation has no parameters, this method returns
     * <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamNames()</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     *
     * @see ParameterListDescriptor#getParamNames
     */
    public String[] getParamNames() {
        return getDefaultPLD().getParamNames();
    }

    /**
     * Returns the default values of the parameters for this operation.
     * If this operation has no parameters, this method returns
     * <code>null</code>.  If a parameter does not have a default value,
     * the constant
     * <code>OperationDescriptor.NO_PARAMETER_DEFAULT</code> will be
     * returned. The <code>validateArguments()</code> and
     * <code>validateRenderableArguments</code> method will return
     * <code>false</code> if an input parameter without a default value
     * is supplied as <code>null</code>, or if an unspecified tailing
     * parameter does not have a default value.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamDefaults()</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     *
     * @see ParameterListDescriptor#getParamDefaults
     */
    public Object[] getParamDefaults() {
        return getDefaultPLD().getParamDefaults();
    }

    /**
     * Returns the default value of specified parameter.  The default
     * value may be <code>null</code>.  If a parameter has no default
     * value, this method returns
     * <code>OperationDescriptor.NO_PARAMETER_DEFAULT</code>.
     *
     * @param index  The index of the parameter whose default
     *        value is queried.
     *
     * @throws IllegalArgumentException if this operation has no parameters.
     * @throws ArrayIndexOutOfBoundsException if there is no parameter
     *         corresponding to the specified <code>index</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamDefaultValue(...)</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     *
     * @see ParameterListDescriptor#getParamDefaultValue
     */
    public Object getParamDefaultValue(int index) {
        return getDefaultPLD().getParamDefaultValue(paramNames[index]);
    }

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.  If the specified parameter is non-numeric,
     * this method returns <code>null</code>.
     *
     * <p> The return value should be of the class types corresponding to
     * the parameter's primitive type, that is, <code>Byte</code> for a
     * <code>byte</code> parameter, <code>Integer</code> for an
     * <code>int</code> parameter, and so forth.
     *
     * <p> The default implementation returns the minimum value
     * in the parameter data type's full range.
     * 
     * @param index  The index of the parameter to be queried.
     *
     * @return  A <code>Number</code> representing the minimum legal value,
     *          or <code>null</code> if the specified parameter is not
     *          numeric.
     *
     * @throws IllegalArgumentException if this operation has no parameters.
     * @throws ArrayIndexOutOfBoundsException if there is no parameter
     *         corresponding to the specified <code>index</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamValueRange()</code>
     *      This will for the time being use <code>getSupportedModes()[0]
     *      </code> for modeName.
     *
     *      <p>If the parameter is not a sub-class of the <code>Number</code>
     *      class then this method returns null.
     *
     *      <p>Else if the above getParamValueRange() returns a non-null
     *      <code>Range</code> then it returns the <code>getMinValue()</code>
     *      of that <code>Range</code>.
     *
     *      <p>Else for <code>Float</code> and <code>Double</code> parameters
     *      it returns the corresponding <code>-MAX_VALUE</code> and
     *      <code>MIN_VALUE</code> for other <code>Number</code> classes.
     *
     * @see ParameterListDescriptor#getParamValueRange
     * @see ParameterListDescriptor#getEnumeratedParameterValues
     * @see ParameterListDescriptor#isParameterValueValid
     */
    public Number getParamMinValue(int index) {
	return null;
    }

    /**
     * Returns the maximum legal value of a specified numeric parameter
     * for this operation.  If the specified parameter is non-numeric,
     * this method returns <code>null</code>.
     *
     * <p> The return value should be of the class type corresponding to
     * the parameter's primitive type, that is, <code>Byte</code> for a
     * <code>byte</code> parameter, <code>Integer</code> for an
     * <code>int</code> parameter, and so forth.
     *
     * <p> The default implementation returns the maximum value
     * in the parameter data type's full range.
     * 
     * @param index  The index of the parameter to be queried.
     * 
     * @return  A <code>Number</code> representing the maximum legal value,
     *          or <code>null</code> if the specified parameter is not
     *          numeric.
     *
     * @throws IllegalArgumentException if this operation has no parameters.
     * @throws ArrayIndexOutOfBoundsException if there is no parameter
     *         corresponding to the specified <code>index</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamValueRange()</code>
     *      This will for the time being use <code>getSupportedModes()[0]
     *      </code> for modeName.
     *
     *      <p>If the parameter is not a sub-class of the <code>Number</code>
     *      class then this method returns null.
     *
     *      <p>Else if the above getParamValueRange() returns a non-null
     *      <code>Range</code> then it returns the <code>getMaxValue()</code>
     *      of that <code>Range</code>.
     *
     *      <p>Else returns the <code>MAX_VALUE</code> of the corresponding
     *      <code>Number</code> class.
     *
     * @see ParameterListDescriptor#getParamValueRange
     * @see ParameterListDescriptor#getEnumeratedParameterValues
     * @see ParameterListDescriptor#isParameterValueValid
     */
    public Number getParamMaxValue(int index) {
	return null;
    }

    /**
     * Returns <code>true</code> if this operation supports the rendered
     * mode, and is capable of handling the given input source(s) for the
     * rendered mode.  The default implementation validates the number of
     * sources, the class type of each source, and null sources.  Subclasses
     * should override this implementation if their requirement on the
     * sources are different from the default.
     *
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>msg</code> is <code>null</code>
     *         and the validation fails.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *		validateSources("rendered", ...)</code>
     *
     * @see #validateSources
     */
    protected boolean validateSources(ParameterBlock args,
                                      StringBuffer msg) {
	if (deprecated) {
	    return (isRenderedSupported() &&
		    validateSources(getSourceClasses(), args, msg));
	} else {
	    return validateSources("rendered", args, msg);
	}
    }

    /**
     * Returns <code>true</code> if this operation supports the
     * renderable mode, and is capable of handling the given input
     * source(s) for the renderable mode.  The default
     * implementation validates the number of sources, the class type of
     * each source, and null sources.  Subclasses should override this
     * implementation if their requirement on the sources are
     * different from the default.
     *
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>msg</code> is <code>null</code>
     *         and the validation fails.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *		validateSources("renderable", ...)</code>
     *
     * @see #validateSources
     */
    protected boolean validateRenderableSources(ParameterBlock args,
                                                StringBuffer msg) {
	if (deprecated) {
	    return (isRenderableSupported() &&
		    validateSources(getRenderableSourceClasses(), args, msg));
	} else {
	    return validateSources("renderable", args, msg);
	}
    }

    /**
     * Returns <code>true</code> if this operation is capable of handling
     * the given input parameters. The default implementation validates the
     * number of parameters, the class type of each parameter, and null
     * parameters. For those non-null numeric parameters, it also checks to
     * see if the parameter value is within the minimum and maximum range.
     * Subclasses should override this implementation if their requirements
     * for the parameter objects are different from the default.
     *
     * <p> JAI allows unspecified tailing parameters if these parameters
     * have default values.  This method automatically sets these unspecified
     * parameters to their default values.  However, if a parameter, which
     * has a default value, is followed by one or more parameters that
     * have no default values, this parameter must be specified in the
     * <code>ParameterBlock</code>; else this method returns
     * <code>false</code>.
     *
     * <p> Note that <code>DeferredData</code> parameters will not be
     * recognized as valid unless the parameter is defined to have class
     * <code>DeferredData.class</code>.
     *
     * @throws IllegalArgumentException if <code>args</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>msg</code> is <code>null</code>
     *         and the validation fails.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *		validateParameters(getSupportedModes()[0], ...)</code>
     *
     * @see #validateParameters
     */
    protected boolean validateParameters(ParameterBlock args,
                                         StringBuffer msg) {
	return validateParameters(getDefaultPLD(), args, msg);
    }

    /**
     * Returns the minimum number of parameters must be supplied in
     * the <code>ParameterBlock</code>.
     */
    private int getMinNumParameters(ParameterListDescriptor pld) {
        // The number of parameters this operation should have. 
        int numParams = pld.getNumParameters();

	Object paramDefaults[] = pld.getParamDefaults();

        for (int i = numParams - 1; i >= 0; i--) {
            if (paramDefaults[i] == ParameterListDescriptor.NO_PARAMETER_DEFAULT) {
                break;
            } else {
                numParams--;
            }
        }

        return numParams;
    }

    private boolean validateSources(Class[] sources,
				    ParameterBlock args,
                                    StringBuffer msg) {

	if ((args == null) || (msg == null))
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        // The number of sources this operation requires. 
        int numSources = getNumSources();

	// Check for the correct number of sources. 
	if (args.getNumSources() < numSources) {
            msg.append(JaiI18N.formatMsg("OperationDescriptorImpl6",
		new Object[] { getName(), new Integer(numSources) }));
	    return false;
	}

        for  (int i = 0; i < numSources; i++) {
            Object s = args.getSource(i);

	    // Check for null source. 
            if (s == null) {
                msg.append(JaiI18N.formatMsg("OperationDescriptorImpl7",
					new Object[] { getName()}));
                return false;
            }

	    // Check for the correct class of each supplied source. 
            Class c = sources[i];
            if (!c.isInstance(s)) {
                msg.append(JaiI18N.formatMsg("OperationDescriptorImpl8",
		    new Object[] {
			    getName(),
			    new Integer(i),
			    new String(c.toString()),
			    new String(s.getClass().toString()) }));
                return false;
            }
        }

        return true;
    }

    private boolean validateParameters(ParameterListDescriptor pld,
				       ParameterBlock args,
                                       StringBuffer msg) {

	if ((args == null) || (msg == null))
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        // The number of parameters this operation should have. 
        int numParams = pld.getNumParameters();

        // The number of parameters supplied. 
        int argNumParams = args.getNumParameters();

	Object paramDefaults[] = pld.getParamDefaults();

	// Check for the correct number of parameters. 
        if (argNumParams < numParams) {
            // The minimum number of parameters this operation must have. 
            if (argNumParams < getMinNumParameters(pld)) {
                msg.append(JaiI18N.formatMsg("OperationDescriptorImpl9",
		    new Object[] { getName(), new Integer(numParams)}));

                return false;

            } else {	// use default values
                for (int i = argNumParams; i < numParams; i++) {
                    args.add(paramDefaults[i]);
                }
            }
        }

        for (int i = 0; i < numParams; i++) {
            Object p = args.getObjectParameter(i);

	    /* Check for null parameter. */
            if (p == null) {
                p = paramDefaults[i];	// get the default parameter value

                if (p == OperationDescriptor.NO_PARAMETER_DEFAULT) {
                    msg.append(
			    JaiI18N.formatMsg("OperationDescriptorImpl11",
			    new Object[] { getName(), new Integer(i) }));
                    return false;

                } else {
                    args.set(p, i);	// replace null parameter with default
                }
            }

	    // Now check if the parameter value is valid
	    try {
		if (!pld.isParameterValueValid(paramNames[i], p)) {
                    msg.append(
			JaiI18N.formatMsg("OperationDescriptorImpl10",
			    new Object[] { getName(), pld.getParamNames()[i] }));
		    return false;
		}
	    } catch (IllegalArgumentException e) {
		msg.append(getName() + " - " + e.getLocalizedMessage());
		return false;
	    }
	}

        return true;
    }

    /**
     * Make sure that <code>modeName</code> is not <code>null</code> and
     * is one of the supported modes.
     */
    private void checkModeName(String modeName) {

	if (modeName == null)
            throw new IllegalArgumentException(
		    JaiI18N.getString("OperationDescriptorImpl12"));

	if (modeIndices.contains(modeName) == false) {
            throw new IllegalArgumentException(
			JaiI18N.formatMsg("OperationDescriptorImpl13",
			    new Object[] { getName(), modeName }));
	}

    }

}
