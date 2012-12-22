/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.drag.test;

import com.lightcrafts.ui.operation.drag.StackableComponent;
import com.lightcrafts.ui.operation.drag.DraggableStack;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class DraggableStackTest {
    private static Random random = new Random();
    static class DraggableLabel extends JPanel implements StackableComponent {
        private JLabel dragPart;
        public DraggableLabel(String text) {
            JLabel label = new JLabel(text);
            label.setHorizontalAlignment(JLabel.CENTER);
            add(label);
            dragPart = new JLabel("drag");
            dragPart.setHorizontalAlignment(JLabel.CENTER);
            add(dragPart);
            setPreferredSize(
                new Dimension(
                    200,
                    getPreferredSize().height + random.nextInt(100)
                )
            );
            setBorder(BorderFactory.createLineBorder(Color.black));
        }
        public JComponent getDraggableComponent() {
            return dragPart;
        }
        public boolean isSwappable() {
            return true;
        }
    }

    public static void main(String[] args) {

        final DraggableStack stack = new DraggableStack();

        stack.push(new DraggableLabel("A"));
        stack.push(new DraggableLabel("B"));
        stack.push(new DraggableLabel("C"));

        JButton add = new JButton("+");
        add.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    stack.push(new DraggableLabel("X"));
                }
            }
        );

        JButton remove = new JButton("-");
        remove.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    stack.pop();
                }
            }
        );

        JScrollPane scroll = new JScrollPane(stack);
        scroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        );
        JFrame frame = new JFrame("Test");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(scroll);
        frame.getContentPane().add(add, BorderLayout.NORTH);
        frame.getContentPane().add(remove, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 400, 400);
        frame.setVisible(true);
    }
}
