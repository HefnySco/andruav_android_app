package com.andruav.util.polygon;

/**
 * Point on 2D landscape
 * https://github.com/sromku/polygon-contains-point
 * @author Roman Kushnarenko (sromku@gmail.com)</br>
 */
public class Point
{
	public Point(double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	public final double x;
	public final double y;


	@Override
	public String toString()
	{
		return String.format("(%.2f,%.2f)", x, y);
	}
}