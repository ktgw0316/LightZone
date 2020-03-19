/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/** An OperationType is a type for Operations.  It lets Engines tell users
 * about their capabilities, and it lets users tell Engines which Operations
 * to insert into pipelines.
 */

public interface OperationType {

    /** A user-presentable String that identifies a unique OperationType.
     * @return The name of this OperationType.
     */
    String getName();
}
