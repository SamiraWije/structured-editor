/* 
GeoGebra - Dynamic Mathematics for Schools
Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package geogebra.gui;

import geogebra.Plain;
import geogebra.gui.inputbar.AutoCompleteTextField;
import geogebra.gui.view.algebra.InputPanel;
import geogebra.kernel.Construction;
import geogebra.kernel.GeoElement;
import geogebra.kernel.GeoJavaScriptButton;
import geogebra.main.Application;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;

public class JavaScriptDialog extends JDialog
    implements
      ActionListener,
      KeyListener,
      WindowListener {

  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;
  private JTextComponent tfCaption, tfScript;
  private JPanel btPanel;
  private JButton btApply, btCancel;
  private InputPanel tfLabel;
  private JPanel optionPane;

  private final Application app;

  private GeoElement geoResult = null;
  private GeoJavaScriptButton button = null;

  InputPanel inputPanel, inputPanel2;

  /**
   * Creates a dialog to create a new GeoNumeric for a slider.
   * 
   * @param x
   *          , y: location of slider in screen coords
   */
  protected JavaScriptDialog(Application app, int x, int y) {
    super(app.getFrame(), true);
    this.app = app;
    addWindowListener(this);

    // create temp geos that may be returned as result
    Construction cons = app.getKernel().getConstruction();
    button = new GeoJavaScriptButton(cons);
    button.setEuclidianVisible(true);
    button.setAbsoluteScreenLoc(x, y);

    createGUI();
    pack();
    setLocationRelativeTo(app.getMainComponent());
  }

  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    Application.debug(tfScript.getText());
    if (source == btApply) {

      button.setLabel(null);
      button.setScript(tfScript.getText());

      // set caption text
      String strCaption = tfCaption.getText().trim();
      if (strCaption.length() > 0)
        button.setCaption(strCaption);

      button.setEuclidianVisible(true);
      button.setLabelVisible(true);
      button.updateRepaint();

      geoResult = button;
      setVisible(false);
    } else if (source == btCancel) {
      geoResult = null;
      setVisible(false);
    }
  }

  private void createGUI() {
    setTitle(Plain.JavaScriptButton);
    setResizable(false);

    // create caption panel
    JLabel captionLabel = new JLabel(geogebra.Menu.Button_Caption + ":");
    String initString = button == null ? "" : button.getCaption();
    InputPanel ip = new InputPanel(initString, app, 1, 15, true, true, false);
    tfCaption = ip.getTextComponent();
    if (tfCaption instanceof AutoCompleteTextField) {
      AutoCompleteTextField atf = (AutoCompleteTextField) tfCaption;
      atf.setAutoComplete(false);
    }

    captionLabel.setLabelFor(tfCaption);
    JPanel captionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    captionPanel.add(captionLabel);
    captionPanel.add(ip);

    // create script panel
    JLabel scriptLabel = new JLabel(Plain.JavaScript + ":");
    initString = button == null || button.getScript().equals("")
        ? "ggbApplet.evalCommand('A=(3,4)');\n"
        : button.getScript();
    InputPanel ip2 = new InputPanel(initString, app, 10, 40, false, false,
        false);
    tfScript = ip2.getTextComponent();
    if (tfScript instanceof AutoCompleteTextField) {
      AutoCompleteTextField atf = (AutoCompleteTextField) tfScript;
      atf.setAutoComplete(false);
    }

    scriptLabel.setLabelFor(tfScript);
    JPanel scriptPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    scriptPanel.add(scriptLabel);
    scriptPanel.add(ip2);

    // buttons
    btApply = new JButton(Plain.Apply);
    btApply.setActionCommand("Apply");
    btApply.addActionListener(this);
    btCancel = new JButton(Plain.Cancel);
    btCancel.setActionCommand("Cancel");
    btCancel.addActionListener(this);
    btPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    btPanel.add(btApply);
    btPanel.add(btCancel);

    // Create the JOptionPane.
    optionPane = new JPanel(new BorderLayout(5, 5));

    // create object list
    optionPane.add(captionPanel, BorderLayout.NORTH);
    optionPane.add(scriptPanel, BorderLayout.CENTER);
    optionPane.add(btPanel, BorderLayout.SOUTH);
    optionPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    // Make this dialog display it.
    setContentPane(optionPane);

    /*
     * 
     * inputPanel = new InputPanel("ggbApplet.evalCommand('A=(3,4)');", app, 10,
     * 50, false, true, false ); inputPanel2 = new
     * InputPanel("function func() {\n}", app, 10, 50, false, true, false );
     * 
     * JPanel centerPanel = new JPanel(new BorderLayout());
     * 
     * centerPanel.add(inputPanel, BorderLayout.CENTER);
     * centerPanel.add(inputPanel2, BorderLayout.SOUTH);
     * getContentPane().add(centerPanel, BorderLayout.CENTER);
     * //centerOnScreen();
     * 
     * setContentPane(centerPanel); pack();
     * setLocationRelativeTo(app.getFrame());
     */
  }

  public GeoElement getResult() {
    if (geoResult != null) {
      // set label of geoResult
      String strLabel;
      try {
        strLabel = app.getKernel().getAlgebraProcessor().parseLabel(
            tfLabel.getText());
      } catch (Exception e) {
        strLabel = null;
      }
      geoResult.setLabel(strLabel);
    }

    return geoResult;
  }

  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case KeyEvent.VK_ENTER :
        btApply.doClick();
        break;

      case KeyEvent.VK_ESCAPE :
        btCancel.doClick();
        e.consume();
        break;
    }
  }

  public void keyReleased(KeyEvent arg0) {
  }

  public void keyTyped(KeyEvent arg0) {
  }

  public void windowActivated(WindowEvent arg0) {
  }

  public void windowClosed(WindowEvent arg0) {
  }

  public void windowClosing(WindowEvent arg0) {
  }

  public void windowDeactivated(WindowEvent arg0) {
  }

  public void windowDeiconified(WindowEvent arg0) {
  }

  public void windowIconified(WindowEvent arg0) {
  }

  public void windowOpened(WindowEvent arg0) {
    // setLabelFieldFocus();
  }

}