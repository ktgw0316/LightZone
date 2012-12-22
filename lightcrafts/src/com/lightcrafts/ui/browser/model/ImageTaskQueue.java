/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import java.util.LinkedList;
import java.awt.*;

public class ImageTaskQueue implements Runnable {

    private LinkedList<ImageTask> queue;

    private Thread thread;
    private boolean pause;
    private boolean stop;

    private LinkedList<ImageTaskQueueListener> listeners;

    public ImageTaskQueue() {
        thread = new Thread(this, "Image Task Queue");
        thread.setPriority(Thread.MIN_PRIORITY);
        queue = new LinkedList<ImageTask>();
        listeners = new LinkedList<ImageTaskQueueListener>();
    }

    void addTask(ImageTask task) {
        synchronized(queue) {
            queue.add(task);
            queue.notifyAll();
        }
    }

    void removeTask(ImageTask task) {
        synchronized(queue) {
            queue.remove(task);
        }
    }

    void removeAllTasks() {
        synchronized(queue) {
            queue.clear();
        }
    }

    public void start() {
        if (! thread.isAlive()) {
            pause = false;
            stop = false;
            thread.start();
        }
    }

    public void stop() {
        pause = false;
        stop = true;
        synchronized(queue) {
            queue.notifyAll();
        }
        synchronized(thread) {
            thread.notifyAll();
        }
    }

    public void pause() {
        synchronized(thread) {
            pause = true;
        }
    }

    public void resume() {
        synchronized(thread) {
            if (pause) {
                pause = false;
                thread.notifyAll();
            }
        }
    }

    public void raiseTask(ImageTask task) {
        synchronized(queue) {
            if (queue.contains(task)) {
                queue.remove(task);
                queue.addFirst(task);
            }
        }
    }

    public void lowerTask(ImageTask task) {
        synchronized(queue) {
            if (queue.contains(task)) {
                queue.remove(task);
                queue.addLast(task);
            }
        }
    }

    public void run() {
        while (! stop) {
            Runnable task;
            synchronized(queue) {
                while (queue.isEmpty() && (! stop)) {
                    waitForNotify(queue);
                    if (stop) {
                        return;
                    }
                }
                task = queue.removeFirst();
                logQueueSize();
            }
            try {
                task.run();
            }
            catch (Throwable t) {
                logTaskError(t);
            }
            synchronized(thread) {
                while (pause && (! stop)) {
                    waitForNotify(thread);
                }
            }
        }
    }

    void addListener(ImageTaskQueueListener listener) {
        listeners.add(listener);
    }

    void removeListener(ImageTaskQueueListener listener) {
        listeners.remove(listener);
    }

    private static void waitForNotify(Object monitor) {
        synchronized(monitor) {
            boolean interrupted;
            do {
                try {
                    interrupted = false;
                    monitor.wait();
                }
                catch (InterruptedException e) {
                    interrupted = true;
                }
            } while (interrupted);
        }
    }

    private void logQueueSize() {
        final int depth = queue.size();
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    for (ImageTaskQueueListener listener : listeners) {
                        listener.queueDepthChanged(depth);
                    }
                }
            }
        );
    }

    private void logTaskError(Throwable t) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Abort ImageTask--");
        buffer.append(t.getClass().getName());
        if (t.getMessage() != null) {
            buffer.append(": ");
            buffer.append(t.getMessage());
        }
        System.err.println(buffer);
        t.printStackTrace();
    }
}
