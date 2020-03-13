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

import java.util.ArrayList;
import java.util.List;

import ru.windcorp.mineragenesis.rb.RockBiomesCP.Workspace;
import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;
import ru.windcorp.mineragenesis.request.ChunkData;

public class BlockMix implements BlockSupplier {
	
	private final short[] mgids;

	public BlockMix(short[] mgids) {
		this.mgids = mgids;
	}

	@Override
	public void addBlocks(BlockData data, BlockCollector collector, Workspace w) {
		for (short mgid : mgids) {
			collector.addBlock(mgid, 1);
		}
	}
	
	public static BlockMix build(Arguments args) throws ConfigurationException {
		List<Short> boxed = new ArrayList<Short>();
		
		while (true) {
			Short obj = args.getBlockOrNull(null);
			if (obj == null) break;
			boxed.add(obj);
		}
		
		short[] mgids = new short[boxed.size()];
		for (int i = 0; i < mgids.length; i++) {
			mgids[i] = boxed.get(i);
		}
		
		return new BlockMix(mgids);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("BlockMix[");
		
		for (short block : mgids) {
			sb.append(ChunkData.getId(block));
			sb.append(':');
			sb.append(ChunkData.getMeta(block));
			sb.append("; ");
		}
		
		sb.setLength(sb.length() - "; ".length());
		
		return sb.append("]").toString();
	}
	
}