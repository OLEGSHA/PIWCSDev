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

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import ru.windcorp.mineragenesis.rb.gen.Dimension;
import ru.windcorp.mineragenesis.rb.gen.RockBiome;

/**
 * @author Javapony
 *
 */
public class RockBiomeRenderer extends Renderer {
	
	private final Map<RockBiome, Integer> colors = new HashMap<>();

	/**
	 * @param dim
	 */
	public RockBiomeRenderer(Dimension dim) {
		super(dim);
		
		dim.forEachRegolithBiome(rb -> colors.put(rb, -1));
		
		float step = 1.0f / colors.size();
		float hue = 0;
		
		for (Map.Entry<RockBiome, Integer> rb : colors.entrySet()) {
			rb.setValue(Color.HSBtoRGB(hue, 0.75f, 0.75f));
			hue += step;
		}
	}

	/**
	 * @see test.preview.Renderer#getColorAt(double, double)
	 */
	@Override
	public int getColorAt(double x, double z) {
		return colors.get(getDimension().getRegolithBiomeAt(Math.floor(x / 16), Math.floor(z / 16)));
	}
	
	/**
	 * @see test.preview.Renderer#getTooltipAt(double, double)
	 */
	@Override
	public String getTooltipAt(double x, double z) {
		return getDimension().getRegolithBiomeAt(Math.floor(x / 16), Math.floor(z / 16)).getName();
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RockBiomes of " + getDimension();
	}

}
