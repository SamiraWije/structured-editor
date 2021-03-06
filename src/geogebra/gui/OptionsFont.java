package geogebra.gui;

import geogebra.Plain;
import geogebra.gui.util.SpringUtilities;
import geogebra.main.Application;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.*;

/**
 * Options for font sizes & language.
 * 
 * @author Florian Sonner
 */
class OptionsFont extends JPanel implements ActionListener {
  /** */
  private static final long serialVersionUID = 1L;

  private final Application app;

  private TitleLabel fontTitleLabel, languageTitleLabel;
  private JLabel guiSizeLabel, axesSizeLabel, euclidianSizeLabel,
      languageLabel;
  private JComboBox guiSizeCb, axesSizeCb, euclidianSizeCb;
  private boolean updateFonts = false;

  private JComboBox languageCb;
  private boolean updateLanguage;

  /**
   * Create a new JPanel for all options for fonts & language.
   * 
   * @param app
   */
  protected OptionsFont(Application app) {
    this.app = app;

    initGUI();
    updateGUI();
  }

  /**
   * A list item from the font list was selected.
   */
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == guiSizeCb) {
      int fontSize = Integer.parseInt((String) guiSizeCb.getSelectedItem());
      app.setGUIFontSize(fontSize, false);
      updateFonts = true;
    } else if (e.getSource() == axesSizeCb) {
      int fontSize = Integer.parseInt((String) axesSizeCb.getSelectedItem());
      app.setAxesFontSize(fontSize, false);
      updateFonts = true;
    } else if (e.getSource() == euclidianSizeCb) {
      int fontSize = Integer.parseInt((String) euclidianSizeCb
          .getSelectedItem());
      app.setEuclidianFontSize(fontSize, false);
      updateFonts = true;
    } else if (e.getSource() == languageCb)
      updateLanguage = true;
  }

  /**
   * Apply the options.
   */
  protected void apply() {
    if (updateFonts) {
      app.resetFonts();
      updateFonts = false;
    }

    if (updateLanguage) {
      Locale loc = Application.supportedLocales.get(languageCb
          .getSelectedIndex());
      app.setLanguage(loc);
      updateLanguage = false;
    }
  }

  /**
   * Initialize the GUI.
   */
  private void initGUI() {
    String[] fontSizeStr = new String[]{"10", "12", "14", "16", "18", "20",
        "24"};

    JPanel panel = new JPanel();

    // font size of GUI
    guiSizeCb = new JComboBox(fontSizeStr);
    guiSizeCb.setMaximumSize(new Dimension(80, 0));
    guiSizeCb.setSelectedItem(Integer.toString(app.getFontSize()));
    guiSizeCb.addActionListener(this);
    guiSizeLabel = new JLabel();

    // font size of drawing pad
    euclidianSizeCb = new JComboBox(fontSizeStr);
    euclidianSizeCb.setMaximumSize(new Dimension(80, 0));
    euclidianSizeCb.setSelectedItem(Integer
        .toString(app.getEuclidianFontSize()));
    euclidianSizeCb.addActionListener(this);
    euclidianSizeLabel = new JLabel();

    // font size of coordinate system
    axesSizeCb = new JComboBox(fontSizeStr);
    axesSizeCb.setMaximumSize(new Dimension(80, 0));
    axesSizeCb.setSelectedItem(Integer.toString(app.getAxesFontSize()));
    axesSizeCb.addActionListener(this);
    axesSizeLabel = new JLabel();

    // construct the font size panel
    fontTitleLabel = new TitleLabel();
    panel.add(fontTitleLabel);
    panel.add(Box.createHorizontalGlue());

    panel.add(guiSizeLabel);
    panel.add(guiSizeCb);
    panel.add(euclidianSizeLabel);
    panel.add(euclidianSizeCb);
    panel.add(axesSizeLabel);
    panel.add(axesSizeCb);

    // language panel
    String[] languages = new String[Application.supportedLocales.size()];
    String ggbLangCode;

    for (int i = 0; i < Application.supportedLocales.size(); i++) {
      Locale loc = Application.supportedLocales.get(i);
      ggbLangCode = loc.getLanguage() + loc.getCountry() + loc.getVariant();

      // enforce to show specialLanguageNames first
      // because here getDisplayLanguage doesn't return a good result
      languages[i] = Application.specialLanguageNames.get(ggbLangCode);
      if (languages[i] == null)
        languages[i] = loc.getDisplayLanguage(Locale.ENGLISH);
    }

    languageCb = new JComboBox(languages);
    languageCb.addActionListener(this);
    languageLabel = new JLabel();

    panel.add(Box.createHorizontalGlue());
    panel.add(new JSeparator());

    languageTitleLabel = new TitleLabel();
    panel.add(languageTitleLabel);
    panel.add(Box.createHorizontalGlue());

    panel.add(languageLabel);
    panel.add(languageCb);

    panel.setLayout(new SpringLayout());
    SpringUtilities.makeCompactGridColspan(panel, 7, 2, // rows, columns
        3, 3, 15, 15);

    // use a GridBagLayout in the main panel
    setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.CENTER;
    c.weightx = 1.0;
    c.weighty = 1E-12;

    c.gridx = 0;
    c.gridy = 0;
    c.weighty = 1.0;

    add(panel, c);
    add(Box.createVerticalGlue(), c);

    setLabels();
  }

  /**
   * Update all labels.
   */
  protected void setLabels() {
    fontTitleLabel.setText(geogebra.Menu.FontSize);
    guiSizeLabel.setText(Plain.FontSizeGUI);
    euclidianSizeLabel.setText(Plain.FontSizeEuclidian);
    axesSizeLabel.setText(Plain.FontSizeAxes);

    languageTitleLabel.setText(geogebra.Menu.Language);
    languageLabel.setText(geogebra.Menu.Language);
  }

  /**
   * Update the GUI.
   */
  protected void updateGUI() {
    guiSizeCb.removeActionListener(this);
    euclidianSizeCb.removeActionListener(this);
    axesSizeCb.removeActionListener(this);

    guiSizeCb.setSelectedItem(Integer.toString(app.getFontSize()));
    euclidianSizeCb.setSelectedItem(Integer
        .toString(app.getEuclidianFontSize()));
    axesSizeCb.setSelectedItem(Integer.toString(app.getAxesFontSize()));

    guiSizeCb.addActionListener(this);
    euclidianSizeCb.addActionListener(this);
    axesSizeCb.addActionListener(this);

    for (int i = 0; i < Application.supportedLocales.size(); i++)
      if (app.getLocale() == Application.supportedLocales.get(i)) {
        languageCb.setSelectedIndex(i);
        break;
      }
  }
}