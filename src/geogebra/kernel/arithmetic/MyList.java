/* 
 GeoGebra - Dynamic Mathematics for Schools
 Copyright Markus Hohenwarter and GeoGebra Inc.,  http://www.geogebra.org

 This file is part of GeoGebra.

 This program is free software; you can redistribute it and/or modify it 
 under the terms of the GNU General Public License as published by 
 the Free Software Foundation.
 
 */

/*
 * Command.java
 *
 * Created on 05. September 2001, 12:05
 */

package geogebra.kernel.arithmetic;

import geogebra.kernel.Kernel;
import geogebra.main.Application;
import geogebra.main.MyError;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * MyList is used to store a list of ExpressionNode objects read by the parser
 * and to evaluate them. So a MyList object is used when a list is entered (e.g.
 * {2, 3, 7, 9}) and also when a list is used for arithmetic operations.
 * 
 * @see ExpressionNode.evaluate()
 * 
 * @author Markus Hohenwarter
 */
public class MyList extends ValidExpression implements ListValue {

  // Michael Borcherds 2008-04-15
  public static ExpressionValue getCell(MyList list, int row, int col) {
    ExpressionValue singleValue = list.getListElement(col).evaluate();
    if (singleValue.isListValue()) {
      ExpressionValue ret = ((ListValue) singleValue).getMyList()
          .getListElement(row).evaluate();
      // if (ret.isListValue()) Application.debug("isList*********");
      return ret;
    }
    return null;
  }
  /*
   * // Michael Borcherds 2008-04-14 private static MyDouble getCell(MyList
   * list, int col, int row) { ExpressionValue
   * singleValue=((ExpressionValue)list.getListElement(col)).evaluate(); if (
   * singleValue.isListValue() ){ ExpressionValue cell =
   * (((ListValue)singleValue).getMyList().getListElement(row)).evaluate(); if
   * (cell.isNumberValue()) { NumberValue cellValue=(NumberValue)cell; MyDouble
   * cellDouble = (MyDouble)cellValue; return cellDouble; } } return null; }
   */
  protected static boolean isElementOf(ExpressionValue a, MyList myList) {
    // Application.debug(a.getClass()+"");

    for (int i = 0; i < myList.size(); i++) {
      ExpressionValue ev = myList.getListElement(i).evaluate();
      if (ExpressionNode.isEqual(a, ev))
        return true;
    }

    /*
     * if (a.isNumberValue()) { double num = ((NumberValue)a).getDouble();
     * 
     * for (int i = 0 ; i < myList.size() ; i++) { ExpressionValue ev =
     * myList.getListElement(i).evaluate(); if (ev.isNumberValue()) { if
     * (Kernel.isEqual(num, ((NumberValue)ev).getDouble(), Kernel.EPSILON))
     * return true; } }
     * 
     * } else if (a.isTextValue()) { String text = ((TextValue)a).toString();
     * 
     * for (int i = 0 ; i < myList.size() ; i++) { ExpressionValue ev =
     * myList.getListElement(i).evaluate(); if (ev.isTextValue()) { if
     * (text.equals(((TextValue)ev).toString())) return true; } }
     * 
     * } else if (a.isGeoElement()) { GeoElement geo = (GeoElement)a;
     * 
     * for (int i = 0 ; i < myList.size() ; i++) { ExpressionValue ev =
     * myList.getListElement(i).evaluate();
     * //Application.debug(ev.getClass()+""); if (ev.isGeoElement()) { if
     * (geo.isEqual((GeoElement)ev)) return true; } }
     * 
     * 
     * }
     */

    return false;
  }
  protected static boolean listContains(MyList list1, MyList list2) {
    if (list2.size() == 0)
      return true; // the empty set is a subset of all sets
    if (list1.size() == 0)
      return false;

    for (int i = 0; i < list2.size(); i++) {
      ExpressionValue ev2 = list2.getListElement(i).evaluate();
      boolean hasEqualMember = false;
      for (int j = 0; j < list1.size(); j++) {
        ExpressionValue ev1 = list1.getListElement(j).evaluate();

        if (ExpressionNode.isEqual(ev1, ev2)) {
          hasEqualMember = true;
          break;
        }

      }

      if (!hasEqualMember)
        return false;

    }

    return true;
  }

  protected static boolean listContainsStrict(MyList list1, MyList list2) {

    // the empty set has no strict subsets of itself
    if (list1.size() == 0)
      return false;
    if (list2.size() == 0)
      return true;

    for (int i = 0; i < list2.size(); i++) {
      ExpressionValue ev2 = list2.getListElement(i).evaluate();
      boolean hasEqualMember = false;
      for (int j = 0; j < list1.size(); j++) {
        ExpressionValue ev1 = list1.getListElement(j).evaluate();

        if (ExpressionNode.isEqual(ev1, ev2)) {
          hasEqualMember = true;
          break;
        }

      }

      if (!hasEqualMember)
        return false;

    }

    // now must check sets aren't equal
    for (int i = 0; i < list1.size(); i++) {
      ExpressionValue ev1 = list1.getListElement(i).evaluate();
      boolean hasEqualMember = false;
      for (int j = 0; j < list2.size(); j++) {
        ExpressionValue ev2 = list2.getListElement(j).evaluate();
        if (ExpressionNode.isEqual(ev1, ev2)) {
          hasEqualMember = true;
          break;
        }
      }
      // we've found an element without a match
      // so lists are not equal
      if (!hasEqualMember)
        return true;

    }

    // lists are equal
    return false;
  }

  private final Kernel kernel;

  private int matrixRows = -1; // -1 means not calculated, 0 means not a matrix

  private int matrixCols = -1; //

  // list for list elements
  private final ArrayList<ExpressionValue> listElements;

  public MyList(Kernel kernel) {
    this(kernel, 20);
  }

  public MyList(Kernel kernel, int size) {
    this.kernel = kernel;
    listElements = new ArrayList<ExpressionValue>(size);
  }

  public void addListElement(ExpressionValue arg) {
    listElements.add(arg);
    matrixRows = -1; // reset
    matrixCols = -1;
  }

  /**
   * Applies an operation to this list using the given value.
   * 
   * @param operation
   *          : int value like ExpressionNode.MULTIPLY
   * @param value
   *          : value that should be applied to this list using the given
   *          operation
   * @param right
   *          : true for <this> <operation> <value>, false for <value>
   *          <operation> <this>
   * @author Markus Hohenwarter
   */
  private void apply(int operation, ExpressionValue value, boolean right) {
    int size = size();

    // if (!right)
    // Application.debug("apply: " + value + " < op: " + operation + " > " +
    // this);
    // else
    // Application.debug("apply: " + this + " < op: " + operation + " > " +
    // value);

    // matrix ^ integer
    if (right && operation == ExpressionNode.POWER && value.isNumberValue()
        && isMatrix()) {

      double power = ((NumberValue) value).getDouble();
      // Application.debug("matrix ^ "+power);

      if (power < -0.5 || !kernel.isInteger(power)) {
        listElements.clear();
        return;
      }

      power = Math.round(power);

      if (power == 0) {
        listElements.clear();
        return;
      }

      if (power != 1) {

        MyList LHlist, RHlist;

        RHlist = (MyList) deepCopy(kernel);
        while (power > 1.0) {
          LHlist = (MyList) deepCopy(kernel);

          matrixMultiply(LHlist, RHlist);
          power--;
        }
        return; // finished matrix multiplication successfully
      }
      // else power = 1, so drop through to standard list code below

    }

    // expression value is list
    MyList valueList = value.isListValue()
        ? ((ListValue) value).getMyList()
        : null;

    // Michael Borcherds 2008-04-14 BEGIN
    // check for matrix multiplication eg
    // {{1,3,5},{2,4,6}}*{{11,14},{12,15},{13,16}}
    // try{
    if (operation == ExpressionNode.MULTIPLY && valueList != null) {
      MyList LHlist, RHlist;

      if (!right) {
        LHlist = valueList;
        RHlist = (MyList) deepCopy(kernel);
      } else {
        RHlist = valueList;
        LHlist = (MyList) deepCopy(kernel);
      }

      boolean isMatrix = LHlist.isMatrix() && RHlist.isMatrix();

      if (isMatrix) {
        matrixMultiply(LHlist, RHlist);
        return; // finished matrix multiplication successfully
      }
    }
    // }
    // catch (Exception e) { } // not valid matrices
    // Michael Borcherds 2008-04-14 END

    matrixRows = -1; // reset
    matrixCols = -1;

    // return empty list if sizes don't match
    if (size == 0 || valueList != null && size != valueList.size()) {
      listElements.clear();
      return;
    }

    // temp ExpressionNode to do evaluation of single elements
    ExpressionNode tempNode = new ExpressionNode(kernel, listElements.get(0));
    tempNode.setOperation(operation);

    for (int i = 0; i < size; i++)
      try {
        // singleValue to apply to i-th element of this list
        ExpressionValue singleValue = valueList == null ? value : valueList
            .getListElement(i);

        // apply operation using singleValue
        if (right) {
          // this operation value
          tempNode.setLeft(listElements.get(i));
          tempNode.setRight(singleValue);
        } else {
          // value operation this
          tempNode.setLeft(singleValue);
          tempNode.setRight(listElements.get(i));
        }

        // evaluate operation
        ExpressionValue operationResult = tempNode.evaluate();

        // Application.debug("        tempNode : " + tempNode + ", result: " +
        // operationResult);

        // set listElement to operation result
        if (!operationResult.isExpressionNode())
          operationResult = new ExpressionNode(kernel, operationResult);
        listElements.set(i, operationResult);
      } catch (MyError err) {
        // TODO: remove
        Application.debug(err.getLocalizedMessage());

        // return empty list if any of the elements aren't numbers
        listElements.clear();
        return;
      }

    // Application.debug("   gives : " + this);

  }

  /**
   * Applies an operation to this list using the given value: <value>
   * <operation> <this>.
   * 
   * @param operation
   *          : int value like ExpressionNode.MULTIPLY
   * @param value
   *          : value that should be applied to this list using the given
   *          operation
   * @author Markus Hohenwarter
   */
  final protected void applyLeft(int operation, ExpressionValue value) {
    apply(operation, value, false);
  }

  /**
   * Applies an operation to this list using the given value: <this> <operation>
   * <value>.
   * 
   * @param operation
   *          : int value like ExpressionNode.MULTIPLY
   * @param value
   *          : value that should be applied to this list using the given
   *          operation
   * @author Markus Hohenwarter
   */
  final protected void applyRight(int operation, ExpressionValue value) {
    apply(operation, value, true);
  }

  final public boolean contains(ExpressionValue ev) {
    return ev == this;
  }

  public ExpressionValue deepCopy(Kernel kernel) {
    // copy arguments
    int size = listElements.size();
    MyList c = new MyList(kernel, size());

    for (int i = 0; i < size; i++)
      c.addListElement(listElements.get(i).deepCopy(kernel));
    return c;
  }

  public ExpressionValue evaluate() {
    return this;
  }

  public ExpressionValue getListElement(int i) {
    return listElements.get(i);
  }

  /**
   * returns 0 if not a matrix
   * 
   * @author Michael Borcherds
   */
  public int getMatrixCols() {
    // check if already calculated
    if (matrixRows != -1 && matrixCols != -1)
      return matrixCols;

    isMatrix(); // do calculation

    return matrixCols;

  }

  /**
   * returns 0 if not a matrix
   * 
   * @author Michael Borcherds
   */
  public int getMatrixRows() {
    // check if already calculated
    if (matrixRows != -1 && matrixCols != -1)
      return matrixRows;

    isMatrix(); // do calculation

    return matrixRows;

  }

  public MyList getMyList() {
    if (isInTree())
      // used in expression node tree: be careful
      return (MyList) deepCopy(kernel);
    else
      // not used anywhere: reuse this object
      return this;
  }

  public HashSet getVariables() {
    HashSet varSet = new HashSet<Object>();
    int size = listElements.size();
    for (int i = 0; i < size; i++) {
      HashSet<?> s = listElements.get(i).getVariables();
      if (s != null)
        varSet.addAll(s);
    }

    return varSet;
  }

  /*
   * public String toString() { }
   */

  final public boolean isBooleanValue() {
    return false;
  }

  public boolean isConstant() {
    return getVariables().size() == 0;
  }

  final public boolean isExpressionNode() {
    return false;
  }

  public boolean isLeaf() {
    return true;
  }

  public boolean isListValue() {
    return true;
  }

  public boolean isMatrix() {
    return isMatrix(this);
  }

  private boolean isMatrix(MyList LHlist) {
    // check if already calculated
    if (matrixRows > 0 && matrixCols > 0)
      return true;

    boolean isMatrix = true;

    int LHrows = LHlist.size(), LHcols = 0;

    // Application.debug("MULT LISTS"+size);

    // check LHlist is a matrix
    ExpressionValue singleValue = LHlist.getListElement(0).evaluate();
    if (singleValue.isListValue()) {
      LHcols = ((ListValue) singleValue).getMyList().size();
      // Application.debug("LHrows"+LHrows);
      if (LHrows > 1)
        for (int i = 1; i < LHrows; i++) // check all rows same length
        {
          // Application.debug(i);
          singleValue = LHlist.getListElement(i).evaluate();
          // Application.debug("size"+((ListValue)singleValue).getMyList().size());
          if (singleValue.isListValue()) {
            if (((ListValue) singleValue).getMyList().size() != LHcols)
              isMatrix = false;
          } else
            isMatrix = false;
        }
    } else
      isMatrix = false;

    // Application.debug("isMatrix="+isMatrix);

    if (isMatrix) {
      matrixCols = LHcols;
      matrixRows = LHrows;
    } else {
      matrixCols = 0;
      matrixRows = 0;
    }

    return isMatrix;

  }

  public boolean isNumberValue() {
    return false;
  }

  public boolean isPolynomialInstance() {
    return false;

    // return evaluate().isPolynomial();
  }

  public boolean isTextValue() {
    return false;
  }

  public boolean isVector3DValue() {
    return false;
  }

  public boolean isVectorValue() {
    return false;
  }

  final private void matrixMultiply(MyList LHlist, MyList RHlist) {
    int LHcols = LHlist.getMatrixCols(), LHrows = LHlist.getMatrixRows();
    int RHcols = RHlist.getMatrixCols(); // RHlist.getMatrixRows();

    ExpressionNode totalNode;
    ExpressionNode tempNode;
    listElements.clear();
    for (int row = 0; row < LHrows; row++) {
      MyList col1 = new MyList(kernel);
      for (int col = 0; col < RHcols; col++) {
        ExpressionValue totalVal = new ExpressionNode(kernel, new MyDouble(
            kernel, 0.0d));
        for (int i = 0; i < LHcols; i++) {
          ExpressionValue leftV = getCell(LHlist, i, row);
          ExpressionValue rightV = getCell(RHlist, col, i);
          tempNode = new ExpressionNode(kernel, leftV, ExpressionNode.MULTIPLY,
              rightV);

          // multiply two cells...
          ExpressionValue operationResult = tempNode.evaluate();

          totalNode = new ExpressionNode(kernel, totalVal, ExpressionNode.PLUS,
              operationResult);
          // totalNode.setLeft(operationResult);
          // totalNode.setRight(totalVal);
          // totalNode.setOperation(ExpressionNode.PLUS);

          // ...then add the result to a running total
          totalVal = totalNode.evaluate();

        }
        tempNode = new ExpressionNode(kernel, totalVal);
        col1.addListElement(tempNode);
      }
      ExpressionNode col1a = new ExpressionNode(kernel, col1);
      listElements.add(col1a);

    }
    matrixRows = -1; // reset
    matrixCols = -1;

  }

  public void resolveVariables() {
    for (int i = 0; i < listElements.size(); i++) {
      ExpressionValue en = listElements.get(i);
      en.resolveVariables();
    }
  }

  public int size() {
    return listElements.size();
  }

  public String toLaTeXString(boolean symbolic) {
    StringBuffer toLaTeXString = new StringBuffer();
    toLaTeXString.append("\\{");

    // first (n-1) elements
    int lastIndex = listElements.size() - 1;
    if (lastIndex > -1) {
      for (int i = 0; i < lastIndex; i++) {
        ExpressionValue exp = listElements.get(i);
        toLaTeXString.append(exp.toLaTeXString(symbolic));
        toLaTeXString.append(", ");
      }

      // last element
      ExpressionValue exp = listElements.get(lastIndex);
      toLaTeXString.append(exp.toLaTeXString(symbolic));
    }

    toLaTeXString.append("\\}");
    return toLaTeXString.toString();
  }

  // Michael Borcherds 2008-02-04
  // adapted from GeoList
  @Override
  public String toString() {
    StringBuffer sbBuildValueString = new StringBuffer();
    sbBuildValueString.append("{");

    // first (n-1) elements
    int lastIndex = listElements.size() - 1;
    if (lastIndex > -1) {
      for (int i = 0; i < lastIndex; i++) {
        ExpressionValue exp = listElements.get(i);
        sbBuildValueString.append(exp.toString()); // .toOutputValueString());
        sbBuildValueString.append(", ");
      }

      // last element
      ExpressionValue exp = listElements.get(lastIndex);
      sbBuildValueString.append(exp.toString());
    }

    sbBuildValueString.append("}");
    return sbBuildValueString.toString();
  }

  public String toValueString() {
    return toString(); // Michael Borcherds 2008-06-05
    /*
     * int size = listElements.size(); for (int i=0; i < size; i++) {
     * ((ExpressionValue) listElements.get(i)).evaluate(); }
     */
  }

}
