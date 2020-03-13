/*
 * MineraGenesis Rock Biomes Addon
 * Copyright (C) 2019  Javapony/OLEGSHA
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.preview;

import ru.windcorp.mineragenesis.rb.gen.Dimension;

/**
 * @author Javapony
 *
 */
public abstract class Renderer {
	
	private final Dimension dim;

	/**
	 * @param dim
	 */
	public Renderer(Dimension dim) {
		this.dim = dim;
	}

	public abstract int getColorAt(double x, double z);
	public abstract String getTooltipAt(double x, double z);

	public Dimension getDimension() {
		return dim;
	}

	protected static int fade(int colorA, int colorB, double coefB) {
		if (coefB <= 0) return colorA;
		else if (coefB >= 1) return colorB;
		
		double coefA = 1 - coefB;
		
		int bA = colorA & 0xFF;
		int bB = colorB & 0xFF;
		int b = (int) (bA * coefA + bB * coefB);
		
		int gA = (colorA >>>= 8) & 0xFF;
		int gB = (colorB >>>= 8) & 0xFF;
		int g = (int) (gA * coefA + gB * coefB);
		
		int rA = (colorA >>> 8) & 0xFF;
		int rB = (colorB >>> 8) & 0xFF;
		int r = (int) (rA * coefA + rB * coefB);
		
		return r << 16 | g << 8 | b;
	}
	
}
