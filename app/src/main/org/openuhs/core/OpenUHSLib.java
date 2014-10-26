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

import javax.swing.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.nio.*;


/**
 * UHS file decoder.
 */
public class OpenUHSLib {
  /** Honor the actual UHS file structure for version 9x auxiliary nodes */
  public static final int AUX_NORMAL = 0;

  /** Drop version 9x auxiliary nodes */
  public static final int AUX_IGNORE = 1;

  /** Move version 9x auxiliary nodes to within the master subject node and make that the new root */
  public static final int AUX_NEST = 2;

  private int logHeader = 0;
  private int logLine = -1;
  private UHSErrorHandler errorHandler = null;


  /**
   * Creates an OpenUHSLib.
   * A DefaultUHSErrorHandler(System.err) will be used.
   *
   * @see #setErrorHandler(UHSErrorHandler) setErrorHandler(UHSErrorHandler)
   */
  public OpenUHSLib() {
    this(new DefaultUHSErrorHandler(System.err));
  }

  /**
   * Creates an OpenUHSLib and sets an error handler.
   *
   * @see #setErrorHandler(UHSErrorHandler) setErrorHandler(UHSErrorHandler)
   */
  public OpenUHSLib(UHSErrorHandler eh) {
    setErrorHandler(eh);
  }


  /**
   * Sets the error handler to notify of exceptions.
   * This is a convenience for logging/muting.
   *
   * @param eh the error handler, or null, for quiet parsing
   */
  public void setErrorHandler(UHSErrorHandler eh) {
    errorHandler = eh;
  }


  /**
   * Generates a decryption key for formats after 88a.
   *
   * @param name the name of the master subject node of the UHS document (not the filename)
   * @return the key
   * @see #decryptNestString(String, int[]) decryptNestString(String, int[])
   * @see #decryptTextHunk(String, int[]) decryptTextHunk(String, int[])
   */
  public int[] generateKey(String name) {
    int[] key = new int[name.length()];
    int[] k = {'k', 'e', 'y'};
    for (int i=0; i < name.length(); i++) {
      key[i] = (int)name.charAt(i) + (k[i%3] ^ (i + 40));
      while (key[i]>127) {
        key[i] -= 96;
      }
    }
    return key;
  }

  /**
   * Decrypts the content of standalone 'hint' hunks, and all 88a blocks.
   * <br />This is only necessary when initially parsing a file.
   *
   * @param input ciphertext
   * @return the decrypted text
   */
  public String decryptString(String input) {
    StringBuffer tmp = new StringBuffer(input.length());

    for (int i=0; i < input.length(); i++) {
      int mychar = (int)input.charAt(i);
      if (mychar < 32) {}
      else if (mychar < 80) {mychar = mychar*2-32;}
      else {mychar = mychar*2-127;}

      tmp.append((char)mychar);
    }

    return tmp.toString();
  }

  /**
   * Decrypts the content of 'nesthint' and 'incentive' hunks.
   * <br />This is only necessary when initially parsing a file.
   *
   * @param input ciphertext
   * @param key this file's hint decryption key
   * @return the decrypted text
   */
  public String decryptNestString(String input, int[] key) {
    StringBuffer tmp = new StringBuffer(input.length());
    int tmpChar = 0;

    for (int i=0; i < input.length(); i++) {
      int codeoffset = i % key.length;
      tmpChar = input.charAt(i) - (key[codeoffset] ^ (i + 40));
      while (tmpChar<32) {
        tmpChar += 96;
      }
      tmp.append((char)tmpChar);
    }

    return tmp.toString();
  }

  /**
   * Decrypts the content of 'text' hunks.
   * <br />This is only necessary when initially parsing a file.
   *
   * @param input ciphertext
   * @param key this file's hint decryption key
   * @return the decrypted text
   */
  public String decryptTextHunk(String input, int[] key) {
    StringBuffer tmp = new StringBuffer(input.length());
    int tmpChar = 0;

    for (int i=0; i < input.length(); i++) {
      int codeoffset = i % key.length;
      tmpChar = input.charAt(i) - (key[codeoffset] ^ (codeoffset + 40));
      while (tmpChar<32) {
        tmpChar += 96;
      }
      tmp.append((char)tmpChar);
    }

    return tmp.toString();
  }

  /**
   * Reads a UHS file into an ArrayList of text lines and an array of binary bytes.
   * Then call an appropriate parser to construct a UHSRootNode and a tree of UHSNodes.
   * <br />
   * <br />This is likely the only method you'll need.
   *
   * @param fileName file to read
   * @param auxStyle option for 9x files AUX_NORMAL, AUX_IGNORE, or AUX_NEST
   * @return the root of a tree of nodes representing the hint file
   * @see #parse88Format(ArrayList, String, int) parse88Format(ArrayList, String, int)
   * @see #parse9xFormat(ArrayList, byte[], long, int) parse9xFormat(ArrayList, byte[], long, int)
   */
  public UHSRootNode parseFile(String fileName, int auxStyle) {
    if (auxStyle != AUX_NORMAL && auxStyle != AUX_IGNORE && auxStyle != AUX_NEST) return null;
    logHeader = 0; logLine = -1;

    String tmp = "";
    //Four-line header is here
    int endSectionTitles = 0;

    int startHintSection = 0;
    int endHintSection = 0;

    ArrayList uhsFileArray = new ArrayList();
    String name = "";

    long rawOffset = -1;
    byte[] rawuhs = null;

    try {
      RandomAccessFile inFile = new RandomAccessFile(fileName, "r");

      logHeader++;
      tmp = inFile.readLine();
      if (!tmp.equals("UHS")) {
        if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Not a UHS file!", logHeader, null);
        return null;
      }

      logHeader++;
      tmp = inFile.readLine();
      name = tmp;

      //The indeces, from this point, of the first/last lines of hints in 88a files
      //After 88a, those lines contain an "upgrade your reader" notice
      logHeader++;
      tmp = inFile.readLine();    //Skip the startHintSection

      logHeader++;
      tmp = inFile.readLine();
      endHintSection = Integer.parseInt(tmp);

      //There's a hunk of binary referenced by offset at the end of 91a and newer files
      //One can skip to it by searching for 0x1Ah.
      byte tmpByte = -1;
      while ((tmpByte = (byte)inFile.read()) != -1 && tmpByte != 0x1a) {
        inFile.getChannel().position( inFile.getChannel().position()-1 );
        logLine++;
        tmp = inFile.readLine();
        uhsFileArray.add(tmp);
      }

      rawOffset = inFile.getChannel().position();
      long binSize = inFile.length()-rawOffset;
      if (binSize > 0 && binSize <= Integer.MAX_VALUE) {
        rawuhs = new byte[(int)binSize];
        inFile.readFully(rawuhs);
      }
      else
        rawOffset = -1;

      inFile.close();
    }
    catch (FileNotFoundException e) {
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "No file", logHeader+logLine+1, e);
      return null;
    }
    catch (IOException e) {
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Could not read file", logHeader+logLine+1, e);
      return null;
    }
    catch (NumberFormatException e) {
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Could not parse header", logHeader+logLine+1, e);
      return null;
    }


    boolean version88a = true;
    for (int i=endHintSection-1+1; i < uhsFileArray.size(); i++) {
      if ( ((String)uhsFileArray.get(i)).equals("** END OF 88A FORMAT **") ) {
        version88a = false;

        //Since v91a, the line count starts here, after the old-style 88a section and its "end of" comment.
        logHeader += i;
        for (int j=1; j <= i; j++) {
          uhsFileArray.remove(0);
        }
        break;
      }
    }


    UHSRootNode rootNode = null;
    if (version88a) {
      rootNode = parse88Format(uhsFileArray, name, endHintSection);
    } else {
      rootNode = parse9xFormat(uhsFileArray, rawuhs, rawOffset, auxStyle);
    }
    return rootNode;
  }


  /**
   * Generates a tree of UHSNodes from UHS 88a.
   * A Version node will be added, since that was not natively reported in 88a.
   *
   * <pre> UHS
   * document title
   * # (index of first hint)
   * # (index of last hint)
   * subject
   * # (index of first question)
   * subject
   * # (index of first question)
   * subject
   * # (index of first question)
   * question (without a '?')
   * # (index of first hint)
   * question (without a '?')
   * # (index of first hint)
   * question (without a '?')
   * # (index of first hint)
   * hint (encrypted)
   * hint (encrypted)
   * hint (encrypted)
   * hint (encrypted)
   * sentence
   * sentence
   * sentence</pre>
   * <br />
   * Below, uhsFileArray begins at the first subject.
   *
   * @param uhsFileArray array of all available lines in the file
   * @param name the UHS document's name (not the filename)
   * @param hintSectionEnd index of the last hint, relative to the first subject (as in the file, 1-based)
   * @return the root of a tree of nodes
   */
  public UHSRootNode parse88Format(ArrayList uhsFileArray, String name, int hintSectionEnd) {
    try {
      UHSRootNode rootNode = new UHSRootNode();
        rootNode.setContent(name, UHSNode.STRING);
      int fudge = 1; //The format's 1-based, the array's 0-based

      int questionSectionStart = Integer.parseInt(getLoggedString(uhsFileArray, 1)) - fudge;

      for (int s=0; s < questionSectionStart; s+=2) {
        UHSNode currentSubject = new UHSNode("Subject");
          currentSubject.setContent(decryptString(getLoggedString(uhsFileArray, s)), UHSNode.STRING);
          rootNode.addChild(currentSubject);

        int firstQuestion = Integer.parseInt(getLoggedString(uhsFileArray, s+1)) - fudge;
        int nextSubjectsFirstQuestion = Integer.parseInt(getLoggedString(uhsFileArray, s+3)) - fudge;
          //On the last loop, s+3 is a question's first hint

        for (int q=firstQuestion; q < nextSubjectsFirstQuestion; q+=2) {
          UHSNode currentQuestion = new UHSNode("Question");
            currentQuestion.setContent(decryptString(getLoggedString(uhsFileArray, q)) +"?", UHSNode.STRING);
            currentSubject.addChild(currentQuestion);

          int firstHint = Integer.parseInt(getLoggedString(uhsFileArray, q+1)) - fudge;
          int lastHint = 0;
          if (s == questionSectionStart - 2 && q == nextSubjectsFirstQuestion - 2) {
            lastHint = hintSectionEnd + 1 - fudge;
              //Line after the final hint
          } else {
            lastHint = Integer.parseInt(getLoggedString(uhsFileArray, q+3)) - fudge;
              //Next question's first hint
          }

          for (int h=firstHint; h < lastHint; h++) {
            UHSNode currentHint = new UHSNode("Hint");
              currentHint.setContent(decryptString(getLoggedString(uhsFileArray, h)), UHSNode.STRING);
              currentQuestion.addChild(currentHint);
          }
        }
      }
      UHSNode blankNode = new UHSNode("Blank");
        blankNode.setContent("--=File Info=--", UHSNode.STRING);
        rootNode.addChild(blankNode);
      UHSNode fauxVersionNode = new UHSNode("Version");
        fauxVersionNode.setContent("Version: 88a", UHSNode.STRING);
        rootNode.addChild(fauxVersionNode);
        UHSNode fauxVersionDataNode = new UHSNode("VersionData");
          fauxVersionDataNode.setContent("This version info was added by OpenUHS during parsing because the 88a format does not report it.", UHSNode.STRING);
          fauxVersionNode.addChild(fauxVersionDataNode);
      UHSNode creditNode = new UHSNode("Credit");
        creditNode.setContent("Credits", UHSNode.STRING);
        rootNode.addChild(creditNode);

      StringBuffer tmpContent = new StringBuffer();
      UHSNode newNode = new UHSNode("CreditData");

      for (int i=hintSectionEnd; i < uhsFileArray.size(); i++) {
        if ( (getLoggedString(uhsFileArray, i)).equals("** END OF 88A FORMAT **") ) break;
        tmpContent.append(getLoggedString(uhsFileArray, i));
      }
      newNode.setContent(tmpContent.toString(), UHSNode.STRING);
        creditNode.addChild(newNode);

      return rootNode;
    }
    catch (NumberFormatException e) {
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Could not parse nodes", logHeader+logLine+1, e);
      return null;
    }
  }


  /**
   * Generates a tree of UHSNodes from UHS 91a format onwards.
   * <br />
   * <br />Versions 91a, 95a, and 96a have been seen in the wild.
   * <br />These UHS files are prepended with an 88a section containing an "upgrade your reader" notice.
   * <br />Below, uhsFileArray begins after "** END OF 88A FORMAT **".
   *
   * <pre> UHS
   * # Subject
   * title
   * ...
   * # version
   * title
   * content
   * # incentive
   * title
   * content
   * # info
   * title
   * content
   * 0x1Ah character
   * {binary hunk}
   * </pre>
   * <br />
   * <br />The root node would normally contain up to four children.
   * <br />A 'subject', containing all the subjects, hints, etc., that users care about.
   * <br />A 'version', mentioning the UHS compiler that made the file.
   * <br />An 'info', mentioning the author, publisher, etc.
   * <br />And an 'incentive', listing nodes to show/block if the reader is unregistered.
   * <br />For convenience, these auxiliary nodes can be treated differently.
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param auxStyle AUX_NORMAL (canon), AUX_IGNORE (omit), or AUX_NEST (move inside the master subject and make that the new root).
   * @return the root of a tree of nodes
   * @see #buildNodes(ArrayList, byte[], long, UHSRootNode, UHSNode, int[], int) buildNodes(ArrayList, byte[], long, UHSRootNode, UHSNode, int[], int)
   */
  public UHSRootNode parse9xFormat(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, int auxStyle) {
    if (auxStyle != AUX_NORMAL && auxStyle != AUX_IGNORE && auxStyle != AUX_NEST) return null;

    try {
      UHSRootNode rootNode = new UHSRootNode();
        rootNode.setContent("root", UHSNode.STRING);

      String name = getLoggedString(uhsFileArray, 2); //This is the title of the master subject node
      int[] key = generateKey(name);

      int index = 1;
      index += buildNodes(uhsFileArray, rawuhs, rawOffset, rootNode, rootNode, key, index);

      if (auxStyle != AUX_IGNORE) {
        if (auxStyle == AUX_NEST) {
          UHSNode tmpChildNode = rootNode.getChild(0);
          rootNode.setChildren(tmpChildNode.getChildren());
          rootNode.setContent(name, UHSNode.STRING);

          UHSNode blankNode = new UHSNode("Blank");
            blankNode.setContent("--=File Info=--", UHSNode.STRING);
            rootNode.addChild(blankNode);
        }
        while (index < uhsFileArray.size()) {
          index += buildNodes(uhsFileArray, rawuhs, rawOffset, rootNode, rootNode, key, index);
        }
      }
      return rootNode;
    }
    catch (NumberFormatException e) {
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Could not parse nodes", logHeader+logLine+1, e);
      return null;
    }
  }


  /**
   * Recursively parses UHS newer than 88a.
   * <br />This recognizes various types of hints, and runs specialized methods to decode them.
   * <br />Unrecognized hints are harmlessly omitted.
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int buildNodes(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    int index = startIndex;

    String tmp = getLoggedString(uhsFileArray, index);
    if (tmp.matches("[0-9]+ [A-Za-z]+$") == true) {
      if (tmp.endsWith("comment")) {
        index += parseCommentNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("credit")) {
        index += parseCreditNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith(" hint")) {
        index += parseHintNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("nesthint")) {
        index += parseNestHintNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("subject")) {
        index += parseSubjectNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("link")) {
        index += parseLinkNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("text")) {
        index += parseTextNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("hyperpng")) {
        index += parseHyperImgNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("gifa")) {
        index += parseHyperImgNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("sound")) {
        index += parseSoundNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("blank")) {
        index += parseBlankNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("version")) {
        index += parseVersionNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("info")) {
        index += parseInfoNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else if (tmp.endsWith("incentive")) {
        index += parseIncentiveNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
      else {
        index += parseUnknownNode(uhsFileArray, rawuhs, rawOffset, rootNode, currentNode, key, index);
      }
    } else {index++;}

    return index-startIndex;
  }

  /**
   * Replaces UHS escaped characters in a hint.
   * <br />Escapes have existed from version 88a onwards in most nodes' content and titles.
   * <br />The # character is the main escape char and is written <b>##</b>.
   *
   * <ul><li><b>#</b> a '#' character.</li>
   * <li><b>#a+</b>[AaEeIiOoUu][:'`^]<b>#a-</b> accent enclosed letter; :=diaeresis,'=acute,`=grave,^=curcumflex.</li>
   * <li><b>#a+</b>[Nn]~<b>#a-</b> accent enclosed letter with a tilde.</li>
   * <li><b>#a+</b>ae<b>#a-</b> an ash character.</li>
   * <li><b>#a+</b>TM<b>#a-</b> a trademark character.</li>
   * <li><b>#w.</b> raw newlines are spaces.</li>
   * <li><b>#w+</b> raw newlines are spaces (default).</li>
   * <li><b>#w-</b> raw newlines are newlines.</li></ul>
   *
   * The following are left for display code to handle (e.g., UHSTextArea).
   * <ul><li><b>#p+</b> proportional font (default).</li>
   * <li><b>#p-</b> non-proportional font.</li></ul>
   *
   * This is displayed, but not a clickable hyperlink.
   * <ul><li><b>#h+</b> through <b>#h-</b> is a hyperlink (http or email).</li></ul>
   * <br />Illustrative UHS: <i>Portal: Achievements</i> (hyperlink)
   *
   * @param currentNode the node whose content needs replacing
   */
  public void parseTextEscapes(UHSNode currentNode) {
    if (currentNode.getContentType() != UHSNode.STRING) return;

    char[] linebreak = new char[] {'^','b','r','e','a','k','^'};
    char[] accentPrefix = new char[] {'#','a','+'};
    char[] accentSuffix = new char[] {'#','a','-'};
    char[] wspcA = new char[] {'#','w','+'};
    char[] wspcB = new char[] {'#','w','.'};
    char[] wnlin = new char[] {'#','w','-'};

    StringBuffer buf = new StringBuffer();
    char[] tmp = ((String)currentNode.getContent()).toCharArray();
    String breakStr = " ";
    char[] chunkA = null;
    char[] chunkB = null;
    boolean fudgedPos = false;

    for (int c=0; c < tmp.length; c++) {
      if (c+1 < tmp.length) {
        if (tmp[c] == '#' && tmp[c+1] == '#') {buf.append('#'); c+=1; continue;}
      }
      if (tmp[c] == '#') {
        if (c+7 < tmp.length) {
          chunkA = new char[] {tmp[c],tmp[c+1],tmp[c+2]};
          chunkB = new char[] {tmp[c+5],tmp[c+6],tmp[c+7]};
          if (Arrays.equals(chunkA, accentPrefix) && Arrays.equals(chunkB, accentSuffix)) {
            if (tmp[c+4] == ':') {
              if (tmp[c+3] == 'A') {buf.append('Ä'); c+=7; continue;}
              if (tmp[c+3] == 'E') {buf.append('Ë'); c+=7; continue;}
              if (tmp[c+3] == 'I') {buf.append('Ï'); c+=7; continue;}
              if (tmp[c+3] == 'O') {buf.append('Ö'); c+=7; continue;}
              if (tmp[c+3] == 'U') {buf.append('Ü'); c+=7; continue;}
              if (tmp[c+3] == 'a') {buf.append('ä'); c+=7; continue;}
              if (tmp[c+3] == 'e') {buf.append('ë'); c+=7; continue;}
              if (tmp[c+3] == 'i') {buf.append('ï'); c+=7; continue;}
              if (tmp[c+3] == 'o') {buf.append('ö'); c+=7; continue;}
              if (tmp[c+3] == 'u') {buf.append('ü'); c+=7; continue;}
            }
            else if (tmp[c+4] == '\'') {
              if (tmp[c+3] == 'A') {buf.append('Á'); c+=7; continue;}
              if (tmp[c+3] == 'E') {buf.append('É'); c+=7; continue;}
              if (tmp[c+3] == 'I') {buf.append('Í'); c+=7; continue;}
              if (tmp[c+3] == 'O') {buf.append('Ó'); c+=7; continue;}
              if (tmp[c+3] == 'U') {buf.append('Ú'); c+=7; continue;}
              if (tmp[c+3] == 'a') {buf.append('á'); c+=7; continue;}
              if (tmp[c+3] == 'e') {buf.append('é'); c+=7; continue;}
              if (tmp[c+3] == 'i') {buf.append('í'); c+=7; continue;}
              if (tmp[c+3] == 'o') {buf.append('ó'); c+=7; continue;}
              if (tmp[c+3] == 'u') {buf.append('ú'); c+=7; continue;}
            }
            else if (tmp[c+4] == '`') {
              if (tmp[c+3] == 'A') {buf.append('À'); c+=7; continue;}
              if (tmp[c+3] == 'E') {buf.append('È'); c+=7; continue;}
              if (tmp[c+3] == 'I') {buf.append('Ì'); c+=7; continue;}
              if (tmp[c+3] == 'O') {buf.append('Ò'); c+=7; continue;}
              if (tmp[c+3] == 'U') {buf.append('Ù'); c+=7; continue;}
              if (tmp[c+3] == 'a') {buf.append('à'); c+=7; continue;}
              if (tmp[c+3] == 'e') {buf.append('è'); c+=7; continue;}
              if (tmp[c+3] == 'i') {buf.append('ì'); c+=7; continue;}
              if (tmp[c+3] == 'o') {buf.append('ò'); c+=7; continue;}
              if (tmp[c+3] == 'u') {buf.append('ù'); c+=7; continue;}
            }
            else if (tmp[c+4] == '^') {
              if (tmp[c+3] == 'A') {buf.append('Â'); c+=7; continue;}
              if (tmp[c+3] == 'E') {buf.append('Ê'); c+=7; continue;}
              if (tmp[c+3] == 'I') {buf.append('Î'); c+=7; continue;}
              if (tmp[c+3] == 'O') {buf.append('Ô'); c+=7; continue;}
              if (tmp[c+3] == 'U') {buf.append('Û'); c+=7; continue;}
              if (tmp[c+3] == 'a') {buf.append('â'); c+=7; continue;}
              if (tmp[c+3] == 'e') {buf.append('ê'); c+=7; continue;}
              if (tmp[c+3] == 'i') {buf.append('î'); c+=7; continue;}
              if (tmp[c+3] == 'o') {buf.append('ô'); c+=7; continue;}
              if (tmp[c+3] == 'u') {buf.append('û'); c+=7; continue;}
            }
            else if (tmp[c+4] == '~') {
              if (tmp[c+3] == 'N') {buf.append('Ñ'); c+=7; continue;}
              if (tmp[c+3] == 'n') {buf.append('ñ'); c+=7; continue;}
            }
            else if (tmp[c+3] == 'a' && tmp[c+4] == 'e') {
              buf.append('æ'); c+=7; continue;
            }
            else if (tmp[c+3] == 'T' && tmp[c+4] == 'M') {
              buf.append('™'); c+=7; continue;
            }
            else {
              if (errorHandler != null) errorHandler.log(UHSErrorHandler.INFO, this, "Unknown accent: "+ tmp[c+3] + tmp[c+4], logHeader+logLine+1, null);
            }
          }
        }
        if (c+2 < tmp.length) {
          chunkA = new char[] {tmp[c],tmp[c+1],tmp[c+2]};
          if (Arrays.equals(chunkA, wspcA) || Arrays.equals(chunkA, wspcB)) {breakStr = " "; c+=2; continue;}
          else if (Arrays.equals(chunkA, wnlin)) {breakStr = "\n"; c+=2; continue;}
        }
      }
      if (c+6 < tmp.length) {
        chunkA = new char[] {tmp[c],tmp[c+1],tmp[c+2],tmp[c+3],tmp[c+4],tmp[c+5],tmp[c+6]};
        if (Arrays.equals(chunkA, linebreak)) {buf.append(breakStr); c += 6; continue;}
      }
      buf.append(tmp[c]);
    }

    currentNode.setContent(buf.toString(), UHSNode.STRING);
  }

  /**
   * Generates a subject UHSNode and its contents.
   *
   * <pre> # subject
   * title
   * embedded hunk
   * embedded hunk
   * embedded hunk</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int parseSubjectNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    UHSNode newNode = new UHSNode("Subject");
      newNode.setContent(getLoggedString(uhsFileArray, index), UHSNode.STRING);
      parseTextEscapes(newNode);
      newNode.setId(startIndex);
      currentNode.addChild(newNode);
      rootNode.addLink(newNode);
    index++;
    innerCount--;

    for (int j=0; j < innerCount;) {
      j += buildNodes(uhsFileArray, rawuhs, rawOffset, rootNode, newNode, key, index+j);
    }

    index += innerCount;
    return index-startIndex;
  }

  /**
   * Generates a nested hint UHSNode and its contents.
   *
   * <pre> # nesthint
   * Question
   * hint (encrypted)
   * -
   * partial hint (encrypted)
   * =
   * embedded hunk
   * rest of hint (encrypted)
   * -
   * hint (encrypted)</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   * @see #decryptNestString(String, int[]) decryptNestString(String, int[])
   */
  public int parseNestHintNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    String breakChar = "^break^";

    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    UHSNode hintNode = new UHSNode("NestHint");
      hintNode.setContent(getLoggedString(uhsFileArray, index), UHSNode.STRING);
      parseTextEscapes(hintNode);
      hintNode.setId(startIndex);
      currentNode.addChild(hintNode);
      rootNode.addLink(hintNode);
    index++;
    innerCount--;

    StringBuffer tmpContent = new StringBuffer();
    UHSNode newNode = new UHSNode("Hint");

    for (int j=0; j < innerCount; j++) {
      tmp = getLoggedString(uhsFileArray, index+j);
      if (tmp.equals("-")) {
        //A hint, add last content
        if (tmpContent.length() > 0) {
          newNode.setContent(tmpContent.toString(), UHSNode.STRING);
          parseTextEscapes(newNode);
          hintNode.addChild(newNode);
          newNode = new UHSNode("Hint");
          tmpContent.delete(0, tmpContent.length());
        }
      }
      else if (tmp.equals("=")) {
        //Nested hunk, add last content
        if (tmpContent.length() > 0) {
          newNode.setContent(tmpContent.toString(), UHSNode.STRING);
          parseTextEscapes(newNode);
          hintNode.addChild(newNode);
        }

        j += buildNodes(uhsFileArray, rawuhs, rawOffset, rootNode, hintNode, key, index+j+1);

        if (tmpContent.length() > 0) {
          newNode = new UHSNode("Hint");
          tmpContent.delete(0, tmpContent.length());
        }
      }
      else {
        if (tmpContent.length() > 0) tmpContent.append(breakChar);
        tmpContent.append( decryptNestString(getLoggedString(uhsFileArray, index+j), key) );
      }

      if (j == innerCount-1 && tmpContent.length() > 0) {
        newNode.setContent(tmpContent.toString(), UHSNode.STRING);
        parseTextEscapes(newNode);
        hintNode.addChild(newNode);
      }
    }

    index += innerCount;
    return index-startIndex;
  }

  /**
   * Generates a normal hint UHSNode.
   *
   * <pre> # hint
   * Question
   * hint (encrypted)
   * -
   * hint (encrypted)</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   * @see #decryptString(String) decryptNestString(String)
   */
  public int parseHintNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    String breakChar = "^break^";

    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1 - 1;

    UHSNode hintNode = new UHSNode("Hint");
      hintNode.setContent(getLoggedString(uhsFileArray, index), UHSNode.STRING);
      parseTextEscapes(hintNode);
      hintNode.setId(startIndex);
      currentNode.addChild(hintNode);
      rootNode.addLink(hintNode);
    index++;

    StringBuffer tmpContent = new StringBuffer();
    UHSNode newNode = new UHSNode("Hint");

    for (int j=0; j < innerCount; j++) {
      tmp = getLoggedString(uhsFileArray, index+j);
      if (tmp.equals("-")) {
        if (tmpContent.length() > 0) {
          newNode.setContent(tmpContent.toString(), UHSNode.STRING);
          parseTextEscapes(newNode);
          hintNode.addChild(newNode);
          newNode = new UHSNode("Hint");
          tmpContent.delete(0, tmpContent.length());
        }
      } else {
        if (tmpContent.length() > 0) tmpContent.append(breakChar);

        tmp = getLoggedString(uhsFileArray, index+j);
        if (tmp.equals(" ")) tmpContent.append("\n \n");
        else tmpContent.append( decryptString(tmp) );
      }

      if (j == innerCount-1 && tmpContent.length() > 0) {
        newNode.setContent(tmpContent.toString(), UHSNode.STRING);
        parseTextEscapes(newNode);
        hintNode.addChild(newNode);
      }
    }

    index += innerCount;
    return index-startIndex;
  }

  /**
   * Generates a comment UHSNode.
   *
   * <pre> # comment
   * title
   * sentence
   * sentence
   * sentence</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int parseCommentNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    String breakChar = " ";

    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    UHSNode commentNode = new UHSNode("Comment");
      commentNode.setContent(getLoggedString(uhsFileArray, index), UHSNode.STRING);
      parseTextEscapes(commentNode);
      commentNode.setId(startIndex);
      currentNode.addChild(commentNode);
      rootNode.addLink(commentNode);
    index++;
    innerCount--;

    StringBuffer tmpContent = new StringBuffer();
    UHSNode newNode = new UHSNode("CommentData");

    for (int j=0; j < innerCount; j++) {
      if (tmpContent.length() > 0) tmpContent.append(breakChar);
      tmpContent.append( getLoggedString(uhsFileArray, index+j) );
    }
    newNode.setContent(tmpContent.toString(), UHSNode.STRING);
    parseTextEscapes(newNode);
    commentNode.addChild(newNode);

    index += innerCount;
    return index-startIndex;
  }

  /**
   * Generates a credit UHSNode.
   *
   * <pre> # credit
   * title
   * sentence
   * sentence
   * sentence</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int parseCreditNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    String breakChar = " ";

    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    UHSNode creditNode = new UHSNode("Credit");
      creditNode.setContent(getLoggedString(uhsFileArray, index), UHSNode.STRING);
      parseTextEscapes(creditNode);
      creditNode.setId(startIndex);
      currentNode.addChild(creditNode);
      rootNode.addLink(creditNode);
    index++;
    innerCount--;

    StringBuffer tmpContent = new StringBuffer();
    UHSNode newNode = new UHSNode("CreditData");

    for (int j=0; j < innerCount; j++) {
      if (tmpContent.length() > 0) tmpContent.append(breakChar);
      tmpContent.append( getLoggedString(uhsFileArray, index+j) );
    }
    newNode.setContent(tmpContent.toString(), UHSNode.STRING);
    parseTextEscapes(newNode);
    creditNode.addChild(newNode);

    index += innerCount;
    return index-startIndex;
  }

  /**
   * Generates a text UHSNode.
   *
   * <pre> # text
   * title
   * 000000 0 offset length</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   * @see #decryptTextHunk(String, int[]) decryptTextHunk(String, int[])
   */
  public int parseTextNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    String breakChar = "\n";

    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    tmp ="";
    UHSNode textNode = new UHSNode("Text");
      textNode.setContent(getLoggedString(uhsFileArray, index), UHSNode.STRING);
      parseTextEscapes(textNode);
      textNode.setId(startIndex);
      currentNode.addChild(textNode);
      rootNode.addLink(textNode);
    index++;

    tmp = getLoggedString(uhsFileArray, index);
    index++;
    long offset = Long.parseLong(tmp.substring(9, tmp.lastIndexOf(" "))) - rawOffset;
    int length = Integer.parseInt(tmp.substring(tmp.lastIndexOf(" ")+1, tmp.length()));

    StringBuffer tmpContent = new StringBuffer();
    UHSNode newNode = new UHSNode("TextData");

    byte[] tmpBytes = null;
    if (rawOffset != -1) tmpBytes = readBinaryHunk(rawuhs, offset, length);
    if (tmpBytes != null) {
      tmp = new String(tmpBytes);
    } else {
      // This error would be at index-1, if not for getLoggedString()'s counter
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Could not read referenced raw bytes", logHeader+logLine+1, null);
      tmp = "";
    }
    String[] lines = tmp.split("(\r\n)|\r|\n");
    for (int i=0; i < lines.length; i++) {
      if (tmpContent.length() > 0) tmpContent.append(breakChar);
      tmpContent.append( decryptTextHunk(lines[i], key) );
    }
    newNode.setContent(tmpContent.toString(), UHSNode.STRING);
    parseTextEscapes(newNode);
    textNode.addChild(newNode);

    return index-startIndex;
  }

  /**
   * Generates a link UHSNode.
   * <br />Nodes like this that have link targets behave like conventional hyperlinks instead of containing child nodes.
   *
   * <pre> # link
   * title
   * index</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int parseLinkNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    UHSNode newNode = new UHSNode("Link");
      newNode.setContent(getLoggedString(uhsFileArray, index), UHSNode.STRING);
      parseTextEscapes(newNode);
      currentNode.addChild(newNode);
    index++;

    int targetIndex = Integer.parseInt(getLoggedString(uhsFileArray, index));
      newNode.setLinkTarget(targetIndex);
    index++;

    //Removed since it ran endlessly when nodes link in both directions.
    //buildNodes(uhsFileArray, rawuhs, rawOffset, rootNode, newNode, key, targetIndex);

    return index-startIndex;
  }

  /**
   * Generates an image-filled UHSNode.
   * <br />The UHS format allows for regions that trigger links or reveal overlaid subimages.
   * <br />UHSHotSpotNode was written to handle regions.
   * <br />
   * <br />Illustrative UHS: <i>The Longest Journey: Chapter 7, the Stone Altar, Can you give me a picture of the solution?</i>
   * <br />Illustrative UHS: <i>Deja Vu I: Sewer, The Map</i>
   *
   * <pre> # hyperpng (or gifa)
   * title
   * 000000 offset length
   * --not-a-gap--
   * x y x+w y+h
   * # link
   * title
   * index
   * --not-a-gap--
   * x y x+w y+h
   * # link
   * title
   * index
   * --not-a-gap--
   * x y x+w y+h
   * # overlay
   * title
   * 000000 offset length x y
   * --not-a-gap--
   * x y x+w y+h
   * # overlay
   * title
   * 000000 offset length x y</pre>
   *
   * <br />gifa has the same structure, but might not officially contain regions.
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   * @see org.openuhs.core.UHSHotSpotNode
   */
  public int parseHyperImgNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    int index = startIndex;
    long offset = 0;
    int length = 0;
    byte[] tmpBytes = null;
    int x = 0;
    int y = 0;
    UHSHotSpotNode hotspotNode = new UHSHotSpotNode("HotSpot");  //This may or may not get used

    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    String type = "";
    if (tmp.indexOf("hyperpng") != -1) type = "Hyperpng";
    else if (tmp.indexOf("gifa") != -1) type = "Hypergif";
    else {
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "parseHyperImgNode() is for hyperpng and gifa hunks only.", logHeader+logLine+1, null);
      index += innerCount;
      return index-startIndex;
    }

    UHSNode imgNode = new UHSNode(type);
    String title = getLoggedString(uhsFileArray, index);
    index++;
    innerCount--;

    String[] tokens = (getLoggedString(uhsFileArray, index)).split(" ");
    index++;
    innerCount--;
    if (tokens.length != 3)
      return innerCount+3;
    //Skip dummy zeroes
    offset = Long.parseLong(tokens[1]) - rawOffset;
    length = Integer.parseInt(tokens[2]);
    tmpBytes = null;
    if (rawOffset != -1) tmpBytes = readBinaryHunk(rawuhs, offset, length);
    if (tmpBytes == null) {
      // This error would be at index-1, if not for getLoggedString()'s counter
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Could not read referenced raw bytes", logHeader+logLine+1, null);
    }
    imgNode.setContent(tmpBytes, UHSNode.IMAGE);

    //This if-else would make regionless hyperimgs standalone and unnested
    //if (innerCount+3 > 3) {
      hotspotNode.addChild(imgNode);

      hotspotNode.setContent(title, UHSNode.STRING);
      hotspotNode.setId(startIndex);
      currentNode.addChild(hotspotNode);
      rootNode.addLink(hotspotNode);
    //} else {
    //  imgNode.setId(startIndex);
    //  currentNode.addChild(imgNode);
    //  rootNode.addLink(imgNode);
    //}


    for (int j=0; j < innerCount;) {
      tokens = (getLoggedString(uhsFileArray, index+j)).split(" ");
      j++;
      if (tokens.length != 4)
        return innerCount+3;
      int zoneX1 = Integer.parseInt(tokens[0])-1;
      int zoneY1 = Integer.parseInt(tokens[1])-1;
      int zoneX2 = Integer.parseInt(tokens[2])-1;
      int zoneY2 = Integer.parseInt(tokens[3])-1;

      tmp = getLoggedString(uhsFileArray, index+j);
      j++;
      if (tmp.matches("[0-9]+ [A-Za-z]+$") == true) {
        int innerInnerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;
        if (tmp.endsWith("overlay")) {
          title = getLoggedString(uhsFileArray, index+j);
          j++;
          tokens = (getLoggedString(uhsFileArray, index+j)).split(" ");
          j++;

          if (tokens.length != 5)
            return innerCount+3;
          //Skip dummy zeroes
          offset = Long.parseLong(tokens[1]) - rawOffset;
          length = Integer.parseInt(tokens[2]);
          int posX = Integer.parseInt(tokens[3])-1;
          int posY = Integer.parseInt(tokens[4])-1;

          UHSNode newNode = new UHSNode("Overlay");
          tmpBytes = null;
          if (rawOffset != -1) tmpBytes = readBinaryHunk(rawuhs, offset, length);
          if (tmpBytes == null) {
            // This error would be at index+j-1, if not for getLoggedString()'s counter
            if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Could not read referenced raw bytes", logHeader+logLine+1, null);
          }
          newNode.setContent(tmpBytes, UHSNode.IMAGE);
          hotspotNode.addChild(newNode);
          hotspotNode.setCoords(newNode, new int[] {zoneX1, zoneY1, zoneX2-zoneX1, zoneY2-zoneY1, posX, posY});
        }
        else if (tmp.endsWith("link")) {
          UHSNode newNode = new UHSNode("Link");
          newNode.setContent(getLoggedString(uhsFileArray, index+j), UHSNode.STRING);
          parseTextEscapes(newNode);
          hotspotNode.addChild(newNode);
          hotspotNode.setCoords(newNode, new int[] {zoneX1, zoneY1, zoneX2-zoneX1, zoneY2-zoneY1, -1, -1});
          j++;
          int targetIndex = Integer.parseInt(getLoggedString(uhsFileArray, index+j));
          newNode.setLinkTarget(targetIndex);
          j++;
        }
        else {
          j += innerInnerCount-1;
        }
      } else {j++;}
    }
    index += innerCount;
    return index-startIndex;
  }


  /**
   * Generates a sound UHSNode.
   * <br />This seems to be limited to PCM WAV audio.
   * <br />
   * <br />Illustrative UHS: Tex Murphy: Overseer: Day Two, Bosworth Clark's Lab, How do I operate that keypad?
   *
   * <pre> # sound
   * title
   * 000000 offset length</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   * @see #decryptTextHunk(String, int[]) decryptTextHunk(String, int[])
   */
  public int parseSoundNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    tmp ="";
    UHSNode soundNode = new UHSNode("Sound");
      soundNode.setContent(getLoggedString(uhsFileArray, index), UHSNode.STRING);
      parseTextEscapes(soundNode);
      soundNode.setId(startIndex);
      currentNode.addChild(soundNode);
      rootNode.addLink(soundNode);
    index++;

    tmp = getLoggedString(uhsFileArray, index);
    index++;
    long offset = Long.parseLong(tmp.substring(tmp.indexOf(" ")+1, tmp.lastIndexOf(" "))) - rawOffset;
    int length = Integer.parseInt(tmp.substring(tmp.lastIndexOf(" ")+1, tmp.length()));

    UHSNode newNode = new UHSNode("SoundData");

    byte[] tmpBytes = null;
    if (rawOffset != -1) tmpBytes = readBinaryHunk(rawuhs, offset, length);
    if (tmpBytes == null) {
      // This error would be at index-1, if not for getLoggedString()'s counter
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Could not read referenced raw bytes", logHeader+logLine+1, null);
    }

    newNode.setContent(tmpBytes, UHSNode.AUDIO);
    soundNode.addChild(newNode);

    return index-startIndex;
  }


  /**
   * Generates a blank UHSNode for spacing.
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int parseBlankNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    UHSNode newNode = new UHSNode("Blank");
      newNode.setContent("^^^", UHSNode.STRING);
      currentNode.addChild(newNode);

    index += innerCount;
    return index-startIndex;
  }


  /**
   * Generates a version UHSNode.
   * This is the version reported by the hint file.
   * It may be inaccurate, blank, or conflict with what is claimed in the info node.
   *
   * <pre> # version
   * title
   * sentence
   * sentence
   * sentence</pre>
   *
   * <br />Illustrative UHS: <i>Frankenstein: Through the Eyes of the Monster (blank version)</i>
   * <br />Illustrative UHS: <i>Kingdom O' Magic (blank version)</i>
   * <br />Illustrative UHS: <i>Out of This World (blank version)</i>
   * <br />Illustrative UHS: <i>Spycraft: The Great Game (blank version)</i>
   * <br />Illustrative UHS: <i>Star Control 3 (blank version)</i>
   * <br />Illustrative UHS: <i>System Shock (blank version)</i>
   * <br />Illustrative UHS: <i>The Bizarre Adventures of Woodruff (blank version)</i>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int parseVersionNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    String breakChar = " ";

    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    UHSNode versionNode = new UHSNode("Version");
      versionNode.setContent("Version: "+ getLoggedString(uhsFileArray, index), UHSNode.STRING);
      parseTextEscapes(versionNode);
      versionNode.setId(startIndex);
      currentNode.addChild(versionNode);
      rootNode.addLink(versionNode);
    index++;
    innerCount--;

    StringBuffer tmpContent = new StringBuffer();
    UHSNode newNode = new UHSNode("VersionData");

    for (int j=0; j < innerCount; j++) {
      if (tmpContent.length() > 0) tmpContent.append(breakChar);
      tmpContent.append( getLoggedString(uhsFileArray, index+j) );
    }
    newNode.setContent(tmpContent.toString(), UHSNode.STRING);
    parseTextEscapes(newNode);
    versionNode.addChild(newNode);

    index += innerCount;
    return index-startIndex;
  }


  /**
   * Generates an info UHSNode.
   *
   * <pre> # info
   * -
   * length=#######
   * date=DD-Mon-YY
   * time=24:00:00
   * author=name
   * publisher=name
   * copyright=sentence
   * copyright=sentence
   * copyright=sentence
   * >sentence
   * >sentence
   * >sentence</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int parseInfoNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    String breakChar = " ";

    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    UHSNode infoNode = new UHSNode("Info");
      infoNode.setContent("Info: "+ getLoggedString(uhsFileArray, index), UHSNode.STRING);
      infoNode.setId(startIndex);
      currentNode.addChild(infoNode);
      rootNode.addLink(infoNode);
    index++;
    innerCount--;

    if (innerCount > 0) {
      StringBuffer lengthBuf = new StringBuffer();
      StringBuffer dateBuf = new StringBuffer();
      StringBuffer timeBuf = new StringBuffer();
      StringBuffer authorBuf = new StringBuffer();
      StringBuffer publisherBuf = new StringBuffer();
      StringBuffer copyrightBuf = new StringBuffer();
      StringBuffer authorNoteBuf = new StringBuffer();
      StringBuffer gameNoteBuf = new StringBuffer();
      StringBuffer noticeBuf = new StringBuffer();

      StringBuffer unknownBuf = new StringBuffer();
      StringBuffer[] buffers = new StringBuffer[] {lengthBuf, dateBuf, timeBuf, authorBuf, publisherBuf, copyrightBuf, authorNoteBuf, gameNoteBuf, noticeBuf, unknownBuf};

      StringBuffer tmpContent = new StringBuffer();
      StringBuffer currentBuffer = null;
      UHSNode newNode = new UHSNode("InfoData");

      for (int j=0; j < innerCount; j++) {
        tmp = getLoggedString(uhsFileArray, index+j);
        if (tmp.startsWith("copyright") || tmp.startsWith("notice") || tmp.startsWith("author-note") || tmp.startsWith("game-note") || tmp.startsWith(">")) breakChar = " ";
        else breakChar = "\n";

        if (tmp.startsWith("length=")) {
          currentBuffer = lengthBuf;
          //tmp = tmp.substring(7);
        }
        else if (tmp.startsWith("date=")) {
          currentBuffer = dateBuf;
          //tmp = tmp.substring(5);
        }
        else if (tmp.startsWith("time=")) {
          currentBuffer = timeBuf;
          //tmp = tmp.substring(5);
        }
        else if (tmp.startsWith("author=")) {
          currentBuffer = authorBuf;
          //tmp = tmp.substring(7);
        }
        else if (tmp.startsWith("publisher=")) {
          currentBuffer = publisherBuf;
          //tmp = tmp.substring(10);
        }
        else if (tmp.startsWith("copyright=")) {
          currentBuffer = copyrightBuf;
          if (currentBuffer.length() == 0) {currentBuffer.append("copyright="); breakChar = "";}
          tmp = tmp.substring(10);
        }
        else if (tmp.startsWith("author-note=")) {
          currentBuffer = authorNoteBuf;
          if (currentBuffer.length() == 0) {currentBuffer.append("author-note="); breakChar = "";}
          tmp = tmp.substring(12);
        }
        else if (tmp.startsWith("game-note=")) {
          currentBuffer = gameNoteBuf;
          if (currentBuffer.length() == 0) {currentBuffer.append("game-note="); breakChar = "";}
          tmp = tmp.substring(10);
        }
        else if (tmp.startsWith(">")) {
          currentBuffer = noticeBuf;
          tmp = tmp.substring(1);
        }
        else {
          currentBuffer = unknownBuf;
          if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, this, "Unknown Info hunk line: "+ tmp, logHeader+logLine+1, null);
        }

        if (currentBuffer.length() > 0) currentBuffer.append(breakChar);
        currentBuffer.append(tmp);
      }

      for (int i=0; i < buffers.length; i++) {
        if (buffers[i].length() == 0) continue;
        if (tmpContent.length() > 0) {
          tmpContent.append("\n");
          if (i == 5 || i == 6 || i == 7 || i == 8) tmpContent.append("\n");
        }
        tmpContent.append(buffers[i]);
      }

      newNode.setContent(tmpContent.toString(), UHSNode.STRING);
      infoNode.addChild(newNode);
    }

    index += innerCount;
    return index-startIndex;
  }


  /**
   * Generates an incentive UHSNode.
   * <br />This node lists IDs to show/block if the reader is unregistered.
   * <br />
   * <br />The list is a space separated string of numbers, each with 'Z' or 'A' appended.
   * <br />'Z' means hide in registered readers, but show in unregistered ones.
   * <br />'A' means deny access in unregistered readers.
   * <br />In some files, there is no list, and the node only occupies 2 lines.
   *
   * <pre> # incentive
   * -
   * ID list (encrypted)</pre>
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int parseIncentiveNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    UHSNode incentiveNode = new UHSNode("Incentive");
      incentiveNode.setContent("Incentive: "+ getLoggedString(uhsFileArray, index), UHSNode.STRING);
      incentiveNode.setId(startIndex);
      currentNode.addChild(incentiveNode);
      rootNode.addLink(incentiveNode);
    index++;
    innerCount--;

    if (innerCount > 0) {
      tmp = getLoggedString(uhsFileArray, index);
      index++;
      UHSNode newNode = new UHSNode("IncentiveData");
        newNode.setContent(decryptNestString(tmp, key), UHSNode.STRING);
        incentiveNode.addChild(newNode);
    }

    return index-startIndex;
  }


  /**
   * Generates a stand-in UHSNode for an unknown hunk.
   *
   * @param uhsFileArray array of all available lines in the file
   * @param rawuhs array of raw bytes at the end of the file
   * @param rawOffset offset to the raw bytes from the beginning of the file
   * @param rootNode an existing root node
   * @param currentNode an existing node to add children to
   * @param key this file's hint decryption key
   * @param startIndex the line number to start parsing from
   * @return the number of lines consumed from the file in parsing children
   */
  public int parseUnknownNode(ArrayList uhsFileArray, byte[] rawuhs, long rawOffset, UHSRootNode rootNode, UHSNode currentNode, int[] key, int startIndex) {
    int index = startIndex;
    String tmp = getLoggedString(uhsFileArray, index);
    index++;
    int innerCount = Integer.parseInt(tmp.substring(0, tmp.indexOf(" "))) - 1;

    if (errorHandler != null) errorHandler.log(UHSErrorHandler.INFO, this, "Unknown Hunk: "+ tmp, logHeader+logLine+1, null);

    UHSNode newNode = new UHSNode("Unknown");
      newNode.setContent("^UNKNOWN HUNK^", UHSNode.STRING);
      currentNode.addChild(newNode);

    index += innerCount;
    return index-startIndex;
  }


  /**
   * Recursively prints the indented contents of a node and its children.
   *
   * @param currentNode a node to start printing from
   * @param indent indention prefix
   * @param spacer indention padding with each level
   * @param outStream a stream to print to
   */
  public void printNode(UHSNode currentNode, String indent, String spacer, PrintStream outStream) {
    int id = currentNode.getId();
    String idStr = (id==-1?"":"^"+id+"^: ");
    String linkStr = (!currentNode.isLink()?"":" (^Link to "+ currentNode.getLinkTarget() +"^)");

    if (currentNode.getContentType() == UHSNode.STRING)
      outStream.println(indent + idStr + currentNode.getContent() + linkStr);
    else if (currentNode.getContentType() == UHSNode.IMAGE)
      outStream.println(indent + idStr +"^IMAGE^"+ linkStr);
    else if (currentNode.getContentType() == UHSNode.AUDIO)
      outStream.println(indent + idStr +"^AUDIO^"+ linkStr);

    for (int i=0; i < currentNode.getChildCount(); i++) {
      printNode(currentNode.getChild(i), indent+spacer, spacer, outStream);
    }
  }


  /**
   * Reads some raw bytes originally from the end of a UHS file.
   * <br />Images, comments, sounds, etc., are stored there.
   *
   * This offset is relative to the start of the raw bytes, not the beginning of the file.
   *
   * @param rawuhs array of bytes at the end of the file (after 0x1Ah)
   * @param offset starting index within the array (must be less than Integer.MAX_VALUE)
   * @param length the desired number of bytes to retrieve
   * @return the relevant bytes, or null if the offset or length is invalid
   */
  public byte[] readBinaryHunk(byte[] rawuhs, long offset, int length) {
    if (offset < 0 || offset > Integer.MAX_VALUE || length < 0 || length > rawuhs.length)
      return null;
    byte[] result = new byte[length];
    for (int i=0; i < length; i++) {
      result[i] = rawuhs[(int)offset+i];
    }
    return result;
  }


  private String getLoggedString(ArrayList uhsFileArray, int n) {
    logLine = n;
    return (String)uhsFileArray.get(n);
  }
}
