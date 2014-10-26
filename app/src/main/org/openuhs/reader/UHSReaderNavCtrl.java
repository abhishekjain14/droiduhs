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

import org.openuhs.core.*;


/**
 * An interface NodePanels use on ancestors to replace themselves in a larger UHS reader GUI.
 */
public interface UHSReaderNavCtrl {

  /**
   * Displays a new node within the current tree.
   *
   * @param newNode the new node
   */
  public void setReaderNode(UHSNode newNode);

  /**
   * Displays a new node within the current tree.
   *
   * @param id ID of the new node
   */
  public void setReaderNode(int id);


  /**
   * Sets the reader's title to the specified string.
   *
   * @param s the title to be displayed in the reader. A null value is treated as an empty string, "".
   */
  public void setReaderTitle(String s);

  /**
   * Gets the title of the reader.
   *
   * @return the title of the reader
   */
  public String getReaderTitle();
}
