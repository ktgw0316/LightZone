/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016-     Masahiro Kitagawa */

package com.lightcrafts.platform;

import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.directory.DirectoryMonitor;
import com.lightcrafts.utils.directory.UnixDirectoryMonitor;
import com.lightcrafts.utils.file.ICC_ProfileFileFilter;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.color.ICC_Profile;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * This interface defines all platform-specific or platform-customized
 * features.
 * <p>
 * Instances are only available through the static initializer that picks an
 * instance based on the operating system.
 */
@SuppressWarnings({"InnerClassFieldHidesOuterClassField"})
public class Platform {

    /**
     * A <code>Type</code> encodes the platform type.
     */
    public enum Type {
        Linux  ( "com.lightcrafts.platform.linux.LinuxPlatform" ),
        MacOSX ( "com.lightcrafts.platform.macosx.MacOSXPlatform" ),
        Windows( "com.lightcrafts.platform.windows.WindowsPlatform" ),
        Other  ( "com.lightcrafts.platform.Platform" );

        /**
         * Gets the fully qualified name of the class that extends
         * {@link Platform} for the platform for this type.
         *
         * @return Returns said class name.
         */
        String getPlatformImplementationClassName() {
            return m_implementationClassName;
        }

        /**
         * Gets the <code>Type</code> for the currently running operating
         * system.
         *
         * @return Returns said <code>Type</code>.
         */
        static Type getTypeForOS() {
            final String osName = System.getProperty( "os.name" ).toLowerCase();
            if ( osName.startsWith( "linux" )
                    || osName.startsWith( "freebsd" )
                    || osName.startsWith( "openbsd" )
                    || osName.startsWith( "sunos" ) )
                return Linux;
            if ( osName.equals( "mac os x" ) )
                return MacOSX;
            if ( osName.startsWith( "windows" ) )
                return Windows;
            return Other;
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * Constructs a <code>Type</code>.
         *
         * @param implementationClassName The fully qualified name of the class
         * that extends {@link Platform} for the new type.
         */
        Type( String implementationClassName ) {
            m_implementationClassName = implementationClassName;
        }

        private final String m_implementationClassName;
    }

    /**
     * Bring the given application to the front.
     *
     * @param appName The name of the application.
     */
    public void bringAppToFront( String appName ) {
        // do nothing by default
    }

    /**
     * Creates a new {@link AlertDialog}.
     *
     * @return Returns a new {@link AlertDialog}.
     * @noinspection MethodMayBeStatic
     */
    public AlertDialog getAlertDialog() {
        return DefaultAlertDialog.INSTANCE;
    }

    /**
     * Gets a directory where images are likely to be found.
     *
     * @return Returns said directory.
     */
    public File getDefaultImageDirectory() {
        final String home = System.getProperty( "user.home" );
        return new File( home, Version.getApplicationName() );
    }

    /**
     * Gets the display name for the given {@link File}.
     *
     * @param file The {@link File} to get the display name for.
     * @return Returns said name.
     */
    public String getDisplayNameOf( File file ) {
        return getFileSystemView().getSystemDisplayName( file );
    }

    /**
     * Gets the {@link FileChooser} for the current <code>Platform</code>.
     *
     * @return Returns said {@link FileChooser}.
     */
    public FileChooser getFileChooser() {
        return new DefaultFileChooser();
    }

    /**
     * Gets the directory where easily-accessible documents go.
     *
     * @return Returns said directory.
     */
    public File getLightZoneDocumentsDirectory() {
        final String home = System.getProperty( "user.home" );
        return new File( home, Version.getApplicationName() );
    }

    /**
     * Returns the Swing pluggable look-and-feel class name suitable for
     * setting on the UIManager.
     */
    public LookAndFeel getLookAndFeel() {
        return UIManager.getLookAndFeel();
    }

    /**
     * Gets a new {@link DirectoryMonitor}.
     *
     * @return Returns said {@link DirectoryMonitor}.
     */
    @SuppressWarnings({"MethodMayBeStatic"})
    public DirectoryMonitor getDirectoryMonitor() {
        return new UnixDirectoryMonitor();
    }

    /**
     * Get the ICC_Profile to use for rendering to the screen on this platform.
     */
    public ICC_Profile getDisplayProfile() {
        return null;
    }

    /**
     * Gets a Collection&gt;ColorProfileInfo&lt; objects that are suitable for
     * exporting images to files.
     */
    public Collection<ColorProfileInfo> getExportProfiles() {
        return Collections.emptySet();
    }

    /**
     * Get the {@link FileSystemView} for this platform.
     *
     * @return Returns said {@link FileSystemView}.
     * @noinspection MethodMayBeStatic
     */
    public FileSystemView getFileSystemView() {
        return FileSystemView.getFileSystemView();
    }

    /**
     * Gets the amount of physical memory installed in the computer.
     *
     * @return Returns the amount of memory in megabytes.
     */
    public int getPhysicalMemoryInMB() {
        long totalPhysicalMemory = 0;
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            final Object attribute = mBeanServer.getAttribute(
                    new ObjectName("java.lang", "type", "OperatingSystem"),
                    "TotalPhysicalMemorySize");
            totalPhysicalMemory = Long.parseLong(attribute.toString());
        } catch (AttributeNotFoundException | MBeanException | InstanceNotFoundException
                | ReflectionException | MalformedObjectNameException e) {
            e.printStackTrace();
        }
        return (int) (totalPhysicalMemory / 1048576);
    }

    /**
     * Gets the path components to the platform's &quot;Pictures&quot; folder.
     *
     * @return Returns an array of the names of the path components starting
     * from the root folder shown in the folder tree (not the root of the
     * filesystem).
     */
    @Deprecated
    public String[] getPathComponentsToPicturesFolder() {
        return getPathComponentsTo(getDefaultImageDirectory());
    }

    public String[] getPathComponentsTo(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        final String sep = Pattern.quote(File.separator);
        return file.getAbsolutePath().split(sep);
    }

    /**
     * Gets the current <code>Platform</code>.
     *
     * @return Returns said <code>Platform</code>.
     */
    public static Platform getPlatform() {
        return m_platform;
    }

    /**
     * Get a Collection of ColorProfileInfo objects that are suitable for
     * sending images to printers.
     */
    public Collection<ColorProfileInfo> getPrinterProfiles() {
        return Collections.emptySet();
    }

    /**
     * Creates a new {@link ProgressDialog}.
     *
     * @return Returns a new {@link ProgressDialog}.
     * @noinspection MethodMayBeStatic
     */
    public ProgressDialog getProgressDialog() {
        return new DefaultProgressDialog();
    }

    /**
     * Check if the current platform is Linux.
     *
     * @return Returns <code>true</code> only if the current platform is Linux.
     */
    public static boolean isLinux() {
        return m_type == Type.Linux;
    }

    /**
     * Check if the current platform is macOS.
     *
     * @return Returns <code>true</code> only if the current platform is macOS.
     */
    public static boolean isMac() {
        return m_type == Type.MacOSX;
    }

    /**
     * Check if the current platform is Windows.
     *
     * @return Returns <code>true</code> only if the current platform is Windows.
     */
    public static boolean isWindows() {
        return m_type == Type.Windows;
    }

    /**
     * Gets the type of the current platform.
     *
     * @return Returns said type.
     */
    public static Type getType() {
        return m_type;
    }

    /**
     * Checks whether this computer has an active Internet connection.
     *
     * @param hostName The fully qualified name of the desired host to connect
     * to.
     * @return Returns <code>true</code> only if this computer currently has
     * an active internet connection and can reach the specified host.
     */
    public boolean hasInternetConnectionTo( String hostName ) {
        try {
            final InetAddress address = InetAddress.getByName(hostName);
            return address.isReachable(2000);
        }
        catch (Throwable t) {
            return false;
        }
    }

    /**
     * Make a given file hidden to the user in the OS's file browser.
     *
     * @param file The {@link File} to hide.
     */
    @SuppressWarnings( { "UnusedDeclaration" } )
    public void hideFile( File file ) throws IOException {
        // do nothing by default
    }

    /**
     * Detect whether the specified key is currently pressed.  The purpose of
     * a platform-specific implementation of this method is to distinguish
     * the synthetic key events generated by auto-repeat.  This default
     * implementation just throws UnsupportedOperationException.
     */
    @SuppressWarnings( { "UnusedDeclaration", "MethodMayBeStatic" } )
    public boolean isKeyPressed( int keyCode )
        throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException(
            "The current Platform does not implement isKeyPressed()"
        );
    }

    /**
     * Checks whether the given {@link File} is special in some way on the
     * platform.
     *
     * @param file The {@link File} to check.
     * @return If the file is special, returns a platform-specific instance of
     * some class derived from {@link File} that implements some special
     * behavior; otherwise returns the passed-in file.
     */
    public File isSpecialFile( File file ) {
        return file;
    }

    /**
     * Loads native libraries for the current <code>Platform</code>.
     */
    public void loadLibraries() throws UnsatisfiedLinkError {
        // do nothing by default
    }

    /**
     * If this Platform supports Java Dialog ModalityTypes, then set the
     * given dialog's ModalityType to DOCUMENT_MODAL.  Otherwise just call
     * Dialog.setModal(true).
     * <p>
     * The "document" modality for dialogs us useful because it allows help
     * windows to be used alongside the modal dialogs they describe.
     */
    @SuppressWarnings( { "MethodMayBeStatic" } )
    public void makeModal(Dialog dialog) {
        dialog.setModal(true);
    }

    /**
     * Move a set of files to the Trash.
     *
     * @param pathNames An array of full paths of files to be moved to the
     * Trash.
     * @return Returns <code>true</code> only if all the file were moved.
     */
    public boolean moveFilesToTrash( String[] pathNames ) {
        for ( String pathName : pathNames ) {
            final File file = new File( pathName );
            if ( !file.delete() )
                return false;
        }
        return true;
    }

    /**
     * Assert that we are now ready to handle opening image files.
     */
    public void readyToOpenFiles() {
        // do nothing
    }

    /**
     * Resolve an alias file.
     *
     * @param file The {@link File} to resolve.
     * @return Returns the resolved path (or the original path if it didn't
     * refer to an alias) or <code>null</code> if there was an error.
     */
    public String resolveAliasFile( File file ) {
        return file.getAbsolutePath();
    }

    /**
     * Tells the OS's file browser to show the folder the given file is in.
     *
     * @param path The full path of the file to show.
     * @return Returns <code>true</code> only if the file was shown
     * successfully.
     */
    public boolean showFileInFolder( String path ) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        final Desktop desktop = Desktop.getDesktop();
        if(!desktop.isSupported(Desktop.Action.OPEN)) {
            return false;
        }

        try {
            Path p = Paths.get(path).toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!Files.isDirectory(p)) {
                p = p.getParent();
            }
            desktop.open(p.toFile());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Show the application help for a specific topic.
     *
     * @param topic The topic to show or <code>null</code> to show the cover
     * page.
     */
    public void showHelpTopic( String topic ) {
        // do nothing
    }

    private PrinterLayer printerLayer = new DefaultPrinterLayer();

    public PrinterLayer getPrinterLayer() {
        return printerLayer;
    }

    ////////// protected ////////////////////////////////////////////////////////

    protected static Collection<ColorProfileInfo> getColorProfiles(
            File profileDir
    ) {
        HashSet<ColorProfileInfo> profiles = new HashSet<>();

        if (! profileDir.isDirectory()) {
            return profiles;
        }

        File[] files = profileDir.listFiles(ICC_ProfileFileFilter.INSTANCE);
        if (files == null) {
            return Collections.emptyList(); // Just in case of I/O error
        }

        for (File file : files) {
            if (file.isDirectory()) {
                profiles.addAll(getColorProfiles(file));
            }
            else if (file.isFile()) {
                String path = file.getAbsolutePath();
                try {
                    final ICC_Profile profile = ICC_Profile.getInstance(path);
                    final String name = ColorProfileInfo.getNameOf(profile);
                    final ColorProfileInfo info = new ColorProfileInfo(name, path);
                    profiles.add(info);
                }
                catch (IOException e) {
                    // Trouble reading the file
                    System.err.println(
                            "Can't read a color profile from " + path + ": "
                                    + e.getMessage()
                    );
                }
                catch (Throwable e) {
                    // Invalid color profile data
                    System.err.println(
                            "Not a valid color profile at " + path + ": "
                                    + e.getMessage()
                    );
                }
            }
        }
        return profiles;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The current <code>Platform</code>.
     */
    private static Platform m_platform;

    /**
     * The type of the current platform.
     */
    private static final Type m_type;

    static {
        m_type = Type.getTypeForOS();
        final String className = m_type.getPlatformImplementationClassName();
        try {
            m_platform = (Platform)Class.forName( className ).newInstance();
        }
        catch ( Exception e ) {
            m_platform = new Platform();
            System.err.println(
                e.getClass().getName() + ": " + e.getMessage()
            );
        }
    }
}
/* vim:set et sw=4 ts=4: */
