/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.templates;

import java.io.*;
import java.util.*;

/**
 * Spools in all the LightZone predefined Templates and writes them out into
 * the folder where TemplateDatabase will find them.
 */
class TemplateDeployer {

    // Don't perform the Template copy operation more than once per VM instance,
    // no matter what.
    private static boolean hasDeployed;

    /**
     * Perform the mass copy of predefined Templates from resources to files.
     */
    static void deploy() {
        Collection<String> templates = getPredefinedTemplateNames();
        for (String template : templates) {
            try {
                migrateTemplate(template);
            }
            catch (IOException e) {
                System.out.println("Failed to migrate template " + template);
                e.printStackTrace();
            }
        }
        hasDeployed = true;
    }

    /**
     * Copy one predefined Template from resources to a file.
     */
    private static void migrateTemplate(String template) throws IOException {
        File dir = TemplateDatabase.TemplateDir;
        File file = new File(dir, template);
        ClassLoader loader = TemplateDeployer.class.getClassLoader();
        InputStream in = loader.getResourceAsStream(
            "com/lightcrafts/templates/resources/" + template
        );
        if (in == null) {
            throw new IOException(
                "Couldn't find resource for template " + template
            );
        }
        byte[] buffer = new byte[10000];    // bigger than any template
        int count;
        try (OutputStream out = new FileOutputStream(file)) {
            do {
                count = in.read(buffer);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            } while (count >= 0);
        } catch (IOException e) {
            System.out.println("Failed to close template " + template);
        }
    }

    /**
     * Try to figure out whether the predefined Templates have already been
     * migrated.
     */
    static boolean hasDeployed() {
        if (hasDeployed) {
            return true;
        }
        Collection<String> names = getPredefinedTemplateNames();
        File dir = TemplateDatabase.TemplateDir;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files == null) {
                return true;
            }
            for (File file : files) {
                String name = file.getName();
                names.remove(name);
            }
        }
        return names.isEmpty();
    }

    private static Collection<String> getPredefinedTemplateNames() {
        InputStream in = TemplateDeployer.class.getResourceAsStream(
            "resources/TemplateList"
        );
        if (in == null) {
            System.out.println("Couldn't access TemplateList");
            return Collections.emptySet();
        }
        InputStreamReader reader = new InputStreamReader(in);
        BufferedReader buffer = new BufferedReader(reader);
        String template;
        ArrayList<String> names = new ArrayList<String>();
        do {
            try {
                template = buffer.readLine();
            }
            catch (IOException e) {
                // Use whatever lines we've read so far.
                System.out.println("Error reading TemplateList");
                e.printStackTrace();
                break;
            }
            if (template != null) {
                names.add(template);
            }
        } while (template != null);
        return names;
    }

    public static void main(String[] args) throws Exception {
        TemplateDatabase.getTemplateKeys();
        System.out.println("hasDeployed = " + hasDeployed());
    }
}
