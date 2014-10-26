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
import java.util.*;
import java.io.*;
import javax.sound.sampled.*;


/**
 * A simple swing component to play sounds.
 */
public class MinimalSoundPlayer extends JPanel {
  private static String playText = ">";
  private static String stopText = "X";

  private byte[] bytes = null;
  private Clip clip = null;
  private int duration = 0;
  private int position = 0;
  private javax.swing.Timer timer = null;

  private JButton playBtn = new JButton(playText);
  private JSlider slider = new JSlider(0, 0, 0);


  public MinimalSoundPlayer(byte[] b) {
    super(new BorderLayout());
    JPanel ctrlPanel = new JPanel();
      ctrlPanel.setLayout(new BoxLayout(ctrlPanel, BoxLayout.X_AXIS));
        ctrlPanel.add(playBtn);
        ctrlPanel.add(Box.createHorizontalStrut(10));
        ctrlPanel.add(slider);
    this.add(ctrlPanel, BorderLayout.NORTH);


    try {
      ByteArrayInputStream bs = new ByteArrayInputStream(b);
      AudioInputStream ain = AudioSystem.getAudioInputStream(bs);
      try {
        //This used to be the entirety of the try{...}
        //DataLine.Info info = new DataLine.Info(Clip.class, ain.getFormat());
        //clip = (Clip) AudioSystem.getLine(info);
        //clip.open(ain);

        AudioFormat baseFormat = ain.getFormat();
        if (baseFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
          AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
          AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, ain);
          int frameLength = (int)decodedStream.getFrameLength();
          int frameSize = decodedFormat.getFrameSize();
          DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat, frameLength * frameSize);
          clip = (Clip) AudioSystem.getLine(info);
          clip.open(ain);
        } else {
          DataLine.Info info = new DataLine.Info(Clip.class, baseFormat, AudioSystem.NOT_SPECIFIED);
          clip = (Clip) AudioSystem.getLine(info);
          clip.open(ain);
        }
      }
      catch(LineUnavailableException e) {e.printStackTrace();}
      finally {ain.close();}
      bytes = b;
      duration = (int)(clip.getMicrosecondLength()/1000);
    }
    catch (UnsupportedAudioFileException e) {e.printStackTrace();}
    catch (IOException e) {e.printStackTrace();}


    if (bytes != null) {
      slider.setMaximum(duration);
      playBtn.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (!clip.isActive()) start();
          else stop();
        }
      });
      slider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          if (slider.getValue() != position) {
            seek(slider.getValue());
          }
        }
      });
      timer = new javax.swing.Timer(100, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (clip.isActive()) {
            position = (int)(clip.getMicrosecondPosition()/1000);
            slider.setValue(position);
          }
          else {
            stop();
            slider.setValue(0);
          }
        }
      });
    } else {
      playBtn.setEnabled(false);
    }
  }

  /**
   * Halts playback.
   */
  public void stop() {
    clip.stop();
    timer.stop();
    playBtn.setText(playText);
  }

  /**
   * Begins/continues playback.
   */
  public void start() {
    clip.start();
    timer.start();
    playBtn.setText(stopText);
  }

  /**
   * Jumps to a new position in the sound.
   *
   * @param newPos the desired position
   */
  public void seek(int newPos) {
    if (newPos < 0 || newPos > duration) return;
    position = newPos;
    clip.setMicrosecondPosition(position * 1000);
    slider.setValue(position);
  }

  /**
   * Gets the sound this component is playing.
   *
   * @return the sound
   */
  public byte[] getSound() {return bytes;}
}
