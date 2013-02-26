/*
 * $RCSfile: OperationDescriptor.java,v $
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

import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This interface provides a comprehensive description of a specific
 * image operation.  All information regarding the operation, such as
 * its name, version, input, and properties should be listed.  Any
 * conditions placed on the operation, such as its source class types and
 * legal parameter range, should also be included, and the methods to
 * enforce these conditions should be implemented.  A set of
 * <code>PropertyGenerator</code>s may be specified to be used as a
 * basis for the operation's property management.
 *
 * <p> Each image operation in JAI must have a descriptor
 * that implements this interface.  The following basic resource data
 * must be provided:
 * <ul>
 * <li> A global operation name that is visible to all and is the same
 *      in all <code>Locale</code>s. </li>
 * <li> A localized operation name that may be used as a synonym for
 *      the global operation name. </li>
 * <li> The name of the vendor defining this operation. </li>
 * <li> A brief description of this operation. </li>
 * <li> An URL where additional documentation on this operation may be
 *      found. </li>
 * <li> The version of this operation. </li>
 * </ul>
 * Additional information must be provided when appropriate.  Only then
 * can this operation be added to an <code>OperationRegistry</code>.
 * Furthermore, it is recommended that a detailed description of the
 * operation's functionality be included in the class comments.
 *
 * <p> JAI currently knows about the following operation modes :
 * "rendered", "renderable", "collection" and "renderableCollection"
 * (these form a subset of the known registry modes returned by
 * <code>RegistryMode.getModes()</code>). All mode names are dealt
 * with in a case insensitive (but retentive) manner. All modes have
 * to accept the same number of source images and the same number of
 * parameters. All the source names and parameter names are also the
 * same across all modes. The class types of the sources and parameters
 * can be different for each mode.
 *
 * <p> For example an operation supporting the "rendered" mode
 * takes <code>RenderedImage</code>s as its sources, can only
 * be used in a rendered operation chain, and produces a
 * <code>RenderedImage</code>. An operation supporting the renderable
 * mode takes <code>RenderableImage</code>s as its sources, can
 * only be used in a renderable operation chain, and produces a
 * <code>RenderableImage</code>.
 *
 * @see JAI
 * @see OperationDescriptorImpl
 *
 */
public interface OperationDescriptor extends RegistryElementDescriptor {

    /**
     * An <code>Object</code> that signifies that
     * a parameter has no default value. Same as
     * <code>ParameterListDescriptor.NO_PARAMETER_DEFAULT</code>
     */
    public static final Object NO_PARAMETER_DEFAULT =
                    ParameterListDescriptor.NO_PARAMETER_DEFAULT;

    /**
     * Returns the resource data for this operation in the specified
     * <code>Locale</code>.  It must contain <code>String</code> data
     * for the following tags:
     * <ul>
     * <li> "GlobalName" - A global operation name that is visible to all
     *      and is the same in all <code>Locale</code>s. </li>
     * <li> "LocalName" - A localized operation name that may be used as a
     *      synonym for the "GlobalName". </li>
     * <li> "Vendor" - The name of the vendor defining this operation.
     *                 Vendors are encouraged to use the Java convention
     *                 of reversed Internet addresses. </li>
     * <li> "Description" - A brief description of this operation. </li>
     * <li> "DocURL" - An URL where additional documentation on this
     *      operation may be found. </li>
     * <li> "Version" - A free-form version indicator of this operation. </li>
     * </ul>
     * In addition, it may contain <code>String</code> data for the
     * following tags when appropriate:
     * <ul>
     * <li> "arg0Desc", "arg1Desc", ... - Description of the input
     *      parameters. </li>
     * <li> "hint0Desc", hint1Desc", ... - Description of the rendering
     *      hints. </li>
     * </ul>
     *
     * @param locale  The <code>Locale</code> for which the information
     *        should be localized.  It may be different from the default
     *        <code>Locale</code>.
     *
     * @return  A two-dimensional array of <code>String</code>s containing
     *          the mandatory and optional resource tags and their
     *          corresponding resource data. (String[i][0] is
     *	        the tag for the i-th resource and String[i][1] is the
     *	        corresponding data)
     */
    String[][] getResources(Locale locale);

    /**
     * Returns the resource data for this operation in the specified
     * <code>Locale</code> in a <code>ResourceBundle</code>.  The
     * resource data values are taken from the
     * <code>getResources()</code> method which must be implemented
     * by each operation descriptor.
     *
     * @param locale  The <code>Locale</code> for which the information
     *        should be localized.  It may be different from the default
     *        <code>Locale</code>.
     *
     * @return  A <code>ResourceBundle</code> containing the mandatory
     *          and optional resource information.
     */
    ResourceBundle getResourceBundle(Locale locale);

    /**
     * Returns the number of sources required by this operation.
     * All modes have the same number of sources.
     */
    int getNumSources();

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
    Class[] getSourceClasses(String modeName);

    /**
     * Returns an array of <code>String</code>s that are the names
     * of the sources of this operation.  If this operation has no
     * sources, this method returns <code>null</code>.
     *
     * @since JAI 1.1
     */
    String[] getSourceNames();

    /**
     * Returns a <code>Class</code> that describes the type of
     * destination this operation produces for the specified mode.
     *
     * @param modeName the operation mode name
     *
     * @throws IllegalArgumentException if modeName is <code>null</code>
     *		or if it is not one of the supported modes.
     *
     * @since JAI 1.1
     */
    Class getDestClass(String modeName);

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
     * @throws IllegalArgumentException if modeName is <code>null</code>
     *
     * @since JAI 1.1
     */
    boolean validateArguments(String modeName, ParameterBlock args, StringBuffer msg);

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
     */
    boolean isImmediate();

    /************************ Generic Methods ************************/

    /**
     * Calculates the region over which two distinct renderings
     * of an operation may be expected to differ.
     *
     * <p> The class of the returned object will vary as a function of
     * the mode of the operation.  For rendered and renderable two-
     * dimensional images this should be an instance of a class which
     * implements <code>java.awt.Shape</code>.
     *
     * @param registryModeName The name of the mode.
     * @param oldParamBlock The previous sources and parameters.
     * @param oldHints The previous hints.
     * @param newParamBlock The current sources and parameters.
     * @param newHints The current hints.
     * @param node The affected node in the processing chain.
     *
     * @return The region over which the data of two renderings of this
     *         operation may be expected to be invalid or <code>null</code>
     *         if there is no common region of validity.  If an empty
     *         <code>java.awt.Shape</code> is returned, this indicates
     *         that all pixels within the bounds of the old rendering
     *         remain valid.
     *
     * @throws IllegalArgumentException if <code>registryModeName</code>
     *         is <code>null</code> or if the operation requires either
     *         sources or parameters and either <code>oldParamBlock</code>
     *         or <code>newParamBlock</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>oldParamBlock</code> or
     *         <code>newParamBlock</code> do not contain sufficient sources
     *         or parameters for the operation in question.
     *
     * @since JAI 1.1
     */
    Object getInvalidRegion(String registryModeName,
                            ParameterBlock oldParamBlock,
                            RenderingHints oldHints,
                            ParameterBlock newParamBlock,
                            RenderingHints newHints,
                            OperationNode node);
    
    /********************** DEPRECATED METHODS *************************/

    // All mode specific methods are deprecated since JAI 1.1
    // in favor of the equivalent methods which accept a modeName
    // as a parameter.

    /**
     * Returns an array of <code>PropertyGenerator</code>s implementing
     * the property inheritance for this operation.  They may be used
     * as a basis for the operation's property management.
     *
     * @return  An array of <code>PropertyGenerator</code>s, or
     *          <code>null</code> if this operation does not have any of
     *          its own <code>PropertyGenerator</code>s.
     *
     * @deprecated as of JAI 1.1 in favor of the equivalent method
     *	that specifies the mode name.
     */
    PropertyGenerator[] getPropertyGenerators();

    /********************** Rendered Mode Methods (deprecated) *********/

    /**
     * Returns <code>true</code> if this operation supports the rendered
     * image mode. That is, it may be performed on <code>RenderedImage</code>
     * sources in a rendered operation chain, and produces a rendered result.
     * The <code>JAI.create()</code> and the 
     * <code>JAI.createCollection()</code> methods should be used to 
     * instantiate the operation.     
     *
     * <p> If this method returns <code>true</code>, all the additional
     * methods that supply the rendered mode information must be
     * implemented.
     *
     * @deprecated as of JAI 1.1 in favor of <code>isModeSupported("rendered")</code>
     */
    boolean isRenderedSupported();

    /**
     * Returns an array of <code>Class</code>es that describe the types
     * of sources required by this operation in the rendered image mode.
     * If this operation has no source, this method returns <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getSourceClasses("rendered")</code>
     */
    Class[] getSourceClasses();

    /**
     * Returns a <code>Class</code> that describes the type of
     * destination this operation produces in the rendered image
     * mode.  Currently JAI supports two destination class types:
     * <code>java.awt.image.RenderedImage.class</code> and
     * <code>java.util.Collection.class</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getDestClass("rendered")</code>
     */
    Class getDestClass();

    /**
     * Returns <code>true</code> if this operation is capable of
     * handling the input rendered source(s) and/or parameter(s)
     * specified in the <code>ParameterBlock</code>, or
     * <code>false</code> otherwise, in which case an explanatory
     * message may be appended to the <code>StringBuffer</code>.
     *
     * <p> This method is the standard place where input arguments are
     * validated against this operation's specification for the rendered
     * mode.  It is called by <code>JAI.create()</code> as a part of its
     * validation process.  Thus it is strongly recommended that the
     * application programs use the <code>JAI.create()</code> methods to
     * instantiate all the rendered operations.
     *
     * <p> This method sets all the undefined parameters in the
     * <code>ParameterBlock</code> to their default values, if the default
     * values are specified.
     *
     * <p> Note that <code>DeferredData</code> parameters will not be
     * recognized as valid unless the parameter is defined to have class
     * <code>DeferredData.class</code>.
     *
     * @param args  Input arguments, including source(s) and/or parameter(s).
     * @param msg  A string that may contain error messages.
     *
     * @deprecated as of JAI 1.1 in favor of <code>validateArguments("rendered", ...)</code>
     */
    boolean validateArguments(ParameterBlock args, StringBuffer msg);
    

    /********************* Renderable Mode Methods (deprecated) ********/

    /**
     * Returns <code>true</code> if this operation supports the renderable
     * image mode. That is, it may be performed on <code>RenderableImage</code>
     * sources in a renderable operation chain, and produces a renderable
     * result.  The <code>JAI.createRenderable()</code> and the
     * <code>JAI.createCollection()</code> methods should be used to
     * instantiate the operation.
     *
     * <p> If this method returns <code>true</code>, all the additional
     * methods that supply the renderable mode information must be
     * implemented.
     *
     * @deprecated as of JAI 1.1 in favor of <code>isModeSupported("renderable")</code>
     */
    boolean isRenderableSupported();

    /**
     * Returns an array of <code>Class</code>es that describe the types
     * of sources required by this operation in the renderable image mode.
     * If this operation does not support the renderable mode, or if it
     * has no source, this method returns <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getSourceClasses("renderable")</code>
     */
    Class[] getRenderableSourceClasses();

    /**
     * Returns a <code>Class</code> that describes the type of
     * destination this operation produces in the renderable image
     * mode.  Currently JAI supports two destination class types:
     * <code>java.awt.image.renderable.RenderableImage.class</code> and
     * <code>java.util.Collection.class</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getDestClass("renderable")</code>
     */
    Class getRenderableDestClass();

    /**
     * Returns <code>true</code> if this operation is capable of handling
     * the input renderable source(s) and/or parameter(s) specified
     * in the <code>ParameterBlock</code>, or <code>false</code>
     * otherwise, in which case an explanatory message may be appended
     * to the <code>StringBuffer</code>.
     *
     * <p> This method is the standard place where input arguments are
     * validated against this operation's specification for the renderable
     * mode.  It is called by <code>JAI.createRenderable()</code> as a
     * part of its validation process.  Thus it is strongly recommended
     * that the application programs use the
     * <code>JAI.createRenderable()</code> method to instantiate all
     * the renderable operations.
     *
     * <p> This method sets all the undefined parameters in the
     * <code>ParameterBlock</code> to their default values, if the default
     * values are specified.
     *
     * <p> Note that <code>DeferredData</code> parameters will not be
     * recognized as valid unless the parameter is defined to have class
     * <code>DeferredData.class</code>.
     *
     * <p> If this operation does not support the renderable mode,
     * this method returns <code>false</code> regardless of the input
     * arguments
     *
     * @param args  Input arguments, including source(s) and/or parameter(s).
     * @param msg  A string that may contain error messages.
     *
     * @deprecated as of JAI 1.1 in favor of <code>validateArguments("renderable", ...)</code>
     */
    boolean validateRenderableArguments(ParameterBlock args, StringBuffer msg);


    /************************ Parameter Methods (deprecated) ***********/

    /**
     * Returns the number of parameters (not including the sources)
     * required by this operation.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getNumParameters()</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     */
    int getNumParameters();

    /**
     * Returns an array of <code>Class</code>es that describe the types
     * of parameters required by this operation.  If this operation
     * has no parameter, this method returns <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamClasses()</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     */
    Class[] getParamClasses();

    /**
     * Returns an array of <code>String</code>s that are the localized
     * parameter names of this operation.  If this operation has no
     * parameter, this method returns <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamNames()</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     */
    String[] getParamNames();

    /**
     * Returns an array of <code>Object</code>s that define the default
     * values of the parameters for this operation.  Default values may
     * be <code>null</code>.  When instantiating the operation, the
     * default values may be used for those parameters whose values are
     * not supplied.  The <code>NO_PARAMETER_DEFAULT</code> static
     * <code>Object</code> indicates that a parameter has no default
     * value.  If this operation has no parameter, this method returns
     * <code>null</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamDefaults()</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     */
    Object[] getParamDefaults();

    /**
     * Returns the default value of a specified parameter.  The default
     * value may be <code>null</code>.  If a parameter has no default
     * value, this method returns <code>NO_PARAMETER_DEFAULT</code>.
     *
     * @param index  The index of the parameter whose default
     *        value is queried.
     *
     * @throws NullPointerException if this operation has no parameter.
     * @throws ArrayIndexOutOfBoundsException if there is no parameter
     *         corresponding to the specified <code>index</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamDefaultValue()</code>
     *      This will for the time being return the above value for
     *      modeName = getSupportedModes()[0]
     */
    Object getParamDefaultValue(int index);

    /**
     * Returns the minimum legal value of a specified numeric parameter
     * for this operation.  If the specified parameter is non-numeric,
     * this method returns <code>null</code>.
     *
     * <p> The return value should be of the class type appropriate for
     * the parameter's type, that is, <code>Byte</code> for a
     * <code>byte</code> parameter, <code>Integer</code> for an
     * <code>int</code> parameter, and so forth.
     *
     * @param index  The index of the numeric parameter whose minimum
     *        value is queried.
     *
     * @return  A <code>Number</code> representing the minimum legal value
     *          of the queried parameter, or <code>null</code>.
     *
     * @throws NullPointerException if this operation has no parameter.
     * @throws ArrayIndexOutOfBoundsException if there is no parameter
     *         corresponding to the specified <code>index</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamValueRange()</code>
     *      This will for the time being return "getMinValue" of the above
     *      return value for modeName = getSupportedModes()[0]
     */
    Number getParamMinValue(int index);

    /**
     * Returns the maximum legal value of a specified numeric parameter
     * for this operation.  If the specified parameter is non-numeric,
     * this method returns <code>null</code>.
     *
     * <p> The return value should be of the class type appropriate for
     * the parameter's type, that is, <code>Byte</code> for a
     * <code>byte</code> parameter, <code>Integer</code> for an
     * <code>int</code> parameter, and so forth.
     *
     * @param index  The index of the numeric parameter whose maximum
     *        value is queried.
     *
     * @return  A <code>Number</code> representing the maximum legal value
     *          of the queried parameter, or <code>null</code>.
     *
     * @throws NullPointerException if this operation has no parameter.
     * @throws ArrayIndexOutOfBoundsException if there is no parameter
     *         corresponding to the specified <code>index</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>
     *      getParameterListDescriptor(modeName).getParamValueRange()</code>
     *      This will for the time being return "getMaxValue" of the above
     *      return value for modeName = getSupportedModes()[0]
     */
    Number getParamMaxValue(int index);
}
