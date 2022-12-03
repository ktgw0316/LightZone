/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.test;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main() method takes the file system path to a java source root and
 * generates a hierarchy of new test classes, one per package, to exercise
 * all calls it finds to Locale.get().  It also generates one more test class
 * in the root directory with a main() method, to invoke all these tests that
 * are produced.
 */
public class LocaleTestGenerator {

    static File[] getSourceFiles(File pkgDir) {
        return pkgDir.listFiles(
            new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".java") &&
                        ! pathname.getName().equals("LocaleTest.java");
                }
            }
        );
    }

    static ArrayList<File> getAllPackageDirs(File sourceRoot) {
        File[] dirs = sourceRoot.listFiles(
            new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            }
        );
        ArrayList<File> list = new ArrayList<File>();
        for (File dir : dirs) {
            list.add(dir);
            list.addAll(getAllPackageDirs(dir));
        }
        return list;
    }

    // Pass in the source root path.
    public static void main(String[] args) throws IOException {
        File root = new File(args[0]);
        ArrayList<File> pkgs = getAllPackageDirs(root);
        StringBuffer globalBuffer = new StringBuffer();
        for (File pkg : pkgs) {
            File[] srcs = getSourceFiles(pkg);
            StringBuffer pkgBuffer = new StringBuffer();
            String pkgName = null;
            File outFile = null;
            for (File src : srcs) {
                BufferedReader reader = new BufferedReader(new FileReader(src));
                Pattern pattern = Pattern.compile("(?m)(?s)^.*(LOCALE.get[^\"]*\"[^\"]*\").*$");
                if (pkgName == null) {
                    String pkgStmnt = reader.readLine();
                    pkgName = pkgStmnt.replaceFirst("package (.*);", "$1");
                    pkgBuffer.append(pkgStmnt).append('\n');
                    pkgBuffer.append("import static ").append(pkgName).append(".Locale.LOCALE;\n");
                    pkgBuffer.append("public class LocaleTest {\n");
                    pkgBuffer.append("    public static void run() {\n");
                }
                while (reader.ready()) {
                    String line = reader.readLine();
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        if (outFile == null) {
                            outFile = new File(pkg, "LocaleTest.java");
                        }
                        String exp = matcher.replaceAll("$1");
                        pkgBuffer.append("        ").append(exp).append(");\n");
                    }
                }
            }
            if (outFile != null) {
                pkgBuffer.append("    }\n");
                pkgBuffer.append("}\n");
                FileWriter writer = new FileWriter(outFile);
                writer.write(pkgBuffer.toString());
                writer.close();
                globalBuffer.append("        try {\n");
                globalBuffer.append("            ").append(pkgName).append(".LocaleTest.run();\n");
                globalBuffer.append("        }\n");
                globalBuffer.append("        catch (MissingResourceException e) {\n");
                globalBuffer.append("            System.err.println(\"").append(pkgName).append(" \" + e.getMessage().replaceAll(\".* key \", \"\"));\n");
                globalBuffer.append("        }\n");
                System.out.println("wrote " + outFile.getAbsolutePath());
            }
        }
        File globalTestFile = new File(root, "LocaleTest.java");
        FileWriter writer = new FileWriter(globalTestFile);
        writer.write("import java.util.MissingResourceException;\n");
        writer.write("public class LocaleTest {\n");
        writer.write("    public static void main(String[] args) {\n");        
        writer.write(globalBuffer.toString());
        writer.write("    }\n");
        writer.write("}\n");
        writer.close();
        System.out.println("wrote " + globalTestFile.getAbsolutePath());
    }
}
