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

import java.io.PrintStream;

import org.openuhs.core.*;


/**
 * A simple logger that prints to a stream.
 */
public class DefaultUHSErrorHandler implements UHSErrorHandler {
  private PrintStream[] outStreams = null;


  /**
   * Creates an error handler.
   *
   * @param out a stream to print to (e.g., System.err)
   */
  public DefaultUHSErrorHandler(PrintStream out) {
    this(new PrintStream[] {out});
  }

  /**
   * Creates an error handler.
   *
   * @param out multiple streams to print to (e.g., System.err)
   */
  public DefaultUHSErrorHandler(PrintStream[] out) {
    if (out != null && out.length > 0) outStreams = (PrintStream[])out.clone();
  }


  public void log(int severity, Object source, String message, int line, Exception e) {
    if (outStreams == null || outStreams.length == 0) return;

    String indent = "  ";

    StringBuffer result = new StringBuffer();

    if (line > 0) {
      if (message != null && message.length() > 0) {
        message = message +" (line "+ line +")";
      } else {
        message = "Something happened on line "+ line;
      }
    }

    String severityPrefix = null;
    if (severity == UHSErrorHandler.ERROR) severityPrefix = "Error:   ";
    else if (severity == UHSErrorHandler.INFO) severityPrefix = "Info:    ";
    else severityPrefix = "?:      ";

    if (message != null && message.length() > 0) {
      String[] chunks = message.split("\n");
      result.append(severityPrefix).append(chunks[0]);
      for (int i=1; i < chunks.length; i++) {
        result.append("\n         ").append(chunks[i]);
      }
    }

    if (e != null) {
      result.append((result.length()>0?"\n":"")).append("Problem: ").append(e.toString());

      StackTraceElement[] tmpStack = e.getStackTrace();
      for (int i=0; i < tmpStack.length; i++) {
        result.append("\n         ").append(indent).append(tmpStack[i].toString());
      }

      Throwable cause = e.getCause();
      while (cause != null) {
        result.append("\nCause:   ").append(cause.toString());

        tmpStack = cause.getStackTrace();
        for (int i=0; i < tmpStack.length; i++) {
          result.append("\n         ").append(indent).append(tmpStack[i].toString());
        }

        cause = cause.getCause();
      }
    }

    for (int i=0; i < outStreams.length; i++) {
      synchronized (outStreams[i]) {
        outStreams[i].println(result.toString());
      }
    }
  }


  /**
   * Prints the current date. An optional call on startup.
   */
  public void logWelcomeMessage() {
    StringBuffer result = new StringBuffer();
    result.append("Started: "+ new java.util.Date().toString());


    for (int i=0; i < outStreams.length; i++) {
      synchronized (outStreams[i]) {
        outStreams[i].println(result.toString());
      }
    }
  }
}
