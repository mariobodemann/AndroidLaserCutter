/**
 * This file is part of LibLaserCut.
 * Copyright (C) 2011 - 2013 Thomas Oster <thomas.oster@rwth-aachen.de>
 * RWTH Aachen University - 52062 Aachen, Germany
 *
 *     LibLaserCut is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     LibLaserCut is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with LibLaserCut.  If not, see <http://www.gnu.org/licenses/>.
 **/
package org.visicut.liblasercut;

import org.visicut.liblasercut.platform.Point;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Thomas Oster <thomas.oster@rwth-aachen.de>
 */
public class Raster3dPart extends JobPart
{

  private GreyscaleRaster image = null;
  private LaserProperty property = null;
  protected Point start = null;
  private double resolution = 500;

  public Raster3dPart(GreyscaleRaster image, LaserProperty laserProperty, Point offset, double resolution)
  {
    this.image = image;
    this.resolution = resolution;
    this.property = laserProperty;
    this.start = offset;
  }

  @Override
  public double getDPI()
  {
      return resolution;
  }

  @Override
  public int getMinX()
  {
    return start.x;
  }

  @Override
  public int getMaxX()
  {
    return start.x+image.getWidth();
  }

  @Override
  public int getMinY()
  {
    return start.y;
  }

  @Override
  public int getMaxY()
  {
    return start.y + image.getHeight();
  }

  /**
   * Returns the upper left point of the given raster
   *
   * @param raster the raster which upper left corner is to determine
   * @return
   */
  public Point getRasterStart()
  {
    return this.start;
  }

  /**
   * Returns one line of the given rasterpart every byte represents one pixel
   * and the value corresponds to the raster power
   *
   * @param raster
   * @param line
   * @return
   */
  public List<Byte> getRasterLine(int line)
  {
    List<Byte> result = new LinkedList<Byte>();
    for (int x = 0; x < image.getWidth(); x++)
    {
      //TOTEST: Black white (byte converssion)
      result.add((byte) image.getGreyScale(x, line));
    }
    return result;
  }

  public int getRasterWidth()
  {
    return this.image.getWidth();
  }

  public int getRasterHeight()
  {
    return this.image.getHeight();
  }

  public LaserProperty getLaserProperty()
  {
    return this.property;
  }

  public List<Byte> getInvertedRasterLine(int line)
  {
    List<Byte> result = new LinkedList<Byte>();
    for (int x = 0; x < image.getWidth(); x++)
    {
      //TOTEST: Black white (byte converssion)
      result.add((byte) (255 - image.getGreyScale(x, line)));
    }
    return result;
  }
}
