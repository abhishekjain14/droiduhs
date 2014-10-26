/*
    OpenUHS: Universal Hint System reader.
    Copyright (C) 2012  David Millis

    The original author can be reached at tvtronix@yahoo.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.openuhs.downloader;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;


public class UHSTableCellRenderer extends DefaultTableCellRenderer {
  Color normalUnselColor = null;


  public UHSTableCellRenderer() {
    //Color changes don't reset on their own, so cache the initial value
    normalUnselColor = this.getBackground();
  }


  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    Component c = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);

    boolean custom = false;
    if (!isSelected && table != null && table.getModel() instanceof DownloadableUHSTableModel) {
      DownloadableUHSTableModel model = (DownloadableUHSTableModel)table.getModel();

      DownloadableUHS tmpUHS = model.getUHS(row);
      if (tmpUHS != null && tmpUHS.getColor() != null) {
        c.setBackground(tmpUHS.getColor());
        custom = true;
      }
    }

    if (!isSelected && !custom) {
      c.setBackground(normalUnselColor);
    }

    return c;
  }
}
