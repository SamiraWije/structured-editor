package ru.ipo.structurededitor.view;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Vector;

import ru.ipo.structurededitor.StructuredEditor;
import ru.ipo.structurededitor.model.DSLBean;
import ru.ipo.structurededitor.view.elements.ComboBoxTextEditorElement;
import ru.ipo.structurededitor.view.elements.VisibleElement;
import ru.ipo.structurededitor.view.events.*;

import javax.swing.event.EventListenerList;

/**
 * Корень дерева ячеек
 */
public class StructuredEditorModel {

    private VisibleElement rootElement;
    //private StructuredEditor editor;
    private VisibleElement focusedElement;
    private EventListenerList listenerList = new EventListenerList();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Добавление слушателя для события "изменение любого свойства класса"
     *
     * @param listener Слушатель
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Добавление слушателя для события "изменение конкретного свойства класса"
     *
     * @param propertyName Имя свойства
     * @param listener     Слушатель
     */
    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /*public StructuredEditor getEditor() {
        return editor;
    } */

    public void addPopupListener(PopupListener l) {
        listenerList.add(PopupListener.class, l);
    }

    public void removePopupListener(PopupListener l) {
        listenerList.remove(PopupListener.class, l);
    }

    public ListDialog showPopup(Vector<String> filteredPopupList, String longStr, int x, int y) {
        return firePopupShow(new PopupEvent(this, filteredPopupList, longStr, x, y));

    }



    protected ListDialog firePopupShow(PopupEvent pe) {
        Object[] listeners = listenerList.getListeners(PopupListener.class);
        /*for (int i = 0; i < listeners.length; i++) {
            ((PopupListener) listeners[i]).showPopup(pe);
        } */
         return ((PopupListener) listeners[0]).showPopup(pe);
    }


    public VisibleElement getFocusedElement() {
        return focusedElement;
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return pcs.getPropertyChangeListeners(propertyName);
    }

    /**
     * Корень отображаемого элемента соответствующего корню дерева ячеек
     *
     * @return
     */
    public VisibleElement getRootElement() {
        return rootElement;
    }

    /*public StructuredEditorUI getUI() {
        if (editor != null)
            return editor.getUI();
        else
            return null;
    } */

    // PropertyChangeSupport

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName,
                                             PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    /* public void setEditor(StructuredEditor editor) {
        this.editor = editor;
    }*/

    /**
     * Установить активный элемент и вызвать всех подписанных слушателей
     *
     * @param focusedElement element to set focus to
     */
    public void setFocusedElement(VisibleElement focusedElement) {
        if (focusedElement == this.focusedElement)
            return;

        // Фокус может иметь только элемент без дочерних элементов
        // Спускаемся к самому вложенному элементу
        if (focusedElement != null) {
            while (focusedElement.getChildrenCount() != 0)
                focusedElement = focusedElement.getChild(0);
        }

        VisibleElement oldValue = this.focusedElement;
        this.focusedElement = focusedElement;
        pcs.firePropertyChange("focusedElement", oldValue, focusedElement);

        if (oldValue != null)
            oldValue.fireFocusChanged(true);
        if (focusedElement != null)
            focusedElement.fireFocusChanged(false);

    }

    public void setRootElement(VisibleElement rootElement) {
        this.rootElement = rootElement;
        setFocusedElement(rootElement);
    }
}