package testSystem;

import geogebra.kernel.GeoElement;
import geogebra.kernel.Relation;
import geogebra.main.Application;
import ru.ipo.structurededitor.model.DSLBean;
import testSystem.structureBuilder.StructureBuilder;
import testSystem.lang.comb.*;
import testSystem.lang.geom.*;
import testSystem.lang.logic.*;
import ru.ipo.structurededitor.view.editors.ArrayEditor;

import java.util.HashMap;


/**
 * Created by IntelliJ IDEA.
 * User: oleg
 * Date: 12.05.11
 * Time: 12:00
 * To change this template use File | Settings | File Templates.
 */
public class TaskVerifier {
    private DSLBean bean, ans;
    private String subSystem;
    private Application app;
    private HashMap<String, Boolean> varValues;
    private boolean formAnswer = false;
    private Examiner ver;
    private int count;

    public TaskVerifier(DSLBean bean, String subSystem, Application app, DSLBean ans, String combAns) {
        this.subSystem = subSystem;
        this.bean = bean;
        if (subSystem.equals("geom")) {
            this.app = app;
        } else if (subSystem.equals("log")) {
            this.ans = ans;
            prepareVarValues((LogicAnswer) ans);
        } else if (subSystem.equals("comb")) {
            this.combAns = combAns;
        }
    }

    private String combAns;

    public void makeLogAnswer() {
        formAnswer = true;
        ((LogicAnswer) ans).setAnswer(new LogicAtomValue[0]);
        logVerify();
        formAnswer = false;

    }

    private void prepareVarValues(LogicAnswer param) {
        varValues = new HashMap<String, Boolean>();
        LogicAtomValue[] answer = param.getAnswer();
        if (answer != null && answer.length > 0)
            for (LogicAtomValue item : answer) {
                if (item != null)
                    varValues.put(item.getName(), item.getVal());
            }
    }

    private boolean geomVerify() {
        Pred[] preds = ((GeoStatement) bean).getPreds();
        for (Pred pred : preds) {
            if (pred instanceof GeoPointGeoLineBinPred) {
                GeoElement geo1, geo2;
                AbstractGeoPoint point = ((GeoPointGeoLineBinPred) pred).getE1();
                AbstractGeoLine line = ((GeoPointGeoLineBinPred) pred).getE2();
                if (point instanceof GeoPointLink)
                    geo1 = ((GeoPointLink) point).getGeo();
                else
                    geo1 = StructureBuilder.getGeoByCaption(((PointElement) point).getName(), app);
                if (line instanceof GeoLineLink)
                    geo2 = ((GeoLineLink) line).getGeo();
                else
                    geo2 = StructureBuilder.getGeoByCaption(((LineElement) line).getName(), app);
                Relation rel = new Relation(app.getKernel());
                if (geo1 == null || geo2 == null)
                    return false;

                String relStr = rel.relation(geo1, geo2);
                System.out.println(relStr);
                if (pred instanceof LaysOnPred && relStr.contains("не лежит на"))
                    return false;
            } else if (pred instanceof GeoLineBinPred) {
                GeoElement geo1, geo2;
                AbstractGeoLine line1 = ((GeoLineBinPred) pred).getE1();
                AbstractGeoLine line2 = ((GeoLineBinPred) pred).getE2();
                if (line1 instanceof GeoLineLink)
                    geo1 = ((GeoLineLink) line1).getGeo();
                else
                    geo1 = StructureBuilder.getGeoByCaption(((LineElement) line1).getName(), app);
                if (line2 instanceof GeoLineLink)
                    geo2 = ((GeoLineLink) line2).getGeo();
                else
                    geo2 = StructureBuilder.getGeoByCaption(((LineElement) line2).getName(), app);
                if (geo1 == null || geo2 == null)
                    return false;
                Relation rel = new Relation(app.getKernel());
                String relStr = rel.relation(geo1, geo2);
                System.out.println(relStr);
                if (pred instanceof ParallPred && !relStr.contains("параллельны") ||
                        pred instanceof PerpendPred && !relStr.contains("перпендикулярны"))
                    return false;
            }
        }
        return true;
    }

    private boolean processKit(Kit kit){
       if (kit instanceof IntSegment){
           int from = ((IntSegment) kit).getFrom();
           int to = ((IntSegment) kit).getTo();
           for (int i = from; i<=to;i++){
               if (ver instanceof CountExaminer){
                   /*if ((boolean) combParse(((CountExaminer) ver).getExpr()))
                       count++;*/
               }
           }
       }
       return false;
    }
    private boolean combVerify() {
        ver = ((Statement) bean).getExaminer();
        Kit kit = ((Statement) bean).getKit();
        return processKit(kit);
    }

    public boolean verify() {
        if (subSystem.equals("geom"))
            return geomVerify();
        else if (subSystem.equals("log"))
            return logVerify();
        else if (subSystem.equals("comb"))
            return combVerify();
        return false;
    }

    private boolean parse(Expr expr) {
        if (expr instanceof ArrayExpr) {
            Expr[] exprs = ((ArrayExpr) expr).getItems();
            boolean res = true, flag = true;
            for (Expr expr1 : exprs) {
                if (flag) {
                    res = parse(expr1);
                    flag = false;
                } else if (expr instanceof LogAndExpr) {
                    res = parse(expr1) && res ;
                } else if (expr instanceof LogOrExpr) {
                    res = parse(expr1) || res;
                }
            }
            return res;
        } else if (expr instanceof LogNotExpr) {
            return !parse(((LogNotExpr)expr).getExpr());
        } else if (expr instanceof LogicAtom) {
            if (formAnswer) {
               Object arr = ((LogicAnswer) ans).getAnswer();
               String name = ((LogicAtom) expr).getVal();
               boolean flag = true;
               for (LogicAtomValue atomValue: (LogicAtomValue[]) arr){
                  if (atomValue.getName().equals(name)){
                      flag=false;
                      break;
                  }
               }
               if (flag){
                    int size = java.lang.reflect.Array.getLength(arr);
                    arr= ArrayEditor.resizeArray(arr,size+1);
                    LogicAtomValue item = new LogicAtomValue();
                    item.setName(name);
                    item.setVal(false);
                    java.lang.reflect.Array.set(arr,size,item);
                    ((LogicAnswer) ans).setAnswer((LogicAtomValue[])arr);
               }
            } else {
                Boolean val = varValues.get(((LogicAtom) expr).getVal());
                if (val != null)
                    return val;
                else
                    return false;
            }
        }
        return false;
    }

    private boolean logVerify() {
        LogicCondition lc = ((LogicStatement) bean).getCondition();
        if (lc==null)
            return false;
        Expr[] exprs = lc.getItems();
        int num = 0;
        boolean example;
        example = lc instanceof TrueLogicCondition;
        for (Expr expr : exprs) {
            if (parse(expr) == example)
                num++;
        }
        return lc.getNum() == num;
    }


}
