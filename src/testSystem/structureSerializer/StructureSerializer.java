package testSystem.structureSerializer;

import geogebra.kernel.GeoElement;
import org.w3c.dom.*;
import ru.ipo.structurededitor.model.DSLBean;
import testSystem.structureBuilder.MyErrorHandler;
import testSystem.lang.comb.Statement;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;


/**
 * Created by IntelliJ IDEA.
 * User: oleg
 * Date: 28.08.2010
 * Time: 12:24:05
 * To change this template use File | Settings | File Templates.
 */


public class StructureSerializer {
    private String fileName;
    private Document document;
    private NodesRegistry nodesRegistry;

    public StructureSerializer(String fileName, NodesRegistry nodesRegistry) {
        this.nodesRegistry = nodesRegistry;
        this.fileName = fileName;
    }


    private Node createSimpleXml(Document document) {
        Element task = document.createElement("task");
        task.setAttribute("name", "билеты");
        return task;
    }

    public void saveStructure(DSLBean bean) {
        try {
            Document document;

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();

            // set error handler for validation errors
            builder.setErrorHandler(new MyErrorHandler());

            // obtain document object from XML document
            document = builder.newDocument();
            //document.appendChild(createSimpleXml(document));

            Element task = document.createElement("task");

            //task.setAttribute("title",(String) getValue(bean,"title"));

            this.document = document;
            document.appendChild(makeElement(task, bean));
            //document.appendChild(render(bean));

            FileOutputStream oXML = new FileOutputStream(new File(fileName));

            // Use a Transformer for output
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer;
            try {
                transformer = transformerFactory.newTransformer();
            } catch (TransformerConfigurationException e) {
                System.out.println("Transformer configuration error: " + e.getMessage());
                return;
            }
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(oXML);
            // transform source into result will do save
            try {
                transformer.transform(source, result);
            } catch (TransformerException e) {
                System.out.println("Error transform: " + e.getMessage());
            }
            //Закрытие файлового потока
            oXML.close();


        } catch (FileNotFoundException fne) {
            System.err.println("File \'"
                    + fileName + "\' not found. ");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Node makeElement(Element node, Object bean) {
        if (bean.getClass().isEnum()) {
            node.setAttribute("name", bean.toString());
            return node;
        }

        try {
            Object val;
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor d : descriptors) {
                if (!d.getName().equals("class") && !d.getName().equals("layout") && !d.getName().equals("viewLayout")) {
                    Node newNode = nodesRegistry.getNode(bean.getClass(), d.getName());
                    newNode = document.adoptNode(newNode);
                    if (!newNode.isEqualNode(nodesRegistry.getDefaultNode())) {
                        switch (newNode.getNodeType()) {
                            case Node.ATTRIBUTE_NODE:
                                val = getValue(bean, d.getName());
                                if (val instanceof Integer)
                                    val = String.valueOf(val);
                                node.setAttribute(newNode.getNodeName(), (String) val);
                                break;
                            case Node.ELEMENT_NODE:
                                if (newNode.getNodeName().equals("verifier")) {
                                    Element tmpNode = node;
                                    node = document.createElement("mathDescription");
                                    tmpNode.appendChild(node);
                                }
                                Element node1 = (Element) node.getParentNode();
                                if (newNode.getNodeName().equals("description") && (node1 != null)) {
                                    node = node1;
                                }
                                if (newNode.getNodeName().equals("set") && bean instanceof Statement) {
                                    Element tmpNode = node;
                                    node = document.createElement("sourceSet");
                                    tmpNode.appendChild(node);
                                }
                                val = getValue(bean, d.getName());

                                if (val != null) {
                                    if (val instanceof String) {
                                        node.appendChild(newNode);
                                        CDATASection cdata = document.createCDATASection((String) val);
                                        newNode.appendChild(cdata);
                                    } else if (val.getClass().isArray()) {
                                        node.appendChild(newNode);
                                        node1 = (Element) node;
                                        node = (Element) newNode;
                                        for (int i = 0; i < Array.getLength(val); i++) {
                                            Object item = Array.get(val, i);
                                            if (item != null) {
                                                newNode = nodesRegistry.getNode(item.getClass());
                                                newNode = document.adoptNode(newNode);
                                                if (!newNode.isEqualNode(nodesRegistry.getDefaultNode())) {
                                                    if (newNode.getNodeName().equals("set") && bean instanceof Statement) {
                                                        Element tmpNode = node;
                                                        node = document.createElement("sourceSet");
                                                        tmpNode.appendChild(node);
                                                    }
                                                    node.appendChild(makeElement((Element) newNode, item));
                                                    node1 = (Element) node.getParentNode();
                                                    if (newNode.getNodeName().equals("set") && bean instanceof Statement && node1 != null) {
                                                        node = node1;
                                                    }
                                                } else
                                                    node.appendChild(newNode);
                                            } else {
                                                newNode = nodesRegistry.getEmptyNode();
                                                newNode = document.adoptNode(newNode);
                                                node.appendChild(newNode);
                                            }
                                        }
                                        node = node1;
                                        /*   if (item != null)
                                        node.appendChild(makeElement((Element) newNode, (DSLBean) item));
                                    else
                                        node.appendChild(newNode);
                                    }                             */
                                    } else
                                        node.appendChild(makeElement((Element) newNode, val));


                                } else
                                    node.appendChild(newNode);
                                node1 = (Element) node.getParentNode();
                                if (newNode.getNodeName().equals("set") && bean instanceof Statement && node1 != null) {
                                    node = node1;
                                }
                        }
                    } else { //No node is associated with property
                        val = getValue(bean, d.getName());
                        if (val != null) {
                            if (val instanceof GeoElement) {
                                node.setAttribute("name", ((GeoElement) val).getCaption());
                            } else if (val instanceof Integer) {
                                Text txt = document.createTextNode(String.valueOf(val));
                                node.appendChild(txt);
                            } else if (val.getClass().isArray()) {
                                if (newNode.getNodeName().equals("predicate")) {
                                    Element tmpNode = node;
                                    node = document.createElement("predicates");
                                    tmpNode.appendChild(node);
                                }
                                for (int i = 0; i < Array.getLength(val); i++) {
                                    Object item = Array.get(val, i);
                                    if (item != null) {
                                        newNode = nodesRegistry.getNode(item.getClass());
                                        newNode = document.adoptNode(newNode);
                                        if (!newNode.isEqualNode(nodesRegistry.getDefaultNode())) {
                                            if (newNode.getNodeName().equals("set") && bean instanceof Statement) {
                                                Element tmpNode = node;
                                                node = document.createElement("sourceSet");
                                                tmpNode.appendChild(node);
                                            }
                                            node.appendChild(makeElement((Element) newNode, item));
                                            Element node1 = (Element) node.getParentNode();
                                            if (newNode.getNodeName().equals("set") && bean instanceof Statement && node1 != null) {
                                                node = node1;
                                            }
                                        } else
                                            node.appendChild(newNode);
                                    } else {
                                        newNode = nodesRegistry.getEmptyNode();
                                        newNode = document.adoptNode(newNode);
                                        node.appendChild(newNode);
                                    }
                                }
                                Element node1 = (Element) node.getParentNode();
                                if (newNode.getNodeName().equals("predicate") && node1 != null) {
                                    node = node1;
                                }

                            } else {
                                newNode = nodesRegistry.getNode(val.getClass());
                                newNode = document.adoptNode(newNode);
                                if (!newNode.isEqualNode(nodesRegistry.getDefaultNode())) {
                                    if (newNode.getNodeName().equals("set") && bean instanceof Statement) {
                                        Element tmpNode = node;
                                        node = document.createElement("sourceSet");
                                        tmpNode.appendChild(node);
                                    }

                                    node.appendChild(makeElement((Element) newNode, val));
                                    if (newNode.getNodeName().equals("set") && bean instanceof Statement) {
                                        node = (Element) node.getParentNode();
                                    }
                                } else
                                    node.appendChild(newNode);
                            }
                        } else {
                            newNode = nodesRegistry.getEmptyNode();
                            newNode = document.adoptNode(newNode);
                            node.appendChild(newNode);
                        }
                    }

                } // d is property
            } //for (d)

        } catch (
                Exception e1
                )

        {
            throw new Error("Fail in StructureSerializer.makeElement()");
        }

        return node;
    }

    private void setValue(DSLBean bean, String fieldName, Object value) {
        try {
            if (value == null) {

                return;
            }

            PropertyDescriptor pd = new PropertyDescriptor(fieldName, bean.getClass());
            Method wm = pd.getWriteMethod();
            //empty = false;
            wm.invoke(bean, value);
        } catch (Exception e1) {
            throw new Error("Fail in StructureSerializer.setValue()");
        }
    }

    private Object getValue(Object bean, String fieldName) {
        try {
            /*if (EmptyFieldsRegistry.getInstance().isEmpty(bean, fieldName)) {
                return null;
            } */
            PropertyDescriptor pd = new PropertyDescriptor(fieldName, bean.getClass());
            Method rm = pd.getReadMethod();
            //empty = false;
            return rm.invoke(bean);
        } catch (Exception e1) {
            throw new Error("Fail in StructureSerializer.getValue()");
        }
    }

    private void setValue(DSLBean bean, String fieldName, Object value, int index) {
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
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
