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

package org.openuhs.downloader;

import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;

import org.openuhs.core.*;


/**
 * A collection of utility methods for downloading/saving files and parsing the official UHS catalog.
 */
public class UHSFetcher {
  public static String catalogUrl = "http://www.uhs-hints.com:80/cgi-bin/update.cgi";
  public static String userAgent = "UHSWIN/5.2";

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
   * Downloads a url and spawns a progress monitor.
   *
   * @param parentComponent a component to be the monitor's parent
   * @param message message describing the operation
   * @param address source url of the data
   * @return the downloaded data
   */
  public static byte[] fetchURL(final Component parentComponent, Object message, String address) {
    HttpURLConnection.setFollowRedirects(false);

    Exception exceptionObj = null;
    String exceptionMsg = null;
    try {
      URL url = new URL(address);
      URLConnection connection = url.openConnection();
      if (connection instanceof HttpURLConnection) {
        HttpURLConnection httpConnection = (HttpURLConnection)connection;
          httpConnection.addRequestProperty("User-Agent", userAgent);
          httpConnection.connect();
        int response = httpConnection.getResponseCode();
        if (response < 200 || response >= 300) {
          if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, null, "Could not fetch file. Response code: "+ response, 0, null);
          return null;
        }
        ProgressMonitorInputStream is = new ProgressMonitorInputStream(parentComponent, message, httpConnection.getInputStream());
        ProgressMonitor pm = is.getProgressMonitor();
          pm.setMaximum(httpConnection.getContentLength());
          pm.setMillisToDecideToPopup(300);
          pm.setMillisToPopup(100);
        int chunkSize = 1024;
        ArrayList bufs = new ArrayList();
        int tmpByte;
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(chunkSize);
        bufs.add(buf);
        while ((tmpByte = is.read()) != -1) {
          buf.put((byte)tmpByte);
          if (buf.remaining()==0) {
            buf = java.nio.ByteBuffer.allocate(chunkSize);
            bufs.add(buf);
          }
        }
        is.close();

        int bufCount = bufs.size();
        int bufTotalSize = chunkSize * bufCount;
        if (bufCount > 0)
          bufTotalSize -= ((java.nio.ByteBuffer)bufs.get(bufCount-1)).remaining();
        byte[] bytes = new byte[bufTotalSize];
        for (int i=0; i <= bufCount-2; i++) {
          buf = ((java.nio.ByteBuffer)bufs.get(i));
          buf.rewind();
          buf.get(bytes, i*chunkSize, chunkSize);
        }
        if (bufCount > 0) {
          buf = ((java.nio.ByteBuffer)bufs.get(bufCount-1));
          buf.flip();
          buf.get(bytes, (bufCount-1)*chunkSize, buf.remaining());
        }
        return bytes;
      }
    }
    catch (InterruptedIOException e) {/* user cancelled */}
    catch (UnknownHostException e) {
      exceptionObj = e;
      exceptionMsg = "Could not locate remote server";
    }
    catch(ConnectException e) {
      exceptionObj = e;
      exceptionMsg = "Could not connect to remote server";
    }
    catch (IOException e) {
      exceptionObj = e;
      exceptionMsg = "Could not download from server";
    }
    if (exceptionObj != null) {
      final Exception finalObj = exceptionObj;
      final String finalMsg = exceptionMsg;
      Runnable r = new Runnable() {
        public void run() {
          if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, null, finalMsg, 0, finalObj);
          JOptionPane.showMessageDialog(parentComponent, finalMsg +":\n "+ finalObj.getMessage(), "OpenUHS Cannot Continue", JOptionPane.ERROR_MESSAGE);
        }
      };
      EventQueue.invokeLater(r);
    }

    return null;
  }


  /**
   *
   * Downloads a catalog of available hint files.
   *
   * @param parentComponent a component to be the monitor's parent
   * @return an array of DownloadableUHS objects
   */
  public static ArrayList fetchCatalog(Component parentComponent) {
    ArrayList catalog = new ArrayList();

    byte[] responseBytes = fetchURL(parentComponent, "Fetching Catalog...", catalogUrl);
    if (responseBytes == null) return catalog;

    String response = new String(responseBytes);
    response = response.replaceAll("[\r\n]", "");

    int length = response.length();
    for (int i=0; i < length;) {
      String fileChunk = parseChunk(response, "<FILE>", "</FILE>", i);
      if (fileChunk == null) break;
      i = response.indexOf("<FILE>", i) + 6 + fileChunk.length() + 7;

      DownloadableUHS tmpUHS = new DownloadableUHS();
        tmpUHS.setTitle( parseChunk(fileChunk, "<FTITLE>", "</FTITLE>", 0) );

        tmpUHS.setUrl( parseChunk(fileChunk, "<FURL>", "</FURL>", 0) );

        tmpUHS.setName( parseChunk(fileChunk, "<FNAME>", "</FNAME>", 0) );

        tmpUHS.setDate( parseChunk(fileChunk, "<FDATE>", "</FDATE>", 0) );

        tmpUHS.setCompressedSize( parseChunk(fileChunk, "<FSIZE>", "</FSIZE>", 0) );

        tmpUHS.setFullSize( parseChunk(fileChunk, "<FFULLSIZE>", "</FFULLSIZE>", 0) );

      catalog.add(tmpUHS);
    }

    return catalog;
  }


  /**
   * Extracts a substring between known tokens.
   * <br />This is used to parse the catalog's xml-like markup.
   *
   * @return the substring, or null if the tokens are not present.
   */
  private static String parseChunk(String line, String prefix, String suffix, int fromIndex) {
    int start = line.indexOf(prefix, fromIndex);
    if (start == -1) return null;
    start += prefix.length();
    int end = line.indexOf(suffix, start);
    if (end == -1) return null;

    if (end - start < 0) return null;
    String tmp = line.substring(start, end);
    return tmp;
  }


  /**
   *
   * Downloads a hint file.
   *
   * @param parentComponent a component to be the monitor's parent
   * @param uhs hint to fetch
   * @return the downloaded data
   */
  public static byte[] fetchUHS(Component parentComponent, DownloadableUHS uhs) {
    byte[] fullBytes = null;

    if (errorHandler != null) errorHandler.log(UHSErrorHandler.INFO, null, "Fetching "+ uhs.getName() +"...", 0, null);
    byte[] responseBytes = fetchURL(parentComponent, "Fetching "+ uhs.getName() +"...", uhs.getUrl());
    if (responseBytes == null) return fullBytes;

    ZipInputStream is = new ZipInputStream(new ByteArrayInputStream(responseBytes));
    try {
      //No need for a while loop; only one file
      //  and contrary to the doc, zip errors can occur if there is no next entry
      ZipEntry ze;
      if ((ze = is.getNextEntry()) != null) {
        if (errorHandler != null) errorHandler.log(UHSErrorHandler.INFO, null, "Extracting "+ ze.getName() +"...", 0, null);

        int chunkSize = 1024;
        ArrayList bufs = new ArrayList();
        int tmpByte;
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(chunkSize);
        bufs.add(buf);
        while ((tmpByte = is.read()) != -1) {
          buf.put((byte)tmpByte);
          if (buf.remaining()==0) {
            buf = java.nio.ByteBuffer.allocate(chunkSize);
            bufs.add(buf);
          }
        }

        int bufCount = bufs.size();
        int bufTotalSize = chunkSize * bufCount;
        if (bufCount > 1)
          bufTotalSize -= ((java.nio.ByteBuffer)bufs.get(bufCount-1)).remaining();
        fullBytes = new byte[bufTotalSize];
        for (int i=0; i <= bufCount-2; i++) {
          buf = ((java.nio.ByteBuffer)bufs.get(i));
          buf.rewind();
          buf.get(fullBytes, i*chunkSize, chunkSize);
        }
        if (bufCount > 1) {
          buf = ((java.nio.ByteBuffer)bufs.get(bufCount-1));
          buf.flip();
          buf.get(fullBytes, (bufCount-1)*chunkSize, buf.remaining());
        }

      }
      is.close();
    }
    catch (ZipException e) {
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, null, "Could not extract downloaded hints for "+ uhs.getName(), 0, e);
    }
    catch (IOException e) {
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, null, "Could not extract downloaded hints for "+ uhs.getName(), 0, e);
    }
    finally {
      try{is.close();}
      catch (Exception f) {}
    }

    return fullBytes;
  }


  /**
   *
   * Saves an array of bytes to a file.
   * <br />If the destination exists, a confirmation dialog will appear.
   *
   * @param parentComponent a component to be the parent of any error popups
   * @param path path to a file to save
   * @param bytes data to save
   * @return true if successful, false otherwise
   */
  public static boolean saveBytes(final Component parentComponent, String path, byte[] bytes) {
    boolean result = false;
    File destFile = new File(path);
    if (bytes == null || new File(destFile.getParent()).exists() == false) return result;

    if (destFile.exists()) {
      final String message = "A file named "+ destFile.getName() +" exists. Overwrite?";
      final int[] delayedChoice = new int[] {JOptionPane.NO_OPTION};
      Runnable r = new Runnable() {
        public void run() {
          delayedChoice[0] = JOptionPane.showConfirmDialog(parentComponent, message, "Overwrite?", JOptionPane.YES_NO_OPTION);
        }
      };
      waitForEventPrompt(r);
      if (delayedChoice[0] == JOptionPane.NO_OPTION) return result;
    }

    BufferedOutputStream dest = null;
    try {
      int chunkSize = 1024;
      FileOutputStream fos = new FileOutputStream(path);
      dest = new BufferedOutputStream(fos, chunkSize);
      dest.write(bytes);
      dest.flush();
      dest.close();

      result = true;
    }
    catch (IOException e) {
      if (errorHandler != null) errorHandler.log(UHSErrorHandler.ERROR, null, "Could not write to "+ path, 0, e);
    }
    finally {
      try{if (dest != null) dest.close();}
      catch (Exception f) {}
    }

    return result;
  }


  /**
   * Gets the User-Agent to use for http requests.
   *
   * @return the agent text
   * @see #setUserAgent(String) setUserAgent(String)
   */
  public static String getUserAgent() {
    return userAgent;
  }

  /**
   * Sets the User-Agent to use for http requests.
   *
   * The default is "UHSWIN/5.2".
   *
   * @param s the agent text
   * @see #getUserAgent() getUserAgent()
   */
  public static void setUserAgent(String s) {
    if (s != null) userAgent = s;
  }


  /**
   * Gets the url of the hint catalog.
   *
   * @return the url
   * @see #setCatalogUrl(String) setCatalogUrl(String)
   */
  public static String getCatalogUrl() {
    return catalogUrl;
  }

  /**
   * Gets the url of the hint catalog.
   *
   * The default is "http://www.uhs-hints.com:80/cgi-bin/update.cgi".
   *
   * @param s the url
   * @see #getCatalogUrl() getCatalogUrl()
   */
  public static void setCatalogUrl(String s) {
    if (s != null) catalogUrl = s;
  }


  private static void waitForEventPrompt(final Runnable r) {
    if (EventQueue.isDispatchThread()) {
      r.run();
    } else {
      final int[] doneLock = new int[] {0};
      Runnable wrapper = new Runnable() {
        public void run() {
          r.run();
          synchronized (doneLock) {doneLock[0] = 1;}
        }
      };

      EventQueue.invokeLater(wrapper);
      synchronized(doneLock) {
        while (doneLock[0] != 1) {
          try {doneLock.wait(500);}
          catch(InterruptedException e) {}
        }
      }
    }
  }
}
