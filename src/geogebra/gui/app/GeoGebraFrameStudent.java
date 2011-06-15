package geogebra.gui.app;

import testSystem.TestEditorGeom;
import geogebra.main.Application;
import geogebra.main.DefaultApplication;
import geogebra.main.GeoGebraPreferences;
import geogebra.util.Util;
import ru.ipo.structurededitor.StructuredEditor;
import testSystem.structureSerializer.NodesRegistry;
import testSystem.lang.geom.GeoStatement;
import ru.ipo.structurededitor.view.StructuredEditorModel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.*;
import java.awt.*;
import java.awt.dnd.DropTarget;

/**
 * Created by IntelliJ IDEA.
 * User: oleg
 * Date: 20.03.11
 * Time: 18:50
 * To change this template use File | Settings | File Templates.
 */

public class GeoGebraFrameStudent extends GeoGebraFrame {
    public static synchronized void main(String[] args) {
        System.out.println("check java version");
        double javaVersion = Util.getJavaVersion();
        if (javaVersion < 1.42) {
            JOptionPane
                    .showMessageDialog(
                            null,
                            "Sorry, GeoGebra cannot be used with your Java version "
                                    + javaVersion
                                    + "\nPlease visit http://www.java.com to get a newer version of Java.");
            return;
        }

        if (Application.MAC_OS)
            initMacSpecifics();

        // set system look and feel
        System.out.println("set system look and feel");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            Application.debug(e + "");
        }

        System.out.println("load list of previously used files");
        GeoGebraPreferences.getPref().loadFileList();

        // create first window and show it
        System.out.println("create first window and show it");
        createNewWindow(args);
    }

    public static synchronized GeoGebraFrame createNewWindow(String[] args) {
        // set Application's size, position and font size
        // TODO: Add layout glass pane (F.S.)
        GeoGebraFrame wnd = new GeoGebraFrameStudent();
        wnd.setLayout(new BorderLayout());

        final DefaultApplication app = new DefaultApplication(args, wnd, true);

        System.out.println("Load GUI JAR");
        app.loadGUIJar();
        app.getGuiManager().initMenubar();

        // init GUI
        System.out.println("init GUI");
        wnd.app = app;

        wnd.getContentPane().add(app.buildApplicationPanel(), BorderLayout.EAST);
        //StructuredEditor
        NodesRegistry nodesRegistry = TestEditorGeom.nodesRegistryPrep();


        //------------Frame preparation

        GeoStatement st = new GeoStatement();
        final StructuredEditorModel model = TestEditorGeom.createModel(st);

//        f.add(new JScrollPane(new JTextArea("asdf")));
        structuredEditor = new StructuredEditor(model,true);


        JPanel taskPanel= new JPanel(new BorderLayout());

        StyleContext sc = new StyleContext();
        final DefaultStyledDocument doc = new DefaultStyledDocument(sc);
        JTextPane textPane = new JTextPane(doc);
        textPane.setEditable(false);
        final Style heading2Style = sc.addStyle("Heading2", null);
        heading2Style.addAttribute(StyleConstants.Foreground, Color.red);
        heading2Style.addAttribute(StyleConstants.FontSize, 16);
        heading2Style.addAttribute(StyleConstants.FontFamily, "serif");
        heading2Style.addAttribute(StyleConstants.Bold, true);


        StyledDocument styledDocument =  textPane.getStyledDocument();


        try {
            //((GeoStatement)model.getObject()).getTitle()
            styledDocument.getLength();
            styledDocument.insertString(0,"Химия и физика\n биология\n",null);
            styledDocument.setParagraphAttributes(0, 1, heading2Style, false);
        }catch (Exception e) {
            throw new Error("Text HTML error"+e);
        }
        Border border = BorderFactory.createLineBorder(Color.BLACK);

        textPane.setBorder(border);
        taskPanel.add(textPane, BorderLayout.CENTER);
        JScrollPane structuredEditorScrPane = new JScrollPane(structuredEditor);
        wnd.getContentPane().add(taskPanel, BorderLayout.CENTER);
        //wnd.getContentPane().add(structuredEditorScrPane, BorderLayout.CENTER);
        structuredEditor.requestFocusInWindow();
        structuredEditor.setApp(app);
        TestEditorGeom.createBars(wnd, structuredEditor, nodesRegistry);
        //---StructuredEditor
        wnd.setDropTarget(new DropTarget(wnd,
                new geogebra.gui.FileDropTargetListener(app)));
        wnd.addWindowFocusListener(wnd);
        updateAllTitles();
        wnd.setVisible(true);

        // init some things in the background
        System.out.println("init some things in the background");
        if (!app.isApplet()) {
            Thread runner = new Thread() {
                @Override
                public void run() {
                    System.out.println("init CAS");
                    app.initCAS();

                    System.out.println("init properties dialog");
                    app.getGuiManager().initPropertiesDialog();

                    System.out.println("init file chooser");
                    app.getGuiManager().initFileChooser();

                    System.out.println("copy Jar files to temp directory");
                    app.downloadJarFiles();
                }
            };
            runner.start();
        }
        //structuredEditor.getApp().getEuclidianView().setSelectionRectangle(new Rectangle(
        //                structuredEditor.getApp().getEuclidianView().getSize()));
        //structuredEditor.getApp().selectAll(0);
        return wnd;
    }

    @Override
    public void setVisible(boolean flag) {
        super.setVisible(flag);
        structuredEditor.setSize(structuredEditor.getPreferredSize());
        structuredEditor.repaint();
    }

    public void updateSize() {
        super.updateSize();
        Dimension dim = getSize(),
                dim1 = structuredEditor.getPreferredSize();
        structuredEditor.setSize(dim1);

        dim.setSize(dim.getWidth() + dim1.getWidth(), dim.getHeight() + dim1.getHeight());
        Rectangle screenSize = app.getScreenSize();

        if (dim.width > screenSize.width
                || dim.height > screenSize.height) {
            dim.width = screenSize.width;
            dim.height = screenSize.height;
            setLocation(0, 0);
        }
        setSize(dim);
    }

    private static StructuredEditor structuredEditor;


}
