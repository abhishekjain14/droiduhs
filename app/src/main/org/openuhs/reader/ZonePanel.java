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
 * A hotspot panel.
 */
public class ZonePanel extends JComponent {
  private boolean showContents = false;
  private JComponent component = null;
  private ZonePanel zoneTarget = null;
  private int linkTarget = -1;

  public ZonePanel() {
    super();
  }

  public ZonePanel(JComponent c) {
    super();
    component = c;
    c.setBounds(0, 0, c.getPreferredSize().width, c.getPreferredSize().height);
  }

  public Dimension getPreferredSize() {
    if (component == null) return super.getPreferredSize();
    else return component.getPreferredSize();
  }
  public void setPreferredSize(Dimension d) {
    if (component == null) super.setPreferredSize(d);
    else component.setPreferredSize(d);
  }
  public Dimension getMinimumSize() {
    if (component == null) return super.getMinimumSize();
    else return component.getMinimumSize();
  }
  public void setMinimumSize(Dimension d) {
    if (component == null) super.setMinimumSize(d);
    else component.setMinimumSize(d);
  }
  public Dimension getMaximumSize() {
    if (component == null) return super.getMaximumSize();
    else return component.getMaximumSize();
  }
  public void setMaximumSize(Dimension d) {
    if (component == null) super.setMaximumSize(d);
    else component.setMaximumSize(d);
  }

  public boolean getContentsVisible() {return showContents;}
  public void setContentsVisible(boolean b) {showContents = b; this.repaint();}

  public ZonePanel getZoneTarget() {return zoneTarget;}
  public void setZoneTarget(ZonePanel z) {zoneTarget = z;}

  public int getLinkTarget() {return linkTarget;}
  public void setLinkTarget(int n) {linkTarget = n;}


  public void paint(Graphics g) {
    super.paint(g);
    if (component != null) {
      if (showContents) {component.paint(g);}
      else {
        paintEdges((Graphics2D)g.create(), Color.GRAY);
      }
    }
    else if (zoneTarget != null) {
      paintEdges((Graphics2D)g.create(), Color.ORANGE);
    }
    else if (linkTarget != -1) {
      paintEdges((Graphics2D)g.create(), Color.GREEN);
    }
    else {
      paintEdges((Graphics2D)g.create(), Color.BLUE);
    }
  }

  private void paintEdges(Graphics2D g2, Color c) {
    g2.setColor(c);
    float dashes[] = {1f,2f};
    g2.setStroke(new BasicStroke(1,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,dashes,0));
    g2.draw(new Rectangle(1, 1, getWidth()-2, getHeight()-2));
  }
}
