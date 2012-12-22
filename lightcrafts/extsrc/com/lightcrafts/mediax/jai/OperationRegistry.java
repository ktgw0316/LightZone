/*
 * $RCSfile: OperationRegistry.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:13 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.PropertyUtil;
import com.lightcrafts.media.jai.util.Service;
import java.awt.RenderingHints;
import java.awt.image.renderable.ContextualRenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import com.lightcrafts.mediax.jai.registry.CIFRegistry;
import com.lightcrafts.mediax.jai.registry.CRIFRegistry;
import com.lightcrafts.mediax.jai.registry.RIFRegistry;
import com.lightcrafts.mediax.jai.registry.CollectionRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;
import com.lightcrafts.mediax.jai.util.ImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * A class responsible for maintaining a registry of various types of
 * factory objects and preferences among them. The operation registry
 * hierarchy looks as follows
 *
 * <pre>
 *
 *                                                      |-object1-
 *                                           |-product1-|-object2-
 *                             |-descriptor1-|          |-object3-
 *                             |             |
 *                             |             |          |-object1-
 *                             |             |-product2-|-object2-
 *                     |-mode1-|                        |-object3-
 *                     |       |
 * |-OperationRegistry-|       |                        |-object1-
 *                     |       |             |-product1-|-object2-
 *                     |       |-descriptor2-|          |-object3-
 *                     |                     |
 *                     |                     |          |-object1-
 *                     |                     |-product2-|-object2-
 *                     |                                |-object3-
 *                     |
 *                     |-mode2-|-descriptor1-|-object1--
 *                             |
 *                             |-descriptor2-|-object1--
 * </pre>
 *
 * <p> The <code>OperationRegistry</code> class maps a descriptor name
 * (for example, an image operation name) into the particular kind of
 * factory object requested, capable of implementing the functionality
 * described by the descriptor. The mapping is constructed in several
 * stages:
 *
 * <p> At the highest level all objects are registered against some mode.
 * A mode is specified by a <code>String</code> which must be one
 * of those returned by <code>RegistryMode.getModeNames()</code>.
 * Examples of known registry modes include "rendered", "renderable",
 * "collection", "renderableCollection", "tileEncoder", "tileDecoder",
 * "remoteRendered", "remoteRenderable", etc.
 *
 * <p> Each registry mode is associated with a <code>
 * RegistryElementDescriptor</code> which describes some functionality
 * to be implemented by factory objects associated with this
 * descriptor. For example, the "rendered" registry mode is associated
 * with <code>OperationDescriptor.class</code> and "tileEncoder"
 * is associated with <code>TileCodecDescriptor.class</code>.
 * Different registry modes can share the same <code>
 * RegistryElementDescriptor</code>. For example "rendered", "renderable"
 * (and other image operation registry modes) are all associated with
 * <code>OperationDescriptor.class</code>.
 *
 * <p> If a registry mode supports preferences (for example "rendered",
 * "tileEncoder" etc.), then the hierarchy of objects registered under
 * that mode looks like that of "mode1" above. Descriptors are first
 * registered against all modes that the specific instance supports. Each
 * factory object that implements the functionality specified by that
 * descriptor is registered against some product (name) under that
 * descriptor. Preferences can be set among products under a given
 * descriptor or among objects under a specific product/descriptor.
 *
 * <p> The ordering of such factory objects is determined by the order
 * of the products attached to an <code>OperationDescriptor</code>,
 * and by the order of the factory objects within each product. The
 * orders are established by setting pairwise preferences, resulting in
 * a partial order which is then sorted topologically. The results of
 * creating a cycle are undefined.
 *
 * <p> The ordering of factory objects within a product is intended to
 * allow vendors to create complex "fallback" chains. An example would
 * be installing a <code>RenderedImageFactory</code> that implements
 * separable convolution ahead of a <code>RenderedImageFactory</code>
 * that implements a more general algorithm.
 *
 * <p> If a registry mode does not support preferences (for example,
 * "renderable", "remoteRenderable" etc.) then the hierarchy of objects
 * registered under that mode looks like that of "mode2" above. Only a
 * single factory object belonging to this mode can be associated with a
 * given descriptor. If multiple objects are registered under the same
 * descriptor, the last one registered is retained.
 *
 * <p> The <code>OperationRegistry</code> has several methods to manage this
 * hierarchy, which accept a <code>modeName</code> and work with <code>
 * Object</code>s. The preferred manner of usage is through the type-safe
 * wrapper class which are specific to each mode (for example <code>
 * RIFRegistry</code>, <code>CRIFRegistry</code> etc.)
 *
 * <p> Vendors are encouraged to use unique product names (by means
 * of the Java programming language convention of reversed Internet
 * addresses) in order to maximize the likelihood of clean installation.
 * See <i>The Java Programming Language</i>, &sect;10.1 for a discussion
 * of this convention in the context of package naming.
 *
 * <p> Users will, for the most part, only wish to set ordering
 * preferences on the product level, since the factory object level
 * orderings will be complex. However, it is possible for a knowledgable
 * user to insert a specific factory object into an existing product for
 * tuning purposes.
 *
 * <p> The <code>OperationRegistry</code> also has the responsibility
 * of associating a set of <code>PropertyGenerators</code>
 * with each descriptor. This set will be coalesced
 * into a <code>PropertySource</code> suitable for
 * use by the getPropertySource() method. If several
 * <code>PropertyGenerator</code>s associated with a particular
 * descriptor generate the same property, only the last one to be
 * registered will have any effect.
 *
 * <p> The registry handles all names (except class names) in a
 * case-insensitive but retentive manner.
 *
 *
 * <p><strong>Initialization and automatic loading of registry objects. </strong>
 *
 * <p> The user has two options for automatic loading of registry
 * objects.
 * <ul>
 * <li> For most normal situations the user can create a
 *      "<code>registryFile.jai</code>" with entries for operators and preferences
 *      specific to their packages. This registry file must be put it in
 *      the META-INF directory of the jarfile or classpath.
 *
 * <li> For situations where more control over the operation registry is
 *      needed, (for example remove an operator registered by JAI etc.) the
 *      user can implement a concrete sub-class of the <code>
 *	OperationRegistrySpi</code> interface
 *      and register it as service provider (see <code>
 *      OperationRegistrySpi</code> for more details). The <code>updateRegistry
 *      </code> method of such registered service providers will be called
 *      with the default <code>OperationRegistry</code> of <code>JAI</code>
 *      once it has been initialized.
 * </ul>
 *
 * <p> The initialization of the <code>OperationRegistry</code>
 * of the default instance of <code>JAI</code> happens as follows
 * <ol>
 * <li> Load the JAI distributed registry file
 *      "<code>META-INF/com.lightcrafts.mediax.jai.registryFile.jai</code>" (from jai_core.jar)
 *
 * <li> Find and load all "<code>META-INF/registryFile.jai</code>" files found
 *      in the classpath in some arbitrary order.
 *
 * <li> Look for registered service providers of <code>OperationRegistrySpi</code>
 *	listed in all "<code>META-INF/services/com.lightcrafts.mediax.jai.OperationRegistrySpi</code>"
 *	files found in the classpath and call their <code>updateRegistry</code>
 *      method passing in the default OperationRegistry. The order of these
 *      calls to <code>updateRegistry</code> is arbitrary.
 * </ol>
 *
 * Note that the user should not make any assumption about the order
 * of loading WITHIN step 2 or 3. If there is a need for the
 * <code>updateRegistry</code> method to be called right after the associated
 * registryFile.jai is read in, the following could be done.
 *
 * <p> The user could give the registry file a package qualified name
 * for e.g xxx.yyy.registryFile.jai and put this in the META-INF
 * directory of the jar file. Then in the concrete class that implements
 * <code>OperationRegistrySpi</code>
 *
 * <pre>
 *  void updateRegistry(OperationRegistry or) {
 *      String registryFile = "META-INF/xxx.yyy.registryFile.jai";
 *      InputStream is = ClassLoader.getResourceAsStream(registryFile);
 *
 *      or.updateFromStream(is);
 *
 *      // Make other changes to "or" ...
 *  }
 * </pre>
 *
 * For information on the format of the registry file, see the
 * <a href="{@docRoot}/serialized-form.html#com.lightcrafts.mediax.jai.OperationRegistry">
 * serialized form</a> of the <code>OperationRegistry</code>.
 *
 * @see OperationRegistrySpi
 * @see RegistryMode
 * @see RegistryElementDescriptor
 */
public class OperationRegistry implements Externalizable {

    /** The JAI packaged registry file */
    // static String JAI_REGISTRY_FILE = "META-INF/com.lightcrafts.mediax.jai.registryFile.jai";
    static String JAI_REGISTRY_FILE = "LCregistryFile.jai";

    /** The user defined registry files that are automatically loaded */
    // static String USR_REGISTRY_FILE = "META-INF/registryFile.jai";
    static String USR_REGISTRY_FILE = "LCregistryFile.jai";

    /**
     * A <code>Hashtable</code> of <code>DescritptorCache</code>s
     * for each registry mode.
     */
    private Hashtable descriptors;

    /**
     * A <code>Hashtable</code> of <code>FactoryCache</code>s
     * for each registry mode.
     */
    private Hashtable factories;

    /**
     * Get the <code>FactoryCache</code> associated with a specified
     * mode. If it does not exist but the mode is a valid registry mode
     * then silently create one.
     */
    private FactoryCache getFactoryCache(String modeName) {

	CaselessStringKey key = new CaselessStringKey(modeName);

	FactoryCache fc = (FactoryCache)factories.get(key);

	if (fc == null) {

	    if (RegistryMode.getMode(modeName) != null) {
		factories.put(key, fc = new FactoryCache(modeName));

	    } else {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationRegistry0", new Object[] {modeName}));
	    }
	}

	return fc;
    }

    /**
     * Get the <code>DescriptorCache</code> associated with a specified
     * mode. If it does not exist but the mode is a valid registry mode
     * then silently create one.
     */
    private DescriptorCache getDescriptorCache(String modeName) {

	CaselessStringKey key = new CaselessStringKey(modeName);

	DescriptorCache dc = (DescriptorCache)descriptors.get(key);

	if (dc == null) {

	    if (RegistryMode.getMode(modeName) != null) {
		descriptors.put(key, dc = new DescriptorCache(modeName));

	    } else {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationRegistry0", new Object[] {modeName}));
	    }
	}

	return dc;
    }

    /**
     * Initialize all the internal OperationRegistry fields.
     *
     * Note that this is not synchronized. It is the caller's
     * reponsiblity to make sure that thread safety is maintained.
     */
    private void initialize() {

	// Create a Hashtable to hold a DescriptorCache for each
	// known registry mode.
	descriptors = new Hashtable();

	// Create a Hashtable to hold a FactoryCache for each
	// known registry mode.
	factories   = new Hashtable();
    }

    /**
     * Default Constructor. The <code>OperationRegistry</code> created is
     * <u>not</u> thread-safe. Note that none of the automatic loading
     * of registry files or services happens here.
     *
     * @see #getThreadSafeOperationRegistry
     */
    public OperationRegistry() {
	initialize();
    }

    /**
     * Creates and returns a new thread-safe version of the
     * <code>OperationRegistry</code> which uses reader-writer locks to
     * wrap every method with a read or a write lock as appropriate.
     * Note that none of the automatic loading of registry files or
     * services is done on this <code>OperationRegistry</code>.
     *
     * @since JAI 1.1
     */
    public static OperationRegistry getThreadSafeOperationRegistry() {
	return new ThreadSafeOperationRegistry();
    }

    /**
     * Creates a new thread-safe <code>OperationRegistry</code>. It
     * is initialized by first reading in the system distributed
     * registry file (<code>JAI_REGISTRY_FILE</code>), then the user
     * installed registry files (<code>USR_REGISTRY_FILE</code>) and
     * then by calling the <code>updateRegistry()</code> method of all
     * registered service providers.
     *
     * @return a properly initialized thread-safe
     *		<code>OperationRegistry</code>
     */
    static OperationRegistry initializeRegistry() {
	try {
            // TODO: this has been hacked...
            // InputStream url = PropertyUtil.getFileFromClasspath(JAI_REGISTRY_FILE);
            InputStream url = JAI.class.getResource(JAI_REGISTRY_FILE).openStream();

            if (url == null) {
		throw new RuntimeException(JaiI18N.getString("OperationRegistry1"));
	    }

	    OperationRegistry registry = new ThreadSafeOperationRegistry();

	    if (url != null)
		RegistryFileParser.loadOperationRegistry(registry, null, url);

	    registry.registerServices(null);
	    return registry;

	} catch (IOException ioe) {
            ImagingListener listener =
                JAI.getDefaultInstance().getImagingListener();
            String message = JaiI18N.getString("OperationRegistry2");
            listener.errorOccurred(message,
                                   new ImagingException(message, ioe),
                                   OperationRegistry.class, false);
            return null;

//	    ioe.printStackTrace();
//	    throw new RuntimeException(
//			JaiI18N.getString("OperationRegistry2"));
	}
    }

    /**
     * Returns a String representation of the registry.
     *
     * @return  the string representation of this <code>OperationRegistry</code>.
     */
    public String toString() {

	StringWriter sw = new StringWriter();

	try {
	    RegistryFileParser.writeOperationRegistry(
				    this, new BufferedWriter(sw));
	    return sw.getBuffer().toString();

	} catch (Exception e) {
	    return "\n[ERROR!] " + e.getMessage();
	}
    }

    /**
     * Writes out the contents of the <code>OperationRegistry</code> to
     * a stream as specified in the <code>writeExternal</code> method.
     * For more information see the
     * <a href="{@docRoot}/serialized-form.html#com.lightcrafts.mediax.jai.OperationRegistry"> serialized form</a>.
     *
     * @param out  The OutputStream to which the <code>OperationRegistry</code>
     *             state is written.
     *
     * @throws IllegalArgumentException if out is null.
     *
     * @see #writeExternal
     */
    public void writeToStream(OutputStream out) throws IOException {
	if (out == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	RegistryFileParser.writeOperationRegistry(this, out);
    }

    /**
     * Initializes the <code>OperationRegistry</code> from an
     * <code>InputStream</code>. All non-IO exceptions encountered while
     * parsing the registry files are caught and their error messages are
     * redirected to <code>System.err</code>. If <code>System.err</code>
     * is null the error messages will never be seen.
     *
     * <p> The <code>InputStream</code> passed in will not be closed by
     * this method, the caller should close the <code>InputStream</code>
     * when it is no longer needed.
     *
     * <p>The format of the data from the <code>InputStream</code> is
     * expected to be the same as that of the <code>writeExternal</code>
     * method as specified in the
     * <a href="{@docRoot}/serialized-form.html#com.lightcrafts.mediax.jai.OperationRegistry"> serialized form</a>.
     *
     * @param in   The <code>InputStream</code> from which to read the data.
     *
     * @throws IllegalArgumentException if in is null.
     *
     * @see #writeExternal
     */
    public void initializeFromStream(InputStream in) throws IOException {

	if (in == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	initialize();
	updateFromStream(in);
    }

    /**
     * Updates the current <code>OperationRegistry</code> with the
     * operations specified by the given <code>InputStream</code>.
     * All non-IO exceptions encountered while parsing the registry
     * files are caught and their error messages are redirected to
     * <code>System.err</code>. If <code>System.err</code> is null the
     * error messages will never be seen.
     *
     * <p> The <code>InputStream</code> passed in will not be closed by
     * this method, the caller should close the <code>InputStream</code>
     * when it is no longer needed.
     *
     * <p>The format of the data from the <code>InputStream</code> is
     * expected to be the same as that of the <code>writeExternal</code>
     * method as specified in the
     * <a href="{@docRoot}/serialized-form.html#com.lightcrafts.mediax.jai.OperationRegistry"> serialized form</a>.
     *
     * @param in   The <code>InputStream</code> from which to read the data.
     *
     * @throws IllegalArgumentException if in is null.
     *
     * @see #writeExternal
     *
     * @since JAI 1.1
     */
    public void updateFromStream(InputStream in) throws IOException {

	if (in == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	RegistryFileParser.loadOperationRegistry(this, null, in);
    }

    /**
     * Restores the contents of the registry from an ObjectInput which
     * was previously written using the <code>writeExternal</code>
     * method.
     *
     * <p>All non-IO exceptions encountered while parsing the registry
     * files are caught and their error messages are redirected to
     * <code>System.err</code>. If <code>System.err</code> is null the
     * error messages will never be seen.
     *
     * @param in   An ObjectInput from which to read the data.
     *
     * @serialData The format of the data from the ObjectInput
     * is expected to be the same as that written out by the
     * <code>writeExternal</code> method. For more information see
     * <a href="{@docRoot}/serialized-form.html#com.lightcrafts.mediax.jai.OperationRegistry"> serialized form</a>.
     * The current implementation is backward compatible with the old
     * JAI 1.0.2 registry file streams.
     *
     * @throws IllegalArgumentException if in is null.
     *
     * @see #writeExternal
     */
    public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {

	if (in == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	byte barray[] = (byte[])in.readObject();
	InputStream s = new ByteArrayInputStream(barray);
	initializeFromStream(s);
    }

    /**
     * Saves the contents of the registry as described in the
     * <a href="{@docRoot}/serialized-form.html#com.lightcrafts.mediax.jai.OperationRegistry"> serialized form</a>.
     *
     * @param out   An ObjectOutput to which to write the data.
     *
     * @throws IllegalArgumentException if out is null.
     *
     * @serialData The format of the data written to the stream is as
     * follows. Each line in the stream can be in one of the formats
     * described below. Space or tab characters seperate keywords in
     * each line. The comment character is <tt>'#'</tt> (<tt>0x23</tt>);
     * on each line all characters following the first comment character
     * are ignored. The stream must be encoded in UTF-8.
     *
     * <p><ol>
     *
     * <li> To register descriptors :<p>
     *
     * <b>descriptor</b> <i>{descriptor-class-name}</i><p>
     * <b>odesc</b>	 <i>{descriptor-class-name}  {descriptor-name}</i><p>
     *
     * <p>The second version above is deprecated and is retained for backward
     * compatibility with JAI 1.0.2. Descriptors are always registered
     * against <i>{descriptor-class}.getName()</i>. The <i>{descriptor-name}</i> in the
     * second version is always ignored.<p>
     *
     * <li> To register factory objects under a product against a specific mode :<p>
     *
     * <b>{registry-mode-name}</b>  <i>{factory-class-name}	{product-name}	{descriptor-name}   {local-name}</i><p>
     * <b>{registry-mode-name}</b>  <i>{factory-class-name}	{descriptor-name}</i><p>
     *
     * <p>The first version above is used to register factory objects against
     * modes that support preferences. The second version is used for those
     * that do not support preferences. <i>{local-name},</i> is an arbitrary name that
     * is unique for a given mode. This is (only) used later on in this file
     * to set preferences between factory objects. See class comments
     * for {@link OperationRegistry} for a discussion on product names.<p>
     *
     * <li> To set preferences between products for a descriptor under a
     * specific mode :<p>
     *
     * <b>prefProduct	{registry-mode-name}</b>	<i>{descriptor-name}   {preferred-product-name} {other-product-name}</i><p>
     * <b>pref	product</b>	<i>{descriptor-name}   {preferred-product-name} {other-product-name}</i><p>
     *
     * <p>The second version above is deprecated and is retained for backward
     * compatibility with JAI 1.0.2. This version is assumed to set
     * product preferences for the <i>"rendered"</i> mode.<p>
     *
     * <li> To set preferences between factory objects for descriptor under a
     * a specific product and registry mode :<p>
     *
     * <b>pref</b>  <i>{registry-mode-name}  {descriptor-name}	{product-name}	{preferred-factory-local-name}	{other-factory-local-name}</i><p>
     * </ol>
     *
     * <p>For example, the stream contents for an "addconst" image operation
     * descriptor might look like this :
     *
     * <pre>
     *    descriptor  com.lightcrafts.mediax.jai.operator.AddConstDescriptor
     *
     *    rendered    com.lightcrafts.media.jai.opimage.AddConstCRIF   com.lightcrafts.media.jai   addconst   sunaddconstrif
     *    rendered    com.lightcrafts.media.jai.mlib.MlibAddConstRIF   com.lightcrafts.media.jai   addconst   mlibaddconstrif
     *
     *    renderable  com.lightcrafts.media.jai.opimage.AddConstCRIF   addconst
     *
     *    pref        rendered   addconst   com.lightcrafts.media.jai   mlibaddconstrif   sunaddconstrif
     * </pre>
     *
     * The above does the following :
     * <ul>
     * <li> register a descriptor for the "addconst" operator. </li>
     * <li> registers two <i>rendered</i> image factories. </li>
     * <li> register one <i>renderable</i> image factory. </li>
     * <li> prefer the MlibAddConstRIF factory over the AddConstCRIF
     * factory for the <i>rendered</i> mode.</li>
     *
     * <p><strong>Note that JAI 1.0.2 will not be able to read the
     * new version of the registry file streams</strong>.
     */
    public void writeExternal(ObjectOutput out) throws IOException {

	if (out == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	ByteArrayOutputStream bstream = new ByteArrayOutputStream();
	writeToStream(bstream);
	out.writeObject(bstream.toByteArray());
    }

    /********************** NEW JAI 1.1 methods *************************/

    /**
     * Remove a registry mode (including pre-defined JAI modes) from
     * the OperationRegistry. When a mode is removed, all associated descriptors
     * are also removed unless associated with another mode. Note
     * that this does <u>not</u> unregister or remove this mode from
     * <code>RegistryMode</code>
     *
     * Also note that a registry mode need not be explicitly added to
     * the <code>OperationRegistry</code>. All modes registered under
     * <code>RegistryMode</code> are automatically recognized by the
     * <code>OperationRegistry</code>.
     *
     * @throws IllegalArgumentException if modeName is <code>null</code>
     *          or if the modeName is not one of the modes returned
     *          <code>RegistryMode.getModes()</code>
     *
     * @since JAI 1.1
     */
    public void removeRegistryMode(String modeName) {

	if (getDescriptorCache(modeName) != null)
	    descriptors.remove(new CaselessStringKey(modeName));

	if (getFactoryCache(modeName) != null)
	    factories.remove(new CaselessStringKey(modeName));
    }

    /**
     * Get's the list of known registry modes known to the
     * <code>OperationRegistry</code>. This might not be all
     * modes listed in <code>RegistryMode.getModeNames()</code>.
     *
     * @since JAI 1.1
     */
    public String[] getRegistryModes() {

	Enumeration e = descriptors.keys();
	int size = descriptors.size();
	String names[] = new String[size];

	for (int i = 0; i < size; i++) {
	    CaselessStringKey key = (CaselessStringKey)e.nextElement();
	    names[i] = key.getName();
	}

	return names;
    }

    //////////////////
    //
    // Set of methods to register/unregister descriptors
    // with the operation registry.

    /**
      * Register a descriptor against all the registry modes it
      * supports. The "descriptor" must be an instance of the
      * RegistryMode.getDescriptorClass() The "descriptor" is keyed on
      * descriptor.getName(). Only one descriptor can be registered
      * against a descriptor name for a given mode.
      *
      * @param descriptor an instance of a concrete sub-class of <code>
      *     RegistryElementDescriptor</code>
      *
      * @throws IllegalArgumentException is descriptor is <code>null</code>
      * @throws IllegalArgumentException if any of the modes returned
      *     by <code>descriptor.getSupportedModes()</code> is not one
      *     of those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if another descriptor with the
      *	    same name has already been registered against any of the
      *	    modes supported by this descriptor.
      *
      * @since JAI 1.1
      */
    public void registerDescriptor(RegistryElementDescriptor descriptor) {
	if (descriptor == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	String[] supportedModes = descriptor.getSupportedModes();

	String descriptorName = descriptor.getName();

	// First make sure that all supported modes are legal registry
	// modes.
	for (int i = 0; i < supportedModes.length; i++) {
	    if (RegistryMode.getMode(supportedModes[i]) == null)
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationRegistry3",
			new Object[] {descriptorName, supportedModes[i]}));
	}

	// Now register the descriptor against each supported mode.
	for (int i = 0; i < supportedModes.length; i++) {

	    DescriptorCache dc = getDescriptorCache(supportedModes[i]);

	    dc.addDescriptor(descriptor);
	}
    }

    /**
      * Unregister a descriptor against all its supported modes from the
      * operation registry.
      *
      * @param descriptor an instance of a concrete sub-class of <code>
      *     RegistryElementDescriptor</code>
      *
      * @throws IllegalArgumentException is descriptor is <code>null</code>
      * @throws IllegalArgumentException if any of the modes returned
      *     by <code>descriptor.getSupportedModes()</code> is not one
      *     of those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if any of the
      *     <code>PropertyGenerator</code>s associated with the
      *     <code>RegistryElementDescriptor</code> to be unregistered is null.
      *
      * @since JAI 1.1
      */
    public void unregisterDescriptor(RegistryElementDescriptor descriptor) {

	if (descriptor == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	String descriptorName = descriptor.getName();

	String[] supportedModes = descriptor.getSupportedModes();

	// First make sure that all supported modes are legal registry
	// modes.
	for (int i = 0; i < supportedModes.length; i++) {
	    if (RegistryMode.getMode(supportedModes[i]) == null)
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationRegistry3",
			new Object[] {descriptorName, supportedModes[i]}));
	}

	// Now unregister the descriptor against each supported mode.
	for (int i = 0; i < supportedModes.length; i++) {

	    DescriptorCache dc = getDescriptorCache(supportedModes[i]);

	    dc.removeDescriptor(descriptor);
	}
    }

    /**
      * Get the <code>RegistryElementDescriptor</code>
      * corresponding to a <code>descriptorClass</code>
      * and a <code>descriptorName</code>. For example,
      * <code>getDescriptor(OperationDescriptor.class, "add")</code>
      * would get the operation descriptor for the "add" image operation.
      * Note that different descriptors might have been registered
      * against each mode associated with the descriptorClass. In this
      * case this methods will arbitrarily return the first descriptor
      * it encounters with a matching descriptorName and descriptorClass.
      *
      * @param descriptorClass the descriptor <code>Class</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @throws IllegalArgumentException if <code>descriptorClass</code> is
      *         <code>null</code> or if the <code>descriptorClass</code> is
      *         not associated with any of the modes returned
      *          <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if descriptorName is <code>null</code>
      *
      * @since JAI 1.1
      */
    public RegistryElementDescriptor getDescriptor(
		    Class descriptorClass, String descriptorName) {

	if ((descriptorClass == null) || (descriptorName == null))
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	String supportedModes[] = RegistryMode.getModeNames(descriptorClass);

	if (supportedModes == null)
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry4",
		    new Object[] {descriptorClass.getName()}));

	RegistryElementDescriptor red;

	// Now look for the descriptor in each supported mode.
	for (int i = 0; i < supportedModes.length; i++) {

	    DescriptorCache dc = getDescriptorCache(supportedModes[i]);

	    if ((red = dc.getDescriptor(descriptorName)) != null)
		return red;
	}

	return null;
    }

    /**
      * Get a <code>List</code> of all <code>RegistryElementDescriptor</code>
      * corresponding to the <code>descriptorClass</code>. For example,
      * <code>getDescriptors(OperationDescriptor.class)</code>
      * would get a list of all image operation descriptors.
      *
      * @param descriptorClass the descriptor <code>Class</code>
      *
      * @throws IllegalArgumentException if <code>descriptorClass</code> is
      *		<code>null</code> or if the <code>descriptorClass</code> is
      *		not associated with any of the modes returned
      *          <code>RegistryMode.getModes()</code>
      *
      * @since JAI 1.1
      */
    public List getDescriptors(Class descriptorClass) {

	if (descriptorClass == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

	String supportedModes[] = RegistryMode.getModeNames(descriptorClass);

	if (supportedModes == null)
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry4",
		    new Object[] {descriptorClass.getName()}));

	List list;
	HashSet set = new HashSet();

	// Now unregister the descriptor against each supported mode.
	for (int i = 0; i < supportedModes.length; i++) {

	    DescriptorCache dc = getDescriptorCache(supportedModes[i]);

	    if ((list = dc.getDescriptors()) != null)
		set.addAll(list);
	}

	return new ArrayList(set);
    }

    /**
      * Get an array of all the descriptor names
      * corresponding to the <code>descriptorClass</code>. For example,
      * <code>getDescriptorNames(OperationDescriptor.class)</code>
      * would get an array of all image operation descriptor names.
      *
      * @param descriptorClass the descriptor <code>Class</code>
      *
      * @throws IllegalArgumentException if <code>descriptorClass</code> is
      *		<code>null</code> or if the <code>descriptorClass</code> is
      *		not associated with any of the modes returned
      *          <code>RegistryMode.getModes()</code>
      *
      * @since JAI 1.1
      */
    public String[] getDescriptorNames(Class descriptorClass) {

	List dlist = getDescriptors(descriptorClass);

	if (dlist != null) {

	    Iterator diter = dlist.iterator();

	    String[] names = new String[dlist.size()];
	    int i = 0;

	    while (diter.hasNext()) {
		RegistryElementDescriptor red =
		    (RegistryElementDescriptor)diter.next();

		names[i++] = red.getName();
	    }

	    return names;
	}

	return null;
    }

    /**
      * Get the <code>RegistryElementDescriptor</code> corresponding to
      * <code>descriptorName</code> which supports the specified mode.
      * This is done by matching up the <code>descriptorName</code>
      * against <code>RegistryElementDescriptor.getName</code> in a
      * case-insensitive manner. This returns <code>null</code> if there
      * no <code>RegistryElementDescriptor</code> corresponding to
      * <code>descriptorName</code> that supports the specified mode.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @throws IllegalArgumentException if modeName is <code>null</code>
      *          or if the modeName is not one of the modes returned
      *          <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if descriptorName is <code>null</code>
      *
      * @since JAI 1.1
      */
    public RegistryElementDescriptor getDescriptor(String modeName,
                                            String descriptorName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    return dc.getDescriptor(descriptorName);

	return null;
    }

    /**
      * Get a list of all <code>RegistryElementDescriptor</code>s registered
      * under a given registry mode.
      *
      * @param modeName the registry mode name as a <code>String</code>
      *
      * @throws IllegalArgumentException if modeName is <code>null</code>
      *          or if the modeName is not one of the modes returned
      *          <code>RegistryMode.getModes()</code>
      *
      * @since JAI 1.1
      */
    public List getDescriptors(String modeName) {
	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    return dc.getDescriptors();

	return null;
    }

    /**
      * Get an array of all descriptor-names of descriptors registered
      * under a given registry mode.
      *
      * @param modeName the registry mode name as a <code>String</code>
      *
      * @throws IllegalArgumentException if modeName is <code>null</code>
      *          or if the modeName is not one of the modes returned
      *          <code>RegistryMode.getModes()</code>
      *
      * @since JAI 1.1
      */
    public String[] getDescriptorNames(String modeName) {
	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    return dc.getDescriptorNames();

	return null;
    }


    //////////////////
    //
    // Set of methods to set/unset/clear product preferences

    /**
      * Set the preference between two products for a descriptor
      * registered under a registry mode.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param preferredProductName the product to be preferred.
      * @param otherProductName the other product.
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if the registry mode does not
      *		    support preferences
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code> under <code>modeName</code>.
      * @throws IllegalArgumentException if either of the products are
      *		    not registered against <code>descriptorName</code>
      *		    under <code>productName</code>.
      *
      * @since JAI 1.1
      */
    public void setProductPreference(String modeName,
                                     String descriptorName,
                                     String preferredProductName,
                                     String otherProductName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    dc.setProductPreference(descriptorName, preferredProductName,
						    otherProductName);
    }

    /**
      * Remove the preference between two products for a descriptor
      * registered under a registry mode.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param preferredProductName the product formerly preferred.
      * @param otherProductName the other product.
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if the registry mode does not
      *		    support preferences
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code> under <code>modeName</code>.
      * @throws IllegalArgumentException if either of the products are
      *		    not registered against <code>descriptorName</code>
      *		    under <code>productName</code>.
      *
      * @since JAI 1.1
      */
    public void unsetProductPreference(String modeName,
                                       String descriptorName,
                                       String preferredProductName,
                                       String otherProductName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    dc.unsetProductPreference(descriptorName, preferredProductName,
						      otherProductName);
    }

    /**
      * Remove all the preferences between products for a descriptor
      * registered under a registry mode.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @throws IllegalArgumentException if modeName is <code>null</code>
      *          or if the modeName is not one of the modes returned
      *          <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if descriptorName is <code>null</code>
      * @throws IllegalArgumentException if the registry mode does not
      *		    support preferences
      *
      * @since JAI 1.1
      */
    public void clearProductPreferences(String modeName,
                                       String descriptorName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    dc.clearProductPreferences(descriptorName);
    }

    /**
      * Returns a list of the pairwise product preferences
      * under a particular descriptor registered against a registry mode.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @return an array of 2-element arrays of Strings.
      *
      * @throws IllegalArgumentException if modeName is <code>null</code>
      *          or if the modeName is not one of the modes returned
      *          <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if descriptorName is <code>null</code>
      * @throws IllegalArgumentException if the registry mode does not
      *		    support preferences
      *
      * @since JAI 1.1
      */
    public String[][] getProductPreferences(String modeName,
                                            String descriptorName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    return dc.getProductPreferences(descriptorName);

	return null;
    }


    /**
      * Returns a list of the products registered under a particular
      * descriptor in an ordering that satisfies all of the pairwise
      * preferences that have been set. Returns <code>null</code> if
      * cycles exist. Returns null if no descriptor has been registered
      * under this descriptorName, or if no products exist for this
      * operation.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @return a <code>Vector</code> of Strings representing product names.
      *
      * @throws IllegalArgumentException if modeName is <code>null</code>
      *          or if the modeName is not one of the modes returned
      *          <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if descriptorName is <code>null</code>
      * @throws IllegalArgumentException if the registry mode does not
      *		    support preferences
      *
      * @since JAI 1.1
      */
    public Vector getOrderedProductList(String modeName,
                                        String descriptorName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    return dc.getOrderedProductList(descriptorName);

	return null;
    }


    //////////////////
    //
    // Set of methods to maintain factory objects to register/unregister
    // & set/unset/clear preferences & get-ordered-lists of factory
    // objects

    /**
     * Get the local name for a factory instance used in the
     * DescriptorCache (to be used in writing out the registry file).
     */
    String getLocalName(String modeName, Object factoryInstance) {

	FactoryCache fc = getFactoryCache(modeName);

	if (fc != null)
	    return fc.getLocalName(factoryInstance);

	return null;
    }

    /**
      * Register a factory object with a particular product and descriptor
      * against a specified mode. For modes that do not support preferences
      * the productName is ignored (can be <code>null</code>)
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param factory the object to be registered.
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code> (productName can be <code>null</code>
      *		    for modes that do not support preferences).
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      *
      * @since JAI 1.1
      */
    public void registerFactory(String modeName,
                                String descriptorName,
                                String productName,
                                Object factory) {

	DescriptorCache dc = getDescriptorCache(modeName);
	FactoryCache    fc = getFactoryCache(modeName);

	if (dc.getDescriptor(descriptorName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry5",
		    new Object[] {descriptorName, modeName}));
	}

	if (factory == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
	}

	if (dc.arePreferencesSupported) {

	    OperationGraph og =
		dc.addProduct(descriptorName, productName);

	    if (og == null) {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationRegistry5",
			new Object[] {descriptorName, modeName}));
	    }

	    og.addOp(new PartialOrderNode(
			factory, factory.getClass().getName()));
	}

	fc.addFactory(descriptorName, productName, factory);
    }

    /**
      * Unregister a factory object previously registered with a product
      * and descriptor against the specified mode. For modes that do
      * not support preferences the productName is ignored (can be
      * <code>null</code>)
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param factory the object to be unregistered.
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code> (productName can be <code>null</code>
      *		    for modes that do not support preferences).
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if the factory object was not previously
      *             registered against descriptorName and productName
      *
      * @since JAI 1.1
      */
    public void unregisterFactory(String modeName,
                                  String descriptorName,
                                  String productName,
                                  Object factory) {

	DescriptorCache dc = getDescriptorCache(modeName);
	FactoryCache    fc = getFactoryCache(modeName);

	if (dc.getDescriptor(descriptorName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry5",
		    new Object[] {descriptorName, modeName}));
	}

	if (factory == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
	}

	fc.removeFactory(descriptorName, productName, factory);

	if (dc.arePreferencesSupported) {

	    OperationGraph og =
		dc.lookupProduct(descriptorName, productName);

	    if (og == null) {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationRegistry5",
			new Object[] {descriptorName, modeName}));
	    }

	    og.removeOp(factory);
	}
    }

    /**
      * Sets a preference between two factory instances for a given
      * operation under a specified product.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param preferredOp the preferred factory object
      * @param otherOp the other factory object
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if either of the factory objects
      *             were not previously registered against
      *             descriptorName and productName
      * @throws IllegalArgumentException if the registry mode does not
      *		    support preferences
      *
      * @since JAI 1.1
      */
    public void setFactoryPreference(String modeName,
                                     String descriptorName,
                                     String productName,
                                     Object preferredOp,
                                     Object otherOp) {

	DescriptorCache dc = getDescriptorCache(modeName);
	FactoryCache    fc = getFactoryCache(modeName);

	if (dc.getDescriptor(descriptorName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry5",
		    new Object[] {descriptorName, modeName}));
	}

	// This should throw an exception if preferences are not
	// supported.
	fc.setPreference(
	    descriptorName, productName, preferredOp, otherOp);

	if (dc.arePreferencesSupported) {

	    OperationGraph og =
		dc.lookupProduct(descriptorName, productName);

	    if (og == null) {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationRegistry5",
			new Object[] {descriptorName, modeName}));
	    }

	    og.setPreference(preferredOp, otherOp);
	}
    }

    /**
      * Unsets a preference between two factory instances for a given
      * operation under a specified product.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param preferredOp the factory object formerly preferred
      * @param otherOp the other factory object
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if either of the factory objects
      *             were not previously registered against
      *             descriptorName and productName
      * @throws IllegalArgumentException if the registry mode does not
      *		    support preferences
      *
      * @since JAI 1.1
      */
    public void unsetFactoryPreference(String modeName,
                                       String descriptorName,
                                       String productName,
                                       Object preferredOp,
                                       Object otherOp) {

	DescriptorCache dc = getDescriptorCache(modeName);
	FactoryCache    fc = getFactoryCache(modeName);

	if (dc.getDescriptor(descriptorName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry5",
		    new Object[] {descriptorName, modeName}));
	}

	// This should throw an exception if preferences are not
	// supported.
	fc.unsetPreference(
	    descriptorName, productName, preferredOp, otherOp);

	if (dc.arePreferencesSupported) {

	    OperationGraph og =
		dc.lookupProduct(descriptorName, productName);

	    if (og == null) {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationRegistry5",
			new Object[] {descriptorName, modeName}));
	    }

	    og.unsetPreference(preferredOp, otherOp);
	}
    }

    /**
      * Removes all preferences between instances of a factory
      * within a product registered under a particular
      * <code>OperationDescriptor</code>.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      *
      * @since JAI 1.1
      */
    public void clearFactoryPreferences(String modeName,
                                        String descriptorName,
                                        String productName) {

	DescriptorCache dc = getDescriptorCache(modeName);
	FactoryCache    fc = getFactoryCache(modeName);

	if (dc.getDescriptor(descriptorName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry5",
		    new Object[] {descriptorName, modeName}));
	}

	Object prefs[][] = fc.getPreferences(descriptorName, productName);

	if (prefs != null) {

	    OperationGraph og =
		dc.lookupProduct(descriptorName, productName);

	    if (og == null) {
		throw new IllegalArgumentException(
		    JaiI18N.formatMsg("OperationRegistry5",
			new Object[] {descriptorName, modeName}));
	    }

	    for (int i = 0; i < prefs.length; i++) {
		og.unsetPreference(prefs[i][0], prefs[i][1]);
	    }
	}

	fc.clearPreferences(descriptorName, productName);
    }

    /**
      * Get all pairwise preferences between instances of a factory
      * within a product registered under a particular
      * <code>OperationDescriptor</code>.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      *
      * @since JAI 1.1
      */
    public Object[][] getFactoryPreferences(String modeName,
					    String descriptorName,
					    String productName) {

	DescriptorCache dc = getDescriptorCache(modeName);
	FactoryCache    fc = getFactoryCache(modeName);

	if (dc.getDescriptor(descriptorName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry5",
		    new Object[] {descriptorName, modeName}));
	}

	return fc.getPreferences(descriptorName, productName);
    }

    /**
      * Returns a list of the factory instances of a product registered
      * under a particular <code>OperationDescriptor</code>, in an
      * ordering that satisfies all of the pairwise preferences that
      * have been set. Returns <code>null</code> if cycles exist.
      * Returns <code>null</code>, if the product does not exist under
      * this descriptorName.
      *
      * If the particular registry mode does not support preferences then the
      * returned <code>List</code> will contain a single factory.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      *
      * @return an ordered <code>List</code> of factory instances
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code> (productName can be <code>null</code>
      *		    for modes that do not support preferences).
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      *
      * @since JAI 1.1
      */
    public List getOrderedFactoryList(String modeName,
                                      String descriptorName,
                                      String productName) {

	DescriptorCache dc = getDescriptorCache(modeName);
	FactoryCache    fc = getFactoryCache(modeName);

	if (dc.getDescriptor(descriptorName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry5",
		    new Object[] {descriptorName, modeName}));
	}

	if (dc.arePreferencesSupported) {

	    OperationGraph og =
		dc.lookupProduct(descriptorName, productName);

	    if (og == null)
		return null;

	    Vector v = og.getOrderedOperationList();

	    if ((v == null) || (v.size() <= 0))
		return null;

	    ArrayList list = new ArrayList(v.size());

	    for (int i = 0; i < v.size(); i++) {
		list.add(((PartialOrderNode)v.elementAt(i)).getData());
	    }

	    return list;

	} else {
	    return fc.getFactoryList(descriptorName, productName);
	}
    }

    /**
      * Returns an <code>Iterator</code> over all factory objects
      * registered with the specified factory and operation names over
      * all products. The order of objects in the iteration will be
      * according to the pairwise preferences among products and image
      * factories within a product. The <code>remove()</code> method
      * of the <code>Iterator</code> may not be implemented. If the
      * particular factory does not have preferences then the returned
      * <code>Iterator</code> will traverse a collection containing the
      * single factory.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @return an <code>Iterator</code> over factory objects
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      *
      * @since JAI 1.1
      */
    public Iterator getFactoryIterator(String modeName,
                                       String descriptorName) {

	DescriptorCache dc = getDescriptorCache(modeName);
	FactoryCache    fc = getFactoryCache(modeName);

	if (dc.getDescriptor(descriptorName) == null) {
	    throw new IllegalArgumentException(
		JaiI18N.formatMsg("OperationRegistry5",
		    new Object[] {descriptorName, modeName}));
	}

	if (dc.arePreferencesSupported) {
	    Vector v = getOrderedProductList(modeName, descriptorName);

	    if ((v == null) || (v.size() <= 0))
		return null;

	    ArrayList list = new ArrayList();

	    List plist;

	    for (int i = 0; i < v.size(); i++) {
		plist = getOrderedFactoryList(modeName,
				descriptorName, (String)v.get(i));
		if (plist != null)
		    list.addAll(plist);
	    }

	    return list.iterator();

	} else {
	    List list = fc.getFactoryList(descriptorName, null);

	    if (list != null)
		return list.iterator();
	}

	return null;
    }

    /**
      * Returns the factory of the specified type for the named
      * operation. This method will return the first factory that would
      * be encountered by the <code>Iterator</code> returned by the
      * <code>getFactoryIterator()</code> method.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @return a registered factory object
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      *
      * @since JAI 1.1
      */
    public Object getFactory(String modeName, String descriptorName) {

	Iterator it = getFactoryIterator(modeName, descriptorName);

	if ((it != null) && it.hasNext())
	    return it.next();

	return null;
    }


    /**
      * Finds the factory of the specified type for the named operation
      * and invokes its default factory method with the supplied
      * parameters. The class of the returned object is that of the
      * object returned by the factory's object creation method.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @return an object created by the factory method
      *
      * @throws IllegalArgumentException if modeName or descriptorName
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      *
      * @since JAI 1.1
      */
    public Object invokeFactory(String modeName,
                                String descriptorName,
                                Object[] args) {

	Iterator it = getFactoryIterator(modeName, descriptorName);

	if (it == null)
	    return null;

	FactoryCache fc = getFactoryCache(modeName);
        ImagingListener listener =
            JAI.getDefaultInstance().getImagingListener();
        Exception savedOne = null;

	while (it.hasNext()) {

	    Object factory = it.next();
	    Object obj;

	    try {
		if ((obj = fc.invoke(factory, args)) != null)
		    return obj;
                savedOne = null;
	    } catch (Exception e) {
                listener.errorOccurred(JaiI18N.getString("OperationRegistry6")+
                                       " \""+descriptorName+"\"",
                                       e, this, false);
                savedOne = e;
//		e.printStackTrace();
	    }
	}

        if (savedOne != null)
            throw new ImagingException(JaiI18N.getString("OperationRegistry7")+
                                       " \""+descriptorName+"\"",
                                       savedOne);

	return null;
    }


    //////////////////
    //
    // Property related methods :
    // If a RegistryElementDescriptor supports properties
    // (arePropertiesSupported() return true) then a property
    // environment has to be managed.
    //
    // In the next four methods if the mode is null then apply to all modes
    // that support properties.

    /**
      * Adds a <code>PropertyGenerator</code> to the registry,
      * associating it with a particular descriptor registered against a
      * registry mode.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param generator the <code>PropertyGenerator</code> to be added.
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if the specified mode does not
      *             support properties.
      *
      * @since JAI 1.1
      */
    public void addPropertyGenerator(String modeName,
                                     String descriptorName,
                                     PropertyGenerator generator) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    dc.addPropertyGenerator(descriptorName, generator);
    }

    /**
      * Removes a <code>PropertyGenerator</code> from its association
      * with a particular descriptor/registry-mode in the registry. If
      * the generator was not associated with the operation, nothing
      * happens.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param generator the <code>PropertyGenerator</code> to be removed.
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if the specified mode does not
      *             support properties.
      *
      * @since JAI 1.1
      */
    public void removePropertyGenerator(String modeName,
                                        String descriptorName,
                                        PropertyGenerator generator) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    dc.removePropertyGenerator(descriptorName, generator);
    }

    /**
      * Forces a property to be copied from the specified source by nodes
      * performing a particular operation. By default, a property is
      * copied from the first source node that emits it. The result of
      * specifying an invalid source is undefined.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param propertyName the name of the property to be copied.
      * @param sourceIndex the index of the source to copy the property from.
      *
      * @throws IllegalArgumentException if any of the <code>String</code>
      *             arguments is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if the specified mode does not
      *             support properties.
      *
      * @since JAI 1.1
      */
    public void copyPropertyFromSource(String modeName,
                                       String descriptorName,
                                       String propertyName,
                                       int sourceIndex) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    dc.copyPropertyFromSource(descriptorName,
				      propertyName, sourceIndex);
    }

    /**
      * Forces a particular property to be suppressed by nodes
      * performing a particular operation.  By default, properties
      * are passed through operations unchanged.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param propertyName the name of the property to be suppressed.
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if the specified mode does not
      *             support properties.
      *
      * @since JAI 1.1
      */
    public void suppressProperty(String modeName,
                                 String descriptorName,
                                 String propertyName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    dc.suppressProperty(descriptorName, propertyName);
    }

    /**
      * Forces all properties to be suppressed by nodes performing a
      * particular operation.  By default, properties are passed
      * through operations unchanged.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if the specified mode does not
      *             support properties.
      *
      * @since JAI 1.1
      */
    public void suppressAllProperties(String modeName,
                                      String descriptorName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    dc.suppressAllProperties(descriptorName);
    }

    /**
      * Removes all property associated information for this registry
      * mode from this <code>OperationRegistry</code>.
      *
      * @param modeName the registry mode name as a <code>String</code>
      *
      * @throws IllegalArgumentException if modeName is null or is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if the specified mode does not
      *             support properties.
      *
      * @since JAI 1.1
      */
    public void clearPropertyState(String modeName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    dc.clearPropertyState();
    }

    /**
      * Returns a list of the properties generated by nodes
      * implementing the descriptor associated with a particular
      * descriptor Name. Returns null if no properties are
      * generated.
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if the specified mode does not
      *             support properties.
      *
      * @since JAI 1.1
      */
    public String[] getGeneratedPropertyNames(String modeName,
                                              String descriptorName) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    return dc.getGeneratedPropertyNames(descriptorName);

	return null;
    }

    /**
      * Merge mode-specific property environment with mode-independent
      * property environment of the descriptor. Array elements of
      * "sources" are expected to be in the same ordering as referenced
      * by the "sourceIndex" parameter of copyPropertyFromSource().
      *
      * @param modeName the registry mode name as a <code>String</code>
      * @param descriptorName the descriptor name as a <code>String</code>
      * @param op the <code>Object</code> from which properties will
      *           be generated.
      * @param sources the <code>PropertySource</code>s corresponding to
      *     the sources of the object representing the named descriptor
      *     in the indicated mode.  The supplied <code>Vector</code> may
      *     be empty to indicate that there are no sources.
      *
      * @return A <code>PropertySource</code> which encapsulates
      *     the global property environment for the object representing
      *     the named descriptor in the indicated mode.
      *
      * @throws IllegalArgumentException if any of the arguments
      *             is <code>null</code>
      * @throws IllegalArgumentException if modeName is not one of
      *             those returned by <code>RegistryMode.getModes()</code>
      * @throws IllegalArgumentException if there is no <code>
      *             RegistryElementDescriptor</code> registered against
      *             the <code>descriptorName</code>
      * @throws IllegalArgumentException if the specified mode does not
      *             support properties.
      *
      * @since JAI 1.1
      */
    public PropertySource getPropertySource(String modeName,
                                            String descriptorName,
					    Object op,
                                            Vector sources) {

	DescriptorCache dc = getDescriptorCache(modeName);

	if (dc != null)
	    return dc.getPropertySource(descriptorName, op, sources);

	return null;
    }

    /**
     * Constructs and returns a <code>PropertySource</code> suitable for
     * use by a given <code>OperationNode</code>.  The
     * <code>PropertySource</code> includes properties copied from prior
     * nodes as well as those generated at the node itself. Additionally,
     * property suppression is taken into account. The actual
     * implementation of <code>getPropertySource()</code> may make use
     * of deferred execution and caching.
     *
     * @param op the <code>OperationNode</code> requesting its
     *        <code>PropertySource</code>.
     *
     * @throws IllegalArgumentException if op is null.
     *
     * @since JAI 1.1
     */
    public PropertySource getPropertySource(OperationNode op) {

	if (op == null)
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));

        // Get the source Vector from the ParameterBlock.
        ParameterBlock pb = op.getParameterBlock();
	Vector pv = (pb == null) ? null : pb.getSources();

        // If the source Vector is null, replace it by a zero-length
        // Vector. This tricks the DescriptorCache into accepting the
        // parameter and the PropertyEnvironment object created in
        // the DescriptorCache works with either a null or zero-length
        // source Vector.
        if(pv == null) {
            pv = new Vector();
        }

	return getPropertySource(op.getRegistryModeName(),
				 op.getOperationName(), op, pv);
    }

    /**
     * Load all the "META-INF/registryFile.jai" files and then
     * called the <code>updateRegistry()</code> of the registered
     * service provider of <code>OperationRegistrySpi</code> found
     * in the classpath corresponding to this class loader. All
     * non-IO exceptions encountered while parsing the registry
     * files are caught and their error messages are redirected to
     * <code>System.err</code>. If <code>System.err</code> is null the
     * error messages will never be seen.
     *
     * <p>This is a convenience method to do automatic detection in runtime
     * loaded jar files
     *
     * <p>Note that the JAI does not keep track of which JAR files have
     * their registry files loaded and/or services initialized. Hence
     * if <code>registerServices</code> is called twice with the
     * same ClassLoader, the loading of the registry files and/or
     * initialization of the services will happen twice.
     *
     * @since JAI 1.1
     */
    public void registerServices(ClassLoader cl) throws IOException {

	// First load all the REGISTRY_FILEs that are found in
	// the specified class loader.
	Enumeration en;

	if (cl == null)
	    en = ClassLoader.getSystemResources(USR_REGISTRY_FILE);
	else
	    en = cl.getResources(USR_REGISTRY_FILE);

	while (en.hasMoreElements()) {
	    URL url = (URL)en.nextElement();

	    RegistryFileParser.loadOperationRegistry(this, cl, url);
	}

	// Now call the "updateRegistry" method for all OperationRegistry
	// service providers.
	Iterator spitr;

	if (cl == null)
	    spitr = Service.providers(OperationRegistrySpi.class);
	else
	    spitr = Service.providers(OperationRegistrySpi.class, cl);

	while (spitr.hasNext()) {

	    OperationRegistrySpi ospi = (OperationRegistrySpi)spitr.next();
	    ospi.updateRegistry(this);
	}
    }

    /********************** DEPRECATED METHODS *************************/

    // OperationDescriptor methods

    /**
     * Registers an <code>OperationDescriptor</code> with the registry. Each
     * operation must have an <code>OperationDescriptor</code> before
     * registerRIF() may be called to add RIFs to the operation.
     *
     * @param odesc an <code>OperationDescriptor</code> containing information
     *        about the operation.
     * @param operationName the operation name as a String.
     *
     * @deprecated as of JAI 1.1 in favor of <code>registerDescriptor(odesc)</code>
     *
     * @see #registerDescriptor(RegistryElementDescriptor)
     *		    registerDescriptor - for list of exceptions thrown.
     */
    public void registerOperationDescriptor(OperationDescriptor odesc,
					    String operationName) {
	registerDescriptor(odesc);
    }

    /**
     * Unregisters an <code>OperationDescriptor</code> from the registry.
     *
     * @param operationName the operation name as a String.
     *
     * @deprecated as of JAI 1.1 in favor of <code>unregisterDescriptor(...)
     *		</code> which accepts an <code>OperationDescriptor</code>
     *		and not a <code>operationName</code>.
     *
     * @see #unregisterDescriptor(RegistryElementDescriptor)
     *		unregisterDescriptor - for list of exceptions thrown.
     */
    public void unregisterOperationDescriptor(String operationName) {

	String[] operationModes =
	    RegistryMode.getModeNames(OperationDescriptor.class);

	RegistryElementDescriptor red;

	for (int i = 0; i < operationModes.length; i++) {
	    if ((red = getDescriptor(operationModes[i], operationName)) != null)
		unregisterDescriptor(red);
	}
    }

    /**
     * Returns the <code>OperationDescriptor</code> that is currently
     * registered under the given name, or null if none exists.
     * Though unlikely, it is possible to have different descriptors
     * registered under different modes. In this case this will
     * arbitrarily return the first operation descriptor found.
     *
     * @param operationName the String to be queried.
     * @return an <code>OperationDescriptor</code>.
     *
     * @throws IllegalArgumentException if operationName is null.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getDescriptor(...)</code>
     *		where the mode name is explicitly specified.
     *
     * @see #getDescriptor(Class, String)
     */
    public OperationDescriptor getOperationDescriptor(String operationName) {
	return (OperationDescriptor)
		    getDescriptor(OperationDescriptor.class, operationName);
    }

    /**
     * Returns a Vector of all currently registered
     * <code>OperationDescriptor</code>s.
     *
     * @return a Vector of <code>OperationDescriptor</code>s.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getDescriptors(
     *	OperationDescriptor.class)</code> which returns a <code>List</code> and
     *	not a <code>Vector</code>. This is currently equivalent to
     *	<code>new Vector(getDescriptors(OperationDescriptor.class))</code>
     *
     * @see #getDescriptors(Class)
     */
    public Vector getOperationDescriptors() {
	List list = getDescriptors(OperationDescriptor.class);

	return list == null ? null : new Vector(list);
    }

    /**
     * Returns a list of names under which all the
     * <code>OperationDescriptor</code>s in the registry are registered.
     *
     * @return a list of currently existing operation names.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getDescriptorNames(
     *		OperationDescriptor.class)</code>.
     *
     * @see #getDescriptorNames(Class)
     */
    public String[] getOperationNames() {
	return getDescriptorNames(OperationDescriptor.class);
    }

    // RenderedImageFactory methods

    /**
     * Registers a RIF with a particular product and operation.
     *
     * @param operationName the operation name as a String.
     * @param productName the product name, as a String.
     * @param RIF the <code>RenderedImageFactory</code> to be registered.
     *
     * @deprecated as of JAI 1.1 in favor of <code>RIFRegistry.register(...)
     *	    </code>. This is currently equivalent to <code>
     *	    RIFRegistry.register(this, operationName, productName, RIF)</code>
     *
     * @see #registerFactory registerFactory - for list of exceptions thrown.
     * @see RIFRegistry#register
     */
    public void registerRIF(String operationName,
			    String productName,
			    RenderedImageFactory RIF) {

	registerFactory(RenderedRegistryMode.MODE_NAME, operationName, productName, RIF);
    }

    /**
     * Unregisters a RIF from a particular product and operation.
     *
     * @param operationName the operation name as a String.
     * @param productName the product name, as a String.
     * @param RIF the <code>RenderedImageFactory</code> to be unregistered.
     *
     * @deprecated as of JAI 1.1 in favor of <code>RIFRegistry.unregister(...)
     *	    </code>. This is currently equivalent to <code>
     *	    RIFRegistry.unregister(this, operationName, productName, RIF)</code>
     *
     * @see #unregisterFactory unregisterFactory - for list of exceptions thrown.
     * @see RIFRegistry#unregister
     */
    public void unregisterRIF(String operationName,
			      String productName,
			      RenderedImageFactory RIF) {

	unregisterFactory(RenderedRegistryMode.MODE_NAME, operationName, productName, RIF);
    }

    /**
     * Registers a CRIF under a particular operation.
     *
     * @param operationName the operation name as a String.
     * @param CRIF the <code>ContextualRenderedImageFactory</code> to be
     * registered.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CRIFRegistry.register(...)
     *	    </code>. This is currently equivalent to <code>
     *	    CRIFRegistry.register(this, operationName, productName, CRIF)</code>
     *
     * @see #registerFactory registerFactory - for list of exceptions thrown.
     * @see CRIFRegistry#register
     */
    public void registerCRIF(String operationName,
			     ContextualRenderedImageFactory CRIF) {


	registerFactory(RenderableRegistryMode.MODE_NAME, operationName, null, CRIF);
    }

    /**
     * Unregisters a CRIF from a particular operation.
     *
     * @param operationName the operation name as a String.
     * @param CRIF the <code>ContextualRenderedImageFactory</code> to be
     * unregistered.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CRIFRegistry.unregister(...)
     *	    </code>. This is currently equivalent to <code>
     *	    CRIFRegistry.unregister(this, operationName, productName, CRIF)</code>
     *
     * @see #unregisterFactory unregisterFactory - for list of exceptions thrown.
     * @see CRIFRegistry#unregister
     */
    public void unregisterCRIF(String operationName,
			       ContextualRenderedImageFactory CRIF) {

	unregisterFactory(RenderableRegistryMode.MODE_NAME, operationName, null, CRIF);
    }

    /**
     * Registers a CIF with a particular product and operation.
     *
     * @param operationName the operation name as a String.
     * @param productName the product name, as a String.
     * @param CIF the <code>CollectionImageFactory</code> to be registered.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CIFRegistry.register(...)
     *	    </code>. This is currently equivalent to <code>
     *	    CIFRegistry.register(this, operationName, productName, CIF)</code>
     *
     * @see #registerFactory registerFactory - for list of exceptions thrown.
     * @see CIFRegistry#register
     */
    public void registerCIF(String operationName,
			    String productName,
			    CollectionImageFactory CIF) {

	registerFactory(CollectionRegistryMode.MODE_NAME, operationName, productName, CIF);
    }

    /**
     * Unregisters a CIF from a particular product and operation.
     *
     * @param operationName the operation name as a String.
     * @param productName the product name, as a String.
     * @param CIF the <code>CollectionImageFactory</code> to be unregistered.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CIFRegistry.unregister(...)
     *	    </code>. This is currently equivalent to <code>
     *	    CIFRegistry.unregister(this, operationName, productName, CIF)</code>
     *
     * @see #unregisterFactory unregisterFactory - for list of exceptions thrown.
     * @see CIFRegistry#unregister
     */
    public void unregisterCIF(String operationName,
			      String productName,
			      CollectionImageFactory CIF) {

	unregisterFactory(CollectionRegistryMode.MODE_NAME, operationName, productName, CIF);
    }

    // Product preferences

    /**
     * Sets a preference between two products registered under a common
     * <code>OperationDescriptor</code>. Any attempt to set a preference
     * between a product and itself will be ignored.
     *
     * @param operationName the operation name as a String.
     * @param preferredProductName the product to be preferred.
     * @param otherProductName the other product.
     *
     * @deprecated as of JAI 1.1 in favor of <code>setProductPreference(...)
     *	    </code> which specifies a <code>modeName</code> also. This is
     *	    currently equivalent to <code>setProductPreference("rendered",
     *	    operationName, preferredProductName, otherProductName)</code>
     *
     * @see #setProductPreference setProductPreference - for list of exceptions thrown.
     */
    public void setProductPreference(String operationName,
				     String preferredProductName,
				     String otherProductName) {

	setProductPreference(RenderedRegistryMode.MODE_NAME,
	    operationName, preferredProductName, otherProductName);
    }

    /**
     * Removes a preference between two products registered under
     * a common <code>OperationDescriptor</code>.
     *
     * @param operationName the operation name as a String.
     * @param preferredProductName the product formerly preferred.
     * @param otherProductName the other product.
     *
     * @deprecated as of JAI 1.1 in favor of <code>unsetProductPreference(...)
     *	    </code> which specifies a <code>modeName</code> also. This is
     *	    currently equivalent to <code>unsetProductPreference("rendered",
     *	    operationName, preferredProductName, otherProductName)</code>
     *
     * @see #unsetProductPreference unsetProductPreference - for list of exceptions thrown.
     */
    public void unsetProductPreference(String operationName,
				       String preferredProductName,
				       String otherProductName) {

	unsetProductPreference(RenderedRegistryMode.MODE_NAME,
	    operationName, preferredProductName, otherProductName);
    }

    /**
     * Removes all preferences between products registered under
     * a common <code>OperationDescriptor</code>.
     *
     * @param operationName the operation name as a String.
     *
     * @deprecated as of JAI 1.1 in favor of <code>clearProductPreferences(...)
     *	    </code> which specifies a <code>modeName</code> also. This is
     *	    currently equivalent to <code>
     *	    clearProductPreferences("rendered", operationName)</code>
     *
     * @see #clearProductPreferences clearProductPreferences - for list of exceptions thrown.
     */
    public void clearProductPreferences(String operationName) {

	clearProductPreferences(RenderedRegistryMode.MODE_NAME, operationName);
    }

    /**
     * Returns a list of the pairwise product preferences
     * under a particular <code>OperationDescriptor</code>. If no product
     * preferences have been set, returns null.
     *
     * @param operationName the operation name as a String.
     * @return an array of 2-element arrays of Strings.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getProductPreferences(...)
     *	    </code> which accepts a <code>modeName</code> also. This is
     *	    currently equivalent to <code>
     *	    getProductPreferences("rendered", operationName)</code>
     *
     * @see #getProductPreferences getProductPreferences - for list of exceptions thrown.
     */
    public String [][] getProductPreferences(String operationName) {

	return getProductPreferences(RenderedRegistryMode.MODE_NAME, operationName);
    }

    /**
     * Returns a list of the products registered under a particular
     * <code>OperationDescriptor</code>, in an ordering that satisfies
     * all of the pairwise preferences that have been set. Returns
     * <code>null</code> if cycles exist. Returns <code>null</code> if
     * no <code>OperationDescriptor</code> has been registered under
     * this operationName, or if no products exist for this operation.
     *
     * @param operationName the operation name as a String.
     * @return a Vector of Strings representing product names.
     *
     * @deprecated as of JAI 1.1 in favor of <code>getOrderedProductList(...)
     *	    </code> which accepts a <code>modeName</code> also. This is
     *	    currently equivalent to
     *	    <code>getOrderedProductList("rendered", operationName)</code>
     *
     * @see #getOrderedProductList getOrderedProductList - for list of exceptions thrown.
     */
    public Vector getOrderedProductList(String operationName) {

	return getOrderedProductList(RenderedRegistryMode.MODE_NAME, operationName);
    }

    // Operation preferences (within a product)

    /**
     * Sets a preference between two RIFs within the same product. Any
     * attempt to set a preference between a RIF and itself will be
     * ignored.
     *
     * @param operationName the operation name as a String.
     * @param productName the name of the product.
     * @param preferredRIF the preferred <code>RenderedImageFactory</code>.
     * @param otherRIF the other <code>RenderedImageFactory</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>RIFRegistry.setPreference(...)
     *	    </code>. This is currently equivalent to <code>
     *	    RIFRegistry.setPreference(this, operationName, productName,
     *	    preferredRIF, otherRIF)</code>
     *
     * @see #setFactoryPreference setFactoryPreference - for list of exceptions thrown.
     * @see RIFRegistry#setPreference
     */
    public void setRIFPreference(String operationName,
				 String productName,
				 RenderedImageFactory preferredRIF,
				 RenderedImageFactory otherRIF) {

	setFactoryPreference(RenderedRegistryMode.MODE_NAME,
			operationName, productName, preferredRIF, otherRIF);
    }

    /**
     * Sets a preference between two CIFs within the same product. Any
     * attempt to set a preference between a CIF and itself will be
     * ignored.
     *
     * @param operationName the operation name as a String.
     * @param productName the name of the product.
     * @param preferredCIF the preferred CollectionRenderedImageFactory.
     * @param otherCIF the other CollectionRenderedImageFactory.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CIFRegistry.setPreference(...)
     *	    </code>. This is currently equivalent to <code>
     *	    CIFRegistry.setPreference(this, operationName, productName,
     *	    preferredCIF, otherCIF)</code>
     *
     * @see #setFactoryPreference setFactoryPreference - for list of exceptions thrown.
     * @see CIFRegistry#setPreference
     */
    public void setCIFPreference(String operationName,
				 String productName,
				 CollectionImageFactory preferredCIF,
				 CollectionImageFactory otherCIF) {

	setFactoryPreference(CollectionRegistryMode.MODE_NAME,
			operationName, productName, preferredCIF, otherCIF);
    }

    /**
     * Removes a preference between two RIFs within the same product.
     *
     * @param operationName the operation name as a String.
     * @param productName the name of the product.
     * @param preferredRIF the formerly preferred
     *        <code>RenderedImageFactory</code>.
     * @param otherRIF the other <code>RenderedImageFactory</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>RIFRegistry.unsetPreference(...)
     *	    </code>. This is currently equivalent to <code>
     *	    RIFRegistry.unsetPreference(this, operationName, productName,
     *	    preferredRIF, otherRIF)</code>
     *
     * @see #unsetFactoryPreference unsetFactoryPreference - for list of exceptions thrown.
     * @see RIFRegistry#unsetPreference
     */
    public void unsetRIFPreference(String operationName,
				   String productName,
				   RenderedImageFactory preferredRIF,
				   RenderedImageFactory otherRIF) {

	unsetFactoryPreference(RenderedRegistryMode.MODE_NAME,
			operationName, productName, preferredRIF, otherRIF);
    }

    /**
     * Removes a preference between two CIFs within the same product.
     *
     * @param operationName the operation name as a String.
     * @param productName the name of the product.
     * @param preferredCIF the formerly preferred
     *        <code>CollectionImageFactory</code>.
     * @param otherCIF the other <code>CollectionImageFactory</code>.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CIFRegistry.unsetPreference(...)
     *	    </code>. This is currently equivalent to <code>
     *	    CIFRegistry.unsetPreference(this, operationName, productName,
     *	    preferredCIF, otherCIF)</code>
     *
     * @see #unsetFactoryPreference unsetFactoryPreference - for list of exceptions thrown.
     * @see CIFRegistry#unsetPreference
     */
    public void unsetCIFPreference(String operationName,
				   String productName,
				   CollectionImageFactory preferredCIF,
				   CollectionImageFactory otherCIF) {

	unsetFactoryPreference(CollectionRegistryMode.MODE_NAME,
			operationName, productName, preferredCIF, otherCIF);
    }

    /**
     * Removes all preferences between RIFs within a product
     * registered under a particular <code>OperationDescriptor</code>.
     *
     * @param operationName the operation name as a String.
     * @param productName the name of the product.
     *
     * @deprecated as of JAI 1.1 in favor of <code>RIFRegistry.clearPreferences(...)
     *	    </code>. This is currently equivalent to <code>
     *	    RIFRegistry.clearPreferences(this, operationName, productName)</code>
     *
     * @see #clearFactoryPreferences clearFactoryPreferences - for list of exceptions thrown.
     * @see RIFRegistry#clearPreferences
     */
    public void clearRIFPreferences(String operationName,
				    String productName) {

	clearFactoryPreferences(
	    RenderedRegistryMode.MODE_NAME, operationName, productName);
    }

    /**
     * Removes all preferences between CIFs within a product
     * registered under a particular <code>OperationDescriptor</code>.
     *
     * @param operationName the operation name as a String.
     * @param productName the name of the product.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CIFRegistry.clearPreferences(...)
     *	    </code>. This is currently equivalent to <code>
     *	    CIFRegistry.clearPreferences(this, operationName, productName)</code>
     *
     * @see #clearFactoryPreferences clearFactoryPreferences - for list of exceptions thrown.
     * @see CIFRegistry#clearPreferences
     */
    public void clearCIFPreferences(String operationName,
				    String productName) {

	clearFactoryPreferences(
	    CollectionRegistryMode.MODE_NAME, operationName, productName);
    }

    /**
     * Removes all RIF and CIF preferences within a product
     * registered under a particular <code>OperationDescriptor</code>.
     *
     * @param operationName the operation name as a String.
     * @param productName the name of the product.
     *
     * @deprecated as of JAI 1.1 in favor of calling <code>
     *	    *IFRegistry.clearPreferences(..)</code> on all image operation
     *	    related modes.
     *
     * @see #clearFactoryPreferences clearFactoryPreferences - for list of exceptions thrown.
     * @see RIFRegistry#clearPreferences
     * @see CIFRegistry#clearPreferences
     */
    public void clearOperationPreferences(String operationName,
				          String productName) {

	String[] operationModes =
	    RegistryMode.getModeNames(OperationDescriptor.class);

	for (int i = 0; i < operationModes.length; i++) {

	    DescriptorCache dc = getDescriptorCache(operationModes[i]);

	    if (!dc.arePreferencesSupported)
		continue;

	    if (getDescriptor(operationModes[i], operationName) == null)
		continue;

	    clearFactoryPreferences(
		    operationModes[i], operationName, productName);
	}
    }

    /**
     * Returns a list of the RIFs of a product registered under a
     * particular <code>OperationDescriptor</code>, in an ordering
     * that satisfies all of the pairwise preferences that have
     * been set. Returns <code>null</code> if cycles exist. Returns
     * <code>null</code>, if the product does not exist under this
     * operationName.
     *
     * @param operationName the operation name as a String.
     * @param productName the name of the product.
     * @return a Vector of RIFs.
     *
     * @deprecated as of JAI 1.1 in favor of <code>RIFRegistry.getOrderedList(...)
     *	    </code> which returns a <code>List</code> and not a
     *	    <code>Vector</code>. This is currently equivalent to <code>
     *	    new Vector(RIFRegistry.getOrderedList(this, operationName,
     *	    productName))</code>
     *
     * @see #getOrderedFactoryList getOrderedFactoryList - for list of exceptions thrown.
     * @see RIFRegistry#getOrderedList
     */
    public Vector getOrderedRIFList(String operationName,
				    String productName) {

	List list = getOrderedFactoryList(
	    RenderedRegistryMode.MODE_NAME, operationName, productName);

	return list == null ? null : new Vector(list);
    }

    /**
     * Returns a list of the CIFs of a product registered under a
     * particular <code>OperationDescriptor</code>, in an ordering
     * that satisfies all of the pairwise preferences that have
     * been set. Returns <code>null</code> if cycles exist. Returns
     * <code>null</code>, if the product does not exist under this
     * operationName.
     *
     * @param operationName the operation name as a String.
     * @param productName the name of the product.
     * @return a Vector of CIFs.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CIFRegistry.getOrderedList(...)
     *	    </code> which returns a <code>List</code> and not a
     *	    <code>Vector</code>. This is currently equivalent to <code>
     *	    new Vector(CIFRegistry.getOrderedList(this, operationName,
     *	    productName))</code>
     *
     * @see #getOrderedFactoryList getOrderedFactoryList - for list of exceptions thrown.
     * @see CIFRegistry#getOrderedList
     */
    public Vector getOrderedCIFList(String operationName,
				    String productName) {

	List list = getOrderedFactoryList(
	    CollectionRegistryMode.MODE_NAME, operationName, productName);

	return list == null ? null : new Vector(list);
    }

    // Create methods

    /**
     * Constructs a PlanarImage (usually a <code>RenderedOp</code>) representing
     * the results of applying a given operation to a particular
     * ParameterBlock and rendering hints.  The registry is used to
     * determine the RIF to be used to instantiate the operation.
     *
     * <p> If none of the RIFs registered with this
     * <code>OperationRegistry</code> returns a non-null value, null is
     * returned.  Exceptions thrown by the RIFs will be caught by this
     * method and will not be propagated.
     *
     * @param operationName the operation name as a String.
     * @param paramBlock the operation's ParameterBlock.
     * @param renderHints a RenderingHints object containing rendering hints.
     *
     * @throws IllegalArgumentException if operationName is null.
     *
     * @deprecated as of JAI 1.1 in favor of <code>RIFRegistry.create(...)
     *	    </code> which returns a <code>RenderedImage</code> and not a
     *	    <code>PlanarImage</code>. This is currently equivalent to <code>
     *	    PlanarImage.wrapRenderedImage(RIFRegistry.create(this,
     *	    operationName, paramBlock, renderHints))</code>
     *
     * @see RIFRegistry#create
     */
    public PlanarImage create(String operationName,
			      ParameterBlock paramBlock,
			      RenderingHints renderHints) {
	return PlanarImage.wrapRenderedImage(
		    RIFRegistry.create(this,
			    operationName, paramBlock, renderHints));
    }

    /**
     * Constructs the CRIF to be used to instantiate the operation.
     * Returns null, if no CRIF is registered with the given operation
     * name.
     *
     * @param operationName the operation name as a String.
     * @param paramBlock    the operation's ParameterBlock.
     *
     * @throws IllegalArgumentException if operationName is null.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CRIFRegistry.get(...)</code>
     *	    This is currently equivalent to <code>CRIFRegistry.get(this,
     *	    operationName)</code>
     *
     * @see CRIFRegistry#get
     */
    public ContextualRenderedImageFactory
        createRenderable(String operationName, ParameterBlock paramBlock) {

	return CRIFRegistry.get(this, operationName);
    }

    /**
     * Constructs a <code>CollectionImage</code> (usually a
     * <code>CollectionOp</code>) representing the results of applying
     * a given operation to a particular ParameterBlock and rendering hints.
     * The registry is used to determine the CIF to be used to instantiate
     * the operation.
     *
     * <p> If none of the CIFs registered with this
     * <code>OperationRegistry</code> returns a non-null value, null is
     * returned.  Exceptions thrown by the CIFs will be caught by this
     * method and will not be propagated.
     *
     * @param operationName  The operation name as a String.
     * @param args  The operation's input parameters.
     * @param hints  A RenderingHints object containing rendering hints.
     *
     * @throws IllegalArgumentException if operationName is null.
     *
     * @deprecated as of JAI 1.1 in favor of <code>CIFRegistry.create(...)
     *	    </code>. This is currently equivalent to <code>
     *	    CIFRegistry.create(this, operationName, args, hints))</code>
     *
     * @see CIFRegistry#create
     */
    public CollectionImage createCollection(String operationName,
					    ParameterBlock args,
					    RenderingHints hints) {
	return CIFRegistry.create(this, operationName, args, hints);
    }

    // Property management

    /**
     * Removes all property associated information from this
     * <code>OperationRegistry</code>.
     *
     * @deprecated as of JAI 1.1 in factor of the version where the modeName
     *	is explicitly specified. This is currently equivalent to <code>
     *	clearPropertyState("rendered")</code>
     *
     * @see #clearPropertyState
     */
    public void clearPropertyState() {

	clearPropertyState(RenderedRegistryMode.MODE_NAME);
    }

    /**
     * Adds a <code>PropertyGenerator</code> to the registry, associating
     * it with a particular <code>OperationDescriptor</code>.
     *
     * @param operationName the operation name as a String.
     * @param generator the <code>PropertyGenerator</code> to be added.
     *
     * @deprecated as of JAI 1.1 in favor of the version where the
     *	    modeName is explicitly specified. This is currently
     *	    equivalent to <code>addPropertyGenerator("rendered", ...)</code>
     *
     * @see #addPropertyGenerator addPropertyGenerator - for list of exceptions thrown.
     */
    public void addPropertyGenerator(String operationName,
				     PropertyGenerator generator) {

	addPropertyGenerator(RenderedRegistryMode.MODE_NAME, operationName, generator);
    }

    /**
     * Removes a <code>PropertyGenerator</code> from its association with a
     * particular <code>OperationDescriptor</code> in the registry.  If
     * the generator was not associated with the operation,
     * nothing happens.
     *
     * @param operationName the operation name as a String.
     * @param generator the <code>PropertyGenerator</code> to be removed.
     *
     * @deprecated as of JAI 1.1 in favor of the version where the
     *	    modeName is explicitly specified. This is currently
     *	    equivalent to <code>removePropertyGenerator("rendered", ...)</code>
     *
     * @see #removePropertyGenerator removePropertyGenerator - for list of exceptions thrown.
     */
    public void removePropertyGenerator(String operationName,
					PropertyGenerator generator) {

	removePropertyGenerator(RenderedRegistryMode.MODE_NAME, operationName, generator);
    }

    /**
     * Forces a particular property to be suppressed by nodes
     * performing a particular operation.  By default, properties
     * are passed through operations unchanged.
     *
     * @param operationName the operation name as a String.
     * @param propertyName the name of the property to be suppressed.
     *
     * @deprecated as of JAI 1.1 in favor of the version where the
     *	    modeName is explicitly specified. This is currently
     *	    equivalent to <code>suppressProperty("rendered", ...)</code>
     *
     * @see #suppressProperty suppressProperty - for list of exceptions thrown.
     */
    public void suppressProperty(String operationName,
				 String propertyName) {

	suppressProperty(RenderedRegistryMode.MODE_NAME, operationName, propertyName);
    }

    /**
     * Forces all properties to be suppressed by nodes performing a
     * particular operation.  By default, properties are passed
     * through operations unchanged.
     *
     * @param operationName the operation name as a String.
     *
     * @deprecated as of JAI 1.1 in favor of the version where the
     *	    modeName is explicitly specified. This is currently
     *	    equivalent to <code>suppressAllProperties("rendered", ...)</code>
     *
     * @see #suppressAllProperties suppressAllProperties - for list of exceptions thrown.
     */
    public void suppressAllProperties(String operationName) {

	suppressAllProperties(RenderedRegistryMode.MODE_NAME, operationName);
    }

    /**
     * Forces a property to be copied from the specified source image
     * by <code>RenderedOp</code> nodes performing a particular
     * operation.  By default, a property is copied from the first
     * source node that emits it. The result of specifying an invalid
     * source is undefined.
     *
     * @param operationName the operation name as a String.
     * @param propertyName the name of the property to be copied.
     * @param sourceIndex the index of the source to copy the property from.
     *
     * @deprecated as of JAI 1.1 in favor of the version where the
     *	    modeName is explicitly specified. This is currently
     *	    equivalent to <code>copyPropertyFromSource("rendered", ...)</code>
     *
     * @see #copyPropertyFromSource copyPropertyFromSource - for list of exceptions thrown.
     */
    public void copyPropertyFromSource(String operationName,
				       String propertyName,
				       int sourceIndex) {

	copyPropertyFromSource(RenderedRegistryMode.MODE_NAME,
		    operationName, propertyName, sourceIndex);
    }

    /**
     * Returns a list of the properties generated by nodes
     * implementing the operation associated with a particular
     * Operation Name. Returns null if no properties are
     * generated.
     *
     * @param operationName the operation name as a String.
     * @return an array of Strings.
     *
     * @deprecated as of JAI 1.1 in favor of the version where the
     *	    modeName is explicitly specified. This is currently
     *	    equivalent to <code>getGeneratedPropertyNames("rendered", ...)</code>
     *
     * @see #getGeneratedPropertyNames getGeneratedPropertyNames - for list of exceptions thrown.
     */
    public String[] getGeneratedPropertyNames(String operationName) {

	return getGeneratedPropertyNames(RenderedRegistryMode.MODE_NAME, operationName);
    }

    /**
     * Constructs and returns a <code>PropertySource</code> suitable for
     * use by a given <code>RenderedOp</code>.  The
     * <code>PropertySource</code> includes properties copied from prior
     * nodes as well as those generated at the node itself. Additionally,
     * property suppression is taken into account. The actual
     * implementation of <code>getPropertySource()</code> may make use
     * of deferred execution and caching.
     *
     * @param op the <code>RenderedOp</code> requesting its
     *        <code>PropertySource</code>.
     *
     * @deprecated as of JAI 1.1 in favor
     *	    <code>RIFRegistry.getPropertySource(op)</code>
     *
     * @see RIFRegistry#getPropertySource #getPropertySource - for list of exceptions thrown.
     */
    public PropertySource getPropertySource(RenderedOp op) {

	return RIFRegistry.getPropertySource(op);
    }

    /**
     * Constructs and returns a <code>PropertySource</code> suitable for
     * use by a given <code>RenderableOp</code>.  The
     * <code>PropertySource</code> includes properties copied from prior
     * nodes as well as those generated at the node itself. Additionally,
     * property suppression is taken into account. The actual implementation
     * of <code>getPropertySource()</code> may make use of deferred
     * execution and caching.
     *
     * @param op the <code>RenderableOp</code> requesting its
     *        <code>PropertySource</code>.
     *
     * @deprecated as of JAI 1.1 in favor
     *	    <code>CRIFRegistry.getPropertySource(op)</code>
     *
     * @see CRIFRegistry#getPropertySource
     */
    public PropertySource getPropertySource(RenderableOp op) {

	return CRIFRegistry.getPropertySource(op);
    }
}
