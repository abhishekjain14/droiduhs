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

import java.awt.Color;


/**
 * Catalog info about a UHS file.
 */
public class DownloadableUHS {
  private static String[] MONTHS = new String[] {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

  private String title = "";
  private String url = "";
  private String name = "";
  private String date = "";
  private String compressedSize = "";
  private String fullSize = "";
  private Color color = null;


  public DownloadableUHS() {}


  public String getTitle() {return title;}

  public void setTitle(String s) {
    if (s==null) title = "";
    else title = s;
  }


  public String getUrl() {return url;}

  public void setUrl(String s) {
    if (s==null) url = "";
    else url = s;
  }


  public String getName() {return name;}

  public void setName(String s) {
    if (s==null) name = "";
    else name = s;
  }


  public String getDate() {return date;}

  public void setDate(String s) {
    if (s==null) date = "";

    String tmpDate = fixDate(s);
    if (tmpDate != null) date = tmpDate;
    else date = s;
  }

  private String fixDate(String s) {
    if (!s.matches("^[0-3][0-9]-[A-Z][a-z][a-z]-[0-9][0-9]$")) return null;

    String[] parts = s.split("-");

    int month = -1;
    for (int i=0; i < MONTHS.length; i++) {
      if (parts[1].equals(MONTHS[i])) {
        month = i+1;
        break;
      }
    }
    if (month == -1) return null;

    if (parts[2].matches("^[7-9].")) parts[2] = "19"+ parts[2];
    else parts[2] = "20"+ parts[2];

    return parts[2] +"-"+ (month<10?"0":"") + month +"-"+ parts[0];
  }

  public boolean hasFixedDate() {return getDate().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}");}


  public String getCompressedSize() {return compressedSize;}

  public void setCompressedSize(String s) {
    if (s==null) compressedSize = "";
    else compressedSize = s;
  }


  public String getFullSize() {return fullSize;}

  public void setFullSize(String s) {
    if (s==null) fullSize = "";
    else fullSize = s;
  }


  public Color getColor() {return color;}

  public void setColor(Color c) {color = c;}
}
