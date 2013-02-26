/*
 * $RCSfile: PropertyUtil.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:01 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codecimpl.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class PropertyUtil {

    private static Hashtable bundles = new Hashtable();
    private static String propertiesDir = "com/lightcrafts/media/jai/codec";
    
    public static InputStream getFileFromClasspath(String path) 
        throws IOException, FileNotFoundException {
        InputStream is;

        final String pathFinal = path;
        final String sep       = File.separator;
        String tmpHome = null;
        try {
            tmpHome = System.getProperty("java.home");
        } catch(Exception e) {
            tmpHome = null; // Redundant
        }
        final String home      = tmpHome;
        final String urlHeader = tmpHome == null ?
            null : home + sep + "lib"  + sep;

        if(home != null) {
            String libExtPath = urlHeader + "ext" + sep + path;
            File libExtFile = new File(libExtPath);
	    try {
		if (libExtFile.exists()) {
		    is = new FileInputStream(libExtFile);
		    if (is != null) {
			return is;
		    }
		}
	    } catch (java.security.AccessControlException e) {
		// When the files are packed into jar files, the
		// permission to access these files in a security environment
		// isn't granted in the policy files in most of the cases.
		// Thus, this java.security.AccessControlException is 
		// thrown.  To continue the searching in the jar files,
		// catch this exception and do nothing here.
		// The fix of 4531516.
	    }
        }

        is = PropertyUtil.class.getResourceAsStream("/" + path);
        if (is != null) {
            return is;
        }

        // The above call doesn't work if the jai is an installed extension
        // in the main jre/lib directory (as of 5-21-1999 the javaplugin
        // doesn't look in the ext diretory which is a bug).  We'll
        // try to load the file from either $java_home/ext/jai_core.jar
        // or $java_home/jai_core.jar.  The ext is where it should be
        // when the bug finally gets fixed.  
        PrivilegedAction p = new PrivilegedAction() {
            public Object run() {
                String localHome = null;
                String localUrlHeader = null;
                if(home != null) {
                    localHome = home;
                    localUrlHeader = urlHeader;
                } else {
                    localHome = System.getProperty("java.home");
                    localUrlHeader = localHome + sep + "lib"  + sep;
                }
                String filenames[] = {
                    localUrlHeader + "ext" + sep + "jai_core.jar",
                    localUrlHeader + "ext" + sep + "jai_codec.jar",
                    localUrlHeader + "jai_core.jar",
                    localUrlHeader + "jai_codec.jar"
                };
                
                for (int i = 0; i < filenames.length; i++) {
                    try {
                        InputStream tmpIS = 
                            getFileFromJar(filenames[i],pathFinal);
                        if (tmpIS != null) {
                            return tmpIS;
                        }
                    } catch (Exception e) {
                    }
                }

                return null;
            }
        };

        return (InputStream)AccessController.doPrivileged(p);
    }

    private static InputStream getFileFromJar(String jarFilename, 
         String path) throws Exception {
        // Look in jar file
        JarFile f = null;
        try {
            f = new JarFile(jarFilename);
        } catch (Exception e) {
        }
        JarEntry ent = f.getJarEntry(path);
        if (ent != null) {
            return f.getInputStream(ent);
        } 
        return null;
    } 
    
    /** Get bundle from .properties files in com/sun/media/jai/codec dir. */
    private static ResourceBundle getBundle(String packageName) {
        ResourceBundle bundle = null;
        
        InputStream in = null;
        try {
            in = getFileFromClasspath(propertiesDir + "/" +
                                      packageName + ".properties");
            if (in != null) {
                bundle = new PropertyResourceBundle(in);
                bundles.put(packageName, bundle);
                return bundle;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    
    public static String getString(String packageName, String key) {
        ResourceBundle b = (ResourceBundle)bundles.get(packageName);
        if (b == null) {
            b = getBundle(packageName);
        }
        return b.getString(key);
    }
    
    /**
     * Utility method to search the full list of property names for
     * matches.  If <code>propertyNames</code> is <code>null</code>
     * then <code>null</code> is returned.
     *
     * @exception IllegalArgumentException if <code>prefix</code> is
     * <code>null</code> and <code>propertyNames</code> is
     * non-<code>null</code>.
     */
    public static String[] getPropertyNames(String[] propertyNames,
					    String prefix) {
        if (propertyNames == null) {
            return null;
        } else if(prefix == null) {
            throw new IllegalArgumentException(JaiI18N.getString("PropertyUtil0"));
        }

        prefix = prefix.toLowerCase();

	Vector names = new Vector();
	for (int i = 0; i < propertyNames.length; i++) {
	    if (propertyNames[i].toLowerCase().startsWith(prefix)) {
		names.addElement(propertyNames[i]);
	    }
	}

        if (names.size() == 0) {
            return null;
        }

	// Copy the strings from the Vector over to a String array.
	String prefixNames[] = new String[names.size()];
	int count = 0;
	for (Iterator it = names.iterator(); it.hasNext(); ) {
	    prefixNames[count++] = (String)it.next();
	}

        return prefixNames;
    }
}
