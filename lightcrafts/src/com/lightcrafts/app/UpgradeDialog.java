/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.toolkit.TextAreaFactory;
import com.lightcrafts.utils.WebBrowser;
import com.lightcrafts.platform.Platform;

import static com.lightcrafts.app.Locale.LOCALE;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class UpgradeDialog extends JDialog {

    private final static String UpgradeUrl =
        "http://www.lightcrafts.com/store";

    // Indicates the user chose the "Buy Now..." option to upgrade.
    private boolean bought;

    public UpgradeDialog(JFrame parent) {
        super(parent);

        JButton buyButton = new JButton(LOCALE.get("UpgradeButton"));
        JButton cancelButton = new JButton(LOCALE.get("UpgradeCancelButton"));

        buyButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    bought = true;
                    WebBrowser.browse(UpgradeUrl);
                    setVisible(false);
                }
            }
        );
        cancelButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            }
        );
        Box buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancelButton);
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(buyButton);
        buttons.setBorder(BorderFactory.createEmptyBorder(6, 12, 12, 12));

        JTextArea text = TextAreaFactory.createTextArea(
            LOCALE.get("UpgradeText1") + '\n' +
            '\n' +
            LOCALE.get("UpgradeText2") + '\n' +
            '\n' +
            LOCALE.get("UpgradeText3"),
            30
        );
        text.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(text);
        panel.add(buttons, BorderLayout.SOUTH);

        text.setBackground(panel.getBackground());

        setTitle(LOCALE.get("UpgradeDialogTitle"));
        setModal(true);
        getContentPane().add(panel);
        getRootPane().setDefaultButton(buyButton);

        pack();

        setLocationRelativeTo(parent);
    }

    public boolean userBought() {
        return bought;
    }

    public static void main(String[] args) throws Exception  {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        UpgradeDialog dialog = new UpgradeDialog(null);
        dialog.setVisible(true);
        System.out.println(dialog.userBought() ? "bought" : "not bought");
        System.exit(0);
    }
}
