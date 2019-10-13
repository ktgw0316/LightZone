/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.net.*;
import java.io.*;
import java.util.ResourceBundle;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

/** Send a Throwable to a server that will log it using an HTTP POST request.
  */

public class ErrorLogger {

    /** Send a Throwable to the server URL specified in the resource bundle.
      * <p>
      * This is a blocking method.  It waits for the HTTP exchange to complete,
      * or for an IOException.
      */
    public static void logError(final Throwable t) {
        try {
            String message = createMessage(t);
            HttpURLConnection conn = connect(message);
            OutputStream out = conn.getOutputStream();
            try (Writer writer = new OutputStreamWriter(out)) {
                writer.write(message);
            }
            conn.disconnect();
            int error = conn.getResponseCode();
            if (error != HttpURLConnection.HTTP_OK) {
                System.err.println(
                    "Error logging failed: " + conn.getResponseMessage()
                );
            }
            else {
                System.err.println("Error logging successful");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a Throwable to the server URL specified in the resource bundle,
     * and optionally fork the work to a daemon thread (so network problems
     * will not hang the app).
     * <p>
     * Forking happens when the monitor Object is not null.  In that case,
     * the logging thread will call monitor.notify after the log connection
     * closes.
     */
    public static void logError(final Throwable t, final Object monitor) {
        if (monitor == null) {
            logError(t);
        }
        else {
            Thread thread = new Thread(
                new Runnable() {
                    public void run() {
                        logError(t);
                        synchronized (monitor) {
                            monitor.notifyAll();
                        }
                    }
                },
                "Error Logger"
            );
            // If logError() blocks, don't hang the app when the user quits:
            thread.setDaemon(true);
            thread.start();
        }
    }

    // Translate a Throwable into an in-memory stack trace:
    private static String createMessage(Throwable t) {
        try {
            final String trace;
            try (StringWriter writer = new StringWriter()) {
                PrintWriter printer = new PrintWriter(writer);
                t.printStackTrace(printer);
                trace = writer.toString();
            }

            StringBuffer buffer = new StringBuffer();
            appendVersion(buffer);
            appendOsEnvironment(buffer);
            appendJavaEnvironment(buffer);

            buffer.append("error=");
            buffer.append(trace);

            String message = buffer.toString();
            message = URLEncoder.encode(message, "UTF-8");
            return message;

        } catch (IOException e) {
            return "ErrorLogger: Couldn't construct message: " + e.getMessage();
        }
    }

    private static void appendVersion(StringBuffer buffer) {
        buffer.append("url=");
        buffer.append(Version.getUri());
        buffer.append("\n");
        buffer.append("revision=");
        buffer.append(Version.getRevisionNumber());
        buffer.append("\n");
    }

    private static void appendProperty(StringBuffer buffer, String prop) {
        String s = System.getProperty(prop);
        buffer.append(prop);
        buffer.append("=");
        buffer.append(s);
        buffer.append("\n");
    }

    private static void appendOsEnvironment(StringBuffer buffer) {
        appendProperty(buffer, "os.name");
        appendProperty(buffer, "os.arch");
        appendProperty(buffer, "os.version");
    }

    private static void appendJavaEnvironment(StringBuffer buffer) {
        appendProperty(buffer, "java.version");
        appendProperty(buffer, "java.vendor");
    }

    // Do all the HTTP magic to send a string via POST request:
    private static HttpURLConnection connect(String message)
        throws IOException
    {
        HttpURLConnection conn = (HttpURLConnection) LogUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty(
            "Content-Type", "application/x-www-form-urlencoded"
        );
        String length = String.valueOf(message.length());
        conn.setRequestProperty("Content-Length", length);
        return conn;
    }

    // This resource bundle has the magic URL where the server is listening:
    private static ResourceBundle Resources = ResourceBundle.getBundle(
        "com/lightcrafts/utils/resources/ErrorLogger"
    );

    private static URL LogUrl;

    static {
        try {
            LogUrl = new URL(Resources.getString("LogUrl"));
        }
        catch (MalformedURLException e) {
            throw new RuntimeException("Misconfigured ErrorLogger URL");
        }
    }

    public static void main(String[] args) {
        try {
            // Make a fat stack trace by abusing the event queue:
            EventQueue.invokeAndWait(
                new Runnable() {
                    public void run() {
                        Throwable t = new Throwable();
                        logError(t);
                    }
                }
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
