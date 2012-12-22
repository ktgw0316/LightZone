/*
 * $RCSfile: RegistryFileParser.java,v $
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StreamTokenizer;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import com.lightcrafts.mediax.jai.util.CaselessStringKey;

/**
 * A class to parse the JAI registry file.
 *
 * @since JAI 1.1
 */
class RegistryFileParser {

    /**
     * Load the <code>OperationRegistry</code> with the descriptors,
     * factories and their preferences from the input stream.
     */
    static void loadOperationRegistry(OperationRegistry or,
		    ClassLoader cl, InputStream is) throws IOException {

	(new RegistryFileParser(or, cl, is)).parseFile();
    }

    /**
     * Load the <code>OperationRegistry</code> with the descriptors,
     * factories and their preferences from the <code>URL</code>.
     */
    static void loadOperationRegistry(OperationRegistry or,
		    ClassLoader cl, URL url) throws IOException {

	(new RegistryFileParser(or, cl, url)).parseFile();
    }

    private URL url;
    private InputStream is;
    private ClassLoader classLoader;

    // The OperationRegistry being read in.
    private OperationRegistry or;
    private StreamTokenizer st;

    // The current token - the one last returned by StreamTokenizer
    private int token;

    // The current line number being parsed in the registry file.
    private int lineno;

    // Table used to map local-factory names to factory instances
    // on a per mode basis.
    private Hashtable localNamesTable;

    /**
     * Create a JAI registry file parser from an <code>URL</code>
     */
    private RegistryFileParser(OperationRegistry or, ClassLoader cl, URL url)
	    throws IOException {

	this(or, cl, url.openStream());
	this.url = url;
    }

    /**
     * Create a JAI registry file parser from the <code>InputStream</code>
     */
    private RegistryFileParser(OperationRegistry or, ClassLoader cl, InputStream is)
	    throws IOException {

	if (or == null)
	    or = JAI.getDefaultInstance().getOperationRegistry();

	this.is  = is;
	this.url = null;
	this.or  = or;
	this.classLoader  = cl;

	// Set up streamtokenizer
	BufferedReader reader =
	    new BufferedReader(new InputStreamReader(is));

	st = new StreamTokenizer(reader);

	st.commentChar('#');
	st.eolIsSignificant(true);
	st.slashSlashComments(true);
	st.slashStarComments(true);

	token  = st.ttype;
	lineno = -1;

	// Initialize a table to map local names to factories.

	localNamesTable = new Hashtable();

	String modeNames[] = RegistryMode.getModeNames();

	for (int i = 0; i < modeNames.length; i++)
	    localNamesTable.put(
		new CaselessStringKey(modeNames[i]), new Hashtable());
    }

    /**
     * Skip all the empty tokens generated due to empty lines
     * and comments.
     */
    private int skipEmptyTokens() throws IOException {

	while (st.sval == null) {
	    if (token == StreamTokenizer.TT_EOF)
		return token;

	    token = st.nextToken();
	}

	return token;
    }

    /**
     * Get an array of <code>String</code>s of words in the
     * next line after skipping over empty and comment lines.
     */
    private String[] getNextLine() throws IOException {

	if (skipEmptyTokens() == StreamTokenizer.TT_EOF)
	    return null;

	Vector v = new Vector();

	lineno = st.lineno();

	while ((token != StreamTokenizer.TT_EOL) && 
	       (token != StreamTokenizer.TT_EOF)) {

	    if (st.sval != null)
		v.addElement(st.sval);

	    token = st.nextToken();
	}

	if (v.size() == 0)
	    return null;

	return (String[])v.toArray(new String[0]);
    }

    // Aliases for backward compatibility
    private static String[][] aliases = {
	{ "odesc"   , "descriptor"  },
	{ "rif"	    , "rendered"    },
	{ "crif"    , "renderable"  },
	{ "cif"	    , "collection"  },
    };

    /**
     * Map old keywords to the new keywords
     */
    private String mapName(String key) {
	for (int i = 0; i < aliases.length; i++)
	    if (key.equalsIgnoreCase(aliases[i][0]))
		return aliases[i][1];

	return key;
    }

    /**
     * Create an instance given the class name.
     */
    private Object getInstance(String className) {

	try {
	    Class descriptorClass = null;
	    String errorMsg = null;

	    // Since the classes listed in the registryFile can
	    // reside anywhere (core, ext, classpath or the specified
	    // classloader) we have to try every place.

	    // First try the specified classloader
	    if (classLoader != null) {
		try {
		    descriptorClass = Class.forName(className,
			    true, classLoader);
		} catch (Exception e) {
		    errorMsg = e.getMessage();
		}
	    }

	    // Next try the callee classloader
	    if (descriptorClass == null) {
		try {
		    descriptorClass = Class.forName(className);
		} catch (Exception e) {
		    errorMsg = e.getMessage();
		}
	    }

	    // Then try the System classloader (because the specified
	    // classloader might be null and the callee classloader
	    // might be an ancestor of the SystemClassLoader
	    if (descriptorClass == null) {
		try {
		    descriptorClass = Class.forName(className,
			    true, ClassLoader.getSystemClassLoader());
		} catch (Exception e) {
		    errorMsg = e.getMessage();
		}
	    }

	    if (descriptorClass == null) {
		registryFileError(errorMsg);
		return null;
	    }

	    return descriptorClass.newInstance();

	} catch (Exception e) {
	    registryFileError(e.getMessage());
	    e.printStackTrace();
	}

	return null;
    }

    /**
     * Parse the entire registry file and load internal structures
     * with the info.
     */
    boolean parseFile() throws IOException {

	// If the file has already been parsed do nothing.
	if (token == StreamTokenizer.TT_EOF)
	    return true;

	String[] keys;

	token = st.nextToken();

	while (token != StreamTokenizer.TT_EOF) {

	    if ((keys = getNextLine()) == null)
		break;

	    RegistryMode mode;

	    String key = mapName(keys[0]);

	    // This indicates a new registry mode to be added.
	    if (key.equalsIgnoreCase("registryMode")) {

		mode = (RegistryMode)getInstance(keys[1]);

		if (mode != null) {
		    if (RegistryMode.addMode(mode) == false)
			registryFileError(
			    JaiI18N.getString("RegistryFileParser10"));
		}

	    // Old format operation-descriptor line OR
	    // the new generic RegistryElementDescriptor line
	    } else if (key.equalsIgnoreCase("descriptor")) {

		registerDescriptor(keys);

	    // If it is a registry mode name, then register the
	    // factory object.
	    } else if ((mode = RegistryMode.getMode(key)) != null) {

		registerFactory(mode, keys);

	    // If the line starts with a "pref" there are two options
	    } else if (key.equalsIgnoreCase("pref")) {

		key = mapName(keys[1]);

		// If what follows is the keyword "product" then
		// it is assumed to be setting product preferences
		// for the "rendered" mode (old file format)
		if (key.equalsIgnoreCase("product")) {

		    setProductPreference(
			RegistryMode.getMode("rendered"), keys);

		// If it is followed by a modeName then it is
		// for setting preferences between factory object.
		} else if ((mode = RegistryMode.getMode(key)) != null) {

		    setFactoryPreference(mode, keys);

		} else {
		    registryFileError(JaiI18N.getString("RegistryFileParser4"));
		}

	    // For setting product preferences
	    } else if (key.equalsIgnoreCase("productPref")) {

		key = mapName(keys[1]);

		// If it is followed by a modeName then it is
		// for setting preferences between products
		if ((mode = RegistryMode.getMode(key)) != null) {

		    setProductPreference(mode, keys);

		} else {
		    registryFileError(JaiI18N.getString("RegistryFileParser5"));
		}
	    } else {
		registryFileError(JaiI18N.getString("RegistryFileParser6"));
	    }
	}

	// If this was read in from an URL, we created the InputStream
	// and so we should close it.
	if (url != null)
	    is.close();

	return true;
    }

    /**
     * Register a descriptor with operation registry.
     */
    private void registerDescriptor(String[] keys) {

	if (keys.length >= 2) {

	    RegistryElementDescriptor red =
		(RegistryElementDescriptor)getInstance(keys[1]);

	    if (red != null) {
		try {
		    or.registerDescriptor(red);
		} catch (Exception e) {
		    registryFileError(e.getMessage());
		}
	    }

	} else {
	    registryFileError(JaiI18N.getString("RegistryFileParser1"));
	}
    }

    /**
     * Register a factory instance against a registry mode
     * under given product and local-name.
     */
    private void registerFactory(RegistryMode mode, String[] keys) {

	Object factory;

	if (mode.arePreferencesSupported()) {

	    if (keys.length >= 5) {

		if ((factory = getInstance(keys[1])) != null) {
		    try {
			or.registerFactory(
			    mode.getName(), keys[3], keys[2], factory);

			mapLocalNameToObject(mode.getName(), keys[4], factory);

		    } catch (Exception e) {
			registryFileError(e.getMessage());
		    }
		}

	    } else {
		registryFileError(
		    JaiI18N.getString("RegistryFileParser2"));
	    }

	} else {
	    if (keys.length >= 3) {

		if ((factory = getInstance(keys[1])) != null) {
		    try {
			or.registerFactory(
			    mode.getName(), keys[2], null, factory);

		    } catch (Exception e) {
			registryFileError(e.getMessage());
		    }
		}

	    } else {
		registryFileError(
		    JaiI18N.getString("RegistryFileParser3"));
	    }
	}
    }

    /**
     * Register a factory instance against a registry mode
     * under given product and local-name.
     */
    private void setProductPreference(RegistryMode mode, String[] keys) {

	String modeName = mode.getName();

	if (mode.arePreferencesSupported()) {

	    if (keys.length >= 5) {

		try {
		    or.setProductPreference(
			modeName, keys[2], keys[3], keys[4]);

		} catch (Exception e) {
		    registryFileError(e.getMessage());
		}

	    } else {
		registryFileError(
		    JaiI18N.getString("RegistryFileParser5"));
	    }

	} else {
	    registryFileError(JaiI18N.getString("RegistryFileParser9"));
	}
    }

    /**
     * Register a factory instance against a registry mode
     * under given product and local-name.
     */
    private void setFactoryPreference(RegistryMode mode, String[] keys) {

	String modeName = mode.getName();
	Object factory;

	if (mode.arePreferencesSupported()) {

	    if (keys.length >= 6) {

		Object preferred = getObjectFromLocalName(modeName, keys[4]);
		Object other     = getObjectFromLocalName(modeName, keys[5]);

		if ((preferred != null) && (other != null)) {

		    try {
			or.setFactoryPreference(
			    modeName, keys[2], keys[3], preferred, other);

		    } catch (Exception e) {
			registryFileError(e.getMessage());
		    }
		}

	    } else {
		registryFileError(
		    JaiI18N.getString("RegistryFileParser4"));
	    }

	} else {
	    registryFileError(JaiI18N.getString("RegistryFileParser7"));
	}
    }

    /**
     * Map local names to factory instances, so that they can
     * be used to directly set preferences later.
     */
    private void mapLocalNameToObject(String modeName, String localName, Object factory) {

	Hashtable modeTable = (Hashtable)
	    localNamesTable.get(new CaselessStringKey(modeName));

	modeTable.put(new CaselessStringKey(localName), factory);
    }

    /**
     * Get object registered under the local name for under the mode.
     */
    private Object getObjectFromLocalName(String modeName, String localName) {

	Hashtable modeTable = (Hashtable)
	    localNamesTable.get(new CaselessStringKey(modeName));

	Object obj = modeTable.get(new CaselessStringKey(localName));

	if (obj == null)
	    registryFileError(localName + ": " +
		    JaiI18N.getString("RegistryFileParser8"));

	return obj;
    }

    private boolean headerLinePrinted = false;

    /**
     * Print the line number and then print the passed in message.
     */
    private void registryFileError(String msg) {

	if (!headerLinePrinted) {

	    if (url != null) {
		errorMsg(JaiI18N.getString("RegistryFileParser11"),
		    new Object[] { url.getPath() });
	    }

	    headerLinePrinted = true;
	}

	errorMsg(JaiI18N.getString("RegistryFileParser0"),
		 new Object[] { new Integer(lineno) });

	if (msg != null)
	    errorMsg(msg, null);
    }

    /**
     * Creates a <code>MessageFormat</code> object and set the
     * <code>Locale</code> to default and formats the message
     */
    private void errorMsg(String key, Object[] args) {
        MessageFormat mf = new MessageFormat(key);
        mf.setLocale(Locale.getDefault());

	if (System.err != null)
	    System.err.println(mf.format(args));
    }

    /**
     * Write the OperationRegistry out to the output stream.
     */
    static void writeOperationRegistry(OperationRegistry or,
			   OutputStream os) throws IOException {

	writeOperationRegistry(or,
	    new BufferedWriter(new OutputStreamWriter(os)));
    }

    /**
     * Write the OperationRegistry out to the output stream.
     */
    static void writeOperationRegistry(OperationRegistry or,
			   BufferedWriter bw) throws IOException {

	// First cycle through all the descriptor classes
	Iterator dcit = RegistryMode.getDescriptorClasses().iterator();

	String tab = "  ";

	while (dcit.hasNext()) {

	    Class descriptorClass = (Class)dcit.next();

	    List descriptors = or.getDescriptors(descriptorClass);

	    // First write all the descriptors corresponding
	    // to this descriptorClass
	    bw.write("#"); bw.newLine();
	    bw.write("# Descriptors corresponding to class : " + descriptorClass.getName()); bw.newLine();
	    bw.write("#"); bw.newLine();

	    if ((descriptors == null) || (descriptors.size() <= 0)) {
		bw.write("# <EMPTY>"); bw.newLine();
	    } else {
		
		Iterator it = descriptors.iterator();

		while (it.hasNext()) {
		    bw.write("descriptor" + tab);
		    bw.write(it.next().getClass().getName());
		    bw.newLine();
		}
	    }
	    bw.newLine();

	    // Now cycle through all registry modes associated
	    // with this descriptorClass and write out the
	    // factories and their preferences

	    String modeNames[] = RegistryMode.getModeNames(descriptorClass);

	    boolean empty;
	    int i, j, k, l;

	    for (i = 0; i < modeNames.length; i++) {
		bw.write("#"); bw.newLine();
		bw.write("# Factories registered under mode : " + modeNames[i]); bw.newLine();
		bw.write("#"); bw.newLine();

		RegistryMode mode = RegistryMode.getMode(modeNames[i]);

		boolean prefs = mode.arePreferencesSupported();

		String[] descriptorNames =
			    or.getDescriptorNames(modeNames[i]);

		// Over all descriptor names for this mode.
		for (j = 0, empty = true; j < descriptorNames.length; j++) {

		    if (prefs) {
			Vector productVector =
			    or.getOrderedProductList(modeNames[i],
						     descriptorNames[j]);

			if (productVector == null)
			    continue;

			String[] productNames =
			    (String[])productVector.toArray(new String[0]);

			// Over all products under which there are
			// factories registered under this descriptor name
			for (k = 0; k < productNames.length; k++) {

			    List factoryList =
				or.getOrderedFactoryList(modeNames[i],
							 descriptorNames[j],
							 productNames[k]);

			    Iterator fit = factoryList.iterator();

			    while (fit.hasNext()) {
				Object instance = fit.next();

				if (instance == null)
				    continue;

				bw.write(modeNames[i] + tab);
				bw.write(instance.getClass().getName() + tab);
				bw.write(productNames[k] + tab);
				bw.write(descriptorNames[j] + tab);
				bw.write(or.getLocalName(modeNames[i], instance));
				bw.newLine();

				empty = false;
			    }
			}
		    } else {
			Iterator fit = or.getFactoryIterator(
					modeNames[i], descriptorNames[j]);

			while (fit.hasNext()) {
			    Object instance = fit.next();

			    if (instance == null)
				continue;

			    bw.write(modeNames[i] + tab);
			    bw.write(instance.getClass().getName() + tab);
			    bw.write(descriptorNames[j]);
			    bw.newLine();

			    empty = false;
			}
		    }
		}

		if (empty) {
		    bw.write("# <EMPTY>"); bw.newLine();
		}
		bw.newLine();

		// If the mode does not support preferences
		// then just continue
		if (!prefs) {
		    bw.write("#"); bw.newLine();
		    bw.write("# Preferences not supported for mode : " + modeNames[i]); bw.newLine();
		    bw.write("#"); bw.newLine();
		    bw.newLine();
		    continue;
		}

		// Next, write the product preferences for this mode
		bw.write("#"); bw.newLine();
		bw.write("# Product preferences for mode : " + modeNames[i]); bw.newLine();
		bw.write("#"); bw.newLine();

		for (j = 0, empty = true; j < descriptorNames.length; j++) {

		    String[][] productPrefs =
			    or.getProductPreferences(modeNames[i],
						     descriptorNames[j]);
		    if (productPrefs == null)
			continue;

		    for (k = 0; k < productPrefs.length; k++) {
			bw.write("productPref" + tab);
			bw.write(modeNames[i] + tab);
			bw.write(descriptorNames[j] + tab);
			bw.write(productPrefs[k][0] + tab);
			bw.write(productPrefs[k][1]);
			bw.newLine();

			empty = false;
		    }
		}

		if (empty) {
		    bw.write("# <EMPTY>"); bw.newLine();
		}
		bw.newLine();

		// Next, write the factory preferences for this mode
		bw.write("#"); bw.newLine();
		bw.write("# Factory preferences for mode : " + modeNames[i]); bw.newLine();
		bw.write("#"); bw.newLine();

		// Over all descriptor names for this mode.
		for (j = 0, empty = true; j < descriptorNames.length; j++) {

		    if (prefs) {
			Vector productVector =
			    or.getOrderedProductList(modeNames[i],
						     descriptorNames[j]);

			if (productVector == null)
			    continue;

			String[] productNames =
			    (String[])productVector.toArray(new String[0]);

			// Over all products under which there are
			// factories registered under this descriptor name
			for (k = 0; k < productNames.length; k++) {

			    Object fprefs[][] = or.getFactoryPreferences(
				modeNames[i], descriptorNames[j], productNames[k]);

			    if (fprefs == null)
				continue;

			    for (l = 0; l < fprefs.length; l++) {
				bw.write("pref" + tab);
				bw.write(modeNames[i] + tab);
				bw.write(descriptorNames[j] + tab);
				bw.write(productNames[k] + tab);
				bw.write(or.getLocalName(modeNames[i], fprefs[l][0]) + tab);
				bw.write(or.getLocalName(modeNames[i], fprefs[l][1]));
				bw.newLine();

				empty = false;
			    }
			}
		    }
		}

		if (empty) {
		    bw.write("# <EMPTY>"); bw.newLine();
		}
		bw.newLine();
	    }
	}

	bw.flush();
    }
}
