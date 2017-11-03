/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

/**
 * An {@code PentaxConstants} defines some constants for Pentax maker-note
 * metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public interface PentaxConstants {

    //
    // Pentax flash values are not decomposable into bits, e.g.., there is no
    // red-eye bit.
    //
    int PENTAX_FLASH_AUTO_FIRED                 = 256;
    int PENTAX_FLASH_AUTO_FIRED_RED_EYE         = 259;
    int PENTAX_FLASH_AUTO_NO_FIRED              = 0;
    int PENTAX_FLASH_OFF_NO_FIRED               = 1;
    int PENTAX_FLASH_ON_FIRED                   = 258;
    int PENTAX_FLASH_ON_NO_FIRE                 = 2;
    int PENTAX_FLASH_ON_RED_EYE                 = 260;
    int PENTAX_FLASH_ON_SLOW_SYNC               = 265;
    int PENTAX_FLASH_ON_SLOW_SYNC_RED_EYE       = 266;
    int PENTAX_FLASH_ON_SOFT                    = 264;
    int PENTAX_FLASH_ON_TRAILING_CURTAIN_SYNC   = 267;
    int PENTAX_FLASH_ON_WIRELESS_CONTROL        = 262;
    int PENTAX_FLASH_ON_WIRELESS_MASTER         = 261;

}
/* vim:set et sw=4 ts=4: */
