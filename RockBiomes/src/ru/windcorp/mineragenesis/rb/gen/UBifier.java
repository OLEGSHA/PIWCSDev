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

import static ru.windcorp.mineragenesis.request.ChunkData.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;
import ru.windcorp.mineragenesis.rb.config.MGID;
import ru.windcorp.mineragenesis.rb.config.VoidVerb;

/**
 * @author Javapony
 *
 */
public class UBifier {
	
	private static final short NOT_ASSIGNED = AIR_MGID;
	private static final int MAP_LENGTH = 1 << METADATA_SIZE;
	
	private static short[][] maps = null;
	
	public static short ubify(short mgid, short undergroundBiome) {
		if (undergroundBiome < 0) return mgid;
		if (maps == null) return mgid;
		
		int id = getId(mgid);
		if (id >= maps.length) return mgid;
		
		short[] map = maps[id];
		if (map == null) return mgid;
		
		short ubBase = map[getMeta(mgid)];
		if (ubBase == NOT_ASSIGNED) return mgid;
		return (short) (ubBase + undergroundBiome);
	}
	
	public static class Setter extends VoidVerb {
		public Setter() {
			super("ubify");
		}
		
		@Override
		protected void runVoid(Arguments args) throws ConfigurationException {
			Map<MGID, Short> compactMap = new HashMap<>();
			int maxId = -1;
			
			while (true) {
				MGID ore = args.get(null, MGID.class, null);
				if (ore == null) break;
				
				short ubBase = args.getBlock(null);
				compactMap.put(ore, ubBase);
				
				if (maxId < ore.id) maxId = ore.id;
			}
			
			if (maps == null) {
				maps = new short[maxId + 1][];
			} else if (maps.length <= maxId) {
				short[][] newMaps = new short[maxId + 1][];
				System.arraycopy(maps, 0, newMaps, 0, maps.length);
				maps = newMaps;
			}
			
			for (Entry<MGID, Short> entry : compactMap.entrySet()) {
				MGID ore = entry.getKey();
				short ubBase = entry.getValue();
				
				if (maps[ore.id] == null) {
					maps[ore.id] = new short[MAP_LENGTH];
				} else {
					assert maps[ore.id].length == MAP_LENGTH;
				}
				
				if (ore.isMetaWildcard()) {
					for (int i = 0; i < MAP_LENGTH; ++i) {
						maps[ore.id][i] = ubBase;
					}
				} else {
					maps[ore.id][ore.meta] = ubBase;
				}
			}
		}
	}

}
