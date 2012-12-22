/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.thread;

/**
 * TaskRunnable is a wrapper class that treats Runnables in a Collection as
 * if they were representing their wrapped objects rather than their own
 * instance.  Good for replacing keyed tasks on the fly.
 */
public abstract class TaskRunnable implements Runnable {

    public boolean equals( Object obj ) {
        if ( getClass() == obj.getClass() ) {
            // The TaskRunnable class must match since multiple tasks
            // on the same key object may occur
            final TaskRunnable task = (TaskRunnable)obj;
            return m_taskKey.equals( task.m_taskKey );
        }
        return false;
    }

    public int hashCode() {
        return m_taskKey.hashCode();
    }

    public String toString() {
        return m_taskKey.toString();
    }

    ////////// protected //////////////////////////////////////////////////////

    protected TaskRunnable( Object taskKey ) {
        m_taskKey = taskKey;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final Object m_taskKey;
}
/* vim:set et sw=4 ts=4: */
