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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;

import org.openuhs.core.*;
import org.openuhs.reader.*;
import org.openuhs.downloader.*;
import org.openuhs.*;


public class SettingsPanel extends JPanel implements ActionListener {
  private ArrayList appliablePanels = new ArrayList();
  private JPanel sectionsPanel = null;
  GridBagConstraints sectionC = null;
  private JButton applyBtn = null;


  public SettingsPanel() {
    super(new BorderLayout());

    JPanel paddingPanel = new JPanel(new BorderLayout());
      sectionsPanel = new JPanel(new GridBagLayout());
        sectionC = new GridBagConstraints();
          sectionC.fill = GridBagConstraints.HORIZONTAL;
          sectionC.weightx = 1.0;
          sectionC.weighty = 0;
          sectionC.insets = new Insets(0, 0, 8, 0);
          sectionC.gridwidth = GridBagConstraints.REMAINDER;  //End Row
          sectionC.gridy = 1;
          paddingPanel.add(sectionsPanel, BorderLayout.NORTH);
      JScrollPane scrollPane = new JScrollPane(paddingPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scrollPane, BorderLayout.CENTER);

    JPanel applyPanel = new JPanel();
      applyPanel.setLayout(new BoxLayout(applyPanel, BoxLayout.X_AXIS));
      applyPanel.add(Box.createHorizontalGlue());
      applyBtn = new JButton("Apply");
        applyPanel.add(applyBtn);
      applyPanel.add(Box.createHorizontalGlue());
      this.add(applyPanel, BorderLayout.SOUTH);

    applyBtn.addActionListener(this);
  }


  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == applyBtn) {
      for (int i=0; i < appliablePanels.size(); i++) {
        ((AppliablePanel)appliablePanels.get(i)).apply();
      }
    }
  }


  public void clear() {
    appliablePanels.clear();
    sectionsPanel.removeAll();
    sectionC.gridy = 1;
    this.validate();
    this.repaint();
  }


  public void addSection(String sectionName, AppliablePanel sectionPanel) {
    appliablePanels.add(sectionPanel);

    JPanel tmpPanel = new JPanel(new BorderLayout());
      tmpPanel.setBorder(BorderFactory.createTitledBorder(sectionName));
      tmpPanel.add(sectionPanel);
      sectionsPanel.add(tmpPanel, sectionC);
    sectionC.gridy++;

    this.validate();
    this.repaint();
  }
}
