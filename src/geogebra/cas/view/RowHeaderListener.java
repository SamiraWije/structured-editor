package geogebra.cas.view;

import geogebra.main.Application;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JList;

public class RowHeaderListener extends MouseAdapter implements MouseMotionListener {

	private CASTable table;
	private JList rowHeader;
	private int mousePressedRow;
	private boolean dragged;
	
	public RowHeaderListener(CASTable table, JList rowHeader) {
		this.table = table;
		this.rowHeader = rowHeader;					
	}
	
	public void mousePressed(MouseEvent e) {
		e.consume();
		table.stopEditing();
				
		// update selection for mouse pressed
		mousePressedRow = rowHeader.locationToIndex(e.getPoint());			
		if (!rowHeader.isSelectedIndex(mousePressedRow)) {
			rowHeader.setSelectedIndex(mousePressedRow);
		}
		dragged = false;
	}
	
	public void mouseDragged(MouseEvent e) {
		e.consume();
			
		// update selection		
		int mouseDraggedRow = rowHeader.locationToIndex(e.getPoint());	
		
		// make sure mouse pressed is initialized, this may not be the case
		// after closing the popup menu
		if (mousePressedRow < 0) {
			table.stopEditing();
			mousePressedRow = mouseDraggedRow;
		}
		
		rowHeader.setSelectionInterval(mousePressedRow, mouseDraggedRow);
		dragged = true;
	}

	public void mouseReleased(MouseEvent e) {
		e.consume();	
		mousePressedRow = -1;
				
		int mouseReleasedRow = rowHeader.locationToIndex(e.getPoint());		
		boolean rightClick = Application.isRightClick(e);
		
		// update selection if:
		//   mouseReleasedRow is not selected yet
		// or
		//   we did a left click without drag
		if (!rowHeader.isSelectedIndex(mouseReleasedRow) || !rightClick && !dragged) {
			rowHeader.setSelectedIndex(mouseReleasedRow);
		}
			
		// handle right click
		if (rightClick) {
			RowHeaderPopupMenu popupMenu = new RowHeaderPopupMenu(rowHeader, table);	
			popupMenu.show(e.getComponent(), e.getX(), e.getY());				
		} 
	}
	
	public void mouseClicked(MouseEvent e) {

	}
	
	public void mouseMoved(MouseEvent e) {
		e.consume();
	}

//	public void valueChanged(ListSelectionEvent e) {
//		if (e.getValueIsAdjusting()) return;
//		
//		Object src = e.getSource();
//		if (src == rowHeader) {
//			// rowheader slection changed -> update table selection
//			int [] selRows = rowHeader.getSelectedIndices();
//			if (selRows.length > 0)
//				table.setRowSelectionInterval(selRows[0], selRows[selRows.length-1]);
//		}
//		else if (src == table) {
//			// table slection changed -> rowheader table selection			
//			int [] selRows = table.getSelectedRows();
//			if (selRows.length > 0)
//				rowHeader.setSelectedIndices(selRows);
//		}
//	}

}
