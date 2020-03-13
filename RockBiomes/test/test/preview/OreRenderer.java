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

import ru.windcorp.mineragenesis.rb.gen.Deposit;
import ru.windcorp.mineragenesis.rb.gen.DimensionComplex;
import ru.windcorp.mineragenesis.rb.gen.RockBiome;
import ru.windcorp.mineragenesis.rb.gen.RockBiomeType;

/**
 * @author Javapony
 *
 */
public class OreRenderer extends Renderer {
	
	private final Map<Deposit, Integer> colors = new HashMap<>();

	/**
	 * @param dim
	 */
	public OreRenderer(DimensionComplex dim) {
		super(dim);
		
		for (RockBiomeType rbt : dim.getRegolith().getAll()) {
			for (RockBiome rb : rbt.getAll()) {
				for (Deposit dep : rb.getDeposits()) {
					colors.put(dep, -1);
				}
			}
		}
		
		float step = 1.0f / colors.size();
		float hue = 0;
		
		for (Map.Entry<Deposit, Integer> rb : colors.entrySet()) {
			rb.setValue(Color.HSBtoRGB(hue, 0.75f, 0.75f));
			hue += step;
		}
	}

	/**
	 * @see test.preview.Renderer#getColorAt(double, double)
	 */
	@Override
	public int getColorAt(double x, double z) {
		RockBiome rb = getDimension().getRegolithBiomeAt(Math.floor(x / 16), Math.floor(z / 16));
		
		for (Deposit dep : rb.getDeposits()) {
			double density = dep.getColumnDensity(x, z);
			if (density > 0) return fade(0x000000, colors.get(dep), density);
		}
		
		return 0x000000;
	}

	/**
	 * @see test.preview.Renderer#getTooltipAt(double, double)
	 */
	@Override
	public String getTooltipAt(double x, double z) {
		RockBiome rb = getDimension().getRegolithBiomeAt(Math.floor(x / 16), Math.floor(z / 16));
		
		for (Deposit dep : rb.getDeposits()) {
			double density = dep.getColumnDensity(x, z);
			if (density > 0) return dep.getName() + " (" + density + ")";
		}
		
		return "No deposits";
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Ores of " + getDimension();
	}

}
