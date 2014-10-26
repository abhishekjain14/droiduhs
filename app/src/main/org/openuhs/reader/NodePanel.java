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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import org.openuhs.core.*;


/**
 * A JPanel that displays a node and its children.
 */
public class NodePanel extends JScrollablePanel {
  private NodePanel pronoun = this;
  private UHSNode node = null;
  private UHSReaderNavCtrl navCtrl = null;


  /**
   * @param n the UHSNode to be used
   * @param c callback used to replace this panel when a child is clicked
   * @param showAll true if all child hints should be revealed, false otherwise
   */
  public NodePanel(UHSNode n, UHSReaderNavCtrl c, boolean showAll) {
    node = n;
    navCtrl = c;

    GridBagLayout layoutGridbag = new GridBagLayout();
    GridBagConstraints layoutC = new GridBagConstraints();
      layoutC.fill = GridBagConstraints.HORIZONTAL;
      layoutC.insets = new Insets(1, 2, 1, 2);
      layoutC.weightx = 1.0;
      layoutC.weighty = 0;
      layoutC.gridy = 0;
      layoutC.gridwidth = GridBagConstraints.REMAINDER;  //End Row
    pronoun.setLayout(layoutGridbag);

    MouseListener clickListener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (pronoun.getParent() != null) {
          JComponent thisComponent = (JComponent)e.getSource();
          JComponent parent = (JComponent)pronoun.getParent();
          for (int i=0; i < ((JComponent)thisComponent.getParent()).getComponentCount(); i++) {
            if ( ((JComponent)thisComponent.getParent()).getComponent(i).equals(thisComponent) ) {
              if (node.getChild(i).isGroup())
                navCtrl.setReaderNode(node.getChild(i));
              else if (node.getChild(i).isLink()) {
                int targetIndex = node.getChild(i).getLinkTarget();
                navCtrl.setReaderNode(targetIndex);
              }
              break;
            }
          }
        }
      }
    };

    MouseInputListener hotspotListener = new MouseInputAdapter() {
      Cursor zoneCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
      Cursor normCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

      private int getZone(UHSHotSpotNode nick, int x, int y) {
        for (int i=1; i < nick.getChildCount(); i++) {
          int[] coords = nick.getCoords(nick.getChild(i));
          if (x > coords[0] && y > coords[1] && x < coords[0]+coords[2] && y < coords[1]+coords[3]) {
            return i;
          }
        }
        return -1;
      }
      public void mouseMoved(MouseEvent e) {
        if (pronoun.getParent() != null) {
          JComponent thisComponent = (JComponent)e.getSource();
          UHSHotSpotNode nick = (UHSHotSpotNode)node;

          int x = e.getX(); int y = e.getY();

          if (getZone(nick, x, y) != -1) thisComponent.setCursor(zoneCursor);
          else thisComponent.setCursor(normCursor);
        }
      }
    };

    MouseListener zoneListener = new MouseInputAdapter() {
      Cursor zoneCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
      Cursor normCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

      public void mouseEntered(MouseEvent e) {
        ZonePanel thisComponent = (ZonePanel)e.getSource();
        thisComponent.setCursor(zoneCursor);
      }
      public void mouseExited(MouseEvent e) {
        ZonePanel thisComponent = (ZonePanel)e.getSource();
        thisComponent.setCursor(normCursor);
      }
      public void mouseClicked(MouseEvent e) {
        ZonePanel thisComponent = (ZonePanel)e.getSource();
        ZonePanel zoneTarget = thisComponent.getZoneTarget();
        int targetIndex = thisComponent.getLinkTarget();
        if (zoneTarget != null)
          zoneTarget.setContentsVisible(!zoneTarget.getContentsVisible());
        else if (targetIndex != -1) {
          navCtrl.setReaderNode(targetIndex);
        }
      }
    };

    boolean allgroup = true;
    if (node instanceof UHSHotSpotNode) {
      UHSHotSpotNode nick = (UHSHotSpotNode)node;
      JLayeredPane sharedPanel = new JLayeredPane();

      for (int i=0; i < node.getChildCount(); i++) {
        int childContentType = node.getChild(i).getContentType();
        if (childContentType == UHSNode.STRING) {
          int[] coords = nick.getCoords(nick.getChild(i));
          ZonePanel spotPanel = new ZonePanel();
            //spotPanel.setToolTipText( (String)node.getChild(i).getContent() );
            spotPanel.setBounds(coords[0], coords[1], coords[2], coords[3]);
            sharedPanel.add(spotPanel, JLayeredPane.PALETTE_LAYER, 0);
            spotPanel.addMouseListener(zoneListener);
          if (node.getChild(i).isLink()) {
            spotPanel.setLinkTarget(node.getChild(i).getLinkTarget());
          }
        }
        else if (childContentType == UHSNode.IMAGE) {
          int[] coords = nick.getCoords(nick.getChild(i));

          JLabel imageLbl = new JLabel(new ImageIcon((byte[])node.getChild(i).getContent()));

          ZonePanel contentPanel = new ZonePanel(imageLbl);
            Dimension pSize = contentPanel.getPreferredSize();
            contentPanel.setBounds(coords[4], coords[5], pSize.width, pSize.height);
            sharedPanel.add(contentPanel, JLayeredPane.DEFAULT_LAYER, 0);
          if (i == 0) {
            contentPanel.setContentsVisible(true);
            sharedPanel.setPreferredSize(pSize);
            sharedPanel.setMinimumSize(pSize);
          } else {
            ZonePanel spotPanel = new ZonePanel();
              spotPanel.setZoneTarget(contentPanel);
              spotPanel.setBounds(coords[0], coords[1], coords[2], coords[3]);
              sharedPanel.add(spotPanel, JLayeredPane.PALETTE_LAYER, 0);
              spotPanel.addMouseListener(zoneListener);
          }
        }
      }
      pronoun.add(sharedPanel, layoutC);
      layoutC.gridy++;
    }
    else {
      for (int i=0; i < node.getChildCount(); i++) {
        UHSNode tmpNode = node.getChild(i);
        int contentType = node.getChild(i).getContentType();

        if (contentType == UHSNode.STRING) {
          UHSTextArea tmpUHSArea = new UHSTextArea(tmpNode);
            tmpUHSArea.setEditable(false);
            tmpUHSArea.setBorder(BorderFactory.createEtchedBorder());
            tmpUHSArea.setVisible(i==0 || showAll);
            pronoun.add(tmpUHSArea, layoutC);
          layoutC.gridy++;
          if (tmpNode.isGroup() || tmpNode.isLink()) {
            tmpUHSArea.addMouseListener(clickListener);
          }
          else if (tmpNode.getType().equals("Blank") == false) {
            allgroup = false;
          }
        }
        else {
          JComponent tmpComp = null;
          if (contentType == UHSNode.IMAGE)
            tmpComp = new JLabel(new ImageIcon((byte[])tmpNode.getContent()));
          else if (contentType == UHSNode.AUDIO)
            tmpComp = new MinimalSoundPlayer((byte[])tmpNode.getContent());
          else
            tmpComp = new JLabel("^UNKNOWN CONTENT^");
          JPanel tmpPanel = new JPanel();
            tmpPanel.add(tmpComp);
            tmpPanel.setVisible(i==0 || showAll);
            pronoun.add(tmpPanel, layoutC);
          layoutC.gridy++;
        }
      }
    }
    if (allgroup || showAll) {
      for (int i=0; i < pronoun.getComponentCount(); i++) {
        ((JComponent)pronoun.getComponent(i)).setVisible(true);
      }
      node.setRevealedAmount(node.getChildCount());
    } else {
      for (int i=0; (i < pronoun.getComponentCount()) && (i < node.getRevealedAmount()); i++) {
        ((JComponent)pronoun.getComponent(i)).setVisible(true);
      }
    }
    layoutC.weighty = 1;
    //pronoun.add(new JLabel(""), layoutC);

    pronoun.revalidate();
    pronoun.repaint();
  }


  /**
   * Gets the node this panel represents.
   *
   * @return the underlying UHSNode
   */
  public UHSNode getNode() {
    return node;
  }


  /**
   * Reveals the next child hint.
   *
   * @return the child's index or -1 if no more to see
   */
  public int showNext() {
    for (int i=0; i < pronoun.getComponentCount(); i++) {
      if ( ((JComponent)pronoun.getComponent(i)).isVisible() == false ) {
        ((JComponent)pronoun.getComponent(i)).setVisible(true);
        node.setRevealedAmount(node.getRevealedAmount()+1);
        pronoun.revalidate();
        pronoun.repaint();
        return i;
      }
    }
    return -1;
  }


  /**
   * Determines whether the child hints have all been revealed.
   *
   * @return true if all children are revealed, false otherwise
   */
  public boolean isComplete() {
    if (node.getChildCount() == node.getRevealedAmount()) return true;
    else return false;
  }
}
