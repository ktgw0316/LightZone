/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

// A Thread that periodically posts an invocation target to the event queue,
// and if the target is not dequeued within a time limit, delivers some
// notification to developers.

class AwtWatchdog extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(AwtWatchdog.class);

    private static long DequeueLimit = 2000;
    private static long ThreadDumpLimit = 15000;
    private static NumberFormat TimeFormat = new DecimalFormat("0.000");

    private static AwtWatchdog Instance;

    private boolean invoked;
    private boolean cancelled;

    private AwtWatchdog() {
        super("AWT Watchdog");
        setDaemon(true);
    }

    static void spawn() {
        if (Instance == null) {
            Instance = new AwtWatchdog();
            Instance.start();
        }
    }

    static void deSpawn() {
        if (Instance != null) {
            Instance.cancel();
        }
    }

    static void dumpThreads() {
        Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
        for (Thread thread : stacks.keySet()) {
            logger.info("{}: {}", thread.getName(), thread.getState());
            StackTraceElement[] stack = stacks.get(thread);
            for (StackTraceElement frame : stack) {
                logger.info("\tat {}", frame);
            }
        }
    }

    public void run() {
        while (! cancelled) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        invoked = true;
                    }
                }
            );
            long start = System.currentTimeMillis();
            do {
                try {
                    Thread.sleep(DequeueLimit);
                }
                catch (InterruptedException e) {
                }
                if (! invoked) {
                    long time = System.currentTimeMillis();
                    String s = TimeFormat.format((time - start) / 1000D);
                    String message =
                        "EventThread blocked for " + s + " seconds";
                    logger.warn(message);
                    if ((time - start) > ThreadDumpLimit) {
                        if (System.getProperty("lightcrafts.debug") == null) {
                            dumpThreads();
                        }
                    }
                }
            } while (! invoked);

            invoked = false;
        }
    }

    void cancel() {
        cancelled = true;
    }
}
