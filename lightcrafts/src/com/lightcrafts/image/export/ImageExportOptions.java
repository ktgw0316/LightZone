/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An <code>ImageExportOptions</code> is a collection of options that can be
 * set for exporting an image to a particular image file format such as JPEG or
 * TIFF.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class ImageExportOptions {

    public final OriginalWidthOption    originalWidth;
    public final OriginalHeightOption   originalHeight;

    /**
     * Checks this <code>ImageExportOptions</code> to another object for
     * equality.
     *
     * @param o The other object to compare to.
     * @return Returns <code>true</code> only if the other object is also an
     * <code>ImageExportOptions</code> and they both are for the same
     * {@link ImageType} and all of their options are equal.
     */
    public boolean equals( Object o ) {
        if ( o == null || !o.getClass().equals( getClass() ) )
            return false;
        final ImageExportOptions otherOpts = (ImageExportOptions)o;

        final File otherExportFile = otherOpts.getExportFile();
        if ( otherExportFile == null ) {
            if ( getExportFile() != null )
                return false;
        } else if ( !otherExportFile.equals( getExportFile() ) )
            return false;

        if ( !otherOpts.getImageType().equals( getImageType() ))
            return false;

        final Map<String,ImageExportOption> om = otherOpts.m_exportOptions;
        if ( om.size() != m_exportOptions.size() )
            return false;
        for ( Map.Entry<String,ImageExportOption> me : om.entrySet() ) {
            final ImageExportOption opt = m_exportOptions.get( me.getKey() );
            if ( opt == null || !opt.equals( me.getValue() ) )
                return false;
        }
        return true;
    }

    /**
     * Gets the auxiliary data that is to be or was used to export an image
     * file.
     *
     * @return Returns said data or <code>null</code> is no data was ever set.
     * @see #setAuxData(byte[])
     */
    public final byte[] getAuxData() {
        return m_auxData;
    }

    /**
     * Gets the {@link File} that is to be or was used to export an image file.
     *
     * @return Returns said file or <code>null</code> is no such file was ever
     * set.
     * @see #setExportFile(File)
     */
    public final File getExportFile() {
        return m_exportFile;
    }

    /**
     * Gets the {@link ImageType} these options are for.
     *
     * @return Returns said {@link ImageType}.
     */
    public final ImageType getImageType() {
        return m_imageType;
    }

    /**
     * Gets the integer value of an option assumed to be an
     * {@link IntegerExportOption}.
     *
     * @param className The name of the class of the option to get the value
     * of.
     * @return If this set of options contains the requested option, returns
     * its integer value; zero otherwise.
     * @throws ClassCastException if the requested option is not an
     * {@link IntegerExportOption}.
     * @see #getOption(String)
     * @see #setIntValueOf(String,int)
     */
    public final int getIntValueOf( String className ) {
        final IntegerExportOption option =
            (IntegerExportOption)getOption( fixClassName( className ) );
        return option != null ? option.getValue() : 0;
    }

    /**
     * Gets a given {@link ImageExportOption}.
     *
     * @param className The name of the class of the option.
     * @return Returns the {@link ImageExportOption} if this set of options
     * contains the requested option, <code>null</code> otherwise.
     * @see #getIntValueOf(String)
     */
    public final ImageExportOption getOption( String className ) {
        return m_exportOptions.get( fixClassName( className ) );
    }

    /**
     * Returns the hash code for this <code>ImageExportOptions</code>.
     *
     * @return Returns said hash code.
     */
    public final int hashCode() {
        return m_imageType.hashCode() + m_exportOptions.hashCode() + 1;
    }

    /**
     * Sets the auxiliary data that is to be or was used to export an image
     * file.
     *
     * @param data The data to use.
     * @see #getAuxData()
     */
    public final void setAuxData( byte[] data ) {
        m_auxData = data;
    }

    /**
     * Sets the {@link File} that is to be or was used to export an image file.
     *
     * @param file The {@link File} to use.
     * @see #getExportFile()
     */
    public final void setExportFile( File file ) {
        m_exportFile = file;
    }

    /**
     * Sets the integer value of an option assumed to be an
     * {@link IntegerExportOption}.
     *
     * @param className The name of the class of the option to set the value
     * of.
     * @param newValue The new value.
     * @throws ClassCastException if the requested option is not an
     * {@link IntegerExportOption}.
     * @throws IllegalArgumentException if there is no option having the given
     * class name.
     * @see #getIntValueOf(String)
     * @see #setValueOf(String,String)
     */
    public final void setIntValueOf( String className, int newValue ) {
        final IntegerExportOption option =
            (IntegerExportOption)getOption( fixClassName( className ) );
        if ( option == null )
            throw new IllegalArgumentException( className );
        option.setValue( newValue );
    }

    /**
     * Sets the value of an option.
     *
     * @param className The name of the class of the option to set the value
     * of.
     * @param newValue The new value.
     * @throws IllegalArgumentException if there is no option having the given
     * class name.
     * @see #setIntValueOf(String,int)
     */
    public final void setValueOf( String className, String newValue ) {
        final ImageExportOption option = getOption( className );
        if ( option == null )
            throw new IllegalArgumentException( className );
        option.setValue( newValue );
    }

    /**
     * Preserve this ImageExportOptions in XML.
     * @param node An XML context in which to save.
     */
    public void write( XmlNode node ) {
        node = node.addChild( ExportOptionsTag );
        node.setAttribute( TypeTag, m_imageType.getName() );
        save( node );
        if ( m_exportFile != null ) {
            node.setAttribute( FileTag, m_exportFile.getAbsolutePath() );
        }
    }

    /**
     * Instantiate a new ImageExportOptions from a preserved state.
     * @param node An XML context in which state was saved
     * @return The new ImageExportOptions
     * @throws XMLException If the given node has invalid content.
     */
    public static ImageExportOptions read( XmlNode node )
        throws XMLException
    {
        node = node.getChild( ExportOptionsTag );
        final String typeName = node.getAttribute( TypeTag );
        final ImageType type = ImageType.getImageTypeByName( typeName );
        if ( type == null ) {
            throw new XMLException( "Unrecognized image export type" );
        }
        final ImageExportOptions options = type.newExportOptions();
        options.restore( node );
        if ( node.hasAttribute( FileTag ) ) {
            options.m_exportFile = new File( node.getAttribute( FileTag ) );
        }
        return options;
    }

    /**
     * Read the state of this <code>ImageExportOption</code>.
     *
     * @param r The {@link ImageExportOptionReader} to read from.
     */
    public void readFrom( ImageExportOptionReader r ) throws IOException {
        originalWidth.readFrom( r );
        originalHeight.readFrom( r );
    }

    /**
     * Write the state of this <code>ImageExportOption</code>.
     *
     * @param w The {@link ImageExportOptionWriter} to read from.
     */
    public void writeTo( ImageExportOptionWriter w ) throws IOException {
        originalWidth.writeTo( w );
        originalHeight.writeTo( w );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct an <code>ImageExportOptions</code>.
     *
     * @param imageType The {@link ImageType} these options are for.
     */
    protected ImageExportOptions( ImageType imageType ) {
        originalWidth   = new OriginalWidthOption( 0, this );
        originalHeight  = new OriginalHeightOption( 0, this );

        m_imageType = imageType;
    }

    @Deprecated
    protected void save( XmlNode node ) {
        originalWidth.save( node );
        originalHeight.save( node );
    }

    @Deprecated
    protected void restore( XmlNode node ) throws XMLException {
        originalWidth.restore( node );
        originalHeight.restore( node );
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Add an {@link ImageExportOption} to this set of options.
     *
     * @param option The {@link ImageExportOption} to add.
     */
    final void addOption( ImageExportOption option ) {
        m_exportOptions.put( option.getName(), option );
    }

    /**
     * An XML tag constant used in save() and restore().
     */
    final static String ValueTag = "value";

    ////////// private ////////////////////////////////////////////////////////

    /**
     * "Fix" a class name by ensuring it's both fully-qualified and ends with
     * "Option" since this is how class names are stored in the
     * {@link #m_exportOptions} map.
     *
     * @param className The name of the class to fix.
     * @return Returns the fixed class name.
     */
    private static String fixClassName( String className ) {
        if ( !className.startsWith( "com." ) )
            className = ImageExportOptions.class.getPackage().getName() + '.'
                        + className;
        if ( !className.endsWith( "Option" ) )
            className += "Option";
        return className;
    }

    /**
     * Auxiliary data that is to be or was used to export an image file.
     */
    private byte[] m_auxData;

    /**
     * The {@link File} that is to be or was used to export an image file.
     */
    private File m_exportFile;

    /**
     * All the options comprising this <code>ImageExportOptions</code>.
     * Each key is the fully-qualified name of a class derived from
     * {@link ImageExportOption} and each value is an instance of
     * {@link ImageExportOption}.
     */
    private final Map<String,ImageExportOption> m_exportOptions =
        new HashMap<String,ImageExportOption>();

    /**
     * The {@link ImageType} this instance of <code>ImageExportOptions</code>
     * is for.
     */
    private final ImageType m_imageType;

    /**
     * An XML tag constant used in save() and restore().
     */
    private final static String ExportOptionsTag = "ExportOptions";

    /**
     * An XML tag constant used in save() and restore().
     */
    private final static String TypeTag = "type";

    /**
     * An XML tag constant used in save() and restore().
     */
    private final static String FileTag = "file";
}
/* vim:set et sw=4 ts=4: */
