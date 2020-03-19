/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

/**
 * A <code>NikonConstants</code> defines some constants for Nikon maker-note
 * metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface NikonConstants {

    /**
     * The size of the Nikon maker-note header (in bytes).
     * The bytes are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td>0-4&nbsp;</td><td><code>Nikon</code></td>
     *      </tr>
     *      <tr>
     *        <td>5-6</td><td>Version number</td>
     *      </tr>
     *      <tr>
     *        <td>7-8</td><td>Unknown</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int NIKON_MAKER_NOTES_HEADER_SIZE = 10;

}
/* vim:set et sw=4 ts=4: */
