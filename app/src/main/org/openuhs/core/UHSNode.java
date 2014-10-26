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
 * A container for hierarchical content.
 * <br />A UHSNode has two attributes: a String indicating its
 * type and an optional id. The id is what the root node uses
 * to determine link destinations.
 * <br />Each node has content: STRING, IMAGE, or AUDIO. Non-String
 * content is stored in raw byte[] form.
 * <br />A node may additionally act as a group, containing nested
 * child nodes. In this case, this node's content should be
 * considered a title. The revealed amount attribute tracks the nth
 * visible child.
 * <br />A non-group node may act as a hyperlink to another node.
 * A link points to an id, resolved by the root node upon clicking.
 */
public class UHSNode {
  public static final int STRING = 0;
  public static final int IMAGE = 1;
  public static final int AUDIO = 2;


  private String type = "";
  private Object content = null;
  private int contentType = STRING;
  private int id = -1;
  private int linkIndex = -1;                                //Either Link or group, not both
  private boolean group = false;
  private ArrayList children = null;
  private int revealedAmt = -1;


  public UHSNode(String inType) {
    setType(inType);
  }


  public String getType() {
    return type;
  }

  public void setType(String inType) {
    type = inType;
  }


  public Object getContent() {
    return content;
  }

  /**
   * Sets this node's content.
   *
   * @param inContent raw content (e.g., String or byte[])
   * @param inContentType one of STRING, IMAGE, or AUDIO
   */
  public void setContent(Object inContent, int inContentType) {
    content = inContent;
    contentType = inContentType;
  }

  public int getContentType() {
    return contentType;
  }


  /**
   * Returns this node's id, or -1 if one is not set.
   */
  public int getId() {
    return id;
  }

  public void setId(int input) {
    id = input;
  }


  public boolean isLink() {
    if (linkIndex != -1) return true;
    else return false;
  }

  public int getLinkTarget() {
    return linkIndex;
  }

  public void setLinkTarget(int input) {
    if (input < 0) return;
    this.removeAllChildren();
    linkIndex = input;
  }


  /**
   * Returns true if this node contains nested child nodes.
   */
  public boolean isGroup() {
    return group;
  }

  /**
   * Returns this node's child nodes.
   *
   * @return an ArrayList of UHSNodes
   */
  public ArrayList getChildren() {
    return children;
  }

  public void setChildren(ArrayList newChildren) {
    if (newChildren == null) {
      this.removeAllChildren();
    }
    else {
      children = newChildren;
      linkIndex = -1;
      group = true;
      revealedAmt = 1;
    }
  }


  public void addChild(UHSNode inChild) {
    if (children == null) {
      linkIndex = -1;
      group = true;
      children = new ArrayList();
    }
    if (inChild != null) {
      children.add(inChild);
      if (revealedAmt < 1) revealedAmt = 1;
    }
  }

  public void removeChild(UHSNode inChild) {
    if (children == null || !children.contains(inChild)) return;
    children.remove(inChild);
    revealedAmt--;
    if (revealedAmt <= 0) revealedAmt = -1;
    if (children.size() == 0) removeAllChildren();
  }

  public void removeChild(int input) {
    if (children == null || this.getChildCount()-1 < input) return;
    children.remove(input);
    revealedAmt--;
    if (revealedAmt <= 0) revealedAmt = -1;
    if (children.size() == 0) removeAllChildren();
  }

  public void removeAllChildren() {
    if (children == null) return;
    children.clear();
    children = null;
    group = false;
    revealedAmt = -1;
  }

  public UHSNode getChild(int input) {
    if (children == null || this.getChildCount()-1 < input) return null;
    return (UHSNode)children.get(input);
  }

  public int indexOfChild(UHSNode inChild) {
    return children.indexOf(inChild);
  }

  public int getChildCount() {
    if (children == null) return 0;
    return children.size();
  }


  /**
   * Sets the number of revealed children.
   *
   * @param n a number greater than 1 and less than or equal to the child count
   */
  public void setRevealedAmount(int n) {
    if (this.getChildCount() < n || n < 1) return;
    revealedAmt = n;
  }

  /**
   * Returns the number of revealed children.
   * <br />Or -1 if there are no children.
   */
  public int getRevealedAmount() {
    return revealedAmt;
  }

}
