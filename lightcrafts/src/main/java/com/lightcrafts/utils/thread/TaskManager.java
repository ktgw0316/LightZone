/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.thread;

import java.util.LinkedList;

/**
 * TaskManager maintains a prioritized list of Runnables for asynchronous
 * execution.
 */
public final class TaskManager implements Runnable  {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>TaskManager</code> having the default number of
     * priority queues.
     */
    public TaskManager() {
        this( DEFAULT_PRIORITY_COUNT );
    }

    /**
     * Construct a <code>TaskManager</code>.
     *
     * @param priorityCount The number of priority queues to have.
     */
    public TaskManager( int priorityCount ) {
        m_priorityCount = priorityCount;
        m_priorityQueues = new LinkedList[ priorityCount ];
        for ( int i = 0; i < m_priorityQueues.length; i++ )
            m_priorityQueues[i] = new LinkedList();

        // Limit the amount of data in flight
        final int numThreads = 1; // Runtime.getRuntime().availableProcessors();
        final Thread[] workerThreads = new Thread[ numThreads ];
        for ( int i = 0; i < numThreads; ++i ) {
            workerThreads[i] = new Thread( this, "Task Manager Worker " + i );
            workerThreads[i].setPriority( Thread.MIN_PRIORITY );
            workerThreads[i].setDaemon( true );
            workerThreads[i].start();
        }
    }

    public void appendTask( Runnable runnable ) {
        appendTask( m_priorityCount / 2, runnable );
    }

    public void appendTask( int priority, Runnable runnable ) {
        addTask( runnable, priority, true );
    }

    public void dispose() {
        synchronized ( m_priorityQueues ) {
            m_stop = true;
            m_priorityQueues.notifyAll();
        }
    }

    public int getMaxPriority() {
        return m_priorityCount - 1;
    }

    public int getMinPriority() {
        return 0;
    }

    public int getNormPriority() {
        return m_priorityCount / 2;
    }

    public void insertTask( Runnable runnable ) {
        insertTask( m_priorityCount / 2, runnable );
    }

    public void insertTask( int priority, Runnable runnable ) {
        addTask( runnable, priority, false );
    }

    public boolean removeTask( Runnable runnable ) {
        for ( int i = 0; i < m_priorityCount; ++i )
            if ( removeTask( i, runnable ) )
                return true;
        return false;
    }

    public boolean removeTask( int priority, Runnable runnable ) {
        final LinkedList queue = m_priorityQueues[ priority ];
        final boolean removed;
        synchronized ( queue ) {
            removed = queue.remove( runnable );
        }
        if ( removed )
            synchronized ( m_priorityQueues ) {
                m_priorityQueues.notifyAll();
            }
        return removed;
    }

    public void resumeTasks() {
        synchronized ( m_priorityQueues ) {
            m_suspended = false;
            m_priorityQueues.notifyAll();
        }
    }

    public void run() {
        while ( !m_stop ) {
            Runnable runnable = null;

            //
            // Go through all the priority queues looking for a runnable to
            // run.
            //
            for ( int i = m_priorityQueues.length - 1;
                  !m_suspended && i >= 0; --i )
            {
                final LinkedList queue = m_priorityQueues[i];
                synchronized ( queue ) {
                    if ( !queue.isEmpty() ) {
                        runnable = (Runnable)queue.removeFirst();
                        break;
                    }
                }
            }

            if ( runnable != null ) {
                //
                // We found a runnable to run: run it!
                //
                try {
                    runnable.run();
                }
                catch ( Throwable t ) {
                    t.printStackTrace();
                }
            } else {
                //
                // There are no runnables to run: wait until something happens.
                //
                synchronized ( m_priorityQueues ) {
                    try {
                        m_priorityQueues.wait();
                    }
                    catch ( InterruptedException e ) {
                        // do nothing
                    }
                }
            }
        }
    }

    public void suspendTasks() {
        synchronized ( m_priorityQueues ) {
            m_suspended = true;
            m_priorityQueues.notifyAll();
        }
    }

    ////////// protected //////////////////////////////////////////////////////

    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    private static final int DEFAULT_PRIORITY_COUNT = 4;

    /**
     * The number of priority queues.
     */
    private final int m_priorityCount;

    /**
     * The priority queues.
     */
    private final LinkedList[] m_priorityQueues;

    /**
     * A flag set by {@link #dispose()} to know when all the worker threads
     * should die.
     */
    private boolean m_stop;

    /**
     * If <code>true</code>, do not run process the prioroty queues.
     */
    private boolean m_suspended;

    private void addTask( Runnable runnable, int priority, boolean append ) {
        final LinkedList queue = m_priorityQueues[ priority ];
        synchronized ( queue ) {
            queue.remove( runnable );
            if ( append )
                queue.addLast( runnable );
            else
                queue.addFirst( runnable );
        }
        synchronized ( m_priorityQueues ) {
            m_priorityQueues.notifyAll();
        }
    }

}
/* vim:set et sw=4 ts=4: */
