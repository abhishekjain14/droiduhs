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

package org.openuhs.core;

import java.util.*;


/**
 * A container for UHSNodes that have clickable regions.
 * <br />Each child node is associated with the bounds of
 * a clickable zone and a point at which the child should
 * appear when the zone is clicked.
 */
public class UHSHotSpotNode extends UHSNode {
  public static int DEFAULT_ZONE_X = 0;
  public static int DEFAULT_ZONE_Y = 0;
  public static int DEFAULT_ZONE_W = 10;
  public static int DEFAULT_ZONE_H = 10;
  public static int DEFAULT_POS_X = -1;
  public static int DEFAULT_POS_Y = -1;

  private Vector coords = new Vector();


  public UHSHotSpotNode(String inType) {
    super(inType);
  }


  /**
   * Gets the zone/position of a child.
   *
   * @param inChild a child node
   * @return an array of zone region dimensions and a position: zx,zy,zw,zh,px,py (-1 for null amounts)
   */
  public int[] getCoords(UHSNode inChild) {
    int index = super.indexOfChild(inChild);
    if (index == -1) return null;
    return (int[])coords.get(index);
  }

  /**
   * Gets the zone/position of a child.
   *
   * @param n index of a child node
   * @return an array of zone region dimensions and a position: zx,zy,zw,zh,px,py (-1 for null amounts)
   */
  public int[] getCoords(int n) {
    if (super.getChildCount()-1 < n) return null;
    return (int[])coords.get(n);
  }

  /**
   * Sets the zone/position of a child.
   *
   * @param inChild a child node
   * @param dimensions an array of zone region dimensions and a position: zx,zy,zw,zh,px,py (-1 for null amounts)
   */
  public void setCoords(UHSNode inChild, int[] dimensions) {
    int index = super.indexOfChild(inChild);
    if (index == -1) return;
    if (dimensions == null || dimensions.length != 6) return;
    coords.set(index, dimensions);
  }

  /**
   * Sets the zone/position of a child.
   *
   * @param n index of a child node
   * @param dimensions an array of zone region dimensions and a position: zx,zy,zw,zh,px,py (-1 for null amounts)
   */
  public void setCoords(int n, int[] dimensions) {
    if (super.getChildCount()-1 < n) return;
    if (dimensions == null || dimensions.length != 6) return;
    coords.set(n, dimensions);
  }


  public Object getContent() {
    return super.getContent();
  }


  /**
   * Overridden to make linking impossible.
   *
   * @param n ID of the node to target
   * @see org.openuhs.core.UHSNode#setLinkTarget(int) UHSNode.setLinkTarget(int)
   */
  public void setLinkTarget(int n) {
    return;
  }


  /**
   * Replace or initialize the current children.
   * <br />This method gives the new nodes default zones/positions.
   *
   * @param inChildren an array of new child UHSNodes
   */
  public void setChildren(ArrayList inChildren) {
    if (inChildren == null || inChildren.size() == 0) {
      this.removeAllChildren();
    }
    else {
      super.setChildren(inChildren);
      coords.removeAllElements();
      for (int i=0; i < inChildren.size(); i++) {
        coords.add(new int[] {DEFAULT_ZONE_X, DEFAULT_ZONE_Y, DEFAULT_ZONE_W, DEFAULT_ZONE_H, DEFAULT_POS_X, DEFAULT_POS_Y});
      }
    }
  }


  public void addChild(UHSNode inChild) {
    super.addChild(inChild);
    coords.add(new int[] {DEFAULT_ZONE_X, DEFAULT_ZONE_Y, DEFAULT_ZONE_W, DEFAULT_ZONE_H, DEFAULT_POS_X, DEFAULT_POS_Y});
  }

  public void removeChild(UHSNode inChild) {
    int index = super.indexOfChild(inChild);
    if (index == -1) return;
    super.removeChild(index);
    coords.remove(index);
  }


  public void removeAllChildren() {
    super.removeAllChildren();
    coords.removeAllElements();
  }
}
