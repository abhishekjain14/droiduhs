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

package org.openuhs.reader;

import java.awt.*;
import javax.swing.*;


/**
 * A slightly modified JPanel.
 * <br />It grows no larger than its enclosing JScrollPane so flowing components within can wrap.
 */
public class JScrollablePanel extends JPanel implements Scrollable {
  public JScrollablePanel() {
    super();
  }

  public JScrollablePanel(LayoutManager layout) {
    super(layout);
  }

  public Dimension getPreferredScrollableViewportSize() {return new Dimension(1,1);}
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {return 40;}
  public boolean getScrollableTracksViewportHeight() {return false;}
  public boolean getScrollableTracksViewportWidth() {return true;}
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {return 20;}
}
