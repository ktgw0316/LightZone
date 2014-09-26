/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Oct 18, 2006
 * Time: 3:37:05 PM
 * To change this template use File | Settings | File Templates.
 */
public final class ForkDaemon {

    ////////// public /////////////////////////////////////////////////////////

    public static ForkDaemon INSTANCE;

    public InputStream getStdErr() {
        return stderr;
    }

    public OutputStream getStdIn() {
        return stdin;
    }

    public InputStream getStdOut() {
        return stdout;
    }

    public synchronized void invoke( String[] args ) {
        try {
            // Flush whatever is hanging in the pipes
            int bytesAvailable = stdout.available();
            if ( bytesAvailable > 0 )
                stdout.read( new byte[ bytesAvailable ] );
            bytesAvailable = stderr.available();
            if ( bytesAvailable > 0 )
                stderr.read( new byte[ bytesAvailable ] );

            stdin.write( CMDSTART.getBytes( "UTF-8" ) );
            stdin.write( '\n' );
            // System.err.print("Invoking: ");
            for ( String arg : args ) {
                // System.err.print(arg + ' ');
                stdin.write( arg.getBytes( "UTF-8" ) );
                stdin.write( '\n' );
            }
            // System.err.println();
            stdin.write( CMDEND.getBytes( "UTF-8" ) );
            stdin.write( '\n' );
            stdin.flush();
        }
        catch ( IOException e ) {
            // TODO: merely printing this is insufficient -- pjl
            e.printStackTrace();
        }
    }

    public static void start() throws IOException {
        INSTANCE = new ForkDaemon();
    }

    ////////// protected //////////////////////////////////////////////////////

    protected void finalize() throws Throwable {
        IOException caughtException = null;
        try {
            stderr.close();
        }
        catch ( IOException e ) {
            caughtException = e;
        }
        try {
            stdout.close();
        }
        catch ( IOException e ) {
            if ( caughtException == null )
                caughtException = e;
        }
        try {
            stdin.close();
        }
        catch ( IOException e ) {
            if ( caughtException == null )
                caughtException = e;
        }
        forkerProcess.destroy();
        if ( caughtException != null )
            throw caughtException;
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    private ForkDaemon() throws IOException {
        forkerProcess = Runtime.getRuntime().exec( FORKDAEMON_PATH );
        stdin = forkerProcess.getOutputStream();
        stdout = forkerProcess.getInputStream();
        stderr = new BufferedInputStream(forkerProcess.getErrorStream());
    }

    private static final String FORKDAEMON_NAME = "LightZone-forkd";
    private static String FORKDAEMON_PATH;

    private static final String CMDSTART = "CMDSTART";
    private static final String CMDEND = "CMDEND";

    private final Process forkerProcess;
    private final OutputStream stdin;
    private final InputStream stdout;
    private final InputStream stderr;

    static {
        FORKDAEMON_PATH = System.getProperty( "java.library.path" ) + File.separatorChar + FORKDAEMON_NAME;
        if (! new File(FORKDAEMON_PATH).canExecute()) {
            String dir = System.getProperty( "install4j.appDir" );
            if ( dir == null )
                dir = ".";
            FORKDAEMON_PATH = dir + File.separatorChar + FORKDAEMON_NAME;
        }
    }

    ////////// main() for testing /////////////////////////////////////////////

    public static void main( String[] args ) {
        try {
            final ForkDaemon daemon = new ForkDaemon();
            final byte buffer[] = new byte[256];

            daemon.invoke( new String[]{ "./dcraw" } );
            while ( true ) {
                final int chars = daemon.stderr.read( buffer );
                if ( chars < 0 )
                    break;
                System.out.println( new String( buffer, 0, chars ) );
            }
            while ( true ) {
                if ( daemon.stdout.available() > 0 ) {
                    final int chars = daemon.stdout.read( buffer );
                    if ( chars < 0 )
                        break;
                    System.out.println( new String( buffer, 0, chars ) );
                }
            }
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
/* vim:set et sw=4 ts=4: */
