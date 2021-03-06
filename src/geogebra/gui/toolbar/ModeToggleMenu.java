/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package geogebra.gui.toolbar;

import geogebra.main.Application;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

import javax.swing.*;

/**
 * JToggle button combined with popup menu for mode selction
 */
public class ModeToggleMenu extends JPanel {

  // sets new mode when item in popup menu is selected
  private class MenuItemListener implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      JMenuItem item = (JMenuItem) e.getSource();
      selectItem(item);
      tbutton.doClick();
    }
  }
  private static final long serialVersionUID = 1L;
  ModeToggleButtonGroup bg;
  private final MyJToggleButton tbutton;
  private final JPopupMenu popMenu;

  private final ArrayList<JMenuItem> menuItemList;
  private final ActionListener popupMenuItemListener;
  private final Application app;

  int size;

  public ModeToggleMenu(Application app, ModeToggleButtonGroup bg) {
    this.app = app;
    this.bg = bg;

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    tbutton = new MyJToggleButton(this, app);
    tbutton.setAlignmentY(BOTTOM_ALIGNMENT);
    add(tbutton);

    popMenu = new JPopupMenu();
    menuItemList = new ArrayList<JMenuItem>();
    popupMenuItemListener = new MenuItemListener();
    size = 0;
  }

  public void addMode(int mode) {
    // add menu item to popu menu
    JMenuItem mi = new JMenuItem();
    mi.setFont(app.getPlainFont());

    // tool name as text
    mi.setText(app.getToolName(mode));

    Icon icon = app.getModeIcon(mode);
    String actionText = Integer.toString(mode);
    mi.setIcon(icon);
    mi.setActionCommand(actionText);
    mi.addActionListener(popupMenuItemListener);

    popMenu.add(mi);
    menuItemList.add(mi);
    size++;

    if (size == 1) {
      // init tbutton
      tbutton.setIcon(icon);
      tbutton.setActionCommand(actionText);

      // tooltip: tool name and tool help
      tbutton.setToolTipText(app.getToolTooltipHTML(mode));

      // add button to button group
      bg.add(tbutton);
    }
  }

  public void addSeparator() {
    popMenu.addSeparator();
  }

  /**
   * Removes all modes from the toggle menu. Used for the temporary perspective.
   * 
   * @author Florian Sonner
   * @version 2008-10-22
   */
  public void clearModes() {
    popMenu.removeAll();
    menuItemList.clear();
    size = 0;
  }

  public int getFirstMode() {
    if (menuItemList == null || menuItemList.size() == 0)
      return -1;
    else {
      JMenuItem mi = menuItemList.get(0);
      return Integer.parseInt(mi.getActionCommand());
    }
  }

  public JToggleButton getJToggleButton() {
    return tbutton;
  }

  public int getToolsCount() {
    return size;
  }

  public boolean isPopupShowing() {
    return popMenu.isShowing();
  }

  public void mouseOver() {
    // popup menu is showing
    JPopupMenu activeMenu = bg.getActivePopupMenu();
    if (activeMenu != null && activeMenu.isShowing())
      setPopupVisible(true);
  }

  private void selectItem(JMenuItem mi) {
    // check if the menu item is already selected
    if (tbutton.isSelected()
        && tbutton.getActionCommand() == mi.getActionCommand())
      return;

    tbutton.setIcon(mi.getIcon());
    tbutton.setToolTipText(app.getToolTooltipHTML(Integer.parseInt(mi
        .getActionCommand())));
    tbutton.setActionCommand(mi.getActionCommand());
    tbutton.setSelected(true);
    // tbutton.requestFocus();
  }

  public boolean selectMode(int mode) {
    String modeText = mode + "";

    for (int i = 0; i < size; i++) {
      JMenuItem mi = menuItemList.get(i);
      // found item for mode?
      if (mi.getActionCommand().equals(modeText)) {
        selectItem(mi);
        return true;
      }
    }
    return false;
  }

  public void setMode(int mode) {
    app.setMode(mode);
  }

  // shows popup menu
  public void setPopupVisible(boolean flag) {
    if (flag) {
      bg.setActivePopupMenu(popMenu);
      if (popMenu.isShowing())
        return;
      Point locButton = tbutton.getLocationOnScreen();
      Point locApp = app.getMainComponent().getLocationOnScreen();

      // display the popup above the button if the toolbar is at the top of the
      // window
      if (app.showToolBarTop())
        popMenu.show(app.getMainComponent(), locButton.x - locApp.x,
            locButton.y - locApp.y + tbutton.getHeight());
      else
        popMenu.show(app.getMainComponent(), locButton.x - locApp.x,
            locButton.y - locApp.y
                - (int) popMenu.getPreferredSize().getHeight());
    } else
      popMenu.setVisible(false);

    tbutton.repaint();
  }

}

class MyJToggleButton extends JToggleButton
    implements
      MouseListener,
      MouseMotionListener,
      ActionListener {
  /**
   * 
   */
  private static final long serialVersionUID = -2740851859596148804L;
  /**
	 * 
	 */
  // private static final long serialVersionUID = 1L;
  private static int BORDER = 6;
  private int iconWidth, iconHeight;
  private GeneralPath gpDown; // the path for an arrow pointing down
  private GeneralPath gpUp; // the path for an arrow pointing up
  private boolean showToolTipText = true;
  private boolean popupTriangleHighlighting = false;
  private final ModeToggleMenu menu;
  private final Application app;

  private static final Color arrowColor = new Color(0, 0, 0, 130);
  // private static final Color selColor = new Color(166, 11, 30,150);
  // private static final Color selColor = new Color(17, 26, 100, 200);
  private static final Color selColor = new Color(0, 0, 153, 200);
  private static final BasicStroke selStroke = new BasicStroke(3f);

  MyJToggleButton(ModeToggleMenu menu, Application app) {
    super();
    this.menu = menu;
    this.app = app;

    // add own listeners
    addMouseListener(this);
    addMouseMotionListener(this);
    addActionListener(this);
  }

  // set mode
  public void actionPerformed(ActionEvent e) {
    menu.setMode(Integer.parseInt(e.getActionCommand()));
  }

  @Override
  public void doClick() {
    super.doClick();
    if (!hasFocus())
      requestFocusInWindow();
  }

  @Override
  public String getToolTipText() {
    if (showToolTipText)
      return super.getToolTipText();
    else
      return null;
  }

  private void initPath() {
    gpDown = new GeneralPath();
    int x = BORDER + iconWidth + 2;
    int y = BORDER + iconHeight + 1;
    gpDown.moveTo(x - 6, y - 5);
    gpDown.lineTo(x, y - 5);
    gpDown.lineTo(x - 3, y);
    gpDown.closePath();

    gpUp = new GeneralPath();
    x = BORDER + iconWidth + 2;
    y = BORDER + 2;
    gpUp.moveTo(x - 6, y);
    gpUp.lineTo(x, y);
    gpUp.lineTo(x - 3, y - 5);
    gpUp.closePath();
  }

  private boolean inPopupTriangle(int x, int y) {
    return menu.size > 1
        && (app.showToolBarTop() ? y > iconHeight - 2 : y < 12);
  }

  public void mouseClicked(MouseEvent e) {

  }

  public void mouseDragged(MouseEvent e) {
    if (inPopupTriangle(e.getX(), e.getY()))
      menu.setPopupVisible(true);
  }

  public void mouseEntered(MouseEvent arg0) {
  }

  public void mouseExited(MouseEvent arg0) {
    if (popupTriangleHighlighting) {
      popupTriangleHighlighting = false;
      repaint();
    }
  }

  public void mouseMoved(MouseEvent e) {
    menu.mouseOver();
    showToolTipText = !menu.isPopupShowing();

    // highlight popup menu triangle
    if (menu.size > 1
        && popupTriangleHighlighting != inPopupTriangle(e.getX(), e.getY())) {
      popupTriangleHighlighting = !popupTriangleHighlighting;
      repaint();
    }
  }

  public void mousePressed(MouseEvent e) {
    menu.setPopupVisible(inPopupTriangle(e.getX(), e.getY()));
    requestFocus();
    // doClick(); removed to stop mode being selected when triangle clicked (for
    // MODE_FITLINE)
  }

  public void mouseReleased(MouseEvent e) {

    // Mathieu Blossier - 2009-07-06
    // doClick seems to be called twice

    doClick();
  }

  @Override
  public void paint(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    Stroke oldStroke = g2.getStroke();

    super.paint(g2);

    if (isSelected()) {
      g2.setColor(selColor);
      g2.setStroke(selStroke);
      g2.drawRect(BORDER - 1, BORDER - 1, iconWidth + 1, iconHeight + 1);

      g2.setStroke(oldStroke);
    }

    // draw little arrow (for popup menu)
    if (menu.size > 1) {
      if (gpDown == null)
        initPath();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);

      GeneralPath usedPath = app.showToolBarTop() ? gpDown : gpUp;

      if (popupTriangleHighlighting || menu.isPopupShowing()) {
        g2.setColor(Color.red);
        g2.fill(usedPath);
        g2.setColor(Color.black);
        g2.draw(usedPath);
      } else {
        g2.setColor(Color.white);
        g2.fill(usedPath);
        g2.setColor(arrowColor);
        g2.draw(usedPath);
      }

    }
  }

  @Override
  public void setIcon(Icon icon) {
    super.setIcon(icon);
    iconWidth = icon.getIconWidth();
    iconHeight = icon.getIconHeight();
    Dimension dim = new Dimension(iconWidth + 2 * BORDER, iconHeight + 2
        * BORDER);
    setPreferredSize(dim);
    setMinimumSize(dim);
    setMaximumSize(dim);
  }

}
