package geogebra.cas.view;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;

public class CASTableCellEditor extends CASTableCell
    implements
      TableCellEditor,
      KeyListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private JTable table;

  private CASTableCellValue cellValue;
  private boolean editing = false;
  private int editingRow;

  private final ArrayList<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

  public CASTableCellEditor(CASView view) {
    super(view);
    getInputArea().addKeyListener(this);
  }

  public void addCellEditorListener(CellEditorListener l) {
    if (!listeners.contains(l))
      listeners.add(l);
  }

  public void cancelCellEditing() {
    // update cellValue's input using editor content
    if (editing)
      cellValue.setInput(getInput());

    fireEditingCanceled();
  }

  protected void fireEditingCanceled() {
    if (editing && editingRow < table.getRowCount()) {
      ChangeEvent ce = new ChangeEvent(this);
      for (int i = 0; i < listeners.size(); i++) {
        CellEditorListener l = listeners.get(i);
        l.editingCanceled(ce);
      }
    }

    editing = false;
  }

  protected void fireEditingStopped() {
    if (editing && editingRow < table.getRowCount()) {
      ChangeEvent ce = new ChangeEvent(this);
      for (int i = 0; i < listeners.size(); i++) {
        CellEditorListener l = listeners.get(i);
        l.editingStopped(ce);
      }
    }

    editing = false;
  }

  public Object getCellEditorValue() {
    return cellValue;
  }

  public final int getEditingRow() {
    return editingRow;
  }

  public String getInputSelectedText() {
    return getInputArea().getSelectedText();
  }

  public int getInputSelectionEnd() {
    return getInputArea().getSelectionEnd();
  }

  public int getInputSelectionStart() {
    return getInputArea().getSelectionStart();
  }

  public String getInputText() {
    return getInputArea().getText();
  }

  public Component getTableCellEditorComponent(JTable table, Object value,
      boolean isSelected, int row, int column) {
    if (value instanceof CASTableCellValue) {
      editing = true;
      editingRow = row;
      cellValue = (CASTableCellValue) value;
      this.table = table;

      // fill input and output panel
      setValue(cellValue);

      // update row height
      updateTableRowHeight(table, row);
    }
    return this;
  }

  public void insertText(String text) {
    getInputArea().replaceSelection(text);
    getInputArea().requestFocusInWindow();
  }

  public boolean isCellEditable(EventObject anEvent) {
    return true;
  }

  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();

    switch (keyCode) {
      case KeyEvent.VK_ESCAPE :
        getInputArea().setText("");
        e.consume();
        break;

    }
  }

  public void keyReleased(KeyEvent arg0) {

  }

  public void keyTyped(KeyEvent e) {
    char ch = e.getKeyChar();
    JTextComponent inputArea = getInputArea();
    String text = inputArea.getText();

    // if closing paranthesis is typed and there is no opening parenthesis for
    // it
    // add one in the beginning
    switch (ch) {
      // closing parentheses: insert opening parenthesis automatically
      case ')' :
        if (text.indexOf('(') < 0)
          inputArea.setText('(' + text);
        break;

      // space, equals: get output of previous row (not in parentheses)
      case ' ' :
      case '=' :
        if (editingRow > 0 && text.length() == 0) {
          CASTableCellValue selCellValue = view.getConsoleTable()
              .getCASTableCellValue(editingRow - 1);
          inputArea.setText(selCellValue.getOutput());
        }
        break;

      // get output of previous row (possibly in parentheses)
      case '+' :
      case '-' :
      case '/' :
      case '*' :
      case '^' :
        if (editingRow > 0 && text.length() == 0) {
          CASTableCellValue selCellValue = view.getConsoleTable()
              .getCASTableCellValue(editingRow - 1);
          String prevOutput = selCellValue.getOutput();
          if (prevOutput.indexOf(' ') < 0)
            inputArea.setText(prevOutput + " ");
          else
            inputArea.setText("(" + prevOutput + ") ");
        }
        break;
    }
  }

  public void removeCellEditorListener(CellEditorListener l) {
    listeners.remove(l);
  }

  @Override
  public void setFont(Font ft) {
    super.setFont(ft);
    if (inputPanel != null)
      inputPanel.setFont(ft.deriveFont(Font.BOLD));
  }

  public boolean shouldSelectCell(EventObject anEvent) {
    return true;
  }

  /**
   * update cellValue's input using editor content
   */
  public boolean stopCellEditing() {
    if (editing) {
      cellValue.setInput(getInput());
      fireEditingStopped();
    }

    return true;
  }

}
