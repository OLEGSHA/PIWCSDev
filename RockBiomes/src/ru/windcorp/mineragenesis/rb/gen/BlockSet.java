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

import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;
import ru.windcorp.mineragenesis.rb.config.MGID;

import static ru.windcorp.mineragenesis.request.ChunkData.*;

/**
 * @author Javapony
 *
 */
public class BlockSet implements BlockPredicate {
	
	private static final int ANY_META = (1 << (1 << METADATA_SIZE)) - 1;
	
	private final int[] data;
	private final int offset;
	
	public BlockSet(MGID[] blocks) {
		int min = Integer.MAX_VALUE;
		int max = 0;
		
		for (MGID block : blocks) {
			if (min > block.id) min = block.id;
			if (max < block.id) max = block.id;
		}
		
		data = new int[max - min + 1];
		offset = min;
		
		for (MGID block : blocks) {
			data[block.id - min] |=
					block.meta == MGID.META_WILDCARD ? ANY_META : 1 << block.meta;
		}
	}
	
	@Override
	public boolean check(short mgid) {
		int index = getId(mgid) - offset;
		return (index >= 0) && (index < data.length) &&
				((data[index] & (1 << getMeta(mgid))) != 0);
	}
	
	public static BlockSet build(Arguments args) throws ConfigurationException {
		return new BlockSet(args.get().toArray(new MGID[0]));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("BlockSet[");
		
		for (int index = 0; index < data.length; index++) {
			if (data[index] == 0) continue;
			
			if (data[index] == ANY_META) {
				sb.append(index + offset); // ID
				sb.append(":*; ");
			} else {
				for (int meta = 0; meta < (1 << METADATA_SIZE); ++meta) {
					if ((data[index] & (1 << meta)) != 0) {
						sb.append(index + offset); // ID
						sb.append(':');
						sb.append(meta);
						sb.append("; ");
					}
				}
			}
		}
		
		sb.setLength(sb.length() - 2); // 2 == "; ".length()
		sb.append(']');
		return sb.toString();
	}

}
