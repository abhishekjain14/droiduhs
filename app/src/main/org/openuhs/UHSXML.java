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

package org.openuhs;

import java.util.ArrayList;
import java.io.IOException;
import java.io.OutputStream;

import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.CDATA;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import org.openuhs.core.*;


public class UHSXML {
  private static UHSErrorHandler errorHandler = new DefaultUHSErrorHandler(System.err);


  /**
   * Sets the error handler to notify of exceptions.
   * This is a convenience for logging/muting.
   * The default handler prints to System.err.
   *
   * @param eh the error handler, or null, for quiet parsing
   */
  public static void setErrorHandler(UHSErrorHandler eh) {
    errorHandler = eh;
  }


  /**
   * Exports a node and its children to xml.
   *
   * @param currentNode a node to start exporting from
   * @param basename prefix for referenced binary files
   * @param out a stream to print to (e.g., System.out)
   */
  public static void exportTree(UHSNode currentNode, String basename, OutputStream out) throws IOException {
    Element rootElement = new Element("uhs");
      rootElement.setAttribute("openuhs-xml-version", "1");
    Document doc = new Document(rootElement);

    exportNode(rootElement, currentNode, basename, 1);

    XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
    outputter.output(doc, out);
  }


  /**
   * Recursively exports a node and its children to xml Elements.
   * Extensions are guessed.
   *
   * @param currentNode a node to start extracting from
   * @param basename prefix for referenced binary files
   * @param n a number for uniqueness, incrementing with each file
   * @return a new value for n
   * @see org.openuhs.UHSUtil#getFileExtension(byte[]) getFileExtension(byte[])
   */
  private static int exportNode(Element parentElement, UHSNode currentNode, String basename, int n) {
    Element currentElement = null;
    if (currentNode instanceof UHSHotSpotNode) currentElement = new Element("hotspot-node");
    else currentElement = new Element("node");

    currentElement.setAttribute("type", currentNode.getType());

    int id = currentNode.getId();
    currentElement.setAttribute("id", (id==-1?"":id+""));

    int contentType = currentNode.getContentType();
    String contentTypeString = "";
    String contentString = "";
    if (contentType == UHSNode.STRING) {
      contentTypeString = "string";
      contentString = (String)currentNode.getContent();
    } else {
      if (contentType == UHSNode.IMAGE) contentTypeString = "image";
      else if (contentType == UHSNode.AUDIO) contentTypeString = "audio";
      else contentTypeString = "unknown";
      contentString = basename + n + (id==-1?"":"_"+id) +"."+ UHSUtil.getFileExtension((byte[])currentNode.getContent());
      n++;
    }
    Element contentElement = new Element("content");
      contentElement.setAttribute("type", contentTypeString);
      contentElement.setContent(new CDATA(contentString));
      currentElement.addContent(contentElement);


    if (currentNode instanceof UHSHotSpotNode) {
      ArrayList children = currentNode.getChildren();
      if (children != null) {
        int childCount = children.size();
        for (int i=0; i < childCount; i++) {
          int[] coords = ((UHSHotSpotNode)currentNode).getCoords((UHSNode)children.get(i));
          Element childElement = new Element("hotspot-child");
            childElement.setAttribute("zx", coords[0]+"");
            childElement.setAttribute("zy", coords[1]+"");
            childElement.setAttribute("zw", coords[2]+"");
            childElement.setAttribute("zh", coords[3]+"");
            childElement.setAttribute("px", coords[4]+"");
            childElement.setAttribute("py", coords[5]+"");
            currentElement.addContent(childElement);
            n = exportNode(childElement, (UHSNode)children.get(i), basename, n);
        }
      }
    }
    else {
      int linkId = currentNode.getLinkTarget();
      currentElement.setAttribute("link-id", (linkId==-1?"":linkId+""));

      ArrayList children = currentNode.getChildren();
      if (children != null) {
        int childCount = children.size();
        for (int i=0; i < childCount; i++) {
          Element childElement = new Element("child");
            currentElement.addContent(childElement);
            n = exportNode(childElement, (UHSNode)children.get(i), basename, n);
        }
      }
    }

    parentElement.addContent(currentElement);
    return n;
  }
}
