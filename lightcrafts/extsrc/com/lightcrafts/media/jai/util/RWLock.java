/*
 * $RCSfile: RWLock.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:01 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A class that provides a classic reader/writer lock functionality.
 * This implementation is based on the JavaWorld article by Tark Modi
 * titled "Lock on to an alternate synchronized mechanism" with some
 * minor modifications.
 *
 * <p>This lock provides the following functionality : <ul>
 *
 * <li> Allows multiple threads read access while only a single
 *	thread can have a write access.</li>
 *
 * <li> Allows a thread owning a read lock to "upgrade" to a write
 *	lock.</li>
 *
 * <li> Allows a thread owning a write lock to "downgrade" to a read
 *	lock.</li>
 *
 * <li> Allows the option to either block or specify a waitTime on
 *	certain requests.
 *
 * </ul>
 */
public final class RWLock {

    // Should a read lock be allowed to upgrade to a write lock if the  
    // thread requesting the write lock already owns the read lock.     
    private boolean allowUpgrades;

    // Lock Types
    private static int READ  = 1;
    private static int WRITE = 2;

    private static int NOT_FOUND = -1;

    // A list of threads that are waiting to acquire a lock, waiting to
    // upgrade their lock or have been granted locks.
    private WaitingList waitingList = new WaitingList();

    private class WaitingList extends LinkedList {

	// Return index of the first writer in the list.
	int indexOfFirstWriter() {

	    ListIterator iter = listIterator(0);
	    int index = 0;

	    while (iter.hasNext()) {
		if (((ReaderWriter)iter.next()).lockType == WRITE)
		    return index;
		index++;
	    }                                         

	    return NOT_FOUND;
	}

	// Return the index of the last thread granted a lock
	int indexOfLastGranted() {

	    ListIterator iter = listIterator(size());
	    int index = size()-1;

	    while (iter.hasPrevious()) {
		if (((ReaderWriter)iter.previous()).granted == true)
		    return index;
		index--;
	    }

	    return NOT_FOUND;
	}

	// Return index of the element for this thread
	int findMe() {
	    return indexOf(new ReaderWriter());
	}
    }

    // Element stored in the waiting list.
    private class ReaderWriter {

	Thread  key;		// set equal to the Thread ID
	int     lockType;	// READ or WRITE	
	int     lockCount;	// # of times lock was taken
	boolean granted;	// Has this thread been granted the lock

	ReaderWriter() {
	    this(0);
	}

	ReaderWriter(int type) {

	    key	      = Thread.currentThread();
	    lockType  = type;
	    lockCount =  0;       
	    granted   = false;
	}

        /**
         * Two elements are equal if their keys are equal. Note that
         * this does not make any instanceof checks since it assumes
         * that this implementation uses the this list a certain way.
         */
	public boolean equals(Object o) {
	    return key == ((ReaderWriter)o).key;
	}
    }

    /**
     * Constructor.
     *
     * @param should upgrades of read locks to write locks be
     *		     allowed ?
     */
    public RWLock(boolean allowUpgrades) {

	this.allowUpgrades = allowUpgrades;
    }

    /**
     * Constructor. Equivalent to <code>RWLock(true)</code>
     * which creates an upgradable reader-writer lock.
     */
    public RWLock() {
	this(true);
    }

    /**
     * Tries to obtain a read lock within the specified time.
     *
     * @param waitTime the time to wait in milliseconds while trying
     *	    to obtain the read lock. A negative value indicates a
     *	    blocking wait.
     *
     * @return true if the read lock was obtained without timing out.
     */
    public synchronized boolean forReading(int waitTime) {

	ReaderWriter element = null;

	// Is there a node for this thread already in the list?
	// If not, create a new one.
	int index = waitingList.findMe();

	if (index != NOT_FOUND) {
	    element = (ReaderWriter)waitingList.get(index);

	} else {
	    element = new ReaderWriter(READ);
	    waitingList.add(element);
	}

	// If a lock has already been granted once just increment the
	// count and return. It does not matter whether the lock granted
	// initially was a READ or WRITE lock.
	if (element.lockCount > 0) {
	    element.lockCount++;       
	    return true;
	}

	long startTime = System.currentTimeMillis();
	long   endTime = waitTime + startTime;

	do {
	    int nextWriter = waitingList.indexOfFirstWriter();       

	    index = waitingList.findMe();

	    // If there is no writer in front of me I get a
	    // read lock. Otherwise, I wait...
	    if ((nextWriter == NOT_FOUND) || (nextWriter > index)) {
		element.lockCount++;
		element.granted = true;
		return true;
	    }

	    // Non-blocking version, just return.  Do not need notifyAll
	    // here since we added and removed the new element within the
	    // same synchronized block and so no other thread ever saw
	    // it.
	    if (waitTime == 0) {
		waitingList.remove(element);                               
		return false;
	    }

	    // Now wait for the lock.
	    try {
		// Negative wait time indicates a blocking wait.
		if (waitTime < 0) {
		    wait();

		} else {
		    long delta = endTime - System.currentTimeMillis();

		    if (delta > 0) wait(delta);
		}
	    } catch (InterruptedException e) {
		// Should never be here.
		// These messages are for debugging purposes only
		System.err.println(element.key.getName() +
			" : interrupted while waiting for a READ lock!");
	    }

	} while ((waitTime < 0) || (endTime > System.currentTimeMillis()));

	// Could not get the lock and timed out.
	waitingList.remove(element);             

	// Important to notify all threads of our removal.
	notifyAll();

	// Failed to get lock. 
	return false;       
    }

    /**
     * Tries to obtain a read lock. Equivalent to <code>forReading(-1) 
     * </code> which will go into a blocking wait attempting to get a   
     * read lock.
     *
     * @return true, always.
     */
    public synchronized boolean forReading() {
	return forReading(-1);
    }

    /**
     * Tries to obtain a write lock withing the specified time. If the  
     * current thread owns a read lock, then it is upgraded to a write  
     * lock if upgrades are allowed, else, throws an UpgradeNotAllowed  
     * exception. If the lock is not owned by the current thread then   
     * it waits for a write lock for the specified time.                
     *
     * @param waitTime the time to wait in milliseconds while trying
     *	    to obtain the write lock. A negative value indicates a
     *	    blocking wait.
     *
     * @return true if the write lock was obtained without timing out.
     *
     * @throws UpgradeNotAllowed if current thread owns a read lock
     *	    and upgrades are not allowed.
     */
    public synchronized boolean forWriting(int waitTime)
			throws UpgradeNotAllowed {

	ReaderWriter element = null;

	// Is there a node for this thread already in the list?
	// If not, create a new one.
	int index = waitingList.findMe();

	if (index != NOT_FOUND) {
	    element = (ReaderWriter)waitingList.get(index);

	} else {
	    element = new ReaderWriter(WRITE);
	    waitingList.add(element);
	}

	// If the thread has a READ lock, we need to upgrade
	if ((element.granted == true) && (element.lockType == READ)) {

	    try {
		if (!upgrade(waitTime))
		    return false;
	    }
	    catch (LockNotHeld e) {
		    return false;
	    }
	}

	// If a lock has already been granted once just increment the
	// count and return.  At this point the thread either had a WRITE
	// lock or was upgraded to have one.
	if (element.lockCount > 0) {
	    element.lockCount++;       
	    return true;
	}

	long startTime = System.currentTimeMillis();
	long   endTime = waitTime + startTime;

	do {
	    // If there are any readers in front of me
	    // I have to wait...
	    index = waitingList.findMe();

	    // If I am the first one in the list I get the lock.
	    if (index == 0) {
		element.lockCount++;
		element.granted = true;
		return true;
	    }

	    // Non-blocking version, just return.  Do not need notifyAll
	    // here since we added and removed the new element within the
	    // same synchronized block and so no other thread ever saw
	    // it.
	    if (waitTime == 0) {
		waitingList.remove(element);                               
		return false;
	    }

	    // Now wait for the lock.
	    try {
		// Negative wait time indicates a blocking wait.
		if (waitTime < 0) {
		    wait();

		} else {
		    long delta = endTime - System.currentTimeMillis();

		    if (delta > 0) wait(delta);
		}
	    } catch (InterruptedException e) {
		// Should never be here.
		// These messages are for debugging purposes only
		System.err.println(element.key.getName() +
			" : interrupted while waiting for a WRITE lock!");
	    }

	} while ((waitTime < 0) || (endTime > System.currentTimeMillis()));

	// Could not get the lock and timed out.
	waitingList.remove(element);                   

	// Notify all threads of our removal.
	notifyAll();

	// Failed to get the lock.
	return false;
    }

    /**
     * Tries to obtain a write lock. Equivalent to <code>forWriting(-1) 
     * </code> which will go into a blocking wait attempting to get a   
     * write lock.
     *
     * @return true, always.
     *
     * @throws UpgradeNotAllowed if current thread owns a read lock
     *	    and upgrades are not allowed.
     */
    public synchronized boolean forWriting()
			throws UpgradeNotAllowed {
	return forWriting(-1);
    }

    /**
     * Try to upgrade a write lock within the specified amount of time. 
     * If the current thread does not own the lock or if upgrades       
     * are not allowed an exception is thrown. If the current thread    
     * already owns a write lock, nothing happens. Otherwise, threads   
     * already owning a read lock are given a higher priority in        
     * receiving the write lock than those that own no lock.            
     *
     * @param waitTime the time to wait in milliseconds while trying
     *	    to upgrade to a write lock. A negative value indicates a
     *	    blocking wait.
     *
     * @return true if the upgrade was performed without timing out.
     *
     * @throws LockNotHeld if the current thread does not own the lock
     * @throws UpgradeNotAllowed if the lock is not currently
     *	    held by the owner.
     */
    public synchronized boolean upgrade(int waitTime)
			throws UpgradeNotAllowed, LockNotHeld {

	if (!allowUpgrades)
	    throw new UpgradeNotAllowed();

	// We should already be in the list. If not, it is an error.
	int index = waitingList.findMe();

	if (index == NOT_FOUND)
	    throw new LockNotHeld();

	// Get the actual element. If the lock type is already WRITE,
	// just return.
	ReaderWriter element = (ReaderWriter)waitingList.get(index);

	if (element.lockType == WRITE)
	    return true;

	// What is the index of the last granted lock?       
	int lastGranted = waitingList.indexOfLastGranted();

	// lastGranted can not be NOT_FOUND, after all we are
	// granted a READ lock!
	if (lastGranted == NOT_FOUND)
	    throw new LockNotHeld();

        // If we are not the last granted lock, then we will position
        // ourselves as such.
	if (index != lastGranted) {
	    waitingList.remove(index);
	    ListIterator iter = waitingList.listIterator(lastGranted);
	    iter.add(element);
	}

	// We want new readers to think this is a write lock      
	// This is important so that they block i.e. do not get granted.
	// Since we are now waiting for a write lock it is
	// important that we were after all granted read locks.
	element.lockType = WRITE;

	long startTime = System.currentTimeMillis();
	long   endTime = waitTime + startTime;

	do {
	    index = waitingList.findMe();

	    if (index == 0) {
		return true;
	    }

	    // Non-blocking version. Do not need notifyAll here since
	    // we changed the lock type back and forth within the same
	    // synchronized block and so no other thread ever saw it.
	    if (waitTime == 0) {

		// Back to READ type            
		element.lockType = READ;            

		// No need to readjust position since it does not matter
		// for already granted locks.
		return false;
	    }

	    // Now wait for the lock.
	    try {
		// Negative wait time indicates a blocking wait.
		if (waitTime < 0) {
		    wait();
		} else {
		    long delta = endTime - System.currentTimeMillis();

		    if (delta > 0) wait(delta);
		}
	    } catch (InterruptedException e) {
		// Should never be here.
		// These messages are for debugging purposes only
		System.err.println(element.key.getName() +
			" : interrupted while waiting to UPGRADE lock!");
	    }

	} while ((waitTime < 0) || (endTime > System.currentTimeMillis()));

	// We failed to upgrade. Go back to original lock type
	element.lockType = READ;

	// Important to notify all threads that we are back 
	// to being a READ lock.
	notifyAll();       

	// Failed to upgrade.
	return false;       
    }

    /**
     * Tries to upgrade to a write lock. Equivalent to                  
     * <code>upgrade(-1)</code> which will go into a blocking wait     
     * attempting to get a upgrade to a write lock.
     *
     * @return true, always.
     *
     * @throws LockNotHeld if some other thread owns the lock
     * @throws UpgradeNotAllowed if the lock is not currently
     *	    held by the owner.
     */
    public synchronized boolean upgrade()
			throws UpgradeNotAllowed, LockNotHeld {
	return upgrade(-1);
    }

    /**
     * Tries to downgrade a write lock to a read lock. If the current   
     * thread does not hold the lock an exception is thrown. If the     
     * current thread already owns a read lock, nothing happens. If it  
     * owns a write lock, then it is downgraded to a read lock. All     
     * threads waiting for a read lock can now get it only if there are 
     * no other threads waiting for a write lock ahead of them.
     *
     * @return true if the downgrade was performed successfully.
     *
     * @throws LockNotHeld if the current thread does not own the lock.
     */
    public synchronized boolean downgrade() throws LockNotHeld {

	// We should already be in the list. If not, it is an error.       
	int index = waitingList.findMe();

	if (index == NOT_FOUND)
	    throw new LockNotHeld();

	// Get the element for this thread
	ReaderWriter e = (ReaderWriter)waitingList.get(index);

	// Downgrade the WRITE lock and notify all threads of the change.
	if (e.lockType == WRITE) {
	    e.lockType = READ;
	    notifyAll();       
	}

	return true;
    }

    /**
     * Tries to relinquish the ownership of a lock. If the              
     * current thread does not hold the lock an exception is            
     * thrown. Note that every call to <code>forReading</code>          
     * and <code>forWriting</code> must have a corresponding            
     * <code>release()</code> call. However, <code>upgrade()</code>     
     * and <code>downgrade()</code> do not have corresponding           
     * <code>release()</code> calls.                                    
     *
     * @throws LockNotHeld if the current thread does not own the lock.
     */
    public synchronized void release() throws LockNotHeld {

	// We should already be in the list. If not, it is an error.       
	int index = waitingList.findMe();

	if (index == NOT_FOUND)
	    throw new LockNotHeld();

	// Get the element for this thread
	ReaderWriter e = (ReaderWriter)waitingList.get(index);

	// If the lock count goes down to zero, 
	// remove the lock and notify all threads of the change.
	if ((--e.lockCount) == 0) {
	    waitingList.remove(index);            
	    notifyAll();      
	}
    }

    /**
     * The exception thrown when trying to upgrade a lock when
     * the lock does not allow upgrades.
     */
    public class UpgradeNotAllowed extends RuntimeException {
    }

    /**
     * The exception thrown when trying to upgrade a lock when
     * the lock is not held by the current thread.
     */
    public class LockNotHeld extends RuntimeException {
    }
}
