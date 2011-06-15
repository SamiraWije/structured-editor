package testSystem;

import testSystem.view.editors.*;
import geogebra.kernel.GeoElement;
import geogebra.kernel.GeoLine;
import geogebra.kernel.GeoPoint;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.ipo.structurededitor.StructuredEditor;
import ru.ipo.structurededitor.controller.EditorsRegistry;
import ru.ipo.structurededitor.controller.ModificationListener;
import ru.ipo.structurededitor.controller.ModificationVector;
import ru.ipo.structurededitor.model.DSLBean;
import ru.ipo.structurededitor.model.DSLBeansRegistry;
import testSystem.structureBuilder.MyErrorHandler;
import testSystem.structureSerializer.NodesRegistry;
import testSystem.lang.geom.*;
import ru.ipo.structurededitor.view.StatusBar;
import ru.ipo.structurededitor.view.StructuredEditorModel;
import testSystem.view.images.ImageGetter;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: Ilya
 * Date: 02.01.2010
 * Time: 17:05:46
 */
public class TestEditorGeom {

    /*static {
        UIManager.installLookAndFeel("UI for structured editor", ComponentUI.class.getName());
    }*/

    //private StructuredEditorModel model;

    public TestEditorGeom() {
        JFrame f = new JFrame("Модуль учителя");
        //f.setLayout(new GridLayout(2,1));
        BorderLayout br = new BorderLayout();
        f.setLayout(br);
        //TODO think of the appropriate place to this default registrations
        //-------Editors registry preparation
        /*EditorsRegistry editorsRegistry = EditorsRegistry.getInstance();
        editorsRegistry.setDefaultEditor(VoidEditor.class);
        // editorsRegistry.setNextArrayEditor(NextArrayDSLBeanEditor.class);
        editorsRegistry.registerEditor(String.class, StringEditor.class);
        editorsRegistry.registerEditor(int.class, IntEditor.class);
        editorsRegistry.registerEditor(double.class, DoubleEditor.class);
        editorsRegistry.registerEditor(Boolean.class, BooleanEditor.class);
        editorsRegistry.registerEditor(Count.class, EnumEditor.class);     */

        //-------Nodes registry preparation
        NodesRegistry nodesRegistry = nodesRegistryPrep();


        //------------Frame preparation

        GeoStatement st = new GeoStatement();
        final StructuredEditorModel model = createModel(st);

//        f.add(new JScrollPane(new JTextArea("asdf")));
        final StructuredEditor structuredEditor = new StructuredEditor(model);
        JScrollPane structuredEditorScrPane = new JScrollPane(structuredEditor);
        f.add(structuredEditorScrPane, BorderLayout.CENTER);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(640, 480);
        //structuredEditorScrPane.setSize(320,480);
        f.setLocationRelativeTo(null);


        /* //XMLViewer
        XMLViewer xmlV = new XMLViewer("f:\\dsl\\IDEA_DSL\\ru\\ipo\\structurededitor\\xmlViewer\\emptytask.xml");
        f.add(new JScrollPane(xmlV));*/


        // Menu, Toolbar
        createBars(f, structuredEditor, nodesRegistry);
        //Status Bar
        StatusBar statusBar = new StatusBar("Нажмите Ctrl+Пробел для выбора вариантов ввода");
        f.add(statusBar, BorderLayout.SOUTH);


        structuredEditorScrPane.requestFocusInWindow();
        f.setVisible(true);

        //model.setModificationVector(modificationVector);


        /*f.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                ((CompositeElement)model.getRootElement()).add(new TextElement(TestEditor.this.model, "!!!"),0);

                System.out.println("e.getKeyCode() = " + e.getKeyCode());
                System.out.println("e.getKeyChar() = '" + e.getKeyChar() + "' (" + (int)e.getKeyChar() + ")");
                System.out.println("e.getModifiers() = " + e.getModifiers());
                System.out.println("e.getModifiersEx() = " + e.getModifiersEx());

                System.out.println();
            }

            public void keyReleased(KeyEvent e) {
            }
        });*/

        //model.getRootElement().gainFocus(new TextPosition(0,0), false, false);
    }

    public static void createBars(JFrame f, StructuredEditor structuredEditor, NodesRegistry nodesRegistry) {
        MyMenuHandler handler = new MyMenuHandler(f, structuredEditor, nodesRegistry, "geom",null,null);
        MenuBar menuBar = new MenuBar();
        f.setMenuBar(menuBar);
        Menu file = new Menu("Задача");
        MenuItem item1, item2, item3, item4, item5;

        item2 = new MenuItem("Открыть . . .");

        if (structuredEditor.isView()){
            file.add(item2);
            file.add(new MenuItem("-"));
            file.add(item4 = new MenuItem("Проверить . . ."));
            file.add(new MenuItem("-"));
            item4.addActionListener(handler);
        } else{
            file.add(item1 = new MenuItem("Создать"));
            file.add(item2);
            file.add(item3 = new MenuItem("Сохранить . . ."));
            item1.addActionListener(handler);
            item3.addActionListener(handler);
        }
        file.add(new MenuItem("-"));
        file.add(item5 = new MenuItem("Выход"));
        menuBar.add(file);
        Menu edit = new Menu("Редактирование");
        final MenuItem undoItem, redoItem;
        edit.add(undoItem = new MenuItem("Отменить"));
        edit.add(redoItem = new MenuItem("Повторить"));
        undoItem.setEnabled(false);
        redoItem.setEnabled(false);
        menuBar.add(edit);
        Menu help = new Menu("Помощь");
        menuBar.add(help);
        //MyMenuHandler handler = new MyMenuHandler(f,xmlV,structuredEditor);

        item2.addActionListener(handler);
        item5.addActionListener(handler);
        undoItem.addActionListener(handler);
        redoItem.addActionListener(handler);
        //ToolBar
        JToolBar toolBar = new JToolBar();
        addButtonToToolBar(toolBar, "menu-open.png", "Открыть . . .", true, handler);
        if (structuredEditor.isView()){
            addButtonToToolBar(toolBar, "verify.png", "Проверить . . .", true, handler);
        } else {
            addButtonToToolBar(toolBar, "save.png", "Сохранить . . .", true, handler);
        }
        final JButton undoButton = addButtonToToolBar(toolBar, "undo.png", "Отменить", true, handler);
        final JButton redoButton = addButtonToToolBar(toolBar, "redo.png", "Повторить", true, handler);
        //addButtonToToolBar(toolBar, "Примеры задач", "Примеры задач . . .", false, handler);
        addButtonToToolBar(toolBar, "help.png", "Помощь", true, handler);
        final ModificationVector modificationVector = structuredEditor.getModel().getModificationVector();
        modificationVector.addModificationListener(new ModificationListener() {
            public void modificationPerformed() {
                if (modificationVector.canRedo()) {
                    redoButton.setEnabled(true);
                    redoItem.setEnabled(true);
                } else {
                    redoButton.setEnabled(false);
                    redoItem.setEnabled(false);
                }
                if (modificationVector.canUndo()) {
                    undoButton.setEnabled(true);
                    undoItem.setEnabled(true);
                } else {
                    undoButton.setEnabled(false);
                    undoItem.setEnabled(false);
                }
            }
        });
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);

        f.add(toolBar, BorderLayout.NORTH);

    }

    public static NodesRegistry nodesRegistryPrep() {
        Document document;
        // obtain the default parser
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            // set error handler for validation errors
            builder.setErrorHandler(new MyErrorHandler());

            document = builder.newDocument();

            NodesRegistry nodesRegistry = new NodesRegistry();

            Element defaultNode = document.createElement("unknown");
            nodesRegistry.setDefaultNode(defaultNode);

            Element emptyNode = document.createElement("empty");
            nodesRegistry.setEmptyNode(emptyNode);


            Attr taskTitle = document.createAttribute("title");
            nodesRegistry.registerNode(GeoStatement.class, "title", taskTitle);

            Element taskDescription = document.createElement("description");
            nodesRegistry.registerNode(GeoStatement.class, "statement", taskDescription);

            //Predicates
            Element predicates = document.createElement("predicates");
            nodesRegistry.registerNode(GeoStatement.class, "preds", predicates);

            Element parallPredicate = document.createElement("predicate");
            parallPredicate.setAttribute("name", "Parall");
            nodesRegistry.registerNode(ParallPred.class, parallPredicate);

            Element perpendPredicate = document.createElement("predicate");
            perpendPredicate.setAttribute("name", "Perpend");
            nodesRegistry.registerNode(PerpendPred.class, perpendPredicate);

            Element laysOnPredicate = document.createElement("predicate");
            laysOnPredicate.setAttribute("name", "LaysOn");
            nodesRegistry.registerNode(LaysOnPred.class, laysOnPredicate);

            //GeoElements
            Element newLine = document.createElement("geoElem");
            newLine.setAttribute("type", "Line");
            newLine.setAttribute("locType", "new");
            nodesRegistry.registerNode(LineElement.class, newLine);

            Attr newLineName = document.createAttribute("name");
            nodesRegistry.registerNode(LineElement.class, "name", newLineName);

            Element newPoint = document.createElement("geoElem");
            newPoint.setAttribute("type", "Point");
            newPoint.setAttribute("locType", "new");
            nodesRegistry.registerNode(PointElement.class, newPoint);

            Attr newPointName = document.createAttribute("name");
            nodesRegistry.registerNode(PointElement.class, "name", newPointName);

            Element givenLine = document.createElement("geoElem");
            givenLine.setAttribute("type", "Line");
            givenLine.setAttribute("locType", "given");
            nodesRegistry.registerNode(GeoLineLink.class, givenLine);

            Attr givenLineName = document.createAttribute("name");
            nodesRegistry.registerNode(GeoLineLink.class, "name", givenLineName);

            Element givenPoint = document.createElement("geoElem");
            givenPoint.setAttribute("type", "Point");
            givenPoint.setAttribute("locType", "given");
            nodesRegistry.registerNode(GeoPointLink.class, givenPoint);

            Attr givenPointName = document.createAttribute("name");
            nodesRegistry.registerNode(GeoPointLink.class, "name", givenPointName);

            // Instruments  - Enum!
            Element tools = document.createElement("tools");
            nodesRegistry.registerNode(GeoStatement.class, "instrums", tools);
            Element tool = document.createElement("tool");
            nodesRegistry.registerNode(Instrum.class, tool);
            //Attr toolName  = document.createAttribute("name");
            //nodesRegistry.registerNode(Instrum.class, "name", toolName);

            return nodesRegistry;

        } catch (Exception e) {
            throw new Error("Failed to register nodes: ", e);
        }
    }

    private static JButton addButtonToToolBar(JToolBar toolBar, String text, String cmd, Boolean pict, ActionListener handler) {
        JButton but;
        if (pict)
            but = new JButton(new ImageIcon(ImageGetter.class.getResource(text)));
        else
            but = new JButton(text);
        but.setActionCommand(cmd);
        but.addActionListener(handler);
        but.setFocusable(false);
        toolBar.add(but);
        return but;
    }

    public static StructuredEditorModel createModel(DSLBean st) {
        /*CompositeElement root = new CompositeElement(model, CompositeElement.Orientation.Vertical);

        CompositeElement _1stLine = new CompositeElement(model, CompositeElement.Orientation.Horizontal);
        CompositeElement _2ndLine = new CompositeElement(model, CompositeElement.Orientation.Horizontal);
        VisibleElement _3rdLine = new TextElement(model, "The 3rd line");
        ComboBoxTextEditorElement<Integer> _4thLine = new ComboBoxTextEditorElement<Integer>(model);

        _1stLine.add(new TextElement(model, "text 1"));
        _1stLine.add(new TextElement(model, "text 2"));
        _1stLine.add(new TextElement(model, "text 3"));

        _2ndLine.add(new TextElement(model, "null value: "));
        _2ndLine.add(new TextEditorElement(model, "initial text"));
        _2ndLine.add(new TextElement(model));

        _4thLine.addValue("value 1", 1);
        _4thLine.addValue("value 2", 2);
        _4thLine.addValue("value 3", 3);

//        model.setRootElement(root);

        root.add(_1stLine);
        root.add(_2ndLine);
        root.add(_3rdLine);
        root.add(new ContainerElement(model, _4thLine));*/

        //Bean1 bean1 = new Bean1();
        DSLBeansRegistry reg = new DSLBeansRegistry();
        reg.clearRegistry();
        reg.registerBean(BinPred.class);
        reg.registerBean(testSystem.lang.geom.Element.class);
        reg.registerBean(GeoElementLink.class);
        reg.registerBean(GeoLineBinPred.class);
        reg.registerBean(GeoLineLink.class);
        reg.registerBean(GeoPointGeoLineBinPred.class);
        reg.registerBean(GeoPointLink.class);
        reg.registerBean(GeoStatement.class);
        reg.registerBean(LaysOnPred.class);
        reg.registerBean(LineElement.class);
        reg.registerBean(Link.class);
        reg.registerBean(ParallPred.class);
        reg.registerBean(PerpendPred.class);
        reg.registerBean(PointElement.class);
        reg.registerBean(Pred.class);

        StructuredEditorModel model = new StructuredEditorModel(st);
        model.setBeansRegistry(reg);
        EditorsRegistry editorsRegistry = model.getEditorsRegistry();
        editorsRegistry.registerEditor(GeoLine.class, GeoLineEditor.class);
        editorsRegistry.registerEditor(GeoPoint.class, GeoPointEditor.class);
        editorsRegistry.registerEditor(GeoElement.class, GeoElementEditor.class);
        model.setEditorsRegistry(editorsRegistry);
        return model;
    }
}