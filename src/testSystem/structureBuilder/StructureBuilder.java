package testSystem.structureBuilder;

import geogebra.euclidian.EuclidianView;
import geogebra.kernel.GeoElement;
import geogebra.main.Application;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import ru.ipo.structurededitor.model.DSLBean;
import testSystem.lang.comb.*;
import testSystem.lang.geom.*;
import testSystem.lang.logic.*;
import ru.ipo.structurededitor.view.editors.ArrayEditor;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: oleg
 * Date: 28.08.2010
 * Time: 12:24:05
 * To change this template use File | Settings | File Templates.
 */
public class StructureBuilder {
    private String fileName;
    private String subSystem;
    private Application app;

    public StructureBuilder(String fileName, String subSystem, Application app) {
        this.fileName = fileName;
        this.subSystem = subSystem;
        this.app = app;
    }

    public DSLBean getStructure() {
        try {
            Document document;
            // obtain the default parser
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            // set error handler for validation errors
            builder.setErrorHandler(new MyErrorHandler());

            // obtain document object from XML document
            document = builder.parse(new File(fileName));
            DSLBean rootBean;
            if (subSystem.equals("comb")) {
                rootBean = new Statement();
            } else if (subSystem.equals("geom")) {
                rootBean = new GeoStatement();
            } else if (subSystem.equals("log")) {
                rootBean = new LogicStatement();
            } else
                rootBean = new Statement();

            processNode(document, rootBean, null);
            return rootBean;
        } catch (SAXParseException spe) {
            System.err.println(
                    "Parse error: " + spe.getMessage());
            System.exit(1);
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (FileNotFoundException fne) {
            System.err.println("File \'"
                    + fileName + "\' not found. ");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setValue(DSLBean bean, String fieldName, Element currentNode, String attrName, String mode) {
        try {
            Object value;
            if (mode.equals("text"))
                value = currentNode.getTextContent();
            else if (mode.equals("innerint"))
                value = Integer.parseInt(currentNode.getTextContent());
            else {
                Attr attr = (Attr) currentNode.getAttributes().getNamedItem(attrName);
                if (attr == null) {
                    return;
                }
                value = attr.getNodeValue();

                if (value == null) {

                    return;
                }
                if (mode.equals("int"))
                    value = Integer.parseInt((String) value);
            }
            PropertyDescriptor pd = new PropertyDescriptor(fieldName, bean.getClass());
            Method wm = pd.getWriteMethod();
            //empty = false;
            wm.invoke(bean, value);
        } catch (Exception e1) {
            throw new Error("Fail in StructureBuilder.setValue()");
        }
    }

    private void immedSetValue(DSLBean bean, String fieldName, Object value) {
        try {

            if (value == null) {

                return;
            }
            PropertyDescriptor pd = new PropertyDescriptor(fieldName, bean.getClass());
            Method wm = pd.getWriteMethod();
            //empty = false;
            wm.invoke(bean, value);
        } catch (Exception e1) {
            throw new Error("Fail in StructureBuilder.setValue()");
        }
    }

    /*private void setValue (DSLBean bean, String fieldName, int index){
        try {
            if (value == null) {

                return;
            }

            PropertyDescriptor pd = new PropertyDescriptor(fieldName, bean.getClass());
            Method wm = pd.getWriteMethod();
            Method rm = pd.getReadMethod();
            Object val = rm.invoke(bean);
            Array.set(val, index, value);
            wm.invoke(bean, val);

        } catch (Exception e1) {
            throw new Error("Fail in indexed StructureBuilder.setValue()");
        }
    } */
    public static GeoElement getGeoByCaption(String caption, Application app ) {
        /*if (geoType.equals("Line") && geoLocType.equals("new")) {
            newBean = new LineElement();
            setValue(newBean, "name", (Element) currentNode, "name", "");
        } else if (geoType.equals("Line") && geoLocType.equals("given")) {*/
        EuclidianView ev = app.getEuclidianView();
        //ev.setSelectionRectangle(new Rectangle(ev.getSize()));
        app.selectAll(0);
        ArrayList geos = app.getSelectedGeos();
        GeoElement geoElement = null;
        for (Object geo : geos) {
            if (geo instanceof GeoElement && ((GeoElement) geo).getCaption().equals(caption)) {
                geoElement = (GeoElement) geo;
                break;
            }
        }
        app.clearSelectedGeos();
        return geoElement;
    }

    private Object processNode(Node currentNode, DSLBean bean, Object arr) {
        switch (currentNode.getNodeType()) {

            // process a Document node
            case Node.DOCUMENT_NODE:
                Document doc = (Document) currentNode;

                System.out.println(
                        "Document node: " + doc.getNodeName() +
                                "\nRoot element: " +
                                doc.getDocumentElement().getNodeName());
                processChildNodes(doc.getChildNodes(), bean, false);
                break;

            // process an Element node*/
            case Node.ELEMENT_NODE:
                String nodeName = currentNode.getNodeName();
                DSLBean newBean;
                boolean processChildren = true;
                if (subSystem.equals("comb")) {
                    if (nodeName.equals("empty")) {
                        if (bean instanceof BinExpr) {
                            arr = "e2";
                        } else if (bean instanceof ArrayExpr) {
                            int index = Array.getLength(arr);
                            arr = ArrayEditor.resizeArray(arr, index + 1);
                        }
                    } else if (nodeName.equals("task")) {
                        setValue(bean, "title", (Element) currentNode, "title", "");
                        processChildNodes(currentNode.getChildNodes(), bean, true);
                    } else if (nodeName.equals("description")) {
                        setValue(bean, "statement", (Element) currentNode, "", "text");
                    } else if (nodeName.equals("constElement")) {
                        if (currentNode.getChildNodes().getLength() > 1) {
                            newBean = new InnerConstantElement();
                            setChildNodesToArray(newBean, "items", currentNode.getChildNodes());
                        } else {
                            newBean = new IntConstantElement();
                            setValue(newBean, "val", (Element) currentNode, "", "innerint");

                        }
                        int index = Array.getLength(arr);
                        arr = ArrayEditor.resizeArray(arr, index + 1);
                        Array.set(arr, index, newBean);
                    } else if (nodeName.equals("set")) {
                        String kitType = currentNode.getAttributes().getNamedItem("type").getNodeValue();
                        if (kitType.equals("EnumerationSet")) {
                            newBean = new EnumKit();
                            setChildNodesToArray(newBean, "items", currentNode.getChildNodes());
                            processChildren = false;
                        } else if (kitType.equals("CombinationSet")) {
                            newBean = new CombKit();
                            setValue(newBean, "k", (Element) currentNode, "length", "int");
                        } else if (kitType.equals("LayoutSet")) {
                            newBean = new LayoutKit();
                            setValue(newBean, "k", (Element) currentNode, "length", "int");
                        } else if (kitType.equals("NumericSet")) {
                            newBean = new IntSegment();
                            setValue(newBean, "from", (Element) currentNode, "first", "int");
                            setValue(newBean, "to", (Element) currentNode, "last", "int");
                            processChildren = false;
                        } else if (kitType.equals("DecartSet")) {
                            newBean = new DescartesPower();
                            setValue(newBean, "pow", (Element) currentNode, "power", "int");
                        } else
                            newBean = null;
                        if (bean instanceof Statement || bean instanceof DescartesPower || bean instanceof CombKit ||
                                bean instanceof LayoutKit) {
                            immedSetValue(bean, "kit", newBean);
                        } else if (bean instanceof IndexExaminer) {
                            immedSetValue(bean, "indexingElem", newBean);
                        }

                        if (processChildren)
                            processChildNodes(currentNode.getChildNodes(), newBean, true);
                    } else if (nodeName.equals("verifier")) {
                        String verifierType = currentNode.getAttributes().getNamedItem("type").getNodeValue();
                        if (verifierType.equals("CountVerifier")) {
                            newBean = new CountExaminer();
                        } else if (verifierType.equals("AnswerVerifier")) {
                            newBean = new AnswerExaminer();
                        } else if (verifierType.equals("IndexVerifier")) {
                            newBean = new IndexExaminer();
                        } else if (verifierType.equals("ListVerifier")) {
                            newBean = new ListExaminer();
                        } else
                            newBean = null;
                        immedSetValue(bean, "examiner", newBean);
                        processChildNodes(currentNode.getChildNodes(), newBean, true);
                    } else if (nodeName.equals("function")) {

                        String functionType = currentNode.getAttributes().getNamedItem("type").getNodeValue();
                        //UnExpr
                        if (functionType.equals("Even")) {
                            newBean = new EvExpr();
                        } else if (functionType.equals("Not")) {
                            newBean = new LogNotExpr();
                        } else if (functionType.equals("Odd")) {
                            newBean = new NotEvExpr();
                        } else if (functionType.equals("ToDigit")) {
                            newBean = new ToNumExpr();
                        }
                        //PrjExpr
                        else if (functionType.equals("Projection")) {
                            newBean = new PrjExpr();
                            setValue(newBean, "ind", (Element) currentNode, "axis", "int");
                        }
                        //BinExpr
                        else if (functionType.equals("Smaller")) {
                            newBean = new SlExpr();
                        } else if (functionType.equals("Greater")) {
                            newBean = new GtExpr();
                        } else if (functionType.equals("Equals")) {
                            newBean = new EqExpr();
                        } else if (functionType.equals("Div")) {
                            newBean = new IntDivExpr();
                        } else if (functionType.equals("Like")) {
                            newBean = new LkExpr();
                        } else if (functionType.equals("Mod")) {
                            newBean = new RemExpr();
                        }
                        //ArrayExpr
                        else if (functionType.equals("Sum")) {
                            newBean = new AddExpr();
                        } else if (functionType.equals("Sub")) {
                            newBean = new DiffExpr();
                        } else if (functionType.equals("And")) {
                            newBean = new LogAndExpr();
                        } else if (functionType.equals("Or")) {
                            newBean = new LogOrExpr();
                        }
                        //CalculableExpr
                        else if (functionType.equals("Parser")) {
                            Node modAttr = currentNode.getAttributes().getNamedItem("mod");
                            if (modAttr == null) {
                                newBean = new CalcExpr();

                            } else {
                                newBean = new ModCalculableExpr();
                                setValue(newBean, "mod", (Element) currentNode, "mod", "int");
                            }
                            setValue(newBean, "ce", (Element) currentNode, "exp", "");
                            processChildren = false;
                        } else
                            newBean = null;
                        if (bean instanceof Examiner || bean instanceof UnExpr || bean instanceof PrjExpr) {
                            immedSetValue(bean, "expr", newBean);

                        } else if (bean instanceof BinExpr) {
                            immedSetValue(bean, (String) arr, newBean);
                            arr = "e2";
                        } else if (bean instanceof ArrayExpr) {
                            int index = Array.getLength(arr);
                            arr = ArrayEditor.resizeArray(arr, index + 1);
                            Array.set(arr, index, newBean);
                        }
                        if (newBean instanceof BinExpr) {
                            processChildren = false;
                            processBinaryChildren(currentNode.getChildNodes(), newBean);
                        } else if (newBean instanceof ArrayExpr) {
                            setChildNodesToArray(newBean, "items", currentNode.getChildNodes());
                            processChildren = false;
                        }
                        if (processChildren)
                            processChildNodes(currentNode.getChildNodes(), newBean, true);
                    } else if (nodeName.equals("current-set-element")) {
                        newBean = new CurElementExpr();
                        if (bean instanceof BinExpr) {
                            immedSetValue(bean, (String) arr, newBean);
                            arr = "e2";
                        } else if (bean instanceof ArrayExpr) {
                            int index = Array.getLength(arr);
                            arr = ArrayEditor.resizeArray(arr, index + 1);
                            Array.set(arr, index, newBean);
                        } else
                            immedSetValue(bean, "expr", newBean);
                    } else
                        processChildNodes(currentNode.getChildNodes(), bean, true);
                } else if (subSystem.equals("geom")) {
                    if (nodeName.equals("empty")) {
                        if (bean instanceof BinPred) {
                            arr = "e2";
                        } else {
                            int index = Array.getLength(arr);
                            arr = ArrayEditor.resizeArray(arr, index + 1);
                        }
                    } else if (nodeName.equals("task")) {
                        setValue(bean, "title", (Element) currentNode, "title", "");
                        processChildNodes(currentNode.getChildNodes(), bean, true);
                    } else if (nodeName.equals("description")) {
                        setValue(bean, "statement", (Element) currentNode, "", "text");
                    } else if (nodeName.equals("predicate")) {
                        String predName = currentNode.getAttributes().getNamedItem("name").getNodeValue();
                        if (predName.equals("Parall")) {
                            newBean = new ParallPred();
                        } else if (predName.equals("Perpend")) {
                            newBean = new PerpendPred();
                        } else if (predName.equals("LaysOn")) {
                            newBean = new LaysOnPred();
                        } else
                            newBean = null;
                        int index = Array.getLength(arr);
                        arr = ArrayEditor.resizeArray(arr, index + 1);
                        Array.set(arr, index, newBean);
                        if (newBean instanceof BinPred) {
                            processChildren = false;
                            processBinaryChildren(currentNode.getChildNodes(), newBean);
                        }
                        if (processChildren)
                            processChildNodes(currentNode.getChildNodes(), newBean, true);
                    } else if (nodeName.equals("geoElem")) {
                        String geoName = currentNode.getAttributes().getNamedItem("name").getNodeValue();
                        String geoType = currentNode.getAttributes().getNamedItem("type").getNodeValue();
                        String geoLocType = currentNode.getAttributes().getNamedItem("locType").getNodeValue();
                        if (geoType.equals("Line") && geoLocType.equals("new")) {
                            newBean = new LineElement();
                            setValue(newBean, "name", (Element) currentNode, "name", "");
                        } else if (geoType.equals("Line") && geoLocType.equals("given")) {
                            newBean = new GeoLineLink();
                            immedSetValue(newBean, "geo", getGeoByCaption(geoName,app));
                        } else if (geoType.equals("Point") && geoLocType.equals("new")) {
                            newBean = new PointElement();
                            setValue(newBean, "name", (Element) currentNode, "name", "");
                        } else if (geoType.equals("Point") && geoLocType.equals("given")) {
                            newBean = new GeoPointLink();
                            immedSetValue(newBean, "geo", getGeoByCaption(geoName,app));
                        } else
                            newBean = null;
                        if (bean instanceof BinPred) {
                            immedSetValue(bean, (String) arr, newBean);
                            arr = "e2";
                        }
                    } else if (nodeName.equals("predicates")) {
                        setChildNodesToArray(bean, "preds", currentNode.getChildNodes());
                    } else if (nodeName.equals("tools")) {
                        setChildNodesToArray(bean, "instrums", currentNode.getChildNodes());
                    } else if (nodeName.equals("tool")) {
                        String value = currentNode.getAttributes().getNamedItem("name").getNodeValue();
                        Instrum instr = Enum.valueOf(Instrum.class, value);
                        int index = Array.getLength(arr);
                        arr = ArrayEditor.resizeArray(arr, index + 1);
                        Array.set(arr, index, instr);
                    } else
                        processChildNodes(currentNode.getChildNodes(), bean, true);
                } else if (subSystem.equals("log")) {
                    if (nodeName.equals("empty")) {
                        if (bean instanceof BinExpr) {
                            arr = "e2";
                        } else if (bean instanceof ArrayExpr) {
                            int index = Array.getLength(arr);
                            arr = ArrayEditor.resizeArray(arr, index + 1);
                        }
                    } else if (nodeName.equals("task")) {
                        setValue(bean, "title", (Element) currentNode, "title", "");
                        processChildNodes(currentNode.getChildNodes(), bean, true);
                    } else if (nodeName.equals("description")) {
                        setValue(bean, "statement", (Element) currentNode, "", "text");

                    } else if (nodeName.equals("condition")) {
                        String verifierType = currentNode.getAttributes().getNamedItem("type").getNodeValue();
                        if (verifierType.equals("True")) {
                            newBean = new TrueLogicCondition();
                            setValue(newBean, "num", (Element) currentNode, "num", "int");
                        } else if (verifierType.equals("False")) {
                            newBean = new FalseLogicCondition();
                            setValue(newBean, "num", (Element) currentNode, "num", "int");
                        } else
                            newBean = null;
                        setChildNodesToArray(newBean, "items", currentNode.getChildNodes());
                        immedSetValue(bean, "condition", newBean);
                    } else if (nodeName.equals("function") || nodeName.equals("atom")) {
                        if (nodeName.equals("atom")) {
                            newBean = new LogicAtom();
                            setValue(newBean, "val", (Element) currentNode, "name", "");
                        } else {
                            String functionType = currentNode.getAttributes().getNamedItem("type").getNodeValue();
                            //UnExpr
                            if (functionType.equals("Not")) {
                                newBean = new LogNotExpr();
                            }
                            //ArrayExpr
                            else if (functionType.equals("And")) {
                                newBean = new LogAndExpr();
                            } else if (functionType.equals("Or")) {
                                newBean = new LogOrExpr();
                            } else newBean = null;
                        }
                        if (bean instanceof UnExpr) {
                            immedSetValue(bean, "expr", newBean);

                        } else if (bean instanceof BinExpr) {
                            immedSetValue(bean, (String) arr, newBean);
                            arr = "e2";
                        } else if (bean instanceof ArrayExpr || bean instanceof LogicCondition) {
                            int index = Array.getLength(arr);
                            arr = ArrayEditor.resizeArray(arr, index + 1);
                            Array.set(arr, index, newBean);
                        }
                        if (newBean instanceof BinExpr) {
                            processChildren = false;
                            processBinaryChildren(currentNode.getChildNodes(), newBean);
                        } else if (newBean instanceof ArrayExpr) {
                            setChildNodesToArray(newBean, "items", currentNode.getChildNodes());
                            processChildren = false;
                        }
                        if (processChildren)
                            processChildNodes(currentNode.getChildNodes(), newBean, true);
                    } else
                        processChildNodes(currentNode.getChildNodes(), bean, true);
                }
                break;

            // process a text node and a CDATA section
            case Node.CDATA_SECTION_NODE:
            case Node.TEXT_NODE:
                Text text = (Text) currentNode;

                if (!text.getNodeValue().trim().equals("")) {
                    System.out.println("\tText: " +
                            text.getNodeValue());
                }
                break;
        }

        return arr;
    }


    private void setChildNodesToArray(DSLBean bean, String fieldName, NodeList children) {
        try {
            PropertyDescriptor pd = new PropertyDescriptor(fieldName, bean.getClass());
            int count = children.getLength();
            Object arr = Array.newInstance(pd.getPropertyType().getComponentType(), 0);
            for (int i = 0; i < count; i++)
                arr = processNode(children.item(i), bean, arr);
            immedSetValue(bean, fieldName, arr);
        } catch (Exception e) {
            throw new Error("Fail in StructureBuilder.setChildNodesToArray()");
        }
    }

    private void processBinaryChildren(NodeList children, DSLBean bean) {
        String arr = "e1";
        int count = children.getLength();
        for (int i = 0; i < count; i++)
            arr = (String) processNode(children.item(i), bean, arr);
    }

    private boolean processChildNodes(NodeList children, DSLBean bean, boolean innerNode) {
        if (children.getLength() != 0) {
            for (int i = 0; i < children.getLength(); i++)
                processNode(children.item(i), bean, null);
            return true;
        } else {

            return false;
        }
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
