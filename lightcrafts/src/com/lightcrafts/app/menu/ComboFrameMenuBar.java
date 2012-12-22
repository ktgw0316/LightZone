/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public final class ComboFrameMenuBar extends JMenuBar {

    // save all disposable menus for disposal
    private List<UpdatableDisposableMenu> menus =
        new ArrayList<UpdatableDisposableMenu>();

    // The BrowserMenu needs to show and hide with the application mode:
    private UpdatableDisposableMenu browserMenu;

    // The WindowMenu needs special cleanup:
    private WindowMenu windowMenu;

    // Need access to the frame so we can add and remove the browser menu:
    private ComboFrame frame;

    // Make a menu bar to show on the Mac when there is no active ComboFrame.
    public ComboFrameMenuBar() {
        this(null);
        update();   // disable everything
    }

    public ComboFrameMenuBar( ComboFrame frame ) {
        this.frame = frame;
        add( new FileMenu( frame ) );
        add( new EditMenu( frame ) );
        browserMenu = new BrowserMenu( frame );
        add( browserMenu );
        add( new OperationMenu( frame ) );
        add( new RegionMenu( frame ) );
        add( new TemplatesMenu( frame ) );
        add( new ViewMenu( frame ) );
        windowMenu = new WindowMenu( frame );
        add( windowMenu );
        add( new HelpMenu( frame ) );
        if ( System.getProperty( "lightcrafts.debug" ) != null )
            add( new DebugMenu( frame ) );
    }

    public JMenu add(JMenu menu) {
        // Menu bars references are held forever by AWT on the Mac,
        // so use only DisposableMenus and be sure to call dispose().
        super.add(menu);
        if (menu instanceof UpdatableDisposableMenu) {
            menus.add((UpdatableDisposableMenu) menu);
        }
        return menu;
    }

    public void update() {
        for (UpdatableDisposableMenu menu : menus) {
            menu.update();
        }
        if (browserMenu == null) {
            browserMenu = new BrowserMenu(frame);
            menus.add(browserMenu);
            add(browserMenu, 2);
        }
    }

    public void dispose() {
        WindowMenu.destroyMenu(windowMenu);
        windowMenu = null;
        for (UpdatableDisposableMenu menu : menus) {
            menu.dispose();
        }
        menus.clear();
        removeAll();
    }
}
