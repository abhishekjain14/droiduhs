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
 * A node to hold all others.
 * <br />Additionally a root node is responsible for tracking nodes that are link targets.
 */
public class UHSRootNode extends UHSNode {
  private HashMap linkMap = new HashMap();


  public UHSRootNode() {
    super("Root");
  }


  /**
   * Makes a node available to target by link nodes.
   *
   * @param newLink the node to add
   */
  public void addLink(UHSNode newLink) {
    linkMap.put(newLink.getId()+"", newLink);
  }

  /**
   * Makes a node unavailable to target by link nodes.
   *
   * @param id ID of the node to remove
   */
  public void removeLinkById(int id) {
    if (!linkMap.containsKey(id+"")) return;
    linkMap.remove(id+"");
  }

  /**
   * Makes a node unavailable to target by link nodes.
   *
   * @param doomedLink the node to remove
   */
  public void removeLink(UHSNode doomedLink) {
    if (!linkMap.containsKey(doomedLink.getId()+"")) return;
    linkMap.remove(doomedLink.getId()+"");
  }

  /**
   * Makes all nodes unavailable to target by link nodes.
   */
  public void removeAllLinks() {
    linkMap.clear();
  }

  /**
   * Gets a link's target.
   *
   * @param id ID of the node to get
   * @return the node, or null if not found
   */
  public UHSNode getLink(int id) {
    Object o = linkMap.get(id+"");
    if (o == null) return null;

    UHSNode newNode = (UHSNode)o;
    if (newNode.isGroup()) return newNode;
    else {
      UHSNode tmpNode = new UHSNode("Temp");
      tmpNode.setContent("", UHSNode.STRING);
      tmpNode.addChild(newNode);
      return tmpNode;
    }
  }

  public int getLinkCount() {
    return linkMap.size();
  }

  public void setChildren(ArrayList inChildren) {
    super.setChildren(inChildren);
    if (this.getChildCount() > 0) this.setRevealedAmount(this.getChildCount());
  }


  public void addChild(UHSNode inChild) {
    super.addChild(inChild);
    if (this.getChildCount() > 0) this.setRevealedAmount(this.getChildCount());
  }
}
