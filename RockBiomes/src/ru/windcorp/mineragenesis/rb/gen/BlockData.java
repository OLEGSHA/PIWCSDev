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
package ru.windcorp.mineragenesis.rb.gen;

import ru.windcorp.mineragenesis.request.ChunkData;

/**
 * @author Javapony
 *
 */
public class BlockData {
	
	public final ColumnData column = new ColumnData();
	
	public int yInt;
	
	public short original;
	
	public double currentDensity = Double.NaN;
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BlockData[XZY: (" + column.xInt + "; " + column.zInt + "; " + yInt + "); Original: "
				+ ChunkData.getId(original) + ":" + ChunkData.getMeta(original) + "; Density: " + currentDensity + "]";
	}

}
