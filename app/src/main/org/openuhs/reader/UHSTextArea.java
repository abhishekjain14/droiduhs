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
import javax.swing.text.*;
import java.util.*;

import org.openuhs.core.*;


/**
 * A JTextPane that understands UHS markup.
 */
public class UHSTextArea extends JTextPane {
  public static Color GROUP_COLOR = Color.BLUE;
  public static Color LINK_COLOR = Color.GREEN.darker().darker();
  public static Color HYPER_COLOR = Color.MAGENTA.darker().darker();
  public static final StyleContext DEFAULT_STYLES = UHSTextArea.getDefaultStyleContext();

  private UHSTextArea pronoun = this;

  private UHSNode node = null;
  private StyledDocument doc = null;

  private String[] styles = {"regular","monospaced","hyper"};
  private char[][] prefixes = {{'#','p','+'}, {'#','p','-'}, {'#','h','+'}};
  private char[][] suffixes = {         null,          null, {'#','h','-'}};
  private int prefixLen = 3;


  /**
   * Constructs a text area with the class-default style context.
   */
  public UHSTextArea(UHSNode n) {
    this(n, DEFAULT_STYLES);
  }

  /**
   * Constructs a text area.
   *
   * @param n the UHSNode to display.
   * @param styleContext a collection of font styles to use.
   */
  public UHSTextArea(UHSNode n, StyleContext styleContext) {
    super();
    node = n;

    doc = new DefaultStyledDocument(styleContext);
    pronoun.setStyledDocument(doc);
    updateContent();
  }


  /**
   * Inserts the node's styled text into the document based on markup.
   */
  public void updateContent() {
    try {doc.remove(0, doc.getLength());}
    catch (BadLocationException e) {}
    if (node.getContentType() != UHSNode.STRING) return;

    if (node.isGroup()) styles[0] = "group";
    else if (node.isLink()) styles[0] = "link";
    else styles[0] = "regular";

    char[] tmpContent = ((String)node.getContent()).toCharArray();
    Stack history = new Stack();
    boolean reverted = false;
    int lastPos = 0;
    int lastStyle = 0;
    int newStyle = -1;
    char[] tmpChunk = null;
    String tmpString = null;
    try {
      for (int i=0; i < tmpContent.length; i++) {
        reverted = false;
        try {lastStyle = ((Integer)history.peek()).intValue();}
        catch (EmptyStackException e) {lastStyle = 0;}

        if (i+prefixLen-1 < tmpContent.length) {
          tmpChunk = new char[] {tmpContent[i],tmpContent[i+1],tmpContent[i+2]};
          newStyle = -1;
          for (int j=0; j < prefixes.length; j++) {
            if (Arrays.equals(prefixes[j], tmpChunk)) {newStyle = j; break;}
          }
          if (newStyle == -1) {
            for (int j=0; j < suffixes.length; j++) {
              if (suffixes[j] == null) continue;
              if (Arrays.equals(suffixes[j], tmpChunk)) {
                try {
                  history.remove(new Integer(j));
                  // Don't update lastValue here
                  // Let the old lastValue make it to the next if
                  newStyle = ((Integer)history.peek()).intValue();
                } catch (EmptyStackException e) {newStyle = 0;}
                reverted = true;
                break;
              }
            }
          }
          if (newStyle != -1) {
            if (i > 0) {
              tmpString = new String(tmpContent, lastPos, i-lastPos);
              doc.insertString(doc.getLength(), tmpString, doc.getStyle(styles[lastStyle]));
            }
            lastPos = i+prefixLen;
            if (!reverted) history.push(new Integer(newStyle));
          }
        }
        if (i == tmpContent.length-1 && lastPos <= i) {
          try {lastStyle = ((Integer)history.peek()).intValue();}
          catch (EmptyStackException e) {lastStyle = 0;}

          tmpString = new String(tmpContent, lastPos, tmpContent.length-lastPos);
          doc.insertString(doc.getLength(), tmpString, doc.getStyle(styles[lastStyle]));
        }
      }
    }
    catch (BadLocationException e) {e.printStackTrace();}
    catch (IndexOutOfBoundsException e) {e.printStackTrace();}

    pronoun.validate();
    pronoun.repaint();
  }



  /**
   * Returns a populated StyleContext for UHSTextAreas.
   * Changes to a style will immediately affect existing
   * components using its descendants only. That style
   * will not update for some reason.<br />
   * Java's default<br />
   * - base<br />
   * - - regular<br />
   * - - - group<br />
   * - - - link<br />
   * - - - monospaced<br />
   * - - - hyper
   */
  private static StyleContext getDefaultStyleContext() {
    StyleContext result = new StyleContext();
    Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
      Style baseStyle = result.addStyle("base", defaultStyle);
        Style regularStyle = result.addStyle("regular", baseStyle);
          Style groupStyle = result.addStyle("group", regularStyle);
            StyleConstants.setForeground(groupStyle, GROUP_COLOR);
          Style linkStyle = result.addStyle("link", regularStyle);
            StyleConstants.setForeground(linkStyle, LINK_COLOR);
          Style monospacedStyle = result.addStyle("monospaced", regularStyle);
            StyleConstants.setFontFamily(monospacedStyle, "Monospaced");
          Style hyperStyle = result.addStyle("hyper", regularStyle);
            StyleConstants.setForeground(hyperStyle, HYPER_COLOR);
            StyleConstants.setUnderline(hyperStyle, true);

    return result;
  }
}
