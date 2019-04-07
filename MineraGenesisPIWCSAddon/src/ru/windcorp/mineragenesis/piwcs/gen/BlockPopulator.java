/* 
 * PIWCS addon for MineraGenesis Minecraft mod
 * Copyright (C) 2019  Javapony and contributors
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
 */
package ru.windcorp.mineragenesis.piwcs.gen;

import java.util.Random;

public interface BlockPopulator {
	
	public static final short NOT_POPULATED = (short) ~0;
	
	void populate(
			int dimension, int chunkX, int chunkZ,
			int x, int z, int y,
			short orignalMgId,
			Random random,
			BlockCollector[] output, int oindex);

}
