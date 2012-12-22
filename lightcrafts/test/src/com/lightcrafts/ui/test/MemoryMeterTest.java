/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.test;

import com.lightcrafts.ui.MemoryMeter;

import javax.swing.*;
import java.util.LinkedList;
import java.awt.*;

public class MemoryMeterTest {

    public static void main(String[] args) {

        JPanel panel = new JPanel();
        panel.add(new MemoryMeter());
        panel.add(new MemoryMeter());
        panel.add(new MemoryMeter());
        panel.add(new MemoryMeter());
        panel.add(new MemoryMeter());
        panel.add(new MemoryMeter());
        panel.add(new MemoryMeter());

        Runnable waster = new Runnable() {
            LinkedList list = new LinkedList();
            public void run() {
                while (true) {
                    list.add(new byte[100000]);
                    System.out.println("" +
                        Runtime.getRuntime().maxMemory() + ": " +
                        Runtime.getRuntime().totalMemory() + ", " +
                        Runtime.getRuntime().freeMemory()
                    );
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        };
        Thread wasteThread = new Thread(waster, "Waster");
        wasteThread.setDaemon(true);
        wasteThread.start();

        JFrame frame = new JFrame("MemoryMeter Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(panel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setBounds(100, 100, 400, 400);
        frame.setVisible(true);
    }
}
