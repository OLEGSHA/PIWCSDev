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

import ru.windcorp.mineragenesis.rb.RockBiomesAddon;
import ru.windcorp.mineragenesis.rb.RockBiomesCP.Workspace;
import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;
import ru.windcorp.mineragenesis.request.ChunkData;

public class RockBiome implements BlockSupplier {
	
	private static int nextId = 0;
	
	private final String name;
	private final int id = nextId++;
	private final Deposit[] deposits;
	private final BlockSupplier rock;
	private final BlockPredicate replaceableBlocks;
	
	public RockBiome(String name, Deposit[] deposits, BlockSupplier rock, BlockPredicate replaceables) {
		this.name = name;
		this.deposits = deposits;
		this.rock = rock;
		this.replaceableBlocks = replaceables;
	}

	public static RockBiome build(Arguments args) throws ConfigurationException {
		return new RockBiome(
				args.get("name", String.class),
				args.get(null, Deposit[].class),
				args.get("rock", BlockSupplier.class),
				args.get("replace", BlockPredicate.class)
		);
	}

	@Override
	public void addBlocks(BlockData data, BlockCollector collector, Workspace w) {
		collector.addBlockSupplier(this::addBlocks0, 1);
	}
	
	protected void addBlocks0(BlockData block, BlockCollector collector, Workspace w) {
		for (Deposit deposit : deposits) {
			deposit.addBlocks(block, collector, w);
		}
		
		if (RockBiomesAddon.CAN_DEBUG && RockBiomesAddon.isDebugging()) {
			if (
					
					(block.yInt == 1)
					||
					(block.yInt == 20 && ((block.column.xInt & 0xF) == 0 || (block.column.zInt & 0xF) == 0))
					
					) {
				rock.addBlocks(block, collector, w);
			} else {
				collector.addBlock(ChunkData.AIR_MGID, 1);
			}
		} else {
			if (getReplaceableBlocks().check(block.original)) {
				rock.addBlocks(block, collector, w);
			}
		}
	}
	
	public void cacheColumn(ColumnData column, Workspace w) {
		for (Deposit d : deposits) d.cacheColumn(column, w.getDepositCache(d));
	}
	
	/**
	 * @return the deposits
	 */
	public Deposit[] getDeposits() {
		return deposits;
	}
	
	/**
	 * @return the rock
	 */
	public BlockSupplier getRock() {
		return rock;
	}
	
	/**
	 * @return the replaceables
	 */
	public BlockPredicate getReplaceableBlocks() {
		return replaceableBlocks;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return "RB " + name + " (" + id + ")";
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	// Using Object's identity-based equals()
	@Override
	public int hashCode() {
		return id;
	}

}
