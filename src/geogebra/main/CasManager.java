package geogebra.main;

import java.util.LinkedList;

import javax.swing.JComponent;

public interface CasManager {

	public void initCellPairs(LinkedList<Object> cellPairList);
	public String getSessionXML();
	public JComponent getCASViewComponent();
	public Object setInputExpression(Object cellValue, String input);
	public Object setOutputExpression(Object cellValue, String output);
	public Object createCellValue();
	public void updateFonts();
}
