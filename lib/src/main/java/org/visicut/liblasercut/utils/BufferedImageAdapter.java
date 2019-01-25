/**
 * This file is part of LibLaserCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 * <p>
 * LibLaserCut is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibLaserCut is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with LibLaserCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.visicut.liblasercut.utils;

import org.visicut.liblasercut.GreyscaleRaster;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class BufferedImageAdapter implements GreyscaleRaster {
  public interface BufferedImage {
    int getRGB(int x, int y);

    void setRGB(int x, int y, int color);

    int getWidth();

    int getHeight();
  }

  public static class Color {
    private int r;
    private int g;
    private int b;

    public Color(int r, int g, int b) {
      this.setRGB(r, g, b);
    }

    public Color(int color) {
      this.setRGB(color);
    }

    int getRGB() {
      return (r & 0xFF) << 16 + (g & 0xFF) << 8 + (b & 0xFF);
    }

    int getRed() {
      return r;
    }

    int getGreen() {
      return g;
    }

    int getBlue() {
      return b;
    }

    void setRGB(int r, int g, int b) {
      this.r = min(255, max(r, 0));
      this.g = min(255, max(g, 0));
      this.b = min(255, max(b, 0));
    }

    void setRGB(int color) {
      this.r = min(255, max(color >> 16, 0));
      this.g = min(255, max(color >> 8, 0));
      this.b = min(255, max(color, 0));
    }
  }

  private BufferedImage img;
  private int colorShift = 0;
  private boolean invertColors = false;

  public BufferedImageAdapter(BufferedImage img) {
    this(img, false);
  }

  public BufferedImageAdapter(BufferedImage img, boolean invertColors) {
    this.img = img;
    this.invertColors = invertColors;
  }

  public void setColorShift(int cs) {
    this.colorShift = cs;
  }

  public int getColorShift() {
    return this.colorShift;
  }

  public int getGreyScale(int x, int line) {
    Color c = new Color(img.getRGB(x, line));
    int value = colorShift + (int) (0.3 * c.getRed() + 0.59 * c.getGreen() + 0.11 * c.getBlue());
    return invertColors ? 255 - max(min(value, 255), 0) : max(min(value, 255), 0);
  }

  public void setGreyScale(int x, int y, int grey) {
    Color c = new Color(grey, grey, grey);
    img.setRGB(x, y, c.getRGB());
  }

  public int getWidth() {
    return img.getWidth();
  }

  public int getHeight() {
    return img.getHeight();
  }
}
