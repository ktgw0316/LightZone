/*
 * $RCSfile: RegistryMode.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:19 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import com.lightcrafts.mediax.jai.registry.CollectionRegistryMode;
import com.lightcrafts.mediax.jai.registry.RemoteRenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RemoteRenderedRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderableCollectionRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderableRegistryMode;
import com.lightcrafts.mediax.jai.registry.RenderedRegistryMode;
import com.lightcrafts.mediax.jai.registry.TileDecoderRegistryMode;
import com.lightcrafts.mediax.jai.registry.TileEncoderRegistryMode;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A class which provides information about a registry mode. The
 * static methods of the class act to maintain a global list of
 * known modes. All <code>RegistryMode</code>s known
 * to JAI are added to this list when this class is loaded. The
 * <code>RegistryMode</code>s installed by JAI cannot be
 * replaced or removed.
 *
 * The <code>String</code>s used to represent the registry modes
 * are all used in a case-insensitive manner.
 *
 * @since JAI 1.1
 */
public class RegistryMode {

    /**
     * Cache of known RegistryMode-s hashed by
     * <code>CaselessStringKey</code>s which wraps a String and performs
     * case-insensitive equals()
     */
    private static Hashtable registryModes;

    /**
     * Set of <code>CaselessStringKey</code>s of registryModes which
     * cannot be replaced or removed.
     */
    private static HashSet immutableNames;

    // Instance variables.
    private CaselessStringKey name;
    private Class   descriptorClass;
    private Class   productClass;
    private Method  factoryMethod;
    private boolean arePreferencesSupported;
    private boolean arePropertiesSupported;

    // Load all JAI-defined registryModes.
    static {
        registryModes  = new Hashtable(4);
	immutableNames = new HashSet();

	// operation modes
        addMode(new RenderedRegistryMode(), true);
        addMode(new RenderableRegistryMode(), true);
        addMode(new CollectionRegistryMode(), true);
        addMode(new RenderableCollectionRegistryMode(), true);

	// remote modes
        addMode(new RemoteRenderedRegistryMode(), true);
        addMode(new RemoteRenderableRegistryMode(), true);

	// Tilecodec modes
        addMode(new TileEncoderRegistryMode(), true);
        addMode(new TileDecoderRegistryMode(), true);
    }

    /**
     * Adds a new RegistryMode to the existing list. If immutable is
     * "true" then the CaselessStringKey is added to immutableNames
     * also. This mode must <u>not</u> already exist in the list.
     */
    private static boolean addMode(RegistryMode mode, boolean immutable) {

	if (registryModes.containsKey(mode.name))
	    return false;

	registryModes.put(mode.name, mode);

	if (immutable)
	    immutableNames.add(mode.name);

	return true;
    }

    /**
     * Adds a new <code>RegistryMode</code> to the existing list. This
     * succeeds only if the mode is <u>not</u> already present in the
     * list. New <code>RegistryMode</code> names can not clash (in a
     * case insensitive manner) with the ones installed by JAI (done
     * statically when this class is loaded)
     *
     * @param mode the new RegistryMode to be added to list
     *
     * @return false if the mode was already in the list. true otherwise
     */
    public synchronized static boolean addMode(RegistryMode mode) {
	return addMode(mode, false);
    }

    /**
     * Removes a mode from the existing list of known registryModes.
     * If the mode is one of the JAI-installed ones, it can not
     * be removed.
     *
     * @param mode the RegistryMode to be removed from the list
     *
     * @return false if the mode can not be removed because it was added
     *	       by JAI or because the mode was not previously add.
     *	       returns true otherwise.
     */
    public synchronized static boolean removeMode(String name) {

	CaselessStringKey key = new CaselessStringKey(name);

	if (immutableNames.contains(key))
	    return false;

	return registryModes.remove(key) != null;
    }

    /**
     * Get the list of the known registry mode names.
     *
     * @return <code>null</code>, if there are no registered modes.
     * Otherwise returns an array of <code>String</code>s of registered
     * mode names.
     */
    public static synchronized String[] getModeNames() {

	String names[] = new String[registryModes.size()];

	int i = 0;

        for (Enumeration e = registryModes.keys(); e.hasMoreElements();) {
            CaselessStringKey key = (CaselessStringKey)e.nextElement();

	    names[i++] = key.getName();
	}

	if (i <= 0)
	    return null;

	return names;
    }

    /**
     * Get a list of all known registry modes associated with the
     * specified <code>descriptorClass</code>.
     *
     * @param descriptorClass a <code>Class</code>
     *
     * @return <code>null</code> if there are no modes registered
     * against the specified descriptorClass. Otherwise returns an
     * array of <code>String</code>s of mode names associated with the
     * descriptorClass.
     */
    public static synchronized String[] getModeNames(Class descriptorClass) {

	String names[] = new String[registryModes.size()];

	int i = 0;

        for (Enumeration e = registryModes.elements(); e.hasMoreElements();) {
            RegistryMode mode = (RegistryMode)e.nextElement();

	    if (mode.getDescriptorClass() == descriptorClass)
		names[i++] = mode.getName();
	}

	if (i <= 0)
	    return null;

	String matchedNames[] = new String[i];

	for (int j = 0; j < i; j++)
	    matchedNames[j] = names[j];

	return matchedNames;
    }

    /**
     * Get the registry mode corresponding to this name.
     */
    public static RegistryMode getMode(String name) {

	CaselessStringKey key = new CaselessStringKey(name);

	return (RegistryMode)registryModes.get(key);
    }

    /**
     * Get a <code>Set</code> of all descriptor classes over
     * all registry modes.
     */
    public static synchronized Set getDescriptorClasses() {
	HashSet set = new HashSet();

        for (Enumeration e = registryModes.elements(); e.hasMoreElements();) {
            RegistryMode mode = (RegistryMode)e.nextElement();

	    set.add(mode.descriptorClass);
	}

	return set;
    }

    /**
     * Constructor. Protected access allows only instantiation of
     * subclasses.
     *
     * @param name name of the registry mode
     * @param descriptorClass the specific sub-class of
     *		<code>RegistryElementDescriptor</code> associated with
     *		this registry mode.
     * @param productClass the <code>Class</code> of the objects
     *		produced by this registry mode. This would typically
     *		be <code>factoryMethod.getReturnType()</code>.
     * @param factoryMethod the method used to "create" an object.
     * @param arePreferencesSupported does this registry mode support
     *          preferences between products or instances of the "modes"
     * @param arePropertiesSupported do properties have to be managed
     *		for this registry mode.
     */
    protected RegistryMode(String name,
			   Class descriptorClass,
			   Class productClass,
                           Method factoryMethod,
			   boolean arePreferencesSupported,
                           boolean arePropertiesSupported) {

	this.name = new CaselessStringKey(name);
	this.descriptorClass = descriptorClass;
	this.productClass = productClass;
	this.factoryMethod = factoryMethod;
	this.arePreferencesSupported = arePreferencesSupported;
	this.arePropertiesSupported = arePropertiesSupported;
    }

    /** Get the registry mode name (case-preserved) */
    public final String getName() {
	return name.getName();
    }

    /** Get the factory method that corresponds to "create" */
    public final Method getFactoryMethod() {
	return factoryMethod;
    }

    /** Does this registry mode support preferences ? */
    public final boolean arePreferencesSupported() {
	return arePreferencesSupported;
    }

    /**
     * Are properties to be managed for this registry mode ?
     */
    public final boolean arePropertiesSupported() {
	return arePropertiesSupported;
    }

    /**
     * Returns the descriptor class that corresponds to this registry mode.
     *
     * For eg. this would be OperationDescriptor for rendered, renderable,
     * collection ...  and TileCodecDescriptor for tilecodec etc.
     */
    public final Class getDescriptorClass() {
	return descriptorClass;
    }

    /**
     * The <code>Class</code> of the objects produced by this
     * registry mode.
     */
    public final Class getProductClass() {
	return productClass;
    }

    /**
     * A convenience method which essentially returns
     * getFactoryMethod().getDeclaringClass()
     */
    public final Class getFactoryClass() {
	return factoryMethod.getDeclaringClass();
    }
}
