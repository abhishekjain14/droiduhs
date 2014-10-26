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
 * An interface for wrapping loggers.
 */
public interface UHSErrorHandler {
  public static final int ERROR = 0;
  public static final int INFO = 1;


  /**
   * Logs an event.
   * It would be wise to synchronize on a lock object.
   *
   * @param severity ERROR or INFO
   * @param source the responsible object, or null
   * @param message
   * @param line line number, or 0 for none
   * @param e an exception, or null
   */
  public void log(int severity, Object source, String message, int line, Exception e);
}
