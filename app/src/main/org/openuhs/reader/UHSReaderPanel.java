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
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.util.*;
import java.io.*;

import org.openuhs.core.*;
import org.openuhs.*;


/**
 * A frame to navigate UHS nodes.
 * <br />
 * <pre> Typical usage:
 * import org.openuhs.core.*;
 * import org.openuhs.reader.*;
 *
 * UHSReaderPanel readerPanel = new UHSReaderPanel();
 * OpenUHSLib UHSLib = new OpenUHSLib();
 * UHSRootNode rootNode = UHSLib.parseFile("./hints/somefile.uhs");
 * if (rootNode != null) {
 *   readerPanel.setUHSNodes(rootNode, rootNode);
 * }</pre>
 */
public class UHSReaderPanel extends JPanel implements UHSReaderNavCtrl {
  public static int SCROLL_TO_TOP = 0;
  public static int SCROLL_TO_BOTTOM = 1;
  public static int SCROLL_IF_INCOMPLETE = 2;

  private UHSErrorHandler errorHandler = null;

  private UHSReaderPanel pronoun = this;
  private String readerTitle = "";

  private UHSRootNode rootNode = null;
  private UHSNode currentNode = null;

  private NodePanel currentNodePanel = null;
  private JScrollPane scrollPane = null;
  private JScrollablePanel scrollView = new JScrollablePanel(new BorderLayout());

  private ArrayList historyArray = new ArrayList();
  private ArrayList futureArray = new ArrayList();
  private JButton openBtn = null;
  private JButton backBtn = null;
  private JButton forwardBtn = null;
  private JButton findBtn = null;

  private JLabel questionLabel = null;
  private JLabel showLabel = null;
  private JButton showNextBtn = null;
  private JCheckBox showAllBox = null;

  private String hintsPath = ".";


  public UHSReaderPanel() {
    super(new BorderLayout());

    setErrorHandler(new DefaultUHSErrorHandler(System.err));

    JPanel ctrlPanel = new JPanel(new BorderLayout());
      JPanel ctrlLeftPanel = new JPanel();
        ctrlLeftPanel.setLayout(new BoxLayout(ctrlLeftPanel, BoxLayout.X_AXIS));
        openBtn = new JButton("Open...");
          ctrlLeftPanel.add(openBtn);
      ctrlPanel.add(ctrlLeftPanel, BorderLayout.WEST);

      JPanel ctrlCenterPanel = new JPanel();
        ctrlCenterPanel.setLayout(new BoxLayout(ctrlCenterPanel, BoxLayout.Y_AXIS));
        JPanel ctrlCenterHolderPanel = new JPanel();
          ctrlCenterHolderPanel.setLayout(new BoxLayout(ctrlCenterHolderPanel, BoxLayout.X_AXIS));
          backBtn = new JButton("<");
            backBtn.setEnabled(false);
            ctrlCenterHolderPanel.add(backBtn);
          forwardBtn = new JButton(">");
            forwardBtn.setEnabled(false);
            ctrlCenterHolderPanel.add(forwardBtn);
          ctrlCenterPanel.add(ctrlCenterHolderPanel);
        ctrlPanel.add(ctrlCenterPanel, BorderLayout.CENTER);

      JPanel ctrlRightPanel = new JPanel();
        ctrlRightPanel.setLayout(new BoxLayout(ctrlRightPanel, BoxLayout.X_AXIS));
        findBtn = new JButton("Find...");
          ctrlRightPanel.add(findBtn);
        ctrlPanel.add(ctrlRightPanel, BorderLayout.EAST);

      JPanel ctrlBottomPanel = new JPanel();
        ctrlBottomPanel.setLayout(new BoxLayout(ctrlBottomPanel, BoxLayout.X_AXIS));
        questionLabel = new JLabel("");
          ctrlBottomPanel.add(questionLabel);
        ctrlPanel.add(ctrlBottomPanel, BorderLayout.SOUTH);


    openBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String fileName = "";
        String homePath = hintsPath;
        File fileHomePath = new File(homePath);
        String homePathAb = fileHomePath.getAbsolutePath().substring(0,fileHomePath.getAbsolutePath().length()-1);

        JFileChooser chooser = new JFileChooser(homePath);
        int status = chooser.showOpenDialog(pronoun);
        if (status == 0) {
          File filegrab = chooser.getSelectedFile();
          if (filegrab.getAbsolutePath().startsWith(homePathAb) == true && filegrab.getAbsolutePath().length() > homePathAb.length())
            fileName = homePath + filegrab.getAbsolutePath().substring(homePathAb.length(), filegrab.getAbsolutePath().length()).replace('\\','/');
          else
            fileName = filegrab.getAbsolutePath().replace('\\','/');
        }
        else return;

        final String finalName = fileName;
        openFile(finalName);
      }
    });

    backBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setReaderNode((UHSNode)historyArray.get(historyArray.size()-1));
      }
    });

    forwardBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setReaderNode((UHSNode)futureArray.get(futureArray.size()-1));
      }
    });

    findBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String tmpString = JOptionPane.showInputDialog(pronoun, "Find what?", "Find text", JOptionPane.QUESTION_MESSAGE);
        if (tmpString == null || tmpString.length() == 0) return;

        UHSNode newNode = new UHSNode("result");
        newNode.setContent("Search Results for \""+ tmpString +"\"", UHSNode.STRING);
        searchNode(newNode, "", 0, (UHSNode)rootNode, tmpString.toLowerCase());
        setReaderNode(newNode);
      }
    });


    JPanel centerPanel = new JPanel();
      centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
      scrollPane = new JScrollPane(scrollView, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //The default scroll speed is too slow
        scrollPane.getHorizontalScrollBar().setUnitIncrement(25);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);
        centerPanel.add(scrollPane);


    JPanel showPanel = new JPanel(new GridLayout(0,3));
      showLabel = new JLabel("");
        showLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        showPanel.add(showLabel);

      JPanel showCenterPanel = new JPanel();
        showCenterPanel.setLayout(new BoxLayout(showCenterPanel, BoxLayout.Y_AXIS));
        JPanel showCenterHolderPanel = new JPanel();
          showCenterHolderPanel.setLayout(new BoxLayout(showCenterHolderPanel, BoxLayout.X_AXIS));
          showNextBtn = new JButton("V");
            showNextBtn.setEnabled(false);
            showCenterHolderPanel.add(showNextBtn);
          showCenterPanel.add(showCenterHolderPanel);
        showPanel.add(showCenterPanel);

      showAllBox = new JCheckBox("Show All");
        showAllBox.setEnabled(true);
        showAllBox.setHorizontalAlignment(SwingConstants.LEFT);
        showPanel.add(showAllBox);


    showNextBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showNext();
        scrollTo(SCROLL_TO_BOTTOM);
        scrollTo(SCROLL_TO_BOTTOM);
      }
    });

    showAllBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if(!showAllBox.isSelected()) return;

        while (showNext() != false);
      }
    });

    this.add(ctrlPanel, BorderLayout.NORTH);
    this.add(centerPanel, BorderLayout.CENTER);
    this.add(showPanel, BorderLayout.SOUTH);

    reset();
  }


  /**
   * Sets the error handler to notify of exceptions.
   * This is a convenience for logging/muting.
   * The default handler prints to System.err.
   *
   * @param eh the error handler, or null, for quiet parsing
   */
  public void setErrorHandler(UHSErrorHandler eh) {
    errorHandler = eh;
  }


  /**
   * Get the dir in which to look for UHS files.
   *
   * @return the path
   */
  public String getHintsPath() {return hintsPath;}

  /**
   * Set the dir in which to look for UHS files.
   * The path must exist and have no trailing slash.
   *
   * The default path is "."
   *
   * @param s the path
   */
  public void setHintsPath(String s) {
    java.io.File tmpFile = new java.io.File(s);
    if (tmpFile.exists() && tmpFile.isDirectory())
      hintsPath = s;
  }


  /**
   * Clears everything.
   */
  public void reset() {
    historyArray.clear();
    futureArray.clear();
    backBtn.setEnabled(false);
    forwardBtn.setEnabled(false);
    findBtn.setEnabled(false);

    showLabel.setText("");
    showNextBtn.setEnabled(false);

    rootNode = null;
    currentNode = null;

    scrollView.removeAll();
    currentNodePanel = null;

    pronoun.setReaderTitle(null);
  }


  /**
   * Opens a UHS file.
   *
   * @param path location of the file
   */
  public void openFile(final String path) {
    ancestorSetNerfed(true);

    if (errorHandler != null) {
      errorHandler.log(UHSErrorHandler.INFO, this, "Opened "+ (new File(path)).getName(), 0, null);
    }

    Thread parseWorker = new Thread() {
      public void run() {
        OpenUHSLib UHSLib = new OpenUHSLib();
          UHSLib.setErrorHandler(errorHandler);
        final UHSRootNode rootNode = UHSLib.parseFile(path, UHSLib.AUX_NEST);
        if (rootNode == null) {
          JOptionPane.showMessageDialog(pronoun, "Unreadable file or parsing error", "OpenUHS Cannot Continue", JOptionPane.ERROR_MESSAGE);
        }
        else {
          // Back to the event thread...
          Runnable r = new Runnable() {
            public void run() {
              setUHSNodes(rootNode, rootNode);
              ancestorSetNerfed(false);
            }
          };
          EventQueue.invokeLater(r);
        }
      }
    };

    parseWorker.start();
  }


  /**
   * Displays a new UHS tree.
   *
   * @param inCurrentNode the new initial node
   * @param inRootNode the new root node
   */
  public void setUHSNodes(UHSNode inCurrentNode, UHSRootNode inRootNode) {
    reset();
    rootNode = inRootNode;
    findBtn.setEnabled(true);
    setReaderNode(inCurrentNode);
    System.gc();
  }


  /**
   * Displays a new node within the current tree.
   * <br />If the node is the same as the next/prev one, breadcrumbs will be traversed.
   *
   * @param newNode the new node
   */
  public void setReaderNode(UHSNode newNode) {
    if (newNode == null) {return;}

    int matchesNextPrev = 0; //-1 prev, 1 next, 0 neither

    if (historyArray.size() > 0 && historyArray.get(historyArray.size()-1).equals(newNode)) {
      matchesNextPrev = -1;
    }
    else if (futureArray.size() > 0 && futureArray.get(futureArray.size()-1).equals(newNode)) {
      matchesNextPrev = 1;
    }


    if (matchesNextPrev == -1) {
      //Move one node into the past
      historyArray.remove(historyArray.size()-1);
      if (currentNode != null) {
        //Leave a breadcrumb before changing to the new node
        futureArray.add(currentNode);
      }
    }
    else if (matchesNextPrev == 1) {
      //Move one node into the future
      futureArray.remove(futureArray.size()-1);
      if (currentNode != null) {
        //Leave a breadcrumb before changing to the new node
        historyArray.add(currentNode);
      }
    }
    else {
      if (currentNode != null) {
        //Leave a breadcrumb before changing to the new node
        historyArray.add(currentNode);
      }
      //Wipe the future
      futureArray.clear();
    }
    backBtn.setEnabled( (historyArray.size() > 0) );
    forwardBtn.setEnabled( (futureArray.size() > 0) );


    currentNode = newNode;
    boolean showAll = false;

    if (currentNode.equals(rootNode)) {
      questionLabel.setText("");
      pronoun.setReaderTitle((String)currentNode.getContent());
      showAll = true;
    }
    else {
      if (currentNode.getContentType() == UHSNode.STRING) questionLabel.setText(currentNode.getType() +"="+ (String)currentNode.getContent());
      else questionLabel.setText("");
      showAll = showAllBox.isSelected();
    }
    scrollView.removeAll();
    currentNodePanel = new NodePanel(currentNode, pronoun, showAll);
    scrollView.add(currentNodePanel);

    scrollTo(SCROLL_IF_INCOMPLETE);

    boolean complete = currentNodePanel.isComplete();
    showLabel.setText("Hint "+ (complete?currentNodePanel.getNode().getChildCount():currentNodePanel.getNode().getRevealedAmount()) +"/"+ currentNodePanel.getNode().getChildCount());
    showNextBtn.setEnabled(!complete);

    pronoun.validate();
    pronoun.repaint();
  }


  /**
   * Displays a new node within the current tree.
   * <br />Nothing will happen if the ID isn't among the root node's list of link targets.
   *
   * @param id ID of the new node
   */
  public void setReaderNode(int id) {
    UHSNode tmpNode = rootNode.getLink(id);
    if (tmpNode != null) setReaderNode(tmpNode);
    else JOptionPane.showMessageDialog(pronoun, "Could not find link target: "+ id, "OpenUHS Cannot Continue", JOptionPane.ERROR_MESSAGE);
  }


  /**
   * Sets the reader's title to the specified string.
   *
   * @param s the title to be displayed in the reader. A null value is treated as an empty string, "".
   */
  public void setReaderTitle(String s) {
    readerTitle = (s!=null?s:"");

    Object ancestor = pronoun.getTopLevelAncestor();
    if (ancestor != null && ancestor instanceof Frame) {
      ((Frame)ancestor).setTitle(readerTitle);
    }
  }

  /**
   * Gets the title of the reader.
   *
   * @return the title of the reader
   */
  public String getReaderTitle() {
    return readerTitle;
  }


  /**
   * Recursively searches for a phrase within children of a node.
   * <br />
   * <br />This'll go into an infinite loop if two nodes have each other as children.
   * <br />Luckily real UHS files aren't structured that way.
   * Link targets don't count as children.
   *
   * @param resultsNode an existing temporary node to add results to
   * @param prefix phrase to prepend to result titles (use "")
   * @param depth recursion level reminder (use 0)
   * @param input the phrase to search for
   */
  public void searchNode(UHSNode resultsNode, String prefix, int depth, UHSNode currentNode, String input) {
    if (input == null || input.length() == 0) return;
    //assuming input is lower case because toLowering it here would be wasteful

    if (currentNode.getContentType() == UHSNode.STRING) {
      prefix = (depth>1?prefix+" : ":"") + currentNode.getContent();
    }
    else {
      prefix = (depth>1?prefix+" : ":"") + "???";
    }

    depth++;
    UHSNode tmpNode = null;
    UHSNode newNode = null;
    boolean beenListed = false;
    for (int i=0; i<currentNode.getChildCount(); i++) {
      tmpNode = ((UHSNode)currentNode.getChild(i));
      if (tmpNode.getContentType() == UHSNode.STRING && beenListed == false) {
        if (((String)tmpNode.getContent()).toLowerCase().indexOf(input) != -1) {
          newNode = new UHSNode("result");
          newNode.setContent(prefix, UHSNode.STRING);
          newNode.setChildren(currentNode.getChildren());
          resultsNode.addChild(newNode);
          beenListed = true;
        }
      }
      searchNode(resultsNode, prefix, depth, currentNode.getChild(i), input);
    }
  }


  /**
   * Reveals the next hint of the current node panel.
   *
   * @return true if successful, false otherwise
   */
  public boolean showNext() {
    if (currentNodePanel == null) return false;

    int revealedIndex = currentNodePanel.showNext();
    if (revealedIndex == -1) {
      showNextBtn.setEnabled(false);
      return false;
    } else {
      int lastHint = currentNodePanel.getNode().getChildCount();
      if ((revealedIndex+1) == lastHint) showNextBtn.setEnabled(false);
      showLabel.setText("Hint "+ (revealedIndex+1) +"/"+ lastHint);
      return true;
    }
  }


  /**
   * Scrolls to the top/bottom of the visible hints.
   * <br />This enqueues a thread to do the scrolling at the next opportunity.
   * <br />The third option defers completeness checking until the thread runs: top if true, bottom otherwise.
   * <br />
   * <br />The threading and yielding is a workaround for JScrollPane goofiness when the content grows.
   *
   * @param position either SCROLL_TO_TOP, SCROLL_TO_BOTTOM, or SCROLL_IF_INCOMPLETE
   * @see SwingUtilities#invokeLater(Runnable) SwingUtilities.InvokeLater(Runnable)
   * @see Thread#yield() Thread.yield()
   */
  public void scrollTo(int position) {
    //Wait a bit longer
    Thread.yield();

    if (position == SCROLL_IF_INCOMPLETE) {
      if (currentNodePanel.isComplete()) position = SCROLL_TO_TOP;
      else position = SCROLL_TO_BOTTOM;
    }
    if (position == SCROLL_TO_TOP) {
      SwingUtilities.invokeLater(new Runnable() {public void run() {scrollPane.getViewport().setViewPosition(new Point(0, 0));}});
    }
    else if (position == SCROLL_TO_BOTTOM) {
      SwingUtilities.invokeLater(new Runnable() {public void run() {
        //Wait some more
        Thread.yield();
        int areaHeight = scrollView.getPreferredSize().height;
        int viewHeight = scrollPane.getViewport().getExtentSize().height;
        if (areaHeight > viewHeight)
        scrollPane.getViewport().setViewPosition(new Point(0, areaHeight-viewHeight));
      }});
    }
  }


  public AppliablePanel getSettingsPanel() {
    AppliablePanel result = new AppliablePanel(new BorderLayout());

    result.add(new JLabel("Text Size"), BorderLayout.WEST);
    final JTextField textSizeField = new JTextField("222");
      textSizeField.setHorizontalAlignment(JTextField.RIGHT);
      textSizeField.setMaximumSize(textSizeField.getPreferredSize());
      textSizeField.setMinimumSize(textSizeField.getPreferredSize());
      textSizeField.setPreferredSize(textSizeField.getPreferredSize());
      result.add(textSizeField, BorderLayout.EAST);

    Style regularStyle = UHSTextArea.DEFAULT_STYLES.getStyle("regular");
    if (regularStyle != null) {
      textSizeField.setText(StyleConstants.getFontSize(regularStyle) +"");
    } else {
      textSizeField.setText("");
    }

    Runnable applyAction = new Runnable() {
      public void run() {
        try {
          int newSize = Integer.parseInt(textSizeField.getText());
          if (newSize > 0) {
            Style baseStyle = UHSTextArea.DEFAULT_STYLES.getStyle("base");
            StyleConstants.setFontSize(baseStyle, newSize);
          }
        }
        catch (NumberFormatException e) {}
      }
    };
    result.setApplyAction(applyAction);

    return result;
  }


  /**
   * Calls setNerfed on the top-level ancestor, if nerfable.
   * A dedicated method was easier than passing the ancestor to runnables.
   */
  private void ancestorSetNerfed(boolean b) {
    boolean nerfable = false;
    Component parentComponent = null;
    Object ancestor = pronoun.getTopLevelAncestor();
    if (ancestor != null) {
      if (ancestor instanceof Nerfable) nerfable = true;
      if (ancestor instanceof Component) parentComponent = (Component)ancestor;
    }

    if (nerfable) ((Nerfable)ancestor).setNerfed(b);
  }
}
