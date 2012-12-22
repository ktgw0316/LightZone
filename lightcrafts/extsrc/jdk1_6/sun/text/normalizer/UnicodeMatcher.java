/*
 * @(#)UnicodeMatcher.java	1.3 05/11/17
 *
 * Portions Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 *******************************************************************************
 * (C) Copyright IBM Corp. 1996-2005 - All Rights Reserved                     *
 *                                                                             *
 * The original version of this source code and documentation is copyrighted   *
 * and owned by IBM, These materials are provided under terms of a License     *
 * Agreement between IBM and Sun. This technology is protected by multiple     *
 * US and International patents. This notice and attribution to IBM may not    *
 * to removed.                                                                 *
 *******************************************************************************
 */

package jdk1_6.sun.text.normalizer;

/**
 * <code>UnicodeMatcher</code> defines a protocol for objects that can
 * match a range of characters in a Replaceable string.
 * @stable ICU 2.0
 */
public interface UnicodeMatcher {

    /**
     * The character at index i, where i < contextStart || i >= contextLimit,
     * is ETHER.  This allows explicit matching by rules and UnicodeSets
     * of text outside the context.  In traditional terms, this allows anchoring
     * at the start and/or end.
     * @stable ICU 2.0
     */
    static final char ETHER = '\uFFFF';

}

//eof
