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
import geogebra.main.Application;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.*;

/**
 * Input Dialog for a GeoAngle object with additional option to choose between
 * "clock wise" and "counter clockwise"
 * 
 * @author hohenwarter
 */
public class AngleInputDialog extends InputDialog {

  private static final long serialVersionUID = 1L;

  private final JRadioButton rbCounterClockWise, rbClockWise;

  public boolean success = true;

  /**
   * Input Dialog for a GeoAngle object.
   */
  protected AngleInputDialog(Application app, String message, String title,
      String initString, boolean autoComplete, InputHandler handler,
      boolean modal) {
    super(app.getFrame(), modal);
    this.app = app;
    inputHandler = handler;
    this.initString = initString;

    // create radio buttons for "clockwise" and "counter clockwise"
    ButtonGroup bg = new ButtonGroup();
    rbCounterClockWise = new JRadioButton(Plain.counterClockwise);
    rbClockWise = new JRadioButton(Plain.clockwise);
    bg.add(rbCounterClockWise);
    bg.add(rbClockWise);
    rbCounterClockWise.setSelected(true);
    JPanel rbPanel = new JPanel(new BorderLayout());
    rbPanel.add(rbCounterClockWise, BorderLayout.NORTH);
    rbPanel.add(rbClockWise, BorderLayout.SOUTH);
    rbPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));

    createGUI(title, message, autoComplete, DEFAULT_COLUMNS, 1, true, true,
        false, false, false, false);
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.add(inputPanel, BorderLayout.CENTER);
    centerPanel.add(rbPanel, BorderLayout.SOUTH);
    getContentPane().add(centerPanel, BorderLayout.CENTER);
    centerOnScreen();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    boolean finished = false;
    success = true;
    try {

      if (source == btOK || source == inputPanel.getTextComponent()) {
        inputText = inputPanel.getText();

        // negative orientation ?
        if (rbClockWise.isSelected())
          inputText = "-(" + inputText + ")";

        finished = inputHandler.processInput(inputText);
      } else if (source == btCancel) {
        finished = true;
        success = false;
      }
    } catch (Exception ex) {
      // do nothing on uninitializedValue
      success = false;
    }
    setVisible(!finished);
  }

  public boolean isCounterClockWise() {
    return rbCounterClockWise.isSelected();
  }
}
