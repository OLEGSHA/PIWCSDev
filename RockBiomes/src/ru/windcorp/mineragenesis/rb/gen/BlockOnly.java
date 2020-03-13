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

import ru.windcorp.mineragenesis.rb.RockBiomesCP.Workspace;
import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;
import ru.windcorp.mineragenesis.request.ChunkData;

public class BlockOnly implements BlockSupplier, BlockPredicate {
	
	private final short block;

	public BlockOnly(short block) {
		this.block = block;
	}

	@Override
	public void addBlocks(BlockData data, BlockCollector collector, Workspace w) {
		collector.addBlock(block, 1);
	}
	
	@Override
	public boolean check(short mgid) {
		return mgid == block;
	}
	
	public static BlockOnly build(Arguments args) throws ConfigurationException {
		return new BlockOnly(args.getBlock(null));
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BlockOnly[" + ChunkData.getId(block) + ":" + ChunkData.getMeta(block) + "]";
	}
	
}