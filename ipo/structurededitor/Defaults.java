package ru.ipo.structurededitor;

import ru.ipo.structurededitor.controller.EditorsRegistry;
import ru.ipo.structurededitor.view.editors.*;

/**
 * Created by IntelliJ IDEA.
 * User: ilya
 * Date: 28.10.2010
 * Time: 1:24:04
 */
public class Defaults {

    public static void registerDefaultEditors() {
        EditorsRegistry<FieldEditor> editorsRegistry = EditorsRegistry.getInstance(FieldEditor.class);

        editorsRegistry.setDefaultEditor(VoidEditor.class);

        editorsRegistry.setEnumEditor(EnumEditor.class);

        editorsRegistry.registerEditor(String.class, StringEditor.class);
        
        editorsRegistry.registerEditor(int.class, IntEditor.class);
        editorsRegistry.registerEditor(Integer.class, IntEditor.class);

        editorsRegistry.registerEditor(double.class, DoubleEditor.class);
        editorsRegistry.registerEditor(Double.class, DoubleEditor.class);

        editorsRegistry.registerEditor(boolean.class, BooleanEditor.class);
        editorsRegistry.registerEditor(Boolean.class, BooleanEditor.class);
    }

}
